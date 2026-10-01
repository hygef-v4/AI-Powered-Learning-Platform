# U15 Grading - Business Rules

## 1. Quyền

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-01 | Chỉ giảng viên của lớp chấm, chốt, công bố, sửa điểm; ngoài quyền → từ chối và audit vi phạm. | US-GRD-003 S4 |
| BR-U15-02 | Người học chỉ xem điểm `PUBLISHED` của chính mình; Chủ nhiệm môn và ADMIN xem sổ điểm (chỉ đọc). | US-GRD-004 |

## 2. Tự chấm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-10 | Trắc nghiệm dùng đáp án phiên bản bài để chấm khi nộp: một đáp án đúng → đủ điểm câu; nhiều đáp án đúng hết mới có điểm (BR-U09-10). Với `GRADED`, job `GRADE_INIT` tạo grade `DETERMINISTIC`, `DRAFT`; với `PRACTICE`, cùng scorer trả kết quả riêng cho U11 và không tạo grade U15. | Câu 1, US-GRD-001, FR-030 |
| BR-U15-11 | Code Lab `GRADED` lấy điểm khi U13 báo qua `CodeGradedPort`; `SANDBOX_ERROR` → giữ `PENDING`, hiện "chưa chấm được". Code Lab `PRACTICE` dùng cùng kết quả test cho U11, không tạo grade U15. | Câu 1, BR-U13-35, FR-030 |
| BR-U15-12 | Bài bật "hiện điểm ngay sau nộp" → điểm tự chấm `PUBLISHED` luôn; không bật → chờ chốt và công bố. | BR-U09-13, US-GRD-001 S2 |
| BR-U15-13 | Giảng viên sửa điểm tự chấm được, bắt buộc lý do. | US-GRD-003 |

## 3. Chấm bài viết/tài liệu

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-20 | Chỉ bài `GRADED` cần Teacher chấm mới tạo grade `PENDING`; Teacher chọn "Chấm tay" hoặc "Nhờ AI đề xuất"; lựa chọn lưu actor/thời gian. Bài `PRACTICE` không vào hàng đợi này. | FR-008, FR-030, US-GRD-002 |
| BR-U15-21 | Chấm tay: câu có rubric → tích checklist (`RubricPort.score`); câu không rubric → nhập điểm 0…điểm câu; phản hồi tùy chọn. Không gọi AI. | US-GRD-002 S2 |
| BR-U15-22 | Nhờ AI: U13 tạo đề xuất (trừ credit giảng viên); đề xuất chỉ để tham khảo, điền sẵn checklist khi giảng viên bấm "Dùng đề xuất"; khác đề xuất khi chốt → ghi lý do. | US-GRD-002 S1, US-GRD-003 S1, S2 |
| BR-U15-23 | AI lỗi → bài nộp không mất, không có điểm giả; giảng viên chấm tay hoặc thử lại. | US-GRD-002 S3 |

## 4. Thang điểm, chốt và công bố

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-30 | Mọi loại bài hiển thị "x / tổng điểm của bài" (tổng điểm câu/rubric), 2 chữ số thập phân; không quy đổi thang 10. | Câu 2, 3 |
| BR-U15-31 | Chốt từng bài hoặc hàng loạt: chỉ điểm `DRAFT` có `finalScore` hợp lệ và `version` khớp; kết quả từng bài trả về; lỗi từng mục không ảnh hưởng mục khác. | US-GRD-005 |
| BR-U15-32 | "Công bố" theo lượt phát hành: mọi điểm `FINALIZED` → `PUBLISHED`; sau khi đã công bố, điểm chốt thêm được công bố ngay. Phát `grade.published`. | Câu 4 |
| BR-U15-33 | Sửa điểm `FINALIZED`/`PUBLISHED` bắt buộc lý do; lưu lịch sử; điểm đã công bố sửa thì người học thấy điểm mới và nhãn "đã cập nhật". | FR-008, US-GRD-003 S2 |
| BR-U15-34 | Bài nộp trễ hiển thị nhãn trễ; giảng viên tự trừ điểm (nếu muốn) qua sửa điểm có lý do; hệ thống không tự trừ. | U08 BR-U08-32 |
| BR-U15-35 | Với bài `GRADED`, lượt nộp cuối là lượt tính điểm chính thức (U11). Mọi kết quả `PRACTICE` nằm ngoài sổ điểm. | BR-U11-31, FR-030 |

## 5. Bài nhóm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-40 | Tài liệu nhóm (bản nộp cuối U14) chỉ chấm tay theo rubric của bài; không gửi AI. | US-GRP-006 S2 |
| BR-U15-41 | Phần đóng góp từng thành viên (các mục người đó là tác giả) chấm tay hoặc nhờ AI đề xuất. | US-GRP-004 S3 |
| BR-U15-42 | Điểm cuối từng thành viên nhập tay, hiển thị cạnh điểm tài liệu chung và điểm đóng góp; không áp công thức; bắt buộc lý do khi khác điểm tài liệu chung. | US-GRP-006 S5 |
| BR-U15-43 | Lỗi tích hợp trừ ở điểm tài liệu chung; trừ thêm cho một thành viên phải ghi lý do và mục liên quan. | US-GRP-006 S4 |

## 6. Sổ điểm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U15-50 | Sổ điểm lớp chỉ gồm publication `GRADED`: hàng = Student, cột = lượt phát hành; ô = điểm lượt nộp cuối "x / tổng" và trạng thái (chưa nộp, trễ, chờ chấm, đã chốt, đã công bố). Không đưa kết quả `PRACTICE` hoặc điểm AI luyện tập vào sổ điểm/xuất bảng điểm. **Không tính điểm tổng.** | FR-030, US-GRD-004 S2 |
| BR-U15-51 | Người học: danh sách bài của mình với điểm đã công bố và phản hồi. | US-GRD-004 S1 |
| BR-U15-52 | Lịch sử điểm (ai, khi nào, trước/sau, lý do) xem được bởi giảng viên lớp và ADMIN. | UC 36 |
| BR-U15-53 | Audit: chọn phương thức chấm, chấp nhận/ghi đè AI, chốt, công bố, sửa điểm, truy cập trái phép. | FR-014 |
| BR-U15-54 | Xin gia hạn cá nhân, phúc khảo điểm và kiểm tra tương đồng bài nộp nằm ngoài phạm vi dự án, không thiết kế hoặc triển khai. | Quyết định phạm vi 2026-09-25 |
