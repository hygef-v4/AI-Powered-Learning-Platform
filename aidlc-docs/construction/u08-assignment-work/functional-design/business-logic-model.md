# U08 Assignment Work — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U08 Assignment Work thuộc Iter 2, người code: Người 3; UC 37–41. Lấy phần Assignment cá nhân, phản hồi Practice và dữ liệu hoạt động của U11 cũ. Lượt Quiz thuộc U11 mới, bài nhóm thuộc U12. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Danh sách bài của tôi | UC 37 | Student Assignments; phần Bài tập trên Student Class Detail |
| F2 Chi tiết bài | UC 38 | Assignment Detail |
| F3 Bắt đầu hoặc tiếp tục lượt | UC 39 | Assignment Detail |
| F4 Làm bài và tự lưu | UC 39 | Assignment Workspace |
| F5 Nhập, xuất DOCX | UC 39, 40 | Assignment Workspace, Submission History |
| F6 Nộp bài | UC 39 | Assignment Workspace |
| F7 Tự nộp | - | - |
| F8 Lịch sử nộp | UC 40 | Submission History |
| F9 Xin phản hồi bằng API cá nhân | UC 41 | Submission History, popup chọn nguồn AI |
| F10 Phản hồi qua agent MCP | UC 41 | - |
| F11 Ghi dữ liệu hoạt động | - | Assignment Workspace |
| F12 Tóm tắt và dọn dữ liệu hoạt động | UC 42 (U09) | - |
| F13 Port cho unit khác | UC của U09, U11, U12, U14 | - |

## 2. Xem bài

### F1 - Danh sách bài của tôi (UC 37)
1. Student mở Student Assignments từ thanh bên Class Dashboard (U08 đăng ký `DashboardNavEntry`) hoặc từ phần Bài tập trên Student Class Detail (`StudentClassSection`, lọc sẵn lớp); frontend gọi `GET /api/v1/me/assignments` kèm lớp, trạng thái.
2. Lấy các lớp `OPEN` đang ghi danh (`ClassAccessPort.listOpenClassesOf`), rồi bài của từng lớp (`AssignmentQueryPort.listForClass`, gồm bài của môn); bỏ bài người học không được thấy (BR-U08-01).
3. Ghép trạng thái của mình: bài cá nhân từ lượt làm; bài nhóm từ `GroupWorkStatusPort` (khi U12 có, chưa có thì chỉ hiện nhãn "Bài nhóm").
4. Trả danh sách lọc theo lớp, trạng thái, sắp theo hạn gần nhất; cột điểm và phân bố ẩn danh do U09 gắn qua slot `StudentAssignmentResult`.
5. Bấm bài mở Assignment Detail; nút lịch sử mở Submission History.

### F2 - Chi tiết bài (UC 38)
1. Frontend gọi `GET /api/v1/me/assignments/{assignmentId}` kèm `classId`.
2. Kiểm R5 với lớp và bài (BR-U08-01); không đạt thì "không tìm thấy".
3. Trả hướng dẫn, khung chỉ đọc (`AssignmentQueryPort.getContent`), rubric từng phần (`RubricPort.listRubrics`), cá nhân hay nhóm, `requiredDiagrams`, lịch, số lượt đã dùng và còn lại, lượt gần nhất, chính sách AI, thông báo theo dõi, `referenceMaterialIds` để U13 hiện tài liệu gắn kèm (BR-U08-03).
4. Hiện nút Bắt đầu hoặc Tiếp tục khi đủ điều kiện; bài nhóm hiện nút của U12 (slot `GroupWorkEntry`); nút Lịch sử nộp.

## 3. Làm và nộp

### F3 - Bắt đầu hoặc tiếp tục lượt (UC 39)
1. Bấm Bắt đầu (hoặc Tiếp tục); frontend gọi `POST /api/v1/assignments/{assignmentId}/attempts` kèm `classId`.
2. Đã có lượt `IN_PROGRESS` thì trả lượt đó.
3. Khóa hàng theo (bài, người học); kiểm BR-U08-10, 11: ghi danh, bài cá nhân, `submissionWindow`, còn lượt.
4. Tạo lượt `IN_PROGRESS`: `attemptNo` kế tiếp, chụp theo BR-U08-12, nội dung bắt đầu từ khung, `deadlineAt`.
5. Mở Assignment Workspace; nếu theo dõi bật thì `useActivityTracker` bắt đầu gửi sự kiện (F11).

### F4 - Làm bài và tự lưu (UC 39)
1. Workspace dùng `DocumentEditor` của U07 ở chế độ người học: block của người soạn bị khóa; người học thêm đoạn, danh sách, bảng, ảnh, sơ đồ (nếu bật) ở bất kỳ đâu.
2. 10 giây sau lần sửa cuối hoặc khi rời trang, frontend gọi `PUT /api/v1/attempts/{attemptId}/content` kèm `contentVersion` (nén gzip).
3. Kiểm chủ lượt, `IN_PROGRESS`, chưa quá `deadlineAt` cộng 30 giây, còn ghi danh; `validateForSave` (BR-U08-14).
4. Lệch `contentVersion` thì `409`; đạt thì lưu, tăng `contentVersion`, trả "Đã lưu lúc …".
5. Ảnh tải qua U03 (`store`) và được gắn (`attach`) khi lưu.

### F5 - Nhập, xuất DOCX (UC 39, 40)
Nhập:
1. Trong Workspace bấm Nhập DOCX, chọn file ≤ 20 MB; frontend gọi `POST /api/v1/attempts/{attemptId}/docx:preview`.
2. Kiểm chủ lượt, `IN_PROGRESS`, chưa hết hạn; gọi `DocxPort.importPreview` với block của người học và cờ sơ đồ; mã sơ đồ chỉ nhận khi trỏ tới đề của bài hoặc lượt của chính mình.
3. Trả bản xem trước; người học xác nhận thì thêm vào cuối bản nháp và lưu như F4 bước 2–4.

Xuất:
1. Trên Workspace hoặc Submission History bấm Tải DOCX; frontend gọi `GET /api/v1/attempts/{attemptId}/export.docx`.
2. Kiểm chủ lượt, hoặc giảng viên chính của lớp ghi trên lượt; gọi `DocxPort.export` (sơ đồ kèm mã `[drawio:attemptId/blockId]`).

### F6 - Nộp bài (UC 39)
1. Bấm Nộp, xác nhận; frontend lưu lần cuối rồi gọi `POST /api/v1/attempts/{attemptId}/submit` kèm `contentVersion`.
2. Lượt đã `SUBMITTED` thì trả lại biên nhận cũ (BR-U08-22).
3. Kiểm chủ lượt, `IN_PROGRESS`, trước `deadlineAt` cộng 30 giây, còn ghi danh; `validateForSubmit`. Có lỗi thì trả danh sách cần sửa; quá hạn thì ghi audit `SUBMIT_REJECTED`.
4. Một transaction: `SUBMITTED`, `submittedAt`, `late`, `receiptHash`, `submitMode = MANUAL`; gọi `SubmissionListener.onSubmitted` (U09); audit `ASSIGNMENT_SUBMITTED`.
5. Trả biên nhận, mở Submission History.

### F7 - Tự nộp
1. `ScheduledScanner` của U08 chạy mỗi phút: lượt `IN_PROGRESS` có `deadlineAt` cộng 30 giây đã qua được nộp bằng bản đã lưu gần nhất, `submitMode = AUTO_DEADLINE`, không kiểm điều kiện nộp, ghi cảnh báo (ví dụ thiếu sơ đồ bắt buộc); gọi `onSubmitted`; audit.
2. U07 ngưng giao bài thì gọi `onRetired` của U08: trong cùng transaction nộp mọi lượt `IN_PROGRESS` của bài với `AUTO_RETIRED` như bước 1.
3. Câu lệnh nộp có điều kiện trạng thái nên chạy lại không nộp hai lần.

## 4. Lịch sử và phản hồi

### F8 - Lịch sử nộp (UC 40)
1. Từ Assignment Detail hoặc Student Assignments mở Submission History; frontend gọi `GET /api/v1/assignments/{assignmentId}/attempts/mine` kèm `classId`.
2. Trả các lượt của mình: lượt thứ, thời điểm nộp, trễ, cách nộp, nhãn "được chấm" (bài `GRADED`, lượt nộp cuối), cảnh báo khi tự nộp.
3. Chọn một lượt: `GET /api/v1/attempts/{attemptId}` trả nội dung chỉ đọc và biên nhận; điểm và nhận xét đã công bố do U09 gắn qua slot `SubmissionResultSection`.
4. Bài `PRACTICE`: hiện lịch sử phản hồi AI của lượt (`GET /api/v1/attempts/{attemptId}/feedback`) và nút "Xin phản hồi AI" (F9); nút Tải DOCX (F5).

### F9 - Xin phản hồi bằng API cá nhân (UC 41)
1. Trên lượt đã nộp của bài `PRACTICE` bấm "Xin phản hồi AI"; `AiSourcePicker` của U06 chỉ hiện kết nối cá nhân của người học; người học chọn nguồn.
2. Frontend gọi `POST /api/v1/attempts/{attemptId}/feedback` kèm nguồn và `requestRef`.
3. Kiểm BR-U08-40, 44: chủ lượt, `SUBMITTED`, `PRACTICE`, còn ghi danh; lượt chưa có yêu cầu đang chờ; chưa quá 20 yêu cầu trong ngày.
4. Dựng prompt theo BR-U08-45 (`AssignmentQueryPort.getContent`, `RubricPort.listRubrics`, `DocumentModelPort.toPlainText`, `compactDiagramXml`) kèm JSON schema của phản hồi.
5. Gọi `AiTaskPort.submit` (loại `PRACTICE_FEEDBACK`, đích là lượt, `inputVersion` là `receiptHash`); tạo phản hồi `PENDING`; audit `PRACTICE_FEEDBACK_REQUESTED`; trả `taskId` cho `AiTaskStatus`.
6. U06 gọi `onReady`: kiểm lại chủ lượt, ghi danh, hash, schema; đạt thì lưu phản hồi `READY` kèm provenance và trả `APPLIED`, không thì trả `STALE` hoặc `REJECTED`. `onFailed` chuyển phản hồi sang `FAILED`.
7. Người học xem phản hồi; muốn thêm thì xin lại, mỗi lần là một bản trong lịch sử.

### F10 - Phản hồi qua agent MCP (UC 41)
1. Agent của người học gọi `read_own_work(attemptId)`: U08 kiểm actor là chủ lượt, lượt `SUBMITTED`, bài `PRACTICE`; trả hướng dẫn, khung, rubric, bài nộp dạng chữ và `submissionHash`.
2. Agent tự gọi mô hình của nó rồi gọi `record_private_feedback(attemptId, submissionHash, feedback)` (credential `READ_WRITE`).
3. U08 kiểm như F9 bước 3 (trừ yêu cầu đang chờ), hash khớp, schema đúng; lưu phản hồi `READY` nguồn MCP (nhãn agent, model nếu agent báo).
4. Giảng viên dùng `read_authorized_submission(attemptId)`: U08 kiểm actor là giảng viên chính của lớp ghi trên lượt, lượt đã nộp; trả bài nộp dạng chữ, không có dữ liệu người khác.

## 5. Dữ liệu hoạt động

### F11 - Ghi dữ liệu hoạt động
1. Lượt có chính sách bật thì `useActivityTracker` trên Workspace gửi lô sự kiện (BR-U08-51) tới `POST /api/v1/attempts/{attemptId}/activity`, tối đa 50 sự kiện mỗi lô, gửi lại khi mạng lỗi.
2. Kiểm chủ lượt, `IN_PROGRESS`, chính sách trong ảnh chụp bật, giới hạn tần suất (BR-U08-52).
3. Ghi sự kiện, bỏ trùng theo `clientEventId`, ghi giờ server.
4. U11 (lượt Quiz) và U12 (tài liệu nhóm) kiểm quyền của mình rồi gọi `ActivityWritePort.record`, chạy tiếp bước 2–3 với loại công việc tương ứng.

### F12 - Tóm tắt và dọn dữ liệu hoạt động
1. U09 kiểm giảng viên được giao lớp rồi gọi `ActivityQueryPort.summary(workRef, accountId)`.
2. U08 ghép `HIDDEN` với `VISIBLE` theo phiên thành khoảng rời trang; khoảng không có heartbeat quá 2 phút, sự kiện lệch thứ tự, giờ client bất thường được đánh dấu "không rõ" (BR-U08-53).
3. Trả trạng thái chính sách, số lần, tổng thời gian đã biết, các khoảng và khoảng không rõ; không có kết luận.
4. Scanner xóa theo lô sự kiện có giờ server cũ hơn `activity.retentionDays` (BR-U08-54).

## 6. Port cho unit khác

### F13 - Port cho unit khác
1. U09 đọc bài nộp theo bài và lớp, nội dung lượt, trạng thái từng sinh viên (chưa làm, đang làm, đã nộp, nộp trễ) qua `SubmissionQueryPort`; cài `SubmissionListener` để tạo dòng chờ chấm.
2. U14 lấy sinh viên chưa nộp của bài sắp hết hạn qua `listStudentsWithoutSubmission` để nhắc 24 giờ trước hạn; với bài nhóm, U08 dựa vào trạng thái nhóm từ `GroupWorkStatusPort` của U12.
3. U11, U12 ghi sự kiện qua `ActivityWritePort`; U12 cài `GroupWorkStatusPort` và slot `GroupWorkEntry`.
