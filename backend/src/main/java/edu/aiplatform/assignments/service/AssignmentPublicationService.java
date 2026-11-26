package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.dto.AssignmentViews;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.ReadinessResult;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.repository.AssignmentRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Phát hành, sửa lịch, ngưng giao (F9, F11; BR-U07-33…36; P2). */
@Service
public class AssignmentPublicationService {

    private final AssignmentRepository assignments;
    private final AssignmentAuthoringService assignmentAuthoringService;
    private final RubricService rubricService;
    private final AssignmentAccess access;
    private final AssignmentSupport support;
    private final AssignmentLifecycle lifecycle;
    private final ISubjectQuery subjects;
    private final Clock clock;

    public AssignmentPublicationService(AssignmentRepository assignments, AssignmentAuthoringService assignmentAuthoringService,
                                        RubricService rubricService, AssignmentAccess access, AssignmentSupport support,
                                        AssignmentLifecycle lifecycle, ISubjectQuery subjects,
                                        Clock clock) {
        this.assignments = assignments;
        this.assignmentAuthoringService = assignmentAuthoringService;
        this.rubricService = rubricService;
        this.access = access;
        this.support = support;
        this.lifecycle = lifecycle;
        this.subjects = subjects;
        this.clock = clock;
    }

    /**
     * F9: một transaction ghi lịch, khóa rubric, chuyển SCHEDULED; mở ngay nếu {@code opensAt} đã tới. Phát hành thẳng
     * từ bài nháp (yêu cầu 2026-10-22 bỏ trạng thái Đã duyệt): luật duyệt của BR-U07-32 kiểm ngay tại đây; bài đã duyệt
     * từ trước vẫn phát hành được.
     */
    @Transactional
    public AssignmentViews.Detail publish(Actor actor, UUID id, AssignmentCommands.Publish command) {
        Assignment current = support.lock(actor, id, command.version(), "ASSIGNMENT_PUBLISHED");
        // Bài của môn chỉ là đề mẫu (yêu cầu 2026-11-09): giảng viên copy về lớp rồi tự giao.
        if (current.scope() == AssignmentScope.SUBJECT) {
            throw ApiException.createConflict("SUBJECT_TEMPLATE_NOT_PUBLISHABLE",
                    "Bài của môn là đề mẫu, giảng viên tạo bài ở lớp từ đề mẫu rồi giao");
        }
        if (!current.canEdit()) {
            throw ApiException.createConflict("ASSIGNMENT_STATUS_INVALID", "Chỉ phát hành bài nháp");
        }
        // Hoàn tất bài rồi mới giao (yêu cầu 2026-11-11); sửa bài sau khi hoàn tất thì bài về nháp, phải hoàn tất lại.
        if (current.status() != AssignmentStatus.REVIEWED) {
            throw ApiException.createConflict("ASSIGNMENT_NOT_FINALIZED", "Hoàn tất bài trước khi xếp lịch");
        }
        List<ValidationIssue> issues = assignmentAuthoringService.listReadinessIssues(current);
        if (!issues.isEmpty()) {
            throw AssignmentProblem.createIssuesProblem(HttpStatus.CONFLICT, "ASSIGNMENT_NOT_READY", "Bài chưa đủ để phát hành",
                    issues);
        }
        requireOwnerOpen(current);
        Instant now = clock.instant();
        Instant opensAt = command.openNow() ? now : command.opensAt();
        if (!command.openNow()) {
            requireNotPast(opensAt, now);
        }
        Integer maxAttempts = resolveAttempts(current, command.maxAttempts());
        validateSchedule(opensAt, command.closesAt(), command.lateUntil(), maxAttempts, current.gradingMode(), now);
        if (current.submissionMode() == SubmissionMode.GROUP) {
            requireGroupsReady(current.classId(), command.confirmUngroupedStudents());
        }
        Assignment saved = assignments.saveAssignment(current.applySchedule(AssignmentStatus.SCHEDULED, opensAt,
                command.closesAt(), command.lateUntil(), maxAttempts, now));
        rubricService.lockAll(saved.id(), now);
        if (!opensAt.isAfter(now)) {
            lifecycle.open(saved.id(), now);
        }
        return support.buildDetail(actor, assignments.findAssignment(saved.id()).orElseThrow(), List.of());
    }

    /** F11 sửa lịch (BR-U07-35): SCHEDULED sửa tự do; OPEN chỉ kéo dài hạn, không đổi số lượt. */
    @Transactional
    public AssignmentViews.Detail changeSchedule(Actor actor, UUID id, AssignmentCommands.Schedule command) {
        Assignment current = support.lock(actor, id, command.version(), "ASSIGNMENT_SCHEDULE_CHANGED");
        Instant now = clock.instant();
        Instant opensAt = command.opensAt() == null ? current.opensAt() : command.opensAt();
        Integer maxAttempts = resolveAttempts(current, command.maxAttempts() == null ? current.maxAttempts() : command.maxAttempts());
        Instant closesAt = command.closesAt() == null ? current.closesAt() : command.closesAt();
        Instant lateUntil = command.lateUntil();
        switch (current.status()) {
            case SCHEDULED -> {
                // Đổi ngày mở thì ngày mới không được ở quá khứ; bỏ hẹn giờ (mở ngay) gửi thời điểm hiện tại.
                if (command.opensAt() != null && !command.opensAt().equals(current.opensAt())) {
                    requireNotPast(command.opensAt(), now);
                }
                validateSchedule(opensAt, closesAt, lateUntil, maxAttempts, current.gradingMode(), now);
            }
            case OPEN -> {
                if (!opensAt.equals(current.opensAt()) || (Assignment.limitsAttempts(current.gradingMode())
                        && !Objects.equals(maxAttempts, current.maxAttempts()))) {
                    throw ApiException.createBadRequest("SCHEDULE_OPEN_LOCKED",
                            "Bài đang mở chỉ kéo dài hạn nộp, không đổi ngày mở và số lượt");
                }
                if (closesAt.isBefore(current.closesAt())
                        || (current.lateUntil() != null && (lateUntil == null || lateUntil.isBefore(current.lateUntil())))) {
                    throw ApiException.createBadRequest("SCHEDULE_SHORTENED", "Bài đang mở không rút ngắn hạn nộp");
                }
                // Đang trong hạn nộp trễ thì hạn nộp đã qua; chỉ cần hạn cuối nhận bài còn ở tương lai.
                validateOrder(opensAt, closesAt, lateUntil);
                if (!(lateUntil != null ? lateUntil : closesAt).isAfter(now)) {
                    throw ApiException.createBadRequest("SCHEDULE_INVALID", "Hạn nộp phải ở tương lai");
                }
            }
            default -> throw ApiException.createConflict("ASSIGNMENT_STATUS_INVALID",
                    "Chỉ sửa lịch bài đã lên lịch hoặc đang mở");
        }
        Assignment saved = assignments.saveAssignment(current.applySchedule(current.status(), opensAt, closesAt, lateUntil,
                maxAttempts, now));
        return support.buildDetail(actor, saved, List.of());
    }

    /** Closes a teacher's open class assignment after saved work has been finalized. */
    @Transactional
    public AssignmentViews.Detail closeAssignment(Actor actor, UUID id, long version) {
        Assignment current = support.lock(actor, id, version, "ASSIGNMENT_CLOSED");
        if (current.scope() != AssignmentScope.CLASS || current.status() != AssignmentStatus.OPEN) {
            throw ApiException.createConflict("ASSIGNMENT_STATUS_INVALID", "Chỉ đóng nhận bài của lớp đang mở");
        }
        if (access.loadClassRef(current.classId()).status() != ClassStatus.OPEN) {
            throw ApiException.createConflict("CLASS_NOT_OPEN", "Lớp không mở, chỉ xem được bài");
        }
        Instant now = clock.instant();
        lifecycle.closeAssignment(id, now, false);
        Assignment closed = assignments.findAssignment(id).orElseThrow();
        return support.buildDetail(actor, closed, List.of());
    }

    /** F11 ngưng giao (BR-U07-36): lý do bắt buộc; listener U08, U12 tự nộp bài đang làm trong cùng transaction. */
    @Transactional
    public AssignmentViews.Detail retire(Actor actor, UUID id, AssignmentCommands.Retire command) {
        String reason = command.reason() == null ? "" : command.reason().strip();
        if (reason.isEmpty() || reason.length() > Assignment.REASON_MAX) {
            throw ApiException.createBadRequest("RETIRE_REASON_INVALID", "Nhập lý do ngưng giao, tối đa 500 ký tự");
        }
        Assignment current = assignments.findForUpdate(id).orElseThrow(() -> access.notFound());
        access.requireManage(actor, current, "ASSIGNMENT_RETIRED");
        AssignmentSupport.requireVersion(current, command.version());
        if (current.status() != AssignmentStatus.SCHEDULED && current.status() != AssignmentStatus.OPEN) {
            throw ApiException.createConflict("ASSIGNMENT_STATUS_INVALID", "Chỉ ngưng giao bài đã lên lịch hoặc đang mở");
        }
        Assignment saved = assignments.saveAssignment(current.markRetired(reason, clock.instant()));
        lifecycle.markRetired(saved.id());
        return support.buildDetail(actor, saved, List.of());
    }

    private void requireOwnerOpen(Assignment assignment) {
        if (assignment.scope() == AssignmentScope.CLASS) {
            if (access.loadClassRef(assignment.classId()).status() != ClassStatus.OPEN) {
                throw ApiException.createConflict("CLASS_NOT_OPEN", "Lớp chưa mở, chưa phát hành được bài");
            }
        } else {
            boolean active;
            try {
                active = subjects.isActive(assignment.subjectId());
            } catch (RuntimeException e) {
                throw ApiException.createUnavailable("ASSIGNMENT_DEPENDENCY_UNAVAILABLE", "Chưa đọc được môn học, thử lại sau");
            }
            if (!active) {
                throw ApiException.createConflict("SUBJECT_NOT_ACTIVE", "Môn không còn hoạt động, chưa phát hành được bài");
            }
        }
    }

    private void requireGroupsReady(UUID classId, boolean confirmed) {
        ReadinessResult readiness = support.groupReadiness(classId);
        if (!readiness.errors().isEmpty()) {
            throw new AssignmentProblem(HttpStatus.CONFLICT, "GROUP_NOT_READY", "Nhóm của lớp chưa sẵn sàng",
                    Map.of("errors", readiness.errors()));
        }
        if (!readiness.warnings().isEmpty() && !confirmed) {
            throw new AssignmentProblem(HttpStatus.CONFLICT, "GROUP_CONFIRMATION_REQUIRED",
                    "Có sinh viên chưa có nhóm, hãy xác nhận để phát hành", Map.of("warnings", readiness.warnings()));
        }
    }

    /** Bài có chấm không lưu số lượt (yêu cầu 2026-11-10): nộp không giới hạn khi còn hạn. */
    private static Integer resolveAttempts(Assignment assignment, Integer requested) {
        return Assignment.limitsAttempts(assignment.gradingMode()) ? requested : null;
    }

    /**
     * BR-U07-33: {@code opensAt < closesAt}, hạn chưa qua, {@code closesAt < lateUntil ≤ closesAt + 30 ngày}; bài Luyện tập
     * thêm số lượt 1–10, bài có chấm không có số lượt (yêu cầu 2026-11-10).
     */
    static void validateSchedule(Instant opensAt, Instant closesAt, Instant lateUntil, Integer maxAttempts,
                                 GradingMode gradingMode, Instant now) {
        if (Assignment.limitsAttempts(gradingMode)
                && (maxAttempts == null || maxAttempts < 1 || maxAttempts > Assignment.ATTEMPTS_MAX)) {
            throw ApiException.createBadRequest("SCHEDULE_INVALID", "Số lượt nộp tối đa từ 1 đến 10");
        }
        validateDeadlines(opensAt, closesAt, lateUntil, now);
    }

    private static void validateDeadlines(Instant opensAt, Instant closesAt, Instant lateUntil, Instant now) {
        validateOrder(opensAt, closesAt, lateUntil);
        if (!closesAt.isAfter(now)) {
            throw ApiException.createBadRequest("SCHEDULE_INVALID", "Hạn nộp phải ở tương lai");
        }
    }

    /** Ngày mở hẹn giờ không ở quá khứ (yêu cầu 2026-11-26); chừa 2 phút cho độ trễ mạng và đồng hồ máy. */
    static void requireNotPast(Instant opensAt, Instant now) {
        if (opensAt != null && opensAt.isBefore(now.minus(OPENS_AT_TOLERANCE))) {
            throw ApiException.createBadRequest("SCHEDULE_INVALID", "Ngày mở phải ở tương lai");
        }
    }

    static final java.time.Duration OPENS_AT_TOLERANCE = java.time.Duration.ofMinutes(2);

    private static void validateOrder(Instant opensAt, Instant closesAt, Instant lateUntil) {
        if (opensAt == null || closesAt == null) {
            throw ApiException.createBadRequest("SCHEDULE_INVALID", "Cần ngày mở và hạn nộp");
        }
        if (!closesAt.isAfter(opensAt)) {
            throw ApiException.createBadRequest("SCHEDULE_INVALID", "Hạn nộp phải sau ngày mở");
        }
        if (lateUntil != null && (!lateUntil.isAfter(closesAt) || lateUntil.isAfter(closesAt.plus(Assignment.LATE_MAX)))) {
            throw ApiException.createBadRequest("SCHEDULE_INVALID", "Hạn nộp trễ phải sau hạn nộp, không quá 30 ngày");
        }
    }

}
