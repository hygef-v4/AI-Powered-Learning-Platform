package edu.aiplatform.academics.infrastructure;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.academics.domain.*;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.subjects.contract.ISubjectClasses;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectClassSummary;
import java.util.*;
import java.util.function.IntFunction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Supplies real, uncached U05 relationships to earlier units without depending on their internals. */
@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "platform.academics.persistence-enabled", havingValue = "true")
public class AcademicsContractAdapter implements IClassAccess,IClassScope,ISubjectClasses {
    private final ICourseClassRepository classes;
    private final IEnrollmentRepository enrollments;
    private final IAcademicsReadRepository queries;
    private final ISubjectQuery subjects;
    private final IAccountLookup accounts;
    public AcademicsContractAdapter(ICourseClassRepository classes,IEnrollmentRepository enrollments,IAcademicsReadRepository queries,ISubjectQuery subjects,IAccountLookup accounts) {
        this.classes=classes;this.enrollments=enrollments;this.queries=queries;this.subjects=subjects;this.accounts=accounts;
    }
    @Override public Optional<ClassRef> getClassRef(UUID classId) { return classes.find(classId).map(AcademicsContractAdapter::reference); }
    @Override public boolean isTeacherOf(UUID accountId,UUID classId) {
        return accountId!=null && classes.find(classId).map(c->accountId.equals(c.teacherId())).orElse(false);
    }
    @Override public boolean isEnrolledStudent(UUID accountId,UUID classId) {
        return accountId!=null && enrollments.find(classId,accountId).map(e->e.status()==EnrollmentStatus.ACTIVE).orElse(false);
    }
    @Override public Optional<UUID> subjectOfClass(UUID classId) { return classes.find(classId).map(CourseClass::subjectId); }
    @Override public List<UUID> listTaughtClasses(UUID accountId) { return queries.listTaught(accountId).stream().map(CourseClass::id).toList(); }
    @Override public boolean isActiveStudent(UUID accountId,UUID classId) { return isEnrolledStudent(accountId,classId); }
    @Override public List<UUID> listActiveStudents(UUID classId) {
        return allPages(offset->enrollments.listByClass(classId,true,100,offset)).stream().map(Enrollment::accountId).toList();
    }
    @Override public boolean isClassManager(UUID accountId,UUID classId) {
        if (accountId==null) return false;
        return classes.find(classId).flatMap(c->subjects.getSubject(c.subjectId())).map(s->accountId.equals(s.managerId())).orElse(false);
    }
    @Override public List<ClassRef> listOpenClassesOf(UUID accountId) {
        var combined=new LinkedHashMap<UUID,ClassRef>();
        allPages(offset->queries.listForTeacher(accountId,null,ClassStatus.OPEN,100,offset)).forEach(c->combined.put(c.id(),reference(c)));
        allPages(offset->queries.listForStudent(accountId,null,ClassStatus.OPEN,100,offset)).forEach(c->combined.put(c.id(),reference(c)));
        return List.copyOf(combined.values());
    }
    @Override public List<ClassRef> listOpenClassesOfSubject(UUID subjectId) {
        return queries.listOfSubject(subjectId).stream().filter(c->c.status()==ClassStatus.OPEN).map(AcademicsContractAdapter::reference).toList();
    }
    @Override public Map<ClassStatus,Long> countClassesByStatus() { return queries.countClassesByStatus(); }
    @Override public long countActiveEnrollments() { return queries.countActiveEnrollments(); }
    @Override public List<SubjectClassSummary> listClassesOfSubject(UUID subjectId) {
        var rows=queries.listOfSubject(subjectId);
        var counts=queries.enrollmentCounts(rows.stream().map(CourseClass::id).toList());
        var names=new HashMap<UUID,String>();
        return rows.stream().map(c->{
            String teacher=c.teacherId()==null?null:names.computeIfAbsent(c.teacherId(),id->accounts.getContact(id).map(a->a.displayName()).orElse(""));
            long active=counts.getOrDefault(c.id(),new IAcademicsReadRepository.EnrollmentCounts(0,0)).active();
            return new SubjectClassSummary(c.id(),c.code(),c.name(),c.term(),c.status().name(),teacher,Math.toIntExact(active));
        }).toList();
    }
    @Override public Map<UUID,Integer> countClasses(Collection<UUID> subjectIds) { return queries.countClassesOfSubjects(subjectIds); }
    @Override public int countUnarchivedClasses(UUID subjectId) { return queries.countUnarchivedClasses(subjectId); }
    private static ClassRef reference(CourseClass c) { return new ClassRef(c.id(),c.subjectId(),c.code(),c.name(),c.status(),c.teacherId(),c.showGradeDistribution()); }
    private static <T> List<T> allPages(IntFunction<List<T>> fetch) {
        var all=new ArrayList<T>();
        for (int offset=0;;offset=Math.addExact(offset,100)) {
            var page=fetch.apply(offset); all.addAll(page);
            if (page.size()<100) return List.copyOf(all);
        }
    }
}
