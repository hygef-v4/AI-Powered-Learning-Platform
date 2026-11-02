package edu.aiplatform.quizzes.model;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.quizzes.contract.QuestionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** BR-U10-02, 03: loại câu, 2–6 lựa chọn, đáp án theo loại, độ dài, điểm, tag. */
class QuestionRulesTest {

    static QuestionContent single(String... options) {
        return content(QuestionType.MCQ_SINGLE, List.of("1"), options);
    }

    static QuestionContent content(QuestionType type, List<String> correct, String... options) {
        List<QuestionOption> list = IntStream.range(0, options.length)
                .mapToObj(i -> new QuestionOption(String.valueOf(i + 1), options[i])).toList();
        return QuestionContent.of(type, "2 + 2 bằng mấy?", list, correct, null, new BigDecimal("1.5"),
                Difficulty.EASY, List.of("số học"));
    }

    private static List<String> codes(QuestionContent content) {
        return QuestionRules.validate(content).stream().map(Violation::code).toList();
    }

    @Test
    void validSingleAndMultiAnswerQuestionsPass() {
        assertThat(QuestionRules.validate(single("3", "4"))).isEmpty();
        assertThat(QuestionRules.validate(content(QuestionType.MCQ_MULTI, List.of("1", "3"), "2", "3", "4", "5", "6", "7")))
                .isEmpty();
    }

    @Test
    void needsTwoToSixOptions() {
        assertThat(codes(single("4"))).contains("OPTION_COUNT");
        assertThat(codes(single("1", "2", "3", "4", "5", "6", "7"))).contains("OPTION_COUNT");
    }

    @Test
    void singleAnswerNeedsExactlyOneCorrectAndMultiAtLeastOne() {
        assertThat(codes(content(QuestionType.MCQ_SINGLE, List.of("1", "2"), "3", "4"))).contains("ANSWER_COUNT");
        assertThat(codes(content(QuestionType.MCQ_SINGLE, List.of(), "3", "4"))).contains("ANSWER_COUNT");
        assertThat(codes(content(QuestionType.MCQ_MULTI, List.of(), "3", "4"))).contains("ANSWER_COUNT");
        assertThat(codes(content(QuestionType.MCQ_MULTI, List.of("9"), "3", "4"))).contains("ANSWER_UNKNOWN");
    }

    @Test
    void optionsMustNotRepeatOrBeEmpty() {
        assertThat(codes(single("Bốn", " bốn "))).contains("OPTION_DUPLICATE");
        assertThat(codes(single("4", " "))).contains("OPTION_EMPTY");
    }

    @Test
    void typeAndStemAreRequiredAndTextIsBounded() {
        QuestionContent noType = content(null, List.of("1"), "3", "4");
        assertThat(codes(noType)).contains("TYPE_REQUIRED");
        QuestionContent blankStem = QuestionContent.of(QuestionType.MCQ_SINGLE, "  ", single("3", "4").options(),
                List.of("1"), null, BigDecimal.ONE, null, null);
        assertThat(codes(blankStem)).contains("REQUIRED");
        QuestionContent longStem = QuestionContent.of(QuestionType.MCQ_SINGLE, "a".repeat(20_001),
                single("3", "4").options(), List.of("1"), null, BigDecimal.ONE, null, null);
        assertThat(codes(longStem)).contains("TOO_LONG");
    }

    @Test
    void pointsArePositiveWithAtMostTwoDecimals() {
        for (String bad : new String[] {"0", "-1", "1.234", "10000"}) {
            QuestionContent c = QuestionContent.of(QuestionType.MCQ_SINGLE, "Câu", single("3", "4").options(),
                    List.of("1"), null, new BigDecimal(bad), null, null);
            assertThat(codes(c)).as(bad).contains("POINTS_INVALID");
        }
        QuestionContent ok = QuestionContent.of(QuestionType.MCQ_SINGLE, "Câu", single("3", "4").options(),
                List.of("1"), null, new BigDecimal("2.50"), null, null);
        assertThat(QuestionRules.validate(ok)).isEmpty();
    }

    @Test
    void atMostTenTagsAndDuplicatesIgnoringCaseAreDropped() {
        List<String> eleven = IntStream.range(0, 11).mapToObj(i -> "t" + i).toList();
        QuestionContent many = QuestionContent.of(QuestionType.MCQ_SINGLE, "Câu", single("3", "4").options(),
                List.of("1"), null, BigDecimal.ONE, null, eleven);
        assertThat(codes(many)).contains("TAG_COUNT");
        QuestionContent dup = QuestionContent.of(QuestionType.MCQ_SINGLE, "Câu", single("3", "4").options(),
                List.of("1"), null, BigDecimal.ONE, null, List.of("Đại số", "đại số", " ", "Hình"));
        assertThat(dup.tags()).containsExactly("Đại số", "Hình");
        assertThat(dup.difficulty()).isEqualTo(Difficulty.MEDIUM);
    }
}
