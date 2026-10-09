# U08 Assessment Core & Publication - Logical Components

**Bản tài liệu 2026-10-09**: UC 41 và vòng đời (tạo, xóa, duyệt, phát hành, lịch, ngưng giao, nhân bản, version) của UC 35, 42, 43, 44, 45 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-ASM-001, US-ASM-008. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Trình duyệt (GV, CN môn)                       U09 / U11 / U04 / U16
   |                                                  |
   v                                                  v
+---------------------------------------------------------------------------------------+
| backend                                                                               |
| AssignmentController --> AssignmentService --> BankQueryPort,                         |
|                                   |             InlineQuestionPort (U06)              |
|                                   |          --> AiDraftPort (U13, C)                 |
|                                   |          --> ContentRefPort (U05)                 |
|                                   +--> ReviewValidator --> TypeConfigPort (U09, C)    |
|                                                        --> CodeLabCheckPort (U13, C)  |
| ScheduleController --> ScheduleService (phát hành, sửa lịch, ngưng giao)              |
|                                   +--> RubricPort.lockForAssignment (U06)             |
|                                   +--> GroupReadinessPort (U12)                       |
| AssignmentQueryService (AssignmentQueryPort, StudentAssignmentView)                   |
| Repository (PostgreSQL: assignments, assignment_questions)                            |
+---------------------------------------------------------------------------------------+
                  | scanner mỗi phút (U03)
                  v
 worker: AssignmentScheduleScanner --> AssignmentLifecyclePort (U11, U14)
                                   --> EventPublisherPort (U03) --> U16
```

**Text alternative**: Giảng viên soạn bài của lớp, Chủ nhiệm môn soạn bài của môn qua `AssignmentController`; `AssignmentService` lấy câu khớp dạng từ ngân hàng U06 hoặc lưu câu riêng qua U06, kiểm học liệu của quiz qua U05, gọi AI qua U13, và dùng `ReviewValidator` (kiểm nội dung dạng bài và rubric của U09, lời giải mẫu Code Lab của U13) khi duyệt. `ScheduleController` phát hành (ghi lịch, khóa rubric qua U06, bài nhóm hỏi U12), sửa lịch và ngưng giao. Các unit khác đọc bài qua `AssignmentQueryService` (bài của lớp gồm cả bài của môn chứa lớp, quiz theo học liệu). Trong worker, `AssignmentScheduleScanner` đổi trạng thái theo lịch, gọi U11/U14 trong transaction và phát event cho U16.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `AssignmentService` | backend | F1-F4, F7, F9, F10; P1 |
| `ReviewValidator` | backend | P5 |
| `ScheduleService` | backend | F5, F7, F9 bước 4-5; P7 |
| `AssignmentCopier` | backend | F11; P8 |
| `AssignmentScheduleScanner` | worker | F6; P2, P6 |
| `AssignmentQueryService`, `StudentViewMapper` | backend | F8; P3, P4 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U08_MAX_QUIZ_QUESTIONS` | 200 |
| `U08_MAX_ATTEMPTS` | 10 |
| `U08_MAX_LATE_DAYS` | 30 |
| `APP_TIMEZONE` | `Asia/Ho_Chi_Minh` |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log đáp án |
| SECURITY-05 | Compliant | Kiểm lịch, đầu vào |
| SECURITY-08 | Compliant | P4, kiểm phạm vi |
| SECURITY-15 | Compliant | P2 UPDATE có điều kiện |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
