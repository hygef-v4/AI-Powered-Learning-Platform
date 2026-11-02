package edu.aiplatform.quizzes.contract.stub;

import edu.aiplatform.quizzes.contract.AnswerReview;
import edu.aiplatform.quizzes.contract.IQuizQuery;
import edu.aiplatform.quizzes.contract.IQuizScoring;
import edu.aiplatform.quizzes.contract.QuestionScore;
import edu.aiplatform.quizzes.contract.QuizAnswer;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizScore;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.contract.QuizStudentView;
import edu.aiplatform.quizzes.contract.QuizView;
import edu.aiplatform.shared.stub.Stubs;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Bản tạm của interface U10: Quiz mẫu đang mở chưa có câu, mọi câu trả lời được 0 điểm, chưa có đáp án để xem. */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class QuizzesContractStubs {

    static QuizView sampleQuiz(UUID quizId) {
        return new QuizView(quizId, QuizScope.CLASS, Stubs.derivedId("class", quizId), Stubs.derivedId("subject", quizId),
                1, "Quiz mẫu", QuizStatus.OPEN, new QuizSettings(false, false, null, null, true, true), false, null,
                List.of(), BigDecimal.ZERO);
    }

    @Bean
    @ConditionalOnMissingBean
    IQuizQuery quizQueryStub() {
        return Stubs.announce(IQuizQuery.class, new IQuizQuery() {
            @Override
            public List<QuizView> listForClass(UUID classId) {
                return List.of();
            }

            @Override
            public Optional<QuizStudentView> getStudentView(UUID quizId) {
                return Optional.of(new QuizStudentView(sampleQuiz(quizId), "", List.of()));
            }

            @Override
            public List<QuizView> listByMaterial(UUID materialId, UUID classId) {
                return List.of();
            }

            @Override
            public Optional<QuizView> get(UUID quizId) {
                return Optional.of(sampleQuiz(quizId));
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    IQuizScoring quizScoringStub() {
        return Stubs.announce(IQuizScoring.class, new IQuizScoring() {
            @Override
            public QuizScore score(UUID quizId, List<QuizAnswer> answers) {
                List<QuestionScore> scores = answers.stream()
                        .map(answer -> new QuestionScore(answer.questionId(), false, BigDecimal.ZERO, BigDecimal.ZERO))
                        .toList();
                return new QuizScore(scores, BigDecimal.ZERO, BigDecimal.ZERO);
            }

            @Override
            public AnswerReview answerReview(UUID quizId) {
                return new AnswerReview(quizId, List.of());
            }
        });
    }
}
