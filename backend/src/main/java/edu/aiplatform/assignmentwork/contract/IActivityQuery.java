package edu.aiplatform.assignmentwork.contract;

import java.util.UUID;

/** Dữ liệu rời trang cho UC 42 sau khi U09 kiểm phân công. */
public interface IActivityQuery {

    ActivitySummary summary(WorkRef workRef, UUID accountId);
}
