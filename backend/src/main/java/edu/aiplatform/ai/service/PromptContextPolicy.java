package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.PromptContext;
import edu.aiplatform.identity.contract.Role;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** Chính sách theo phạm vi: độ dài, vai trò được dùng và câu báo thân thiện. Một nguồn duy nhất cho cả hai tầng. */
enum PromptContextPolicy {

    EXAM_EDITING(PromptContext.EXAM_EDITING, 4_000, EnumSet.of(Role.TEACHER, Role.SUBJECT_MANAGER),
            "dịch đề, đổi ngữ cảnh, thêm/bớt yêu cầu bài tập, đổi độ khó hoặc soạn câu hỏi"),
    GRADING_RUBRIC(PromptContext.GRADING_RUBRIC, 2_000, EnumSet.of(Role.TEACHER),
            "chỉnh barem, phân phối điểm và tiêu chí đánh giá bài làm"),
    STUDENT_FEEDBACK(PromptContext.STUDENT_FEEDBACK, 1_000, EnumSet.of(Role.STUDENT),
            "giải thích lỗi sai và gợi ý hướng tiếp cận, không giải hộ toàn bộ bài"),
    GENERAL(PromptContext.GENERAL, 2_000, EnumSet.allOf(Role.class),
            "các câu hỏi liên quan đến học tập và giảng dạy trên hệ thống");

    static final int MIN_TEXT_CHARS = 2;

    static final String INJECTION_MESSAGE = "Yêu cầu có nội dung giống câu lệnh điều khiển hệ thống AI nên chưa được "
            + "gửi. Hãy mô tả trực tiếp thay đổi bạn muốn, không yêu cầu AI đổi vai trò hay bỏ qua quy tắc.";
    static final String HARMFUL_MESSAGE = "Yêu cầu có nội dung không phù hợp hoặc có thể gây hại nên không được gửi "
            + "tới AI. Hãy điều chỉnh nội dung và thử lại.";
    static final String PERMISSION_MESSAGE = "Bạn không có quyền dùng chức năng AI này.";
    static final String STATE_MESSAGE = "Chức năng AI này chưa dùng được ở trạng thái hiện tại. Hãy tải lại trang và "
            + "thử lại.";
    static final String UNAVAILABLE_MESSAGE = "Chưa kiểm tra được yêu cầu nên chưa gửi tới AI. Vui lòng thử lại sau.";

    private final PromptContext context;
    private final int maxChars;
    private final Set<Role> allowedRoles;
    private final String scope;

    PromptContextPolicy(PromptContext context, int maxChars, Set<Role> allowedRoles, String scope) {
        this.context = context;
        this.maxChars = maxChars;
        this.allowedRoles = allowedRoles;
        this.scope = scope;
    }

    static PromptContextPolicy of(PromptContext context) {
        for (PromptContextPolicy policy : values()) {
            if (policy.context == context) {
                return policy;
            }
        }
        throw new IllegalArgumentException("Chưa có chính sách cho phạm vi " + context);
    }

    int maxChars() {
        return maxChars;
    }

    boolean allows(Role role) {
        return role != null && allowedRoles.contains(role);
    }

    String tooLongMessage() {
        return "Yêu cầu tối đa " + String.format(Locale.ROOT, "%,d", maxChars).replace(',', '.') + " ký tự.";
    }

    String tooShortMessage() {
        return "Yêu cầu quá ngắn. Hãy mô tả rõ điều bạn muốn AI làm.";
    }

    String offTopicMessage() {
        return "Yêu cầu nằm ngoài phạm vi của chức năng này. Ở đây AI chỉ hỗ trợ " + scope + ".";
    }
}
