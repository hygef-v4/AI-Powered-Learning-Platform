package edu.aiplatform.quizzes.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.quizzes.model.Question;
import edu.aiplatform.quizzes.model.QuestionContent;
import edu.aiplatform.quizzes.model.QuestionRules;
import edu.aiplatform.quizzes.model.QuestionScope;
import edu.aiplatform.quizzes.model.Violation;
import edu.aiplatform.quizzes.repository.IQuestionRepository;
import edu.aiplatform.quizzes.service.QuestionViews.ProposalView;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionView;
import edu.aiplatform.quizzes.service.QuestionViews.ReviewInfo;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giảng viên đề xuất câu vào ngân hàng của môn (BR-U10-09; góp ý 2026-10-29): đây là trường hợp duy nhất còn bước duyệt.
 * Câu riêng của Quiz vẫn không cần duyệt; đề xuất là bản sao mới nên câu trong Quiz giữ nguyên. Kiểm quyền theo lớp.
 */
@Service
public class QuestionProposalService {

    private static final String NOT_FOUND = "QUESTION_NOT_FOUND";

    private final IQuestionRepository questions;
    private final QuizSupport support;
    private final QuizAccess access;
    private final ActiveSubjects activeSubjects;
    private final QuestionDuplicates duplicates;
    private final ISubjectQuery subjects;
    private final IEventPublisher events;
    private final QuizAudit audit;
    private final Clock clock;

    public QuestionProposalService(IQuestionRepository questions, QuizSupport support, QuizAccess access,
                                   ActiveSubjects activeSubjects, QuestionDuplicates duplicates, ISubjectQuery subjects,
                                   IEventPublisher events, QuizAudit audit, Clock clock) {
        this.questions = questions;
        this.support = support;
        this.access = access;
        this.activeSubjects = activeSubjects;
        this.duplicates = duplicates;
        this.subjects = subjects;
        this.events = events;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ProposalView> list(Actor actor, UUID classId) {
        requireClass(actor, classId);
        return questions.listProposals(classId, actor.accountId()).stream().map(this::view).toList();
    }

    @Transactional
    public ProposalView propose(Actor actor, UUID classId, QuestionContent content) {
        requireClass(actor, classId);
        ClassRef ref = support.classRef(classId);
        activeSubjects.require(ref.subjectId());
        validate(content);
        duplicates.requireUnique(ref.subjectId(), content, null);
        Instant now = clock.instant();
        Question q = Question.newProposal(ref.subjectId(), content, actor.accountId(), now);
        questions.insert(q);
        questions.insertProposal(q.id(), classId, now);
        audit.success(actor, "QUESTION_PROPOSED", QuestionBankService.OBJECT, q.id(),
                Map.of("subjectId", ref.subjectId().toString(), "classId", classId.toString()));
        notifyManager(q, now);
        return view(q);
    }

    /** Sửa đề xuất đang chờ hoặc đã bị từ chối; lưu là gửi lại để duyệt. */
    @Transactional
    public ProposalView resubmit(Actor actor, UUID classId, UUID id, QuestionContent content, long version) {
        Question q = own(actor, classId, id, version);
        activeSubjects.require(q.subjectId());
        validate(content);
        duplicates.requireUnique(q.subjectId(), content, q.lineageId());
        Instant now = clock.instant();
        Question changed = q.resubmitted(content, now);
        if (!questions.update(changed)) {
            throw versionConflict();
        }
        audit.success(actor, "QUESTION_PROPOSED", QuestionBankService.OBJECT, id,
                Map.of("subjectId", q.subjectId().toString(), "classId", classId.toString(), "resubmitted", true));
        notifyManager(changed, now);
        return view(questions.find(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND)));
    }

    /** Rút đề xuất chưa được duyệt. */
    @Transactional
    public void withdraw(Actor actor, UUID classId, UUID id, long version) {
        Question q = own(actor, classId, id, version);
        if (!questions.deleteProposal(id, version)) {
            throw versionConflict();
        }
        audit.success(actor, "QUESTION_DELETED", QuestionBankService.OBJECT, id,
                Map.of("subjectId", q.subjectId().toString(), "withdrawn", true));
    }

    private Question own(Actor actor, UUID classId, UUID id, long version) {
        requireClass(actor, classId);
        Question q = questions.find(id)
                .filter(found -> found.scope() == QuestionScope.BANK && actor.accountId().equals(found.createdBy()))
                .filter(found -> questions.proposalClass(id).filter(classId::equals).isPresent())
                .orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        if (!q.isProposal()) {
            throw ApiException.conflict("QUESTION_NOT_PENDING", "Câu đã được duyệt, không sửa được nữa");
        }
        if (q.version() != version) {
            throw versionConflict();
        }
        return q;
    }

    private void requireClass(Actor actor, UUID classId) {
        access.requireClass(actor, Actions.QUIZ_CLASS_MANAGE, classId, "CLASS", classId, "CLASS_NOT_FOUND");
    }

    private ProposalView view(Question q) {
        ReviewInfo review = questions.latestReview(q.id())
                .map(r -> new ReviewInfo(r.decision(), r.reason(), r.createdAt())).orElse(null);
        return new ProposalView(QuestionView.of(q), review,
                q.isProposal() ? duplicates.similarTo(q.subjectId(), q.content().stem(), q.lineageId()) : List.of());
    }

    /** Thông báo Chủ nhiệm môn; U04 lỗi thì vẫn lưu đề xuất, người duyệt thấy ở tab Chờ duyệt. */
    private void notifyManager(Question q, Instant now) {
        UUID manager;
        try {
            manager = subjects.getSubject(q.subjectId()).map(s -> s.managerId()).orElse(null);
        } catch (RuntimeException unavailable) {
            return;
        }
        if (manager == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("questionId", q.id().toString());
        payload.put("subjectId", q.subjectId().toString());
        payload.put("recipientIds", List.of(manager.toString()));
        events.publish(new DomainEvent(1, UUID.randomUUID(), EventTypes.QUESTION_PROPOSED, "U10", now,
                UUID.randomUUID().toString(), payload));
    }

    private static void validate(QuestionContent content) {
        List<Violation> violations = QuestionRules.validate(content);
        if (!violations.isEmpty()) {
            throw new QuestionInvalidException(violations);
        }
    }

    private static ApiException versionConflict() {
        return ApiException.conflict("QUESTION_VERSION_CONFLICT", "Câu hỏi đã được thay đổi ở phiên khác; hãy tải lại");
    }
}
