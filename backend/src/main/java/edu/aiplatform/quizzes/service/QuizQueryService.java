package edu.aiplatform.quizzes.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.quizzes.contract.IQuizQuery;
import edu.aiplatform.quizzes.contract.QuizOptionView;
import edu.aiplatform.quizzes.contract.QuizQuestionView;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.contract.QuizStudentView;
import edu.aiplatform.quizzes.contract.QuizView;
import edu.aiplatform.quizzes.repository.IQuestionRepository;
import edu.aiplatform.quizzes.repository.IQuizRepository;
import edu.aiplatform.quizzes.repository.IQuizRepository.QuizSummary;
import edu.aiplatform.quizzes.model.Question;
import edu.aiplatform.quizzes.model.Quiz;
import edu.aiplatform.quizzes.model.QuizItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link IQuizQuery} cho U11, U13 (F14): Quiz {@code OPEN} của lớp và của môn chứa lớp (BR-U10-25); đề cho
 * người học không có đáp án, giải thích (P2, NFR-U10-31). Không kiểm quyền: unit gọi tự kiểm người học.
 */
@Service
public class QuizQueryService implements IQuizQuery {

    private final IQuizRepository quizzes;
    private final IQuestionRepository questions;
    private final IClassAccess classes;

    public QuizQueryService(IQuizRepository quizzes, IQuestionRepository questions, IClassAccess classes) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.classes = classes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizView> listForClass(UUID classId) {
        Optional<ClassRef> ref = classes.getClassRef(classId);
        if (ref.isEmpty()) {
            return List.of();
        }
        List<QuizView> result = new ArrayList<>();
        quizzes.listClassQuizzes(classId).stream().filter(s -> s.quiz().status() == QuizStatus.OPEN)
                .forEach(s -> result.add(view(s)));
        quizzes.listSubjectQuizzes(ref.get().subjectId(), List.of(QuizStatus.OPEN)).forEach(s -> result.add(view(s)));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizStudentView> getStudentView(UUID quizId) {
        return quizzes.find(quizId).map(quiz -> {
            List<QuizItem> items = quizzes.items(quizId);
            Map<UUID, Question> byId = questions.findAll(items.stream().map(QuizItem::questionId).toList()).stream()
                    .collect(Collectors.toMap(Question::id, Function.identity()));
            List<QuizQuestionView> views = items.stream().filter(item -> byId.containsKey(item.questionId()))
                    .map(item -> {
                        Question q = byId.get(item.questionId());
                        return new QuizQuestionView(q.id(), q.content().type(), q.content().stem(),
                                q.content().options().stream().map(o -> new QuizOptionView(o.id(), o.text())).toList(),
                                item.points());
                    })
                    .toList();
            BigDecimal max = items.stream().map(QuizItem::points).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new QuizStudentView(view(quiz, max), quiz.instructions(), views);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizView> listByMaterial(UUID materialId, UUID classId) {
        return classes.getClassRef(classId)
                .map(ref -> quizzes.listOpenByMaterial(materialId, classId, ref.subjectId()).stream().map(this::view).toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizView> get(UUID quizId) {
        return quizzes.find(quizId).map(quiz -> view(quiz,
                quizzes.items(quizId).stream().map(QuizItem::points).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    private QuizView view(QuizSummary summary) {
        return view(summary.quiz(), summary.maxScore());
    }

    private static QuizView view(Quiz q, BigDecimal maxScore) {
        return new QuizView(q.id(), q.scope(), q.classId(), q.subjectId(), q.versionNo(), q.title(), q.status(),
                q.settings(), q.activityMonitoringEnabled(), q.activityNotice(), q.referenceMaterialIds(), maxScore,
                q.publishedAt());
    }
}
