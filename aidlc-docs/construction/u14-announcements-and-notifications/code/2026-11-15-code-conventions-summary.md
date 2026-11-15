# U14 — Áp dụng quy ước mã nguồn (2026-11-15)

Thay đổi chỉ đổi tên và định dạng lại theo [Shared Code Conventions](../../code-conventions.md). Hành vi, URL, JSON, JPQL/SQL, queue, routing key, sự kiện, job type, mã lỗi và message giữ nguyên. Danh mục đổi tên đầy đủ nằm trong [kế hoạch sinh mã](../../plans/code-generation-plan/2026-11-15-u04-u13-u14-code-conventions-code-generation-plan.md).

## Backend `announcements`

- Sửa 7 file main và 1 file test.
- Controller: `listClassAnnouncements`, `createAnnouncement`, `editAnnouncement`, `deleteAnnouncement`, `listMyAnnouncements`.
- Service:
  - method: `listAnnouncements`, `loadAnnouncement`, `requireVersion`, `saveAnnouncement`, `buildItemView`, `buildAuditMetadata`, `hashText`, `recordAudit`;
  - field: `announcementRepository`, `announcementAccessPolicy`, `classAccess`, `accountLookup`, `eventPublisher`.
- Repository: `findAnnouncement`, `listAnnouncements`, `insertAnnouncement`, `updateAnnouncement`; `em` → `entityManager`.
- Policy: `requireClassAccess`, `listReadableClasses`, `rejectAccess`.
- Khác kế hoạch: `Announcement.bounded` đổi thành `requireTextLength` thay vì `requireMaxLength`, vì method vừa cắt khoảng trắng vừa từ chối chuỗi rỗng lẫn chuỗi quá dài.
- Biến một chữ cái đổi thành tên đầy đủ (`announcement`, `courseClass`, `value`).

## Backend `notifications`

- Sửa 19 file main và 10 file test.
- Controller: `listNotifications`, `countUnreadNotifications`, `markNotificationRead`, `markAllNotificationsRead`, `streamNotificationEvents`; preference: `loadPreferences`, `updatePreferences`; exception handler: `handleInvalidRequest`, `handleDatabaseFailure`.
- Repository: `insertNotification`, `findNotification`, `countUnread`, `markRead`, `findPreferences`, `savePreference`, `insertEmailDelivery`, `lockEmailDelivery`, `listPendingEmails`, `listDeferredEmails`, `countReservedEmails`, `lockEmailBudget`, `insertReminder`, `deleteOldNotifications`.
- Service:
  - `createNotification`, `resolveAuthorizedChannel`, `buildChannel`, `runAfterCommit`;
  - `scheduleEmail`, `sendEmail`, `markEmailFailed`, `releaseDeferredEmails`;
  - `hasCapacity`, `calculateNextDay`, `isSameDay`, `isEligible`, `remindStudents`, `handleEvent`, `resolveTitle`;
  - `resolveRecipients` cùng các helper `list…`, `build…Link`, `readId`, `readOptionalId`, `readIds`;
  - `summarizeType`, `resolveDisplaySummary`, `buildStudentLink`, `parseAssignmentId`, `buildRetryExchange`.
- `RecipientResolver.openClass` đổi thành `findOpenClass`. Method trả `null` khi lớp không mở và nơi gọi dùng kết quả, nên giữ kiểu trả về như cũ.
- Listener: `receiveEventMessage`; field `notificationEventService`, `objectMapper`, `rabbitTemplate`.
- Giữ nguyên:
  - method hiện thực interface (`IJobHandler.jobType/queue/timeout/handle/onFailed`, `IPendingSweeper.findStalePending`, `IScheduledScanner.name/scan`);
  - override `TransactionSynchronization.afterCommit()`;
  - `equals(Object o)`, tham số lambda, `Logger log`.
- `ClassGroupServiceTest` (groups) gọi `RecipientResolver.readId` theo tên mới.
- Dòng code còn dài hơn 100 ký tự chỉ là chuỗi JPQL hoặc SQL.

## Frontend

- API:
  - `announcementsApi`: `listAnnouncements`, `createAnnouncement`, `editAnnouncement`, `deleteAnnouncement`; `describeAnnouncementError`; `enc` → `encodePathSegment`;
  - `notificationsApi`: `countUnread`, `listNotifications`, `markRead`, `markAllRead`, `loadPreferences` (`savePreferences` giữ nguyên vì đã đúng mẫu).
- Helper: `sanitizeMarkdownUrl`, `sanitizeNotificationLink`.
- Callback UI: `handleSubmit`, `handleToggleClass`, `handleClose`, `handleSave`, `handleRemove`, `handleReload`, `handleToggle`, `handleOpen`, `handleMarkAllRead`, `handleShowSettings`, `handleRefresh`.
- Sửa 8 file trong `frontend/src/features/{announcements,notifications}/` và 3 file test (khóa `vi.mock` đổi theo tên mới).

## Kiểm tra

- Backend:
  - `mvn -o -B clean test` (subjects, materials, announcements, notifications và `ClassGroupServiceTest`): 493 test, 0 lỗi; 40 test bỏ qua vì không có Docker;
  - khi chạy cả package `groups`, hai test tích hợp Testcontainers báo lỗi "Could not find a valid Docker environment" (do môi trường, không do thay đổi).
- So sánh literal trong diff: không đổi chuỗi nào.
- Frontend:
  - `tsc --noEmit` và ESLint sạch; Vitest bốn feature: 23 file, 178 test đạt;
  - chạy toàn bộ Vitest: 1087/1092 đạt. 5 test lỗi khi chạy song song: 4 test hết thời gian 5 giây, 1 test `groupdocs` không tìm thấy phần tử. 4 test thuộc grading, groupdocs, shared document (không import các feature đã sửa), 1 test thuộc `ClassAnnouncementsPage`. Chạy lại riêng 5 file đó: 72/72 đạt.
