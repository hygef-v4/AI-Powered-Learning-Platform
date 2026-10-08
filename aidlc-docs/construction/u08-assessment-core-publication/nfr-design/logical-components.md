# U08 Assessment Core & Publication - Logical Components

**Bản tài liệu 2026-10-08**: UC 38; primary stories: US-ASM-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Trình duyệt (GV, CN môn)                       U09 / U10 / U11 / U04 / U16
   |                                                  |
   v                                                  v
 +------------------------------- backend ---------------------------------------------+
 | AssignmentController --> AssignmentService --> BankQueryPort,                       |
 |                                  |             InlineQuestionPort (U06)             |
 |                                  |          --> AiDraftPort (U13, C)                |
 |                                  +--> ReviewValidator --> TypeConfigPort (U09, C)   |
 |                                                       --> CodeLabCheckPort (U13, C) |
 | ScheduleController --> ScheduleService (phát hành, sửa lịch, ngưng giao)            |
 |                                  +--> GroupReadinessPort (U12)                      |
 | AssignmentQueryService (AssignmentQueryPort, StudentAssignmentView)                 |
 | Repository (PostgreSQL: assignments, assignment_questions)                          |
 +-------------------------------------------------------------------------------------+
                  | scanner mỗi phút (U03)
                  v
 worker: AssignmentScheduleScanner --> AssignmentLifecyclePort (U11, U14)
                                   --> EventPublisherPort (U03) --> U16
```

**Text alternative**: Giảng viên soạn bài qua `AssignmentController`; `AssignmentService` lấy câu từ ngân hàng U06 hoặc lưu câu riêng qua U06, gọi AI qua U13 (rubric từng câu/từng phần do U09 tạo, sửa và nhân bản qua `TypeConfigPort`), và dùng `ReviewValidator` (kiểm cấu hình loại bài của U09, lời giải mẫu Code Lab của U13) khi duyệt. Phát hành bài nhóm hỏi U12 nhóm đã sẵn sàng chưa. `ScheduleController` phát hành (ghi lịch), sửa lịch và ngưng giao. Các unit khác đọc bài qua `AssignmentQueryService`. Trong worker, `AssignmentScheduleScanner` đổi trạng thái theo lịch, gọi U11/U14 trong transaction và phát event cho U16.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `AssignmentService` | backend | F1-F3, F7; P1 |
| `ReviewValidator` | backend | P5 |
| `ScheduleService` | backend | F4, F6, F7 |
| `AssignmentScheduleScanner` | worker | F5; P2, P6 |
| `AssignmentQueryService`, `StudentViewMapper` | backend | F8; P3, P4 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U08_MAX_QUIZ_QUESTIONS` | 200 |
| `U08_MAX_OTHER_QUESTIONS` | 20 |
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
