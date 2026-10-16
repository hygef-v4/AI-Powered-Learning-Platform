# U11 Quiz Practice — business-rules

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U11 Quiz Practice thuộc Iter 3, người code: Người 1; UC 49–52. Lấy phần lượt Quiz của U11 cũ và chấm Quiz luyện tập của U15 cũ. Quiz chỉ có chế độ luyện tập (D01). Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Xem Quiz (UC 49, 50)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U11-01 | Chỉ Student (R5) làm Quiz; Teacher, Subject Manager, Admin không có màn hay API làm bài. Người học thấy Quiz `OPEN` của lớp `OPEN` mình đang ghi danh (Quiz của lớp và Quiz của môn chứa lớp, theo `IQuizQuery` của U10). | FR-007, BR-U10-25 |
| BR-U11-02 | Quiz Practice History: danh sách gộp các lớp `OPEN` đang học, gồm Quiz đang mở và Quiz mình đã làm (kể cả Quiz đã ngưng); mỗi Quiz hiện lớp, số lượt đã làm, điểm cao nhất và điểm lượt gần nhất nếu Quiz cho hiện điểm, nhãn "chưa làm"; lọc theo lớp; Quiz đã ngưng chỉ xem lại lượt cũ. | UC 49 |
| BR-U11-03 | Quiz Practice Detail: tiêu đề, lớp, tài liệu gắn kèm (U13 hiện qua slot `QuizMaterialSection`), số câu, tổng điểm, giới hạn giờ, số lượt đã dùng và còn lại (hoặc "không giới hạn"), cài đặt hiện điểm và đáp án, thông báo theo dõi hoạt động nếu bật, danh sách lượt của mình. Nút Làm Quiz hoặc Tiếp tục chỉ hiện khi Quiz `OPEN` và còn lượt; không thì nêu lý do. | UC 50 |

## 2. Lượt làm (UC 51)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U11-10 | Bấm Làm Quiz tạo lượt và tính vào số lượt; hết lượt thì từ chối. Mỗi người tối đa một lượt `IN_PROGRESS` mỗi Quiz; bấm lại thì mở lượt đó. Lượt ghi `classId` là lớp người học làm Quiz. | FR-007 |
| BR-U11-11 | Lượt chụp version Quiz, cài đặt, chính sách theo dõi và seed trộn; thứ tự câu và đáp án theo seed khi Quiz bật trộn. `deadlineAt` là giờ bắt đầu cộng giới hạn giờ, trống nếu Quiz không giới hạn. Sửa Quiz sau đó không ảnh hưởng lượt đã có. | FR-018 |
| BR-U11-12 | Đề lấy qua `getStudentView` của U10, không có đáp án. Tự lưu câu trả lời 2 giây sau lần chọn cuối và khi rời trang, kèm `answerVersion`; lệch thì `409`. Lưu sau `deadlineAt` bị từ chối, trừ lần lưu cuối trong 30 giây ân hạn. | FR-018 |
| BR-U11-13 | Nộp tay: lượt của mình, `IN_PROGRESS`, trước `deadlineAt` cộng 30 giây; câu chưa trả lời được tính sai. Nộp lặp trả cùng kết quả. | UC 51 |
| BR-U11-14 | Tự nộp câu trả lời đã lưu gần nhất: hết giờ (`AUTO_TIME_LIMIT`, scanner mỗi phút) và khi Quiz bị ngưng hoặc có version mới được phát hành (`AUTO_RETIRED`, trong `onRetired`). | US-ASM-003 |
| BR-U11-15 | Nộp thì câu trả lời bất biến; chấm ngay trong transaction nộp bằng `IQuizScoring.score` của U10 (đáp án ghim, `MCQ_MULTI` đúng hết mới có điểm); lưu điểm và đúng sai từng câu trên lượt. | US-ASM-003 S2, BR-U10-16 |

## 3. Kết quả (UC 52)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U11-20 | Quiz Result: câu trả lời của mình từng câu, số câu đúng; điểm khi Quiz bật hiện điểm; đáp án đúng và giải thích (qua `answerReview` của U10) khi Quiz đặt `AFTER_SUBMIT`, `NEVER` thì không hiện. Có nút quay lại và Làm lại khi còn lượt. | UC 52 |
| BR-U11-21 | Kết quả là luyện tập: mỗi lượt giữ riêng, không vào sổ điểm, không chốt hay công bố. | FR-007, D01 |
| BR-U11-22 | Người học chỉ xem lượt của mình; ID của người khác trả "không tìm thấy". | US-ASM-003 S3 |

## 4. Giảng viên và dữ liệu hoạt động

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U11-30 | Quiz bật theo dõi: Quiz Taking gửi sự kiện qua `useActivityTracker`; U11 kiểm chủ lượt, `IN_PROGRESS`, chính sách đã chụp rồi gọi `IActivityWrite` của U08. | FR-039 |
| BR-U11-31 | Giảng viên chính của lớp xem lượt Quiz của sinh viên lớp mình trên Quiz Detail (U11 gắn qua slot `QuizDetailSection` của U10): số lượt, điểm cao nhất, điểm lượt gần nhất, thời điểm nộp, câu trả lời từng lượt (chỉ đọc) và dòng thời gian hoạt động (`IActivityQuery`). Không sửa kết quả; Admin, Chủ nhiệm môn không dạy lớp không xem. | UC 42, US-INT-001 S3 |

## 5. Audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U11-40 | Audit: `QUIZ_ATTEMPT_SUBMITTED` (tay hoặc tự), `QUIZ_SUBMIT_REJECTED` (quá giờ, hết lượt). Không audit từng lần lưu và từng sự kiện hoạt động. | FR-014 |
| BR-U11-41 | Mọi API kiểm quyền phía server; ngoài phạm vi trả `404` và ghi audit `ACCESS_DENIED`. U01, U05, U10 không trả lời thì từ chối (fail closed). | SEC-002 |
