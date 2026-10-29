package edu.aiplatform.identity.contract;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Tra tài khoản cho U02, U04, U05, U14, U15; mỗi lần tìm tối đa 20 kết quả. */
public interface IAccountLookup {

    List<AccountContact> findStudents(String query, int limit);

    List<AccountContact> findAccounts(String query, Set<Role> roles, int limit);

    List<AccountContact> findByEmails(Collection<String> emails);

    Optional<AccountContact> getContact(UUID accountId);

    Map<Role, Map<AccountStatus, Long>> countByRoleAndStatus();
}
