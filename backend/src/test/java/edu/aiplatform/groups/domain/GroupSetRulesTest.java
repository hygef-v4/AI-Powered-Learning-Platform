package edu.aiplatform.groups.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import edu.aiplatform.groups.domain.GroupSetRules.Draft;
import edu.aiplatform.groups.domain.GroupSetRules.Issue;
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
    void everyProblemIsReportedPerGroup() {
        List<Issue> issues = GroupSetRules.validate(List.of(
                new Draft("g1", null, "Nhóm 1", null, List.of(a)),
                new Draft("g2", null, "nhóm 1", b, List.of(a, b)),
                new Draft("g3", null, " ", null, List.of()),
                new Draft("g4", null, "Nhóm 4", UUID.randomUUID(), List.of(c, UUID.randomUUID()))), active);

        assertThat(issues).extracting(Issue::groupKey, Issue::code).contains(
                tuple("g1", "GROUP_WITHOUT_LEADER"),
                tuple("g2", "GROUP_NAME_DUPLICATE"),
                tuple("g2", "MEMBER_IN_TWO_GROUPS"),
                tuple("g3", "GROUP_NAME_INVALID"),
                tuple("g3", "GROUP_WITHOUT_MEMBER"),
                tuple("g4", "GROUP_WITHOUT_LEADER"),
                tuple("g4", "MEMBER_NOT_ACTIVE"));
    }

    @Test
    void nameLongerThanHundredIsRejected() {
        assertThat(GroupSetRules.validate(List.of(new Draft("g1", null, "x".repeat(101), a, List.of(a))), active))
                .extracting(Issue::code).containsExactly("GROUP_NAME_INVALID");
    }
}
