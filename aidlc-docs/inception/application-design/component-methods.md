# Component Methods

Các chữ ký dưới đây là contract cấp cao; DTO, quy tắc và API chi tiết nằm trong Functional Design và code generation plan của từng unit (`aidlc-docs/construction/`).

## Identity & Access (U01)

```text
requestActivation(schoolEmail, clientContext) -> Accepted
activateAccount(otp, newPassword) -> ActivationResult
authenticate(schoolEmail, password, clientContext) -> Session
refresh(refreshCookie) -> Session
logout(sessionId) -> void
requestPasswordReset(email) -> Accepted
resetPassword(otp, newPassword) -> void
updateProfile(actor, profilePatch) -> Profile
changeRole(admin, userId, role) -> Account
changeAccountStatus(admin, userId, status, reason) -> Account
importAccounts(admin, csvFile) -> ImportResult
authorize(actor, action, resourceRef) -> AuthorizationDecision
```

## Audit, Job & Event (U02)

```text
recordAudit(event) -> void
queryAudit(admin, filters, page) -> AuditPage
enqueue(jobType, payload, idempotencyKey, runAt?) -> JobReference
getJobStatus(actor, jobId) -> JobStatus
publishEvent(routingKey, event) -> void            // sau commit
```

## File & Artifact (U03)

```text
store(actor, purpose, file) -> Artifact
attach(artifactId, scopeType, scopeId, actor) -> Artifact
issueDownloadToken(artifactId, accountId) -> DownloadToken
open(artifactId) -> Stream
validateAvatar(artifactId, actor) -> boolean
```

## Academic & Learning Access (U04)

```text
createSubject(admin, command) -> Subject
assignSubjectManager(admin, subjectId, accountId) -> Subject
createClass(admin, subjectId, command) -> Class
assignTeacher(admin, classId, accountId) -> Class
changeClassState(actor, classId, targetState, version) -> Class
enrollStudents(actor, classId, emailsOrIds) -> EnrollmentRowResult[]
removeEnrollment(actor, classId, studentId) -> Enrollment
setInviteCode(actor, classId, enabled, expiresAt) -> InviteCode
joinByInviteCode(student, code) -> Enrollment
listMyClasses(student) -> ClassList
isActiveStudent(accountId, classId) -> boolean
```

## Content & RAG (U05)

```text
createChapter(actor, scope, title) -> Chapter
createLesson(actor, chapterId, title) -> LessonDraft
editDraftItems(actor, lessonId, itemChanges) -> LessonDraft
publishLesson(actor, lessonId) -> LessonVersion
linkSubjectLesson(actor, classChapterId, subjectLessonId) -> ClassLessonLink
retryIngestion(actor, sourceDocumentId) -> JobReference
listPublishedContent(classId) -> PublishedContent
retrieve(scope, query, k, requesterId, requestRef) -> RagChunk[]
postClassAnnouncement(actor, classId, title, body) -> ClassAnnouncement
askClassQuestion(actor, classId, title, body) -> ClassQuestion
answerClassQuestion(actor, questionId, body) -> ClassAnswer
listClassDiscussion(actor, classId) -> ClassDiscussion
```

## Question Bank (U06)

```text
createDraft(actor, scope, itemType, definition) -> BankItem
activate(actor, bankItemId) -> BankItem
newDraftFrom(actor, activeId) -> BankItem
retire(actor, bankItemId) -> BankItem
clone(actor, bankItemId, targetScope) -> BankItem
search(actor, scope, filter) -> Page<BankItem>
importItems(actor, scope, questionType, file) -> ImportRowResult[]
score(rubricId, checkedItemIds) -> Score
```

## Payment & AI Credit (U07)

```text
createPayment(account, packageId, idempotencyKey) -> CheckoutLink
handlePayosWebhook(rawBody, signature) -> WebhookResult
reconcilePendingPayments(job) -> ReconcileResult
reserve(accountId, credits, requestRef) -> Reservation
settle(reservationId, actualCredits) -> void
release(reservationId) -> void
```

Các method thanh toán, số dư và sổ cái phục vụ tài khoản `ACTIVE` có vai trò `STUDENT`, `TEACHER`, `SUBJECT_MANAGER` hoặc `ADMIN`. `STUDENT` chỉ được dùng credit cho AI chấm attempt `PRACTICE` Text Essay/Diagram Essay của chính mình; mọi yêu cầu AI khác bị backend từ chối.

## Assessment, Types, Template & Copy (U08-U10)

```text
createAssignment(teacher, classId, type) -> Assignment
editComponents(actor, assignmentId, changes, version) -> Assignment
review(actor, assignmentId) -> ReviewResult
publish(teacher, assignmentId, classId, schedule, latePolicy, attempts) -> Publication
retirePublication(actor, publicationId, reason) -> Publication
newVersion(actor, assignmentId) -> Assignment
cloneAssignment(actor, assignmentId) -> Assignment
setTypeConfig(actor, assignmentId, config) -> TypeConfig
saveSkeleton(actor, assignmentId, blocks) -> Skeleton
importSkeletonDocx(actor, assignmentId, docx) -> SkeletonPreview
previewStudentDocx(student, attemptId, docx) -> StudentBlockPreview
exportDocx(document) -> Stream
releaseTemplate(subjectManager, templateId) -> TemplateRelease
copyTemplateToClass(teacher, templateId, classId) -> Assignment
copyToClass(teacher, assignmentId, targetClassId) -> Assignment
diff(fromAssignmentId, toAssignmentId) -> AssignmentDiff
```

## Attempt, Group & Group Document (U11, U12, U14)

```text
startAttempt(student, publicationId) -> AttemptSnapshot
saveAttempt(student, attemptId, content, contentVersion) -> SaveReceipt
submitAttempt(student, attemptId) -> SubmissionReceipt
saveClassGroups(teacher, classId, groups[], versions) -> ClassGroups
randomSplit(teacher, classId, maxSize) -> ClassGroupsPreview
requestLeaderChange(student, groupId, reason, proposedLeaderId?) -> LeaderChangeRequest
decideLeaderChange(teacher, requestId, decision) -> Group
addOrEditSubSection(leader, groupDocumentId, parentSectionId, title, orderNo) -> Section
assignSection(leader, sectionId, memberId) -> Section
claimSection(student, sectionId) -> Section
saveSectionDraft(student, sectionId, blocks, version) -> Section
markSectionDone(student, sectionId) -> SectionRevision
releaseSection(actor, sectionId) -> Section
submitGroupDocument(leader, groupDocumentId) -> GroupSubmission   // only when status = REVIEW
```

## AI, Code Execution, Grading, Notification (U13, U15, U16)

```text
requestQuestionDraft(actor, target, params) -> AiProposal
requestGradingProposal(teacher, submissionRef) -> AiProposal
runCode(actor, kind, ownerRef, files) -> CodeRun          // TRY | VERIFY | GRADE
gradeManually(teacher, gradeId, items, feedback, version) -> Grade
finalizeGrades(teacher, gradeIds) -> FinalizeResult[]
publishGrades(teacher, publicationId) -> PublishResult
overrideGrade(teacher, gradeId, score, reason) -> Grade
getGradebook(actor, classId) -> Gradebook
listNotifications(account, page) -> Page<Notification>
setEmailPreference(account, type, enabled) -> Preference
getSubmissionProgress(actor, publicationId) -> Progress
getStudentDashboard(student) -> StudentDashboard
exportGradebook(actor, classId, publicationId, format) -> Stream
```
