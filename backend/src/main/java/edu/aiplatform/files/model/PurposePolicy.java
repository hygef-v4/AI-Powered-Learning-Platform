package edu.aiplatform.files.model;

import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.settings.contract.SettingKey;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Quy tắc theo {@code purpose}: role được tải lên (BR-U03-04). Trần dung lượng của học liệu và ảnh lấy từ Settings
 * (BR-U03-02, 87), luôn bị kẹp trong trần cứng 50 MB và 5 MB; tệp nguồn soạn bài (2 MB) và tệp nhập đề (10 MB) chỉ dùng
 * trần cứng. Loại tệp cố định, không có trong Settings: học liệu PDF, DOCX, PPTX, XLSX; ảnh
 * trong tài liệu PNG, JPEG; tệp nguồn soạn bài bằng AI TXT, DOCX, PDF, PNG, JPEG, WEBP; tệp nhập đề PNG, JPEG, WEBP, PDF (trần 10 MB).
 */
@Component
public class PurposePolicy {

    public static final SettingKey<Integer> MATERIAL_MAX_MB = new SettingKey<>("files.material.maxSizeMb", Integer.class);
    public static final SettingKey<Integer> IMAGE_MAX_MB = new SettingKey<>("files.documentImage.maxSizeMb", Integer.class);

    static final Map<ArtifactPurpose, Integer> HARD_CAP_MB = Map.of(ArtifactPurpose.MATERIAL, 50,
            ArtifactPurpose.DOCUMENT_IMAGE, 5, ArtifactPurpose.ASSIGNMENT_SOURCE, 2, ArtifactPurpose.ASSIGNMENT_IMPORT, 10);
    static final Map<ArtifactPurpose, Set<FileType>> SUPPORTED = Map.of(
            ArtifactPurpose.MATERIAL, EnumSet.of(FileType.PDF, FileType.DOCX, FileType.PPTX, FileType.XLSX),
            ArtifactPurpose.DOCUMENT_IMAGE, EnumSet.of(FileType.PNG, FileType.JPEG),
            ArtifactPurpose.ASSIGNMENT_SOURCE,
            EnumSet.of(FileType.TXT, FileType.DOCX, FileType.PDF, FileType.PNG, FileType.JPEG, FileType.WEBP),
            ArtifactPurpose.ASSIGNMENT_IMPORT, EnumSet.of(FileType.PNG, FileType.JPEG, FileType.WEBP, FileType.PDF));
    static final Map<ArtifactPurpose, Set<Role>> UPLOADERS = Map.of(
            ArtifactPurpose.MATERIAL, EnumSet.of(Role.TEACHER, Role.SUBJECT_MANAGER),
            ArtifactPurpose.DOCUMENT_IMAGE, EnumSet.of(Role.STUDENT, Role.TEACHER, Role.SUBJECT_MANAGER),
            ArtifactPurpose.ASSIGNMENT_SOURCE, EnumSet.of(Role.TEACHER, Role.SUBJECT_MANAGER),
            ArtifactPurpose.ASSIGNMENT_IMPORT, EnumSet.of(Role.TEACHER, Role.SUBJECT_MANAGER));

    private final ISettings settings;

    public PurposePolicy(ISettings settings) {
        this.settings = settings;
    }

    public boolean canUpload(Role role, ArtifactPurpose purpose) {
        return UPLOADERS.get(purpose).contains(role);
    }

    public PurposeLimits resolveLimits(ArtifactPurpose purpose) {
        int cap = HARD_CAP_MB.get(purpose);
        SettingKey<Integer> sizeKey = switch (purpose) {
            case MATERIAL -> MATERIAL_MAX_MB;
            case DOCUMENT_IMAGE -> IMAGE_MAX_MB;
            case ASSIGNMENT_SOURCE, ASSIGNMENT_IMPORT -> null;
        };
        int size = sizeKey == null ? cap
                : settings.get(sizeKey).map(value -> Math.max(1, Math.min(cap, value))).orElse(cap);
        return new PurposeLimits(purpose, size, EnumSet.copyOf(SUPPORTED.get(purpose)));
    }
}
