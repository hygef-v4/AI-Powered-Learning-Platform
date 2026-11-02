package edu.aiplatform.quizzes.application;

import edu.aiplatform.quizzes.contract.AnswerKey;
import edu.aiplatform.quizzes.contract.AnswerReview;
import edu.aiplatform.quizzes.contract.IQuizScoring;
import edu.aiplatform.quizzes.contract.QuestionScore;
import edu.aiplatform.quizzes.contract.QuizAnswer;
import edu.aiplatform.quizzes.contract.QuizScore;
import edu.aiplatform.quizzes.domain.IQuestionRepository;
import edu.aiplatform.quizzes.domain.IQuizRepository;
import edu.aiplatform.quizzes.domain.Question;
import edu.aiplatform.quizzes.domain.QuizItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link IQuizScoring} cho U11 (F14, BR-U10-16): chấm theo đáp án của phiên bản đã ghim; đáp án không rời U10
 * ngoài {@link #answerReview}. {@code MCQ_SINGLE} và {@code MCQ_MULTI} đều phải chọn đúng tập đáp án đúng.
 */
@Service
public class QuizScorer implements IQuizScoring {

    private final IQuizRepository quizzes;
    private final IQuestionRepository questions;

    public QuizScorer(IQuizRepository quizzes, IQuestionRepository questions) {
        this.quizzes = quizzes;
        this.questions = questions;
    }

    @Override
    @Transactional(readOnly = true)
    public QuizScore score(UUID quizId, List<QuizAnswer> answers) {
        List<QuizItem> items = quizzes.items(quizId);
        Map<UUID, Question> byId = byId(items);
        Map<UUID, Set<String>> selected = new HashMap<>();
        for (QuizAnswer answer : answers == null ? List.<QuizAnswer>of() : answers) {
            selected.put(answer.questionId(), answer.selectedOptionIds() == null ? Set.of() : answer.selectedOptionIds());
        }
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        List<QuestionScore> scores = new ArrayList<>();
        for (QuizItem item : items) {
            Question q = byId.get(item.questionId());
            Set<String> correct = q == null ? Set.of() : new HashSet<>(q.content().correctOptionIds());
            boolean ok = q != null && !correct.isEmpty() && correct.equals(selected.getOrDefault(q.id(), Set.of()));
            BigDecimal points = ok ? item.points() : BigDecimal.ZERO;
            scores.add(new QuestionScore(item.questionId(), ok, points, item.points()));
            total = total.add(points);
            max = max.add(item.points());
        }
        return new QuizScore(scores, total, max);
    }

    @Override
    @Transactional(readOnly = true)
    public AnswerReview answerReview(UUID quizId) {
        List<QuizItem> items = quizzes.items(quizId);
        Map<UUID, Question> byId = byId(items);
        List<AnswerKey> keys = items.stream().filter(item -> byId.containsKey(item.questionId()))
                .map(item -> {
                    Question q = byId.get(item.questionId());
                    return new AnswerKey(q.id(), Set.copyOf(q.content().correctOptionIds()), q.content().explanation());
                })
                .toList();
        return new AnswerReview(quizId, keys);
    }

    private Map<UUID, Question> byId(List<QuizItem> items) {
        return questions.findAll(items.stream().map(QuizItem::questionId).toList()).stream()
                .collect(Collectors.toMap(Question::id, Function.identity()));
    }
}
