package edu.aiplatform.quizzes.repository;

import edu.aiplatform.quizzes.model.Quiz;
import edu.aiplatform.quizzes.model.QuizItem;

import edu.aiplatform.quizzes.contract.QuizStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Bảng {@code quizzes}, {@code quiz_items}; chỉ dùng trong U10. Ghi có điều kiện {@code version} (NFR-U10-10). */
public interface IQuizRepository {

    Optional<Quiz> find(UUID id);

    void insert(Quiz quiz);

    /** Cập nhật khi {@code version} trong DB còn bằng {@code quiz.version()}; tăng version. */
    boolean update(Quiz quiz);

    /** Xóa Quiz chưa từng phát hành cùng câu, câu riêng, đề xuất AI (BR-U10-24). */
    boolean delete(UUID id, long version);

    List<QuizItem> items(UUID quizId);

    void replaceItems(UUID quizId, List<QuizItem> items);

    /** Quiz List của môn (Quiz của môn), mới cập nhật trước. */
    QuizPage listSubjectQuizzes(UUID subjectId, QuizStatus status, String text, int page, int size);

    /** Quiz của một lớp (mọi trạng thái). */
    List<QuizSummary> listClassQuizzes(UUID classId);

    /** Quiz của môn ở các trạng thái cho trước. */
    List<QuizSummary> listSubjectQuizzes(UUID subjectId, Collection<QuizStatus> statuses);

    /** Quiz của các lớp cho trước (nguồn copy), trừ một lớp. */
    List<QuizSummary> listClassQuizzesOf(Collection<UUID> classIds, UUID excludeClassId);

    /** Quiz {@code OPEN} gắn học liệu, của lớp hoặc của môn chứa lớp. */
    List<QuizSummary> listOpenByMaterial(UUID materialId, UUID classId, UUID subjectId);

    /** Quiz đang {@code OPEN} khác {@code quizId} trong cùng chuỗi version (nối bằng {@code NEW_VERSION}). */
    Optional<Quiz> findOpenInVersionChain(UUID quizId);

    /** Lớn nhất {@code version_no} trong chuỗi version bắt đầu từ Quiz gốc của {@code quizId}. */
    int maxVersionNo(UUID quizId);

    record QuizSummary(Quiz quiz, int itemCount, BigDecimal maxScore) {
    }

    record QuizPage(List<QuizSummary> items, long total, int page, int size) {
    }
}
