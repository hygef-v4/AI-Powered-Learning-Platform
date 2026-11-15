# Báo cáo rà soát độ "clean" codebase và kiến trúc — 12/11/2026

## Kết luận

**Tổng thể: khá sạch, chưa hoàn toàn clean.** Cấu trúc theo nghiệp vụ và tầng được giữ nhất quán ở cả backend lẫn frontend; typecheck và lint sạch, không còn TODO/FIXME, không có `console.log`, không có `any`. Còn **1 rủi ro kiến trúc cần xử lý (P2)**, **3 khoảng cách kiến trúc đã biết hoặc mới phát hiện (P3)** và một số nợ quy ước đặt tên/kích thước file.

Baseline: nhánh `feat/ai-prompt-guardrail`, HEAD `65fcc764`, kèm các thay đổi chưa commit của đợt sửa bug (`AiTaskJobHandler`, `QuotaReservationService`, `JobDispatcher`, `QuizTakingPage`...). Báo cáo không sửa mã ứng dụng.

| Mã | Ưu tiên | Vấn đề | Trạng thái |
|---|---|---|---|
| ARCH-01 | P2 | Contract stub bật mặc định ở mọi môi trường, stub phân quyền trả `true` | Rủi ro mới |
| ARCH-02 | P3 | Phụ thuộc vòng `groups` ↔ `groupdocs`, dùng thẳng model/repository/service của nhau | Mới |
| ARCH-03 | P3 | Còn JPA ở U04, U13 (module), U14; lộ exception Hibernate lên service | Đã ghi nhận trong quy định persistence |
| ARCH-04 | P3 | `backup` dùng nội bộ `files`; `files` dùng `settings.model` | Mới |
| CONV-01 | P4 | Nợ quy ước tên: repository tiền tố `Jdbc`, dependency đặt tên chung | Áp dụng dần khi chạm code |
| CONV-02 | P4 | File quá lớn, đặc biệt `DocumentEditor.tsx` 1.968 dòng | Nên tách khi sửa tiếp |
| HYG-01 | P4 | `backend/node_modules/.vitest-cache` bị commit vào git | Dọn nhanh |

## Kiểm tra đã chạy

| Kiểm tra | Kết quả |
|---|---|
| `npm run typecheck` (frontend) | Qua, exit 0 |
| `npm run lint` (frontend) | Qua, exit 0 |
| Backend `mvn test-compile` (image `maven:3.9-eclipse-temurin-17`, offline) | Qua, exit 0 (main + test) |
| Quét import chéo module backend/frontend | Xem ARCH-02, ARCH-04 |
| Quét JPA, SQL trong controller/service, tên interface/repository | Xem ARCH-03, CONV-01 |
| Quét TODO/FIXME, `System.out`, `console.log`, `any`, `@ts-ignore`, catch rỗng | Sạch (chỉ `System.out` hợp lệ trong CLI `BackupDecryptTool`) |

Không chạy lại toàn bộ test suite: báo cáo bug cùng ngày đã ghi nhận Maven verify 2342/2342 và frontend 1018/1018 trên cùng HEAD (chưa tính thay đổi chưa commit).

## Điểm đã sạch

- **Module hóa backend nhất quán**: 20 module nghiệp vụ và `shared`, mỗi module có `contract/controller/service/model` (+ `repository`, `integration`, `config`, `dto` khi cần). 16/20 module chỉ giao tiếp với module khác qua `contract`.
- **Phân tầng đúng**: không controller nào import repository; chỉ 1 service chạy SQL trực tiếp (xem CONV-01).
- **Interface đúng quy ước `I` prefix**, không còn hậu tố `Port`.
- **Frontend**: `shared/` không import `features/`; liên kết giữa feature phần lớn đi qua `slots`/`api`/`model`. Không có `any`, `@ts-ignore`; 16 `eslint-disable` đều có lý do cụ thể (`no-img-element`, `exhaustive-deps`).
- **Kiểm thử dày**: 275 file test backend, 146 file test frontend, cây thư mục test khớp cây source.
- **Tài liệu kiến trúc cập nhật**: quy ước code, quy định persistence ghi rõ khoảng cách JPA còn lại thay vì tuyên bố sai.

## ARCH-01 — Contract stub bật mặc định, stub phân quyền luôn cho phép

Vị trí: `backend/src/main/java/edu/aiplatform/*/contract/stub/*ContractStubs.java` (≈1.000 dòng), `shared/stub/Stubs.java`.

Mọi lớp stub dùng `@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)` và `@ConditionalOnMissingBean`. Không có file `application*.yml` hay cấu hình `infra/` nào đặt thuộc tính này về `false`, nên stub được phép kích hoạt ở production. Một số stub kiểm quyền trả `true` vô điều kiện, ví dụ:

- `IdentityContractStubs`: `ISubjectScope.isSubjectManager`, `IClassScope.isTeacherOf`, `isEnrolledStudent`.
- `AcademicsContractStubs`: `IClassAccess.isActiveStudent`, `isClassManager`; `getClassRef` trả lớp mẫu `OPEN`.
- `SubjectsContractStubs`: một kiểm tra trả `true`.

Hiện các unit thật đã cài nên stub thường không được dùng. Rủi ro xảy ra khi một bean thật không được tạo (đổi `@Profile`/`@ConditionalOn...`, đổi tên/tách interface, lỗi refactor): ứng dụng vẫn khởi động, chỉ log một dòng `WARN`, và kiểm quyền chuyển thành **cho phép tất cả**. Đây là kiểu lỗi fail-open khó phát hiện. `ContractStubsTest` chỉ kiểm stub có thể tắt và bị thay bởi bean thật, chưa có test khẳng định context đầy đủ không còn stub nào.

Hướng xử lý (chọn một):

1. Đổi `matchIfMissing = false`, chỉ bật stub trong profile test/dev; hoặc
2. Xóa stub của các interface đã có implementation thật (cần xác nhận từng interface); hoặc
3. Tối thiểu: thêm test khởi động context đầy đủ khẳng định không có bean nào được tạo từ `*ContractStubs`, và cho stub phân quyền trả `false` (fail-closed).

## ARCH-02 — Phụ thuộc vòng `groups` ↔ `groupdocs`

`groups` import trực tiếp nội bộ của `groupdocs` và ngược lại, không qua `contract`:

- `groups/service/ClassGroupService.java`, `MyGroupService.java` → `groupdocs.model.GroupDocument`, `GroupSection`, `groupdocs.repository.GroupDocumentRepository`, `groupdocs.service.GroupDocumentService`, `IGroupDocStreams`.
- `groups/controller/GroupController.java` → `groupdocs.service.GroupWorkQueryService` (và record lồng); `GroupExceptionHandler` → `groupdocs.controller.GroupDocController`, `groupdocs.service.GroupSubmissionService.SubmitInvalidException`.
- `groupdocs/service/GroupDocAccess`, `GroupDocumentService`, `GroupSubmissionService`, `GroupWorkQueryService`, `GroupWorkspaceService` → `groups.repository.GroupRepository`, `groups.model.ClassGroup`, `groups.service.GroupAccess`.

Tổng 28 import chéo. Hai module thực chất là một bounded context bị chia đôi; thay đổi repository/model của một bên phá bên kia, vi phạm quy tắc "unit khác sử dụng public `contract`". Hướng xử lý: hoặc gộp thành một module (có hai package con), hoặc đưa `ClassGroup`, `GroupAccess`, truy vấn tài liệu nhóm và exception cần dùng chung ra `contract` của module sở hữu và cắt chiều phụ thuộc ngược.

## ARCH-03 — Khoảng cách JPA còn lại

Khớp với bảng trong [quy định persistence](../persistence-conventions.md); nhắc lại để không bị coi là "đã clean":

| Unit | File còn JPA |
|---|---|
| U04 subjects | `ISubjectJpaRepository`, `SubjectJpaEntity`, `SubjectRepositoryAdapter` |
| U13 materials | `MaterialModuleRepositoryAdapter` |
| U14 announcements/notifications | `Announcement`, `Notification`, `NotificationPreference`, `DeadlineReminder`, `EmailDelivery`, hai `*RepositoryAdapter` |

Vấn đề mới cần lưu ý: chi tiết persistence lộ khỏi repository — `subjects/service/SubjectCommandService.java:73` bắt `org.hibernate.exception.ConstraintViolationException`; `notifications/controller/NotificationExceptionHandler.java:3` xử lý `jakarta.persistence.PersistenceException`. Khi chuyển JDBC, phải dịch lỗi tại repository để service/controller không đổi theo cơ chế DB. Đồng thời model U14 (`Announcement`, `Notification`...) đang mang annotation JPA, trái nguyên tắc "domain model độc lập ORM".

## ARCH-04 — Phụ thuộc nội bộ `backup` → `files`, `files` → `settings`

- `backup/config/BackupConfig.java` → `files.config.FilesProperties`, `files.integration.DriveClientFactory`.
- `backup/integration/DriveBackupStore.java` → `files.integration.DriveClientFactory`.
- `backup/integration/LocalBackupStore`, `UnconfiguredBackupStore`, `backup/service/BackupJobHandler`, `BackupRetentionService` → `files.service.StorageUnavailableException`; `BackupCleanupTarget` → `files.service.IDriveCleanupTarget`.
- `files/service/FileSettingDefinitions.java` → `settings.model.SettingValidator`.

Một chiều, không tạo vòng, nhưng vẫn vượt ranh giới contract. Hướng xử lý: đưa `DriveClientFactory`/cấu hình Drive dùng chung, `StorageUnavailableException`, `IDriveCleanupTarget` và `SettingValidator` lên `contract` (hoặc `shared`) của module sở hữu.

## CONV-01 — Nợ quy ước đặt tên (áp dụng dần, không đổi hàng loạt)

Theo mục 1 của quy ước, chỉ đổi khi sửa nghiệp vụ trong phạm vi liên quan. Ghi nhận để ưu tiên:

- Repository tiền tố `Jdbc` (quy ước cấm): `quizpractice/repository/JdbcQuizAttemptRepository`, `quizzes/repository/JdbcQuestionRepository`, `JdbcQuizAiDraftRepository`, `JdbcQuizRepository`.
- 130 field service không có hậu tố `Service` (ví dụ `AiAdminController.quota`, `AiTaskJobHandler.quota`, `AssignmentController.authoring`); 33 field đặt tên chung `service`/`repository` (ví dụ `AnnouncementController.service`).
- 19 method controller dùng tên chung `get/save/update/detail/...`.
- `ai/service/McpWriteRequestService.java` tự chạy SQL bằng `JdbcTemplate` trong service (quy định: SQL thuộc repository), tên field `jdbc`, `json`, `manager`, và nhiều câu lệnh không có khoảng trắng quanh `=`.
- Frontend: file chứa component nhưng tên `lowerCamelCase`: `assignmentNav.tsx`, `groupDocNav.tsx`, `moduleNav.tsx`, `subjectMutation.tsx`, `inputs.tsx`, `session.tsx` — cần xem từng file, file chỉ chứa tiện ích/hook thì hợp lệ.

## CONV-02 — File quá lớn

16/768 file Java và 12/471 file TS/TSX vượt 400 dòng. Đáng tách nhất:

| File | Dòng | Gợi ý |
|---|---|---|
| `frontend/src/shared/document/DocumentEditor.tsx` | 1.968 | Tách toolbar, xử lý bảng/ảnh, phím tắt thành hook/component riêng |
| `frontend/src/features/assignments/pages/AssignmentFormPage.tsx` | 867 | Tách các section form |
| `assignments/service/DocumentModelService.java` | 785 | |
| `assignments/service/DocxImporter.java` | 780 | |
| `assignments/service/AssignmentAiDraftService.java` | 777 | |
| `academics/service/AcademicsService.java` | 639 | Tách theo use case |
| `grading/service/GradingService.java` | 595 | |

Module `assignments` tập trung nhiều file lớn nhất (6/16 file Java > 400 dòng).

## HYG-01 — Vệ sinh repo

- `backend/node_modules/.vitest-cache/CACHEDIR.TAG` và `_metadata.json` đã được commit; thư mục này là cache sinh ra do chạy vitest sai thư mục. Nên `git rm --cached` và thêm `backend/node_modules/` vào `.gitignore`.
- Working tree đang có thay đổi chưa commit của đợt sửa bug 12/11 (10 file sửa, 1 file mới `QuotaUnavailableException.java`). Nên hoàn tất/commit trước khi coi nhánh là sạch.
- `.gitignore` đã che `debug.log`, `.tmp/`, `backend/target/`, `.env`, `*.tsbuildinfo` — ổn.

## Đề xuất thứ tự xử lý

1. ARCH-01: tắt stub mặc định hoặc thêm test đảm bảo không stub nào chạy trong context đầy đủ (rủi ro bảo mật fail-open, công sửa nhỏ).
2. HYG-01: dọn cache bị commit (vài phút).
3. ARCH-02: quyết định gộp hay tách sạch `groups`/`groupdocs` — cần lựa chọn thiết kế của nhóm.
4. ARCH-03: lên kế hoạch migration JDBC cho U04, U13 module, U14 theo quy định persistence.
5. ARCH-04, CONV-01, CONV-02: xử lý dần khi chạm vào các file liên quan.

## Phạm vi chưa kiểm

Không chạy ứng dụng để xác nhận stub nào đang active thực tế; không đánh giá hiệu năng, bảo mật chuyên sâu, UI/accessibility; không chấm điểm từng method theo toàn bộ quy ước đặt tên (chỉ quét theo mẫu).
