package edu.aiplatform.assignmentwork.application;

import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.GroupWorkStatus;
import edu.aiplatform.assignmentwork.contract.IGroupWorkStatus;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.assignmentwork.contract.StudentSubmissionStatus;
import edu.aiplatform.assignmentwork.contract.SubmissionView;
import edu.aiplatform.assignmentwork.domain.AssignmentAttempt;
import edu.aiplatform.assignmentwork.domain.AttemptRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link ISubmissionQuery} (F13): trạng thái nộp từng sinh viên và lượt được chấm (U09), bài nộp, sinh viên chưa
 * nộp để nhắc hạn (U14). Unit gọi tự kiểm quyền trước khi đọc.
 */
@Service
public class SubmissionQueryService implements ISubmissionQuery {

    private final AttemptRepository attempts;
    private final IAssignmentQuery assignments;
    private final IClassAccess classes;
    private final IGroupWorkStatus groupWork;

    public SubmissionQueryService(AttemptRepository attempts, IAssignmentQuery assignments, IClassAccess classes,
                                  IGroupWorkStatus groupWork) {
        this.attempts = attempts;
        this.assignments = assignments;
        this.classes = classes;
        this.groupWork = groupWork;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentSubmissionStatus> listSubmissions(UUID assignmentId, UUID classId) {
        GradingMode mode = assignments.get(assignmentId).map(AssignmentView::gradingMode).orElse(GradingMode.GRADED);
        Map<UUID, List<AssignmentAttempt>> byStudent = attempts.listOfClass(assignmentId, classId).stream()
                .collect(Collectors.groupingBy(AssignmentAttempt::accountId));
        Set<UUID> students = new LinkedHashSet<>(classes.listActiveStudents(classId));
        students.addAll(byStudent.keySet());
        List<StudentSubmissionStatus> result = new ArrayList<>();
        for (UUID accountId : students) {
            List<AssignmentAttempt> list = byStudent.getOrDefault(accountId, List.of());
            AssignmentAttempt latest = list.stream().max(Comparator.comparingInt(AssignmentAttempt::attemptNo))
                    .orElse(null);
            AssignmentAttempt lastSubmitted = list.stream().filter(a -> a.status() == AttemptStatus.SUBMITTED)
                    .max(Comparator.comparingInt(AssignmentAttempt::attemptNo)).orElse(null);
            UUID graded = mode == GradingMode.GRADED && lastSubmitted != null ? lastSubmitted.id() : null;
            result.add(new StudentSubmissionStatus(accountId, graded, list.size(),
                    latest == null ? null : latest.status(),
                    lastSubmitted == null ? null : lastSubmitted.submittedAt(),
                    lastSubmitted != null && lastSubmitted.late()));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubmissionView> getSubmission(UUID attemptId) {
        return attempts.find(attemptId).filter(a -> a.status() == AttemptStatus.SUBMITTED)
                .map(a -> new SubmissionView(a.id(), a.assignmentId(), a.classId(), a.accountId(), a.content(),
                        a.receiptHash(), a.submittedAt(), a.late(), a.submitMode()));
    }

    /** Sinh viên đang học chưa có lượt nộp; bài nhóm dựa vào trạng thái nhóm của U12 (F13 bước 2). */
    @Override
    @Transactional(readOnly = true)
    public List<UUID> listStudentsWithoutSubmission(UUID assignmentId, UUID classId) {
        Optional<AssignmentView> view = assignments.get(assignmentId);
        List<UUID> students = classes.listActiveStudents(classId);
        if (view.isPresent() && view.get().submissionMode() == SubmissionMode.GROUP) {
            List<UUID> missing = new ArrayList<>();
            for (UUID accountId : students) {
                GroupWorkStatus status;
                try {
                    status = groupWork.statusFor(accountId, List.of(assignmentId)).get(assignmentId);
                } catch (RuntimeException e) {
                    status = null;
                }
                if (status == null || !status.submitted()) {
                    missing.add(accountId);
                }
            }
            return missing;
        }
        Set<UUID> submitted = attempts.listOfClass(assignmentId, classId).stream()
                .filter(a -> a.status() == AttemptStatus.SUBMITTED).map(AssignmentAttempt::accountId)
                .collect(Collectors.toSet());
        return students.stream().filter(id -> !submitted.contains(id)).toList();
    }
}
