package edu.aiplatform.quizzes.model;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.quizzes.contract.QuestionType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionFingerprintTest {

    private static QuestionContent content(String stem, String... options) {
        List<QuestionOption> list = new java.util.ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            list.add(new QuestionOption(String.valueOf(i + 1), options[i]));
        }
        return QuestionContent.of(QuestionType.MCQ_SINGLE, stem, list, List.of("1"), null, BigDecimal.ONE, Difficulty.EASY, List.of());
    }

    @Test
    void hashIgnoresCaseSpacingOptionOrderAndUnicodeForm() {
        String composed = "Thuật toán sắp xếp nào ổn định?";
        String decomposed = java.text.Normalizer.normalize(composed, java.text.Normalizer.Form.NFD);
        assertThat(QuestionFingerprint.hash(content(composed, "A", "B")))
                .isEqualTo(QuestionFingerprint.hash(content("  " + decomposed.toUpperCase() + "\n", "b", " a ")));
    }

    @Test
    void hashChangesWithOptionsAndIgnoresPointsAndExplanation() {
        QuestionContent base = content("Câu hỏi", "A", "B");
        assertThat(QuestionFingerprint.hash(base)).isNotEqualTo(QuestionFingerprint.hash(content("Câu hỏi", "A", "C")));
        QuestionContent other = QuestionContent.of(QuestionType.MCQ_MULTI, "Câu hỏi", base.options(), List.of("2"), "Giải thích",
                new BigDecimal("9"), Difficulty.HARD, List.of("thẻ"));
        assertThat(QuestionFingerprint.hash(other)).isEqualTo(QuestionFingerprint.hash(base));
    }

    @Test
    void similaritySeparatesRewordedFromUnrelated() {
        String a = QuestionFingerprint.normalize("Độ phức tạp thời gian của thuật toán sắp xếp nổi bọt là gì?");
        String b = QuestionFingerprint.normalize("Độ phức tạp thời gian của thuật toán sắp xếp nổi bọt là bao nhiêu?");
        String c = QuestionFingerprint.normalize("Giao thức nào hoạt động ở tầng giao vận?");
        assertThat(QuestionFingerprint.similarity(a, a)).isEqualTo(1.0);
        assertThat(QuestionFingerprint.similarity(a, b)).isGreaterThanOrEqualTo(0.7);
        assertThat(QuestionFingerprint.similarity(a, c)).isLessThan(0.3);
    }
}
