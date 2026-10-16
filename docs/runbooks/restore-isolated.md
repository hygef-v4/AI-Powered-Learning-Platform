# Khôi phục backup vào môi trường cô lập

Runbook của U03 (BR-U03-98, NFR-U03-75). Chỉ khôi phục **database**; tệp người dùng trên Google Drive không có trong backup (BR-U03-91). Không khôi phục thẳng vào production: luôn chạy ở môi trường cô lập trước, kiểm xong mới quyết định bước tiếp.

## Chuẩn bị

- Máy của người vận hành có Docker, **không phải VPS production**.
- Private key RSA của người vận hành (`private-key.pem`, PKCS#8), giữ ngoài VPS. Public key tương ứng nằm trong biến `U03_BACKUP_PUBLIC_KEY` của production.
- Image backend đúng phiên bản ghi trong lần backup (`app_version` trên Admin Dashboard) hoặc mới hơn, để có công cụ giải mã.

Tạo cặp khóa lần đầu (chạy trên máy người vận hành, rồi đưa public key lên `.env` của VPS):

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out private-key.pem
openssl pkey -in private-key.pem -pubout -out public-key.pem
```

## Các bước

1. Trên Admin Dashboard, chọn lần backup `SUCCESS` cần khôi phục, ghi lại ngày chạy.
2. Trong thư mục backup trên Google Drive (`GOOGLE_BACKUP_FOLDER_ID`), mở thư mục con trùng mã lần chạy, tải `archive.alapbk` về máy.
3. Chạy script (tạo PostgreSQL 16 tạm, giải mã, `pg_restore`, đếm số dòng, ghi thời gian):

   ```bash
   infra/scripts/restore-isolated.sh archive.alapbk private-key.pem ghcr.io/max2min/platform-backend:<tag>
   ```

4. Kiểm theo danh sách dưới đây trên database tạm (script in lệnh `psql` để kết nối).
5. Lưu tệp bằng chứng `restore-evidence-<thời điểm>.txt` do script tạo, kèm kết quả kiểm tay.
6. Xong thì xóa container tạm (`docker rm -f <tên>` theo lệnh script in ra) và xóa tệp dump đã giải mã.

## Danh sách kiểm sau khôi phục

- Số dòng các bảng chính khớp ước lượng lúc backup (tài khoản, môn, lớp, ghi danh, bài, bài nộp, điểm, audit).
- Quyền: role và phân công môn, lớp của vài tài khoản mẫu đúng.
- Tài liệu và XML sơ đồ Draw.io của vài bài làm mở được (JSON hợp lệ).
- Bài nộp và điểm đã công bố của vài lớp mẫu.
- Quota và usage AI của vài giảng viên.
- Số dòng `audit_logs` không giảm so với lần kiểm trước.
- Tham chiếu tệp (`file_id`) trỏ tới tệp đang có trên Drive; tệp bị xóa sau thời điểm backup sẽ thiếu.

## RPO, RTO

- RPO: khoảng cách giữa hai lần backup (tối đa 24 giờ với lịch hằng ngày) cộng thời gian từ lần backup cuối tới sự cố.
- RTO: script ghi thời điểm bắt đầu và kết thúc; cộng thêm thời gian tải archive và kiểm tay. Ghi số đo thật vào bằng chứng, không cam kết trước.

## Khi cần khôi phục production

Ngoài phạm vi runbook này. Làm theo quy trình riêng có chấp thuận của người phụ trách: bật bảo trì, dump database hiện tại, khôi phục vào database mới, xóa Redis và hàng đợi RabbitMQ, chép lại `audit_logs` mới hơn thời điểm backup, rồi mới trỏ ứng dụng sang database mới.
