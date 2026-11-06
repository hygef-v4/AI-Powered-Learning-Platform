package edu.aiplatform.academics.repository;

import org.springframework.transaction.support.TransactionSynchronizationManager;

final class PersistenceChecks {
    private PersistenceChecks() {
    }

    static void requireWritableTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException(
                    "U05 writes require an explicit writable transaction and subject lock");
        }
    }

    static void validatePage(int limit, int offset) {
        if (limit < 1 || limit > 100 || offset < 0)
            throw new IllegalArgumentException("Page size must be 1–100 and offset non-negative");
    }
}
