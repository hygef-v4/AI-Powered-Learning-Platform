package edu.aiplatform.groups.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import edu.aiplatform.groups.model.GroupSetRules.Draft;
import edu.aiplatform.groups.model.GroupSetRules.Issue;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroupSetRulesTest {

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID c = UUID.randomUUID();
    private final Set<UUID> active = Set.of(a, b, c);

    @Test
    void validSetHasNoIssue() {
        assertThat(GroupSetRules.validate(List.of(new Draft("g1", null, "Nhóm 1", a, List.of(a, b)),
                new Draft("g2", null, "Nhóm 2", c, List.of(c))), active)).isEmpty();
    }

    @Test
    void nameLongerThanHundredIsRejected() {
        assertThat(GroupSetRules.validate(List.of(new Draft("g1", null, "x".repeat(101), a, List.of(a))), active))
                .extracting(Issue::code).containsExactly("GROUP_NAME_INVALID");
    }
}
