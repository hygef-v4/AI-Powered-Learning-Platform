package edu.aiplatform.identity.controller;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import edu.aiplatform.shared.error.ApiException;
import java.util.ArrayList;
import java.util.List;

/**
 * Body chỉ nhận đúng các field khai báo (BR-U01-52). Spring Boot tắt {@code FAIL_ON_UNKNOWN_PROPERTIES} toàn cục và
 * {@code @JsonIgnoreProperties(ignoreUnknown = false)} không bật lại được, nên gom field lạ ở đây rồi từ chối.
 */
abstract class StrictRequest {

    private final List<String> unknownFields = new ArrayList<>();

    @JsonAnySetter
    void unknown(String name, Object value) {
        unknownFields.add(name);
    }

    /** Có field không được phép (avatar, email, role, status, accountId...) thì trả 400 trước khi ghi. */
    void rejectUnknownFields() {
        if (!unknownFields.isEmpty()) {
            throw ApiException.badRequest("INVALID_REQUEST",
                    "Dữ liệu gửi lên không hợp lệ hoặc có trường không được phép sửa");
        }
    }
}
