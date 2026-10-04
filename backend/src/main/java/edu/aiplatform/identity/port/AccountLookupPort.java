package edu.aiplatform.identity.port;

import edu.aiplatform.shared.contract.Role;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** U01 cung cấp cho U04, U16. */
public interface AccountLookupPort {
    List<AccountContact> findStudents(String query, int limit);
    List<AccountContact> findByEmails(List<String> schoolEmails);
    Optional<AccountContact> getContact(UUID accountId);
    /** Số tài khoản theo vai trò và trạng thái cho UC 18. */
    Map<Role, Map<AccountStatus, Long>> countByRoleAndStatus();
}
