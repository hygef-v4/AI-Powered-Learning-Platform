package edu.aiplatform.notifications.application;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.notifications.domain.INotificationRepository;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(timeout=10)
public class NotificationPreferenceService {
    public static final Set<String> TYPES=Set.of("ENROLLED","ASSIGNMENT_OPENED","GRADE_PUBLISHED","DEADLINE_REMINDER","ANNOUNCEMENT_IMPORTANT");
    private final INotificationRepository repository;private final NotificationAccess access;
    public NotificationPreferenceService(INotificationRepository repository,NotificationAccess access) {this.repository=repository;this.access=access;}
    public Map<String,Boolean> get(Actor actor) {
        UUID id=student(actor);var stored=repository.preferences(id);Map<String,Boolean> values=new TreeMap<>();TYPES.forEach(t->values.put(t,stored.getOrDefault(t,true)));return values;
    }
    public Map<String,Boolean> put(Actor actor,Map<String,Boolean> values) {
        UUID id=student(actor);
        if(values==null||values.isEmpty()||values.keySet().stream().anyMatch(Objects::isNull)||!TYPES.containsAll(values.keySet())||values.values().stream().anyMatch(Objects::isNull))throw ApiException.badRequest("NOTIFICATION_PREFERENCE_INVALID","Loại email không hợp lệ");
        values.forEach((type,enabled)->repository.preference(id,type,enabled));return get(actor);
    }
    private UUID student(Actor actor) {UUID id=access.self(actor);if(actor.role()!=Role.STUDENT)throw access.denied(actor,null);return id;}
}
