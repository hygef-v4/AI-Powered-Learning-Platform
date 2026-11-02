package edu.aiplatform.materials.application;

import edu.aiplatform.materials.domain.*;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizStatus;
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
    public record Detail(UUID id,Material.Scope scope,UUID subjectId,UUID classId,UUID moduleId,Material.Kind kind,String title,
                         String description,int orderNo,Material.Status status,int currentVersionNo,long version,
                         Instant updatedAt,boolean canManage,Version current,List<Version> versions) { }
    /** Quiz gắn trong module; {@code canManage}: người xem gỡ được khỏi module. */
    public record ModuleQuiz(UUID quizId,String title,QuizStatus status,QuizScope scope,UUID classId,boolean canManage) { }
    /** Module của môn; học liệu lấy từ danh sách học liệu theo {@code moduleId}. */
    public record Module(UUID id,String title,int orderNo,long version,boolean canManage,List<ModuleQuiz> quizzes) { }
}
