package edu.aiplatform.materials.service;

import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.quizzes.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module của môn (U13): Chủ nhiệm môn thêm, đổi tên, xóa, sắp xếp; học liệu của môn và của lớp bắt buộc thuộc một module.
 * Quiz (U10) gắn vào module qua bảng nối; người xem chỉ thấy Quiz họ được thấy (sinh viên: Quiz đang mở của lớp).
 */
@Service
@Transactional(timeout = 10)
public class MaterialModuleService {
    private final IMaterialModuleRepository materialModuleRepository;
    private final MaterialAccessPolicy materialAccessPolicy;
    private final IQuizQuery quizQuery;
    private final IAudit audit;
    private final Clock clock;

    public MaterialModuleService(
            IMaterialModuleRepository materialModuleRepository,
            MaterialAccessPolicy materialAccessPolicy,
            IQuizQuery quizQuery,
            IAudit audit,
            Clock clock) {
        this.materialModuleRepository = materialModuleRepository;
        this.materialAccessPolicy = materialAccessPolicy;
        this.quizQuery = quizQuery;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(timeout = 10, noRollbackFor = ApiException.class)
    public List<MaterialViews.Module> listSubjectModules(Actor actor, UUID subjectId) {
        materialAccessPolicy.requireSubjectListAccess(actor, subjectId);
        boolean editable = materialAccessPolicy.canManageModules(actor, subjectId);
        if (editable) ensureDefaultModule(actor, subjectId);
        // Trang môn chỉ hiện Quiz của môn; Quiz của lớp chỉ giảng viên của lớp đó mở được.
        return buildModuleViews(actor, subjectId, editable, q -> q.scope() == QuizScope.SUBJECT);
    }

    @Transactional(timeout = 10, noRollbackFor = ApiException.class)
    public List<MaterialViews.Module> listClassModules(Actor actor, UUID classId, boolean student) {
        var c = materialAccessPolicy.requireClassListAccess(actor, classId, student);
        if (!student) ensureDefaultModule(actor, c.subjectId());
        Predicate<QuizView> visible;
        if (student) {
            Set<UUID> open =
                    quizQuery.listForClass(classId).stream()
                            .map(QuizView::quizId)
                            .collect(Collectors.toSet());
            visible = q -> open.contains(q.quizId());
        } else
            visible =
                    q ->
                            q.scope() == QuizScope.SUBJECT
                                    ? q.status() == QuizStatus.OPEN
                                            || (q.status() == QuizStatus.RETIRED
                                                    && q.publishedAt() != null)
                                    : classId.equals(q.classId());
        return buildModuleViews(actor, c.subjectId(), false, visible);
    }

    /**
     * Học liệu luôn thuộc một module, mà chỉ Chủ nhiệm môn tạo được module: môn chưa có module nào (môn mới) thì tạo
     * "Module 1" khi người soạn mở trang, để giảng viên của lớp thêm được học liệu.
     */
    private void ensureDefaultModule(Actor actor, UUID subjectId) {
        if (!materialModuleRepository.listModules(subjectId).isEmpty()) return;
        materialModuleRepository.lockModuleCreation(subjectId);
        if (!materialModuleRepository.listModules(subjectId).isEmpty()
                || materialAccessPolicy.requireSubject(subjectId).status()
                        != edu.aiplatform.subjects.contract.SubjectStatus.ACTIVE) return;
        var now = clock.instant();
        var module =
                new MaterialModule(
                        UUID.randomUUID(),
                        subjectId,
                        "Module 1",
                        0,
                        actor.accountId(),
                        now,
                        now,
                        0);
        materialModuleRepository.insertModule(module);
        recordAudit(actor, "MATERIAL_MODULE_CREATED", module);
    }

    public MaterialViews.Module createModule(
            Actor actor, UUID subjectId, MaterialCommands.ModuleInput input) {
        materialAccessPolicy.requireModuleManagement(actor, subjectId);
        materialModuleRepository.lockModuleCreation(subjectId);
        var now = clock.instant();
        var module =
                new MaterialModule(
                        UUID.randomUUID(),
                        subjectId,
                        input.title(),
                        materialModuleRepository.listModules(subjectId).size(),
                        actor.accountId(),
                        now,
                        now,
                        0);
        materialModuleRepository.insertModule(module);
        recordAudit(actor, "MATERIAL_MODULE_CREATED", module);
        return new MaterialViews.Module(
                module.id(), module.title(), module.orderNo(), module.version(), true, List.of());
    }

    public void renameModule(Actor actor, UUID id, MaterialCommands.ModuleUpdate input) {
        var module = requireWritableModule(actor, id, input.version());
        var next = module.reviseModule(input.title(), module.orderNo(), clock.instant());
        if (
                !materialModuleRepository.updateModule(
                        next, module.version())) throw createVersionConflict();
        recordAudit(actor, "MATERIAL_MODULE_UPDATED", next);
    }

    /** Chỉ xóa module không còn học liệu đang dùng; học liệu đã lưu trữ dời sang module đầu tiên còn lại của môn. */
    public void deleteModule(Actor actor, UUID id, long expected) {
        var module = requireWritableModule(actor, id, expected);
        if (materialModuleRepository.countActiveMaterials(id) > 0)
            throw ApiException.createConflict(
                    "MATERIAL_MODULE_NOT_EMPTY",
                    "Module còn học liệu, chuyển hoặc lưu trữ học liệu trước khi xóa");
        if (materialModuleRepository.countArchivedMaterials(id) > 0) {
            var other =
                    materialModuleRepository.listModules(module.subjectId()).stream()
                            .filter(m -> !m.id().equals(id))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            ApiException.createConflict(
                                                    "MATERIAL_MODULE_LAST",
                                                    "Môn cần ít nhất một module để giữ học liệu đã lưu trữ"));
            materialModuleRepository.moveArchivedMaterials(id, other.id());
        }
        if (!materialModuleRepository.deleteModule(id, expected)) throw createVersionConflict();
        recordAudit(actor, "MATERIAL_MODULE_DELETED", module);
    }

    public void reorderModules(Actor actor, UUID subjectId, MaterialCommands.Reorder input) {
        materialAccessPolicy.requireModuleManagement(actor, subjectId);
        Set<UUID> seen = new HashSet<>();
        List<MaterialModule> rows = new ArrayList<>();
        for (var position : input.items()) {
            if (!seen.add(position.id()))
                throw ApiException.createBadRequest(
                        "MATERIAL_REORDER_INVALID", "Danh sách có module trùng");
            var module =
                    materialModuleRepository.findModule(position.id())
                            .filter(m -> m.subjectId().equals(subjectId))
                            .orElseThrow(() -> ApiException.createNotFound("MATERIAL_NOT_FOUND"));
            if (module.version() != position.version()) throw createVersionConflict();
            rows.add(module);
        }
        Map<UUID, Integer> positions = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) positions.put(rows.get(i).id(), i);
        rows.sort(Comparator.comparing(MaterialModule::id));
        for (var module : rows) {
            var next = module.reviseModule(module.title(), positions.get(module.id()), clock.instant());
            if (
                    !materialModuleRepository.updateModule(
                            next, module.version())) throw createVersionConflict();
        }
    }

    /** Gắn Quiz vào module (Quiz đang ở module khác thì chuyển sang). */
    public void attachQuiz(Actor actor, UUID moduleId, UUID quizId) {
        var module =
                materialModuleRepository.findModule(moduleId)
                        .orElseThrow(() -> ApiException.createNotFound("MATERIAL_NOT_FOUND"));
        var quiz = requireManageableQuiz(actor, module, quizId);
        var current = materialModuleRepository.findQuizLink(quizId);
        if (current.isPresent()) {
            if (current.get().moduleId().equals(moduleId)) return;
            materialModuleRepository.unlinkModuleQuiz(current.get().moduleId(), quizId);
        }
        int order =
                (int)
                        materialModuleRepository.listQuizLinks(module.subjectId()).stream()
                                .filter(l -> l.moduleId().equals(moduleId))
                                .count();
        materialModuleRepository.linkQuiz(
                new MaterialModule.QuizLink(moduleId, quiz.quizId(), order));
        recordAudit(actor, "MATERIAL_MODULE_QUIZ_ATTACHED", module);
    }

    public void detachQuiz(Actor actor, UUID moduleId, UUID quizId) {
        var module =
                materialModuleRepository.findModule(moduleId)
                        .orElseThrow(() -> ApiException.createNotFound("MATERIAL_NOT_FOUND"));
        requireManageableQuiz(actor, module, quizId);
        if (!materialModuleRepository.unlinkModuleQuiz(moduleId, quizId)) throw ApiException
                .createNotFound("MATERIAL_NOT_FOUND");
        recordAudit(actor, "MATERIAL_MODULE_QUIZ_DETACHED", module);
    }

    private QuizView requireManageableQuiz(Actor actor, MaterialModule module, UUID quizId) {
        var quiz =
                quizQuery.loadQuiz(quizId)
                        .orElseThrow(() -> ApiException.createNotFound("MATERIAL_QUIZ_NOT_FOUND"));
        if (!quiz.subjectId().equals(module.subjectId()))
            throw ApiException.createBadRequest(
                    "MATERIAL_QUIZ_INVALID", "Quiz không thuộc môn của module");
        if (!materialAccessPolicy.canManageQuiz(
                actor, quiz.subjectId(), quiz.scope() == QuizScope.CLASS ? quiz.classId() : null)) {
            audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(),
                    "ACCESS_DENIED", "MATERIAL_MODULE", module.id().toString(), AuditResult.DENIED,
                    "QUIZ_MANAGE_DENIED", Map.of("quizId", quizId.toString()), clock.instant()));
            throw ApiException.createNotFound("MATERIAL_QUIZ_NOT_FOUND");
        }
        return quiz;
    }

    private List<MaterialViews.Module> buildModuleViews(
            Actor actor, UUID subjectId, boolean editable, Predicate<QuizView> visible) {
        Map<UUID, List<MaterialModule.QuizLink>> links =
                materialModuleRepository.listQuizLinks(subjectId).stream()
                        .collect(
                                Collectors.groupingBy(
                                        MaterialModule.QuizLink::moduleId,
                                        LinkedHashMap::new,
                                        Collectors.toList()));
        Map<String, Boolean> manageable = new HashMap<>();
        return materialModuleRepository.listModules(subjectId).stream()
                .map(
                        module -> {
                            var items =
                                    links.getOrDefault(module.id(), List.of()).stream()
                                            .map(link -> quizQuery.loadQuiz(link.quizId()).orElse(null))
                                            .filter(Objects::nonNull)
                                            .filter(visible)
                                            .map(
                                                    quiz ->
                                                            buildModuleQuiz(
                                                                    actor,
                                                                    subjectId,
                                                                    quiz,
                                                                    manageable))
                                            .toList();
                            return new MaterialViews.Module(
                                    module.id(),
                                    module.title(),
                                    module.orderNo(),
                                    module.version(),
                                    editable,
                                    items);
                        })
                .toList();
    }

    /** {@code manageable} lưu kết quả kiểm quyền theo lớp để không gọi lại trong cùng một lượt đọc. */
    private MaterialViews.ModuleQuiz buildModuleQuiz(
            Actor actor, UUID subjectId, QuizView quiz, Map<String, Boolean> manageable) {
        UUID classId = quiz.scope() == QuizScope.CLASS ? quiz.classId() : null;
        boolean manage =
                manageable.computeIfAbsent(
                        String.valueOf(classId),
                        key -> materialAccessPolicy.canManageQuiz(actor, subjectId, classId));
        return new MaterialViews.ModuleQuiz(
                quiz.quizId(), quiz.title(), quiz.status(), quiz.scope(), quiz.classId(), manage);
    }

    private MaterialModule requireWritableModule(Actor actor, UUID id, long expected) {
        var module =
                materialModuleRepository
                        .findModule(id)
                        .orElseThrow(() -> ApiException.createNotFound("MATERIAL_NOT_FOUND"));
        materialAccessPolicy.requireModuleManagement(actor, module.subjectId());
        if (module.version() != expected) throw createVersionConflict();
        return module;
    }

    private ApiException createVersionConflict() {
        return ApiException.createConflict(
                "MATERIAL_VERSION_CONFLICT", "Module đã thay đổi, vui lòng tải lại");
    }

    private void recordAudit(Actor actor, String action, MaterialModule module) {
        audit.record(
                new AuditEvent(
                        UUID.randomUUID(),
                        actor.accountId(),
                        action,
                        "MATERIAL_MODULE",
                        module.id().toString(),
                        AuditResult.SUCCESS,
                        null,
                        Map.of("subjectId", module.subjectId().toString()),
                        clock.instant()));
    }
}
