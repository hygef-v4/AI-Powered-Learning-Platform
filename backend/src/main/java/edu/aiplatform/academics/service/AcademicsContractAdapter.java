package edu.aiplatform.academics.service;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.academics.model.*;
import edu.aiplatform.academics.repository.*;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.subjects.contract.ISubjectClasses;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectClassSummary;
import edu.aiplatform.subjects.contract.SubjectRef;
import java.time.Clock;
import java.util.*;
import java.util.function.IntFunction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Supplies real, uncached U05 relationships to earlier units without depending on their internals. */
@Service
@Transactional(readOnly = true)
public class AcademicsContractAdapter implements IClassAccess, IClassScope, ISubjectClasses {
    private final ICourseClassRepository courseClassRepository;
    private final IEnrollmentRepository enrollmentRepository;
    private final IAcademicsReadRepository academicsReadRepository;
    private final ISubjectQuery subjectQuery;
    private final IAccountLookup accountLookup;
    private final IAudit audit;
    private final Clock clock;

    public AcademicsContractAdapter(ICourseClassRepository courseClassRepository,
            IEnrollmentRepository enrollmentRepository,
            IAcademicsReadRepository academicsReadRepository, ISubjectQuery subjectQuery,
            IAccountLookup accountLookup, IAudit audit, Clock clock) {
        this.courseClassRepository = courseClassRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.academicsReadRepository = academicsReadRepository;
        this.subjectQuery = subjectQuery;
        this.accountLookup = accountLookup;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public Optional<ClassRef> getClassRef(UUID classId) {
        return courseClassRepository.find(classId).map(this::mapClassReference);
    }

    @Override
    public boolean isTeacherOf(UUID accountId, UUID classId) {
        return accountId != null && courseClassRepository.find(classId)
                .map(c -> accountId.equals(c.teacherId())).orElse(false);
    }

    @Override
    public boolean isEnrolledStudent(UUID accountId, UUID classId) {
        return accountId != null && enrollmentRepository.find(classId, accountId)
                .map(e -> e.status() == EnrollmentStatus.ACTIVE).orElse(false);
    }

    @Override
    public Optional<UUID> subjectOfClass(UUID classId) {
        return courseClassRepository.find(classId).map(CourseClass::subjectId);
    }

    @Override
    public List<UUID> listTaughtClasses(UUID accountId) {
        return academicsReadRepository.listTaught(accountId).stream().map(CourseClass::id).toList();
    }

    @Override
    public boolean isActiveStudent(UUID accountId, UUID classId) {
        return isEnrolledStudent(accountId, classId);
    }

    @Override
    public List<UUID> listActiveStudents(UUID classId) {
        return allPages(offset -> enrollmentRepository.listByClass(classId, true, 100, offset))
                .stream().map(Enrollment::accountId).toList();
    }

    @Override
    public boolean isClassManager(UUID accountId, UUID classId) {
        if (accountId == null)
            return false;
        return courseClassRepository.find(classId)
                .flatMap(c -> subjectQuery.getSubject(c.subjectId()))
                .map(s -> accountId.equals(s.managerId())).orElse(false);
    }

    @Override
    public List<ClassRef> listOpenClassesOf(UUID accountId) {
        var combined = new LinkedHashMap<UUID, ClassRef>();
        allPages(offset -> academicsReadRepository.listForTeacher(accountId, null, ClassStatus.OPEN,
                100, offset)).forEach(c -> combined.put(c.id(), mapClassReference(c)));
        allPages(offset -> academicsReadRepository.listForStudent(accountId, null, ClassStatus.OPEN,
                100, offset)).forEach(c -> combined.put(c.id(), mapClassReference(c)));
        return List.copyOf(combined.values());
    }

    @Override
    public List<ClassRef> listOpenClassesOfSubject(UUID subjectId) {
        return academicsReadRepository.listOfSubject(subjectId).stream()
                .filter(c -> c.status() == ClassStatus.OPEN).map(this::mapClassReference).toList();
    }

    @Override
    public Map<ClassStatus, Long> countClassesByStatus() {
        return academicsReadRepository.countClassesByStatus();
    }

    @Override
    public long countActiveEnrollments() {
        return academicsReadRepository.countActiveEnrollments();
    }

    @Override
    public List<SubjectClassSummary> listClassesOfSubject(UUID subjectId) {
        var rows = academicsReadRepository.listOfSubject(subjectId);
        var counts = academicsReadRepository
                .enrollmentCounts(rows.stream().map(CourseClass::id).toList());
        var names = new HashMap<UUID, String>();
        return rows.stream().map(c -> {
            String teacher = c.teacherId() == null ? null
                    : names.computeIfAbsent(c.teacherId(), id -> accountLookup.getContact(id)
                            .map(a -> a.displayName()).orElse(""));
            long active = counts
                    .getOrDefault(c.id(), new IAcademicsReadRepository.EnrollmentCounts(0, 0))
                    .active();
            return new SubjectClassSummary(c.id(), c.code(), c.name(), c.term(), c.status().name(),
                    teacher, Math.toIntExact(active));
        }).toList();
    }

    @Override
    public Map<UUID, Integer> countClasses(Collection<UUID> subjectIds) {
        return academicsReadRepository.countClassesOfSubjects(subjectIds);
    }

    @Override
    public int countUnarchivedClasses(UUID subjectId) {
        return academicsReadRepository.countUnarchivedClasses(subjectId);
    }

    /** U04 đã khóa dòng môn trong cùng transaction, nên không thao tác ghi lớp nào của U05 chạy chồng (BR-U04-04). */
    @Override
    @Transactional
    public List<UUID> archiveClassesOfSubject(UUID subjectId, Actor actor) {
        Objects.requireNonNull(subjectId);
        Objects.requireNonNull(actor);
        var now = clock.instant();
        var archived = new ArrayList<UUID>();
        for (CourseClass current : academicsReadRepository.listOfSubject(subjectId)) {
            if (current.status() == ClassStatus.ARCHIVED)
                continue;
            CourseClass changed = current.changeStatus(ClassStatus.ARCHIVED, true, false, now);
            courseClassRepository.update(changed, current.version());
            audit.record(
                    new AuditEvent(UUID.randomUUID(), actor.accountId(), "CLASS_STATUS_CHANGED",
                            "COURSE_CLASS", current.id().toString(), AuditResult.SUCCESS, null,
                            Map.of("before", current.status().name(), "after",
                                    ClassStatus.ARCHIVED.name(), "cause", "SUBJECT_ARCHIVED"),
                            now));
            archived.add(current.id());
        }
        return List.copyOf(archived);
    }

    private ClassRef mapClassReference(CourseClass c) {
        String subjectCode = subjectQuery.getSubject(c.subjectId()).map(SubjectRef::code)
                .orElse(null);
        return new ClassRef(c.id(), c.subjectId(), c.code(), c.name(), c.status(), c.teacherId(),
                c.showGradeDistribution(), subjectCode);
    }

    private static <T> List<T> allPages(IntFunction<List<T>> fetch) {
        var all = new ArrayList<T>();
        for (int offset = 0;; offset = Math.addExact(offset, 100)) {
            var page = fetch.apply(offset);
            all.addAll(page);
            if (page.size() < 100)
                return List.copyOf(all);
        }
    }
}
