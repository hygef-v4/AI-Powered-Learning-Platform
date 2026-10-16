package edu.aiplatform.files.contract;

/** Chuỗi ký HMAC gồm {@code fileId}, người tải, {@code purpose}, hạn 1 giờ; frontend gửi lại cho unit sở hữu. */
public record FileRef(String value) {
}
