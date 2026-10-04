package edu.aiplatform.billing.port;

import java.time.Instant;

/** Adapter PayOS. */
public interface PaymentProviderPort {
    record CheckoutLink(String checkoutUrl, Instant expiresAt) {}

    record ProviderStatus(String status, long amountVnd, String providerReference) {}

    CheckoutLink createLink(long orderCode, long amountVnd, String description, String returnUrl, String cancelUrl);
    ProviderStatus getStatus(long orderCode);
    boolean verifyWebhookSignature(String rawBody, String signature);
}
