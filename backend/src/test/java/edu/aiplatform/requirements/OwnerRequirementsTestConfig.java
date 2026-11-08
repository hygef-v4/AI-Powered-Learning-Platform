package edu.aiplatform.requirements;

import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.aiplatform.academics.repository.*;
import edu.aiplatform.academics.service.AcademicsContractAdapter;
import edu.aiplatform.announcements.controller.AnnouncementController;
import edu.aiplatform.announcements.controller.MyAnnouncementController;
import edu.aiplatform.announcements.repository.AnnouncementRepositoryAdapter;
import edu.aiplatform.announcements.service.*;
import edu.aiplatform.audit.repository.AuditLogRepository;
import edu.aiplatform.audit.service.AuditStore;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.config.IdentityProperties;
import edu.aiplatform.identity.controller.IdentityHttpSecurity;
import edu.aiplatform.identity.integration.*;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.service.*;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.service.*;
import edu.aiplatform.materials.controller.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.materials.service.*;
import edu.aiplatform.notifications.controller.*;
import edu.aiplatform.notifications.model.Notification;
import edu.aiplatform.notifications.repository.NotificationRepositoryAdapter;
import edu.aiplatform.notifications.service.*;
import edu.aiplatform.quizzes.contract.IQuizQuery;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.subjects.controller.SubjectAdminController;
import edu.aiplatform.subjects.repository.*;
import edu.aiplatform.subjects.service.*;
import edu.aiplatform.shared.config.SecurityConfig;
import edu.aiplatform.shared.error.GlobalExceptionHandler;
import edu.aiplatform.identity.contract.IMail;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.*;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/** Existing owner adapters and production security; isolated DB, remote queues/storage/session store bounded by mocks. */
@Configuration
@EnableWebMvc
@EnableWebSecurity
@EnableTransactionManagement
@EnableJpaRepositories(basePackageClasses = ISubjectJpaRepository.class)
@Import({SecurityConfig.class, IdentityHttpSecurity.class, GlobalExceptionHandler.class,
        SubjectAdminController.class, SubjectAdminService.class, SubjectCommandService.class,
        SubjectStatusService.class, SubjectAuditRecorder.class, SubjectClassesReader.class,
        SubjectRepositoryAdapter.class, SubjectQueryService.class, SubjectScopeService.class,
        SubjectMutationGuard.class, CourseClassRepository.class, EnrollmentRepository.class,
        AcademicsReadRepository.class, AcademicsContractAdapter.class,
        AccountRepository.class, AccountLookupService.class, AuthorizationService.class,
        SessionAuthority.class, JwtService.class, AccountAdminService.class,
        AuditLogRepository.class, AuditStore.class,
        MaterialRepositoryAdapter.class, MaterialModuleRepositoryAdapter.class,
        MaterialAccessPolicy.class, MaterialService.class, MaterialQueryService.class,
        MaterialModuleService.class, ReferenceMaterialService.class, MaterialMcpTools.class,
        MaterialController.class, MaterialModuleController.class, SubjectMaterialController.class,
        ClassMaterialController.class, MyMaterialController.class, MaterialExceptionHandler.class,
        AnnouncementRepositoryAdapter.class, AnnouncementService.class, AnnouncementAccessPolicy.class,
        AnnouncementController.class, MyAnnouncementController.class,
        NotificationRepositoryAdapter.class, NotificationService.class,
        NotificationAccess.class, NotificationPreferenceService.class, NotificationController.class,
        NotificationPreferenceController.class, EmailDeliveryService.class, EmailCapPolicy.class,
        AfterCommitExecutor.class, EventPublisher.class, EmailSendJobHandler.class,
        JobHandlerRegistry.class, JobDispatcher.class, edu.aiplatform.assignmentwork.service.SubmissionQueryService.class,
        DeadlineReminderScanner.class, RecipientResolver.class, NotificationEventService.class})
class OwnerRequirementsTestConfig {
    @Bean
    org.springframework.core.convert.ConversionService conversionService() {
        return new org.springframework.boot.convert.ApplicationConversionService();
    }

    @Bean
    DataSource dataSource() {
        var database = RequirementsCoverageIntegrationTest.DATABASE;
        var pool = new com.zaxxer.hikari.HikariConfig();
        pool.setJdbcUrl(database.getJdbcUrl());
        pool.setUsername(database.getUsername());
        pool.setPassword(database.getPassword());
        pool.setMaximumPoolSize(10);
        pool.setMinimumIdle(2);
        var source = new com.zaxxer.hikari.HikariDataSource(pool);
        new JdbcTemplate(source).execute("create role app nologin");
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        return source;
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(source);
        factory.setPackagesToScan("edu.aiplatform.subjects.repository", "edu.aiplatform.materials.repository",
                Notification.class.getPackageName(), edu.aiplatform.announcements.model.Announcement.class.getPackageName());
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate", "hibernate.jdbc.time_zone", "UTC",
                "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
        return factory;
    }

    @Bean
    @Primary
    EntityManager entityManager(EntityManagerFactory factory) {
        return SharedEntityManagerCreator.createSharedEntityManager(factory);
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager manager) {
        return new TransactionTemplate(manager);
    }

    @Bean
    JdbcClient jdbcClient(DataSource source) {
        return JdbcClient.create(source);
    }

    @Bean
    NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource source) {
        return new NamedParameterJdbcTemplate(source);
    }

    @Bean
    ObjectMapper objectMapper() {
        return org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json()
                .modulesToInstall(new JavaTimeModule()).build();
    }

    @Bean
    Clock clock() {
        return Clock.fixed(RequirementsCoverageIntegrationTest.NOW, ZoneOffset.UTC);
    }

    @Bean
    IdentityProperties identityProperties() {
        return IdentityTestData.properties();
    }

    @Bean
    SessionStore sessionStore() {
        return mock(SessionStore.class);
    }

    @Bean
    OtpChallengeStore otpChallengeStore() {
        return mock(OtpChallengeStore.class);
    }

    @Bean
    VerificationTicketStore verificationTicketStore() {
        return mock(VerificationTicketStore.class);
    }

    @Bean
    IArtifact artifact() {
        return mock(IArtifact.class);
    }

    @Bean
    IQuizQuery quizQuery() {
        return mock(IQuizQuery.class);
    }

    @Bean
    IAssignmentQuery assignmentQuery() {
        return mock(IAssignmentQuery.class);
    }

    @Bean
    IJob job() {
        return mock(IJob.class);
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
    IMessageSender messageSender() {
        return mock(IMessageSender.class);
    }

    @Bean
    edu.aiplatform.assignmentwork.repository.AttemptRepository attemptRepository() {
        return mock(edu.aiplatform.assignmentwork.repository.AttemptRepository.class);
    }
    @Bean
    edu.aiplatform.assignmentwork.contract.IGroupWorkStatus groupWorkStatus() {
        return mock(edu.aiplatform.assignmentwork.contract.IGroupWorkStatus.class);
    }
}
