package edu.aiplatform.materials.service;

import edu.aiplatform.files.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.academics.contract.ClassStatus;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, timeout = 10, noRollbackFor = ApiException.class)
public class MaterialQueryService {
    private final IMaterialRepository materialRepository;
    private final MaterialAccessPolicy materialAccessPolicy;
    private final IArtifact artifact;

    public MaterialQueryService(
            IMaterialRepository materialRepository,
            MaterialAccessPolicy materialAccessPolicy,
            IArtifact artifact) {
        this.materialRepository = materialRepository;
        this.materialAccessPolicy = materialAccessPolicy;
        this.artifact = artifact;
    }

    public List<MaterialViews.Detail> listSubjectMaterials(Actor actor, UUID subjectId) {
        materialAccessPolicy.requireSubjectListAccess(actor, subjectId);
        return materialRepository.listMaterials(subjectId, null).stream()
                .map(m -> buildDetailView(actor, m, false, null))
                .toList();
    }

    public List<MaterialViews.Detail> listClassMaterials(
            Actor actor, UUID classId, boolean student, List<UUID> ids) {
        if (ids != null && ids.size() > 100)
            throw ApiException.badRequest("MATERIAL_IDS_LIMIT", "Tối đa 100 học liệu");
        var c = materialAccessPolicy.requireClassListAccess(actor, classId, student);
        return materialRepository.listMaterials(c.subjectId(), classId).stream()
                .filter(m -> !student || m.status() == Material.Status.ACTIVE && m.enabled())
                .filter(m -> ids == null || ids.contains(m.id()))
                .map(m -> buildDetailView(actor, m, false, classId))
                .toList();
    }

    public Material loadAuthorizedMaterial(Actor actor, UUID id, UUID classId) {
        Material m =
                materialRepository
                        .findMaterial(id)
                        .orElseThrow(() -> ApiException.notFound("MATERIAL_NOT_FOUND"));
        materialAccessPolicy.requireReadAccess(actor, m, classId);
        return m;
    }

    public MaterialViews.Detail loadMaterial(Actor actor, UUID id, UUID classId) {
        return buildDetailView(actor, loadAuthorizedMaterial(actor, id, classId), true, classId);
    }

    public DownloadToken issueDownloadToken(Actor actor, UUID id, UUID classId, Integer versionNo) {
        Material m = loadAuthorizedMaterial(actor, id, classId);
        int number = versionNo == null ? m.currentVersionNo() : versionNo;
        if (m.kind() != Material.Kind.FILE
                || (actor.role() == Role.STUDENT && number != m.currentVersionNo()))
            throw ApiException.notFound("MATERIAL_NOT_FOUND");
        MaterialVersion v =
                materialRepository
                        .findVersion(id, number)
                        .orElseThrow(() -> ApiException.notFound("MATERIAL_NOT_FOUND"));
        return artifact.issueDownloadToken(v.fileId(), actor.accountId());
    }

    private MaterialViews.Detail buildDetailView(
            Actor actor, Material material, boolean history, UUID contextClassId) {
        MaterialVersion current =
                materialRepository
                        .findVersion(material.id(), material.currentVersionNo())
                        .orElseThrow(() -> ApiException.notFound("MATERIAL_NOT_FOUND"));
        boolean editable =
                actor.role() != Role.STUDENT
                        && !(contextClassId != null && material.scope() == Material.Scope.SUBJECT)
                        && material.status() == Material.Status.ACTIVE
                        && materialAccessPolicy.canManageMaterial(actor, material)
                        && materialAccessPolicy
                                .requireSubject(material.subjectId())
                                .status() == SubjectStatus.ACTIVE
                        && (material.classId() == null
                                || materialAccessPolicy.requireClass(material.classId()).status()
                                        != ClassStatus.ARCHIVED);
        List<MaterialViews.Version> versions =
                !history
                        ? List.of()
                        : actor.role() == Role.STUDENT
                                ? List.of(MaterialViews.Version.of(current))
                                : materialRepository.listVersions(material.id()).stream()
                                        .map(MaterialViews.Version::of)
                                        .toList();
        return new MaterialViews.Detail(
                material.id(),
                material.scope(),
                material.subjectId(),
                material.classId(),
                material.moduleId(),
                material.kind(),
                material.title(),
                material.description(),
                material.orderNo(),
                material.status(),
                material.currentVersionNo(),
                material.version(),
                material.updatedAt(),
                editable,
                MaterialViews.Version.of(current),
                versions,
                material.enabled(),
                material.descriptionDocument());
    }
}
