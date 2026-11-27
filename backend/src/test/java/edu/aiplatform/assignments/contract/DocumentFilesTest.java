package edu.aiplatform.assignments.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Gắn lại {@code fileId} của ảnh khi lưu tài liệu (TC-PERF-004: một lượt qua bản đã lưu). */
class DocumentFilesTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static DocumentBlock image(UUID id, String fileId, String fileRef) {
        ObjectNode content = JSON.createObjectNode().put("alt", "Hình");
        if (fileId != null) {
            content.put(DocumentFiles.FILE_ID, fileId);
        }
        if (fileRef != null) {
            content.put(DocumentFiles.FILE_REF, fileRef);
        }
        return new DocumentBlock(id, BlockOrigin.STUDENT, BlockType.IMAGE, content);
    }

    private static DocumentBlock paragraph(UUID id) {
        return new DocumentBlock(id, BlockOrigin.STUDENT, BlockType.PARAGRAPH,
                JSON.createObjectNode().put("text", "Đoạn " + id));
    }

    @Test
    void savedImageKeepsItsFileIdAndClientFileIdIsDropped() {
        UUID kept = UUID.randomUUID(), forged = UUID.randomUUID(), fresh = UUID.randomUUID();
        DocumentModel previous = new DocumentModel(List.of(image(kept, "file-1", null), paragraph(UUID.randomUUID())));
        DocumentModel incoming = new DocumentModel(List.of(image(kept, null, null), image(forged, "stolen", null),
                image(fresh, null, "ref-9")));

        DocumentModel resolved = DocumentFiles.resolveFiles(previous, incoming, ref -> "attached-" + ref);

        assertThat(resolved.blocks().get(0).content().path(DocumentFiles.FILE_ID).asText()).isEqualTo("file-1");
        assertThat(resolved.blocks().get(1).content().has(DocumentFiles.FILE_ID)).isFalse();
        assertThat(resolved.blocks().get(2).content().path(DocumentFiles.FILE_ID).asText()).isEqualTo("attached-ref-9");
        assertThat(resolved.blocks().get(2).content().has(DocumentFiles.FILE_REF)).isFalse();
    }

    @Test
    void resolvedFileIdsMatchPerBlockLookupOnLargeDocumentsWithDuplicateIds() {
        Random random = new Random(42);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            ids.add(UUID.randomUUID());
        }
        List<DocumentBlock> previousBlocks = new ArrayList<>();
        List<DocumentBlock> incomingBlocks = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            // Id lặp lại có chủ ý: block ảnh đầu tiên có id đó phải thắng như fileId(previous, id).
            UUID id = ids.get(random.nextInt(ids.size()));
            previousBlocks.add(switch (random.nextInt(3)) {
                case 0 -> paragraph(id);
                case 1 -> image(id, null, null);
                default -> image(id, "file-" + i, null);
            });
            incomingBlocks.add(random.nextBoolean() ? image(ids.get(random.nextInt(ids.size())), null, null)
                    : paragraph(UUID.randomUUID()));
        }
        DocumentModel previous = new DocumentModel(previousBlocks);

        DocumentModel resolved = DocumentFiles.resolveFiles(previous, new DocumentModel(incomingBlocks), null);

        for (int i = 0; i < incomingBlocks.size(); i++) {
            DocumentBlock in = incomingBlocks.get(i);
            DocumentBlock out = resolved.blocks().get(i);
            if (in.type() == BlockType.IMAGE) {
                assertThat(out.content().path(DocumentFiles.FILE_ID).isTextual()
                        ? java.util.Optional.of(out.content().path(DocumentFiles.FILE_ID).asText())
                        : java.util.Optional.empty())
                        .isEqualTo(DocumentFiles.fileId(previous, in.id()));
            } else {
                assertThat(out).isSameAs(in);
            }
        }
    }
}
