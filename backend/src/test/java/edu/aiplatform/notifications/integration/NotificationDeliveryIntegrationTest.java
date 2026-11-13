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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
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
    @Autowired EntityManager em;
    @Autowired IAssignmentQuery assignments;
    @Autowired IClassAccess classes;
    @Autowired ISubmissionQuery submissions;
    @Autowired IJob jobs;
    @Autowired IAccountLookup accounts;
    @Autowired ISseHub sse;
    @Autowired DeadlineReminderScanner scanner;
    @Autowired NotificationEventService events;
    @Autowired INotificationRepository notificationRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired NotificationService notificationService;
    @Autowired edu.aiplatform.announcements.repository.IAnnouncementRepository announcementRepository;

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
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(source);
            factory.setPackagesToScan(Notification.class.getPackageName(),
                    edu.aiplatform.announcements.model.Announcement.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(
                    Map.of(
                            "hibernate.hbm2ddl.auto",
                            "validate",
                            "hibernate.jdbc.time_zone",
                            "UTC",
                            "hibernate.physical_naming_strategy",
                            "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
            return factory;
        }

        @Bean
        EntityManager entityManager(EntityManagerFactory factory) {
            return SharedEntityManagerCreator.createSharedEntityManager(factory);
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        IAssignmentQuery assignments() {
            return mock(IAssignmentQuery.class);
        }

        @Bean
        IClassAccess classes() {
            return mock(IClassAccess.class);
        }

        @Bean
        ISubmissionQuery submissions() {
            return mock(ISubmissionQuery.class);
        }

        @Bean
        IJob jobs() {
            return mock(IJob.class);
        }

        @Bean
        IAccountLookup accounts() {
            return mock(IAccountLookup.class);
        }

        @Bean
        IMail mail() {
            return mock(IMail.class);
        }

        @Bean
        ISseHub sse() {
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
        EmailCapPolicy cap(INotificationRepository repo) {
            return new EmailCapPolicy(repo, 300);
        }
    }

    @BeforeEach
    void clean() {
        reset(assignments, classes, submissions, jobs, accounts, sse);
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
                assertThat(notificationRepository.insert(notice)).isTrue();
                notificationRepository.addEmail(createDelivery(notice, "ENROLLED", EmailDelivery.Status.SENT));
            }
        });
        int deletedCount = transaction.execute(tx -> notificationRepository.deleteOld(cutoff));
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
        transaction.executeWithoutResult(tx -> notificationRepository.preference(recipient, "ENROLLED", false));
        notificationService.create(UUID.randomUUID(), NotificationType.ENROLLED, List.of(recipient, other),
                "Enrollment", "Open class", "/classes/fixture", "ENROLLED", null);
        var jdbc = new JdbcTemplate(dataSource);
        UUID ownId = jdbc.queryForObject("select id from notifications where recipient_id=?", UUID.class, recipient);
        UUID otherId = jdbc.queryForObject("select id from notifications where recipient_id=?", UUID.class, other);
        assertThat(jdbc.queryForObject("select status from email_deliveries where notification_id=?",
                String.class, ownId)).isEqualTo("SKIPPED");
        Map<String, Boolean> savedPreferences = transaction.execute(tx -> notificationRepository.preferences(recipient));
        assertThat(savedPreferences).containsEntry("ENROLLED", false);
        boolean foreignRead = transaction.execute(tx -> notificationRepository.read(otherId, recipient, NOW));
        boolean ownRead = transaction.execute(tx -> notificationRepository.read(ownId, recipient, NOW));
        assertThat(foreignRead).isFalse();
        assertThat(ownRead).isTrue();
        var ownUnread = createNotice(recipient, NOW.plusSeconds(1));
        transaction.executeWithoutResult(tx -> notificationRepository.insert(ownUnread));
        transaction.executeWithoutResult(tx -> notificationRepository.readAll(recipient, NOW.plusSeconds(1)));
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
        transaction.executeWithoutResult(tx -> notices.forEach(notificationRepository::insert));
        var emailDeliveryService = new EmailDeliveryService(notificationRepository,
                new EmailCapPolicy(notificationRepository, 1), jobs, accounts, mock(IMail.class),
                Clock.fixed(NOW, ZoneOffset.UTC), assignments, "https://learning.example.test");
        var start = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            var futures = notices.stream().map(notice -> executor.submit(() -> {
                start.await();
                transaction.executeWithoutResult(tx -> emailDeliveryService.schedule(notice, "ENROLLED", null));
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
        verify(jobs).enqueue(anyString(), anyMap(), anyString());
    }

    @Test
    void deferredRepositoryOrdersEveryPriorityAndExcludesFutureReservations() {
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        List<String> types = List.of("ENROLLED", "GRADE_PUBLISHED", "ASSIGNMENT_OPENED",
                "ANNOUNCEMENT_IMPORTANT", "DEADLINE_REMINDER", "BACKUP_FAILED");
        transaction.executeWithoutResult(tx -> {
            for (String type : types) {
                var notice = createNotice(UUID.randomUUID(), NOW);
                notificationRepository.insert(notice);
                notificationRepository.addEmail(createDelivery(notice, type, EmailDelivery.Status.DEFERRED));
            }
            var future = createNotice(UUID.randomUUID(), NOW);
            notificationRepository.insert(future);
            var delivery = createDelivery(future, "BACKUP_FAILED", EmailDelivery.Status.DEFERRED);
            delivery.scheduledFor = NOW.plusSeconds(1);
            notificationRepository.addEmail(delivery);
        });
        List<EmailDelivery> deferred = transaction.execute(tx -> notificationRepository.deferred(NOW));
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
        var old = edu.aiplatform.announcements.model.Announcement.create(classId, teacher,
                "Older", "Body", false, false, NOW.minusSeconds(10));
        var recent = edu.aiplatform.announcements.model.Announcement.create(classId, teacher,
                "Newest", "Body", false, false, NOW);
        var deleted = edu.aiplatform.announcements.model.Announcement.create(classId, teacher,
                "Deleted", "Body", false, false, NOW.plusSeconds(1));
        deleted.delete(teacher, NOW.plusSeconds(2));
        var foreign = edu.aiplatform.announcements.model.Announcement.create(otherClass, teacher,
                "Foreign", "Body", false, false, NOW.plusSeconds(3));
        transaction.executeWithoutResult(tx -> List.of(old, recent, deleted, foreign)
                .forEach(announcementRepository::insert));
        List<edu.aiplatform.announcements.model.Announcement> feed = transaction.execute(
                tx -> announcementRepository.list(List.of(classId), 1, 20));
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
        when(assignments.listClosingBetween(any(), any())).thenReturn(List.of(a));
        when(assignments.get(assignment)).thenReturn(Optional.of(a));
        when(classes.listOpenClassesOfSubject(subject))
                .thenReturn(List.of(course(first, subject), course(second, subject)));
        for (UUID classId : List.of(first, second)) {
            when(classes.getClassRef(classId)).thenReturn(Optional.of(course(classId, subject)));
            when(classes.listActiveStudents(classId)).thenReturn(List.of(student));
            when(submissions.listStudentsWithoutSubmission(assignment, classId))
                    .thenReturn(List.of(student));
        }
        doAnswer(
                        call -> {
                            em.flush();
                            assertThat(
                                            new JdbcTemplate(dataSource)
                                                    .queryForObject(
                                                            "select count(*) from email_deliveries",
                                                            Integer.class))
                                    .isEqualTo(1);
                            throw new IllegalStateException("fixture enqueue unavailable");
                        })
                .doNothing()
                .when(jobs)
                .enqueue(anyString(), anyMap(), anyString());
        scanner.scan(NOW);
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
        scanner.scan(NOW.plusSeconds(60));
        scanner.scan(NOW.plusSeconds(120));
        for (String table : List.of("deadline_reminders", "notifications", "email_deliveries")) {
            assertThat(jdbc.queryForObject("select count(*) from " + table, Integer.class))
                    .as(table)
                    .isEqualTo(2);
        }
        verify(jobs, times(3)).enqueue(anyString(), anyMap(), anyString());
        verify(sse, times(2)).publish(anyString(), anyMap());
    }

    @Test
    void replayedSubjectOpeningKeepsPerClassLinksAndDoesNotDuplicateNotificationOrEmail() {
        UUID assignment = UUID.randomUUID(), subject = UUID.randomUUID();
        var ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        for (UUID classId : ids) {
            UUID student = UUID.randomUUID();
            when(classes.getClassRef(classId)).thenReturn(Optional.of(course(classId, subject)));
            when(classes.listActiveStudents(classId)).thenReturn(List.of(student));
            when(classes.isActiveStudent(student, classId)).thenReturn(true);
            when(accounts.getContact(student))
                    .thenReturn(
                            Optional.of(
                                    new AccountContact(
                                            student,
                                            "fixture@example.test",
                                            "Student",
                                            Role.STUDENT,
                                            AccountStatus.ACTIVE)));
        }
        when(classes.listOpenClassesOfSubject(subject))
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
        events.accept(event);
        events.accept(event);
        var jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForList("select link from notifications", String.class))
                .containsExactlyInAnyOrderElementsOf(
                        ids.stream()
                                .map(id -> "/my-assignments/" + assignment + "?classId=" + id)
                                .toList());
        assertThat(jdbc.queryForObject("select count(*) from email_deliveries", Integer.class))
                .isEqualTo(2);
        verify(jobs, times(2)).enqueue(anyString(), anyMap(), anyString());
        verify(sse, times(2)).publish(anyString(), anyMap());
    }
}
