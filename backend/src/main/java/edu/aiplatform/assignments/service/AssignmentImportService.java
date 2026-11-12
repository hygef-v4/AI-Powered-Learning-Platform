package edu.aiplatform.assignments.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.ai.contract.AiPrompt;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiSourceChoice;
import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskRequest;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTask;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentFiles;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.assignments.dto.AssignmentViews;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentAiDraft;
import edu.aiplatform.assignments.model.ImportPage;
import edu.aiplatform.assignments.repository.AiDraftRepository;
import edu.aiplatform.assignments.repository.AssignmentRepository;
import edu.aiplatform.assignments.repository.ImportPageRepository;
import edu.aiplatform.assignments.service.ImportFileReader.CheckedFiles;
import edu.aiplatform.assignments.service.ImportFileReader.ImportFile;
import edu.aiplatform.assignments.service.ImportPageNormalizer.NormalizedPages;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Nhập đề cũ từ ảnh/PDF thành khung đề chỉnh sửa được (BR-U07-87…101): kiểm tệp, chuẩn hóa trang, tạo bản nháp AI chế
 * độ {@code IMPORT}, gửi tác vụ đọc ảnh; nhận kết quả thì cắt hình và dựng khung đề để xem trước; ghi đề khi người dùng
 * chọn Thay toàn bộ hoặc Chèn vào cuối.
 */
@Service
public class AssignmentImportService {

    static final String TASK_TYPE = "ASSIGNMENT_IMPORT";
    static final String MODE = "IMPORT";
    static final Duration PAGE_TTL = Duration.ofHours(24);
    static final int MAX_REQUEST_REF = 100;
    static final String VISION_UNAVAILABLE = "Chưa có model AI đọc được ảnh; hãy cấu hình hoặc chọn model khác";

    private final AssignmentRepository assignments;
    private final AiDraftRepository drafts;
    private final ImportPageRepository importPages;
    private final AssignmentSupport support;
    private final AssignmentAccess access;
    private final AssignmentAudit audit;
    private final IAiTask aiTasks;
    private final ImportFileReader importFileReader;
    private final ImportPageNormalizer importPageNormalizer;
    private final FigureCropper figureCropper;
    private final AssignmentAuthoringService assignmentAuthoringService;
    private final DocumentModelService documentModelService;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public AssignmentImportService(AssignmentRepository assignments, AiDraftRepository drafts,
                                   ImportPageRepository importPages, AssignmentSupport support,
                                   AssignmentAccess access, AssignmentAudit audit, IAiTask aiTasks,
                                   ImportFileReader importFileReader, ImportPageNormalizer importPageNormalizer,
                                   FigureCropper figureCropper, AssignmentAuthoringService assignmentAuthoringService,
                                   DocumentModelService documentModelService, ObjectMapper json,
                                   PlatformTransactionManager transactions, Clock clock) {
        this.assignments = assignments;
        this.drafts = drafts;
        this.importPages = importPages;
        this.support = support;
        this.access = access;
        this.audit = audit;
        this.aiTasks = aiTasks;
        this.importFileReader = importFileReader;
        this.importPageNormalizer = importPageNormalizer;
        this.figureCropper = figureCropper;
        this.assignmentAuthoringService = assignmentAuthoringService;
        this.documentModelService = documentModelService;
        this.json = json;
        this.transaction = new TransactionTemplate(transactions);
        this.clock = clock;
    }

    public record StartImport(String requestRef, AiSource source, AiProvider provider) {
    }

    /** F-IMP1: kiểm và chuẩn hóa tệp trước khi tạo bản nháp; lỗi tệp không tạo gì và không tốn quota. */
    public AssignmentViews.AiDraft startImport(Actor actor, UUID id, List<ImportFile> files, StartImport command) {
        String requestRef = command.requestRef() == null ? "" : command.requestRef().strip();
        if (requestRef.isEmpty() || requestRef.length() > MAX_REQUEST_REF) {
            throw ApiException.badRequest("REQUEST_REF_INVALID", "Thiếu mã yêu cầu");
        }
        if (command.provider() != null && command.source() != AiSource.PERSONAL) {
            throw ApiException.badRequest("AI_SOURCE_INVALID", "Nguồn AI không hợp lệ");
        }
        Assignment assignment = transaction.execute(status -> requireImportable(actor, id));
        var existing = drafts.findByRequest(id, requestRef);
        if (existing.isPresent()) {
            return AssignmentAiDraftService.view(existing.get());
        }
        CheckedFiles checked = importFileReader.readFiles(actor, files);
        NormalizedPages normalized = importPageNormalizer.normalizePages(checked);
        UUID draftId = UUID.randomUUID();
        AiPrompt prompt = ExamImportPrompt.buildPrompt(draftId, normalized.pages());
        List<AiSourceChoice> order = sourceOrder(actor, command, prompt);
        if (order.isEmpty()) {
            throw ApiException.badRequest("AI_VISION_UNAVAILABLE", VISION_UNAVAILABLE);
        }
        Instant now = clock.instant();
        ObjectNode provenance = JsonNodeFactory.instance.objectNode();
        provenance.put("requestedBy", actor.accountId().toString());
        provenance.put("origin", "AI_TASK");
        provenance.put("mode", MODE);
        provenance.put("source", command.source() == null ? "AUTO" : command.source().name());
        ObjectNode summary = provenance.putObject("import");
        summary.put("fileKind", checked.kind().name());
        summary.put("pageCount", normalized.pages().size());
        summary.put("totalBytes", checked.totalBytes());
        ArrayNode pageWarnings = provenance.putArray("pageWarnings");
        normalized.warnings().forEach(w -> pageWarnings.addObject().put("kind", w.kind()).put("page", w.page())
                .put("message", w.message()));
        AssignmentAiDraft draft = new AssignmentAiDraft(draftId, id, AssignmentAiDraft.Origin.AI_TASK, null,
                requestRef, AssignmentAiDraft.Status.PENDING, null, provenance, assignment.version(), null,
                actor.accountId(), now, null);
        transaction.executeWithoutResult(status -> {
            drafts.insert(draft);
            importPages.insertPages(draftId, normalized.pages(), now);
            audit.success(actor, "ASSIGNMENT_IMPORT_REQUESTED", id, AssignmentAudit.details("draftId", draftId,
                    "fileKind", checked.kind().name(), "pageCount", normalized.pages().size(),
                    "totalBytes", checked.totalBytes()));
        });
        AiTaskView task = null;
        AiSourceChoice picked = null;
        for (int index = 0; index < order.size() && task == null; index++) {
            AiSourceChoice choice = order.get(index);
            boolean last = index == order.size() - 1;
            try {
                task = aiTasks.submit(new AiTaskRequest(actor, choice.source(), choice.provider(), TASK_TYPE,
                        AssignmentAiDraftService.TARGET_PREFIX + draftId, assignment.version(), requestRef, prompt,
                        ExamImportPrompt.OUTPUT_SCHEMA, ExamImportPrompt.MAX_OUTPUT_TOKENS));
                picked = choice;
            } catch (RuntimeException e) {
                if (!last) {
                    continue;
                }
                failDraft(draftId, e instanceof ApiException api ? api.getMessage() : "AI chưa sẵn sàng");
                if (e instanceof ApiException api) {
                    throw api;
                }
                return AssignmentAiDraftService.view(drafts.find(draftId).orElseThrow());
            }
        }
        AiTaskView submitted = task;
        AiSourceChoice chosen = picked;
        transaction.executeWithoutResult(status -> {
            ObjectNode next = drafts.find(draftId).orElseThrow().provenance().deepCopy();
            next.put("source", chosen.source().name());
            if (chosen.provider() != null) {
                next.put("provider", chosen.provider().name());
            }
            if (submitted.model() != null) {
                next.put("model", submitted.model());
            }
            drafts.attachTask(draftId, submitted.taskId(), next);
        });
        if (submitted.state() == AiTaskState.FAILED || submitted.state() == AiTaskState.CANCELLED) {
            failDraft(draftId, submitted.safeError() == null ? "AI không trả kết quả" : submitted.safeError());
        }
        return AssignmentAiDraftService.view(drafts.find(draftId).orElseThrow());
    }

    private Assignment requireImportable(Actor actor, UUID id) {
        Assignment assignment = support.manageable(actor, id, "ASSIGNMENT_IMPORT_REQUESTED");
        access.requireOwnerWritable(assignment);
        if (!assignment.editable()) {
            throw ApiException.conflict("ASSIGNMENT_NOT_EDITABLE", "Bài đã phát hành, không nhập đề được");
        }
        return assignment;
    }

    /** Nguồn chọn đích danh, hoặc thứ tự tự chọn của U06 (đã chỉ còn model đọc được ảnh, BR-U07-100). */
    private List<AiSourceChoice> sourceOrder(Actor actor, StartImport command, AiPrompt prompt) {
        if (command.source() != null && command.provider() != null) {
            return List.of(new AiSourceChoice(command.source(), command.provider()));
        }
        List<AiSourceChoice> order = aiTasks.sourceOrder(actor, prompt, ExamImportPrompt.MAX_OUTPUT_TOKENS);
        if (command.source() == null) {
            return order;
        }
        return order.stream().filter(choice -> choice.source() == command.source()).toList();
    }

    /** F-IMP3: cắt hình ngoài transaction (lưu lên kho tệp), rồi ghi đề xuất READY và xóa ảnh trang. */
    ApplyOutcome acceptResult(AiTaskResult result) {
        UUID draftId = AssignmentAiDraftService.draftId(result.targetRef());
        if (draftId == null) {
            return ApplyOutcome.REJECTED;
        }
        AssignmentAiDraft draft = drafts.find(draftId).orElse(null);
        if (draft == null || draft.status() != AssignmentAiDraft.Status.PENDING) {
            importPages.deletePages(draftId);
            return ApplyOutcome.REJECTED;
        }
        Map<Integer, ImportPage> pages = importPages.listPages(draftId).stream()
                .collect(Collectors.toMap(ImportPage::pageNo, Function.identity()));
        ObjectNode proposal;
        try {
            JsonNode output = json.readTree(result.output());
            ExamImportMapper.MappedImport mapped = ExamImportMapper.mapResult(output, pages,
                    draft.provenance().path("pageWarnings"),
                    (page, box) -> figureCropper.cropFigure(result.actor(), page, box));
            if (mapped.document().blocks().isEmpty()) {
                return rejectDraft(draftId, "AI không đọc được nội dung nào trong đề, hãy thử lại");
            }
            ValidationResult check = documentModelService.validateSkeleton(mapped.document());
            if (!check.ok()) {
                return rejectDraft(draftId, "AI trả nội dung không đọc được, hãy thử lại");
            }
            proposal = mapped.proposal();
            proposal.set("document", json.valueToTree(mapped.document()));
        } catch (JsonProcessingException | RuntimeException e) {
            return rejectDraft(draftId, "AI trả nội dung không đọc được, hãy thử lại");
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
        ObjectNode ready = proposal;
        return transaction.execute(status -> {
            int updated = drafts.markReady(draftId, ready, provenance);
            importPages.deletePages(draftId);
            return updated == 1 ? ApplyOutcome.APPLIED : ApplyOutcome.STALE;
        });
    }

    void rejectResult(AiTaskFailure failure) {
        UUID draftId = AssignmentAiDraftService.draftId(failure.targetRef());
        if (draftId != null) {
            failDraft(draftId, failure.safeError() == null ? "AI không trả kết quả" : failure.safeError());
        }
    }

    private ApplyOutcome rejectDraft(UUID draftId, String message) {
        failDraft(draftId, message);
        return ApplyOutcome.REJECTED;
    }

    private void failDraft(UUID draftId, String message) {
        transaction.executeWithoutResult(status -> {
            drafts.markFailed(draftId, message, clock.instant());
            importPages.deletePages(draftId);
        });
    }

    /** Bỏ bản nháp nhập: xóa ảnh trang còn lại (BR-U07-99). */
    void discardPages(UUID draftId) {
        importPages.deletePages(draftId);
    }

    /**
     * Bản nháp nhập đề READY: thêm {@code proposal.previews} (id khối → link tải 5 phút) cho hình đã cắt chưa gắn,
     * để màn xem trước hiện được hình (FR-IMP-05.1). Không lưu; mỗi lần đọc cấp link mới.
     */
    static AssignmentViews.AiDraft withPreviews(AssignmentViews.AiDraft view, Actor actor, IArtifact artifacts) {
        if (artifacts == null || !"READY".equals(view.status()) || !isImport(view.proposal())) {
            return view;
        }
        ObjectNode proposal = view.proposal().deepCopy();
        ObjectNode previews = proposal.putObject("previews");
        for (JsonNode block : proposal.path("document").path("blocks")) {
            String fileRef = block.path("content").path(DocumentFiles.FILE_REF).asText("");
            if ("IMAGE".equals(block.path("type").asText()) && !fileRef.isEmpty()) {
                artifacts.issuePreviewToken(new FileRef(fileRef), actor)
                        .ifPresent(token -> previews.put(block.path("id").asText(), token.url()));
            }
        }
        return new AssignmentViews.AiDraft(view.draftId(), view.assignmentId(), view.origin(), view.status(),
                view.taskId(), proposal, view.provenance(), view.safeError(), view.baseVersion(), view.createdAt(),
                view.decidedAt());
    }

    static boolean isImport(JsonNode proposal) {
        return proposal != null && ExamImportMapper.KIND.equals(proposal.path("kind").asText());
    }

    /** F-IMP4 (BR-U07-98): ghi đề nhập vào khung, thay toàn bộ hoặc nối sau khối hiện có. */
    AssignmentViews.Detail applyImport(Actor actor, UUID id, UUID draftId, AssignmentCommands.ApplyDraft command) {
        String placement = command.placement() == null ? "REPLACE" : command.placement();
        if (!"REPLACE".equals(placement) && !"APPEND".equals(placement)) {
            throw ApiException.badRequest("IMPORT_PLACEMENT_INVALID", "Cách ghi đề không hợp lệ");
        }
        try {
            return transaction.execute(status -> {
                Assignment current = support.lockEditable(actor, id, command.version(), "ASSIGNMENT_IMPORT_APPLIED");
                AssignmentAiDraft draft = drafts.find(draftId).filter(d -> d.assignmentId().equals(id))
                        .orElseThrow(() -> ApiException.notFound("AI_DRAFT_NOT_FOUND"));
                if (draft.status() != AssignmentAiDraft.Status.READY || !isImport(draft.proposal())) {
                    throw ApiException.conflict("AI_DRAFT_NOT_READY", "Đề xuất chưa sẵn sàng hoặc đã xử lý");
                }
                DocumentModel imported = readDocument(draft.proposal().path("document"));
                List<DocumentBlock> blocks = new ArrayList<>();
                if ("APPEND".equals(placement) && current.skeleton() != null) {
                    blocks.addAll(current.skeleton().blocks());
                }
                blocks.addAll(imported.blocks());
                DocumentModel skeleton = new DocumentModel(List.copyOf(blocks),
                        current.skeleton() == null ? null : current.skeleton().title());
                String title = command.replaceTitle() ? draft.proposal().path("title").asText("") : null;
                AssignmentViews.Detail detail = assignmentAuthoringService.saveImportedSkeleton(actor, id,
                        command.version(), skeleton, title);
                if (drafts.markApplied(draftId, clock.instant()) != 1) {
                    throw ApiException.conflict("AI_DRAFT_NOT_READY", "Đề xuất đã được xử lý");
                }
                audit.success(actor, "ASSIGNMENT_IMPORT_APPLIED", id, AssignmentAudit.details("draftId", draftId,
                        "placement", placement, "blockCount", imported.blocks().size(),
                        "imageCount", draft.proposal().path("counts").path("images").asInt(0),
                        "replacedTitle", title != null && !title.isBlank()));
                return detail;
            });
        } catch (ApiException e) {
            if ("FILE_REF_EXPIRED".equals(e.code()) || "FILE_REF_INVALID".equals(e.code())) {
                throw ApiException.conflict("AI_DRAFT_EXPIRED",
                        "Hình trong bản nhập đã quá hạn; hãy nhập lại tệp đề");
            }
            throw e;
        }
    }

    private DocumentModel readDocument(JsonNode node) {
        try {
            DocumentModel document = json.treeToValue(node, DocumentModel.class);
            if (document == null || document.blocks() == null || document.blocks().isEmpty()) {
                throw ApiException.conflict("AI_DRAFT_INVALID", "Bản nhập không có nội dung");
            }
            return document;
        } catch (JsonProcessingException e) {
            throw ApiException.conflict("AI_DRAFT_INVALID", "Bản nhập không đọc được");
        }
    }

    /** Job dọn: xóa ảnh trang của bản nháp quá 24 giờ (BR-U07-91). */
    int deleteExpiredPages(Instant now) {
        return importPages.deletePagesOlderThan(now.minus(PAGE_TTL));
    }
}
