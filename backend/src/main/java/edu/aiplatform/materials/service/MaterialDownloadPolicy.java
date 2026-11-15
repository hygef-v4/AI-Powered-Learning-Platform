package edu.aiplatform.materials.service;

import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.IArtifactDownloadPolicy;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.materials.repository.IMaterialRepository;
import edu.aiplatform.shared.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MaterialDownloadPolicy implements IArtifactDownloadPolicy {
    private final IMaterialRepository materialRepository;
    private final MaterialAccessPolicy materialAccessPolicy;

    public MaterialDownloadPolicy(
            IMaterialRepository materialRepository, MaterialAccessPolicy materialAccessPolicy) {
        this.materialRepository = materialRepository;
        this.materialAccessPolicy = materialAccessPolicy;
    }

    @Override
    public ArtifactPurpose purpose() {
        return ArtifactPurpose.MATERIAL;
    }

    @Override
    @Transactional(readOnly = true, timeout = 10)
    public void requireDownload(Actor actor, String fileId) {
        for (var material : materialRepository.findByFileId(fileId)) {
            try {
                materialAccessPolicy.requireReadAccess(actor, material, null);
                if (actor.role() == Role.STUDENT
                        && materialRepository
                                .findVersion(material.id(), material.currentVersionNo())
                                .filter(current -> fileId.equals(current.fileId()))
                                .isEmpty()) continue;
                return;
            } catch (ApiException rejected) {
                if (rejected.status() != HttpStatus.NOT_FOUND) throw rejected;
            }
        }
        throw ApiException.notFound("MATERIAL_NOT_FOUND");
    }
}
