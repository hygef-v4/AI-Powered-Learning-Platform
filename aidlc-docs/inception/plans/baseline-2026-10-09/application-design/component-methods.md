# Component Methods

Các chữ ký là contract cấp cao, cập nhật theo 73 UC ngày 2026-10-09. DTO/API và method đã đổi/bổ sung đã được phản ánh vào Functional Design/code plan tại Construction; existing contracts/code cần triển khai revision theo thiết kế hiện hành.

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
getProfile(actor) -> Profile                                  // UC 06
updateProfile(actor, profilePatch) -> Profile                 // UC 07: displayName/phoneNumber, không avatar/email/role/status
listAccounts(admin, filter, page) -> Page<AccountSummary>    // UC 59
getAccount(admin, userId) -> AccountDetail                    // UC 61
updateAccountInformation(admin, userId, patch, version) -> Account // UC 62
createAccount(admin, schoolEmail, displayName, role) -> Account
changeRole(admin, userId, role) -> Account
changeAccountStatus(admin, userId, status) -> Account
validateAccountImport(admin, csvFile) -> AccountImportResult
commitAccountImport(admin, csvFile) -> AccountImportResult
authorize(actor, action, resourceRef) -> AuthorizationDecision
countByRoleAndStatus() -> Map<Role, Map<Status, Long>>      // AccountLookupPort, UC 58
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
listSettings(admin, group, page) -> SettingPage              // UC 70
getSetting(admin, key) -> SettingDetail
updateSetting(admin, key, value, version) -> SettingDetail  // UC 71; audit nguyên tử
getSettingValue(key) -> TypedValue                          // SettingsPort, cache tối đa 30 giây
```

## Academic & Learning Access (U04)

```text
listSubjects(admin, filter, page) / getSubject(admin, subjectId)   // UC 64/66; related classes read-only
createSubject(admin, command) / updateSubject(admin, subjectId, patch, version) // UC 65/67
assignSubjectManager(admin, subjectId, accountId) -> Subject // chỉ SUBJECT_MANAGER ACTIVE
listManagedSubjects(subjectManager) -> Subject[]           // UC 53, R2
listManagedClasses(subjectManager, subjectId, page) / getManagedClass(subjectManager, classId) // UC 47/48
createClass(subjectManager, subjectId, command) -> Class    // UC 49, R2
assignTeacher(subjectManager, classId, accountId) -> Class  // UC 50; TEACHER/SUBJECT_MANAGER ACTIVE
updateClass(subjectManager, classId, patch, version) -> Class // UC 52, gồm lifecycle/distribution
enrollStudents(subjectManager, classId, studentIds) / removeEnrollment(subjectManager, classId, studentId) // UC 51
listAssignedClasses(teacher, page) / getAssignedClass(teacher, classId) // UC 31/32, R3/R4
listMyClasses(student) / getStudentClass(student, classId)  // UC 13/14, R5
isActiveStudent(accountId, classId) / listActiveStudents(classId)
listOpenClassesOf(accountId) -> ClassRef[]                  // feed announcements, current enrollment/teaching
countSubjectsByStatus() / countClassesByStatus() / countActiveEnrollments() // UC 58
```

Admin không tạo/sửa lớp hay enrollment; phân công quản lý môn không cấp quyền chấm lớp. Không mã mời.

## Content & RAG (U05)

```text
createModule(subjectManager, subjectId, title) / updateModule(subjectManager, moduleId, patch) // Material List, R2
uploadLessons(actor, moduleId, classId?, fileRefs[] | youtubeUrl) -> Lesson[] // hold cùng transaction tạo lesson; R2/R3/R4
quoteUploadCredit(actor) -> CreditQuote
updateLesson(actor, lessonId, patch) -> Lesson              // thông tin/archive, không thay tệp/link
listModulesForSubject(subjectManager, subjectId) / listForClass(classId) -> ModuleLessonTree
getLearningMaterial(actor, lessonId, classId?) -> MaterialView // UC 15; summary, scoped source
issueLessonDownload(actor, classId?, lessonId) -> DownloadToken
getLessonRef(lessonId) -> LessonRef                        // ContentRefPort U06/U08/U09/U11
retrieve(scope, query, k, requesterId, requestRef) -> LessonPassage[]
postClassAnnouncement(teacher, classId, title, body) -> Announcement // UC 36
listAnnouncements(actor, classId?, page) -> AnnouncementPage // UC 30, classes OPEN học/dạy
updateAnnouncement(teacher, announcementId, patch, version) / deleteAnnouncement(teacher, announcementId, version) // UC 36
```

Không comments hoặc rescan API; requestMaterialSummary(actor, lessonId, classId?, idempotencyKey) từ màn xem: quyền xem R2/R3/R4/R5, giữ credit của requester; upload không AI/credit. Worker summary/embedding và retry theo U05. Update/delete announcement không phát notification mới.

## Rubric & Subject Question Bank (U06)

```text
listSubjectQuestions(subjectManager, subjectId, filter, page) / getQuestion(subjectManager, questionId) // UC 56, R2
createDraft(subjectManager, subjectId, type, definition) / updateDraft(subjectManager, questionId, definition, version)
activate(subjectManager, questionId) / newDraftFrom(subjectManager, activeId) / retire(subjectManager, questionId)
deleteQuestion(subjectManager, questionId, version)         // UC 57, chưa dùng mới xóa; bản đã dùng giữ history
pickRandom(actor, subjectId, filter, n, excludeIds) -> QuestionVersion[] // selector U08 kiểm quyền bài
importItems(subjectManager, subjectId, type, file) -> ImportRowResult[]
saveInlineQuestion(actor, assignmentId, definition) / copyToAssignment(sourceQuestionId, targetAssignmentId) // ASSIGNMENT
ensureRubricForQuestion(assignmentId, questionId) / ensureRubricForPart(assignmentId, partId) // tự tạo trống khi soạn
getRubric(actor, rubricId) / saveRubric(actor, rubricId, criteria, version) // UC 46, bài DRAFT R2/R3/R4
lockForAssignment(assignmentId) -> void                    // còn rubric trống thì từ chối publish
score(rubricId, checkedItemIds) -> Score
```

Không ngân hàng lớp hoặc rubric dùng chung trong ngân hàng. Rubric tự tạo/xóa theo câu/phần; rubric khóa khi phát hành. Tên port/method chi tiết theo Functional Design của U06.

## Payment & AI Credit (U07)

```text
listCreditPackages(account) -> CreditPackage[]               // UC 09
getCreditPackageSettings(admin, filter, page) -> Page<CreditPackage> // UC 68
addCreditPackage(admin, command) -> CreditPackage              // UC 69
editCreditPackage(admin, packageId, patch, version) -> CreditPackage // UC 69; không sửa snapshot giao dịch
listPlatformPayments(admin, filter, page) -> Page<PaymentSummary> // UC 72, chỉ đọc
getPaymentResult(account, paymentId) -> PaymentResult          // UC 11, chỉ chủ giao dịch
createPayment(account, packageId, idempotencyKey) -> CheckoutLink
handlePayosWebhook(rawBody, signature) -> WebhookResult
checkPayment(orderCode) -> PaymentStatus                // PaymentScanner + PAYOS_CHECK
balance(account) -> Balance                             // tặng tháng đặt lại khi đọc
reserve(accountId, credits, purpose, attemptRef?) -> {reserved, fromFree}   // U13 gọi trong transaction
settle(accountId, reserved, fromFree, actualCredits) -> void            // trả phần dư hoặc trừ thêm, không âm
release(accountId, reserved, fromFree) -> void                          // hoàn toàn bộ chỉ khi actualCredits = 0; đã dùng thì settle, kể cả lỗi/quá hạn
listCreditUsage(account, page) -> Page<CreditUsage>     // qua CreditUsagePort (U13 cài)
```

Các method thanh toán và số dư phục vụ tài khoản `ACTIVE` có vai trò `STUDENT`, `TEACHER`, `SUBJECT_MANAGER` (Admin không có ví); không có sổ cái, phần giữ/trừ nằm trên `ai_suggestions`. `STUDENT` dùng credit cho AI chấm attempt PRACTICE Text/Diagram Essay của mình hoặc MATERIAL_SUMMARY qua U05 khi có quyền xem; EMBEDDING chỉ child của HOLD này. Các AI khác bị từ chối.

## Assessment, Types & Copy (U08, U09)

```text
listTeacherAssignments(actor, classId, filter, page) -> Page<AssignmentSummary> // UC 41
createAssignment(teacher, classId, type, gradingMode) -> Assignment   // bài lớp, vòng đời U08
editComponents(actor, assignmentId, changes) -> Assignment          // khóa dòng, chỉ DRAFT
pickRandomQuestions(actor, assignmentId, filter, n) -> QuestionPreview[]
addAiQuestions(actor, assignmentId, suggestionId, kept[]) -> Assignment
review(actor, assignmentId) -> ReviewResult
publish(teacher, assignmentId, schedule, latePolicy, attempts) -> Assignment
updateSchedule(teacher, assignmentId, schedule) -> Assignment
deleteDraftAssignment(actor, assignmentId, version) -> void   // UC 35/42–45: DRAFT chưa từng phát hành
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
listSubjectAssignments(subjectManager, subjectId, filter, page) -> AssignmentPage // R2, bài của môn
createSubjectAssignment(subjectManager, subjectId, type, mode) -> Assignment // không GROUP; lịch chung
createQuiz(actor, lessonId, command) -> Assignment          // quiz lớp hoặc môn, không lịch
copyFromClass(teacher, sourceAssignmentId, targetClassId) -> Assignment // dạy cả hai lớp; rubric đi theo bài
```

## Attempt, Group & Group Document (U11, U12, U14)

```text
listQuizPracticeHistory(student, classId?, page) -> QuizHistoryPage // UC 18
getQuizPracticeDetail(student, quizId, classId) / getQuizResult(student, attemptId) // UC 19/21
listQuizzesOfLesson(lessonId, classId) -> QuizSummary[]      // Learning Material
listStudentAssignments(student, classId, type, page) -> Page<AssignmentSummary> // UC 22: gộp bốn dạng bài, không quiz
getStudentAssignment(student, assignmentId) -> AssignmentDetail // UC 23
listSubmissionHistory(student, assignmentId, page) -> Page<SubmissionSummary> // UC 28
getMyGroup(student, classId) -> GroupView                      // UC 16, U12
startAttempt(student, assignmentId, classId) -> AttemptSnapshot
saveAttempt(student, attemptId, content, contentVersion) -> SaveReceipt
submitAttempt(student, attemptId) -> SubmissionReceipt
requestPracticeAiGrading(student, attemptId) -> AiSuggestion   // UC 29: Student bấm chấm, U11 gọi PracticeGradingPort (U13)
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
quote(accountId) -> CreditQuote
hold(task, actor, targetType, targetId, requestRef) -> HoldSnapshot
findHold(targetType, targetId, requestRef) -> HoldSnapshot?
begin(task, actor, target, requestRef, holdId?, scanClaimId?) -> UsageStart // RUN/REPLAY/BUSY/IN_PROGRESS/CLOSED
complete(ticket, tokens, cost, checkpoint?) / fail(ticket, usage?) -> CallSnapshot
release(holdId) -> HoldSnapshot // AiUsagePort U05 khai báo, U13 cài; settle đã dùng, hoàn toàn bộ khi chưa dùng
requestQuestionDraft(actor, target, params) -> AiSuggestion
requestSkeletonDraft(actor, target, params) -> AiSuggestion   // SKELETON_DRAFT cho U09
listStudentSubmissions(actor, classId, assignmentId, filter, page) -> Page<SubmissionSummary> // UC 37, R3/R4
requestGradingProposal(teacher, evaluationId) -> AiSuggestion    // UC 38: AI Grading Proposals
requestGradingProposalBatch(teacher, evaluationIds) -> AiSuggestion[]   // chấm hàng loạt, xác nhận từng bài sau
runCode(actor, kind, ownerRef, files) -> CodeRunResult    // TRY | VERIFY | GRADE; VERIFY/GRADE ở code_runs; kết quả mới nhất ở attempts.run_result
gradeManually(teacher, evaluationId, rubricChecks, feedback, version) -> Evaluation   // checklist rubric; dùng đề xuất AI cũng lưu bằng hàm này
finalizeGrades(teacher, evaluationIds) -> FinalizeResult[]
publishGrade(teacher, evaluationId) -> Evaluation          // công bố một bài nộp đã chốt
publishGrades(teacher, evaluationIds) -> PublishResult[]  // công bố hàng loạt; công bố theo bài/lớp, suy từ evaluations; không grades_released_at trên assignments
overrideGrade(teacher, evaluationId, score, reason) -> Evaluation
getGradebook(actor, classId) -> Gradebook
listNotifications(account, page) -> Page<Notification>
setEmailPreference(account, type, enabled) -> EmailPreferences   // accounts.email_preferences
getSubmissionProgress(actor, assignmentId) -> Progress
getAdminStatistics(admin) -> AdminStatistics
getGradeDistribution(student, classId) -> List<GradeDistribution>
exportGradebook(actor, classId, assignmentId, format) -> Stream
```

Vai trò teacher chỉ Teacher hoặc Subject Manager được giao dạy lớp R3/R4; subjectManager chỉ Subject Manager được giao môn R2. Admin không được dùng hai ngữ cảnh học thuật đó. Mọi command validate/version/audit; query kiểm scope hiện thời trước khi trả dữ liệu. Method catalogue cấp cao; chữ ký DTO chi tiết ở thiết kế unit, code/contracts còn cần revision.
