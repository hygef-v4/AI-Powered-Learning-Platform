package edu.aiplatform.quizzes.domain;

import edu.aiplatform.quizzes.contract.QuestionType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Bảng {@code questions}; chỉ dùng trong U10. Ghi có điều kiện {@code version} (NFR-U10-10). */
public interface IQuestionRepository {

    Optional<Question> find(UUID id);

    Optional<Question> findBankDraft(UUID lineageId);

    /** Mọi phiên bản của lineage, mới nhất trước. */
    List<Question> lineage(UUID lineageId);

    int maxVersionNo(UUID lineageId);

    void insert(Question question);

    /** Cập nhật nội dung, trạng thái khi {@code version} trong DB còn bằng {@code question.version()}; tăng version. */
    boolean update(Question question);

    boolean delete(UUID id, long version);

    /** Question List (BR-U10-06): mỗi lineage một dòng, kèm cờ có bản nháp. */
    BankPage searchBank(UUID subjectId, BankQuery query);

    /** Số Quiz đang ghim phiên bản này. */
    int countQuizzesUsing(UUID questionId);

    record BankQuery(QuestionType type, Difficulty difficulty, QuestionStatus status, String text, int page, int size) {
    }

    record BankRow(Question question, boolean hasDraft) {
    }

    record BankPage(List<BankRow> items, long total, int page, int size) {
    }
}
