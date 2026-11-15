package edu.aiplatform.assignments.service;

import edu.aiplatform.ai.contract.AiImage;
import edu.aiplatform.ai.contract.IAiImageSource;
import edu.aiplatform.assignments.repository.AiDraftRepository;
import edu.aiplatform.files.contract.IArtifact;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Worker reads only images already attached to an authorized draft request. */
@Component
public class AssignmentSourceImageSource implements IAiImageSource {
    static final String SOURCE = "assignment-source";
    private final AiDraftRepository drafts;
    private final IArtifact artifacts;

    public AssignmentSourceImageSource(AiDraftRepository drafts, IArtifact artifacts) {
        this.drafts = drafts;
        this.artifacts = artifacts;
    }

    @Override
    public String source() {
        return SOURCE;
    }

    @Override
    public Optional<AiImage> load(String ref) {
        if (ref == null) return Optional.empty();
        String[] parts = ref.split("/", -1);
        if (parts.length != 2) return Optional.empty();
        try {
            UUID draftId = UUID.fromString(parts[0]);
            int index = Integer.parseInt(parts[1]);
            if (index < 0 || index >= AssignmentAiDraftService.MAX_SOURCE_FILES) return Optional.empty();
            var draft = drafts.find(draftId);
            if (draft.isEmpty()) return Optional.empty();
            var file = draft.get().provenance().path("sourceFiles").path(index);
            String mediaType = file.path("mediaType").asText("");
            if (!Set.of("image/png", "image/jpeg").contains(mediaType)
                    || file.path("fileId").asText("").isBlank()
                    || file.path("byteSize").asLong() < 1
                    || file.path("byteSize").asLong() > AssignmentAiDraftService.MAX_SOURCE_BYTES) {
                return Optional.empty();
            }
            try (var input = artifacts.open(file.path("fileId").asText())) {
                byte[] bytes = input.readNBytes((int) AssignmentAiDraftService.MAX_SOURCE_BYTES + 1);
                return bytes.length == file.path("byteSize").asLong()
                        ? Optional.of(new AiImage(mediaType, bytes)) : Optional.empty();
            }
        } catch (IllegalArgumentException | IOException e) {
            return Optional.empty();
        }
    }
}
