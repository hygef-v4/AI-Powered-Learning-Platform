package edu.aiplatform.billing.application;

import edu.aiplatform.billing.port.*;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U07: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class CreditPortService implements CreditPort {

    @Override
    public CreditReservation reserve(UUID accountId, long credits, CreditPurpose purpose, UUID attemptRef) {
        throw new UnsupportedOperationException("Chưa cài: U07");
    }

    @Override
    public void settle(UUID accountId, CreditReservation reservation, long actualCredits) {
        // Chưa cài: U07.
    }

    @Override
    public void release(UUID accountId, CreditReservation reservation) {
        // Chưa cài: U07.
    }

    @Override
    public CreditBalance balance(UUID accountId) {
        throw new UnsupportedOperationException("Chưa cài: U07");
    }
}
