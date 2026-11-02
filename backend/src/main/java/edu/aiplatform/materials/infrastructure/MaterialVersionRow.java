package edu.aiplatform.materials.infrastructure;

import edu.aiplatform.materials.domain.MaterialVersion;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "ReferenceMaterialVersionRow")
@Table(name = "material_versions")
@IdClass(MaterialVersionRow.Key.class)
class MaterialVersionRow {
    @Id UUID materialId;
    @Id int versionNo;
    String fileId;
    String fileName;
    String mimeType;
    Long byteSize;
    String videoUrl;
    String videoId;
    @Enumerated(EnumType.STRING) MaterialVersion.TextStatus textStatus;
    @Column(columnDefinition="text") String extractedText;
    UUID createdBy;
    Instant createdAt;
    protected MaterialVersionRow() { }
    public static class Key implements Serializable {
        public UUID materialId;
        public int versionNo;
        public Key() { }
        public Key(UUID id, int version) { materialId=id; versionNo=version; }
        @Override public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(materialId,k.materialId) && versionNo==k.versionNo;
        }
        @Override public int hashCode() { return Objects.hash(materialId,versionNo); }
    }
    static MaterialVersionRow of(MaterialVersion v) {
        MaterialVersionRow r=new MaterialVersionRow();
        r.materialId=v.materialId(); r.versionNo=v.versionNo(); r.fileId=v.fileId(); r.fileName=v.fileName();
        r.mimeType=v.mimeType(); r.byteSize=v.byteSize(); r.videoUrl=v.videoUrl(); r.videoId=v.videoId();
        r.textStatus=v.textStatus(); r.extractedText=v.extractedText(); r.createdBy=v.createdBy();
        r.createdAt=v.createdAt(); return r;
    }
    MaterialVersion snapshot() {
        return new MaterialVersion(materialId,versionNo,fileId,fileName,mimeType,byteSize,videoUrl,videoId,
                textStatus,extractedText,createdBy,createdAt);
    }
}
