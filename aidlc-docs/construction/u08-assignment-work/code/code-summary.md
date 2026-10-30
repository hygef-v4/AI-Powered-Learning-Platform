# U08 Assignment Work — code summary

**Current source snapshot 2026-10-30:** [as-built-baseline.md](as-built-baseline.md) — source-observed; các số test/ngữ cảnh checkout dưới đây là lịch sử, không phải kết quả chạy lại tại HEAD hiện tại.

## Ghi nhận trước baseline 2026-10-30 (lịch sử)

Code viết ngày 2026-10-20 theo [plan](../../plans/code-generation-plan/u08-assignment-work-code-generation-plan.md), nhánh `assignment`, làm cùng U07. Integration test do tester viết.

## Business logic

| Luồng | Class chính |
|---|---|
| F1 Bài của tôi (UC 37) | `StudentAssignmentService.list`: lớp `OPEN` đang học (`IClassAccess`), bài của lớp và bài của môn (`IAssignmentQuery.listForClass`); thấy `OPEN`, `CLOSED`, và `RETIRED` khi đã có lượt nộp; trạng thái của mình `NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`, `SUBMITTED_LATE`, `MISSED`, `GROUP` (bài nhóm lấy từ `IGroupWorkStatus`); sắp theo hạn |
| F2 Chi tiết bài (UC 38) | `detail`: đề, khung chỉ đọc, rubric, lịch, số lượt đã dùng, lượt gần nhất, lý do không bắt đầu được, lời báo theo dõi hoạt động, `referenceMaterialIds` cho U13 |
| F3 Bắt đầu lượt (UC 39) | `AttemptService.start`: khóa advisory theo (bài, người học); có lượt đang làm thì trả lượt đó; kiểm ghi danh, bài cá nhân, cửa sổ nộp, còn lượt (từ chối ghi `SUBMIT_REJECTED`); lượt mới chụp version, chế độ, hạn, sơ đồ bắt buộc, chính sách theo dõi; nội dung bắt đầu từ khung |
| F4 Làm bài, tự lưu | `save`: chủ lượt, còn làm, chưa quá hạn cộng 30 giây ân hạn, `contentVersion` khớp (P1); làm sạch, `validateForSave`, sơ đồ chỉ khi bài bật, gắn ảnh mới, khối của giảng viên lấy đúng bản của đề. Bài được kéo dài hạn thì lượt nhận hạn mới |
| F5 DOCX | `importPreview` qua `IDocx` (khối người học; tiêu đề trong DOCX thành đoạn đậm; mã sơ đồ chỉ nhận đề của bài hoặc lượt của chính mình); `export` cho chủ lượt hoặc giảng viên chính của lớp, mã `attempt/attemptId/blockId` và link `/attempts/{id}/diagrams/{blockId}` |
| F6 Nộp (UC 39) | `submit`: nộp lặp trả cùng biên nhận; quá hạn ghi `SUBMIT_REJECTED`; `validateForSubmit`; `AttemptSubmitter` cập nhật có điều kiện trạng thái (P2), `late` khi sau `closesAt`, `receiptHash` SHA-256, gọi `ISubmissionListener` trong cùng transaction, audit `ASSIGNMENT_SUBMITTED` |
| F7 Tự nộp | `AutoSubmitScanner` (mỗi lượt một transaction, `AUTO_DEADLINE`) và `AssignmentRetiredListener` (cài `IAssignmentLifecycleListener`, `AUTO_RETIRED` trong transaction ngưng giao của U07); nộp bản đã lưu, lỗi kiểm ghi thành cảnh báo cho giảng viên |
| F8 Lịch sử (UC 40) | `history`: các lượt, lượt được chấm (Graded: lượt nộp cuối), số góp ý AI |
| F9, F10 Góp ý Practice (UC 41) | `PracticeFeedbackService`, `PracticeFeedbackResultHandler` (`PRACTICE_FEEDBACK`), `AssignmentWorkMcpTools` (`read_own_work`, `record_private_feedback`, `read_authorized_submission`): chỉ lượt đã nộp của mình, bài Practice, còn ghi danh; một yêu cầu chờ mỗi lượt; 20 yêu cầu mỗi ngày (theo giờ Việt Nam, web và MCP cộng chung); kết quả lệch `submissionHash` hoặc sai định dạng không lưu; nhãn "AI ước tính, không phải điểm chính thức" |
| F11, F12 Dữ liệu hoạt động | `ActivityService` (cài `IActivityWrite`, `IActivityQuery`): chỉ thu khi chính sách của lượt bật và lượt đang làm; bỏ trùng theo `clientEventId`; ≤ 50 sự kiện mỗi lô, ≤ 60 mỗi phút mỗi công việc (bộ đếm Redis, Redis lỗi thì vẫn ghi); `ActivitySummaryCalculator` ghép rời trang, đánh dấu khoảng không rõ; `ActivityRetentionScanner` xóa theo lô 10.000; `ActivitySettingDefinitions` khai báo `activity.retentionDays` (nhóm `ACTIVITY`, 1–365, không mặc định) |
| F13 Contract cho unit khác | `SubmissionQueryService` (cài `ISubmissionQuery`): trạng thái từng sinh viên và lượt được chấm, bài nộp, sinh viên chưa nộp (bài nhóm theo `IGroupWorkStatus`) |

Domain: `AssignmentAttempt`, `AttemptSnapshot`, `PracticeFeedback`, `ActivityEvent`; repository `AttemptRepository`, `PracticeFeedbackRepository`, `ActivityEventRepository` (`JdbcClient`).

Kiểm quyền: action mới trong `Actions`/`AccessPolicy` của U01: `ASSIGNMENT_WORK` (Student, phạm vi lớp, ghi danh `ACTIVE`) và `ASSIGNMENT_ATTEMPT_REVIEW` (Teacher, Subject Manager; phạm vi lớp) cho giảng viên xem bài đã nộp. Lớp phải `OPEN`; lượt của người khác trả 404 kèm audit `ACCESS_DENIED`; U01, U05, U07 lỗi trả 503. Audit: `ASSIGNMENT_SUBMITTED`, `SUBMIT_REJECTED`, `PRACTICE_FEEDBACK_REQUESTED`; không audit lưu nháp và sự kiện hoạt động.

## API

Gốc `/api/v1`.

| Endpoint | UC |
|---|---|
| `GET /me/assignments?classId=`, `/me/assignments/{assignmentId}?classId=`, `/me/assignments/{assignmentId}/blocks/{blockId}/image?classId=` | 37, 38 |
| `POST /assignments/{assignmentId}/attempts`; `GET /attempts/{attemptId}`; `PUT .../content`; `POST .../docx:preview`; `POST .../submit`; `GET .../receipt`; `GET .../blocks/{blockId}/image`; `GET .../diagrams/{blockId}` | 39 |
| `GET /assignments/{assignmentId}/attempts/mine?classId=`; `GET /attempts/{attemptId}/export.docx` | 40 |
| `POST`, `GET /attempts/{attemptId}/feedback` | 41 |
| `POST /attempts/{attemptId}/activity` | 39 |

## Migration

`db/migration/assignmentwork/V20260925_1400__create_assignment_work_tables.sql`: `assignment_attempts` (unique lượt theo số, một lượt `IN_PROGRESS` mỗi (bài, người học), lượt đã nộp phải có biên nhận), `practice_feedback` (unique `request_ref`, `task_id`, một yêu cầu chờ mỗi lượt), `activity_events` (unique `(account_id, work_kind, work_id, client_event_id)`); `app` không được `DELETE` lượt và góp ý, chỉ xóa sự kiện hoạt động.

## Cấu hình

`platform.assignment-work.feedback-per-day` (`U08_FEEDBACK_PER_DAY`, 20), `platform.assignment-work.activity-events-per-minute` (`U08_ACTIVITY_EVENTS_PER_MINUTE`, 60).

## Frontend

- Route (chỉ Student): `/my-assignments` (#38), `/my-assignments/[assignmentId]` (#39), `/my-assignments/[assignmentId]/history` (#41), `/attempts/[attemptId]` (#40 Workspace), `/attempts/[attemptId]/receipt`, `/attempts/[attemptId]/diagrams/[blockId]`.
- `features/assignmentwork`: `StudentAssignmentsPage`, `ClassAssignmentsCard` (gắn `StudentClassSection`), `ClassCardAssignments` (gắn `ClassCardSection`), `StudentAssignmentDetailPage`, `AssignmentWorkspacePage` (trang giấy dùng `DocumentEditor` của U07, đề bài ở đầu trang, rubric của mỗi mục ở lề phải ngang tiêu đề; tự lưu 10 giây sau lần sửa cuối và khi rời trang, đếm ngược theo giờ server, popup nộp có kiểm sơ bộ, nhập DOCX thêm vào cuối), `ReceiptPage`, `SubmissionHistoryPage`, `PracticeFeedbackPanel`, `AttemptDiagramPage`; host slot `StudentAssignmentResult`, `SubmissionResultSection`, `GroupWorkEntry`, `AssignmentMaterialSection`.
- `shared/activity`: `useActivityTracker` (trạng thái hiển thị của trang, heartbeat 60 giây, gom lô, gửi lại khi lỗi), `ActivityTimeline` cho U09, U11.

## Unit test

Backend: bắt đầu lượt (không ghi danh, bài nhóm, hết hạn có audit, hết lượt, trả lượt đang làm, lượt mới từ khung); lưu lệch `contentVersion`, quá hạn cộng ân hạn; khối giảng viên lưu đúng bản của đề; nộp thiếu điều kiện, nộp hai lần cùng biên nhận, nộp trễ; tự nộp khi hết hạn chạy một lần và giữ cảnh báo, hạn được kéo dài; ngưng giao tự nộp; lượt của người khác; góp ý (lượt đang làm, bài Graded, yêu cầu chờ, hạn mức ngày, hash lệch, sai định dạng, kết quả hợp lệ, agent sai hash); tóm tắt hoạt động (cặp rời trang, thiếu heartbeat, thiếu số thứ tự), chính sách tắt, lượt đã nộp, giới hạn tần suất, Redis lỗi, quá hạn lưu, dọn theo lô; danh sách bài (ẩn bài chưa mở, bài ngưng giao, trạng thái của mình), chi tiết (hết lượt, lượt được chấm), Teacher dùng màn Student; `ISubmissionQuery`. Frontend: kiểm sơ bộ khi nộp, bài sắp đến hạn, đếm ngược, thời lượng, `useActivityTracker`.

## Chạy thử trên Docker (2026-10-20)

Student thấy bài, mở chi tiết, bắt đầu lượt (bấm lại trả cùng lượt), tự lưu, lưu lệch phiên bản trả 409, xóa khối giảng viên trả 400, nộp qua popup, nộp lại cùng biên nhận, biên nhận, lịch sử, tải DOCX bài làm; xin góp ý bài Graded trả 409, bài Practice ghi yêu cầu và báo AI chưa sẵn sàng (bản tạm của U06); Teacher gọi API của Student trả 404; ngưng giao xong vẫn thấy bài vì đã nộp, bắt đầu lượt mới trả 409. Slot trên thẻ lớp và trang lớp của Student hiển thị đúng.

## Khác với thiết kế

- Giới hạn tần suất sự kiện hoạt động dùng bộ đếm `INCR` của Redis theo phút thay cho Bucket4j; hạn mức góp ý đếm trong database.
- Body lưu nháp chưa nén gzip.
- `inputVersion` của task góp ý là `contentVersion` (kiểu số); hash bài nộp lưu riêng trong `practice_feedback` để so khi nhận kết quả.
- Popup góp ý dùng ô chọn nhà cung cấp đơn giản vì `AiSourcePicker` của U06 chưa có.
- Chưa có `contracts/openapi/assignment-work.yaml` (giống U01–U05).

## Thay đổi theo yêu cầu 2026-10-21 (trang kiểu Google Docs)

Các thay đổi dưới đây (cả vòng 2026-10-21 và các vòng 2026-10-22) đã đưa vào functional-design ngày 2026-10-22 (business-rules, business-logic-model, domain-entities, frontend-components); các bảng dưới giữ lại để truy vết.

Người dùng yêu cầu làm theo cách này dù khác thiết kế; các BR liên quan trong functional-design chưa sửa.

| Thay đổi | Khác thiết kế | Code |
|---|---|---|
| Trang thông tin trước khi làm bài (#39): chỉ có thông tin (lớp, hình thức, chế độ, số phần, sơ đồ cần có, lịch, số lượt, lượt gần nhất, lời báo theo dõi, học liệu); không có hướng dẫn, khung đề, rubric; đề chỉ hiện khi đã bấm Bắt đầu hoặc Tiếp tục | Thay BR-U08-03 | `WorkViews.Detail` (bỏ `instructions`, `skeleton`, `parts`, `rubrics`; thêm `partCount`), `StudentAssignmentDetailPage`; bỏ API `GET /me/assignments/{id}/blocks/{blockId}/image` |
| Người học không xem rubric: lượt làm không trả rubric, workspace không có thẻ rubric; tool `read_own_work` không có rubric; prompt góp ý Practice không có rubric (prompt đi qua API cá nhân của chính người học); góp ý bỏ phần ước tính theo rubric và điểm ước tính, thêm nhận xét theo từng phần | Thay phần rubric của BR-U08-42, 45 | `WorkViews.Attempt`, `AttemptService`, `PracticeFeedbackService`, `FeedbackFormat`, `PracticeFeedbackPanel` |
| Workspace: chỉ viết trong các phần lá (phần chưa có bài làm có khung nét đứt "Viết câu trả lời cho phần này…"); không có nút thêm ở phần chung, tiêu đề cha; di chuyển block chỉ trong phần; sơ đồ của giảng viên trong phần có nút "Vẽ bằng Draw.io"; nhập DOCX chèn vào cuối phần đang chọn | Theo BR-U07-15 mới | `AssignmentWorkspacePage`, `insertIntoPart`, `DocumentEditor`; `AttemptService.prepare` giữ nét vẽ của người học trên sơ đồ của đề, lấy loại, mô tả theo đề |
| Sơ đồ luôn dùng được, kể cả lượt đã chụp khi bài còn tắt sơ đồ | Theo U07 | `AttemptSnapshot` ép `diagramEnabled = true` |

Sửa lỗi tìm thấy ở bước Review:
- Nhập DOCX vào lượt làm chạy trong transaction chỉ đọc mà lại khóa dòng (`SELECT … FOR UPDATE`), PostgreSQL sẽ từ chối: bỏ `readOnly` (`AttemptService.importPreview`).
- Workspace: lỗi mạng khi tự lưu từng bị coi là đã lưu nên bấm Nộp có thể nộp bản cũ; nay giữ trạng thái chưa lưu và tự thử lại; đang lưu thì lần lưu sau chờ lần trước xong; lệch phiên bản hoặc hết hạn thì dừng tự lưu và Nộp báo chưa lưu được.
- Lưu khi rời trang: bài lớn hơn khoảng 60 KB dùng yêu cầu thường thay cho `keepalive` (trình duyệt giới hạn khoảng 64 KB).

Unit test thêm: nét vẽ trên sơ đồ của đề được giữ, prompt và tool của người học không có rubric, góp ý bỏ phần rubric; frontend: chèn DOCX vào phần, kiểm sơ bộ tính sơ đồ của đề đã vẽ. Chưa chạy thử trên Docker sau thay đổi này.

## Thay đổi theo yêu cầu 2026-10-22 (vòng sửa giao diện)

| Thay đổi | Khác thiết kế | Code |
|---|---|---|
| Người học điền thẳng vào bảng của đề nằm trong phần (sửa ô, thêm hàng, cột; không xóa); lưu giữ các ô người học điền, hàng tiêu đề theo đề | Theo BR-U07-15 mới (U07 ngày 2026-10-22) | `AttemptService.prepare` (`filled`) |
| Sơ đồ của đề người học vẽ: lưu cả loại sơ đồ suy từ hình vẽ (không lấy loại theo đề nữa), mô tả vẫn theo đề | Theo U07: loại sơ đồ suy từ hình vẽ | `AttemptService.drawn` |
| Workspace theo trang soạn mới: chèn sơ đồ một nút mở Draw.io ở cột phải; phần lá khung màu ruột trắng; không có chữ gợi ý ở dòng trống (thay "Viết câu trả lời cho phần này…") | Giao diện | `DocumentEditor` (dùng chung U07) |
| Danh sách bài của người học: Hình thức và Chế độ là hai cột riêng; bỏ chữ "Có sơ đồ" | Giao diện | `StudentAssignmentsPage` |

Unit test thêm: bảng của đề người học điền được lưu, hàng tiêu đề giữ theo đề; loại sơ đồ theo hình vẽ.

## Thay đổi theo yêu cầu 2026-10-22 (vòng sửa thứ hai)

| Thay đổi | Khác thiết kế | Code |
|---|---|---|
| Lượt làm trả thêm `teacherTables` (số hàng, cột của mỗi bảng của đề theo id block): người học chỉ thêm hàng, cột sau phần của đề và xóa được hàng, cột mình thêm; bảng người học tự tạo thì xóa được cả bảng | Thêm trường vào API lượt làm | `WorkViews.Attempt`, `WorkViews.TableShape`, `AttemptService.teacherTables`, `AssignmentWorkspacePage` |
| Trang thông tin bài và trang làm bài không hiện tổng điểm | Bỏ "tổng X điểm" ở #39, #40 | `StudentAssignmentDetailPage`, `AssignmentWorkspacePage` |

Unit test thêm: cỡ bảng của đề trong lượt làm; frontend: người học xóa hàng mình thêm, không xóa hàng của đề.

## Thay đổi theo yêu cầu 2026-10-22 (vòng sửa thứ ba)

| Thay đổi | Khác thiết kế | Code |
|---|---|---|
| Bỏ trường `teacherTables` của lượt làm (vòng trước): ô người học thêm vào bảng của đề nay mang cờ `added` trong chính nội dung bài làm, nên biết hàng, cột nào người học xóa được mà không cần cỡ bảng của đề; `AttemptService.prepare` chép cả cờ khi lưu | Thay dòng `teacherTables` ở vòng trước | `WorkViews.Attempt`, `AttemptService`, `DocumentEditor` |
| Thông báo của trang làm bài chỉ ghi trạng thái: "Bài đang mở ở nơi khác, bản đang sửa chưa lưu", "Mất kết nối, chưa lưu", "Chưa lưu: …", "Đã nộp lúc … · chỉ xem", "Đã quá hạn", "Bài chưa lưu" | Giao diện | `AssignmentWorkspacePage` |

## Còn lại

- Bước 13 Review: đối chiếu UC 37–41, story, BR-U08 với code, người dùng duyệt.

## Thay đổi 2026-10-24

- Bỏ `ActivitySettingDefinitions` (cài đặt `activity.retentionDays`): thời hạn giữ sự kiện hoạt động cố định 1 ngày (`ActivityService.RETENTION`); `ActivityRetentionScanner` xóa sự kiện cũ hơn 1 ngày, tóm tắt báo `EXPIRED` sau 1 ngày.
