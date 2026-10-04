package edu.aiplatform.identity.port;

/** SMTP: Mailpit khi local/test, Brevo khi demo/production; U01 (OTP) và U16 (thông báo) dùng. */
public interface MailPort {
    /** Ném {@link edu.aiplatform.jobs.port.RetryableJobException} khi SMTP lỗi tạm. */
    void send(MailMessage message);
}
