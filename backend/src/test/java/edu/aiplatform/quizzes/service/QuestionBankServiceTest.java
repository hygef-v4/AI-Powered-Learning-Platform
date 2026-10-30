package edu.aiplatform.quizzes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.repository.IQuestionRepository;
import edu.aiplatform.quizzes.model.Question;
import edu.aiplatform.quizzes.model.QuestionContent;
import edu.aiplatform.quizzes.model.QuestionOption;
import edu.aiplatform.quizzes.model.QuestionScope;
import edu.aiplatform.quizzes.model.QuestionStatus;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

/** F1, F2 của U10: quyền R2 (BR-U10-01, 50, 52), phiên bản (BR-U10-04), kích hoạt, ngưng, xóa (BR-U10-05). */
class QuestionBankServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T08:00:00Z");
    private final UUID subject = UUID.randomUUID();
    private final Actor manager = new Actor(UUID.randomUUID(), Role.SUBJECT_MANAGER);
    private final IQuestionRepository repo = mock(IQuestionRepository.class);
    private final ISubjectQuery subjects = mock(ISubjectQuery.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final List<AuditEvent> audits = new ArrayList<>();
    private QuestionBankService service;

    @BeforeEach
    void setup() {
        IAudit audit = audits::add;
        QuizAudit quizAudit = new QuizAudit(audit, Clock.fixed(NOW, ZoneOffset.UTC));
        service = new QuestionBankService(repo, new ActiveSubjects(subjects),
                new QuizAccess(authorization, quizAudit), quizAudit, Clock.fixed(NOW, ZoneOffset.UTC));
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(subjects.isActive(subject)).thenReturn(true);
        when(repo.update(any())).thenReturn(true);
        when(repo.lineage(any())).thenReturn(List.of());
        when(repo.findBankDraft(any())).thenReturn(Optional.empty());
    }

    static QuestionContent content() {
        return QuestionContent.of(QuestionType.MCQ_SINGLE, "Thủ đô của Việt Nam?",
                List.of(new QuestionOption("1", "Hà Nội"), new QuestionOption("2", "Huế")), List.of("1"),
                "Hà Nội là thủ đô.", BigDecimal.ONE, Difficulty.EASY, List.of("địa lý"));
    }

    private Question stored(Question q) {
        when(repo.find(q.id())).thenReturn(Optional.of(q));
        return q;
    }

    private Question draft() {
        return stored(Question.newBankDraft(subject, content(), manager.accountId(), NOW));
    }

    private Question active() {
        return stored(Question.newBankDraft(subject, content(), manager.accountId(), NOW).activate(NOW));
    }

    private static ApiException api(Runnable call) {
        try {
            call.run();
        } catch (ApiException failure) {
            return failure;
        }
        throw new AssertionError("Không ném ApiException");
    }

    @Test
    void createStoresDraftAndAuditsWithoutContent() {
        var detail = service.create(manager, subject, content());

        assertThat(detail.question().status()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(detail.question().versionNo()).isEqualTo(1);
        verify(repo).insert(any());
        verify(authorization).authorize(manager, Actions.QUESTION_BANK_MANAGE, ResourceRef.subject(subject));
        assertThat(audits).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("QUESTION_CREATED");
            assertThat(e.details().toString()).doesNotContain("Hà Nội", "Thủ đô");
        });
    }

    @Test
    void invalidContentIsRejectedWithViolations() {
        QuestionContent bad = QuestionContent.of(QuestionType.MCQ_SINGLE, "Câu", List.of(new QuestionOption("1", "A")),
                List.of("1"), null, BigDecimal.ONE, null, null);
        assertThatThrownBy(() -> service.create(manager, subject, bad))
                .isInstanceOfSatisfying(QuestionInvalidException.class,
                        e -> assertThat(e.violations()).extracting(v -> v.code()).contains("OPTION_COUNT"));
        verify(repo, never()).insert(any());
    }

    @Test
    void outOfScopeIsNotFoundAndAuditedAsDenied() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("OUT_OF_SCOPE"));
        Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
        Question q = draft();

        ApiException failure = api(() -> service.detail(teacher, q.id()));

        assertThat(failure.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(audits).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("ACCESS_DENIED");
            assertThat(e.result()).isEqualTo(AuditResult.DENIED);
            assertThat(e.objectId()).isEqualTo(q.id().toString());
        });
    }

    @Test
    void authorizationOutageFailsClosedWithoutDeniedAudit() {
        when(authorization.authorize(any(), anyString(), any()))
                .thenReturn(AuthorizationDecision.deny("DEPENDENCY_UNAVAILABLE"));
        ApiException failure = api(() -> service.list(manager, subject,
                new QuestionBankService.Filter(null, null, null, null, null, null)));
        assertThat(failure.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(audits).isEmpty();
    }

    @Test
    void archivedSubjectIsReadOnlyAndSubjectOutageIsBusy() {
        when(subjects.isActive(subject)).thenReturn(false);
        assertThat(api(() -> service.create(manager, subject, content())).code()).isEqualTo("SUBJECT_NOT_ACTIVE");
        when(subjects.isActive(subject)).thenThrow(new IllegalStateException("U04 down"));
        assertThat(api(() -> service.create(manager, subject, content())).status())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void listRejectsPageSizeOverHundred() {
        ApiException failure = api(() -> service.list(manager, subject,
                new QuestionBankService.Filter(null, null, null, null, 0, 101)));
        assertThat(failure.code()).isEqualTo("QUESTION_QUERY_INVALID");
    }

    @Test
    void updateNeedsDraftAndMatchingVersion() {
        Question active = active();
        assertThat(api(() -> service.update(manager, active.id(), content(), 0)).code())
                .isEqualTo("QUESTION_NOT_EDITABLE");

        Question draft = draft();
        assertThat(api(() -> service.update(manager, draft.id(), content(), 7)).code())
                .isEqualTo("QUESTION_VERSION_CONFLICT");
        when(repo.update(any())).thenReturn(false);
        assertThat(api(() -> service.update(manager, draft.id(), content(), 0)).status())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void editingActiveQuestionOpensNextDraftOrReturnsExistingOne() {
        Question active = active();
        when(repo.maxVersionNo(active.lineageId())).thenReturn(1);

        var opened = service.openDraft(manager, active.id());
        assertThat(opened.question().versionNo()).isEqualTo(2);
        assertThat(opened.question().lineageId()).isEqualTo(active.lineageId());
        assertThat(opened.question().status()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(audits).extracting(AuditEvent::action).containsExactly("QUESTION_CREATED");

        Question existing = active.nextDraft(2, manager.accountId(), NOW);
        when(repo.findBankDraft(active.lineageId())).thenReturn(Optional.of(existing));
        assertThat(service.openDraft(manager, active.id()).question().id()).isEqualTo(existing.id());
    }

    @Test
    void concurrentDraftOpeningReturnsTheDraftThatWon() {
        Question active = active();
        Question winner = active.nextDraft(2, UUID.randomUUID(), NOW);
        when(repo.findBankDraft(active.lineageId())).thenReturn(Optional.empty(), Optional.of(winner));
        doThrow(new DuplicateKeyException("ux_questions_bank_draft")).when(repo).insert(any());

        assertThat(service.openDraft(manager, active.id()).question().id()).isEqualTo(winner.id());
    }

    @Test
    void activateValidatesAndAudits() {
        Question draft = draft();
        Question activated = draft.activate(NOW);
        when(repo.find(draft.id())).thenReturn(Optional.of(draft), Optional.of(activated));

        assertThat(service.activate(manager, draft.id(), 0).question().status()).isEqualTo(QuestionStatus.ACTIVE);
        verify(repo).update(any());
        assertThat(audits).extracting(AuditEvent::action).containsExactly("QUESTION_ACTIVATED");
        Question again = active();
        assertThat(api(() -> service.activate(manager, again.id(), 0)).code()).isEqualTo("QUESTION_NOT_DRAFT");
    }

    @Test
    void retireOnlyActiveQuestions() {
        Question draft = draft();
        assertThat(api(() -> service.retire(manager, draft.id(), 0)).code()).isEqualTo("QUESTION_NOT_ACTIVE");
        Question active = active();
        when(repo.find(active.id())).thenReturn(Optional.of(active), Optional.of(active.retire(NOW)));
        assertThat(service.retire(manager, active.id(), 0).question().status()).isEqualTo(QuestionStatus.RETIRED);
        assertThat(audits).extracting(AuditEvent::action).containsExactly("QUESTION_RETIRED");
    }

    @Test
    void deleteOnlyNeverActivatedDraftsOthersMustBeRetired() {
        Question active = active();
        assertThat(api(() -> service.delete(manager, active.id(), 0)).code()).isEqualTo("QUESTION_DELETE_BLOCKED");
        verify(repo, never()).delete(any(), eq(0L));

        Question draft = draft();
        when(repo.delete(draft.id(), 0)).thenReturn(true);
        service.delete(manager, draft.id(), 0);
        assertThat(audits).extracting(AuditEvent::action).containsExactly("QUESTION_DELETED");
    }

    @Test
    void quizInlineQuestionsAreNotReachableThroughTheBank() {
        Question bank = Question.newBankDraft(subject, content(), manager.accountId(), NOW);
        Question inline = new Question(bank.id(), bank.lineageId(), 1, QuestionScope.QUIZ,
                subject, UUID.randomUUID(), content(), QuestionStatus.DRAFT, null, manager.accountId(), NOW, NOW, 0);
        stored(inline);
        assertThat(api(() -> service.detail(manager, inline.id())).status()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
