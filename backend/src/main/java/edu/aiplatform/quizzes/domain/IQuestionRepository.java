package edu.aiplatform.quizzes.domain;

import edu.aiplatform.quizzes.contract.QuestionType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    List<Question> findAll(Collection<UUID> ids);

    /** Câu riêng của một Quiz. */
    List<Question> inlineOf(UUID quizId);

    /** Xóa câu riêng của Quiz đang soạn (sau khi đã gỡ khỏi {@code quiz_items}). */
    boolean deleteInline(UUID questionId, UUID quizId);

    /** Câu {@code ACTIVE} mới nhất mỗi lineage của môn để chọn vào Quiz (BR-U10-14), trừ các lineage đã có. */
    BankPage searchActive(UUID subjectId, PickQuery query);

    /** Bốc ngẫu nhiên tối đa {@code count} câu {@code ACTIVE} khớp bộ lọc, trừ các lineage đã có. */
    List<Question> randomActive(UUID subjectId, PickQuery query, int count);

    /** Số câu {@code ACTIVE} khớp bộ lọc ("Có n câu phù hợp"). */
    long countActive(UUID subjectId, PickQuery query);

    record PickQuery(Difficulty difficulty, String tag, String text, Set<UUID> excludeLineages, int page, int size) {
    }

    record BankQuery(QuestionType type, Difficulty difficulty, QuestionStatus status, String text, int page, int size) {
    }

    record BankRow(Question question, boolean hasDraft) {
    }

    record BankPage(List<BankRow> items, long total, int page, int size) {
    }
}
