package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.dto.AssignmentViews;

import static edu.aiplatform.assignments.service.AssignmentFixtures.diagram;
import static edu.aiplatform.assignments.service.AssignmentFixtures.heading;
import static edu.aiplatform.assignments.service.AssignmentFixtures.paragraph;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentRubric;
import edu.aiplatform.assignments.model.PartCalculator;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** F3, F4, F5, F8, F14; BR-U07-02…04, 10…16, 20…24, 30…32, 70, 71. */
class AssignmentAuthoringServiceTest {

    private final AssignmentFixtures f = new AssignmentFixtures();

    private static String code(Runnable action) {
        try {
            action.run();
        } catch (ApiException e) {
            return e.code();
        }
        throw new AssertionError("Cần lỗi nghiệp vụ");
    }

    @SuppressWarnings("unchecked")
    private static List<ValidationIssue> issues(Runnable action) {
        try {
            action.run();
        } catch (AssignmentProblem e) {
            return (List<ValidationIssue>) e.properties().get("issues");
        }
        throw new AssertionError("Cần lỗi có danh sách chỗ cần sửa");
    }

    @Test
    void outsiderGetsNotFoundAndDeniedAudit() {
        f.deny();
        assertThat(code(() -> f.authoring.createForClass(f.teacher, f.classId,
                new AssignmentCommands.Create(SubmissionMode.INDIVIDUAL, GradingMode.GRADED, false, null))))
                .isEqualTo("ASSIGNMENT_NOT_FOUND");
        verify(f.audit).record(argThat(e -> e.result() == AuditResult.DENIED && e.action().equals("ACCESS_DENIED")));
        assertThat(f.store).isEmpty();
    }

    @Test
    void createsDraftWithOneWholeDocumentPartAndEmptyRubric() {
        AssignmentViews.Detail created = f.authoring.createForClass(f.teacher, f.classId,
                new AssignmentCommands.Create(SubmissionMode.INDIVIDUAL, GradingMode.PRACTICE, false, null));
        assertThat(created.status()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(created.diagramEnabled()).as("sơ đồ luôn bật, cờ gửi lên bị bỏ qua").isTrue();
        assertThat(created.versionNo()).isEqualTo(1);
        assertThat(created.parts()).singleElement().satisfies(p -> assertThat(p.partId()).isEqualTo(PartCalculator.WHOLE_DOCUMENT));
        assertThat(f.rubricsOf(created.id())).singleElement().satisfies(r -> assertThat(r.criteria()).isEmpty());
        verify(f.audit).record(argThat(e -> e.action().equals("ASSIGNMENT_CREATED") && e.result() == AuditResult.SUCCESS));
    }

    @Test
    void groupNeedsU12AndOnlyClassScope() {
        assertThat(code(() -> f.authoring.createForClass(f.teacher, f.classId,
                new AssignmentCommands.Create(SubmissionMode.GROUP, GradingMode.GRADED, false, null))))
                .isEqualTo("GROUP_NOT_SUPPORTED");
        assertThat(code(() -> f.authoring.createForSubject(f.manager, f.subjectId,
                new AssignmentCommands.Create(SubmissionMode.GROUP, GradingMode.GRADED, false, null))))
                .isEqualTo("GROUP_NOT_ALLOWED");
        assertThat(code(() -> AssignmentAuthoringService.modes(SubmissionMode.GROUP, GradingMode.PRACTICE)))
                .isEqualTo("GROUP_MUST_BE_GRADED");
    }

    @Test
    void archivedClassIsReadOnly() {
        when(f.classes.getClassRef(any())).thenAnswer(i -> Optional.of(f.classRef(i.getArgument(0), ClassStatus.ARCHIVED)));
        assertThat(code(() -> f.authoring.createForClass(f.teacher, f.classId,
                new AssignmentCommands.Create(SubmissionMode.INDIVIDUAL, GradingMode.GRADED, false, null))))
                .isEqualTo("ASSIGNMENT_READ_ONLY");
    }

    @Test
    void savingSkeletonRecomputesPartsAndSyncsRubrics() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.REVIEWED, new DocumentModel(List.of()), true);
        DocumentBlock h1 = heading("H1", "Phân tích");
        DocumentBlock h2 = heading("H2", "Use case");
        DocumentBlock h3 = heading("H1", "Thiết kế");
        AssignmentViews.Detail saved = f.authoring.saveSkeleton(f.teacher, a.id(),
                new AssignmentCommands.Skeleton(a.version(), new DocumentModel(List.of(h1, h2, paragraph("x"), h3)), null, false));
        assertThat(saved.status()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(saved.parts()).extracting(AssignmentViews.Part::partId)
                .containsExactly(h2.id().toString(), h3.id().toString());
        assertThat(f.rubricsOf(a.id())).extracting(AssignmentRubric::partId)
                .containsExactlyInAnyOrder(h2.id().toString(), h3.id().toString());
        assertThat(saved.version()).isEqualTo(a.version() + 1);
    }

    @Test
    void staleVersionIsConflictAndDiagramsAlwaysAllowed() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        assertThat(code(() -> f.authoring.saveSkeleton(f.teacher, a.id(),
                new AssignmentCommands.Skeleton(a.version() - 1, new DocumentModel(List.of()), null, false))))
                .isEqualTo("ASSIGNMENT_VERSION_CONFLICT");
        // Bài cũ lưu diagramEnabled = false vẫn được thêm sơ đồ.
        Assignment legacy = f.store.computeIfPresent(a.id(), (id, x) -> x.withDetails(x.title(), x.instructions(), false,
                x.gradingMode(), x.requiredDiagrams(), x.referenceMaterialIds(), false, null, x.updatedAt()));
        assertThat(legacy.diagramEnabled()).isTrue();
        AssignmentViews.Detail saved = f.authoring.saveSkeleton(f.teacher, legacy.id(), new AssignmentCommands.Skeleton(
                legacy.version(), new DocumentModel(List.of(diagram(DocumentModelServiceTest.DIAGRAM))), null, false));
        assertThat(saved.skeleton().blocks()).singleElement().satisfies(b -> assertThat(b.type()).isEqualTo(BlockType.DIAGRAM));
    }

    @Test
    void diagramsCannotBeTurnedOff() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT,
                new DocumentModel(List.of(diagram(DocumentModelServiceTest.DIAGRAM))), false);
        AssignmentViews.Detail saved = f.authoring.updateDetails(f.teacher, a.id(), new AssignmentCommands.Details(
                a.version(), null, null, false, null, Map.of(DiagramType.CLASS, 1), null, null, null));
        assertThat(saved.diagramEnabled()).isTrue();
        assertThat(saved.requiredDiagrams()).containsEntry(DiagramType.CLASS, 1);
    }

    @Test
    void activityMonitoringTurnsOnWithoutRetentionSetting() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        assertThat(f.authoring.updateDetails(f.teacher, a.id(), new AssignmentCommands.Details(a.version(),
                null, null, null, null, null, null, true, "Có ghi nhận")).activityMonitoringEnabled()).isTrue();
    }

    @Test
    void reviewNeedsCompleteRubricsThenAudits() {
        Assignment empty = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of(heading("H1", "Mục"))), false);
        assertThat(issues(() -> f.authoring.review(f.teacher, empty.id(), empty.version())))
                .extracting(ValidationIssue::code).contains("RUBRIC_INCOMPLETE");
        Assignment ready = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of(heading("H1", "Mục"))), true);
        assertThat(f.authoring.review(f.teacher, ready.id(), ready.version()).status()).isEqualTo(AssignmentStatus.REVIEWED);
        verify(f.audit).record(argThat(e -> e.action().equals("ASSIGNMENT_REVIEWED")));
    }

    @Test
    void rubricRulesAndTotal() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        String part = a.parts().get(0).partId();
        RubricCriterion bad = new RubricCriterion(null, "T", "", CriterionKind.TEXT,
                List.of(new RubricItem(null, "x", new BigDecimal("1.234"))));
        assertThat(code(() -> f.authoring.updateRubric(f.teacher, a.id(), part,
                new AssignmentCommands.RubricUpdate(a.version(), List.of(bad))))).isEqualTo("RUBRIC_POINTS_INVALID");
        RubricCriterion good = new RubricCriterion(null, "Nội dung", "", CriterionKind.TEXT, List.of(
                new RubricItem(null, "a", new BigDecimal("2.5")), new RubricItem(null, "b", new BigDecimal("1.25")),
                new RubricItem(null, "chưa có điểm", null)));
        AssignmentViews.Rubric saved = f.authoring.updateRubric(f.teacher, a.id(), part,
                new AssignmentCommands.RubricUpdate(a.version(), List.of(good)));
        assertThat(saved.totalPoints()).isEqualByComparingTo("3.75");
        assertThat(saved.complete()).isFalse();
        assertThat(saved.criteria().get(0).items()).allSatisfy(item -> assertThat(item.itemId()).isNotNull());
    }

    @Test
    void lockedRubricCannotChangeAfterPublish() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.SCHEDULED, new DocumentModel(List.of()), true);
        assertThat(code(() -> f.authoring.updateRubric(f.teacher, a.id(), a.parts().get(0).partId(),
                new AssignmentCommands.RubricUpdate(a.version(), AssignmentFixtures.completeCriteria()))))
                .isEqualTo("ASSIGNMENT_NOT_EDITABLE");
    }

    @Test
    void onlyUnpublishedAssignmentsCanBeDeleted() {
        Assignment open = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        assertThat(code(() -> f.authoring.delete(f.teacher, open.id(), open.version()))).isEqualTo("ASSIGNMENT_NOT_EDITABLE");
        Assignment draft = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        f.authoring.delete(f.teacher, draft.id(), draft.version());
        assertThat(f.store).doesNotContainKey(draft.id());
        verify(f.audit).record(argThat(e -> e.action().equals("ASSIGNMENT_DELETED")));
    }

    @Test
    void teachersOfTheSubjectOnlyReadPublishedSubjectAssignments() {
        Assignment draft = f.put(AssignmentScope.SUBJECT, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        Assignment open = f.put(AssignmentScope.SUBJECT, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        UUID taught = UUID.randomUUID();
        when(f.authorization.authorize(any(), any(), any())).thenAnswer(i -> {
            var ref = i.<edu.aiplatform.identity.contract.ResourceRef>getArgument(2);
            return taught.equals(ref.id()) ? edu.aiplatform.identity.contract.AuthorizationDecision.allow()
                    : edu.aiplatform.identity.contract.AuthorizationDecision.deny("OUT");
        });
        when(f.classScope.listTaughtClasses(f.teacher.accountId())).thenReturn(List.of(taught));
        when(f.classScope.subjectOfClass(taught)).thenReturn(Optional.of(f.subjectId));
        AssignmentViews.Detail detail = f.authoring.detail(f.teacher, open.id());
        assertThat(detail.permissions().canManage()).isFalse();
        assertThat(detail.permissions().canEditContent()).isFalse();
        assertThat(code(() -> f.authoring.detail(f.teacher, draft.id()))).isEqualTo("ASSIGNMENT_NOT_FOUND");
        verify(f.rubricRepository, never()).save(any());
    }
}
