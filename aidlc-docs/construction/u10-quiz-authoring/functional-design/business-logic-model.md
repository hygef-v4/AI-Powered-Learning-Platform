# U10 Quiz Authoring — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U10 Quiz Authoring thuộc Iter 2, người code: Người 1; UC 46–48. Lấy ngân hàng câu hỏi của U06 cũ, phần Quiz của U08 cũ và cài đặt Quiz của U09 cũ. Ngân hàng chỉ còn câu trắc nghiệm theo UC 46, 47. Làm Quiz thuộc U11. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Xem ngân hàng | UC 46 | Question List, Question Detail |
| F2 Tạo, sửa, kích hoạt, xóa câu | UC 47 | Question Detail |
| F3 Nhập câu từ tệp | UC 47 | Question List |
| F4 AI soạn câu cho ngân hàng | UC 47 | Question List, popup chọn nguồn AI |
| F5 Danh sách Quiz | UC 48 | Quiz List; Teacher Class Detail, tab Evals |
| F6 Tạo Quiz | UC 48 | Quiz List, tab Evals |
| F7 Soạn câu và cài đặt Quiz | UC 48 | Quiz Detail |
| F8 AI soạn câu cho Quiz | UC 48 | Quiz Detail, popup chọn nguồn AI |
| F9 Xem trước, duyệt, phát hành | UC 48 | Quiz Detail |
| F10 Ngưng Quiz | UC 48 | Quiz Detail |
| F11 Nhân bản, version mới, copy | UC 48 | Quiz Detail, tab Evals |
| F12 Xóa Quiz nháp | UC 48 | Quiz Detail |
| F13 Tool MCP | UC 32 (U06) | - |
| F14 Port cho unit khác | UC của U11, U13, U14 | - |

## 2. Ngân hàng

### F1 - Xem ngân hàng (UC 46)
1. Chủ nhiệm môn bấm Question List trên Manager Dashboard (U10 đăng ký `DashboardNavEntry`), chọn môn mình quản lý; frontend gọi `GET /api/v1/subjects/{subjectId}/questions` kèm bộ lọc.
2. Kiểm R2 (BR-U10-01).
3. Trả bản `ACTIVE` mới nhất mỗi lineage kèm cờ "có bản nháp" và các câu mới chỉ có bản nháp (BR-U10-06).
4. Bấm câu mở Question Detail: `GET /api/v1/questions/{id}` gồm nội dung, đáp án, giải thích và lịch sử phiên bản.

### F2 - Tạo, sửa, kích hoạt, xóa câu (UC 47)
1. Tạo: trên Question List bấm "Tạo câu", nhập loại, nội dung, lựa chọn, đáp án, giải thích, điểm, độ khó, tag; `POST /api/v1/subjects/{subjectId}/questions` tạo bản `DRAFT`.
2. Sửa bản `DRAFT`: `PATCH /api/v1/questions/{id}` kèm `version`; kiểm BR-U10-02, 03.
3. Sửa bản `ACTIVE`: `POST /api/v1/questions/{id}/versions` tạo bản `DRAFT` mới cùng lineage (đã có bản nháp thì mở bản đó) (BR-U10-04).
4. Kích hoạt: `POST /api/v1/questions/{id}/activate`; kiểm nội dung hợp lệ; bản `ACTIVE` cũ của lineage giữ nguyên cho Quiz đang ghim; audit `QUESTION_ACTIVATED`.
5. Ngưng: `POST .../retire` (không chọn vào Quiz mới được nữa). Xóa: `DELETE .../{id}` chỉ với bản `DRAFT` chưa từng kích hoạt, còn lại trả gợi ý ngưng dùng (BR-U10-05). Audit.

### F3 - Nhập câu từ tệp (UC 47)
1. Trên Question List bấm Nhập, tải tệp mẫu, chọn tệp ≤ 5 MB; frontend gọi `POST /api/v1/subjects/{subjectId}/questions:import`.
2. Kiểm R2, định dạng, ≤ 500 dòng (BR-U10-07).
3. Kiểm từng dòng như F2 bước 1; dòng hợp lệ tạo câu `DRAFT`, dòng lỗi không tạo.
4. Trả bảng kết quả từng dòng; audit `QUESTIONS_IMPORTED` một lần.

### F4 - AI soạn câu cho ngân hàng (UC 47)
1. Trên Question List bấm "Soạn bằng AI": nhập số câu, loại, độ khó, chủ đề; chọn tài liệu tham khảo qua `ReferenceMaterialPort` (chưa có U13 thì bỏ qua); chọn nguồn trong `AiSourcePicker` của U06.
2. Frontend gọi `POST /api/v1/subjects/{subjectId}/question-ai-drafts`; kiểm R2.
3. Lấy chữ tài liệu (`extractText`), dựng prompt kèm JSON schema; gọi `AiTaskPort.submit` (loại `QUIZ_QUESTION_DRAFT`, đích là ngân hàng môn); tạo đề xuất `PENDING`.
4. U06 gọi `onReady`: kiểm schema và quy tắc câu; lưu đề xuất `READY` kèm provenance.
5. Chủ nhiệm môn chọn, sửa, chấp nhận câu: mỗi câu thành câu `DRAFT` của ngân hàng; audit `QUIZ_AI_DRAFT_ACCEPTED`. Kích hoạt như F2 bước 4.

## 3. Quiz

### F5 - Danh sách Quiz (UC 48)
1. Chủ nhiệm môn mở Quiz List từ Manager Dashboard, chọn môn: `GET /api/v1/subjects/{subjectId}/quizzes`.
2. Giảng viên mở tab Evals; phần Quiz do U10 gắn qua `TeacherClassTab`: `GET /api/v1/classes/{classId}/quizzes` trả Quiz của lớp và Quiz của môn chứa lớp (nhãn "Của môn", chỉ đọc).
3. Mỗi dòng: tiêu đề, số câu, tổng điểm, trạng thái, version, tài liệu gắn kèm; mặc định ẩn `RETIRED`; bấm Quiz mở Quiz Detail; nút "Tạo Quiz".

### F6 - Tạo Quiz (UC 48)
1. Bấm "Tạo Quiz" ở Quiz List (Quiz của môn) hoặc tab Evals (Quiz của lớp; có thể chọn "Copy Quiz lớp khác", F11).
2. Kiểm quyền (R2 với môn, R3/R4 với lớp) và lớp, môn chưa lưu trữ.
3. Tạo Quiz `DRAFT`, `versionNo` 1, cài đặt mặc định (không trộn, không giới hạn giờ, 1 lượt, hiện điểm, không hiện đáp án, theo dõi tắt); audit `QUIZ_CREATED`; mở Quiz Detail.

### F7 - Soạn câu và cài đặt Quiz (UC 48)
1. Quiz Detail gọi `GET /api/v1/quizzes/{id}`: thông tin, cài đặt, danh sách câu kèm điểm, tổng điểm.
2. Thêm câu ngân hàng: chọn tay (`GET /api/v1/subjects/{subjectId}/questions?status=ACTIVE`) hoặc bốc ngẫu nhiên (`POST /api/v1/quizzes/{id}/items:random-preview`); xem lại, bỏ hoặc bốc lại; lưu `PUT /api/v1/quizzes/{id}/items` kèm `version` (BR-U10-13, 14).
3. Viết câu riêng: `POST /api/v1/quizzes/{id}/inline-questions` theo BR-U10-02, 03.
4. Sắp thứ tự, đặt điểm từng câu; lưu kèm `version`.
5. Sửa tiêu đề, hướng dẫn, cài đặt, theo dõi hoạt động, tài liệu gắn kèm: `PATCH /api/v1/quizzes/{id}` kèm `version`; bật theo dõi cần `activity.retentionDays` (đọc qua `SettingsPort`) (BR-U10-15).
6. Quiz đang `REVIEWED` thì về `DRAFT`.

### F8 - AI soạn câu cho Quiz (UC 48)
1. Trên Quiz Detail của Quiz `DRAFT` bấm "Soạn bằng AI", nhập yêu cầu, chọn tài liệu và nguồn như F4 bước 1.
2. Frontend gọi `POST /api/v1/quizzes/{id}/ai-drafts`; kiểm quyền và `DRAFT`.
3. Gọi `AiTaskPort.submit` (loại `QUIZ_QUESTION_DRAFT`, đích là Quiz, `inputVersion` là `version` hiện tại); tạo đề xuất `PENDING`.
4. `onReady`: Quiz còn `DRAFT` thì lưu đề xuất `READY`; không thì `REJECTED`.
5. Người soạn chọn, sửa, chấp nhận: câu thành câu riêng của Quiz, thêm vào cuối danh sách (không vượt 200 câu); kèm `version`; audit `QUIZ_AI_DRAFT_ACCEPTED`.

### F9 - Xem trước, duyệt, phát hành (UC 48)
1. Xem trước như người học: câu, lựa chọn, không có đáp án.
2. Duyệt: `POST /api/v1/quizzes/{id}/review` kèm `version`; kiểm BR-U10-21; lỗi thì trả danh sách cần sửa; đạt thì `REVIEWED`; audit.
3. Phát hành: `POST /api/v1/quizzes/{id}/publish` kèm `version`; kiểm lớp `OPEN` (`ClassAccessPort`) hoặc môn `ACTIVE` (`SubjectQueryPort`).
4. Một transaction: Quiz `OPEN`, `publishedAt`; có version cũ đang `OPEN` thì chuyển `RETIRED` và gọi `QuizLifecycleListener.onRetired` của nó; audit `QUIZ_PUBLISHED`.
5. Sau commit phát `quiz.published` (Quiz, lớp hoặc môn) cho U14.

### F10 - Ngưng Quiz (UC 48)
1. Bấm Ngưng, nhập lý do; `POST /api/v1/quizzes/{id}/retire`.
2. Kiểm quyền và `OPEN`.
3. Một transaction: `RETIRED`, gọi `onRetired` (U11 tự nộp lượt đang làm); audit `QUIZ_RETIRED`.

### F11 - Nhân bản, version mới, copy (UC 48)
1. Nhân bản: `POST /api/v1/quizzes/{id}/clone`; version mới: `POST .../versions` (kiểm chưa có version đang mở); copy giữa lớp: `GET /api/v1/classes/{classId}/quiz-copy-sources`, `POST /api/v1/classes/{classId}/quizzes:copy` (kiểm dạy cả hai lớp).
2. Một transaction: tạo Quiz `DRAFT` theo BR-U10-31 (câu ngân hàng giữ phiên bản ghim, câu riêng sao mới); audit `QUIZ_CLONED`, `QUIZ_VERSION_CREATED` hoặc `QUIZ_COPIED`.
3. Mở Quiz Detail của Quiz mới.

### F12 - Xóa Quiz nháp (UC 48)
1. Bấm Xóa, xác nhận; `DELETE /api/v1/quizzes/{id}` kèm `version`.
2. Kiểm quyền và Quiz chưa từng phát hành (BR-U10-24).
3. Xóa Quiz, câu riêng, đề xuất AI; audit `QUIZ_DELETED`.

## 4. MCP và unit khác

### F13 - Tool MCP
1. `read_quiz_context(quizId)`: kiểm actor là giảng viên hoặc Chủ nhiệm môn có quyền với Quiz; trả tiêu đề, cài đặt, câu, lựa chọn, đáp án, giải thích, `version`.
2. `save_quiz_draft(quizId, expectedVersion, questions)`: credential `READ_WRITE`, Quiz `DRAFT`, `expectedVersion` khớp; kiểm câu theo BR-U10-02, 03; lưu thành đề xuất `READY` nguồn MCP; người soạn chấp nhận như F8 bước 5.
3. Người học gọi thì bị từ chối (BR-U10-42).

### F14 - Port cho unit khác
1. U11 lấy Quiz người học được thấy (`QuizQueryPort.listForClass`), đề không có đáp án và cài đặt (`getStudentView`), chấm bài (`QuizScoringPort.score`) và đáp án để hiện sau khi nộp theo cài đặt (`answerReview`).
2. U13 lấy Quiz gắn một tài liệu (`listByMaterial`) để hiện trên Learning Material; U11 cài `QuizLifecycleListener` và slot `QuizDetailSection`.
3. U14 nhận `quiz.published` để báo trong app.
