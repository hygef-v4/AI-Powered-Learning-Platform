package edu.aiplatform.notifications.application;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.notifications.domain.NotificationType;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class RecipientResolver {
    public record Resolved(NotificationType type,List<UUID> recipients,String link,String emailType) { }
    private final IClassAccess classes;private final IAccountLookup accounts;
    public RecipientResolver(IClassAccess classes,IAccountLookup accounts) {this.classes=classes;this.accounts=accounts;}
    public List<Resolved> resolve(DomainEvent event) {
        if (EventTypes.ASSIGNMENT_OPENED.equals(event.eventType())) return assignmentGroups(event.payload());
        var p=event.payload();UUID classId=optionalId(p,"classId");List<UUID> ids;NotificationType type;String link;String email=null;
        switch(event.eventType()) {
            case EventTypes.ENROLLMENT_ACTIVATED -> {type=NotificationType.ENROLLED;ids=List.of(id(p,"accountId"));link="/classes/"+id(p,"classId");email="ENROLLED";}
            case EventTypes.QUIZ_PUBLISHED -> {type=NotificationType.QUIZ_PUBLISHED;ids=students(p);link="/quiz-practice/"+id(p,"quizId");}
            case EventTypes.GRADE_PUBLISHED -> {type=NotificationType.GRADE_PUBLISHED;ids=ids(p,"studentIds");link=gradeLink(p);email="GRADE_PUBLISHED";}
            case EventTypes.CLASS_ANNOUNCEMENT_POSTED -> {
                type=NotificationType.ANNOUNCEMENT;ids=activeStudents(id(p,"classId"));UUID author=id(p,"actorId");ids=ids.stream().filter(i->!i.equals(author)).toList();
                id(p,"announcementId");link="/announcements?classId="+classId;
                if(Boolean.TRUE.equals(p.get("important"))&&Boolean.TRUE.equals(p.get("sendEmail")))email="ANNOUNCEMENT_IMPORTANT";
            }
            case EventTypes.GROUP_LEADER_CHANGE_REQUESTED -> {type=NotificationType.LEADER_CHANGE_REQUESTED;var c=openClass(id(p,"classId"));ids=c==null||c.teacherId()==null?List.of():List.of(c.teacherId());link=teacherGroupLink(p);}
            case EventTypes.GROUP_LEADER_CHANGED -> {type=NotificationType.LEADER_CHANGED;ids=members(p);link=groupLink(p);}
            case EventTypes.GROUP_LEADER_REQUEST_REJECTED -> {type=NotificationType.LEADER_REQUEST_REJECTED;ids=members(p);link=groupLink(p);}
            case EventTypes.GROUP_MEMBERSHIP_CHANGED -> {type=NotificationType.GROUP_MEMBERSHIP_CHANGED;ids=members(p);link=groupLink(p);}
            case EventTypes.GROUP_SUBMITTED -> {type=NotificationType.GROUP_SUBMITTED;ids=members(p);link=groupLink(p);}
            case EventTypes.BACKUP_FAILED -> {type=NotificationType.BACKUP_FAILED;ids=admins();id(p,"runId");link="/admin";email="BACKUP_FAILED";}
            default -> throw new IllegalArgumentException("Unsupported notification event");
        }
        boolean teacher=type==NotificationType.LEADER_CHANGE_REQUESTED;
        boolean admin=type==NotificationType.BACKUP_FAILED;
        List<UUID> recipients=ids.stream().distinct().filter(account->{
            var c=accounts.getContact(account).orElse(null);if(c==null||c.status()!=AccountStatus.ACTIVE)return false;
            if(admin)return c.role()==Role.ADMIN;
            if(teacher)return c.role()==Role.TEACHER||c.role()==Role.SUBJECT_MANAGER;
            return c.role()==Role.STUDENT&&(classId==null||openClass(classId)!=null&&classes.isActiveStudent(account,classId));
        }).toList();
        return List.of(new Resolved(type,recipients,link,email));
    }
    private List<Resolved> assignmentGroups(Map<String,Object> payload) {
        UUID assignmentId = id(payload,"assignmentId");
        List<ClassRef> targets;
        if ("SUBJECT".equals(payload.get("scope"))) {
            UUID subjectId = id(payload,"subjectId");
            targets = classes.listOpenClassesOfSubject(subjectId).stream()
                    .filter(c -> subjectId.equals(c.subjectId())).toList();
        } else if ("CLASS".equals(payload.get("scope"))) {
            targets = classes.getClassRef(id(payload,"classId")).map(List::of).orElse(List.of());
        } else throw new IllegalArgumentException("Invalid assignment scope");
        Set<UUID> seen = new HashSet<>();
        List<Resolved> groups = new ArrayList<>();
        // Stable choice if inconsistent data lists one student in multiple classes of the subject.
        for (var target : targets.stream().filter(c -> c.status()==ClassStatus.OPEN)
                .sorted(Comparator.comparing(ClassRef::classId)).toList()) {
            if (openClass(target.classId()) == null) continue;
            List<UUID> recipients = classes.listActiveStudents(target.classId()).stream().distinct()
                    .filter(student -> {
                        var account = accounts.getContact(student).orElse(null);
                        return account != null && account.status()==AccountStatus.ACTIVE && account.role()==Role.STUDENT
                                && classes.isActiveStudent(student,target.classId());
                    }).filter(seen::add).toList();
            if (!recipients.isEmpty()) groups.add(new Resolved(NotificationType.ASSIGNMENT_OPENED, recipients,
                    AssignmentNotificationLink.student(assignmentId,target.classId()),"ASSIGNMENT_OPENED"));
        }
        return List.copyOf(groups);
    }
    private List<UUID> admins() {
        List<UUID> all=new ArrayList<>();UUID cursor=null;Set<UUID> seen=new HashSet<>();
        while(true){var page=accounts.listActiveAdmins(cursor,100);if(page.isEmpty())break;
            for(var a:page)if(!seen.add(a.accountId()))throw new IllegalStateException("Invalid Admin pagination");else if(a.role()==Role.ADMIN&&a.status()==AccountStatus.ACTIVE)all.add(a.accountId());
            cursor=page.get(page.size()-1).accountId();if(page.size()<100)break;
        }return all;
    }
    private List<UUID> students(Map<String,Object> p) {
        if("SUBJECT".equals(p.get("scope")))return classes.listOpenClassesOfSubject(id(p,"subjectId")).stream().filter(c->c.status()==ClassStatus.OPEN).flatMap(c->classes.listActiveStudents(c.classId()).stream()).distinct().toList();
        return activeStudents(id(p,"classId"));
    }
    private List<UUID> activeStudents(UUID classId) {return openClass(classId)==null?List.of():classes.listActiveStudents(classId);}
    private ClassRef openClass(UUID id) {return classes.getClassRef(id).filter(c->c.status()==ClassStatus.OPEN).orElse(null);}
    /** Sinh viên xem điểm ở chi tiết bài; sự kiện cũ thiếu mã bài thì mở lớp. */
    private String gradeLink(Map<String,Object> p) {UUID assignmentId=optionalId(p,"assignmentId");UUID classId=id(p,"classId");return assignmentId==null?"/classes/"+classId:AssignmentNotificationLink.student(assignmentId,classId);}
    /** Trang Nhóm của tôi của lớp. */
    private String groupLink(Map<String,Object> p) {id(p,"groupId");return "/classes/"+id(p,"classId")+"/group";}
    /** Giảng viên duyệt yêu cầu ở bảng nhóm trong trang Sinh viên của lớp mình dạy. */
    private String teacherGroupLink(Map<String,Object> p) {id(p,"groupId");return "/teaching/classes/"+id(p,"classId")+"?tab=students";}
    private List<UUID> members(Map<String,Object> p) {return ids(p,p.containsKey("memberIds")?"memberIds":"recipientIds");}
    public static UUID id(Map<String,Object> p,String key) {UUID id=optionalId(p,key);if(id==null)throw new IllegalArgumentException("Missing event identifier");return id;}
    private static UUID optionalId(Map<String,Object> p,String key) {Object value=p.get(key);return value==null?null:UUID.fromString(value.toString());}
    private List<UUID> ids(Map<String,Object> p,String key) {if(!(p.get(key) instanceof Collection<?> values)||values.size()>10000)throw new IllegalArgumentException("Invalid recipient list");return values.stream().map(v->UUID.fromString(Objects.toString(v))).toList();}
}
