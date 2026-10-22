package edu.aiplatform.quizzes.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.quizzes.application.QuestionViews.QuestionView;
import edu.aiplatform.quizzes.application.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.application.QuizViews.QuizInfo;
import edu.aiplatform.quizzes.application.QuizViews.QuizItemView;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.domain.IQuestionRepository;
import edu.aiplatform.quizzes.domain.IQuizRepository;
import edu.aiplatform.quizzes.domain.Question;
import edu.aiplatform.quizzes.domain.QuestionScope;
import edu.aiplatform.quizzes.domain.Quiz;
import edu.aiplatform.quizzes.domain.QuizItem;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Phần dùng chung của các service Quiz: tải, kiểm quyền (BR-U10-10, 50, 52), dựng Quiz Detail, lưu có version. */
@Component
class QuizSupport {

    static final String OBJECT = "QUIZ";
    static final String NOT_FOUND = "QUIZ_NOT_FOUND";

    private final IQuizRepository quizzes;
    private final IQuestionRepository questions;
    private final IClassAccess classes;
    private final QuizAccess access;
    private final ActiveSubjects activeSubjects;
    private final QuizActivityPolicy activity;

    QuizSupport(IQuizRepository quizzes, IQuestionRepository questions, IClassAccess classes, QuizAccess access,
                ActiveSubjects activeSubjects, QuizActivityPolicy activity) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.classes = classes;
        this.access = access;
        this.activeSubjects = activeSubjects;
        this.activity = activity;
    }

    boolean activityRetentionConfigured() {
        return activity.retentionConfigured();
    }

    Quiz load(UUID id) {
        return quizzes.find(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
    }

    /** Quiz của môn: Chủ nhiệm môn của môn (R2); Quiz của lớp: giảng viên của lớp (R3/R4). Không đạt → 404. */
    void requireManage(Actor actor, Quiz quiz) {
        if (quiz.scope() == QuizScope.SUBJECT) {
            access.requireSubject(actor, Actions.QUIZ_SUBJECT_MANAGE, quiz.subjectId(), OBJECT, quiz.id(), NOT_FOUND);
        } else {
            access.requireClass(actor, Actions.QUIZ_CLASS_MANAGE, quiz.classId(), OBJECT, quiz.id(), NOT_FOUND);
        }
    }

    /** Hỏi quyền sửa Quiz mà không ghi từ chối (dùng khi nhận kết quả AI, H6). */
    boolean canManage(Actor actor, Quiz quiz) {
        return quiz.scope() == QuizScope.SUBJECT
                ? access.allows(actor, Actions.QUIZ_SUBJECT_MANAGE, ResourceRef.subject(quiz.subjectId()))
                : access.allows(actor, Actions.QUIZ_CLASS_MANAGE, ResourceRef.courseClass(quiz.classId()));
    }

    Quiz loadManaged(Actor actor, UUID id) {
        Quiz quiz = load(id);
        requireManage(actor, quiz);
        return quiz;
    }

    /** Quiz đang soạn được: người có quyền, Quiz còn nháp/đã duyệt, lớp/môn chưa lưu trữ. */
    Quiz loadEditable(Actor actor, UUID id, long version) {
        Quiz quiz = loadManaged(actor, id);
        if (!quiz.isEditable()) {
            throw ApiException.conflict("QUIZ_NOT_EDITABLE", "Quiz đã phát hành không sửa được; hãy tạo version mới");
        }
        requireVersion(quiz, version);
        requireContainerActive(quiz);
        return quiz;
    }

    /**
     * Quiz người xem được: người có quyền sửa; hoặc giảng viên của lớp {@code classId} xem Quiz của môn chứa lớp đã
     * phát hành (chỉ xem). Trả về {@code true} khi người xem có quyền sửa.
     */
    boolean requireView(Actor actor, Quiz quiz, UUID classId) {
        if (quiz.scope() == QuizScope.SUBJECT && classId != null
                && !access.allows(actor, Actions.QUIZ_SUBJECT_MANAGE, ResourceRef.subject(quiz.subjectId()))) {
            access.requireClass(actor, Actions.QUIZ_CLASS_MANAGE, classId, OBJECT, quiz.id(), NOT_FOUND);
            ClassRef ref = classRef(classId);
            if (!ref.subjectId().equals(quiz.subjectId())
                    || (quiz.status() != QuizStatus.OPEN && quiz.status() != QuizStatus.RETIRED)) {
                throw ApiException.notFound(NOT_FOUND);
            }
            return false;
        }
        requireManage(actor, quiz);
        return true;
    }

    /** Môn còn {@code ACTIVE}; với Quiz của lớp, lớp chưa lưu trữ. */
    void requireContainerActive(Quiz quiz) {
        activeSubjects.require(quiz.subjectId());
        if (quiz.scope() == QuizScope.CLASS && classRef(quiz.classId()).status() == ClassStatus.ARCHIVED) {
            throw ApiException.conflict("CLASS_ARCHIVED", "Lớp đã lưu trữ, quiz của lớp chỉ xem");
        }
    }

    ClassRef classRef(UUID classId) {
        try {
            return classes.getClassRef(classId).orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        } catch (ApiException rejected) {
            throw rejected;
        } catch (RuntimeException unavailable) {
            throw ApiException.unavailable("CLASS_UNAVAILABLE", "Chưa kiểm tra được lớp, vui lòng thử lại");
        }
    }

    static void requireVersion(Quiz quiz, long version) {
        if (quiz.version() != version) {
            throw versionConflict();
        }
    }

    /** Lưu thay đổi của Quiz (kiểm version); trả bản có version mới. */
    Quiz save(Quiz quiz) {
        if (!quizzes.update(quiz)) {
            throw versionConflict();
        }
        return load(quiz.id());
    }

    static ApiException versionConflict() {
        return ApiException.conflict("QUIZ_VERSION_CONFLICT", "Quiz đã được thay đổi ở phiên khác; hãy tải lại");
    }

    Map<UUID, Question> questionsOf(List<QuizItem> items) {
        return questions.findAll(items.stream().map(QuizItem::questionId).toList()).stream()
                .collect(Collectors.toMap(Question::id, Function.identity()));
    }

    QuizDetail detail(Quiz quiz, boolean canManage) {
        List<QuizItem> items = quizzes.items(quiz.id());
        Map<UUID, Question> byId = questionsOf(items);
        List<QuizItemView> views = items.stream()
                .filter(item -> byId.containsKey(item.questionId()))
                .map(item -> {
                    Question q = byId.get(item.questionId());
                    return new QuizItemView(item.position(), item.points(), q.scope() == QuestionScope.QUIZ,
                            QuestionView.of(q));
                })
                .toList();
        BigDecimal max = items.stream().map(QuizItem::points).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new QuizDetail(QuizInfo.of(quiz), views, max, canManage, canManage && activity.retentionConfigured());
    }
}
