package edu.aiplatform.quizzes.repository;

import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.Question;
import edu.aiplatform.quizzes.model.QuestionReview;
import edu.aiplatform.quizzes.model.QuestionStatus;
import edu.aiplatform.quizzes.model.Quiz;

import edu.aiplatform.quizzes.contract.QuestionType;
import java.time.Instant;
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

    /** Câu {@code ACTIVE} mới nhất mỗi lineage của môn, phần đủ để sinh đề (BR-U10-17). */
    List<PoolEntry> activePool(UUID subjectId);

    /** Khóa theo môn tới hết transaction để hai lần lưu cùng lúc không cùng lọt kiểm trùng (BR-U10-08). */
    void lockSubject(UUID subjectId);

    /** Câu đang mở của ngân hàng (nháp, chờ duyệt, đang dùng) có cùng hash, khác lineage; ưu tiên câu đang dùng. */
    Optional<Question> findDuplicate(UUID subjectId, String contentHash, UUID excludeLineage);

    /** Nội dung đã chuẩn hóa của các câu đang mở khác lineage, để tìm câu gần giống. */
    List<StemEntry> bankStems(UUID subjectId, UUID excludeLineage);

    /** Các nhóm câu trùng hash trong ngân hàng (báo cáo cho Chủ nhiệm môn). */
    List<List<StemEntry>> duplicateGroups(UUID subjectId);

    /** Câu chưa có dấu vân tay (dữ liệu trước khi thêm cột). */
    List<Question> withoutFingerprint(int limit);

    void saveFingerprint(Question question);

    void insertReview(QuestionReview review);

    Optional<QuestionReview> latestReview(UUID questionId);

    void insertProposal(UUID questionId, UUID classId, Instant now);

    /** Lớp mà câu đề xuất được gửi từ đó. */
    Optional<UUID> proposalClass(UUID questionId);

    /** Câu đề xuất của một người ở một lớp, mới cập nhật trước. */
    List<Question> listProposals(UUID classId, UUID createdBy);

    /** Xóa câu đề xuất chưa được duyệt (người đề xuất rút lại). */
    boolean deleteProposal(UUID id, long version);

    record PoolEntry(UUID id, UUID lineageId, List<String> tags, Difficulty difficulty, String contentHash) {
    }

    record StemEntry(UUID id, String normalizedStem, String stem, QuestionStatus status) {
    }

    record PickQuery(Difficulty difficulty, String tag, String text, Set<UUID> excludeLineages, int page, int size) {
    }

    record BankQuery(QuestionType type, Difficulty difficulty, QuestionStatus status, String text, int page, int size) {
    }

    record BankRow(Question question, boolean hasDraft) {
    }

    record BankPage(List<BankRow> items, long total, int page, int size) {
    }
}
