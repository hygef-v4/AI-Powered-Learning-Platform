package edu.aiplatform.quizzes.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.quizzes.application.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.application.QuizViews.QuizRow;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.domain.IQuestionRepository;
import edu.aiplatform.quizzes.domain.IQuizRepository;
import edu.aiplatform.quizzes.domain.Question;
import edu.aiplatform.quizzes.domain.QuestionScope;
import edu.aiplatform.quizzes.domain.Quiz;
import edu.aiplatform.quizzes.domain.QuizItem;
import edu.aiplatform.quizzes.domain.QuizOrigin;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhân bản, version mới, copy Quiz giữa lớp (UC 48; F11; BR-U10-30, 31). Sao tiêu đề, hướng dẫn, cài đặt, theo
 * dõi, tài liệu và câu: câu ngân hàng giữ phiên bản đã ghim, câu riêng sao thành câu riêng mới. Không sao lượt
 * làm, kết quả, đề xuất AI.
 */
@Service
public class QuizCopyService {

    private final IQuizRepository quizzes;
    private final IQuestionRepository questions;
    private final IClassScope classScope;
    private final QuizSupport support;
    private final QuizAccess access;
    private final QuizAudit audit;
    private final Clock clock;

    public QuizCopyService(IQuizRepository quizzes, IQuestionRepository questions, IClassScope classScope,
                           QuizSupport support, QuizAccess access, QuizAudit audit, Clock clock) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.classScope = classScope;
        this.support = support;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    /** Nhân bản trong cùng lớp hoặc môn: Quiz mới độc lập, version 1. */
    @Transactional
    public QuizDetail cloneQuiz(Actor actor, UUID id) {
        Quiz source = support.loadManaged(actor, id);
        support.requireContainerActive(source);
        Quiz copy = copy(actor, source, QuizOrigin.CLONE, 1, source.classId());
        audit.success(actor, "QUIZ_CLONED", QuizSupport.OBJECT, copy.id(), Map.of("sourceQuizId", id.toString()));
        return support.detail(copy, true);
    }

    /** Version mới từ Quiz đã phát hành (BR-U10-30); mỗi chuỗi tối đa một version đang soạn. */
    @Transactional
    public QuizDetail newVersion(Actor actor, UUID id) {
        Quiz source = support.loadManaged(actor, id);
        if (source.status() != QuizStatus.OPEN && source.status() != QuizStatus.RETIRED) {
            throw ApiException.conflict("QUIZ_NOT_PUBLISHED", "Chỉ tạo version mới từ quiz đã phát hành");
        }
        support.requireContainerActive(source);
        Quiz copy;
        try {
            copy = copy(actor, source, QuizOrigin.NEW_VERSION, quizzes.maxVersionNo(id) + 1, source.classId());
        } catch (DuplicateKeyException exists) {
            throw ApiException.conflict("QUIZ_VERSION_EXISTS", "Quiz này đã có một version mới đang soạn");
        }
        audit.success(actor, "QUIZ_VERSION_CREATED", QuizSupport.OBJECT, copy.id(),
                Map.of("sourceQuizId", id.toString(), "versionNo", copy.versionNo()));
        return support.detail(copy, true);
    }

    /** Quiz của các lớp khác cùng môn mà giảng viên đang dạy (nguồn copy). */
    @Transactional(readOnly = true)
    public List<QuizRow> copySources(Actor actor, UUID classId) {
        access.requireClass(actor, Actions.QUIZ_CLASS_MANAGE, classId, "CLASS", classId, "CLASS_NOT_FOUND");
        ClassRef target = support.classRef(classId);
        List<UUID> sameSubject = new ArrayList<>();
        for (UUID taught : taughtClasses(actor)) {
            if (!taught.equals(classId) && support.classRef(taught).subjectId().equals(target.subjectId())) {
                sameSubject.add(taught);
            }
        }
        return quizzes.listClassQuizzesOf(sameSubject, classId).stream().map(s -> QuizRow.of(s, true)).toList();
    }

    /** Copy Quiz của lớp khác sang lớp này: giảng viên phải dạy cả hai lớp, hai lớp cùng môn. */
    @Transactional
    public QuizDetail copyToClass(Actor actor, UUID classId, UUID sourceQuizId) {
        access.requireClass(actor, Actions.QUIZ_CLASS_MANAGE, classId, "CLASS", classId, "CLASS_NOT_FOUND");
        Quiz source = support.load(sourceQuizId);
        if (source.scope() != QuizScope.CLASS || source.classId().equals(classId)) {
            throw ApiException.notFound(QuizSupport.NOT_FOUND);
        }
        access.requireClass(actor, Actions.QUIZ_CLASS_MANAGE, source.classId(), QuizSupport.OBJECT, sourceQuizId,
                QuizSupport.NOT_FOUND);
        ClassRef target = support.classRef(classId);
        if (!target.subjectId().equals(source.subjectId())) {
            throw ApiException.conflict("QUIZ_COPY_SUBJECT_MISMATCH", "Chỉ copy quiz giữa các lớp cùng môn");
        }
        Quiz copy = copy(actor, source, QuizOrigin.CLASS_COPY, 1, classId);
        support.requireContainerActive(copy);
        audit.success(actor, "QUIZ_COPIED", QuizSupport.OBJECT, copy.id(),
                Map.of("sourceQuizId", sourceQuizId.toString(), "classId", classId.toString()));
        return support.detail(copy, true);
    }

    private Quiz copy(Actor actor, Quiz source, QuizOrigin origin, int versionNo, UUID classId) {
        Instant now = clock.instant();
        Quiz copy = source.copyAs(origin, versionNo, classId, actor.accountId(), now);
        quizzes.insert(copy);
        Map<UUID, Question> byId = support.questionsOf(quizzes.items(source.id()));
        List<QuizItem> items = new ArrayList<>();
        for (QuizItem item : quizzes.items(source.id())) {
            Question q = byId.get(item.questionId());
            UUID questionId = item.questionId();
            if (q != null && q.scope() == QuestionScope.QUIZ) {
                Question inline = q.copyInline(copy.id(), actor.accountId(), now);
                questions.insert(inline);
                questionId = inline.id();
            }
            items.add(new QuizItem(copy.id(), questionId, item.position(), item.points()));
        }
        quizzes.replaceItems(copy.id(), items);
        return copy;
    }

    private List<UUID> taughtClasses(Actor actor) {
        try {
            return classScope.listTaughtClasses(actor.accountId());
        } catch (RuntimeException unavailable) {
            throw ApiException.unavailable("CLASS_UNAVAILABLE", "Chưa kiểm tra được lớp, vui lòng thử lại");
        }
    }
}
