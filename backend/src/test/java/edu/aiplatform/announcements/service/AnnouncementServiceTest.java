package edu.aiplatform.announcements.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.announcements.model.*;
import edu.aiplatform.announcements.repository.*;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AnnouncementServiceTest {
    UUID classId=UUID.randomUUID(),teacherId=UUID.randomUUID();Actor teacher=new Actor(teacherId,Role.TEACHER);
    Instant now=Instant.parse("2026-10-21T08:00:00Z");Clock clock=Clock.fixed(now,ZoneOffset.UTC);
    IAnnouncementRepository repo;IAuthorization auth;IClassAccess classes;IAudit audit;IEventPublisher events;IAccountLookup accounts;AnnouncementAccessPolicy access;AnnouncementService service;ClassRef course;
    @BeforeEach void setup(){repo=mock(IAnnouncementRepository.class);auth=mock(IAuthorization.class);classes=mock(IClassAccess.class);audit=mock(IAudit.class);events=mock(IEventPublisher.class);accounts=mock(IAccountLookup.class);
        course=new ClassRef(classId,UUID.randomUUID(),"SWP391","Lớp thử",ClassStatus.OPEN,teacherId,false);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(course));when(auth.authorize(any(),any(),any())).thenReturn(AuthorizationDecision.allow());when(accounts.getContact(any())).thenReturn(Optional.empty());
        access=new AnnouncementAccessPolicy(auth,classes,audit,clock);service=new AnnouncementService(repo,access,classes,accounts,audit,events,clock);}
    Announcement item(){var a=Announcement.create(classId,teacherId,"Tiêu đề","Nội dung **Markdown**",true,true,now.minusSeconds(60));when(repo.find(a.id)).thenReturn(Optional.of(a));return a;}
    @Test void createPublishesOnlyMetadataAndAudits(){var created=service.create(teacher,classId,new AnnouncementCommands.Create(" Mới ","Nội dung",true,true));assertThat(created.title()).isEqualTo("Mới");verify(repo).insert(any());verify(events).publish(argThat(e->e.eventType().equals(EventTypes.CLASS_ANNOUNCEMENT_POSTED)&&!e.payload().containsKey("body")&&e.payload().get("sendEmail").equals(true)));verify(audit).record(argThat(e->e.action().equals("ANNOUNCEMENT_CREATED")&&!e.details().toString().contains("Nội dung")));}
    @Test void editPreservesAuthorAndFlagsAndDoesNotRepublish(){var a=item();when(repo.update(a,0)).thenReturn(true);var edited=service.edit(teacher,a.id,new AnnouncementCommands.Edit("Đổi","Nội dung sửa",0));assertThat(edited.createdBy()).isEqualTo(teacherId);assertThat(edited.createdAt()).isEqualTo(now.minusSeconds(60));assertThat(edited.important()).isTrue();assertThat(edited.sendEmail()).isTrue();verifyNoInteractions(events);verify(audit).record(argThat(e->e.details().containsKey("before")&&e.details().containsKey("after")));}
    @Test void staleVersionDoesNotWrite(){var a=item();assertThatThrownBy(()->service.edit(teacher,a.id,new AnnouncementCommands.Edit("Đổi","Sửa",1))).isInstanceOf(ApiException.class).extracting("code").isEqualTo("ANNOUNCEMENT_STALE");verify(repo,never()).update(any(),anyLong());verifyNoInteractions(events);}
    @Test void concurrentCasFailureDoesNotAuditSuccess(){var a=item();when(repo.update(a,0)).thenReturn(false);assertThatThrownBy(()->service.delete(teacher,a.id,0)).isInstanceOf(ApiException.class);verify(audit,never()).record(argThat(e->e.result()==AuditResult.SUCCESS));}
    @Test void deleteIsSoftAndDoesNotRepublish(){var a=item();when(repo.update(a,0)).thenReturn(true);service.delete(teacher,a.id,0);assertThat(a.status).isEqualTo(Announcement.Status.DELETED);assertThat(a.deletedBy).isEqualTo(teacherId);verifyNoInteractions(events);}
    @ParameterizedTest @EnumSource(value=Role.class,names={"ADMIN","STUDENT"}) void cannotManage(Role role){var actor=new Actor(UUID.randomUUID(),role);assertThatThrownBy(()->access.require(actor,classId,true)).isInstanceOf(ApiException.class);verify(audit).record(argThat(e->e.result()==AuditResult.DENIED));}
    @Test void anotherTeacherCannotManage(){assertThatThrownBy(()->access.require(new Actor(UUID.randomUUID(),Role.TEACHER),classId,true)).isInstanceOf(ApiException.class);}
    @Test void managerMustActuallyTeachTheClass(){assertThat(access.require(new Actor(teacherId,Role.SUBJECT_MANAGER),classId,true)).isEqualTo(course);assertThatThrownBy(()->access.require(new Actor(UUID.randomUUID(),Role.SUBJECT_MANAGER),classId,true)).isInstanceOf(ApiException.class);}
    @Test void activeStudentReadsOnlyOpenEnrolledClass(){var student=new Actor(UUID.randomUUID(),Role.STUDENT);when(classes.isActiveStudent(student.accountId(),classId)).thenReturn(true);assertThat(access.require(student,classId,false)).isEqualTo(course);when(classes.getClassRef(classId)).thenReturn(Optional.of(new ClassRef(classId,course.subjectId(),"C","Closed",ClassStatus.ARCHIVED,teacherId,false)));assertThatThrownBy(()->access.require(student,classId,false)).isInstanceOf(ApiException.class);}
    @Test void authorizationDenialEvenForTeacherIsAudited(){when(auth.authorize(any(),any(),any())).thenReturn(AuthorizationDecision.deny("DISABLED"));assertThatThrownBy(()->access.require(teacher,classId,true)).isInstanceOf(ApiException.class);verify(audit).record(argThat(e->e.action().equals("ACCESS_DENIED")));}
    @Test void invalidMarkdownAndEmailCombinationRejected(){assertThatThrownBy(()->Announcement.create(classId,teacherId,"t","b",false,true,now)).isInstanceOf(ApiException.class);for(String body:List.of("","<script>alert(1)</script>","x".repeat(5001)))assertThatThrownBy(()->Announcement.create(classId,teacherId,"t",body,false,false,now)).isInstanceOf(ApiException.class);}
    @Test void filteredFeedKeepsAllClassOptions(){when(classes.listOpenClassesOf(teacherId)).thenReturn(List.of(course));when(repo.list(anyList(),anyInt(),anyInt())).thenReturn(List.of());var result=service.list(teacher,classId,1,20);assertThat(result.classes()).hasSize(1);assertThat(result.items()).isEmpty();}
    @Test void badPaginationRejected(){assertThatThrownBy(()->service.list(teacher,null,1,51)).isInstanceOf(ApiException.class);}
}
