package edu.aiplatform.quizzes.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.junit.jupiter.api.Test;

class QuizMcpAuthoringServiceTest {
    final QuizAuthoringService authoring=mock(QuizAuthoringService.class);
    final QuizAiDraftService drafts=mock(QuizAiDraftService.class);
    final QuizAccess access=mock(QuizAccess.class);
    final IMcpWriteRequest writes=mock(IMcpWriteRequest.class);
    final QuizMcpAuthoringService service=new QuizMcpAuthoringService(authoring,drafts,access,writes,new ObjectMapper());
    @Test void ownershipLossPreventsReplay() {
        Actor actor=new Actor(UUID.randomUUID(),Role.TEACHER); UUID id=UUID.randomUUID();
        doThrow(ApiException.notFound("CLASS_NOT_FOUND")).when(access).requireClass(eq(actor),anyString(),eq(id),anyString(),eq(id),anyString());
        assertThatThrownBy(() -> service.call(actor,"create_quiz_draft",Map.of("classId",id.toString(),"requestRef","retry",
                "title","Quiz","questions",List.of()))) .isInstanceOf(ApiException.class);
        verifyNoInteractions(authoring,writes);
    }
    @Test void rejectsStudentBeforeReadingTargets() {
        assertThatThrownBy(() -> service.call(new Actor(UUID.randomUUID(),Role.STUDENT),"create_quiz_draft",Map.of()))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(authoring,writes);
    }
    @Test void invalidQuestionCannotCreateContainer() {
        var input=Map.<String,Object>of("classId",UUID.randomUUID().toString(),"title","Invalid","requestRef","bad",
                "questions",List.of(Map.of("type","MCQ_SINGLE","stem","Question","options",List.of("A","A"),"correct",List.of(1,2))));
        assertThatThrownBy(() -> service.call(new Actor(UUID.randomUUID(),Role.TEACHER),"create_quiz_draft",input)).isInstanceOf(ApiException.class);
        verifyNoInteractions(authoring,writes);
    }
}
