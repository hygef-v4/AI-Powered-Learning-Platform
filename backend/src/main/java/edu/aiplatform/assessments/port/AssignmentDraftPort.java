package edu.aiplatform.assessments.port;

import edu.aiplatform.questionbank.port.AssignmentType;
import edu.aiplatform.shared.contract.ActorRef;
import java.util.UUID;

/** U08 cung cấp cho U10: tạo bài nháp từ nguồn copy (lớp hoặc template). */
public interface AssignmentDraftPort {
    AssignmentView createDraftFrom(UUID sourceAssignmentId, UUID targetClassId, UUID targetSubjectId);
    AssignmentView createTemplate(ActorRef actor, UUID subjectId, AssignmentType type, GradingMode mode, String title);
}
