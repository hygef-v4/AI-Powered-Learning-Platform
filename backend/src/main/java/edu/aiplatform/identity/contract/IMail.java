package edu.aiplatform.identity.contract;

/** Gửi email qua SMTP (Brevo ở demo và production, Mailpit ở local); dùng bởi U01 (OTP) và U14. */
public interface IMail {

    void send(MailMessage message);
}
