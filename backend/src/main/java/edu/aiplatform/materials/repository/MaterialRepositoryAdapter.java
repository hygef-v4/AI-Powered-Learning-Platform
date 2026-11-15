package edu.aiplatform.materials.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.shared.content.FormattedText;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true, timeout = 10)
public class MaterialRepositoryAdapter implements IMaterialRepository {
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public MaterialRepositoryAdapter(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Material> findMaterial(UUID id) {
        return jdbcClient
                .sql("select * from materials where id = :id")
                .param("id", id)
                .query(this::mapMaterial)
                .optional();
    }

    @Override
    public List<Material> findByFileId(String fileId) {
        return jdbcClient
                .sql("""
                select m.* from materials m where exists
                    (select 1 from material_versions v where v.material_id=m.id and v.file_id=:file)
                """)
                .param("file", fileId)
                .query(this::mapMaterial)
                .list();
    }

    @Override
    public List<Material> listMaterials(UUID subjectId, UUID classId) {
        String classFilter =
                classId == null ? "class_id is null" : "(class_id is null or class_id = :classId)";
        return jdbcClient
                .sql("select * from materials where subject_id = :subject and "
                        + classFilter
                        + " order by scope desc, order_no, id")
                .param("subject", subjectId)
                .params(classId == null ? java.util.Map.of() : java.util.Map.of("classId", classId))
                .query(this::mapMaterial)
                .list();
    }

    @Override
    @Transactional(timeout = 10)
    public void insertMaterial(Material material, MaterialVersion version) {
        jdbcClient
                .sql("""
                insert into materials (id, scope, subject_id, class_id, module_id, kind, title, description,
                    order_no, status, current_version_no, created_by, created_at, updated_at, version,
                    enabled, description_document)
                values (:id, :scope, :subject, :classId, :module, :kind, :title, :description, :orderNo,
                    :status, :contentVersion, :creator, :created, :updated, :version, :enabled, cast(:document as jsonb))
                """)
                .param("id", material.id())
                .param("scope", material.scope().name())
                .param("subject", material.subjectId())
                .param("classId", material.classId())
                .param("module", material.moduleId())
                .param("kind", material.kind().name())
                .param("title", material.title())
                .param("description", material.description())
                .param("orderNo", material.orderNo())
                .param("status", material.status().name())
                .param("contentVersion", material.currentVersionNo())
                .param("creator", material.createdBy())
                .param("created", Timestamp.from(material.createdAt()))
                .param("updated", Timestamp.from(material.updatedAt()))
                .param("version", material.version())
                .param("enabled", material.enabled())
                .param("document", writeDocument(material.descriptionDocument()))
                .update();
        insertVersion(version);
    }

    @Override
    @Transactional(timeout = 10)
    public boolean updateMaterial(Material material, long expectedVersion) {
        return jdbcClient
                        .sql("""
                update materials set module_id=:module, title=:title, description=:description,
                    order_no=:orderNo, status=:status, current_version_no=:contentVersion,
                    updated_at=:updated, version=:version, enabled=:enabled, description_document=cast(:document as jsonb)
                where id=:id and version=:expected
                """)
                        .param("module", material.moduleId())
                        .param("title", material.title())
                        .param("description", material.description())
                        .param("orderNo", material.orderNo())
                        .param("status", material.status().name())
                        .param("contentVersion", material.currentVersionNo())
                        .param("updated", Timestamp.from(material.updatedAt()))
                        .param("version", material.version())
                        .param("id", material.id())
                        .param("enabled", material.enabled())
                        .param("document", writeDocument(material.descriptionDocument()))
                        .param("expected", expectedVersion)
                        .update()
                == 1;
    }

    @Override
    @Transactional(timeout = 10)
    public void insertVersion(MaterialVersion version) {
        jdbcClient
                .sql("""
                insert into material_versions (material_id, version_no, file_id, file_name, mime_type, byte_size,
                    video_url, video_id, text_status, extracted_text, created_by, created_at)
                values (:id, :version, :file, :name, :mime, :bytes, :url, :video, :status, :text, :creator, :created)
                """)
                .param("id", version.materialId())
                .param("version", version.versionNo())
                .param("file", version.fileId())
                .param("name", version.fileName())
                .param("mime", version.mimeType())
                .param("bytes", version.byteSize())
                .param("url", version.videoUrl())
                .param("video", version.videoId())
                .param("status", version.textStatus() == null ? null : version.textStatus().name())
                .param("text", version.extractedText())
                .param("creator", version.createdBy())
                .param("created", Timestamp.from(version.createdAt()))
                .update();
    }

    @Override
    public List<MaterialVersion> listVersions(UUID id) {
        return jdbcClient
                .sql("select * from material_versions where material_id=:id order by version_no desc")
                .param("id", id)
                .query(this::mapVersion)
                .list();
    }

    @Override
    public Optional<MaterialVersion> findVersion(UUID id, int versionNo) {
        return jdbcClient
                .sql("select * from material_versions where material_id=:id and version_no=:version")
                .param("id", id)
                .param("version", versionNo)
                .query(this::mapVersion)
                .optional();
    }

    @Override
    @Transactional(timeout = 10)
    public boolean completeText(
            UUID id, int versionNo, MaterialVersion.TextStatus status, String text) {
        return jdbcClient
                        .sql("update material_versions set text_status=:status, extracted_text=:text "
                                + "where material_id=:id and version_no=:version and text_status='PENDING'")
                        .param("status", status.name())
                        .param("text", text)
                        .param("id", id)
                        .param("version", versionNo)
                        .update()
                == 1;
    }

    @Override
    public List<MaterialVersion> listStalePendingVersions(Instant olderThan) {
        return jdbcClient
                .sql("""
                select v.* from material_versions v join materials m on m.id=v.material_id
                where v.version_no=m.current_version_no and v.text_status='PENDING' and v.created_at<:before
                order by v.created_at limit 100
                """)
                .param("before", Timestamp.from(olderThan))
                .query(this::mapVersion)
                .list();
    }

    private Material mapMaterial(ResultSet rs, int row) throws SQLException {
        return new Material(
                rs.getObject("id", UUID.class),
                Material.Scope.valueOf(rs.getString("scope")),
                rs.getObject("subject_id", UUID.class),
                rs.getObject("class_id", UUID.class),
                rs.getObject("module_id", UUID.class),
                Material.Kind.valueOf(rs.getString("kind")),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("order_no"),
                Material.Status.valueOf(rs.getString("status")),
                rs.getInt("current_version_no"),
                rs.getObject("created_by", UUID.class),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getLong("version"),
                rs.getBoolean("enabled"),
                readDocument(rs.getString("description_document")));
    }

    private MaterialVersion mapVersion(ResultSet rs, int row) throws SQLException {
        String status = rs.getString("text_status");
        return new MaterialVersion(
                rs.getObject("material_id", UUID.class),
                rs.getInt("version_no"),
                rs.getString("file_id"),
                rs.getString("file_name"),
                rs.getString("mime_type"),
                rs.getObject("byte_size", Long.class),
                rs.getString("video_url"),
                rs.getString("video_id"),
                status == null ? null : MaterialVersion.TextStatus.valueOf(status),
                rs.getString("extracted_text"),
                rs.getObject("created_by", UUID.class),
                rs.getTimestamp("created_at").toInstant());
    }

    private String writeDocument(FormattedText document) {
        if (document == null) return null;
        try {
            return objectMapper.writeValueAsString(document);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Không ghi được mô tả học liệu", failure);
        }
    }

    private FormattedText readDocument(String document) {
        if (document == null) return null;
        try {
            return objectMapper.readValue(document, FormattedText.class);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Mô tả học liệu trong DB không hợp lệ", failure);
        }
    }
}
