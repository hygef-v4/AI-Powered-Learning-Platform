# Kết quả sửa các điểm rà soát trên nền staging — 2026-11-16

[Kế hoạch và phạm vi](../plans/code-generation-plan/2026-11-16-review-fixes-on-staging.md). Nhánh `chi-cuongg/audit-project-bugs-docs`, làm trên `origin/staging@f1c351dd` rồi gộp `origin/staging@5f9c0173` (giao diện đa ngôn ngữ, Google Drive). Số liệu dưới đây là của bản đã gộp.

## Đã sửa

| Điểm rà soát | Thay đổi |
|---|---|
| Nhắc hạn 24 giờ không quét bù | `DeadlineReminderScanner` xét mọi bài còn dưới 24 giờ tới hạn chưa có bản ghi nhắc; câu nhắc đổi thành "còn dưới 24 giờ" |
| Lượt nộp thứ 11 của bài có chấm bị database từ chối | Migration `V20261116_1010__unlimited_attempt_numbers.sql` bỏ trần 10 của `attempt_no` |
| Sót bảng và nhãn của đánh giá chéo | Migration `V20261116_1000__drop_group_peer_reviews.sql`; bỏ nhãn audit `GROUP_PEER_REVIEW_SAVED` |
| Frontend còn mã `GRADE_LOCKED` | Bỏ ở `format.ts` và `BatchActionDialog.tsx` |
| OpenAPI grading còn mô tả khóa điểm | Ghi điều kiện ghi điểm hiện hành: lớp OPEN và bài đã ngừng nhận bài |
| Tài liệu lệch | `requirements.md` (số unit cũ ở UC 14/15, 42, 66; số story; hai cặp số mục trùng; tham chiếu `use-cases-73.md`; FR-019), `stories.md`, `docs/usecase_table.md`, `aidlc-state.md`, `2026-11-02-locked-grades.md`, BR-U08-70, BR-U12-73, BR-U14-30, `current-code-baseline.md` |

## Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| Backend `mvn -B verify`, JDK 21 (CI dùng JDK 17), có Docker | 2.546 test, 0 thất bại, 0 lỗi, 1 bỏ qua (`GradingAiEvaluationTest`, cần khóa AI thật); JAR đóng gói được |
| `scripts/assert-required-backend-tests.ps1` | Qua: 31, 7, 11, 1, 27 test của năm bộ bắt buộc, không bỏ qua; 58 ca tích hợp có truy vết |
| Migration trên PostgreSQL thật (Testcontainers) | 48 migration áp xong, tới `v20261116.1010` |
| Frontend ESLint, TypeScript | Qua |
| Frontend Vitest | 1.159/1.159 qua, 157 file |
| Frontend production build | Qua |

Trước khi gộp, trên `f1c351dd`: backend 2.492 test không thất bại, frontend 1.134/1.134. Khi gộp có 5 file xung đột: `aidlc-state.md`, `audit.md` và ba file frontend mà staging vừa chuyển sang khóa thông báo đa ngôn ngữ; ba file đó lấy bản staging rồi bỏ lại các dòng `GRADE_LOCKED` và nhãn đánh giá chéo, kèm ba khóa thông báo không còn dùng trong `en.json`, `ja.json`, `vi.json`.

## Chưa kiểm

- Trình duyệt và E2E.
- Migration bỏ trần `attempt_no` chỉ được kiểm là áp được; chưa có test tạo lượt thứ 11.
- JDK 17 như CI: máy này chỉ có JDK 21, biên dịch với `release 17`.

## Giới hạn

- Migration bỏ bảng `group_peer_reviews` xóa dữ liệu đánh giá chéo cũ nếu còn trên staging hoặc production; không khôi phục được ngoài bản backup.
- Lần quét đầu sau khi triển khai sẽ nhắc mọi bài còn dưới 24 giờ tới hạn mà trước đó bị lỡ nhắc.
- Phần chặn nộp lại, trạng thái `CANCELLED` và hộp xác nhận công bố không nằm trong nhánh này; lý do ở kế hoạch. Bản đầy đủ ở nhánh cục bộ `backup/review-fixes-full-2026-11-15`.

Security Baseline: SECURITY-05, 08, 15 không bị ảnh hưởng (không đổi đầu vào, phân quyền hay xử lý lỗi); các quy tắc khác N/A. Resiliency: RESILIENCY-04/06/10 N/A. PBT tắt.
