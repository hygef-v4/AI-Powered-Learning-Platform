package edu.aiplatform.assignmentwork.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.ai.contract.IAiTask;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.IDocx;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.IGroupWorkStatus;
import edu.aiplatform.assignmentwork.contract.ISubmissionListener;
import edu.aiplatform.assignmentwork.contract.SubmitMode;
import edu.aiplatform.assignmentwork.domain.AssignmentAttempt;
import edu.aiplatform.assignmentwork.domain.AttemptRepository;
import edu.aiplatform.assignmentwork.domain.AttemptSnapshot;
import edu.aiplatform.assignmentwork.domain.PracticeFeedbackRepository;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Dựng các service U08 với kho lượt làm giả trong bộ nhớ và contract giả của U01, U05, U07. */
final class WorkFixtures {

    static final Instant NOW = Instant.parse("2026-10-20T03:00:00Z");

    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    final UUID classId = UUID.randomUUID();
    final UUID subjectId = UUID.randomUUID();
    final UUID assignmentId = UUID.randomUUID();
    final Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);
    final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    final DocumentBlock teacherBlock = paragraph(BlockOrigin.TEACHER, "Đề");
    final DocumentModel skeleton = new DocumentModel(List.of(teacherBlock));

    final Map<UUID, AssignmentAttempt> store = new LinkedHashMap<>();
    AssignmentView view = view(AssignmentStatus.OPEN, SubmissionMode.INDIVIDUAL, GradingMode.GRADED,
            NOW.plus(Duration.ofDays(1)), null, 2);
    SubmissionWindow window = SubmissionWindow.ON_TIME;
    ValidationResult submitResult = new ValidationResult(List.of(), List.of());

    final AttemptRepository attempts = mock(AttemptRepository.class);
    final PracticeFeedbackRepository feedback = mock(PracticeFeedbackRepository.class);
    final IAuthorization authorization = mock(IAuthorization.class);
    final IClassAccess classes = mock(IClassAccess.class);
    final IAssignmentQuery assignments = mock(IAssignmentQuery.class);
    final IDocumentModel documents = mock(IDocumentModel.class);
    final IDocx docx = mock(IDocx.class);
    final IArtifact artifacts = mock(IArtifact.class);
    final IAudit audit = mock(IAudit.class);
    final IAiTask aiTasks = mock(IAiTask.class);
    final IGroupWorkStatus groupWork = mock(IGroupWorkStatus.class);
    final ISubmissionListener listener = mock(ISubmissionListener.class);
    @SuppressWarnings("unchecked")
    final ObjectProvider<ISubmissionListener> listeners = mock(ObjectProvider.class);
    final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    final WorkAudit workAudit;
    final WorkAccess access;
    final AttemptSubmitter submitter;
    final AttemptService attemptService;
    final StudentAssignmentService studentAssignments;
    final PracticeFeedbackService feedbackService;

    WorkFixtures() {
        workAudit = new WorkAudit(audit, clock);
        access = new WorkAccess(authorization, classes, assignments, attempts, workAudit);
        submitter = new AttemptSubmitter(attempts, assignments, documents, listeners, workAudit, json);
        attemptService = new AttemptService(access, attempts, submitter, assignments, documents, docx, artifacts, workAudit,
                clock, "http://localhost:8088");
        studentAssignments = new StudentAssignmentService(access, classes, assignments, groupWork, attempts, feedback,
                clock);
        feedbackService = new PracticeFeedbackService(access, attempts, feedback, assignments, documents, aiTasks,
                workAudit, json, transactions, clock, 20);
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.getClassRef(classId)).thenReturn(Optional.of(classRef(ClassStatus.OPEN)));
        when(classes.isActiveStudent(any(), any())).thenReturn(true);
        when(classes.listOpenClassesOf(any())).thenReturn(List.of(classRef(ClassStatus.OPEN)));
        when(classes.listActiveStudents(classId)).thenReturn(List.of(student.accountId()));
        when(assignments.get(any())).thenAnswer(i -> i.getArgument(0).equals(view.assignmentId())
                ? Optional.of(view) : Optional.empty());
        when(assignments.getContent(any())).thenAnswer(i -> Optional.of(new AssignmentContent(view.assignmentId(), 1,
                "Hướng dẫn", skeleton, List.of(new DocumentPart("DOCUMENT", null, List.of(), "Toàn bài")),
                Map.of(DiagramType.CLASS, 1), List.of())));
        when(assignments.submissionWindow(any(), any())).thenAnswer(i -> window);
        when(assignments.listForClass(any())).thenAnswer(i -> List.of(view));
        when(documents.sanitize(any())).thenAnswer(i -> i.getArgument(0));
        when(documents.validateForSave(any(), any())).thenReturn(new ValidationResult(List.of(), List.of()));
        when(documents.validateForSubmit(any(), any(), any())).thenAnswer(i -> submitResult);
        when(documents.toPlainText(any())).thenReturn("chữ");
        when(listeners.orderedStream()).thenAnswer(i -> Stream.of(listener));
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(groupWork.statusFor(any(), anyCollection())).thenReturn(Map.of());
        wireAttempts();
    }

    ClassRef classRef(ClassStatus status) {
        return new ClassRef(classId, subjectId, "C1", "Lớp 1", status, teacher.accountId(), false);
    }

    AssignmentView view(AssignmentStatus status, SubmissionMode mode, GradingMode grading, Instant closesAt,
                        Instant lateUntil, Integer maxAttempts) {
        return new AssignmentView(assignmentId, AssignmentScope.CLASS, classId, subjectId, 1, "Bài 1", mode, grading,
                true, status, NOW.minus(Duration.ofDays(1)), closesAt, lateUntil, maxAttempts, false, null, List.of());
    }

    AssignmentAttempt attempt(int no, AttemptStatus status, Instant deadline, boolean late) {
        AttemptSnapshot snapshot = AttemptSnapshot.of(view, Map.of(DiagramType.CLASS, 1));
        AssignmentAttempt a = new AssignmentAttempt(UUID.randomUUID(), assignmentId, classId, student.accountId(), no,
                snapshot, skeleton, 0, status, status == AttemptStatus.SUBMITTED ? SubmitMode.MANUAL : null, late,
                NOW.minus(Duration.ofHours(2)), deadline, NOW.minus(Duration.ofHours(1)),
                status == AttemptStatus.SUBMITTED ? NOW.minus(Duration.ofMinutes(30)) : null,
                status == AttemptStatus.SUBMITTED ? "hash-" + no : null, List.of());
        store.put(a.id(), a);
        return a;
    }

    static DocumentBlock paragraph(BlockOrigin origin, String text) {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.putArray("runs").addObject().put("text", text);
        return new DocumentBlock(UUID.randomUUID(), origin, BlockType.PARAGRAPH, content);
    }

    private static AssignmentAttempt copy(AssignmentAttempt a, DocumentModel content, long version, AttemptStatus status,
                                          SubmitMode mode, boolean late, Instant deadline, Instant savedAt,
                                          Instant submittedAt, String hash, List<edu.aiplatform.assignments.contract.ValidationIssue> warnings) {
        return new AssignmentAttempt(a.id(), a.assignmentId(), a.classId(), a.accountId(), a.attemptNo(), a.snapshot(),
                content, version, status, mode, late, a.startedAt(), deadline, savedAt, submittedAt, hash, warnings);
    }

    private void wireAttempts() {
        when(attempts.find(any())).thenAnswer(i -> Optional.ofNullable(store.get(i.<UUID>getArgument(0))));
        when(attempts.findForUpdate(any())).thenAnswer(i -> Optional.ofNullable(store.get(i.<UUID>getArgument(0))));
        doAnswer(i -> {
            AssignmentAttempt a = i.getArgument(0);
            store.put(a.id(), a);
            return null;
        }).when(attempts).insert(any());
        when(attempts.inProgress(any(), any())).thenAnswer(i -> store.values().stream()
                .filter(a -> a.assignmentId().equals(i.getArgument(0)) && a.accountId().equals(i.getArgument(1))
                        && a.inProgress()).findFirst());
        when(attempts.maxAttemptNo(any(), any())).thenAnswer(i -> store.values().stream()
                .filter(a -> a.assignmentId().equals(i.getArgument(0)) && a.accountId().equals(i.getArgument(1)))
                .mapToInt(AssignmentAttempt::attemptNo).max().orElse(0));
        when(attempts.listMine(any(UUID.class), any(UUID.class))).thenAnswer(i -> store.values().stream()
                .filter(a -> a.assignmentId().equals(i.getArgument(0)) && a.accountId().equals(i.getArgument(1)))
                .sorted(Comparator.comparingInt(AssignmentAttempt::attemptNo).reversed()).toList());
        when(attempts.listMine(any(UUID.class), anyCollection())).thenAnswer(i -> {
            Collection<UUID> ids = i.getArgument(1);
            return store.values().stream().filter(a -> a.accountId().equals(i.getArgument(0)) && ids.contains(a.assignmentId()))
                    .toList();
        });
        when(attempts.listOfClass(any(), any())).thenAnswer(i -> store.values().stream()
                .filter(a -> a.assignmentId().equals(i.getArgument(0)) && a.classId().equals(i.getArgument(1))).toList());
        when(attempts.inProgressOf(any())).thenAnswer(i -> store.values().stream()
                .filter(a -> a.assignmentId().equals(i.getArgument(0)) && a.inProgress()).map(AssignmentAttempt::id).toList());
        when(attempts.overdue(any(), anyInt())).thenAnswer(i -> store.values().stream()
                .filter(a -> a.inProgress() && a.deadlineAt().isBefore(i.getArgument(0))).map(AssignmentAttempt::id).toList());
        when(attempts.saveContent(any(), any(), anyLong(), any())).thenAnswer(i -> {
            AssignmentAttempt a = store.get(i.<UUID>getArgument(0));
            if (a == null || !a.inProgress() || a.contentVersion() != i.<Long>getArgument(2)) {
                return 0;
            }
            store.put(a.id(), copy(a, i.getArgument(1), a.contentVersion() + 1, a.status(), a.submitMode(), a.late(),
                    a.deadlineAt(), i.getArgument(3), a.submittedAt(), a.receiptHash(), a.submitWarnings()));
            return 1;
        });
        when(attempts.extendDeadline(any(), any())).thenAnswer(i -> {
            AssignmentAttempt a = store.get(i.<UUID>getArgument(0));
            Instant deadline = i.getArgument(1);
            if (a == null || !a.inProgress() || !a.deadlineAt().isBefore(deadline)) {
                return 0;
            }
            store.put(a.id(), copy(a, a.content(), a.contentVersion(), a.status(), a.submitMode(), a.late(), deadline,
                    a.lastSavedAt(), a.submittedAt(), a.receiptHash(), a.submitWarnings()));
            return 1;
        });
        when(attempts.submit(any(), any(), anyBoolean(), any(), any(), any())).thenAnswer(i -> {
            AssignmentAttempt a = store.get(i.<UUID>getArgument(0));
            if (a == null || !a.inProgress()) {
                return 0;
            }
            store.put(a.id(), copy(a, a.content(), a.contentVersion(), AttemptStatus.SUBMITTED, i.getArgument(1),
                    i.getArgument(2), a.deadlineAt(), a.lastSavedAt(), i.getArgument(4), i.getArgument(3), i.getArgument(5)));
            return 1;
        });
    }
}
