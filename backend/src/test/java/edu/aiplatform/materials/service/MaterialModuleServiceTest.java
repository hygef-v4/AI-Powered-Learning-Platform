package edu.aiplatform.materials.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.quizzes.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class MaterialModuleServiceTest {
    final UUID subject = UUID.randomUUID(),
            classId = UUID.randomUUID(),
            moduleId = UUID.randomUUID(),
            otherId = UUID.randomUUID();
    final Actor manager = new Actor(UUID.randomUUID(), Role.SUBJECT_MANAGER);
    final Instant now = Instant.parse("2026-10-29T00:00:00Z");
    IMaterialModuleRepository materialModuleRepository;
    MaterialAccessPolicy materialAccessPolicy;
    IQuizQuery quizQuery;
    MaterialModuleService materialModuleService;
    IAudit audit;
    MaterialModule module, other;

    @BeforeEach
    void setup() {
        materialModuleRepository = mock(IMaterialModuleRepository.class);
        materialAccessPolicy = mock(MaterialAccessPolicy.class);
        quizQuery = mock(IQuizQuery.class);
        audit = mock(IAudit.class);
        materialModuleService =
                new MaterialModuleService(
                        materialModuleRepository,
                        materialAccessPolicy,
                        quizQuery,
                        Clock.fixed(now, ZoneOffset.UTC));
        module =
                new MaterialModule(
                        moduleId, subject, "Module 1", 0, manager.accountId(), now, now, 0);
        other =
                new MaterialModule(
                        otherId, subject, "Module 2", 1, manager.accountId(), now, now, 0);
        when(materialModuleRepository.findModule(moduleId)).thenReturn(Optional.of(module));
        when(materialModuleRepository.listModules(subject)).thenReturn(List.of(module, other));
        when(materialModuleRepository.deleteModule(any(), anyLong())).thenReturn(true);
    }

    QuizView quiz(UUID id, QuizScope scope, UUID subjectId, UUID classOf, QuizStatus status) {
        return new QuizView(
                id,
                scope,
                classOf,
                subjectId,
                1,
                "Quiz",
                status,
                null,
                false,
                null,
                BigDecimal.TEN,
                status == QuizStatus.OPEN || status == QuizStatus.RETIRED ? now : null);
    }

    @Test
    void moduleNamesAndOrdersRespectDocumentedBounds() {
        assertThat(module.reviseModule("m".repeat(200), 0, now).title()).hasSize(200);
        for (String title : List.of("", "   ", "m".repeat(201))) {
            assertThatThrownBy(() -> module.reviseModule(title, 0, now))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> module.reviseModule("Valid", -1, now))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void managerDefaultModuleIsCreatedOnlyOnceAcrossRepeatedOpen() {
        when(materialAccessPolicy.canManageModules(manager, subject)).thenReturn(true);
        when(materialAccessPolicy.requireSubject(subject))
                .thenReturn(
                        new edu.aiplatform.subjects.contract.SubjectRef(
                                subject,
                                "S",
                                "Subject",
                                edu.aiplatform.subjects.contract.SubjectStatus.ACTIVE,
                                manager.accountId()));
        var stored = new ArrayList<MaterialModule>();
        when(materialModuleRepository.listModules(subject)).thenAnswer(call -> List.copyOf(stored));
        doAnswer(call -> { stored.add(call.getArgument(0)); return null; })
                .when(materialModuleRepository)
                .insertModule(any());
        materialModuleService.listSubjectModules(manager, subject);
        materialModuleService.listSubjectModules(manager, subject);
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).title()).isEqualTo("Module 1");
        verify(materialModuleRepository).insertModule(any());
    }

    @Test
    void teacherOpeningArchivedSubjectDoesNotCreateDefaultModule() {
        Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
        when(materialAccessPolicy.requireClassListAccess(teacher, classId, false))
                .thenReturn(
                        new ClassRef(
                                classId,
                                subject,
                                "C",
                                "Class",
                                ClassStatus.ARCHIVED,
                                teacher.accountId(),
                                false));
        when(materialAccessPolicy.requireSubject(subject))
                .thenReturn(
                        new edu.aiplatform.subjects.contract.SubjectRef(
                                subject,
                                "S",
                                "Subject",
                                edu.aiplatform.subjects.contract.SubjectStatus.ARCHIVED,
                                null));
        when(materialModuleRepository.listModules(subject)).thenReturn(List.of());
        assertThat(materialModuleService.listClassModules(teacher, classId, false)).isEmpty();
        verify(materialModuleRepository, never()).insertModule(any());
    }

    @Test
    void lastEmptyModuleMayBeDeleted() {
        when(materialModuleRepository.listModules(subject)).thenReturn(List.of(module));
        materialModuleService.deleteModule(manager, moduleId, 0);
        verify(materialModuleRepository).deleteModule(moduleId, 0);
        verify(materialModuleRepository, never()).moveArchivedMaterials(any(), any());
    }

    @Test
    void reorderAcceptsProperSubsetWithoutChangingUnrequestedModule() {
        when(materialModuleRepository.findModule(otherId)).thenReturn(Optional.of(other));
        when(materialModuleRepository.updateModule(any(), anyLong())).thenReturn(true);
        materialModuleService.reorderModules(manager, subject,
                new MaterialCommands.Reorder(List.of(new MaterialCommands.Position(otherId, 0L))));
        verify(materialModuleRepository)
                .updateModule(argThat(m -> m.id().equals(otherId) && m.orderNo() == 0), eq(0L));
        verify(materialModuleRepository, never())
                .updateModule(argThat(m -> m.id().equals(moduleId)), anyLong());
    }

    @Test
    void detachChecksCurrentQuizPermissionAndExactLinkBeforeSuccessAudit() {
        UUID quizId = UUID.randomUUID();
        when(quizQuery.loadQuiz(quizId)).thenReturn(Optional.of(
                quiz(quizId, QuizScope.SUBJECT, subject, null, QuizStatus.DRAFT)));
        when(materialAccessPolicy.canManageQuiz(manager, subject, null)).thenReturn(false);
        assertThatThrownBy(() -> materialModuleService.detachQuiz(manager, moduleId, quizId))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_QUIZ_NOT_FOUND"));
        verify(materialModuleRepository, never()).unlinkModuleQuiz(any(), any());
        when(materialAccessPolicy.canManageQuiz(manager, subject, null)).thenReturn(true);
        when(materialModuleRepository.unlinkModuleQuiz(moduleId, quizId)).thenReturn(false);
        assertThatThrownBy(() -> materialModuleService.detachQuiz(manager, moduleId, quizId))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_NOT_FOUND"));
        when(materialModuleRepository.unlinkModuleQuiz(moduleId, quizId)).thenReturn(true);
        materialModuleService.detachQuiz(manager, moduleId, quizId);
    }

    @Test
    void teacherCannotSeeNeverPublishedRetiredSubjectQuizInMaterialModule() {
        Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
        UUID quizId = UUID.randomUUID();
        QuizView unpublished =
                new QuizView(
                        quizId,
                        QuizScope.SUBJECT,
                        null,
                        subject,
                        1,
                        "Unpublished",
                        QuizStatus.RETIRED,
                        null,
                        false,
                        null,
                        BigDecimal.TEN,
                        null);
        when(materialAccessPolicy.requireClassListAccess(teacher, classId, false))
                .thenReturn(
                        new ClassRef(
                                classId,
                                subject,
                                "C",
                                "Class",
                                ClassStatus.OPEN,
                                teacher.accountId(),
                                false));
        when(materialModuleRepository.listQuizLinks(subject))
                .thenReturn(List.of(new MaterialModule.QuizLink(moduleId, quizId, 0)));
        when(quizQuery.loadQuiz(quizId)).thenReturn(Optional.of(unpublished));
        assertThat(materialModuleService.listClassModules(teacher, classId, false).get(0).quizzes())
                .isEmpty();
    }

    @Test
    void lostDeleteCasDoesNotAuditSuccess() {
        when(materialModuleRepository.deleteModule(moduleId, 0)).thenReturn(false);
        assertThatThrownBy(() -> materialModuleService.deleteModule(manager, moduleId, 0))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
    }

    @Test
    void lostRenameCasDoesNotAuditSuccess() {
        when(materialModuleRepository.updateModule(any(), anyLong())).thenReturn(false);
        assertThatThrownBy(
                        () ->
                                materialModuleService.renameModule(
                                        manager,
                                        moduleId,
                                        new MaterialCommands.ModuleUpdate("New", 0L)))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
    }

    @Test
    void
            subjectViewExcludesEveryClassQuizWhileTeacherViewKeepsOnlyOwnClassAndPublishedSubjectQuizzes() {
        Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
        UUID ownDraft = UUID.randomUUID(),
                ownRetired = UUID.randomUUID(),
                foreignOpen = UUID.randomUUID(),
                subjectDraft = UUID.randomUUID(),
                subjectOpen = UUID.randomUUID(),
                subjectRetired = UUID.randomUUID();
        List<QuizView> rows =
                List.of(
                        quiz(ownDraft, QuizScope.CLASS, subject, classId, QuizStatus.DRAFT),
                        quiz(ownRetired, QuizScope.CLASS, subject, classId, QuizStatus.RETIRED),
                        quiz(
                                foreignOpen,
                                QuizScope.CLASS,
                                subject,
                                UUID.randomUUID(),
                                QuizStatus.OPEN),
                        quiz(subjectDraft, QuizScope.SUBJECT, subject, null, QuizStatus.DRAFT),
                        quiz(subjectOpen, QuizScope.SUBJECT, subject, null, QuizStatus.OPEN),
                        quiz(subjectRetired, QuizScope.SUBJECT, subject, null, QuizStatus.RETIRED));
        when(materialModuleRepository.listQuizLinks(subject))
                .thenReturn(
                        rows.stream()
                                .map(q -> new MaterialModule.QuizLink(moduleId, q.quizId(), 0))
                                .toList());
        rows.forEach(q -> when(quizQuery.loadQuiz(q.quizId())).thenReturn(Optional.of(q)));
        when(materialAccessPolicy.canManageModules(manager, subject)).thenReturn(true);
        when(materialAccessPolicy.canManageQuiz(manager, subject, null)).thenReturn(true);

        var subjectView = materialModuleService.listSubjectModules(manager, subject).get(0);
        assertThat(subjectView.canManage()).isTrue();
        assertThat(subjectView.quizzes())
                .extracting(MaterialViews.ModuleQuiz::quizId)
                .containsExactly(subjectDraft, subjectOpen, subjectRetired);
        assertThat(subjectView.quizzes()).allSatisfy(q -> assertThat(q.canManage()).isTrue());

        when(materialAccessPolicy.requireClassListAccess(teacher, classId, false))
                .thenReturn(
                        new ClassRef(
                                classId,
                                subject,
                                "C",
                                "Class",
                                ClassStatus.OPEN,
                                teacher.accountId(),
                                false));
        when(materialAccessPolicy.canManageQuiz(teacher, subject, classId)).thenReturn(true);
        var classView = materialModuleService.listClassModules(teacher, classId, false).get(0);
        assertThat(classView.canManage()).isFalse();
        assertThat(classView.quizzes())
                .extracting(MaterialViews.ModuleQuiz::quizId)
                .containsExactly(ownDraft, ownRetired, subjectOpen, subjectRetired);
        assertThat(classView.quizzes())
                .filteredOn(q -> q.scope() == QuizScope.CLASS)
                .allSatisfy(q -> assertThat(q.canManage()).isTrue());
        assertThat(classView.quizzes())
                .filteredOn(q -> q.scope() == QuizScope.SUBJECT)
                .allSatisfy(q -> assertThat(q.canManage()).isFalse());
    }

    @Test
    void teacherOpeningAClassOfASubjectWithoutModulesGetsADefaultModule() {
        var teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
        UUID fresh = UUID.randomUUID();
        when(materialAccessPolicy.requireClassListAccess(teacher, classId, false))
                .thenReturn(
                        new ClassRef(
                                classId,
                                fresh,
                                "C",
                                "Class",
                                ClassStatus.OPEN,
                                teacher.accountId(),
                                false));
        when(materialAccessPolicy.requireSubject(fresh))
                .thenReturn(
                        new edu.aiplatform.subjects.contract.SubjectRef(
                                fresh,
                                "SWP",
                                "Môn",
                                edu.aiplatform.subjects.contract.SubjectStatus.ACTIVE,
                                null));
        when(materialModuleRepository.listModules(fresh)).thenReturn(List.of());

        materialModuleService.listClassModules(teacher, classId, false);

        verify(materialModuleRepository)
                .insertModule(
                        argThat(m -> m.subjectId().equals(fresh) && m.title().equals("Module 1")));
    }
}
