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
    IMaterialModuleRepository repo;MaterialAccessPolicy access;IQuizQuery quizzes;MaterialModuleService service;
    MaterialModule module,other;

    @BeforeEach void setup() {
        repo=mock(IMaterialModuleRepository.class);access=mock(MaterialAccessPolicy.class);quizzes=mock(IQuizQuery.class);
        service=new MaterialModuleService(repo,access,quizzes,mock(IAudit.class),Clock.fixed(now,ZoneOffset.UTC));
        module=new MaterialModule(moduleId,subject,"Module 1",0,manager.accountId(),now,now,0);
        other=new MaterialModule(otherId,subject,"Module 2",1,manager.accountId(),now,now,0);
        when(repo.find(moduleId)).thenReturn(Optional.of(module));
        when(repo.list(subject)).thenReturn(List.of(module,other));
        when(repo.delete(any(),anyLong())).thenReturn(true);
    }
    QuizView quiz(UUID id,QuizScope scope,UUID subjectId,UUID classOf,QuizStatus status) {
        return new QuizView(id,scope,classOf,subjectId,1,"Quiz",status,null,false,null,List.of(),BigDecimal.TEN);
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
        UUID quizId=UUID.randomUUID();
        when(quizzes.get(quizId)).thenReturn(Optional.of(quiz(quizId,QuizScope.SUBJECT,UUID.randomUUID(),null,QuizStatus.OPEN)));
        assertThatThrownBy(()->service.attach(manager,moduleId,quizId)).isInstanceOf(ApiException.class);
        verify(repo,never()).link(any());
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
}
