# Sửa các điểm rà soát còn thiếu trên staging — 2026-11-16

Nguồn: báo cáo rà soát toàn dự án ngày 2026-11-15 trên `3b86e57d` và lựa chọn của người dùng ngày 2026-11-16 (phương án 1): chỉ mang lên staging những điểm staging còn thiếu.

Nhánh `chi-cuongg/audit-project-bugs-docs`, đưa lên ngang `origin/staging@f1c351dd` rồi áp thay đổi. Unit chạm tới: U14, U08 (chỉ migration), U12 (chỉ migration), frontend của U02 và U09, tài liệu. Không đổi URL, HTTP method, JSON hay hành vi chấm điểm.

## Vì sao phạm vi thu hẹp

Bản sửa đầu tiên (trên `3b86e57d`) còn gồm chặn nộp lại khi đã có điểm công bố, trạng thái `CANCELLED` của lượt làm và hộp xác nhận công bố. Trong lúc đó staging nhận `fe492df5`: chỉ chấm và công bố khi bài đã ngừng nhận bài (`CLOSED` hoặc `RETIRED`), bài `CLOSED` không mở lại, đóng bài tự nộp mọi lượt đang làm (BR-U09-42, BR-U08-23, BR-U12-43). Với quy tắc đó không còn bản nộp nào đến sau điểm công bố, nên ba phần trên không mang lên. Toàn bộ bản sửa đầu được giữ ở nhánh cục bộ `backup/review-fixes-full-2026-11-15` (commit `e8908f0e`).

## Các bước

- [x] 1. U14: `DeadlineReminderScanner` quét mọi bài còn dưới 24 giờ tới hạn chưa có bản ghi nhắc thay cho cửa sổ 5 phút; câu nhắc đổi thành "còn dưới 24 giờ"; viết lại hai test về cửa sổ, thêm một test.
- [x] 2. Migration `groups/V20261116_1000__drop_group_peer_reviews.sql`: bỏ bảng đánh giá chéo không còn mã nào dùng.
- [x] 3. Migration `assignmentwork/V20261116_1010__unlimited_attempt_numbers.sql`: bỏ trần 10 của `attempt_no`.
- [x] 4. Frontend: bỏ xử lý mã `GRADE_LOCKED` (`format.ts`, `BatchActionDialog.tsx`) và nhãn audit `GROUP_PEER_REVIEW_SAVED`; cập nhật ba test.
- [x] 5. Tài liệu: OpenAPI grading bỏ `GRADE_LOCKED` và ghi điều kiện ghi điểm hiện hành; `2026-11-02-locked-grades.md` gắn ghi chú đã thay; `requirements.md` (số unit cũ, số story, số mục trùng, tham chiếu file không còn, FR-019); `stories.md`; `docs/usecase_table.md`; `aidlc-state.md` (key reveal); BR-U08-70, BR-U12-73, BR-U14-30; `current-code-baseline.md`.
- [x] 6. Build và test trên nền staging; ghi [kết quả](../../build-and-test/2026-11-16-review-fixes-on-staging-results.md).

## Không làm

- Các điểm rà soát staging đã tự sửa trước đó: dòng "quyết định mới nhất" của `requirements.md` và `use-cases.md`, FR-027, US-ASM-009, dòng đầu `api-inventory.md`, liên kết `GroupPeerReviewService` trong `services.md`, ghi chú key reveal trong `requirements.md`.
- Ô chọn môn/lớp trên sidebar: chưa có quyết định của người dùng.
- BR-U09-10 và BR-U12-42 giữ nguyên: còn đúng với mã trên staging.
