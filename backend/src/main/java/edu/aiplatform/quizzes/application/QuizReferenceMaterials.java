package edu.aiplatform.quizzes.application;

import edu.aiplatform.ai.contract.IReferenceMaterial;
import edu.aiplatform.ai.contract.MaterialText;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.domain.Quiz;
import edu.aiplatform.shared.error.ApiException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tài liệu tham khảo của U13 qua {@code IReferenceMaterial} (H8): danh sách chọn được theo lớp hoặc môn của Quiz,
 * kiểm id khi gắn vào Quiz hoặc gửi AI, lấy chữ cho prompt. U13 lỗi không chặn soạn tay (BR-U10-52).
 */
@Service
public class QuizReferenceMaterials {

    private final IReferenceMaterial materials;
    private final QuizSupport support;
    private final QuizAccess access;

    public QuizReferenceMaterials(IReferenceMaterial materials, QuizSupport support, QuizAccess access) {
        this.materials = materials;
        this.support = support;
        this.access = access;
    }

    /** Tài liệu gắn được vào Quiz hoặc đưa vào popup AI của Quiz. */
    @Transactional(readOnly = true)
    public List<SelectableMaterial> forQuiz(Actor actor, UUID quizId) {
        Quiz quiz = support.loadManaged(actor, quizId);
        return list(actor, scopeOf(quiz));
    }

    /** Tài liệu của môn cho popup AI của ngân hàng câu hỏi. */
    @Transactional(readOnly = true)
    public List<SelectableMaterial> forSubject(Actor actor, UUID subjectId) {
        access.requireSubject(actor, Actions.QUESTION_BANK_MANAGE, subjectId, "SUBJECT", subjectId,
                "SUBJECT_NOT_FOUND");
        return list(actor, ResourceRef.subject(subjectId));
    }

    static ResourceRef scopeOf(Quiz quiz) {
        return quiz.scope() == QuizScope.CLASS ? ResourceRef.courseClass(quiz.classId())
                : ResourceRef.subject(quiz.subjectId());
    }

    /** Mọi id phải nằm trong danh sách chọn được; {@code needText}: tài liệu đã có chữ (dùng cho AI). */
    List<SelectableMaterial> requireSelectable(Actor actor, ResourceRef scope, Collection<UUID> ids,
                                               boolean needText) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, SelectableMaterial> selectable = list(actor, scope).stream()
                .collect(Collectors.toMap(SelectableMaterial::materialId, Function.identity(), (a, b) -> a));
        List<SelectableMaterial> chosen = new ArrayList<>();
        for (UUID id : ids) {
            SelectableMaterial material = selectable.get(id);
            if (material == null || (needText && !material.hasText())) {
                throw ApiException.badRequest("MATERIAL_NOT_SELECTABLE",
                        needText ? "Có tài liệu không chọn được hoặc chưa trích được chữ"
                                : "Có tài liệu không chọn được cho quiz này");
            }
            chosen.add(material);
        }
        return chosen;
    }

    /** Chữ của tài liệu cho prompt, tổng tối đa {@link QuizDraftPrompt#MAX_MATERIAL_CHARS} ký tự. */
    List<MaterialText> texts(Actor actor, List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        try {
            return materials.extractText(actor, ids, QuizDraftPrompt.MAX_MATERIAL_CHARS);
        } catch (ApiException rejected) {
            throw rejected;
        } catch (RuntimeException unavailable) {
            throw unavailable();
        }
    }

    private List<SelectableMaterial> list(Actor actor, ResourceRef scope) {
        try {
            List<SelectableMaterial> result = materials.listSelectable(actor, scope);
            return result == null ? List.of() : result;
        } catch (ApiException rejected) {
            throw rejected;
        } catch (RuntimeException unavailable) {
            throw unavailable();
        }
    }

    private static ApiException unavailable() {
        return ApiException.unavailable("MATERIAL_SERVICE_UNAVAILABLE",
                "Chưa lấy được tài liệu tham khảo, vui lòng thử lại");
    }
}
