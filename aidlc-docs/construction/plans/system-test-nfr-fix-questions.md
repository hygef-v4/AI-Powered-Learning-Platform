# Câu hỏi sửa case Fail sau system test NFR (Report-5.4, 2026-11-26)

Người dùng yêu cầu hỏi lần lượt từng câu. Mỗi câu ghi hiện trạng đã kiểm trên systest (commit be90c802) và trong mã. Câu trả lời được ghi ngay sau khi người dùng chọn.

## Câu 1: TC-SEC-048, mã trả về khi vượt giới hạn đăng nhập

Hiện trạng: giới hạn theo IP hoạt động (sau 30 lần sai, lần 31 bị chặn kể cả khi đúng mật khẩu), nhưng trả `401 AUTH_FAILED` trung tính. NFR-U01-22 yêu cầu đăng nhập trả cùng lỗi trung tính. Test case kỳ vọng `429` kèm thông báo thử lại.

A) Giữ 401 trung tính, sửa kết quả mong đợi của test case, đánh dấu Pass, không tạo Jira

B) Đổi sang 429 kèm Retry-After, sửa NFR-U01-22, tạo Bug Jira

[Answer]: A

## Câu 2: TC-SEC-050, audit khi lưu bản nháp bài tập

Hiện trạng: 409 khi lưu bản cũ, giữ nội dung tab 1, bài đã phát hành không sửa được rubric. Không có audit khi lưu bản nháp, đúng BR-U02-10 (không ghi audit cho U07). BR-U07-70 vẫn ghi "ghi audit `ACCESS_DENIED`", mâu thuẫn BR-U02-10.

A) Giữ BR-U02-10: bỏ phần audit trong kết quả mong đợi, đánh dấu Pass, không tạo Jira; sửa BR-U07-70 cho khớp BR-U02-10

B) Ghi audit khi lưu bài tập: sửa BR-U02-10 và code U07, tạo Bug Jira

[Answer]: A

## Câu 3: TC-PERF-002, đăng nhập dưới tải

Hiện trạng: mỗi lần đăng nhập tính bcrypt cost 12 một lần. Backend 2 CPU đo được khoảng 1,3 lần đăng nhập/giây; 100 người cùng lúc phải chờ 45–60 s, 5,2% lỗi. Ước tính 2 CPU có thể đạt khoảng 5–8 lần/giây, nên có thể còn chỗ nghẽn khác. Mục tiêu 500 ms cho 100 lần đăng nhập đồng thời không khả thi với bcrypt 12.

A) Giữ bcrypt 12; đo và sửa chỗ nghẽn (tạo Jira nếu là lỗi), giới hạn số bcrypt chạy cùng lúc để không còn timeout 60 s, đổi mục tiêu sang dạng tốc độ (con số chốt sau khi đo)

B) Hạ bcrypt cost xuống 10, sửa NFR

C) Giữ mục tiêu, ghi là giới hạn phần cứng

[Answer]: A

## Câu 4: TC-PERF-020, nhịp quét lịch

Hiện trạng: `ScheduledScanRunner` chạy `fixedDelay = 60_000`; đóng bài trễ 73 s (mục tiêu 1 phút), mở bài trễ 11 s. Bộ quét dùng chung cho mở/đóng bài, tự nộp bài và tự nộp quiz.

A) Đổi nhịp quét thành biến môi trường, mặc định 15 s; tạo Bug Jira; sửa BR/NFR U03; chạy lại case

B) Giữ 60 s, sửa mục tiêu thành ≤ 90 s

[Answer]: A

## Câu 5: Pool kết nối và giới hạn tài nguyên (PERF-001, 004, 005, liên quan 009)

Hiện trạng: Hikari `maximum-pool-size: 10` cố định; log PERF-004/005 có `Connection is not available`. Compose giới hạn backend 2 CPU / 1 GiB, Postgres 1 CPU / 1 GiB; chưa biết cấu hình VPS (staging dùng chung).

A) Pool đọc từ biến môi trường (backend mặc định 20, worker 10, tổng dưới `max_connections` 100); sửa các đoạn code chậm; đo lại với giới hạn CPU hiện tại; nâng CPU để sau khi có `nproc`, `free -h` của VPS

B) Như A và nâng giới hạn CPU/RAM ngay

C) Chỉ đo thử, chưa sửa

[Answer]: A

## Câu 6: TC-SEC-051, khoá mã hoá backup

Hiện trạng: code backup mã hoá khi có `U03_BACKUP_PUBLIC_KEY`; biến này trống trên VPS production và staging. Việc sửa nằm trên VPS, không nằm trong repo.

A) Tạo Bug Jira và hướng dẫn, chờ nhóm chọn người giữ khoá

B) Người dùng tự giữ khoá riêng và tự đặt khoá công khai lên VPS ngay theo hướng dẫn

[Answer]: B. Hướng dẫn: `.tmp/systemtest/sec051-backup-key-guide.md` (ngoài repo).

## Không cần hỏi

- TC-SEC-036: SEC-005 ghi rõ log không chứa token, nên ẩn token tải tệp trong access log của nginx (Bug Jira).
- TC-SEC-052, 053: tắt `server_tokens`, nâng image nginx lên bản vá mới nhất, thêm HSTS cho các location tĩnh (Bug Jira).
