# Shared infrastructure — Construction revision 2026-10-13

Nguồn [current contract](current-srs-contract.md), [Inception requirements](../inception/requirements/requirements.md). Documentation-only; không thay Compose/runtime/migration. Baseline cũ trong historical/baseline-before-66-uc-2026-10-13/.

## Deployable và data

| Component | Scope |
|---|---|
| Nginx / frontend | HTTPS edge; Next.js desktop-first; cookie secure/HttpOnly/SameSite và CSRF cho mutation |
| Spring Boot backend / worker | Một modular monolith cùng code, 15 logical units; worker jobs/email/AI/backup |
| PostgreSQL | Durable domain, quota windows/reservations, audit, outbox; Flyway migrator riêng runtime role |
| Redis | Sessions/cache/rate limits có TTL; không authoritative quota hoặc missing usage; mất Redis không reset usage budget |
| RabbitMQ | jobs/finite retry và platform.events/realtime; durable outbox hồi phục từ PostgreSQL |
| Google Drive | Private live files và independent encrypted backup folder; credential least privilege |
| Brevo / Mailpit | Demo/production SMTP qua MailPort; local/test Mailpit; provider quota có giới hạn |

Không deploy payment gateway, code-execution sandbox hoặc queue jobs.code/jobs.payos. Queue mục tiêu jobs.internal/jobs.email/jobs.ai/jobs.drive/jobs.backup và jobs.notification/realtime; link YouTube không cần processing queue; extension video-processing/RAG chỉ bật khi chốt riêng. Queue count/concurrency/CPU/RAM phải benchmark hạ tầng hiện có, không giả lập bằng con số baseline có sandbox.

## Network và secrets

- Only Nginx publish 80/443; datastore/backend/worker internal, admin RabbitMQ qua SSH tunnel. Firewalls hạn chế ingress, outgoing allowlist theo adapters provider/Drive/SMTP actual configuration.
- /api routes auth/version/rate; /mcp route cùng HTTPS được cấu hình transport client thực hỗ trợ, authentication gắn ALAP actor/scopes/expiry/revoke; không expose database hay provider secrets.
- Frame CSP/origin checks chỉ Draw.io cần thiết; YouTube optional. SSE buffering disabled với keepalive và current membership auth, không dữ liệu private-draft khác.
- DB/migrator/Redis/RabbitMQ/JWT/SMTP/Drive secrets từ environment/secret store, không commit/log. API key người dùng/tổ chức app-managed lưu encrypted ciphertext + keyVersion; master key riêng ngoài DB, decrypt server only, masked response. Quota/model policy non-secret trong Settings/owner tables.
- Backup archive encryption key/recovery secrets lưu riêng ngoài archive và VPS, quyền operator. Rotate phải preserve ability giải mã snapshot cũ, không hủy key version còn retention; không mã hóa toàn bộ DB bắt buộc.

## Backup và restore runbook

1. Admin xác nhận schedule/timezone/retention/folder/storage quota và operator key custody; configuration không tự bật lịch số giả.
2. One scheduled run/slot: consistent PostgreSQL snapshot, schema/app version và file manifest. File nguồn immutable version hoặc content hash; block SUCCESS nếu không lấy được phiên bản phù hợp snapshot. Không đặt lock toàn ứng dụng lâu cho upload.
3. Copy referenced files to independent backup folder, verify hashes, dump/manifest encrypt authenticated archive, upload private Drive và read-back integrity check. Persist SUCCESS chỉ khi đầy đủ; FAILED ghi safeError/outbox cảnh báo U16.
4. Retention chỉ sau success và preserve last-good; retries bounded, không xóa live source, không sync-delete backups theo live deletion. Drive dung lượng insufficient rõ FAILED, không giả archive complete.
5. Isolated restore: lấy retained key/migration artifacts, decrypt/check hashes, restore DB/schema/files và map logical file refs; verify scopes, document XML, submissions/grades, usage/quota và audit counts; Redis sessions không backup để resurrect token cũ.
6. Sau restore thu hồi ALAP sessions/MCP credentials đã snapshot, không tự dispatch outbox/jobs đang pending từ snapshot trước khi đối chiếu; tránh email/AI lặp và UNKNOWN quota charge. Re-enable task dispatch có operator evidence. Không tự reset measured quota trong active windows.
7. Record RPO/RTO thực đo + full evidence; checksum không thay restore test. Production restore/cutover cần runbook/chấp thuận vận hành riêng, không endpoint UI/MCP.

## Deployment/migration/rollback

Pin image/dependency lock, SHA tags, healthchecks and logs bounded. Additive migrations và data audit trước code refactor; giữ rollback-compatible fields/snapshot representations. Backup + isolated restore trước migration quan trọng; copy mapping old assessment types (không silently convert retired code/payment). Deploy không claim code được generated bởi docs. Rollback image không tự rollback destructive DB schema.

## Email / observability

OTP U01, learning emails + important announcements + backup failure U16 shared MailPort. After-commit outbox dedup event/recipient/channel, finite retry, provider accepted mới SENT, no inbox guarantee. Rate/config budget chừa OTP, không cam kết free provider always available. Link minimal login, no grades/raw files/keys/archive attachments. Admin có backup alerts, không academic class emails.

Healthcheck readiness/logs và application backup failure alerts; không full monitoring stack, multi-zone, autoscaling hoặc automated DR. Target p95 500ms/100 concurrent baseline chỉ local metadata APIs; asynchronous external jobs measured riêng. Model/provider compatibility, Drive credentials/quota và SMTP thực tế phải test khi code/deploy.

## Extensions

Enabled Security subset 03/04/05/08/09/12/15 và Resiliency subset 04/06/10 được thể hiện auth/input/secrets/audit/failures/health/deploy. Other rules N/A theo state; PBT disabled. Runtime compliance chưa verified, không dựa design để đánh tests pass.

## Current UC table integration

UC70/71 Settings UI routes masked institutional API reads/mutations through U13; secret input is protected/encrypted at the connection owner and never exposed in logs or ordinary Settings JSON. U03 neutral adapters fail closed/unavailable when U13 is not ready; no hard dependency cycle is introduced.

YouTube references store validated educational video URLs/title, without automatic download/transcription/AI jobs. Open externally or embed only when supported by validated source/origin and CSP. Invalid links, unavailable videos or disabled embedding return clear user-facing states; learning work remains usable.
