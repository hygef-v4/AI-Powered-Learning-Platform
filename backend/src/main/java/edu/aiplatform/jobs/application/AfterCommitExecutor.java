package edu.aiplatform.jobs.application;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Chạy việc gửi message sau commit (P8): có transaction thì đăng ký {@code afterCommit}, rollback thì không chạy;
 * không có transaction thì chạy ngay. Việc gửi chạy trên luồng riêng để RabbitMQ chậm không chặn request.
 */
@Component
public class AfterCommitExecutor {

    private final Executor executor;

    /** 2 luồng gửi, hàng chờ 1.000; đầy thì bỏ (sweeper gửi lại việc, sự kiện được phép mất). */
    @Autowired
    public AfterCommitExecutor() {
        this(new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1_000), runnable -> {
            Thread thread = new Thread(runnable, "job-send");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.DiscardPolicy()));
    }

    AfterCommitExecutor(Executor executor) {
        this.executor = executor;
    }

    public void run(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(task);
                }
            });
        } else {
            executor.execute(task);
        }
    }
}
