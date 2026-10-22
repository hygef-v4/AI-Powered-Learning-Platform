package edu.aiplatform.groupdocs.application;

import edu.aiplatform.jobs.contract.IJobHandler;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.JobQueue;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.jobs.contract.RetryableJobException;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Worker tạo tài liệu nhóm khi bài nhóm mở (F7). Idempotent vì tạo dùng {@code ON CONFLICT DO NOTHING}. Message mất
 * không làm thiếu tài liệu: người học mở bài thì tài liệu được tạo ngay (F7 bước 3).
 */
@Component
public class GroupDocCreateJobHandler implements IJobHandler {

    private static final Logger log = LoggerFactory.getLogger(GroupDocCreateJobHandler.class);

    private final GroupDocumentService documents;

    public GroupDocCreateJobHandler(GroupDocumentService documents) {
        this.documents = documents;
    }

    @Override
    public String jobType() {
        return JobTypes.GROUP_DOC_CREATE;
    }

    @Override
    public JobQueue queue() {
        return JobQueue.TRIGGERED;
    }

    @Override
    public void handle(JobMessage message) {
        UUID assignmentId;
        try {
            assignmentId = UUID.fromString(String.valueOf(message.payload().get("assignmentId")));
        } catch (IllegalArgumentException e) {
            log.warn("Bỏ qua job {} thiếu assignmentId", message.idempotencyKey());
            return;
        }
        try {
            int created = documents.ensureDocuments(assignmentId);
            log.info("Tạo {} tài liệu nhóm cho bài {}", created, assignmentId);
        } catch (DataAccessException e) {
            throw new RetryableJobException("Không ghi được tài liệu nhóm", e);
        } catch (RuntimeException e) {
            // U07 hoặc U05 tạm không trả lời: gửi lại theo backoff của U03.
            throw new RetryableJobException("Không đọc được bài hoặc lớp", e);
        }
    }

    @Override
    public void onFailed(JobMessage message, String error) {
        log.warn("Hết lượt tạo tài liệu nhóm {}: {}", message.idempotencyKey(), error);
    }

    @Override
    public Duration timeout() {
        return Duration.ofMinutes(2);
    }
}
