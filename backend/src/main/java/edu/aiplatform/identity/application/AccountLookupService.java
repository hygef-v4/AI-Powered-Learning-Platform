package edu.aiplatform.identity.application;

import edu.aiplatform.identity.port.*;
import edu.aiplatform.shared.contract.Role;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U01: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U01: Bước 18.
 */
@Service
public class AccountLookupService implements AccountLookupPort {

    @Override
    public List<AccountContact> findStudents(String query, int limit) {
        return List.of();
    }

    @Override
    public List<AccountContact> findByEmails(List<String> schoolEmails) {
        return List.of();
    }

    @Override
    public Optional<AccountContact> getContact(UUID accountId) {
        return Optional.empty();
    }

    @Override
    public Map<Role, Map<AccountStatus, Long>> countByRoleAndStatus() {
        return Map.of();
    }
}
