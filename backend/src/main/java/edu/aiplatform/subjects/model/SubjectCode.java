package edu.aiplatform.subjects.model;

import edu.aiplatform.shared.error.ApiException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Mã môn theo BR-U04-02/Q09: chuẩn hóa một lần, không có thao tác đổi mã. */
public record SubjectCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9-]{1,20}");

    public SubjectCode {
        value = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw ApiException.badRequest("SUBJECT_CODE_INVALID",
                    "Mã môn phải gồm 1–20 ký tự chữ hoa, số hoặc dấu -");
        }
    }
}
