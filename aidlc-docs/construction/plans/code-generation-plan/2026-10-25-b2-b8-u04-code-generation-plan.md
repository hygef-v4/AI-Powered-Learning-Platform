# Sửa B2, B8 và B7/U04

Yêu cầu triển khai được người dùng duyệt ngày 2026-10-25: tạo nhánh fix/... từ staging và sửa theo đề xuất `inception/requirements/2026-10-25-b2-b8-b7-u04-review.md`.

Nhánh: `fix/notification-links-reminders-u04-tests`, base staging `ea0d2c8`. Giữ nguyên tài liệu chưa commit từ lượt review. Không đổi U06/U12, không triển khai chuyển hướng lịch sử (phần bổ sung tùy chọn), không mở rộng cửa sổ nhắc hạn.

## Ngữ cảnh và ranh giới

U14 sở hữu notification/reminder/email và dùng IClassAccess, IAccountLookup, IAssignmentQuery, ISubmissionQuery. B2 giữ route/authorization U08 và source event id/unique key hiện có; B8 giữ marker/notification/email trong cùng transaction có timeout. U04 thay test persistence/transaction theo nghiệp vụ lưu trữ môn hiện hành, giữ accounts/U05 fixtures hiện có. Không migration hay API contract mới.

## Các bước triển khai

- [x] 1. Kiểm tra trạng thái, tạo nhánh từ staging và đọc lại đề xuất, code/test, quy tắc construction liên quan.
- [x] 2. B2: thêm regression cho link theo từng lớp, dedup, lọc recipient và email link cũ/mới; sửa resolver/service, helper link và scanner/email tương ứng.
- [x] 3. B8: thêm regression lỗi từng bài/lớp; catch ngoài transaction và log ngữ cảnh; kiểm rollback marker/notification/email trên PostgreSQL cô lập.
- [x] 4. U04: thay gate env bằng PostgreSQL Testcontainers, cập nhật assertion/version/manager/concurrency và tên test; chạy suite thật, xử lý các lỗi còn phát hiện.
- [x] 5. CI: kiểm report bắt buộc U04 và test transaction U14 thực sự chạy, không skip; giữ mvn verify pipeline hiện hữu.
- [x] 6. Chạy scoped rồi toàn backend verify, rà diff và lưu kết quả/giới hạn/extension compliance. Cập nhật state/audit cùng checklist; không tự commit/push/deploy.

## Tiêu chí

- Bài CLASS/SUBJECT gửi link my-assignments đúng lớp, dedup ổn định; email hỗ trợ link mới/cũ, link hỏng không retry vô hạn, kiểm trạng thái/hạn vẫn hoạt động.
- Lỗi lớp/bài không chặn mục tiếp theo; lỗi transaction rollback marker/notification/email, lượt sau tạo đúng một bộ dữ liệu.
- U04 chạy PostgreSQL thật, không gate U04_TEST_*, giữ kiểm quyền DELETE, CAS, timeout, rollback và khóa concurrency.
- CI không xanh giả khi suite bắt buộc skip. Test cục bộ không có Docker phải được báo trung thực, không tính skipped thành pass.

Extensions kế thừa aidlc-state: SECURITY-03/05/08/15 và RESILIENCY-10 áp dụng vào log/path/recipient/transaction; SECURITY-04/09/12 và RESILIENCY-04/06 N/A cho phần thay đổi này; các rule ngoài phạm vi đồ án và PBT disabled giữ cấu hình đã chốt.

## Bằng chứng scoped

78 test đạt, 0 failures/errors/skipped, gồm U04 31 và PostgreSQL U14 2. Lỗi U04 thứ năm quan sát được ở lượt chạy đầu là `archiveAlsoArchivesOpenAndDraftFixtures` gọi helper yêu cầu transaction bên ngoài transaction; đã sửa fixture assertion và xác minh cả hai lớp ARCHIVED. CI guard kiểm fixture pass và từ chối report missing/skip/failure/zero.

Toàn backend `mvn -B verify` BUILD SUCCESS (exit 0): 1.375 ca, 1.371 đạt, 4 skipped U06/U12 ngoài phạm vi, 0 failure/error; ArchUnit và JAR đạt. CI guard trên report thật và git diff --check đạt. Kết quả/giới hạn: `construction/build-and-test/2026-10-25-b2-b8-u04-results.md`. Không authenticated UI/SMTP thật, commit/push/deploy; tất cả bước triển khai trong phạm vi được duyệt hoàn thành.
