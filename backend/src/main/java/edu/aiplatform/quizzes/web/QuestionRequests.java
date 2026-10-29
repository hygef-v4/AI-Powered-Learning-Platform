package edu.aiplatform.quizzes.web;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.domain.Difficulty;
import edu.aiplatform.quizzes.domain.QuestionContent;
import edu.aiplatform.quizzes.domain.QuestionOption;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.springframework.util.MultiValueMap;

/** Đọc JSON của API ngân hàng câu hỏi: field lạ, sai kiểu → {@code 400 QUESTION_REQUEST_INVALID}. */
final class QuestionRequests {

    private static final Set<String> CONTENT_FIELDS = Set.of("type", "stem", "options", "correctOptionIds",
            "explanation", "defaultPoints", "difficulty", "tags");
    private static final Set<String> QUERY_FIELDS = Set.of("type", "difficulty", "status", "q", "page", "size");

    private QuestionRequests() {
    }

    record Update(QuestionContent content, long version) {
    }

    static QuestionContent create(JsonNode body) {
        fields(body, CONTENT_FIELDS);
        return content(body);
    }

    static Update update(JsonNode body) {
        Set<String> allowed = new HashSet<>(CONTENT_FIELDS);
        allowed.add("version");
        fields(body, allowed);
        return new Update(content(body), version(body));
    }

    static long versionOnly(JsonNode body) {
        fields(body, Set.of("version"));
        return version(body);
    }

    static void query(MultiValueMap<String, String> params) {
        if (!QUERY_FIELDS.containsAll(params.keySet()) || params.values().stream().anyMatch(v -> v.size() != 1)) {
            throw invalid("Chỉ nhận type, difficulty, status, q, page, size, mỗi trường một giá trị");
        }
        String q = params.getFirst("q");
        if (q != null && q.length() > 200) {
            throw invalid("Từ khóa tối đa 200 ký tự");
        }
    }

    static QuestionContent content(JsonNode body) {
        QuestionType type = enumValue(body, "type", QuestionType.class, true);
        List<QuestionOption> options = new ArrayList<>();
        JsonNode optionNodes = array(body, "options");
        for (JsonNode node : optionNodes) {
            if (!node.isObject()) {
                throw invalid("Mỗi lựa chọn cần dạng {id, text}");
            }
            fields(node, Set.of("id", "text"));
            options.add(new QuestionOption(text(node, "id", true), text(node, "text", true)));
        }
        List<String> correct = new ArrayList<>();
        for (JsonNode node : array(body, "correctOptionIds")) {
            if (!node.isTextual()) {
                throw invalid("correctOptionIds cần danh sách chuỗi");
            }
            correct.add(node.asText());
        }
        List<String> tags = new ArrayList<>();
        if (body.has("tags") && !body.get("tags").isNull()) {
            for (JsonNode node : array(body, "tags")) {
                if (!node.isTextual()) {
                    throw invalid("tags cần danh sách chuỗi");
                }
                tags.add(node.asText());
            }
        }
        return QuestionContent.of(type, text(body, "stem", true), options, correct, text(body, "explanation", false),
                points(body), enumValue(body, "difficulty", Difficulty.class, false), tags);
    }

    static void fields(JsonNode body, Set<String> allowed) {
        if (body == null || !body.isObject()) {
            throw invalid("Cần một JSON object");
        }
        Iterator<String> names = body.fieldNames();
        while (names.hasNext()) {
            if (!allowed.contains(names.next())) {
                throw invalid("Có trường không được phép trong yêu cầu");
            }
        }
    }

    static JsonNode array(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || !value.isArray()) {
            throw invalid(field + " cần là danh sách");
        }
        return value;
    }

    static String text(JsonNode body, String field, boolean required) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) {
            if (required) {
                throw invalid("Thiếu " + field);
            }
            return null;
        }
        if (!value.isTextual()) {
            throw invalid(field + " cần là chuỗi");
        }
        return value.asText();
    }

    private static BigDecimal points(JsonNode body) {
        JsonNode value = body.get("defaultPoints");
        if (value == null || value.isNull()) {
            throw invalid("Thiếu defaultPoints");
        }
        if (!value.isNumber()) {
            throw invalid("defaultPoints cần là số");
        }
        return value.decimalValue();
    }

    static <E extends Enum<E>> E enumValue(JsonNode body, String field, Class<E> type, boolean required) {
        String value = text(body, field, required);
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException unknown) {
            throw invalid(field + " không hợp lệ");
        }
    }

    static long version(JsonNode body) {
        JsonNode value = body.get("version");
        if (value == null || !value.canConvertToExactIntegral() || value.asLong() < 0) {
            throw invalid("Thiếu version hoặc version không hợp lệ");
        }
        return value.asLong();
    }

    static ApiException invalid(String message) {
        return ApiException.badRequest("QUESTION_REQUEST_INVALID", message);
    }
}
