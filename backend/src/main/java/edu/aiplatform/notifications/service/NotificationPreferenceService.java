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
    public static Set<String> typesFor(Role role) {
        return switch(role) {
            case STUDENT -> TYPES;
            case TEACHER -> Set.of("LEADER_CHANGE_REQUESTED","QUESTION_REVIEWED");
            case SUBJECT_MANAGER -> Set.of("LEADER_CHANGE_REQUESTED","QUESTION_REVIEWED","QUESTION_PROPOSED");
            case ADMIN -> Set.of("BACKUP_FAILED");
        };
    }
    public static boolean defaultEnabled(String type) {return TYPES.contains(type)||"BACKUP_FAILED".equals(type);}
    private final INotificationRepository repository;private final NotificationAccess access;
    public NotificationPreferenceService(INotificationRepository repository,NotificationAccess access) {this.repository=repository;this.access=access;}
    public Map<String,Boolean> get(Actor actor) {
        UUID id=access.self(actor);var stored=repository.preferences(id);Map<String,Boolean> values=new TreeMap<>();
        typesFor(actor.role()).forEach(t->values.put(t,"BACKUP_FAILED".equals(t)||stored.getOrDefault(t,defaultEnabled(t))));return values;
    }
    public Map<String,Boolean> put(Actor actor,Map<String,Boolean> values) {
        UUID id=access.self(actor);
        if(values==null||values.isEmpty()||values.keySet().stream().anyMatch(Objects::isNull)||!typesFor(actor.role()).containsAll(values.keySet())||values.values().stream().anyMatch(Objects::isNull)||Boolean.FALSE.equals(values.get("BACKUP_FAILED")))throw ApiException.badRequest("NOTIFICATION_PREFERENCE_INVALID","Loại email không hợp lệ");
        values.forEach((type,enabled)->repository.preference(id,type,enabled));return get(actor);
    }
}
