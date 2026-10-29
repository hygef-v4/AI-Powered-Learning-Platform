package edu.aiplatform.announcements.application;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementAccessPolicy {
    private final IAuthorization authorization;
    private final IClassAccess classes;
    private final IAudit audit;
    private final Clock clock;
    public AnnouncementAccessPolicy(IAuthorization authorization,IClassAccess classes,IAudit audit,Clock clock) {
        this.authorization=authorization;this.classes=classes;this.audit=audit;this.clock=clock;
    }
    public ClassRef require(Actor actor,UUID id,boolean write) {
        if(actor==null||actor.role()==Role.ADMIN)throw denied(actor,id);
        var decision=authorization.authorize(actor,write?Actions.ANNOUNCEMENT_MANAGE:Actions.ANNOUNCEMENT_READ,ResourceRef.courseClass(id));
        if(!decision.allowed())throw denied(actor,id);
        var c=classes.getClassRef(id).orElseThrow(()->denied(actor,id));
        if(c.status()!=ClassStatus.OPEN||write&&(!Objects.equals(c.teacherId(),actor.accountId())||actor.role()==Role.STUDENT))throw denied(actor,id);
        if(!write&&(actor.role()==Role.STUDENT?!classes.isActiveStudent(actor.accountId(),id):!Objects.equals(c.teacherId(),actor.accountId())))throw denied(actor,id);
        return c;
    }
    public List<ClassRef> list(Actor actor,UUID classId) {
        if(actor==null||actor.role()==Role.ADMIN)throw denied(actor,classId);
        if(!authorization.authorize(actor,Actions.NOTIFICATION_SELF,ResourceRef.account(actor.accountId())).allowed())throw denied(actor,classId);
        if(classId!=null)return List.of(require(actor,classId,false));
        return classes.listOpenClassesOf(actor.accountId()).stream().filter(c->c.status()==ClassStatus.OPEN)
                .filter(c->actor.role()==Role.STUDENT?classes.isActiveStudent(actor.accountId(),c.classId()):Objects.equals(c.teacherId(),actor.accountId()))
                .filter(c->authorization.authorize(actor,Actions.ANNOUNCEMENT_READ,ResourceRef.courseClass(c.classId())).allowed()).toList();
    }
    public ApiException denied(Actor actor,UUID id) {
        audit.record(new AuditEvent(UUID.randomUUID(),actor==null?null:actor.accountId(),"ACCESS_DENIED","ANNOUNCEMENT",id==null?null:id.toString(),AuditResult.DENIED,"OUT_OF_SCOPE",Map.of(),clock.instant()));
        return ApiException.notFound("ANNOUNCEMENT_NOT_FOUND");
    }
}
