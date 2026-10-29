package edu.aiplatform.quizzes.web;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.quizzes.application.QuizAuthoringService;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.domain.Difficulty;
import edu.aiplatform.shared.error.ApiException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.util.MultiValueMap;

/** Đọc JSON của API Quiz: field lạ, sai kiểu → {@code 400 QUIZ_REQUEST_INVALID}. */
final class QuizRequests {

    private QuizRequests() {
    }

    record Details(QuizAuthoringService.Details details, long version) {
    }

    record Items(List<QuizAuthoringService.ItemInput> items, long version) {
    }

    record Retire(String reason, long version) {
    }

    record RandomPreview(int count, Difficulty difficulty, String tag) {
    }

    static String title(JsonNode body) {
        fields(body, Set.of("title"));
        return text(body, "title", true);
    }

    static Details details(JsonNode body) {
        fields(body, Set.of("title", "instructions", "settings", "activityMonitoringEnabled", "activityNotice",
                "referenceMaterialIds", "version"));
        JsonNode settings = body.get("settings");
        if (settings == null || !settings.isObject()) {
            throw invalid("settings cần là object");
        }
        fields(settings, Set.of("shuffleQuestions", "shuffleOptions", "timeLimitMinutes", "maxAttempts",
                "showScoreAfterSubmit", "showCorrectAnswers"));
        QuizSettings parsed = new QuizSettings(bool(settings, "shuffleQuestions"), bool(settings, "shuffleOptions"),
                integer(settings, "timeLimitMinutes"), integer(settings, "maxAttempts"),
                bool(settings, "showScoreAfterSubmit"), bool(settings, "showCorrectAnswers"));
        List<UUID> materials = new ArrayList<>();
        JsonNode refs = body.get("referenceMaterialIds");
        if (refs != null && !refs.isNull()) {
            if (!refs.isArray()) {
                throw invalid("referenceMaterialIds cần là danh sách");
            }
            for (JsonNode node : refs) {
                materials.add(uuid(node, "referenceMaterialIds"));
            }
        }
        return new Details(new QuizAuthoringService.Details(text(body, "title", true), text(body, "instructions", false),
                parsed, bool(body, "activityMonitoringEnabled"), text(body, "activityNotice", false), materials),
                version(body));
    }

    static Items items(JsonNode body) {
        fields(body, Set.of("items", "version"));
        JsonNode list = body.get("items");
        if (list == null || !list.isArray()) {
            throw invalid("items cần là danh sách");
        }
        List<QuizAuthoringService.ItemInput> items = new ArrayList<>();
        for (JsonNode node : list) {
            fields(node, Set.of("questionId", "points"));
            JsonNode points = node.get("points");
            if (points != null && !points.isNull() && !points.isNumber()) {
                throw invalid("points cần là số");
            }
            items.add(new QuizAuthoringService.ItemInput(uuid(node.get("questionId"), "questionId"),
                    points == null || points.isNull() ? null : points.decimalValue()));
        }
        return new Items(items, version(body));
    }

    static Retire retire(JsonNode body) {
        fields(body, Set.of("reason", "version"));
        return new Retire(text(body, "reason", false), version(body));
    }

    static UUID copySource(JsonNode body) {
        fields(body, Set.of("sourceQuizId"));
        return uuid(body.get("sourceQuizId"), "sourceQuizId");
    }

    static RandomPreview randomPreview(JsonNode body) {
        fields(body, Set.of("count", "difficulty", "tag"));
        Integer count = integer(body, "count");
        if (count == null) {
            throw invalid("Thiếu count");
        }
        String difficulty = text(body, "difficulty", false);
        Difficulty parsed;
        try {
            parsed = difficulty == null ? null : Difficulty.valueOf(difficulty);
        } catch (IllegalArgumentException unknown) {
            throw invalid("difficulty không hợp lệ");
        }
        return new RandomPreview(count, parsed, text(body, "tag", false));
    }

    static void query(MultiValueMap<String, String> params, Set<String> allowed) {
        if (!allowed.containsAll(params.keySet()) || params.values().stream().anyMatch(v -> v.size() != 1)) {
            throw invalid("Chỉ nhận " + String.join(", ", allowed) + ", mỗi trường một giá trị");
        }
        String q = params.getFirst("q");
        if (q != null && q.length() > 200) {
            throw invalid("Từ khóa tối đa 200 ký tự");
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

    private static String text(JsonNode body, String field, boolean required) {
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

    private static boolean bool(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || !value.isBoolean()) {
            throw invalid(field + " cần là true hoặc false");
        }
        return value.asBoolean();
    }

    private static Integer integer(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.canConvertToExactIntegral() || !value.canConvertToInt()) {
            throw invalid(field + " cần là số nguyên");
        }
        return value.asInt();
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

    private static long version(JsonNode body) {
        JsonNode value = body.get("version");
        if (value == null || !value.canConvertToExactIntegral() || value.asLong() < 0) {
            throw invalid("Thiếu version hoặc version không hợp lệ");
        }
        return value.asLong();
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest("QUIZ_REQUEST_INVALID", message);
    }
}
