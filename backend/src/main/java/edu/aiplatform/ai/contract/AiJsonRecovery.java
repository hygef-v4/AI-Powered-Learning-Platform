package edu.aiplatform.ai.contract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Khôi phục JSON khi mô hình hoặc agent gửi đúng nội dung nhưng sai lớp bọc (yêu cầu 2026-11-27): khối {@code ```json},
 * chữ dẫn trước hoặc sau JSON, cả JSON bị mã hóa thành một chuỗi văn bản ở gốc, hay một trường object/array bị gửi dạng
 * chuỗi. Chỉ đổi kiểu khi schema chờ object/array và chuỗi đọc ra đúng kiểu đó; nội dung không phải JSON vẫn để bước
 * kiểm schema từ chối như trước.
 */
public final class AiJsonRecovery {

    private static final int MAX_UNWRAP_DEPTH = 3;
    private static final String FENCE = "```";

    private AiJsonRecovery() {
    }

    /**
     * Chuỗi JSON dùng để kiểm schema và lưu kết quả. Kết quả đọc được trực tiếp và không cần ép kiểu thì trả nguyên văn
     * (giữ số thập phân như mô hình viết); không khôi phục được thì cũng trả nguyên văn.
     */
    public static String recoverOutputText(ObjectMapper json, String output, String schema) {
        JsonNode schemaNode = readJson(json, schema);
        if (output == null || schemaNode == null) {
            return output;
        }
        JsonNode direct = readJson(json, output);
        JsonNode recovered = recoverOutput(json, output, schemaNode);
        if (recovered == null || recovered.equals(direct)) {
            return output;
        }
        return recovered.toString();
    }

    /** Đọc kết quả AI thành JSON rồi ép các trường theo schema; {@code null} nếu không thấy JSON nào. */
    public static JsonNode recoverOutput(ObjectMapper json, String output, JsonNode schema) {
        if (output == null || output.isBlank()) {
            return null;
        }
        String text = stripFence(output.strip());
        JsonNode node = readJson(json, text);
        if (node == null) {
            node = readEnclosedJson(json, text);
        }
        if (node == null) {
            return null;
        }
        for (int depth = 0; depth < MAX_UNWRAP_DEPTH && node.isTextual(); depth++) {
            JsonNode inner = readJson(json, stripFence(node.asText().strip()));
            if (inner == null || !(inner.isObject() || inner.isArray() || inner.isTextual())) {
                break;
            }
            node = inner;
        }
        return schema == null ? node : coerceToSchema(json, node, schema);
    }

    /** Bản sao của {@code value} trong đó các chuỗi chứa JSON được đổi sang object/array theo schema. */
    public static JsonNode coerceToSchema(ObjectMapper json, JsonNode value, JsonNode schema) {
        if (value == null || schema == null || !schema.isObject()) {
            return value;
        }
        return coerceNode(json, value.deepCopy(), schema);
    }

    private static JsonNode coerceNode(ObjectMapper json, JsonNode value, JsonNode schema) {
        if (schema == null || !schema.isObject()) {
            return value;
        }
        Set<String> types = listSchemaTypes(schema);
        JsonNode current = value;
        if (current.isTextual() && !types.contains("string")
                && (types.contains("object") || types.contains("array"))) {
            JsonNode parsed = readJson(json, stripFence(current.asText().strip()));
            if (parsed != null && (parsed.isObject() && types.contains("object")
                    || parsed.isArray() && types.contains("array"))) {
                current = parsed;
            }
        }
        if (current instanceof ObjectNode object) {
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode fieldSchema = findPropertySchema(schema, field.getKey());
                if (fieldSchema != null) {
                    field.setValue(coerceNode(json, field.getValue(), fieldSchema));
                }
            }
        } else if (current instanceof ArrayNode array) {
            JsonNode itemSchema = findItemSchema(schema);
            for (int index = 0; itemSchema != null && index < array.size(); index++) {
                array.set(index, coerceNode(json, array.get(index), itemSchema));
            }
        }
        return current;
    }

    /** Kiểu khai báo ở {@code type} (chuỗi hoặc mảng) và ở các nhánh {@code anyOf}/{@code oneOf}/{@code allOf}. */
    private static Set<String> listSchemaTypes(JsonNode schema) {
        Set<String> types = new HashSet<>();
        JsonNode type = schema.path("type");
        if (type.isTextual()) {
            types.add(type.asText());
        } else if (type.isArray()) {
            type.forEach(item -> types.add(item.asText()));
        }
        if (schema.has("properties") || schema.has("additionalProperties")) {
            types.add("object");
        }
        if (schema.has("items")) {
            types.add("array");
        }
        for (String combinator : new String[] {"anyOf", "oneOf", "allOf"}) {
            schema.path(combinator).forEach(branch -> types.addAll(listSchemaTypes(branch)));
        }
        return types;
    }

    private static JsonNode findPropertySchema(JsonNode schema, String name) {
        JsonNode property = schema.path("properties").get(name);
        if (property != null) {
            return property;
        }
        for (String combinator : new String[] {"anyOf", "oneOf", "allOf"}) {
            for (JsonNode branch : schema.path(combinator)) {
                JsonNode found = findPropertySchema(branch, name);
                if (found != null) {
                    return found;
                }
            }
        }
        JsonNode additional = schema.get("additionalProperties");
        return additional != null && additional.isObject() ? additional : null;
    }

    private static JsonNode findItemSchema(JsonNode schema) {
        JsonNode items = schema.get("items");
        if (items != null && items.isObject()) {
            return items;
        }
        for (String combinator : new String[] {"anyOf", "oneOf", "allOf"}) {
            for (JsonNode branch : schema.path(combinator)) {
                JsonNode found = findItemSchema(branch);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Bỏ cặp {@code ```} (kèm nhãn ngôn ngữ như {@code json}) bao quanh toàn bộ nội dung. */
    private static String stripFence(String text) {
        if (!text.startsWith(FENCE) || !text.endsWith(FENCE) || text.length() < 2 * FENCE.length()) {
            return text;
        }
        int bodyStart = text.indexOf('\n');
        int bodyEnd = text.length() - FENCE.length();
        if (bodyStart < 0 || bodyStart >= bodyEnd) {
            return text;
        }
        return text.substring(bodyStart + 1, bodyEnd).strip();
    }

    /**
     * JSON nằm giữa chữ dẫn: lấy từ dấu mở đầu tiên tới dấu đóng tương ứng cuối cùng. Thử loại dấu mở xuất hiện trước,
     * để {@code Kết quả: [{"a":1}]} cho cả mảng chứ không phải object bên trong.
     */
    private static JsonNode readEnclosedJson(ObjectMapper json, String text) {
        int objectStart = text.indexOf('{');
        int arrayStart = text.indexOf('[');
        boolean arrayFirst = arrayStart >= 0 && (objectStart < 0 || arrayStart < objectStart);
        char[][] pairs = arrayFirst ? new char[][] {{'[', ']'}, {'{', '}'}} : new char[][] {{'{', '}'}, {'[', ']'}};
        for (char[] pair : pairs) {
            int start = text.indexOf(pair[0]);
            int end = text.lastIndexOf(pair[1]);
            if (start >= 0 && end > start) {
                JsonNode node = readJson(json, text.substring(start, end + 1));
                if (node != null && (node.isObject() || node.isArray())) {
                    return node;
                }
            }
        }
        return null;
    }

    private static JsonNode readJson(ObjectMapper json, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            JsonNode node = json.reader().with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(text);
            return node == null || node.isMissingNode() ? null : node;
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
