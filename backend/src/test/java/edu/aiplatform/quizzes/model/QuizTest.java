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
}
