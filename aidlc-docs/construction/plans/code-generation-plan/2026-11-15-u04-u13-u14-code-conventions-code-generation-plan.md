# U04/U13/U14 — áp dụng quy ước mã nguồn: kế hoạch Code Generation

Dựa trên [yêu cầu CC-01…06](../../../inception/requirements/2026-11-15-u04-u13-u14-conventions.md) và [execution plan](../../../inception/plans/2026-11-15-u04-u13-u14-conventions-execution-plan.md), cả hai người dùng duyệt lúc 2026-11-15T09:29:55Z. Nhánh `fix/code-conventions-u04-u13-u14`. Một kế hoạch gộp cho 3 unit, làm tuần tự.

## Quy tắc áp dụng chung

- **Field và tham số dependency**: dùng tên type viết `lowerCamelCase`; với interface thì bỏ tiền tố `I`.
  - Ví dụ: `ISubjectRepository subjectRepository`, `IClassAccess classAccess`, `IAccountLookup accountLookup`, `IEventPublisher eventPublisher`, `ISseHub sseHub`, `IArtifact artifact`, `IJob job`, `IQuizQuery quizQuery`, `SubjectAuditRecorder subjectAuditRecorder`.
  - Tham số constructor đổi cùng tên với field.
- **Tham số một chữ cái** (`m`, `v`, `a`, `n`, `e`, `c`, `l`, `r`) trong chữ ký method: đổi thành tên nghiệp vụ như `material`, `version`, `announcement`, `notification`, `delivery`, `courseClass`. Lambda ngắn được giữ.
- **Method domain model** tác động lên chính đối tượng (`Subject.archive`, `Subject.reopen`, `Material.revise`, `Announcement.edit/delete`): giữ nguyên, vì đối tượng của động từ chính là receiver. Chỉ đổi tên mơ hồ hoặc tên danh từ.
- **Giữ nguyên** các tên sau:
  - URL, HTTP method, JSON, SQL, migration, event, MCP tool, queue, job type.
  - Contract `subjects.contract` và các method implement contract của unit khác: `ISubjectScope`, `IArtifactDownloadPolicy`, `IMcpToolProvider` (`tools`, `call`), `IJobHandler`/`IPendingSweeper` (`jobType`, `queue`, `timeout`, `handle`, `onFailed`, `findStalePending`), `IQuizLifecycleListener`, `IReferenceMaterial`, `IScheduledScanner` (`name`, `scan`), `IAssignmentSourceTextExtractor`.
  - Method framework: SAX/Tika, `TransactionSynchronization.afterCommit()`, `close()` của `@PreDestroy`, `equals`/`hashCode`.
  - Tên method `@Bean`, vì tên bean suy ra từ tên method: `subjectQueryStub`, `subjectClassesStub`, `notificationDeclarables`.
  - Spring Data derived query, accessor record và static factory: `Subject.create`, `Announcement.create`, `YoutubeVideo.parse`, `MaterialViews.Version.of`, `YoutubeVideo.url()`.
  - Logger `log`: 37/38 file của dự án đang dùng tên này, nó không phải hằng giá trị.
- **Callback UI frontend**: đặt `handle...` (`submit` → `handleSubmit`, `save` → `handleSave`, `close` → `handleClose`…). Helper không phải callback đặt theo động từ + đối tượng.
- **Dòng code** dài khoảng 100 ký tự trở xuống. SQL, literal và URL được phép dài hơn.

## Danh mục đổi tên — U04 `subjects`

| File | Cũ → mới |
|---|---|
| SubjectAdminController | field `reads/commands/statuses` → `subjectAdminService/subjectCommandService/subjectStatusService`; `list` → `listSubjects`, `create` → `createSubject`, `detail` → `loadSubject`, `classes` → `listSubjectClasses`, `information` → `updateSubjectInformation`, `manager` → `changeSubjectManager`, `status` → `changeSubjectStatus` |
| SubjectExceptionHandler | `api` → `handleApiException`, `invalid` → `handleInvalidRequest`, `stale` → `handleStaleVersion`, `integrity` → `handleIntegrityViolation`, `storage` → `handleStorageFailure`, `unexpected` → `handleUnexpectedFailure`, `problem` → `createProblem` |
| SubjectRequests | `create` → `parseCreate`, `information` → `parseUpdateInformation`, `manager` → `parseChangeManager`, `status` → `parseChangeStatus`, `query` → `validateListQuery`, `fields` → `requireAllowedFields`, `require` → `requireField`, `text` → `readText`, `uuid` → `readUuid`, `version` → `readVersion`, `invalid` → `createInvalidRequest` |
| SubjectResponses | `detail` → `mapSubjectDetail` |
| Subject | `changed` → `copyWithChanges` |
| ISubjectRepository + SubjectRepositoryAdapter | `existsByCode` → `hasSubjectWithCode`, `findPage` → `findSubjectPage`, `count` → `countSubjects`, `countByStatus` → `countSubjectsByStatus`, `findManagedBy` → `findSubjectsManagedBy`, `insert` → `insertSubject`, `update` → `updateSubject`; field `rows` → `subjectJpaRepository`; `snapshot` → `mapToSubject`, `pattern` → `buildLikePattern` |
| ISubjectJpaRepository, SubjectJpaEntity | `findPage` → `findSubjectPage` (@Query); `forInsert` → `createFrom`, `toDomain` → `mapToSubject` |
| SubjectAdminAccess | field `audit` → `subjectAuditRecorder`; `require` → `requireAccess`, `authorizationUnavailable` → `createAuthorizationUnavailable`, `reject` → `rejectAccess` |
| SubjectAdminService | field `repository/access/accounts/classes` → `subjectRepository/subjectAdminAccess/accountLookup/subjectClassesReader`; `list` → `listSubjects`, `detail` → `loadSubject`, `classes` → `listSubjectClasses`, `subject` → `requireSubject`, `managers` → `loadManagers` |
| SubjectAuditRecorder | `accessDenied/created/informationUpdated/managerChanged/statusChanged` → `recordAccessDenied/recordCreated/recordInformationUpdated/recordManagerChanged/recordStatusChanged`, `record` → `recordEvent` |
| SubjectClassesReader | field `provider` → `subjectClasses`; `counts` → `countClasses`, `classes` → `listClasses`, `read` → `readWithTimeout` |
| SubjectCommandService | field như AdminService, `audit` → `subjectAuditRecorder`; `create` → `createSubject`, `archivedSubject` → `createArchivedSubjectError`, `lockManager` → `lockManagerAccount`, `subject` → `requireSubject`, `save` → `saveSubject` |
| SubjectStatusService | field `repository/access/classes/audit` → `subjectRepository/subjectAdminAccess/subjectClassesProvider/subjectAuditRecorder`; `classesUnavailable` → `createClassesUnavailableError` |
| SubjectMutationGuard, SubjectQueryService, SubjectScopeService | field `repository` → `subjectRepository`, `audit` → `subjectAuditRecorder`; `reference` → `mapToSubjectRef` |
| SubjectLifecycleBudget, SubjectsContractStubs | `remaining` → `calculateRemaining`; `classesUnavailable` → `createClassesUnavailableError` |

## Danh mục đổi tên — U13 `materials`

| File | Cũ → mới |
|---|---|
| Class/Subject/My/Material/MaterialModule controller | field `commands/queries/modules` → `materialService/materialQueryService/materialModuleService`. Class: `list/create/reorder` → `listClassMaterials/createClassMaterial/reorderClassMaterials`. Subject: `listSubjectMaterials/createSubjectMaterial/reorderSubjectMaterials`. My: `list` → `listMyMaterials`. Material: `detail` → `loadMaterial`, `update` → `updateMaterial`, `replace` → `replaceMaterialVersion`, `archive` → `archiveMaterial`, `download` → `issueDownloadToken`. Module: `subject` → `listSubjectModules`, `create` → `createModule`, `reorder` → `reorderModules`, `teaching` → `listClassModules`, `mine` → `listMyClassModules`, `rename` → `renameModule`, `delete` → `deleteModule`, `attach` → `attachQuiz`, `detach` → `detachQuiz` |
| MaterialExceptionHandler | `api/invalid/unexpected/problem` → `handleApiException/handleInvalidRequest/handleUnexpectedFailure/createProblem` |
| MaterialTextExtractor | field `artifacts` → `artifact`; `extract` → `extractText`, `parse` → `parseDocument` |
| Material | `inModule` → `moveToModule`, `withPresentation` → `changePresentation` |
| IMaterialRepository + adapter | `find` → `findMaterial`, `list` → `listMaterials`, `insert` → `insertMaterial`, `update` → `updateMaterial`, `versions` → `listVersions`, `version` → `findVersion`, `stalePending` → `listStalePendingVersions`; field `jdbc/json` → `jdbcClient/objectMapper`; tham số `m/v` → `material/version`; xuống dòng chuỗi `.param(...)` |
| IMaterialModuleRepository + adapter | `find` → `findModule`, `list` → `listModules`, `insert` → `insertModule`, `update` → `updateModule`, `delete` → `deleteModule`, `activeMaterials` → `countActiveMaterials`, `archivedMaterials` → `countArchivedMaterials`, `quizLinks` → `listQuizLinks`, `quizLink` → `findQuizLink`, `link` → `linkQuiz`, `unlink` → `unlinkModuleQuiz`; private `count` → `countByModule`, `module` → `mapModule`, `link` → `mapQuizLink`, `instant` → `convertInstant`; tham số `m/l/r/expected/from/to` → tên nghiệp vụ |
| MaterialAccessPolicy | field `subjects/classes` → `subjectQuery/classAccess`; `dependency` → `callDependency`, `allowed` → `isAllowed`, `subject` → `requireSubject`, `courseClass` → `requireClass`, `managesSubject` → `isSubjectManager`, `manageModules` → `requireModuleManagement`, `subjectList` → `requireSubjectListAccess`, `classList` → `requireClassListAccess`, `staffCanRead` → `canStaffRead`, `studentCanRead` → `canStudentRead`, `canManage` → `canManageMaterial`, `write` → `requireWriteAccess`, `read` → `requireReadAccess`, `matches` → `isMaterialOfClass`, `deny` → `denyAccess` |
| MaterialModuleService | field `modules/access/quizzes` → `materialModuleRepository/materialAccessPolicy/quizQuery`; `forSubject` → `listSubjectModules`, `forClass` → `listClassModules`, `create/rename/delete/reorder/attach/detach` → `createModule/renameModule/deleteModule/reorderModules/attachQuiz/detachQuiz`, `manageableQuiz` → `requireManageableQuiz`, `views` → `buildModuleViews` (tách lambda lồng sâu ra method), `writable` → `requireWritableModule`, `conflict` → `createVersionConflict`, `record` → `recordAudit` |
| MaterialQueryService | field `repository/access/artifacts` → `materialRepository/materialAccessPolicy/artifact`; `subjectList` → `listSubjectMaterials`, `classList` → `listClassMaterials`, `authorized` → `loadAuthorizedMaterial`, `detail` → `loadMaterial`, `download` → `issueDownloadToken`, `view` → `buildDetailView` |
| MaterialService | field `repository/modules/access/artifacts/jobs` → `materialRepository/materialModuleRepository/materialAccessPolicy/artifact/job`; `create/update/replace/archive/reorder` → `createMaterial/updateMaterial/replaceMaterialVersion/archiveMaterial/reorderMaterials`, `writable` → `requireWritableMaterial`, `module` → `requireModule`, `save` → `saveMaterial`, `conflict` → `createVersionConflict`, `source` → `buildVersion`, `enqueue` → `enqueueTextExtraction`, `record` → `recordAudit` |
| Còn lại | `MaterialTextExtractJobHandler`: `repository/extractor` → `materialRepository/materialTextExtractor`, `id/number` → `readMaterialId/readVersionNo`. `AssignmentSourceTextService.extractor`, `MaterialDownloadPolicy/MaterialMcp*Tools/ReferenceMaterialService` (`repository/queries`), `QuizDeletionHandler.moduleRepository` → tên theo type. Xuống dòng ở `Material`, `MaterialCommands`, `MaterialDownloadPolicy` |

## Danh mục đổi tên — U14 `announcements`, `notifications`

| File | Cũ → mới |
|---|---|
| AnnouncementController, MyAnnouncementController | field `service` → `announcementService`; `list/create/edit/delete` → `listClassAnnouncements/createAnnouncement/editAnnouncement/deleteAnnouncement`; My: `list` → `listMyAnnouncements` |
| IAnnouncementRepository + adapter | `find/list/insert/update` → `findAnnouncement/listAnnouncements/insertAnnouncement/updateAnnouncement`; field `em` → `entityManager`; tham số `a` → `announcement` |
| Announcement | `bounded` → `requireMaxLength` (đổi theo hành vi thực tế khi sửa) |
| AnnouncementAccessPolicy | field `classes` → `classAccess`; `require` → `requireClassAccess`, `list` → `listReadableClasses`, `denied` → `rejectAccess` |
| AnnouncementService | field `repository/access/classes/accounts/events` → `announcementRepository/announcementAccessPolicy/classAccess/accountLookup/eventPublisher`; `list/create/edit/delete` → `listAnnouncements/createAnnouncement/editAnnouncement/deleteAnnouncement`, `load` → `loadAnnouncement`, `checkVersion` → `requireVersion`, `save` → `saveAnnouncement`, `view` → `buildItemView`, `metadata` → `buildAuditMetadata`, `hash` → `hashText`, `record` → `recordAudit` |
| NotificationController | field `service/sse` → `notificationService/sseHub`; `list` → `listNotifications`, `unread` → `countUnreadNotifications`, `read` → `markNotificationRead`, `readAll` → `markAllNotificationsRead`, `events` → `streamNotificationEvents` |
| NotificationExceptionHandler, NotificationPreferenceController | `invalid/database` → `handleInvalidRequest/handleDatabaseFailure`; field `service` → `notificationPreferenceService`, `get/put` → `loadPreferences/updatePreferences` |
| NotificationEventListener | field `service/json/rabbit` → `notificationEventService/objectMapper/rabbitTemplate`; `receive` → `receiveEventMessage` |
| INotificationRepository + adapter | `insert` → `insertNotification`, `find` → `findNotification`, `list` → `listNotifications`, `unread` → `countUnread`, `read` → `markRead`, `readAll` → `markAllRead`, `preferences` → `findPreferences`, `preference` → `savePreference`, `addEmail` → `insertEmailDelivery`, `lockEmail` → `lockEmailDelivery`, `pending` → `listPendingEmails`, `deferred` → `listDeferredEmails`, `reserved` → `countReservedEmails`, `lockBudget` → `lockEmailBudget`, `reminder` → `insertReminder`, `deleteOld` → `deleteOldNotifications`; field `em` → `entityManager` |
| NotificationService | field `repository/access/emails/sse` → `notificationRepository/notificationAccess/emailDeliveryService/sseHub`; `create` → `createNotification`, `list` → `listNotifications`, `unread` → `countUnread`, `read` → `markRead`, `readAll` → `markAllRead`, `authorizedChannel` → `resolveAuthorizedChannel`, `channel` → `buildChannel`, private `afterCommit(Runnable)` → `runAfterCommit` |
| NotificationPreferenceService | field `repository/access` → `notificationRepository/notificationAccess`; `typesFor` → `listTypesForRole`, `defaultEnabled` → `isEnabledByDefault`, `get/put` → `loadPreferences/updatePreferences` |
| EmailDeliveryService, EmailCapPolicy | field `repository/cap/jobs/accounts/assignments` → `notificationRepository/emailCapPolicy/job/accountLookup/assignmentQuery`; `schedule` → `scheduleEmail`, `send` → `sendEmail`, `failed` → `markEmailFailed`, `releaseDeferred` → `releaseDeferredEmails`, `expired` → `isExpired`, `enqueue` → `enqueueEmail`; `available` → `hasCapacity`, `nextDay` → `calculateNextDay`, `sameDay` → `isSameDay` |
| DeadlineReminderScanner | field `assignments/classes/submissions/repository/notifications/transaction` → `assignmentQuery/classAccess/submissionQuery/notificationRepository/notificationService/transactionTemplate`; `eligible` → `isEligible`, `remind` → `remindStudents` |
| EmailSendJobHandler, NotificationMaintenanceScanner | field `delivery/emails/repository` → `emailDeliveryService/notificationRepository`; `id` → `readDeliveryId` |
| NotificationAccess, NotificationEventService | `self` → `requireSelf`, `denied` → `rejectAccess`; field `resolver/notifications` → `recipientResolver/notificationService`, `accept` → `handleEvent`, `title` → `resolveTitle` |
| RecipientResolver | field `classes/accounts` → `classAccess/accountLookup`; `resolve` → `resolveRecipients`, `assignmentGroups` → `resolveAssignmentGroups`, `admins/students/activeStudents/members` → `listAdmins/listStudents/listActiveStudents/listMembers`, `openClass` → `findOpenClass`, `gradeLink/groupLink/proposalLink/teacherGroupLink` → `buildGradeLink/buildGroupLink/buildProposalLink/buildTeacherGroupLink`, `id/optionalId/ids` → `readId/readOptionalId/readIds` |
| NotificationSummaries, AssignmentNotificationLink, NotificationTopology | `forType` → `summarizeType`, `forDisplay` → `resolveDisplaySummary`; `student` → `buildStudentLink`, `assignmentId` → `parseAssignmentId`; `retryExchange` → `buildRetryExchange` |

Tên chọn theo hành vi đọc được từ code. Nếu khi sửa thấy hành vi khác tên dự kiến (ví dụ `openClass` ném lỗi thay vì trả `Optional`), sẽ chọn tên khớp hành vi (`requireOpenClass`) và ghi lại trong summary.

## Danh mục đổi tên — frontend

| Feature | Cũ → mới |
|---|---|
| subjects | file `hooks/subjectMutation.tsx` → `hooks/useSubjectMutation.tsx`; `subjectProblem` → `readSubjectProblem`, `needsSubjectReload` → `shouldReloadSubject`; api `getSubjects/getSubject/getSubjectClasses/subjectPath` → `listSubjects/loadSubject/listSubjectClasses/buildSubjectPath`; `ManagerPicker.label` → `formatManagerLabel`; callback trong component → `handle...` |
| materials | file `components/moduleNav.tsx` → `components/ModuleNav.tsx`; `moduleContext` → `resolveModuleContext`, `sourceOf` → `resolveSource`; api `scopePath/materialPath/modulePath/outlinePaths/contextQuery/write` → `buildScopePath/buildMaterialPath/buildModulePath/buildOutlinePaths/buildContextQuery/sendWrite`; `materialApi.create/update/replace/archive/reorder/token` → `createMaterial/updateMaterial/replaceMaterialVersion/archiveMaterial/reorderMaterials/issueDownloadToken`; `moduleApi.create/rename/remove/reorder` → `createModule/renameModule/deleteModule/reorderModules`; `materialError` → `describeMaterialError`, `validYoutube` → `isValidYoutubeUrl`; helper `documentKind/pageNonce/noncedStyles/cellText/quizDetails/removalAction/removalMessage/download/storageKey/read/write/subscribe/pendingText/itemKey/itemTitle/itemIcon/itemHref/boardHref/fileDescription/materialHref` → động từ + đối tượng (`detectDocumentKind`, `readPageNonce`, `applyStyleNonce`, `formatCellText`, `describeQuiz`, `resolveRemovalAction`, `describeRemoval`, `downloadMaterial`, `buildStorageKey`, `readCollapsed`, `writeCollapsed`, `subscribeCollapse`, `describePending`, `buildItemKey`, `resolveItemTitle`, `resolveItemIcon`, `buildItemHref`, `buildBoardHref`, `describeFile`, `buildMaterialHref`) |
| announcements | `announcementsApi.list/create/edit/remove` → `listAnnouncements/createAnnouncement/editAnnouncement/deleteAnnouncement`; `announcementError` → `describeAnnouncementError`; `safeMarkdownUrl` → `sanitizeMarkdownUrl`; callback → `handle...` |
| notifications | `notificationsApi.count/list/read/readAll/preferences` → `countUnread/listNotifications/markRead/markAllRead/loadPreferences`; `safeNotificationLink` → `sanitizeNotificationLink`; callback → `handle...` |

Prop callback (`onSaved`, `onClose`…), React component, hook `use...` và tên class CSS giữ nguyên. Đồng thời cập nhật import ở 9 file ngoài feature và trong test.

## Các bước

- [x] 1. U04 backend main: đổi tên theo bảng U04, xuống dòng chỗ dài.
- [x] 2. U04 test (`backend/src/test/.../subjects`, `requirements/*`): đổi field `service/commands/statuses` và tên method được gọi; compile, chạy test `subjects`.
- [x] 3. U13 backend main: đổi tên theo bảng U13; định dạng lại `MaterialRepositoryAdapter`, `Material`, `MaterialService`, `MaterialCommands`, `MaterialDownloadPolicy`; tách lambda trong `MaterialModuleService`.
- [x] 4. U13 test và `requirements/*`; compile, chạy test `materials`.
- [x] 5. U14 `announcements` main và test.
- [x] 6. U14 `notifications` main và test; compile, chạy test `announcements`, `notifications`.
- [x] 7. Frontend `subjects` (đổi tên file bằng `git mv`) cùng caller.
- [x] 8. Frontend `materials` (`git mv moduleNav.tsx ModuleNav.tsx`) cùng caller.
- [x] 9. Frontend `announcements`, `notifications` cùng caller.
- [x] 10. Frontend test (`frontend/tests/features/...`, mock theo tên mới); chạy `tsc`, ESLint, Vitest cho các feature.
- [x] 11. Grep lại các mẫu vi phạm của lần rà soát; rà diff để chắc không đổi chuỗi URL, JSON, SQL, code lỗi hay message.
- [x] 12. Cập nhật tài liệu hiện hành nhắc tên cũ (code summary và as-built của U04/U13/U14); viết summary `aidlc-docs/construction/{u04,u13,u14}-*/code/2026-11-15-code-conventions-summary.md`.

Build and Test (`mvn -o -B verify` toàn bộ, frontend build) chạy ở stage sau.

## Extension

- **Security**: không đổi phân quyền, validation, audit hay log. Kiểm qua test hiện có.
- **Resiliency**: không đổi timeout, khóa, transaction hay retry.
- **PBT**: tắt.
