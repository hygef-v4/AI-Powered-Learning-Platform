package edu.aiplatform.academics.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.academics.domain.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.subjects.contract.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AcademicsContractAdapterTest {
    final ICourseClassRepository classes = mock(ICourseClassRepository.class);
    final IEnrollmentRepository enrollments = mock(IEnrollmentRepository.class);
    final IAcademicsReadRepository queries = mock(IAcademicsReadRepository.class);
    final ISubjectQuery subjects = mock(ISubjectQuery.class);
    final IAccountLookup accounts = mock(IAccountLookup.class);
    final AcademicsContractAdapter adapter = new AcademicsContractAdapter(classes, enrollments, queries, subjects, accounts);
    final UUID classId=UUID.randomUUID(), subjectId=UUID.randomUUID(), teacher=UUID.randomUUID(), student=UUID.randomUUID();
    final Instant now=Instant.parse("2026-10-18T00:00:00Z");
    CourseClass course(UUID teacherId, ClassStatus status) {
        return new CourseClass(classId, subjectId, "SE01", "Lop", null, "2026", status, teacherId, false, now, now, 0);
    }

    @Test void realCrossUnitContractsReplaceTheirStubs() {
        var runner=new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean(ICourseClassRepository.class,()->classes)
                .withBean(IEnrollmentRepository.class,()->enrollments)
                .withBean(IAcademicsReadRepository.class,()->queries)
                .withBean(ISubjectQuery.class,()->subjects)
                .withBean(IAccountLookup.class,()->accounts)
                .withUserConfiguration(AcademicsContractAdapter.class)
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        edu.aiplatform.academics.contract.stub.AcademicsContractStubs.class,
                        edu.aiplatform.identity.contract.stub.IdentityContractStubs.class,
                        edu.aiplatform.subjects.contract.stub.SubjectsContractStubs.class));
        runner.run(context->{
            assertThat(context).hasSingleBean(IClassAccess.class).hasSingleBean(IClassScope.class).hasSingleBean(ISubjectClasses.class);
            assertThat(context.getBean(IClassAccess.class)).isSameAs(context.getBean(AcademicsContractAdapter.class));
            assertThat(context.getBean(IClassScope.class)).isSameAs(context.getBean(AcademicsContractAdapter.class));
            assertThat(context.getBean(ISubjectClasses.class)).isSameAs(context.getBean(AcademicsContractAdapter.class));
        });
    }

    @Test void teacherScopeReadsCurrentAssignmentWithoutCachingAcrossCalls() {
        when(classes.find(classId)).thenReturn(Optional.of(course(teacher,ClassStatus.DRAFT)), Optional.of(course(UUID.randomUUID(),ClassStatus.DRAFT)));
        assertThat(adapter.isTeacherOf(teacher,classId)).isTrue();
        assertThat(adapter.isTeacherOf(teacher,classId)).isFalse();
        verify(classes,times(2)).find(classId);
    }

    @Test void removedStudentLosesScopeImmediatelyEvenWhenClassIsArchived() {
        var enrollment=Enrollment.create(classId,student,EnrollmentSource.MANUAL,now);
        when(enrollments.find(classId,student)).thenReturn(Optional.of(enrollment),Optional.of(enrollment.remove(now)));
        assertThat(adapter.isEnrolledStudent(student,classId)).isTrue();
        assertThat(adapter.isEnrolledStudent(student,classId)).isFalse();
    }

    @Test void unknownClassDoesNotGrantTeacherOrManagerScope() {
        when(classes.find(classId)).thenReturn(Optional.empty());
        assertThat(adapter.isTeacherOf(teacher,classId)).isFalse();
        assertThat(adapter.isClassManager(teacher,classId)).isFalse();
        assertThat(adapter.subjectOfClass(classId)).isEmpty();
    }

    @Test void managerComesFromCurrentSubjectOwnerNotTeacherAssignment() {
        when(classes.find(classId)).thenReturn(Optional.of(course(teacher,ClassStatus.OPEN)));
        UUID manager=UUID.randomUUID();
        when(subjects.getSubject(subjectId)).thenReturn(Optional.of(new SubjectRef(subjectId,"SWP","Project",SubjectStatus.ACTIVE,manager)));
        assertThat(adapter.isClassManager(manager,classId)).isTrue();
        assertThat(adapter.isClassManager(teacher,classId)).isFalse();
    }

    @Test void activeStudentContractDoesNotSilentlyTruncateAtOnePage() {
        var first=IntStream.range(0,100).mapToObj(i -> Enrollment.create(classId,UUID.randomUUID(),EnrollmentSource.MANUAL,now)).toList();
        var last=Enrollment.create(classId,student,EnrollmentSource.MANUAL,now);
        when(enrollments.listByClass(classId,true,100,0)).thenReturn(first);
        when(enrollments.listByClass(classId,true,100,100)).thenReturn(List.of(last));
        assertThat(adapter.listActiveStudents(classId)).hasSize(101).contains(student);
    }

    @Test void openClassesCombineTeacherAndStudentRelationshipsWithoutDuplicates() {
        var course=course(teacher,ClassStatus.OPEN);
        when(queries.listForTeacher(teacher,null,ClassStatus.OPEN,100,0)).thenReturn(List.of(course));
        when(queries.listForStudent(teacher,null,ClassStatus.OPEN,100,0)).thenReturn(List.of(course));
        assertThat(adapter.listOpenClassesOf(teacher)).hasSize(1).extracting(ClassRef::classId).containsExactly(classId);
    }

    @Test void subjectContractCombinesCountsAndMinimalTeacherContact() {
        var course=course(teacher,ClassStatus.OPEN);
        when(queries.listOfSubject(subjectId)).thenReturn(List.of(course));
        when(queries.enrollmentCounts(List.of(classId))).thenReturn(Map.of(classId,new IAcademicsReadRepository.EnrollmentCounts(3,1)));
        when(accounts.getContact(teacher)).thenReturn(Optional.of(new AccountContact(teacher,"hidden@example.test","Teacher",Role.TEACHER,AccountStatus.ACTIVE)));
        assertThat(adapter.listClassesOfSubject(subjectId)).containsExactly(new SubjectClassSummary(classId,"SE01","Lop","2026","OPEN","Teacher",3));
    }
}
