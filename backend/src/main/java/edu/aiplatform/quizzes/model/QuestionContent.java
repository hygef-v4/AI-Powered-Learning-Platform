package edu.aiplatform.quizzes.model;

import edu.aiplatform.quizzes.contract.QuestionType;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Nội dung soạn được của một phiên bản câu (BR-U10-02, 03). Tạo bằng {@link #of} để chuẩn hóa (cắt khoảng trắng,
 * bỏ tag trùng), rồi kiểm bằng {@link QuestionRules#validate}.
 */
public record QuestionContent(QuestionType type, String stem, List<QuestionOption> options,
                              List<String> correctOptionIds, String explanation, BigDecimal defaultPoints,
                              Difficulty difficulty, List<String> tags) {

    public QuestionContent {
        options = options == null ? List.of() : List.copyOf(options);
        correctOptionIds = correctOptionIds == null ? List.of() : List.copyOf(correctOptionIds);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public static QuestionContent of(QuestionType type, String stem, List<QuestionOption> options,
                                     List<String> correctOptionIds, String explanation, BigDecimal defaultPoints,
                                     Difficulty difficulty, List<String> tags) {
        List<QuestionOption> cleanOptions = options == null ? List.of() : options.stream()
                .map(o -> new QuestionOption(trim(o.id()), trim(o.text())))
                .toList();
        List<String> cleanCorrect = correctOptionIds == null ? List.of() : correctOptionIds.stream()
                .map(QuestionContent::trim)
                .distinct()
                .toList();
        Set<String> seenTags = new HashSet<>();
        List<String> cleanTags = tags == null ? List.of() : tags.stream()
                .map(QuestionContent::trim)
                .filter(t -> t != null && !t.isEmpty())
                .filter(t -> seenTags.add(t.toLowerCase(Locale.ROOT)))
                .toList();
        String cleanExplanation = trim(explanation);
        return new QuestionContent(type, trim(stem), cleanOptions, cleanCorrect,
                cleanExplanation == null || cleanExplanation.isEmpty() ? null : cleanExplanation,
                defaultPoints, difficulty == null ? Difficulty.MEDIUM : difficulty, cleanTags);
    }

    private static String trim(String value) {
        return value == null ? null : value.strip();
    }
}
