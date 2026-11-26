package edu.aiplatform.ai.contract;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Stable semantic JSON hash: object order and equivalent number representations do not matter. */
public final class McpFingerprint {
    // Ngăn tạo đối tượng cho lớp chỉ cung cấp các hàm tĩnh.
    private McpFingerprint() {
    }

    // Băm JSON đã chuẩn hóa để so nội dung ngay cả khi thứ tự thuộc tính khác nhau.
    public static String of(ObjectMapper json, Object value) {
        try {
            byte[] canonicalJson = canonicalize(json.valueToTree(value)).toString()
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalJson));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // Sắp khóa object đệ quy và giữ nguyên thứ tự mảng để tạo biểu diễn băm ổn định.
    private static JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode out = JsonNodeFactory.instance.objectNode();
            List<String> keys = new ArrayList<>();
            node.fieldNames().forEachRemaining(keys::add);
            Collections.sort(keys);
            keys.forEach(key -> out.set(key, canonicalize(node.get(key))));
            return out;
        }
        if (node.isArray()) {
            ArrayNode out = JsonNodeFactory.instance.arrayNode();
            node.forEach(item -> out.add(canonicalize(item)));
            return out;
        }
        if (node.isNumber()) {
            return DecimalNode.valueOf(node.decimalValue().stripTrailingZeros());
        }
        return node;
    }
}
