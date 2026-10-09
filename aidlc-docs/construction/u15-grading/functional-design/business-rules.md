# U15 Grading - Business Rules

**Bản tài liệu 2026-10-09**: UC 37, 38, 39, 40 (chốt và công bố là luồng phụ của UC 38, 39) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Hỗ trợ: kết quả quiz luyện tập (UC 20, 21), điểm của Student trên Student Assignments, Assignment Detail, Submission History (UC 22, 23, 28), kết quả AI chấm Practice (UC 29, U13 ghi qua U15).

## 1. Quyền

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-01 | Chỉ giảng viên lớp (R3/R4: Teacher hoặc Subject Manager là giảng viên chính của lớp) xem bài nộp, chấm, chốt, công bố, sửa điểm và xem sổ điểm của lớp đó. Chủ nhiệm môn (R2) không chấm bài của môn ở lớp mình không dạy. Admin không vào lớp, không chấm, không xem sổ điểm. Ngoài quyền → 404 và audit. | UC 37–40; current SRS contract |
| BR-U15-02 | Student chỉ thấy điểm `PUBLISHED` và phản hồi của chính mình, và kết quả Practice của chính mình. | UC 21, 22, 23, 28, 29 |
| BR-U15-03 | Bài của lớp và bài của môn đều chấm theo lớp: mỗi đánh giá thuộc đúng một lớp (lớp của sinh viên hoặc của nhóm); giảng viên của lớp nào chấm, chốt, công bố cho sinh viên lớp đó. Bài của môn hiện trong Submission Detail và sổ điểm của từng lớp, mỗi lớp chỉ thấy bài nộp của lớp mình. | BR-U08-37; người dùng chốt 2026-10-09 |

## 2. Tự chấm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-10 | Quiz chỉ là quiz luyện tập (UC 20, 21): khi Student nộp, U11 gọi `PracticeResultPort.scoreQuiz`; `QuizScorer` chấm theo đáp án của version câu đã ghim: một đáp án đúng → đủ điểm câu; nhiều đáp án đúng phải chọn đúng hết mới có điểm (BR-U09-10). Kết quả ghi `evaluations` `kind = PRACTICE`, `DETERMINISTIC`, `PUBLISHED` cho chủ lượt, ngay trong transaction nộp. Không có quiz `GRADED`. | UC 20, 21; BR-U08-61; FR-030 |
| BR-U15-11 | Code Lab `GRADED` lấy điểm khi U13 báo qua `CodeGradedPort` (đánh giá `ATTEMPT` → `DRAFT`); `SANDBOX_ERROR` → giữ `PENDING`, hiện "chưa chấm được". Code Lab `PRACTICE` dùng cùng kết quả test, U13 ghi qua `PracticeResultPort.record` (`kind = PRACTICE`). | UC 25; BR-U13-35, FR-030 |
| BR-U15-12 | Không có điểm bài `GRADED` tự công bố khi nộp: điểm tự chấm của Code Lab chờ giảng viên chốt và công bố như mọi bài. Việc hiện điểm/đáp án của lượt quiz luyện tập theo cài đặt quiz (`showScoreAfterSubmit`, `showCorrectAnswers`, U09) do U11 hiển thị trên Quiz Result. | BR-U09-13; người dùng chốt 2026-10-09 |
| BR-U15-13 | Giảng viên sửa điểm tự chấm của Code Lab được, bắt buộc lý do. | US-GRD-003; UC 39 |

## 3. Chấm bài viết/tài liệu

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-20 | Chỉ bài `GRADED` (Text Essay, Diagram Essay, Code Lab, bài nhóm) tạo đánh giá trong Submission Detail; giảng viên chấm tay (UC 39) hoặc nhờ AI đề xuất (UC 38); điểm lưu như nhau (người chấm, thời điểm), không ghi riêng là có dùng AI. Bài `PRACTICE` và quiz không vào Submission Detail; giảng viên không chấm Practice. | FR-008, FR-030, US-GRD-002; UC 37–39 |
| BR-U15-21 | Mọi bài giảng viên chấm theo tiêu chí đều có rubric của chính bài (U06, khóa khi phát hành): Text Essay một rubric mỗi câu; Diagram Essay và bài nhóm một rubric mỗi phần của khung. Chấm tay (UC 39) là tích checklist rubric của từng câu/phần, điểm tính bằng `RubricPort.score`, điểm bài là tổng điểm các câu/phần; phản hồi tùy chọn. Không có ô nhập điểm cho câu không rubric. Không gọi AI. Code Lab chấm theo test (BR-U15-11). | UC 39; BR-U06-32…34; người dùng chốt 2026-10-04 |
| BR-U15-22 | Nhờ AI (UC 38) trong panel AI đề xuất của Grading Workspace (U15 tự làm, không dùng popup của U11): chỉ bài có rubric (Text Essay, Diagram Essay, tài liệu chung của bài nhóm); U13 tạo đề xuất theo rubric của bài, trừ credit của giảng viên; đề xuất chỉ để tham khảo, điền sẵn checklist và phần giải thích khi giảng viên bấm "Dùng đề xuất". Giảng viên sửa phần giải thích của AI nếu muốn; điểm khác đề xuất không cần ghi lý do. | UC 38; US-GRD-002 S1, US-GRD-003; người dùng chốt 2026-10-04 |
| BR-U15-23 | AI lỗi → bài nộp không mất, không có điểm giả; giảng viên chấm tay hoặc thử lại. | US-GRD-002 S3 |
| BR-U15-24 | Chấm hàng loạt bằng AI: trong Submission Detail, giảng viên chọn nhiều bài cần chấm theo rubric và bấm "Chấm hàng loạt bằng AI". U13 kiểm đủ credit cho cả lô (thiếu thì báo, không chạy), tạo một đề xuất cho mỗi bài và xử lý nền theo lô; lô tính một lần vào giới hạn tần suất. Xong thì giảng viên mở Grading Workspace để xác nhận từng bài (dùng hoặc sửa đề xuất rồi lưu). | UC 38; người dùng chốt 2026-10-04 |
| BR-U15-25 | Grading Workspace có nút ‹ › chuyển giữa các bài nộp của cùng bài trong lớp (theo bộ lọc hiện tại của Submission Detail, hoặc theo lô vừa chấm); lưu điểm một bài thì tự chuyển sang bài kế. | Người dùng chốt 2026-10-04 |

## 4. Thang điểm, chốt và công bố

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-30 | Mọi loại bài hiển thị "x / tổng điểm của bài" (tổng điểm câu/rubric/test), 2 chữ số thập phân; không quy đổi thang 10. | Câu 2, 3 |
| BR-U15-31 | Chốt từng bài (Grading Workspace) hoặc hàng loạt (Submission Detail, hộp xác nhận): chỉ đánh giá `DRAFT` có `score` hợp lệ và `version` khớp; kết quả từng bài trả về; lỗi từng mục không ảnh hưởng mục khác. Chốt là luồng phụ của UC 38, 39. | US-GRD-005 |
| BR-U15-32 | Công bố từng bài nộp (Grading Workspace) hoặc công bố hàng loạt (Submission Detail: chọn nhiều bài `FINALIZED`, có "chọn tất cả đã chốt", rồi bấm "Công bố hàng loạt"); mỗi bài → `PUBLISHED`, kết quả trả từng mục. Thời điểm công bố của (bài, lớp) không lưu riêng mà suy ra từ `published_at` của các đánh giá đã công bố của lớp đó (lần đầu = MIN, gần nhất = MAX); không dùng cột `assignments.grades_released_at`. Mỗi lần công bố phát một `grade.published` kèm bài, lớp và danh sách người học vừa có điểm. Bài của môn công bố riêng ở từng lớp. | Câu 4; BR-U08-37; người dùng chốt 2026-10-04, 2026-10-09 |
| BR-U15-33 | Sửa điểm `FINALIZED`/`PUBLISHED` bắt buộc lý do; thêm phần tử `history`; điểm đã công bố sửa thì người học thấy điểm mới và nhãn "đã cập nhật". | FR-008, US-GRD-003 S2 |
| BR-U15-34 | Bài nộp trễ hiển thị nhãn trễ; giảng viên tự trừ điểm (nếu muốn) qua sửa điểm có lý do; hệ thống không tự trừ. | BR-U08-32 |
| BR-U15-35 | Với bài `GRADED`, lượt nộp cuối là lượt tính điểm chính thức (U11). Mọi kết quả `PRACTICE` (quiz luyện tập, Practice Code Lab, AI chấm Practice của UC 29) nằm ngoài sổ điểm, Submission Detail và bước chốt/công bố. | BR-U11-31, FR-030 |

## 5. Bài nhóm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-40 | Tài liệu nhóm (bản nộp cuối U14, đánh giá `GROUP_DOCUMENT`) chấm như bài `DOCUMENT`: chấm tay theo rubric của từng phần (UC 39) hoặc nhờ AI đề xuất (UC 38); điểm tài liệu chung là tổng điểm các phần; tài liệu hiển thị bình thường, không tô màu theo tác giả. Bài nhóm chỉ là bài của lớp. | US-GRP-006; người dùng chốt 2026-10-04 |
| BR-U15-41 | Mỗi thành viên có điểm đóng góp riêng (`MEMBER.score`), mặc định bằng điểm tài liệu chung nên mọi thành viên như nhau; giảng viên chấm tay điểm đóng góp từng người nếu cần. Điểm đóng góp không gửi AI chấm. | Người dùng chốt 2026-10-04 |
| BR-U15-42 | Không áp công thức ghép điểm; khi điểm đóng góp của một thành viên khác điểm tài liệu chung, ghi lý do là tùy chọn. | Người dùng chốt 2026-10-04 |
| BR-U15-43 | Không có điểm tích hợp riêng: lỗi các phần không khớp nhau khi ghép (ví dụ tên, phần tử lệch giữa các sơ đồ) trừ ở rubric của phần liên quan, nên điểm tài liệu chung vẫn bằng tổng các phần. Trừ thêm cho một thành viên thì giảng viên sửa điểm đóng góp của người đó, ghi lý do và phần liên quan nếu muốn. | US-GRP-006 S4; người dùng chốt 2026-10-04 |
| BR-U15-44 | Nhóm nộp lại: bản nộp mới ghi đè `submitted_snapshot` của tài liệu nhóm, không tạo lượt mới (BR-U14-32, quyết định 2026-10-06); bản nộp cuối là bản được chấm; đánh giá `GROUP_DOCUMENT` và `MEMBER` quay về `PENDING` cho bản mới, điểm trước đó giữ trong `history`. | Người dùng chốt 2026-10-04 |

## 6. Sổ điểm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-50 | Sổ điểm lớp (GradeBook, UC 40, mở bằng "Sổ điểm" trên tab Evals của Teacher Class Detail) chỉ gồm bài `GRADED` của lớp và bài `GRADED` của môn chứa lớp; gom theo sinh viên đang ghi danh: mỗi sinh viên là một mục đóng/mở, mở ra thấy các bài với điểm lượt nộp cuối "x / tổng" và trạng thái (chưa nộp, trễ, chờ chấm, đã chốt, đã công bố). Không có quiz, kết quả `PRACTICE` hoặc điểm AI đề xuất. **Không tính điểm tổng.** | UC 40; FR-030, US-GRD-004 S2; người dùng chốt 2026-10-04 |
| BR-U15-51 | Student xem điểm đã công bố và phản hồi của từng bài cạnh bài trên Student Assignments (UC 22), trong Assignment Detail và Submission History (UC 23, 28); không có trang My Grades riêng, không mở sổ điểm của giảng viên. | US-GRD-004 S1; người dùng chốt 2026-10-04 |
| BR-U15-52 | Lịch sử điểm chỉ giảng viên lớp (R3/R4) xem, trong Grading Workspace và từ sổ điểm; giữ actor/thời điểm/trước-sau/lý do. Chủ nhiệm môn không tự có quyền. | FR-009/024, UC 40 |
| BR-U15-53 | Audit: lưu điểm, chốt, công bố, sửa điểm, yêu cầu AI đề xuất, truy cập trái phép. | FR-014 |
| BR-U15-54 | Xin gia hạn cá nhân, phúc khảo điểm và kiểm tra tương đồng bài nộp nằm ngoài phạm vi dự án, không thiết kế hoặc triển khai. | Quyết định phạm vi 2026-09-25 |
| BR-U15-55 | Xuất bảng điểm (phần "Export" của UC 40) do U16 tạo tệp CSV/XLSX từ `GradebookQueryPort`; nút xuất nằm trên sổ điểm; cùng quyền R3/R4 và cùng phạm vi bài như BR-U15-50. | UC 40; BR-U16-43…45 |
