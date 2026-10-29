package edu.aiplatform.quizpractice.web;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.assignmentwork.contract.ActivityEventInput;
import edu.aiplatform.assignmentwork.contract.ActivityKind;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.util.MultiValueMap;

/** Đọc JSON của API Quiz Practice: field lạ, sai kiểu → {@code 400 QUIZ_PRACTICE_REQUEST_INVALID}. */
final class PracticeRequests {

    private static final int MAX_QUESTIONS = 200;
    private static final int MAX_OPTIONS = 6;

    private PracticeRequests() {
    }

    record Answers(Map<UUID, List<String>> answers, long answerVersion) {
    }

    static UUID classId(JsonNode body) {
        fields(body, Set.of("classId"));
        return uuid(body.get("classId"), "classId");
    }

    static Answers answers(JsonNode body) {
        fields(body, Set.of("answers", "answerVersion"));
        JsonNode map = body.get("answers");
        if (map == null || !map.isObject() || map.size() > MAX_QUESTIONS) {
            throw invalid("answers cần là object câu → danh sách lựa chọn");
        }
        Map<UUID, List<String>> answers = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> entries = map.fields();
        while (entries.hasNext()) {
            Map.Entry<String, JsonNode> entry = entries.next();
            UUID questionId;
            try {
                questionId = UUID.fromString(entry.getKey());
            } catch (IllegalArgumentException invalid) {
                throw invalid("Mã câu không hợp lệ");
            }
            JsonNode options = entry.getValue();
            if (!options.isArray() || options.size() > MAX_OPTIONS) {
                throw invalid("Lựa chọn của một câu cần là danh sách tối đa 6 mục");
            }
            List<String> ids = new ArrayList<>();
            for (JsonNode option : options) {
                if (!option.isTextual()) {
                    throw invalid("Mã lựa chọn cần là chuỗi");
                }
                ids.add(option.asText());
            }
            answers.put(questionId, ids);
        }
        JsonNode version = body.get("answerVersion");
        if (version == null || !version.canConvertToExactIntegral() || version.asLong() < 0) {
            throw invalid("Thiếu answerVersion");
        }
        return new Answers(answers, version.asLong());
    }

    static List<ActivityEventInput> activity(JsonNode body) {
        fields(body, Set.of("events"));
        JsonNode list = body.get("events");
        if (list == null || !list.isArray()) {
            throw invalid("events cần là danh sách");
        }
        List<ActivityEventInput> events = new ArrayList<>();
        for (JsonNode node : list) {
            fields(node, Set.of("sessionId", "clientEventId", "sequence", "kind", "clientTime"));
            JsonNode sequence = node.get("sequence");
            JsonNode kind = node.get("kind");
            JsonNode eventId = node.get("clientEventId");
            JsonNode time = node.get("clientTime");
            if (sequence == null || !sequence.canConvertToExactIntegral() || kind == null || !kind.isTextual()
                    || eventId == null || !eventId.isTextual() || eventId.asText().length() > 100
                    || time == null || !time.isTextual()) {
                throw invalid("Sự kiện hoạt động không hợp lệ");
            }
            try {
                events.add(new ActivityEventInput(uuid(node.get("sessionId"), "sessionId"), eventId.asText(),
                        sequence.asLong(), ActivityKind.valueOf(kind.asText()), Instant.parse(time.asText())));
            } catch (IllegalArgumentException | DateTimeParseException invalid) {
                throw invalid("Sự kiện hoạt động không hợp lệ");
            }
        }
        return events;
    }

    static void query(MultiValueMap<String, String> params, Set<String> allowed) {
        if (!allowed.containsAll(params.keySet()) || params.values().stream().anyMatch(v -> v.size() != 1)) {
            throw invalid("Chỉ nhận " + String.join(", ", allowed) + ", mỗi trường một giá trị");
        }
    }

    private static void fields(JsonNode body, Set<String> allowed) {
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

    private static UUID uuid(JsonNode value, String field) {
        if (value == null || !value.isTextual()) {
            throw invalid(field + " cần là mã định danh");
        }
        try {
            return UUID.fromString(value.asText());
        } catch (IllegalArgumentException invalid) {
            throw invalid(field + " cần là mã định danh");
        }
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest("QUIZ_PRACTICE_REQUEST_INVALID", message);
    }
}
