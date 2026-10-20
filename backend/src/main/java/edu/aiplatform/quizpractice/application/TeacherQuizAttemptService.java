package edu.aiplatform.quizpractice.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignmentwork.contract.ActivitySummary;
import edu.aiplatform.assignmentwork.contract.IActivityQuery;
import edu.aiplatform.assignmentwork.contract.WorkKind;
import edu.aiplatform.assignmentwork.contract.WorkRef;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.quizpractice.application.PracticeViews.ClassAttempts;
import edu.aiplatform.quizpractice.application.PracticeViews.StudentAttempts;
import edu.aiplatform.quizpractice.application.PracticeViews.TeacherAttemptDetail;
import edu.aiplatform.quizpractice.domain.IQuizAttemptRepository;
import edu.aiplatform.quizpractice.domain.QuizAttempt;
import edu.aiplatform.quizzes.contract.QuizView;
import edu.aiplatform.shared.error.ApiException;
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
 * Giảng viên xem lượt Quiz của sinh viên lớp mình (UC 42 phần Quiz; F9; BR-U11-31): chỉ giảng viên của lớp, chỉ đọc,
 * không sửa kết quả. Dữ liệu hoạt động lấy qua {@link IActivityQuery} của U08; lỗi thì bỏ trống phần đó.
 */
@Service
public class TeacherQuizAttemptService {

    private final IQuizAttemptRepository attempts;
    private final IClassAccess classes;
    private final IAccountLookup accounts;
    private final IActivityQuery activity;
    private final PracticeSupport support;
    private final PracticeAccess access;

    public TeacherQuizAttemptService(IQuizAttemptRepository attempts, IClassAccess classes, IAccountLookup accounts,
                                     IActivityQuery activity, PracticeSupport support, PracticeAccess access) {
        this.attempts = attempts;
        this.classes = classes;
        this.accounts = accounts;
        this.activity = activity;
        this.support = support;
        this.access = access;
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
        List<StudentAttempts> rows = new ArrayList<>();
        for (UUID student : students) {
            List<QuizAttempt> own = byStudent.getOrDefault(student, List.of());
            AccountContact contact = contact(student);
            List<QuizAttempt> submitted = own.stream().filter(a -> !a.inProgress()).toList();
            QuizAttempt last = submitted.stream().max(Comparator.comparing(QuizAttempt::submittedAt)).orElse(null);
            rows.add(new StudentAttempts(student, contact == null ? null : contact.displayName(),
                    contact == null ? null : contact.email(), own.size(),
                    submitted.stream().map(QuizAttempt::score).filter(Objects::nonNull).max(Comparator.naturalOrder())
                            .orElse(null),
                    last == null ? null : last.score(), last == null ? null : last.submittedAt(),
                    own.stream().map(a -> PracticeSupport.row(a, true)).toList()));
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
            } catch (RuntimeException unavailable) {
                summary = null;
            }
        }
        return new TeacherAttemptDetail(support.result(attempt, true, false),
                contact == null ? null : contact.displayName(), summary);
    }

    private AccountContact contact(UUID accountId) {
        try {
            return accounts.getContact(accountId).orElse(null);
        } catch (RuntimeException unavailable) {
            return null;
        }
    }
}
