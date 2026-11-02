package edu.aiplatform.groupdocs.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.groupdocs.domain.GroupSection.Status;
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
        GroupSection claimed = open().claimedBy(holder, holder, NOW);
        assertThat(claimed.status()).isEqualTo(Status.CLAIMED);
        assertThat(SectionRules.canEdit(claimed, holder)).isNull();
        assertThat(SectionRules.canEdit(claimed, other)).isEqualTo("SECTION_NOT_HELD");
        assertThat(SectionRules.canClaim(claimed, other)).isEqualTo("SECTION_CLAIMED_BY_OTHER");
        assertThat(SectionRules.canClaim(open(), other)).isNull();
    }

    @Test
    void doneMovesDraftToPublishedAndRecordsAuthor() {
        GroupSection done = open().claimedBy(holder, holder, NOW).withDraft(text, NOW).done(NOW);
        assertThat(done.status()).isEqualTo(Status.DONE);
        assertThat(done.publishedBlocks()).isEqualTo(text);
        assertThat(done.draftBlocks()).isNull();
        assertThat(done.claimedBy()).isNull();
        assertThat(done.lastAuthorId()).isEqualTo(holder);
        assertThat(SectionRules.canClaim(done, other)).as("mục Xong nhận lại được").isNull();
    }

    @Test
    void releaseDropsDraftAndReturnsToOpenOrDone() {
        GroupSection empty = open().claimedBy(holder, holder, NOW).withDraft(text, NOW).released();
        assertThat(empty.status()).isEqualTo(Status.OPEN);
        assertThat(empty.draftBlocks()).isNull();

        GroupSection published = open().claimedBy(holder, holder, NOW).withDraft(text, NOW).done(NOW)
                .claimedBy(other, other, NOW).released();
        assertThat(published.status()).isEqualTo(Status.DONE);
        assertThat(published.publishedBlocks()).isEqualTo(text);
    }

    @Test
    void releaseAllowedForHolderLeaderAndTeacherOnly() {
        GroupSection claimed = open().claimedBy(holder, holder, NOW);
        assertThat(SectionRules.canRelease(claimed, holder, false, false)).isNull();
        assertThat(SectionRules.canRelease(claimed, other, true, false)).isNull();
        assertThat(SectionRules.canRelease(claimed, other, false, true)).isNull();
        assertThat(SectionRules.canRelease(claimed, other, false, false)).isEqualTo("SECTION_RELEASE_FORBIDDEN");
        assertThat(SectionRules.canRelease(open(), holder, false, false)).isEqualTo("SECTION_NOT_CLAIMED");
    }
}
