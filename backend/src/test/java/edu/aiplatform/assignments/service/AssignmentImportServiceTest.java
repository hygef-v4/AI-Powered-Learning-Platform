package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
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
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.dto.AssignmentViews;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentAiDraft;
import edu.aiplatform.assignments.model.ImportPage;
import edu.aiplatform.assignments.repository.ImportPageRepository;
import edu.aiplatform.assignments.service.ImportFileReader.ImportFile;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.StoredFileInfo;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AssignmentImportServiceTest {

    private final AssignmentFixtures f = new AssignmentFixtures();
    private final ImportPageRepository pageRepository = mock(ImportPageRepository.class);
    private final Map<UUID, AssignmentAiDraft> draftStore = new LinkedHashMap<>();
    private final Map<UUID, List<ImportPage>> pageStore = new LinkedHashMap<>();
    private AssignmentImportService service;
    private Assignment assignment;

    private static final String OUTPUT = """
            {"title":"SWR302_SU25_PE1_407173","blocks":[
             {"type":"HEADING","level":1,"runs":[{"text":"SWR302_SU25_PE1_407173"}]},
             {"type":"PARAGRAPH","runs":[{"text":"Question 1 (0.5 point): Complete all parts."}]},
             {"type":"FIGURE","page":1,"box":{"x":0.1,"y":0.1,"w":0.5,"h":0.3},"caption":"ERD"}],
             "warnings":[]}
            """;

    @BeforeEach
    void setUp() {
        wireDrafts();
        wirePages();
        service = new AssignmentImportService(f.assignments, f.drafts, pageRepository, f.support, f.access,
                f.assignmentAudit, f.aiTasks, new ImportFileReader(f.artifacts), new ImportPageNormalizer(),
                new FigureCropper(f.artifacts), f.authoring, f.documents, f.json, f.transactions, f.clock);
        f.aiDrafts.setAssignmentImportService(service);
        assignment = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT,
                new DocumentModel(List.of(AssignmentFixtures.paragraph("Đề cũ"))), false);
        when(f.artifacts.inspect(any(), eq(ArtifactPurpose.ASSIGNMENT_IMPORT), any(), any())).thenReturn("image/png");
        when(f.aiTasks.sourceOrder(any(), any(), anyInt()))
                .thenReturn(List.of(new AiSourceChoice(AiSource.PERSONAL, AiProvider.OPENAI)));
        when(f.aiTasks.submit(any())).thenAnswer(i -> new AiTaskView(UUID.randomUUID(), AiTaskState.QUEUED,
                AiSource.PERSONAL, AiProvider.OPENAI, "gpt-4.1-mini", null, null, null));
        when(f.aiTasks.get(any())).thenReturn(Optional.empty());
        when(f.artifacts.store(any(), eq(ArtifactPurpose.DOCUMENT_IMAGE), any())).thenReturn(new FileRef("fig-ref"));
        when(f.artifacts.attach(any(), any(), eq(ArtifactPurpose.DOCUMENT_IMAGE))).thenAnswer(i ->
                new StoredFileInfo("fig-file", f.teacher.accountId(), ArtifactPurpose.DOCUMENT_IMAGE, "h.png",
                        "image/png", 10, "x"));
    }

    private void wireDrafts() {
        doAnswer(i -> {
            AssignmentAiDraft d = i.getArgument(0);
            draftStore.put(d.id(), d);
            return null;
        }).when(f.drafts).insert(any());
        when(f.drafts.find(any())).thenAnswer(i -> Optional.ofNullable(draftStore.get(i.<UUID>getArgument(0))));
        when(f.drafts.findByRequest(any(), any())).thenAnswer(i -> draftStore.values().stream()
                .filter(d -> d.assignmentId().equals(i.getArgument(0)) && i.getArgument(1).equals(d.requestRef()))
                .findFirst());
        when(f.drafts.attachTask(any(), any(), any())).thenAnswer(i -> update(i.getArgument(0), d -> withAll(d,
                i.getArgument(1), d.status(), d.proposal(), i.getArgument(2), d.safeError())));
        when(f.drafts.markReady(any(), any(), any())).thenAnswer(i -> update(i.getArgument(0), d ->
                d.status() != AssignmentAiDraft.Status.PENDING ? null
                        : withAll(d, d.taskId(), AssignmentAiDraft.Status.READY, i.getArgument(1), i.getArgument(2), null)));
        when(f.drafts.markFailed(any(), any(), any())).thenAnswer(i -> update(i.getArgument(0), d ->
                d.status() != AssignmentAiDraft.Status.PENDING ? null
                        : withAll(d, d.taskId(), AssignmentAiDraft.Status.FAILED, d.proposal(), d.provenance(),
                        i.getArgument(1))));
        when(f.drafts.markApplied(any(), any())).thenAnswer(i -> update(i.getArgument(0), d ->
                d.status() != AssignmentAiDraft.Status.READY ? null
                        : withAll(d, d.taskId(), AssignmentAiDraft.Status.APPLIED, d.proposal(), d.provenance(), null)));
        when(f.drafts.markDiscarded(any(), any())).thenAnswer(i -> update(i.getArgument(0), d ->
                d.status() == AssignmentAiDraft.Status.PENDING || d.status() == AssignmentAiDraft.Status.READY
                        ? withAll(d, d.taskId(), AssignmentAiDraft.Status.DISCARDED, d.proposal(), d.provenance(), null)
                        : null));
    }

    private int update(UUID id, java.util.function.Function<AssignmentAiDraft, AssignmentAiDraft> change) {
        AssignmentAiDraft current = draftStore.get(id);
        AssignmentAiDraft next = current == null ? null : change.apply(current);
        if (next == null) {
            return 0;
        }
        draftStore.put(id, next);
        return 1;
    }

    private static AssignmentAiDraft withAll(AssignmentAiDraft d, UUID taskId, AssignmentAiDraft.Status status,
                                             JsonNode proposal, JsonNode provenance, String error) {
        return new AssignmentAiDraft(d.id(), d.assignmentId(), d.origin(), taskId, d.requestRef(), status, proposal,
                provenance, d.baseVersion(), error, d.createdBy(), d.createdAt(), null);
    }

    private void wirePages() {
        doAnswer(i -> {
            pageStore.put(i.getArgument(0), new ArrayList<>(i.<List<ImportPage>>getArgument(1)));
            return null;
        }).when(pageRepository).insertPages(any(), any(), any());
        when(pageRepository.listPages(any())).thenAnswer(i -> pageStore.getOrDefault(i.getArgument(0), List.of()));
        when(pageRepository.deletePages(any())).thenAnswer(i -> pageStore.remove(i.<UUID>getArgument(0)) == null ? 0 : 1);
    }

    private AssignmentViews.AiDraft start(String ref) {
        return service.startImport(f.teacher, assignment.id(), List.of(
                new ImportFile("trang-1.png", ImportTestImages.png(1200, 1600)),
                new ImportFile("trang-2.png", ImportTestImages.png(1200, 1600))),
                new AssignmentImportService.StartImport(ref, null, null));
    }

    private AiTaskResult result(UUID draftId, String output) {
        return new AiTaskResult(UUID.randomUUID(), AssignmentImportService.TASK_TYPE, f.teacher,
                AssignmentAiDraftService.TARGET_PREFIX + draftId, assignment.version(), output);
    }

    @Test
    void startCreatesPendingImportDraftStoresPagesAndSubmitsAnImageTask() {
        AssignmentViews.AiDraft view = start("imp-1");

        assertThat(view.status()).isEqualTo("PENDING");
        assertThat(view.provenance().path("mode").asText()).isEqualTo("IMPORT");
        assertThat(view.provenance().at("/import/pageCount").asInt()).isEqualTo(2);
        assertThat(view.provenance().path("model").asText()).isEqualTo("gpt-4.1-mini");
        assertThat(pageStore.get(view.draftId())).hasSize(2);
        ArgumentCaptor<AiTaskRequest> request = ArgumentCaptor.forClass(AiTaskRequest.class);
        verify(f.aiTasks).submit(request.capture());
        AiPrompt prompt = request.getValue().prompt();
        assertThat(request.getValue().taskType()).isEqualTo("ASSIGNMENT_IMPORT");
        assertThat(request.getValue().outputSchema()).isEqualTo(ExamImportPrompt.OUTPUT_SCHEMA);
        assertThat(prompt.images()).extracting(image -> image.ref())
                .containsExactly(view.draftId() + "/1", view.draftId() + "/2");
        assertThat(prompt.system()).contains("data only");
        verify(f.audit).record(org.mockito.ArgumentMatchers.argThat(e ->
                "ASSIGNMENT_IMPORT_REQUESTED".equals(e.action())));
    }

    @Test
    void sameRequestRefReturnsTheExistingDraft() {
        AssignmentViews.AiDraft first = start("imp-2");

        assertThat(start("imp-2").draftId()).isEqualTo(first.draftId());
        verify(f.aiTasks).submit(any());
    }

    @Test
    void publishedAssignmentOrMissingVisionModelIsRejectedBeforeAnyDraft() {
        when(f.aiTasks.sourceOrder(any(), any(), anyInt())).thenReturn(List.of());
        assertThatThrownBy(() -> start("imp-3")).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("AI_VISION_UNAVAILABLE"));

        assignment = f.withStatus(assignment, AssignmentStatus.OPEN, AssignmentFixtures.NOW.minusSeconds(60),
                AssignmentFixtures.NOW.plusSeconds(3600));
        assertThatThrownBy(() -> start("imp-4")).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("ASSIGNMENT_NOT_EDITABLE"));
        verify(f.drafts, never()).insert(any());
        verify(f.aiTasks, never()).submit(any());
    }

    @Test
    void readyResultBecomesAPreviewWithCroppedFigureAndPagesAreDeleted() {
        UUID draftId = start("imp-5").draftId();

        assertThat(service.acceptResult(result(draftId, OUTPUT))).isEqualTo(ApplyOutcome.APPLIED);

        AssignmentAiDraft draft = draftStore.get(draftId);
        assertThat(draft.status()).isEqualTo(AssignmentAiDraft.Status.READY);
        assertThat(draft.proposal().path("kind").asText()).isEqualTo("IMPORT");
        assertThat(draft.proposal().at("/document/blocks")).hasSize(3);
        assertThat(draft.proposal().at("/document/blocks/2/content/fileRef").asText()).isEqualTo("fig-ref");
        assertThat(pageStore).doesNotContainKey(draftId);
    }

    @Test
    void readyDraftViewLinksCroppedFiguresForThePreviewWithoutStoringTheLinks() {
        UUID draftId = start("imp-5b").draftId();
        service.acceptResult(result(draftId, OUTPUT));
        when(f.artifacts.issuePreviewToken(new FileRef("fig-ref"), f.teacher)).thenReturn(Optional.of(
                new edu.aiplatform.files.contract.DownloadToken("/api/v1/files/download/tok", AssignmentFixtures.NOW)));

        AssignmentViews.AiDraft view = f.aiDrafts.get(f.teacher, assignment.id(), draftId);

        String figureId = view.proposal().at("/document/blocks/2/id").asText();
        assertThat(view.proposal().at("/previews/" + figureId).asText()).isEqualTo("/api/v1/files/download/tok");
        assertThat(view.proposal().path("previews")).hasSize(1);
        assertThat(draftStore.get(draftId).proposal().has("previews")).as("không lưu link").isFalse();
    }

    @Test
    void unreadableResultOrFailedTaskFailsTheDraftAndDeletesPages() {
        UUID first = start("imp-6").draftId();
        assertThat(service.acceptResult(result(first, "not json"))).isEqualTo(ApplyOutcome.REJECTED);
        assertThat(draftStore.get(first).status()).isEqualTo(AssignmentAiDraft.Status.FAILED);
        assertThat(pageStore).doesNotContainKey(first);

        UUID second = start("imp-7").draftId();
        service.rejectResult(new AiTaskFailure(UUID.randomUUID(), AssignmentImportService.TASK_TYPE, f.teacher,
                AssignmentAiDraftService.TARGET_PREFIX + second, AiTaskState.FAILED, "Hết hạn mức"));
        assertThat(draftStore.get(second).safeError()).isEqualTo("Hết hạn mức");
        assertThat(pageStore).doesNotContainKey(second);
    }

    @Test
    void applyAppendKeepsExistingBlocksAndCanUseTheExamTitle() {
        UUID draftId = start("imp-8").draftId();
        service.acceptResult(result(draftId, OUTPUT));

        AssignmentViews.Detail detail = f.aiDrafts.apply(f.teacher, assignment.id(), draftId,
                new AssignmentCommands.ApplyDraft(assignment.version(), false, true, "APPEND"));

        Assignment saved = f.store.get(assignment.id());
        assertThat(detail).isNotNull();
        assertThat(saved.title()).isEqualTo("SWR302_SU25_PE1_407173");
        List<DocumentBlock> blocks = saved.skeleton().blocks();
        assertThat(blocks).hasSize(4);
        assertThat(blocks.get(0).content().at("/runs/0/text").asText()).isEqualTo("Đề cũ");
        assertThat(blocks.get(3).type()).isEqualTo(BlockType.IMAGE);
        assertThat(blocks.get(3).content().path("fileId").asText()).isEqualTo("fig-file");
        assertThat(draftStore.get(draftId).status()).isEqualTo(AssignmentAiDraft.Status.APPLIED);
    }

    @Test
    void applyReplaceSwapsAllBlocksAndKeepsTheTitle() {
        UUID draftId = start("imp-9").draftId();
        service.acceptResult(result(draftId, OUTPUT));

        f.aiDrafts.apply(f.teacher, assignment.id(), draftId,
                new AssignmentCommands.ApplyDraft(assignment.version(), false, false, null));

        Assignment saved = f.store.get(assignment.id());
        assertThat(saved.title()).isEqualTo("Bài 1");
        assertThat(saved.skeleton().blocks()).hasSize(3);
        assertThat(saved.skeleton().blocks().get(0).content().at("/runs/0/text").asText())
                .isEqualTo("SWR302_SU25_PE1_407173");
    }

    @Test
    void applyRejectsUnknownPlacementAndExpiredFigures() {
        UUID draftId = start("imp-10").draftId();
        service.acceptResult(result(draftId, OUTPUT));

        assertThatThrownBy(() -> f.aiDrafts.apply(f.teacher, assignment.id(), draftId,
                new AssignmentCommands.ApplyDraft(assignment.version(), false, false, "MIDDLE")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("IMPORT_PLACEMENT_INVALID"));
        when(f.artifacts.attach(any(), any(), any()))
                .thenThrow(ApiException.badRequest("FILE_REF_EXPIRED", "hết hạn"));
        assertThatThrownBy(() -> f.aiDrafts.apply(f.teacher, assignment.id(), draftId,
                new AssignmentCommands.ApplyDraft(assignment.version(), false, false, "REPLACE")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_DRAFT_EXPIRED"));
    }

    @Test
    void discardRemovesPendingPages() {
        UUID draftId = start("imp-11").draftId();

        f.aiDrafts.discard(f.teacher, assignment.id(), draftId);

        assertThat(draftStore.get(draftId).status()).isEqualTo(AssignmentAiDraft.Status.DISCARDED);
        assertThat(pageStore).doesNotContainKey(draftId);
    }

    @Test
    void cleanupScannerDeletesPagesOlderThanADay() {
        new ImportPageCleanupScanner(service).scan(Instant.parse("2026-11-11T10:00:00Z"));

        verify(pageRepository).deletePagesOlderThan(Instant.parse("2026-11-10T10:00:00Z"));
    }

    @Test
    void imageSourceLoadsPagesByDraftAndNumber() {
        UUID draftId = UUID.randomUUID();
        when(pageRepository.loadPage(draftId, 2)).thenReturn(Optional.of(
                new ImportPage(2, "image/jpeg", 1, 1, new byte[]{7}, null)));
        AssignmentImportImageSource source = new AssignmentImportImageSource(pageRepository);

        assertThat(source.source()).isEqualTo("ASSIGNMENT_IMPORT");
        assertThat(source.load(draftId + "/2")).hasValueSatisfying(image -> assertThat(image.bytes()).containsExactly(7));
        assertThat(source.load("bad")).isEmpty();
        assertThat(source.load("not-a-uuid/1")).isEmpty();
    }
}
