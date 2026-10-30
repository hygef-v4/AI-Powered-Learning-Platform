package edu.aiplatform.quizzes.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** BR-U10-04, 05: phiên bản bất biến khi đã kích hoạt, nháp mới cùng lineage, chỉ xóa nháp chưa từng kích hoạt. */
class QuestionTest {

    private static final Instant NOW = Instant.parse("2026-10-20T08:00:00Z");
    private final UUID subject = UUID.randomUUID();
    private final UUID author = UUID.randomUUID();

    @Test
    void newBankQuestionStartsAsDraftVersionOneOfItsOwnLineage() {
        Question q = Question.newBankDraft(subject, QuestionRulesTest.single("3", "4"), author, NOW);
        assertThat(q.lineageId()).isEqualTo(q.id());
        assertThat(q.versionNo()).isEqualTo(1);
        assertThat(q.scope()).isEqualTo(QuestionScope.BANK);
        assertThat(q.status()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(q.isDeletable()).isTrue();
    }

    @Test
    void activeQuestionIsNotEditableAndOpensNextDraftInSameLineage() {
        Question active = Question.newBankDraft(subject, QuestionRulesTest.single("3", "4"), author, NOW)
                .activate(NOW.plusSeconds(60));
        assertThat(active.status()).isEqualTo(QuestionStatus.ACTIVE);
        assertThat(active.activatedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(active.isDeletable()).isFalse();
        assertThatThrownBy(() -> active.withContent(QuestionRulesTest.single("5", "6"), NOW))
                .isInstanceOf(IllegalStateException.class);

        Question draft = active.nextDraft(2, author, NOW.plusSeconds(120));
        assertThat(draft.id()).isNotEqualTo(active.id());
        assertThat(draft.lineageId()).isEqualTo(active.lineageId());
        assertThat(draft.versionNo()).isEqualTo(2);
        assertThat(draft.status()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(draft.activatedAt()).isNull();
        assertThat(draft.content()).isEqualTo(active.content());
    }

    @Test
    void onlyActiveQuestionsRetireAndDraftsDoNotOpenDrafts() {
        Question draft = Question.newBankDraft(subject, QuestionRulesTest.single("3", "4"), author, NOW);
        assertThatThrownBy(() -> draft.retire(NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> draft.nextDraft(2, author, NOW)).isInstanceOf(IllegalStateException.class);
        Question retired = draft.activate(NOW).retire(NOW);
        assertThat(retired.status()).isEqualTo(QuestionStatus.RETIRED);
        assertThat(retired.isDeletable()).isFalse();
        assertThatThrownBy(() -> retired.activate(NOW)).isInstanceOf(IllegalStateException.class);
    }
}
