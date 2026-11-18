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
                        audit,
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
        verify(audit).record(argThat(e -> e.action().equals("MATERIAL_MODULE_CREATED")));
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
        verifyNoInteractions(audit);
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
    void reorderRequestValidationCoversEmptyMissingAndFiveHundredBoundary() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new MaterialCommands.Reorder(List.of()))).isNotEmpty();
            assertThat(validator.validate(new MaterialCommands.Reorder(
                    List.of(new MaterialCommands.Position(null, null))))).hasSize(2);
            var rows = java.util.stream.IntStream.range(0, 500)
                    .mapToObj(i -> new MaterialCommands.Position(UUID.randomUUID(), 0L)).toList();
            assertThat(validator.validate(new MaterialCommands.Reorder(rows))).isEmpty();
            var overflow = new ArrayList<>(rows);
            overflow.add(new MaterialCommands.Position(UUID.randomUUID(), 0L));
            assertThat(validator.validate(new MaterialCommands.Reorder(overflow))).hasSize(1);
        }
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
        verify(audit).record(argThat(event -> event.action().equals("ACCESS_DENIED")
                && event.result() == edu.aiplatform.audit.contract.AuditResult.DENIED));
        clearInvocations(audit);
        when(materialAccessPolicy.canManageQuiz(manager, subject, null)).thenReturn(true);
        when(materialModuleRepository.unlinkModuleQuiz(moduleId, quizId)).thenReturn(false);
        assertThatThrownBy(() -> materialModuleService.detachQuiz(manager, moduleId, quizId))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_NOT_FOUND"));
        verifyNoInteractions(audit);
        when(materialModuleRepository.unlinkModuleQuiz(moduleId, quizId)).thenReturn(true);
        materialModuleService.detachQuiz(manager, moduleId, quizId);
        verify(audit).record(argThat(event -> event.action().equals("MATERIAL_MODULE_QUIZ_DETACHED")
                && event.actorId().equals(manager.accountId()) && event.objectId().equals(moduleId.toString())));
    }

    @Test
    void attachToSameModuleIsIdempotentButForeignDetachCannotWrite() {
        UUID quizId = UUID.randomUUID();
        when(quizQuery.loadQuiz(quizId)).thenReturn(Optional.of(
                quiz(quizId, QuizScope.SUBJECT, subject, null, QuizStatus.DRAFT)));
        when(materialAccessPolicy.canManageQuiz(manager, subject, null)).thenReturn(true);
        when(materialModuleRepository.findQuizLink(quizId))
                .thenReturn(Optional.of(new MaterialModule.QuizLink(moduleId, quizId, 0)));
        materialModuleService.attachQuiz(manager, moduleId, quizId);
        verify(materialModuleRepository, never()).linkQuiz(any());
        verify(materialModuleRepository, never()).unlinkModuleQuiz(any(), any());
        verifyNoInteractions(audit);
        when(quizQuery.loadQuiz(quizId)).thenReturn(Optional.of(
                quiz(quizId, QuizScope.SUBJECT, UUID.randomUUID(), null, QuizStatus.DRAFT)));
        assertThatThrownBy(() -> materialModuleService.detachQuiz(manager, moduleId, quizId))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_QUIZ_INVALID"));
        verify(materialModuleRepository, never()).unlinkModuleQuiz(any(), any());
        verifyNoInteractions(audit);
    }

    @Test
    void deleteRefusesModuleWithActiveMaterials() {
        when(materialModuleRepository.countActiveMaterials(moduleId)).thenReturn(2);
        assertThatThrownBy(() -> materialModuleService.deleteModule(manager, moduleId, 0))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_MODULE_NOT_EMPTY"));
        verify(materialModuleRepository, never()).deleteModule(any(), anyLong());
    }

    @Test
    void deleteMovesArchivedMaterialsToAnotherModule() {
        when(materialModuleRepository.countArchivedMaterials(moduleId)).thenReturn(1);
        materialModuleService.deleteModule(manager, moduleId, 0);
        verify(materialModuleRepository).moveArchivedMaterials(moduleId, otherId);
        verify(materialModuleRepository).deleteModule(moduleId, 0);
    }

    @Test
    void deleteOfStaleVersionConflicts() {
        assertThatThrownBy(() -> materialModuleService.deleteModule(manager, moduleId, 5))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.readStatus().value()).isEqualTo(409));
    }

    @Test
    void attachRejectsQuizOfAnotherSubject() {
        UUID quizId = UUID.randomUUID(), foreignSubject = UUID.randomUUID();
        when(quizQuery.loadQuiz(quizId))
                .thenReturn(
                        Optional.of(
                                quiz(
                                        quizId,
                                        QuizScope.SUBJECT,
                                        foreignSubject,
                                        null,
                                        QuizStatus.OPEN)));
        when(materialAccessPolicy.canManageQuiz(manager, foreignSubject, null)).thenReturn(true);
        assertThatThrownBy(() -> materialModuleService.attachQuiz(manager, moduleId, quizId))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> {
                            assertThat(e.readStatus().value()).isEqualTo(400);
                            assertThat(e.readCode()).isEqualTo("MATERIAL_QUIZ_INVALID");
                        });
        verify(materialModuleRepository, never()).linkQuiz(any());
        verify(materialModuleRepository, never()).unlinkModuleQuiz(any(), any());
        verifyNoInteractions(audit);
    }

    @Test
    void attachMovesQuizFromItsPreviousModule() {
        UUID quizId = UUID.randomUUID();
        when(quizQuery.loadQuiz(quizId))
                .thenReturn(
                        Optional.of(
                                quiz(quizId, QuizScope.SUBJECT, subject, null, QuizStatus.DRAFT)));
        when(materialAccessPolicy.canManageQuiz(manager, subject, null)).thenReturn(true);
        when(materialModuleRepository.findQuizLink(quizId))
                .thenReturn(Optional.of(new MaterialModule.QuizLink(otherId, quizId, 0)));
        materialModuleService.attachQuiz(manager, moduleId, quizId);
        verify(materialModuleRepository).unlinkModuleQuiz(otherId, quizId);
        verify(materialModuleRepository).linkQuiz(new MaterialModule.QuizLink(moduleId, quizId, 0));
    }

    @Test
    void studentsSeeOnlyOpenQuizzesOfTheirClass() {
        Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);
        UUID open = UUID.randomUUID(), draft = UUID.randomUUID();
        when(materialAccessPolicy.requireClassListAccess(student, classId, true))
                .thenReturn(
                        new ClassRef(
                                classId, subject, "C", "Class", ClassStatus.OPEN, null, false));
        when(materialModuleRepository.listQuizLinks(subject))
                .thenReturn(
                        List.of(
                                new MaterialModule.QuizLink(moduleId, open, 0),
                                new MaterialModule.QuizLink(moduleId, draft, 1)));
        when(quizQuery.loadQuiz(open))
                .thenReturn(
                        Optional.of(quiz(open, QuizScope.SUBJECT, subject, null, QuizStatus.OPEN)));
        when(quizQuery.loadQuiz(draft))
                .thenReturn(
                        Optional.of(
                                quiz(draft, QuizScope.SUBJECT, subject, null, QuizStatus.DRAFT)));
        when(quizQuery.listForClass(classId))
                .thenReturn(List.of(quiz(open, QuizScope.SUBJECT, subject, null, QuizStatus.OPEN)));
        var views = materialModuleService.listClassModules(student, classId, true);
        assertThat(views).hasSize(2);
        assertThat(views.get(0).quizzes())
                .extracting(MaterialViews.ModuleQuiz::quizId)
                .containsExactly(open);
        assertThat(views.get(0).canManage()).isFalse();
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
    void lastModuleWithArchivedMaterialCannotBeDeleted() {
        when(materialModuleRepository.listModules(subject)).thenReturn(List.of(module));
        when(materialModuleRepository.countArchivedMaterials(moduleId)).thenReturn(1);

        assertThatThrownBy(() -> materialModuleService.deleteModule(manager, moduleId, 0))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_MODULE_LAST"));

        verify(materialModuleRepository, never()).moveArchivedMaterials(any(), any());
        verify(materialModuleRepository, never()).deleteModule(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void lostDeleteCasDoesNotAuditSuccess() {
        when(materialModuleRepository.deleteModule(moduleId, 0)).thenReturn(false);
        assertThatThrownBy(() -> materialModuleService.deleteModule(manager, moduleId, 0))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
        verifyNoInteractions(audit);
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
        verifyNoInteractions(audit);
    }

    @Test
    void reorderRejectsDuplicateStaleOrForeignModulesBeforeAnyWrite() {
        var first = new MaterialCommands.Position(moduleId, 0L);
        assertThatThrownBy(
                        () ->
                                materialModuleService.reorderModules(
                                        manager,
                                        subject,
                                        new MaterialCommands.Reorder(List.of(first, first))))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_REORDER_INVALID"));
        when(materialModuleRepository.findModule(otherId)).thenReturn(Optional.of(other));
        assertThatThrownBy(
                        () ->
                                materialModuleService.reorderModules(
                                        manager,
                                        subject,
                                        new MaterialCommands.Reorder(
                                                List.of(
                                                        first,
                                                        new MaterialCommands.Position(
                                                                otherId, 1L)))))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
        when(materialModuleRepository.findModule(otherId))
                .thenReturn(
                        Optional.of(
                                new MaterialModule(
                                        otherId,
                                        UUID.randomUUID(),
                                        "Foreign",
                                        0,
                                        manager.accountId(),
                                        now,
                                        now,
                                        0)));
        assertThatThrownBy(
                        () ->
                                materialModuleService.reorderModules(
                                        manager,
                                        subject,
                                        new MaterialCommands.Reorder(
                                                List.of(
                                                        first,
                                                        new MaterialCommands.Position(
                                                                otherId, 0L)))))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_NOT_FOUND"));
        verify(materialModuleRepository, never()).updateModule(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void reorderPersistsRequestedPositionsAndRejectsLostCas() {
        when(materialModuleRepository.findModule(otherId)).thenReturn(Optional.of(other));
        when(materialModuleRepository.updateModule(any(), anyLong())).thenReturn(true);
        var command =
                new MaterialCommands.Reorder(
                        List.of(
                                new MaterialCommands.Position(otherId, 0L),
                                new MaterialCommands.Position(moduleId, 0L)));
        materialModuleService.reorderModules(manager, subject, command);
        verify(materialModuleRepository)
                .updateModule(
                        argThat(
                                m ->
                                        m.id().equals(otherId)
                                                && m.orderNo() == 0
                                                && m.version() == 1),
                        eq(0L));
        verify(materialModuleRepository)
                .updateModule(
                        argThat(
                                m ->
                                        m.id().equals(moduleId)
                                                && m.orderNo() == 1
                                                && m.version() == 1),
                        eq(0L));

        when(materialModuleRepository.updateModule(any(), anyLong())).thenReturn(false);
        assertThatThrownBy(() -> materialModuleService.reorderModules(manager, subject, command))
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

    @Test
    void studentViewNeverCreatesModules() {
        var student = new Actor(UUID.randomUUID(), Role.STUDENT);
        when(materialAccessPolicy.requireClassListAccess(student, classId, true))
                .thenReturn(
                        new ClassRef(
                                classId, subject, "C", "Class", ClassStatus.OPEN, null, false));
        when(materialModuleRepository.listModules(subject)).thenReturn(List.of());

        materialModuleService.listClassModules(student, classId, true);

        verify(materialModuleRepository, never()).insertModule(any());
    }
}
