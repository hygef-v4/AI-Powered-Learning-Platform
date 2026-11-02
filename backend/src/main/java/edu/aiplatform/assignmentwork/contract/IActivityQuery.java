package edu.aiplatform.assignmentwork.contract;

import java.time.Duration;
import java.util.UUID;

/** Dữ liệu rời trang cho UC 42 sau khi U09 kiểm phân công. */
public interface IActivityQuery {

    ActivitySummary summary(WorkRef workRef, UUID accountId);

    /** Thời hạn giữ sự kiện (BR-U08-54); quá hạn thì không còn dòng thời gian. */
    default Duration retention() {
        return Duration.ofDays(1);
    }
}
