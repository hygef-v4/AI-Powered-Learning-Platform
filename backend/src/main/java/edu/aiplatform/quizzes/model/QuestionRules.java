package edu.aiplatform.quizzes.model;

import edu.aiplatform.quizzes.contract.QuestionType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Quy tắc nội dung câu trắc nghiệm (BR-U10-02, 03); dùng cho tạo tay, sửa, kích hoạt và nhập tệp. */
public final class QuestionRules {

    public static final int MIN_OPTIONS = 2;
    public static final int MAX_OPTIONS = 6;
    public static final int MAX_TEXT = 20_000;
    public static final int MAX_TAGS = 10;
    public static final int MAX_TAG_LENGTH = 50;
    public static final int MAX_OPTION_ID_LENGTH = 20;
    public static final BigDecimal MAX_POINTS = new BigDecimal("9999.99");

    private QuestionRules() {
    }

    public static List<Violation> validate(QuestionContent content) {
        List<Violation> violations = new ArrayList<>();
        if (content.type() == null) {
            violations.add(new Violation("type", "TYPE_REQUIRED", "Chọn loại câu: một đáp án hoặc nhiều đáp án"));
        }
        text(violations, "stem", content.stem(), true, "Nội dung câu hỏi");
        text(violations, "explanation", content.explanation(), false, "Giải thích");
        options(violations, content);
        points(violations, content.defaultPoints());
        tags(violations, content.tags());
        return List.copyOf(violations);
    }

    private static void text(List<Violation> violations, String field, String value, boolean required, String label) {
        if (value == null || value.isEmpty()) {
            if (required) {
                violations.add(new Violation(field, "REQUIRED", label + " không được để trống"));
            }
            return;
        }
        if (value.codePointCount(0, value.length()) > MAX_TEXT) {
            violations.add(new Violation(field, "TOO_LONG", label + " tối đa 20.000 ký tự"));
        }
    }

    private static void options(List<Violation> violations, QuestionContent content) {
        List<QuestionOption> options = content.options();
        if (options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) {
            violations.add(new Violation("options", "OPTION_COUNT", "Cần 2–6 lựa chọn"));
        }
        Set<String> ids = new HashSet<>();
        Set<String> texts = new HashSet<>();
        for (QuestionOption option : options) {
            if (option.id() == null || option.id().isEmpty() || option.id().length() > MAX_OPTION_ID_LENGTH) {
                violations.add(new Violation("options", "OPTION_ID_INVALID", "Mã lựa chọn không hợp lệ"));
            } else if (!ids.add(option.id())) {
                violations.add(new Violation("options", "OPTION_ID_DUPLICATE", "Mã lựa chọn bị trùng"));
            }
            if (option.text() == null || option.text().isEmpty()) {
                violations.add(new Violation("options", "OPTION_EMPTY", "Lựa chọn không được để trống"));
            } else if (option.text().codePointCount(0, option.text().length()) > MAX_TEXT) {
                violations.add(new Violation("options", "TOO_LONG", "Lựa chọn tối đa 20.000 ký tự"));
            } else if (!texts.add(option.text().toLowerCase(Locale.ROOT))) {
                violations.add(new Violation("options", "OPTION_DUPLICATE", "Các lựa chọn không được trùng nội dung"));
            }
        }
        List<String> correct = content.correctOptionIds();
        if (correct.stream().anyMatch(id -> !ids.contains(id))) {
            violations.add(new Violation("correctOptionIds", "ANSWER_UNKNOWN", "Đáp án đúng phải là một lựa chọn của câu"));
        } else if (content.type() == QuestionType.MCQ_SINGLE && correct.size() != 1) {
            violations.add(new Violation("correctOptionIds", "ANSWER_COUNT", "Câu một đáp án cần đúng 1 đáp án đúng"));
        } else if (content.type() == QuestionType.MCQ_MULTI && correct.isEmpty()) {
            violations.add(new Violation("correctOptionIds", "ANSWER_COUNT", "Câu nhiều đáp án cần ít nhất 1 đáp án đúng"));
        }
    }

    private static void points(List<Violation> violations, BigDecimal points) {
        if (points == null || points.signum() <= 0 || points.compareTo(MAX_POINTS) > 0
                || points.stripTrailingZeros().scale() > 2) {
            violations.add(new Violation("defaultPoints", "POINTS_INVALID", "Điểm phải lớn hơn 0, tối đa 2 chữ số thập phân"));
        }
    }

    private static void tags(List<Violation> violations, List<String> tags) {
        if (tags.size() > MAX_TAGS) {
            violations.add(new Violation("tags", "TAG_COUNT", "Tối đa 10 thẻ"));
        }
        if (tags.stream().anyMatch(t -> t.codePointCount(0, t.length()) > MAX_TAG_LENGTH)) {
            violations.add(new Violation("tags", "TAG_TOO_LONG", "Mỗi thẻ tối đa 50 ký tự"));
        }
    }
}
