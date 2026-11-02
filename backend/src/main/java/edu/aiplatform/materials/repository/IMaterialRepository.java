package edu.aiplatform.materials.repository;

import edu.aiplatform.materials.model.Material;
import edu.aiplatform.materials.model.MaterialVersion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMaterialRepository {
    Optional<Material> find(UUID id);
    List<Material> list(UUID subjectId, UUID classId);
    void insert(Material material, MaterialVersion version);
    boolean update(Material material, long expectedVersion);
    void insertVersion(MaterialVersion version);
    List<MaterialVersion> versions(UUID id);
    Optional<MaterialVersion> version(UUID id, int versionNo);
    boolean completeText(UUID id, int versionNo, MaterialVersion.TextStatus status, String text);
    List<MaterialVersion> stalePending(Instant olderThan);
}
