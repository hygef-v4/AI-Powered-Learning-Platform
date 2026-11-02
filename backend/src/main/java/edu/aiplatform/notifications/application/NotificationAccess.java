package edu.aiplatform.notifications.application;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
@Service
public class NotificationAccess {
    private final IAuthorization authorization;private final IAudit audit;private final Clock clock;
    public NotificationAccess(IAuthorization authorization,IAudit audit,Clock clock) {this.authorization=authorization;this.audit=audit;this.clock=clock;}
    public UUID self(Actor actor) {
        if(actor==null||actor.accountId()==null||!authorization.authorize(actor,Actions.NOTIFICATION_SELF,ResourceRef.account(actor.accountId())).allowed())throw denied(actor,null);
        return actor.accountId();
    }
    public ApiException denied(Actor actor,UUID id) {
        audit.record(new AuditEvent(UUID.randomUUID(),actor==null?null:actor.accountId(),"ACCESS_DENIED","NOTIFICATION",id==null?null:id.toString(),AuditResult.DENIED,"NOT_OWNER",Map.of(),clock.instant()));
        return ApiException.notFound("NOTIFICATION_NOT_FOUND");
    }
}
