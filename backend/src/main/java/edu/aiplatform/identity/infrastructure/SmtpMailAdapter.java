package edu.aiplatform.identity.infrastructure;

import edu.aiplatform.identity.port.*;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U01: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U01: SMTP: Mailpit/Brevo.
 */
@Component
public class SmtpMailAdapter implements MailPort {

    @Override
    public void send(MailMessage message) {
        // Chưa cài: U01.
    }
}
