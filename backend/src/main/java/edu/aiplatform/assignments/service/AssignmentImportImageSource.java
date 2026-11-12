package edu.aiplatform.assignments.service;

import edu.aiplatform.ai.contract.AiImage;
import edu.aiplatform.ai.contract.IAiImageSource;
import edu.aiplatform.assignments.repository.ImportPageRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Nạp ảnh trang {@code draftId/pageNo} cho worker AI (BR-U06-55, BR-U07-91). */
@Component
public class AssignmentImportImageSource implements IAiImageSource {

    private final ImportPageRepository importPages;

    public AssignmentImportImageSource(ImportPageRepository importPages) {
        this.importPages = importPages;
    }

    @Override
    public String source() {
        return ExamImportPrompt.IMAGE_SOURCE;
    }

    @Override
    public Optional<AiImage> load(String ref) {
        int slash = ref == null ? -1 : ref.indexOf('/');
        if (slash < 0) {
            return Optional.empty();
        }
        try {
            UUID draftId = UUID.fromString(ref.substring(0, slash));
            int pageNo = Integer.parseInt(ref.substring(slash + 1));
            return importPages.loadPage(draftId, pageNo).map(page -> new AiImage(page.mediaType(), page.bytes()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
