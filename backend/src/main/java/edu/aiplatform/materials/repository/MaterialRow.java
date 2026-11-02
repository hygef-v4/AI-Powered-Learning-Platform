package edu.aiplatform.materials.repository;

import edu.aiplatform.materials.model.Material;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "ReferenceMaterialRow")
@Table(name = "materials")
class MaterialRow {
    @Id UUID id;
    @Enumerated(EnumType.STRING) Material.Scope scope;
    UUID subjectId;
    UUID classId;
    UUID moduleId;
    @Enumerated(EnumType.STRING) Material.Kind kind;
    String title;
    String description;
    int orderNo;
    @Enumerated(EnumType.STRING) Material.Status status;
    int currentVersionNo;
    UUID createdBy;
    Instant createdAt;
    Instant updatedAt;
    long version;
    protected MaterialRow() { }
    static MaterialRow of(Material m) {
        MaterialRow r = new MaterialRow();
        r.id=m.id(); r.scope=m.scope(); r.subjectId=m.subjectId(); r.classId=m.classId(); r.moduleId=m.moduleId(); r.kind=m.kind();
        r.title=m.title(); r.description=m.description(); r.orderNo=m.orderNo(); r.status=m.status();
        r.currentVersionNo=m.currentVersionNo(); r.createdBy=m.createdBy(); r.createdAt=m.createdAt();
        r.updatedAt=m.updatedAt(); r.version=m.version(); return r;
    }
    Material snapshot() {
        return new Material(id,scope,subjectId,classId,moduleId,kind,title,description,orderNo,status,currentVersionNo,
                createdBy,createdAt,updatedAt,version);
    }
}
