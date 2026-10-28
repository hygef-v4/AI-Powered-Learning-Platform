package edu.aiplatform.assignmentwork.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.GroupWorkStatus;
import edu.aiplatform.assignmentwork.contract.IGroupWorkStatus;
import edu.aiplatform.assignmentwork.domain.AssignmentAttempt;
import edu.aiplatform.assignmentwork.domain.AttemptRepository;
import edu.aiplatform.assignmentwork.domain.PracticeFeedbackRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Student Assignments, Assignment Detail, lịch sử nộp (F1, F2, F8; BR-U08-01…03, 30…32). */
@Service
public class StudentAssignmentService {

    private final WorkAccess access;
    private final IClassAccess classes;
    private final IAssignmentQuery assignments;
    private final IGroupWorkStatus groupWork;
    private final AttemptRepository attempts;
    private final PracticeFeedbackRepository feedback;
    private final Clock clock;

    public StudentAssignmentService(WorkAccess access, IClassAccess classes, IAssignmentQuery assignments,
                                    IGroupWorkStatus groupWork, AttemptRepository attempts,
                                    PracticeFeedbackRepository feedback, Clock clock) {
        this.access = access;
        this.classes = classes;
        this.assignments = assignments;
        this.groupWork = groupWork;
        this.attempts = attempts;
        this.feedback = feedback;
        this.clock = clock;
    }

    /** F1: bài của các lớp OPEN đang học, gồm bài của môn; sắp theo hạn gần nhất. */
    @Transactional(readOnly = true)
    public List<WorkViews.Row> list(Actor actor, UUID classId) {
        if (actor == null || actor.accountId() == null || actor.role() != Role.STUDENT) {
            throw access.deny(actor, "STUDENT_ASSIGNMENT_LIST", "ACCOUNT", actor == null ? null : actor.accountId(),
                    "NOT_STUDENT");
        }
        List<ClassRef> refs;
        if (classId != null) {
            refs = List.of(access.requireStudentOfClass(actor, classId, "STUDENT_ASSIGNMENT_LIST"));
        } else {
            refs = WorkAccess.call(() -> classes.listOpenClassesOf(actor.accountId())).stream()
                    .filter(ref -> access.isActiveStudent(actor.accountId(), ref.classId())).toList();
        }
        Map<UUID, ClassRef> classOf = new HashMap<>();
        Map<UUID, AssignmentView> views = new HashMap<>();
        for (ClassRef ref : refs) {
            for (AssignmentView view : WorkAccess.call(() -> assignments.listForClass(ref.classId()))) {
                if (WorkAccess.belongs(view, ref) && (view.status() == AssignmentStatus.OPEN
                        || view.status() == AssignmentStatus.CLOSED || view.status() == AssignmentStatus.RETIRED)) {
                    views.putIfAbsent(view.assignmentId(), view);
                    classOf.putIfAbsent(view.assignmentId(), ref);
                }
            }
        }
        Map<UUID, List<AssignmentAttempt>> mine = attempts.listMine(actor.accountId(), views.keySet()).stream()
                .collect(Collectors.groupingBy(AssignmentAttempt::assignmentId));
        List<UUID> groupIds = views.values().stream().filter(v -> v.submissionMode() == SubmissionMode.GROUP)
                .map(AssignmentView::assignmentId).toList();
        Map<UUID, GroupWorkStatus> groups = groupIds.isEmpty() ? Map.of() : groupStatus(actor.accountId(), groupIds);
        Instant now = clock.instant();
        List<WorkViews.Row> rows = new ArrayList<>();
        for (AssignmentView view : views.values()) {
            List<AssignmentAttempt> list = mine.getOrDefault(view.assignmentId(), List.of());
            boolean submitted = list.stream().anyMatch(a -> a.status() == AttemptStatus.SUBMITTED);
            if (view.status() == AssignmentStatus.RETIRED && !submitted) {
                continue;
            }
            rows.add(row(view, classOf.get(view.assignmentId()), list, groups.get(view.assignmentId()), now));
        }
        rows.sort(Comparator.comparing(WorkViews.Row::closesAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    /**
     * F2: trang thông tin trước khi làm bài: lịch, số lượt, lượt gần nhất, nút Bắt đầu/Tiếp tục. Không trả hướng dẫn,
     * khung đề, rubric (yêu cầu 2026-10-21 thay BR-U08-03): đề chỉ hiện khi đã vào lượt làm.
     */
    @Transactional(readOnly = true)
    public WorkViews.Detail detail(Actor actor, UUID assignmentId, UUID classId) {
        ClassRef ref = access.requireStudentOfClass(actor, classId, "STUDENT_ASSIGNMENT_VIEW");
        AssignmentView view = access.requireVisible(actor, ref, assignmentId, "STUDENT_ASSIGNMENT_VIEW");
        AssignmentContent content = WorkAccess.call(() -> assignments.getContent(assignmentId))
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND"));
        List<AssignmentAttempt> list = attempts.listMine(assignmentId, actor.accountId());
        Instant now = clock.instant();
        SubmissionWindow window = WorkAccess.call(() -> assignments.submissionWindow(assignmentId, now));
        AssignmentAttempt inProgress = list.stream().filter(AssignmentAttempt::inProgress).findFirst().orElse(null);
        String blocked = startBlocked(view, window, list.size(), inProgress);
        Map<UUID, Long> feedbackCounts = feedback.readyCounts(list.stream().map(AssignmentAttempt::id).toList());
        UUID graded = gradedAttemptId(view.gradingMode(), list);
        WorkViews.AttemptSummary latest = list.isEmpty() ? null
                : summary(list.get(0), graded, feedbackCounts.getOrDefault(list.get(0).id(), 0L));
        BigDecimal maxPoints = content.rubrics().stream().map(RubricView::maxPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new WorkViews.Detail(view.assignmentId(), ref.classId(), ref.code(), ref.name(), view.scope(),
                view.title(), view.submissionMode(), view.gradingMode(), view.diagramEnabled(), view.status(),
                view.opensAt(), view.closesAt(), view.lateUntil(), view.maxAttempts(), topLevelParts(content.parts(), content.rubrics()), maxPoints,
                content.requiredDiagrams(), view.activityMonitoringEnabled(),
                view.activityMonitoringEnabled() ? view.activityNotice() : null, view.referenceMaterialIds(),
                list.size(), latest, inProgress == null ? null : inProgress.id(),
                inProgress != null || blocked == null, blocked, window, now);
    }

    /** F8: các lượt của mình, lượt được chấm, số phản hồi AI (BR-U08-30, 31). */
    @Transactional(readOnly = true)
    public List<WorkViews.AttemptSummary> history(Actor actor, UUID assignmentId, UUID classId) {
        ClassRef ref = access.requireStudentOfClass(actor, classId, "SUBMISSION_HISTORY_VIEW");
        AssignmentView view = access.requireVisible(actor, ref, assignmentId, "SUBMISSION_HISTORY_VIEW");
        List<AssignmentAttempt> list = attempts.listMine(assignmentId, actor.accountId());
        Map<UUID, Long> counts = feedback.readyCounts(list.stream().map(AssignmentAttempt::id).toList());
        UUID graded = gradedAttemptId(view.gradingMode(), list);
        return list.stream().map(a -> summary(a, graded, counts.getOrDefault(a.id(), 0L))).toList();
    }

    /** Bài GRADED chấm lượt nộp cuối (BR-U08-30); Practice không có lượt được chấm. */
    /**
     * Số bài hiện cho người học: số nhánh con trực tiếp của gốc tài liệu (heading cấp cao nhất của mỗi phần, hoặc chính phần
     * nếu không có heading cha) mà nhánh đó hoặc con cháu của nó có rubric; khung không có heading là một bài nếu có rubric.
     */
    static int topLevelParts(List<DocumentPart> parts, List<RubricView> rubrics) {
        if (parts == null || parts.isEmpty() || rubrics == null || rubrics.isEmpty()) {
            return 0;
        }
        Set<String> graded = rubrics.stream().map(RubricView::partId).collect(Collectors.toSet());
        return (int) parts.stream()
                .filter(part -> graded.contains(part.partId()))
                .map(part -> part.ancestorHeadingIds().isEmpty() ? part.partId() : part.ancestorHeadingIds().get(0).toString())
                .distinct().count();
    }

    static UUID gradedAttemptId(GradingMode mode, List<AssignmentAttempt> attempts) {
        if (mode != GradingMode.GRADED) {
            return null;
        }
        return attempts.stream().filter(a -> a.status() == AttemptStatus.SUBMITTED)
                .max(Comparator.comparingInt(AssignmentAttempt::attemptNo)).map(AssignmentAttempt::id).orElse(null);
    }

    static String startBlocked(AssignmentView view, SubmissionWindow window, int used, AssignmentAttempt inProgress) {
        if (inProgress != null) {
            return null;
        }
        if (view.submissionMode() == SubmissionMode.GROUP) {
            return "GROUP_ASSIGNMENT";
        }
        if (window == SubmissionWindow.CLOSED) {
            return "SUBMISSION_CLOSED";
        }
        if (view.maxAttempts() != null && used >= view.maxAttempts()) {
            return "ATTEMPTS_EXHAUSTED";
        }
        return null;
    }

    static WorkViews.AttemptSummary summary(AssignmentAttempt a, UUID graded, long feedbackCount) {
        return new WorkViews.AttemptSummary(a.id(), a.attemptNo(), a.status(), a.startedAt(), a.deadlineAt(),
                a.lastSavedAt(), a.submittedAt(), a.late(), a.submitMode(), a.receiptHash(), a.id().equals(graded),
                feedbackCount, a.submitWarnings());
    }

    private WorkViews.Row row(AssignmentView view, ClassRef ref, List<AssignmentAttempt> list, GroupWorkStatus group,
                              Instant now) {
        AssignmentAttempt inProgress = list.stream().filter(AssignmentAttempt::inProgress).findFirst().orElse(null);
        List<AssignmentAttempt> submitted = list.stream().filter(a -> a.status() == AttemptStatus.SUBMITTED).toList();
        AssignmentAttempt last = submitted.stream().max(Comparator.comparingInt(AssignmentAttempt::attemptNo))
                .orElse(null);
        String status;
        if (view.submissionMode() == SubmissionMode.GROUP) {
            status = "GROUP";
        } else if (inProgress != null) {
            status = "IN_PROGRESS";
        } else if (last != null) {
            status = last.late() ? "SUBMITTED_LATE" : "SUBMITTED";
        } else if (view.status() != AssignmentStatus.OPEN || closed(view, now)) {
            status = "MISSED";
        } else {
            status = "NOT_STARTED";
        }
        return new WorkViews.Row(view.assignmentId(), ref.classId(), ref.code(), ref.name(), view.scope(), view.title(),
                view.submissionMode(), view.gradingMode(), view.diagramEnabled(), view.status(), view.opensAt(),
                view.closesAt(), view.lateUntil(), view.maxAttempts(), status, list.size(), submitted.size(),
                inProgress == null ? null : inProgress.id(), last == null ? null : last.submittedAt(),
                group == null ? null : new WorkViews.GroupWork(group.groupDocumentId(), group.submitted(),
                        group.submittedAt()));
    }

    private static boolean closed(AssignmentView view, Instant now) {
        Instant deadline = view.lateUntil() != null ? view.lateUntil() : view.closesAt();
        return deadline != null && now.isAfter(deadline);
    }

    /** U12 lỗi hoặc chưa có thì bài nhóm chỉ có nhãn (BR-U08-62). */
    private Map<UUID, GroupWorkStatus> groupStatus(UUID accountId, List<UUID> ids) {
        try {
            Map<UUID, GroupWorkStatus> result = groupWork.statusFor(accountId, ids);
            return result == null ? Map.of() : result;
        } catch (RuntimeException e) {
            return Map.of();
        }
    }
}
