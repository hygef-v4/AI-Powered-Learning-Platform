package edu.aiplatform.quizzes.service;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.quizzes.service.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.contract.IQuizLifecycleListener;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.repository.IQuizRepository;
import edu.aiplatform.quizzes.model.Quiz;
import edu.aiplatform.quizzes.model.QuizItem;
import edu.aiplatform.quizzes.model.QuizRules;
import edu.aiplatform.quizzes.model.Violation;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Duyệt, phát hành, ngưng Quiz (UC 48; F9, F10; P3). Phát hành version mới thì version cũ đang mở bị ngưng và
 * {@link IQuizLifecycleListener} (U11 cài) chạy trong cùng transaction; lỗi thì rollback cả.
 */
@Service
public class QuizPublicationService {

    private final IQuizRepository quizzes;
    private final QuizSupport support;
    private final ObjectProvider<IQuizLifecycleListener> listeners;
    private final IEventPublisher events;
    private final QuizAudit audit;
    private final Clock clock;

    public QuizPublicationService(IQuizRepository quizzes, QuizSupport support,
                                  ObjectProvider<IQuizLifecycleListener> listeners, IEventPublisher events,
                                  QuizAudit audit, Clock clock) {
        this.quizzes = quizzes;
        this.support = support;
        this.listeners = listeners;
        this.events = events;
        this.audit = audit;
        this.clock = clock;
    }

    /** Người soạn tự duyệt sau khi xem trước (BR-U10-21); lỗi trả danh sách cần sửa theo câu. */
    @Transactional
    public QuizDetail review(Actor actor, UUID id, long version) {
        Quiz quiz = support.loadManaged(actor, id);
        if (quiz.status() != QuizStatus.DRAFT) {
            throw ApiException.conflict("QUIZ_NOT_DRAFT", "Chỉ duyệt được quiz đang nháp");
        }
        QuizSupport.requireVersion(quiz, version);
        support.requireContainerActive(quiz);
        requireReviewable(quiz);
        Quiz reviewed = support.save(quiz.reviewed(clock.instant()));
        audit.success(actor, "QUIZ_REVIEWED", QuizSupport.OBJECT, id, Map.of());
        return support.detail(reviewed, true);
    }

    /** Phát hành từ {@code REVIEWED} (BR-U10-22): lớp {@code OPEN} hoặc môn {@code ACTIVE}; mở ngay. */
    @Transactional
    public QuizDetail publish(Actor actor, UUID id, long version) {
        Quiz quiz = support.loadManaged(actor, id);
        if (quiz.status() != QuizStatus.REVIEWED) {
            throw ApiException.conflict("QUIZ_NOT_REVIEWED", "Cần duyệt quiz trước khi phát hành");
        }
        QuizSupport.requireVersion(quiz, version);
        support.requireContainerActive(quiz);
        if (quiz.scope() == QuizScope.CLASS && support.classRef(quiz.classId()).status() != ClassStatus.OPEN) {
            throw ApiException.conflict("CLASS_NOT_OPEN", "Lớp chưa mở nên chưa phát hành được quiz của lớp");
        }
        // Câu ngân hàng có thể đã bị ngưng sau khi duyệt.
        requireReviewable(quiz);
        Instant now = clock.instant();
        quizzes.findOpenInVersionChain(id).ifPresent(old -> retire(actor, old, "Đã phát hành version " + quiz.versionNo(), now));
        Quiz published = support.save(quiz.published(now));
        audit.success(actor, "QUIZ_PUBLISHED", QuizSupport.OBJECT, id, Map.of("versionNo", quiz.versionNo()));
        Map<String, Object> payload = new HashMap<>();
        payload.put("quizId", id.toString());
        payload.put("scope", quiz.scope().name());
        payload.put("subjectId", quiz.subjectId().toString());
        if (quiz.classId() != null) {
            payload.put("classId", quiz.classId().toString());
        }
        payload.put("title", quiz.title());
        events.publish(new DomainEvent(1, UUID.randomUUID(), EventTypes.QUIZ_PUBLISHED, "U10", now,
                UUID.randomUUID().toString(), payload));
        return support.detail(published, true);
    }

    /** Ngưng Quiz đang mở, lý do bắt buộc (BR-U10-23); lượt đang làm được U11 tự nộp trong cùng transaction. */
    @Transactional
    public QuizDetail retire(Actor actor, UUID id, String reason, long version) {
        Quiz quiz = support.loadManaged(actor, id);
        if (quiz.status() == QuizStatus.RETIRED) {
            throw ApiException.conflict("QUIZ_NOT_OPEN", "Quiz đã lưu trữ");
        }
        String cleanReason = reason == null || reason.isBlank() ? "Lưu trữ" : reason.strip();
        if (cleanReason.codePointCount(0, cleanReason.length()) > QuizRules.MAX_REASON) {
            throw new RuleViolationException(HttpStatus.BAD_REQUEST, "QUIZ_INVALID", "Cần lý do ngưng quiz",
                    List.of(new Violation("reason", "REQUIRED", "Lý do ngưng bắt buộc, tối đa 1.000 ký tự")));
        }
        QuizSupport.requireVersion(quiz, version);
        return support.detail(retire(actor, quiz, cleanReason, clock.instant()), true);
    }

    /** Mở lại Quiz đã lưu trữ (Quiz đã phát hành mở lại, Quiz lưu trữ khi còn nháp về nháp). */
    @Transactional
    public QuizDetail reopen(Actor actor, UUID id, long version) {
        Quiz quiz = support.loadManaged(actor, id);
        if (quiz.status() != QuizStatus.RETIRED) {
            throw ApiException.conflict("QUIZ_NOT_RETIRED", "Quiz chưa lưu trữ");
        }
        QuizSupport.requireVersion(quiz, version);
        support.requireContainerActive(quiz);
        Quiz reopened = support.save(quiz.reopened(clock.instant()));
        audit.success(actor, "QUIZ_REOPENED", QuizSupport.OBJECT, quiz.id(), Map.of("status", reopened.status().name()));
        return support.detail(reopened, true);
    }

    private Quiz retire(Actor actor, Quiz quiz, String reason, Instant now) {
        Quiz retired = support.save(quiz.retired(reason, now));
        listeners.orderedStream().forEach(listener -> listener.onRetired(quiz.id()));
        audit.success(actor, "QUIZ_RETIRED", QuizSupport.OBJECT, quiz.id(), Map.of("reason", reason));
        return retired;
    }

    private void requireReviewable(Quiz quiz) {
        List<QuizItem> items = quizzes.items(quiz.id());
        List<Violation> violations = new ArrayList<>(QuizRules.validateForReview(quiz, items, support.questionsOf(items)));
        violations.addAll(QuizRules.validateActivity(quiz.activityMonitoringEnabled(), quiz.activityNotice()));
        if (!violations.isEmpty()) {
            throw new RuleViolationException(HttpStatus.BAD_REQUEST, "QUIZ_REVIEW_FAILED",
                    "Quiz chưa đủ điều kiện; hãy sửa các mục được nêu", violations);
        }
    }
}
