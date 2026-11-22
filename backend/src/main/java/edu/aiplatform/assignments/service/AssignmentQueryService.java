package edu.aiplatform.assignments.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.AssignmentDayCount;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.repository.AssignmentRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cài {@link IAssignmentQuery} (F16): đọc bài, lịch, nội dung đã khóa; cửa sổ nộp; bài sắp hết hạn. */
@Service
public class AssignmentQueryService implements IAssignmentQuery {

    private final AssignmentRepository assignments;
    private final RubricService rubricService;
    private final IClassAccess classes;

    public AssignmentQueryService(AssignmentRepository assignments, RubricService rubricService, IClassAccess classes) {
        this.assignments = assignments;
        this.rubricService = rubricService;
        this.classes = classes;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentView> loadAssignment(UUID assignmentId) {
        return assignments.findAssignment(assignmentId).map(Assignment::view);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void lockSubmission(UUID assignmentId) {
        assignments.lockSubmission(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentContent> getContent(UUID assignmentId) {
        return assignments.findAssignment(assignmentId).map(a -> new AssignmentContent(a.id(), a.versionNo(), a.instructions(),
                a.skeleton(), a.parts(), a.requiredDiagrams(), rubricService.listRubrics(a.id())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentDayCount> countGivenByDay(Instant from, Instant to, ZoneId zone) {
        return assignments.countGivenByDay(from, to, zone);
    }

    /** Bài của lớp (mọi trạng thái) và bài của môn chứa lớp đã phát hành; unit gọi tự lọc theo trạng thái. */
    @Override
    @Transactional(readOnly = true)
    public List<AssignmentView> listForClass(UUID classId) {
        Optional<ClassRef> ref = classes.getClassRef(classId);
        if (ref.isEmpty()) {
            return List.of();
        }
        return assignments.listForClass(classId, ref.get().subjectId()).stream().map(Assignment::view).toList();
    }

    /** BR-U07-34: đúng hạn tới {@code closesAt}, trễ tới {@code lateUntil}, sau đó hoặc chưa mở là đóng. */
    @Override
    @Transactional(readOnly = true)
    public SubmissionWindow submissionWindow(UUID assignmentId, Instant now) {
        return assignments.findAssignment(assignmentId).map(a -> resolveWindow(a, now)).orElse(SubmissionWindow.CLOSED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentView> listClosingBetween(Instant from, Instant to) {
        return assignments.listClosingBetween(from, to).stream().map(Assignment::view).toList();
    }

    static SubmissionWindow resolveWindow(Assignment a, Instant now) {
        if (a.status() != AssignmentStatus.OPEN || a.opensAt() == null || now.isBefore(a.opensAt())) {
            return SubmissionWindow.CLOSED;
        }
        if (!now.isAfter(a.closesAt())) {
            return SubmissionWindow.ON_TIME;
        }
        if (a.lateUntil() != null && !now.isAfter(a.lateUntil())) {
            return SubmissionWindow.LATE;
        }
        return SubmissionWindow.CLOSED;
    }
}
