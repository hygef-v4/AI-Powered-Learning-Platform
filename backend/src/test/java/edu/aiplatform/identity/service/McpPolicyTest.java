package edu.aiplatform.identity.service;

import static org.assertj.core.api.Assertions.*;
import edu.aiplatform.identity.contract.*;
import org.junit.jupiter.api.Test;

class McpPolicyTest {
    @Test void mcpAdmissionIsSeparateFromPersonalAiAndAcademicPermissions() {
        assertThat(AccessPolicy.findRule("MCP_SELF").orElseThrow().roles())
                .containsExactlyInAnyOrder(Role.STUDENT, Role.TEACHER, Role.SUBJECT_MANAGER);
        assertThat(AccessPolicy.findRule(Actions.AI_PERSONAL).orElseThrow().roles())
                .contains(Role.STUDENT).doesNotContain(Role.ADMIN);
        assertThat(AccessPolicy.findRule(Actions.GRADING_WRITE).orElseThrow().roles()).doesNotContain(Role.ADMIN);
    }
}
