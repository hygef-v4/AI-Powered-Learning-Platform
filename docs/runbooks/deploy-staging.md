# Staging trên VPS cho nhóm kiểm thử

Staging chạy trên cùng VPS với production, để cả nhóm kiểm thử chung một địa chỉ và một bộ dữ liệu.

- **Địa chỉ:** `https://<STAGING_DOMAIN>:8443`.
- **Code:** bản mới nhất của nhánh `staging` đã qua CI. VPS tự cập nhật khoảng 5 phút sau mỗi lần merge vào `staging`.
- **Email:** không gửi ra ngoài. OTP và thông báo nằm trong hộp thư Mailpit `https://<STAGING_DOMAIN>:8443/mailpit/`, có mật khẩu chung của nhóm.
- **Tách khỏi production:** checkout riêng `/opt/platform-staging`, `.env` riêng, project compose `platform-staging` (database, Redis, RabbitMQ, volume riêng), Nginx riêng ở cổng 8443. Không đổi cổng 80/443 hay container nào của production.

Tệp liên quan:

| Tệp | Vai trò |
|---|---|
| `infra/docker-compose.staging.yml` | Ghép với `docker-compose.yml`: cổng 8443, Mailpit, lưu tệp trên volume, giới hạn tài nguyên nhỏ hơn |
| `infra/nginx/staging/default.conf.template` | Nginx HTTPS của staging, `/mailpit/` có mật khẩu |
| `infra/.env.staging.example` | Mẫu `.env` của staging |
| `infra/deploy/platform-staging-deploy.service`, `.timer` | Tự deploy theo nhánh `staging` bằng cùng `platform-deploy.sh` |

`<STAGING_DOMAIN>` là tên miền của staging, ví dụ `staging.<tên miền production>`. Không ghi IP, mật khẩu hay khóa thật vào repo.

## 0. Kiểm tra trước

- **RAM:** VPS 6 GB. Giới hạn của staging tổng khoảng 3,1 GB; đo trên máy thử, khi không tải cả stack dùng khoảng 1 GB. Chạy `free -h` và `docker stats --no-stream`. Còn dưới 2 GB trống thì bật swap theo [deploy-vps.md](deploy-vps.md) mục 2, hoặc chỉ bật staging trong buổi kiểm thử (mục 8).
- **Đĩa:** `df -h /`, cần còn ít nhất 5 GB cho image và dữ liệu staging.
- **CI:** nhánh `staging` phải có lần chạy `CI` thành công sau thay đổi này. Job `images` đẩy image tag theo SHA lên GHCR, như với `main`.

## 1. Tên miền và tường lửa

1. Tạo bản ghi A `<STAGING_DOMAIN>` trỏ về IP VPS. Nếu dùng Cloudflare thì để **DNS only** (mây xám), giống production ([deploy-vps.md](deploy-vps.md) mục 7b).
2. Mở cổng: `ufw allow 8443/tcp`.

## 2. Lấy mã và tạo `.env`

```bash
git clone https://github.com/hygef-v4/AI-Powered-Learning-Platform.git /opt/platform-staging
cd /opt/platform-staging && git checkout --detach origin/staging
cd infra
cp .env.staging.example .env && chmod 600 .env
```

Điền `.env`:
- Mọi mật khẩu và khóa phải **khác production**. Tạo bằng `openssl rand -base64 32`.
- `DOMAIN`, `APP_PUBLIC_URL` (có `:8443`), `U01_BOOTSTRAP_ADMIN_EMAIL`.
- Mẫu đặt `U03_REQUIRE_DRIVE=false` nên tệp lưu trên đĩa. Muốn staging lưu lên Google Drive thì thêm các biến ở [deploy-vps.md](deploy-vps.md) mục 6. Dùng chung tài khoản Gmail với production thì đặt `GOOGLE_DRIVE_FOLDER_NAME` khác production, nếu không tệp và backup của hai môi trường nằm chung thư mục.
- `IMAGE_TAG` là SHA của lần CI thành công mới nhất trên `staging`:

  ```bash
  curl -fsS 'https://api.github.com/repos/hygef-v4/AI-Powered-Learning-Platform/actions/workflows/ci.yml/runs?branch=staging&event=push&status=success&per_page=1' | jq -r '.workflow_runs[0].head_sha'
  ```

Giữ nguyên ba dòng `COMPOSE_PROJECT_NAME`, `COMPOSE_PATH_SEPARATOR`, `COMPOSE_FILE`. Nhờ chúng, `docker compose --env-file .env ...` trong thư mục này luôn tác động lên staging. Nếu thiếu, lệnh sẽ nhắm vào project `platform` của production.

## 3. Mật khẩu Mailpit và token Cloudflare

```bash
cd /opt/platform-staging/infra
docker run --rm httpd:2.4-alpine htpasswd -nbB <tên đăng nhập> '<mật khẩu chung của nhóm>' > staging-htpasswd
chmod 644 staging-htpasswd
cp /opt/platform/infra/cloudflare.ini cloudflare.ini && chmod 600 cloudflare.ini   # token Edit zone DNS của tên miền
```

Phải tạo `staging-htpasswd` trước khi khởi động. Nếu chưa có tệp, Docker tạo một thư mục cùng tên và Nginx không chạy. Cả hai tệp đều nằm trong `.gitignore`.

Nếu `<STAGING_DOMAIN>` thuộc zone Cloudflare khác với production, tạo token mới theo [deploy-vps.md](deploy-vps.md) mục 7b.

## 4. Cấp chứng chỉ HTTPS

Cổng 80 thuộc Nginx production, và VPS chặn kết nối quốc tế đi vào, nên staging cấp chứng chỉ bằng DNS-01:

```bash
cd /opt/platform-staging/infra
docker compose --env-file .env run --rm --entrypoint certbot certbot \
  certonly --dns-cloudflare --dns-cloudflare-credentials /run/secrets/cloudflare.ini \
  --dns-cloudflare-propagation-seconds 30 -d <STAGING_DOMAIN> \
  --email <EMAIL_CUA_BAN> --agree-tos --no-eff-email --non-interactive
```

Chứng chỉ lưu trong volume `platform-staging_letsencrypt`. Service `certbot` tự gia hạn mỗi 12 giờ, và Nginx staging tự nạp lại chứng chỉ mỗi 12 giờ.

## 5. Khởi động và kiểm tra

```bash
cd /opt/platform-staging/infra
docker compose --env-file .env pull
docker compose --env-file .env up -d
docker compose --env-file .env ps        # mọi dịch vụ running; backend, worker, nginx... healthy
curl -I https://<STAGING_DOMAIN>:8443/
```

Lần đầu Flyway tạo schema mới, mất 1–2 phút.

Kích hoạt admin staging:
1. Mở `https://<STAGING_DOMAIN>:8443`, chọn kích hoạt tài khoản và nhập `U01_BOOTSTRAP_ADMIN_EMAIL`.
2. Lấy OTP ở `https://<STAGING_DOMAIN>:8443/mailpit/`.

Kết quả chạy thử trên máy dev (2026-11-15, image của `main` từ GHCR, chứng chỉ tự ký):
- Mọi container đều `healthy`. Trang chủ trả 200.
- `/mailpit/` trả 401 khi không có hoặc sai mật khẩu, 200 khi đúng.
- HTTP vào cổng 8443 được chuyển sang HTTPS.
- OTP kích hoạt admin đã tới Mailpit.

## 6. Tự động deploy theo nhánh `staging`

```bash
cd /opt/platform-staging
install -m 755 infra/deploy/platform-deploy.sh /usr/local/bin/platform-staging-deploy
install -m 644 infra/deploy/platform-staging-deploy.service infra/deploy/platform-staging-deploy.timer /etc/systemd/system/
systemctl daemon-reload
systemctl start platform-staging-deploy.service      # thử một lần: không có bản mới thì không làm gì
systemctl enable --now platform-staging-deploy.timer
```

- **Tách khỏi production:** staging dùng bản script riêng (`platform-staging-deploy`), cài từ checkout staging. Sửa script trên `staging` không đổi script production đang dùng.
- **Quy trình giống production** ([deploy-vps.md](deploy-vps.md) mục 10): kéo image, sao lưu database vào `/var/backups/platform-staging` (giữ 3 bản), `git checkout` đúng SHA, chờ khỏe, lỗi thì tự quay về.
- **Không deploy cùng lúc:** staging và production dùng chung khóa. Lượt đến sau báo "đang có một lần deploy khác chạy" và thử lại ở lần hẹn giờ kế tiếp.

Lệnh tay:

```bash
export PLATFORM_DIR=/opt/platform-staging STATE_DIR=/var/lib/platform-staging-deploy BACKUP_DIR=/var/backups/platform-staging \
  DEPLOY_BRANCH=staging DEPLOY_COMPOSE_PROJECT=platform-staging \
  DEPLOY_COMPOSE_FILES="docker-compose.yml docker-compose.staging.yml" DEPLOY_HTTPS_PORT=8443
platform-staging-deploy status
platform-staging-deploy rollback
journalctl -u platform-staging-deploy.service -n 50
```

Khi script trong repo thay đổi, chạy lại lệnh `install` đầu tiên của mục này.

## 7. Cho nhóm kiểm thử

1. Admin staging tạo tài khoản cho từng thành viên, với email thật hoặc email thử như `ten.test@fpt.edu.vn`. Không dùng chung một tài khoản.
2. Gửi riêng cho từng người, không dán vào nhóm chat:
   - địa chỉ staging;
   - tên đăng nhập và mật khẩu Mailpit.
3. Mỗi người tự kích hoạt tài khoản bằng OTP đọc trong Mailpit. Mailpit chứa email của **mọi** tài khoản staging, nên chỉ phát mật khẩu cho người trong nhóm.
4. Chấm bằng AI: mỗi người tự thêm key AI thử, có giới hạn, ở phần cài đặt AI. Không dùng key của production.
5. Báo lỗi bằng GitHub Issues: bước làm, kết quả mong đợi, kết quả thực tế, ảnh chụp, vai trò và tài khoản.

## 8. Bật/tắt và dọn dữ liệu

```bash
cd /opt/platform-staging/infra
systemctl stop platform-staging-deploy.timer && docker compose --env-file .env stop      # tắt khi không kiểm thử
docker compose --env-file .env start && systemctl start platform-staging-deploy.timer    # bật lại
docker compose --env-file .env down -v    # XÓA toàn bộ dữ liệu staging, kể cả chứng chỉ; làm lại từ mục 4
```

Chỉ chạy `down -v` trong `/opt/platform-staging/infra`, sau khi đã kiểm tra `docker compose --env-file .env config | head -1` in ra `name: platform-staging`.

## Sự cố thường gặp

| Hiện tượng | Nguyên nhân và cách xử lý |
|---|---|
| Nginx staging khởi động lại liên tục | Chưa có chứng chỉ cho `DOMAIN` (làm lại mục 4), hoặc `staging-htpasswd` là thư mục: xóa đi rồi tạo lại tệp (mục 3) |
| Đăng nhập xong bị đẩy ra | Mở bằng HTTP hoặc bằng IP. Phải dùng `https://<STAGING_DOMAIN>:8443`, và `APP_PUBLIC_URL` phải có `:8443` |
| Không thấy OTP | Xem `docker compose --env-file .env logs worker mailpit`. OTP do worker gửi |
| Production chậm hoặc bị OOM khi staging chạy | `docker stats`. Tắt staging (mục 8) hoặc thêm swap |
| Timer staging báo "đang có một lần deploy khác chạy" | Production đang deploy; lần hẹn giờ sau sẽ chạy tiếp |
