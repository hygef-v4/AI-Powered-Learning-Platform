# Deploy lên VPS

Runbook triển khai bản production/demo bằng `infra/docker-compose.yml` trên một VPS Ubuntu 24.04 x86_64 (tối thiểu 6 vCPU, 6 GB RAM, 40 GB đĩa). VPS **không build mã nguồn**: CI build image khi có commit vào `main` và đẩy lên `ghcr.io/hygef-v4/platform-backend` và `ghcr.io/hygef-v4/platform-frontend`, tag theo SHA đầy đủ của commit. Hai image này công khai, không cần `docker login`.

Trong runbook: `<VPS_IP>` là IP của VPS, `<DOMAIN>` là tên miền trỏ về IP đó. Không ghi IP, mật khẩu hay khóa thật vào repo.

## Chuẩn bị

| Cần có | Dùng cho | Bắt buộc |
|---|---|---|
| Tên miền trỏ về `<VPS_IP>` (ví dụ subdomain DuckDNS miễn phí) | Chứng chỉ HTTPS của Let's Encrypt; cookie đăng nhập đặt cờ `Secure` nên phải chạy HTTPS | Có |
| Tài khoản Brevo: SMTP login và SMTP key (hoặc API key nếu VPS chặn SMTP), email người gửi thuộc tên miền đã xác thực | Gửi OTP kích hoạt tài khoản (cả Admin đầu tiên) và email thông báo | Có |
| Service account Google Drive, Shared Drive, thư mục backup | Lưu tệp người dùng và backup database | Không, xem mục 6 |
| Cặp khóa RSA của người vận hành | Mã hóa backup (xem `restore-isolated.md`) | Khi bật backup |

## 1. Đăng nhập và cập nhật máy

```bash
ssh root@<VPS_IP>
passwd
apt update && apt -y upgrade
timedatectl set-timezone Asia/Ho_Chi_Minh
```

Đổi mật khẩu root ngay lần đầu vì mật khẩu do nhà cung cấp cấp đã hiện trên trang quản lý. Nên thêm SSH key của bạn vào `~/.ssh/authorized_keys` rồi tắt đăng nhập bằng mật khẩu (`PasswordAuthentication no` trong `/etc/ssh/sshd_config`, sau đó `systemctl restart ssh`). Chỉ tắt sau khi đã đăng nhập thử bằng key thành công ở một cửa sổ khác.

## 2. Tường lửa và swap

```bash
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

Compose chỉ mở cổng 80 và 443 (Nginx). Postgres, Redis và RabbitMQ nằm trong mạng `internal`, không ra ngoài.

## 3. Cài Docker

```bash
curl -fsSL https://get.docker.com | sh
docker --version
docker compose version
```

## 4. Lấy cấu hình triển khai

VPS chỉ cần thư mục `infra/` (compose, cấu hình Nginx, script khởi tạo Postgres).

```bash
git clone https://github.com/hygef-v4/AI-Powered-Learning-Platform.git /opt/platform
cd /opt/platform
git checkout <SHA>
cd infra
```

`<SHA>` là commit trên `main` có job `images` của CI đã chạy thành công. Lấy bằng `gh run list --branch main` hoặc trên tab Actions. Image chỉ tồn tại cho commit trên `main`, không có cho `staging`.

## 5. Tạo `infra/.env`

```bash
cp .env.example .env
chmod 600 .env
```

Sinh giá trị bí mật. Mật khẩu dùng dạng hex để không có ký tự đặc biệt (script khởi tạo Postgres nhúng mật khẩu vào câu SQL):

```bash
for v in POSTGRES_PASSWORD POSTGRES_MIGRATOR_PASSWORD POSTGRES_APP_PASSWORD REDIS_PASSWORD RABBITMQ_PASSWORD BACKUP_DB_PASSWORD; do
  echo "$v=$(openssl rand -hex 24)"
done
echo "U01_JWT_SECRET=$(openssl rand -base64 48)"
echo "U03_FILEREF_SECRET=$(openssl rand -base64 48)"
echo "U06_AI_KEY_ENCRYPTION_KEY=$(openssl rand -base64 32)"
```

Mở `nano .env`, dán các giá trị trên và điền:

| Biến | Giá trị |
|---|---|
| `DOMAIN` | `<DOMAIN>` |
| `APP_PUBLIC_URL` | `https://<DOMAIN>` |
| `GHCR_OWNER` | `hygef-v4` (mặc định trong file mẫu là `max2min`, phải sửa) |
| `IMAGE_TAG` | `<SHA>` đầy đủ 40 ký tự |
| `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM` | SMTP login, SMTP key và email người gửi đã xác minh trên Brevo |
| `MAIL_TRANSPORT`, `BREVO_API_KEY` | Giữ `smtp` nếu VPS gửi được ra cổng 587. Nếu nhà cung cấp chặn SMTP: `brevo-api` và API key của Brevo (SMTP & API → API Keys), khi đó `SMTP_USER`, `SMTP_PASSWORD` để trống |
| `U01_BOOTSTRAP_ADMIN_EMAIL` | Email Admin đầu tiên, tên miền nào cũng được nhưng phải nhận được thư |

Lưu bản sao `.env` ở nơi an toàn ngoài VPS (trình quản lý mật khẩu). Mất `U06_AI_KEY_ENCRYPTION_KEY` thì mọi API key AI đã lưu không giải mã được. Không đổi các mật khẩu Postgres sau lần chạy đầu: script khởi tạo chỉ chạy một lần khi volume `pgdata` còn trống.

## 6. Lưu tệp: Google Drive hoặc thư mục trên VPS

Mặc định `U03_REQUIRE_DRIVE=true`. Thiếu cấu hình Drive thì ứng dụng vẫn chạy nhưng upload, tải tệp và backup báo tạm thời không khả dụng.

**Cách A, Google Drive (khuyến nghị cho production).** Điền vào `.env`:

- `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY`: tệp JSON key của service account, mã hóa base64 trên một dòng (`base64 -w0 key.json`).
- `GOOGLE_SHARED_DRIVE_ID`: ID Shared Drive đã thêm service account làm thành viên.
- `GOOGLE_BACKUP_FOLDER_ID`: ID thư mục backup trong Shared Drive.
- `U03_BACKUP_PUBLIC_KEY`: phần base64 của `public-key.pem` trên một dòng (bỏ hai dòng `-----BEGIN/END-----`).

**Cách B, lưu trên đĩa VPS (demo).** Tạo `infra/docker-compose.override.yml`. Compose tự ghép tệp này khi chạy không có `-f`:

```yaml
services:
  backend:
    environment:
      U03_REQUIRE_DRIVE: "false"
      U03_LOCAL_STORAGE_DIR: /app/data/files
      U03_LOCAL_BACKUP_DIR: /app/data/backups
    volumes:
      - files-data:/app/data/files
  worker:
    environment:
      U03_REQUIRE_DRIVE: "false"
      U03_LOCAL_STORAGE_DIR: /app/data/files
      U03_LOCAL_BACKUP_DIR: /app/data/backups
    volumes:
      - files-data:/app/data/files
      - backups-data:/app/data/backups

volumes:
  files-data: {}
  backups-data: {}
```

Với cách B, tệp và backup nằm cùng máy với database: hỏng VPS là mất cả hai. Định kỳ chép volume `platform_backups-data` ra ngoài.

## 7. Cấp chứng chỉ HTTPS lần đầu

Nginx cần chứng chỉ có sẵn mới khởi động được khối `listen 443`, còn service `certbot` chỉ gia hạn. Lần đầu cấp bằng chế độ standalone trước khi bật Nginx. Kiểm tên miền đã trỏ đúng IP (`ping <DOMAIN>`) rồi chạy:

```bash
docker compose --env-file .env run --rm -p 80:80 --entrypoint certbot certbot \
  certonly --standalone -d <DOMAIN> --email <EMAIL_CUA_BAN> --agree-tos --no-eff-email
```

Chứng chỉ lưu trong volume `platform_letsencrypt`, Nginx dùng chung volume đó. Service `certbot` gia hạn mỗi 12 giờ qua webroot; tham số `--webroot` của lệnh gia hạn thay cho chế độ standalone đã lưu.

Nginx chỉ đọc chứng chỉ lúc khởi động. Sau mỗi lần gia hạn (khoảng 60 ngày một lần) phải nạp lại: `docker compose --env-file .env exec nginx nginx -s reload`. Có thể giao việc này cho cron của máy, ví dụ tệp `/etc/cron.d/platform-nginx-reload`:

```
30 4 * * * root cd /opt/platform/infra && docker compose --env-file .env exec -T nginx nginx -s reload >/dev/null 2>&1
```

### 7b. Khi VPS không nhận kết nối từ nước ngoài: DNS-01 qua Cloudflare

Một số nhà cung cấp VPS trong nước chặn kết nối quốc tế đi vào. Máy kiểm tra của Let's Encrypt ở nước ngoài nên cách ở trên báo `Timeout during connect`. DNS-01 chứng minh quyền sở hữu bằng bản ghi TXT, không cần kết nối vào VPS.

1. Thêm tên miền vào Cloudflare (gói Free), đổi máy chủ tên miền ở nơi đăng ký sang hai máy chủ Cloudflare cấp. Bản ghi A của tên miền để **DNS only** (mây xám): proxy của Cloudflare kết nối tới VPS từ nước ngoài nên cũng bị chặn.
2. Tạo API token: My Profile → API Tokens → Create Token → mẫu **Edit zone DNS**, Zone Resources chọn đúng tên miền.
3. Trên VPS, lưu token vào `infra/cloudflare.ini` (quyền 600, không commit):

   ```
   dns_cloudflare_api_token = <TOKEN>
   ```

4. Trong `infra/docker-compose.override.yml`, đổi service `certbot` sang image có plugin Cloudflare và gắn tệp token:

   ```yaml
   services:
     certbot:
       image: certbot/dns-cloudflare:v4.1.1
       volumes:
         - letsencrypt:/etc/letsencrypt
         - ./cloudflare.ini:/run/secrets/cloudflare.ini:ro
       entrypoint: ["/bin/sh", "-c", "trap exit TERM; while :; do certbot renew --quiet; sleep 12h & wait $${!}; done"]
   ```

5. Cấp chứng chỉ (lệnh gia hạn dùng lại cách cấp đã lưu):

   ```bash
   docker compose --env-file .env run --rm --entrypoint certbot certbot \
     certonly --dns-cloudflare --dns-cloudflare-credentials /run/secrets/cloudflare.ini \
     --dns-cloudflare-propagation-seconds 30 -d <DOMAIN> \
     --email <EMAIL_CUA_BAN> --agree-tos --no-eff-email --non-interactive
   ```

## 8. Khởi động

```bash
docker compose --env-file .env pull
docker compose --env-file .env up -d
docker compose --env-file .env ps
```

Lần đầu backend chạy Flyway migration, mất khoảng 1–2 phút trước khi `healthy`. Nginx chỉ khởi động sau khi backend và frontend đã `healthy`. Theo dõi:

```bash
docker compose --env-file .env logs -f backend worker
```

## 9. Kiểm tra

```bash
curl -I https://<DOMAIN>
docker compose --env-file .env ps
free -h
docker stats --no-stream
```

Mở `https://<DOMAIN>`, kích hoạt Admin đầu tiên bằng OTP gửi tới `U01_BOOTSTRAP_ADMIN_EMAIL`. Không nhận được OTP thì xem `docker compose --env-file .env logs backend worker | grep -iE 'mail|otp'` (OTP do worker gửi) và kiểm thông tin Brevo.

## 10. Tự động deploy và rollback

Khi có commit mới trên `main` và CI chạy thành công (đã đẩy image lên GHCR), VPS tự deploy bản đó trong khoảng 2 phút. Nếu bản mới không khỏe, VPS tự quay về bản trước.

GitHub Actions không SSH vào VPS. Lý do: nhà cung cấp chặn kết nối từ nước ngoài, và VPS cũng không tới được máy chủ Actions để chạy self-hosted runner. Vì vậy VPS tự kéo bản mới về:
- Timer `platform-deploy.timer` gọi `platform-deploy check` mỗi 2 phút.
- Lệnh này hỏi GitHub API lần chạy thành công mới nhất của `ci.yml` trên `main`. Repo và image đều public nên không cần token.

Mỗi lần deploy làm theo thứ tự:
1. Kéo image backend và frontend của SHA mới, `git fetch`. Lỗi ở bước này thì dừng, chưa đổi gì.
2. Sao lưu database vào `/var/backups/platform/predeploy-<thời điểm>-<SHA cũ>.dump`, giữ 5 bản gần nhất.
3. `git checkout` đúng SHA, đặt `IMAGE_TAG`, chạy `docker compose up -d`.
4. Trong 5 phút, mọi container phải `running` và có health `healthy` (hoặc không có healthcheck), và `https://<DOMAIN>/` phải trả 2xx.
5. Không đạt thì tự quay về bản trước. SHA lỗi vào danh sách bỏ qua để timer không deploy lặp lại. Service kết thúc `failed`.

### Cài lần đầu

Chạy sau khi đã deploy tay xong mục 1–9:

```bash
cd /opt/platform
install -m 755 infra/deploy/platform-deploy.sh /usr/local/bin/platform-deploy
install -m 644 infra/deploy/platform-deploy.service infra/deploy/platform-deploy.timer /etc/systemd/system/
systemctl daemon-reload
platform-deploy deploy "$(sed -n 's/^IMAGE_TAG=//p' infra/.env)"   # thử toàn bộ quy trình với bản đang chạy
systemctl enable --now platform-deploy.timer
```

Khi script trong repo thay đổi, chạy lại lệnh `install` đầu tiên (deploy không tự thay script đang chạy).

### Theo dõi và rollback

```bash
platform-deploy status                         # bản đang chạy, bản quay về được, danh sách bỏ qua, lịch sử
journalctl -u platform-deploy.service -n 50    # log các lần deploy
platform-deploy rollback                       # quay về bản gần nhất trước đó
platform-deploy rollback <SHA>                 # quay về một bản cụ thể đã có image trên GHCR
platform-deploy unskip <SHA>                   # cho timer deploy lại bản đã bỏ qua (sau khi đã sửa nguyên nhân)
systemctl stop platform-deploy.timer           # tạm dừng tự deploy; bật lại: systemctl start platform-deploy.timer
```

- Rollback đưa bản đang chạy vào danh sách bỏ qua, nên timer không đẩy nó lên lại. Commit mới hơn trên `main` vẫn được deploy bình thường.
- Gọi `rollback` nhiều lần thì lùi tiếp về các bản cũ hơn.
- Timer luôn deploy bản thành công mới nhất trên `main`. Nếu deploy tay một bản không phải mới nhất, timer sẽ đổi lại về bản mới nhất; muốn giữ bản tay thì dừng timer.

### Giới hạn: migration database

Rollback chỉ quay lại image và cấu hình. Migration Flyway mà bản mới đã chạy thì **không** đảo ngược. Nếu bản cũ không chạy được trên schema mới, rollback tự động cũng sẽ lỗi; log khi đó ghi rõ đường dẫn bản dump trước deploy. Khôi phục:

```bash
cd /opt/platform/infra
systemctl stop platform-deploy.timer
docker compose --env-file .env stop backend worker
docker compose --env-file .env exec -T postgres pg_restore -U postgres -d platform --clean --if-exists < /var/backups/platform/predeploy-<...>.dump
platform-deploy rollback <SHA cũ>    # SHA nằm ở cuối tên tệp dump (12 ký tự đầu; lấy đủ 40 ký tự trong `platform-deploy status`)
systemctl start platform-deploy.timer
```

Dữ liệu ghi vào sau thời điểm dump sẽ mất. Để tránh tình huống này, migration nên tương thích ngược: thêm cột hoặc bảng ở một bản, chỉ xóa ở bản sau.

### Deploy tay (khi timer tắt)

```bash
platform-deploy deploy <SHA>   # vẫn có sao lưu, kiểm tra sức khỏe và tự quay về
docker image prune -f          # thỉnh thoảng dọn image cũ
```

## Sự cố thường gặp

| Hiện tượng | Nguyên nhân thường gặp |
|---|---|
| `get.docker.com` treo, hoặc `pull` báo `registry-1.docker.io ... i/o timeout` | Mạng của nhà cung cấp VPS không tới được máy chủ Docker trên AWS. Cài `docker.io docker-compose-v2` từ kho Ubuntu và đặt `{"registry-mirrors": ["https://mirror.gcr.io"]}` vào `/etc/docker/daemon.json`, rồi `systemctl restart docker` |
| Certbot báo `Timeout during connect (likely firewall problem)` dù `ufw` đã mở cổng 80 | Nhà cung cấp chặn kết nối từ nước ngoài vào VPS (máy kiểm tra của Let's Encrypt ở nước ngoài). Kiểm bằng công cụ kiểm tra cổng nhiều quốc gia; nhờ nhà cung cấp mở, hoặc cấp chứng chỉ qua DNS-01 |
| `manifest unknown` khi `pull` | `IMAGE_TAG` không phải commit trên `main` đã qua job `images`, hoặc `GHCR_OWNER` còn là `max2min` |
| `required variable ... is missing` | Thiếu biến bắt buộc trong `.env` hoặc quên `--env-file .env` |
| Nginx khởi động lại liên tục | Chưa có chứng chỉ cho `<DOMAIN>` (làm lại mục 7) hoặc `DOMAIN` trong `.env` khác tên đã cấp |
| Backend `unhealthy`, log báo lỗi xác thực Postgres | Đã đổi mật khẩu Postgres sau lần chạy đầu; volume `pgdata` vẫn giữ mật khẩu cũ |
| Log báo `API Brevo trả HTTP 401 (unauthorized)` | `BREVO_API_KEY` sai, hoặc Brevo chặn IP lạ: tắt hoặc thêm IP VPS ở Security → Authorised IPs |
| Log báo `API Brevo trả HTTP 400` | `SMTP_FROM` chưa phải người gửi hay tên miền đã xác thực trên Brevo |
| RabbitMQ `unhealthy` dù vẫn chạy; backend và worker không khởi động lại (`dependency failed to start`) | `rabbitmq-diagnostics ping` chậm khi giới hạn 0.5 CPU (khoảng 7 giây). `infra/docker-compose.yml` đã tăng timeout lên 20 giây; bản cũ hơn thì đặt healthcheck tương tự trong `docker-compose.override.yml` |
| `platform-deploy` báo `không kéo được image` | CI của SHA đó chưa xong job `images`, hoặc mạng tới GHCR lỗi tạm; timer thử lại ở lần sau |
| `platform-deploy status` có SHA trong Bỏ qua | Bản đó đã lỗi khi deploy hoặc đã bị rollback. Xem `journalctl -u platform-deploy.service`, sửa trên `main` (commit mới sẽ tự deploy) hoặc `platform-deploy unskip <SHA>` |
| Đăng nhập xong bị đẩy ra | Truy cập bằng HTTP hoặc bằng IP thay vì `https://<DOMAIN>` |
| Không nhận được OTP, log backend báo `MailConnectException` hoặc `Connection timed out` tới máy chủ SMTP | Nhà cung cấp chặn SMTP đi ra ngoài. Kiểm bằng `nc -zvw5 smtp-relay.brevo.com 587` (`succeeded` là mở, `timed out` là bị chặn); nhờ nhà cung cấp mở cổng 587, hoặc chuyển sang `MAIL_TRANSPORT=brevo-api` (mục 5) rồi `up -d backend worker` |
