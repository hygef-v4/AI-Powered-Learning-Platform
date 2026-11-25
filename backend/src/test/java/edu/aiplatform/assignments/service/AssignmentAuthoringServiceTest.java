package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.dto.AssignmentViews;

import static edu.aiplatform.assignments.service.AssignmentFixtures.diagram;
import static edu.aiplatform.assignments.service.AssignmentFixtures.heading;
import static edu.aiplatform.assignments.service.AssignmentFixtures.paragraph;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentRubric;
import edu.aiplatform.assignments.model.PartCalculator;
import edu.aiplatform.identity.contract.ResourceRef;
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
            return e.readCode();
        }
        throw new AssertionError("Cần lỗi nghiệp vụ");
    }

    @SuppressWarnings("unchecked")
    private static List<ValidationIssue> issues(Runnable action) {
        try {
            action.run();
        } catch (AssignmentProblem e) {
            return (List<ValidationIssue>) e.describeProperties().get("issues");
        }
        throw new AssertionError("Cần lỗi có danh sách chỗ cần sửa");
    }

    @Test
    void outsiderGetsNotFoundAndDeniedAudit() {
        f.deny();
        assertThat(code(() -> f.authoring.createForClass(f.teacher, f.classId,
                new AssignmentCommands.Create(SubmissionMode.INDIVIDUAL, GradingMode.GRADED, false, null))))
                .isEqualTo("ASSIGNMENT_NOT_FOUND");
        assertThat(f.store).isEmpty();
    }

    @Test
    void archivedSubjectRejectsCreateAndEditEvenWhenManagerAuthorizationAllows() {
        Assignment original = f.put(AssignmentScope.SUBJECT, AssignmentStatus.DRAFT,
                new DocumentModel(List.of()), false);
        when(f.subjects.getSubject(f.subjectId)).thenReturn(Optional.of(
                new edu.aiplatform.subjects.contract.SubjectRef(f.subjectId, "S1", "Subject",
                        edu.aiplatform.subjects.contract.SubjectStatus.ARCHIVED, null)));
        when(f.subjects.isActive(f.subjectId)).thenReturn(false);
        assertThat(code(() -> f.authoring.createForSubject(f.manager, f.subjectId,
                new AssignmentCommands.Create(SubmissionMode.INDIVIDUAL, GradingMode.GRADED, true, null))))
                .isEqualTo("ASSIGNMENT_READ_ONLY");
        assertThat(code(() -> f.authoring.saveSkeleton(f.manager, original.id(),
                new AssignmentCommands.Skeleton(original.version(), new DocumentModel(List.of()), null, false))))
                .isEqualTo("ASSIGNMENT_READ_ONLY");
        assertThat(f.store).containsOnlyKeys(original.id());
        assertThat(f.store.get(original.id())).isEqualTo(original);
    }

    @Test
    void materialsForAiCreateComeFromTheClassOrSubjectBeforeTheAssignmentExists() {
        UUID uml = UUID.randomUUID();
        UUID erd = UUID.randomUUID();
        when(f.materials.listSelectable(any(), argThat(ref -> ResourceRef.courseClass(f.classId).equals(ref))))
                .thenReturn(List.of(new SelectableMaterial(uml, "UML", true)));
        when(f.materials.listSelectable(any(), argThat(ref -> ResourceRef.subject(f.subjectId).equals(ref))))
                .thenReturn(List.of(new SelectableMaterial(erd, "ERD", false)));

        assertThat(f.authoring.listSelectableMaterialsForClass(f.teacher, f.classId))
                .containsExactly(new AssignmentViews.Material(uml, "UML", true));
        assertThat(f.authoring.listSelectableMaterialsForSubject(f.manager, f.subjectId))
                .containsExactly(new AssignmentViews.Material(erd, "ERD", false));
    }

    @Test
    void groupNeedsU12InClassButSubjectTemplateMayBeGroup() {
        assertThat(code(() -> f.authoring.createForClass(f.teacher, f.classId,
                new AssignmentCommands.Create(SubmissionMode.GROUP, GradingMode.GRADED, false, null))))
                .isEqualTo("GROUP_NOT_SUPPORTED");
        AssignmentViews.Detail template = f.authoring.createForSubject(f.manager, f.subjectId,
                new AssignmentCommands.Create(SubmissionMode.GROUP, GradingMode.GRADED, false, "Đồ án nhóm"));
        assertThat(template.submissionMode()).isEqualTo(SubmissionMode.GROUP);
        assertThat(template.warnings()).extracting(ValidationIssue::code).doesNotContain("GROUP_NOT_SUPPORTED");
        assertThat(code(() -> f.copies.copyToClass(f.teacher, f.classId, template.id(), null)))
                .as("lớp chưa dùng được bài nhóm thì không copy đề mẫu nhóm về").isEqualTo("GROUP_NOT_SUPPORTED");
        assertThat(code(() -> AssignmentAuthoringService.validateModes(SubmissionMode.GROUP, GradingMode.PRACTICE)))
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
    void staleVersionIsConflictAndDiagramsAlwaysAllowed() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        assertThat(code(() -> f.authoring.saveSkeleton(f.teacher, a.id(),
                new AssignmentCommands.Skeleton(a.version() - 1, new DocumentModel(List.of()), null, false))))
                .isEqualTo("ASSIGNMENT_VERSION_CONFLICT");
        // Bài cũ lưu diagramEnabled = false vẫn được thêm sơ đồ.
        Assignment legacy = f.store.computeIfPresent(a.id(), (id, x) -> x.updateDetails(x.title(), x.instructions(), false,
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
    void sectionsWithoutRubricItemsAreNotGradedAndOnlyHalfWrittenItemsBlockReview() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT,
                new DocumentModel(List.of(heading("H1", "Có chấm"), heading("H1", "Không chấm"))), false);
        String graded = a.parts().get(0).partId();
        f.authoring.updateRubric(f.teacher, a.id(), graded, new AssignmentCommands.RubricUpdate(a.version(), AssignmentFixtures.completeCriteria()));
        Assignment current = f.store.get(a.id());
        assertThat(f.authoring.review(f.teacher, a.id(), current.version()).status()).isEqualTo(AssignmentStatus.REVIEWED);
        assertThat(f.rubrics.listRubrics(a.id())).extracting(RubricView::partId).containsExactly(graded);
        Assignment half = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of(heading("H1", "Dở"))), false);
        RubricCriterion noPoints = new RubricCriterion(null, "Đúng", "", CriterionKind.TEXT, List.of(new RubricItem(null, "Đúng", null)));
        f.authoring.updateRubric(f.teacher, half.id(), half.parts().get(0).partId(), new AssignmentCommands.RubricUpdate(half.version(), List.of(noPoints)));
        assertThat(issues(() -> f.authoring.review(f.teacher, half.id(), f.store.get(half.id()).version())))
                .extracting(ValidationIssue::message).containsExactly("Bài làm chưa có rubric chấm điểm");
    }

    @Test
    void rubricWithZeroTotalOrHalfWrittenItemsIsMissingPointsButBlankRowsAreIgnored() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT,
                new DocumentModel(List.of(heading("H1", "Một"), heading("H1", "Hai"))), false);
        String one = a.parts().get(0).partId();
        String two = a.parts().get(1).partId();
        f.authoring.updateRubric(f.teacher, a.id(), one, new AssignmentCommands.RubricUpdate(a.version(),
                AssignmentFixtures.completeCriteria()));
        RubricCriterion zero = new RubricCriterion(null, "Đủ", "", CriterionKind.TEXT,
                List.of(new RubricItem(null, "Có", BigDecimal.ZERO)));
        f.authoring.updateRubric(f.teacher, a.id(), two, new AssignmentCommands.RubricUpdate(f.store.get(a.id()).version(),
                List.of(zero)));
        assertThat(issues(() -> f.authoring.review(f.teacher, a.id(), f.store.get(a.id()).version())))
                .extracting(ValidationIssue::code, ValidationIssue::blockId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("RUBRIC_POINTS_MISSING", a.parts().get(1).headingBlockId()));
    }

    @Test
    void wholeAssignmentRubricAcceptsZeroPointsAndIsListedFirstOnlyWhenFilled() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of(heading("H1", "Mục"))), true);
        assertThat(f.rubrics.listRubrics(a.id())).extracting(RubricView::partId).doesNotContain(RubricService.WHOLE);
        RubricCriterion whole = new RubricCriterion(null, "Trình bày", "", CriterionKind.TEXT, List.of(
                new RubricItem(null, "Đúng mẫu", new BigDecimal("1")), new RubricItem(null, "Có mục lục", BigDecimal.ZERO)));
        AssignmentViews.Rubric saved = f.authoring.updateRubric(f.teacher, a.id(), RubricService.WHOLE,
                new AssignmentCommands.RubricUpdate(a.version(), List.of(whole)));
        assertThat(saved.partTitle()).isEqualTo("Toàn bài");
        assertThat(saved.complete()).isTrue();
        assertThat(f.rubrics.listRubrics(a.id()).get(0).partId()).isEqualTo(RubricService.WHOLE);
        AssignmentViews.Detail detail = f.authoring.loadAssignment(f.teacher, a.id());
        assertThat(detail.parts()).extracting(AssignmentViews.Part::partId)
                .containsExactly(RubricService.WHOLE, a.parts().get(0).partId());
        assertThat(detail.totalPoints()).isEqualByComparingTo("6.00");
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
        assertThat(code(() -> f.authoring.deleteAssignment(f.teacher, open.id(), open.version()))).isEqualTo("ASSIGNMENT_NOT_EDITABLE");
        Assignment draft = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        f.authoring.deleteAssignment(f.teacher, draft.id(), draft.version());
        assertThat(f.store).doesNotContainKey(draft.id());
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
        AssignmentViews.Detail detail = f.authoring.loadAssignment(f.teacher, open.id());
        assertThat(detail.permissions().canManage()).isFalse();
        assertThat(detail.permissions().canEditContent()).isFalse();
        assertThat(code(() -> f.authoring.loadAssignment(f.teacher, draft.id()))).isEqualTo("ASSIGNMENT_NOT_FOUND");
        verify(f.rubricRepository, never()).saveRubric(any());
    }
}
