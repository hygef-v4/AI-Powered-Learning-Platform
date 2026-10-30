package edu.aiplatform.materials.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.domain.*;
import edu.aiplatform.quizzes.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class MaterialModuleServiceTest {
    final UUID subject=UUID.randomUUID(),classId=UUID.randomUUID(),moduleId=UUID.randomUUID(),otherId=UUID.randomUUID();
    final Actor manager=new Actor(UUID.randomUUID(),Role.SUBJECT_MANAGER);
    final Instant now=Instant.parse("2026-10-29T00:00:00Z");
    IMaterialModuleRepository repo;MaterialAccessPolicy access;IQuizQuery quizzes;MaterialModuleService service;IAudit audit;
    MaterialModule module,other;

    @BeforeEach void setup() {
        repo=mock(IMaterialModuleRepository.class);access=mock(MaterialAccessPolicy.class);quizzes=mock(IQuizQuery.class);
        audit=mock(IAudit.class);
        service=new MaterialModuleService(repo,access,quizzes,audit,Clock.fixed(now,ZoneOffset.UTC));
        module=new MaterialModule(moduleId,subject,"Module 1",0,manager.accountId(),now,now,0);
        other=new MaterialModule(otherId,subject,"Module 2",1,manager.accountId(),now,now,0);
        when(repo.find(moduleId)).thenReturn(Optional.of(module));
        when(repo.list(subject)).thenReturn(List.of(module,other));
        when(repo.delete(any(),anyLong())).thenReturn(true);
    }
    QuizView quiz(UUID id,QuizScope scope,UUID subjectId,UUID classOf,QuizStatus status) {
        return new QuizView(id,scope,classOf,subjectId,1,"Quiz",status,null,false,null,List.of(),BigDecimal.TEN,
                status==QuizStatus.OPEN||status==QuizStatus.RETIRED?now:null);
    }

    @Test void deleteRefusesModuleWithActiveMaterials() {
        when(repo.activeMaterials(moduleId)).thenReturn(2);
        assertThatThrownBy(()->service.delete(manager,moduleId,0))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("MATERIAL_MODULE_NOT_EMPTY"));
        verify(repo,never()).delete(any(),anyLong());
    }
    @Test void deleteMovesArchivedMaterialsToAnotherModule() {
        when(repo.archivedMaterials(moduleId)).thenReturn(1);
        service.delete(manager,moduleId,0);
        verify(repo).moveArchivedMaterials(moduleId,otherId);verify(repo).delete(moduleId,0);
    }
    @Test void deleteOfStaleVersionConflicts() {
        assertThatThrownBy(()->service.delete(manager,moduleId,5)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.status().value()).isEqualTo(409));
    }
    @Test void attachRejectsQuizOfAnotherSubject() {
        UUID quizId=UUID.randomUUID(),foreignSubject=UUID.randomUUID();
        when(quizzes.get(quizId)).thenReturn(Optional.of(quiz(quizId,QuizScope.SUBJECT,foreignSubject,null,QuizStatus.OPEN)));
        when(access.canManageQuiz(manager,foreignSubject,null)).thenReturn(true);
        assertThatThrownBy(()->service.attach(manager,moduleId,quizId)).isInstanceOfSatisfying(ApiException.class,e->{
            assertThat(e.status().value()).isEqualTo(400);
            assertThat(e.code()).isEqualTo("MATERIAL_QUIZ_INVALID");
        });
        verify(repo,never()).link(any());verify(repo,never()).unlink(any(),any());verifyNoInteractions(audit);
    }
    @Test void attachMovesQuizFromItsPreviousModule() {
        UUID quizId=UUID.randomUUID();
        when(quizzes.get(quizId)).thenReturn(Optional.of(quiz(quizId,QuizScope.SUBJECT,subject,null,QuizStatus.DRAFT)));
        when(access.canManageQuiz(manager,subject,null)).thenReturn(true);
        when(repo.quizLink(quizId)).thenReturn(Optional.of(new MaterialModule.QuizLink(otherId,quizId,0)));
        service.attach(manager,moduleId,quizId);
        verify(repo).unlink(otherId,quizId);verify(repo).link(new MaterialModule.QuizLink(moduleId,quizId,0));
    }
    @Test void studentsSeeOnlyOpenQuizzesOfTheirClass() {
        Actor student=new Actor(UUID.randomUUID(),Role.STUDENT);
        UUID open=UUID.randomUUID(),draft=UUID.randomUUID();
        when(access.classList(student,classId,true)).thenReturn(new ClassRef(classId,subject,"C","Class",ClassStatus.OPEN,null,false));
        when(repo.quizLinks(subject)).thenReturn(List.of(new MaterialModule.QuizLink(moduleId,open,0),new MaterialModule.QuizLink(moduleId,draft,1)));
        when(quizzes.get(open)).thenReturn(Optional.of(quiz(open,QuizScope.SUBJECT,subject,null,QuizStatus.OPEN)));
        when(quizzes.get(draft)).thenReturn(Optional.of(quiz(draft,QuizScope.SUBJECT,subject,null,QuizStatus.DRAFT)));
        when(quizzes.listForClass(classId)).thenReturn(List.of(quiz(open,QuizScope.SUBJECT,subject,null,QuizStatus.OPEN)));
        var views=service.forClass(student,classId,true);
        assertThat(views).hasSize(2);
        assertThat(views.get(0).quizzes()).extracting(MaterialViews.ModuleQuiz::quizId).containsExactly(open);
        assertThat(views.get(0).canManage()).isFalse();
    }

    @Test void teacherCannotSeeNeverPublishedRetiredSubjectQuizInMaterialModule() {
        Actor teacher=new Actor(UUID.randomUUID(),Role.TEACHER);
        UUID quizId=UUID.randomUUID();
        QuizView unpublished=new QuizView(quizId,QuizScope.SUBJECT,null,subject,1,"Unpublished",QuizStatus.RETIRED,
                null,false,null,List.of(),BigDecimal.TEN,null);
        when(access.classList(teacher,classId,false)).thenReturn(new ClassRef(classId,subject,"C","Class",ClassStatus.OPEN,teacher.accountId(),false));
        when(repo.quizLinks(subject)).thenReturn(List.of(new MaterialModule.QuizLink(moduleId,quizId,0)));
        when(quizzes.get(quizId)).thenReturn(Optional.of(unpublished));
        assertThat(service.forClass(teacher,classId,false).get(0).quizzes()).isEmpty();
    }

    @Test void lastModuleWithArchivedMaterialCannotBeDeleted() {
        when(repo.list(subject)).thenReturn(List.of(module));
        when(repo.archivedMaterials(moduleId)).thenReturn(1);

        assertThatThrownBy(()->service.delete(manager,moduleId,0))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("MATERIAL_MODULE_LAST"));

        verify(repo,never()).moveArchivedMaterials(any(),any());verify(repo,never()).delete(any(),anyLong());
        verifyNoInteractions(audit);
    }

    @Test void lostDeleteCasDoesNotAuditSuccess() {
        when(repo.delete(moduleId,0)).thenReturn(false);
        assertThatThrownBy(()->service.delete(manager,moduleId,0))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
        verifyNoInteractions(audit);
    }

    @Test void lostRenameCasDoesNotAuditSuccess() {
        when(repo.update(any(),anyLong())).thenReturn(false);
        assertThatThrownBy(()->service.rename(manager,moduleId,new MaterialCommands.ModuleUpdate("New",0L)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
        verifyNoInteractions(audit);
    }

    @Test void reorderRejectsDuplicateStaleOrForeignModulesBeforeAnyWrite() {
        var first=new MaterialCommands.Position(moduleId,0L);
        assertThatThrownBy(()->service.reorder(manager,subject,new MaterialCommands.Reorder(List.of(first,first))))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("MATERIAL_REORDER_INVALID"));
        when(repo.find(otherId)).thenReturn(Optional.of(other));
        assertThatThrownBy(()->service.reorder(manager,subject,new MaterialCommands.Reorder(List.of(first,new MaterialCommands.Position(otherId,1L)))))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
        when(repo.find(otherId)).thenReturn(Optional.of(new MaterialModule(otherId,UUID.randomUUID(),"Foreign",0,manager.accountId(),now,now,0)));
        assertThatThrownBy(()->service.reorder(manager,subject,new MaterialCommands.Reorder(List.of(first,new MaterialCommands.Position(otherId,0L)))))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("MATERIAL_NOT_FOUND"));
        verify(repo,never()).update(any(),anyLong());verifyNoInteractions(audit);
    }

    @Test void reorderPersistsRequestedPositionsAndRejectsLostCas() {
        when(repo.find(otherId)).thenReturn(Optional.of(other));
        when(repo.update(any(),anyLong())).thenReturn(true);
        var command=new MaterialCommands.Reorder(List.of(new MaterialCommands.Position(otherId,0L),new MaterialCommands.Position(moduleId,0L)));
        service.reorder(manager,subject,command);
        verify(repo).update(argThat(m->m.id().equals(otherId)&&m.orderNo()==0&&m.version()==1),eq(0L));
        verify(repo).update(argThat(m->m.id().equals(moduleId)&&m.orderNo()==1&&m.version()==1),eq(0L));

        when(repo.update(any(),anyLong())).thenReturn(false);
        assertThatThrownBy(()->service.reorder(manager,subject,command))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
    }

    @Test void subjectViewExcludesEveryClassQuizWhileTeacherViewKeepsOnlyOwnClassAndPublishedSubjectQuizzes() {
        Actor teacher=new Actor(UUID.randomUUID(),Role.TEACHER);
        UUID ownDraft=UUID.randomUUID(),ownRetired=UUID.randomUUID(),foreignOpen=UUID.randomUUID(),
                subjectDraft=UUID.randomUUID(),subjectOpen=UUID.randomUUID(),subjectRetired=UUID.randomUUID();
        List<QuizView> rows=List.of(
                quiz(ownDraft,QuizScope.CLASS,subject,classId,QuizStatus.DRAFT),
                quiz(ownRetired,QuizScope.CLASS,subject,classId,QuizStatus.RETIRED),
                quiz(foreignOpen,QuizScope.CLASS,subject,UUID.randomUUID(),QuizStatus.OPEN),
                quiz(subjectDraft,QuizScope.SUBJECT,subject,null,QuizStatus.DRAFT),
                quiz(subjectOpen,QuizScope.SUBJECT,subject,null,QuizStatus.OPEN),
                quiz(subjectRetired,QuizScope.SUBJECT,subject,null,QuizStatus.RETIRED));
        when(repo.quizLinks(subject)).thenReturn(rows.stream().map(q->new MaterialModule.QuizLink(moduleId,q.quizId(),0)).toList());
        rows.forEach(q->when(quizzes.get(q.quizId())).thenReturn(Optional.of(q)));
        when(access.canManageModules(manager,subject)).thenReturn(true);
        when(access.canManageQuiz(manager,subject,null)).thenReturn(true);

        var subjectView=service.forSubject(manager,subject).get(0);
        assertThat(subjectView.canManage()).isTrue();
        assertThat(subjectView.quizzes()).extracting(MaterialViews.ModuleQuiz::quizId)
                .containsExactly(subjectDraft,subjectOpen,subjectRetired);
        assertThat(subjectView.quizzes()).allSatisfy(q->assertThat(q.canManage()).isTrue());

        when(access.classList(teacher,classId,false)).thenReturn(new ClassRef(classId,subject,"C","Class",ClassStatus.OPEN,teacher.accountId(),false));
        when(access.canManageQuiz(teacher,subject,classId)).thenReturn(true);
        var classView=service.forClass(teacher,classId,false).get(0);
        assertThat(classView.canManage()).isFalse();
        assertThat(classView.quizzes()).extracting(MaterialViews.ModuleQuiz::quizId)
                .containsExactly(ownDraft,ownRetired,subjectOpen,subjectRetired);
        assertThat(classView.quizzes()).filteredOn(q->q.scope()==QuizScope.CLASS).allSatisfy(q->assertThat(q.canManage()).isTrue());
        assertThat(classView.quizzes()).filteredOn(q->q.scope()==QuizScope.SUBJECT).allSatisfy(q->assertThat(q.canManage()).isFalse());
    }
}
