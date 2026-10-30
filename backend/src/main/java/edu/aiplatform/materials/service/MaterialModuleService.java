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
@Transactional(timeout=10)
public class MaterialModuleService {
    private final IMaterialModuleRepository modules;
    private final MaterialAccessPolicy access;
    private final IQuizQuery quizzes;
    private final IAudit audit;
    private final Clock clock;
    public MaterialModuleService(IMaterialModuleRepository modules,MaterialAccessPolicy access,IQuizQuery quizzes,IAudit audit,Clock clock) {
        this.modules=modules;this.access=access;this.quizzes=quizzes;this.audit=audit;this.clock=clock;
    }

    @Transactional(readOnly=true,timeout=10,noRollbackFor=ApiException.class)
    public List<MaterialViews.Module> forSubject(Actor actor,UUID subjectId) {
        access.subjectList(actor,subjectId);
        boolean editable=access.canManageModules(actor,subjectId);
        // Trang môn chỉ hiện Quiz của môn; Quiz của lớp chỉ giảng viên của lớp đó mở được.
        return views(actor,subjectId,editable,q->q.scope()==QuizScope.SUBJECT);
    }

    @Transactional(readOnly=true,timeout=10,noRollbackFor=ApiException.class)
    public List<MaterialViews.Module> forClass(Actor actor,UUID classId,boolean student) {
        var c=access.classList(actor,classId,student);
        Predicate<QuizView> visible;
        if(student) {
            Set<UUID> open=quizzes.listForClass(classId).stream().map(QuizView::quizId).collect(Collectors.toSet());
            visible=q->open.contains(q.quizId());
        } else visible=q->q.scope()==QuizScope.SUBJECT
                ? q.status()==QuizStatus.OPEN||(q.status()==QuizStatus.RETIRED&&q.publishedAt()!=null)
                : classId.equals(q.classId());
        return views(actor,c.subjectId(),false,visible);
    }

    public MaterialViews.Module create(Actor actor,UUID subjectId,MaterialCommands.ModuleInput input) {
        access.manageModules(actor,subjectId);
        var now=clock.instant();
        var module=new MaterialModule(UUID.randomUUID(),subjectId,input.title(),modules.list(subjectId).size(),actor.accountId(),now,now,0);
        modules.insert(module);record(actor,"MATERIAL_MODULE_CREATED",module);
        return new MaterialViews.Module(module.id(),module.title(),module.orderNo(),module.version(),true,List.of());
    }

    public void rename(Actor actor,UUID id,MaterialCommands.ModuleUpdate input) {
        var module=writable(actor,id,input.version());
        var next=module.revise(input.title(),module.orderNo(),clock.instant());
        if(!modules.update(next,module.version()))throw conflict();
        record(actor,"MATERIAL_MODULE_UPDATED",next);
    }

    /** Chỉ xóa module không còn học liệu đang dùng; học liệu đã lưu trữ dời sang module đầu tiên còn lại của môn. */
    public void delete(Actor actor,UUID id,long expected) {
        var module=writable(actor,id,expected);
        if(modules.activeMaterials(id)>0)
            throw ApiException.conflict("MATERIAL_MODULE_NOT_EMPTY","Module còn học liệu, chuyển hoặc lưu trữ học liệu trước khi xóa");
        if(modules.archivedMaterials(id)>0) {
            var other=modules.list(module.subjectId()).stream().filter(m->!m.id().equals(id)).findFirst()
                    .orElseThrow(()->ApiException.conflict("MATERIAL_MODULE_LAST","Môn cần ít nhất một module để giữ học liệu đã lưu trữ"));
            modules.moveArchivedMaterials(id,other.id());
        }
        if(!modules.delete(id,expected))throw conflict();
        record(actor,"MATERIAL_MODULE_DELETED",module);
    }

    public void reorder(Actor actor,UUID subjectId,MaterialCommands.Reorder input) {
        access.manageModules(actor,subjectId);
        Set<UUID> seen=new HashSet<>();List<MaterialModule> rows=new ArrayList<>();
        for(var position:input.items()) {
            if(!seen.add(position.id()))throw ApiException.badRequest("MATERIAL_REORDER_INVALID","Danh sách có module trùng");
            var module=modules.find(position.id()).filter(m->m.subjectId().equals(subjectId))
                    .orElseThrow(()->ApiException.notFound("MATERIAL_NOT_FOUND"));
            if(module.version()!=position.version())throw conflict();
            rows.add(module);
        }
        Map<UUID,Integer> positions=new HashMap<>();for(int i=0;i<rows.size();i++)positions.put(rows.get(i).id(),i);
        rows.sort(Comparator.comparing(MaterialModule::id));
        for(var module:rows) {
            var next=module.revise(module.title(),positions.get(module.id()),clock.instant());
            if(!modules.update(next,module.version()))throw conflict();
        }
    }

    /** Gắn Quiz vào module (Quiz đang ở module khác thì chuyển sang). */
    public void attach(Actor actor,UUID moduleId,UUID quizId) {
        var module=modules.find(moduleId).orElseThrow(()->ApiException.notFound("MATERIAL_NOT_FOUND"));
        var quiz=manageableQuiz(actor,module,quizId);
        var current=modules.quizLink(quizId);
        if(current.isPresent()) {
            if(current.get().moduleId().equals(moduleId))return;
            modules.unlink(current.get().moduleId(),quizId);
        }
        int order=(int)modules.quizLinks(module.subjectId()).stream().filter(l->l.moduleId().equals(moduleId)).count();
        modules.link(new MaterialModule.QuizLink(moduleId,quiz.quizId(),order));
        record(actor,"MATERIAL_MODULE_QUIZ_ATTACHED",module);
    }

    public void detach(Actor actor,UUID moduleId,UUID quizId) {
        var module=modules.find(moduleId).orElseThrow(()->ApiException.notFound("MATERIAL_NOT_FOUND"));
        manageableQuiz(actor,module,quizId);
        if(!modules.unlink(moduleId,quizId))throw ApiException.notFound("MATERIAL_NOT_FOUND");
        record(actor,"MATERIAL_MODULE_QUIZ_DETACHED",module);
    }

    private QuizView manageableQuiz(Actor actor,MaterialModule module,UUID quizId) {
        var quiz=quizzes.get(quizId).orElseThrow(()->ApiException.notFound("MATERIAL_QUIZ_NOT_FOUND"));
        if(!quiz.subjectId().equals(module.subjectId()))
            throw ApiException.badRequest("MATERIAL_QUIZ_INVALID","Quiz không thuộc môn của module");
        if(!access.canManageQuiz(actor,quiz.subjectId(),quiz.scope()==QuizScope.CLASS?quiz.classId():null))
            throw ApiException.notFound("MATERIAL_QUIZ_NOT_FOUND");
        return quiz;
    }

    private List<MaterialViews.Module> views(Actor actor,UUID subjectId,boolean editable,Predicate<QuizView> visible) {
        Map<UUID,List<MaterialModule.QuizLink>> links=modules.quizLinks(subjectId).stream()
                .collect(Collectors.groupingBy(MaterialModule.QuizLink::moduleId,LinkedHashMap::new,Collectors.toList()));
        Map<String,Boolean> manageable=new HashMap<>();
        return modules.list(subjectId).stream().map(module->{
            var items=links.getOrDefault(module.id(),List.of()).stream()
                    .map(link->quizzes.get(link.quizId()).orElse(null))
                    .filter(Objects::nonNull).filter(visible)
                    .map(q->{
                        UUID classId=q.scope()==QuizScope.CLASS?q.classId():null;
                        boolean manage=manageable.computeIfAbsent(String.valueOf(classId),k->access.canManageQuiz(actor,subjectId,classId));
                        return new MaterialViews.ModuleQuiz(q.quizId(),q.title(),q.status(),q.scope(),q.classId(),manage);
                    }).toList();
            return new MaterialViews.Module(module.id(),module.title(),module.orderNo(),module.version(),editable,items);
        }).toList();
    }

    private MaterialModule writable(Actor actor,UUID id,long expected) {
        var module=modules.find(id).orElseThrow(()->ApiException.notFound("MATERIAL_NOT_FOUND"));
        access.manageModules(actor,module.subjectId());
        if(module.version()!=expected)throw conflict();
        return module;
    }
    private ApiException conflict() { return ApiException.conflict("MATERIAL_VERSION_CONFLICT","Module đã thay đổi, vui lòng tải lại"); }
    private void record(Actor actor,String action,MaterialModule module) {
        audit.record(new AuditEvent(UUID.randomUUID(),actor.accountId(),action,"MATERIAL_MODULE",module.id().toString(),
                AuditResult.SUCCESS,null,Map.of("subjectId",module.subjectId().toString()),clock.instant()));
    }
}
