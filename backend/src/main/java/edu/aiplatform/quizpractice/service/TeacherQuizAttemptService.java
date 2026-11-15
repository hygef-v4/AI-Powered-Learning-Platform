package edu.aiplatform.quizpractice.service;


import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignmentwork.contract.ActivityPolicyState;
import edu.aiplatform.assignmentwork.contract.ActivitySummary;
import edu.aiplatform.assignmentwork.contract.IActivityQuery;
import edu.aiplatform.assignmentwork.contract.WorkKind;
import edu.aiplatform.assignmentwork.contract.WorkRef;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.quizpractice.dto.PracticeViews.ClassAttempts;
import edu.aiplatform.quizpractice.dto.PracticeViews.StudentAttempts;
import edu.aiplatform.quizpractice.dto.PracticeViews.TeacherAttemptDetail;
import edu.aiplatform.quizpractice.repository.IQuizAttemptRepository;
import edu.aiplatform.quizpractice.model.ActivityOutcome;
import edu.aiplatform.quizpractice.model.ActivitySignal;
import edu.aiplatform.quizpractice.model.PenaltyDecision;
import edu.aiplatform.quizpractice.model.QuizAttempt;
import edu.aiplatform.quizzes.contract.QuizView;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giảng viên xem lượt Quiz của sinh viên lớp mình (UC 42 phần Quiz; F9; BR-U11-31): chỉ giảng viên của lớp, chỉ sửa được
 * quyết định trừ điểm (BR-U11-33). Dữ liệu hoạt động lấy qua {@link IActivityQuery} của U08; lỗi thì bỏ trống phần đó.
 */
@Service
public class TeacherQuizAttemptService {

    private static final int MAX_REASON = 1_000;

    private final IQuizAttemptRepository attempts;
    private final IClassAccess classes;
    private final IAccountLookup accounts;
    private final IActivityQuery activity;
    private final PracticeSupport support;
    private final PracticeAccess access;
    private final Clock clock;

    public TeacherQuizAttemptService(IQuizAttemptRepository attempts, IClassAccess classes, IAccountLookup accounts,
                                     IActivityQuery activity, PracticeSupport support, PracticeAccess access, Clock clock) {
        this.attempts = attempts;
        this.classes = classes;
        this.accounts = accounts;
        this.activity = activity;
        this.support = support;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ClassAttempts list(Actor actor, UUID classId, UUID quizId) {
        access.requireClass(actor, Actions.QUIZ_CLASS_MANAGE, classId, "QUIZ", quizId, PracticeSupport.QUIZ_NOT_FOUND);
        ClassRef ref = support.classRef(classId);
        QuizView quiz = support.quizInClass(quizId, ref);
        Map<UUID, List<QuizAttempt>> byStudent = attempts.listByQuizAndClass(quizId, classId).stream()
                .collect(Collectors.groupingBy(QuizAttempt::accountId));
        Set<UUID> students = new LinkedHashSet<>(PracticeSupport.call(() -> classes.listActiveStudents(classId)));
        students.addAll(byStudent.keySet());
        Map<UUID, AccountContact> contacts = contacts(students);
        List<StudentAttempts> rows = new ArrayList<>();
        for (UUID student : students) {
            List<QuizAttempt> own = byStudent.getOrDefault(student, List.of());
            AccountContact contact = contacts.get(student);
            List<QuizAttempt> submitted = own.stream().filter(a -> !a.inProgress()).toList();
            QuizAttempt last = submitted.stream().max(Comparator.comparing(QuizAttempt::submittedAt)).orElse(null);
            boolean warning = submitted.stream().anyMatch(a -> a.outcome() != null && a.outcome().signal() != null
                    && a.outcome().signal() != ActivitySignal.OK && a.outcome().signal() != ActivitySignal.DISABLED);
            boolean pending = submitted.stream().anyMatch(a -> a.outcome() != null && a.outcome().pendingDecision());
            rows.add(new StudentAttempts(student, contact == null ? null : contact.displayName(),
                    contact == null ? null : contact.email(), own.size(),
                    submitted.stream().map(QuizAttempt::score).filter(Objects::nonNull).max(Comparator.naturalOrder())
                            .orElse(null),
                    last == null ? null : last.score(), last == null ? null : last.submittedAt(),
                    own.stream().map(PracticeSupport::teacherRow).toList(),
                    last == null || last.outcome() == null ? null : last.outcome().leaveCount(), warning, pending,
                    contact == null ? null : contact.studentCode()));
        }
        rows.sort(Comparator.comparing((StudentAttempts s) -> s.attempts() == 0)
                .thenComparing(s -> s.displayName() == null ? "" : s.displayName()));
        int attempted = (int) rows.stream().filter(s -> s.attempts() > 0).count();
        return new ClassAttempts(quizId, classId, quiz.settings().maxAttempts(), attempted, rows.size(), rows);
    }

    @Transactional(readOnly = true)
    public TeacherAttemptDetail detail(Actor actor, UUID classId, UUID attemptId) {
        access.requireClass(actor, Actions.QUIZ_CLASS_MANAGE, classId, PracticeAccess.OBJECT, attemptId,
                PracticeSupport.ATTEMPT_NOT_FOUND);
        QuizAttempt attempt = attempts.find(attemptId)
                .filter(a -> a.classId().equals(classId))
                .orElseThrow(() -> ApiException.notFound(PracticeSupport.ATTEMPT_NOT_FOUND));
        if (attempt.inProgress()) {
            throw ApiException.conflict("ATTEMPT_IN_PROGRESS", "Sinh viên chưa nộp lượt này");
        }
        AccountContact contact = contact(attempt.accountId());
        ActivitySummary summary = null;
        if (attempt.snapshot().activityMonitoringEnabled()) {
            try {
                summary = activity.summary(new WorkRef(WorkKind.QUIZ_ATTEMPT, attemptId, classId), attempt.accountId());
                // U08 không biết giờ bắt đầu của lượt Quiz: sự kiện đã bị dọn thì báo hết hạn, không báo "chưa có dữ liệu".
                if (summary.policyState() == ActivityPolicyState.NO_DATA
                        && attempt.startedAt().isBefore(clock.instant().minus(activity.retention()))) {
                    summary = new ActivitySummary(ActivityPolicyState.EXPIRED, 0, Duration.ZERO, List.of(), 0,
                            "Dữ liệu đã quá thời hạn lưu và bị xóa.");
                }
            } catch (RuntimeException unavailable) {
                summary = null;
            }
        }
        return new TeacherAttemptDetail(support.result(attempt, true, false),
                contact == null ? null : contact.displayName(), summary,
                attempt.outcome() == null ? null : attempt.outcome().signal());
    }

    /**
     * Giảng viên quyết định trừ hay không trừ điểm của một lượt có mức gợi ý, bắt buộc có lý do (BR-U11-33): tín hiệu rời
     * trang có thể sai nên hệ thống không tự trừ. Giảng viên được chọn số điểm khác mức gợi ý, không quá điểm gốc, và được
     * đổi quyết định; mỗi lần đều ghi audit.
     */
    @Transactional
    public TeacherAttemptDetail decidePenalty(Actor actor, UUID classId, UUID attemptId, PenaltyDecision decision,
                                              BigDecimal points, String reason) {
        access.requireClass(actor, Actions.QUIZ_CLASS_MANAGE, classId, PracticeAccess.OBJECT, attemptId,
                PracticeSupport.ATTEMPT_NOT_FOUND);
        ActivityOutcome outcome = attempts.find(attemptId).filter(a -> a.classId().equals(classId))
                .orElseThrow(() -> ApiException.notFound(PracticeSupport.ATTEMPT_NOT_FOUND)).outcome();
        String clean = reason == null ? "" : reason.strip();
        if (clean.isEmpty() || clean.codePointCount(0, clean.length()) > MAX_REASON) {
            throw ApiException.badRequest("PENALTY_REASON_INVALID", "Cần lý do cho quyết định, tối đa 1.000 ký tự");
        }
        if (outcome == null || !outcome.decidable()) {
            throw notDecidable();
        }
        boolean apply = decision == PenaltyDecision.APPLIED;
        if (apply && (points == null || points.signum() <= 0 || points.stripTrailingZeros().scale() > 2
                || points.compareTo(outcome.rawScore()) > 0)) {
            throw ApiException.badRequest("PENALTY_POINTS_INVALID",
                    "Điểm trừ phải lớn hơn 0, không quá điểm gốc, tối đa 2 chữ số thập phân");
        }
        BigDecimal applied = apply ? points : BigDecimal.ZERO;
        if (!attempts.decidePenalty(attemptId, decision, applied, actor.accountId(), clock.instant(), clean)) {
            throw notDecidable();
        }
        access.success(actor, apply ? "QUIZ_PENALTY_APPLIED" : "QUIZ_PENALTY_DISMISSED", attemptId,
                Map.of("classId", classId.toString(), "points", applied.toPlainString(),
                        "suggestedPenalty", outcome.suggestedPenalty().toPlainString()));
        return detail(actor, classId, attemptId);
    }

    private static ApiException notDecidable() {
        return ApiException.conflict("PENALTY_NOT_DECIDABLE", "Lượt này không có mức trừ gợi ý");
    }

    /** Tên, email cả lớp trong một lần tra; U01 lỗi thì danh sách vẫn hiện, chỉ thiếu tên. */
    private Map<UUID, AccountContact> contacts(Set<UUID> accountIds) {
        try {
            return accounts.getContacts(accountIds);
        } catch (RuntimeException unavailable) {
            return Map.of();
        }
    }

    private AccountContact contact(UUID accountId) {
        try {
            return accounts.getContact(accountId).orElse(null);
        } catch (RuntimeException unavailable) {
            return null;
        }
    }
}
