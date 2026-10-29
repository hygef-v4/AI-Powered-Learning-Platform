package edu.aiplatform.files.domain;

import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.settings.contract.SettingKey;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Quy tắc theo {@code purpose}: role được tải lên (BR-U03-04), trần dung lượng và loại cho phép lấy từ Settings
 * (BR-U03-02, 87). Giá trị trong Settings luôn bị kẹp trong trần cứng 50 MB và 5 MB.
 */
@Component
public class PurposePolicy {

    public static final SettingKey<Integer> MATERIAL_MAX_MB = new SettingKey<>("files.material.maxSizeMb", Integer.class);
    public static final SettingKey<String[]> MATERIAL_TYPES = new SettingKey<>("files.material.allowedTypes", String[].class);
    public static final SettingKey<Integer> IMAGE_MAX_MB = new SettingKey<>("files.documentImage.maxSizeMb", Integer.class);
    public static final SettingKey<String[]> IMAGE_TYPES = new SettingKey<>("files.documentImage.allowedTypes", String[].class);

    static final Map<ArtifactPurpose, Integer> HARD_CAP_MB = Map.of(ArtifactPurpose.MATERIAL, 50,
            ArtifactPurpose.DOCUMENT_IMAGE, 5);
    static final Map<ArtifactPurpose, Set<FileType>> SUPPORTED = Map.of(
            ArtifactPurpose.MATERIAL, EnumSet.of(FileType.PDF, FileType.DOCX, FileType.PPTX),
            ArtifactPurpose.DOCUMENT_IMAGE, EnumSet.of(FileType.PNG, FileType.JPEG, FileType.GIF, FileType.SVG));
    static final Map<ArtifactPurpose, Set<Role>> UPLOADERS = Map.of(
            ArtifactPurpose.MATERIAL, EnumSet.of(Role.TEACHER, Role.SUBJECT_MANAGER),
            ArtifactPurpose.DOCUMENT_IMAGE, EnumSet.of(Role.STUDENT, Role.TEACHER, Role.SUBJECT_MANAGER));

    private final ISettings settings;

    public PurposePolicy(ISettings settings) {
        this.settings = settings;
    }

    public boolean canUpload(Role role, ArtifactPurpose purpose) {
        return UPLOADERS.get(purpose).contains(role);
    }

    public PurposeLimits limits(ArtifactPurpose purpose) {
        int cap = HARD_CAP_MB.get(purpose);
        SettingKey<Integer> sizeKey = purpose == ArtifactPurpose.MATERIAL ? MATERIAL_MAX_MB : IMAGE_MAX_MB;
        SettingKey<String[]> typesKey = purpose == ArtifactPurpose.MATERIAL ? MATERIAL_TYPES : IMAGE_TYPES;
        int size = settings.get(sizeKey).map(value -> Math.max(1, Math.min(cap, value))).orElse(cap);
        Set<FileType> supported = SUPPORTED.get(purpose);
        Set<FileType> allowed = settings.get(typesKey)
                .map(names -> Arrays.stream(names).map(FileType::valueOf).filter(supported::contains)
                        .collect(() -> EnumSet.noneOf(FileType.class), Set::add, Set::addAll))
                .filter(set -> !set.isEmpty())
                .orElseGet(() -> EnumSet.copyOf(supported));
        return new PurposeLimits(purpose, size, allowed);
    }
}
