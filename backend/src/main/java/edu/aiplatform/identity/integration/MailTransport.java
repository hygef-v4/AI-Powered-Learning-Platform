package edu.aiplatform.identity.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Chọn cách chuyển email của {@link edu.aiplatform.identity.contract.IMail} theo {@code MAIL_TRANSPORT}: {@code smtp}
 * (mặc định) hoặc {@code brevo-api} khi nhà cung cấp VPS chặn SMTP đi ra ngoài. Giá trị khác làm backend không khởi động,
 * tránh rơi về stub bỏ qua email mà không ai biết.
 */
@Component
public class MailTransport {

    public static final String PROPERTY = "platform.identity.mail-transport";
    public static final String SMTP = "smtp";
    public static final String BREVO_API = "brevo-api";

    public MailTransport(@Value("${" + PROPERTY + ":" + SMTP + "}") String transport) {
        if (!SMTP.equalsIgnoreCase(transport) && !BREVO_API.equalsIgnoreCase(transport)) {
            throw new IllegalStateException("MAIL_TRANSPORT phải là " + SMTP + " hoặc " + BREVO_API);
        }
    }
}
