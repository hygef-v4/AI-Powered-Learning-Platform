package edu.aiplatform.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Role;
import org.junit.jupiter.api.Test;

class McpRolePolicyTest {
    @Test void learnerAndEducatorsCanConnectButAdminCannot() {
        assertThat(AccessPolicy.ruleFor(Actions.MCP_SELF).orElseThrow().roles())
                .containsExactlyInAnyOrder(Role.STUDENT, Role.TEACHER, Role.SUBJECT_MANAGER);
        assertThat(AccessPolicy.ruleFor(Actions.AI_ADMIN).orElseThrow().roles())
                .containsExactly(Role.ADMIN);
        assertThat(AccessPolicy.ruleFor(Actions.AI_SHARED_USE).orElseThrow().roles())
                .doesNotContain(Role.STUDENT, Role.ADMIN);
    }
}
