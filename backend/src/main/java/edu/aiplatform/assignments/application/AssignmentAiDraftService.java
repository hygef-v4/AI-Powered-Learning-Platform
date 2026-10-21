package edu.aiplatform.assignments.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.ai.contract.AiPrompt;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskRequest;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTask;
import edu.aiplatform.ai.contract.IReferenceMaterial;
import edu.aiplatform.ai.contract.MaterialText;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.domain.AiDraftRepository;
import edu.aiplatform.assignments.domain.Assignment;
import edu.aiplatform.assignments.domain.AssignmentAiDraft;
import edu.aiplatform.assignments.domain.AssignmentRepository;
import edu.aiplatform.assignments.domain.PartCalculator;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * AI soạn bản nháp (F6; BR-U07-50…52; P5): đề xuất nằm riêng trong {@code assignment_ai_drafts}; chỉ lệnh Áp dụng kèm
 * {@code version} mới thay khung. Prompt chỉ có yêu cầu, cài đặt bài và chữ tài liệu đã chọn; không có dữ liệu người
 * học. Task gửi ngoài transaction; kết quả về theo {@code targetRef} của đề xuất.
 */
@Service
public class AssignmentAiDraftService {

    static final String TASK_TYPE = "ASSIGNMENT_DRAFT";
    static final String TARGET_PREFIX = "assignment-draft:";
    static final int MAX_REQUEST = 4_000;
    static final int MATERIAL_CHARS = 30_000;
    static final int MAX_OUTPUT_TOKENS = 8_000;

    private final AssignmentRepository assignments;
    private final AiDraftRepository drafts;
    private final RubricService rubrics;
    private final AssignmentSupport support;
    private final AssignmentAccess access;
    private final AssignmentAudit audit;
    private final IAiTask aiTasks;
    private final IReferenceMaterial materials;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public AssignmentAiDraftService(AssignmentRepository assignments, AiDraftRepository drafts, RubricService rubrics,
                                    AssignmentSupport support, AssignmentAccess access, AssignmentAudit audit,
                                    IAiTask aiTasks, IReferenceMaterial materials, ObjectMapper json,
                                    PlatformTransactionManager transactions, Clock clock) {
        this.assignments = assignments;
        this.drafts = drafts;
        this.rubrics = rubrics;
        this.support = support;
        this.access = access;
        this.audit = audit;
        this.aiTasks = aiTasks;
        this.materials = materials;
        this.json = json;
        this.transaction = new TransactionTemplate(transactions);
        this.clock = clock;
    }

    /** F6 bước 2–4: tạo đề xuất PENDING, gửi task AI; {@code requestRef} chống gửi trùng. */
    public AssignmentViews.AiDraft request(Actor actor, UUID id, AssignmentCommands.AiDraftRequest command) {
        String prompt = command.request() == null ? "" : command.request().strip();
        if (prompt.isEmpty() || prompt.length() > MAX_REQUEST) {
            throw ApiException.badRequest("AI_REQUEST_INVALID", "Nhập yêu cầu, tối đa 4.000 ký tự");
        }
        String requestRef = command.requestRef();
        if (requestRef == null || requestRef.isBlank() || requestRef.length() > 100) {
            throw ApiException.badRequest("REQUEST_REF_INVALID", "Thiếu mã yêu cầu");
        }
        if (command.source() == null || (command.source() == AiSource.PERSONAL) != (command.provider() != null)) {
            throw ApiException.badRequest("AI_SOURCE_INVALID", "Chọn nguồn AI");
        }
        List<UUID> materialIds = command.materialIds() == null ? List.of()
                : List.copyOf(new LinkedHashSet<>(command.materialIds()));
        if (materialIds.size() > Assignment.MATERIALS_MAX) {
            throw ApiException.badRequest("TOO_MANY_MATERIALS", "Chọn tối đa 10 tài liệu tham khảo");
        }
        Prepared prepared = transaction.execute(status -> {
            Assignment assignment = support.manageable(actor, id, "ASSIGNMENT_AI_DRAFT_REQUESTED");
            access.requireOwnerWritable(assignment);
            if (assignment.status() != AssignmentStatus.DRAFT) {
                throw ApiException.conflict("ASSIGNMENT_STATUS_INVALID", "Chỉ soạn bằng AI khi bài đang nháp");
            }
            var existing = drafts.findByRequest(id, requestRef);
            if (existing.isPresent()) {
                return new Prepared(assignment, existing.get(), false);
            }
            if (!materialIds.isEmpty()) {
                Set<UUID> allowed = selectable(actor, assignment).stream().map(SelectableMaterial::materialId)
                        .collect(Collectors.toSet());
                if (!allowed.containsAll(materialIds)) {
                    throw ApiException.badRequest("MATERIAL_NOT_ALLOWED", "Có tài liệu ngoài phạm vi của bài");
                }
            }
            ObjectNode provenance = JsonNodeFactory.instance.objectNode();
            provenance.put("requestedBy", actor.accountId().toString());
            provenance.put("origin", "AI_TASK");
            provenance.put("source", command.source().name());
            if (command.provider() != null) {
                provenance.put("provider", command.provider().name());
            }
            ArrayNode ids = provenance.putArray("materialIds");
            materialIds.forEach(m -> ids.add(m.toString()));
            AssignmentAiDraft draft = new AssignmentAiDraft(UUID.randomUUID(), id, AssignmentAiDraft.Origin.AI_TASK,
                    null, requestRef, AssignmentAiDraft.Status.PENDING, null, provenance, assignment.version(), null,
                    actor.accountId(), clock.instant(), null);
            drafts.insert(draft);
            return new Prepared(assignment, draft, true);
        });
        if (!prepared.created()) {
            return view(prepared.draft());
        }
        AssignmentAiDraft draft = prepared.draft();
        List<MaterialText> texts = materialIds.isEmpty() ? List.of() : texts(actor, materialIds);
        AiTaskView task;
        try {
            task = aiTasks.submit(new AiTaskRequest(actor, command.source(), command.provider(), TASK_TYPE,
                    TARGET_PREFIX + draft.id(), prepared.assignment().version(), requestRef,
                    prompt(prepared.assignment(), prompt, texts), AiDraftProposal.OUTPUT_SCHEMA, MAX_OUTPUT_TOKENS));
        } catch (ApiException e) {
            transaction.executeWithoutResult(s -> drafts.markFailed(draft.id(), e.getMessage(), clock.instant()));
            throw e;
        } catch (RuntimeException e) {
            transaction.executeWithoutResult(s -> drafts.markFailed(draft.id(), "AI chưa sẵn sàng", clock.instant()));
            return view(drafts.find(draft.id()).orElseThrow());
        }
        transaction.executeWithoutResult(s -> {
            ObjectNode provenance = draft.provenance().deepCopy();
            if (task.model() != null) {
                provenance.put("model", task.model());
            }
            ArrayNode used = provenance.putArray("materials");
            texts.forEach(t -> used.addObject().put("materialId", t.materialId().toString()).put("title", t.title())
                    .put("truncated", t.truncated()));
            drafts.attachTask(draft.id(), task.taskId(), provenance);
            if (task.state() == AiTaskState.FAILED || task.state() == AiTaskState.CANCELLED) {
                drafts.markFailed(draft.id(), task.safeError() == null ? "AI không trả kết quả" : task.safeError(),
                        clock.instant());
            }
        });
        return view(drafts.find(draft.id()).orElseThrow());
    }

    public AssignmentViews.AiDraft get(Actor actor, UUID id, UUID draftId) {
        return transaction.execute(status -> {
            support.manageable(actor, id, "ASSIGNMENT_AI_DRAFT_VIEW");
            return view(draft(id, draftId));
        });
    }

    /** F6 bước 6 (BR-U07-51): thay khung (và hướng dẫn nếu chọn), tính lại phần, điền gợi ý rubric. */
    public AssignmentViews.Detail apply(Actor actor, UUID id, UUID draftId, AssignmentCommands.ApplyDraft command) {
        return transaction.execute(status -> {
            Assignment current = support.lockEditable(actor, id, command.version(), "ASSIGNMENT_AI_DRAFT_APPLIED");
            AssignmentAiDraft draft = draft(id, draftId);
            if (draft.status() != AssignmentAiDraft.Status.READY || draft.proposal() == null) {
                throw ApiException.conflict("AI_DRAFT_NOT_READY", "Đề xuất chưa sẵn sàng hoặc đã xử lý");
            }
            AiDraftProposal.Built built = AiDraftProposal.build(draft.proposal());
            Instant now = clock.instant();
            List<DocumentPart> parts = PartCalculator.compute(built.skeleton());
            Map<String, List<RubricCriterion>> suggestions = new HashMap<>();
            for (DocumentPart part : parts) {
                if (part.headingBlockId() != null && built.rubricByHeading().containsKey(part.headingBlockId())) {
                    suggestions.put(part.partId(), RubricService.normalize(
                            built.rubricByHeading().get(part.headingBlockId()), current.diagramEnabled()));
                }
            }
            String instructions = command.replaceInstructions()
                    ? draft.proposal().path("instructions").asText("") : current.instructions();
            Assignment saved = assignments.save(current.withSkeleton(instructions, built.skeleton(), parts, now));
            rubrics.replaceAll(saved, suggestions, now);
            if (drafts.markApplied(draft.id(), now) != 1) {
                throw ApiException.conflict("AI_DRAFT_NOT_READY", "Đề xuất đã được xử lý");
            }
            audit.success(actor, "ASSIGNMENT_AI_DRAFT_APPLIED", saved.id(), AssignmentAudit.details(
                    "draftId", draft.id(), "origin", draft.origin(), "provenance", provenanceSummary(draft.provenance()),
                    "partCount", parts.size(), "replacedInstructions", command.replaceInstructions()));
            return support.detail(actor, saved, List.of());
        });
    }

    /** F6 bước 7: bỏ đề xuất, bài không đổi. */
    public AssignmentViews.AiDraft discard(Actor actor, UUID id, UUID draftId) {
        AssignmentAiDraft draft = transaction.execute(status -> {
            support.manageable(actor, id, "ASSIGNMENT_AI_DRAFT_DISCARDED");
            AssignmentAiDraft current = draft(id, draftId);
            if (drafts.markDiscarded(current.id(), clock.instant()) != 1) {
                throw ApiException.conflict("AI_DRAFT_NOT_READY", "Đề xuất đã được xử lý");
            }
            return current;
        });
        if (draft.status() == AssignmentAiDraft.Status.PENDING && draft.taskId() != null) {
            try {
                aiTasks.cancel(actor, draft.taskId());
            } catch (RuntimeException ignored) {
                // Hủy task là việc phụ; đề xuất đã bỏ.
            }
        }
        return view(drafts.find(draftId).orElseThrow());
    }

    /** Tool MCP {@code save_assignment_draft} (BR-U07-53): lưu đề xuất READY, không sửa thẳng bài. */
    AssignmentAiDraft saveFromAgent(Actor actor, UUID id, long expectedVersion, JsonNode raw, String agentLabel) {
        return transaction.execute(status -> {
            Assignment assignment = support.manageable(actor, id, "ASSIGNMENT_AI_DRAFT_SAVED");
            access.requireOwnerWritable(assignment);
            if (assignment.status() != AssignmentStatus.DRAFT) {
                throw ApiException.conflict("ASSIGNMENT_STATUS_INVALID", "Bài không còn nháp");
            }
            AssignmentSupport.requireVersion(assignment, expectedVersion);
            ObjectNode proposal = AiDraftProposal.normalize(raw, assignment.diagramEnabled());
            if (proposal == null) {
                throw ApiException.badRequest("AI_DRAFT_INVALID", "Đề xuất không đúng định dạng");
            }
            ObjectNode provenance = JsonNodeFactory.instance.objectNode();
            provenance.put("requestedBy", actor.accountId().toString());
            provenance.put("origin", "MCP");
            if (agentLabel != null && !agentLabel.isBlank()) {
                provenance.put("agentLabel", agentLabel.length() > 100 ? agentLabel.substring(0, 100) : agentLabel);
            }
            AssignmentAiDraft draft = new AssignmentAiDraft(UUID.randomUUID(), id, AssignmentAiDraft.Origin.MCP, null,
                    null, AssignmentAiDraft.Status.READY, proposal, provenance, assignment.version(), null,
                    actor.accountId(), clock.instant(), null);
            drafts.insert(draft);
            return draft;
        });
    }

    /** F6 bước 5: bài còn nháp thì lưu đề xuất READY; bài đã xóa hoặc phát hành thì REJECTED. */
    ApplyOutcome onReady(AiTaskResult result) {
        UUID draftId = draftId(result.targetRef());
        if (draftId == null) {
            return ApplyOutcome.REJECTED;
        }
        return transaction.execute(status -> {
            AssignmentAiDraft draft = drafts.find(draftId).orElse(null);
            if (draft == null || draft.status() != AssignmentAiDraft.Status.PENDING) {
                return ApplyOutcome.REJECTED;
            }
            Assignment assignment = assignments.find(draft.assignmentId()).orElse(null);
            if (assignment == null || assignment.status() != AssignmentStatus.DRAFT) {
                drafts.markFailed(draftId, "Bài không còn ở trạng thái nháp", clock.instant());
                return ApplyOutcome.REJECTED;
            }
            ObjectNode proposal;
            try {
                proposal = AiDraftProposal.normalize(json.readTree(result.output()), assignment.diagramEnabled());
            } catch (JsonProcessingException | RuntimeException e) {
                proposal = null;
            }
            if (proposal == null) {
                drafts.markFailed(draftId, "Kết quả AI không đúng định dạng", clock.instant());
                return ApplyOutcome.REJECTED;
            }
            ObjectNode provenance = draft.provenance().deepCopy();
            aiTasks.get(result.taskId()).ifPresent(task -> {
                if (task.model() != null) {
                    provenance.put("model", task.model());
                }
                if (task.provider() != null) {
                    provenance.put("provider", task.provider().name());
                }
            });
            return drafts.markReady(draftId, proposal, provenance) == 1 ? ApplyOutcome.APPLIED : ApplyOutcome.STALE;
        });
    }

    void onFailed(AiTaskFailure failure) {
        UUID draftId = draftId(failure.targetRef());
        if (draftId != null) {
            transaction.executeWithoutResult(status -> drafts.markFailed(draftId,
                    failure.safeError() == null ? "AI không trả kết quả" : failure.safeError(), clock.instant()));
        }
    }

    private AssignmentAiDraft draft(UUID assignmentId, UUID draftId) {
        return drafts.find(draftId).filter(d -> d.assignmentId().equals(assignmentId))
                .orElseThrow(() -> ApiException.notFound("AI_DRAFT_NOT_FOUND"));
    }

    private List<SelectableMaterial> selectable(Actor actor, Assignment assignment) {
        ResourceRef scope = assignment.scope() == AssignmentScope.CLASS
                ? ResourceRef.courseClass(assignment.classId()) : ResourceRef.subject(assignment.subjectId());
        try {
            List<SelectableMaterial> list = materials.listSelectable(actor, scope);
            return list == null ? List.of() : list;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private List<MaterialText> texts(Actor actor, List<UUID> ids) {
        try {
            List<MaterialText> list = materials.extractText(actor, ids, MATERIAL_CHARS);
            return list == null ? List.of() : list;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    static AiPrompt prompt(Assignment assignment, String request, List<MaterialText> texts) {
        String system = "Bạn là trợ lý soạn đề bài tập cho giảng viên đại học, viết tiếng Việt. Trả về đúng một JSON "
                + "theo schema. Đề gồm hướng dẫn chung và các mục theo cây tiêu đề cấp 1–6; dưới mỗi mục có đoạn "
                + "hướng dẫn cho sinh viên. Mục nhỏ nhất của mỗi nhánh kèm gợi ý rubric: tiêu chí và các mục chấm có "
                + "điểm dương. Không vẽ sơ đồ, không đưa dữ liệu cá nhân.";
        StringBuilder user = new StringBuilder("Yêu cầu của giảng viên:\n").append(request).append("\n\nCài đặt bài: ")
                .append(assignment.submissionMode() == edu.aiplatform.assignments.contract.SubmissionMode.GROUP
                        ? "bài nhóm" : "bài cá nhân")
                .append(", chế độ ").append(assignment.gradingMode().name())
                .append(assignment.diagramEnabled() ? ", có dùng sơ đồ Draw.io" : ", không dùng sơ đồ");
        if (!assignment.requiredDiagrams().isEmpty()) {
            user.append(", sơ đồ bắt buộc: ").append(assignment.requiredDiagrams());
        }
        if (!texts.isEmpty()) {
            user.append("\n\nTài liệu tham khảo:");
            for (MaterialText text : texts) {
                user.append("\n\n### ").append(text.title()).append('\n').append(text.text());
            }
        }
        return new AiPrompt(system, user.toString());
    }

    private AssignmentViews.AiDraft view(AssignmentAiDraft d) {
        return new AssignmentViews.AiDraft(d.id(), d.assignmentId(), d.origin().name(), d.status().name(), d.taskId(),
                d.proposal(), d.provenance(), d.safeError(), d.baseVersion(), d.createdAt(), d.decidedAt());
    }

    private static Map<String, Object> provenanceSummary(JsonNode provenance) {
        Map<String, Object> out = new HashMap<>();
        provenance.fields().forEachRemaining(e -> {
            if (e.getValue().isValueNode()) {
                out.put(e.getKey(), e.getValue().asText());
            }
        });
        return out;
    }

    private static UUID draftId(String targetRef) {
        if (targetRef == null || !targetRef.startsWith(TARGET_PREFIX)) {
            return null;
        }
        try {
            return UUID.fromString(targetRef.substring(TARGET_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record Prepared(Assignment assignment, AssignmentAiDraft draft, boolean created) {
    }
}
