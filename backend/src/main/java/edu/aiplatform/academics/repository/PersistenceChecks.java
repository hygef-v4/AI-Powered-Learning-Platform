package edu.aiplatform.academics.repository;

import org.springframework.transaction.support.TransactionSynchronizationManager;

final class PersistenceChecks {
    /** Prevents instantiation of this persistence validation utility. */
    private PersistenceChecks() {
    }

    /** Requires an active writable transaction before a repository mutation. */
    static void requireWritableTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException(
                    "U05 writes require an explicit writable transaction and subject lock");
        }
    }

    /** Validates the requested persistence page size and offset. */
    static void validatePage(int limit, int offset) {
        if (limit < 1 || limit > 100 || offset < 0)
            throw new IllegalArgumentException("Page size must be 1–100 and offset non-negative");
    }
}
