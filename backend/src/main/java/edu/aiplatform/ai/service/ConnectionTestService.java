package edu.aiplatform.ai.service;

import edu.aiplatform.ai.config.AiProperties;

import edu.aiplatform.ai.service.IProviderGateway.ProviderException;
import edu.aiplatform.ai.service.IProviderGateway.OutputLimitException;
import edu.aiplatform.ai.service.IProviderGateway.Request;
import edu.aiplatform.ai.service.IProviderGateway.Response;
import edu.aiplatform.ai.service.IProviderGateway.Usage;
import edu.aiplatform.ai.model.AiConnection;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra kết nối bằng yêu cầu tổng hợp cố định có ngân sách token suy nghĩ, không dùng nội dung học vụ (BR-U06-04);
 * kết quả trả về đã làm sạch (không có key hay nội dung phản hồi của provider).
 */
@Component
public class ConnectionTestService {

    static final String SYSTEM = "Bạn là bộ kiểm tra kết nối.";
    static final String USER = "Trả lời đúng một từ: OK";
    static final int MAX_OUTPUT = 1024;

    private final IProviderGateway gateway;
    private final KeyCipher cipher;
    private final AiProperties properties;

    public ConnectionTestService(IProviderGateway gateway, KeyCipher cipher, AiProperties properties) {
        this.gateway = gateway;
        this.cipher = cipher;
        this.properties = properties;
    }

    public Result test(AiConnection connection) {
        String key = cipher.decryptConnectionKey(connection);
        try {
            Response response = gateway.call(connection.provider(), new Request(connection.model(), key, SYSTEM, USER, null,
                    MAX_OUTPUT, properties.testTimeout()));
            return new Result(true, "Đạt", response.usage());
        } catch (OutputLimitException e) {
            // Probe chỉ kiểm key/model: provider đã chấp nhận, không cần dùng nội dung bị cắt.
            return new Result(true, "Đạt", e.usage());
        } catch (ProviderException e) {
            return new Result(false, e.getMessage(), e.usage());
        }
    }

    public record Result(boolean ok, String message, Usage usage) {
    }
}
