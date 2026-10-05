package edu.aiplatform.jobs.application;

import edu.aiplatform.shared.web.CorrelationIdFilter;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Chạy sau commit nếu đang trong transaction (rollback thì bỏ), ngược lại chạy ngay (P8). */
final class AfterCommit {

    private AfterCommit() {}

    static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /** Correlation ID của request hiện tại; việc nền phát sinh ngoài request thì sinh mới. */
    static String correlationId() {
        String id = MDC.get(CorrelationIdFilter.MDC_KEY);
        return id != null ? id : UUID.randomUUID().toString();
    }
}
