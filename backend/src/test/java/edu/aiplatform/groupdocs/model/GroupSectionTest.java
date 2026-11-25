package edu.aiplatform.groupdocs.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.groupdocs.model.GroupSection.Status;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroupSectionTest {

    private static final Instant NOW = Instant.parse("2026-10-20T03:00:00Z");
    private final UUID holder = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final List<DocumentBlock> text = List.of(new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT,
            BlockType.PARAGRAPH, JsonNodeFactory.instance.objectNode().put("text", "Nội dung")));

    private GroupSection open() {
        return new GroupSection(UUID.randomUUID(), UUID.randomUUID(), "p1", null, List.of(), 1, "1. Tổng quan", Status.OPEN,
                null, null, null, null, null, null, null, 0);
    }

    @Test
    void claimStartsDraftFromPublishedAndOnlyHolderEdits() {
        GroupSection claimed = open().markClaimed(holder, holder, NOW);
        assertThat(claimed.status()).isEqualTo(Status.CLAIMED);
        assertThat(SectionRules.canEdit(claimed, holder)).isNull();
        assertThat(SectionRules.canEdit(claimed, other)).isEqualTo("SECTION_NOT_HELD");
        assertThat(SectionRules.canClaim(claimed, other)).isEqualTo("SECTION_CLAIMED_BY_OTHER");
        assertThat(SectionRules.canClaim(open(), other)).isNull();
    }

    @Test
    void releaseAllowedForHolderLeaderAndTeacherOnly() {
        GroupSection claimed = open().markClaimed(holder, holder, NOW);
        assertThat(SectionRules.canRelease(claimed, holder, false, false)).isNull();
        assertThat(SectionRules.canRelease(claimed, other, true, false)).isNull();
        assertThat(SectionRules.canRelease(claimed, other, false, true)).isNull();
        assertThat(SectionRules.canRelease(claimed, other, false, false)).isEqualTo("SECTION_RELEASE_FORBIDDEN");
        assertThat(SectionRules.canRelease(open(), holder, false, false)).isEqualTo("SECTION_NOT_CLAIMED");
    }
}
