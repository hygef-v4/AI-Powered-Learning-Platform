package edu.aiplatform.quizzes.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.junit.jupiter.api.Test;
class QuestionBankMcpToolsTest {
    final QuestionBankService bank=mock(QuestionBankService.class);
    final QuizAccess access=mock(QuizAccess.class);
    final IMcpWriteRequest writes=mock(IMcpWriteRequest.class);
    final QuestionBankMcpTools tools=new QuestionBankMcpTools(bank,null,access,null,writes,new ObjectMapper());
    final Actor manager=new Actor(UUID.randomUUID(),Role.SUBJECT_MANAGER);
    @Test void detailedContextCannotMixSubjectAndQuestionIds() {
        UUID subject=UUID.randomUUID(),question=UUID.randomUUID();
        var view=mock(QuestionViews.QuestionView.class);when(view.subjectId()).thenReturn(UUID.randomUUID());
        when(bank.detail(manager,question)).thenReturn(new QuestionViews.QuestionDetail(view,List.of(),0,null));
        assertThatThrownBy(() -> tools.call(manager,"read_question_bank_context",Map.of("subjectId",subject.toString(),"questionId",question.toString())))
                .isInstanceOf(ApiException.class);
    }
    @Test void foreignSubjectAndNonManagerCannotReadOrWriteBank() {
        for(var role:List.of(Role.TEACHER,Role.ADMIN,Role.STUDENT))
            assertThatThrownBy(() -> tools.call(new Actor(UUID.randomUUID(),role),"list_bank_questions",Map.of())).isInstanceOf(ApiException.class);
        UUID subject=UUID.randomUUID();
        doThrow(ApiException.notFound("SUBJECT_NOT_FOUND")).when(access).requireSubject(eq(manager),anyString(),eq(subject),anyString(),eq(subject),anyString());
        assertThatThrownBy(() -> tools.call(manager,"save_question_bank_draft",Map.of("subjectId",subject.toString(),"requestRef","retry")))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(bank,writes);
    }
}
