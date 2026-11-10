package edu.aiplatform.identity.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpServer;
import edu.aiplatform.identity.contract.MailMessage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

/** Adapter gọi HTTP server giả của JDK thay cho API Brevo thật. */
class BrevoApiMailAdapterTest {

    private static final String KEY = "xkeysib-test";
    private final JsonMapper json = JsonMapper.builder().build();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastKey = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastMethod = new AtomicReference<>();
    private HttpServer server;
    private URI endpoint;
    private volatile int status = 201;
    private volatile String reply = "{\"messageId\":\"<1@smtp-relay.mailin.fr>\"}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastKey.set(exchange.getRequestHeaders().getFirst("api-key"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v3/smtp/email");
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private BrevoApiMailAdapter adapter(String from, String key) {
        return new BrevoApiMailAdapter(json, from, key, endpoint);
    }

    @Test
    void postsTransactionalEmailWithApiKeyAndNamedSender() throws IOException {
        adapter("Nền tảng học tập <no-reply@alapedu.online>", KEY)
                .send(new MailMessage("sv@gmail.com", "Mã OTP", "Mã của bạn: 012345", "<p>012345</p>"));

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/v3/smtp/email");
        assertThat(lastKey.get()).isEqualTo(KEY);
        JsonNode body = json.readTree(lastBody.get());
        assertThat(body.path("sender").path("email").asText()).isEqualTo("no-reply@alapedu.online");
        assertThat(body.path("sender").path("name").asText()).isEqualTo("Nền tảng học tập");
        assertThat(body.path("to")).hasSize(1);
        assertThat(body.path("to").path(0).path("email").asText()).isEqualTo("sv@gmail.com");
        assertThat(body.path("subject").asText()).isEqualTo("Mã OTP");
        assertThat(body.path("textContent").asText()).isEqualTo("Mã của bạn: 012345");
        assertThat(body.path("htmlContent").asText()).isEqualTo("<p>012345</p>");
    }

    @Test
    void textOnlyEmailOmitsHtmlAndSenderName() throws IOException {
        adapter("no-reply@alapedu.online", KEY).send(new MailMessage("gv@gmail.com", "Thông báo", "Nội dung", null));

        JsonNode body = json.readTree(lastBody.get());
        assertThat(body.has("htmlContent")).isFalse();
        assertThat(body.path("sender").has("name")).isFalse();
        assertThat(body.path("sender").path("email").asText()).isEqualTo("no-reply@alapedu.online");
    }

    @Test
    void rejectedRequestReportsStatusAndCodeButNotKeyOrContent() {
        status = 401;
        reply = "{\"code\":\"unauthorized\",\"message\":\"Key not found: xkeysib-test for sv@gmail.com\"}";

        assertThatThrownBy(() -> adapter("no-reply@alapedu.online", KEY)
                .send(new MailMessage("sv@gmail.com", "Mã OTP", "012345", null)))
                .isInstanceOf(MailSendException.class)
                .hasMessage("API Brevo trả HTTP 401 (unauthorized)");
    }

    @Test
    void rateLimitWithoutJsonBodyStillFails() {
        status = 429;
        reply = "too many requests";

        assertThatThrownBy(() -> adapter("no-reply@alapedu.online", KEY)
                .send(new MailMessage("sv@gmail.com", "Mã OTP", "012345", null)))
                .isInstanceOf(MailSendException.class)
                .hasMessage("API Brevo trả HTTP 429");
    }

    @Test
    void unreachableApiFailsSoTheJobRetries() {
        server.stop(0);

        assertThatThrownBy(() -> adapter("no-reply@alapedu.online", KEY)
                .send(new MailMessage("sv@gmail.com", "Mã OTP", "012345", null)))
                .isInstanceOf(MailSendException.class)
                .hasMessageStartingWith("Không gọi được API Brevo");
    }

    @Test
    void missingConfigurationFailsBeforeCallingBrevo() {
        MailMessage message = new MailMessage("sv@gmail.com", "Mã OTP", "012345", null);

        assertThatThrownBy(() -> adapter("", KEY).send(message))
                .isInstanceOf(IllegalStateException.class).hasMessage("Chưa cấu hình SMTP_FROM");
        assertThatThrownBy(() -> adapter("no-reply@alapedu.online", " ").send(message))
                .isInstanceOf(IllegalStateException.class).hasMessage("Chưa cấu hình BREVO_API_KEY");
        assertThatThrownBy(() -> adapter("not an address", KEY).send(message))
                .isInstanceOf(IllegalStateException.class).hasMessage("SMTP_FROM không phải địa chỉ email hợp lệ");
        assertThat(lastBody.get()).isNull();
    }
}
