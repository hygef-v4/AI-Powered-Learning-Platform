# UI và SMTP local — kết quả nghiệm thu B2/B8

Ngày 2026-10-25, múi giờ Asia/Saigon. Người dùng yêu cầu “kiểm giúp tôi” sau khi chọn phạm vi UI + SMTP qua Mailpit local. Nhánh `fix/notification-links-reminders-u04-tests`.

## Kết quả

**Đạt trong phạm vi UI trình duyệt thật, API/DB/worker thật và SMTP thật vào Mailpit local.** Không cần thêm bản sửa application code trong lượt nghiệm thu này.

| Ca | Kết quả | Bằng chứng |
|---|---|---|
| Runtime đúng bản sửa | PASS | Backend và worker QA dùng image mới từ JAR đã mvn verify; SHA256 runtime backend khớp `92eac13dae73e6e071e8f137c63b4a4ae5c1f856904a3ab4ef3a3f236828293b`. |
| SMTP activation | PASS | 5 tài khoản QA mới nhận OTP qua worker/SMTP Mailpit và kích hoạt/đăng nhập thành công; không reset tài khoản hiện hữu. |
| B2 bài CLASS | PASS | Sinh viên A bấm thông báo “Có bài tập mới mở” → `/my-assignments/7f28bfc9-3626-47c5-ba83-368a9e849cdb?classId=42ae5ebc-4bbc-4e5d-9887-6e753a5392ad`; UI hiện tên bài và B2QA-A. |
| B2 bài SUBJECT | PASS | Hai sinh viên nhận cùng assignment `a98ffea4-bd41-4ce4-b1c8-534c2624c685`, mỗi người có classId riêng; bấm chuông mở UI hiện lần lượt B2QA-A và B2QA-B. |
| SMTP mở bài | PASS | 3 thư nhận thật ở Mailpit, đúng người nhận, đúng route/classId; email delivery DB đều SENT. Bấm link trong thư của B mở tab bài chung lớp B. |
| Nhắc hạn trong chuông | PASS | Worker scheduler thật tạo DEADLINE_REMINDER; B bấm chuông “Bài tập sắp đến hạn” mở bài chung lớp B. |
| SMTP nhắc hạn | PASS | 3 thư nhận thật (A: bài riêng + bài chung; B: bài chung), đúng link/classId và email delivery SENT. Bấm link trong email nhắc hạn của B mở đúng UI bài/lớp. |
| Quyền chéo lớp | PASS | API của cả A/B khi đổi sang classId lớp kia trả 404. UI B khi sửa query thành lớp A hiện “Không tìm thấy hoặc bạn không có quyền.” |
| B8 cô lập lỗi | PASS | Trigger thử nghiệm chỉ chặn insert marker của bài chung/lớp A trong DB QA. Lượt scan 21:41:31 ghi REMINDER_CLASS_FAILED cho A, lớp B và bài riêng A vẫn gửi thành công. Notification lớp B được tạo lúc 21:41:31.518 sau log catch lỗi lúc 21:41:31.493. |
| Phục hồi và chống trùng | PASS | Gỡ trigger/function QA; bài chung/lớp A được gửi ở lượt 21:43:31 trong cửa sổ hiện hành. Kiểm lúc 21:44:58 có đúng 3 marker, 3 reminder notification và 3 thư; cùng 3 message ID giữa lần chụp 21:43:36 và 21:44:57. |
| Dọn fixture gây lỗi | PASS | `pg_trigger` không còn `qa_b8_fail_reminder`. |

## Môi trường và dữ liệu

- Stack tách biệt `b2-ui-smtp-qa`, UI QA cổng 8188 và Mailpit 8125, chỉ bind loopback. Dùng DB volume, Redis, RabbitMQ riêng; frontend image local có sẵn, backend/worker image `platform-b2-qa:local` chứa JAR hiện tại.
- Môn B2QA: `c7205b77-1936-4989-a404-20aae1012cda`; lớp A `42ae5ebc-4bbc-4e5d-9887-6e753a5392ad`, lớp B `de222c6e-3f79-4e19-8805-c5ba091a2e07`. Mỗi lớp một sinh viên QA, đều do API tạo/ghi danh.
- Chỉ chỉnh timestamp hai bài QA trong DB cô lập để vào cửa sổ nhắc: opensAt cách trước 48 giờ, closesAt khoảng sau 24 giờ; không đổi clock hệ thống hoặc logic/cửa sổ scanner. Trigger gây lỗi chỉ áp cho bài chung/lớp A.
- SMTP adapter, job handler, RabbitMQ và scanner scheduler đều là implementation thật. Mailpit giữ thư tại local, không relay tới email Internet.
- Stack `platform` cổng 8088 và dữ liệu/tài khoản của nó giữ nguyên. Không commit/push/deploy production.

## Bằng chứng lưu tại workspace

Các file dưới `.tmp/b2-ui-smtp/` được gitignore. Không đưa password/OTP/cookie vào báo cáo hoặc screenshot kết quả.

- `opened-evidence.json`, `reminder-evidence-initial.json`, `reminder-evidence.json`: metadata thư và link; các script `check-opened.mjs`/`check-reminders.mjs` gọi API và Mailpit thật.
- `b8-before-recovery.txt`, `b8-injected-failure-log.txt`, `final-db-evidence.txt`: marker/log lỗi/phục hồi/SENT và xác nhận trigger đã gỡ.
- UI: `ui-class-a.png`, `ui-subject-class-a.png`, `ui-subject-class-b.png`, `ui-reminder-class-b.png`, `ui-cross-class-denied.png`.
- SMTP và link đích: `smtp-opened-class-b.png`, `smtp-reminder-class-b.png`, `ui-from-opened-email-b.png`, `ui-from-reminder-email-b.png`.

## Giới hạn

- Kết quả này xác nhận SMTP qua Mailpit local; chưa kiểm SMTP provider Internet, TLS/auth, deliverability hoặc hộp thư người thật.
- Không chuyển đổi link lịch sử đã lưu/gửi, không kiểm phục hồi sau outage vượt cửa sổ 5 phút và không chứng nhận toàn bộ UI/mobile/trình duyệt khác.
- B8 được kiểm failure injection thật ở lớp/DB QA; không gây lỗi trên stack platform hoặc dữ liệu thật.
- Kết quả U04 31/31 và full backend verify ở báo cáo `2026-10-25-b2-b8-u04-results.md` vẫn giữ nguyên; lượt này không thay source nên không lặp lại toàn suite.

## Trạng thái sau nghiệm thu

Đã dừng cả 8 container QA bằng `docker compose stop` (exit 0), giữ container/volume để có thể khởi động lại. Không còn container QA chạy; 7 container stack platform vẫn healthy. Screenshot và bằng chứng JSON/SQL/log giữ trong `.tmp/b2-ui-smtp/`. Không có thay đổi application source, commit/push hoặc deploy trong lượt nghiệm thu.

## Extensions

SECURITY-03/05/08/15 và RESILIENCY-10 được đối chiếu bằng log có ngữ cảnh, link nội bộ, kiểm quyền chéo, rollback/cô lập lỗi và thử lại; mọi credential là QA mới và không đưa vào báo cáo. SECURITY-04/09/12 và RESILIENCY-04/06 N/A cho phạm vi không sửa headers/hardening/auth production/deployment/health. Các rule khác ngoài phạm vi đồ án và PBT disabled theo aidlc-state. Không coi đây là chứng nhận production.
