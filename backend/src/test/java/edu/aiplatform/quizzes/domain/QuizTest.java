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
        assertThat(open.isEditable()).isFalse();
        assertThat(open.isDeletable()).isFalse();
        assertThatThrownBy(() -> open.edited(NOW)).isInstanceOf(IllegalStateException.class);

        Quiz retired = open.retired("Hết dùng", NOW);
        assertThat(retired.status()).isEqualTo(QuizStatus.RETIRED);
        assertThat(retired.retiredReason()).isEqualTo("Hết dùng");
        assertThatThrownBy(() -> draft().published(NOW)).isInstanceOf(IllegalStateException.class);
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
