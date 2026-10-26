package edu.aiplatform.quizzes.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizzes.application.QuestionViews.QuestionPage;
import edu.aiplatform.quizzes.application.QuestionViews.QuestionRow;
import edu.aiplatform.quizzes.application.QuestionViews.QuestionView;
import edu.aiplatform.quizzes.application.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.application.QuizViews.QuizPage;
import edu.aiplatform.quizzes.application.QuizViews.QuizRow;
import edu.aiplatform.quizzes.application.QuizViews.RandomPreview;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.domain.Difficulty;
import edu.aiplatform.quizzes.domain.IQuestionRepository;
import edu.aiplatform.quizzes.domain.IQuestionRepository.PickQuery;
import edu.aiplatform.quizzes.domain.IQuizRepository;
import edu.aiplatform.quizzes.domain.Question;
import edu.aiplatform.quizzes.domain.QuestionContent;
import edu.aiplatform.quizzes.domain.QuestionRules;
import edu.aiplatform.quizzes.domain.QuestionScope;
import edu.aiplatform.quizzes.domain.QuestionStatus;
import edu.aiplatform.quizzes.domain.Quiz;
import edu.aiplatform.quizzes.domain.QuizItem;
import edu.aiplatform.quizzes.domain.QuizRules;
import edu.aiplatform.quizzes.domain.Violation;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Soạn Quiz (UC 48; F5, F6, F7, F12): danh sách, tạo, thông tin và cài đặt, câu từ ngân hàng (chọn tay, bốc ngẫu
 * nhiên), câu riêng, xóa Quiz chưa phát hành. Mọi thay đổi kèm {@code version} của Quiz; Quiz đã duyệt về nháp.
 */
@Service
public class QuizAuthoringService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private static final int STEM_PREVIEW = 300;

    private final IQuizRepository quizzes;
    private final IQuestionRepository questions;
    private final QuizSupport support;
    private final QuizAccess access;
    private final QuizAudit audit;
    private final QuizReferenceMaterials materials;
    private final Clock clock;

    public QuizAuthoringService(IQuizRepository quizzes, IQuestionRepository questions, QuizSupport support,
                                QuizAccess access, QuizAudit audit, QuizReferenceMaterials materials, Clock clock) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.support = support;
        this.access = access;
        this.audit = audit;
        this.materials = materials;
        this.clock = clock;
    }

    /** Thông tin, cài đặt Quiz (F7 bước 5). */
    public record Details(String title, String instructions, QuizSettings settings, boolean activityMonitoringEnabled,
                          String activityNotice, List<UUID> referenceMaterialIds) {
    }

    /** Một câu khi lưu danh sách câu; {@code points} trống thì lấy điểm mặc định của câu. */
    public record ItemInput(UUID questionId, BigDecimal points) {
    }

    @Transactional(readOnly = true)
    public QuizPage listSubject(Actor actor, UUID subjectId, QuizStatus status, String text, Integer page, Integer size) {
        access.requireSubject(actor, Actions.QUIZ_SUBJECT_MANAGE, subjectId, "SUBJECT", subjectId, "SUBJECT_NOT_FOUND");
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_PAGE_SIZE : size;
        if (p < 0 || s < 1 || s > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("QUIZ_QUERY_INVALID", "page ≥ 0, size từ 1 đến 100");
        }
        var result = quizzes.listSubjectQuizzes(subjectId, status, text, p, s);
        return new QuizPage(result.items().stream().map(r -> QuizRow.of(r, false)).toList(), result.total(),
                result.page(), result.size());
    }

    /** Tab Đánh giá: Quiz của lớp và Quiz đã phát hành của môn chứa lớp (nhãn "Của môn", chỉ xem) (F5 bước 2). */
    @Transactional(readOnly = true)
    public List<QuizRow> listClass(Actor actor, UUID classId) {
        access.requireClass(actor, Actions.QUIZ_CLASS_MANAGE, classId, "CLASS", classId, "CLASS_NOT_FOUND");
        ClassRef ref = support.classRef(classId);
        List<QuizRow> rows = new ArrayList<>();
        quizzes.listClassQuizzes(classId).forEach(s -> rows.add(QuizRow.of(s, false)));
        quizzes.listSubjectQuizzes(ref.subjectId(), List.of(QuizStatus.OPEN, QuizStatus.RETIRED))
                .forEach(s -> rows.add(QuizRow.of(s, true)));
        return rows;
    }

    @Transactional
    public QuizDetail createForSubject(Actor actor, UUID subjectId, String title) {
        access.requireSubject(actor, Actions.QUIZ_SUBJECT_MANAGE, subjectId, "SUBJECT", subjectId, "SUBJECT_NOT_FOUND");
        return create(actor, Quiz.create(QuizScope.SUBJECT, null, subjectId, title, actor.accountId(), clock.instant()));
    }

    @Transactional
    public QuizDetail createForClass(Actor actor, UUID classId, String title) {
        access.requireClass(actor, Actions.QUIZ_CLASS_MANAGE, classId, "CLASS", classId, "CLASS_NOT_FOUND");
        ClassRef ref = support.classRef(classId);
        return create(actor, Quiz.create(QuizScope.CLASS, classId, ref.subjectId(), title, actor.accountId(),
                clock.instant()));
    }

    private QuizDetail create(Actor actor, Quiz quiz) {
        support.requireContainerActive(quiz);
        reject(QuizRules.validateDetails(quiz.title(), null, quiz.settings(), null, List.of()));
        quizzes.insert(quiz);
        audit.success(actor, "QUIZ_CREATED", QuizSupport.OBJECT, quiz.id(), Map.of("scope", quiz.scope().name()));
        return support.detail(quiz, true);
    }

    @Transactional(readOnly = true)
    public QuizDetail get(Actor actor, UUID id, UUID classId) {
        Quiz quiz = support.load(id);
        return support.detail(quiz, support.requireView(actor, quiz, classId));
    }

    @Transactional
    public QuizDetail updateDetails(Actor actor, UUID id, Details details, long version) {
        Quiz quiz = support.loadEditable(actor, id, version);
        String title = details.title() == null ? null : details.title().strip();
        List<Violation> violations = new ArrayList<>(QuizRules.validateDetails(title, details.instructions(),
                details.settings(), details.activityNotice(), details.referenceMaterialIds()));
        // K2: bật thì phải có lời báo cho sinh viên; thời hạn giữ dữ liệu cố định 1 ngày nên luôn bật được (BR-U10-15).
        violations.addAll(QuizRules.validateActivity(details.activityMonitoringEnabled(), details.activityNotice()));
        reject(violations);
        // H8: chỉ kiểm tài liệu mới gắn, để U13 lỗi không chặn lưu các thay đổi khác (BR-U10-52).
        List<UUID> added = details.referenceMaterialIds() == null ? List.of() : details.referenceMaterialIds().stream()
                .filter(m -> !quiz.referenceMaterialIds().contains(m)).distinct().toList();
        materials.requireSelectable(actor, QuizReferenceMaterials.scopeOf(quiz), added, false);
        Quiz changed = quiz.withDetails(title, blankToNull(details.instructions()), details.settings(),
                details.activityMonitoringEnabled(), blankToNull(details.activityNotice()),
                details.referenceMaterialIds(), clock.instant());
        return support.detail(support.save(changed), true);
    }

    /**
     * Lưu danh sách câu theo thứ tự (F7 bước 2–4): câu ngân hàng phải {@code ACTIVE} cùng môn (câu đã ghim từ trước
     * được giữ); câu riêng phải thuộc Quiz. Câu riêng bị bỏ khỏi danh sách thì xóa.
     */
    @Transactional
    public QuizDetail saveItems(Actor actor, UUID id, List<ItemInput> inputs, long version) {
        Quiz quiz = support.loadEditable(actor, id, version);
        List<QuizItem> current = quizzes.items(id);
        Set<UUID> pinned = current.stream().map(QuizItem::questionId).collect(Collectors.toSet());
        Map<UUID, Question> byId = questions.findAll(inputs.stream().map(ItemInput::questionId).distinct().toList())
                .stream().collect(Collectors.toMap(Question::id, q -> q));
        List<QuizItem> items = new ArrayList<>();
        List<Violation> violations = new ArrayList<>();
        int position = 0;
        for (ItemInput input : inputs) {
            position++;
            Question q = byId.get(input.questionId());
            String field = "items[" + position + "]";
            if (q == null || !q.subjectId().equals(quiz.subjectId())
                    || (q.scope() == QuestionScope.QUIZ && !id.equals(q.quizId()))) {
                violations.add(new Violation(field, "QUESTION_MISSING", "Câu " + position + " không tồn tại"));
                continue;
            }
            if (q.scope() == QuestionScope.BANK && q.status() != QuestionStatus.ACTIVE && !pinned.contains(q.id())) {
                violations.add(new Violation(field, "QUESTION_NOT_ACTIVE",
                        "Câu " + position + " chưa kích hoạt hoặc đã ngưng dùng"));
                continue;
            }
            BigDecimal points = input.points() == null ? q.content().defaultPoints() : input.points();
            items.add(new QuizItem(id, q.id(), position, points));
        }
        violations.addAll(QuizRules.validateItems(items));
        reject(violations);
        Set<UUID> kept = items.stream().map(QuizItem::questionId).collect(Collectors.toSet());
        quizzes.replaceItems(id, items);
        questions.inlineOf(id).stream().filter(q -> !kept.contains(q.id()))
                .forEach(q -> questions.deleteInline(q.id(), id));
        return support.detail(support.save(quiz.edited(clock.instant())), true);
    }

    /** Bốc thử N câu {@code ACTIVE} khớp bộ lọc, không trùng câu đã có, không vượt chỗ trống (BR-U10-14). */
    @Transactional(readOnly = true)
    public RandomPreview randomPreview(Actor actor, UUID id, int count, Difficulty difficulty, String tag) {
        Quiz quiz = support.loadManaged(actor, id);
        List<QuizItem> items = quizzes.items(id);
        int free = QuizRules.MAX_ITEMS - items.size();
        if (count < 1 || count > free) {
            throw ApiException.badRequest("QUIZ_RANDOM_COUNT_INVALID", "Số câu từ 1 đến " + Math.max(free, 0));
        }
        PickQuery query = new PickQuery(difficulty, tag, null, lineagesOf(items), 0, count);
        List<QuestionView> picked = questions.randomActive(quiz.subjectId(), query, count).stream()
                .map(QuestionView::of).toList();
        return new RandomPreview(picked, questions.countActive(quiz.subjectId(), query), free);
    }

    /** Câu {@code ACTIVE} của môn chứa Quiz để chọn tay (E2), trừ câu đã có trong Quiz. */
    @Transactional(readOnly = true)
    public QuestionPage bankQuestions(Actor actor, UUID id, Difficulty difficulty, String tag, String text,
                                      Integer page, Integer size) {
        Quiz quiz = support.loadManaged(actor, id);
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_PAGE_SIZE : size;
        if (p < 0 || s < 1 || s > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("QUIZ_QUERY_INVALID", "page ≥ 0, size từ 1 đến 100");
        }
        var result = questions.searchActive(quiz.subjectId(),
                new PickQuery(difficulty, tag, text, lineagesOf(quizzes.items(id)), p, s));
        List<QuestionRow> rows = result.items().stream().map(row -> {
            Question q = row.question();
            return new QuestionRow(q.id(), q.lineageId(), q.versionNo(), q.content().type(), preview(q.content().stem()),
                    q.content().difficulty(), q.content().tags(), q.status(), false, q.updatedAt());
        }).toList();
        return new QuestionPage(rows, result.total(), result.page(), result.size());
    }

    /** Thêm câu riêng vào cuối Quiz (BR-U10-13). */
    @Transactional
    public QuizDetail addInline(Actor actor, UUID id, QuestionContent content, long version) {
        Quiz quiz = support.loadEditable(actor, id, version);
        rejectQuestion(QuestionRules.validate(content));
        List<QuizItem> items = new ArrayList<>(quizzes.items(id));
        if (items.size() >= QuizRules.MAX_ITEMS) {
            throw ApiException.conflict("QUIZ_FULL", "Quiz tối đa 200 câu");
        }
        Question inline = Question.newInline(id, quiz.subjectId(), content, actor.accountId(), clock.instant());
        questions.insert(inline);
        items.add(new QuizItem(id, inline.id(), items.size() + 1, content.defaultPoints()));
        quizzes.replaceItems(id, items);
        return support.detail(support.save(quiz.edited(clock.instant())), true);
    }

    @Transactional
    public QuizDetail updateInline(Actor actor, UUID id, UUID questionId, QuestionContent content, long version) {
        Quiz quiz = support.loadEditable(actor, id, version);
        Question q = inlineOf(id, questionId);
        rejectQuestion(QuestionRules.validate(content));
        if (!questions.update(q.withContent(content, clock.instant()))) {
            throw QuizSupport.versionConflict();
        }
        return support.detail(support.save(quiz.edited(clock.instant())), true);
    }

    @Transactional
    public QuizDetail deleteInline(Actor actor, UUID id, UUID questionId, long version) {
        Quiz quiz = support.loadEditable(actor, id, version);
        inlineOf(id, questionId);
        List<QuizItem> remaining = new ArrayList<>();
        for (QuizItem item : quizzes.items(id)) {
            if (!item.questionId().equals(questionId)) {
                remaining.add(new QuizItem(id, item.questionId(), remaining.size() + 1, item.points()));
            }
        }
        quizzes.replaceItems(id, remaining);
        questions.deleteInline(questionId, id);
        return support.detail(support.save(quiz.edited(clock.instant())), true);
    }

    /** Xóa Quiz chưa từng phát hành, kèm câu riêng và đề xuất AI (F12, BR-U10-24). */
    @Transactional
    public void delete(Actor actor, UUID id, long version) {
        Quiz quiz = support.loadManaged(actor, id);
        if (!quiz.isDeletable()) {
            throw ApiException.conflict("QUIZ_DELETE_BLOCKED", "Quiz đã phát hành không xóa được; hãy ngưng quiz");
        }
        QuizSupport.requireVersion(quiz, version);
        if (!quizzes.delete(id, version)) {
            throw QuizSupport.versionConflict();
        }
        audit.success(actor, "QUIZ_DELETED", QuizSupport.OBJECT, id, Map.of("scope", quiz.scope().name()));
    }

    private Question inlineOf(UUID quizId, UUID questionId) {
        return questions.find(questionId)
                .filter(q -> q.scope() == QuestionScope.QUIZ && quizId.equals(q.quizId()))
                .orElseThrow(() -> ApiException.notFound("QUESTION_NOT_FOUND"));
    }

    private Set<UUID> lineagesOf(List<QuizItem> items) {
        return questions.findAll(items.stream().map(QuizItem::questionId).toList()).stream()
                .map(Question::lineageId).collect(Collectors.toSet());
    }

    private static void reject(List<Violation> violations) {
        if (!violations.isEmpty()) {
            throw new RuleViolationException(HttpStatus.BAD_REQUEST, "QUIZ_INVALID", "Quiz chưa hợp lệ", violations);
        }
    }

    private static void rejectQuestion(List<Violation> violations) {
        if (!violations.isEmpty()) {
            throw new QuestionInvalidException(violations);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String preview(String stem) {
        if (stem.codePointCount(0, stem.length()) <= STEM_PREVIEW) {
            return stem;
        }
        return stem.substring(0, stem.offsetByCodePoints(0, STEM_PREVIEW)) + "…";
    }
}
