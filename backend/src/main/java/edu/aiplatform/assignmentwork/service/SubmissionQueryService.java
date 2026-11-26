package edu.aiplatform.assignmentwork.service;

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
import edu.aiplatform.assignmentwork.repository.AttemptRepository;
import edu.aiplatform.assignmentwork.repository.AttemptRepository.AttemptStatusRow;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        GradingMode mode = assignments.loadAssignment(assignmentId).map(AssignmentView::gradingMode).orElse(GradingMode.GRADED);
        // Chỉ đọc cột trạng thái: U09 gọi hàm này cho từng bản chấm khi chốt hàng loạt (TC-PERF-011).
        List<AttemptStatusRow> rows = attempts.listStatusesOfClass(classId, List.of(assignmentId));
        return summarizeSubmissions(rows, classes.listActiveStudents(classId), mode);
    }

    /**
     * Như {@link #listSubmissions} cho nhiều bài của một lớp: đọc danh sách lớp, chế độ chấm và lượt làm mỗi thứ một lần
     * thay vì một lần cho mỗi bài (bảng điểm 30 bài, TC-PERF-009). Kết quả giữ thứ tự {@code assignmentIds}.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<StudentSubmissionStatus>> listSubmissionsByAssignment(Collection<UUID> assignmentIds,
                                                                              UUID classId) {
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, GradingMode> modes = new HashMap<>();
        for (AssignmentView view : assignments.listForClass(classId)) {
            if (view.gradingMode() != null) {
                modes.put(view.assignmentId(), view.gradingMode());
            }
        }
        Map<UUID, List<AttemptStatusRow>> rowsByAssignment = attempts.listStatusesOfClass(classId, assignmentIds)
                .stream().collect(Collectors.groupingBy(AttemptStatusRow::assignmentId));
        List<UUID> roster = classes.listActiveStudents(classId);
        Map<UUID, List<StudentSubmissionStatus>> result = new LinkedHashMap<>();
        for (UUID assignmentId : assignmentIds) {
            GradingMode mode = modes.containsKey(assignmentId) ? modes.get(assignmentId)
                    : assignments.loadAssignment(assignmentId).map(AssignmentView::gradingMode)
                            .orElse(GradingMode.GRADED);
            result.put(assignmentId, summarizeSubmissions(rowsByAssignment.getOrDefault(assignmentId, List.of()),
                    roster, mode));
        }
        return result;
    }

    /** Một dòng cho mỗi sinh viên đang học, cộng người còn lượt làm dù đã rời lớp. */
    private static List<StudentSubmissionStatus> summarizeSubmissions(List<AttemptStatusRow> rows, List<UUID> roster,
                                                                     GradingMode mode) {
        Map<UUID, List<AttemptStatusRow>> byStudent = rows.stream()
                .collect(Collectors.groupingBy(AttemptStatusRow::accountId));
        Set<UUID> students = new LinkedHashSet<>(roster);
        students.addAll(byStudent.keySet());
        List<StudentSubmissionStatus> result = new ArrayList<>();
        for (UUID accountId : students) {
            List<AttemptStatusRow> list = byStudent.getOrDefault(accountId, List.of());
            AttemptStatusRow latest = list.stream().max(Comparator.comparingInt(AttemptStatusRow::attemptNo))
                    .orElse(null);
            AttemptStatusRow lastSubmitted = list.stream().filter(a -> a.status() == AttemptStatus.SUBMITTED)
                    .max(Comparator.comparingInt(AttemptStatusRow::attemptNo)).orElse(null);
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
        return attempts.findAttempt(attemptId).filter(a -> a.status() == AttemptStatus.SUBMITTED)
                .map(a -> new SubmissionView(a.id(), a.assignmentId(), a.classId(), a.accountId(), a.content(),
                        a.receiptHash(), a.submittedAt(), a.late(), a.submitMode()));
    }

    /** Sinh viên đang học chưa có lượt nộp; bài nhóm dựa vào trạng thái nhóm của U12 (F13 bước 2). */
    @Override
    @Transactional(readOnly = true)
    public List<UUID> listStudentsWithoutSubmission(UUID assignmentId, UUID classId) {
        Optional<AssignmentView> view = assignments.loadAssignment(assignmentId);
        List<UUID> students = classes.listActiveStudents(classId);
        if (view.isPresent() && view.get().submissionMode() == SubmissionMode.GROUP) {
            List<UUID> missing = new ArrayList<>();
            for (UUID accountId : students) {
                GroupWorkStatus status = groupWork.statusFor(accountId, List.of(assignmentId))
                        .get(assignmentId);
                if (status == null || !status.submitted()) {
                    missing.add(accountId);
                }
            }
            return missing;
        }
        Set<UUID> submitted = attempts.listStatusesOfClass(classId, List.of(assignmentId)).stream()
                .filter(a -> a.status() == AttemptStatus.SUBMITTED).map(AttemptStatusRow::accountId)
                .collect(Collectors.toSet());
        return students.stream().filter(id -> !submitted.contains(id)).toList();
    }
}
