package edu.aiplatform.ai.application;

import edu.aiplatform.ai.application.IProviderGateway.ProviderException;
import edu.aiplatform.ai.application.IProviderGateway.Request;
import edu.aiplatform.ai.application.IProviderGateway.Response;
import edu.aiplatform.ai.application.IProviderGateway.Usage;
import edu.aiplatform.ai.domain.AiConnection;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra kết nối bằng một yêu cầu tổng hợp cố định, tối đa 5 token ra, không dùng nội dung học vụ (BR-U06-04);
 * kết quả trả về đã làm sạch (không có key hay nội dung phản hồi của provider).
 */
@Component
public class ConnectionTestService {

    static final String SYSTEM = "Bạn là bộ kiểm tra kết nối.";
    static final String USER = "Trả lời đúng một từ: OK";
    static final int MAX_OUTPUT = 5;

    private final IProviderGateway gateway;
    private final KeyCipher cipher;
    private final AiProperties properties;

    public ConnectionTestService(IProviderGateway gateway, KeyCipher cipher, AiProperties properties) {
        this.gateway = gateway;
        this.cipher = cipher;
        this.properties = properties;
    }

    public Result test(AiConnection connection) {
        String key = cipher.decrypt(connection.encryptedKey());
        try {
            Response response = gateway.call(connection.provider(), new Request(connection.model(), key, SYSTEM, USER, null,
                    MAX_OUTPUT, properties.testTimeout()));
            return new Result(true, "Đạt", response.usage());
        } catch (ProviderException e) {
            return new Result(false, e.getMessage(), e.usage());
        }
    }

    public record Result(boolean ok, String message, Usage usage) {
    }
}
