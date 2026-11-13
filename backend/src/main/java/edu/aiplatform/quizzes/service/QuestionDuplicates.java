package edu.aiplatform.quizzes.service;

import edu.aiplatform.quizzes.model.QuestionContent;
import edu.aiplatform.quizzes.model.QuestionFingerprint;
import edu.aiplatform.quizzes.repository.IQuestionRepository;
import edu.aiplatform.quizzes.service.QuestionViews.SimilarQuestion;
import edu.aiplatform.shared.error.ApiException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Chống trùng câu trong ngân hàng của môn (BR-U10-08): giống hệt thì từ chối, gần giống thì chỉ cảnh báo. Không so với
 * các phiên bản cùng lineage, vì phiên bản mới được phép giống bản cũ. Phải gọi trong transaction có khóa theo môn.
 */
@Component
class QuestionDuplicates {

    static final double SIMILAR_FROM = 0.7;
    private static final int MAX_SIMILAR = 5;
    private static final int PREVIEW = 80;

    private final IQuestionRepository questions;

    QuestionDuplicates(IQuestionRepository questions) {
        this.questions = questions;
    }

    /** Khóa theo môn rồi từ chối {@code 409 QUESTION_DUPLICATE} nếu đã có câu giống hệt. */
    void requireUnique(UUID subjectId, QuestionContent content, UUID excludeLineage) {
        questions.lockSubject(subjectId);
        duplicateOf(subjectId, content, excludeLineage).ifPresent(message -> {
            throw ApiException.conflict("QUESTION_DUPLICATE", message);
        });
    }

    /** Thông báo trùng nếu đã có câu giống hệt; không khóa, dùng khi người gọi đã khóa. */
    java.util.Optional<String> duplicateOf(UUID subjectId, QuestionContent content, UUID excludeLineage) {
        return questions.findDuplicate(subjectId, QuestionFingerprint.hash(content), excludeLineage)
                .map(found -> "Câu này trùng với câu đã có trong ngân hàng: \"" + preview(found.content().stem()) + "\"");
    }

    List<SimilarQuestion> similarTo(UUID subjectId, String stem, UUID excludeLineage) {
        String normalized = QuestionFingerprint.normalize(stem);
        List<SimilarQuestion> found = new ArrayList<>();
        for (var entry : questions.bankStems(subjectId, excludeLineage)) {
            double score = QuestionFingerprint.similarity(normalized, entry.normalizedStem());
            if (score >= SIMILAR_FROM) {
                found.add(new SimilarQuestion(entry.id(), entry.stem(), entry.status(), (int) Math.round(score * 100)));
            }
        }
        return found.stream().sorted(Comparator.comparingInt(SimilarQuestion::percent).reversed())
                .limit(MAX_SIMILAR).toList();
    }

    private static String preview(String stem) {
        return stem.codePointCount(0, stem.length()) <= PREVIEW ? stem : stem.substring(0, stem.offsetByCodePoints(0, PREVIEW)) + "…";
    }
}
