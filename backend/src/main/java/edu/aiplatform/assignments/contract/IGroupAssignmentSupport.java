package edu.aiplatform.assignments.contract;

import java.util.UUID;

/** U07 khai báo, U12 cài: bật bài {@code GROUP}; chưa có U12 thì không chọn được bài nhóm. */
public interface IGroupAssignmentSupport {

    ReadinessResult checkReadiness(UUID classId);
}
