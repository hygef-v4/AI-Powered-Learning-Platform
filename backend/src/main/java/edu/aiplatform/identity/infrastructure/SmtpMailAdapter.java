package edu.aiplatform.identity.infrastructure;

import edu.aiplatform.identity.application.IdentityProperties;
import edu.aiplatform.identity.contract.IMail;
import edu.aiplatform.identity.contract.MailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Cài {@link IMail} bằng SMTP (NFR-U01-30): Mailpit ở local/test, Brevo SMTP ở demo/production. Timeout kết nối và gửi
 * cấu hình ở {@code spring.mail.properties}. Không log nội dung (có thể chứa OTP).
 */
@Component
public class SmtpMailAdapter implements IMail {

    private final JavaMailSender sender;
    private final String from;

    public SmtpMailAdapter(JavaMailSender sender, IdentityProperties properties) {
        this.sender = sender;
        this.from = properties.mailFrom();
    }

    @Override
    public void send(MailMessage message) {
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình SMTP_FROM");
        }
        try {
            MimeMessage mime = sender.createMimeMessage();
            boolean multipart = message.htmlBody() != null;
            MimeMessageHelper helper = new MimeMessageHelper(mime, multipart, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            if (multipart) {
                helper.setText(message.textBody(), message.htmlBody());
            } else {
                helper.setText(message.textBody());
            }
            sender.send(mime);
        } catch (MessagingException e) {
            throw new MailSendException("Không dựng được email", e);
        }
    }
}
