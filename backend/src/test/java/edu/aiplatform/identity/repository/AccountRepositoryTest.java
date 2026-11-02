package edu.aiplatform.identity.repository;

import edu.aiplatform.identity.model.AccountSearch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Phần thuần Java của repository; truy vấn SQL do tester kiểm với PostgreSQL thật. */
class AccountRepositoryTest {

    @Test
    void escapesLikeWildcards() {
        assertThat(AccountRepository.escapeLike("50%_a\\b")).isEqualTo("50\\%\\_a\\\\b");
    }

    @Test
    void searchClampsPageAndSize() {
        AccountSearch search = new AccountSearch(null, null, "  ", -1, 500);
        assertThat(search.page()).isZero();
        assertThat(search.size()).isEqualTo(AccountSearch.MAX_SIZE);
        assertThat(search.query()).isNull();
        assertThat(new AccountSearch(null, null, null, 0, 0).size()).isEqualTo(20);
    }
}
