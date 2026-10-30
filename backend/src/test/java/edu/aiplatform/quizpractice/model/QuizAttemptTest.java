package edu.aiplatform.quizpractice.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.contract.QuizOptionView;
import edu.aiplatform.quizzes.contract.QuizQuestionView;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.contract.QuizStudentView;
import edu.aiplatform.quizzes.contract.QuizView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** BR-U11-11 (chụp, trộn theo seed), BR-U11-12, 13 (hạn và 30 giây ân hạn), BR-U11-15 (nộp một lần). */
class QuizAttemptTest {

    private static final Instant NOW = Instant.parse("2026-10-20T08:00:00Z");

    static QuizStudentView view(boolean shuffle, Integer timeLimit, int questions) {
        QuizSettings settings = new QuizSettings(shuffle, shuffle, timeLimit, 2, true, true);
        QuizView quiz = new QuizView(UUID.randomUUID(), QuizScope.CLASS, UUID.randomUUID(), UUID.randomUUID(), 3,
                "Luyện tập", QuizStatus.OPEN, settings, false, null, List.of(), new BigDecimal(questions));
        List<QuizQuestionView> list = IntStream.range(0, questions).mapToObj(i -> new QuizQuestionView(UUID.randomUUID(),
                QuestionType.MCQ_SINGLE, "Câu " + i, List.of(new QuizOptionView("1", "A"), new QuizOptionView("2", "B"),
                new QuizOptionView("3", "C"), new QuizOptionView("4", "D")), BigDecimal.ONE)).toList();
        return new QuizStudentView(quiz, null, list);
    }

    @Test
    void sameSeedGivesSameShuffledOrderAndNoShuffleKeepsOrder() {
        QuizStudentView view = view(true, null, 12);
        AttemptSnapshot a = AttemptSnapshot.of(view, 42);
        AttemptSnapshot b = AttemptSnapshot.of(view, 42);
        assertThat(a.questionOrder()).isEqualTo(b.questionOrder()).containsExactlyInAnyOrderElementsOf(
                view.questions().stream().map(QuizQuestionView::questionId).toList());
        assertThat(a.optionOrder()).isEqualTo(b.optionOrder());
        assertThat(AttemptSnapshot.of(view, 7).questionOrder()).isNotEqualTo(a.questionOrder());

        QuizStudentView plain = view(false, null, 5);
        AttemptSnapshot kept = AttemptSnapshot.of(plain, 42);
        assertThat(kept.questionOrder()).isEqualTo(plain.questions().stream().map(QuizQuestionView::questionId).toList());
        assertThat(kept.optionOrder().values()).allSatisfy(o -> assertThat(o).containsExactly("1", "2", "3", "4"));
        assertThat(kept.versionNo()).isEqualTo(3);
        UUID first = kept.questionOrder().get(0);
        assertThat(kept.contains(first, List.of("2"))).isTrue();
        assertThat(kept.contains(first, List.of("9"))).isFalse();
        assertThat(kept.contains(UUID.randomUUID(), List.of())).isFalse();
    }

    @Test
    void deadlineFollowsTimeLimitWithThirtySecondsGrace() {
        QuizAttempt timed = QuizAttempt.start(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                AttemptSnapshot.of(view(false, 10, 1), 1), NOW);
        assertThat(timed.deadlineAt()).isEqualTo(NOW.plusSeconds(600));
        assertThat(timed.acceptsAnswersAt(NOW.plusSeconds(629))).isTrue();
        assertThat(timed.acceptsAnswersAt(NOW.plusSeconds(631))).isFalse();
        assertThat(timed.overdueAt(NOW.plusSeconds(631))).isTrue();

        QuizAttempt untimed = QuizAttempt.start(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                AttemptSnapshot.of(view(false, null, 1), 1), NOW);
        assertThat(untimed.deadlineAt()).isNull();
        assertThat(untimed.acceptsAnswersAt(NOW.plusSeconds(86_400))).isTrue();
        assertThat(untimed.overdueAt(NOW.plusSeconds(86_400))).isFalse();
    }

    @Test
    void savingBumpsVersionAndSubmittedAttemptIsFinal() {
        QuizAttempt attempt = QuizAttempt.start(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                AttemptSnapshot.of(view(false, null, 1), 1), NOW);
        UUID q = attempt.snapshot().questionOrder().get(0);
        QuizAttempt saved = attempt.withAnswers(Map.of(q, List.of("2")), NOW.plusSeconds(5));
        assertThat(saved.answerVersion()).isEqualTo(1);
        assertThat(saved.lastSavedAt()).isEqualTo(NOW.plusSeconds(5));

        QuizAttempt submitted = saved.submitted(SubmitMode.MANUAL, BigDecimal.ONE, BigDecimal.ONE,
                List.of(new QuestionResult(q, true, BigDecimal.ONE, BigDecimal.ONE)), NOW.plusSeconds(10));
        assertThat(submitted.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(submitted.correctCount()).isEqualTo(1);
        assertThat(submitted.acceptsAnswersAt(NOW)).isFalse();
        assertThatThrownBy(() -> submitted.withAnswers(Map.of(), NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> submitted.submitted(SubmitMode.MANUAL, BigDecimal.ZERO, BigDecimal.ONE, List.of(), NOW))
                .isInstanceOf(IllegalStateException.class);
    }
}
