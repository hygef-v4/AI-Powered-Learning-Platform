package edu.aiplatform.groups.service;

import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.IAccountLookup;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Tra họ tên, email một lần cho mỗi người trong một lần đọc; U01 lỗi thì để trống, không chặn màn. */
public final class ProposalPeople {

    private final IAccountLookup accounts;
    private final Map<UUID, Optional<AccountContact>> cache = new HashMap<>();

    ProposalPeople(IAccountLookup accounts) {
        this.accounts = accounts;
    }

    Person loadPerson(UUID accountId) {
        Optional<AccountContact> contact = cache.computeIfAbsent(accountId, key -> {
            try {
                return accounts.getContact(key);
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        });
        return new Person(accountId, contact.map(AccountContact::displayName).orElse(null),
                contact.map(AccountContact::email).orElse(null));
    }

    public record Person(UUID accountId, String name, String email) {
    }
}
