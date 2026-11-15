package edu.aiplatform.materials.repository;

import edu.aiplatform.materials.model.Material;
import edu.aiplatform.materials.model.MaterialVersion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMaterialRepository {
    Optional<Material> findMaterial(UUID id);

    List<Material> findByFileId(String fileId);

    List<Material> listMaterials(UUID subjectId, UUID classId);

    void insertMaterial(Material material, MaterialVersion version);

    boolean updateMaterial(Material material, long expectedVersion);

    void insertVersion(MaterialVersion version);

    List<MaterialVersion> listVersions(UUID id);

    Optional<MaterialVersion> findVersion(UUID id, int versionNo);

    boolean completeText(UUID id, int versionNo, MaterialVersion.TextStatus status, String text);

    List<MaterialVersion> listStalePendingVersions(Instant olderThan);
}
