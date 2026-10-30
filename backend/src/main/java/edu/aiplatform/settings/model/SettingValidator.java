package edu.aiplatform.settings.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.shared.error.ApiException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Kiểm giá trị theo khai báo (BR-U03-82): kiểu, khoảng, tập cho phép, ràng buộc giữa các mục. Giá trị rỗng chỉ
 * hợp lệ với mục không có mặc định. Trả giá trị đã chuẩn hóa (tập chọn nhiều theo thứ tự khai báo, bỏ trùng).
 */
public final class SettingValidator {

    /** Ràng buộc: mục kiểu BOOLEAN chỉ bật được khi mục có khóa này đã có giá trị (BR-U03-88). */
    public static final String REQUIRES_WHEN_TRUE = "requiresWhenTrue";
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String OPTIONS = "options";
    public static final String NON_EMPTY = "nonEmpty";
    /** Chỉ để hiển thị (ví dụ "MB"), không dùng khi kiểm. */
    public static final String UNIT = "unit";

    private static final Pattern TIME_OF_DAY = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");

    private SettingValidator() {
    }

    public static JsonNode validate(SettingDefinition definition, JsonNode value) {
        if (value == null || value.isNull()) {
            if (definition.defaultValue() != null) {
                throw invalid("SETTING_REQUIRED", "Mục này phải có giá trị");
            }
            return NullNode.getInstance();
        }
        Map<String, Object> constraints = definition.constraints() == null ? Map.of() : definition.constraints();
        return switch (definition.type()) {
            case INTEGER -> integer(value, constraints);
            case BOOLEAN -> {
                if (!value.isBoolean()) {
                    throw invalid("SETTING_TYPE", "Giá trị phải là bật hoặc tắt");
                }
                yield value;
            }
            case TIME_OF_DAY -> {
                if (!value.isTextual() || !TIME_OF_DAY.matcher(value.asText()).matches()) {
                    throw invalid("SETTING_TYPE", "Giờ phải có dạng giờ:phút, ví dụ 02:30");
                }
                yield value;
            }
            case ENUM -> {
                if (!value.isTextual() || !options(constraints).contains(value.asText())) {
                    throw invalid("SETTING_OPTION", "Giá trị không nằm trong danh sách cho phép");
                }
                yield value;
            }
            case ENUM_SET -> enumSet(value, constraints);
        };
    }

    /**
     * Kiểm ràng buộc giữa các mục khi đổi {@code definition} thành {@code value}, với giá trị hiện hành của các mục
     * khác trong {@code current}.
     */
    public static void validateCross(SettingDefinition definition, JsonNode value, Collection<SettingDefinition> all,
                                     Map<String, JsonNode> current) {
        Object required = definition.constraints() == null ? null : definition.constraints().get(REQUIRES_WHEN_TRUE);
        if (required != null && value != null && value.isBoolean() && value.asBoolean()) {
            JsonNode other = current.get(required.toString());
            if (other == null || other.isNull()) {
                throw invalid("SETTING_DEPENDENCY", "Cần đặt " + required + " trước khi bật mục này");
            }
        }
        if (value == null || value.isNull()) {
            for (SettingDefinition other : all) {
                Object dependsOn = other.constraints() == null ? null : other.constraints().get(REQUIRES_WHEN_TRUE);
                JsonNode otherValue = current.get(other.key());
                if (definition.key().equals(dependsOn) && otherValue != null && otherValue.asBoolean(false)) {
                    throw invalid("SETTING_DEPENDENCY", "Đang bật " + other.key() + " nên không bỏ trống được mục này");
                }
            }
        }
    }

    private static JsonNode integer(JsonNode value, Map<String, Object> constraints) {
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw invalid("SETTING_TYPE", "Giá trị phải là số nguyên");
        }
        int number = value.asInt();
        Object min = constraints.get(MIN);
        Object max = constraints.get(MAX);
        if ((min != null && number < ((Number) min).intValue()) || (max != null && number > ((Number) max).intValue())) {
            throw invalid("SETTING_RANGE", "Giá trị phải từ " + min + " đến " + max);
        }
        return value;
    }

    private static JsonNode enumSet(JsonNode value, Map<String, Object> constraints) {
        if (!value.isArray()) {
            throw invalid("SETTING_TYPE", "Giá trị phải là danh sách");
        }
        List<String> options = options(constraints);
        Set<String> chosen = new LinkedHashSet<>();
        for (JsonNode item : value) {
            if (!item.isTextual() || !options.contains(item.asText())) {
                throw invalid("SETTING_OPTION", "Có giá trị không nằm trong danh sách cho phép");
            }
            chosen.add(item.asText());
        }
        if (chosen.isEmpty() && Boolean.TRUE.equals(constraints.get(NON_EMPTY))) {
            throw invalid("SETTING_REQUIRED", "Phải chọn ít nhất một giá trị");
        }
        ArrayNode normalized = JsonNodeFactory.instance.arrayNode();
        options.stream().filter(chosen::contains).forEach(normalized::add);
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private static List<String> options(Map<String, Object> constraints) {
        Object options = constraints.get(OPTIONS);
        return options instanceof List<?> list ? (List<String>) list : new ArrayList<>();
    }

    private static ApiException invalid(String code, String message) {
        return ApiException.badRequest(code, message);
    }
}
