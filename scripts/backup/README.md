# Khôi phục cơ sở dữ liệu từ base backup và WAL

Hệ thống chụp base backup (`pg_basebackup`) lên Google Drive mỗi ngày theo giờ trong Cài đặt và đẩy WAL lên Drive
mỗi 5 phút. Khôi phục dựng lại cơ sở dữ liệu tại một thời điểm bất kỳ trong 7 ngày gần nhất.

## 1. Khôi phục vào Postgres cô lập (tự động)

Chạy ở thư mục gốc repo, trên máy có Docker và image backend (`platform-backend:local` hoặc image đang chạy):

```bash
# Về thời điểm mới nhất có trên Drive
scripts/backup/restore-pitr.sh --env-file infra/.env

# Về một thời điểm cụ thể (ví dụ ngay trước khi xóa nhầm), bản backup có mã hóa
scripts/backup/restore-pitr.sh --env-file infra/.env \
  --target-time 2026-11-26T16:55:00+07:00 --private-key ~/keys/backup-private.pem
```

Script tự làm:

1. Tải base backup mới nhất trước thời điểm chọn và các tệp WAL từ Google Drive (`BackupRestoreTool`, đọc cấu hình
   Drive từ tệp `--env-file`), giải mã nếu có khóa riêng.
2. Giải nén base backup, đặt `restore_command` và `recovery_target_time`.
3. Khởi động container Postgres 16 cô lập ở `127.0.0.1:55432` (đổi bằng `--port`), chờ phát lại WAL xong.
4. In số dòng các bảng chính và thời điểm của bản ghi audit mới nhất để đối chiếu.

Script không đụng tới cơ sở dữ liệu đang chạy.

## 2. Chuyển ứng dụng sang bản đã khôi phục (làm tay sau khi kiểm tra)

1. Kiểm tra dữ liệu trên container cô lập: `docker exec -it <container> psql -U postgres -d platform`.
2. Dừng ứng dụng: `docker compose ... stop backend worker`.
3. Sao lưu thư mục dữ liệu hiện tại (volume `pgdata`) trước khi thay.
4. Dừng Postgres, chép thư mục `pgdata` đã khôi phục vào volume `pgdata` (giữ quyền sở hữu `postgres`), xóa
   `recovery.signal` nếu còn và bỏ các dòng `restore_command`, `recovery_target_*` mà script thêm vào cuối
   `postgresql.auto.conf`.
5. Khởi động lại Postgres, backend và worker; kiểm tra đăng nhập, quyền và dữ liệu (BR-U03-98).
6. Chạy một base backup mới sớm nhất có thể (đặt giờ chạy gần trong Cài đặt) để có điểm xuất phát cho chuỗi WAL mới.

## 3. Việc làm một lần

- Volume dữ liệu tạo trước 2026-11-26 chưa có role `backup_replicator`. Chạy một lần (thay mật khẩu bằng
  `BACKUP_DB_PASSWORD`):

  ```sql
  CREATE ROLE backup_replicator LOGIN REPLICATION PASSWORD '<BACKUP_DB_PASSWORD>';
  ```

- Base backup chỉ khôi phục được đúng Postgres 16. Trước khi nâng phiên bản lớn của Postgres, chạy `pg_dump` thủ công
  một lần và lưu riêng.

## 4. Khóa mã hóa (tùy chọn)

- Có `U03_BACKUP_PUBLIC_KEY` thì base backup và WAL được mã hóa. Khóa riêng không để trên VPS; cất ở ít nhất hai nơi.
- Tạo cặp khóa:

  ```bash
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out backup-private.pem
  openssl pkey -in backup-private.pem -pubout -out backup-public.pem
  ```

  Đặt nội dung `backup-public.pem` vào `U03_BACKUP_PUBLIC_KEY`.
