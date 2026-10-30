package edu.aiplatform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Kiểm JSON theo JSON Schema 2020-12: kết quả AI (BR-U06-23) và tham số tool MCP (BR-U06-33). */
@Component
public class JsonSchemaChecker {

    private final ObjectMapper json;
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    public JsonSchemaChecker(ObjectMapper json) {
        this.json = json;
    }

    /** Schema có đọc được không (unit gọi gửi schema sai thì từ chối ngay khi gửi task). */
    public boolean validSchema(String schema) {
        try {
            factory.getSchema(json.readTree(schema));
            return true;
        } catch (RuntimeException | JsonProcessingException e) {
            return false;
        }
    }

    /** Trả lỗi đầu tiên nếu {@code value} không khớp, rỗng nếu khớp. */
    public Optional<String> check(String schema, String value) {
        JsonNode node;
        try {
            node = json.readTree(value);
        } catch (JsonProcessingException e) {
            return Optional.of("không phải JSON");
        }
        return check(schema, node);
    }

    public Optional<String> check(String schema, JsonNode node) {
        try {
            JsonSchema compiled = factory.getSchema(json.readTree(schema));
            Set<ValidationMessage> errors = compiled.validate(node);
            return errors.stream().findFirst().map(ValidationMessage::getMessage);
        } catch (JsonProcessingException e) {
            return Optional.of("schema không đọc được");
        }
    }
}
