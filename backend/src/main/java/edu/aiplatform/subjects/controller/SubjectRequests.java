package edu.aiplatform.subjects.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.service.SubjectCommandService;
import edu.aiplatform.subjects.service.SubjectStatusService;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;

/** Parse JSON riêng U04: không đổi Jackson toàn dự án hoặc âm thầm bỏ field/ép number thành text. */
public final class SubjectRequests {
    private SubjectRequests() {}

    public static SubjectCommandService.Create parseCreate(JsonNode body) {
        requireAllowedFields(body, Set.of("code", "name", "description", "managerId"));
        return new SubjectCommandService.Create(
                readText(body, "code", true),
                readText(body, "name", true),
                readText(body, "description", false),
                readUuid(body, "managerId", false));
    }

    public static SubjectCommandService.UpdateInformation parseUpdateInformation(JsonNode body) {
        requireAllowedFields(body, Set.of("name", "description", "version"));
        requireField(body, "description");
        return new SubjectCommandService.UpdateInformation(
                readText(
                        body,
                        "name",
                        true), readText(body, "description", false), readVersion(body));
    }

    public static SubjectCommandService.ChangeManager parseChangeManager(JsonNode body) {
        requireAllowedFields(body, Set.of("managerId", "version"));
        return new SubjectCommandService.ChangeManager(
                readUuid(body, "managerId", true), readVersion(body));
    }

    public static SubjectStatusService.ChangeStatus parseChangeStatus(JsonNode body) {
        requireAllowedFields(body, Set.of("status", "version"));
        SubjectStatus status;
        try {
            status = SubjectStatus.valueOf(readText(body, "status", true));
        } catch (IllegalArgumentException invalid) {
            throw createInvalidRequest("status", "Trạng thái phải là ACTIVE hoặc ARCHIVED");
        }
        return new SubjectStatusService.ChangeStatus(status, readVersion(body));
    }

    public static void validateListQuery(MultiValueMap<String, String> params) {
        if (!Set.of("q", "status", "page", "size").containsAll(params.keySet())
                || params.values().stream().anyMatch(values -> values.size() != 1)) {
            throw createInvalidRequest(
                    "query", "Chỉ nhận q, status, page, size, mỗi trường một giá trị");
        }
        String query = params.getFirst("q");
        if (query != null && query.length() > 200) {
            throw createInvalidRequest("q", "Từ khóa tối đa 200 ký tự");
        }
    }

    private static void requireAllowedFields(JsonNode body, Set<String> allowed) {
        if (body == null || !body.isObject()) {
            throw createInvalidRequest("body", "Cần một JSON object");
        }
        var names = body.fieldNames();
        while (names.hasNext()) {
            if (!allowed.contains(names.next())) {
                throw createInvalidRequest("body", "Có trường không được phép trong yêu cầu");
            }
        }
    }

    private static JsonNode requireField(JsonNode body, String field) {
        if (!body.has(field)) {
            throw createInvalidRequest(field, "Thiếu trường bắt buộc");
        }
        return body.get(field);
    }

    private static String readText(JsonNode body, String field, boolean required) {
        JsonNode value = required ? requireField(body, field) : body.get(field);
        if (value == null || value.isNull()) {
            if (required) throw createInvalidRequest(field, "Cần chuỗi ký tự");
            return null;
        }
        if (!value.isTextual()) throw createInvalidRequest(field, "Cần chuỗi ký tự");
        return value.textValue();
    }

    private static UUID readUuid(JsonNode body, String field, boolean required) {
        if (required) requireField(body, field);
        String value = readText(body, field, false);
        if (value == null) return null;
        try {
            UUID parsed = UUID.fromString(value);
            if (!parsed.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
            return parsed;
        } catch (IllegalArgumentException invalid) {
            throw createInvalidRequest(field, "Cần UUID hợp lệ hoặc null");
        }
    }

    private static long readVersion(JsonNode body) {
        JsonNode value = requireField(body, "version");
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 1) {
            throw createInvalidRequest("version", "Cần version là số nguyên từ 1");
        }
        return value.longValue();
    }

    private static InvalidRequest createInvalidRequest(String field, String message) {
        return new InvalidRequest(field, message);
    }

    public static final class InvalidRequest extends ApiException {
        private final Map<String, String> fieldErrors;

        private InvalidRequest(String field, String message) {
            super(HttpStatus.BAD_REQUEST, "SUBJECT_REQUEST_INVALID", message);
            fieldErrors = Map.of(field, message);
        }

        public Map<String, String> fieldErrors() {
            return fieldErrors;
        }
    }
}
