# Construction - Current SRS contract (2026-10-09)

Đồng bộ theo [73 UC](../../docs/use-cases-73.md), [requirements](../inception/requirements/requirements.md), [51 stories](../inception/user-stories/stories.md) và [ownership 15 unit](../inception/application-design/unit-of-work-story-map.md). Nhãn màn từ `docs/G21_Diagrams.drawio` (Page-2).

## Quyền và thuật ngữ dùng trong mọi unit

| Phạm vi | Kiểm tra tại backend |
|---|---|
| R1 - User | Hồ sơ và notification của chính User; ví và Payment Result chỉ Student/Teacher/Subject Manager của chính tài khoản; activation/recovery có quy tắc riêng |
| R2 - Tài nguyên môn | Subject Manager ACTIVE có subjects.manager_id tương ứng, không role-only; materials/bài của môn/Subject Question Bank |
| R3/R4 - Giảng dạy lớp | Teacher/Subject Manager ACTIVE có teacher_id tương ứng; materials lớp, bài, submissions/chấm/Gradebook/export |
| R5 - Student | Enrollment/membership và nội dung được phép của chính mình; chỉ leader nộp bài nhóm |
| Admin Full | Account, Subject (gồm gán Chủ nhiệm môn), Admin Dashboard, Credit Package, Settings, global Payment History và Audit Log. Không vào Manager Dashboard hay Class Dashboard, không nhận phân công môn/lớp, không dùng R2/R3/R4 (người dùng chốt 2026-10-09) |

Subject Manager → Teacher; Teacher, Student → User; Administrator chỉ dùng chức năng User và quản trị; không vai trò nào kế thừa Student. Mỗi tài khoản giữ một role cao nhất, cộng scope phân công hiện thời. Trong mô tả unit, “giảng viên lớp” nghĩa là actor đủ R3/R4 (Teacher hoặc Subject Manager được giao dạy lớp), “Chủ nhiệm môn hiện tại” nghĩa là Subject Manager đủ R2, không kiểm literal role để loại Subject Manager khỏi chức năng Teacher. Quyền đọc Class Detail cấu trúc không cấp quyền dạy. Teacher không sửa thông tin/vòng đời lớp, không thêm/gỡ sinh viên và không bật/tắt phân bố điểm (chỉ Chủ nhiệm môn, UC 51–52); mã mời đã bỏ (2026-10-09); nhóm là support action kiểm riêng.

Scope được đọc ở server trên hành động tài nguyên, không dựa vào UI hoặc cache role; ngoài scope trả lỗi an toàn/fail closed và audit theo unit. Thay phân công thu hồi quyền tài nguyên ngay ở request sau; cửa sổ JWT cũ không thay thế việc kiểm scope hiện thời.

## Phạm vi chức năng đã đổi

- Profile UC 07 chỉ displayName/phoneNumber, không avatar. Account Detail/patch Admin UC 61/62 riêng, email định danh bất biến.
- Chỉ có Subject Question Bank (UC 56–57), giữ câu của mọi dạng bài (quiz, Text Essay, Code Lab, khung tài liệu), có version; không có Class Question Bank. Teacher chọn câu ACTIVE của môn khi soạn bài/quiz, không quản trị ngân hàng. Draft chưa dùng có thể xóa; bản đã dùng/ACTIVE chỉ ngưng dùng và giữ lịch sử. Rubric (UC 46) bắt buộc, tự tạo trống cho mỗi câu Text Essay hoặc phần Diagram/Group; sửa khi bài còn nháp, phải điền đủ trước khi phát hành, khóa khi phát hành.
- Announcement UC 36 có create/update/soft-delete, version/audit; create mới gửi notification, update/delete không gửi lại. Không có bình luận dưới thông báo (bỏ 2026-10-09).
- Credit Package List/Detail UC 68–69: Admin xem/add/edit với version/audit, giữ snapshot giao dịch; không delete gói; mức tặng định kỳ sửa trên Settings (UC 70–71). Chỉ Student, Teacher, Subject Manager có ví (UC 08–11); Admin không có ví.
- Payment History UC 72: Admin query toàn nền tảng read-only; /me và kết quả giao dịch trên Credit Package Checkout vẫn owner-only; không refund/manual balance/reconciliation.
- Audit UC 73: Admin read/search, append-only records, không sửa/xóa.
- Student Practice AI UC 29 và Teacher AI Grading Proposals UC 38 riêng quyền/kết quả; không chấm Essay Practice chỉ vì submit. Chấm chính thức theo rubric/Teacher final và không đưa Practice vào Gradebook.
- Bài của môn (FR-027, người dùng chốt 2026-10-09): Chủ nhiệm môn soạn, duyệt, phát hành cho mọi lớp của môn với một lịch chung (không có bài nhóm); giảng viên từng lớp chỉ xem và chấm sinh viên lớp mình. Quiz là quiz luyện tập gắn học liệu, không lịch, không vào sổ điểm. Mọi bài qua bước duyệt trước khi phát hành.

## Primary ownership hiện hành

| Unit | Primary UC | Primary stories |
|---|---|---|
| U01 | 01, 02, 03, 04, 05, 06, 07, 59, 60, 61, 62, 63 | US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007 |
| U02 | 73 | US-AUD-001 |
| U03 | 70, 71 | US-SET-001 |
| U04 | 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67 | US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001 |
| U05 | 15, 30, 33, 34, 36, 54, 55 | US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005 |
| U06 | 46, 56, 57 | US-QBK-001, US-QBK-002 |
| U07 | 08, 09, 10, 11, 68, 69, 72 | US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005 |
| U08 | 41 | US-ASM-001, US-ASM-008, US-ASM-009, US-ASM-010 |
| U09 | 35, 42, 43, 44, 45 | US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007 |
| U11 | 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29 | US-ASM-003, US-ASM-012 |
| U12 | 16, 17 | US-GRP-001, US-GRP-002 |
| U13 | Không primary UC; hỗ trợ AI/Judge0 | US-AIG-001, US-AIG-002, US-AIG-003 |
| U14 | 27 | US-GRP-004, US-GRP-005 |
| U15 | 37, 38, 39, 40 | US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005 |
| U16 | 12, 58 | US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001 |

Tổng 73 UC/51 story/15 unit; mỗi mục đúng một primary unit. U10 đã bỏ: version/copy và bài của môn chuyển U08; U09 chủ trì cấu hình/authoring, U08 vòng đời. U03 chủ trì Settings, U13 khai báo cấu hình AI và hỗ trợ qua port. Không tạo lại US-CAT-005 đã rút; US-SET-001 thay phần story đó trong catalog 51 story.

## Màn hình và contracts

Class Dashboard cho Student/Teacher và Subject Manager được giao dạy; Manager Dashboard cho Subject Manager quản lý môn; Admin Dashboard cho Admin. Student Class Detail/Teacher Class Detail tách theo quyền. Material List/Material Detail, Learning Material, Question List/Question Detail (ngân hàng môn), Quiz List/Quiz Detail, Quiz Practice History/Detail/Taking/Result, Student Assignments, Assignment Detail/Submission History, Assignment List/Assignment Form, Student Submissions/Submission Detail/Grading Workspace, Class Announcements, My Credit Package/Public Credit Packages/Credit Package Checkout, Credit Package List/Detail, Setting List/Detail, Payment History và Audit Log theo Page-2. UC 12 dùng chuông/popup chung, không có trang Notifications riêng.

Component/route nội bộ có thể giữ tên kỹ thuật và reuse; tài liệu phải chỉ rõ nhãn/entry và quyền tương ứng. Existing contracts/port interfaces và mã U03 đã sinh chưa tự đổi vì tài liệu đổi. Khi triển khai revision phải rà DTO/method/status/schema theo thiết kế mới, dùng migration forward-only nếu baseline cũ đã áp dụng.

## Baseline và validation

Thiết kế và code plans hiện hành được sửa theo yêu cầu; các câu hỏi/câu trả lời và phê duyệt cũ giữ nguyên làm lịch sử. U03 plan bước [x] và generated summaries là code thực tế baseline trước; revision implementation tasks giữ [ ] đến khi thực sự làm. Không ghi nhận approval/code/runtime checks mới từ việc sửa tài liệu.

Enabled Security-03/04/05/08/09/12/15 và Resiliency-04/06/10 giữ hiệu lực theo phạm vi dự án; các sửa đổi có object authorization, validation, version/audit, bí mật an toàn, hữu hạn retries và deployment checks. Runtime verification N/A cho task documentation; Property-Based Testing disabled.

## Credit và checkpoint học liệu — sau recheck 2026-10-09

Học liệu chỉ reserve một HOLD khi chấp nhận yêu cầu tóm tắt; các child call tóm tắt/chunk/merge/embedding không reserve thêm. AiUsagePort.release(holdId) của U13 chốt tổng: đã dùng AI thì CreditPort.settle và trả dư, bằng 0 mới CreditPort.release hoàn toàn bộ. Scanner dùng cùng quy tắc; child creditStatus NONE không hoàn HOLD riêng. U05 phục hồi qua findHold/UsageStart REPLAY/CallSnapshot, không đọc repository U13; complete ghi checkpoint/usage cùng transaction với kiểm claim hợp lệ. Chi tiết ở [contract U05](u05-content-material-rag/functional-design/domain-entities.md).

## Revision View Material — 2026-10-09 (ưu tiên yêu cầu mới)

Upload qua U03/tạo lesson U05 chỉ lưu và trích chữ/phụ đề, không AI/credit. Student, Teacher, Subject Manager có quyền xem bấm Tóm tắt tài liệu trên Learning Material/Material Detail. HOLD chỉ tạo lúc nhận yêu cầu, payer là người bấm; một summary dùng chung, không giữ/trừ lại khi xem hoặc bấm trùng. Student được MATERIAL_SUMMARY và child EMBEDDING của HOLD này ngoài Practice grading, không được embedding/RAG độc lập. EXTRACTED chưa summary không vào RAG; AI deadline 24 giờ và HOLD fallback 25 giờ tính từ yêu cầu, không từ upload. Quyền sửa/upload không mở rộng cho Student; Admin denied.
