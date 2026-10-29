package edu.aiplatform.identity.contract;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Tra tài khoản cho U02, U04, U05, U14, U15; mỗi lần tìm tối đa 20 kết quả. */
public interface IAccountLookup {

    /** U14: keyset page of ACTIVE Admins, ordered by UUID; maximum page size100. */
    default List<AccountContact> listActiveAdmins(UUID afterId, int limit) {
        throw new UnsupportedOperationException("Active Admin pagination is unavailable");
    }

    List<AccountContact> findStudents(String query, int limit);

    List<AccountContact> findAccounts(String query, Set<Role> roles, int limit);

    List<AccountContact> findByEmails(Collection<String> emails);

    Optional<AccountContact> getContact(UUID accountId);

    /**
     * Thông tin liên hệ theo lô, bỏ qua id không có; dùng khi hiện danh sách nhiều người để không tra từng người một.
     * Bản mặc định gọi {@link #getContact} từng id; U01 cài bằng một truy vấn.
     */
    default Map<UUID, AccountContact> getContacts(Collection<UUID> accountIds) {
        Map<UUID, AccountContact> contacts = new LinkedHashMap<>();
        for (UUID id : accountIds) {
            getContact(id).ifPresent(contact -> contacts.put(id, contact));
        }
        return contacts;
    }

    /**
     * Như {@link #getContact} nhưng khóa dòng tài khoản ({@code FOR SHARE}) tới hết transaction của bên gọi; phải gọi
     * trong transaction. U04, U05 dùng khi gán Chủ nhiệm môn, giảng viên để U01 không đổi role hay vô hiệu hóa tài khoản
     * xen giữa lúc kiểm và lúc ghi phân công (BR-U01-64, 71). Gọi trước khi khóa dòng môn hoặc lớp để thứ tự khóa luôn là
     * tài khoản rồi mới tới phân công, như U01.
     */
    Optional<AccountContact> lockContact(UUID accountId);

    Map<Role, Map<AccountStatus, Long>> countByRoleAndStatus();
}
