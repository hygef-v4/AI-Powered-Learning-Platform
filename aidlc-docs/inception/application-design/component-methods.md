# Component Methods

Các chữ ký dưới đây là contract cấp cao; DTO, quy tắc và API chi tiết nằm trong Functional Design và code generation plan của từng unit (`aidlc-docs/construction/`).

## Identity & Access (U01)

```text
requestActivation(schoolEmail, clientContext) -> Accepted
verifyActivationOtp(schoolEmail, otp) -> OtpTicket          // bước 1: chỉ nhập OTP
activateAccount(otpTicket, newPassword) -> Session          // bước 2: mở sau khi OTP đúng; tự đăng nhập
authenticate(schoolEmail, password, clientContext) -> Session
refresh(refreshCookie) -> Session
logout(sessionId) -> void
requestPasswordReset(schoolEmail, clientContext) -> Accepted
verifyPasswordResetOtp(schoolEmail, otp) -> OtpTicket
resetPassword(otpTicket, newPassword) -> void
changePassword(actor, currentPassword, newPassword) -> Session
getProfile(actor) -> Profile
updateProfile(actor, profilePatch) -> Profile
listAccounts(admin, filter, page) -> Page<AccountSummary>
createAccount(admin, schoolEmail, displayName, role) -> Account
changeRole(admin, userId, role) -> Account
changeAccountStatus(admin, userId, status) -> Account
validateAccountImport(admin, csvFile) -> AccountImportResult
commitAccountImport(admin, csvFile) -> AccountImportResult
authorize(actor, action, resourceRef) -> AuthorizationDecision
countByRoleAndStatus() -> Map<Role, Map<Status, Long>>      // AccountLookupPort, UC 18
```

## Audit (U02)

```text
record(event) -> void
queryAudit(admin, filters, page) -> AuditPage
```

## File, Job & Event (U03)

```text
enqueue(jobType, payload, idempotencyKey) -> void      // có transaction thì gửi sau commit, không có thì gửi ngay; không có bảng job
registerHandler(jobType, queue, handler, onFailed) -> void   // unit sở hữu đăng ký trong worker
registerSweeper(jobType, pendingQuery) / registerScanner(name, scan) -> void
publishEvent(routingKey, event) -> void            // sau commit
store(actor, purpose, file) -> FileRef
attach(fileRef, actor, purpose) -> FileInfo
issueDownloadToken(fileId, accountId) -> DownloadToken
open(fileId) -> Stream
validateAvatar(fileRef, actor) -> fileId
```

## Academic & Learning Access (U04)

```text
createSubject(admin, command) -> Subject
updateSubject(admin, subjectId, patch, version) -> Subject
changeSubjectState(admin, subjectId, targetState) -> Subject   // lưu trữ khi mọi lớp ARCHIVED
assignSubjectManager(admin, subjectId, accountId) -> Subject
createClass(adminOrSubjectManager, subjectId, command) -> Class
assignTeacher(adminOrSubjectManager, classId, accountId) -> Class
updateClass(actor, classId, patch, version) -> Class
changeClassState(actor, classId, targetState, version) -> Class
setGradeDistribution(actor, classId, enabled) -> Class
enrollStudents(actor, classId, emailsOrIds) -> EnrollmentRowResult[]
removeEnrollment(actor, classId, studentId) -> Enrollment
setInviteCode(actor, classId, enabled, expiresAt) -> InviteCode
joinByInviteCode(student, code) -> Enrollment
listMyClasses(student) -> ClassList   // Đang học / Đã kết thúc
getStudentClass(student, classId) -> StudentClassView
isActiveStudent(accountId, classId) -> boolean
listActiveStudents(classId) -> AccountRef[]
countSubjectsByStatus() / countClassesByStatus() / countActiveEnrollments() -> Counts   // UC 18
```

## Content & RAG (U05)

```text
createModule(subjectManager, subjectId, title) -> Module      // trên Subject Detail; mọi lớp của môn dùng chung
updateModule(subjectManager, moduleId, patch) -> Module       // đổi tên, thứ tự, lưu trữ
uploadLessons(actor, moduleId, classId?, fileRefs[] | youtubeUrl) -> Lesson[]   // không classId (Subject Detail, CN môn) → học liệu của môn; có classId (Class Detail, GV) → của lớp
updateLesson(actor, lessonId, patch) -> Lesson                // đổi tên, thứ tự, lưu trữ theo quyền
rescan(actor, lessonId) -> Lesson
listModulesForSubject(subjectManager, subjectId) -> ModuleLessonTree   // Subject Detail: module và học liệu của môn
listForClass(classId) -> ModuleLessonTree                     // PublishedContentPort cho U04
issueLessonDownload(student, classId, lessonId) -> DownloadToken
retrieve(scope, query, k, requesterId) -> LessonPassage[]
postClassAnnouncement(teacher, classId, title, body) -> Announcement
listAnnouncements(actor, classId, page) -> AnnouncementWithLatestComments[]   // 2 bình luận mới nhất mỗi thông báo
commentOnAnnouncement(actor, announcementId, body) -> AnnouncementComment
listComments(actor, announcementId, page) -> AnnouncementComment[]
hideAnnouncementOrComment(teacher, id, reason) -> void
```

## Question Bank (U06)

```text
createDraft(actor, scope, kind, definition) -> BankItem      // kind = QUESTION | RUBRIC
activate(actor, bankItemId) -> BankItem
newDraftFrom(actor, activeId) -> BankItem
retire(actor, bankItemId) -> BankItem
clone(actor, bankItemId, targetScope) -> BankItem
search(actor, scope, filter) -> Page<BankItem>
pickRandom(actor, scope, filter, n, excludeIds) -> BankItem[]   // chọn ngẫu nhiên khi soạn đề
importItems(actor, scope, questionType, file) -> ImportRowResult[]
saveInlineQuestion(actor, assignmentId, definition) -> Question   // scope_type = ASSIGNMENT
createRubricForAssignment(actor, scope, criteria) -> Rubric   // tạo khi soạn đề
reviseRubric(actor, rubricId, criteria) -> Rubric             // phiên bản mới; bài DRAFT chuyển sang bản mới
cloneRubricForAssignment(rubricId, targetScope) -> Rubric     // nhân bản theo đề
score(rubricId, checkedItemIds) -> Score
```

## Payment & AI Credit (U07)

```text
createPayment(account, packageId, idempotencyKey) -> CheckoutLink
handlePayosWebhook(rawBody, signature) -> WebhookResult
checkPayment(orderCode) -> PaymentStatus                // PaymentScanner + PAYOS_CHECK
balance(account) -> Balance                             // tặng tháng đặt lại khi đọc
reserve(accountId, credits, purpose, attemptRef?) -> {reserved, fromFree}   // U13 gọi trong transaction
settle(accountId, reserved, fromFree, actualCredits) -> void            // trả phần dư hoặc trừ thêm, không âm
release(accountId, reserved, fromFree) -> void                          // AI lỗi hoặc giữ quá 30 phút
listCreditUsage(account, page) -> Page<CreditUsage>     // qua CreditUsagePort (U13 cài)
```

Các method thanh toán và số dư phục vụ tài khoản `ACTIVE` có vai trò `STUDENT`, `TEACHER`, `SUBJECT_MANAGER` hoặc `ADMIN`; không có sổ cái, phần giữ/trừ nằm trên `ai_suggestions`. `STUDENT` chỉ được dùng credit cho AI chấm attempt `PRACTICE` Text Essay/Diagram Essay của chính mình; mọi yêu cầu AI khác bị backend từ chối.

## Assessment, Types, Template & Copy (U08-U10)

```text
createAssignment(teacher, classId, type, gradingMode) -> Assignment   // bài trống; copy do U10
editComponents(actor, assignmentId, changes) -> Assignment          // khóa dòng, chỉ DRAFT
pickRandomQuestions(actor, assignmentId, filter, n) -> QuestionPreview[]
addAiQuestions(actor, assignmentId, suggestionId, kept[]) -> Assignment
review(actor, assignmentId) -> ReviewResult
publish(teacher, assignmentId, schedule, latePolicy, attempts) -> Assignment
updateSchedule(teacher, assignmentId, schedule) -> Assignment
retireAssignment(actor, assignmentId, reason) -> Assignment
newVersion(actor, assignmentId) -> Assignment
cloneAssignment(actor, assignmentId) -> Assignment
setTypeConfig(actor, assignmentId, config) -> TypeConfig
saveSkeleton(actor, assignmentId, blocks) -> Skeleton             // tự chia phần theo heading nhỏ nhất mỗi nhánh
saveQuestionRubric(actor, assignmentId, questionId, criteria) -> Assignment  // Text Essay: mỗi câu một rubric, điểm câu = tổng rubric
savePartRubric(actor, assignmentId, partId, criteria) -> SkeletonPart     // mỗi phần một rubric
requestSkeletonDraft(actor, assignmentId, params) -> AiSuggestion        // Diagram Essay, bài nhóm: AI soạn khung (U13)
applySkeletonDraft(actor, assignmentId, suggestionId) -> Skeleton         // xem trước rồi thay khung; gợi ý rubric từng phần điền sẵn
importSkeletonDocx(actor, assignmentId, docx) -> SkeletonPreview
previewStudentDocx(student, attemptId, docx) -> StudentBlockPreview
exportDocx(document) -> Stream
releaseTemplate(subjectManager, templateId) -> Assignment   // template = dòng assignments có subject_id
deleteTemplate(subjectManager, templateId) -> void          // CN môn hiện tại; đã phát hành thì WITHDRAWN
copyTemplateToClass(teacher, templateId, classId) -> Assignment      // bước 2 tạo bài, cùng dạng và chế độ
copyFromClass(teacher, sourceAssignmentId, targetClassId) -> Assignment // dạy cả hai lớp; rubric đi theo bài
diff(fromAssignmentId, toAssignmentId) -> AssignmentDiff
```

## Attempt, Group & Group Document (U11, U12, U14)

```text
startAttempt(student, assignmentId) -> AttemptSnapshot
saveAttempt(student, attemptId, content, contentVersion) -> SaveReceipt
submitAttempt(student, attemptId) -> SubmissionReceipt
requestPracticeAiGrading(student, attemptId) -> AiSuggestion   // UC 40: Student bấm chấm, U11 gọi PracticeGradingPort (U13)
saveClassGroups(teacher, classId, groups[], versions) -> ClassGroups
randomSplit(teacher, classId, maxSize) -> ClassGroupsPreview
requestLeaderChange(student, groupId, reason, proposedLeaderId?) -> LeaderChangeRequest
decideLeaderChange(teacher, requestId, decision) -> Group
assignSection(leader, groupDocumentId, sectionId, memberId) -> GroupDocument
claimSection(student, groupDocumentId, sectionId) -> GroupDocument
saveSectionDraft(student, groupDocumentId, sectionId, blocks, version) -> GroupDocument
markSectionDone(student, groupDocumentId, sectionId) -> GroupDocument   // thêm phần tử revisions
releaseSection(actor, groupDocumentId, sectionId) -> GroupDocument
submitGroupDocument(leader, groupDocumentId) -> SubmissionReceipt      // bất kỳ lúc nào trước hạn; ghi submitted_snapshot
```

## AI, Code Execution, Grading, Notification (U13, U15, U16)

```text
requestQuestionDraft(actor, target, params) -> AiSuggestion
requestSkeletonDraft(actor, target, params) -> AiSuggestion   // SKELETON_DRAFT cho U09
requestGradingProposal(teacher, evaluationId) -> AiSuggestion
requestGradingProposalBatch(teacher, evaluationIds) -> AiSuggestion[]   // chấm hàng loạt, xác nhận từng bài sau
runCode(actor, kind, ownerRef, files) -> CodeRunResult    // TRY | VERIFY | GRADE; ghi vào attempts/questions
gradeManually(teacher, evaluationId, rubricChecks, feedback, version) -> Evaluation   // checklist rubric; dùng đề xuất AI cũng lưu bằng hàm này
finalizeGrades(teacher, evaluationIds) -> FinalizeResult[]
publishGrade(teacher, evaluationId) -> Evaluation          // công bố một bài nộp đã chốt
publishGrades(teacher, evaluationIds) -> PublishResult[]  // công bố hàng loạt; lần đầu ghi assignments.grades_released_at
overrideGrade(teacher, evaluationId, score, reason) -> Evaluation
getGradebook(actor, classId) -> Gradebook
listNotifications(account, page) -> Page<Notification>
setEmailPreference(account, type, enabled) -> EmailPreferences   // accounts.email_preferences
getSubmissionProgress(actor, assignmentId) -> Progress
getAdminStatistics(admin) -> AdminStatistics
getGradeDistribution(student, classId) -> List<GradeDistribution>
exportGradebook(actor, classId, assignmentId, format) -> Stream
```
