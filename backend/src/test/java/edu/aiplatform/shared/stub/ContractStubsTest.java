package edu.aiplatform.shared.stub;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.academics.contract.stub.AcademicsContractStubs;
import edu.aiplatform.ai.contract.IAiTask;
import edu.aiplatform.ai.contract.IReferenceMaterial;
import edu.aiplatform.ai.contract.stub.AiContractStubs;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.IDocx;
import edu.aiplatform.assignments.contract.IGroupAssignmentSupport;
import edu.aiplatform.assignments.contract.IRubric;
import edu.aiplatform.assignments.contract.stub.AssignmentsContractStubs;
import edu.aiplatform.assignmentwork.contract.IActivityQuery;
import edu.aiplatform.assignmentwork.contract.IActivityWrite;
import edu.aiplatform.assignmentwork.contract.IGroupWorkStatus;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.assignmentwork.contract.stub.AssignmentWorkContractStubs;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.audit.contract.IAuditQuery;
import edu.aiplatform.audit.contract.stub.AuditContractStubs;
import edu.aiplatform.backup.contract.IBackupStatus;
import edu.aiplatform.backup.contract.stub.BackupContractStubs;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.files.contract.stub.FilesContractStubs;
import edu.aiplatform.grading.contract.IGroupEvaluation;
import edu.aiplatform.grading.contract.IGroupSubmissionSource;
import edu.aiplatform.grading.contract.stub.GradingContractStubs;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.groupdocs.contract.stub.GroupDocsContractStubs;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.identity.contract.IMail;
import edu.aiplatform.identity.contract.ISubjectScope;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.contract.stub.IdentityContractStubs;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.stub.JobsContractStubs;
import edu.aiplatform.quizzes.contract.IQuizQuery;
import edu.aiplatform.quizzes.contract.IQuizScoring;
import edu.aiplatform.quizzes.contract.stub.QuizzesContractStubs;
import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.settings.contract.stub.SettingsContractStubs;
import edu.aiplatform.subjects.contract.ISubjectClasses;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.stub.SubjectsContractStubs;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ContractStubsTest {

    static final List<Class<?>> STUBBED = List.of(
            IAuthorization.class, IAccountLookup.class, IMail.class, ISubjectScope.class, IClassScope.class,
            IAudit.class, IAuditQuery.class, IArtifact.class, IJob.class, IEventPublisher.class, ISettings.class,
            IBackupStatus.class, ISubjectQuery.class, ISubjectClasses.class, IClassAccess.class, IAiTask.class,
            IReferenceMaterial.class, IAssignmentQuery.class, IRubric.class, IDocumentModel.class, IDocx.class,
            IGroupAssignmentSupport.class, ISubmissionQuery.class, IActivityWrite.class, IActivityQuery.class,
            IGroupWorkStatus.class, IGroupEvaluation.class, IGroupSubmissionSource.class, IQuizQuery.class,
            IQuizScoring.class, ISseHub.class);

    final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
            IdentityContractStubs.class, AuditContractStubs.class, FilesContractStubs.class, JobsContractStubs.class,
            SettingsContractStubs.class, BackupContractStubs.class, SubjectsContractStubs.class,
            AcademicsContractStubs.class, AiContractStubs.class, AssignmentsContractStubs.class,
            AssignmentWorkContractStubs.class, GradingContractStubs.class, QuizzesContractStubs.class,
            GroupDocsContractStubs.class));

    @Test
    void everySingleImplementationInterfaceHasAStubWhenNoUnitImplementsIt() {
        runner.run(context -> {
            for (Class<?> type : STUBBED) {
                assertThat(context).as(type.getSimpleName()).hasSingleBean(type);
            }
        });
    }

    @Test
    void stubsAllowPermissionsAndReturnEmptyLists() {
        runner.run(context -> {
            Actor actor = new Actor(UUID.randomUUID(), Role.STUDENT);
            UUID classId = UUID.randomUUID();
            assertThat(context.getBean(IAuthorization.class).authorize(actor, "ANY", ResourceRef.courseClass(classId))
                    .allowed()).isTrue();
            assertThat(context.getBean(IClassScope.class).isEnrolledStudent(actor.accountId(), classId)).isTrue();
            assertThat(context.getBean(IClassAccess.class).listActiveStudents(classId)).isEmpty();
            assertThat(context.getBean(IClassAccess.class).getClassRef(classId)).get()
                    .extracting(ref -> ref.classId()).isEqualTo(classId);
        });
    }

    @Test
    void realImplementationReplacesTheStub() {
        IAuthorization denyAll = (actor, action, resourceRef) -> AuthorizationDecision.deny("real");
        runner.withBean(IAuthorization.class, () -> denyAll).run(context ->
                assertThat(context.getBean(IAuthorization.class)).isSameAs(denyAll));
    }

    @Test
    void stubsCanBeTurnedOff() {
        runner.withPropertyValues("platform.contract-stubs.enabled=false").run(context ->
                assertThat(context).doesNotHaveBean(IAuthorization.class));
    }
}
