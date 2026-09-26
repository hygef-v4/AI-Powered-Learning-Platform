# U04 Subject, Class, Enrollment & Learning Access - Business Rules

## 1. Môn học

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U04-01 | Chỉ ADMIN tạo, sửa, lưu trữ môn và gán Chủ nhiệm môn. | UC-CAT-02..04 |
| BR-U04-02 | `code` môn duy nhất, chỉ chữ hoa/số/`-`, không đổi sau khi tạo. | UC-CAT-03 |
| BR-U04-03 | Mỗi môn tối đa 1 Chủ nhiệm môn; người được gán phải có role `SUBJECT_MANAGER` và không `DISABLED`. Gán người mới thay người cũ. | Câu 3 |
| BR-U04-04 | Lưu trữ môn chỉ khi mọi lớp của môn đã `ARCHIVED`; môn `ARCHIVED` không tạo lớp mới; có thể mở lại. Không xóa môn. | Câu 12 |

## 2. Lớp học

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U04-10 | Chỉ ADMIN tạo lớp; lớp thuộc đúng 1 môn `ACTIVE`, bắt đầu ở `DRAFT`. | Câu 11, UC-CAT-06 |
| BR-U04-11 | `code` lớp duy nhất trong môn, không đổi; `subjectId` không đổi. | Thiết kế |
| BR-U04-12 | Mỗi lớp đúng 1 giảng viên chính, role `INSTRUCTOR` hoặc `SUBJECT_MANAGER`, không `DISABLED`. Chỉ ADMIN gán/đổi. Bỏ trống giảng viên chỉ khi lớp không ở `OPEN`. | Câu 2, UC-CAT-08 |
| BR-U04-13 | Người quản lý lớp = ADMIN, giảng viên của lớp, Chủ nhiệm môn của môn. Người quản lý lớp được sửa `name`, `description`, `term`, đổi trạng thái, ghi danh, quản lý mã mời. | Câu 11, FR-002 |
| BR-U04-14 | Chuyển trạng thái hợp lệ: `DRAFT→OPEN`, `DRAFT→ARCHIVED`, `OPEN→ARCHIVED`, `ARCHIVED→OPEN`. `OPEN` cần có giảng viên và môn `ACTIVE`. | Câu 1 |
| BR-U04-15 | Mở lại lớp `ARCHIVED` bị từ chối nếu có người học `ACTIVE` của lớp đang ở lớp chưa lưu trữ khác cùng môn; trả danh sách người vướng. | BR-U04-22 |
| BR-U04-16 | Lưu trữ lớp giữ nguyên ghi danh và dữ liệu học tập; lớp `ARCHIVED` chỉ đọc với người quản lý. | UC-CAT-09 |
| BR-U04-17 | Người quản lý lớp bật/tắt `showGradeDistribution`; mặc định tắt. U16 chỉ hiện phân bố điểm ẩn danh khi cờ bật và đủ mẫu theo BR-U16-42. | US-RPT-002 |

## 3. Ghi danh

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U04-20 | Ghi danh chỉ vào lớp `DRAFT` hoặc `OPEN`. Người được ghi danh phải có role `LEARNER`, trạng thái `ACTIVE` hoặc `PENDING`. | UC-CAT-11 |
| BR-U04-21 | Mỗi `(lớp, người học)` một bản ghi. Đã `ACTIVE` → không làm gì, báo "đã ghi danh". Đang `REMOVED` → chuyển lại `ACTIVE`. | Câu 8, US-CAT-003 S2 |
| BR-U04-22 | Một người học chỉ `ACTIVE` ở tối đa 1 lớp chưa lưu trữ của mỗi môn. | Câu 10 |
| BR-U04-23 | Thêm từng người: tìm theo email/tên (≤ 20 kết quả, chỉ `LEARNER`). Thêm theo danh sách: dán email hoặc CSV 1 cột, ≤ 200 dòng; xử lý từng dòng, dòng hợp lệ vẫn được ghi danh. | Câu 4 |
| BR-U04-24 | Kết quả từng dòng: `ENROLLED`, `RESTORED`, `ALREADY_ENROLLED`, `NOT_FOUND`, `NOT_LEARNER`, `DISABLED`, `IN_OTHER_CLASS`, `INVALID_EMAIL`, `DUPLICATE_IN_LIST`. | Câu 4 |
| BR-U04-25 | Gỡ ghi danh: chuyển `REMOVED`, giữ lịch sử; quyền mới bị thu hồi ngay. Cần xác nhận trên UI. | US-CAT-003 S3 |
| BR-U04-26 | Khi ghi danh có hiệu lực với người học (ghi danh vào lớp `OPEN`, hoặc lớp `DRAFT` chuyển `OPEN` thì với mọi ghi danh `ACTIVE`), phát event `ENROLLMENT_ACTIVATED`; U16 gửi thông báo trong app **và** email. | Câu 6 |

## 4. Mã mời

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U04-30 | Mỗi lớp 1 mã 8 ký tự, bảng chữ `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (bỏ ký tự dễ nhầm), sinh ngẫu nhiên an toàn, duy nhất. | Câu 5 |
| BR-U04-31 | Người quản lý lớp bật mã (bắt buộc hạn, mặc định 7 ngày, tối đa 30 ngày), tắt mã, hoặc đổi mã (mã cũ hết hiệu lực ngay). | Câu 5 |
| BR-U04-32 | Tự ghi danh cần: mã đúng, đang bật, chưa hết hạn, lớp `OPEN`, người gọi role `LEARNER`, thỏa BR-U04-21, 22. | US-CAT-005 S1 |
| BR-U04-33 | Mã sai/hết hạn/tắt/lớp không mở → cùng một thông báo "mã không hợp lệ hoặc đã hết hạn", không lộ thông tin lớp. | US-CAT-005 S2 |
| BR-U04-34 | Rate limit 10 lần nhập sai mỗi giờ mỗi tài khoản; vượt → từ chối tới hết cửa sổ và audit. | US-CAT-005 S2 |

## 5. Truy cập của người học

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U04-40 | Người học thấy danh sách lớp mình `ACTIVE`: lớp `OPEN` ở mục "Đang học", lớp `ARCHIVED` ở mục "Đã kết thúc" (chỉ thông tin lớp). Lớp `DRAFT` không hiện. | US-LRN-001, US-CAT-002 S2 |
| BR-U04-41 | Nội dung lớp chỉ trả khi ghi danh `ACTIVE` và lớp `OPEN`; lấy nội dung đã phát hành của lớp và của môn qua `PublishedContentPort`. | US-LRN-001 S1, FR-003 |
| BR-U04-42 | Không ghi danh, bị gỡ, hoặc lớp không mở → trả "không tìm thấy", không lộ tên hay metadata lớp. | US-LRN-001 S2 |
| BR-U04-43 | Thanh toán không ảnh hưởng quyền vào lớp; U04 không gọi U07. | Câu 7 |
| BR-U04-44 | Không lưu tiến độ, lộ trình hay trạng thái hoàn thành bài học. | unit-of-work.md |

## 6. Phân quyền, audit và lỗi

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U04-50 | Danh sách: ADMIN thấy mọi môn/lớp; Chủ nhiệm môn thấy môn mình và các lớp của môn; giảng viên thấy lớp mình dạy. Trang ≤ 100, tìm theo mã/tên. | UC-CAT-01, 05 |
| BR-U04-51 | Truy cập ngoài phạm vi → "không tìm thấy" và audit `ACCESS_DENIED`. | US-CAT-002 S3 |
| BR-U04-52 | Audit: tạo/sửa/lưu trữ môn, gán Chủ nhiệm môn, tạo/sửa/đổi trạng thái lớp, gán giảng viên, ghi danh/gỡ (mỗi người một sự kiện), đổi mã mời, vượt rate limit mã mời. | SEC-005, FR-003 |
| BR-U04-53 | Lỗi cấu hình (môn/lớp/người không tồn tại, sai role) → từ chối toàn bộ thao tác đó, thông báo cách sửa, không lộ dữ liệu người khác. | US-CAT-001 S2 |
| BR-U04-54 | U01 không gọi được → từ chối (fail closed). | BR-U01-93 |
