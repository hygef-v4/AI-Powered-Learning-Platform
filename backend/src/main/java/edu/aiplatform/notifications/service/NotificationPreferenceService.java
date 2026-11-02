package edu.aiplatform.notifications.service;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.notifications.repository.INotificationRepository;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(timeout=10)
public class NotificationPreferenceService {
    public static final Set<String> TYPES=Set.of("ENROLLED","ASSIGNMENT_OPENED","GRADE_PUBLISHED","DEADLINE_REMINDER","ANNOUNCEMENT_IMPORTANT");
    /** Loại email mỗi vai trò tắt, bật được (yêu cầu 2026-11-02: mọi vai trò đều có cài đặt thông báo). */
    public static final Map<Role,Set<String>> TYPES_BY_ROLE=Map.of(
            Role.STUDENT,TYPES,
            Role.TEACHER,Set.of("LEADER_CHANGE_REQUESTED","QUESTION_REVIEWED"),
            Role.SUBJECT_MANAGER,Set.of("LEADER_CHANGE_REQUESTED","QUESTION_PROPOSED","QUESTION_REVIEWED"),
            Role.ADMIN,Set.of("BACKUP_FAILED"));
    private final INotificationRepository repository;private final NotificationAccess access;
    public NotificationPreferenceService(INotificationRepository repository,NotificationAccess access) {this.repository=repository;this.access=access;}
    public Map<String,Boolean> get(Actor actor) {
        UUID id=access.self(actor);var stored=repository.preferences(id);Map<String,Boolean> values=new TreeMap<>();types(actor).forEach(t->values.put(t,stored.getOrDefault(t,true)));return values;
    }
    public Map<String,Boolean> put(Actor actor,Map<String,Boolean> values) {
        UUID id=access.self(actor);
        if(values==null||values.isEmpty()||values.keySet().stream().anyMatch(Objects::isNull)||!types(actor).containsAll(values.keySet())||values.values().stream().anyMatch(Objects::isNull))throw ApiException.badRequest("NOTIFICATION_PREFERENCE_INVALID","Loại email không hợp lệ");
        values.forEach((type,enabled)->repository.preference(id,type,enabled));return get(actor);
    }
    private Set<String> types(Actor actor) {return TYPES_BY_ROLE.getOrDefault(actor.role(),Set.of());}
}
