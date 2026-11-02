package edu.aiplatform.identity.application;

import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.domain.EmailPolicy;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link IAccountLookup} (domain-entities U01 mục 13): thông tin liên hệ tối thiểu (email, tên, role, trạng thái),
 * không có mật khẩu hay số điện thoại; mỗi lần tìm tối đa 20 kết quả. Unit gọi tự kiểm quyền của thao tác của mình.
 */
@Service
public class AccountLookupService implements IAccountLookup {

    public static final int MAX_RESULTS = 20;

    @Override
    public List<AccountContact> listActiveAdmins(UUID afterId,int limit) {
        return accounts.listActiveAdmins(afterId,Math.max(1,Math.min(100,limit))).stream()
                .map(AccountLookupService::contact).toList();
    }

    private final AccountRepository accounts;

    public AccountLookupService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    /** Sinh viên đang hoạt động (U05 thêm vào lớp; chỉ Student ACTIVE được ghi danh). */
    @Override
    public List<AccountContact> findStudents(String query, int limit) {
        return accounts.findForLookup(query, Set.of(Role.STUDENT), AccountStatus.ACTIVE, clamp(limit)).stream()
                .map(AccountLookupService::contact).toList();
    }

    /** Mọi trạng thái; người gọi tự lọc theo {@code status} khi cần (ví dụ chỉ nhận ACTIVE, MSG30). */
    @Override
    public List<AccountContact> findAccounts(String query, Set<Role> roles, int limit) {
        return accounts.findForLookup(query, roles == null ? Set.of() : roles, null, clamp(limit)).stream()
                .map(AccountLookupService::contact).toList();
    }

    @Override
    public List<AccountContact> findByEmails(Collection<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }
        List<String> normalized = emails.stream().map(EmailPolicy::normalize).filter(e -> !e.isEmpty())
                .distinct().toList();
        return accounts.findByEmails(normalized).stream().map(AccountLookupService::contact).toList();
    }

    @Override
    public Optional<AccountContact> getContact(UUID accountId) {
        return accountId == null ? Optional.empty() : accounts.findById(accountId).map(AccountLookupService::contact);
    }

    @Override
    public Map<UUID, AccountContact> getContacts(Collection<UUID> accountIds) {
        List<UUID> ids = accountIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<UUID, AccountContact> contacts = new LinkedHashMap<>();
        accounts.findByIds(ids).forEach(account -> contacts.put(account.id(), contact(account)));
        return contacts;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<AccountContact> lockContact(UUID accountId) {
        return accountId == null ? Optional.empty()
                : accounts.findByIdForShare(accountId).map(AccountLookupService::contact);
    }

    /** Số đếm cho Admin Dashboard (U15); không có dữ liệu cá nhân. */
    @Override
    public Map<Role, Map<AccountStatus, Long>> countByRoleAndStatus() {
        return accounts.countByRoleAndStatus();
    }

    private static int clamp(int limit) {
        return limit < 1 ? MAX_RESULTS : Math.min(limit, MAX_RESULTS);
    }

    private static AccountContact contact(Account account) {
        return new AccountContact(account.id(), account.schoolEmail(), account.displayName(), account.role(),
                account.status());
    }
}
