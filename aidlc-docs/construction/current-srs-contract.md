# Construction - Current SRS contract (2026-10-08)

Đồng bộ theo [70 UC và màn hình](../../docs/use-cases-and-screens.md), [requirements](../inception/requirements/requirements.md), [51 stories](../inception/user-stories/stories.md) và [ownership 16 unit](../inception/application-design/unit-of-work-story-map.md). Nhãn màn từ `screen-flow (1).drawio`.

## Quyền và thuật ngữ dùng trong mọi unit

| Phạm vi | Kiểm tra tại backend |
|---|---|
| R1 - User | Hồ sơ, ví, Payment Result, notification của chính tài khoản; activation/recovery có quy tắc riêng |
| R2 - Tài nguyên môn | Subject Manager ACTIVE có subjects.manager_id tương ứng, không role-only; materials/template/Subject Question Bank |
| R3/R4 - Giảng dạy lớp | Teacher/Subject Manager ACTIVE có teacher_id tương ứng; materials lớp, bài, submissions/chấm/Gradebook/export |
| R5 - Student | Enrollment/membership và nội dung được phép của chính mình; chỉ leader nộp bài nhóm |
| Admin Full | Account, Subject (gồm gán Chủ nhiệm môn), Admin Dashboard, Credit Package, Settings, global Payment History và Audit Log. Không vào Manager Dashboard hay Class Dashboard, không nhận phân công môn/lớp, không dùng R2/R3/R4 (người dùng chốt 2026-10-09) |

Subject Manager → Teacher; Teacher, Student → User; Administrator chỉ dùng chức năng User và quản trị; không vai trò nào kế thừa Student. Mỗi tài khoản giữ một role cao nhất, cộng scope phân công hiện thời. Trong mô tả unit, “giảng viên lớp” nghĩa là actor đủ R3/R4 (Teacher hoặc Subject Manager được giao dạy lớp), “Chủ nhiệm môn hiện tại” nghĩa là Subject Manager đủ R2, không kiểm literal role để loại Subject Manager khỏi chức năng Teacher. Quyền đọc Class Detail cấu trúc không cấp quyền dạy. Teacher không sửa thông tin/vòng đời lớp và không thêm/gỡ sinh viên (chỉ Chủ nhiệm môn, UC 51–52); mã mời đã bỏ (2026-10-09); nhóm là support action kiểm riêng.

Scope được đọc ở server trên hành động tài nguyên, không dựa vào UI hoặc cache role; ngoài scope trả lỗi an toàn/fail closed và audit theo unit. Thay phân công thu hồi quyền tài nguyên ngay ở request sau; cửa sổ JWT cũ không thay thế việc kiểm scope hiện thời.

## Phạm vi chức năng đã đổi

- Profile UC 07 chỉ displayName/phoneNumber, không avatar. Account Detail/patch Admin UC 60/61 riêng, email định danh bất biến.
- Chỉ có Subject Question Bank (UC 56–57), giữ câu của mọi dạng bài (quiz, Text Essay, Code Lab, khung tài liệu), có version; không có Class Question Bank. Teacher chọn câu ACTIVE của môn khi soạn bài/quiz, không quản trị ngân hàng. Draft chưa dùng có thể xóa; bản đã dùng/ACTIVE chỉ ngưng dùng và giữ lịch sử. Rubric (UC 46) bắt buộc, tự tạo trống cho mỗi câu Text Essay hoặc phần Diagram/Group; sửa khi bài còn nháp, phải điền đủ trước khi phát hành, khóa khi phát hành.
- Announcement UC 36 có create/update/soft-delete, version/audit; create mới gửi notification, update/delete không gửi lại. Không có bình luận dưới thông báo (bỏ 2026-10-09).
- Credit Package List/Detail UC 68–69: Admin xem/add/edit với version/audit, giữ snapshot giao dịch; không delete gói; mức tặng định kỳ sửa trên Settings (UC 70–71). Chỉ Student, Teacher, Subject Manager có ví (UC 08–11); Admin không có ví.
- Payment History UC 72: Admin query toàn nền tảng read-only; /me và kết quả giao dịch trên Credit Package Checkout vẫn owner-only; không refund/manual balance/reconciliation.
- Audit UC 70: Admin read/search, append-only records, không sửa/xóa.
- Student Practice AI UC 25 và Teacher AI Grading Proposals UC 35 riêng quyền/kết quả; không chấm Essay Practice chỉ vì submit. Chấm chính thức theo rubric/Teacher final và không đưa Practice vào Gradebook.
- Bài của môn (FR-027, người dùng chốt 2026-10-09): Chủ nhiệm môn soạn, duyệt, phát hành cho mọi lớp của môn với một lịch chung (không có bài nhóm); giảng viên từng lớp chỉ xem và chấm sinh viên lớp mình. Quiz là quiz luyện tập gắn học liệu, không lịch, không vào sổ điểm. Mọi bài qua bước duyệt trước khi phát hành.

## Primary ownership hiện hành

| Unit | Primary UC | Primary stories |
|---|---|---|
| U01 | 01, 02, 03, 04, 05, 06, 07, 58, 59, 60, 61, 62 | US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007 |
| U02 | 70 | US-AUD-001 |
| U03 | Không primary UC | Không primary story |
| U04 | 12, 13, 27, 28, 45, 46, 47, 48, 49, 50, 63, 64, 65, 66 | US-CAT-001, US-CAT-002, US-CAT-003, US-CAT-005, US-LRN-001 |
| U05 | 14, 26, 29, 30, 31, 51, 52 | US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005 |
| U06 | 32, 33, 44, 55, 56 | US-QBK-001, US-QBK-002 |
| U07 | 08, 09, 10, 67, 68, 69 | US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005 |
| U08 | 38 | US-ASM-001 |
| U09 | 39, 40, 41, 42, 43 | US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007 |
| U10 | 53, 54 | US-ASM-008, US-ASM-009, US-ASM-010 |
| U11 | 17, 18, 19, 20, 21, 22, 24, 25 | US-ASM-003, US-ASM-012 |
| U12 | 15, 16 | US-GRP-001, US-GRP-002 |
| U13 | Không primary UC | US-AIG-001, US-AIG-002, US-AIG-003 |
| U14 | 23 | US-GRP-004, US-GRP-005 |
| U15 | 34, 35, 36, 37 | US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005 |
| U16 | 11, 57 | US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001 |

Tổng 70 UC/51 story, mỗi mục đúng một primary unit. U03/U13 hỗ trợ qua port; U02 chủ trì Audit. Các bảng truy vết có đóng góp nhiều unit không cộng lặp ownership.

## Màn hình và contracts

My Classes cho Student; Assigned Classes cho Teacher; Subject Classes cho quản lý cấu trúc. Các nhãn khác theo bảng UC/màn: Learning Material, Uploaded Learning Materials, Class Announcements, Class/Subject Question Bank, Subject Template/Template Editor, Credit Packages/Credit Package Setting/Payment History, Student Submissions, Assignment Detail/Submission History, các editor/workspace và năm danh sách bài.

Component/route nội bộ có thể giữ tên kỹ thuật và reuse; tài liệu phải chỉ rõ nhãn/entry và quyền tương ứng. Existing contracts/port interfaces và mã U03 đã sinh chưa tự đổi vì tài liệu đổi. Khi triển khai revision phải rà DTO/method/status/schema theo thiết kế mới, dùng migration forward-only nếu baseline cũ đã áp dụng.

## Baseline và validation

Thiết kế và code plans hiện hành được sửa theo yêu cầu; các câu hỏi/câu trả lời và phê duyệt cũ giữ nguyên làm lịch sử. U03 plan bước [x] và generated summaries là code thực tế baseline trước; revision implementation tasks giữ [ ] đến khi thực sự làm. Không ghi nhận approval/code/runtime checks mới từ việc sửa tài liệu.

Enabled Security-03/04/05/08/09/12/15 và Resiliency-04/06/10 giữ hiệu lực theo phạm vi dự án; các sửa đổi có object authorization, validation, version/audit, bí mật an toàn, hữu hạn retries và deployment checks. Runtime verification N/A cho task documentation; Property-Based Testing disabled.
