package edu.aiplatform.jobs.service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AfterCommitExecutor.class);

    private final Executor executor;

    /** 2 luồng gửi, hàng chờ 1.000; đầy thì báo cho bên gửi qua {@code onRejected} thay vì bỏ im lặng. */
    @Autowired
    public AfterCommitExecutor() {
        this(new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1_000), runnable -> {
            Thread thread = new Thread(runnable, "job-send");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy()));
    }

    AfterCommitExecutor(Executor executor) {
        this.executor = executor;
    }

    public void run(Runnable task) {
        run(task, () -> log.warn("Hàng chờ gửi message đầy, bỏ một message"));
    }

    /** Như {@link #run(Runnable)}; hàng chờ gửi đầy thì gọi {@code onRejected} (không ném lỗi sau commit). */
    public void run(Runnable task, Runnable onRejected) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    execute(task, onRejected);
                }
            });
        } else {
            execute(task, onRejected);
        }
    }

    private void execute(Runnable task, Runnable onRejected) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            onRejected.run();
        }
    }
}
