# U08 Assessment Core & Publication - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U08. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-ASM-001; khóa nội dung cho US-QBK-002 S2, S3. **Use case**: UC-ASM-01, UC-ASM-07.
- **Thiết kế nguồn**: `construction/u08-assessment-core-publication/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `JobPort`, `AuditPort`, `EventPublisherPort` | U02 | Dùng thật |
| `ClassAccessPort` | U04 | Dùng thật |
| `BankQueryPort`, `DefinitionValidationPort`, `QuestionEditor`, `QuestionView` | U06 | Dùng thật |
| `ContentRefPort` | U05 | Dùng thật (câu riêng gắn chương/bài) |
| `TypeConfigCheckPort` | U09 (`C`) | Adapter tạm luôn đạt; U09 thay |
| `AiDraftPort` | U13 (`C`) | Adapter tạm báo "AI chưa sẵn sàng"; ẩn nút AI khi chưa có; U13 thay |

### Dữ liệu U08 sở hữu

PostgreSQL `assignments`, `assignment_components`, `publications`; queue `jobs.u08.publication-open`, `jobs.u08.publication-close`; routing key `u08.assignment.*`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  u08/
    api/                AssignmentController, PublicationController, LearnerAssignmentController, DTO
    application/        AssignmentService, ReviewValidator, PublicationService,
                        AssignmentQueryService, LearnerViewMapper
    domain/             Assignment, AssignmentComponent, Publication, trạng thái,
                        SubmissionWindow
    infrastructure/     JPA repository, PassTypeConfigCheckAdapter, UnavailableAiDraftAdapter
    worker/             PublicationScheduleHandler
    port/               AssignmentQueryPort, TypeConfigCheckPort, AiDraftPort
/backend/src/main/resources/db/migration/u08/
/frontend/src/app/teaching/classes/[id]/assignments/
/frontend/src/app/teaching/assignments/[id]/
/contracts/openapi/u08-assessment.yaml
/contracts/messages/u08-assignment-events.json
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - Biến cấu hình U08 theo `logical-components.md` §3; `TZ=UTC` cho `backend`/`worker`.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain: `Assignment` (aggregate, khóa khi không `DRAFT`), `AssignmentComponent` (một nguồn), `Publication`, `SubmissionWindow` (P1, P3, BR-U08-10…14, 33).
- [ ] **Bước 3** - Port và adapter tạm: `AssignmentQueryPort`, `TypeConfigCheckPort`, `AiDraftPort`.
- [ ] **Bước 4** - `AssignmentService`: tạo, thêm từ ngân hàng/câu riêng, điểm, sửa, xóa nháp, nhân bản, lưu trữ, audit (F1, F7, BR-U08-01, 10…15, 41, 42).
- [ ] **Bước 5** - AI draft: gọi `AiDraftPort`, thêm câu giữ lại với `origin = AI` (F2, BR-U08-21).
- [ ] **Bước 6** - `ReviewValidator` và duyệt (F3, P5, BR-U08-20, 22).
- [ ] **Bước 7** - `PublicationService`: phát hành một lớp, kiểm lịch/nộp trễ/số lượt, khóa bài, tạo job, sửa lịch, ngưng giao, audit (F4, F6, F7, P2, BR-U08-02, 30…34, 40).
- [ ] **Bước 8** - `PublicationScheduleHandler` (UPDATE có điều kiện, phát event) (F5, P2, P6, BR-U08-35, 36).
- [ ] **Bước 9** - `AssignmentQueryService` và `LearnerViewMapper` (F8, P3, P4, BR-U08-03).
- [ ] **Bước 10** - Unit test mọi `BR-U08-xx`, gồm ranh giới `closesAt`/`lateUntil` và bài `LOCKED` không sửa được.
- [ ] **Bước 11** - Tóm tắt: `aidlc-docs/construction/u08-assessment-core-publication/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 12** - Flyway `V20260925_1500__u08_assessment.sql` theo `infrastructure-design.md` §2.
- [ ] **Bước 13** - JPA repository.
- [ ] **Bước 14** - Integration test Testcontainers: job mở/đóng chạy lặp, đổi lịch sau khi tạo job, hai lượt phát hành cùng lớp bị chặn, event gửi sau commit.
- [ ] **Bước 15** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 16** - `/contracts/openapi/u08-assessment.yaml` và schema event.
- [ ] **Bước 17** - Controller + DTO + validation (giờ nhận ISO 8601 có offset).
- [ ] **Bước 18** - Test MockMvc: phát hành sai lớp bị từ chối và audit, người học không thấy đáp án, không thấy bài `SCHEDULED`/`RETIRED`, sửa bài `LOCKED` trả `409`.
- [ ] **Bước 19** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 20** - `AssignmentListPage`, `AssignmentEditorPage` (`ComponentList`, `AddFromBankDialog`, `InlineQuestionEditor`, `TypeConfigSlot`).
- [ ] **Bước 21** - `AiDraftDialog` (ẩn khi AI chưa sẵn sàng), `PreviewDialog`, `ReviewButton`.
- [ ] **Bước 22** - `PublishDialog`, `PublicationList` (sửa lịch, ngưng giao), `CloneButton`, `ArchiveButton`.
- [ ] **Bước 23** - Test frontend: cảnh báo khóa khi phát hành, kiểm lịch phía client, giờ hiển thị Việt Nam.
- [ ] **Bước 24** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 25** - Cập nhật `README.md`: vòng đời bài, lịch mở/đóng, cách U09 cài `TypeConfigCheckPort`, cách U11 dùng `isSubmissionOpen`.
- [ ] **Bước 26** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-ASM-001 S1 (UC-ASM-07) | 6, 7, 8, 21, 22 |
| US-ASM-001 S2 | 6, 7, 18 |
| UC-ASM-01 | 9, 20 |
| US-QBK-002 S2, S3 (khóa nội dung) | 2, 4, 10, 18 |
| FR-006 (AI draft vào bản nháp) | 5, 21 |

## 5. Ngoài phạm vi

- Cấu hình riêng từng loại bài và đề chung cấp môn (U09); template/copy/simulation (U10); lượt làm (U11); nhóm (U12); AI thật (U13).
