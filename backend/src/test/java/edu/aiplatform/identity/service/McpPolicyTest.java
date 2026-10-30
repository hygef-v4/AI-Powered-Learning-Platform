package edu.aiplatform.identity.service;

import static org.assertj.core.api.Assertions.*;
import edu.aiplatform.identity.contract.*;
import org.junit.jupiter.api.Test;

class McpPolicyTest {
    @Test void mcpAdmissionIsSeparateFromPersonalAiAndAcademicPermissions() {
        assertThat(AccessPolicy.ruleFor("MCP_SELF").orElseThrow().roles())
                .containsExactlyInAnyOrder(Role.STUDENT, Role.TEACHER, Role.SUBJECT_MANAGER);
        assertThat(AccessPolicy.ruleFor(Actions.AI_PERSONAL).orElseThrow().roles())
                .contains(Role.STUDENT).doesNotContain(Role.ADMIN);
        assertThat(AccessPolicy.ruleFor(Actions.GRADING_WRITE).orElseThrow().roles()).doesNotContain(Role.ADMIN);
    }
}
