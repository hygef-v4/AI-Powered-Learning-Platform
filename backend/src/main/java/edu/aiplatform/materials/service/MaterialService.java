package edu.aiplatform.materials.service;

import edu.aiplatform.audit.contract.*;
import edu.aiplatform.files.contract.*;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.shared.content.FormattedText;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(timeout = 10)
public class MaterialService {
    private final IMaterialRepository materialRepository;
    private final IMaterialModuleRepository materialModuleRepository;
    private final MaterialAccessPolicy materialAccessPolicy;
    private final IArtifact artifact;
    private final IJob job;
    private final IAudit audit;
    private final Clock clock;

    public MaterialService(
            IMaterialRepository materialRepository,
            IMaterialModuleRepository materialModuleRepository,
            MaterialAccessPolicy materialAccessPolicy,
            IArtifact artifact,
            IJob job,
            IAudit audit,
            Clock clock) {
        this.materialRepository = materialRepository;
        this.materialModuleRepository = materialModuleRepository;
        this.materialAccessPolicy = materialAccessPolicy;
        this.artifact = artifact;
        this.job = job;
        this.audit = audit;
        this.clock = clock;
    }

    public Material createMaterial(
            Actor actor, UUID scopeId, boolean classScope, MaterialCommands.Create input) {
        UUID subjectId =
                classScope
                        ? materialAccessPolicy
                                .requireClassListAccess(actor, scopeId, false)
                                .subjectId()
                        : scopeId;
        if (!classScope) materialAccessPolicy.requireSubjectListAccess(actor, subjectId);
        boolean enabled = !Boolean.FALSE.equals(input.enabled());
        Material material =
                new Material(
                        UUID.randomUUID(),
                        classScope ? Material.Scope.CLASS : Material.Scope.SUBJECT,
                        subjectId,
                        classScope ? scopeId : null,
                        requireModule(input.moduleId(), subjectId),
                        input.kind(),
                        input.title(),
                        input.description(),
                        input.orderNo(),
                        Material.Status.ACTIVE,
                        1,
                        actor.accountId(),
                        clock.instant(),
                        clock.instant(),
                        0).changePresentation(enabled, input.descriptionDocument());
        materialAccessPolicy.requireWriteAccess(actor, material);
        MaterialVersion version = buildVersion(actor, material, 1, input.source());
        materialRepository.insertMaterial(material, version);
        enqueueTextExtraction(version);
        recordAudit(actor, "MATERIAL_CREATED", material);
        return material;
    }

    public Material updateMaterial(Actor actor, UUID id, MaterialCommands.Update input) {
        Material m = requireWritableMaterial(actor, id, input.version());
        Material target =
                input.moduleId() == null || input.moduleId().equals(m.moduleId())
                        ? m
                        : m.moveToModule(requireModule(input.moduleId(), m.subjectId()));
        boolean enabled = input.enabled() == null ? m.enabled() : input.enabled();
        boolean sameDescription =
                Objects.toString(m.description(), "")
                        .strip()
                        .equals(Objects.toString(input.description(), "").strip());
        FormattedText document =
                input.descriptionDocument() != null
                        ? input.descriptionDocument()
                        : sameDescription ? m.descriptionDocument() : null;
        Material next =
                target.revise(
                                input.title(),
                                input.description(),
                                input.orderNo(),
                                m.currentVersionNo(),
                                m.status(),
                                clock.instant())
                        .changePresentation(enabled, document);
        saveMaterial(next, m.version());
        recordAudit(actor, "MATERIAL_UPDATED", next);
        return next;
    }

    public Material replaceMaterialVersion(Actor actor, UUID id, MaterialCommands.Replace input) {
        Material m = requireWritableMaterial(actor, id, input.version());
        MaterialVersion version = buildVersion(actor, m, m.currentVersionNo() + 1, input.source());
        Material next =
                m.revise(
                        m.title(),
                        m.description(),
                        m.orderNo(),
                        version.versionNo(),
                        m.status(),
                        clock.instant());
        saveMaterial(next, m.version());
        materialRepository.insertVersion(version);
        enqueueTextExtraction(version);
        recordAudit(actor, "MATERIAL_VERSION_CREATED", next);
        return next;
    }

    public Material archiveMaterial(Actor actor, UUID id, long expected) {
        Material m = requireWritableMaterial(actor, id, expected);
        Material next =
                m.revise(
                        m.title(),
                        m.description(),
                        m.orderNo(),
                        m.currentVersionNo(),
                        Material.Status.ARCHIVED,
                        clock.instant());
        saveMaterial(next, m.version());
        recordAudit(actor, "MATERIAL_ARCHIVED", next);
        return next;
    }

    public void reorderMaterials(
            Actor actor, UUID scopeId, boolean classScope, MaterialCommands.Reorder input) {
        if (classScope) materialAccessPolicy.requireClassListAccess(actor, scopeId, false);
        else materialAccessPolicy.requireSubjectListAccess(actor, scopeId);
        Set<UUID> seen = new HashSet<>();
        List<Material> rows = new ArrayList<>();
        for (var position : input.items()) {
            if (!seen.add(position.id()))
                throw ApiException.badRequest(
                        "MATERIAL_REORDER_INVALID", "Danh sách có học liệu trùng");
            Material m = requireWritableMaterial(actor, position.id(), position.version());
            if (classScope
                    ? !scopeId.equals(m.classId())
                    : m.classId() != null || !scopeId.equals(m.subjectId()))
                throw ApiException.notFound("MATERIAL_NOT_FOUND");
            rows.add(m);
        }
        // Lock/update in UUID order to avoid deadlocks between two reorder transactions.
        Map<UUID, Integer> positions = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) positions.put(rows.get(i).id(), i);
        rows.sort(Comparator.comparing(Material::id));
        for (Material m : rows) {
            Material next =
                    m.revise(
                            m.title(),
                            m.description(),
                            positions.get(m.id()),
                            m.currentVersionNo(),
                            m.status(),
                            clock.instant());
            saveMaterial(next, m.version());
            recordAudit(actor, "MATERIAL_UPDATED", next);
        }
    }

    private Material requireWritableMaterial(Actor actor, UUID id, long expected) {
        Material m =
                materialRepository
                        .findMaterial(id)
                        .orElseThrow(() -> ApiException.notFound("MATERIAL_NOT_FOUND"));
        materialAccessPolicy.requireWriteAccess(actor, m);
        if (expected != m.version()) throw createVersionConflict();
        return m;
    }

    /** Module phải là module của đúng môn chứa học liệu. */
    private UUID requireModule(UUID moduleId, UUID subjectId) {
        if (moduleId == null
                || materialModuleRepository
                        .findModule(moduleId)
                        .filter(m -> m.subjectId().equals(subjectId))
                        .isEmpty())
            throw ApiException.badRequest("MATERIAL_MODULE_INVALID", "Chọn module của môn");
        return moduleId;
    }

    private void saveMaterial(Material material, long expected) {
        if (!materialRepository.updateMaterial(material, expected)) throw createVersionConflict();
    }

    private ApiException createVersionConflict() {
        return ApiException.conflict(
                "MATERIAL_VERSION_CONFLICT", "Học liệu đã thay đổi, vui lòng tải lại");
    }

    private MaterialVersion buildVersion(
            Actor actor, Material material, int number, MaterialCommands.Source input) {
        if (input == null)
            throw ApiException.badRequest(
                    "MATERIAL_SOURCE_REQUIRED", "Chọn tệp hoặc đường dẫn video");
        if (material.kind() == Material.Kind.YOUTUBE_LINK) {
            if (input.fileRef() != null)
                throw ApiException.badRequest(
                        "MATERIAL_SOURCE_INVALID", "Nguồn không phù hợp loại học liệu");
            YoutubeVideo video;
            try {
                video = YoutubeVideo.parse(input.videoUrl());
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("MATERIAL_YOUTUBE_INVALID", e.getMessage());
            }
            return new MaterialVersion(
                    material.id(),
                    number,
                    null,
                    null,
                    null,
                    null,
                    video.url(),
                    video.id(),
                    null,
                    null,
                    actor.accountId(),
                    clock.instant());
        }
        if (input.videoUrl() != null || input.fileRef() == null || input.fileRef().isBlank())
            throw ApiException.badRequest("MATERIAL_SOURCE_INVALID", "Chọn tệp đã tải lên");
        StoredFileInfo file =
                artifact.attach(new FileRef(input.fileRef()), actor, ArtifactPurpose.MATERIAL);
        if (file == null
                || file.purpose() != ArtifactPurpose.MATERIAL
                || !actor.accountId().equals(file.ownerAccountId()))
            throw ApiException.badRequest("MATERIAL_FILE_INVALID", "Tệp không phù hợp");
        return new MaterialVersion(
                material.id(),
                number,
                file.fileId(),
                file.originalFileName(),
                file.mediaType(),
                file.byteSize(),
                null,
                null,
                MaterialVersion.TextStatus.PENDING,
                null,
                actor.accountId(),
                clock.instant());
    }

    private void enqueueTextExtraction(MaterialVersion version) {
        if (version.fileId() != null)
            job.enqueue(
                    JobTypes.MATERIAL_TEXT_EXTRACT,
                    Map.of(
                            "materialId",
                            version.materialId().toString(),
                            "versionNo",
                            version.versionNo()),
                    "material:" + version.materialId() + ":" + version.versionNo());
    }

    private void recordAudit(Actor actor, String action, Material material) {
        audit.record(
                new AuditEvent(
                        UUID.randomUUID(),
                        actor.accountId(),
                        action,
                        "MATERIAL",
                        material.id().toString(),
                        AuditResult.SUCCESS,
                        null,
                        Map.of(
                                "versionNo",
                                material.currentVersionNo(),
                                "version",
                                material.version()),
                        clock.instant()));
    }
}
