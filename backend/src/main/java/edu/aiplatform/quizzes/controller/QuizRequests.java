package edu.aiplatform.quizzes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.quizzes.service.QuizAuthoringService;
import edu.aiplatform.quizzes.service.QuizPaperService;
import edu.aiplatform.quizzes.contract.LeavePenalty;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.QuizPaperMode;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
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

    record DrawRules(QuizPaperMode paperMode, List<QuizPaperService.RuleInput> rules, long version) {
    }

    static String readTitle(JsonNode body) {
        rejectUnknownFields(body, Set.of("title"));
        return readText(body, "title", true);
    }

    static Details parseDetails(JsonNode body) {
        rejectUnknownFields(body, Set.of("title", "instructions", "settings", "activityMonitoringEnabled", "activityNotice",
                "version", "instructionsDocument"));
        JsonNode settings = body.get("settings");
        if (settings == null || !settings.isObject()) {
            throw createInvalidError("settings cần là object");
        }
        rejectUnknownFields(settings, Set.of("shuffleQuestions", "shuffleOptions", "timeLimitMinutes", "maxAttempts",
                "showScoreAfterSubmit", "showCorrectAnswers", "leavePenalty"));
        QuizSettings parsed = new QuizSettings(readBool(settings, "shuffleQuestions"), readBool(settings, "shuffleOptions"),
                readInteger(settings, "timeLimitMinutes"), readInteger(settings, "maxAttempts"),
                readBool(settings, "showScoreAfterSubmit"), readBool(settings, "showCorrectAnswers"),
                leavePenalty(settings.get("leavePenalty")));
        return new Details(new QuizAuthoringService.Details(readText(body, "title", true), readText(body, "instructions", false),
                parsed, readBool(body, "activityMonitoringEnabled"), readText(body, "activityNotice", false),
                edu.aiplatform.shared.content.FormattedText.parseDocument(body.get("instructionsDocument"))),
                readVersion(body));
    }

    private static LeavePenalty leavePenalty(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        rejectUnknownFields(node, Set.of("freeLeaves", "pointsPerLeave", "maxPenalty", "minAwaySeconds"));
        Integer free = readInteger(node, "freeLeaves");
        Integer minAway = readInteger(node, "minAwaySeconds");
        BigDecimal perLeave = readDecimal(node, "pointsPerLeave");
        if (free == null || minAway == null || perLeave == null) {
            throw createInvalidError("leavePenalty cần freeLeaves, pointsPerLeave, minAwaySeconds");
        }
        return new LeavePenalty(free, perLeave, readDecimal(node, "maxPenalty"), minAway);
    }

    /** Lưu chế độ đề và quy tắc sinh đề (BR-U10-17). */
    static DrawRules drawRules(JsonNode body) {
        rejectUnknownFields(body, Set.of("paperMode", "rules", "version"));
        String mode = readText(body, "paperMode", true);
        QuizPaperMode parsedMode;
        try {
            parsedMode = QuizPaperMode.valueOf(mode);
        } catch (IllegalArgumentException unknown) {
            throw createInvalidError("paperMode không hợp lệ");
        }
        JsonNode list = body.get("rules");
        if (list == null || !list.isArray()) {
            throw createInvalidError("rules cần là danh sách");
        }
        List<QuizPaperService.RuleInput> rules = new ArrayList<>();
        for (JsonNode node : list) {
            rejectUnknownFields(node, Set.of("tag", "difficulty", "questionCount", "pointsEach"));
            Integer count = readInteger(node, "questionCount");
            BigDecimal points = readDecimal(node, "pointsEach");
            if (count == null || points == null) {
                throw createInvalidError("Mỗi quy tắc cần questionCount và pointsEach");
            }
            rules.add(new QuizPaperService.RuleInput(readText(node, "tag", true), parseDifficulty(readText(node, "difficulty", false)),
                    count, points));
        }
        return new DrawRules(parsedMode, rules, readVersion(body));
    }

    static Items parseItems(JsonNode body) {
        rejectUnknownFields(body, Set.of("items", "version"));
        JsonNode list = body.get("items");
        if (list == null || !list.isArray()) {
            throw createInvalidError("items cần là danh sách");
        }
        List<QuizAuthoringService.ItemInput> items = new ArrayList<>();
        for (JsonNode node : list) {
            rejectUnknownFields(node, Set.of("questionId", "points"));
            JsonNode points = node.get("points");
            if (points != null && !points.isNull() && !points.isNumber()) {
                throw createInvalidError("points cần là số");
            }
            items.add(new QuizAuthoringService.ItemInput(parseUuid(node.get("questionId"), "questionId"),
                    points == null || points.isNull() ? null : points.decimalValue()));
        }
        return new Items(items, readVersion(body));
    }

    static Retire retire(JsonNode body) {
        rejectUnknownFields(body, Set.of("reason", "version"));
        return new Retire(readText(body, "reason", false), readVersion(body));
    }

    static UUID copySource(JsonNode body) {
        rejectUnknownFields(body, Set.of("sourceQuizId"));
        return parseUuid(body.get("sourceQuizId"), "sourceQuizId");
    }

    static RandomPreview parseRandomPreview(JsonNode body) {
        rejectUnknownFields(body, Set.of("count", "difficulty", "tag"));
        Integer count = readInteger(body, "count");
        if (count == null) {
            throw createInvalidError("Thiếu count");
        }
        return new RandomPreview(count, parseDifficulty(readText(body, "difficulty", false)), readText(body, "tag", false));
    }

    private static Difficulty parseDifficulty(String value) {
        try {
            return value == null ? null : Difficulty.valueOf(value);
        } catch (IllegalArgumentException unknown) {
            throw createInvalidError("difficulty không hợp lệ");
        }
    }

    private static BigDecimal readDecimal(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isNumber()) {
            throw createInvalidError(field + " cần là số");
        }
        return value.decimalValue();
    }

    static void query(MultiValueMap<String, String> params, Set<String> allowed) {
        if (!allowed.containsAll(params.keySet()) || params.values().stream().anyMatch(v -> v.size() != 1)) {
            throw createInvalidError("Chỉ nhận " + String.join(", ", allowed) + ", mỗi trường một giá trị");
        }
        String q = params.getFirst("q");
        if (q != null && q.length() > 200) {
            throw createInvalidError("Từ khóa tối đa 200 ký tự");
        }
    }

    private static void rejectUnknownFields(JsonNode body, Set<String> allowed) {
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

    private static String readText(JsonNode body, String field, boolean required) {
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

    private static boolean readBool(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || !value.isBoolean()) {
            throw createInvalidError(field + " cần là true hoặc false");
        }
        return value.asBoolean();
    }

    private static Integer readInteger(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.canConvertToExactIntegral() || !value.canConvertToInt()) {
            throw createInvalidError(field + " cần là số nguyên");
        }
        return value.asInt();
    }

    private static UUID parseUuid(JsonNode value, String field) {
        if (value == null || !value.isTextual()) {
            throw createInvalidError(field + " cần là mã định danh");
        }
        try {
            return UUID.fromString(value.asText());
        } catch (IllegalArgumentException invalid) {
            throw createInvalidError(field + " cần là mã định danh");
        }
    }

    private static long readVersion(JsonNode body) {
        JsonNode value = body.get("version");
        if (value == null || !value.canConvertToExactIntegral() || value.asLong() < 0) {
            throw createInvalidError("Thiếu version hoặc version không hợp lệ");
        }
        return value.asLong();
    }

    private static ApiException createInvalidError(String message) {
        return ApiException.createBadRequest("QUIZ_REQUEST_INVALID", message);
    }
}
