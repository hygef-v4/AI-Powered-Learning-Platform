# U11 Attempt & Submission - Business Rules

**Bản tài liệu 2026-10-09**: UC 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); danh sách quiz của học liệu trên Learning Material (UC 15, màn của U05); primary stories: US-ASM-003, US-ASM-012. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Bắt đầu lượt

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-01 | Bài tập: chỉ người học ghi danh `ACTIVE` của lớp `OPEN`, bài thuộc lớp đó hoặc là bài của môn chứa lớp, bài đang nhận bài nộp (`isSubmissionOpen` trả `ON_TIME` hoặc `LATE`). Quiz: quiz `OPEN` của lớp hoặc của môn chứa lớp, học liệu của quiz còn hiện trong lớp (BR-U08-63); quiz `OPEN` luôn `ON_TIME`. | FR-007, US-ASM-003 S2, BR-U08-03, 63 |
| BR-U11-02 | Bấm "Bắt đầu làm"/"Làm quiz" tạo lượt và tính vào số lượt; chụp version bài, cấu hình (gồm cài đặt quiz), chính sách hạn và seed trộn. | Câu 2 |
| BR-U11-03 | Mỗi người học tối đa một lượt `IN_PROGRESS` mỗi bài hoặc quiz; bấm lại thì mở lượt đang làm. | Câu 2 |
| BR-U11-04 | Số lượt theo `maxAttempts` của U08: bài tập 1-10 (cả `GRADED` và `PRACTICE`); quiz 1-10 hoặc không giới hạn. Hết lượt → từ chối. | FR-007, BR-U08-62 |
| BR-U11-05 | Chỉ Student (R5) dùng chức năng của U11; Teacher, Subject Manager, Admin không có màn hay API làm bài (Admin không vào `/classes/*`). Mỗi lượt ghi `classId` là lớp người học làm bài (lớp của bài, hoặc lớp người học đang ghi danh với bài/quiz của môn) để giảng viên lớp đó chấm và để lọc theo lớp. | Người dùng chốt 2026-10-09; BR-U08-37 |
| BR-U11-06 | `deadlineAt`: quiz = `startedAt + timeLimitMinutes` nếu có, không thì rỗng (quiz không có hạn đóng); bài tập = hạn cuối nhận bài (`lateUntil` nếu cho nộp trễ, không thì `closesAt`). Bài tập không có giới hạn giờ làm. | BR-U09-12, BR-U08-32 |

## 2. Làm bài và lưu nháp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-10 | Tự lưu 10 giây sau lần sửa cuối và khi rời trang; hiện "Đã lưu lúc …". | US-ASM-003 S4 |
| BR-U11-11 | Lưu gửi kèm `contentVersion`; lệch → `409` "bài đang được mở ở nơi khác, tải lại"; không ghi đè im lặng. | Thiết kế |
| BR-U11-12 | Lưu nháp kiểm cấu trúc (`DocumentModelPort.validateForSave` cho tài liệu; câu/lựa chọn tồn tại cho quiz). | BR-U09-36 |
| BR-U11-13 | Bản nháp không phải bài nộp; chỉ chính người học đọc/sửa. | US-ASM-003 S4 |
| BR-U11-14 | Lưu sau `deadlineAt` bị từ chối (trừ lần lưu cuối gửi trong 30 giây ân hạn mạng). Lượt không có `deadlineAt` (quiz không giới hạn giờ) lưu được tới khi nộp hoặc quiz bị ngưng. | Thiết kế |
| BR-U11-15 | Codelab Workspace: nút "Chạy thử" gọi U13 với test công khai; không tính là nộp. | UC 25 |

## 3. Nộp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-20 | Nộp tay kiểm: lượt `IN_PROGRESS` của chính mình, trước `deadlineAt` (+30 giây ân hạn) nếu có, nội dung hợp lệ (`validateForSubmit` cho tài liệu). | US-ASM-003 S1, S2 |
| BR-U11-21 | Nộp: nội dung → bất biến, `submittedAt` theo giờ server, `late` nếu bài tập nộp sau `closesAt`, `receiptHash`. Chỉ bài tập `GRADED` gọi `SubmissionSubmittedPort` để U15 tạo dòng đánh giá chờ chấm; quiz (luôn `PRACTICE`) gọi `PracticeResultPort.scoreQuiz` (U15 chấm bằng `QuizScorer`); `PRACTICE` Code Lab gọi `CodeRunPort.grade` (U13 chạy mọi test rồi ghi kết quả qua U15); `PRACTICE` Text/Diagram Essay **không tự chấm khi nộp**: lượt ở trạng thái chưa chấm AI, chờ Student bấm chấm (BR-U11-35). Quiz và Practice không vào sổ điểm. | FR-007, FR-030, BR-U08-61 |
| BR-U11-22 | Biên nhận hiển thị: mã lượt, thời điểm nộp, lượt thứ mấy, trễ hay không, mã băm. | UC 24, 25, 26 |
| BR-U11-23 | Tự nộp **bài hiện tại** (bản đã lưu gần nhất, kể cả lần lưu cuối client gửi khi hết giờ) khi: hết giới hạn giờ của quiz (`AUTO_TIME_LIMIT`), hết hạn nhận bài tập (`AUTO_DEADLINE`), bài bị ngưng giao, quiz bị ngưng hoặc có version mới được phát hành (`AUTO_RETIRED`). Không kiểm điều kiện nộp; lỗi điều kiện ghi thành cảnh báo cho giảng viên. Học liệu bị lưu trữ không tự nộp lượt quiz đang làm. | Câu 3, 4; BR-U08-40, 64 |
| BR-U11-24 | Lượt rỗng (chưa lưu gì) khi tự nộp vẫn được nộp với nội dung rỗng. | Câu 3 |

## 4. Danh sách, chi tiết, lịch sử và lượt được chấm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-30 | Mỗi lượt giữ nguyên, lượt mới không ghi đè. | US-ASM-003 S5 |
| BR-U11-31 | Bài tập `GRADED`: lượt được chấm chính thức là **lượt nộp cuối**; giao diện đánh dấu rõ. Bài `PRACTICE` và quiz: mỗi lượt giữ kết quả luyện tập riêng, không chọn lượt cho sổ điểm. | FR-007, FR-030 |
| BR-U11-32 | Người học chỉ xem lượt của mình; ID của người khác → "không tìm thấy". | US-ASM-003 S3 |
| BR-U11-33 | Bài tập `GRADED` chỉ hiện điểm/phản hồi đã công bố của U15; `PRACTICE` hiện kết quả từng lượt riêng (điểm và kết quả test Code Lab không lộ nội dung test ẩn; điểm và phản hồi AI Text/Diagram Essay), hoặc trạng thái "chưa chấm AI" kèm nút chấm; quiz theo BR-U11-54. | FR-030, BR-U09-13 |
| BR-U11-34 | Người học tải bài Text Essay/Diagram Essay của mình ra DOCX (U09). | BR-U09-52 |
| BR-U11-35 | Chấm AI bài Practice (UC 29): trên Submission History, Student chọn lượt đã nộp của mình và bấm "Chấm với AI"; U11 kiểm chủ lượt, lượt đã nộp, bài `PRACTICE` Text/Diagram Essay rồi gọi `PracticeGradingPort` (U13); U13 kiểm credit tại lúc bấm. Thiếu credit thì báo "Không đủ credit AI", không gọi AI; mua thêm rồi bấm lại được. Mỗi lượt tối đa một kết quả AI hợp lệ; quá 5 phút chưa có kết quả thì báo lỗi (BR-U13-24) và được bấm lại. Chỉ Student đó xem kết quả. | FR-008, FR-030, UC 29; US-ASM-012 |
| BR-U11-36 | Student Assignments (UC 22) mở từ Class Dashboard là **một danh sách gộp** các lớp `OPEN` mình đang ghi danh: bài của lớp và bài của môn (Text Essay, Code Lab, Diagram Essay, bài nhóm), không gồm quiz; lọc theo lớp (mở từ Student Class Detail thì lọc sẵn lớp đó), dạng bài, trạng thái. Mỗi bài hiện lớp, dạng, chế độ, nhãn "Của môn", hạn, lượt gần nhất (chưa làm, đang làm, đã nộp, trễ, hết hạn; bài nhóm là trạng thái nộp của nhóm), điểm đã công bố (U15) và phân bố điểm ẩn danh (U16). Bấm bài → Assignment Detail. | UC 22; BR-U08-03; người dùng chốt 2026-10-09 |
| BR-U11-37 | Assignment Detail (UC 23): hướng dẫn, đề từng câu (Text Essay, Code Lab) hoặc khung tài liệu (Diagram Essay, bài nhóm), tiêu chí chấm (rubric từng câu/phần, điểm từng câu), lịch, số lượt còn lại, lượt gần nhất; không lộ đáp án, `answerGuide`, test ẩn, lời giải mẫu. Nút "Bắt đầu làm"/"Tiếp tục" chỉ hiện khi đủ BR-U11-01, 04; bài nhóm có nút "Mở bài nhóm" (U14) thay cho nút làm bài; nút "Lịch sử nộp" mở Submission History. | UC 23; US-ASM-003 S6 |
| BR-U11-38 | Submission History (UC 28) mở từ Assignment Detail: danh sách lượt của mình (lượt thứ, thời điểm nộp, trễ, cách nộp, "được chấm", điểm nếu được hiện) và lượt đang xem (mặc định lượt gần nhất, nút ‹ › chuyển lượt), chỉ đọc, kèm biên nhận, tải DOCX và chấm AI Practice; không đổi quyền chủ lượt. | UC 28 |

## 5. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-40 | Audit: nộp (tay/tự), từ chối nộp do quá hạn/hết lượt, truy cập bài người khác bị chặn, yêu cầu chấm AI Practice. Không audit từng lần lưu nháp. | FR-014 |

## 6. Quiz luyện tập

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-50 | Quiz Practice History (UC 18) mở từ Class Dashboard: danh sách gộp lượt quiz của mình ở các lớp `OPEN` đang học, nhóm theo quiz, mới nhất trước, lọc theo lớp (mở từ một lớp thì lọc sẵn). Mỗi lượt: quiz, học liệu, lớp, lượt thứ, thời điểm nộp, điểm nếu `showScoreAfterSubmit`. Bấm quiz → Quiz Practice Detail; bấm lượt đã nộp → Quiz Result; lượt đang làm → Quiz Taking. Lượt của quiz đã `RETIRED` hoặc học liệu đã lưu trữ vẫn xem được (chỉ đọc). | UC 18; BR-U08-63 |
| BR-U11-51 | Quiz Practice Detail (UC 19): tiêu đề, học liệu, lớp, số câu, tổng điểm, giới hạn giờ, số lượt đã dùng/còn lại (hoặc "không giới hạn"), cài đặt hiện điểm/đáp án. Nút "Làm quiz"/"Tiếp tục" → Quiz Taking khi quiz `OPEN`, học liệu còn hiện và còn lượt; không thì ẩn nút và nêu lý do. | UC 19 |
| BR-U11-52 | Learning Material (UC 15, U05) nhúng `LessonQuizList` của U11: quiz `OPEN` của học liệu trong lớp (`listQuizzesOfLesson`), mỗi quiz có tiêu đề, số câu, giới hạn giờ, lượt đã dùng/còn lại, điểm lượt gần nhất nếu được hiện; nút "Làm quiz" bắt đầu hoặc tiếp tục lượt rồi mở Quiz Taking. | UC 15, 20; BR-U05-03 |
| BR-U11-53 | Quiz Taking (UC 20): thứ tự câu/đáp án theo seed của lượt khi `shuffleQuestions`/`shuffleOptions` bật; đồng hồ khi có `timeLimitMinutes`; tự lưu; nộp tay hoặc hết giờ tự nộp; nộp xong mở Quiz Result. | UC 20; BR-U09-11, 12 |
| BR-U11-54 | Quiz Result (UC 21): lựa chọn của mình từng câu; điểm khi `showScoreAfterSubmit`; đáp án đúng và giải thích khi `showCorrectAnswers = AFTER_SUBMIT`; `NEVER` thì không hiện đáp án đúng. Không có `AFTER_CLOSE`. Kết quả không vào sổ điểm. | UC 21; BR-U09-13 |
