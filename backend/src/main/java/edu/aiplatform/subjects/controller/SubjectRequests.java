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
    private SubjectRequests() { }

    public static SubjectCommandService.Create create(JsonNode body) {
        fields(body, Set.of("code", "name", "description", "managerId"));
        return new SubjectCommandService.Create(text(body, "code", true), text(body, "name", true),
                text(body, "description", false), uuid(body, "managerId", false));
    }

    public static SubjectCommandService.UpdateInformation information(JsonNode body) {
        fields(body, Set.of("name", "description", "version"));
        require(body, "description");
        return new SubjectCommandService.UpdateInformation(text(body, "name", true), text(body, "description", false), version(body));
    }

    public static SubjectCommandService.ChangeManager manager(JsonNode body) {
        fields(body, Set.of("managerId", "version"));
        return new SubjectCommandService.ChangeManager(uuid(body, "managerId", true), version(body));
    }

    public static SubjectStatusService.ChangeStatus status(JsonNode body) {
        fields(body, Set.of("status", "version"));
        SubjectStatus status;
        try {
            status = SubjectStatus.valueOf(text(body, "status", true));
        } catch (IllegalArgumentException invalid) {
            throw invalid("status", "Trạng thái phải là ACTIVE hoặc ARCHIVED");
        }
        return new SubjectStatusService.ChangeStatus(status, version(body));
    }

    public static void query(MultiValueMap<String, String> params) {
        if (!Set.of("q", "status", "page", "size").containsAll(params.keySet())
                || params.values().stream().anyMatch(values -> values.size() != 1)) {
            throw invalid("query", "Chỉ nhận q, status, page, size, mỗi trường một giá trị");
        }
        String query = params.getFirst("q");
        if (query != null && query.length() > 200) {
            throw invalid("q", "Từ khóa tối đa 200 ký tự");
        }
    }

    private static void fields(JsonNode body, Set<String> allowed) {
        if (body == null || !body.isObject()) {
            throw invalid("body", "Cần một JSON object");
        }
        var names = body.fieldNames();
        while (names.hasNext()) {
            if (!allowed.contains(names.next())) {
                throw invalid("body", "Có trường không được phép trong yêu cầu");
            }
        }
    }

    private static JsonNode require(JsonNode body, String field) {
        if (!body.has(field)) {
            throw invalid(field, "Thiếu trường bắt buộc");
        }
        return body.get(field);
    }

    private static String text(JsonNode body, String field, boolean required) {
        JsonNode value = required ? require(body, field) : body.get(field);
        if (value == null || value.isNull()) {
            if (required) throw invalid(field, "Cần chuỗi ký tự");
            return null;
        }
        if (!value.isTextual()) throw invalid(field, "Cần chuỗi ký tự");
        return value.textValue();
    }

    private static UUID uuid(JsonNode body, String field, boolean required) {
        if (required) require(body, field);
        String value = text(body, field, false);
        if (value == null) return null;
        try {
            UUID parsed = UUID.fromString(value);
            if (!parsed.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
            return parsed;
        } catch (IllegalArgumentException invalid) {
            throw invalid(field, "Cần UUID hợp lệ hoặc null");
        }
    }

    private static long version(JsonNode body) {
        JsonNode value = require(body, "version");
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 1) {
            throw invalid("version", "Cần version là số nguyên từ 1");
        }
        return value.longValue();
    }

    private static InvalidRequest invalid(String field, String message) {
        return new InvalidRequest(field, message);
    }

    public static final class InvalidRequest extends ApiException {
        private final Map<String, String> fieldErrors;

        private InvalidRequest(String field, String message) {
            super(HttpStatus.BAD_REQUEST, "SUBJECT_REQUEST_INVALID", message);
            fieldErrors = Map.of(field, message);
        }

        public Map<String, String> fieldErrors() { return fieldErrors; }
    }
}
