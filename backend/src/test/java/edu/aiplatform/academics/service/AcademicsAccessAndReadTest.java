package edu.aiplatform.academics.service;

import edu.aiplatform.academics.dto.AcademicsDtos;

import edu.aiplatform.academics.dto.AcademicsDtos.ClassView;
import edu.aiplatform.academics.dto.AcademicsDtos.Person;
import edu.aiplatform.academics.dto.AcademicsDtos.SubjectSummary;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.model.*;
import edu.aiplatform.academics.repository.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcademicsAccessAndReadTest {
    private final ICourseClassRepository courseClassRepository = mock(ICourseClassRepository.class);
    private final IEnrollmentRepository enrollmentRepository = mock(IEnrollmentRepository.class);
    private final IAcademicsReadRepository academicsReadRepository = mock(IAcademicsReadRepository.class);
    private final ISubjectMutationGuard subjectMutationGuard = mock(ISubjectMutationGuard.class);
    private final IAccountLookup accountLookup = mock(IAccountLookup.class);
    private final ISubjectQuery subjectQuery = mock(ISubjectQuery.class);
    private final ISubjectScope scope = mock(ISubjectScope.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IAudit audit = mock(IAudit.class);
    private final IEventPublisher eventPublisher = mock(IEventPublisher.class);
    private final UUID studentId = UUID.randomUUID(), teacherId = UUID.randomUUID(), managerId = UUID.randomUUID(), adminId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID(), classId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-18T00:00:00Z");
    private AcademicsService academicsService;

    @BeforeEach void setUp() {
        PlatformTransactionManager transactions = new AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
            @Override protected void doCommit(DefaultTransactionStatus status) { }
            @Override protected void doRollback(DefaultTransactionStatus status) { }
        };
        academicsService = new AcademicsService(courseClassRepository,enrollmentRepository,academicsReadRepository,subjectMutationGuard,accountLookup,subjectQuery,scope,authorization,audit,eventPublisher,
                transactions,Clock.fixed(now,ZoneOffset.UTC));
        for (Role role : Role.values()) {
            Actor actor = actor(role);
            when(accountLookup.getContact(actor.accountId())).thenReturn(Optional.of(contact(actor.accountId(),role,AccountStatus.ACTIVE)));
        }
        when(authorization.authorize(any(),anyString(),any())).thenReturn(AuthorizationDecision.allow());
        when(scope.isSubjectManager(managerId,subjectId)).thenReturn(true);
        when(scope.listManagedSubjects(managerId)).thenReturn(List.of(subjectId));
        when(subjectQuery.getSubject(subjectId)).thenReturn(Optional.of(subject(subjectId,managerId)));
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(course(classId,ClassStatus.OPEN,teacherId)));
        when(enrollmentRepository.find(classId,studentId)).thenReturn(Optional.of(enrollment(studentId)));
    }

    @AfterEach void readsAndDeniedMutationsNeverWrite() {
        verify(courseClassRepository,never()).insert(any());
        verify(courseClassRepository,never()).update(any(),anyLong());
        verify(enrollmentRepository,never()).insert(any());
        verify(enrollmentRepository,never()).update(any(),any());
        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest(name="{0} access to {1} audience: allowed={2}")
    @CsvSource({
        "STUDENT,student,true", "STUDENT,teacher,false", "STUDENT,manager,false",
        "TEACHER,student,false", "TEACHER,teacher,true", "TEACHER,manager,false",
        "SUBJECT_MANAGER,student,false", "SUBJECT_MANAGER,teacher,true", "SUBJECT_MANAGER,manager,true",
        "ADMIN,student,false", "ADMIN,teacher,false", "ADMIN,manager,false"
    })
    void roleAudienceMatrix(Role role,String audience,boolean allowed) {
        if (allowed) {
            assertThat(academicsService.listClasses(actor(role),audience,null,null,null,1,20)).isEmpty();
            verify(authorization).authorize(actor(role),"CLASS_"+audience.toUpperCase(Locale.ROOT),ResourceRef.none());
            verifyNoInteractions(audit);
        } else {
            assertProblem(() -> academicsService.listClasses(actor(role),audience,null,null,null,1,20),403,"ROLE_DENIED");
            verifyNoInteractions(academicsReadRepository,courseClassRepository,enrollmentRepository,subjectQuery,scope,authorization);
            assertDenialAudit();
        }
    }

    @Test void anonymousCallerCannotRetrieveAnyClassData() {
        assertProblem(() -> academicsService.loadClass(null,"student",classId),401,"AUTHENTICATION_REQUIRED");
        verifyNoInteractions(accountLookup,authorization,courseClassRepository,enrollmentRepository,academicsReadRepository,subjectQuery,scope);
        assertDenialAudit();
    }

    @ParameterizedTest
    @MethodSource("malformedActors")
    void incompletePrincipalCannotReadData(Actor actor) {
        assertProblem(() -> academicsService.loadClass(actor,"student",classId),401,"AUTHENTICATION_REQUIRED");
        verifyNoInteractions(accountLookup,authorization,courseClassRepository,enrollmentRepository,academicsReadRepository,subjectQuery,scope);
        assertDenialAudit();
    }

    static Stream<Actor> malformedActors() {
        return Stream.of(new Actor(null,Role.STUDENT),new Actor(UUID.randomUUID(),null));
    }

    @ParameterizedTest
    @EnumSource(value=AccountStatus.class,names={"PENDING","DISABLED"})
    void nonActiveCallerIsRejectedBeforeDataLookup(AccountStatus status) {
        when(accountLookup.getContact(studentId)).thenReturn(Optional.of(contact(studentId,Role.STUDENT,status)));
        assertProblem(() -> academicsService.loadClass(actor(Role.STUDENT),"student",classId),403,"ACCOUNT_INACTIVE");
        verifyNoInteractions(authorization,courseClassRepository,enrollmentRepository,academicsReadRepository,subjectQuery,scope);
        assertDenialAudit();
    }

    @Test void missingAccountCannotUseAnOtherwiseValidPrincipal() {
        when(accountLookup.getContact(studentId)).thenReturn(Optional.empty());
        assertProblem(() -> academicsService.loadClass(actor(Role.STUDENT),"student",classId),403,"ACCOUNT_INACTIVE");
        verifyNoInteractions(authorization,courseClassRepository,enrollmentRepository,academicsReadRepository,subjectQuery,scope);
    }

    @Test void stalePrincipalRoleCannotElevateCurrentAccount() {
        when(accountLookup.getContact(managerId)).thenReturn(Optional.of(contact(managerId,Role.TEACHER,AccountStatus.ACTIVE)));
        assertProblem(() -> academicsService.updateClassInfo(actor(Role.SUBJECT_MANAGER),classId,"Changed",null,7),403,"ACCOUNT_INACTIVE");
        verifyNoInteractions(authorization,courseClassRepository,enrollmentRepository,academicsReadRepository,subjectQuery,scope,subjectMutationGuard);
        assertDenialAudit();
    }

    @ParameterizedTest
    @CsvSource({"denied,403,ACCESS_DENIED", "null,403,ACCESS_DENIED", "unavailable,503,ACADEMICS_DEPENDENCY_UNAVAILABLE"})
    void failedGlobalAuthorizationStopsBeforeDataRetrieval(String decision,int status,String code) {
        if (decision.equals("denied")) when(authorization.authorize(any(),anyString(),any())).thenReturn(AuthorizationDecision.deny("private policy reason"));
        else if (decision.equals("null")) when(authorization.authorize(any(),anyString(),any())).thenReturn(null);
        else when(authorization.authorize(any(),anyString(),any())).thenThrow(new IllegalStateException("upstream secret"));
        assertProblem(() -> academicsService.listClasses(actor(Role.STUDENT),"student",null,null,null,1,20),status,code);
        verifyNoInteractions(courseClassRepository,enrollmentRepository,academicsReadRepository,subjectQuery,scope);
        verify(audit).record(argThat(e -> e.result()==(status==503?AuditResult.FAILURE:AuditResult.DENIED) && e.details().isEmpty()));
    }

    @ParameterizedTest
    @ValueSource(booleans={false,true})
    void scopedAuthorizationDenialOrMissingDecisionHidesClass(boolean missingDecision) {
        when(authorization.authorize(actor(Role.STUDENT),"CLASS_VIEW",ResourceRef.courseClass(classId)))
                .thenReturn(missingDecision?null:AuthorizationDecision.deny("sensitive scope"));
        assertProblem(() -> academicsService.loadClass(actor(Role.STUDENT),"student",classId),404,"CLASS_OUT_OF_SCOPE");
        verifyNoInteractions(academicsReadRepository,subjectQuery);
        verify(accountLookup,never()).getContact(teacherId);
        assertDenialAudit();
    }

    @Test void accountDependencyFailureIsSafeAndCannotRetrieveClasses() {
        when(accountLookup.getContact(studentId)).thenThrow(new IllegalStateException("upstream secret"));
        assertProblem(() -> academicsService.loadClass(actor(Role.STUDENT),"student",classId),503,"ACADEMICS_DEPENDENCY_UNAVAILABLE");
        verifyNoInteractions(authorization,courseClassRepository,enrollmentRepository,academicsReadRepository,subjectQuery,scope);
    }

    @Test void managerCannotRequestAClassFromAnUnmanagedSubject() {
        when(scope.isSubjectManager(managerId,subjectId)).thenReturn(false);
        assertProblem(() -> academicsService.loadClass(actor(Role.SUBJECT_MANAGER),"manager",classId),404,"CLASS_OUT_OF_SCOPE");
        verifyNoInteractions(academicsReadRepository,subjectQuery);
        verify(accountLookup,never()).getContact(teacherId);
        assertDenialAudit();
    }

    @Test void subjectFilterOutsideManagerScopeDoesNotBecomeAnUnscopedQuery() {
        UUID foreignSubject = UUID.randomUUID();
        assertProblem(() -> academicsService.listClasses(actor(Role.SUBJECT_MANAGER),"manager",foreignSubject,"private",ClassStatus.OPEN,1,20),404,"SUBJECT_OUT_OF_SCOPE");
        verifyNoInteractions(academicsReadRepository,courseClassRepository,enrollmentRepository,subjectQuery);
        assertDenialAudit();
    }

    @Test void currentSubjectOwnerMustMatchBeforeMutationEvenWhenScopeAllows() {
        when(subjectQuery.getSubject(subjectId)).thenReturn(Optional.of(subject(subjectId,UUID.randomUUID())));
        assertProblem(() -> academicsService.updateClassInfo(actor(Role.SUBJECT_MANAGER),classId,"Changed",null,7),404,"SUBJECT_OUT_OF_SCOPE");
        verify(subjectMutationGuard).lockActive(org.mockito.ArgumentMatchers.eq(subjectId),org.mockito.ArgumentMatchers.any());
        verify(courseClassRepository,never()).update(any(),anyLong());
        verifyNoInteractions(academicsReadRepository);
        assertDenialAudit();
    }

    @Test void subjectScopeDependencyFailureCannotExposeManagerClassData() {
        when(scope.listManagedSubjects(managerId)).thenThrow(new IllegalStateException("upstream secret"));
        assertProblem(() -> academicsService.listClasses(actor(Role.SUBJECT_MANAGER),"manager",null,null,null,1,20),503,"ACADEMICS_DEPENDENCY_UNAVAILABLE");
        verifyNoInteractions(academicsReadRepository,courseClassRepository,enrollmentRepository,subjectQuery);
    }

    @ParameterizedTest
    @CsvSource({"STUDENT,student", "TEACHER,teacher", "SUBJECT_MANAGER,manager"})
    void successfulListsPreserveScopedFilteringPagingAndCompleteClassDtos(Role role,String audience) {
        CourseClass expected = course(classId,ClassStatus.OPEN,teacherId);
        when(academicsReadRepository.listForStudent(studentId,"CS 101",ClassStatus.OPEN,7,14)).thenReturn(List.of(expected));
        when(academicsReadRepository.listForTeacher(teacherId,"CS 101",ClassStatus.OPEN,7,14)).thenReturn(List.of(expected));
        when(academicsReadRepository.listForManager(List.of(subjectId),"CS 101",ClassStatus.OPEN,7,14)).thenReturn(List.of(expected));
        when(academicsReadRepository.enrollmentCounts(List.of(classId))).thenReturn(Map.of(classId,new IAcademicsReadRepository.EnrollmentCounts(12,3)));

        var result = academicsService.listClasses(actor(role),audience,null,"CS 101",ClassStatus.OPEN,3,7);

        assertThat(result).singleElement().satisfies(view -> assertClassDto(view,12,3));
        if (role==Role.STUDENT) {
            verify(academicsReadRepository).listForStudent(studentId,"CS 101",ClassStatus.OPEN,7,14);
            verify(academicsReadRepository,never()).listForTeacher(any(),any(),any(),anyInt(),anyInt());
            verify(academicsReadRepository,never()).listForManager(any(),any(),any(),anyInt(),anyInt());
        } else if (role==Role.TEACHER) {
            verify(academicsReadRepository).listForTeacher(teacherId,"CS 101",ClassStatus.OPEN,7,14);
            verify(academicsReadRepository,never()).listForStudent(any(),any(),any(),anyInt(),anyInt());
            verify(academicsReadRepository,never()).listForManager(any(),any(),any(),anyInt(),anyInt());
        } else {
            verify(academicsReadRepository).listForManager(List.of(subjectId),"CS 101",ClassStatus.OPEN,7,14);
            verify(academicsReadRepository,never()).listForStudent(any(),any(),any(),anyInt(),anyInt());
            verify(academicsReadRepository,never()).listForTeacher(any(),any(),any(),anyInt(),anyInt());
        }
        verify(courseClassRepository,never()).listBySubject(any(),anyInt(),anyInt());
        verifyNoInteractions(audit);
    }

    @Test void requestedManagedSubjectNarrowsTheQueryToThatSubjectOnly() {
        UUID anotherSubject = UUID.randomUUID();
        when(scope.listManagedSubjects(managerId)).thenReturn(List.of(subjectId,anotherSubject));
        assertThat(academicsService.listClasses(actor(Role.SUBJECT_MANAGER),"manager",subjectId,"C",ClassStatus.DRAFT,2,100)).isEmpty();
        verify(academicsReadRepository).listForManager(List.of(subjectId),"C",ClassStatus.DRAFT,100,100);
        verify(academicsReadRepository,never()).listForManager(eq(List.of(subjectId,anotherSubject)),any(),any(),anyInt(),anyInt());
    }

    @Test void emptyManagerScopeReturnsNoClassesWithoutCallingUnscopedDataQueries() {
        when(scope.listManagedSubjects(managerId)).thenReturn(List.of());
        assertThat(academicsService.listClasses(actor(Role.SUBJECT_MANAGER),"manager",null,null,null,1,20)).isEmpty();
        verify(academicsReadRepository,never()).listForManager(any(),any(),any(),anyInt(),anyInt());
        verify(academicsReadRepository,never()).listForStudent(any(),any(),any(),anyInt(),anyInt());
        verify(academicsReadRepository,never()).listForTeacher(any(),any(),any(),anyInt(),anyInt());
        verifyNoInteractions(courseClassRepository,enrollmentRepository,subjectQuery);
    }

    @ParameterizedTest
    @CsvSource({"STUDENT,student", "TEACHER,teacher", "SUBJECT_MANAGER,manager"})
    void successfulDetailReturnsSubjectTeacherAndCountsWithoutWriting(Role role,String audience) {
        when(academicsReadRepository.enrollmentCounts(List.of(classId))).thenReturn(Map.of(classId,new IAcademicsReadRepository.EnrollmentCounts(12,3)));
        assertClassDto(academicsService.loadClass(actor(role),audience,classId),12,3);
        if (role==Role.SUBJECT_MANAGER) verify(authorization).authorize(actor(role),"CLASS_MANAGE",ResourceRef.subject(subjectId));
        else verify(authorization).authorize(actor(role),"CLASS_VIEW",ResourceRef.courseClass(classId));
        if (role==Role.STUDENT) verify(enrollmentRepository).find(classId,studentId);
        verifyNoInteractions(audit);
    }

    @Test void subjectManagerCanReadTeachingDetailWhenCurrentlyAssigned() {
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(course(classId,ClassStatus.DRAFT,managerId)));
        ClassView view = academicsService.loadClass(actor(Role.SUBJECT_MANAGER),"teacher",classId);
        assertThat(view.id()).isEqualTo(classId);
        assertThat(view.teacher().id()).isEqualTo(managerId);
        assertThat(view.status()).isEqualTo(ClassStatus.DRAFT);
        verifyNoInteractions(scope);
    }

    @Test void draftWithNoTeacherUsesNullTeacherAndZeroCounts() {
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(course(classId,ClassStatus.DRAFT,null)));
        var view = academicsService.loadClass(actor(Role.SUBJECT_MANAGER),"manager",classId);
        assertThat(view.teacher()).isNull();
        assertThat(view.activeStudentCount()).isZero();
        assertThat(view.removedStudentCount()).isZero();
        verify(accountLookup,never()).getContact(teacherId);
    }

    @ParameterizedTest
    @EnumSource(value=ClassStatus.class,names={"DRAFT","ARCHIVED"})
    void studentDetailHidesClassesThatAreNotOpen(ClassStatus status) {
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(course(classId,status,teacherId)));
        assertProblem(() -> academicsService.loadClass(actor(Role.STUDENT),"student",classId),404,"CLASS_OUT_OF_SCOPE");
        verify(enrollmentRepository,never()).find(any(),any());
        verifyNoInteractions(academicsReadRepository,subjectQuery);
    }

    @ParameterizedTest
    @ValueSource(booleans={false,true})
    void studentDetailHidesRemovedOrMissingEnrollment(boolean removed) {
        when(enrollmentRepository.find(classId,studentId)).thenReturn(removed?Optional.of(enrollment(studentId).removeEnrollment(now)):Optional.empty());
        assertProblem(() -> academicsService.loadClass(actor(Role.STUDENT),"student",classId),404,"CLASS_OUT_OF_SCOPE");
        verifyNoInteractions(academicsReadRepository,subjectQuery);
        assertDenialAudit();
    }

    @Test void studentCannotRetrieveTheClassRosterEvenWhenEnrolled() {
        assertProblem(() -> academicsService.listStudents(actor(Role.STUDENT),"student",classId,1,20),403,"ROLE_DENIED");
        verify(enrollmentRepository,never()).listByClass(any(),anyBoolean(),anyInt(),anyInt());
        verifyNoInteractions(subjectQuery,academicsReadRepository);
    }

    @ParameterizedTest
    @CsvSource({"TEACHER,teacher,true", "SUBJECT_MANAGER,manager,false"})
    void rosterReturnsStudentContactAndEnrollmentWithCorrectStatusScope(Role role,String audience,boolean activeOnly) {
        Enrollment value = enrollment(studentId);
        when(enrollmentRepository.listByClass(classId,activeOnly,5,5)).thenReturn(List.of(value));
        var result = academicsService.listStudents(actor(role),audience,classId,2,5);
        assertThat(result).containsExactly(new AcademicsDtos.Student(studentId,"STUDENT name","student@example.com",value.enrolledAt(),EnrollmentStatus.ACTIVE));
        verify(enrollmentRepository).listByClass(classId,activeOnly,5,5);
        verifyNoInteractions(academicsReadRepository,subjectQuery,audit);
    }

    @Test void teacherStudentPopupReturnsOnlyTheActiveStudentInThisClass() {
        Enrollment value = enrollment(studentId);
        var result = academicsService.loadStudent(actor(Role.TEACHER),classId,studentId);
        assertThat(result).isEqualTo(new AcademicsDtos.Student(studentId,"STUDENT name","student@example.com",value.enrolledAt(),EnrollmentStatus.ACTIVE));
        verify(enrollmentRepository).find(classId,studentId);
        verifyNoInteractions(academicsReadRepository,subjectQuery,audit);
    }

    @ParameterizedTest
    @ValueSource(booleans={false,true})
    void teacherStudentPopupHidesRemovedOrUnknownStudentWithoutLookingUpContact(boolean removed) {
        when(enrollmentRepository.find(classId,studentId)).thenReturn(removed?Optional.of(enrollment(studentId).removeEnrollment(now)):Optional.empty());
        assertProblem(() -> academicsService.loadStudent(actor(Role.TEACHER),classId,studentId),404,"STUDENT_NOT_FOUND");
        verify(accountLookup,never()).getContact(studentId);
        assertDenialAudit();
    }

    @Test void staleTeacherCannotRetrieveStudentContactsAfterAssignmentChanged() {
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(course(classId,ClassStatus.OPEN,managerId)));
        assertProblem(() -> academicsService.loadStudent(actor(Role.TEACHER),classId,studentId),404,"CLASS_OUT_OF_SCOPE");
        verifyNoInteractions(enrollmentRepository,academicsReadRepository,subjectQuery);
        verify(accountLookup,never()).getContact(studentId);
    }

    @Test void managedSubjectSummaryCountsEveryStatusAndSuppliesZeroForAbsentStatus() {
        when(academicsReadRepository.listOfSubject(subjectId)).thenReturn(List.of(course(classId,ClassStatus.OPEN,teacherId),course(UUID.randomUUID(),ClassStatus.OPEN,teacherId),course(UUID.randomUUID(),ClassStatus.DRAFT,null)));
        SubjectSummary result = academicsService.loadManagedSubject(actor(Role.SUBJECT_MANAGER),subjectId);
        assertThat(result).isEqualTo(new SubjectSummary(subjectId,"CS101","Computer Science",SubjectStatus.ACTIVE,
                Map.of(ClassStatus.DRAFT,1L,ClassStatus.OPEN,2L,ClassStatus.ARCHIVED,0L)));
        verify(academicsReadRepository).listOfSubject(subjectId);
        verifyNoInteractions(audit);
    }

    @Test void managedSubjectsExcludesOutOfScopeSubjectsAndKeepsIndependentCounts() {
        UUID otherId = UUID.randomUUID(), foreignId = UUID.randomUUID();
        when(subjectQuery.listManagedSubjects(managerId)).thenReturn(List.of(subject(subjectId,managerId),subject(otherId,managerId),subject(foreignId,UUID.randomUUID())));
        when(scope.isSubjectManager(managerId,otherId)).thenReturn(true);
        when(academicsReadRepository.listOfSubject(subjectId)).thenReturn(List.of(course(classId,ClassStatus.ARCHIVED,teacherId)));
        when(academicsReadRepository.listOfSubject(otherId)).thenReturn(List.of());
        var results = academicsService.listManagedSubjects(actor(Role.SUBJECT_MANAGER));
        assertThat(results).extracting(SubjectSummary::id).containsExactly(subjectId,otherId);
        assertThat(results.get(0).classCounts()).containsEntry(ClassStatus.ARCHIVED,1L).containsEntry(ClassStatus.OPEN,0L);
        assertThat(results.get(1).classCounts()).containsExactlyInAnyOrderEntriesOf(Map.of(ClassStatus.DRAFT,0L,ClassStatus.OPEN,0L,ClassStatus.ARCHIVED,0L));
        verify(academicsReadRepository,never()).listOfSubject(foreignId);
        verifyNoInteractions(audit);
    }

    @Test void managedSubjectWrongScopeDoesNotRequestSubjectDetailsOrCounts() {
        UUID foreignId = UUID.randomUUID();
        assertProblem(() -> academicsService.loadManagedSubject(actor(Role.SUBJECT_MANAGER),foreignId),404,"SUBJECT_OUT_OF_SCOPE");
        verifyNoInteractions(subjectQuery,academicsReadRepository);
        assertDenialAudit();
    }

    @ParameterizedTest
    @ValueSource(strings={"subject","teacher","counts"})
    void missingClassDetailDependencyFailsSafelyInsteadOfReturningPartialData(String dependency) {
        if (dependency.equals("subject")) when(subjectQuery.getSubject(subjectId)).thenReturn(Optional.empty());
        else if (dependency.equals("teacher")) when(accountLookup.getContact(teacherId)).thenReturn(Optional.empty());
        else when(academicsReadRepository.enrollmentCounts(any())).thenThrow(new IllegalStateException("upstream secret"));
        String code = dependency.equals("subject")?"SUBJECT_UNAVAILABLE":dependency.equals("teacher")?"ACCOUNT_UNAVAILABLE":"ACADEMICS_DEPENDENCY_UNAVAILABLE";
        assertProblem(() -> academicsService.loadClass(actor(Role.STUDENT),"student",classId),503,code);
        verify(audit).record(argThat(e -> e.result()==AuditResult.FAILURE));
    }

    @Test void studentCandidatesKeepActiveAndPendingStudentsFilterOthersAndCapAtTwenty() {
        var eligible = new ArrayList<AccountContact>();
        eligible.add(contact(UUID.randomUUID(),Role.STUDENT,AccountStatus.PENDING));
        for (int i=0;i<24;i++) eligible.add(contact(UUID.randomUUID(),Role.STUDENT,AccountStatus.ACTIVE));
        var response = new ArrayList<AccountContact>(List.of(contact(UUID.randomUUID(),Role.STUDENT,AccountStatus.DISABLED),contact(UUID.randomUUID(),Role.TEACHER,AccountStatus.ACTIVE)));
        response.addAll(eligible);
        when(accountLookup.findStudents("name or email",20)).thenReturn(response);
        var result = academicsService.listStudentCandidates(actor(Role.SUBJECT_MANAGER),classId,"name or email");
        assertThat(result).containsExactlyElementsOf(eligible.stream().limit(20).map(this::person).toList());
        verify(accountLookup).findStudents("name or email",20);
        verifyNoInteractions(academicsReadRepository,subjectQuery,audit);
    }

    @Test void teacherCandidatesKeepOnlyActiveTeachingRolesAndCapAfterFiltering() {
        var eligible = new ArrayList<AccountContact>();
        eligible.add(contact(UUID.randomUUID(),Role.SUBJECT_MANAGER,AccountStatus.ACTIVE));
        for (int i=0;i<24;i++) eligible.add(contact(UUID.randomUUID(),Role.TEACHER,AccountStatus.ACTIVE));
        var response = new ArrayList<AccountContact>(List.of(contact(UUID.randomUUID(),Role.TEACHER,AccountStatus.PENDING),contact(UUID.randomUUID(),Role.SUBJECT_MANAGER,AccountStatus.DISABLED),contact(UUID.randomUUID(),Role.ADMIN,AccountStatus.ACTIVE),contact(UUID.randomUUID(),Role.STUDENT,AccountStatus.ACTIVE)));
        response.addAll(eligible);
        when(accountLookup.findAccounts("lecturer",Set.of(Role.TEACHER,Role.SUBJECT_MANAGER),20)).thenReturn(response);
        assertThat(academicsService.listTeacherCandidates(actor(Role.SUBJECT_MANAGER),"lecturer"))
                .containsExactlyElementsOf(eligible.stream().limit(20).map(this::person).toList());
        verify(accountLookup).findAccounts("lecturer",Set.of(Role.TEACHER,Role.SUBJECT_MANAGER),20);
        verifyNoInteractions(academicsReadRepository,subjectQuery,courseClassRepository,enrollmentRepository,audit);
    }

    @Test void studentCandidatesOutsideManagerScopeNeverCallAccountSearch() {
        when(scope.isSubjectManager(managerId,subjectId)).thenReturn(false);
        assertProblem(() -> academicsService.listStudentCandidates(actor(Role.SUBJECT_MANAGER),classId,"private"),404,"CLASS_OUT_OF_SCOPE");
        verify(accountLookup,never()).findStudents(any(),anyInt());
        assertDenialAudit();
    }

    @ParameterizedTest
    @ValueSource(booleans={false,true})
    void failedCandidateLookupReturnsSafe503InsteadOfAnEmptyOrPartialList(boolean students) {
        if (students) when(accountLookup.findStudents(any(),anyInt())).thenThrow(new IllegalStateException("upstream secret"));
        else when(accountLookup.findAccounts(any(),anySet(),anyInt())).thenThrow(new IllegalStateException("upstream secret"));
        assertProblem(() -> {
            if (students) academicsService.listStudentCandidates(actor(Role.SUBJECT_MANAGER),classId,"term");
            else academicsService.listTeacherCandidates(actor(Role.SUBJECT_MANAGER),"term");
        },503,"ACADEMICS_DEPENDENCY_UNAVAILABLE");
        verify(audit).record(argThat(e -> e.result()==AuditResult.FAILURE && e.details().isEmpty()));
    }

    @ParameterizedTest
    @ValueSource(booleans={false,true})
    void excessiveCandidateSearchIsRejectedBeforeUpstreamSearch(boolean students) {
        assertProblem(() -> {
            if (students) academicsService.listStudentCandidates(actor(Role.SUBJECT_MANAGER),classId,"q".repeat(201));
            else academicsService.listTeacherCandidates(actor(Role.SUBJECT_MANAGER),"q".repeat(201));
        },400,"INVALID_QUERY");
        verify(accountLookup,never()).findStudents(any(),anyInt());
        verify(accountLookup,never()).findAccounts(any(),anySet(),anyInt());
    }

    @ParameterizedTest(name="{1} cannot invoke {0}")
    @MethodSource("forbiddenMutationRoles")
    void everyMutationRejectsNonManagerRolesBeforeRetrievingData(String operation,Role role) {
        assertProblem(() -> invokeMutation(operation,actor(role)),403,"ROLE_DENIED");
        verifyNoInteractions(courseClassRepository,enrollmentRepository,academicsReadRepository,subjectQuery,scope,subjectMutationGuard,authorization,accountLookup);
        assertDenialAudit();
    }

    static Stream<Arguments> forbiddenMutationRoles() {
        return mutationOperations().flatMap(operation -> Stream.of(Role.STUDENT,Role.TEACHER,Role.ADMIN)
                .map(role -> Arguments.of(operation,role)));
    }

    @ParameterizedTest(name="manager without write permission cannot invoke {0}")
    @MethodSource("mutationOperations")
    void everyMutationRequiresManagerWritePermissionEvenWhenViewAndSubjectScopeAllow(String operation) {
        when(authorization.authorize(actor(Role.SUBJECT_MANAGER),"CLASS_MANAGE",ResourceRef.subject(subjectId)))
                .thenReturn(AuthorizationDecision.deny("private policy reason"));
        assertProblem(() -> invokeMutation(operation,actor(Role.SUBJECT_MANAGER)),404,"CLASS_OUT_OF_SCOPE");
        verify(authorization).authorize(actor(Role.SUBJECT_MANAGER),"CLASS_MANAGE",ResourceRef.subject(subjectId));
        assertDenialAudit();
    }

    static Stream<String> mutationOperations() {
        return Stream.of("create","updateInfo","assignTeacher","changeStatus","enroll","enrollBatch","removeEnrollment","activateEnrollment");
    }

    private void invokeMutation(String operation,Actor actor) {
        switch(operation) {
            case "create" -> academicsService.createClass(actor,subjectId,"CS101-B","New class",null,teacherId);
            case "updateInfo" -> academicsService.updateClassInfo(actor,classId,"Changed",null,7);
            case "assignTeacher" -> academicsService.assignClassTeacher(actor,classId,managerId,7);
            case "changeStatus" -> academicsService.changeClassStatus(actor,classId,ClassStatus.ARCHIVED,7);
            case "enroll" -> academicsService.enrollStudent(actor,classId,studentId);
            case "enrollBatch" -> academicsService.enrollStudents(actor,classId,"student@example.com");
            case "removeEnrollment" -> academicsService.changeEnrollmentStatus(actor,classId,studentId,EnrollmentStatus.REMOVED);
            case "activateEnrollment" -> academicsService.changeEnrollmentStatus(actor,classId,studentId,EnrollmentStatus.ACTIVE);
            default -> throw new IllegalArgumentException("Unknown test operation: "+operation);
        }
    }
    private Actor actor(Role role) {
        return new Actor(switch(role) {case STUDENT -> studentId;case TEACHER -> teacherId;case SUBJECT_MANAGER -> managerId;case ADMIN -> adminId;},role);
    }
    private AccountContact contact(UUID id,Role role,AccountStatus status) {
        return new AccountContact(id,role.name().toLowerCase(Locale.ROOT)+"@example.com",role+" name",role,status);
    }
    private Person person(AccountContact contact) {return new Person(contact.accountId(),contact.displayName(),contact.email());}
    private SubjectRef subject(UUID id,UUID owner) {return new SubjectRef(id,"CS101","Computer Science",SubjectStatus.ACTIVE,owner);}
    private CourseClass course(UUID id,ClassStatus status,UUID assignedTeacher) {
        return new CourseClass(id,subjectId,"CS101-A","Algorithms",null,status,assignedTeacher,true,now.minusSeconds(3600),now,7);
    }
    private Enrollment enrollment(UUID id) {return Enrollment.createEnrollment(classId,id,EnrollmentSource.MANUAL,now.minusSeconds(120));}
    private void assertClassDto(ClassView view,long active,long removed) {
        assertThat(view).isEqualTo(new ClassView(classId,new AcademicsDtos.Subject(subjectId,"CS101","Computer Science"),"CS101-A","Algorithms","",ClassStatus.OPEN,
                new Person(teacherId,"TEACHER name","teacher@example.com"),active,removed,now.minusSeconds(3600),true,7));
    }
    private void assertProblem(Runnable call,int status,String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class,error -> {
            assertThat(error.status().value()).isEqualTo(status);
            assertThat(error.code()).isEqualTo(code);
            assertThat(error.getMessage()).doesNotContain("upstream secret","private policy reason","sensitive scope");
        });
    }
    private void assertDenialAudit() {
        verify(audit).record(argThat(e -> e.action().equals("ACCESS_DENIED") && e.result()==AuditResult.DENIED && e.details().isEmpty()));
    }
}
