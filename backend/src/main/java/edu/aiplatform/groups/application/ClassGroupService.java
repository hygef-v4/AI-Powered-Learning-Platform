package edu.aiplatform.groups.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groupdocs.application.GroupDocumentService;
import edu.aiplatform.groupdocs.application.IGroupDocStreams;
import edu.aiplatform.groupdocs.domain.GroupDocument;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.groupdocs.domain.GroupSection;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.groups.domain.GroupSetRules;
import edu.aiplatform.groups.domain.GroupSetRules.Draft;
import edu.aiplatform.groups.domain.GroupSetRules.Issue;
import edu.aiplatform.groups.domain.LeaderChangeRequest;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC 57 phần nhóm (F1, F2, F14): đọc bộ nhóm, lưu cả bộ kèm {@code version}, chia ngẫu nhiên. Kiểm toàn bộ trước
 * rồi mới ghi trong một transaction; người rời hoặc chuyển nhóm được nhả mục đang giữ (BR-U12-05).
 */
@Service
public class ClassGroupService {

    private final GroupRepository groups;
    private final GroupDocumentRepository documents;
    private final GroupDocumentService groupDocuments;
    private final IGroupDocStreams streams;
    private final GroupAccess access;
    private final IClassAccess classes;
    private final IAccountLookup accounts;
    private final IAudit audit;
    private final IEventPublisher events;
    private final Clock clock;
    private final RandomGenerator random;

    @Autowired
    public ClassGroupService(GroupRepository groups, GroupDocumentRepository documents, GroupDocumentService groupDocuments,
                             IGroupDocStreams streams, GroupAccess access, IClassAccess classes, IAccountLookup accounts,
                             IAudit audit, IEventPublisher events, Clock clock) {
        this(groups, documents, groupDocuments, streams, access, classes, accounts, audit, events, clock, new SecureRandom());
    }

    ClassGroupService(GroupRepository groups, GroupDocumentRepository documents, GroupDocumentService groupDocuments,
                      IGroupDocStreams streams, GroupAccess access, IClassAccess classes, IAccountLookup accounts,
                      IAudit audit, IEventPublisher events, Clock clock, RandomGenerator random) {
        this.groups = groups;
        this.documents = documents;
        this.groupDocuments = groupDocuments;
        this.streams = streams;
        this.access = access;
        this.classes = classes;
        this.accounts = accounts;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
        this.random = random;
    }

    // ---- đọc -----------------------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public GroupSetView view(Actor actor, UUID classId) {
        ClassRef ref = access.requireTeacher(actor, classId);
        Set<UUID> active = new LinkedHashSet<>(activeStudents(classId));
        List<ClassGroup> current = groups.listGroups(classId);
        People people = new People();
        Set<UUID> grouped = new HashSet<>();
        List<GroupView> views = new ArrayList<>();
        for (ClassGroup g : current) {
            grouped.addAll(g.memberIds());
            List<MemberView> members = g.memberIds().stream()
                    .map(id -> people.member(id, active.contains(id), id.equals(g.leaderId()))).toList();
            boolean leaderActive = g.leaderId() != null && active.contains(g.leaderId());
            views.add(new GroupView(g.id(), g.name(), leaderActive ? g.leaderId() : null, members,
                    documents.groupHasContent(g.id())));
        }
        List<PersonView> ungrouped = active.stream().filter(id -> !grouped.contains(id)).map(people::person).toList();
        List<RequestView> pending = groups.listPendingOfClass(classId).stream()
                .map(r -> requestView(r, current, people)).toList();
        return new GroupSetView(classId, ref.status().name(), groups.setVersion(classId), views, ungrouped, pending);
    }

    // ---- chia ngẫu nhiên -------------------------------------------------------------------------------------------

    /** Bản xem trước: nhóm hiện có giữ nguyên, thêm nhóm mới cho sinh viên chưa có nhóm (F2). */
    @Transactional(readOnly = true)
    public List<DraftView> randomPreview(Actor actor, UUID classId, Integer maxSize) {
        ClassRef ref = access.requireTeacher(actor, classId);
        GroupAccess.requireEditable(ref);
        if (maxSize == null || maxSize < RandomGroupSplitter.MIN_SIZE || maxSize > RandomGroupSplitter.MAX_SIZE) {
            throw ApiException.badRequest("GROUP_SIZE_INVALID", "Sĩ số tối đa từ 2 đến 20");
        }
        Set<UUID> active = new LinkedHashSet<>(activeStudents(classId));
        List<ClassGroup> current = groups.listGroups(classId);
        Set<String> names = new HashSet<>();
        List<DraftView> preview = new ArrayList<>();
        for (ClassGroup g : current) {
            names.add(g.name().toLowerCase());
            List<UUID> members = g.memberIds().stream().filter(active::contains).toList();
            UUID leader = members.contains(g.leaderId()) ? g.leaderId() : null;
            preview.add(new DraftView(g.id(), g.name(), leader, members, false));
            active.removeAll(g.memberIds());
        }
        int next = 1;
        for (RandomGroupSplitter.Split split : RandomGroupSplitter.split(List.copyOf(active), maxSize, random)) {
            String name;
            do {
                name = "Nhóm " + next++;
            } while (names.contains(name.toLowerCase()));
            names.add(name.toLowerCase());
            preview.add(new DraftView(null, name, split.leaderId(), split.members(), true));
        }
        return preview;
    }

    // ---- lưu cả bộ --------------------------------------------------------------------------------------------------

    @Transactional
    public GroupSetView save(Actor actor, UUID classId, SaveCommand command) {
        ClassRef ref = access.requireTeacher(actor, classId);
        GroupAccess.requireEditable(ref);
        if (command == null || command.version() == null || command.groups() == null) {
            throw ApiException.badRequest("GROUP_SET_REQUIRED", "Cần gửi cả bộ nhóm kèm version");
        }
        long version = groups.setVersion(classId);
        if (version != command.version()) {
            throw versionConflict();
        }
        Set<UUID> active = new HashSet<>(activeStudents(classId));
        List<ClassGroup> current = groups.listGroups(classId);
        Map<UUID, ClassGroup> byId = new LinkedHashMap<>();
        Set<UUID> formerMembers = new HashSet<>();
        current.forEach(g -> {
            byId.put(g.id(), g);
            formerMembers.addAll(g.memberIds());
        });

        // Người đã rời lớp (BR-U12-07) được bỏ khỏi nhóm trong lần lưu này thay vì báo lỗi.
        List<Draft> drafts = new ArrayList<>();
        List<Issue> issues = new ArrayList<>();
        int index = 0;
        for (Draft d : command.groups()) {
            String key = d.id() != null ? d.id().toString() : (d.key() != null ? d.key() : "new-" + index);
            index++;
            List<UUID> members = (d.memberIds() == null ? List.<UUID>of() : d.memberIds()).stream()
                    .filter(Objects::nonNull).distinct()
                    .filter(id -> active.contains(id) || !formerMembers.contains(id)).toList();
            if (d.id() != null && !byId.containsKey(d.id())) {
                issues.add(new Issue(key, "GROUP_NOT_IN_CLASS", "Nhóm không thuộc lớp này"));
            }
            drafts.add(new Draft(key, d.id(), d.name() == null ? null : d.name().trim(), d.leaderId(), members));
        }
        issues.addAll(GroupSetRules.validate(drafts, active));
        Set<UUID> kept = new HashSet<>();
        drafts.stream().map(Draft::id).filter(Objects::nonNull).forEach(kept::add);
        List<ClassGroup> deleted = current.stream().filter(g -> !kept.contains(g.id())).toList();
        for (ClassGroup g : deleted) {
            if (documents.groupHasContent(g.id())) {
                issues.add(new Issue(g.id().toString(), "GROUP_IN_USE",
                        "Nhóm " + g.name() + " đã có tài liệu hoặc bản nộp, chỉ chuyển thành viên được"));
            }
        }
        if (!issues.isEmpty()) {
            throw new GroupSetInvalidException(issues);
        }
        Instant now = clock.instant();
        if (!groups.bumpSetVersion(classId, version, now)) {
            throw versionConflict();
        }
        apply(actor, classId, current, byId, drafts, deleted, now);
        return view(actor, classId);
    }

    private void apply(Actor actor, UUID classId, List<ClassGroup> current, Map<UUID, ClassGroup> byId, List<Draft> drafts,
                       List<ClassGroup> deleted, Instant now) {
        Map<UUID, UUID> before = new HashMap<>();
        current.forEach(g -> g.memberIds().forEach(m -> before.put(m, g.id())));

        // Gán id cho nhóm mới trước để so sánh thành viên theo nhóm.
        Map<String, UUID> ids = new LinkedHashMap<>();
        drafts.forEach(d -> ids.put(d.key(), d.id() != null ? d.id() : UUID.randomUUID()));
        Map<UUID, UUID> after = new HashMap<>();
        drafts.forEach(d -> d.memberIds().forEach(m -> after.put(m, ids.get(d.key()))));

        Set<UUID> moved = new LinkedHashSet<>();
        before.forEach((member, group) -> {
            if (!group.equals(after.get(member))) {
                moved.add(member);
                releaseHeldSections(group, member, actor, now);
                // Đóng kênh SSE của người rời nhóm với tài liệu cũ (sau commit, F14 bước 1).
                streams.closeFor(member, documents.listByGroup(group).stream().map(GroupDocument::id).toList());
            }
        });
        after.forEach((member, group) -> {
            if (!group.equals(before.get(member))) {
                moved.add(member);
            }
        });

        groups.removeMembers(classId, moved);
        for (ClassGroup g : deleted) {
            groups.cancelPendingOf(g.id(), actor.accountId(), "Nhóm đã bị xóa", now);
            groups.deleteGroup(g.id());
            record(actor, "GROUP_DELETED", g.id(), Map.of("classId", classId.toString(), "name", g.name()));
        }
        // Đổi tên tạm các nhóm giữ lại để hai nhóm đổi tên cho nhau không vướng unique.
        drafts.stream().filter(d -> d.id() != null && !byId.get(d.id()).name().equals(d.name()))
                .forEach(d -> groups.renameTemporarily(d.id()));

        List<UUID> leaderChangedGroups = new ArrayList<>();
        List<UUID> createdGroups = new ArrayList<>();
        for (Draft d : drafts) {
            UUID id = ids.get(d.key());
            if (d.id() == null) {
                createdGroups.add(id);
                groups.insertGroup(new ClassGroup(id, classId, d.name(), d.leaderId(), actor.accountId(), now, now, List.of()));
                record(actor, "GROUP_CREATED", id, details(classId, d));
            } else {
                ClassGroup old = byId.get(d.id());
                boolean renamed = !old.name().equals(d.name());
                boolean leaderChanged = !d.leaderId().equals(old.leaderId());
                boolean membersChanged = !new HashSet<>(old.memberIds()).equals(new HashSet<>(d.memberIds()));
                if (renamed || leaderChanged) {
                    groups.updateGroup(id, d.name(), d.leaderId(), now);
                }
                if (leaderChanged) {
                    // Đổi trưởng nhóm trực tiếp hủy yêu cầu đang chờ của nhóm (UC 57).
                    groups.cancelPendingOf(id, actor.accountId(), "Giảng viên đã đổi trưởng nhóm", now);
                    leaderChangedGroups.add(id);
                    Map<String, Object> change = new LinkedHashMap<>();
                    change.put("classId", classId.toString());
                    change.put("before", old.leaderId() == null ? null : old.leaderId().toString());
                    change.put("after", d.leaderId().toString());
                    record(actor, "GROUP_LEADER_CHANGED", id, change);
                } else if (membersChanged) {
                    cancelOrphanedRequest(id, d.memberIds(), actor, now);
                }
                if (renamed || membersChanged) {
                    record(actor, "GROUP_UPDATED", id, details(classId, d));
                }
            }
        }
        for (Draft d : drafts) {
            UUID id = ids.get(d.key());
            for (UUID member : d.memberIds()) {
                if (moved.contains(member)) {
                    groups.addMember(id, classId, member, now);
                }
            }
        }
        // Nhóm mới khi lớp có bài nhóm đang mở: tạo tài liệu ngay (F7 bước 4, F14 bước 2).
        groupDocuments.ensureForGroups(classId, createdGroups);
        // Mỗi nhóm một sự kiện có groupId (U14 bắt buộc): người vào nhóm theo nhóm mới, người bị gỡ theo nhóm đã rời.
        Map<UUID, List<UUID>> movedByGroup = new LinkedHashMap<>();
        for (UUID member : moved) {
            UUID group = after.getOrDefault(member, before.get(member));
            movedByGroup.computeIfAbsent(group, k -> new ArrayList<>()).add(member);
        }
        movedByGroup.forEach((group, members) ->
                publish(EventTypes.GROUP_MEMBERSHIP_CHANGED, classId, group, List.copyOf(members)));
        for (UUID id : leaderChangedGroups) {
            drafts.stream().filter(d -> id.equals(d.id())).findFirst()
                    .ifPresent(d -> publish(EventTypes.GROUP_LEADER_CHANGED, classId, id, d.memberIds()));
        }
    }

    /** Yêu cầu chờ của người đã rời nhóm, hoặc đề cử người đã rời nhóm, tự hủy (BR-U12-07). */
    private void cancelOrphanedRequest(UUID groupId, List<UUID> members, Actor actor, Instant now) {
        Optional<LeaderChangeRequest> pending = groups.findPending(groupId);
        if (pending.isPresent() && (!members.contains(pending.get().requestedBy())
                || (pending.get().proposedLeaderId() != null && !members.contains(pending.get().proposedLeaderId())))) {
            groups.cancelPendingOf(groupId, actor.accountId(), "Người gửi hoặc người được đề cử đã rời nhóm", now);
        }
    }

    /** Người rời nhóm: nhả mục đang giữ, bỏ bản nháp chưa Xong; nội dung đã Xong giữ tác giả (BR-U12-05, F14). */
    private void releaseHeldSections(UUID groupId, UUID member, Actor actor, Instant now) {
        for (GroupSection section : documents.lockSectionsHeldBy(groupId, member)) {
            if (documents.updateSection(section.released())) {
                documents.touch(section.documentId(), now);
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("documentId", section.documentId().toString());
                details.put("holder", member.toString());
                details.put("cause", "MEMBER_REMOVED");
                record(actor, "GROUP_SECTION_RELEASED", section.id(), details);
            }
        }
    }

    // ---- tiện ích -----------------------------------------------------------------------------------------------

    private List<UUID> activeStudents(UUID classId) {
        try {
            return classes.listActiveStudents(classId);
        } catch (RuntimeException e) {
            throw ApiException.unavailable("GROUP_DEPENDENCY_UNAVAILABLE", "Hệ thống tạm bận, vui lòng thử lại sau");
        }
    }

    private static Map<String, Object> details(UUID classId, Draft d) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("classId", classId.toString());
        details.put("name", d.name());
        details.put("leaderId", d.leaderId().toString());
        details.put("members", d.memberIds().stream().map(UUID::toString).toList());
        return details;
    }

    private void record(Actor actor, String action, UUID objectId, Map<String, Object> details) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), action, "CLASS_GROUP", objectId.toString(),
                AuditResult.SUCCESS, null, details, clock.instant()));
    }

    private void publish(String type, UUID classId, UUID groupId, List<UUID> recipients) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("classId", classId.toString());
        if (groupId != null) {
            payload.put("groupId", groupId.toString());
        }
        payload.put("recipientIds", recipients.stream().map(UUID::toString).toList());
        events.publish(new DomainEvent(1, UUID.randomUUID(), type, "U12", clock.instant(), null, payload));
    }

    private static ApiException versionConflict() {
        return ApiException.conflict("GROUP_SET_VERSION_CONFLICT", "Bộ nhóm vừa được thay đổi, hãy tải lại");
    }

    private RequestView requestView(LeaderChangeRequest r, List<ClassGroup> current, People people) {
        String groupName = current.stream().filter(g -> g.id().equals(r.groupId())).map(ClassGroup::name).findFirst()
                .orElse("");
        return new RequestView(r.id(), r.groupId(), groupName, people.person(r.requestedBy()),
                r.proposedLeaderId() == null ? null : people.person(r.proposedLeaderId()), r.reason(), r.createdAt());
    }

    /** Tra tên, email một lần cho mỗi người trong một lần đọc. */
    private final class People {
        private final Map<UUID, Optional<AccountContact>> cache = new HashMap<>();

        PersonView person(UUID id) {
            Optional<AccountContact> c = cache.computeIfAbsent(id, key -> {
                try {
                    return accounts.getContact(key);
                } catch (RuntimeException e) {
                    return Optional.empty();
                }
            });
            return new PersonView(id, c.map(AccountContact::displayName).orElse(null),
                    c.map(AccountContact::email).orElse(null));
        }

        MemberView member(UUID id, boolean active, boolean leader) {
            PersonView p = person(id);
            return new MemberView(id, p.name(), p.email(), active, leader);
        }
    }

    // ---- kiểu dữ liệu ----------------------------------------------------------------------------------------

    public record SaveCommand(Long version, List<Draft> groups) {
    }

    public record GroupSetView(UUID classId, String classStatus, long version, List<GroupView> groups,
                               List<PersonView> ungrouped, List<RequestView> pendingRequests) {
    }

    /** {@code leaderId} rỗng khi trưởng nhóm đã rời lớp (nhóm thiếu trưởng nhóm, BR-U12-07). */
    public record GroupView(UUID id, String name, UUID leaderId, List<MemberView> members, boolean used) {
    }

    /** {@code active} sai là "Đã rời lớp" (BR-U12-07). */
    public record MemberView(UUID accountId, String name, String email, boolean active, boolean leader) {
    }

    public record PersonView(UUID accountId, String name, String email) {
    }

    public record RequestView(UUID id, UUID groupId, String groupName, PersonView requestedBy, PersonView proposedLeader,
                              String reason, Instant createdAt) {
    }

    /** Một nhóm trong bản xem trước; {@code id} rỗng là nhóm mới. */
    public record DraftView(UUID id, String name, UUID leaderId, List<UUID> memberIds, boolean generated) {
    }
}
