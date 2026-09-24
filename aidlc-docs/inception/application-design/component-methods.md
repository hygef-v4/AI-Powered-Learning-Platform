# Component Methods

Các chữ ký dưới đây là contract cấp cao; DTO/schema và business rule chi tiết được chốt tại Functional Design.

`token` trong `activateAccount` và `resetPassword` là mã OTP dùng một lần: gửi qua email, lưu dạng hash có TTL trong Redis, giới hạn số lần nhập sai và bị xóa ngay sau khi dùng thành công. Không có cơ chế token link riêng bên cạnh OTP. OTP kích hoạt chỉ được gửi khi người dùng gọi `requestActivation`; tạo hoặc nhập tài khoản không gửi email. `requestActivation` và `requestPasswordReset` luôn trả `Accepted` trung tính và bị giới hạn tần suất theo email và client.

## Identity & Access

```text
requestActivation(schoolEmail, clientContext) -> Accepted
activateAccount(token, newPassword) -> ActivationResult
authenticate(schoolEmail, password, clientContext) -> Session
logout(sessionId) -> void
requestPasswordReset(email) -> Accepted
resetPassword(token, newPassword) -> void
updateProfile(actor, profilePatch) -> Profile
assignRolesAndScopes(admin, userId, roleScopePatch) -> AuthorizationSnapshot
changeAccountStatus(admin, userId, status, reason) -> Account
authorize(actor, action, resourceRef) -> AuthorizationDecision
```
## Academic & Group

```text
createSubject(admin, command) -> Subject
createClass(admin, command) -> Class
assignInstructor(admin, classId, instructorId) -> Assignment
changeClassState(actor, classId, targetState) -> Class
enrollLearner(actor, classId, learnerId) -> Enrollment
joinByInviteCode(learner, code) -> Enrollment
createGroups(instructor, classId, groupCommands) -> GroupSet
appointLeader(instructor, groupId, learnerId) -> Group
requestLeaderChange(learner, groupId, reason, proposedLearnerId?) -> LeaderChangeRequest
decideLeaderChange(instructor, requestId, decision) -> Group
assignIndividualParts(instructor, groupAssignmentId, allocations) -> AllocationSet
```

## Content, Learning & Banks

```text
uploadSubjectMaterial(subjectManager, subjectId, fileRef, metadata) -> Material
registerYoutubeSource(actor, lessonId, videoOrPlaylistUrl) -> SourceRegistration
requestTranscriptIngestion(actor, sourceId) -> JobReference
publishClassContent(instructor, classId, contentVersionId) -> PublishedContent
searchAuthorizedContent(actor, scope, query) -> SearchResult
getAuthorizedClassContent(learner, classId) -> PublishedContentList
createRubric(actor, scope, rubricDraft) -> RubricVersion
createQuestion(actor, scope, questionDraft) -> QuestionVersion
createQuestionVersion(actor, questionStableKey, changeSet) -> QuestionVersion
publishQuestionVersion(actor, questionVersionId, effectivePolicy) -> VersionPublication
cloneBankItem(actor, itemVersionId, targetScope) -> DraftVersion
analyzeQuestion(actor, questionId, period) -> QuestionAnalytics
```

## Assessment & Submission

```text
createAssessmentDraft(author, scope, assessmentDraft) -> AssessmentVersion
reviseAssignedAssessment(instructor, assignmentStableKey, baseVersionId, changeSet) -> AssessmentVersion
reviewAssessment(author, assessmentVersionId) -> ReviewResult
publishClassAssessment(instructor, assessmentVersionId, classId, schedule) -> Publication
publishCommonAssessment(subjectManager, assessmentVersionId, subjectId, schedule) -> PublicationSet
publishSubjectTemplate(subjectManager, assessmentVersionId, subjectId) -> AssessmentTemplateVersion
copySubjectTemplate(instructor, templateVersionId, targetClassId) -> AssessmentVersion
copyAssessmentToClass(instructor, sourceAssessmentId, targetClassId) -> AssessmentVersion
copyRubricToClass(instructor, sourceRubricVersionId, targetClassId) -> RubricVersion
configureSimulationExam(instructor, assessmentVersionId, simulationPolicy) -> AssessmentVersion
startAttempt(learner, publicationId) -> AttemptSnapshot
saveAttemptDraft(learner, publicationId, payload, idempotencyKey) -> DraftReceipt
submitAttempt(learner, publicationId, payloadRef, idempotencyKey) -> SubmissionReceipt
submitDrawioXml(learner, publicationId, fullXmlArtifact, idempotencyKey) -> SubmissionReceipt
submitIndividualGroupPart(learner, allocationId, artifact, idempotencyKey) -> SubmissionReceipt
requestGroupComposite(instructor, groupAssignmentId, orderedPartVersions) -> JobReference
finalizeGroupComposite(instructor, compositeVersionId, selectionPatch) -> CompositeVersion
cloneAssessment(author, assessmentVersionId, targetScope) -> AssessmentVersion
retirePublication(author, publicationId, reason) -> Publication
```

## Grading, AI & Code Execution

```text
gradeDeterministic(submissionId) -> PreliminaryGrade
recordManualGrade(instructor, submissionId, gradeDraft) -> GradeDraft
requestAiGradeProposal(instructor, submissionId, rubricVersionId) -> JobReference
createCompactDrawioArtifact(aiJobId, fullXmlArtifactId) -> DerivedArtifact
reviewGradeProposal(instructor, proposalId, decisionPatch) -> GradeDraft
finalizeGrade(instructor, submissionId, finalGrade, reason?) -> FinalGrade
recordCompositeGrade(instructor, compositeVersionId, rubricResult) -> GroupGrade
finalizeMemberGrade(instructor, groupAssignmentId, learnerId, evidence, finalGrade, reason) -> FinalGrade
publishGrades(instructor, publicationId, gradeIds) -> PublicationResult
requestExtension(learner, publicationId, request) -> ExtensionRequest
requestRegrade(learner, gradeId, request) -> RegradeRequest
runSimilarityCheck(instructor, submissionIds) -> JobReference
runCode(actor, codeLabVersionId, sourceArtifact, mode) -> JobReference
generateAssessmentDraft(actor, scopedSources, generationRequest) -> JobReference
```

## Payment, Notification, Reporting & Audit

```text
createPaymentIntent(learner, productId, idempotencyKey) -> PaymentIntent
handleVerifiedWebhook(rawEvent, signature) -> WebhookResult
grantEntitlement(paymentEventId, learnerId, productId) -> Entitlement
reconcilePayments(admin, period) -> JobReference
queueNotification(eventId, recipients, templateData) -> NotificationBatch
getSubmissionReport(instructor, classId, filters) -> SubmissionReport
requestGradeExport(actor, scope, format) -> JobReference
compareAiAndFinalGrades(actor, scope, period) -> AiQualityReport
recordAudit(event) -> void
queryAudit(admin, filters, page) -> AuditPage
```

## File & Job Platform

```text
store(actor, purpose, file) -> Artifact
storeDerived(sourceArtifactId, purpose, bytes) -> Artifact
attach(artifactId, scopeType, scopeId, actor) -> Artifact
issueDownloadToken(artifactId, accountId) -> DownloadToken
open(artifactId) -> Stream
deleteDerived(artifactId) -> void
enqueue(jobType, payloadRef, policy) -> JobReference
claim(workerId, supportedTypes) -> JobLease
complete(jobId, resultRef) -> JobStatus
fail(jobId, failureClass, safeMessage) -> JobStatus
getJobStatus(actor, jobId) -> JobStatus
```
