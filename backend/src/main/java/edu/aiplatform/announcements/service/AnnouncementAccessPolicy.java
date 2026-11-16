package edu.aiplatform.announcements.service;

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
    private final IClassAccess classAccess;
    private final IAudit audit;
    private final Clock clock;

    public AnnouncementAccessPolicy(
            IAuthorization authorization, IClassAccess classAccess, IAudit audit, Clock clock) {
        this.authorization = authorization;
        this.classAccess = classAccess;
        this.audit = audit;
        this.clock = clock;
    }

    public ClassRef requireClassAccess(Actor actor, UUID id, boolean write) {
        if (actor == null || actor.role() == Role.ADMIN) throw rejectAccess(actor, id);
        var decision =
                authorization.authorize(
                        actor,
                        write ? Actions.ANNOUNCEMENT_MANAGE : Actions.ANNOUNCEMENT_READ,
                        ResourceRef.courseClass(id));
        if (!decision.allowed()) throw rejectAccess(actor, id);
        var courseClass = classAccess.getClassRef(id).orElseThrow(() -> rejectAccess(actor, id));
        if (courseClass.status() != ClassStatus.OPEN
                || write
                        && (!Objects.equals(courseClass.teacherId(), actor.accountId())
                                || actor.role() == Role.STUDENT)) throw rejectAccess(actor, id);
        if (!write
                && (actor.role() == Role.STUDENT
                        ? !classAccess.isActiveStudent(actor.accountId(), id)
                        : !Objects.equals(courseClass.teacherId(), actor.accountId())))
            throw rejectAccess(actor, id);
        return courseClass;
    }

    public List<ClassRef> listReadableClasses(Actor actor, UUID classId) {
        if (actor == null || actor.role() == Role.ADMIN) throw rejectAccess(actor, classId);
        if (!authorization
                .authorize(actor, Actions.NOTIFICATION_SELF, ResourceRef.account(actor.accountId()))
                .allowed()) throw rejectAccess(actor, classId);
        if (classId != null) return List.of(requireClassAccess(actor, classId, false));
        return classAccess.listOpenClassesOf(actor.accountId()).stream()
                .filter(c -> c.status() == ClassStatus.OPEN)
                .filter(
                        c ->
                                actor.role() == Role.STUDENT
                                        ? classAccess.isActiveStudent(
                                                actor.accountId(), c.classId())
                                        : Objects.equals(c.teacherId(), actor.accountId()))
                .filter(
                        c ->
                                authorization
                                        .authorize(
                                                actor,
                                                Actions.ANNOUNCEMENT_READ,
                                                ResourceRef.courseClass(c.classId()))
                                        .allowed())
                .toList();
    }

    public ApiException rejectAccess(Actor actor, UUID id) {
        audit.record(
                new AuditEvent(
                        UUID.randomUUID(),
                        actor == null ? null : actor.accountId(),
                        "ACCESS_DENIED",
                        "ANNOUNCEMENT",
                        id == null ? null : id.toString(),
                        AuditResult.DENIED,
                        "OUT_OF_SCOPE",
                        Map.of(),
                        clock.instant()));
        return ApiException.createNotFound("ANNOUNCEMENT_NOT_FOUND");
    }
}
