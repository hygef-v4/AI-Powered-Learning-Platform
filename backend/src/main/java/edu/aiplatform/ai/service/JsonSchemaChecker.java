package edu.aiplatform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Kiểm JSON theo JSON Schema 2020-12: kết quả AI (BR-U06-23) và tham số tool MCP (BR-U06-33). */
@Component
public class JsonSchemaChecker {

    private final ObjectMapper json;
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    // Only tool schemas: they are fixed at startup. AI output schemas vary per task and would grow this map forever.
    private final Map<String, JsonSchema> toolSchemas = new ConcurrentHashMap<>();

    public JsonSchemaChecker(ObjectMapper json) {
        this.json = json;
    }

    /** Schema có đọc được không (unit gọi gửi schema sai thì từ chối ngay khi gửi task). */
    public boolean isValidSchema(String schema) {
        try {
            factory.getSchema(json.readTree(schema));
            return true;
        } catch (RuntimeException | JsonProcessingException e) {
            return false;
        }
    }

    /** Trả lỗi đầu tiên nếu {@code value} không khớp, rỗng nếu khớp. */
    public Optional<String> validateValue(String schema, String value) {
        JsonNode node;
        try {
            node = json.readTree(value);
        } catch (JsonProcessingException e) {
            return Optional.of("không phải JSON");
        }
        return validateValue(schema, node);
    }

    public Optional<String> validateValue(String schema, JsonNode node) {
        try {
            return validateCompiled(compileSchema(schema), node);
        } catch (UncheckedIOException e) {
            return Optional.of("schema không đọc được");
        }
    }

    /** Như {@link #validateValue(String, JsonNode)} nhưng giữ schema đã biên dịch; chỉ dùng cho schema cố định của tool MCP. */
    public Optional<String> validateToolArguments(String toolSchema, JsonNode arguments) {
        try {
            return validateCompiled(toolSchemas.computeIfAbsent(toolSchema, this::compileSchema), arguments);
        } catch (UncheckedIOException e) {
            return Optional.of("schema không đọc được");
        }
    }

    private static Optional<String> validateCompiled(JsonSchema compiled, JsonNode node) {
        Set<ValidationMessage> errors = compiled.validate(node);
        return errors.stream().findFirst().map(ValidationMessage::getMessage);
    }

    private JsonSchema compileSchema(String schema) {
        try {
            return factory.getSchema(json.readTree(schema));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
