package edu.aiplatform.quizzes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.quizzes.service.QuizAiDraftService.DraftRequest;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.QuestionContent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Đọc JSON của API đề xuất AI: field lạ, sai kiểu → {@code 400 QUESTION_REQUEST_INVALID}. */
final class AiDraftRequests {

    private static final Set<String> REQUEST_FIELDS = Set.of("instructions", "count", "type", "difficulty",
            "materialIds", "source", "provider", "requestRef");

    private AiDraftRequests() {
    }

    record QuizDraft(DraftRequest request, long version) {
    }

    record Accept(List<QuestionContent> questions, long version) {
    }

    /** Yêu cầu AI cho ngân hàng (không có {@code version}). */
    static DraftRequest parseBankRequest(JsonNode body) {
        QuestionRequests.rejectUnknownFields(body, REQUEST_FIELDS);
        return request(body);
    }

    /** Yêu cầu AI cho Quiz, kèm {@code version} của Quiz. */
    static QuizDraft parseQuizDraft(JsonNode body) {
        Set<String> allowed = new HashSet<>(REQUEST_FIELDS);
        allowed.add("version");
        QuestionRequests.rejectUnknownFields(body, allowed);
        return new QuizDraft(request(body), QuestionRequests.readVersion(body));
    }

    /** Câu được chấp nhận (dạng nội dung câu như API ngân hàng); {@code withVersion} cho đích Quiz. */
    static Accept accept(JsonNode body, boolean withVersion) {
        QuestionRequests.rejectUnknownFields(body, withVersion ? Set.of("questions", "version") : Set.of("questions"));
        List<QuestionContent> questions = new ArrayList<>();
        for (JsonNode node : QuestionRequests.readArray(body, "questions")) {
            QuestionRequests.rejectUnknownFields(node, Set.of("type", "stem", "options", "correctOptionIds", "explanation",
                    "defaultPoints", "difficulty", "tags"));
            questions.add(QuestionRequests.parseContent(node));
        }
        return new Accept(questions, withVersion ? QuestionRequests.readVersion(body) : 0);
    }

    private static DraftRequest request(JsonNode body) {
        JsonNode count = body.get("count");
        if (count == null || !count.canConvertToInt()) {
            throw QuestionRequests.createInvalidError("count cần là số nguyên");
        }
        List<UUID> materials = new ArrayList<>();
        if (body.hasNonNull("materialIds")) {
            for (JsonNode node : QuestionRequests.readArray(body, "materialIds")) {
                if (!node.isTextual()) {
                    throw QuestionRequests.createInvalidError("materialIds cần danh sách id");
                }
                try {
                    materials.add(UUID.fromString(node.asText()));
                } catch (IllegalArgumentException malformed) {
                    throw QuestionRequests.createInvalidError("materialIds có id không hợp lệ");
                }
            }
        }
        return new DraftRequest(QuestionRequests.readText(body, "instructions", false), count.asInt(),
                QuestionRequests.parseEnumValue(body, "type", QuestionType.class, false),
                QuestionRequests.parseEnumValue(body, "difficulty", Difficulty.class, false), materials,
                QuestionRequests.parseEnumValue(body, "source", AiSource.class, false),
                QuestionRequests.parseEnumValue(body, "provider", AiProvider.class, false),
                QuestionRequests.readText(body, "requestRef", true));
    }
}
