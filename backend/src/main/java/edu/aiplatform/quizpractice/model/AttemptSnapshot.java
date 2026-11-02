package edu.aiplatform.quizpractice.model;

import edu.aiplatform.quizzes.contract.QuizQuestionView;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStudentView;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Chụp lúc bắt đầu lượt (BR-U11-11): version Quiz, cài đặt, chính sách theo dõi, seed và thứ tự câu, thứ tự lựa chọn
 * đã trộn theo seed (G5). Sửa Quiz sau đó không ảnh hưởng lượt đã có. Lượt của Quiz sinh đề có thêm
 * {@code questionPoints}: đúng các câu đã bốc và điểm từng câu (BR-U11-11); lượt đề cố định và lượt cũ để trống.
 */
public record AttemptSnapshot(int versionNo, String title, QuizSettings settings, boolean activityMonitoringEnabled,
                              String activityNotice, long seed, List<UUID> questionOrder,
                              Map<UUID, List<String>> optionOrder, BigDecimal maxScore,
                              Map<UUID, BigDecimal> questionPoints) {

    public AttemptSnapshot {
        questionOrder = List.copyOf(questionOrder);
        optionOrder = Map.copyOf(optionOrder);
        questionPoints = questionPoints == null ? null : Map.copyOf(questionPoints);
    }

    public AttemptSnapshot(int versionNo, String title, QuizSettings settings, boolean activityMonitoringEnabled,
                           String activityNotice, long seed, List<UUID> questionOrder,
                           Map<UUID, List<String>> optionOrder, BigDecimal maxScore) {
        this(versionNo, title, settings, activityMonitoringEnabled, activityNotice, seed, questionOrder, optionOrder,
                maxScore, null);
    }

    /** Trộn câu rồi trộn lựa chọn từng câu bằng cùng một dãy ngẫu nhiên từ {@code seed}: cùng seed cho cùng thứ tự. */
    public static AttemptSnapshot of(QuizStudentView view, long seed) {
        QuizSettings settings = view.quiz().settings();
        Random random = new Random(seed);
        List<QuizQuestionView> questions = new ArrayList<>(view.questions());
        if (settings.shuffleQuestions()) {
            Collections.shuffle(questions, random);
        }
        Map<UUID, List<String>> options = new LinkedHashMap<>();
        for (QuizQuestionView question : questions) {
            List<String> ids = new ArrayList<>(question.options().stream().map(o -> o.optionId()).toList());
            if (settings.shuffleOptions()) {
                Collections.shuffle(ids, random);
            }
            options.put(question.questionId(), ids);
        }
        return new AttemptSnapshot(view.quiz().versionNo(), view.quiz().title(), settings,
                view.quiz().activityMonitoringEnabled(), view.quiz().activityNotice(), seed,
                questions.stream().map(QuizQuestionView::questionId).toList(), options, view.quiz().maxScore(),
                view.generated() ? questions.stream().collect(Collectors.toMap(QuizQuestionView::questionId,
                        QuizQuestionView::points)) : null);
    }

    /** Câu và lựa chọn có thuộc đề của lượt không (NFR-U11-31). */
    public boolean contains(UUID questionId, List<String> optionIds) {
        List<String> allowed = optionOrder.get(questionId);
        return allowed != null && allowed.containsAll(optionIds);
    }
}
