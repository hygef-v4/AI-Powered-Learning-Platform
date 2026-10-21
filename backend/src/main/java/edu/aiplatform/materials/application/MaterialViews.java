package edu.aiplatform.materials.application;

import edu.aiplatform.materials.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MaterialViews {
    private MaterialViews() { }
    public record Version(int versionNo,String fileName,String mimeType,Long byteSize,String videoUrl,String videoId,
                          MaterialVersion.TextStatus textStatus,UUID createdBy,Instant createdAt) {
        static Version of(MaterialVersion v) {
            return new Version(v.versionNo(),v.fileName(),v.mimeType(),v.byteSize(),v.videoUrl(),v.videoId(),v.textStatus(),v.createdBy(),v.createdAt());
        }
    }
    public record Detail(UUID id,Material.Scope scope,UUID subjectId,UUID classId,Material.Kind kind,String title,
                         String description,int orderNo,Material.Status status,int currentVersionNo,long version,
                         Instant updatedAt,boolean canManage,Version current,List<Version> versions) { }
}
