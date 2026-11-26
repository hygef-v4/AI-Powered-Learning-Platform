# Thiết kế backup bằng base backup và WAL (U03)

**Ngày**: 2026-11-26
**Trạng thái**: Đã duyệt 2026-11-26 (bỏ cảnh báo, tự động hóa khôi phục)
**Thay thế**: backup `pg_dump` hằng ngày (BR-U03-90 đến BR-U03-96)

## 1. Quyết định của người dùng

| Mục | Quyết định |
|---|---|
| `pg_dump` | Bỏ hẳn |
| Bản chụp đầy đủ | Base backup vật lý (`pg_basebackup`) mỗi 24 giờ, chạy theo giờ `backup.dailyAt` trong Cài đặt |
| WAL | Đẩy lên Google Drive mỗi 5 phút |
| Nơi lưu | Chỉ Google Drive. Không cấu hình Drive thì không lưu backup và không lưu WAL |
| Mã hóa | RSA không bắt buộc: có `U03_BACKUP_PUBLIC_KEY` thì mã hóa cả base backup và WAL |
| Cảnh báo | Không có cảnh báo riêng cho WAL |
| Khôi phục | Tự động hóa tối đa bằng công cụ và script |

## 2. Mục tiêu

| Chỉ số | Hiện tại (`pg_dump`) | Sau thay đổi |
|---|---|---|
| Mất dữ liệu tối đa khi mất VPS (RPO) | 24 giờ | Khoảng 6 phút (WAL chốt mỗi 5 phút, worker đẩy trong vòng 1 phút) |
| Khôi phục về thời điểm bất kỳ | Không | Có, trong 7 ngày gần nhất |
| Ảnh hưởng tới người dùng | `pg_dump` giữ transaction dài | Base backup đọc file dữ liệu với checkpoint giãn và giới hạn tốc độ; WAL chỉ chép file đã ghi xong |

## 3. Thành phần

### 3.1 PostgreSQL (docker-compose)

- Thêm tham số: `archive_mode=on`, `archive_timeout=5min`, `wal_level=replica` (mặc định của Postgres 16, ghi rõ để không bị đổi).
- `archive_command` chỉ chép trên máy, không gọi mạng, để Postgres không bao giờ phải chờ Drive:
  `test ! -f /wal-archive/%f && cp %p /wal-archive/%f.tmp && mv /wal-archive/%f.tmp /wal-archive/%f`
- Volume mới `wal-archive`, gắn vào `postgres` và `worker`. Hai container phải cùng quyền ghi trên volume (xử lý bằng group chung hoặc quyền thư mục khi khởi tạo).
- Tệp `pg_hba.conf` riêng (gắn qua `hba_file`) có thêm dòng `host replication backup_replicator <mạng internal> scram-sha-256`, áp dụng được cho cả volume dữ liệu đã có.

### 3.2 Role cơ sở dữ liệu

- Role mới `backup_replicator` có quyền `REPLICATION LOGIN`, dùng mật khẩu `BACKUP_DB_PASSWORD`. Role `backup_reader` (chỉ dùng cho `pg_dump`) bị bỏ.
- Volume mới: tạo trong `infra/postgres/init/01-roles.sh`. Volume đã có: người vận hành chạy một câu SQL một lần, ghi trong runbook.

### 3.3 Base backup hằng ngày (thay `PgDumpRunner`)

- `BackupScheduleScanner` giữ nguyên lịch: đến giờ `backup.dailyAt`, mỗi ngày một lần, chỉ khi có Drive.
- `PgBaseBackupRunner` chạy `pg_basebackup -D - -Ft -X fetch -z --checkpoint=spread --max-rate=<giới hạn>`, ghi ra một tệp tar nén duy nhất có kèm WAL cần để bản chụp tự nhất quán.
- `BackupJobHandler` giữ luồng hiện có: mã hóa nếu có khóa, tải lên `backups/<runId>/`, đối chiếu kích thước và MD5, ghi `SUCCESS`, ghi audit `BACKUP_COMPLETED` hoặc `BACKUP_FAILED`.
- Giữ 7 bản thành công gần nhất (7 ngày); không bao giờ xóa bản thành công mới nhất.

### 3.4 Đẩy WAL (mới)

- `WalShipScanner` chạy trong worker mỗi phút, chỉ một worker chạy tại một thời điểm (advisory lock).
- Với mỗi tệp WAL đã ghi xong trong `/wal-archive` (bỏ qua `.tmp`), theo thứ tự tên: nén gzip, mã hóa nếu có khóa, tải lên `backups/wal/<tên tệp>`, đối chiếu kích thước và MD5, ghi một dòng vào bảng `wal_segments`, rồi xóa tệp trên máy.
- Lỗi Drive hoặc mạng: giữ tệp trên máy, thử lại ở phút sau. Không ghi audit cho từng tệp WAL để tránh nhiễu nhật ký.
- Không cấu hình Drive hoặc đang tắt `backup.enabled`: worker xóa tệp WAL trên máy, không lưu ở đâu cả (WAL không dùng được nếu không có base backup).
- Bảng mới `wal_segments (file_name primary key, archive_ref, byte_size, uploaded_at, purged_at)`.

### 3.5 Dọn bản cũ

- Sau mỗi base backup thành công: xóa base backup quá 7 ngày, rồi xóa trên Drive các tệp WAL tải lên trước thời điểm chụp của base backup cũ nhất còn giữ (trừ lùi 1 giờ để an toàn).

### 3.6 Theo dõi

- Không có cảnh báo riêng cho WAL (quyết định của người dùng). Giữ sự kiện `backup.failed` khi base backup thất bại.
- Thời điểm đẩy WAL gần nhất nằm trong bảng `wal_segments` (`max(uploaded_at)`); chưa đưa lên trang Tổng quan để giữ phạm vi thay đổi.

### 3.7 Khôi phục tự động

- `BackupRestoreTool` (chạy từ `app.jar`, không cần cơ sở dữ liệu cũ): đọc cấu hình Drive từ biến môi trường, chọn base backup mới nhất trước thời điểm cần khôi phục, tải base backup và toàn bộ WAL từ thời điểm đó, giải mã nếu có khóa riêng, giải nén thành thư mục dữ liệu Postgres sẵn sàng phát lại (`recovery.signal`, `restore_command` đọc WAL từ thư mục cục bộ, `recovery_target_time` nếu có).
- Script `scripts/backup/restore-pitr.sh`: chạy công cụ trên trong một container tạm dùng image backend, khởi động một container Postgres 16 cô lập trên thư mục vừa dựng, chờ phát lại WAL xong và chạy truy vấn kiểm tra số dòng các bảng chính.
- Chuyển ứng dụng sang cơ sở dữ liệu đã khôi phục vẫn do người vận hành làm sau khi kiểm tra (giữ BR-U03-98: khôi phục vào môi trường cô lập trước). Các bước ghi trong `scripts/backup/README.md`.
- Trước khi nâng phiên bản lớn của Postgres, người vận hành chạy `pg_dump` thủ công một lần (base backup chỉ khôi phục được đúng Postgres 16).

## 4. Phần bị bỏ

- `PgDumpRunner`, role `backup_reader`, các nhắc đến `pg_dump` hằng ngày trong tài liệu U03 và README.

## 5. Kiểm thử

- Unit test: lệnh `pg_basebackup`, chọn base backup và WAL trong `BackupRestoreTool`, `WalShipScanner` (tải lên, lỗi giữ tệp, không có Drive thì xóa, bỏ qua `.tmp`), dọn bản cũ theo base backup cũ nhất, scanner không chạy khi không có Drive.
- Kiểm thử trên stack Docker local: chạy base backup, ghi dữ liệu mẫu, chờ WAL được đẩy, khôi phục vào container cô lập về một thời điểm chọn trước, đối chiếu dữ liệu.

## 6. Rủi ro

| Rủi ro | Biện pháp |
|---|---|
| Worker dừng lâu khiến WAL dồn và đầy ổ | Không có cảnh báo (quyết định của người dùng); volume WAL cần dư dung lượng; kiểm tra thời điểm đẩy WAL gần nhất trong bảng `wal_segments` |
| Quyền ghi giữa container Postgres và worker trên volume chung | Kiểm thử trên stack Docker trước khi gộp |
| Volume dữ liệu đã có chưa có role `backup_replicator` | Câu SQL một lần trong runbook; base backup báo lỗi rõ ràng nếu thiếu role |
| Dung lượng Google Drive (15 GB với Drive cá nhân miễn phí) | 7 base backup cộng WAL 7 ngày; theo dõi dung lượng, chuyển sang object storage khi cần |
