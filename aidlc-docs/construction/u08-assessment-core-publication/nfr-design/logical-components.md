# U08 Assessment Core & Publication - Logical Components

## 1. Sơ đồ

```
 Trình duyệt (GV, CN môn)                       U09 / U10 / U11 / U04 / U16
   |                                                  |
   v                                                  v
 +------------------------------- backend -------------------------------------+
 | AssignmentController --> AssignmentService --> BankQueryPort (U06)           |
 |                                  |          --> AiDraftPort (U13, C)          |
 |                                  +--> ReviewValidator --> TypeConfigPort |
 |                                                           (U09, C)            |
 | PublicationController --> PublicationService --> JobPort (U02)               |
 | AssignmentQueryService (AssignmentQueryPort, LearnerAssignmentView)          |
 | Repository (PostgreSQL: assignments, assignment_components, publications)    |
 +------------------------------------------------------------------------------+
                  | job PUBLICATION_OPEN / CLOSE
                  v
 worker: PublicationScheduleHandler --> EventPublisherPort (U02) --> U11, U16
```

**Text alternative**: Giảng viên soạn bài qua `AssignmentController`; `AssignmentService` lấy câu từ ngân hàng U06, gọi AI qua U13, và dùng `ReviewValidator` (có kiểm cấu hình loại bài của U09) khi duyệt. `PublicationController` phát hành và tạo job mở/đóng qua U02. Các unit khác đọc bài qua `AssignmentQueryService`. Trong worker, `PublicationScheduleHandler` đổi trạng thái theo lịch và phát event cho U11, U16.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `AssignmentService` | backend | F1-F3, F7; P1 |
| `ReviewValidator` | backend | P5 |
| `PublicationService` | backend | F4, F6, F7; P2 |
| `PublicationScheduleHandler` | worker | F5; P2, P6 |
| `AssignmentQueryService`, `LearnerViewMapper` | backend | F8; P3, P4 |

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
