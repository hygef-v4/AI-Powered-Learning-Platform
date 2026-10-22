package edu.aiplatform.quizzes.domain;

import edu.aiplatform.quizzes.contract.QuizSettings;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Quy tắc Quiz: thông tin, cài đặt (BR-U10-15), câu và điểm (BR-U10-13), điều kiện duyệt (BR-U10-21). */
public final class QuizRules {

    public static final int MAX_TITLE = 200;
    public static final int MAX_INSTRUCTIONS = 5_000;
    public static final int MAX_NOTICE = 1_000;
    public static final int MAX_ITEMS = 200;
    public static final int MAX_MATERIALS = 10;
    public static final int MAX_TIME_LIMIT = 300;
    public static final int MAX_ATTEMPTS = 10;
    public static final int MAX_REASON = 1_000;

    private QuizRules() {
    }

    public static List<Violation> validateDetails(String title, String instructions, QuizSettings settings,
                                                  String notice, List<UUID> materials) {
        List<Violation> violations = new ArrayList<>();
        if (title == null || title.isBlank()) {
            violations.add(new Violation("title", "REQUIRED", "Tên quiz không được để trống"));
        } else if (length(title) > MAX_TITLE) {
            violations.add(new Violation("title", "TOO_LONG", "Tên quiz tối đa 200 ký tự"));
        }
        if (instructions != null && length(instructions) > MAX_INSTRUCTIONS) {
            violations.add(new Violation("instructions", "TOO_LONG", "Hướng dẫn tối đa 5.000 ký tự"));
        }
        if (notice != null && length(notice) > MAX_NOTICE) {
            violations.add(new Violation("activityNotice", "TOO_LONG", "Lời báo tối đa 1.000 ký tự"));
        }
        if (materials != null && materials.size() > MAX_MATERIALS) {
            violations.add(new Violation("referenceMaterialIds", "MATERIAL_COUNT", "Gắn tối đa 10 học liệu"));
        }
        violations.addAll(validateSettings(settings));
        return List.copyOf(violations);
    }

    /** Bật ghi nhận hoạt động cần lời báo cho sinh viên và thời hạn lưu đã đặt (BR-U10-15, K2). */
    public static List<Violation> validateActivity(boolean enabled, String notice, boolean retentionConfigured) {
        List<Violation> violations = new ArrayList<>();
        if (!enabled) {
            return violations;
        }
        if (notice == null || notice.isBlank()) {
            violations.add(new Violation("activityNotice", "ACTIVITY_NOTICE_REQUIRED",
                    "Cần lời báo cho sinh viên khi bật ghi nhận hoạt động"));
        }
        if (!retentionConfigured) {
            violations.add(new Violation("activityMonitoringEnabled", "ACTIVITY_RETENTION_NOT_SET",
                    "Admin chưa đặt thời hạn lưu dữ liệu hoạt động"));
        }
        return violations;
    }

    public static List<Violation> validateSettings(QuizSettings settings) {
        List<Violation> violations = new ArrayList<>();
        if (settings == null) {
            violations.add(new Violation("settings", "REQUIRED", "Thiếu cấu hình quiz"));
            return violations;
        }
        Integer time = settings.timeLimitMinutes();
        if (time != null && (time < 1 || time > MAX_TIME_LIMIT)) {
            violations.add(new Violation("settings.timeLimitMinutes", "TIME_LIMIT_INVALID",
                    "Thời gian làm bài từ 1 đến 300 phút hoặc để trống"));
        }
        Integer attempts = settings.maxAttempts();
        if (attempts != null && (attempts < 1 || attempts > MAX_ATTEMPTS)) {
            violations.add(new Violation("settings.maxAttempts", "ATTEMPTS_INVALID",
                    "Số lượt từ 1 đến 10 hoặc không giới hạn"));
        }
        return violations;
    }

    /** Danh sách câu khi lưu: tối đa 200, không trùng câu, điểm > 0 tối đa 2 chữ số thập phân. */
    public static List<Violation> validateItems(List<QuizItem> items) {
        List<Violation> violations = new ArrayList<>();
        if (items.size() > MAX_ITEMS) {
            violations.add(new Violation("items", "ITEM_COUNT", "Quiz tối đa 200 câu"));
        }
        Set<UUID> seen = new HashSet<>();
        for (QuizItem item : items) {
            if (!seen.add(item.questionId())) {
                violations.add(new Violation("items", "ITEM_DUPLICATE", "Một câu chỉ xuất hiện một lần trong quiz"));
            }
            if (!validPoints(item.points())) {
                violations.add(new Violation("items", "POINTS_INVALID",
                        "Điểm câu phải lớn hơn 0, tối đa 2 chữ số thập phân"));
            }
        }
        return List.copyOf(violations);
    }

    /**
     * Điều kiện duyệt (BR-U10-21): 1–200 câu, câu ngân hàng còn {@code ACTIVE} và cùng môn, câu riêng hợp lệ,
     * điểm và cài đặt hợp lệ. Lỗi theo câu có {@code field} dạng {@code items[n]} (n từ 1).
     */
    public static List<Violation> validateForReview(Quiz quiz, List<QuizItem> items, Map<UUID, Question> questions) {
        List<Violation> violations = new ArrayList<>(validateDetails(quiz.title(), quiz.instructions(),
                quiz.settings(), quiz.activityNotice(), quiz.referenceMaterialIds()));
        if (items.isEmpty()) {
            violations.add(new Violation("items", "ITEM_COUNT", "Quiz cần ít nhất 1 câu"));
        }
        violations.addAll(validateItems(items));
        for (QuizItem item : items) {
            String field = "items[" + item.position() + "]";
            Question q = questions.get(item.questionId());
            if (q == null) {
                violations.add(new Violation(field, "QUESTION_MISSING", "Câu " + item.position() + " không còn tồn tại"));
            } else if (q.scope() == QuestionScope.BANK) {
                if (q.status() != QuestionStatus.ACTIVE || !q.subjectId().equals(quiz.subjectId())) {
                    violations.add(new Violation(field, "QUESTION_NOT_ACTIVE",
                            "Câu " + item.position() + " lấy từ ngân hàng đã ngưng dùng; hãy bỏ hoặc thay câu khác"));
                }
            } else if (!quiz.id().equals(q.quizId())) {
                violations.add(new Violation(field, "QUESTION_MISSING", "Câu " + item.position() + " không thuộc quiz"));
            } else {
                for (Violation v : QuestionRules.validate(q.content())) {
                    violations.add(new Violation(field, v.code(), "Câu " + item.position() + ": " + v.message()));
                }
            }
        }
        return List.copyOf(violations);
    }

    public static boolean validPoints(BigDecimal points) {
        return points != null && points.signum() > 0 && points.compareTo(QuestionRules.MAX_POINTS) <= 0
                && points.stripTrailingZeros().scale() <= 2;
    }

    private static int length(String value) {
        return value.codePointCount(0, value.length());
    }
}
