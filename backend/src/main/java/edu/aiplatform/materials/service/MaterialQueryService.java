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
            throw ApiException.createBadRequest("MATERIAL_IDS_LIMIT", "Tối đa 100 học liệu");
        var c = materialAccessPolicy.requireClassListAccess(actor, classId, student);
        return materialRepository.listMaterials(c.subjectId(), classId).stream()
                .filter(m -> !student || m.status() == Material.Status.ACTIVE && m.enabled())
                .filter(m -> ids == null || ids.contains(m.id()))
                .map(m -> buildDetailView(actor, m, false, classId))
                // Danh sách học liệu: bản đã lưu trữ chỉ hiện với người khôi phục được (yêu cầu 2026-11-26), nên giảng
                // viên không thấy học liệu môn đã lưu trữ. Tra theo id (tài liệu tham khảo của bài) giữ như cũ (BR-U13-13).
                .filter(view -> ids != null || view.status() == Material.Status.ACTIVE || view.canManage())
                .toList();
    }

    public Material loadAuthorizedMaterial(Actor actor, UUID id, UUID classId) {
        Material m =
                materialRepository
                        .findMaterial(id)
                        .orElseThrow(() -> ApiException.createNotFound("MATERIAL_NOT_FOUND"));
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
            throw ApiException.createNotFound("MATERIAL_NOT_FOUND");
        MaterialVersion v =
                materialRepository
                        .findVersion(id, number)
                        .orElseThrow(() -> ApiException.createNotFound("MATERIAL_NOT_FOUND"));
        return artifact.issueDownloadToken(v.fileId(), actor.accountId());
    }

    private MaterialViews.Detail buildDetailView(
            Actor actor, Material material, boolean history, UUID contextClassId) {
        MaterialVersion current =
                materialRepository
                        .findVersion(material.id(), material.currentVersionNo())
                        .orElseThrow(() -> ApiException.createNotFound("MATERIAL_NOT_FOUND"));
        // Học liệu đã lưu trữ vẫn "quản lý được" để người quản lý khôi phục (yêu cầu 2026-11-26); sửa và lưu trữ lại
        // vẫn bị chặn ở requireWriteAccess và giao diện không cho sửa học liệu ARCHIVED.
        boolean editable =
                actor.role() != Role.STUDENT
                        && !(contextClassId != null && material.scope() == Material.Scope.SUBJECT)
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
                                ? List.of(MaterialViews.Version.createVersion(current))
                                : materialRepository.listVersions(material.id()).stream()
                                        .map(MaterialViews.Version::createVersion)
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
                MaterialViews.Version.createVersion(current),
                versions,
                material.enabled(),
                material.descriptionDocument());
    }
}
