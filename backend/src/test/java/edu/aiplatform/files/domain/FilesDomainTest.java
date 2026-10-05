package edu.aiplatform.files.domain;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.files.port.ArtifactPurpose;
import edu.aiplatform.shared.contract.Role;
import org.junit.jupiter.api.Test;

/** BR-U03-02, 04, 08, 23. */
class FilesDomainTest {

    @Test
    void sizeCapsPerPurpose() {
        assertThat(PurposePolicy.of(ArtifactPurpose.MATERIAL).maxBytes()).isEqualTo(50 * PurposePolicy.MB);
        assertThat(PurposePolicy.of(ArtifactPurpose.AVATAR).maxBytes()).isEqualTo(50 * PurposePolicy.MB);
        assertThat(PurposePolicy.of(ArtifactPurpose.DOCUMENT_IMAGE).maxBytes()).isEqualTo(5 * PurposePolicy.MB);
    }

    @Test
    void uploaderRolesPerPurpose() {
        assertThat(PurposePolicy.of(ArtifactPurpose.AVATAR).uploaderRoles()).containsExactlyInAnyOrder(Role.values());
        assertThat(PurposePolicy.of(ArtifactPurpose.MATERIAL).allowsRole(Role.STUDENT)).isFalse();
        assertThat(PurposePolicy.of(ArtifactPurpose.MATERIAL).allowsRole(Role.SUBJECT_MANAGER)).isTrue();
        assertThat(PurposePolicy.of(ArtifactPurpose.DOCUMENT_IMAGE).allowsRole(Role.STUDENT)).isTrue();
        assertThat(PurposePolicy.of(ArtifactPurpose.DOCUMENT_IMAGE).allowsRole(Role.ADMIN)).isFalse();
    }

    @Test
    void onlyImagesAndPdfAreInline() {
        assertThat(PurposePolicy.isInline("image/svg+xml")).isTrue();
        assertThat(PurposePolicy.isInline(PurposePolicy.PDF)).isTrue();
        assertThat(PurposePolicy.isInline(PurposePolicy.DOCX)).isFalse();
    }

    @Test
    void fileNamesAreSanitized() {
        assertThat(FileNameSanitizer.sanitize("C:\\Users\\x\\..\\bài 1.pdf")).isEqualTo("bài 1.pdf");
        assertThat(FileNameSanitizer.sanitize("../../etc/passwd")).isEqualTo("passwd");
        assertThat(FileNameSanitizer.sanitize("a\u0000b\nc.png")).isEqualTo("abc.png");
        assertThat(FileNameSanitizer.sanitize("  ")).isEqualTo("file");
        assertThat(FileNameSanitizer.sanitize(null)).isEqualTo("file");
        assertThat(FileNameSanitizer.sanitize("x".repeat(300) + ".pdf")).hasSize(255);
    }
}
