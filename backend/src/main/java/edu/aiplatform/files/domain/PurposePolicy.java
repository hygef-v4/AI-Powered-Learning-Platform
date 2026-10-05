package edu.aiplatform.files.domain;

import edu.aiplatform.files.port.ArtifactPurpose;
import edu.aiplatform.shared.contract.Role;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Allowlist loại tệp, trần dung lượng và vai trò được upload theo `purpose` (BR-U03-02, 03, 04; domain-entities §3). */
public record PurposePolicy(Set<String> mediaTypes, long maxBytes, Set<Role> uploaderRoles) {

    public static final long MB = 1024L * 1024L;

    public static final String PDF = "application/pdf";
    public static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    public static final String SVG = "image/svg+xml";

    private static final Map<ArtifactPurpose, PurposePolicy> POLICIES = new EnumMap<>(Map.of(
            ArtifactPurpose.AVATAR, new PurposePolicy(
                    Set.of("image/jpeg", "image/png", "image/webp"), 50 * MB,
                    Set.of(Role.STUDENT, Role.TEACHER, Role.SUBJECT_MANAGER, Role.ADMIN)),
            ArtifactPurpose.MATERIAL, new PurposePolicy(
                    Set.of(PDF, DOCX, PPTX), 50 * MB,
                    Set.of(Role.TEACHER, Role.SUBJECT_MANAGER)),
            ArtifactPurpose.DOCUMENT_IMAGE, new PurposePolicy(
                    Set.of("image/png", "image/jpeg", "image/gif", SVG), 5 * MB,
                    Set.of(Role.STUDENT, Role.TEACHER, Role.SUBJECT_MANAGER))));

    public static PurposePolicy of(ArtifactPurpose purpose) {
        return POLICIES.get(purpose);
    }

    public boolean allowsRole(Role role) {
        return uploaderRoles.contains(role);
    }

    public boolean allowsMediaType(String mediaType) {
        return mediaTypes.contains(mediaType);
    }

    /** Ảnh và PDF mở trong trình duyệt; còn lại luôn `attachment` (BR-U03-23). */
    public static boolean isInline(String mediaType) {
        return mediaType.startsWith("image/") || PDF.equals(mediaType);
    }
}
