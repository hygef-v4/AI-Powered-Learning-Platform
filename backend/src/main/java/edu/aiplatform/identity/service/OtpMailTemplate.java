package edu.aiplatform.identity.service;

import edu.aiplatform.identity.contract.MailMessage;
import edu.aiplatform.identity.model.OtpPurpose;
import java.time.Duration;

/** Email OTP chỉ chứa mã và thời hạn; không có mật khẩu, link đăng nhập hay thông tin tài khoản khác (NFR-U01-32). */
final class OtpMailTemplate {

    private OtpMailTemplate() {
    }

    static MailMessage build(String to, OtpPurpose purpose, String code, Duration ttl) {
        String action = purpose == OtpPurpose.ACTIVATION ? "kích hoạt tài khoản" : "đặt lại mật khẩu";
        String subject = "Mã xác thực " + action + " - Nền tảng học tập";
        long minutes = ttl.toMinutes();
        String text = "Mã xác thực để " + action + " của bạn là: " + code + "\n\n"
                + "Mã có hiệu lực trong " + minutes + " phút và chỉ dùng được một lần.\n"
                + "Nếu bạn không yêu cầu mã này, hãy bỏ qua email.";
        String html = "<p>Mã xác thực để " + action + " của bạn là:</p>"
                + "<p style=\"font-size:24px;font-weight:bold;letter-spacing:6px\">" + code + "</p>"
                + "<p>Mã có hiệu lực trong " + minutes + " phút và chỉ dùng được một lần.</p>"
                + "<p>Nếu bạn không yêu cầu mã này, hãy bỏ qua email.</p>";
        return new MailMessage(to, subject, text, html);
    }
}
