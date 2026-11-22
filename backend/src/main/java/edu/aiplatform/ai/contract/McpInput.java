package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.*;

/** Shared bounded MCP input parsing; resource authorization remains with each domain. */
public final class McpInput {
    private static final int MAX_TEXT_LENGTH = 10000;
    private static final String UUID_PATTERN =
            "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}";

    private final Map<String, Object> values;

    public McpInput(Map<String, Object> values) {
        this.values = values == null ? Map.of() : values;
    }

    public Object readValue(String key) {
        return values.get(key);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public String readText(String key, String fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof String text) || text.length() > MAX_TEXT_LENGTH) {
            throw createInvalidArguments();
        }
        return text;
    }

    public UUID readUuid(String key) {
        try {
            String text = readText(key, null);
            if (text == null || !text.matches(UUID_PATTERN)) {
                throw createInvalidArguments();
            }
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            throw createInvalidArguments();
        }
    }

    public int readInteger(String key, int fallback, int min, int max) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof Number)) {
            throw createInvalidArguments();
        }
        try {
            int number = new BigDecimal(value.toString()).intValueExact();
            if (number < min || number > max) {
                throw createInvalidArguments();
            }
            return number;
        } catch (ArithmeticException | NumberFormatException e) {
            throw createInvalidArguments();
        }
    }

    public long readVersion(String key) {
        Object value = values.get(key);
        if (!(value instanceof Number)) {
            throw createInvalidArguments();
        }
        try {
            long number = new BigDecimal(value.toString()).longValueExact();
            if (number < 0) {
                throw createInvalidArguments();
            }
            return number;
        } catch (ArithmeticException | NumberFormatException e) {
            throw createInvalidArguments();
        }
    }

    public <E extends Enum<E>> E readChoice(String key, Class<E> type, E fallback) {
        String value = readText(key, null);
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw createInvalidArguments();
        }
    }

    public int readPage() {
        return readInteger("page", 0, 0, 1_000_000);
    }

    public int readSize() {
        return readInteger("size", 20, 1, 100);
    }

    public static <T> Map<String, Object> paginateItems(List<T> items, int page, int size) {
        long start = (long) page * size;
        int from = (int) Math.min(start, items.size());
        int to = Math.min(from + size, items.size());
        return Map.of("items", items.subList(from, to), "total", items.size(),
                "page", page, "size", size);
    }

    public static void requireTeaching(Actor actor) {
        if (actor == null
                || (actor.role() != Role.TEACHER && actor.role() != Role.SUBJECT_MANAGER)) {
            throw ApiException.createNotFound("MCP_TOOL_NOT_FOUND");
        }
    }

    public static void requireAdmin(Actor actor) {
        if (actor == null || actor.role() != Role.ADMIN) {
            throw ApiException.createNotFound("MCP_TOOL_NOT_FOUND");
        }
    }

    public static ApiException createInvalidArguments() {
        return ApiException.createBadRequest("INVALID_MCP_ARGUMENTS", "Tham số MCP không hợp lệ");
    }
}
