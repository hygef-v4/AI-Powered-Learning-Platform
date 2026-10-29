package edu.aiplatform.subjects.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import edu.aiplatform.identity.contract.ISubjectScope;
import edu.aiplatform.identity.contract.stub.IdentityContractStubs;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectClasses;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.stub.SubjectsContractStubs;
import edu.aiplatform.subjects.domain.ISubjectRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;

class SubjectContractsWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdentityContractStubs.class, SubjectsContractStubs.class))
            .withUserConfiguration(RealSubjects.class)
            .withBean(ISubjectRepository.class, () -> mock(ISubjectRepository.class))
            .withBean(edu.aiplatform.audit.contract.IAudit.class, () -> mock(edu.aiplatform.audit.contract.IAudit.class))
            .withBean(java.time.Clock.class, java.time.Clock::systemUTC);

    @TestConfiguration
    @Import({SubjectQueryService.class, SubjectScopeService.class, SubjectAuditRecorder.class})
    static class RealSubjects {
    }

    @TestConfiguration
    @Import({SubjectAdminService.class, SubjectClassesReader.class})
    static class AdminReads {
    }

    @TestConfiguration
    @Import({SubjectCommandService.class})
    static class Commands {
    }

    @TestConfiguration
    @Import({SubjectStatusService.class, SubjectMutationGuard.class})
    static class Lifecycle {
    }

    @Test
    void archiveFailsClosedWhenU05CannotArchiveClassesAndKeepsTheSubjectGuardForU05() {
        ISubjectClasses legacy = mock(ISubjectClasses.class);
        runner.withUserConfiguration(Lifecycle.class)
                .withBean(ISubjectClasses.class, () -> legacy)
                .run(context -> {
                    assertThat(context).hasSingleBean(edu.aiplatform.subjects.contract.ISubjectMutationGuard.class);
                    UUID id = UUID.randomUUID();
                    org.mockito.Mockito.when(legacy.archiveClassesOfSubject(org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("down"));
                    org.mockito.Mockito.when(context.getBean(ISubjectRepository.class).findByIdForUpdate(
                            org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any(java.time.Duration.class)))
                            .thenReturn(java.util.Optional.of(edu.aiplatform.subjects.domain.Subject.create(id, "ABC", "Name", null, null, java.time.Instant.now())));
                    var admin = new edu.aiplatform.identity.contract.Actor(UUID.randomUUID(), edu.aiplatform.identity.contract.Role.ADMIN);
                    assertThatThrownBy(() -> context.getBean(SubjectStatusService.class).changeStatus(admin, id,
                            new SubjectStatusService.ChangeStatus(edu.aiplatform.subjects.contract.SubjectStatus.ARCHIVED, 1L)))
                            .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SUBJECT_CLASSES_UNAVAILABLE");
                });
    }

    @Test
    void commandBeanWiresWithContractProviders() {
        runner.withUserConfiguration(Commands.class)
                .run(context -> assertThat(context).hasSingleBean(SubjectCommandService.class));
    }

    @Test
    void adminReadsWireWithContractProvidersAndCloseTheirReader() {
        runner.withUserConfiguration(AdminReads.class)
                .run(context -> {
            assertThat(context).hasSingleBean(SubjectAdminService.class).hasSingleBean(SubjectClassesReader.class);
            assertThat(context.getBean(SubjectClassesReader.class).classes(UUID.randomUUID()).availability())
                    .isEqualTo(SubjectAdminViews.Availability.UNAVAILABLE);
        });
    }

    @Test
    void realQueryAndScopeReplaceOnlyTheirOwnStubs() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(ISubjectQuery.class).hasSingleBean(ISubjectScope.class);
            assertThat(context.getBean(ISubjectQuery.class)).isInstanceOf(SubjectQueryService.class);
            assertThat(context.getBean(ISubjectScope.class)).isInstanceOf(SubjectScopeService.class);
            assertThat(context).doesNotHaveBean("subjectQueryStub").doesNotHaveBean("subjectScopeStub");
            // U05 chưa cài vẫn có đúng một placeholder, nhưng placeholder không được giả 0 lớp.
            assertThat(context).hasSingleBean(ISubjectClasses.class);
            ISubjectClasses classes = context.getBean(ISubjectClasses.class);
            assertThatThrownBy(() -> classes.listClassesOfSubject(UUID.randomUUID()))
                    .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SUBJECT_CLASSES_UNAVAILABLE");
            assertThatThrownBy(() -> classes.countClasses(List.of(UUID.randomUUID())))
                    .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SUBJECT_CLASSES_UNAVAILABLE");
            assertThatThrownBy(() -> classes.countUnarchivedClasses(UUID.randomUUID()))
                    .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SUBJECT_CLASSES_UNAVAILABLE");
        });
    }

    @Test
    void disablingStubsDoesNotDisableTheRealSubjectServices() {
        runner.withPropertyValues("platform.contract-stubs.enabled=false").run(context -> {
            assertThat(context).hasSingleBean(ISubjectQuery.class).hasSingleBean(ISubjectScope.class);
            assertThat(context).doesNotHaveBean(ISubjectClasses.class);
        });
    }

    @Test
    void providedU05BeanReplacesTheUnavailableClassesPlaceholder() {
        ISubjectClasses providedClasses = mock(ISubjectClasses.class);
        runner.withBean(ISubjectClasses.class, () -> providedClasses).run(context -> {
            assertThat(context).hasSingleBean(ISubjectClasses.class).doesNotHaveBean("subjectClassesStub");
            assertThat(context.getBean(ISubjectClasses.class)).isSameAs(providedClasses);
        });
    }
}
