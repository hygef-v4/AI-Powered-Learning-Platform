package edu.aiplatform.materials.application;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.domain.Material;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.*;
import java.time.Clock;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class MaterialAccessPolicy {
    private final IAuthorization authorization;
    private final ISubjectQuery subjects;
    private final IClassAccess classes;
    private final IClassScope classScope;
    private final IAudit audit;
    private final Clock clock;
    public MaterialAccessPolicy(IAuthorization authorization, ISubjectQuery subjects, IClassAccess classes,
                                IClassScope classScope, IAudit audit, Clock clock) {
        this.authorization=authorization;this.subjects=subjects;this.classes=classes;
        this.classScope=classScope;this.audit=audit;this.clock=clock;
    }
    private <T> T dependency(Supplier<T> operation) {
        try { return operation.get(); }
        catch(ApiException e) { throw e; }
        catch(RuntimeException e) { throw ApiException.unavailable("MATERIAL_DEPENDENCY_UNAVAILABLE","Không kiểm được quyền học liệu, vui lòng thử lại"); }
    }
    private boolean allowed(Actor actor,String action,ResourceRef ref) {
        if(actor==null||actor.accountId()==null||actor.role()==null)return false;
        var decision=dependency(()->authorization.authorize(actor,action,ref));
        if(decision==null||"DEPENDENCY_UNAVAILABLE".equals(decision.reason()))
            throw ApiException.unavailable("MATERIAL_AUTH_UNAVAILABLE","Không kiểm được quyền học liệu, vui lòng thử lại");
        return decision.allowed();
    }
    public SubjectRef subject(UUID id) {
        return dependency(()->subjects.getSubject(id)).orElseThrow(()->ApiException.notFound("MATERIAL_NOT_FOUND"));
    }
    public ClassRef courseClass(UUID id) {
        return dependency(()->classes.getClassRef(id)).orElseThrow(()->ApiException.notFound("MATERIAL_NOT_FOUND"));
    }
    private boolean managesSubject(Actor actor,UUID id) {
        return actor!=null && actor.role()==Role.SUBJECT_MANAGER
                && allowed(actor,Actions.MATERIAL_SUBJECT_MANAGE,ResourceRef.subject(id));
    }
    public void subjectList(Actor actor,UUID subjectId) {
        if(!managesSubject(actor,subjectId))deny(actor,subjectId);
        subject(subjectId);
    }
    public ClassRef classList(Actor actor,UUID classId,boolean student) {
        ClassRef c=courseClass(classId);
        boolean ok=student ? studentCanRead(actor,c) : staffCanRead(actor,c);
        if(!ok)deny(actor,classId);
        return c;
    }
    private boolean staffCanRead(Actor actor,ClassRef c) {
        return actor!=null && (actor.role()==Role.TEACHER||actor.role()==Role.SUBJECT_MANAGER)
                && actor.accountId().equals(c.teacherId())
                && allowed(actor,Actions.MATERIAL_STAFF_READ,ResourceRef.courseClass(c.classId()));
    }
    private boolean studentCanRead(Actor actor,ClassRef c) {
        return actor!=null && actor.role()==Role.STUDENT && c.status()==ClassStatus.OPEN
                && allowed(actor,Actions.MATERIAL_STUDENT_READ,ResourceRef.courseClass(c.classId()))
                && dependency(()->classes.isActiveStudent(actor.accountId(),c.classId()));
    }
    public boolean canManage(Actor actor,Material m) {
        if(m.scope()==Material.Scope.SUBJECT)return managesSubject(actor,m.subjectId());
        ClassRef c=courseClass(m.classId());
        return Objects.equals(c.subjectId(),m.subjectId()) && staffCanRead(actor,c);
    }
    public void write(Actor actor,Material m) {
        if(!canManage(actor,m))deny(actor,m.id());
        if(subject(m.subjectId()).status()!=SubjectStatus.ACTIVE
                ||(m.classId()!=null && courseClass(m.classId()).status()==ClassStatus.ARCHIVED))
            throw ApiException.conflict("MATERIAL_SCOPE_READ_ONLY","Môn hoặc lớp đã lưu trữ, học liệu chỉ đọc");
        if(m.status()!=Material.Status.ACTIVE)
            throw ApiException.conflict("MATERIAL_ARCHIVED","Học liệu đã được lưu trữ");
    }
    public void read(Actor actor,Material m,UUID classId) {
        if(actor==null||actor.role()==null||actor.role()==Role.ADMIN){deny(actor,m.id());return;}
        if(actor.role()==Role.STUDENT) {
            if(m.status()!=Material.Status.ACTIVE){deny(actor,m.id());return;}
            List<ClassRef> candidates=classId==null ? dependency(()->classes.listOpenClassesOf(actor.accountId()))
                    : List.of(courseClass(classId));
            for(ClassRef c:candidates) if(matches(m,c)&&studentCanRead(actor,c))return;
        } else {
            if(m.scope()==Material.Scope.SUBJECT && managesSubject(actor,m.subjectId()))return;
            if(m.scope()==Material.Scope.CLASS) {
                ClassRef c=courseClass(m.classId());
                if((classId==null||classId.equals(c.classId()))&&matches(m,c)&&staffCanRead(actor,c))return;
            } else if(classId!=null) {
                ClassRef c=courseClass(classId);if(matches(m,c)&&staffCanRead(actor,c))return;
            } else {
                for(UUID id:dependency(()->classScope.listTaughtClasses(actor.accountId()))) {
                    ClassRef c=courseClass(id);if(matches(m,c)&&staffCanRead(actor,c))return;
                }
            }
        }
        deny(actor,m.id());
    }
    private boolean matches(Material m,ClassRef c) {
        return m.subjectId().equals(c.subjectId()) && (m.scope()==Material.Scope.SUBJECT||m.classId().equals(c.classId()));
    }
    private void deny(Actor actor,UUID id) {
        audit.record(new AuditEvent(UUID.randomUUID(),actor==null?null:actor.accountId(),"ACCESS_DENIED","MATERIAL",
                id.toString(),AuditResult.DENIED,"OUT_OF_SCOPE",Map.of(),clock.instant()));
        throw ApiException.notFound("MATERIAL_NOT_FOUND");
    }
}
