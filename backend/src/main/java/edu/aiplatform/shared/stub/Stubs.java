package edu.aiplatform.shared.stub;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tiện ích cho bản tạm của interface: bản tạm chỉ chạy khi unit sở hữu chưa cài thật, trả dữ liệu mẫu
 * (danh sách rỗng, quyền {@code true}, đối tượng mẫu theo ID) để unit khác code và chạy tiếp.
 */
public final class Stubs {

    private static final Logger log = LoggerFactory.getLogger("contract-stub");

    private Stubs() {
    }

    /** Ghi cảnh báo lúc khởi động rằng interface đang dùng bản tạm, rồi trả bản tạm. */
    public static <T> T announce(Class<T> type, T stub) {
        log.warn("{} đang dùng bản tạm: unit sở hữu chưa cài, dữ liệu trả về chỉ là dữ liệu mẫu", type.getSimpleName());
        return stub;
    }

    /** Ghi một lời gọi tới bản tạm không làm gì (gửi mail, ghi audit, gửi việc nền...). */
    public static void called(Class<?> type, String what) {
        log.info("{} (bản tạm): {}", type.getSimpleName(), what);
    }

    /** ID mẫu cố định suy từ ID khác, để cùng một đầu vào luôn trả cùng kết quả. */
    public static UUID derivedId(String kind, UUID source) {
        return UUID.nameUUIDFromBytes((kind + ":" + source).getBytes(StandardCharsets.UTF_8));
    }
}
