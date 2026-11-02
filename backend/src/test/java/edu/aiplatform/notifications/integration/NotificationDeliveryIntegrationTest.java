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
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
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

    @TestConfiguration
    @EnableTransactionManagement
    @Import({NotificationRepositoryAdapter.class, NotificationService.class, EmailDeliveryService.class,
            DeadlineReminderScanner.class, NotificationEventService.class, RecipientResolver.class})
    static class Config {
        @Bean DataSource dataSource() {
            var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("create role app nologin");
            jdbc.execute("create table course_classes (id uuid primary key)");
            Flyway.configure().dataSource(source).locations("classpath:db/migration/notifications")
                    .baselineOnMigrate(true).baselineVersion("0").load().migrate();
            return source;
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(source);
            factory.setPackagesToScan(Notification.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate", "hibernate.jdbc.time_zone", "UTC",
                    "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
            return factory;
        }
        @Bean EntityManager entityManager(EntityManagerFactory factory) {return SharedEntityManagerCreator.createSharedEntityManager(factory);}
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {return new JpaTransactionManager(factory);}
        @Bean IAssignmentQuery assignments() {return mock(IAssignmentQuery.class);}
        @Bean IClassAccess classes() {return mock(IClassAccess.class);}
        @Bean ISubmissionQuery submissions() {return mock(ISubmissionQuery.class);}
        @Bean IJob jobs() {return mock(IJob.class);}
        @Bean IAccountLookup accounts() {return mock(IAccountLookup.class);}
        @Bean IMail mail() {return mock(IMail.class);}
        @Bean ISseHub sse() {return mock(ISseHub.class);}
        @Bean NotificationAccess access() {return mock(NotificationAccess.class);}
        @Bean Clock clock() {return Clock.fixed(NOW, ZoneOffset.UTC);}
        @Bean EmailCapPolicy cap(INotificationRepository repo) {return new EmailCapPolicy(repo, 300);}
    }

    @BeforeEach void clean() {
        reset(assignments, classes, submissions, jobs, accounts, sse);
        new JdbcTemplate(dataSource).execute("truncate notifications, email_deliveries, deadline_reminders");
    }

    ClassRef course(UUID classId, UUID subject) {
        return new ClassRef(classId, subject, "C", "Class", ClassStatus.OPEN, UUID.randomUUID(), false);
    }

    @Test void failedEmailEnqueueRollsBackMarkerNotificationAndEmailThenNextScanRetriesExactlyOnce() {
        UUID assignment = UUID.randomUUID(), subject = UUID.randomUUID();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), student = UUID.randomUUID();
        var a = new AssignmentView(assignment, AssignmentScope.SUBJECT, null, subject, 1, "Bài",
                SubmissionMode.INDIVIDUAL, GradingMode.GRADED, false, AssignmentStatus.OPEN,
                NOW.minusSeconds(60), NOW.plus(Duration.ofHours(24)), null, null, false, null, List.of());
        when(assignments.listClosingBetween(any(), any())).thenReturn(List.of(a));
        when(assignments.get(assignment)).thenReturn(Optional.of(a));
        when(classes.listOpenClassesOfSubject(subject)).thenReturn(List.of(course(first, subject), course(second, subject)));
        for (UUID classId : List.of(first, second)) {
            when(classes.getClassRef(classId)).thenReturn(Optional.of(course(classId, subject)));
            when(classes.listActiveStudents(classId)).thenReturn(List.of(student));
            when(submissions.listStudentsWithoutSubmission(assignment, classId)).thenReturn(List.of(student));
        }
        doAnswer(call -> {
            em.flush();
            assertThat(new JdbcTemplate(dataSource).queryForObject("select count(*) from email_deliveries", Integer.class)).isEqualTo(1);
            throw new IllegalStateException("fixture enqueue unavailable");
        }).doNothing().when(jobs).enqueue(anyString(), anyMap(), anyString());
        scanner.scan(NOW);
        var jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("select count(*) from deadline_reminders where class_id = ?", Integer.class, first)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from deadline_reminders", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from notifications", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from email_deliveries", Integer.class)).isEqualTo(1);
        scanner.scan(NOW.plusSeconds(60));
        scanner.scan(NOW.plusSeconds(120));
        for (String table : List.of("deadline_reminders", "notifications", "email_deliveries")) {
            assertThat(jdbc.queryForObject("select count(*) from " + table, Integer.class)).as(table).isEqualTo(2);
        }
        verify(jobs, times(3)).enqueue(anyString(), anyMap(), anyString());
        verify(sse, times(2)).publish(anyString(), anyMap());
    }

    @Test void replayedSubjectOpeningKeepsPerClassLinksAndDoesNotDuplicateNotificationOrEmail() {
        UUID assignment = UUID.randomUUID(), subject = UUID.randomUUID();
        var ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        for (UUID classId : ids) {
            UUID student = UUID.randomUUID();
            when(classes.getClassRef(classId)).thenReturn(Optional.of(course(classId, subject)));
            when(classes.listActiveStudents(classId)).thenReturn(List.of(student));
            when(classes.isActiveStudent(student, classId)).thenReturn(true);
            when(accounts.getContact(student)).thenReturn(Optional.of(
                    new AccountContact(student, "fixture@example.test", "Student", Role.STUDENT, AccountStatus.ACTIVE)));
        }
        when(classes.listOpenClassesOfSubject(subject)).thenReturn(ids.stream().map(id -> course(id, subject)).toList());
        var event = new DomainEvent(1, UUID.randomUUID(), EventTypes.ASSIGNMENT_OPENED, "U07", NOW, null,
                Map.of("assignmentId", assignment.toString(), "scope", "SUBJECT", "subjectId", subject.toString()));
        events.accept(event);
        events.accept(event);
        var jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForList("select link from notifications", String.class)).containsExactlyInAnyOrderElementsOf(
                ids.stream().map(id -> "/my-assignments/" + assignment + "?classId=" + id).toList());
        assertThat(jdbc.queryForObject("select count(*) from email_deliveries", Integer.class)).isEqualTo(2);
        verify(jobs, times(2)).enqueue(anyString(), anyMap(), anyString());
        verify(sse, times(2)).publish(anyString(), anyMap());
    }
}
