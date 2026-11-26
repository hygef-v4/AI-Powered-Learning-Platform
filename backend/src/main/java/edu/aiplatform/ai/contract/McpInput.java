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

    // Biểu diễn yêu cầu thiếu tham số bằng map rỗng trước khi đọc từng trường.
    public McpInput(Map<String, Object> values) {
        this.values = values == null ? Map.of() : values;
    }

    // Lấy giá trị thô khi kiểu dữ liệu cần được provider kiểm tra riêng.
    public Object readValue(String key) {
        return values.get(key);
    }

    // Kiểm trường đã xuất hiện trong map, kể cả khi giá trị được gửi là null.
    public boolean has(String key) {
        return values.containsKey(key);
    }

    // Dùng mặc định nếu thiếu và từ chối kiểu khác chuỗi hoặc văn bản quá dài.
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

    // Chỉ chấp nhận chuỗi UUID đúng định dạng tham chiếu của tool.
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

    // Đọc số nguyên trong khoảng caller cho phép, dùng mặc định nếu thiếu.
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

    // Yêu cầu số nguyên không âm để đối chiếu version của dữ liệu.
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

    // Đổi giá trị chuỗi sang enum của provider hoặc dùng lựa chọn mặc định khi thiếu.
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

    // Đọc chỉ số trang bắt đầu từ 0 và chặn giá trị ngoài giới hạn.
    public int readPage() {
        return readInteger("page", 0, 0, 1_000_000);
    }

    // Giới hạn một trang ở 100 phần tử, mặc định 20.
    public int readSize() {
        return readInteger("size", 20, 1, 100);
    }

    // Cắt danh sách đã lọc và giữ số lượng tổng để caller biết còn trang hay không.
    public static <T> Map<String, Object> paginateItems(List<T> items, int page, int size) {
        long start = (long) page * size;
        int from = (int) Math.min(start, items.size());
        int to = Math.min(from + size, items.size());
        return Map.of("items", items.subList(from, to), "total", items.size(),
                "page", page, "size", size);
    }

    // Chỉ nhận vai trò giảng viên hoặc quản lý môn ở các tool nghiệp vụ dành cho staff.
    public static void requireTeaching(Actor actor) {
        if (actor == null
                || (actor.role() != Role.TEACHER && actor.role() != Role.SUBJECT_MANAGER)) {
            throw ApiException.createNotFound("MCP_TOOL_NOT_FOUND");
        }
    }

    // Tạo lỗi 400 dùng chung để provider trả cùng mã khi tham số không hợp lệ.
    public static ApiException createInvalidArguments() {
        return ApiException.createBadRequest("INVALID_MCP_ARGUMENTS", "Tham số MCP không hợp lệ");
    }
}
