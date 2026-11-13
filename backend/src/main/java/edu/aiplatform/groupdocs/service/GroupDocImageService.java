package edu.aiplatform.groupdocs.service;

import edu.aiplatform.assignments.contract.DocumentFiles;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.groupdocs.model.GroupDocument;
import edu.aiplatform.groupdocs.model.GroupSection;
import edu.aiplatform.groupdocs.repository.GroupDocumentRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ảnh trong tài liệu nhóm theo id block (như ảnh của bài cá nhân): thành viên hoặc giảng viên của lớp xem ảnh của phần
 * chung, nội dung đã đăng, bản nộp và bản nháp của chính mình; cấp link tải 5 phút của U03.
 */
@Service
public class GroupDocImageService {

    private final GroupDocAccess access;
    private final GroupDocumentRepository documents;
    private final IArtifact artifacts;

    public GroupDocImageService(GroupDocAccess access, GroupDocumentRepository documents, IArtifact artifacts) {
        this.access = access;
        this.documents = documents;
        this.artifacts = artifacts;
    }

    @Transactional(readOnly = true)
    public String imageUrl(Actor actor, UUID documentId, UUID blockId) {
        GroupDocContext ctx = access.read(actor, documentId);
        GroupDocument document = ctx.document();
        List<DocumentModel> sources = new ArrayList<>();
        sources.add(document.sharedBlocks());
        if (document.submittedDocument() != null) {
            sources.add(document.submittedDocument());
        }
        for (GroupSection section : documents.listSections(documentId)) {
            if (section.publishedBlocks() != null) {
                sources.add(new DocumentModel(section.publishedBlocks()));
            }
            if (section.draftBlocks() != null && ctx.actorId().equals(section.claimedBy())) {
                sources.add(new DocumentModel(section.draftBlocks()));
            }
        }
        String fileId = sources.stream().map(source -> DocumentFiles.fileId(source, blockId))
                .flatMap(Optional::stream).findFirst()
                .orElseThrow(() -> ApiException.notFound("IMAGE_NOT_FOUND"));
        return artifacts.issueDownloadToken(fileId, actor.accountId()).url();
    }
}
