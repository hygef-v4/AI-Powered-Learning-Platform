package edu.aiplatform.quizzes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** BR-U10-20, 24, 31: vòng đời Quiz, xóa khi chưa phát hành, bản sao về nháp. */
class QuizTest {

    private static final Instant NOW = Instant.parse("2026-10-20T08:00:00Z");
    private final UUID subject = UUID.randomUUID();
    private final UUID author = UUID.randomUUID();

    private Quiz draft() {
        return Quiz.create(QuizScope.SUBJECT, null, subject, "Luyện tập", author, NOW);
    }

    @Test
    void lifecycleGoesDraftReviewedOpenRetired() {
        Quiz reviewed = draft().reviewed(NOW);
        assertThat(reviewed.status()).isEqualTo(QuizStatus.REVIEWED);
        assertThat(reviewed.edited(NOW).status()).as("sửa Quiz đã duyệt về nháp").isEqualTo(QuizStatus.DRAFT);

        Quiz open = reviewed.published(NOW);
        assertThat(open.status()).isEqualTo(QuizStatus.OPEN);
        assertThat(open.isEditable()).as("quiz đang mở sửa tại chỗ").isTrue();
        assertThat(open.isDeletable()).isFalse();
        assertThat(open.edited(NOW).status()).as("sửa quiz đang mở vẫn mở").isEqualTo(QuizStatus.OPEN);

        Quiz retired = open.retired("Hết dùng", NOW);
        assertThat(retired.status()).isEqualTo(QuizStatus.RETIRED);
        assertThat(retired.retiredReason()).isEqualTo("Hết dùng");
        assertThat(retired.reopened(NOW).status()).as("trùng thời điểm vẫn là Quiz từng phát hành")
                .isEqualTo(QuizStatus.OPEN);
        assertThatThrownBy(() -> draft().published(NOW)).isInstanceOf(IllegalStateException.class);

        Quiz later = open.retired("Hết dùng", NOW.plusSeconds(60));
        assertThat(later.reopened(NOW.plusSeconds(120)).status()).as("quiz đã phát hành mở lại").isEqualTo(QuizStatus.OPEN);
        assertThat(later.reopened(NOW.plusSeconds(120)).retiredAt()).isNull();
        Quiz archivedDraft = draft().retired("Lưu trữ", NOW);
        assertThat(archivedDraft.publishedAt()).as("lưu trữ không phải phát hành").isNull();
        assertThat(archivedDraft.reopened(NOW.plusSeconds(60)).status()).as("lưu trữ khi còn nháp thì về nháp").isEqualTo(QuizStatus.DRAFT);
        assertThat(archivedDraft.reopened(NOW.plusSeconds(60)).publishedAt()).isNull();
        assertThat(archivedDraft.reopened(NOW.plusSeconds(60)).isDeletable()).isTrue();
    }

    @Test
    void copyStartsAsDraftWithoutPublicationHistory() {
        Quiz open = draft().withDetails("Bài", "Hướng dẫn", new QuizSettings(true, true, 30, 3, true, true), false,
                null, List.of(UUID.randomUUID()), NOW).reviewed(NOW).published(NOW);

        Quiz copy = open.copyAs(QuizOrigin.NEW_VERSION, 2, null, author, NOW);

        assertThat(copy.id()).isNotEqualTo(open.id());
        assertThat(copy.sourceQuizId()).isEqualTo(open.id());
        assertThat(copy.status()).isEqualTo(QuizStatus.DRAFT);
        assertThat(copy.publishedAt()).isNull();
        assertThat(copy.versionNo()).isEqualTo(2);
        assertThat(copy.settings()).isEqualTo(open.settings());
        assertThat(copy.referenceMaterialIds()).isEqualTo(open.referenceMaterialIds());
        assertThat(copy.isDeletable()).isTrue();
    }

    @Test
    void itemPointsMustBePositiveWithTwoDecimals() {
        assertThat(QuizRules.validPoints(new BigDecimal("0.25"))).isTrue();
        assertThat(QuizRules.validPoints(BigDecimal.ZERO)).isFalse();
        assertThat(QuizRules.validPoints(new BigDecimal("0.125"))).isFalse();
        assertThat(QuizRules.validateSettings(new QuizSettings(false, false, 0, 0, true, false)))
                .extracting(Violation::code).containsExactly("TIME_LIMIT_INVALID", "ATTEMPTS_INVALID");
    }
}
