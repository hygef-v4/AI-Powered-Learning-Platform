package edu.aiplatform.quizzes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.QuestionContent;
import edu.aiplatform.quizzes.model.QuestionOption;
import edu.aiplatform.quizzes.service.QuestionBankService;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.util.MultiValueMap;

/** Đọc JSON của API ngân hàng câu hỏi: field lạ, sai kiểu → {@code 400 QUESTION_REQUEST_INVALID}. */
final class QuestionRequests {

    private static final Set<String> CONTENT_FIELDS = Set.of("type", "stem", "options", "correctOptionIds",
            "explanation", "defaultPoints", "difficulty", "tags");
    private static final Set<String> QUERY_FIELDS = Set.of("type", "difficulty", "status", "q", "page", "size");
    private static final int MAX_BULK = 200;

    private QuestionRequests() {
    }

    record Update(QuestionContent content, long version) {
    }

    record Reject(String reason, long version) {
    }

    record Bulk(List<QuestionBankService.ReviewItem> items, String reason) {
    }

    static Reject reject(JsonNode body) {
        rejectUnknownFields(body, Set.of("reason", "version"));
        return new Reject(readText(body, "reason", true), readVersion(body));
    }

    /** Danh sách {id, version}, tối đa 200 câu mỗi lần. */
    static Bulk parseBulk(JsonNode body) {
        rejectUnknownFields(body, Set.of("items", "reason"));
        JsonNode list = readArray(body, "items");
        if (list.isEmpty() || list.size() > MAX_BULK) {
            throw createInvalidError("Mỗi lần xử lý 1–200 câu");
        }
        List<QuestionBankService.ReviewItem> items = new ArrayList<>();
        for (JsonNode node : list) {
            rejectUnknownFields(node, Set.of("id", "version"));
            UUID id;
            try {
                id = UUID.fromString(readText(node, "id", true));
            } catch (IllegalArgumentException malformed) {
                throw createInvalidError("id cần là mã định danh");
            }
            items.add(new QuestionBankService.ReviewItem(id, readVersion(node)));
        }
        return new Bulk(items, readText(body, "reason", false));
    }

    static QuestionContent parseCreate(JsonNode body) {
        rejectUnknownFields(body, CONTENT_FIELDS);
        return parseContent(body);
    }

    static Update parseUpdate(JsonNode body) {
        Set<String> allowed = new HashSet<>(CONTENT_FIELDS);
        allowed.add("version");
        rejectUnknownFields(body, allowed);
        return new Update(parseContent(body), readVersion(body));
    }

    static long readVersionOnly(JsonNode body) {
        rejectUnknownFields(body, Set.of("version"));
        return readVersion(body);
    }

    static void query(MultiValueMap<String, String> params) {
        if (!QUERY_FIELDS.containsAll(params.keySet()) || params.values().stream().anyMatch(v -> v.size() != 1)) {
            throw createInvalidError("Chỉ nhận type, difficulty, status, q, page, size, mỗi trường một giá trị");
        }
        String q = params.getFirst("q");
        if (q != null && q.length() > 200) {
            throw createInvalidError("Từ khóa tối đa 200 ký tự");
        }
    }

    static QuestionContent parseContent(JsonNode body) {
        QuestionType type = parseEnumValue(body, "type", QuestionType.class, true);
        List<QuestionOption> options = new ArrayList<>();
        JsonNode optionNodes = readArray(body, "options");
        for (JsonNode node : optionNodes) {
            if (!node.isObject()) {
                throw createInvalidError("Mỗi lựa chọn cần dạng {id, text}");
            }
            rejectUnknownFields(node, Set.of("id", "text"));
            options.add(new QuestionOption(readText(node, "id", true), readText(node, "text", true)));
        }
        List<String> correct = new ArrayList<>();
        for (JsonNode node : readArray(body, "correctOptionIds")) {
            if (!node.isTextual()) {
                throw createInvalidError("correctOptionIds cần danh sách chuỗi");
            }
            correct.add(node.asText());
        }
        List<String> tags = new ArrayList<>();
        if (body.has("tags") && !body.get("tags").isNull()) {
            for (JsonNode node : readArray(body, "tags")) {
                if (!node.isTextual()) {
                    throw createInvalidError("tags cần danh sách chuỗi");
                }
                tags.add(node.asText());
            }
        }
        return QuestionContent.createContent(type, readText(body, "stem", true), options, correct, readText(body, "explanation", false),
                parsePoints(body), parseEnumValue(body, "difficulty", Difficulty.class, false), tags);
    }

    static void rejectUnknownFields(JsonNode body, Set<String> allowed) {
        if (body == null || !body.isObject()) {
            throw createInvalidError("Cần một JSON object");
        }
        Iterator<String> names = body.fieldNames();
        while (names.hasNext()) {
            if (!allowed.contains(names.next())) {
                throw createInvalidError("Có trường không được phép trong yêu cầu");
            }
        }
    }

    static JsonNode readArray(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || !value.isArray()) {
            throw createInvalidError(field + " cần là danh sách");
        }
        return value;
    }

    static String readText(JsonNode body, String field, boolean required) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) {
            if (required) {
                throw createInvalidError("Thiếu " + field);
            }
            return null;
        }
        if (!value.isTextual()) {
            throw createInvalidError(field + " cần là chuỗi");
        }
        return value.asText();
    }

    private static BigDecimal parsePoints(JsonNode body) {
        JsonNode value = body.get("defaultPoints");
        if (value == null || value.isNull()) {
            throw createInvalidError("Thiếu defaultPoints");
        }
        if (!value.isNumber()) {
            throw createInvalidError("defaultPoints cần là số");
        }
        return value.decimalValue();
    }

    static <E extends Enum<E>> E parseEnumValue(JsonNode body, String field, Class<E> type, boolean required) {
        String value = readText(body, field, required);
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException unknown) {
            throw createInvalidError(field + " không hợp lệ");
        }
    }

    static long readVersion(JsonNode body) {
        JsonNode value = body.get("version");
        if (value == null || !value.canConvertToExactIntegral() || value.asLong() < 0) {
            throw createInvalidError("Thiếu version hoặc version không hợp lệ");
        }
        return value.asLong();
    }

    static ApiException createInvalidError(String message) {
        return ApiException.createBadRequest("QUESTION_REQUEST_INVALID", message);
    }
}
