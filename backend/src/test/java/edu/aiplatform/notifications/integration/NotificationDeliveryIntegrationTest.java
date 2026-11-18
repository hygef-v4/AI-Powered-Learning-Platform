package edu.aiplatform.notifications.integration;

import edu.aiplatform.notifications.repository.NotificationRepositoryAdapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.notifications.service.*;
import edu.aiplatform.notifications.model.*;
import edu.aiplatform.notifications.repository.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** Real U14 SQL/transactions; U01/U05/U07/U08, SMTP, job queue and SSE remain test boundaries. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NotificationDeliveryIntegrationTest.Config.class)
@Testcontainers(disabledWithoutDocker = true)
class NotificationDeliveryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withPassword(UUID.randomUUID().toString());

    static final Instant NOW = Instant.parse("2026-10-25T08:00:00Z");
    @Autowired DataSource dataSource;
    @Autowired IAssignmentQuery assignmentQuery;
    @Autowired IClassAccess classAccess;
    @Autowired ISubmissionQuery submissionQuery;
    @Autowired IJob job;
    @Autowired IAccountLookup accountLookup;
    @Autowired ISseHub sseHub;
    @Autowired DeadlineReminderScanner deadlineReminderScanner;
    @Autowired NotificationEventService notificationEventService;
    @Autowired INotificationRepository notificationRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired NotificationService notificationService;
    @Autowired edu.aiplatform.announcements.repository.IAnnouncementRepository announcementRepository;
    @Autowired IMail mail;
    @Autowired EmailDeliveryService emailDeliveryService;

    @TestConfiguration
    @EnableTransactionManagement
    @Import({
        NotificationRepositoryAdapter.class,
        edu.aiplatform.announcements.repository.AnnouncementRepositoryAdapter.class,
        NotificationService.class,
        EmailDeliveryService.class,
        DeadlineReminderScanner.class,
        NotificationEventService.class,
        RecipientResolver.class
    })
    static class Config {
        @Bean
        DataSource dataSource() {
            var source =
                    new DriverManagerDataSource(
                            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("create role app nologin");
            jdbc.execute("create table course_classes (id uuid primary key)");
            Flyway.configure()
                    .dataSource(source)
                    .locations("classpath:db/migration/notifications")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load()
                    .migrate();
            return source;
        }

        @Bean
        JdbcClient jdbcClient(DataSource source) {
            return JdbcClient.create(source);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }

        @Bean
        IAssignmentQuery assignmentQuery() {
            return mock(IAssignmentQuery.class);
        }

        @Bean
        IClassAccess classAccess() {
            return mock(IClassAccess.class);
        }

        @Bean
        ISubmissionQuery submissionQuery() {
            return mock(ISubmissionQuery.class);
        }

        @Bean
        IJob job() {
            return mock(IJob.class);
        }

        @Bean
        IAccountLookup accountLookup() {
            return mock(IAccountLookup.class);
        }

        @Bean
        IMail mail() {
            return mock(IMail.class);
        }

        @Bean
        ISseHub sseHub() {
            return mock(ISseHub.class);
        }

        @Bean
        NotificationAccess access() {
            return mock(NotificationAccess.class);
        }

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        EmailCapPolicy emailCapPolicy(INotificationRepository notificationRepository) {
            return new EmailCapPolicy(notificationRepository, 300);
        }
    }

    @BeforeEach
    void clean() {
        reset(assignmentQuery, classAccess, submissionQuery, job, accountLookup, sseHub, mail);
        new JdbcTemplate(dataSource)
                .execute("truncate notifications, email_deliveries, deadline_reminders, notification_preferences, announcements");
    }

    private Notification createNotice(UUID recipient, Instant createdAt) {
        var notice = new Notification();
        notice.id = UUID.randomUUID();
        notice.recipientId = recipient;
        notice.type = NotificationType.ENROLLED;
        notice.sourceEventId = UUID.randomUUID();
        notice.title = "Enrollment";
        notice.summary = "Open class";
        notice.link = "/classes/fixture";
        notice.createdAt = createdAt;
        return notice;
    }

    private EmailDelivery createDelivery(Notification notice, String type, EmailDelivery.Status status) {
        var delivery = new EmailDelivery();
        delivery.id = UUID.randomUUID();
        delivery.notificationId = notice.id;
        delivery.recipientId = notice.recipientId;
        delivery.type = type;
        delivery.status = status;
        delivery.priority = EmailCapPolicy.PRIORITY.get(type);
        delivery.scheduledFor = NOW;
        delivery.createdAt = notice.createdAt;
        return delivery;
    }

    @Test
    void retentionDeletesOnlyOlderThan180DaysAndCascadesOnlyTheirDeliveries() {
        Instant cutoff = NOW.minus(Duration.ofDays(180));
        var old = createNotice(UUID.randomUUID(), cutoff.minusSeconds(1));
        var exact = createNotice(old.recipientId, cutoff);
        var newer = createNotice(old.recipientId, cutoff.plusSeconds(1));
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(tx -> {
            for (var notice : List.of(old, exact, newer)) {
                assertThat(notificationRepository.insertNotification(notice)).isTrue();
                notificationRepository.insertEmailDelivery(
                        createDelivery(notice, "ENROLLED", EmailDelivery.Status.SENT));
            }
        });
        int deletedCount =
                transaction.execute(tx -> notificationRepository.deleteOldNotifications(cutoff));
        assertThat(deletedCount).isEqualTo(1);
        var jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForList("select id from notifications", UUID.class))
                .containsExactlyInAnyOrder(exact.id, newer.id);
        assertThat(jdbc.queryForList("select notification_id from email_deliveries", UUID.class))
                .containsExactlyInAnyOrder(exact.id, newer.id);
    }

    @Test
    void savedOptOutPersistsWhileInAppNoticeAndOwnerOnlyReadStateRemain() {
        UUID recipient = UUID.randomUUID(), other = UUID.randomUUID();
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(
                tx -> notificationRepository.savePreference(recipient, "ENROLLED", false));
        notificationService.createNotification(
                UUID.randomUUID(),
                NotificationType.ENROLLED,
                List.of(recipient, other),
                "Enrollment",
                "Open class",
                "/classes/fixture",
                "ENROLLED",
                null);
        var jdbc = new JdbcTemplate(dataSource);
        UUID ownId = jdbc.queryForObject("select id from notifications where recipient_id=?", UUID.class, recipient);
        UUID otherId = jdbc.queryForObject("select id from notifications where recipient_id=?", UUID.class, other);
        assertThat(jdbc.queryForObject("select status from email_deliveries where notification_id=?",
                String.class, ownId)).isEqualTo("SKIPPED");
        Map<String, Boolean> savedPreferences =
                transaction.execute(tx -> notificationRepository.findPreferences(recipient));
        assertThat(savedPreferences).containsEntry("ENROLLED", false);
        boolean foreignRead =
                transaction.execute(tx -> notificationRepository.markRead(otherId, recipient, NOW));
        boolean ownRead =
                transaction.execute(tx -> notificationRepository.markRead(ownId, recipient, NOW));
        assertThat(foreignRead).isFalse();
        assertThat(ownRead).isTrue();
        var ownUnread = createNotice(recipient, NOW.plusSeconds(1));
        transaction.executeWithoutResult(
                tx -> notificationRepository.insertNotification(ownUnread));
        transaction.executeWithoutResult(
                tx -> notificationRepository.markAllRead(recipient, NOW.plusSeconds(1)));
        assertThat(jdbc.queryForObject("select read_at from notifications where id=?",
                java.sql.Timestamp.class, otherId)).isNull();
        assertThat(jdbc.queryForObject("select read_at from notifications where id=?",
                java.sql.Timestamp.class, ownId).toInstant()).isEqualTo(NOW);
        assertThat(jdbc.queryForObject("select read_at from notifications where id=?",
                java.sql.Timestamp.class, ownUnread.id).toInstant()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void concurrentSchedulesReserveOnlyAvailableDailySlot() throws Exception {
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        var notices = java.util.stream.IntStream.range(0, 4)
                .mapToObj(index -> createNotice(UUID.randomUUID(), NOW)).toList();
        transaction.executeWithoutResult(
                tx -> notices.forEach(notificationRepository::insertNotification));
        var emailDeliveryService = new EmailDeliveryService(notificationRepository,
                new EmailCapPolicy(
                        notificationRepository, 1), job, accountLookup, mock(IMail.class),
                Clock.fixed(NOW, ZoneOffset.UTC), assignmentQuery, "https://learning.example.test");
        var start = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            var futures = notices.stream().map(notice -> executor.submit(() -> {
                start.await();
                transaction.executeWithoutResult(
                        tx -> emailDeliveryService.scheduleEmail(notice, "ENROLLED", null));
                return null;
            })).toList();
            start.countDown();
            for (var future : futures) future.get(15, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        var jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("select count(*) from email_deliveries where status='PENDING'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from email_deliveries where status='DEFERRED'", Integer.class))
                .isEqualTo(3);
        verify(job).enqueue(anyString(), anyMap(), anyString());
    }

    @Test
    void deferredRepositoryOrdersEveryPriorityAndExcludesFutureReservations() {
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        List<String> types = List.of("ENROLLED", "GRADE_PUBLISHED", "ASSIGNMENT_OPENED",
                "ANNOUNCEMENT_IMPORTANT", "DEADLINE_REMINDER", "BACKUP_FAILED");
        transaction.executeWithoutResult(tx -> {
            for (String type : types) {
                var notice = createNotice(UUID.randomUUID(), NOW);
                notificationRepository.insertNotification(notice);
                notificationRepository.insertEmailDelivery(
                        createDelivery(notice, type, EmailDelivery.Status.DEFERRED));
            }
            var future = createNotice(UUID.randomUUID(), NOW);
            notificationRepository.insertNotification(future);
            var delivery = createDelivery(future, "BACKUP_FAILED", EmailDelivery.Status.DEFERRED);
            delivery.scheduledFor = NOW.plusSeconds(1);
            notificationRepository.insertEmailDelivery(delivery);
        });
        List<EmailDelivery> deferred =
                transaction.execute(tx -> notificationRepository.listDeferredEmails(NOW));
        assertThat(deferred)
                .extracting(delivery -> delivery.type)
                .containsExactly("BACKUP_FAILED", "DEADLINE_REMINDER", "ANNOUNCEMENT_IMPORTANT",
                        "ASSIGNMENT_OPENED", "GRADE_PUBLISHED", "ENROLLED");
    }

    @Test
    void announcementRepositoryExcludesDeletedAndForeignClassAndOrdersNewestFirst() {
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        UUID classId = UUID.randomUUID(), otherClass = UUID.randomUUID(), teacher = UUID.randomUUID();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.update("insert into course_classes(id) values (?),(?)", classId, otherClass);
        var old = edu.aiplatform.announcements.model.Announcement.createAnnouncement(classId, teacher,
                "Older", "Body", false, false, NOW.minusSeconds(10));
        var recent = edu.aiplatform.announcements.model.Announcement.createAnnouncement(classId, teacher,
                "Newest", "Body", false, false, NOW);
        var deleted = edu.aiplatform.announcements.model.Announcement.createAnnouncement(classId, teacher,
                "Deleted", "Body", false, false, NOW.plusSeconds(1));
        deleted.markDeleted(teacher, NOW.plusSeconds(2));
        var foreign = edu.aiplatform.announcements.model.Announcement.createAnnouncement(otherClass, teacher,
                "Foreign", "Body", false, false, NOW.plusSeconds(3));
        transaction.executeWithoutResult(tx -> List.of(old, recent, deleted, foreign)
                .forEach(announcementRepository::insertAnnouncement));
        List<edu.aiplatform.announcements.model.Announcement> feed = transaction.execute(
                tx -> announcementRepository.listAnnouncements(List.of(classId), 1, 20));
        assertThat(feed)
                .extracting(announcement -> announcement.id).containsExactly(recent.id, old.id);
    }

    ClassRef course(UUID classId, UUID subject) {
        return new ClassRef(
                classId, subject, "C", "Class", ClassStatus.OPEN, UUID.randomUUID(), false);
    }

    @Test
    void failedEmailEnqueueRollsBackMarkerNotificationAndEmailThenNextScanRetriesExactlyOnce() {
        UUID assignment = UUID.randomUUID(), subject = UUID.randomUUID();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), student = UUID.randomUUID();
        var a =
                new AssignmentView(
                        assignment,
                        AssignmentScope.SUBJECT,
                        null,
                        subject,
                        1,
                        "Bài",
                        SubmissionMode.INDIVIDUAL,
                        GradingMode.GRADED,
                        false,
                        AssignmentStatus.OPEN,
                        NOW.minusSeconds(60),
                        NOW.plus(Duration.ofHours(24)),
                        null,
                        null,
                        false,
                        null,
                        List.of());
        when(assignmentQuery.listClosingBetween(any(), any())).thenReturn(List.of(a));
        when(assignmentQuery.get(assignment)).thenReturn(Optional.of(a));
        when(classAccess.listOpenClassesOfSubject(subject))
                .thenReturn(List.of(course(first, subject), course(second, subject)));
        for (UUID classId : List.of(first, second)) {
            when(classAccess.getClassRef(classId))
                    .thenReturn(Optional.of(course(classId, subject)));
            when(classAccess.listActiveStudents(classId)).thenReturn(List.of(student));
            when(submissionQuery.listStudentsWithoutSubmission(assignment, classId))
                    .thenReturn(List.of(student));
        }
        doAnswer(
                        call -> {
                            assertThat(
                                            new JdbcTemplate(dataSource)
                                                    .queryForObject(
                                                            "select count(*) from email_deliveries",
                                                            Integer.class))
                                    .isEqualTo(1);
                            throw new IllegalStateException("fixture enqueue unavailable");
                        })
                .doNothing()
                .when(job)
                .enqueue(anyString(), anyMap(), anyString());
        deadlineReminderScanner.scan(NOW);
        var jdbc = new JdbcTemplate(dataSource);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from deadline_reminders where class_id = ?",
                                Integer.class,
                                first))
                .isZero();
        assertThat(jdbc.queryForObject("select count(*) from deadline_reminders", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from notifications", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from email_deliveries", Integer.class))
                .isEqualTo(1);
        deadlineReminderScanner.scan(NOW.plusSeconds(60));
        deadlineReminderScanner.scan(NOW.plusSeconds(120));
        for (String table : List.of("deadline_reminders", "notifications", "email_deliveries")) {
            assertThat(jdbc.queryForObject("select count(*) from " + table, Integer.class))
                    .as(table)
                    .isEqualTo(2);
        }
        verify(job, times(3)).enqueue(anyString(), anyMap(), anyString());
        verify(sseHub, times(2)).publish(anyString(), anyMap());
    }

    @Test
    void replayedSubjectOpeningKeepsPerClassLinksAndDoesNotDuplicateNotificationOrEmail() {
        UUID assignment = UUID.randomUUID(), subject = UUID.randomUUID();
        var ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        for (UUID classId : ids) {
            UUID student = UUID.randomUUID();
            when(classAccess.getClassRef(classId))
                    .thenReturn(Optional.of(course(classId, subject)));
            when(classAccess.listActiveStudents(classId)).thenReturn(List.of(student));
            when(classAccess.isActiveStudent(student, classId)).thenReturn(true);
            when(accountLookup.getContact(student))
                    .thenReturn(
                            Optional.of(
                                    new AccountContact(
                                            student,
                                            "fixture@example.test",
                                            "Student",
                                            Role.STUDENT,
                                            AccountStatus.ACTIVE)));
        }
        when(classAccess.listOpenClassesOfSubject(subject))
                .thenReturn(ids.stream().map(id -> course(id, subject)).toList());
        var event =
                new DomainEvent(
                        1,
                        UUID.randomUUID(),
                        EventTypes.ASSIGNMENT_OPENED,
                        "U07",
                        NOW,
                        null,
                        Map.of(
                                "assignmentId",
                                assignment.toString(),
                                "scope",
                                "SUBJECT",
                                "subjectId",
                                subject.toString()));
        notificationEventService.handleEvent(event);
        notificationEventService.handleEvent(event);
        var jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForList("select link from notifications", String.class))
                .containsExactlyInAnyOrderElementsOf(
                        ids.stream()
                                .map(id -> "/my-assignments/" + assignment + "?classId=" + id)
                                .toList());
        assertThat(jdbc.queryForObject("select count(*) from email_deliveries", Integer.class))
                .isEqualTo(2);
        verify(job, times(2)).enqueue(anyString(), anyMap(), anyString());
        verify(sseHub, times(2)).publish(anyString(), anyMap());
    }

    private org.springframework.transaction.support.TransactionTemplate createTransaction() {
        return new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    }

    private EmailDelivery insertDelivery(
            String type, EmailDelivery.Status status, Instant scheduledFor, Instant expiresAt) {
        var notice = createNotice(UUID.randomUUID(), NOW);
        var delivery = createDelivery(notice, type, status);
        delivery.scheduledFor = scheduledFor;
        delivery.expiresAt = expiresAt;
        createTransaction().executeWithoutResult(tx -> {
            notificationRepository.insertNotification(notice);
            notificationRepository.insertEmailDelivery(delivery);
        });
        when(accountLookup.getContact(notice.recipientId))
                .thenReturn(Optional.of(new AccountContact(notice.recipientId,
                        "fixture@example.test", "Student", Role.STUDENT, AccountStatus.ACTIVE)));
        return delivery;
    }

    private Map<String, Object> readDelivery(UUID id) {
        return new JdbcTemplate(dataSource).queryForMap(
                "select status,attempts,last_error,sent_at,scheduled_for from email_deliveries where id=?",
                id);
    }

    private static Instant readInstant(Map<String, Object> row, String column) {
        return row.get(column) == null ? null : ((java.sql.Timestamp) row.get(column)).toInstant();
    }

    private boolean tryLockFromOtherConnection(UUID id, String lockClause) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "select id from email_deliveries where id=? " + lockClause + " nowait")) {
            connection.setAutoCommit(false);
            statement.setObject(1, id);
            statement.executeQuery().close();
            connection.rollback();
            return true;
        } catch (java.sql.SQLException failure) {
            if ("55P03".equals(failure.getSQLState())) return false;
            throw new IllegalStateException(failure);
        }
    }

    @Test
    void announcementRepositoryRoundTripsAndUpdatesOnlyActiveRowWithExpectedVersion() {
        UUID classId = UUID.randomUUID(), teacher = UUID.randomUUID();
        new JdbcTemplate(dataSource).update("insert into course_classes(id) values (?)", classId);
        var original = edu.aiplatform.announcements.model.Announcement.createAnnouncement(
                classId, teacher, "Title", "Body", true, true, NOW);
        createTransaction().executeWithoutResult(
                tx -> announcementRepository.insertAnnouncement(original));
        var stored = createTransaction().execute(
                tx -> announcementRepository.findAnnouncement(original.id).orElseThrow());
        assertThat(stored).usingRecursiveComparison().isEqualTo(original);
        assertThat(stored.version).isZero();
        assertThat(announcementRepository.findAnnouncement(UUID.randomUUID())).isEmpty();

        stored.edit("Changed", "New body", teacher, NOW.plusSeconds(5));
        assertThat(isTrueInTransaction(
                () -> announcementRepository.updateAnnouncement(stored, 1))).isFalse();
        assertThat(isTrueInTransaction(
                () -> announcementRepository.updateAnnouncement(stored, 0))).isTrue();
        assertThat(stored.version).isEqualTo(1);
        var edited = createTransaction().execute(
                tx -> announcementRepository.findAnnouncement(original.id).orElseThrow());
        assertThat(edited).usingRecursiveComparison().isEqualTo(stored);

        edited.markDeleted(teacher, NOW.plusSeconds(9));
        assertThat(isTrueInTransaction(
                () -> announcementRepository.updateAnnouncement(edited, 1))).isTrue();
        var deleted = createTransaction().execute(
                tx -> announcementRepository.findAnnouncement(original.id).orElseThrow());
        assertThat(deleted.status)
                .isEqualTo(edu.aiplatform.announcements.model.Announcement.Status.DELETED);
        assertThat(deleted.deletedAt).isEqualTo(NOW.plusSeconds(9));
        assertThat(deleted.version).isEqualTo(2);
        assertThat(isTrueInTransaction(
                () -> announcementRepository.updateAnnouncement(deleted, 2))).isFalse();
        List<edu.aiplatform.announcements.model.Announcement> noClassFeed =
                announcementRepository.listAnnouncements(List.of(), 1, 20);
        assertThat(noClassFeed).isEmpty();
    }

    private boolean isTrueInTransaction(java.util.function.BooleanSupplier action) {
        return Boolean.TRUE.equals(createTransaction().execute(tx -> action.getAsBoolean()));
    }

    @Test
    void notificationReadsPageCountAndRoundTripOnlyRecipientRows() {
        UUID recipient = UUID.randomUUID();
        var oldest = createNotice(recipient, NOW.minusSeconds(2));
        var middle = createNotice(recipient, NOW.minusSeconds(1));
        var newest = createNotice(recipient, NOW);
        var foreign = createNotice(UUID.randomUUID(), NOW.plusSeconds(1));
        createTransaction().executeWithoutResult(tx -> List.of(oldest, middle, newest, foreign)
                .forEach(notificationRepository::insertNotification));
        assertThat(isTrueInTransaction(
                () -> notificationRepository.insertNotification(newest))).isFalse();
        var transaction = createTransaction();
        List<Notification> firstPage = transaction.execute(
                tx -> notificationRepository.listNotifications(recipient, false, 1, 2));
        assertThat(firstPage).extracting(notice -> notice.id)
                .containsExactly(newest.id, middle.id);
        List<Notification> secondPage = transaction.execute(
                tx -> notificationRepository.listNotifications(recipient, false, 2, 2));
        assertThat(secondPage).extracting(notice -> notice.id).containsExactly(oldest.id);
        assertThat(isTrueInTransaction(
                () -> notificationRepository.markRead(middle.id, recipient, NOW))).isTrue();
        assertThat(isTrueInTransaction(
                () -> notificationRepository.markRead(UUID.randomUUID(), recipient, NOW))).isFalse();
        List<Notification> unreadPage = transaction.execute(
                tx -> notificationRepository.listNotifications(recipient, true, 1, 10));
        assertThat(unreadPage).extracting(notice -> notice.id)
                .containsExactly(newest.id, oldest.id);
        Long unreadCount = transaction.execute(tx -> notificationRepository.countUnread(recipient));
        assertThat(unreadCount).isEqualTo(2);
        var stored = transaction.execute(
                tx -> notificationRepository.findNotification(middle.id).orElseThrow());
        middle.readAt = NOW;
        assertThat(stored).usingRecursiveComparison().isEqualTo(middle);
    }

    @Test
    void preferenceUpsertKeepsOneRowPerAccountAndType() {
        UUID account = UUID.randomUUID();
        var transaction = createTransaction();
        transaction.executeWithoutResult(tx -> {
            notificationRepository.savePreference(account, "ENROLLED", false);
            notificationRepository.savePreference(account, "ENROLLED", true);
            notificationRepository.savePreference(account, "GRADE_PUBLISHED", false);
        });
        Map<String, Boolean> saved =
                transaction.execute(tx -> notificationRepository.findPreferences(account));
        assertThat(saved).containsExactlyInAnyOrderEntriesOf(
                Map.of("ENROLLED", true, "GRADE_PUBLISHED", false));
        Map<String, Boolean> unknown = transaction.execute(
                tx -> notificationRepository.findPreferences(UUID.randomUUID()));
        assertThat(unknown).isEmpty();
    }

    @Test
    void sentEmailPersistsSentStateAttemptAndTime() {
        var delivery = insertDelivery("ENROLLED", EmailDelivery.Status.PENDING, NOW, null);
        emailDeliveryService.sendEmail(delivery.id);
        var row = readDelivery(delivery.id);
        assertThat(row.get("status")).isEqualTo("SENT");
        assertThat(row.get("attempts")).isEqualTo(1);
        assertThat(row.get("last_error")).isNull();
        assertThat(readInstant(row, "sent_at")).isEqualTo(NOW);
        verify(mail).send(any());
    }

    @Test
    void mailFailureKeepsEmailPendingButPersistsTheAttempt() {
        var delivery = insertDelivery("ENROLLED", EmailDelivery.Status.PENDING, NOW, null);
        doThrow(new IllegalStateException("smtp down")).when(mail).send(any());
        assertThatThrownBy(() -> emailDeliveryService.sendEmail(delivery.id))
                .isInstanceOf(RetryableJobException.class);
        var row = readDelivery(delivery.id);
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("attempts")).isEqualTo(1);
        assertThat(readInstant(row, "sent_at")).isNull();
    }

    @Test
    void expiredOrIneligibleEmailIsSkippedWithoutSending() {
        var expired = insertDelivery("ENROLLED", EmailDelivery.Status.PENDING, NOW, NOW);
        var invalidLink = insertDelivery("DEADLINE_REMINDER", EmailDelivery.Status.PENDING, NOW,
                NOW.plusSeconds(60));
        emailDeliveryService.sendEmail(expired.id);
        emailDeliveryService.sendEmail(invalidLink.id);
        assertThat(readDelivery(expired.id).get("status")).isEqualTo("SKIPPED");
        assertThat(readDelivery(invalidLink.id))
                .containsEntry("status", "SKIPPED")
                .containsEntry("last_error", "INVALID_ASSIGNMENT_LINK")
                .containsEntry("attempts", 0);
        verify(mail, never()).send(any());
    }

    @Test
    void pendingEmailFromAnotherDayIsDeferredWhenTodayHasNoCapacity() {
        var delivery = insertDelivery("ENROLLED", EmailDelivery.Status.PENDING,
                NOW.minus(Duration.ofDays(1)), null);
        var noCapacityService = new EmailDeliveryService(notificationRepository,
                new EmailCapPolicy(notificationRepository, 0), job, accountLookup, mail,
                Clock.fixed(NOW, ZoneOffset.UTC), assignmentQuery, "https://learning.example.test");
        createTransaction().executeWithoutResult(tx -> noCapacityService.sendEmail(delivery.id));
        var row = readDelivery(delivery.id);
        assertThat(row.get("status")).isEqualTo("DEFERRED");
        assertThat(readInstant(row, "scheduled_for"))
                .isEqualTo(new EmailCapPolicy(notificationRepository, 0).calculateNextDay(NOW));
        verify(mail, never()).send(any());
    }

    @Test
    void exhaustedRetriesMarkOnlyPendingEmailFailed() {
        var pending = insertDelivery("ENROLLED", EmailDelivery.Status.PENDING, NOW, null);
        var sent = insertDelivery("ENROLLED", EmailDelivery.Status.SENT, NOW, null);
        emailDeliveryService.markEmailFailed(pending.id);
        emailDeliveryService.markEmailFailed(sent.id);
        assertThat(readDelivery(pending.id))
                .containsEntry("status", "FAILED")
                .containsEntry("attempts", 5)
                .containsEntry("last_error", "MAIL_RETRIES_EXHAUSTED");
        assertThat(readDelivery(sent.id)).containsEntry("status", "SENT").containsEntry("attempts", 0);
    }

    @Test
    void deferredReleaseCountsEmailsReleasedEarlierInTheSameRun() {
        Instant due = NOW.minus(Duration.ofHours(1));
        var backup = insertDelivery("BACKUP_FAILED", EmailDelivery.Status.DEFERRED, due, null);
        var reminder = insertDelivery("DEADLINE_REMINDER", EmailDelivery.Status.DEFERRED, due, null);
        var expired = insertDelivery("ANNOUNCEMENT_IMPORTANT", EmailDelivery.Status.DEFERRED, due,
                NOW.minusSeconds(1));
        var enrolled = insertDelivery("ENROLLED", EmailDelivery.Status.DEFERRED, due, null);
        var capTwoService = new EmailDeliveryService(notificationRepository,
                new EmailCapPolicy(notificationRepository, 2), job, accountLookup, mail,
                Clock.fixed(NOW, ZoneOffset.UTC), assignmentQuery, "https://learning.example.test");
        createTransaction().executeWithoutResult(tx -> capTwoService.releaseDeferredEmails(NOW));
        for (var released : List.of(backup, reminder)) {
            var row = readDelivery(released.id);
            assertThat(row.get("status")).isEqualTo("PENDING");
            assertThat(readInstant(row, "scheduled_for")).isEqualTo(NOW);
        }
        assertThat(readDelivery(expired.id).get("status")).isEqualTo("SKIPPED");
        assertThat(readDelivery(enrolled.id).get("status")).isEqualTo("DEFERRED");
        verify(job, times(2)).enqueue(anyString(), anyMap(), anyString());
    }

    /**
     * Hibernate 6.6 trên PostgreSQL bỏ qua hint lock.timeout=2000 của em.find: khi caller chưa đặt
     * lock_timeout (markEmailFailed không lấy budget lock), lệnh khóa chờ tới khi hàng được nhả.
     */
    @Test
    void emailRowLockAllowsKeyShareAndWaitsForTheHolderWithoutOwnTimeout() throws Exception {
        var delivery = insertDelivery("ENROLLED", EmailDelivery.Status.PENDING, NOW, null);
        createTransaction().executeWithoutResult(tx -> {
            assertThat(notificationRepository.lockEmailDelivery(delivery.id)).isPresent();
            assertThat(tryLockFromOtherConnection(delivery.id, "for key share")).isTrue();
            assertThat(tryLockFromOtherConnection(delivery.id, "for no key update")).isFalse();
        });
        var locked = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var holder = executor.submit(() -> {
                try (var connection = dataSource.getConnection();
                        var statement = connection.prepareStatement(
                                "select id from email_deliveries where id=? for update")) {
                    connection.setAutoCommit(false);
                    statement.setObject(1, delivery.id);
                    statement.executeQuery().close();
                    locked.countDown();
                    Thread.sleep(4000);
                    connection.rollback();
                }
                return null;
            });
            assertThat(locked.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            long started = System.nanoTime();
            Optional<EmailDelivery> lockedDelivery = createTransaction().execute(
                    tx -> notificationRepository.lockEmailDelivery(delivery.id));
            long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
            assertThat(lockedDelivery).isPresent();
            assertThat(elapsedMillis).isGreaterThanOrEqualTo(3500);
            holder.get(15, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }
}
