# Tự động deploy lên VPS khi có code mới trên main

Yêu cầu ngày 2026-11-06: "tôi muốn setup ci cd để khi code được đẩy lên main sẽ tự động deploy lại ở vps (nhưng có thể rollback lại bản gần nhất nếu lỗi)". Người dùng chọn cách để VPS tự kéo bản mới về.

## Hạn chế mạng (đã kiểm từ VPS)

- Từ nước ngoài không vào được VPS (xvps chặn). GitHub-hosted runner vì thế không SSH được vào VPS.
- VPS không tới được `pipelines`/`broker.actions.githubusercontent.com`, nên không cài được self-hosted runner. Repo lại public, nên self-hosted runner cũng không nên dùng.
- VPS tới được `api.github.com`, `github.com` và `ghcr.io`. Image trên GHCR public, repo public: không cần token.

## Thiết kế

- Không đổi CI: CI vẫn kiểm backend, frontend và đẩy image tag theo SHA khi push lên `main`. Job `deploy` mẫu (`if: false`) bị bỏ và thay bằng ghi chú trỏ tới runbook.
- `infra/deploy/platform-deploy.sh` cài trên VPS ở `/usr/local/bin/platform-deploy`, có các lệnh:
  - `check` (timer gọi mỗi 2 phút):
    - Hỏi GitHub API lần chạy `ci.yml` mới nhất trên `main` có `push` và `success`. Lần chạy thành công nghĩa là job `images` đã đẩy image.
    - Nếu SHA khác bản đang chạy và không nằm trong danh sách bỏ qua thì deploy.
  - `deploy <sha>`:
    1. Khóa `flock` để không chạy hai lần cùng lúc.
    2. Kéo hai image. Lỗi ở bước này thì dừng, chưa đổi gì.
    3. `git fetch` và kiểm tra commit có tồn tại.
    4. `pg_dump` vào `/var/backups/platform/predeploy-*.dump`, giữ 5 bản mới nhất. Lỗi thì dừng.
    5. `git checkout --detach <sha>`, đặt `IMAGE_TAG`, chạy `docker compose up -d --remove-orphans`.
    6. Kiểm tra sức khỏe trong tối đa 5 phút: mọi container có healthcheck đều `healthy`, không container nào dừng, và `https://$DOMAIN/` trả 200 (gọi thẳng vào 127.0.0.1).
    7. Lỗi thì tự quay về bản trước (checkout và `IMAGE_TAG` cũ, `up -d`, kiểm tra lại), đưa SHA lỗi vào danh sách bỏ qua để timer không deploy lặp lại.
  - `rollback [sha]`:
    - Quay về bản gần nhất trước đó, hoặc về SHA chỉ định.
    - Đưa bản đang chạy vào danh sách bỏ qua để timer không đẩy lên lại.
    - Commit mới hơn trên `main` vẫn được deploy bình thường.
  - `status`: in bản đang chạy, bản trước, danh sách bỏ qua, lịch sử và trạng thái timer.
  - `unskip <sha>`: bỏ SHA khỏi danh sách bỏ qua.
- Trạng thái lưu ở `/var/lib/platform-deploy/` (`current`, `previous`, `skip`, `history.log`); log ghi vào journald.
- systemd: `platform-deploy.service` (oneshot) và `platform-deploy.timer` (2 phút; chạy lần đầu sau khi máy khởi động 2 phút).
- **Giới hạn của rollback:** chỉ quay lại image và cấu hình. Migration Flyway đã chạy thì không đảo ngược được. Nếu bản cũ không chạy được trên schema mới, khôi phục bằng bản dump trước deploy theo runbook. Vì vậy migration nên viết tương thích ngược (thêm cột hay bảng trước, xóa sau).
- Sửa healthcheck RabbitMQ: `rabbitmq-diagnostics ping` trên VPS mất khoảng 7 giây, vượt timeout 5 giây. Container bị báo unhealthy dù vẫn chạy, nên backend và worker (`depends_on: service_healthy`) không khởi động lại được, và deploy nào cũng sẽ thất bại. Đổi timeout thành 20 giây, thêm `start_period`.

## Kế hoạch

- [x] Viết `infra/deploy/platform-deploy.sh`, `platform-deploy.service`, `platform-deploy.timer`.
- [x] Test script bằng lệnh giả (`docker`, `git`, `curl`):
  - deploy thành công;
  - lỗi sức khỏe thì tự quay về;
  - pull lỗi thì không đổi gì;
  - rollback thủ công và danh sách bỏ qua;
  - `check` không làm gì khi không có bản mới.
- [x] Chạy shellcheck.
- [x] Sửa healthcheck RabbitMQ trong `infra/docker-compose.yml`, bỏ job `deploy` mẫu trong CI.
- [x] Cài lên VPS:
  - script và unit systemd;
  - áp dụng sửa healthcheck RabbitMQ bằng override riêng của VPS cho tới khi bản sửa lên `main`;
  - chạy `status`, `check` (không có bản mới), `deploy` lại đúng SHA đang chạy để thử toàn bộ đường đi;
  - bật timer.
- [x] Viết runbook `docs/runbooks/deploy-vps.md` (mục tự động deploy, rollback, khôi phục database), cập nhật `aidlc-state.md`.

## Kết quả

- Test script bằng lệnh giả (`infra/deploy/test-platform-deploy.sh`, container `ubuntu:24.04`): 11/11 qua. Các trường hợp:
  - không có bản mới;
  - deploy thành công;
  - lỗi sức khỏe, hoặc `up -d` lỗi: tự quay về và bỏ qua SHA đó;
  - pull lỗi, thiếu commit, GitHub API lỗi: không đổi gì;
  - rollback thủ công hai lần;
  - rollback về SHA chỉ định rồi `unskip`;
  - chỉ giữ số bản dump cho phép;
  - lệnh `status` và đầu vào sai.
- Kiểm chéo: tắt bước bỏ qua SHA lỗi thì 2 test tương ứng báo lỗi. Shellcheck không cảnh báo.
- CI có thêm job `deploy-script` (shellcheck và bộ test trên). Job `deploy` mẫu đã bỏ.
- Trên VPS:
  - Đã cài script và unit systemd; `systemd-analyze verify` qua.
  - `check` không làm gì vì `main` vẫn là `b91413ca`.
  - `deploy` lại đúng SHA đang chạy: sao lưu 188 KB (51 bảng dữ liệu), RabbitMQ được tạo lại với healthcheck mới và khỏe; kiểm tra sức khỏe qua; mất 28 giây.
  - Đã bật timer; lần chạy đầu thành công, lần sau cách 2 phút.
  - Healthcheck RabbitMQ mới đặt tạm trong `docker-compose.override.yml` của VPS cho tới khi bản sửa lên `main`.
- Chưa kiểm:
  - Chưa thấy một deploy thật từ commit mới trên `main`, vì cần merge.
  - Chưa thử một lần quay về thật trên VPS; đường này chỉ test bằng lệnh giả.
  - Bản dump nằm trên cùng đĩa VPS, không phải bản sao lưu ngoài máy.
- Rà soát trước khi merge vào `staging` (2026-11-07): thông báo khi quay về cũng lỗi từng có thể nêu lỗi của lần deploy trước; đã sửa và thêm test (12 test). Job `deploy-script` đã chạy trên GitHub và qua. Script đã cài trên VPS là bản trước bản sửa này; cài lại theo runbook mục 10 khi tiện.
