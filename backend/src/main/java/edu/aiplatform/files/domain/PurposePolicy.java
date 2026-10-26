package edu.aiplatform.files.domain;

import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.settings.contract.SettingKey;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Quy tắc theo {@code purpose}: role được tải lên (BR-U03-04), trần dung lượng lấy từ Settings (BR-U03-02, 87) và luôn
 * bị kẹp trong trần cứng 50 MB, 5 MB, 2 MB. Loại tệp cố định, không có trong Settings: học liệu PDF, DOCX, PPTX; ảnh
 * trong tài liệu PNG, JPEG; tệp nguồn soạn bài bằng AI TXT, DOCX, PDF.
 */
@Component
public class PurposePolicy {

    public static final SettingKey<Integer> MATERIAL_MAX_MB = new SettingKey<>("files.material.maxSizeMb", Integer.class);
    public static final SettingKey<Integer> IMAGE_MAX_MB = new SettingKey<>("files.documentImage.maxSizeMb", Integer.class);
    public static final SettingKey<Integer> ASSIGNMENT_SOURCE_MAX_MB = new SettingKey<>("files.assignmentSource.maxSizeMb", Integer.class);

    static final Map<ArtifactPurpose, Integer> HARD_CAP_MB = Map.of(ArtifactPurpose.MATERIAL, 50,
            ArtifactPurpose.DOCUMENT_IMAGE, 5, ArtifactPurpose.ASSIGNMENT_SOURCE, 2);
    static final Map<ArtifactPurpose, Set<FileType>> SUPPORTED = Map.of(
            ArtifactPurpose.MATERIAL, EnumSet.of(FileType.PDF, FileType.DOCX, FileType.PPTX),
            ArtifactPurpose.DOCUMENT_IMAGE, EnumSet.of(FileType.PNG, FileType.JPEG),
            ArtifactPurpose.ASSIGNMENT_SOURCE, EnumSet.of(FileType.TXT, FileType.DOCX, FileType.PDF));
    static final Map<ArtifactPurpose, Set<Role>> UPLOADERS = Map.of(
            ArtifactPurpose.MATERIAL, EnumSet.of(Role.TEACHER, Role.SUBJECT_MANAGER),
            ArtifactPurpose.DOCUMENT_IMAGE, EnumSet.of(Role.STUDENT, Role.TEACHER, Role.SUBJECT_MANAGER),
            ArtifactPurpose.ASSIGNMENT_SOURCE, EnumSet.of(Role.TEACHER, Role.SUBJECT_MANAGER));

    private final ISettings settings;

    public PurposePolicy(ISettings settings) {
        this.settings = settings;
    }

    public boolean canUpload(Role role, ArtifactPurpose purpose) {
        return UPLOADERS.get(purpose).contains(role);
    }

    public PurposeLimits limits(ArtifactPurpose purpose) {
        int cap = HARD_CAP_MB.get(purpose);
        SettingKey<Integer> sizeKey = switch (purpose) {
            case MATERIAL -> MATERIAL_MAX_MB;
            case DOCUMENT_IMAGE -> IMAGE_MAX_MB;
            case ASSIGNMENT_SOURCE -> ASSIGNMENT_SOURCE_MAX_MB;
        };
        int size = settings.get(sizeKey).map(value -> Math.max(1, Math.min(cap, value))).orElse(cap);
        return new PurposeLimits(purpose, size, EnumSet.copyOf(SUPPORTED.get(purpose)));
    }
}
