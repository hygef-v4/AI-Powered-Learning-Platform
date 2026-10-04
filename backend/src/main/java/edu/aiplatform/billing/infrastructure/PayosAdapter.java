package edu.aiplatform.billing.infrastructure;

import edu.aiplatform.billing.port.*;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U07: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U07: Adapter PayOS.
 */
@Component
public class PayosAdapter implements PaymentProviderPort {

    @Override
    public CheckoutLink createLink(long orderCode, long amountVnd, String description, String returnUrl, String cancelUrl) {
        throw new UnsupportedOperationException("Chưa cài: U07");
    }

    @Override
    public ProviderStatus getStatus(long orderCode) {
        throw new UnsupportedOperationException("Chưa cài: U07");
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        return false;
    }
}
