package edu.aiplatform.assignmentwork.contract;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** U08 khai báo, U12 cài: trạng thái bài nhóm của người học trên Student Assignments (khóa là {@code assignmentId}). */
public interface IGroupWorkStatus {

    Map<UUID, GroupWorkStatus> statusFor(UUID accountId, Collection<UUID> assignmentIds);
}
