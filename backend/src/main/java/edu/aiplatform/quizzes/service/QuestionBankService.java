package edu.aiplatform.quizzes.service;

import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionDetail;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionPage;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionRow;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionView;
import edu.aiplatform.quizzes.service.QuestionViews.VersionEntry;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.repository.IQuestionRepository;
import edu.aiplatform.quizzes.repository.IQuestionRepository.BankQuery;
import edu.aiplatform.quizzes.model.Question;
import edu.aiplatform.quizzes.model.QuestionContent;
import edu.aiplatform.quizzes.model.QuestionRules;
import edu.aiplatform.quizzes.model.QuestionScope;
import edu.aiplatform.quizzes.model.QuestionStatus;
import edu.aiplatform.quizzes.model.Violation;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ngân hàng câu hỏi của môn (UC 46, 47; F1, F2; P1). Chỉ Chủ nhiệm môn của môn (R2, BR-U10-01). Môn đã lưu trữ chỉ
 * đọc. Không log nội dung câu (NFR-U10-32).
 */
@Service
public class QuestionBankService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    static final String OBJECT = "QUESTION";
    static final String NOT_FOUND = "QUESTION_NOT_FOUND";
    private static final int STEM_PREVIEW = 300;

    private final IQuestionRepository questions;
    private final ActiveSubjects activeSubjects;
    private final QuizAccess access;
    private final QuizAudit audit;
    private final Clock clock;

    public QuestionBankService(IQuestionRepository questions, ActiveSubjects activeSubjects, QuizAccess access,
                               QuizAudit audit, Clock clock) {
        this.questions = questions;
        this.activeSubjects = activeSubjects;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    public record Filter(QuestionType type, Difficulty difficulty, QuestionStatus status, String text, Integer page,
                         Integer size) {
    }

    @Transactional(readOnly = true)
    public QuestionPage list(Actor actor, UUID subjectId, Filter filter) {
        access.requireSubject(actor, Actions.QUESTION_BANK_VIEW, subjectId, "SUBJECT", subjectId, "SUBJECT_NOT_FOUND");
        int page = filter.page() == null ? 0 : filter.page();
        int size = filter.size() == null ? DEFAULT_PAGE_SIZE : filter.size();
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("QUESTION_QUERY_INVALID", "page ≥ 0, size từ 1 đến 100");
        }
        var result = questions.searchBank(subjectId,
                new BankQuery(filter.type(), filter.difficulty(), filter.status(), filter.text(), page, size));
        List<QuestionRow> rows = result.items().stream().map(row -> {
            Question q = row.question();
            return new QuestionRow(q.id(), q.lineageId(), q.versionNo(), q.content().type(), preview(q.content().stem()),
                    q.content().difficulty(), q.content().tags(), q.status(), row.hasDraft(), q.updatedAt());
        }).toList();
        return new QuestionPage(rows, result.total(), result.page(), result.size());
    }

    @Transactional(readOnly = true)
    public QuestionDetail detail(Actor actor, UUID id) {
        Question q = load(id);
        access.requireSubject(actor, Actions.QUESTION_BANK_VIEW, q.subjectId(), OBJECT, id, NOT_FOUND);
        return detailOf(q);
    }

    @Transactional
    public QuestionDetail create(Actor actor, UUID subjectId, QuestionContent content) {
        access.requireSubject(actor, Actions.QUESTION_BANK_MANAGE, subjectId, "SUBJECT", subjectId, "SUBJECT_NOT_FOUND");
        activeSubjects.require(subjectId);
        validate(content);
        Question q = Question.newBankDraft(subjectId, content, actor.accountId(), clock.instant());
        questions.insert(q);
        audit.success(actor, "QUESTION_CREATED", OBJECT, q.id(), Map.of("subjectId", subjectId.toString(),
                "versionNo", q.versionNo()));
        return detailOf(q);
    }

    /** Sửa bản nháp (F2 bước 2); bản đã kích hoạt phải mở bản nháp mới qua {@link #openDraft}. */
    @Transactional
    public QuestionDetail update(Actor actor, UUID id, QuestionContent content, long version) {
        Question q = loadForChange(actor, id);
        if (!q.isDraft()) {
            throw ApiException.conflict("QUESTION_NOT_EDITABLE",
                    "Câu đang dùng không sửa trực tiếp được; hãy mở bản nháp mới");
        }
        requireVersion(q, version);
        validate(content);
        Question changed = q.withContent(content, clock.instant());
        save(changed);
        return detailOf(reload(id));
    }

    /** Sửa câu đã kích hoạt: mở bản nháp mới cùng lineage, đã có thì trả bản đó (F2 bước 3, BR-U10-04). */
    @Transactional
    public QuestionDetail openDraft(Actor actor, UUID id) {
        Question q = loadForChange(actor, id);
        if (q.isDraft()) {
            return detailOf(q);
        }
        var existing = questions.findBankDraft(q.lineageId());
        if (existing.isPresent()) {
            return detailOf(existing.get());
        }
        Question draft = q.nextDraft(questions.maxVersionNo(q.lineageId()) + 1, actor.accountId(), clock.instant());
        try {
            questions.insert(draft);
        } catch (DuplicateKeyException concurrent) {
            // Người khác vừa mở bản nháp của cùng lineage (unique một nháp mỗi lineage).
            return detailOf(questions.findBankDraft(q.lineageId()).orElseThrow(() -> concurrent));
        }
        audit.success(actor, "QUESTION_CREATED", OBJECT, draft.id(), Map.of("subjectId", q.subjectId().toString(),
                "versionNo", draft.versionNo(), "fromQuestionId", q.id().toString()));
        return detailOf(draft);
    }

    @Transactional
    public QuestionDetail activate(Actor actor, UUID id, long version) {
        Question q = loadForChange(actor, id);
        if (!q.isDraft()) {
            throw ApiException.conflict("QUESTION_NOT_DRAFT", "Chỉ kích hoạt được câu đang ở trạng thái nháp");
        }
        requireVersion(q, version);
        validate(q.content());
        save(q.activate(clock.instant()));
        audit.success(actor, "QUESTION_ACTIVATED", OBJECT, id, Map.of("versionNo", q.versionNo()));
        return detailOf(reload(id));
    }

    @Transactional
    public QuestionDetail retire(Actor actor, UUID id, long version) {
        Question q = loadForChange(actor, id);
        if (q.status() != QuestionStatus.ACTIVE) {
            throw ApiException.conflict("QUESTION_NOT_ACTIVE", "Chỉ ngưng được câu đang dùng");
        }
        requireVersion(q, version);
        save(q.retire(clock.instant()));
        audit.success(actor, "QUESTION_RETIRED", OBJECT, id, Map.of("versionNo", q.versionNo()));
        return detailOf(reload(id));
    }

    /** Xóa hẳn bản nháp chưa từng kích hoạt; bản đã kích hoạt chỉ ngưng dùng (BR-U10-05). */
    @Transactional
    public void delete(Actor actor, UUID id, long version) {
        Question q = loadForChange(actor, id);
        if (!q.isDeletable()) {
            throw ApiException.conflict("QUESTION_DELETE_BLOCKED",
                    "Câu đã kích hoạt không xóa được; hãy ngưng dùng câu này");
        }
        requireVersion(q, version);
        if (!questions.delete(id, version)) {
            throw versionConflict();
        }
        audit.success(actor, "QUESTION_DELETED", OBJECT, id, Map.of("versionNo", q.versionNo()));
    }

    private Question loadForChange(Actor actor, UUID id) {
        Question q = load(id);
        access.requireSubject(actor, Actions.QUESTION_BANK_MANAGE, q.subjectId(), OBJECT, id, NOT_FOUND);
        activeSubjects.require(q.subjectId());
        return q;
    }

    private Question load(UUID id) {
        return questions.find(id)
                .filter(q -> q.scope() == QuestionScope.BANK)
                .orElseThrow(() -> ApiException.notFound(NOT_FOUND));
    }

    private Question reload(UUID id) {
        return questions.find(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
    }

    private QuestionDetail detailOf(Question q) {
        List<VersionEntry> versions = questions.lineage(q.lineageId()).stream()
                .map(v -> new VersionEntry(v.id(), v.versionNo(), v.status(), v.createdAt(), v.activatedAt()))
                .toList();
        UUID draftId = q.isDraft() ? null : questions.findBankDraft(q.lineageId()).map(Question::id).orElse(null);
        return new QuestionDetail(QuestionView.of(q), versions, questions.countQuizzesUsing(q.id()), draftId);
    }


    private static void validate(QuestionContent content) {
        List<Violation> violations = QuestionRules.validate(content);
        if (!violations.isEmpty()) {
            throw new QuestionInvalidException(violations);
        }
    }

    private static void requireVersion(Question q, long version) {
        if (q.version() != version) {
            throw versionConflict();
        }
    }

    private void save(Question q) {
        if (!questions.update(q)) {
            throw versionConflict();
        }
    }

    private static ApiException versionConflict() {
        return ApiException.conflict("QUESTION_VERSION_CONFLICT", "Câu hỏi đã được thay đổi ở phiên khác; hãy tải lại");
    }

    private static String preview(String stem) {
        if (stem.codePointCount(0, stem.length()) <= STEM_PREVIEW) {
            return stem;
        }
        return stem.substring(0, stem.offsetByCodePoints(0, STEM_PREVIEW)) + "…";
    }
}
