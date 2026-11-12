package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.PromptContext;
import edu.aiplatform.identity.contract.Role;
import java.util.List;

/**
 * Từ điển ý định cho Tầng 2 (tiếng Việt có dấu và tiếng Anh). Không phải mẫu tấn công cụ thể như Tầng 1 mà là các nhóm
 * khái niệm để đánh giá logic: ai là đối tượng của câu, câu muốn làm gì, có nằm trong phạm vi chức năng không.
 */
final class PromptLexicon {

    private PromptLexicon() {
    }

    record PhraseSet(List<String> phrases) {

        static PhraseSet of(String... phrases) {
            return new PhraseSet(List.of(phrases));
        }

        boolean matches(PromptTextAnalysis.Tokens tokens) {
            return count(tokens) > 0;
        }

        int count(PromptTextAnalysis.Tokens tokens) {
            int hits = 0;
            for (String phrase : phrases) {
                if (tokens.containsPhrase(phrase)) {
                    hits++;
                }
            }
            return hits;
        }
    }

    // ---- Câu nhắm vào chính hệ thống AI ----

    /** Không gồm "hệ thống"/"system": đề bài nghiệp vụ dùng hai từ này rất thường xuyên. */
    static final PhraseSet AI_REFERENCE = PhraseSet.of("bạn", "mày", "ai", "bot", "chatbot", "mô hình", "trợ lý",
            "you", "assistant", "model", "gpt", "chatgpt", "claude", "gemini", "llm");

    /** Chỉ các dạng ra lệnh rõ; "quên"/"forget" đứng riêng bị loại vì "đừng quên…" là câu dặn bình thường. */
    static final PhraseSet CONTROL_VERB = PhraseSet.of("bỏ qua", "phớt lờ", "lờ đi", "quên đi", "quên hết",
            "quên mọi", "hãy quên", "không tuân theo", "không làm theo", "đừng làm theo", "đừng tuân theo", "vô hiệu",
            "ghi đè", "thoát khỏi", "tiết lộ", "ignore", "disregard", "forget all", "forget your", "forget previous",
            "forget everything", "override", "bypass", "disable", "reveal", "leak", "stop following");

    /** Đối tượng chỉ có nghĩa với hệ thống AI. */
    static final PhraseSet STRONG_TARGET = PhraseSet.of("chỉ dẫn", "chỉ thị", "prompt", "system prompt",
            "kiểm duyệt", "instruction", "instructions", "guardrail", "guardrails", "guidelines", "system message");

    /** Đối tượng cũng là từ nghiệp vụ (quy tắc đặt tên, giới hạn thời gian…): chỉ tính khi có từ chỉ định hệ thống. */
    static final PhraseSet WEAK_TARGET = PhraseSet.of("quy tắc", "ràng buộc", "giới hạn", "bộ lọc", "chính sách",
            "vai trò", "rule", "rules", "restriction", "restrictions", "filter", "filters", "policy", "policies",
            "role");

    static final PhraseSet SYSTEM_QUALIFIER = PhraseSet.of("trước đó", "ở trên", "ban đầu", "của bạn", "được giao",
            "previous", "prior", "above", "earlier", "original", "initial", "your");

    // ---- Tự nhận quyền cao hơn ----

    static final PhraseSet ADMIN_CLAIM = PhraseSet.of("tôi là admin", "em là admin", "tôi là quản trị viên",
            "với quyền admin", "với quyền quản trị", "quyền admin", "quyền quản trị", "quyền root", "cấp quyền",
            "nâng quyền", "toàn quyền", "chế độ quản trị", "i am the admin", "i am an admin", "i am admin",
            "i am an administrator", "as an admin", "as administrator", "admin access", "admin privileges",
            "root access", "sudo", "superuser", "grant me", "elevate my", "escalate my", "privilege escalation");

    static final PhraseSet TEACHER_CLAIM = PhraseSet.of("tôi là giảng viên", "tôi là giáo viên", "em là giảng viên",
            "với quyền giảng viên", "quyền giảng viên", "i am the teacher", "i am a teacher", "i am the instructor",
            "as the teacher", "as a teacher", "as the instructor");

    static final PhraseSet MANAGER_CLAIM = PhraseSet.of("tôi là trưởng bộ môn", "tôi là quản lý môn",
            "quyền trưởng bộ môn", "quyền quản lý môn", "i am the subject manager", "as the subject manager",
            "as head of department");

    static final PhraseSet SCORE_TAMPERING = PhraseSet.of("đổi điểm", "sửa điểm", "nâng điểm", "cộng điểm",
            "cho em điểm", "cho tôi điểm", "cho em 10", "cho tôi 10", "điểm tối đa cho em", "điểm tối đa cho tôi",
            "change my grade", "change my score", "set my score", "set my grade", "give me full marks",
            "give me full score", "mark me as passed");

    // ---- Nội dung gây hại ----

    static final PhraseSet HARM_ACTION = PhraseSet.of("viết", "tạo", "chế tạo", "chế", "làm", "code", "lập trình",
            "phát tán", "triển khai", "hướng dẫn cách", "cách làm", "cài", "write", "create", "build", "develop",
            "make", "deploy", "spread", "how to", "craft");

    static final PhraseSet HARM_OBJECT = PhraseSet.of("mã độc", "malware", "ransomware", "keylogger", "trojan",
            "botnet", "virus máy tính", "sâu máy tính", "computer virus", "bom", "thuốc nổ", "bomb", "explosive",
            "explosives", "vũ khí", "weapon", "weapons", "chất độc", "poison", "ma túy", "đánh cắp mật khẩu",
            "steal passwords", "steal credentials", "chiếm đoạt tài khoản");

    /** Khung đánh giá/học thuật trong cùng câu làm chủ đề nguy hiểm trở thành nội dung giảng dạy. */
    static final PhraseSet ASSESSMENT_FRAME = PhraseSet.of("câu hỏi", "đề", "đề bài", "bài tập", "quiz",
            "trắc nghiệm", "khái niệm", "lý thuyết", "phòng chống", "phòng tránh", "phát hiện", "nhận diện",
            "phòng thủ", "question", "questions", "exam", "assignment", "concept", "theory", "detect", "detection",
            "prevent", "prevention", "defense", "defence");

    /** Đòi chi tiết vận hành thì khung đánh giá không còn làm giảm rủi ro. */
    static final PhraseSet OPERATIONAL_DETAIL = PhraseSet.of("mã nguồn hoàn chỉnh", "chạy được", "có thể chạy",
            "từng bước", "chi tiết cách", "công thức chế", "step by step", "working code", "full code",
            "complete code", "ready to run", "source code");

    static final PhraseSet ALWAYS_HARMFUL = PhraseSet.of("ấu dâm", "khiêu dâm trẻ em", "child porn", "csam",
            "muốn tự tử", "muốn tự sát", "kill myself", "tấn công khủng bố", "terrorist attack", "mass shooting",
            "giết người hàng loạt");

    // ---- Phạm vi chức năng ----

    static final PhraseSet OFF_TOPIC = PhraseSet.of("thời tiết", "giá vàng", "bitcoin", "tiền điện tử",
            "chứng khoán", "cổ phiếu", "xổ số", "lô đề", "cá cược", "cá độ", "bói", "tử vi", "công thức nấu",
            "nấu ăn", "đặt vé", "vé máy bay", "khách sạn", "lời bài hát", "thơ tình", "chuyện cười", "hẹn hò",
            "weather", "stock price", "crypto", "cryptocurrency", "lottery", "gambling", "betting", "horoscope",
            "recipe", "cooking", "flight ticket", "hotel booking", "song lyrics", "love poem", "joke", "dating");

    static final PhraseSet EXAM_EDITING_SCOPE = PhraseSet.of("đề", "đề bài", "đề thi", "câu", "câu hỏi", "bài",
            "bài tập", "yêu cầu", "dịch", "tiếng anh", "tiếng việt", "ngữ cảnh", "bối cảnh", "tình huống", "chủ đề",
            "độ khó", "khó", "dễ", "thêm", "bớt", "xóa", "bỏ", "sửa", "đổi", "viết lại", "soạn", "tạo", "trắc nghiệm",
            "đáp án", "lựa chọn", "phương án", "barem", "rubric", "điểm", "chương", "bài học", "tài liệu", "nội dung",
            "ôn tập", "kiến thức", "quiz", "exam", "question", "questions", "assignment", "task", "translate",
            "english", "vietnamese", "context", "scenario", "difficulty", "easier", "harder", "add", "remove",
            "delete", "rewrite", "generate", "create", "topic", "case", "chapter", "material");

    static final PhraseSet GRADING_RUBRIC_SCOPE = PhraseSet.of("barem", "rubric", "thang điểm", "điểm", "tiêu chí",
            "tiêu chuẩn", "trọng số", "phân phối", "phân bổ", "mức", "mức độ", "đánh giá", "chấm", "chấm điểm", "câu",
            "phần", "criteria", "criterion", "weight", "weights", "level", "levels", "score", "scores", "points",
            "marks", "grading", "band", "descriptor", "part");

    static final PhraseSet STUDENT_FEEDBACK_SCOPE = PhraseSet.of("giải thích", "lỗi", "sai", "vì sao", "tại sao",
            "gợi ý", "hướng", "hướng tiếp cận", "cách tiếp cận", "hiểu", "nhận xét", "cải thiện", "góp ý",
            "chưa đúng", "chỗ nào", "phần nào", "sửa", "hint", "hints", "explain", "explanation", "why", "mistake",
            "mistakes", "error", "errors", "wrong", "approach", "improve", "feedback");

    static final PhraseSet FULL_SOLUTION = PhraseSet.of("giải hộ", "làm hộ", "viết hộ", "làm giúp", "làm bài giúp",
            "giải giúp toàn bộ", "giải toàn bộ", "lời giải đầy đủ", "lời giải hoàn chỉnh", "đáp án đầy đủ",
            "toàn bộ đáp án", "toàn bộ lời giải", "bài giải mẫu", "cho em đáp án", "cho tôi đáp án", "đưa đáp án",
            "full solution", "complete solution", "entire solution", "solve it for me", "solve this for me",
            "do my homework", "do my assignment", "write the whole", "write it for me", "give me the answer",
            "give me the answers", "answer key");

    static final PhraseSet GRADE_MANIPULATION = PhraseSet.of("nâng điểm cho", "hạ điểm cho", "cho điểm tối đa",
            "cho qua hết", "đánh trượt hết", "give full marks", "full marks to", "pass everyone", "fail everyone");

    static PhraseSet scopeOf(PromptContext context) {
        switch (context) {
            case EXAM_EDITING:
                return EXAM_EDITING_SCOPE;
            case GRADING_RUBRIC:
                return GRADING_RUBRIC_SCOPE;
            case STUDENT_FEEDBACK:
                return STUDENT_FEEDBACK_SCOPE;
            default:
                return PhraseSet.of();
        }
    }

    /** Phạm vi hẹp: câu phải có ít nhất một dấu hiệu đúng chức năng. */
    static boolean requiresScopeSignal(PromptContext context) {
        return context == PromptContext.GRADING_RUBRIC || context == PromptContext.STUDENT_FEEDBACK;
    }

    static PhraseSet forbiddenIntentOf(PromptContext context) {
        switch (context) {
            case STUDENT_FEEDBACK:
                return FULL_SOLUTION;
            case GRADING_RUBRIC:
                return GRADE_MANIPULATION;
            default:
                return PhraseSet.of();
        }
    }

    /** Tự nhận vai trò cao hơn vai trò thật của người gửi. */
    static boolean claimsHigherRole(Role actual, PromptTextAnalysis.Tokens tokens) {
        if (ADMIN_CLAIM.matches(tokens)) {
            return actual != Role.ADMIN;
        }
        if (MANAGER_CLAIM.matches(tokens)) {
            return actual == Role.STUDENT || actual == Role.TEACHER;
        }
        if (TEACHER_CLAIM.matches(tokens)) {
            return actual == Role.STUDENT;
        }
        return actual == Role.STUDENT && SCORE_TAMPERING.matches(tokens);
    }
}
