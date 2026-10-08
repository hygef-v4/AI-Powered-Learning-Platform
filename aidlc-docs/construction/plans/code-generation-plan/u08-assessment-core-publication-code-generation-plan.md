# U08 Assessment Core & Publication - Code Generation Plan

**Bản tài liệu 2026-10-08**: UC 38; primary stories: US-ASM-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U08. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-ASM-001; khóa nội dung cho US-QBK-002 S2, S3.
- **Primary UC hiện hành**: UC 38. Supporting flows theo current-srs-contract.md.
- **Thiết kế nguồn**: `construction/u08-assessment-core-publication/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `ScheduledScanner`, `EventPublisherPort` | U03 | Dùng thật |
| `ClassAccessPort` | U04 | Dùng thật |
| `BankQueryPort`, `InlineQuestionPort`, `QuestionEditor`, `QuestionView` | U06 | Dùng thật |
| U08 cài `RubricOwnerPort` (U06 khai báo) | cho U06 | Thay adapter rỗng của U06: sửa rubric ở Question Bank thì bài `DRAFT` chuyển sang phiên bản mới (BR-U06-36) |
| `ContentRefPort` | U05 | Dùng thật (module/lesson trong bộ lọc chọn ngẫu nhiên và phạm vi yêu cầu AI) |
| `TypeConfigPort` | U09 (`C`) | Adapter tạm luôn đạt; U09 thay |
| `GroupReadinessPort` | U12 (`C`) | Dùng thật: U12 code trước U08 trong wave 2; U08 khai báo port và cắm `GroupReadinessService` của U12 |
| `CodeLabCheckPort` | U13 (`C`) | Adapter tạm luôn đạt (bỏ qua kiểm lời giải mẫu); U13 thay |
| `AssignmentLifecyclePort` | U11, U14 (`C`) | Adapter rỗng; U11, U14 (wave 3) thay |
| Copy template, copy bài lớp khác | U10 (wave 4) | `CreateAssignmentDialog` chỉ có "Bài trống"; U10 gắn hai lựa chọn copy vào |
| `AiDraftPort` | U13 (`C`) | Adapter tạm báo "AI chưa sẵn sàng"; ẩn nút AI khi chưa có; U13 thay |

### Dữ liệu U08 sở hữu

PostgreSQL `assignments` (gồm lịch mở/đóng), `assignment_questions`; scanner mở/đóng theo lịch trong worker; event `assignment.opened` (chỉ cho thông báo U16).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  assessments/
    api/                AssignmentController, ScheduleController, StudentAssignmentController, DTO
    application/        AssignmentService, ReviewValidator, ScheduleService,
                        AssignmentQueryService, StudentViewMapper
    domain/             Assignment, AssignmentQuestion, trạng thái,
                        SubmissionWindow
    infrastructure/     JPA repository, PassTypeConfigCheckAdapter, PassCodeLabCheckAdapter,
                        UnavailableAiDraftAdapter, NoopLifecycleAdapter, GroupReadinessAdapter (gọi U12)
    worker/             AssignmentScheduleScanner
    port/               AssignmentQueryPort, AssignmentExtensionPort, TypeConfigPort, AiDraftPort,
                        AssignmentLifecyclePort, GroupReadinessPort, CodeLabCheckPort
/backend/src/main/resources/db/migration/assessments/
/frontend/src/app/teaching/classes/[id]/assignments/
/frontend/src/app/teaching/assignments/[id]/
/contracts/openapi/assessments.yaml
/contracts/messages/assignment-events.json
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - Biến cấu hình U08 theo `logical-components.md` §3; `TZ=UTC` cho `backend`/`worker`.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain: `Assignment` (aggregate, khóa từ `SCHEDULED`, gồm lịch), `AssignmentQuestion`, `SubmissionWindow` (P1, P3, BR-U08-10…14, 33).
- [ ] **Bước 3** - Port và adapter: `AssignmentQueryPort`; port khai báo `TypeConfigPort` (luôn đạt), `CodeLabCheckPort` (luôn đạt), `AiDraftPort` (báo chưa sẵn sàng), `AssignmentLifecyclePort` (rỗng), `GroupReadinessPort` (cắm U12); `AssignmentExtensionPort` cho U09, U10, U15, U16 ghi cấu hình loại bài, template/lineage, công bố điểm, mốc nhắc hạn. U08 sở hữu `gradingMode` và ràng buộc năm dạng bài.
- [ ] **Bước 4** - `AssignmentService`: tạo (chọn dạng và chế độ trước, BR-U08-17), thêm câu từ ngân hàng (chọn tay hoặc ngẫu nhiên qua `BankQueryPort.pickRandom`, BR-U08-18) hoặc câu riêng (qua `InlineQuestionPort`), cài `RubricOwnerPort.repoint` qua `TypeConfigPort.repointRubric` (rubric do U09 tạo, BR-U08-16, BR-U06-36), nhân bản rubric qua `TypeConfigPort.copy` khi nhân bản/tạo version (BR-U06-35), điểm, sửa, xóa nháp, nhân bản, tạo version mới sau khi ngừng giao/đóng, audit (F1, F7, BR-U08-01, 10…15, 41…44).
- [ ] **Bước 5** - AI draft: gọi `AiDraftPort`, lưu câu giữ lại thành câu riêng của bài qua `InlineQuestionPort` (F2, BR-U08-21).
- [ ] **Bước 6** - `ReviewValidator` (câu, điểm; cấu hình và rubric qua `TypeConfigPort`; `CodeLabCheckPort`) và duyệt (F3, P5, BR-U08-20, 22).
- [ ] **Bước 7** - `ScheduleService`: phát hành (ghi lịch, kiểm lịch/nộp trễ/số lượt, bài nhóm hỏi `GroupReadinessPort`, khóa bài), sửa lịch, ngưng giao gọi `onRetired` trong transaction, audit (F4, F6, F7, BR-U08-02, 30…34, 40).
- [ ] **Bước 8** - `AssignmentScheduleScanner` đăng ký với U03 (UPDATE có điều kiện mỗi phút; mở bài gọi `AssignmentLifecyclePort.onOpened` trong transaction rồi phát `assignment.opened` sau commit; đóng bài theo `closes_at`/`late_until`) (F5, P2, P6, BR-U08-35, 36). Port khai báo ở Bước 3 kèm adapter rỗng tới khi U11/U14 có.
- [ ] **Bước 9** - `AssignmentQueryService`, `StudentViewMapper` và `AssignmentExtensionService` (cài `AssignmentExtensionPort`: chỉ cho U09 ghi `config` và điểm câu Text Essay theo rubric khi bài `DRAFT`, U10 ghi `subject_id`/`source_assignment_id`/trạng thái template, U15 ghi `grades_released_at` bài `GRADED`, U16 ghi `reminder_sent_at`) (F8, P3, P4, BR-U08-03).
- [ ] **Bước 10** - Unit test mọi `BR-U08-xx`, gồm ranh giới `closes_at`/`late_until`, bài đã phát hành không sửa được, chọn ngẫu nhiên không trùng câu đã có, nhân bản bài nhân bản đủ rubric từng câu/phần.
- [ ] **Bước 11** - Tóm tắt: `aidlc-docs/construction/u08-assessment-core-publication/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 12** - Flyway `V20260925_1500__u08_assessment.sql` theo `infrastructure-design.md` §2.
- [ ] **Bước 13** - JPA repository.
- [ ] **Bước 14** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test Testcontainers: scanner mở/đóng chạy lặp, đổi lịch rồi quét, phát hành bài đã phát hành bị chặn, hai người sửa cùng bài chạy lần lượt (khóa dòng), event gửi sau commit.
- [ ] **Bước 15** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 16** - `/contracts/openapi/assessments.yaml` và schema event.
- [ ] **Bước 17** - Controller + DTO + validation (giờ nhận ISO 8601 có offset).
- [ ] **Bước 18** - Test MockMvc: phát hành sai lớp bị từ chối và audit, người học không thấy đáp án, không thấy bài `SCHEDULED`/`RETIRED`, sửa bài đã phát hành trả `409`.
- [ ] **Bước 19** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 20** - `AssignmentListPage` (`CreateAssignmentDialog`: dạng + chế độ rồi nguồn; lượt này chỉ "Bài trống", U10 gắn copy), `AssignmentEditorPage` (`QuestionList`, `AddFromBankDialog` có tab chọn ngẫu nhiên, `InlineQuestionEditor`, `TypeConfigSlot`).
- [ ] **Bước 21** - `AiDraftDialog` (ẩn khi AI chưa sẵn sàng), `PreviewDialog`, `ReviewButton`.
- [ ] **Bước 22** - `PublishDialog`, `ScheduleSection` (sửa lịch, ngưng giao), `CloneButton`, `NewVersionButton`.
- [ ] **Bước 23** - Test frontend: cảnh báo khóa khi phát hành, kiểm lịch phía client, giờ hiển thị Việt Nam.
- [ ] **Bước 24** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 25** - Cập nhật `README.md`: vòng đời bài, lịch mở/đóng, cách U09 cài `TypeConfigPort`, cách U11 dùng `isSubmissionOpen`.
- [ ] **Bước 26** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-ASM-001 S1 (UC 38, 39, 40, 41, 42, 43) | 6, 7, 8, 21, 22 |
| US-ASM-001 S2 | 6, 7, 18 |
| UC 38, 39, 40, 41, 42, 43 | 9, 20 |
| US-QBK-002 S2, S3 (khóa nội dung) | 2, 4, 10, 18 |
| FR-006 (AI draft vào bản nháp) | 5, 21 |

## 5. Ngoài phạm vi

- Cấu hình riêng từng loại bài (U09); template/copy (U10); lượt làm (U11); nhóm (U12); AI thật (U13). Simulation Exam đã rút.

## 6. Revision implementation scope - 2026-10-08
- [ ] R3/R4 cho Assignment List và CRUD/lifecycle; Admin không assignment không đọc/sửa bài dạy, Admin/SM được giao dạy thực hiện như Teacher.
- [ ] UC 38 danh sách Teacher, UC 39–43 phần loại bài do U09 chủ trì; giữ supporting review/publish/version/copy.
