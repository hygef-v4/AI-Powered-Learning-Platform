package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.junit.jupiter.api.Test;

class AssignmentMcpAuthoringServiceTest {
    final AssignmentAuthoringService authoring=mock(AssignmentAuthoringService.class);
    final AssignmentAiDraftService drafts=mock(AssignmentAiDraftService.class);
    final AssignmentAccess access=mock(AssignmentAccess.class);
    final IMcpWriteRequest writes=mock(IMcpWriteRequest.class);
    final AssignmentMcpAuthoringService service=new AssignmentMcpAuthoringService(authoring,drafts,access,writes,new ObjectMapper());
    final Actor actor=new Actor(UUID.randomUUID(),Role.TEACHER);
    @Test void rejectsAmbiguousScopeBeforeAnyCreation() {
        assertThatThrownBy(() -> service.call(actor,"create_assignment_draft",Map.of("classId",UUID.randomUUID().toString(),
                "subjectId",UUID.randomUUID().toString(),"requestRef","x","draft",Map.of())))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(authoring,writes);
    }
    @Test void replayRechecksOwnershipBeforeReturningStoredIds() {
        UUID id=UUID.randomUUID();
        doThrow(ApiException.notFound("CLASS_NOT_FOUND")).when(access).requireClassManager(eq(actor),eq(id),anyString());
        assertThatThrownBy(() -> service.call(actor,"create_assignment_draft",Map.of("classId",id.toString(),
                "requestRef","replay","title","Draft","draft",Map.of())))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(writes,authoring);
    }
    @Test void adminCannotCreateAcademicDrafts() {
        assertThatThrownBy(() -> service.call(new Actor(UUID.randomUUID(),Role.ADMIN),"create_assignment_draft",Map.of()))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(authoring,writes);
    }
}
