package edu.aiplatform.assignments.application;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.ReadinessResult;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.domain.Assignment;
import edu.aiplatform.assignments.domain.AssignmentRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
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
    private final AssignmentAuthoringService authoring;
    private final RubricService rubrics;
    private final AssignmentAccess access;
    private final AssignmentSupport support;
    private final AssignmentLifecycle lifecycle;
    private final AssignmentAudit audit;
    private final ISubjectQuery subjects;
    private final Clock clock;

    public AssignmentPublicationService(AssignmentRepository assignments, AssignmentAuthoringService authoring,
                                        RubricService rubrics, AssignmentAccess access, AssignmentSupport support,
                                        AssignmentLifecycle lifecycle, AssignmentAudit audit, ISubjectQuery subjects,
                                        Clock clock) {
        this.assignments = assignments;
        this.authoring = authoring;
        this.rubrics = rubrics;
        this.access = access;
        this.support = support;
        this.lifecycle = lifecycle;
        this.audit = audit;
        this.subjects = subjects;
        this.clock = clock;
    }

    /** F9: một transaction ghi lịch, khóa rubric, chuyển SCHEDULED; mở ngay nếu {@code opensAt} đã tới. */
    @Transactional
    public AssignmentViews.Detail publish(Actor actor, UUID id, AssignmentCommands.Publish command) {
        Assignment current = support.lock(actor, id, command.version(), "ASSIGNMENT_PUBLISHED");
        if (current.status() != AssignmentStatus.REVIEWED) {
            throw ApiException.conflict("ASSIGNMENT_STATUS_INVALID", "Chỉ phát hành bài đã duyệt");
        }
        List<ValidationIssue> issues = authoring.readinessIssues(current);
        if (!issues.isEmpty()) {
            throw AssignmentProblem.issues(HttpStatus.CONFLICT, "ASSIGNMENT_NOT_READY", "Bài chưa đủ để phát hành",
                    issues);
        }
        requireOwnerOpen(current);
        Instant now = clock.instant();
        Instant opensAt = command.openNow() ? now : command.opensAt();
        validateSchedule(opensAt, command.closesAt(), command.lateUntil(), command.maxAttempts(), now);
        if (current.submissionMode() == SubmissionMode.GROUP) {
            requireGroupsReady(current.classId(), command.confirmUngroupedStudents());
        }
        Assignment saved = assignments.save(current.withSchedule(AssignmentStatus.SCHEDULED, opensAt,
                command.closesAt(), command.lateUntil(), command.maxAttempts(), now));
        rubrics.lockAll(saved.id(), now);
        audit.success(actor, "ASSIGNMENT_PUBLISHED", saved.id(), AssignmentAudit.details("scope", saved.scope(),
                "classId", saved.classId(), "subjectId", saved.subjectId(), "opensAt", opensAt,
                "closesAt", saved.closesAt(), "lateUntil", saved.lateUntil(), "maxAttempts", saved.maxAttempts()));
        if (!opensAt.isAfter(now)) {
            lifecycle.open(saved.id(), now);
        }
        return support.detail(actor, assignments.find(saved.id()).orElseThrow(), List.of());
    }

    /** F11 sửa lịch (BR-U07-35): SCHEDULED sửa tự do; OPEN chỉ kéo dài hạn, không đổi số lượt. */
    @Transactional
    public AssignmentViews.Detail changeSchedule(Actor actor, UUID id, AssignmentCommands.Schedule command) {
        Assignment current = support.lock(actor, id, command.version(), "ASSIGNMENT_SCHEDULE_CHANGED");
        Instant now = clock.instant();
        Instant opensAt = command.opensAt() == null ? current.opensAt() : command.opensAt();
        Integer maxAttempts = command.maxAttempts() == null ? current.maxAttempts() : command.maxAttempts();
        Instant closesAt = command.closesAt() == null ? current.closesAt() : command.closesAt();
        Instant lateUntil = command.lateUntil();
        switch (current.status()) {
            case SCHEDULED -> validateSchedule(opensAt, closesAt, lateUntil, maxAttempts, now);
            case OPEN -> {
                if (!opensAt.equals(current.opensAt()) || !Objects.equals(maxAttempts, current.maxAttempts())) {
                    throw ApiException.badRequest("SCHEDULE_OPEN_LOCKED",
                            "Bài đang mở chỉ kéo dài hạn nộp, không đổi ngày mở và số lượt");
                }
                if (closesAt.isBefore(current.closesAt())
                        || (current.lateUntil() != null && (lateUntil == null || lateUntil.isBefore(current.lateUntil())))) {
                    throw ApiException.badRequest("SCHEDULE_SHORTENED", "Bài đang mở không rút ngắn hạn nộp");
                }
                // Đang trong hạn nộp trễ thì hạn nộp đã qua; chỉ cần hạn cuối nhận bài còn ở tương lai.
                validateOrder(opensAt, closesAt, lateUntil);
                if (!(lateUntil != null ? lateUntil : closesAt).isAfter(now)) {
                    throw ApiException.badRequest("SCHEDULE_INVALID", "Hạn nộp phải ở tương lai");
                }
            }
            default -> throw ApiException.conflict("ASSIGNMENT_STATUS_INVALID",
                    "Chỉ sửa lịch bài đã lên lịch hoặc đang mở");
        }
        Assignment saved = assignments.save(current.withSchedule(current.status(), opensAt, closesAt, lateUntil,
                maxAttempts, now));
        audit.success(actor, "ASSIGNMENT_SCHEDULE_CHANGED", saved.id(), AssignmentAudit.details(
                "before", schedule(current), "after", schedule(saved)));
        return support.detail(actor, saved, List.of());
    }

    /** F11 ngưng giao (BR-U07-36): lý do bắt buộc; listener U08, U12 tự nộp bài đang làm trong cùng transaction. */
    @Transactional
    public AssignmentViews.Detail retire(Actor actor, UUID id, AssignmentCommands.Retire command) {
        String reason = command.reason() == null ? "" : command.reason().strip();
        if (reason.isEmpty() || reason.length() > Assignment.REASON_MAX) {
            throw ApiException.badRequest("RETIRE_REASON_INVALID", "Nhập lý do ngưng giao, tối đa 500 ký tự");
        }
        Assignment current = assignments.findForUpdate(id).orElseThrow(() -> access.deny(actor, "ASSIGNMENT_RETIRED",
                AssignmentAudit.OBJECT_ASSIGNMENT, id, "NOT_FOUND"));
        access.requireManage(actor, current, "ASSIGNMENT_RETIRED");
        AssignmentSupport.requireVersion(current, command.version());
        if (current.status() != AssignmentStatus.SCHEDULED && current.status() != AssignmentStatus.OPEN) {
            throw ApiException.conflict("ASSIGNMENT_STATUS_INVALID", "Chỉ ngưng giao bài đã lên lịch hoặc đang mở");
        }
        Assignment saved = assignments.save(current.retired(reason, clock.instant()));
        lifecycle.retired(saved.id());
        audit.success(actor, "ASSIGNMENT_RETIRED", saved.id(), AssignmentAudit.details("reason", reason,
                "previousStatus", current.status()));
        return support.detail(actor, saved, List.of());
    }

    private void requireOwnerOpen(Assignment assignment) {
        if (assignment.scope() == AssignmentScope.CLASS) {
            if (access.classRef(assignment.classId()).status() != ClassStatus.OPEN) {
                throw ApiException.conflict("CLASS_NOT_OPEN", "Lớp chưa mở, chưa phát hành được bài");
            }
        } else {
            boolean active;
            try {
                active = subjects.isActive(assignment.subjectId());
            } catch (RuntimeException e) {
                throw ApiException.unavailable("ASSIGNMENT_DEPENDENCY_UNAVAILABLE", "Chưa đọc được môn học, thử lại sau");
            }
            if (!active) {
                throw ApiException.conflict("SUBJECT_NOT_ACTIVE", "Môn không còn hoạt động, chưa phát hành được bài");
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

    /** BR-U07-33: {@code opensAt < closesAt}, hạn chưa qua, lượt 1–10, {@code closesAt < lateUntil ≤ closesAt + 30 ngày}. */
    static void validateSchedule(Instant opensAt, Instant closesAt, Instant lateUntil, Integer maxAttempts,
                                 Instant now) {
        if (maxAttempts == null || maxAttempts < 1 || maxAttempts > Assignment.ATTEMPTS_MAX) {
            throw ApiException.badRequest("SCHEDULE_INVALID", "Số lượt nộp tối đa từ 1 đến 10");
        }
        validateDeadlines(opensAt, closesAt, lateUntil, now);
    }

    private static void validateDeadlines(Instant opensAt, Instant closesAt, Instant lateUntil, Instant now) {
        validateOrder(opensAt, closesAt, lateUntil);
        if (!closesAt.isAfter(now)) {
            throw ApiException.badRequest("SCHEDULE_INVALID", "Hạn nộp phải ở tương lai");
        }
    }

    private static void validateOrder(Instant opensAt, Instant closesAt, Instant lateUntil) {
        if (opensAt == null || closesAt == null) {
            throw ApiException.badRequest("SCHEDULE_INVALID", "Cần ngày mở và hạn nộp");
        }
        if (!closesAt.isAfter(opensAt)) {
            throw ApiException.badRequest("SCHEDULE_INVALID", "Hạn nộp phải sau ngày mở");
        }
        if (lateUntil != null && (!lateUntil.isAfter(closesAt) || lateUntil.isAfter(closesAt.plus(Assignment.LATE_MAX)))) {
            throw ApiException.badRequest("SCHEDULE_INVALID", "Hạn nộp trễ phải sau hạn nộp, không quá 30 ngày");
        }
    }

    private static Map<String, Object> schedule(Assignment a) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("opensAt", a.opensAt() == null ? null : a.opensAt().toString());
        out.put("closesAt", a.closesAt() == null ? null : a.closesAt().toString());
        out.put("lateUntil", a.lateUntil() == null ? "" : a.lateUntil().toString());
        out.put("maxAttempts", a.maxAttempts());
        return out;
    }
}
