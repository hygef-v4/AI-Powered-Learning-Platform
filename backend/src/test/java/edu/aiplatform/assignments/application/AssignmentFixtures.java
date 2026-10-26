package edu.aiplatform.assignments.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
import edu.aiplatform.ai.contract.IAssignmentSourceTextExtractor;
import edu.aiplatform.ai.contract.IReferenceMaterial;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentLifecycleListener;
import edu.aiplatform.assignments.contract.IGroupAssignmentSupport;
import edu.aiplatform.assignments.contract.ReadinessResult;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.domain.AiDraftRepository;
import edu.aiplatform.assignments.domain.Assignment;
import edu.aiplatform.assignments.domain.AssignmentOrigin;
import edu.aiplatform.assignments.domain.AssignmentRepository;
import edu.aiplatform.assignments.domain.AssignmentRubric;
import edu.aiplatform.assignments.domain.PartCalculator;
import edu.aiplatform.assignments.domain.RubricRepository;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectRef;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Dựng các service U07 với repository giả trong bộ nhớ và contract giả của unit khác. */
final class AssignmentFixtures {

    static final Instant NOW = Instant.parse("2026-10-20T03:00:00Z");

    final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    final UUID classId = UUID.randomUUID();
    final UUID subjectId = UUID.randomUUID();
    final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    final Actor manager = new Actor(UUID.randomUUID(), Role.SUBJECT_MANAGER);
    final Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);

    final Map<UUID, Assignment> store = new LinkedHashMap<>();
    final Map<UUID, AssignmentRubric> rubricStore = new LinkedHashMap<>();

    final AssignmentRepository assignments = mock(AssignmentRepository.class);
    final RubricRepository rubricRepository = mock(RubricRepository.class);
    final AiDraftRepository drafts = mock(AiDraftRepository.class);
    final IAuthorization authorization = mock(IAuthorization.class);
    final IClassScope classScope = mock(IClassScope.class);
    final IClassAccess classes = mock(IClassAccess.class);
    final ISubjectQuery subjects = mock(ISubjectQuery.class);
    final IAudit audit = mock(IAudit.class);
    final IGroupAssignmentSupport groups = mock(IGroupAssignmentSupport.class);
    final ISettings settings = mock(ISettings.class);
    final IArtifact artifacts = mock(IArtifact.class);
    final IReferenceMaterial materials = mock(IReferenceMaterial.class);
    final IAssignmentSourceTextExtractor sourceTexts = mock(IAssignmentSourceTextExtractor.class);
    final IEventPublisher events = mock(IEventPublisher.class);
    final IAiTask aiTasks = mock(IAiTask.class);
    final IAssignmentLifecycleListener listener = mock(IAssignmentLifecycleListener.class);
    @SuppressWarnings("unchecked")
    final ObjectProvider<IAssignmentLifecycleListener> listeners = mock(ObjectProvider.class);
    final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    final AssignmentAudit assignmentAudit = new AssignmentAudit(audit, clock);
    final AssignmentAccess access = new AssignmentAccess(authorization, classScope, classes, subjects, assignmentAudit);
    final AssignmentSupport support = new AssignmentSupport(assignments, rubricRepository, access, groups, subjects);
    final DocumentModelService documents = new DocumentModelService(json);
    final RubricService rubrics = new RubricService(rubricRepository, assignments);
    final AssignmentLifecycle lifecycle = new AssignmentLifecycle(assignments, listeners, events);
    final AssignmentAuthoringService authoring = new AssignmentAuthoringService(assignments, rubrics, documents, access,
            support, assignmentAudit, artifacts, materials, clock);
    final AssignmentPublicationService publication = new AssignmentPublicationService(assignments, authoring, rubrics,
            access, support, lifecycle, assignmentAudit, subjects, clock);
    final AssignmentCopyService copies = new AssignmentCopyService(assignments, rubrics, access, support,
            assignmentAudit, classScope, materials, clock);
    final AssignmentAiDraftService aiDrafts = new AssignmentAiDraftService(assignments, drafts, rubrics, support, access,
            assignmentAudit, aiTasks, materials, json, artifacts, sourceTexts, transactions, clock);

    AssignmentFixtures() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.getClassRef(any())).thenAnswer(i -> Optional.of(classRef(i.getArgument(0), ClassStatus.OPEN)));
        when(subjects.getSubject(any())).thenAnswer(i -> Optional.of(new SubjectRef(i.getArgument(0), "S1", "Môn 1",
                SubjectStatus.ACTIVE, manager.accountId())));
        when(subjects.isActive(any())).thenReturn(true);
        when(groups.checkReadiness(any())).thenReturn(new ReadinessResult(List.of(IGroupAssignmentSupport.UNAVAILABLE),
                List.of()));
        when(settings.get(any())).thenReturn(Optional.empty());
        when(listeners.orderedStream()).thenAnswer(i -> Stream.of(listener));
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        wireAssignments();
        wireRubrics();
    }

    ClassRef classRef(UUID id, ClassStatus status) {
        String code = id == null ? "C0" : "C" + id.toString().substring(0, 4);
        return new ClassRef(id, subjectId, code, "Lớp", status, teacher.accountId(), false);
    }

    /** Bài lưu sẵn trong kho giả; rubric tạo theo phần, đầy đủ nếu {@code completeRubrics}. */
    Assignment put(AssignmentScope scope, AssignmentStatus status, DocumentModel skeleton, boolean completeRubrics) {
        Assignment a = new Assignment(UUID.randomUUID(), scope, scope == AssignmentScope.CLASS ? classId : null,
                subjectId, null, AssignmentOrigin.MANUAL, 1, SubmissionMode.INDIVIDUAL, true, GradingMode.GRADED,
                "Bài 1", "Hướng dẫn", skeleton, PartCalculator.compute(skeleton), Map.of(), List.of(), false, null,
                status, status == AssignmentStatus.DRAFT || status == AssignmentStatus.REVIEWED ? null : NOW.minusSeconds(3600),
                status == AssignmentStatus.DRAFT || status == AssignmentStatus.REVIEWED ? null : NOW.plusSeconds(86_400),
                null, status == AssignmentStatus.DRAFT || status == AssignmentStatus.REVIEWED ? null : 2, null,
                teacher.accountId(), NOW, NOW, 3);
        store.put(a.id(), a);
        a.parts().forEach(part -> {
            AssignmentRubric rubric = AssignmentRubric.empty(a.id(), part.partId(), NOW);
            if (completeRubrics) {
                rubric = rubric.withCriteria(completeCriteria(), NOW);
            }
            rubricStore.put(rubric.id(), rubric);
        });
        return a;
    }

    Assignment withStatus(Assignment a, AssignmentStatus status, Instant opensAt, Instant closesAt) {
        Assignment next = a.withSchedule(status, opensAt, closesAt, null, 2, NOW);
        store.put(next.id(), next);
        return next;
    }

    static List<RubricCriterion> completeCriteria() {
        return List.of(new RubricCriterion(UUID.randomUUID(), "Nội dung", "", CriterionKind.TEXT,
                List.of(new RubricItem(UUID.randomUUID(), "Đúng yêu cầu", new BigDecimal("5.00")))));
    }

    static DocumentBlock heading(String style, String text) {
        ObjectNode content = JsonNodeFactory.instance.objectNode().put("style", style).put("text", text);
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.HEADING, content);
    }

    static DocumentBlock paragraph(String text) {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.putArray("runs").addObject().put("text", text);
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.PARAGRAPH, content);
    }

    static DocumentBlock diagram(String xml) {
        ObjectNode content = JsonNodeFactory.instance.objectNode().put("diagramType", "CLASS").put("xml", xml).put("alt", "Lớp");
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.DIAGRAM, content);
    }

    List<AssignmentRubric> rubricsOf(UUID assignmentId) {
        return rubricStore.values().stream().filter(r -> r.assignmentId().equals(assignmentId)).toList();
    }

    void deny() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("OUT_OF_SCOPE"));
    }

    private void wireAssignments() {
        when(assignments.find(any())).thenAnswer(i -> Optional.ofNullable(store.get(i.<UUID>getArgument(0))));
        when(assignments.findForUpdate(any())).thenAnswer(i -> Optional.ofNullable(store.get(i.<UUID>getArgument(0))));
        doAnswer(i -> {
            Assignment a = i.getArgument(0);
            store.put(a.id(), a);
            return null;
        }).when(assignments).insert(any());
        when(assignments.save(any())).thenAnswer(i -> {
            Assignment a = i.getArgument(0);
            Assignment current = store.get(a.id());
            if (current == null || current.version() != a.version()) {
                throw new OptimisticLockingFailureException("assignment");
            }
            Assignment next = a.withVersion(a.version() + 1);
            store.put(next.id(), next);
            return next;
        });
        when(assignments.delete(any(), anyLong())).thenAnswer(i -> {
            Assignment current = store.get(i.<UUID>getArgument(0));
            if (current == null || current.version() != i.<Long>getArgument(1) || !current.editable()) {
                return 0;
            }
            store.remove(current.id());
            return 1;
        });
        when(assignments.open(any(), any())).thenAnswer(i -> {
            Assignment current = store.get(i.<UUID>getArgument(0));
            Instant now = i.getArgument(1);
            if (current == null || current.status() != AssignmentStatus.SCHEDULED || current.opensAt().isAfter(now)) {
                return 0;
            }
            store.put(current.id(), current.withStatus(AssignmentStatus.OPEN, now).withVersion(current.version() + 1));
            return 1;
        });
        when(assignments.hasOpenVersion(any())).thenAnswer(i -> store.values().stream().anyMatch(a ->
                i.getArgument(0).equals(a.sourceAssignmentId()) && a.origin() == AssignmentOrigin.NEW_VERSION
                        && a.editable()));
        when(assignments.listOfClass(any())).thenAnswer(i -> store.values().stream()
                .filter(a -> a.scope() == AssignmentScope.CLASS && i.getArgument(0).equals(a.classId())).toList());
        when(assignments.listForClass(any(), any())).thenAnswer(i -> store.values().stream()
                .filter(a -> (a.scope() == AssignmentScope.CLASS && i.getArgument(0).equals(a.classId()))
                        || (a.scope() == AssignmentScope.SUBJECT && a.published())).toList());
        when(assignments.listOfSubject(any())).thenAnswer(i -> store.values().stream()
                .filter(a -> a.scope() == AssignmentScope.SUBJECT).toList());
    }

    private void wireRubrics() {
        when(rubricRepository.listByAssignment(any())).thenAnswer(i -> rubricsOf(i.getArgument(0)));
        when(rubricRepository.find(any())).thenAnswer(i -> Optional.ofNullable(rubricStore.get(i.<UUID>getArgument(0))));
        doAnswer(i -> {
            AssignmentRubric r = i.getArgument(0);
            rubricStore.put(r.id(), r);
            return null;
        }).when(rubricRepository).insert(any());
        when(rubricRepository.save(any())).thenAnswer(i -> {
            AssignmentRubric r = i.getArgument(0);
            AssignmentRubric current = rubricStore.get(r.id());
            if (current == null || current.version() != r.version() || current.locked()) {
                throw new OptimisticLockingFailureException("rubric");
            }
            AssignmentRubric next = new AssignmentRubric(r.id(), r.assignmentId(), r.partId(), r.criteria(),
                    r.totalPoints(), r.lockedAt(), r.createdAt(), r.updatedAt(), r.version() + 1);
            rubricStore.put(next.id(), next);
            return next;
        });
        when(rubricRepository.deleteParts(any(), anyCollection())).thenAnswer(i -> {
            UUID id = i.getArgument(0);
            Collection<String> parts = i.getArgument(1);
            List<UUID> gone = rubricStore.values().stream().filter(r -> r.assignmentId().equals(id)
                    && parts.contains(r.partId()) && !r.locked()).map(AssignmentRubric::id).toList();
            gone.forEach(rubricStore::remove);
            return gone.size();
        });
        when(rubricRepository.lockAll(any(), any())).thenAnswer(i -> {
            UUID id = i.getArgument(0);
            Instant now = i.getArgument(1);
            int count = 0;
            for (AssignmentRubric r : List.copyOf(rubricStore.values())) {
                if (r.assignmentId().equals(id) && !r.locked()) {
                    rubricStore.put(r.id(), new AssignmentRubric(r.id(), r.assignmentId(), r.partId(), r.criteria(),
                            r.totalPoints(), now, r.createdAt(), now, r.version() + 1));
                    count++;
                }
            }
            return count;
        });
    }
}
