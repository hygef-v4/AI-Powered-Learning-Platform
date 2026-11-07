package edu.aiplatform.identity.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.identity.config.IdentityProperties;
import edu.aiplatform.identity.contract.IMail;
import edu.aiplatform.identity.contract.MailMessage;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Component;

/**
 * Cài {@link IMail} bằng API HTTPS giao dịch của Brevo ({@code MAIL_TRANSPORT=brevo-api}), dùng khi mạng chặn SMTP đi ra
 * ngoài. Địa chỉ API cố định; khóa lấy từ {@code BREVO_API_KEY}, người gửi từ {@code SMTP_FROM}. Timeout giống SMTP. Lỗi
 * ném ra để job gọi thử lại, và được log cảnh báo vì log thử lại của job không ghi lý do. Thông báo lỗi và log chỉ có mã
 * HTTP và mã lỗi của Brevo, không có khóa, người nhận hay nội dung (có thể chứa OTP).
 */
@Component
@ConditionalOnProperty(name = MailTransport.PROPERTY, havingValue = MailTransport.BREVO_API)
public class BrevoApiMailAdapter implements IMail {

    private static final Logger log = LoggerFactory.getLogger(BrevoApiMailAdapter.class);
    static final URI ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern ERROR_CODE = Pattern.compile("[a-z_]{1,40}");

    private final ObjectMapper json;
    private final String from;
    private final String apiKey;
    private final URI endpoint;
    private final HttpClient http;

    @Autowired
    public BrevoApiMailAdapter(ObjectMapper json, IdentityProperties properties,
                               @Value("${platform.identity.brevo-api-key:}") String apiKey) {
        this(json, properties.mailFrom(), apiKey, ENDPOINT);
    }

    /** Chỉ test dùng để trỏ tới HTTP server giả. */
    BrevoApiMailAdapter(ObjectMapper json, String from, String apiKey, URI endpoint) {
        this.json = json;
        this.from = from;
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    @Override
    public void send(MailMessage message) {
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình SMTP_FROM");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình BREVO_API_KEY");
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(REQUEST_TIMEOUT)
                .header("api-key", apiKey)
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body(message), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("Không gọi được API Brevo: {}", e.getClass().getSimpleName());
            throw new MailSendException("Không gọi được API Brevo: " + e.getClass().getSimpleName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MailSendException("Bị ngắt khi gọi API Brevo", e);
        }
        if (response.statusCode() / 100 != 2) {
            String error = "API Brevo trả HTTP " + response.statusCode() + errorCode(response.body());
            log.warn(error);
            throw new MailSendException(error);
        }
    }

    private String body(MailMessage message) {
        InternetAddress sender;
        try {
            sender = new InternetAddress(from, true);
        } catch (AddressException e) {
            throw new IllegalStateException("SMTP_FROM không phải địa chỉ email hợp lệ");
        }
        ObjectNode body = json.createObjectNode();
        ObjectNode senderNode = body.putObject("sender").put("email", sender.getAddress());
        if (sender.getPersonal() != null && !sender.getPersonal().isBlank()) {
            senderNode.put("name", sender.getPersonal());
        }
        body.putArray("to").addObject().put("email", message.to());
        body.put("subject", message.subject());
        body.put("textContent", message.textBody());
        if (message.htmlBody() != null) {
            body.put("htmlContent", message.htmlBody());
        }
        try {
            return json.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không dựng được email", e);
        }
    }

    /** Mã lỗi ngắn của Brevo (ví dụ {@code unauthorized}); bỏ phần message vì có thể nhắc lại dữ liệu gửi đi. */
    private String errorCode(String body) {
        try {
            String code = json.readTree(body == null ? "" : body).path("code").asText("");
            return ERROR_CODE.matcher(code).matches() ? " (" + code + ")" : "";
        } catch (JsonProcessingException e) {
            return "";
        }
    }
}
