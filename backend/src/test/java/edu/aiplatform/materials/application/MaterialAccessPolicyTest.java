package edu.aiplatform.materials.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.domain.Material;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MaterialAccessPolicyTest {
    final UUID account=UUID.randomUUID(),subject=UUID.randomUUID(),classId=UUID.randomUUID(),other=UUID.randomUUID();
    final Instant now=Instant.parse("2026-10-21T00:00:00Z");
    IAuthorization auth;ISubjectQuery subjects;IClassAccess classes;IClassScope scopes;IAudit audit;MaterialAccessPolicy policy;
    @BeforeEach void setup() {
        auth=mock(IAuthorization.class);subjects=mock(ISubjectQuery.class);classes=mock(IClassAccess.class);
        scopes=mock(IClassScope.class);audit=mock(IAudit.class);policy=new MaterialAccessPolicy(auth,subjects,classes,scopes,audit,Clock.fixed(now,ZoneOffset.UTC));
        when(auth.authorize(any(),anyString(),any())).thenReturn(AuthorizationDecision.allow());
        when(subjects.getSubject(subject)).thenReturn(Optional.of(new SubjectRef(subject,"S","Subject",SubjectStatus.ACTIVE,account)));
        when(classes.getClassRef(classId)).thenReturn(Optional.of(courseClass(ClassStatus.OPEN,account)));
        when(classes.isActiveStudent(account,classId)).thenReturn(true);
        when(scopes.listTaughtClasses(account)).thenReturn(List.of(classId));
        when(classes.listOpenClassesOf(account)).thenReturn(List.of(courseClass(ClassStatus.OPEN,account)));
    }
    Material material(boolean classScope,Material.Status status) {
        return new Material(UUID.randomUUID(),classScope?Material.Scope.CLASS:Material.Scope.SUBJECT,subject,classScope?classId:null,new UUID(0,1),
                Material.Kind.FILE,"Title","",0,status,1,account,now,now,0);
    }
    ClassRef courseClass(ClassStatus status,UUID teacher) {return new ClassRef(classId,subject,"C","Class",status,teacher,false);}
    @ParameterizedTest @EnumSource(value=Role.class,names={"TEACHER","SUBJECT_MANAGER"})
    void assignedTeacherReadsAndWritesOwnClass(Role role) {
        Actor actor=new Actor(account,role);var m=material(true,Material.Status.ACTIVE);
        policy.read(actor,m,classId);policy.write(actor,m);verifyNoInteractions(audit);
    }
    @Test void managerWithoutTeachingAssignmentCannotReadPrivateClass() {
        when(classes.getClassRef(classId)).thenReturn(Optional.of(courseClass(ClassStatus.OPEN,other)));
        denied(()->policy.read(new Actor(account,Role.SUBJECT_MANAGER),material(true,Material.Status.ACTIVE),classId));
    }
    @Test void learnerFromOtherClassSameSubjectCannotReadPrivateMaterial() {
        UUID second=UUID.randomUUID();when(classes.getClassRef(second)).thenReturn(Optional.of(new ClassRef(second,subject,"C2","Other",ClassStatus.OPEN,other,false)));
        denied(()->policy.read(new Actor(account,Role.STUDENT),material(true,Material.Status.ACTIVE),second));
    }
    @Test void learnerReadsSubjectAndOwnClassActiveMaterial() {
        policy.read(new Actor(account,Role.STUDENT),material(false,Material.Status.ACTIVE),classId);
        policy.read(new Actor(account,Role.STUDENT),material(true,Material.Status.ACTIVE),classId);verifyNoInteractions(audit);
    }
    @Test void archivedMaterialIsHiddenFromStudent() {denied(()->policy.read(new Actor(account,Role.STUDENT),material(false,Material.Status.ARCHIVED),classId));}
    @ParameterizedTest @EnumSource(value=ClassStatus.class,names={"DRAFT","ARCHIVED"})
    void learnerNeedsOpenClass(ClassStatus status) {
        when(classes.getClassRef(classId)).thenReturn(Optional.of(courseClass(status,account)));
        denied(()->policy.read(new Actor(account,Role.STUDENT),material(false,Material.Status.ACTIVE),classId));
    }
    @Test void removedStudentCannotRead() {
        when(classes.isActiveStudent(account,classId)).thenReturn(false);
        denied(()->policy.read(new Actor(account,Role.STUDENT),material(false,Material.Status.ACTIVE),classId));
    }
    @Test void teacherReadsInheritedSubjectMaterialOfArchivedClass() {
        when(classes.getClassRef(classId)).thenReturn(Optional.of(courseClass(ClassStatus.ARCHIVED,account)));
        policy.read(new Actor(account,Role.TEACHER),material(false,Material.Status.ACTIVE),null);
    }
    @Test void archivedClassPreventsWritesButNotStaffRead() {
        when(classes.getClassRef(classId)).thenReturn(Optional.of(courseClass(ClassStatus.ARCHIVED,account)));
        var m=material(true,Material.Status.ACTIVE);Actor actor=new Actor(account,Role.TEACHER);policy.read(actor,m,classId);
        assertThatThrownBy(()->policy.write(actor,m)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.status().value()).isEqualTo(409));
    }
    @Test void adminCannotReadEvenWhenAuthorizationMockAllows() {denied(()->policy.read(new Actor(account,Role.ADMIN),material(false,Material.Status.ACTIVE),null));verifyNoInteractions(auth);}
    @Test void deniedCurrentAccountOverridesActorRoleAndScope() {
        when(auth.authorize(any(),anyString(),any())).thenReturn(AuthorizationDecision.deny("ACCOUNT_NOT_ACTIVE"));
        denied(()->policy.read(new Actor(account,Role.SUBJECT_MANAGER),material(false,Material.Status.ACTIVE),null));
    }
    @Test void unavailableAuthorizationFailsClosed() {
        when(auth.authorize(any(),anyString(),any())).thenThrow(new IllegalStateException("private data"));
        assertThatThrownBy(()->policy.read(new Actor(account,Role.SUBJECT_MANAGER),material(false,Material.Status.ACTIVE),null))
                .isInstanceOfSatisfying(ApiException.class,e->{assertThat(e.status().value()).isEqualTo(503);assertThat(e.getMessage()).doesNotContain("private data");});
    }
    @Test void managerReadsAndManagesOwnSubjectButCannotListOrReadAnUnassignedSubject() {
        Actor manager=new Actor(account,Role.SUBJECT_MANAGER);
        policy.subjectList(manager,subject);
        policy.read(manager,material(false,Material.Status.ARCHIVED),null);
        policy.write(manager,material(false,Material.Status.ACTIVE));
        assertThat(policy.canManageModules(manager,subject)).isTrue();
        verify(auth,atLeastOnce()).authorize(manager,Actions.MATERIAL_SUBJECT_MANAGE,ResourceRef.subject(subject));
        verifyNoInteractions(audit);

        when(auth.authorize(manager,Actions.MATERIAL_SUBJECT_MANAGE,ResourceRef.subject(other)))
                .thenReturn(AuthorizationDecision.deny("OUT_OF_SUBJECT_SCOPE"));
        denied(()->policy.subjectList(manager,other));
    }

    @Test void foreignSubjectManagerCannotReadDetailOrChangeModules() {
        Actor manager=new Actor(other,Role.SUBJECT_MANAGER);
        when(auth.authorize(eq(manager),anyString(),any())).thenReturn(AuthorizationDecision.deny("OUT_OF_SUBJECT_SCOPE"));
        denied(()->policy.read(manager,material(false,Material.Status.ACTIVE),null));
        assertThat(policy.canManageModules(manager,subject)).isFalse();
        assertThat(policy.canManageQuiz(manager,subject,null)).isFalse();
    }
    void denied(Runnable call) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.status().value()).isEqualTo(404));
        verify(audit).record(argThat(event->event.action().equals("ACCESS_DENIED")&&event.result()==AuditResult.DENIED));
    }
}
