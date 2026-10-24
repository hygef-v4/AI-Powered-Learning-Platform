package edu.aiplatform.assignments.domain;

import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.SubmissionMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Một version của bài (domain-entities U07 mục 2). Bất biến; service tạo bản mới rồi lưu qua repository có điều kiện
 * {@code version}. Mọi thay đổi, kể cả đổi trạng thái của scanner, tăng {@code version} (bắt đầu 0).
 */
public record Assignment(UUID id, AssignmentScope scope, UUID classId, UUID subjectId, UUID sourceAssignmentId,
                         AssignmentOrigin origin, int versionNo, SubmissionMode submissionMode, boolean diagramEnabled,
                         GradingMode gradingMode, String title, String instructions, DocumentModel skeleton,
                         List<DocumentPart> parts, Map<DiagramType, Integer> requiredDiagrams,
                         List<UUID> referenceMaterialIds, boolean activityMonitoringEnabled, String activityNotice,
                         AssignmentStatus status, Instant opensAt, Instant closesAt, Instant lateUntil,
                         Integer maxAttempts, String retiredReason, UUID createdBy, Instant createdAt,
                         Instant updatedAt, long version) {

    public static final int TITLE_MAX = 200;
    public static final int INSTRUCTIONS_MAX = 20_000;
    public static final int NOTICE_MAX = 1_000;
    public static final int REASON_MAX = 500;
    public static final int MATERIALS_MAX = 10;
    public static final int REQUIRED_DIAGRAM_MAX = 20;
    public static final int ATTEMPTS_MAX = 10;
    public static final java.time.Duration LATE_MAX = java.time.Duration.ofDays(30);

    public Assignment {
        // Sơ đồ luôn dùng được, không còn bật/tắt theo bài (yêu cầu 2026-10-21); bài cũ lưu false cũng coi là bật.
        diagramEnabled = true;
        parts = List.copyOf(parts);
        requiredDiagrams = Map.copyOf(requiredDiagrams);
        referenceMaterialIds = List.copyOf(referenceMaterialIds);
    }

    /** Nội dung chỉ sửa khi còn nháp hoặc đã duyệt (BR-U07-30). */
    public boolean editable() {
        return status == AssignmentStatus.DRAFT || status == AssignmentStatus.REVIEWED;
    }

    public boolean published() {
        return !editable();
    }

    /** Hạn cuối nhận bài: {@code lateUntil} nếu có, không thì {@code closesAt}. */
    public Instant finalDeadline() {
        return lateUntil != null ? lateUntil : closesAt;
    }

    public Assignment withDetails(String title, String instructions, boolean diagramEnabled, GradingMode gradingMode,
                                  Map<DiagramType, Integer> requiredDiagrams, List<UUID> materials,
                                  boolean activityMonitoringEnabled, String activityNotice, Instant now) {
        return new Assignment(id, scope, classId, subjectId, sourceAssignmentId, origin, versionNo, submissionMode,
                diagramEnabled, gradingMode, title, instructions, skeleton, parts, requiredDiagrams, materials,
                activityMonitoringEnabled, activityNotice, backToDraft(), opensAt, closesAt, lateUntil, maxAttempts,
                retiredReason, createdBy, createdAt, now, version);
    }

    public Assignment withSkeleton(String instructions, DocumentModel skeleton, List<DocumentPart> parts, Instant now) {
        return withAuthoring(title, instructions, skeleton, parts, now);
    }

    /** Thay tiêu đề có chọn, hướng dẫn và khung trong một lần lưu/version duy nhất. */
    public Assignment withAuthoring(String title, String instructions, DocumentModel skeleton,
                                    List<DocumentPart> parts, Instant now) {
        return new Assignment(id, scope, classId, subjectId, sourceAssignmentId, origin, versionNo, submissionMode,
                diagramEnabled, gradingMode, title, instructions, skeleton, parts, requiredDiagrams,
                referenceMaterialIds, activityMonitoringEnabled, activityNotice, backToDraft(), opensAt, closesAt,
                lateUntil, maxAttempts, retiredReason, createdBy, createdAt, now, version);
    }

    /** Sửa rubric của bài đã duyệt cũng đưa bài về nháp (F5 bước 4). */
    public Assignment touched(Instant now) {
        return withStatus(backToDraft(), now);
    }

    public Assignment withStatus(AssignmentStatus next, Instant now) {
        return new Assignment(id, scope, classId, subjectId, sourceAssignmentId, origin, versionNo, submissionMode,
                diagramEnabled, gradingMode, title, instructions, skeleton, parts, requiredDiagrams,
                referenceMaterialIds, activityMonitoringEnabled, activityNotice, next, opensAt, closesAt, lateUntil,
                maxAttempts, retiredReason, createdBy, createdAt, now, version);
    }

    public Assignment withSchedule(AssignmentStatus next, Instant opensAt, Instant closesAt, Instant lateUntil,
                                   Integer maxAttempts, Instant now) {
        return new Assignment(id, scope, classId, subjectId, sourceAssignmentId, origin, versionNo, submissionMode,
                diagramEnabled, gradingMode, title, instructions, skeleton, parts, requiredDiagrams,
                referenceMaterialIds, activityMonitoringEnabled, activityNotice, next, opensAt, closesAt, lateUntil,
                maxAttempts, retiredReason, createdBy, createdAt, now, version);
    }

    public Assignment retired(String reason, Instant now) {
        return new Assignment(id, scope, classId, subjectId, sourceAssignmentId, origin, versionNo, submissionMode,
                diagramEnabled, gradingMode, title, instructions, skeleton, parts, requiredDiagrams,
                referenceMaterialIds, activityMonitoringEnabled, activityNotice, AssignmentStatus.RETIRED, opensAt,
                closesAt, lateUntil, maxAttempts, reason, createdBy, createdAt, now, version);
    }

    /** Bản sao nháp cho nhân bản, version mới, copy giữa lớp (BR-U07-43): không sao lịch, lý do ngưng giao. */
    public Assignment copyAsDraft(UUID newId, UUID newClassId, UUID newSubjectId, AssignmentOrigin newOrigin,
                                  int newVersionNo, String newTitle, List<UUID> materials, UUID actorId, Instant now) {
        return new Assignment(newId, scope, newClassId, newSubjectId, id, newOrigin, newVersionNo, submissionMode,
                diagramEnabled, gradingMode, newTitle, instructions, skeleton, parts, requiredDiagrams, materials,
                activityMonitoringEnabled, activityNotice, AssignmentStatus.DRAFT, null, null, null, null, null,
                actorId, now, now, 0);
    }

    public Assignment withVersion(long nextVersion) {
        return new Assignment(id, scope, classId, subjectId, sourceAssignmentId, origin, versionNo, submissionMode,
                diagramEnabled, gradingMode, title, instructions, skeleton, parts, requiredDiagrams,
                referenceMaterialIds, activityMonitoringEnabled, activityNotice, status, opensAt, closesAt, lateUntil,
                maxAttempts, retiredReason, createdBy, createdAt, updatedAt, nextVersion);
    }

    public AssignmentView view() {
        return new AssignmentView(id, scope, classId, subjectId, versionNo, title, submissionMode, gradingMode,
                diagramEnabled, status, opensAt, closesAt, lateUntil, maxAttempts, activityMonitoringEnabled,
                activityNotice, referenceMaterialIds);
    }

    private AssignmentStatus backToDraft() {
        return status == AssignmentStatus.REVIEWED ? AssignmentStatus.DRAFT : status;
    }
}
