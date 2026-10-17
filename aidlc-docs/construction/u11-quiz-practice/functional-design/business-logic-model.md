# U11 Quiz Practice — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U11 Quiz Practice thuộc Iter 3, người code: Người 1; UC 49–52. Lấy phần lượt Quiz của U11 cũ và chấm Quiz luyện tập của U15 cũ. Quiz chỉ có chế độ luyện tập (D01). Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Lịch sử luyện tập | UC 49 | Quiz Practice History |
| F2 Chi tiết Quiz | UC 50 | Quiz Practice Detail; danh sách Quiz trên Learning Material |
| F3 Bắt đầu hoặc tiếp tục lượt | UC 51 | Quiz Practice Detail, Learning Material |
| F4 Làm Quiz và tự lưu | UC 51 | Quiz Taking |
| F5 Nộp và chấm | UC 51 | Quiz Taking |
| F6 Tự nộp | - | - |
| F7 Xem kết quả | UC 52 | Quiz Result |
| F8 Ghi dữ liệu hoạt động | - | Quiz Taking |
| F9 Giảng viên xem lượt | UC 42 | Quiz Detail (phần của U11) |

## 2. Xem Quiz

### F1 - Lịch sử luyện tập (UC 49)
1. Student mở Quiz Practice History từ mục Luyện tập Quiz trên menu bên (U11 đăng ký `NavEntry`); frontend gọi `GET /api/v1/me/quizzes` kèm lớp.
2. Lấy các lớp `OPEN` đang học (`IClassAccess.listOpenClassesOf`), Quiz đang mở của từng lớp (`IQuizQuery.listForClass`) và các lượt của mình.
3. Ghép thành danh sách Quiz: số lượt, điểm cao nhất, điểm lượt gần nhất (chỉ khi Quiz cho hiện điểm), nhãn "chưa làm"; Quiz đã ngưng mà có lượt thì hiện để xem lại (BR-U11-02).
4. Bấm Quiz mở Quiz Practice Detail; bấm lượt đã nộp mở Quiz Result; lượt đang làm mở Quiz Taking.

### F2 - Chi tiết Quiz (UC 50)
1. Frontend gọi `GET /api/v1/me/quizzes/{quizId}` kèm `classId`.
2. Kiểm R5 với lớp và Quiz (BR-U11-01); không đạt thì "không tìm thấy".
3. Trả thông tin theo BR-U11-03 (`IQuizQuery.get`) và các lượt của mình.
4. Nút Làm Quiz hoặc Tiếp tục khi Quiz `OPEN` và còn lượt; không thì nêu lý do (đã ngưng, hết lượt).
5. Learning Material (U13) nhúng `MaterialQuizList` của U11 qua slot `LearningMaterialSection`: Quiz gắn tài liệu đó (`IQuizQuery.listByMaterial`) kèm lượt đã dùng, còn lại và nút Làm Quiz.

## 3. Làm Quiz

### F3 - Bắt đầu hoặc tiếp tục lượt (UC 51)
1. Bấm Làm Quiz (hoặc Tiếp tục); frontend gọi `POST /api/v1/quizzes/{quizId}/attempts` kèm `classId`.
2. Đã có lượt `IN_PROGRESS` thì trả lượt đó.
3. Khóa hàng theo (Quiz, người học); kiểm ghi danh, Quiz `OPEN` của lớp hoặc môn chứa lớp, còn lượt (BR-U11-10).
4. Tạo lượt: `attemptNo` kế tiếp, chụp theo BR-U11-11, sinh seed, `deadlineAt`.
5. Mở Quiz Taking.

### F4 - Làm Quiz và tự lưu (UC 51)
1. Quiz Taking gọi `GET /api/v1/quiz-attempts/{attemptId}`: đề không có đáp án (`getStudentView`), thứ tự theo seed, câu trả lời đã lưu, giờ còn lại.
2. Có đồng hồ khi Quiz giới hạn giờ; thanh điều hướng câu đánh dấu câu đã trả lời.
3. 2 giây sau lần chọn cuối hoặc khi rời trang: `PUT /api/v1/quiz-attempts/{attemptId}/answers` kèm `answerVersion`.
4. Kiểm chủ lượt, `IN_PROGRESS`, chưa quá `deadlineAt` cộng 30 giây, câu và lựa chọn thuộc đề; lệch `answerVersion` thì `409`; đạt thì lưu (BR-U11-12).

### F5 - Nộp và chấm (UC 51)
1. Bấm Nộp, xác nhận (báo số câu chưa trả lời); frontend lưu lần cuối rồi gọi `POST /api/v1/quiz-attempts/{attemptId}/submit`.
2. Lượt đã nộp thì trả kết quả cũ.
3. Kiểm chủ lượt, `IN_PROGRESS`, trước `deadlineAt` cộng 30 giây; quá giờ thì audit `QUIZ_SUBMIT_REJECTED` (scanner sẽ tự nộp).
4. Một transaction: `SUBMITTED`, `submittedAt`, `submitMode = MANUAL`; gọi `IQuizScoring.score` và lưu điểm, đúng sai từng câu (BR-U11-15); audit `QUIZ_ATTEMPT_SUBMITTED`.
5. Mở Quiz Result.

### F6 - Tự nộp
1. `IScheduledScanner` của U11 chạy mỗi phút: lượt `IN_PROGRESS` có `deadlineAt` cộng 30 giây đã qua được nộp bằng câu trả lời đã lưu, `AUTO_TIME_LIMIT`, chấm như F5 bước 4.
2. U10 ngưng Quiz hoặc phát hành version mới thì gọi `onRetired` của U11: trong cùng transaction nộp và chấm mọi lượt `IN_PROGRESS` của Quiz đó với `AUTO_RETIRED`.
3. Câu lệnh nộp có điều kiện trạng thái nên chạy lại không nộp hai lần.

### F7 - Xem kết quả (UC 52)
1. Frontend gọi `GET /api/v1/quiz-attempts/{attemptId}/result`.
2. Kiểm chủ lượt và lượt đã nộp.
3. Trả câu trả lời của mình, số câu đúng; điểm nếu Quiz cho hiện điểm; đáp án đúng và giải thích qua `answerReview` nếu Quiz đặt `AFTER_SUBMIT` (BR-U11-20).
4. Nút quay lại Quiz Practice Detail và Làm lại khi còn lượt.

## 4. Hoạt động và giảng viên

### F8 - Ghi dữ liệu hoạt động
1. Lượt có chính sách bật thì `useActivityTracker` (U08) trên Quiz Taking gửi lô sự kiện tới `POST /api/v1/quiz-attempts/{attemptId}/activity`.
2. U11 kiểm chủ lượt, `IN_PROGRESS`, chính sách đã chụp bật.
3. Gọi `IActivityWrite.record` với loại công việc `QUIZ_ATTEMPT`; U08 bỏ trùng, giới hạn tần suất, ghi giờ server.

### F9 - Giảng viên xem lượt (UC 42)
1. Trên Quiz Detail (U10), phần Lượt làm do U11 gắn qua `QuizDetailSection`; frontend gọi `GET /api/v1/classes/{classId}/quizzes/{quizId}/attempts`.
2. Kiểm giảng viên chính của lớp; Quiz là Quiz của lớp hoặc của môn chứa lớp.
3. Trả mỗi sinh viên: số lượt, điểm cao nhất, điểm lượt gần nhất, thời điểm nộp.
4. Mở một lượt: câu trả lời chỉ đọc, điểm, và `ActivityTimeline` (U08) với dữ liệu từ `IActivityQuery.summary` (BR-U11-31).
