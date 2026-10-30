package edu.aiplatform.identity.model;

import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import java.util.List;

/**
 * Bộ lọc Account List (UC 08): role, trạng thái, chuỗi tìm theo email hoặc tên; trang đánh số từ 0.
 * Trường rỗng nghĩa là không lọc theo trường đó.
 */
public record AccountSearch(Role role, AccountStatus status, String query, int page, int size) {

    public static final int MAX_SIZE = 50;

    public AccountSearch {
        page = Math.max(page, 0);
        size = size < 1 ? 20 : Math.min(size, MAX_SIZE);
        query = query == null || query.isBlank() ? null : query.strip();
    }

    public record Result(List<Account> items, long total) {
    }
}
