# U15 Grading - Business Rules

## 1. Quyền

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-01 | Chỉ giảng viên của lớp chấm, chốt, công bố, sửa điểm; ngoài quyền → từ chối và audit vi phạm. | US-GRD-003 S4 |
| BR-U15-02 | Người học chỉ xem điểm `PUBLISHED` của chính mình; Chủ nhiệm môn của môn xem sổ điểm (chỉ đọc); ADMIN không xem sổ điểm. | US-GRD-004 |

## 2. Tự chấm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-10 | Trắc nghiệm dùng đáp án phiên bản bài để chấm khi nộp: một đáp án đúng → đủ điểm câu; nhiều đáp án đúng hết mới có điểm (BR-U09-10). Với `GRADED`, trong transaction nộp tạo `evaluations` `ATTEMPT` `DETERMINISTIC`, `DRAFT`; với `PRACTICE`, cùng scorer ghi `evaluations` `PRACTICE` (ngoài sổ điểm). | Câu 1, US-GRD-001, FR-030 |
| BR-U15-11 | Code Lab `GRADED` lấy điểm khi U13 báo qua `CodeGradedPort`; `SANDBOX_ERROR` → giữ `PENDING`, hiện "chưa chấm được". Code Lab `PRACTICE` dùng cùng kết quả test, ghi `evaluations` `PRACTICE`. | Câu 1, BR-U13-35, FR-030 |
| BR-U15-12 | Bài bật "hiện điểm ngay sau nộp" → điểm tự chấm `PUBLISHED` luôn; không bật → chờ chốt và công bố. | BR-U09-13, US-GRD-001 S2 |
| BR-U15-13 | Giảng viên sửa điểm tự chấm được, bắt buộc lý do. | US-GRD-003 |

## 3. Chấm bài viết/tài liệu

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-20 | Chỉ bài `GRADED` cần Teacher chấm mới tạo đánh giá `PENDING` trong hàng chấm; Teacher chấm tay hoặc nhờ AI đề xuất; điểm lưu như nhau (người chấm, thời điểm), không ghi riêng là có dùng AI. Bài `PRACTICE` không vào hàng đợi này. | FR-008, FR-030, US-GRD-002 |
| BR-U15-21 | Mọi bài giảng viên chấm đều có rubric (Text Essay: một rubric mỗi câu; Diagram Essay và bài nhóm: một rubric mỗi phần của khung); chấm tay là tích checklist rubric của từng câu/phần, điểm tính bằng `RubricPort.score`, điểm bài là tổng điểm các câu/phần; phản hồi tùy chọn. Không có ô nhập điểm cho câu không rubric. Không gọi AI. | US-GRD-002 S2; người dùng chốt 2026-10-04 |
| BR-U15-22 | Nhờ AI: U13 tạo đề xuất (trừ credit giảng viên); đề xuất chỉ để tham khảo, điền sẵn checklist và phần giải thích khi giảng viên bấm "Dùng đề xuất". Giảng viên sửa phần giải thích của AI nếu muốn; điểm khác đề xuất không cần ghi lý do. | US-GRD-002 S1, US-GRD-003; người dùng chốt 2026-10-04 |
| BR-U15-23 | AI lỗi → bài nộp không mất, không có điểm giả; giảng viên chấm tay hoặc thử lại. | US-GRD-002 S3 |
| BR-U15-24 | Chấm hàng loạt: trong Grading Queue, giảng viên chọn nhiều bài cần chấm theo rubric và bấm "Chấm hàng loạt bằng AI". U13 kiểm đủ credit cho cả lô (thiếu thì báo, không chạy), tạo một đề xuất cho mỗi bài và xử lý nền theo lô; lô tính một lần vào giới hạn tần suất. Xong thì giảng viên mở Grading Workspace để xác nhận từng bài (dùng hoặc sửa đề xuất rồi lưu). | Người dùng chốt 2026-10-04 |
| BR-U15-25 | Grading Workspace có nút ‹ › chuyển giữa các bài trong hàng chờ (theo bộ lọc hiện tại, hoặc theo lô vừa chấm); lưu điểm một bài thì tự chuyển sang bài kế. | Người dùng chốt 2026-10-04 |

## 4. Thang điểm, chốt và công bố

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-30 | Mọi loại bài hiển thị "x / tổng điểm của bài" (tổng điểm câu/rubric), 2 chữ số thập phân; không quy đổi thang 10. | Câu 2, 3 |
| BR-U15-31 | Chốt từng bài hoặc hàng loạt: chỉ đánh giá `DRAFT` có `score` hợp lệ và `version` khớp; kết quả từng bài trả về; lỗi từng mục không ảnh hưởng mục khác. | US-GRD-005 |
| BR-U15-32 | Công bố từng bài nộp (trong Grading Workspace) hoặc công bố hàng loạt: trong Grading Queue chọn nhiều bài `FINALIZED` (có "chọn tất cả đã chốt") rồi bấm "Công bố hàng loạt"; mỗi bài → `PUBLISHED`, kết quả trả từng mục. Lần công bố đầu của bài ghi `assignments.grades_released_at`. Mỗi lần công bố (từng bài hoặc hàng loạt) phát một `grade.published` kèm bài và danh sách người học vừa có điểm. | Câu 4; người dùng chốt 2026-10-04 |
| BR-U15-33 | Sửa điểm `FINALIZED`/`PUBLISHED` bắt buộc lý do; thêm phần tử `history`; điểm đã công bố sửa thì người học thấy điểm mới và nhãn "đã cập nhật". | FR-008, US-GRD-003 S2 |
| BR-U15-34 | Bài nộp trễ hiển thị nhãn trễ; giảng viên tự trừ điểm (nếu muốn) qua sửa điểm có lý do; hệ thống không tự trừ. | U08 BR-U08-32 |
| BR-U15-35 | Với bài `GRADED`, lượt nộp cuối là lượt tính điểm chính thức (U11). Mọi kết quả `PRACTICE` (đánh giá `kind = PRACTICE`) nằm ngoài sổ điểm, hàng chấm và bước công bố. | BR-U11-31, FR-030 |

## 5. Bài nhóm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-40 | Tài liệu nhóm (bản nộp cuối U14, đánh giá `GROUP_DOCUMENT`) chấm như bài `DOCUMENT`: chấm tay theo rubric của từng phần hoặc nhờ AI đề xuất (UC 40); điểm tài liệu chung là tổng điểm các phần; tài liệu hiển thị bình thường, không tô màu theo tác giả. | Người dùng chốt 2026-10-04 |
| BR-U15-41 | Mỗi thành viên có điểm đóng góp riêng (`MEMBER.score`), mặc định bằng điểm tài liệu chung nên mọi thành viên như nhau; giảng viên chấm tay điểm đóng góp từng người nếu cần. Điểm đóng góp không gửi AI chấm. | Người dùng chốt 2026-10-04 |
| BR-U15-42 | Không áp công thức ghép điểm; khi điểm đóng góp của một thành viên khác điểm tài liệu chung, ghi lý do là tùy chọn. | Người dùng chốt 2026-10-04 |
| BR-U15-43 | Không có điểm tích hợp riêng: lỗi các phần không khớp nhau khi ghép (ví dụ tên, phần tử lệch giữa các sơ đồ) trừ ở rubric của phần liên quan, nên điểm tài liệu chung vẫn bằng tổng các phần. Trừ thêm cho một thành viên thì giảng viên sửa điểm đóng góp của người đó, ghi lý do và phần liên quan nếu muốn. | US-GRP-006 S4; người dùng chốt 2026-10-04 |
| BR-U15-44 | Nhóm nộp lại xử lý như lượt nộp mới của bài `DOCUMENT`: bản nộp cuối là bản được chấm; đánh giá `GROUP_DOCUMENT` và `MEMBER` quay về `PENDING` cho bản mới, điểm trước đó giữ trong `history`. | Người dùng chốt 2026-10-04 |

## 6. Sổ điểm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-50 | Sổ điểm lớp (màn Gradebook, UC 35 phía giảng viên/Chủ nhiệm môn) chỉ gồm bài `GRADED`, gom theo sinh viên: mỗi sinh viên là một mục đóng/mở, mở ra thấy các bài với điểm lượt nộp cuối "x / tổng" và trạng thái (chưa nộp, trễ, chờ chấm, đã chốt, đã công bố). Không đưa kết quả `PRACTICE` hoặc điểm AI luyện tập vào sổ điểm/xuất bảng điểm. **Không tính điểm tổng.** | FR-030, US-GRD-004 S2; người dùng chốt 2026-10-04 |
| BR-U15-51 | Người học xem điểm đã công bố và phản hồi của từng bài ngay trên Assignment List của lớp (không ở Class Detail, không có trang My Grades riêng). | US-GRD-004 S1; người dùng chốt 2026-10-04 |
| BR-U15-52 | Lịch sử điểm (ai, khi nào, trước/sau, lý do) xem được bởi giảng viên lớp và Chủ nhiệm môn của môn, mở từ Gradebook. | UC 36 |
| BR-U15-53 | Audit: lưu điểm, chốt, công bố, sửa điểm, truy cập trái phép. | FR-014 |
| BR-U15-54 | Xin gia hạn cá nhân, phúc khảo điểm và kiểm tra tương đồng bài nộp nằm ngoài phạm vi dự án, không thiết kế hoặc triển khai. | Quyết định phạm vi 2026-09-25 |
