package edu.aiplatform.identity.service;

import static org.assertj.core.api.Assertions.*;
import edu.aiplatform.identity.contract.*;
import org.junit.jupiter.api.Test;

class GradingPolicyTest {
    @Test void gradingRequiresTeachingScopeAndRejectsAdminAndStudent() {
        for (String action : new String[]{"GRADING_REVIEW","GRADING_WRITE","GRADEBOOK_READ","GRADEBOOK_EXPORT"}) {
            var rule=AccessPolicy.ruleFor(action).orElseThrow();
            assertThat(rule.scope()).isEqualTo(ScopeKind.CLASS);
            assertThat(rule.roles()).containsExactlyInAnyOrder(Role.TEACHER,Role.SUBJECT_MANAGER);
        }
    }
    @Test void studentGradesRequireClassScope() {
        var rule=AccessPolicy.ruleFor("MY_GRADE_READ").orElseThrow();
        assertThat(rule.scope()).isEqualTo(ScopeKind.CLASS);
        assertThat(rule.roles()).containsExactly(Role.STUDENT);
    }
}
