package edu.aiplatform.materials.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.model.Material;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MaterialAccessPolicyTest {
    @Test
    void disabledMaterialIsPrivateToStaffUntilEnabled() {
        var disabled = material(false, Material.Status.ACTIVE).changePresentation(false, null);
        assertThatThrownBy(
                () -> materialAccessPolicy.requireReadAccess(
                        new Actor(account, Role.STUDENT), disabled, classId))
                .isInstanceOf(ApiException.class);
        assertThatCode(
                () -> materialAccessPolicy.requireReadAccess(
                        new Actor(account, Role.SUBJECT_MANAGER), disabled, null))
                .doesNotThrowAnyException();
        assertThatCode(
                () -> materialAccessPolicy.requireReadAccess(
                        new Actor(account, Role.STUDENT),
                        disabled.changePresentation(true, null),
                        classId)).doesNotThrowAnyException();
    }

    final UUID account = UUID.randomUUID(),
            subject = UUID.randomUUID(),
            classId = UUID.randomUUID(),
            other = UUID.randomUUID();
    final Instant now = Instant.parse("2026-10-21T00:00:00Z");
    IAuthorization authorization;
    ISubjectQuery subjectQuery;
    IClassAccess classAccess;
    IClassScope classScope;
    IAudit audit;
    MaterialAccessPolicy materialAccessPolicy;

    @BeforeEach
    void setup() {
        authorization = mock(IAuthorization.class);
        subjectQuery = mock(ISubjectQuery.class);
        classAccess = mock(IClassAccess.class);
        classScope = mock(IClassScope.class);
        audit = mock(IAudit.class);
        materialAccessPolicy =
                new MaterialAccessPolicy(
                        authorization, subjectQuery, classAccess, classScope, audit, Clock.fixed(
                                now, ZoneOffset.UTC));
        when(authorization.authorize(any(), anyString(), any()))
                .thenReturn(AuthorizationDecision.allow());
        when(subjectQuery.getSubject(subject))
                .thenReturn(
                        Optional.of(
                                new SubjectRef(
                                        subject, "S", "Subject", SubjectStatus.ACTIVE, account)));
        when(classAccess.getClassRef(classId))
                .thenReturn(Optional.of(courseClass(ClassStatus.OPEN, account)));
        when(classAccess.isActiveStudent(account, classId)).thenReturn(true);
        when(classScope.listTaughtClasses(account)).thenReturn(List.of(classId));
        when(classAccess.listOpenClassesOf(account))
                .thenReturn(List.of(courseClass(ClassStatus.OPEN, account)));
    }

    Material material(boolean classScope, Material.Status status) {
        return new Material(
                UUID.randomUUID(),
                classScope ? Material.Scope.CLASS : Material.Scope.SUBJECT,
                subject,
                classScope ? classId : null,
                new UUID(0, 1),
                Material.Kind.FILE,
                "Title",
                "",
                0,
                status,
                1,
                account,
                now,
                now,
                0);
    }

    ClassRef courseClass(ClassStatus status, UUID teacher) {
        return new ClassRef(classId, subject, "C", "Class", status, teacher, false);
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"TEACHER", "SUBJECT_MANAGER"})
    void assignedTeacherReadsAndWritesOwnClass(Role role) {
        Actor actor = new Actor(account, role);
        var m = material(true, Material.Status.ACTIVE);
        materialAccessPolicy.requireReadAccess(actor, m, classId);
        materialAccessPolicy.requireWriteAccess(actor, m);
        verifyNoInteractions(audit);
    }

    @Test
    void managerWithoutTeachingAssignmentCannotReadPrivateClass() {
        when(classAccess.getClassRef(classId))
                .thenReturn(Optional.of(courseClass(ClassStatus.OPEN, other)));
        denied(
                () ->
                        materialAccessPolicy.requireReadAccess(
                                new Actor(account, Role.SUBJECT_MANAGER),
                                material(true, Material.Status.ACTIVE),
                                classId));
    }

    @Test
    void learnerFromOtherClassSameSubjectCannotReadPrivateMaterial() {
        UUID second = UUID.randomUUID();
        when(classAccess.getClassRef(second))
                .thenReturn(
                        Optional.of(
                                new ClassRef(
                                        second,
                                        subject,
                                        "C2",
                                        "Other",
                                        ClassStatus.OPEN,
                                        other,
                                        false)));
        denied(
                () ->
                        materialAccessPolicy.requireReadAccess(
                                new Actor(account, Role.STUDENT),
                                material(true, Material.Status.ACTIVE),
                                second));
    }

    @Test
    void learnerReadsSubjectAndOwnClassActiveMaterial() {
        materialAccessPolicy.requireReadAccess(
                new Actor(account, Role.STUDENT), material(false, Material.Status.ACTIVE), classId);
        materialAccessPolicy.requireReadAccess(
                new Actor(account, Role.STUDENT), material(true, Material.Status.ACTIVE), classId);
        verifyNoInteractions(audit);
    }

    @Test
    void archivedMaterialIsHiddenFromStudent() {
        denied(
                () ->
                        materialAccessPolicy.requireReadAccess(
                                new Actor(account, Role.STUDENT),
                                material(false, Material.Status.ARCHIVED),
                                classId));
    }

    @ParameterizedTest
    @EnumSource(
            value = ClassStatus.class,
            names = {"DRAFT", "ARCHIVED"})
    void learnerNeedsOpenClass(ClassStatus status) {
        when(classAccess.getClassRef(classId))
                .thenReturn(Optional.of(courseClass(status, account)));
        denied(
                () ->
                        materialAccessPolicy.requireReadAccess(
                                new Actor(account, Role.STUDENT),
                                material(false, Material.Status.ACTIVE),
                                classId));
    }

    @Test
    void removedStudentCannotRead() {
        when(classAccess.isActiveStudent(account, classId)).thenReturn(false);
        denied(
                () ->
                        materialAccessPolicy.requireReadAccess(
                                new Actor(account, Role.STUDENT),
                                material(false, Material.Status.ACTIVE),
                                classId));
    }

    @Test
    void teacherReadsInheritedSubjectMaterialOfArchivedClass() {
        when(classAccess.getClassRef(classId))
                .thenReturn(Optional.of(courseClass(ClassStatus.ARCHIVED, account)));
        materialAccessPolicy.requireReadAccess(
                new Actor(account, Role.TEACHER), material(false, Material.Status.ARCHIVED), null);
    }

    @Test
    void archivedClassPreventsWritesButNotStaffRead() {
        when(classAccess.getClassRef(classId))
                .thenReturn(Optional.of(courseClass(ClassStatus.ARCHIVED, account)));
        var m = material(true, Material.Status.ACTIVE);
        Actor actor = new Actor(account, Role.TEACHER);
        materialAccessPolicy.requireReadAccess(actor, m, classId);
        assertThatThrownBy(() -> materialAccessPolicy.requireWriteAccess(actor, m))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.readStatus().value()).isEqualTo(409));
    }

    @Test
    void archivedSubjectBlocksWritesEvenWhenAuthorizationAllowsAndRetainsTeacherRead() {
        when(subjectQuery.getSubject(subject)).thenReturn(Optional.of(new SubjectRef(
                subject, "S", "Subject", SubjectStatus.ARCHIVED, null)));
        Actor manager = new Actor(account, Role.SUBJECT_MANAGER);
        Material active = material(false, Material.Status.ACTIVE);
        materialAccessPolicy.requireReadAccess(new Actor(account, Role.TEACHER), active, classId);
        assertThatThrownBy(() -> materialAccessPolicy.requireWriteAccess(manager, active))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.readStatus().value()).isEqualTo(409);
                    assertThat(e.readCode()).isEqualTo("MATERIAL_SCOPE_READ_ONLY");
                });
        assertThatThrownBy(() -> materialAccessPolicy.requireModuleManagement(manager, subject))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_SCOPE_READ_ONLY"));
        verifyNoInteractions(audit);
    }

    @Test
    void unavailableSubjectAndClassFailClosedWithoutPrivateDetails() {
        when(subjectQuery.getSubject(subject))
                .thenThrow(new IllegalStateException("private-subject"));
        assertThatThrownBy(
                () -> materialAccessPolicy.requireSubjectListAccess(
                        new Actor(account, Role.SUBJECT_MANAGER), subject))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.readStatus().value()).isEqualTo(503);
                    assertThat(e.getMessage()).doesNotContain("private-subject");
                });
        when(classAccess.getClassRef(classId))
                .thenThrow(new IllegalStateException("private-class"));
        assertThatThrownBy(
                () -> materialAccessPolicy.requireClassListAccess(
                        new Actor(account, Role.TEACHER), classId, false))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.readStatus().value()).isEqualTo(503);
                    assertThat(e.getMessage()).doesNotContain("private-class");
                });
    }

    @Test
    void adminCannotReadEvenWhenAuthorizationMockAllows() {
        denied(
                () ->
                        materialAccessPolicy.requireReadAccess(
                                new Actor(account, Role.ADMIN),
                                material(false, Material.Status.ACTIVE),
                                null));
        verifyNoInteractions(authorization);
    }

    @Test
    void deniedCurrentAccountOverridesActorRoleAndScope() {
        when(authorization.authorize(any(), anyString(), any()))
                .thenReturn(AuthorizationDecision.deny("ACCOUNT_NOT_ACTIVE"));
        denied(
                () ->
                        materialAccessPolicy.requireReadAccess(
                                new Actor(account, Role.SUBJECT_MANAGER),
                                material(false, Material.Status.ACTIVE),
                                null));
    }

    @Test
    void unavailableAuthorizationFailsClosed() {
        when(authorization.authorize(any(), anyString(), any()))
                .thenThrow(new IllegalStateException("private data"));
        assertThatThrownBy(
                        () ->
                                materialAccessPolicy.requireReadAccess(
                                        new Actor(account, Role.SUBJECT_MANAGER),
                                        material(false, Material.Status.ACTIVE),
                                        null))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> {
                            assertThat(e.readStatus().value()).isEqualTo(503);
                            assertThat(e.getMessage()).doesNotContain("private data");
                        });
    }

    @Test
    void managerReadsAndManagesOwnSubjectButCannotListOrReadAnUnassignedSubject() {
        Actor manager = new Actor(account, Role.SUBJECT_MANAGER);
        materialAccessPolicy.requireSubjectListAccess(manager, subject);
        materialAccessPolicy.requireReadAccess(
                manager, material(false, Material.Status.ARCHIVED), null);
        materialAccessPolicy.requireWriteAccess(manager, material(false, Material.Status.ACTIVE));
        assertThat(materialAccessPolicy.canManageModules(manager, subject)).isTrue();
        verify(authorization, atLeastOnce())
                .authorize(manager, Actions.MATERIAL_SUBJECT_MANAGE, ResourceRef.subject(subject));
        verifyNoInteractions(audit);

        when(
                authorization.authorize(
                        manager, Actions.MATERIAL_SUBJECT_MANAGE, ResourceRef.subject(other)))
                .thenReturn(AuthorizationDecision.deny("OUT_OF_SUBJECT_SCOPE"));
        denied(() -> materialAccessPolicy.requireSubjectListAccess(manager, other));
    }

    @Test
    void foreignSubjectManagerCannotReadDetailOrChangeModules() {
        Actor manager = new Actor(other, Role.SUBJECT_MANAGER);
        when(authorization.authorize(eq(manager), anyString(), any()))
                .thenReturn(AuthorizationDecision.deny("OUT_OF_SUBJECT_SCOPE"));
        denied(
                () -> materialAccessPolicy.requireReadAccess(
                        manager, material(false, Material.Status.ACTIVE), null));
        assertThat(materialAccessPolicy.canManageModules(manager, subject)).isFalse();
        assertThat(materialAccessPolicy.canManageQuiz(manager, subject, null)).isFalse();
    }

    void denied(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.readStatus().value()).isEqualTo(404));
        verify(audit)
                .record(
                        argThat(
                                event ->
                                        event.action().equals("ACCESS_DENIED")
                                                && event.result() == AuditResult.DENIED));
    }
}
