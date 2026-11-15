package edu.aiplatform.notifications.service;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.notifications.model.NotificationType;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class RecipientResolver {
    public record Resolved(
            NotificationType type, List<UUID> recipients, String link, String emailType) {}

    private final IClassAccess classAccess;
    private final IAccountLookup accountLookup;

    public RecipientResolver(IClassAccess classAccess, IAccountLookup accountLookup) {
        this.classAccess = classAccess;
        this.accountLookup = accountLookup;
    }

    public List<Resolved> resolveRecipients(DomainEvent event) {
        if (EventTypes.ASSIGNMENT_OPENED.equals(event.eventType()))
            return resolveAssignmentGroups(event.payload());
        var payload = event.payload();
        // Nhóm đến từ đề xuất được duyệt: thành viên đã nhận thông báo duyệt, không báo trùng (BR-U12-95).
        if (EventTypes.GROUP_MEMBERSHIP_CHANGED.equals(event.eventType())
                && "PROPOSAL_APPROVED".equals(payload.get("reason"))) return List.of();
        UUID classId = readOptionalId(payload, "classId");
        List<UUID> ids;
        NotificationType type;
        String link;
        String email = null;
        switch (event.eventType()) {
            case EventTypes.ENROLLMENT_ACTIVATED -> {
                type = NotificationType.ENROLLED;
                ids = List.of(readId(payload, "accountId"));
                link = "/classes/" + readId(payload, "classId");
                email = "ENROLLED";
            }
            case EventTypes.QUIZ_PUBLISHED -> {
                type = NotificationType.QUIZ_PUBLISHED;
                ids = listStudents(payload);
                link = "/quiz-practice/" + readId(payload, "quizId");
            }
            case EventTypes.GRADE_PUBLISHED -> {
                type = NotificationType.GRADE_PUBLISHED;
                ids = readIds(payload, "studentIds");
                link = buildGradeLink(payload);
                email = "GRADE_PUBLISHED";
            }
            case EventTypes.CLASS_ANNOUNCEMENT_POSTED -> {
                type = NotificationType.ANNOUNCEMENT;
                ids = listActiveStudents(readId(payload, "classId"));
                UUID author = readId(payload, "actorId");
                ids = ids.stream().filter(i -> !i.equals(author)).toList();
                readId(payload, "announcementId");
                link = "/classes/" + classId + "/announcements";
                if (Boolean.TRUE.equals(payload.get("important"))
                        && Boolean.TRUE.equals(payload.get("sendEmail")))
                    email = "ANNOUNCEMENT_IMPORTANT";
            }
            case EventTypes.GROUP_LEADER_CHANGE_REQUESTED -> {
                type = NotificationType.LEADER_CHANGE_REQUESTED;
                var courseClass = findOpenClass(readId(payload, "classId"));
                ids =
                        courseClass == null || courseClass.teacherId() == null
                                ? List.of()
                                : List.of(courseClass.teacherId());
                link = buildTeacherGroupLink(payload);
                email = "LEADER_CHANGE_REQUESTED";
            }
            case EventTypes.GROUP_LEADER_CHANGED -> {
                type = NotificationType.LEADER_CHANGED;
                ids = listMembers(payload);
                link = buildGroupLink(payload);
            }
            case EventTypes.GROUP_LEADER_REQUEST_REJECTED -> {
                type = NotificationType.LEADER_REQUEST_REJECTED;
                ids = listMembers(payload);
                link = buildGroupLink(payload);
            }
            case EventTypes.GROUP_MEMBERSHIP_CHANGED -> {
                type = NotificationType.GROUP_MEMBERSHIP_CHANGED;
                ids = listMembers(payload);
                link = buildGroupLink(payload);
            }
            case EventTypes.GROUP_SUBMITTED -> {
                type = NotificationType.GROUP_SUBMITTED;
                ids = listMembers(payload);
                link = buildGroupLink(payload);
            }
            case EventTypes.GROUP_PROPOSAL_INVITED -> {
                type = NotificationType.GROUP_PROPOSAL_INVITED;
                ids = readIds(payload, "recipientIds");
                link = buildProposalLink(payload);
            }
            case EventTypes.GROUP_PROPOSAL_SUBMITTED -> {
                type = NotificationType.GROUP_PROPOSAL_SUBMITTED;
                var courseClass = findOpenClass(readId(payload, "classId"));
                ids =
                        courseClass == null || courseClass.teacherId() == null
                                ? List.of()
                                : List.of(courseClass.teacherId());
                readId(payload, "proposalId");
                link = "/teaching/classes/" + readId(payload, "classId") + "?tab=students";
            }
            case EventTypes.GROUP_PROPOSAL_APPROVED -> {
                type = NotificationType.GROUP_PROPOSAL_APPROVED;
                ids = readIds(payload, "recipientIds");
                link = buildProposalLink(payload);
            }
            case EventTypes.GROUP_PROPOSAL_REJECTED -> {
                type = NotificationType.GROUP_PROPOSAL_REJECTED;
                ids = readIds(payload, "recipientIds");
                link = buildProposalLink(payload);
            }
            case EventTypes.QUESTION_PROPOSED -> {
                type = NotificationType.QUESTION_PROPOSED;
                ids = readIds(payload, "recipientIds");
                link =
                        "/manager/questions?subjectId="
                                + readId(payload, "subjectId")
                                + "&status=PENDING";
                email = "QUESTION_PROPOSED";
            }
            case EventTypes.QUESTION_REVIEWED -> {
                type = NotificationType.QUESTION_REVIEWED;
                ids = readIds(payload, "recipientIds");
                link =
                        classId == null
                                ? "/teaching"
                                : "/teaching/classes/" + classId + "?tab=materials";
                email = "QUESTION_REVIEWED";
            }
            case EventTypes.BACKUP_FAILED -> {
                type = NotificationType.BACKUP_FAILED;
                ids = listAdmins();
                readId(payload, "runId");
                link = "/admin";
                email = "BACKUP_FAILED";
            }
            default -> throw new IllegalArgumentException("Unsupported notification event");
        }
        boolean teacher =
                type == NotificationType.LEADER_CHANGE_REQUESTED
                        || type == NotificationType.GROUP_PROPOSAL_SUBMITTED
                        || type == NotificationType.QUESTION_PROPOSED
                        || type == NotificationType.QUESTION_REVIEWED;
        boolean admin = type == NotificationType.BACKUP_FAILED;
        List<UUID> recipients =
                ids.stream()
                        .distinct()
                        .filter(
                                account -> {
                                    var contact = accountLookup.getContact(account).orElse(null);
                                    if (contact == null || contact.status() != AccountStatus.ACTIVE)
                                        return false;
                                    if (admin) return contact.role() == Role.ADMIN;
                                    if (teacher)
                                        return contact.role() == Role.TEACHER
                                                || contact.role() == Role.SUBJECT_MANAGER;
                                    return contact.role() == Role.STUDENT
                                            && (classId == null
                                                    || findOpenClass(classId) != null
                                                            && classAccess.isActiveStudent(
                                                                    account, classId));
                                })
                        .toList();
        return List.of(new Resolved(type, recipients, link, email));
    }

    private List<Resolved> resolveAssignmentGroups(Map<String, Object> payload) {
        UUID assignmentId = readId(payload, "assignmentId");
        List<ClassRef> targets;
        if ("SUBJECT".equals(payload.get("scope"))) {
            UUID subjectId = readId(payload, "subjectId");
            targets =
                    classAccess.listOpenClassesOfSubject(subjectId).stream()
                            .filter(c -> subjectId.equals(c.subjectId()))
                            .toList();
        } else if ("CLASS".equals(payload.get("scope"))) {
            targets =
                    classAccess
                            .getClassRef(readId(payload, "classId"))
                            .map(List::of)
                            .orElse(List.of());
        } else throw new IllegalArgumentException("Invalid assignment scope");
        Set<UUID> seen = new HashSet<>();
        List<Resolved> groups = new ArrayList<>();
        // Stable choice if inconsistent data lists one student in multiple classes of the subject.
        for (var target :
                targets.stream()
                        .filter(c -> c.status() == ClassStatus.OPEN)
                        .sorted(Comparator.comparing(ClassRef::classId))
                        .toList()) {
            if (findOpenClass(target.classId()) == null) continue;
            List<UUID> recipients =
                    classAccess.listActiveStudents(target.classId()).stream()
                            .distinct()
                            .filter(
                                    student -> {
                                        var account =
                                                accountLookup.getContact(student).orElse(null);
                                        return account != null
                                                && account.status() == AccountStatus.ACTIVE
                                                && account.role() == Role.STUDENT
                                                && classAccess.isActiveStudent(
                                                        student, target.classId());
                                    })
                            .filter(seen::add)
                            .toList();
            if (!recipients.isEmpty())
                groups.add(
                        new Resolved(
                                NotificationType.ASSIGNMENT_OPENED,
                                recipients,
                                AssignmentNotificationLink.buildStudentLink(
                                        assignmentId, target.classId()),
                                "ASSIGNMENT_OPENED"));
        }
        return List.copyOf(groups);
    }

    private List<UUID> listAdmins() {
        List<UUID> all = new ArrayList<>();
        UUID cursor = null;
        Set<UUID> seen = new HashSet<>();
        while (true) {
            var page = accountLookup.listActiveAdmins(cursor, 100);
            if (page.isEmpty()) break;
            for (var account : page)
                if (!seen.add(account.accountId()))
                    throw new IllegalStateException("Invalid Admin pagination");
                else if (account.role() == Role.ADMIN && account.status() == AccountStatus.ACTIVE)
                    all.add(account.accountId());
            cursor = page.get(page.size() - 1).accountId();
            if (page.size() < 100) break;
        }
        return all;
    }

    private List<UUID> listStudents(Map<String, Object> payload) {
        if ("SUBJECT".equals(payload.get("scope")))
            return classAccess.listOpenClassesOfSubject(readId(payload, "subjectId")).stream()
                    .filter(c -> c.status() == ClassStatus.OPEN)
                    .flatMap(c -> classAccess.listActiveStudents(c.classId()).stream())
                    .distinct()
                    .toList();
        return listActiveStudents(readId(payload, "classId"));
    }

    private List<UUID> listActiveStudents(UUID classId) {
        return findOpenClass(classId) == null ? List.of() : classAccess.listActiveStudents(classId);
    }

    private ClassRef findOpenClass(UUID id) {
        return classAccess.getClassRef(id).filter(c -> c.status() == ClassStatus.OPEN).orElse(null);
    }

    /** Sinh viên xem điểm ở chi tiết bài; sự kiện cũ thiếu mã bài thì mở lớp. */
    private String buildGradeLink(Map<String, Object> payload) {
        UUID assignmentId = readOptionalId(payload, "assignmentId");
        UUID classId = readId(payload, "classId");
        return assignmentId == null
                ? "/classes/" + classId
                : AssignmentNotificationLink.buildStudentLink(assignmentId, classId);
    }

    /** Trang Nhóm của tôi của lớp. */
    private String buildGroupLink(Map<String, Object> payload) {
        readId(payload, "groupId");
        return "/classes/" + readId(payload, "classId") + "/group";
    }

    /** Giảng viên duyệt yêu cầu ở trang Sinh viên của lớp mình dạy, mở sẵn đúng nhóm gửi yêu cầu. */
    private String buildProposalLink(Map<String, Object> payload) {
        readId(payload, "proposalId");
        return "/classes/" + readId(payload, "classId") + "/group";
    }

    private String buildTeacherGroupLink(Map<String, Object> payload) {
        return "/teaching/classes/"
                + readId(payload, "classId")
                + "?tab=students&group="
                + readId(payload, "groupId");
    }

    private List<UUID> listMembers(Map<String, Object> payload) {
        return readIds(payload, payload.containsKey("memberIds") ? "memberIds" : "recipientIds");
    }

    public static UUID readId(Map<String, Object> payload, String key) {
        UUID id = readOptionalId(payload, key);
        if (id == null) throw new IllegalArgumentException("Missing event identifier");
        return id;
    }

    private static UUID readOptionalId(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : UUID.fromString(value.toString());
    }

    private List<UUID> readIds(Map<String, Object> payload, String key) {
        if (!(payload.get(key) instanceof Collection<?> values) || values.size() > 10000)
            throw new IllegalArgumentException("Invalid recipient list");
        return values.stream().map(v -> UUID.fromString(Objects.toString(v))).toList();
    }
}
