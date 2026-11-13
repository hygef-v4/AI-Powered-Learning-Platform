package edu.aiplatform.materials.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.service.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.IMaterialRepository;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.*;
import java.io.ByteArrayInputStream;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MaterialDownloadPolicyTest {
    @Test
    void studentGrantIsRecheckedAfterDisableWhileStaffCanStillReadHistory() throws Exception {
        UUID subject = UUID.randomUUID(), classId = UUID.randomUUID(), managerId = UUID.randomUUID();
        Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);
        Actor manager = new Actor(managerId, Role.SUBJECT_MANAGER);
        Instant now = Instant.parse("2026-11-09T00:00:00Z");
        Material material = new Material(UUID.randomUUID(), Material.Scope.SUBJECT, subject, null,
                UUID.randomUUID(), Material.Kind.FILE, "Title", "", 0, Material.Status.ACTIVE,
                1, managerId, now, now, 0);
        var current = new AtomicReference<>(material);
        var repository = mock(IMaterialRepository.class);
        when(repository.findByFileId("file")).thenAnswer(call -> List.of(current.get()));
        when(repository.version(material.id(), 1)).thenReturn(Optional.of(new MaterialVersion(material.id(),
                1, "file", "ref.pdf", "application/pdf", 1L, null, null,
                MaterialVersion.TextStatus.EXTRACTED, "source", managerId, now)));
        var authorization = mock(IAuthorization.class);
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        var subjects = mock(ISubjectQuery.class);
        when(subjects.getSubject(subject)).thenReturn(Optional.of(new SubjectRef(subject, "S", "Subject",
                SubjectStatus.ACTIVE, managerId)));
        var classes = mock(IClassAccess.class);
        when(classes.listOpenClassesOf(student.accountId())).thenReturn(List.of(new ClassRef(classId, subject,
                "C", "Class", ClassStatus.OPEN, managerId, false)));
        when(classes.isActiveStudent(student.accountId(), classId)).thenReturn(true);
        var access = new MaterialAccessPolicy(authorization, subjects, classes, mock(IClassScope.class),
                mock(IAudit.class), Clock.fixed(now, ZoneOffset.UTC));
        var tokens = mock(DownloadTokenService.class);
        when(tokens.resolve("student-token")).thenReturn(Optional.of(new DownloadTokenService.Grant("file", student.accountId())));
        when(tokens.resolve("staff-token")).thenReturn(Optional.of(new DownloadTokenService.Grant("file", managerId)));
        var storage = mock(IStorage.class);
        when(storage.metadata("file")).thenReturn(Optional.of(new StoredMetadata(managerId,
                ArtifactPurpose.MATERIAL, "ref.pdf", "application/pdf", 1, "sha")));
        when(storage.open("file")).thenAnswer(call -> new ByteArrayInputStream(new byte[] {1}));
        var downloadService = new DownloadService(tokens, storage, mock(FileAudit.class),
                List.of(new MaterialDownloadPolicy(repository, access)));

        try (var content = downloadService.open(student, "student-token").content()) {
            assertThat(content.read()).isEqualTo(1);
        }
        current.set(material.withPresentation(false, null));
        assertThatThrownBy(() -> downloadService.open(student, "student-token"))
                .isInstanceOfSatisfying(ApiException.class, failure -> assertThat(failure.code()).isEqualTo("MATERIAL_NOT_FOUND"));
        verify(storage, times(1)).open("file");
        try (var content = downloadService.open(manager, "staff-token").content()) {
            assertThat(content.read()).isEqualTo(1);
        }
        current.set(material.withPresentation(true, null));
        try (var content = downloadService.open(student, "student-token").content()) {
            assertThat(content.read()).isEqualTo(1);
        }
        current.set(material.revise(material.title(), "", 0, 2, Material.Status.ACTIVE, now));
        assertThatThrownBy(() -> downloadService.open(student, "student-token"))
                .isInstanceOf(ApiException.class);
        verify(storage, times(3)).open("file");
    }
}
