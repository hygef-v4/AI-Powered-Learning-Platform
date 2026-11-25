package edu.aiplatform.announcements.service;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementAccessPolicy {
    private final IAuthorization authorization;
    private final IClassAccess classAccess;

    public AnnouncementAccessPolicy(
            IAuthorization authorization, IClassAccess classAccess) {
        this.authorization = authorization;
        this.classAccess = classAccess;
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
        return ApiException.createNotFound("ANNOUNCEMENT_NOT_FOUND");
    }
}
