package edu.aiplatform.quizzes.application;

import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.domain.DraftStatus;
import edu.aiplatform.quizzes.domain.DraftTarget;
import edu.aiplatform.quizzes.domain.IQuestionRepository;
import edu.aiplatform.quizzes.domain.IQuizAiDraftRepository;
import edu.aiplatform.quizzes.domain.IQuizRepository;
import edu.aiplatform.quizzes.domain.Question;
import edu.aiplatform.quizzes.domain.QuestionScope;
import edu.aiplatform.quizzes.domain.QuestionStatus;
import edu.aiplatform.quizzes.domain.Quiz;
import edu.aiplatform.quizzes.domain.QuizAiDraft;
import edu.aiplatform.quizzes.domain.QuizItem;
import edu.aiplatform.quizzes.domain.QuizOrigin;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;

/** Bản giả trong bộ nhớ của các repository U10, đủ để kiểm luồng soạn, phát hành, copy Quiz (không kiểm SQL). */
class InMemoryQuizStore {

    final Map<UUID, Quiz> quizzes = new LinkedHashMap<>();
    final Map<UUID, List<QuizItem>> items = new HashMap<>();
    final Map<UUID, Question> questions = new LinkedHashMap<>();
    final Quizzes quizRepository = new Quizzes();
    final Questions questionRepository = new Questions();
    final Map<UUID, QuizAiDraft> drafts = new LinkedHashMap<>();
    final Drafts draftRepository = new Drafts();

    void put(Question q) {
        questions.put(q.id(), q);
    }

    class Quizzes implements IQuizRepository {

        @Override
        public Optional<Quiz> find(UUID id) {
            return Optional.ofNullable(quizzes.get(id));
        }

        @Override
        public void insert(Quiz quiz) {
            if (quiz.origin() == QuizOrigin.NEW_VERSION && quizzes.values().stream().anyMatch(q ->
                    q.origin() == QuizOrigin.NEW_VERSION && quiz.sourceQuizId().equals(q.sourceQuizId())
                            && (q.status() == QuizStatus.DRAFT || q.status() == QuizStatus.REVIEWED))) {
                throw new DuplicateKeyException("ux_quizzes_open_new_version");
            }
            quizzes.put(quiz.id(), quiz);
        }

        @Override
        public boolean update(Quiz quiz) {
            Quiz current = quizzes.get(quiz.id());
            if (current == null || current.version() != quiz.version()) {
                return false;
            }
            quizzes.put(quiz.id(), withVersion(quiz, quiz.version() + 1));
            return true;
        }

        @Override
        public boolean delete(UUID id, long version) {
            Quiz q = quizzes.get(id);
            if (q == null || q.version() != version || q.publishedAt() != null) {
                return false;
            }
            items.remove(id);
            questions.values().removeIf(question -> id.equals(question.quizId()));
            quizzes.remove(id);
            return true;
        }

        @Override
        public List<QuizItem> items(UUID quizId) {
            return items.getOrDefault(quizId, List.of()).stream().sorted(Comparator.comparingInt(QuizItem::position)).toList();
        }

        @Override
        public void replaceItems(UUID quizId, List<QuizItem> list) {
            items.put(quizId, List.copyOf(list));
        }

        @Override
        public QuizPage listSubjectQuizzes(UUID subjectId, QuizStatus status, String text, int page, int size) {
            List<QuizSummary> all = quizzes.values().stream()
                    .filter(q -> q.scope() == QuizScope.SUBJECT && q.subjectId().equals(subjectId))
                    .filter(q -> status == null || q.status() == status).map(this::summary).toList();
            return new QuizPage(all.stream().skip((long) page * size).limit(size).toList(), all.size(), page, size);
        }

        @Override
        public List<QuizSummary> listClassQuizzes(UUID classId) {
            return quizzes.values().stream().filter(q -> q.scope() == QuizScope.CLASS && q.classId().equals(classId))
                    .map(this::summary).toList();
        }

        @Override
        public List<QuizSummary> listSubjectQuizzes(UUID subjectId, Collection<QuizStatus> statuses) {
            return quizzes.values().stream()
                    .filter(q -> q.scope() == QuizScope.SUBJECT && q.subjectId().equals(subjectId) && statuses.contains(q.status()))
                    .map(this::summary).toList();
        }

        @Override
        public List<QuizSummary> listClassQuizzesOf(Collection<UUID> classIds, UUID excludeClassId) {
            return quizzes.values().stream().filter(q -> q.scope() == QuizScope.CLASS && classIds.contains(q.classId())
                    && !q.classId().equals(excludeClassId)).map(this::summary).toList();
        }

        @Override
        public List<QuizSummary> listOpenByMaterial(UUID materialId, UUID classId, UUID subjectId) {
            return quizzes.values().stream().filter(q -> q.status() == QuizStatus.OPEN && q.referenceMaterialIds().contains(materialId))
                    .map(this::summary).toList();
        }

        @Override
        public Optional<Quiz> findOpenInVersionChain(UUID quizId) {
            List<UUID> chain = chain(quizId);
            return quizzes.values().stream().filter(q -> chain.contains(q.id()) && !q.id().equals(quizId)
                    && q.status() == QuizStatus.OPEN).findFirst();
        }

        @Override
        public int maxVersionNo(UUID quizId) {
            return chain(quizId).stream().mapToInt(id -> quizzes.get(id).versionNo()).max().orElse(0);
        }

        private List<UUID> chain(UUID quizId) {
            Quiz root = quizzes.get(quizId);
            while (root.origin() == QuizOrigin.NEW_VERSION && quizzes.containsKey(root.sourceQuizId())) {
                root = quizzes.get(root.sourceQuizId());
            }
            List<UUID> result = new ArrayList<>(List.of(root.id()));
            for (int i = 0; i < result.size(); i++) {
                UUID parent = result.get(i);
                quizzes.values().stream().filter(q -> q.origin() == QuizOrigin.NEW_VERSION && parent.equals(q.sourceQuizId()))
                        .forEach(q -> result.add(q.id()));
            }
            return result;
        }

        private QuizSummary summary(Quiz q) {
            List<QuizItem> list = items(q.id());
            return new QuizSummary(q, list.size(), list.stream().map(QuizItem::points).reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        private static Quiz withVersion(Quiz q, long version) {
            return new Quiz(q.id(), q.scope(), q.classId(), q.subjectId(), q.sourceQuizId(), q.origin(), q.versionNo(),
                    q.title(), q.instructions(), q.settings(), q.activityMonitoringEnabled(), q.activityNotice(),
                    q.referenceMaterialIds(), q.status(), q.publishedAt(), q.retiredAt(), q.retiredReason(), q.createdBy(),
                    q.createdAt(), q.updatedAt(), version);
        }

    }

    class Questions implements IQuestionRepository {

        @Override
        public Optional<Question> find(UUID id) {
            return Optional.ofNullable(questions.get(id));
        }

        @Override
        public boolean delete(UUID id, long version) {
            return questions.remove(id) != null;
        }

        @Override
        public Optional<Question> findBankDraft(UUID lineageId) {
            return questions.values().stream().filter(q -> q.lineageId().equals(lineageId) && q.isDraft()).findFirst();
        }

        @Override
        public List<Question> lineage(UUID lineageId) {
            return questions.values().stream().filter(q -> q.lineageId().equals(lineageId)).toList();
        }

        @Override
        public int maxVersionNo(UUID lineageId) {
            return lineage(lineageId).stream().mapToInt(Question::versionNo).max().orElse(0);
        }

        @Override
        public void insert(Question question) {
            questions.put(question.id(), question);
        }

        @Override
        public boolean update(Question question) {
            Question current = questions.get(question.id());
            if (current == null || current.version() != question.version()) {
                return false;
            }
            questions.put(question.id(), question);
            return true;
        }

        @Override
        public BankPage searchBank(UUID subjectId, BankQuery query) {
            return new BankPage(List.of(), 0, query.page(), query.size());
        }

        @Override
        public int countQuizzesUsing(UUID questionId) {
            return 0;
        }

        @Override
        public List<Question> findAll(Collection<UUID> ids) {
            return ids.stream().map(questions::get).filter(q -> q != null).toList();
        }

        @Override
        public List<Question> inlineOf(UUID quizId) {
            return questions.values().stream().filter(q -> q.scope() == QuestionScope.QUIZ && quizId.equals(q.quizId())).toList();
        }

        @Override
        public boolean deleteInline(UUID questionId, UUID quizId) {
            Question q = questions.get(questionId);
            return q != null && quizId.equals(q.quizId()) && questions.remove(questionId) != null;
        }

        private List<Question> active(UUID subjectId, PickQuery query) {
            return questions.values().stream().filter(q -> q.scope() == QuestionScope.BANK && q.subjectId().equals(subjectId)
                            && q.status() == QuestionStatus.ACTIVE
                            && (query.excludeLineages() == null || !query.excludeLineages().contains(q.lineageId()))
                            && (query.difficulty() == null || q.content().difficulty() == query.difficulty()))
                    .toList();
        }

        @Override
        public BankPage searchActive(UUID subjectId, PickQuery query) {
            List<Question> all = active(subjectId, query);
            return new BankPage(all.stream().map(q -> new BankRow(q, false)).toList(), all.size(), query.page(), query.size());
        }

        @Override
        public List<Question> randomActive(UUID subjectId, PickQuery query, int count) {
            return active(subjectId, query).stream().limit(count).toList();
        }

        @Override
        public long countActive(UUID subjectId, PickQuery query) {
            return active(subjectId, query).size();
        }
    }

    class Drafts implements IQuizAiDraftRepository {

        @Override
        public void insert(QuizAiDraft draft) {
            drafts.put(draft.id(), draft);
        }

        @Override
        public Optional<QuizAiDraft> find(UUID id) {
            return Optional.ofNullable(drafts.get(id));
        }

        @Override
        public Optional<QuizAiDraft> findByRequest(UUID createdBy, String requestRef) {
            return drafts.values().stream()
                    .filter(d -> d.createdBy().equals(createdBy) && requestRef.equals(d.requestRef())).findFirst();
        }

        @Override
        public List<QuizAiDraft> listOpenForQuiz(UUID quizId) {
            return drafts.values().stream().filter(d -> quizId.equals(d.quizId()) && d.status().isOpen()).toList();
        }

        @Override
        public List<QuizAiDraft> listOpenForBank(UUID subjectId) {
            return drafts.values().stream().filter(d -> d.target() == DraftTarget.BANK
                    && d.subjectId().equals(subjectId) && d.status().isOpen()).toList();
        }

        @Override
        public boolean transition(QuizAiDraft next, DraftStatus expected) {
            QuizAiDraft current = drafts.get(next.id());
            if (current == null || current.status() != expected) {
                return false;
            }
            UUID task = next.taskId() != null ? next.taskId() : current.taskId();
            drafts.put(next.id(), new QuizAiDraft(next.id(), next.target(), next.quizId(), next.subjectId(),
                    next.origin(), task, next.status(), next.proposal(), next.provenance(), next.baseVersion(),
                    next.createdBy(), next.createdAt(), next.decidedAt()));
            return true;
        }

        @Override
        public void attachTask(UUID id, UUID taskId) {
            QuizAiDraft d = drafts.get(id);
            if (d != null && d.taskId() == null) {
                drafts.put(id, new QuizAiDraft(d.id(), d.target(), d.quizId(), d.subjectId(), d.origin(), taskId,
                        d.status(), d.proposal(), d.provenance(), d.baseVersion(), d.createdBy(), d.createdAt(),
                        d.decidedAt()));
            }
        }
    }
}
