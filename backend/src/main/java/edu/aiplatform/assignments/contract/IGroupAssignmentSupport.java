package edu.aiplatform.assignments.contract;

import java.util.UUID;

/** U07 khai báo, U12 cài: bật bài {@code GROUP}; chưa có U12 thì không chọn được bài nhóm. */
public interface IGroupAssignmentSupport {

    /** Lỗi bản tạm trả khi U12 chưa cài: U07 không cho tạo, phát hành bài {@code GROUP} (BR-U07-03). */
    String UNAVAILABLE = "GROUP_SUPPORT_UNAVAILABLE";


    ReadinessResult checkReadiness(UUID classId);
}
