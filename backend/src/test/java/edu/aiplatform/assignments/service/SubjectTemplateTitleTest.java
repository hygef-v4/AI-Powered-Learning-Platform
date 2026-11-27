package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.dto.AssignmentViews;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tên đề mẫu không trùng trong cùng môn; danh sách chọn đề mẫu có ngày tạo (yêu cầu 2026-11-27). */
class SubjectTemplateTitleTest {

    private final AssignmentFixtures f = new AssignmentFixtures();

    private static String code(Runnable action) {
        try {
            action.run();
        } catch (ApiException e) {
            return e.readCode();
        }
        throw new AssertionError("Cần lỗi nghiệp vụ");
    }

    private AssignmentViews.Detail createTemplate(String title) {
        return f.authoring.createForSubject(f.manager, f.subjectId,
                new AssignmentCommands.Create(SubmissionMode.INDIVIDUAL, GradingMode.GRADED, false, title));
    }

    private AssignmentViews.Detail rename(AssignmentViews.Detail current, String title) {
        return f.authoring.updateDetails(f.manager, current.id(), new AssignmentCommands.Details(current.version(),
                title, null, null, null, null, null, null, null));
    }

    @Test
    void enteredTitleThatMatchesAnotherTemplateIsRejected() {
        createTemplate("Đề cuối kỳ");
        assertThat(code(() -> createTemplate("  đề   CUỐI kỳ ")))
                .as("so không phân biệt hoa thường và khoảng trắng thừa").isEqualTo("TEMPLATE_TITLE_DUPLICATE");
        AssignmentViews.Detail other = createTemplate("Đề giữa kỳ");
        assertThat(code(() -> rename(other, "Đề cuối kỳ"))).isEqualTo("TEMPLATE_TITLE_DUPLICATE");
        assertThat(rename(other, "Đề giữa kỳ 2").title()).isEqualTo("Đề giữa kỳ 2");
    }

    @Test
    void classAssignmentsAndRetiredTemplatesDoNotBlockTheName() {
        f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), true);
        Assignment retired = f.put(AssignmentScope.SUBJECT, AssignmentStatus.RETIRED, new DocumentModel(List.of()), true);
        assertThat(createTemplate(retired.title()).title()).isEqualTo("Bài 1");
        assertThat(f.authoring.createForClass(f.teacher, f.classId,
                new AssignmentCommands.Create(SubmissionMode.INDIVIDUAL, GradingMode.GRADED, false, "Bài 1")).title())
                .as("bài của lớp không theo quy tắc này").isEqualTo("Bài 1");
    }

    @Test
    void existingDuplicatesStillSaveWhenTheTitleIsUnchanged() {
        Assignment first = f.put(AssignmentScope.SUBJECT, AssignmentStatus.DRAFT, new DocumentModel(List.of()), true);
        f.put(AssignmentScope.SUBJECT, AssignmentStatus.DRAFT, new DocumentModel(List.of()), true);
        AssignmentViews.Detail saved = f.authoring.updateDetails(f.manager, first.id(), new AssignmentCommands.Details(
                first.version(), first.title(), "Hướng dẫn mới", null, null, null, null, null, null));
        assertThat(saved.instructions()).isEqualTo("Hướng dẫn mới");
    }

    @Test
    void generatedTitlesGetANumberInsteadOfFailing() {
        AssignmentViews.Detail blank = createTemplate(null);
        AssignmentViews.Detail blankAgain = createTemplate("");
        assertThat(blank.title()).isEqualTo(AssignmentAuthoringService.DEFAULT_TITLE);
        assertThat(blankAgain.title()).isEqualTo(AssignmentAuthoringService.DEFAULT_TITLE + " (2)");

        AssignmentViews.Detail template = createTemplate("Lab UML");
        assertThat(f.copies.cloneAssignment(f.manager, template.id()).title()).isEqualTo("Lab UML (2)");
        assertThat(f.copies.copyToSubject(f.manager, f.subjectId, template.id(), null).title()).isEqualTo("Lab UML (3)");
        assertThat(code(() -> f.copies.copyToSubject(f.manager, f.subjectId, template.id(), "lab uml")))
                .as("tên nhập tay trùng thì báo lỗi, không tự đánh số").isEqualTo("TEMPLATE_TITLE_DUPLICATE");
    }

    @Test
    void longGeneratedTitleIsShortenedToFitTheNumber() {
        String longTitle = "A".repeat(Assignment.TITLE_MAX);
        AssignmentViews.Detail template = createTemplate(longTitle);
        String cloned = f.copies.cloneAssignment(f.manager, template.id()).title();
        assertThat(cloned).hasSize(Assignment.TITLE_MAX).endsWith(" (2)");
    }

    @Test
    void versionsOfTheSameTemplateShareTheName() {
        Assignment closed = f.put(AssignmentScope.SUBJECT, AssignmentStatus.CLOSED, new DocumentModel(List.of()), true);
        AssignmentViews.Detail next = f.copies.createNewVersion(f.manager, closed.id());
        assertThat(next.title()).isEqualTo(closed.title());
        AssignmentViews.Detail renamed = rename(next, "Tên tạm");
        assertThat(rename(renamed, closed.title()).title()).as("đặt lại tên của version trước").isEqualTo("Bài 1");
    }

    @Test
    void templateOptionsCarryCreationDate() {
        AssignmentViews.Detail target = createTemplate("Đề đang soạn");
        createTemplate("Đề mẫu");
        assertThat(f.aiDrafts.listTemplates(f.manager, target.id()))
                .extracting(AssignmentViews.AiTemplateOption::title, AssignmentViews.AiTemplateOption::createdAt)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Đề mẫu", AssignmentFixtures.NOW));
    }
}
