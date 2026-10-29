package edu.aiplatform.ai.contract;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Stable semantic JSON hash: object order and equivalent number representations do not matter. */
public final class McpFingerprint {
    private McpFingerprint() {}
    public static String of(ObjectMapper json, Object value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical(json.valueToTree(value)).toString().getBytes(StandardCharsets.UTF_8)));
        } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static JsonNode canonical(JsonNode node) {
        if(node.isObject()) {
            ObjectNode out=JsonNodeFactory.instance.objectNode();
            List<String> keys=new ArrayList<>();node.fieldNames().forEachRemaining(keys::add);Collections.sort(keys);
            keys.forEach(key -> out.set(key,canonical(node.get(key))));return out;
        }
        if(node.isArray()) {ArrayNode out=JsonNodeFactory.instance.arrayNode();node.forEach(n -> out.add(canonical(n)));return out;}
        if(node.isNumber())return DecimalNode.valueOf(node.decimalValue().stripTrailingZeros());
        return node;
    }
}
