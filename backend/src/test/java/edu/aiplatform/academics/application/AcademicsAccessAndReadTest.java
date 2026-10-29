package edu.aiplatform.academics.application;

import edu.aiplatform.academics.application.AcademicsDtos.ClassView;
import edu.aiplatform.academics.application.AcademicsDtos.Person;
import edu.aiplatform.academics.application.AcademicsDtos.SubjectSummary;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.domain.*;
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
    private final ICourseClassRepository classes = mock(ICourseClassRepository.class);
    private final IEnrollmentRepository enrollments = mock(IEnrollmentRepository.class);
    private final IAcademicsReadRepository reads = mock(IAcademicsReadRepository.class);
    private final ISubjectMutationGuard lock = mock(ISubjectMutationGuard.class);
    private final IAccountLookup accounts = mock(IAccountLookup.class);
    private final ISubjectQuery subjects = mock(ISubjectQuery.class);
    private final ISubjectScope scope = mock(ISubjectScope.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IAudit audit = mock(IAudit.class);
    private final IEventPublisher events = mock(IEventPublisher.class);
    private final UUID studentId = UUID.randomUUID(), teacherId = UUID.randomUUID(), managerId = UUID.randomUUID(), adminId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID(), classId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-18T00:00:00Z");
    private AcademicsService service;

    @BeforeEach void setUp() {
        PlatformTransactionManager transactions = new AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
            @Override protected void doCommit(DefaultTransactionStatus status) { }
            @Override protected void doRollback(DefaultTransactionStatus status) { }
        };
        service = new AcademicsService(classes,enrollments,reads,lock,accounts,subjects,scope,authorization,audit,events,
                transactions,Clock.fixed(now,ZoneOffset.UTC));
        for (Role role : Role.values()) {
            Actor actor = actor(role);
            when(accounts.getContact(actor.accountId())).thenReturn(Optional.of(contact(actor.accountId(),role,AccountStatus.ACTIVE)));
        }
        when(authorization.authorize(any(),anyString(),any())).thenReturn(AuthorizationDecision.allow());
        when(scope.isSubjectManager(managerId,subjectId)).thenReturn(true);
        when(scope.listManagedSubjects(managerId)).thenReturn(List.of(subjectId));
        when(subjects.getSubject(subjectId)).thenReturn(Optional.of(subject(subjectId,managerId)));
        when(classes.find(classId)).thenReturn(Optional.of(course(classId,ClassStatus.OPEN,teacherId)));
        when(enrollments.find(classId,studentId)).thenReturn(Optional.of(enrollment(studentId)));
    }

    @AfterEach void readsAndDeniedMutationsNeverWrite() {
        verify(classes,never()).insert(any());
        verify(classes,never()).update(any(),anyLong());
        verify(enrollments,never()).insert(any());
        verify(enrollments,never()).update(any(),any());
        verifyNoInteractions(events);
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
            assertThat(service.list(actor(role),audience,null,null,null,1,20)).isEmpty();
            verify(authorization).authorize(actor(role),"CLASS_"+audience.toUpperCase(Locale.ROOT),ResourceRef.none());
            verifyNoInteractions(audit);
        } else {
            assertProblem(() -> service.list(actor(role),audience,null,null,null,1,20),403,"ROLE_DENIED");
            verifyNoInteractions(reads,classes,enrollments,subjects,scope,authorization);
            assertDenialAudit();
        }
    }

    @Test void anonymousCallerCannotRetrieveAnyClassData() {
        assertProblem(() -> service.detail(null,"student",classId),401,"AUTHENTICATION_REQUIRED");
        verifyNoInteractions(accounts,authorization,classes,enrollments,reads,subjects,scope);
        assertDenialAudit();
    }

    @ParameterizedTest
    @MethodSource("malformedActors")
    void incompletePrincipalCannotReadData(Actor actor) {
        assertProblem(() -> service.detail(actor,"student",classId),401,"AUTHENTICATION_REQUIRED");
        verifyNoInteractions(accounts,authorization,classes,enrollments,reads,subjects,scope);
        assertDenialAudit();
    }

    static Stream<Actor> malformedActors() {
        return Stream.of(new Actor(null,Role.STUDENT),new Actor(UUID.randomUUID(),null));
    }

    @ParameterizedTest
    @EnumSource(value=AccountStatus.class,names={"PENDING","DISABLED"})
    void nonActiveCallerIsRejectedBeforeDataLookup(AccountStatus status) {
        when(accounts.getContact(studentId)).thenReturn(Optional.of(contact(studentId,Role.STUDENT,status)));
        assertProblem(() -> service.detail(actor(Role.STUDENT),"student",classId),403,"ACCOUNT_INACTIVE");
        verifyNoInteractions(authorization,classes,enrollments,reads,subjects,scope);
        assertDenialAudit();
    }

    @Test void missingAccountCannotUseAnOtherwiseValidPrincipal() {
        when(accounts.getContact(studentId)).thenReturn(Optional.empty());
        assertProblem(() -> service.detail(actor(Role.STUDENT),"student",classId),403,"ACCOUNT_INACTIVE");
        verifyNoInteractions(authorization,classes,enrollments,reads,subjects,scope);
    }

    @Test void stalePrincipalRoleCannotElevateCurrentAccount() {
        when(accounts.getContact(managerId)).thenReturn(Optional.of(contact(managerId,Role.TEACHER,AccountStatus.ACTIVE)));
        assertProblem(() -> service.updateInfo(actor(Role.SUBJECT_MANAGER),classId,"Changed",null,null,7),403,"ACCOUNT_INACTIVE");
        verifyNoInteractions(authorization,classes,enrollments,reads,subjects,scope,lock);
        assertDenialAudit();
    }

    @ParameterizedTest
    @CsvSource({"denied,403,ACCESS_DENIED", "null,403,ACCESS_DENIED", "unavailable,503,ACADEMICS_DEPENDENCY_UNAVAILABLE"})
    void failedGlobalAuthorizationStopsBeforeDataRetrieval(String decision,int status,String code) {
        if (decision.equals("denied")) when(authorization.authorize(any(),anyString(),any())).thenReturn(AuthorizationDecision.deny("private policy reason"));
        else if (decision.equals("null")) when(authorization.authorize(any(),anyString(),any())).thenReturn(null);
        else when(authorization.authorize(any(),anyString(),any())).thenThrow(new IllegalStateException("upstream secret"));
        assertProblem(() -> service.list(actor(Role.STUDENT),"student",null,null,null,1,20),status,code);
        verifyNoInteractions(classes,enrollments,reads,subjects,scope);
        verify(audit).record(argThat(e -> e.result()==(status==503?AuditResult.FAILURE:AuditResult.DENIED) && e.details().isEmpty()));
    }

    @ParameterizedTest
    @ValueSource(booleans={false,true})
    void scopedAuthorizationDenialOrMissingDecisionHidesClass(boolean missingDecision) {
        when(authorization.authorize(actor(Role.STUDENT),"CLASS_VIEW",ResourceRef.courseClass(classId)))
                .thenReturn(missingDecision?null:AuthorizationDecision.deny("sensitive scope"));
        assertProblem(() -> service.detail(actor(Role.STUDENT),"student",classId),404,"CLASS_OUT_OF_SCOPE");
        verifyNoInteractions(reads,subjects);
        verify(accounts,never()).getContact(teacherId);
        assertDenialAudit();
    }

    @Test void accountDependencyFailureIsSafeAndCannotRetrieveClasses() {
        when(accounts.getContact(studentId)).thenThrow(new IllegalStateException("upstream secret"));
        assertProblem(() -> service.detail(actor(Role.STUDENT),"student",classId),503,"ACADEMICS_DEPENDENCY_UNAVAILABLE");
        verifyNoInteractions(authorization,classes,enrollments,reads,subjects,scope);
    }

    @Test void managerCannotRequestAClassFromAnUnmanagedSubject() {
        when(scope.isSubjectManager(managerId,subjectId)).thenReturn(false);
        assertProblem(() -> service.detail(actor(Role.SUBJECT_MANAGER),"manager",classId),404,"CLASS_OUT_OF_SCOPE");
        verifyNoInteractions(reads,subjects);
        verify(accounts,never()).getContact(teacherId);
        assertDenialAudit();
    }

    @Test void subjectFilterOutsideManagerScopeDoesNotBecomeAnUnscopedQuery() {
        UUID foreignSubject = UUID.randomUUID();
        assertProblem(() -> service.list(actor(Role.SUBJECT_MANAGER),"manager",foreignSubject,"private",ClassStatus.OPEN,1,20),404,"SUBJECT_OUT_OF_SCOPE");
        verifyNoInteractions(reads,classes,enrollments,subjects);
        assertDenialAudit();
    }

    @Test void currentSubjectOwnerMustMatchBeforeMutationEvenWhenScopeAllows() {
        when(subjects.getSubject(subjectId)).thenReturn(Optional.of(subject(subjectId,UUID.randomUUID())));
        assertProblem(() -> service.updateInfo(actor(Role.SUBJECT_MANAGER),classId,"Changed",null,null,7),404,"SUBJECT_OUT_OF_SCOPE");
        verify(lock).lockActive(org.mockito.ArgumentMatchers.eq(subjectId),org.mockito.ArgumentMatchers.any());
        verify(classes,never()).update(any(),anyLong());
        verifyNoInteractions(reads);
        assertDenialAudit();
    }

    @Test void subjectScopeDependencyFailureCannotExposeManagerClassData() {
        when(scope.listManagedSubjects(managerId)).thenThrow(new IllegalStateException("upstream secret"));
        assertProblem(() -> service.list(actor(Role.SUBJECT_MANAGER),"manager",null,null,null,1,20),503,"ACADEMICS_DEPENDENCY_UNAVAILABLE");
        verifyNoInteractions(reads,classes,enrollments,subjects);
    }

    @ParameterizedTest
    @CsvSource({"STUDENT,student", "TEACHER,teacher", "SUBJECT_MANAGER,manager"})
    void successfulListsPreserveScopedFilteringPagingAndCompleteClassDtos(Role role,String audience) {
        CourseClass expected = course(classId,ClassStatus.OPEN,teacherId);
        when(reads.listForStudent(studentId,"CS 101",ClassStatus.OPEN,7,14)).thenReturn(List.of(expected));
        when(reads.listForTeacher(teacherId,"CS 101",ClassStatus.OPEN,7,14)).thenReturn(List.of(expected));
        when(reads.listForManager(List.of(subjectId),"CS 101",ClassStatus.OPEN,7,14)).thenReturn(List.of(expected));
        when(reads.enrollmentCounts(List.of(classId))).thenReturn(Map.of(classId,new IAcademicsReadRepository.EnrollmentCounts(12,3)));

        var result = service.list(actor(role),audience,null,"CS 101",ClassStatus.OPEN,3,7);

        assertThat(result).singleElement().satisfies(view -> assertClassDto(view,12,3));
        if (role==Role.STUDENT) {
            verify(reads).listForStudent(studentId,"CS 101",ClassStatus.OPEN,7,14);
            verify(reads,never()).listForTeacher(any(),any(),any(),anyInt(),anyInt());
            verify(reads,never()).listForManager(any(),any(),any(),anyInt(),anyInt());
        } else if (role==Role.TEACHER) {
            verify(reads).listForTeacher(teacherId,"CS 101",ClassStatus.OPEN,7,14);
            verify(reads,never()).listForStudent(any(),any(),any(),anyInt(),anyInt());
            verify(reads,never()).listForManager(any(),any(),any(),anyInt(),anyInt());
        } else {
            verify(reads).listForManager(List.of(subjectId),"CS 101",ClassStatus.OPEN,7,14);
            verify(reads,never()).listForStudent(any(),any(),any(),anyInt(),anyInt());
            verify(reads,never()).listForTeacher(any(),any(),any(),anyInt(),anyInt());
        }
        verify(classes,never()).listBySubject(any(),anyInt(),anyInt());
        verifyNoInteractions(audit);
    }

    @Test void requestedManagedSubjectNarrowsTheQueryToThatSubjectOnly() {
        UUID anotherSubject = UUID.randomUUID();
        when(scope.listManagedSubjects(managerId)).thenReturn(List.of(subjectId,anotherSubject));
        assertThat(service.list(actor(Role.SUBJECT_MANAGER),"manager",subjectId,"C",ClassStatus.DRAFT,2,100)).isEmpty();
        verify(reads).listForManager(List.of(subjectId),"C",ClassStatus.DRAFT,100,100);
        verify(reads,never()).listForManager(eq(List.of(subjectId,anotherSubject)),any(),any(),anyInt(),anyInt());
    }

    @Test void emptyManagerScopeReturnsNoClassesWithoutCallingUnscopedDataQueries() {
        when(scope.listManagedSubjects(managerId)).thenReturn(List.of());
        assertThat(service.list(actor(Role.SUBJECT_MANAGER),"manager",null,null,null,1,20)).isEmpty();
        verify(reads,never()).listForManager(any(),any(),any(),anyInt(),anyInt());
        verify(reads,never()).listForStudent(any(),any(),any(),anyInt(),anyInt());
        verify(reads,never()).listForTeacher(any(),any(),any(),anyInt(),anyInt());
        verifyNoInteractions(classes,enrollments,subjects);
    }

    @ParameterizedTest
    @CsvSource({"STUDENT,student", "TEACHER,teacher", "SUBJECT_MANAGER,manager"})
    void successfulDetailReturnsSubjectTeacherAndCountsWithoutWriting(Role role,String audience) {
        when(reads.enrollmentCounts(List.of(classId))).thenReturn(Map.of(classId,new IAcademicsReadRepository.EnrollmentCounts(12,3)));
        assertClassDto(service.detail(actor(role),audience,classId),12,3);
        if (role==Role.SUBJECT_MANAGER) verify(authorization).authorize(actor(role),"CLASS_MANAGE",ResourceRef.subject(subjectId));
        else verify(authorization).authorize(actor(role),"CLASS_VIEW",ResourceRef.courseClass(classId));
        if (role==Role.STUDENT) verify(enrollments).find(classId,studentId);
        verifyNoInteractions(audit);
    }

    @Test void subjectManagerCanReadTeachingDetailWhenCurrentlyAssigned() {
        when(classes.find(classId)).thenReturn(Optional.of(course(classId,ClassStatus.DRAFT,managerId)));
        ClassView view = service.detail(actor(Role.SUBJECT_MANAGER),"teacher",classId);
        assertThat(view.id()).isEqualTo(classId);
        assertThat(view.teacher().id()).isEqualTo(managerId);
        assertThat(view.status()).isEqualTo(ClassStatus.DRAFT);
        verifyNoInteractions(scope);
    }

    @Test void draftWithNoTeacherUsesNullTeacherAndZeroCounts() {
        when(classes.find(classId)).thenReturn(Optional.of(course(classId,ClassStatus.DRAFT,null)));
        var view = service.detail(actor(Role.SUBJECT_MANAGER),"manager",classId);
        assertThat(view.teacher()).isNull();
        assertThat(view.activeStudentCount()).isZero();
        assertThat(view.removedStudentCount()).isZero();
        verify(accounts,never()).getContact(teacherId);
    }

    @ParameterizedTest
    @EnumSource(value=ClassStatus.class,names={"DRAFT","ARCHIVED"})
    void studentDetailHidesClassesThatAreNotOpen(ClassStatus status) {
        when(classes.find(classId)).thenReturn(Optional.of(course(classId,status,teacherId)));
        assertProblem(() -> service.detail(actor(Role.STUDENT),"student",classId),404,"CLASS_OUT_OF_SCOPE");
        verify(enrollments,never()).find(any(),any());
        verifyNoInteractions(reads,subjects);
    }

    @ParameterizedTest
    @ValueSource(booleans={false,true})
    void studentDetailHidesRemovedOrMissingEnrollment(boolean removed) {
        when(enrollments.find(classId,studentId)).thenReturn(removed?Optional.of(enrollment(studentId).remove(now)):Optional.empty());
        assertProblem(() -> service.detail(actor(Role.STUDENT),"student",classId),404,"CLASS_OUT_OF_SCOPE");
        verifyNoInteractions(reads,subjects);
        assertDenialAudit();
    }

    @Test void studentCannotRetrieveTheClassRosterEvenWhenEnrolled() {
        assertProblem(() -> service.students(actor(Role.STUDENT),"student",classId,1,20),403,"ROLE_DENIED");
        verify(enrollments,never()).listByClass(any(),anyBoolean(),anyInt(),anyInt());
        verifyNoInteractions(subjects,reads);
    }

    @ParameterizedTest
    @CsvSource({"TEACHER,teacher,true", "SUBJECT_MANAGER,manager,false"})
    void rosterReturnsStudentContactAndEnrollmentWithCorrectStatusScope(Role role,String audience,boolean activeOnly) {
        Enrollment value = enrollment(studentId);
        when(enrollments.listByClass(classId,activeOnly,5,5)).thenReturn(List.of(value));
        var result = service.students(actor(role),audience,classId,2,5);
        assertThat(result).containsExactly(new AcademicsDtos.Student(studentId,"STUDENT name","student@example.com",value.enrolledAt(),EnrollmentStatus.ACTIVE));
        verify(enrollments).listByClass(classId,activeOnly,5,5);
        verifyNoInteractions(reads,subjects,audit);
    }

    @Test void teacherStudentPopupReturnsOnlyTheActiveStudentInThisClass() {
        Enrollment value = enrollment(studentId);
        var result = service.student(actor(Role.TEACHER),classId,studentId);
        assertThat(result).isEqualTo(new AcademicsDtos.Student(studentId,"STUDENT name","student@example.com",value.enrolledAt(),EnrollmentStatus.ACTIVE));
        verify(enrollments).find(classId,studentId);
        verifyNoInteractions(reads,subjects,audit);
    }

    @ParameterizedTest
    @ValueSource(booleans={false,true})
    void teacherStudentPopupHidesRemovedOrUnknownStudentWithoutLookingUpContact(boolean removed) {
        when(enrollments.find(classId,studentId)).thenReturn(removed?Optional.of(enrollment(studentId).remove(now)):Optional.empty());
        assertProblem(() -> service.student(actor(Role.TEACHER),classId,studentId),404,"STUDENT_NOT_FOUND");
        verify(accounts,never()).getContact(studentId);
        assertDenialAudit();
    }

    @Test void staleTeacherCannotRetrieveStudentContactsAfterAssignmentChanged() {
        when(classes.find(classId)).thenReturn(Optional.of(course(classId,ClassStatus.OPEN,managerId)));
        assertProblem(() -> service.student(actor(Role.TEACHER),classId,studentId),404,"CLASS_OUT_OF_SCOPE");
        verifyNoInteractions(enrollments,reads,subjects);
        verify(accounts,never()).getContact(studentId);
    }

    @Test void managedSubjectSummaryCountsEveryStatusAndSuppliesZeroForAbsentStatus() {
        when(reads.listOfSubject(subjectId)).thenReturn(List.of(course(classId,ClassStatus.OPEN,teacherId),course(UUID.randomUUID(),ClassStatus.OPEN,teacherId),course(UUID.randomUUID(),ClassStatus.DRAFT,null)));
        SubjectSummary result = service.managedSubject(actor(Role.SUBJECT_MANAGER),subjectId);
        assertThat(result).isEqualTo(new SubjectSummary(subjectId,"CS101","Computer Science",SubjectStatus.ACTIVE,
                Map.of(ClassStatus.DRAFT,1L,ClassStatus.OPEN,2L,ClassStatus.ARCHIVED,0L)));
        verify(reads).listOfSubject(subjectId);
        verifyNoInteractions(audit);
    }

    @Test void managedSubjectsExcludesOutOfScopeSubjectsAndKeepsIndependentCounts() {
        UUID otherId = UUID.randomUUID(), foreignId = UUID.randomUUID();
        when(subjects.listManagedSubjects(managerId)).thenReturn(List.of(subject(subjectId,managerId),subject(otherId,managerId),subject(foreignId,UUID.randomUUID())));
        when(scope.isSubjectManager(managerId,otherId)).thenReturn(true);
        when(reads.listOfSubject(subjectId)).thenReturn(List.of(course(classId,ClassStatus.ARCHIVED,teacherId)));
        when(reads.listOfSubject(otherId)).thenReturn(List.of());
        var results = service.managedSubjects(actor(Role.SUBJECT_MANAGER));
        assertThat(results).extracting(SubjectSummary::id).containsExactly(subjectId,otherId);
        assertThat(results.get(0).classCounts()).containsEntry(ClassStatus.ARCHIVED,1L).containsEntry(ClassStatus.OPEN,0L);
        assertThat(results.get(1).classCounts()).containsExactlyInAnyOrderEntriesOf(Map.of(ClassStatus.DRAFT,0L,ClassStatus.OPEN,0L,ClassStatus.ARCHIVED,0L));
        verify(reads,never()).listOfSubject(foreignId);
        verifyNoInteractions(audit);
    }

    @Test void managedSubjectWrongScopeDoesNotRequestSubjectDetailsOrCounts() {
        UUID foreignId = UUID.randomUUID();
        assertProblem(() -> service.managedSubject(actor(Role.SUBJECT_MANAGER),foreignId),404,"SUBJECT_OUT_OF_SCOPE");
        verifyNoInteractions(subjects,reads);
        assertDenialAudit();
    }

    @ParameterizedTest
    @ValueSource(strings={"subject","teacher","counts"})
    void missingClassDetailDependencyFailsSafelyInsteadOfReturningPartialData(String dependency) {
        if (dependency.equals("subject")) when(subjects.getSubject(subjectId)).thenReturn(Optional.empty());
        else if (dependency.equals("teacher")) when(accounts.getContact(teacherId)).thenReturn(Optional.empty());
        else when(reads.enrollmentCounts(any())).thenThrow(new IllegalStateException("upstream secret"));
        String code = dependency.equals("subject")?"SUBJECT_UNAVAILABLE":dependency.equals("teacher")?"ACCOUNT_UNAVAILABLE":"ACADEMICS_DEPENDENCY_UNAVAILABLE";
        assertProblem(() -> service.detail(actor(Role.STUDENT),"student",classId),503,code);
        verify(audit).record(argThat(e -> e.result()==AuditResult.FAILURE));
    }

    @Test void studentCandidatesKeepActiveAndPendingStudentsFilterOthersAndCapAtTwenty() {
        var eligible = new ArrayList<AccountContact>();
        eligible.add(contact(UUID.randomUUID(),Role.STUDENT,AccountStatus.PENDING));
        for (int i=0;i<24;i++) eligible.add(contact(UUID.randomUUID(),Role.STUDENT,AccountStatus.ACTIVE));
        var response = new ArrayList<AccountContact>(List.of(contact(UUID.randomUUID(),Role.STUDENT,AccountStatus.DISABLED),contact(UUID.randomUUID(),Role.TEACHER,AccountStatus.ACTIVE)));
        response.addAll(eligible);
        when(accounts.findStudents("name or email",20)).thenReturn(response);
        var result = service.studentCandidates(actor(Role.SUBJECT_MANAGER),classId,"name or email");
        assertThat(result).containsExactlyElementsOf(eligible.stream().limit(20).map(this::person).toList());
        verify(accounts).findStudents("name or email",20);
        verifyNoInteractions(reads,subjects,audit);
    }

    @Test void teacherCandidatesKeepOnlyActiveTeachingRolesAndCapAfterFiltering() {
        var eligible = new ArrayList<AccountContact>();
        eligible.add(contact(UUID.randomUUID(),Role.SUBJECT_MANAGER,AccountStatus.ACTIVE));
        for (int i=0;i<24;i++) eligible.add(contact(UUID.randomUUID(),Role.TEACHER,AccountStatus.ACTIVE));
        var response = new ArrayList<AccountContact>(List.of(contact(UUID.randomUUID(),Role.TEACHER,AccountStatus.PENDING),contact(UUID.randomUUID(),Role.SUBJECT_MANAGER,AccountStatus.DISABLED),contact(UUID.randomUUID(),Role.ADMIN,AccountStatus.ACTIVE),contact(UUID.randomUUID(),Role.STUDENT,AccountStatus.ACTIVE)));
        response.addAll(eligible);
        when(accounts.findAccounts("lecturer",Set.of(Role.TEACHER,Role.SUBJECT_MANAGER),20)).thenReturn(response);
        assertThat(service.teacherCandidates(actor(Role.SUBJECT_MANAGER),"lecturer"))
                .containsExactlyElementsOf(eligible.stream().limit(20).map(this::person).toList());
        verify(accounts).findAccounts("lecturer",Set.of(Role.TEACHER,Role.SUBJECT_MANAGER),20);
        verifyNoInteractions(reads,subjects,classes,enrollments,audit);
    }

    @Test void studentCandidatesOutsideManagerScopeNeverCallAccountSearch() {
        when(scope.isSubjectManager(managerId,subjectId)).thenReturn(false);
        assertProblem(() -> service.studentCandidates(actor(Role.SUBJECT_MANAGER),classId,"private"),404,"CLASS_OUT_OF_SCOPE");
        verify(accounts,never()).findStudents(any(),anyInt());
        assertDenialAudit();
    }

    @ParameterizedTest
    @ValueSource(booleans={false,true})
    void failedCandidateLookupReturnsSafe503InsteadOfAnEmptyOrPartialList(boolean students) {
        if (students) when(accounts.findStudents(any(),anyInt())).thenThrow(new IllegalStateException("upstream secret"));
        else when(accounts.findAccounts(any(),anySet(),anyInt())).thenThrow(new IllegalStateException("upstream secret"));
        assertProblem(() -> {
            if (students) service.studentCandidates(actor(Role.SUBJECT_MANAGER),classId,"term");
            else service.teacherCandidates(actor(Role.SUBJECT_MANAGER),"term");
        },503,"ACADEMICS_DEPENDENCY_UNAVAILABLE");
        verify(audit).record(argThat(e -> e.result()==AuditResult.FAILURE && e.details().isEmpty()));
    }

    @ParameterizedTest
    @ValueSource(booleans={false,true})
    void excessiveCandidateSearchIsRejectedBeforeUpstreamSearch(boolean students) {
        assertProblem(() -> {
            if (students) service.studentCandidates(actor(Role.SUBJECT_MANAGER),classId,"q".repeat(201));
            else service.teacherCandidates(actor(Role.SUBJECT_MANAGER),"q".repeat(201));
        },400,"INVALID_QUERY");
        verify(accounts,never()).findStudents(any(),anyInt());
        verify(accounts,never()).findAccounts(any(),anySet(),anyInt());
    }

    @ParameterizedTest(name="{1} cannot invoke {0}")
    @MethodSource("forbiddenMutationRoles")
    void everyMutationRejectsNonManagerRolesBeforeRetrievingData(String operation,Role role) {
        assertProblem(() -> invokeMutation(operation,actor(role)),403,"ROLE_DENIED");
        verifyNoInteractions(classes,enrollments,reads,subjects,scope,lock,authorization,accounts);
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
        return Stream.of("create","updateInfo","assignTeacher","gradeDistribution","changeStatus","enroll","enrollBatch","removeEnrollment","activateEnrollment");
    }

    private void invokeMutation(String operation,Actor actor) {
        switch(operation) {
            case "create" -> service.create(actor,subjectId,"CS101-B","New class",null,null,teacherId);
            case "updateInfo" -> service.updateInfo(actor,classId,"Changed",null,null,7);
            case "assignTeacher" -> service.assignTeacher(actor,classId,managerId,7);
            case "gradeDistribution" -> service.gradeDistribution(actor,classId,false,7);
            case "changeStatus" -> service.changeStatus(actor,classId,ClassStatus.ARCHIVED,7);
            case "enroll" -> service.enroll(actor,classId,studentId);
            case "enrollBatch" -> service.enrollBatch(actor,classId,"student@example.com");
            case "removeEnrollment" -> service.enrollmentStatus(actor,classId,studentId,EnrollmentStatus.REMOVED);
            case "activateEnrollment" -> service.enrollmentStatus(actor,classId,studentId,EnrollmentStatus.ACTIVE);
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
        return new CourseClass(id,subjectId,"CS101-A","Algorithms",null,null,status,assignedTeacher,true,now.minusSeconds(3600),now,7);
    }
    private Enrollment enrollment(UUID id) {return Enrollment.create(classId,id,EnrollmentSource.MANUAL,now.minusSeconds(120));}
    private void assertClassDto(ClassView view,long active,long removed) {
        assertThat(view).isEqualTo(new ClassView(classId,new AcademicsDtos.Subject(subjectId,"CS101","Computer Science"),"CS101-A","Algorithms","","",ClassStatus.OPEN,
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
