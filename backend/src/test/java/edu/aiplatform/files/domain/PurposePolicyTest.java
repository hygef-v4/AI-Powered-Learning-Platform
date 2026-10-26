package edu.aiplatform.files.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.settings.contract.ISettings;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PurposePolicyTest {

    final ISettings settings = mock(ISettings.class);
    final PurposePolicy policy = new PurposePolicy(settings);

    @Test
    void usesHardCapsAndAllSupportedTypesWhenNotConfigured() {
        when(settings.get(any())).thenReturn(Optional.empty());

        PurposeLimits material = policy.limits(ArtifactPurpose.MATERIAL);
        assertThat(material.maxSizeMb()).isEqualTo(50);
        assertThat(material.allowedTypes()).containsExactlyInAnyOrder(FileType.PDF, FileType.DOCX, FileType.PPTX);
        assertThat(material.allowedTypes()).doesNotContain(FileType.TXT);
        assertThat(policy.limits(ArtifactPurpose.DOCUMENT_IMAGE).maxSizeMb()).isEqualTo(5);
        assertThat(policy.limits(ArtifactPurpose.ASSIGNMENT_SOURCE).maxSizeMb()).isEqualTo(2);
        assertThat(policy.limits(ArtifactPurpose.ASSIGNMENT_SOURCE).allowedTypes())
                .containsExactlyInAnyOrder(FileType.TXT, FileType.DOCX, FileType.PDF);
    }

    @Test
    void readsSizeFromSettingsAndClampsToHardCapWithFixedTypes() {
        when(settings.get(PurposePolicy.MATERIAL_MAX_MB)).thenReturn(Optional.of(10));
        when(settings.get(PurposePolicy.IMAGE_MAX_MB)).thenReturn(Optional.of(99));

        PurposeLimits material = policy.limits(ArtifactPurpose.MATERIAL);
        assertThat(material.maxBytes()).isEqualTo(10L * 1024 * 1024);
        assertThat(material.allowedTypes()).containsExactlyInAnyOrder(FileType.PDF, FileType.DOCX, FileType.PPTX);
        PurposeLimits image = policy.limits(ArtifactPurpose.DOCUMENT_IMAGE);
        assertThat(image.maxSizeMb()).isEqualTo(5);
        assertThat(image.allowedTypes()).containsExactlyInAnyOrder(FileType.PNG, FileType.JPEG);
    }

    @Test
    void onlyEducatorsUploadMaterialsAndAdminUploadsNothing() {
        assertThat(policy.canUpload(Role.TEACHER, ArtifactPurpose.MATERIAL)).isTrue();
        assertThat(policy.canUpload(Role.STUDENT, ArtifactPurpose.MATERIAL)).isFalse();
        assertThat(policy.canUpload(Role.STUDENT, ArtifactPurpose.DOCUMENT_IMAGE)).isTrue();
        assertThat(policy.canUpload(Role.ADMIN, ArtifactPurpose.DOCUMENT_IMAGE)).isFalse();
        assertThat(policy.canUpload(Role.ADMIN, ArtifactPurpose.MATERIAL)).isFalse();
        assertThat(policy.canUpload(Role.TEACHER, ArtifactPurpose.ASSIGNMENT_SOURCE)).isTrue();
        assertThat(policy.canUpload(Role.SUBJECT_MANAGER, ArtifactPurpose.ASSIGNMENT_SOURCE)).isTrue();
        assertThat(policy.canUpload(Role.STUDENT, ArtifactPurpose.ASSIGNMENT_SOURCE)).isFalse();
    }
}
