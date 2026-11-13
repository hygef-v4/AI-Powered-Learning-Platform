package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignments.model.AssignmentAiDraft;
import edu.aiplatform.files.contract.*;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AssignmentSourceImageSourceTest {
    private final AssignmentFixtures f = new AssignmentFixtures();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void imagesReachGenerationAndRevisionWithoutBeingPassedToTextExtractor(boolean revise) {
        var assignment = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        AtomicReference<AssignmentAiDraft> saved = new AtomicReference<>();
        doAnswer(call -> { saved.set(call.getArgument(0)); return null; }).when(f.drafts).insert(any());
        when(f.drafts.find(any())).thenAnswer(call -> Optional.ofNullable(saved.get()));
        when(f.drafts.updatePendingProvenance(any(), any())).thenAnswer(call -> {
            ObjectNode next = ((ObjectNode) call.getArgument(1)).deepCopy();
            ((ObjectNode) saved.get().provenance()).removeAll().setAll(next);
            return 1;
        });
        when(f.artifacts.attach(any(), eq(f.teacher), eq(ArtifactPurpose.ASSIGNMENT_SOURCE)))
                .thenReturn(new StoredFileInfo("private-image", f.teacher.accountId(), ArtifactPurpose.ASSIGNMENT_SOURCE,
                        "diagram.png", "image/png", 3, "hash"));
        when(f.aiTasks.sourceOrder(any(), any(), anyInt())).thenReturn(List.of(new AiSourceChoice(AiSource.SHARED, null)));
        when(f.aiTasks.submit(any())).thenReturn(new AiTaskView(java.util.UUID.randomUUID(), AiTaskState.QUEUED,
                AiSource.SHARED, null, "vision", null, null, null));

        f.aiDrafts.request(f.teacher, assignment.id(), new AssignmentCommands.AiDraftRequest(
                "Soạn bài theo tài liệu tham khảo", List.of(), List.of("signed-ref"), null, null, "image-test",
                null, null, null, revise ? "REVISE" : null));

        verify(f.sourceTexts, never()).extract(anyList(), anyInt());
        verify(f.aiTasks).sourceOrder(eq(f.teacher), argThat(prompt -> prompt.images().size() == 1), anyInt());
        verify(f.aiTasks).submit(argThat(request -> request.prompt().images().size() == 1
                && request.prompt().images().get(0).ref().equals(saved.get().id() + "/0")
                && request.prompt().images().get(0).source().equals(AssignmentSourceImageSource.SOURCE)));
        var loader = new AssignmentSourceImageSource(f.drafts, f.artifacts);
        when(f.artifacts.open("private-image")).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        assertThat(loader.load(saved.get().id() + "/0")).isPresent();
        assertThat(loader.load("private-image")).isEmpty();
        assertThat(loader.load(saved.get().id() + "/-1")).isEmpty();
        assertThat(loader.load(saved.get().id() + "/3")).isEmpty();
    }

    @Test
    void imagePreviewDoesNotCallModelOrTextParserAndRejectsOtherOwners() {
        var assignment = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        when(f.artifacts.attach(any(), eq(f.teacher), eq(ArtifactPurpose.ASSIGNMENT_SOURCE)))
                .thenReturn(new StoredFileInfo("private-image", f.teacher.accountId(), ArtifactPurpose.ASSIGNMENT_SOURCE,
                        "diagram.jpg", "image/jpeg", 3, "hash"));
        assertThat(f.aiDrafts.previewSources(f.teacher, assignment.id(), List.of("signed-ref")))
                .singleElement().satisfies(preview -> assertThat(preview.previewText()).contains("Ảnh tham khảo"));
        verifyNoInteractions(f.sourceTexts, f.aiTasks);
        when(f.artifacts.attach(any(), eq(f.teacher), eq(ArtifactPurpose.ASSIGNMENT_SOURCE)))
                .thenReturn(new StoredFileInfo("other-image", java.util.UUID.randomUUID(), ArtifactPurpose.ASSIGNMENT_SOURCE,
                        "other.png", "image/png", 3, "hash"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> f.aiDrafts.previewSources(f.teacher,
                assignment.id(), List.of("other-ref"))).hasMessageContaining("không hợp lệ");
    }
}
