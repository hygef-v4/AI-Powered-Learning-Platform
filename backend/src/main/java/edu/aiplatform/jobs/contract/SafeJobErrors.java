package edu.aiplatform.jobs.contract;

import java.util.Set;
import java.util.concurrent.TimeoutException;

/** Only application-owned, fixed diagnostics may cross the worker persistence/logging boundary. */
public final class SafeJobErrors {
    public static final String UNKNOWN = "Xử lý việc nền thất bại";
    private static final String EXHAUSTED = "Hết lượt thử lại: ";
    private static final Set<String> ALLOWED = Set.of(
            UNKNOWN,
            "Quá thời gian xử lý",
            "Không đủ dung lượng tạm để dump database",
            "Chưa cấu hình U03_BACKUP_PUBLIC_KEY",
            "U03_BACKUP_PUBLIC_KEY không hợp lệ",
            "Không chạy được pg_dump trong worker",
            "pg_dump quá thời gian",
            "pg_dump bị ngắt",
            "Bản tải lên không khớp kích thước hoặc checksum",
            "Chưa ghi được kết quả backup",
            "Lưu trữ hoặc tệp tạm tạm thời lỗi");

    private SafeJobErrors() { }

    public static String from(Throwable error) {
        return error instanceof TimeoutException ? "Quá thời gian xử lý" : sanitize(error.getMessage());
    }

    public static String sanitize(String error) {
        if (error == null) return UNKNOWN;
        if (ALLOWED.contains(error)) return error;
        if (error.startsWith(EXHAUSTED) && ALLOWED.contains(error.substring(EXHAUSTED.length()))) return error;
        return UNKNOWN;
    }
}
