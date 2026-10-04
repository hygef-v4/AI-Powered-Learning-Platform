package edu.aiplatform.billing.event;

import java.util.UUID;

public record PaymentPaid(UUID paymentId, UUID accountId, long credits) {}
