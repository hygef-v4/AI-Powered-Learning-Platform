package edu.aiplatform.materials.service;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, timeout = 10, noRollbackFor = ApiException.class)
public class ReferenceMaterialService implements IReferenceMaterial {
    public static final int MAX_CHARS = 1_000_000;
    private final MaterialQueryService materialQueryService;
    private final IMaterialRepository materialRepository;

    public ReferenceMaterialService(
            MaterialQueryService materialQueryService, IMaterialRepository materialRepository) {
        this.materialQueryService = materialQueryService;
        this.materialRepository = materialRepository;
    }

    @Override
    public List<SelectableMaterial> listSelectable(Actor actor, ResourceRef scope) {
        if (scope == null || scope.id() == null)
            throw ApiException.createBadRequest("MATERIAL_SCOPE_REQUIRED", "Chọn môn hoặc lớp");
        List<MaterialViews.Detail> items =
                switch (scope.scope()) {
                    case SUBJECT -> materialQueryService.listSubjectMaterials(actor, scope.id());
                    case CLASS ->
                            materialQueryService.listClassMaterials(
                                    actor,
                                    scope.id(),
                                    actor != null && actor.role() == Role.STUDENT,
                                    null);
                    default ->
                            throw ApiException.createBadRequest(
                                    "MATERIAL_SCOPE_INVALID", "Chọn môn hoặc lớp");
                };
        return items.stream()
                .filter(m -> m.status() == Material.Status.ACTIVE && m.enabled())
                .map(
                        m ->
                                new SelectableMaterial(
                                        m.id(),
                                        m.title(),
                                        m.current().textStatus()
                                                == MaterialVersion.TextStatus.EXTRACTED,
                                        m.kind() == Material.Kind.YOUTUBE_LINK))
                .toList();
    }

    @Override
    public List<MaterialText> extractText(Actor actor, List<UUID> ids, int maxChars) {
        if (ids == null
                || ids.size() > 10
                || ids.stream().anyMatch(Objects::isNull)
                || maxChars < 1)
            throw ApiException.createBadRequest(
                    "MATERIAL_TEXT_LIMIT", "Chọn tối đa 10 học liệu và giới hạn ký tự hợp lệ");
        // Authorize every ID before reading or exposing any text.
        List<Material> materials =
                ids
                        .stream()
                        .distinct()
                        .map(id -> materialQueryService.loadAuthorizedMaterial(actor, id, null))
                        .toList();
        if (materials.stream().anyMatch(m -> m.status() != Material.Status.ACTIVE || !m.enabled()))
            throw ApiException.createNotFound("MATERIAL_NOT_FOUND");
        int remaining = Math.min(maxChars, MAX_CHARS);
        List<MaterialText> result = new ArrayList<>();
        for (Material m : materials) {
            MaterialVersion v =
                    materialRepository
                            .findVersion(m.id(), m.currentVersionNo())
                            .orElseThrow(() -> ApiException.createNotFound("MATERIAL_NOT_FOUND"));
            String text =
                    m.kind() == Material.Kind.YOUTUBE_LINK
                            ? m.title() + "\n" + m.description() + "\n" + v.videoUrl()
                            : v.textStatus() == MaterialVersion.TextStatus.EXTRACTED
                                    ? Objects.toString(v.extractedText(), "")
                                    : "";
            int length = Math.min(remaining, text.length());
            result.add(
                    new MaterialText(
                            m.id(), m.title(), text.substring(0, length), length < text.length(),
                            v.versionNo()));
            remaining -= length;
        }
        return result;
    }
}
