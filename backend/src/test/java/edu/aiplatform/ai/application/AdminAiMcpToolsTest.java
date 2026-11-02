package edu.aiplatform.ai.application;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.junit.jupiter.api.Test;
class AdminAiMcpToolsTest {
    final QuotaPolicyService quota=mock(QuotaPolicyService.class);
    final AiConnectionService connections=mock(AiConnectionService.class);
    final AdminAiMcpTools tools=new AdminAiMcpTools(quota,connections);
    @Test void teachingRolesAndStudentCannotReadAdminReports() {
        for(var role:List.of(Role.TEACHER,Role.SUBJECT_MANAGER,Role.STUDENT))
            assertThatThrownBy(() -> tools.call(new Actor(UUID.randomUUID(),role),"read_ai_connection_status",Map.of())).isInstanceOf(ApiException.class);
        verifyNoInteractions(quota,connections);
    }
    @Test void connectionsUseOnlyMaskedDtosAndToolsAreReadOnly() {
        var admin=new Actor(UUID.randomUUID(),Role.ADMIN);
        when(connections.listInstitutional(admin)).thenReturn(List.of());
        assertThat(tools.call(admin,"read_ai_connection_status",Map.of()).error()).isFalse();
        assertThat(tools.tools()).allMatch(t -> t.kind()==edu.aiplatform.ai.contract.McpToolKind.READ && t.allowedRoles().equals(Set.of(Role.ADMIN)));
    }
}
