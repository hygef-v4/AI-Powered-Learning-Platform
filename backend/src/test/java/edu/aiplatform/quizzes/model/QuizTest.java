package edu.aiplatform.quizzes.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** BR-U10-20, 24, 31: vòng đời Quiz, xóa khi chưa phát hành, bản sao về nháp. */
class QuizTest {
    @Test
    void legacySettingsUpdatePreservesFormattingWithTrailingBlankParagraphs() {
        var document = new edu.aiplatform.shared.content.FormattedText(java.util.List.of(
                new edu.aiplatform.shared.content.FormattedText.Block(
                        edu.aiplatform.shared.content.FormattedText.Type.PARAGRAPH,
                        edu.aiplatform.shared.content.FormattedText.Align.CENTER, 0,
                        java.util.List.of(new edu.aiplatform.shared.content.FormattedText.Run("Nội dung", true, null, null))),
                new edu.aiplatform.shared.content.FormattedText.Block(
                        edu.aiplatform.shared.content.FormattedText.Type.PARAGRAPH,
                        edu.aiplatform.shared.content.FormattedText.Align.LEFT, 0,
                        java.util.List.of(new edu.aiplatform.shared.content.FormattedText.Run("", null, null, null)))));
        Quiz quiz = draft().replaceInstructionsDocument(document);
        Quiz edited = quiz.updateDetails(quiz.title(), quiz.instructions().strip(), quiz.settings(), false, null, NOW);
        assertThat(edited.instructionsDocument()).isEqualTo(document);
        assertThat(edited.instructions()).isEqualTo("Nội dung\n");
        assertThat(edited.markPublished(NOW).markRetired("Archive", NOW).markReopened(NOW).instructionsDocument()).isEqualTo(document);
    }

    @Test
    void formattedInstructionsSurvivePublicationCopiesAndSettingsOnlyEdits() {
        var document = new edu.aiplatform.shared.content.FormattedText(java.util.List.of(
                new edu.aiplatform.shared.content.FormattedText.Block(
                        edu.aiplatform.shared.content.FormattedText.Type.PARAGRAPH,
                        edu.aiplatform.shared.content.FormattedText.Align.CENTER, 0,
                        java.util.List.of(new edu.aiplatform.shared.content.FormattedText.Run("Nội dung", true, null, null)))));
        Quiz quiz = draft().replaceInstructionsDocument(document);
        assertThat(quiz.instructions()).isEqualTo("Nội dung");
        assertThat(quiz.markPublished(NOW).instructionsDocument()).isEqualTo(document);
        assertThat(quiz.copyAs(QuizOrigin.CLONE, 1, null, author, NOW).instructionsDocument()).isEqualTo(document);
        assertThat(quiz.updateDetails(quiz.title(), quiz.instructions(), quiz.settings(), false, null, NOW)
                .instructionsDocument()).isEqualTo(document);
        assertThat(quiz.updateDetails(quiz.title(), "Plain edit", quiz.settings(), false, null, NOW)
                .instructionsDocument()).isNull();
    }

    private static final Instant NOW = Instant.parse("2026-10-20T08:00:00Z");
    private final UUID subject = UUID.randomUUID();
    private final UUID author = UUID.randomUUID();

    private Quiz draft() {
        return Quiz.createQuiz(QuizScope.SUBJECT, null, subject, "Luyện tập", author, NOW);
    }

    @Test
    void lifecycleGoesDraftOpenRetired() {
        assertThat(draft().markEdited(NOW).status()).as("sửa nháp vẫn nháp").isEqualTo(QuizStatus.DRAFT);

        Quiz open = draft().markPublished(NOW);
        assertThat(open.status()).isEqualTo(QuizStatus.OPEN);
        assertThat(open.isEditable()).as("quiz đang mở sửa tại chỗ").isTrue();
        assertThat(open.isDeletable()).isFalse();
        assertThat(open.markEdited(NOW).status()).as("sửa quiz đang mở vẫn mở").isEqualTo(QuizStatus.OPEN);

        Quiz retired = open.markRetired("Hết dùng", NOW);
        assertThat(retired.status()).isEqualTo(QuizStatus.RETIRED);
        assertThat(retired.retiredReason()).isEqualTo("Hết dùng");
        assertThat(retired.markReopened(NOW).status()).as("trùng thời điểm vẫn là Quiz từng phát hành")
                .isEqualTo(QuizStatus.OPEN);
        assertThatThrownBy(() -> open.markPublished(NOW)).as("chỉ phát hành từ nháp").isInstanceOf(IllegalStateException.class);

        Quiz later = open.markRetired("Hết dùng", NOW.plusSeconds(60));
        assertThat(later.markReopened(NOW.plusSeconds(120)).status()).as("quiz đã phát hành mở lại").isEqualTo(QuizStatus.OPEN);
        assertThat(later.markReopened(NOW.plusSeconds(120)).retiredAt()).isNull();
        Quiz archivedDraft = draft().markRetired("Lưu trữ", NOW);
        assertThat(archivedDraft.publishedAt()).as("lưu trữ không phải phát hành").isNull();
        assertThat(archivedDraft.markReopened(NOW.plusSeconds(60)).status()).as("lưu trữ khi còn nháp thì về nháp").isEqualTo(QuizStatus.DRAFT);
        assertThat(archivedDraft.markReopened(NOW.plusSeconds(60)).publishedAt()).isNull();
        assertThat(archivedDraft.markReopened(NOW.plusSeconds(60)).isDeletable()).isTrue();
    }

    @Test
    void copyStartsAsDraftWithoutPublicationHistory() {
        Quiz open = draft().updateDetails("Bài", "Hướng dẫn", new QuizSettings(true, true, 30, 3, true, true), false,
                null, NOW).changePaperMode(QuizPaperMode.GENERATED, NOW).markPublished(NOW);

        Quiz copy = open.copyAs(QuizOrigin.NEW_VERSION, 2, null, author, NOW);

        assertThat(copy.id()).isNotEqualTo(open.id());
        assertThat(copy.sourceQuizId()).isEqualTo(open.id());
        assertThat(copy.status()).isEqualTo(QuizStatus.DRAFT);
        assertThat(copy.publishedAt()).isNull();
        assertThat(copy.versionNo()).isEqualTo(2);
        assertThat(copy.settings()).isEqualTo(open.settings());
        assertThat(copy.paperMode()).isEqualTo(QuizPaperMode.GENERATED);
        assertThat(copy.isDeletable()).isTrue();
    }

    @Test
    void itemPointsMustBePositiveWithTwoDecimals() {
        assertThat(QuizRules.isValidPoints(new BigDecimal("0.25"))).isTrue();
        assertThat(QuizRules.isValidPoints(BigDecimal.ZERO)).isFalse();
        assertThat(QuizRules.isValidPoints(new BigDecimal("0.125"))).isFalse();
        assertThat(QuizRules.validateSettings(new QuizSettings(false, false, 0, 0, true, false)))
                .extracting(Violation::code).containsExactly("TIME_LIMIT_INVALID", "ATTEMPTS_INVALID");
    }
}
