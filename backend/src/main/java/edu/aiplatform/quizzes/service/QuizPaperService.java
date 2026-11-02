package edu.aiplatform.quizzes.service;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.DrawRule;
import edu.aiplatform.quizzes.model.Question;
import edu.aiplatform.quizzes.model.Quiz;
import edu.aiplatform.quizzes.model.QuizPaperMode;
import edu.aiplatform.quizzes.model.QuizRules;
import edu.aiplatform.quizzes.model.Violation;
import edu.aiplatform.quizzes.repository.IQuestionRepository;
import edu.aiplatform.quizzes.repository.IQuestionRepository.PoolEntry;
import edu.aiplatform.quizzes.repository.IQuizRepository;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionView;
import edu.aiplatform.quizzes.service.QuizViews.BankTag;
import edu.aiplatform.quizzes.service.QuizViews.PaperPreview;
import edu.aiplatform.quizzes.service.QuizViews.PaperPreviewItem;
import edu.aiplatform.quizzes.service.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.service.QuizViews.RuleShortage;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sinh đề theo quy tắc (BR-U10-17; thầy Kiên 2026-10-29): người soạn đặt "thẻ nào lấy bao nhiêu câu", mỗi lượt làm bốc
 * một đề riêng từ các câu {@code ACTIVE} của ngân hàng môn. Thiếu câu thì chặn phát hành và từ chối bắt đầu lượt.
 */
@Service
public class QuizPaperService {

    private static final SecureRandom SEEDS = new SecureRandom();

    private final IQuizRepository quizzes;
    private final IQuestionRepository questions;
    private final QuizSupport support;
    private final Clock clock;

    public QuizPaperService(IQuizRepository quizzes, IQuestionRepository questions, QuizSupport support, Clock clock) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.support = support;
        this.clock = clock;
    }

    public record RuleInput(String tag, Difficulty difficulty, int questionCount, BigDecimal pointsEach) {
    }

    /** Đổi chế độ đề hoặc quy tắc: như sửa danh sách câu, Quiz đang mở đã có lượt thì phải tạo version mới. */
    @Transactional
    public QuizDetail saveRules(Actor actor, UUID id, QuizPaperMode mode, List<RuleInput> inputs, long version) {
        Quiz quiz = support.loadEditable(actor, id, version);
        support.requireNoAttemptsWhenOpen(quiz);
        List<DrawRule> rules = new ArrayList<>();
        for (RuleInput input : inputs) {
            String tag = input.tag() == null ? null : input.tag().strip();
            rules.add(new DrawRule(id, rules.size() + 1, tag, input.difficulty(), input.questionCount(), input.pointsEach()));
        }
        List<Violation> violations = QuizRules.validateDrawRules(rules);
        if (!violations.isEmpty()) {
            throw new RuleViolationException(HttpStatus.BAD_REQUEST, "QUIZ_INVALID", "Quy tắc sinh đề chưa hợp lệ", violations);
        }
        quizzes.replaceDrawRules(id, rules);
        return support.detail(support.save(quiz.withPaperMode(mode, clock.instant())), true);
    }

    @Transactional(readOnly = true)
    public List<BankTag> bankTags(Actor actor, UUID id) {
        Quiz quiz = support.loadManaged(actor, id);
        Map<String, String> display = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PoolEntry entry : questions.activePool(quiz.subjectId())) {
            for (String tag : entry.tags()) {
                String key = tag.strip().toLowerCase(Locale.ROOT);
                display.putIfAbsent(key, tag.strip());
                counts.merge(key, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream().map(e -> new BankTag(display.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(BankTag::tag, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Transactional(readOnly = true)
    public PaperPreview preview(Actor actor, UUID id) {
        Quiz quiz = support.loadManaged(actor, id);
        List<DrawRule> rules = quizzes.drawRules(id);
        QuizPaperPlanner.Plan plan = QuizPaperPlanner.plan(rules, questions.activePool(quiz.subjectId()), Set.of(),
                SEEDS.nextLong());
        Map<UUID, Question> byId = questions.findAll(plan.picks().values().stream().flatMap(List::stream)
                        .map(PoolEntry::id).toList()).stream()
                .collect(Collectors.toMap(Question::id, Function.identity()));
        List<PaperPreviewItem> items = new ArrayList<>();
        for (DrawRule rule : rules) {
            for (PoolEntry entry : plan.picks().get(rule.position())) {
                items.add(new PaperPreviewItem(rule.position(), rule.pointsEach(), QuestionView.of(byId.get(entry.id()))));
            }
        }
        return new PaperPreview(items, QuizSupport.maxScoreOf(rules), shortages(rules, plan));
    }

    /** Điều kiện phát hành Quiz sinh đề: có quy tắc hợp lệ và kho đủ câu cho mọi quy tắc. */
    List<Violation> violationsOf(Quiz quiz) {
        List<DrawRule> rules = quizzes.drawRules(quiz.id());
        if (rules.isEmpty()) {
            return List.of(new Violation("drawRules", "RULE_COUNT", "Quiz sinh đề cần ít nhất 1 quy tắc"));
        }
        List<Violation> violations = new ArrayList<>(QuizRules.validateDrawRules(rules));
        QuizPaperPlanner.Plan plan = QuizPaperPlanner.plan(rules, questions.activePool(quiz.subjectId()), Set.of(), 0L);
        for (RuleShortage s : shortages(rules, plan)) {
            violations.add(new Violation("drawRules[" + s.position() + "]", "POOL_INSUFFICIENT", shortageText(s)));
        }
        return List.copyOf(violations);
    }

    /** Bốc đề cho một lượt; trả câu và điểm theo thứ tự quy tắc. */
    Map<UUID, BigDecimal> draw(Quiz quiz, Set<UUID> seenQuestionIds, long seed) {
        List<DrawRule> rules = quizzes.drawRules(quiz.id());
        Set<UUID> seenLineages = questions.findAll(seenQuestionIds).stream().map(Question::lineageId)
                .collect(Collectors.toCollection(HashSet::new));
        QuizPaperPlanner.Plan plan = QuizPaperPlanner.plan(rules, questions.activePool(quiz.subjectId()), seenLineages, seed);
        List<RuleShortage> shortages = shortages(rules, plan);
        if (rules.isEmpty() || !shortages.isEmpty()) {
            throw ApiException.conflict("QUIZ_POOL_INSUFFICIENT",
                    "Ngân hàng câu hỏi hiện không đủ câu để tạo đề; hãy báo giảng viên");
        }
        Map<UUID, BigDecimal> paper = new LinkedHashMap<>();
        for (DrawRule rule : rules) {
            plan.picks().get(rule.position()).forEach(entry -> paper.put(entry.id(), rule.pointsEach()));
        }
        return paper;
    }

    /** Tên các Quiz sinh đề đang mở của môn không còn đủ câu (cảnh báo khi ngưng câu, BR-U10-17). */
    List<String> openQuizzesShortOfQuestions(UUID subjectId) {
        List<PoolEntry> pool = questions.activePool(subjectId);
        return quizzes.listOpenGenerated(subjectId).stream()
                .filter(quiz -> {
                    List<DrawRule> rules = quizzes.drawRules(quiz.id());
                    return !shortages(rules, QuizPaperPlanner.plan(rules, pool, Set.of(), 0L)).isEmpty();
                })
                .map(Quiz::title).toList();
    }

    private static List<RuleShortage> shortages(List<DrawRule> rules, QuizPaperPlanner.Plan plan) {
        return rules.stream().filter(r -> plan.picked(r.position()) < r.questionCount())
                .map(r -> new RuleShortage(r.position(), r.tag(), r.difficulty(), r.questionCount(), plan.picked(r.position())))
                .toList();
    }

    private static String shortageText(RuleShortage s) {
        return "Quy tắc " + s.position() + " (thẻ \"" + s.tag() + "\"): cần " + s.required() + " câu, kho chỉ cấp được "
                + s.available() + " câu";
    }
}
