package edu.aiplatform.billing.port;

import java.util.UUID;

/** U07 cung cấp cho U13; gọi trong transaction đổi `ai_suggestions.credit_status` (BR-U07-40…43). */
public interface CreditPort {
    /** Student chỉ được PRACTICE_GRADING với attempt hợp lệ. Thiếu credit → {@link InsufficientCreditException}. */
    CreditReservation reserve(UUID accountId, long credits, CreditPurpose purpose, UUID attemptRef);
    void settle(UUID accountId, CreditReservation reservation, long actualCredits);
    void release(UUID accountId, CreditReservation reservation);
    CreditBalance balance(UUID accountId);
}
