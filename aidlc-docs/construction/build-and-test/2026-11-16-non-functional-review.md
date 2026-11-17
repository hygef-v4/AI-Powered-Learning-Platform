# Báo cáo kiểm tra yêu cầu phi chức năng

Ngày: **16/11/2026**, múi giờ Việt Nam. Phạm vi: backend, frontend, worker, cấu hình Docker/Nginx, CI, tài liệu NFR và bộ kiểm thử trong repository.

## 1. Kết luận

**Dự án có nhiều cơ chế bảo vệ phù hợp quy mô MVP, nhưng chưa đủ bằng chứng để xác nhận đạt toàn bộ NFR.** Hai vấn đề cần ưu tiên là healthcheck phụ thuộc đồng bộ vào Google Drive và thiếu giới hạn thời gian database thống nhất. Có thêm các rủi ro về cạnh tranh khi bắt đầu Quiz, tài nguyên worker và khả năng tái tạo/khôi phục môi trường.

Không phát hiện bằng chứng mất dữ liệu hoặc sự cố production trong lần rà soát này. Đây là đánh giá mã nguồn/cấu hình và kiểm thử local có giới hạn, không phải chứng nhận an toàn hay benchmark production.

- **8 phát hiện:** 2 P1, 5 P2, 1 P3. P1: ưu tiên xử lý vì có thể ảnh hưởng khả dụng; P2: xử lý trước đợt nghiệm thu liên quan; P3: chỉnh tài liệu để tránh hiểu sai.
- **Các khoảng trống nghiệm thu:** tải mục tiêu, restore thực tế/RPO/RTO, accessibility xuyên hành trình, và lỗi phụ thuộc kéo dài.
- **Kiểm tra mới chạy đạt:** 2 test CSP; MCP hook 14 tình huống; 1 shell harness dọn tệp khi giải mã lỗi; 4 probe backup biên dịch trực tiếp từ source hiện tại.
- **Kiểm tra bị chặn:** 4 suite frontend thiếu `next-intl`; Maven thiếu hai dependency trong cache offline; integration/container không chạy được vì Docker engine chưa hoạt động. Deploy harness có 11 ca lỗi do Git Bash thiếu `flock`, 3 ca qua; không coi đó là lỗi deploy production đã được tái hiện.

## 2. Cơ sở và giới hạn đánh giá

- Baseline đọc mã: nhánh `docs/sync-ops-docs-staging`, HEAD `281362a5f5c27fb6034ada22e45f21363968cc73`. So sánh `d8ac7d212a0f07d27e83aa476291b24a3a45e2ac..HEAD` không có khác biệt trong `backend`, `frontend`, `infra`, `.github` tại lúc kiểm tra.
- Nguồn tiêu chí chính: [requirements](../../inception/requirements/requirements.md), đặc biệt NFR-001…005, SEC-001…006, REL-001…005, FR-038. Tài liệu NFR từng unit dùng để truy vết; không tự biến tiêu chí lịch sử thành scope mới khi requirements đã thay đổi.
- Áp dụng `CLAUDE.md`, quy ước code và phạm vi Security/Resiliency đã chốt. Đây là review-only; không mở lại giai đoạn thiết kế hoặc sinh mã. PBT tiếp tục tắt.
- Chỉ đọc mã/cấu hình, chạy test/probe local; không truy cập VPS, không tải thử trên production, không gọi AI thật, không sửa dữ liệu học tập, không triển khai.
- Có công việc khác diễn ra đồng thời trong workspace. Báo cáo performance, readiness, kế hoạch sửa performance và thay đổi state/audit của lượt khác được giữ nguyên. Các nhận định dưới đây gắn với baseline trên.
- Lúc chốt báo cáo **20:25 giờ Việt Nam**, HEAD đã chuyển sang `b121f0ca` và lượt khác đang sửa Next config, layout/registry, authenticated layout, notification polling cùng test. Báo cáo này không nghiệm thu các thay đổi frontend đang làm đó; không nhận chúng là thay đổi của lần review NFR.
- Báo cáo readiness local có sẵn (`2026-11-16-staging-readiness.md`, ngoài bộ thay đổi NFR) ghi CI thành công ở `d8ac7d21`; lần review này không truy vấn lại CI. Không dùng kết quả cũ để ghi các test local bị chặn thành PASS.
- [Báo cáo performance cùng ngày của lượt khác](2026-11-16-performance-report.md) có số đo production lúc tải thấp và nêu rõ chưa load test. Không dùng số đo đó để xác nhận p95 ở tải mục tiêu; không sao chép nó thành kết quả do lần review này đo.

## 3. Ma trận NFR

| Nhóm | Đánh giá | Bằng chứng và giới hạn |
|---|---|---|
| Công nghệ/tương thích, NFR-001 | Có triển khai | Next.js/TypeScript, Spring Boot, PostgreSQL, API `/api/v1`. Chưa chạy lại toàn bộ contract test. |
| Hiệu năng/quy mô, NFR-003 | Chưa nghiệm thu | Mục tiêu dưới 1.000 tài khoản, dưới 100 đồng thời, p95 API không AI ≤500 ms. Chưa có phép đo tải đại diện trong lần này; F02/F03 có thể ảnh hưởng. |
| Bảo mật, SEC-001…006 | Có kiểm soát; xác minh một phần | Auth mặc định, CSRF, kiểm session phía server, bcrypt, mã hóa API key, CSP nonce, giới hạn upload. CSP và probe crypto qua; auth/resource authorization chưa chạy lại. |
| Độ sẵn sàng, REL-002/003 | Có khoảng cách | Healthcheck, timeout Redis/SMTP/AI, retry hữu hạn đã có. F01/F02/F04 ảnh hưởng khi phụ thuộc chậm hoặc không phản hồi. |
| Toàn vẹn dữ liệu | Có cơ chế; chưa kiểm lại race bằng DB thật | Transaction, version/CAS, khóa và idempotency hiện diện. Cần kiểm thử đồng thời PostgreSQL/Redis thật sau sửa. |
| Backup/khôi phục, FR-038/REL-004 | Kiểm tra thành phần đạt; chưa nghiệm thu hệ thống | Probe mã hóa/chống sửa/cắt archive đạt. Chưa restore database thật bằng image phát hành; RPO/RTO chưa đo. F05/F08. |
| Khả năng sử dụng/trợ năng, NFR-002 | Có nền tảng; thiếu nghiệm thu toàn trình | Dialog có focus trap, Esc, trả focus, ARIA; có test tương ứng. Chưa kiểm keyboard/screen reader/contrast xuyên các hành trình ở baseline này. |
| Khả năng kiểm thử, NFR-004 | Có bộ test; môi trường local chưa tái tạo đủ | CI có backend verify, frontend lint/typecheck/test/build, shell harness. E2E nằm riêng trong `scripts/e2e`, không có job E2E/load/a11y trong workflow CI được đọc. |
| Triển khai/tái tạo, NFR-005/REL-001 | Một phần | App image gắn SHA, chạy non-root, health/rollback script và resource limits có sẵn. Base/service images còn tag thay đổi được (F06). |
| Quan sát/bảo trì | Có nền tảng; cần bổ sung bằng chứng vận hành | Log rotation, audit, dashboard và ArchUnit có sẵn. Chưa có cấu hình request timing/correlation xuyên HTTP→job được tìm thấy trong phạm vi kiểm tra; tài liệu security bị lệch (F07). |

## 4. Phát hiện có bằng chứng

### F01 — P1: Healthcheck chờ Google Drive lâu hơn ngân sách Docker

**Bằng chứng:** [DriveHealthIndicator.java](../../../backend/src/main/java/edu/aiplatform/files/integration/DriveHealthIndicator.java), dòng 33–53, gọi Drive đồng bộ khi cache 60 giây hết hạn. [DriveClientFactory.java](../../../backend/src/main/java/edu/aiplatform/files/integration/DriveClientFactory.java), dòng 31–32, 78–94, đặt connect 5 giây, read 120 giây và tối đa 3 lần retry. [backend/Dockerfile](../../../backend/Dockerfile), dòng 20–21, chỉ dành 3 giây cho `/health`.

**Hệ quả:** khi Drive chậm, HTTP healthcheck có thể hết hạn trước khi indicator kịp trả `UP` với trạng thái `DEGRADED`. `checkedAt` chỉ cập nhật sau lời gọi; các probe tiếp theo có thể tiếp tục gọi Drive trong lúc probe trước chưa xong. Docker có thể đánh dấu unhealthy và quy trình triển khai chờ health/rollback có thể phản ứng với lỗi của một phụ thuộc vốn được thiết kế cho phép suy giảm. Docker Compose không tự restart chỉ vì unhealthy; báo cáo không giả định hành vi đó.

**Đề xuất:** tách liveness nhanh khỏi kiểm phụ thuộc, hoặc kiểm Drive nền và trả trạng thái cache; giới hạn deadline tổng nhỏ hơn ngân sách probe và tránh nhiều lần refresh đồng thời. **Nghiệm thu:** mô phỏng Drive treo/timeout, chứng minh probe cơ bản phản hồi trong 3 giây và không sinh hàng loạt call. Hiện đây là phát hiện tĩnh, chưa fault-injection trên container.

### F02 — P1: Chưa có ngân sách timeout database thống nhất cho luồng JDBC

**Bằng chứng:** [application.yml](../../../backend/src/main/resources/application.yml), phần datasource, chưa cấu hình JDBC socket/query timeout hoặc transaction timeout mặc định; [script tạo role](../../../infra/postgres/init/01-roles.sh) không đặt `statement_timeout`/`lock_timeout`. Một số unit có timeout riêng, nhưng [QuizAttemptService.start](../../../backend/src/main/java/edu/aiplatform/quizpractice/service/QuizAttemptService.java), dòng 57–85, chỉ dùng `@Transactional`; [lockStart](../../../backend/src/main/java/edu/aiplatform/quizpractice/repository/JdbcQuizAttemptRepository.java), dòng 55–59, chờ row/advisory lock.

**Hệ quả:** repository không bảo đảm lời gọi database luôn kết thúc trong ngân sách của REL-003. Nếu một transaction giữ khóa lâu và DB không được cấu hình bổ sung bên ngoài repo, request chờ khóa có thể giữ connection/thread, gây dây chuyền cho các request khác. Timeout HTTP ở Nginx và thời gian chờ lấy connection không thay thế giới hạn thực thi/chờ khóa của SQL.

**Đề xuất:** đặt ngân sách phù hợp cho lock, statement, transaction và I/O; chọn ngoại lệ riêng cho backup/tác vụ dài. Kiểm tra giá trị thực tế trên môi trường triển khai trước kết luận cấu hình DB ngoài repo cũng thiếu. **Nghiệm thu:** giữ khóa bằng connection A, gọi API bằng B, xác minh B thoát trong deadline, rollback và trả lỗi an toàn, pool hồi phục.

### F03 — P2: Bắt đầu cùng một Quiz bị tuần tự hóa giữa các sinh viên

**Bằng chứng:** [JdbcQuizAttemptRepository.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/repository/JdbcQuizAttemptRepository.java), dòng 55–59, khóa `quizzes ... for update` trước advisory lock riêng `(quiz, account)`. [QuizAttemptService.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/service/QuizAttemptService.java), dòng 57–85, giữ transaction qua đọc đề/lịch sử, tạo paper và insert attempt.

**Hệ quả:** các sinh viên khác nhau bắt đầu cùng Quiz phải đợi cùng một khóa độc quyền, kể cả nhánh mở lại lượt đang làm. Đây là nguy cơ dồn hàng lúc bắt đầu buổi thi; chưa có số đo để khẳng định vi phạm 500 ms.

**Đề xuất:** đánh giá khóa chia sẻ cho đọc/bắt đầu, giữ khóa riêng theo người học và bảo vệ tương tác với edit/retire. Không bỏ khóa trực tiếp. **Nghiệm thu:** race start/start, start/edit, start/retire trên PostgreSQL thật và benchmark burst 50–90 người cùng Quiz.

### F04 — P2: Worker chưa giới hạn tổng số handler sống sau timeout

**Bằng chứng:** [JobDispatcher.java](../../../backend/src/main/java/edu/aiplatform/jobs/service/JobDispatcher.java), dòng 37, dùng `newCachedThreadPool`; dòng 101–103 chỉ `future.get` có hạn rồi `cancel(true)`. Map `running` chống trùng theo một idempotency key, không giới hạn tổng số key. [JobListener.java](../../../backend/src/main/java/edu/aiplatform/jobs/integration/JobListener.java) giới hạn listener theo queue, nhưng listener tiếp tục nhận việc khi dispatcher đã trả timeout.

**Hệ quả có điều kiện:** handler không chịu interrupt, ví dụ I/O còn treo, vẫn sống sau timeout; nhiều job khác key tiếp tục được nhận có thể tích lũy thread/connection. Chưa tái hiện OOM; không kết luận tình huống này đang xảy ra.

**Đề xuất:** executor có giới hạn và backpressure, deadline I/O thực sự, theo dõi handler còn sống sau timeout. **Nghiệm thu:** handler giả cố tình bỏ qua interrupt với nhiều key, xác minh thread/queue không vượt trần và queue khác còn phục vụ.

### F05 — P2: Script restore có thể chờ PostgreSQL vô hạn

**Bằng chứng:** [restore-isolated.sh](../../../infra/scripts/restore-isolated.sh), dòng 40, vòng `until ... pg_isready; do sleep 1; done` không deadline hoặc kiểm container đã thoát. Script chỉ ghi kết thúc/thời gian restore ở dòng 53 trở đi.

**Hệ quả:** container PostgreSQL lỗi khởi động hoặc hết tài nguyên khiến quy trình phục hồi treo, không tạo kết quả thất bại hữu hạn để người vận hành xử lý. Test restore hiện chạy chỉ chứng minh dọn tệp ở nhánh giải mã thất bại, chưa bao phủ nhánh này.

**Đề xuất:** deadline khởi động, kiểm trạng thái container, thông báo lỗi rõ và hướng dẫn dọn tài nguyên tạm. **Nghiệm thu:** container không bao giờ ready hoặc thoát sớm phải làm script exit khác 0 trong thời gian xác định.

### F06 — P2: Base image và dịch vụ hạ tầng chưa khóa bất biến

**Bằng chứng:** [backend/Dockerfile](../../../backend/Dockerfile) dùng `maven:3.9-eclipse-temurin-17`, `eclipse-temurin:17-jre-alpine`; [frontend/Dockerfile](../../../frontend/Dockerfile) dùng `node:22-alpine`; [docker-compose.yml](../../../infra/docker-compose.yml) dùng các tag như `postgres:16-alpine`, `redis:7.4-alpine`, `nginx:1.28-alpine`, chưa khóa digest.

**Hệ quả:** cùng source có thể build/pull nội dung khác ở hai thời điểm. App image gắn commit SHA là điểm tốt, nhưng chưa khóa toàn bộ đầu vào build/runtime theo mục tiêu tái tạo NFR-005. Đây không phải bằng chứng các image hiện tại có lỗ hổng.

**Đề xuất:** khóa digest đã kiểm chứng, có quy trình cập nhật định kỳ và lưu manifest image của bản phát hành. **Nghiệm thu:** manifest chỉ rõ digest của mọi service/base image; diễn tập rollback bằng đúng artifact.

### F07 — P2: Hướng dẫn kiểm thử security còn cho phép đọc lại API key đã bị loại bỏ

**Bằng chứng:** [security-test-instructions.md](security-test-instructions.md) vẫn yêu cầu ngoại lệ Admin reveal institutional key. Trong khi [aidlc-state.md](../../aidlc-state.md), mục Extension Configuration, xác nhận D15 đóng ngày 11/11: API key chỉ ghi, chỉ MCP token có thể hiển thị lại; requirements hiện hành cũng ghi API key write-only.

**Hệ quả:** người kiểm thử có thể dùng tiêu chí đã hết hiệu lực, báo lỗi sai hoặc bỏ sót hồi quy ở boundary secret. Không kết luận source đang lộ key từ lỗi tài liệu này.

**Đề xuất:** cập nhật checklist theo D15, tách API key khỏi MCP token, bổ sung tiêu chí API-key reveal bị từ chối. **Nghiệm thu:** truy vết checklist tới quyết định và test hiện hành, không còn mô tả mâu thuẫn.

### F08 — P3: Công thức RPO trong runbook dễ tính trùng khoảng thời gian

**Bằng chứng:** [restore-isolated.md](../../../docs/runbooks/restore-isolated.md), dòng 46, cộng “khoảng cách giữa hai lần backup” với “thời gian từ lần backup cuối tới sự cố”.

**Hệ quả:** hai khoảng có thể chồng nhau và không dùng đúng thời điểm snapshot phục hồi được. Backup cách nhau hằng ngày cũng không bảo đảm mất dữ liệu tối đa 24 giờ nếu backup lỗi liên tiếp.

**Đề xuất:** ghi khoảng dữ liệu mất thực tế bằng thời điểm sự cố trừ thời điểm snapshot mới nhất có thể phục hồi; mục tiêu RPO phải được chốt riêng. RTO tính tới khi dịch vụ được kiểm chứng phục hồi, không chỉ thời gian `pg_restore`.

## 5. Khoảng trống và giới hạn cần theo dõi

### Hiệu năng và quan sát

Chưa tìm thấy kịch bản load test tự động hoặc bằng chứng p95/p99 tải đại diện trong bộ artifact được rà soát. Có [hướng dẫn performance](performance-test-instructions.md), nhưng hướng dẫn không phải kết quả đo. Nên chuẩn bị dữ liệu gần 1.000 tài khoản, chạy các mức 10/50/90 người đồng thời, có warmup và thời gian giữ tải, tách login, đọc danh sách, autosave, start/submit Quiz, download và tác vụ AI nền. Ghi p50/p95/p99, lỗi, lock wait, connection pool, queue depth, CPU/RAM/GC. Đối chiếu p95 ≤500 ms cho API đồng bộ không AI và tiêu chí riêng đã xác nhận.

Nginx trong repo chưa có `log_format` chứa `$request_time`/`$upstream_response_time`; backend chỉ expose health và chưa tìm thấy cấu hình request correlation chung. Có correlation ID trong message job, nhưng chưa đủ chứng minh nối được một HTTP request với các job/log sau đó. Có thể bổ sung timing/correlation vào log hiện có; không cần mở rộng sang monitoring stack trả phí hoặc ngoài scope.

### Backup và phục hồi

Mã hóa backup hiện **tùy chọn theo as-built đã ghi trong FR-038**: [BackupJobHandler.java](../../../backend/src/main/java/edu/aiplatform/backup/service/BackupJobHandler.java), dòng 184–199, thiếu public key thì cảnh báo và chép dump nguyên trạng; vẫn có thể ghi SUCCESS. Đây là rủi ro cấu hình đã được tài liệu thừa nhận, không trình bày thành regression mới. Chưa kiểm tra production có đặt khóa hay không. Với dữ liệu học tập thật, nên xác minh trạng thái mã hóa và lưu khóa giải mã riêng.

Chưa thấy bằng chứng restore thật đủ quyền, dữ liệu bài nộp/điểm, quota/usage và tham chiếu file ở baseline này. Cần diễn tập với archive thật trên môi trường cô lập, ghi snapshotAt, checksum, image digest, thời điểm bắt đầu/kết thúc kiểm tra dữ liệu và phục hồi dịch vụ. Backup database **không bao gồm tệp người dùng trên Drive**, đúng giới hạn dự án; mất tệp Drive không được giải quyết bằng restore DB.

### Accessibility và khả năng bảo trì

[Dialog](../../../frontend/src/shared/ui/Dialog.tsx) có focus trap, trả focus, ARIA và test bàn phím. Tuy nhiên chưa đủ để kết luận toàn bộ sản phẩm đáp ứng trợ năng. Nên kiểm bằng bàn phím và screen reader các luồng login/OTP, xem học liệu, làm/nộp bài và xem kết quả; kiểm mobile cho các luồng NFR-002 yêu cầu, không ép canvas phức tạp thành mục tiêu mobile mới.

ArchUnit, test contract, migration và shared conventions là nền tảng tốt. [Persistence conventions](../persistence-conventions.md) đã ghi rõ quá trình chuyển tiếp JDBC/JPA ở U04/U13/U14; không coi toàn bộ dự án đã dùng JDBC. Đây là nợ kỹ thuật có chủ đích cần quản lý theo scope riêng.

CI chưa có dependency vulnerability scan, E2E browser, load hoặc a11y gate trong workflow được kiểm tra. Không có kết quả CVE scan mới trong lần này và không tuyên bố dependency an toàn. Có thể thêm các kiểm tra phù hợp sau khi thống nhất chi phí vận hành; PBT vẫn là loại kiểm thử đã quyết định không dùng.

## 6. Kiểm thử đã thực hiện trong lần này

| Kiểm tra | Kết quả mới | Ý nghĩa/giới hạn |
|---|---|---|
| `node scripts/mcp-hooks/test_hooks.mjs` | PASS, 14 tình huống, exit 0 | Validation proposal, reject dữ liệu sai, đầu ra không phản chiếu secret fixture. Không thay cho kiểm quyền MCP server. |
| Vitest: proxy + client + download + document + noncedStyles | 1 file/2 test PASS; 4 file lỗi import | CSP nonce/header qua. Bốn suite còn lại không load vì `next-intl` thiếu trong node_modules; không phải 4 assertion failure. |
| Maven offline, 9 suite được chọn | BLOCKED trước khi chạy test, exit 1 | Cache thiếu `org.jsoup:jsoup:1.23.2` và `imageio-webp:3.12.0`. Không có kết quả backend suite mới. |
| Probe `BackupEncryptor`/`BackupDecryptTool` | PASS, 4 tình huống, exit 0 | Biên dịch hai class source hiện tại bằng JDK 21; roundtrip hơn 1 MiB, từ chối archive bị sửa, bị cắt, raw dump không đọc private key. Không thay test runtime Java 17/container. |
| `bash infra/scripts/test-restore-isolated.sh` | PASS, exit 0 | Harness dùng docker giả: dọn secret/dump khi bước giải mã lỗi; không restore PostgreSQL thật. |
| `bash infra/deploy/test-platform-deploy.sh` | 3 ca qua, 11 ca lỗi, exit 1 | Git Bash thiếu `flock`; chưa xác minh suite trong môi trường Linux phù hợp. Không thay `flock` bằng stub để làm xanh. |
| `docker version --format '{{.Server.Version}}'` | BLOCKED | Docker engine pipe không tồn tại; config Docker cũng báo access denied. Không chạy integration/load/restore thật. |

Lệnh Vitest đã chạy tại `frontend`:

```text
npm test -- --run tests/proxy.test.ts tests/shared/api/client.test.ts tests/shared/api/download.test.ts tests/shared/document/document.test.ts tests/features/materials/components/noncedStyles.test.ts
```

Lệnh Maven đã chạy tại `backend` bằng Maven cục bộ:

```text
mvn -o -B -s ../scripts/maven-local-cache-settings.xml -Dmaven.repo.local=D:/m2/repository -Dtest=IdentityWebSecurityTest,SessionStoreTest,SessionAuthorityTest,BackupEncryptorTest,BackupJobHandlerTest,JobDispatcherTest,HttpProviderGatewayTest,DashboardSourceReaderTest,ArchitectureTest test
```

Log Maven nằm ở `.tmp/nfr-20261116-backend.log`; probe và dữ liệu giả ở `.tmp/nfr-audit/`. Đây là artifact local không theo dõi bằng Git. Không sử dụng tổng test từ `target/surefire-reports` có sẵn làm bằng chứng chạy mới.

## 7. Thứ tự xử lý và điều kiện đóng báo cáo

1. **Backend/hạ tầng:** sửa F01/F02, kiểm health/lock timeout bằng phụ thuộc giả và PostgreSQL thật; bổ sung giới hạn worker F04.
2. **Nhóm vận hành:** xử lý F05, xác minh mã hóa backup, diễn tập restore và đo RPO/RTO; lưu đúng digest image (F06).
3. **Backend/QA:** kiểm lại F03 bằng race test và load test có burst cùng Quiz; đo mục tiêu NFR-003 trên stack cô lập.
4. **QA/frontend:** đồng bộ môi trường dependency, chạy lại suite bị chặn và kiểm a11y/responsive các hành trình chính.
5. **Tài liệu/QA:** cập nhật F07/F08, liên kết tiêu chí với test/evidence và thêm timing/correlation cần thiết để chẩn đoán.

Chỉ đóng từng mục khi có bằng chứng tương ứng; CI xanh hoặc probe crypto xanh không đóng thay benchmark, restore hay accessibility.

## 8. Phạm vi extension

| Control đã bật | Kết quả trong review |
|---|---|
| SECURITY-03 | Có logging/audit; chưa chứng nhận toàn bộ nội dung log runtime. Correlation/timing là khoảng trống vận hành. |
| SECURITY-04 | PASS cho 2 test CSP/header đã chạy; HTTPS/header site thật không kiểm trong lượt này. |
| SECURITY-05 | Có validation và giới hạn upload; 14 MCP hook cases qua; chưa nghiệm thu mọi API. |
| SECURITY-08 | Có guard và auth mặc định; backend authorization suite chưa chạy được. |
| SECURITY-09 | Có non-root và secret qua cấu hình; F06 liên quan khả năng tái tạo, không có scan runtime/CVE mới. |
| SECURITY-12 | Có bcrypt/session/cookie theo SEC-001; kiểm thử backend bị chặn. MFA và breached-password list đã ngoài scope dự án, không ghi thành lỗi. |
| SECURITY-15 | Có fail-closed/error handling; chưa nghiệm thu fault-injection đầy đủ. F02/F04 cần xử lý để giới hạn tài nguyên khi lỗi. |
| RESILIENCY-04 | Có deploy/rollback theo SHA; harness local chưa qua vì môi trường thiếu `flock`, restore thật chưa kiểm. |
| RESILIENCY-06 | **Chưa đạt ở thiết kế timeout healthcheck hiện tại: F01.** |
| RESILIENCY-10 | **Chưa bảo đảm đầy đủ timeout/isolation: F02/F04.** |
| Control khác / PBT | N/A theo scope rút gọn / PBT disabled. Không mở rộng yêu cầu ngoài quyết định hiện hành. |

Báo cáo hoàn tất việc rà soát, **không phê duyệt chuyển sang triển khai** khi các mục chưa đạt/chưa kiểm chứng vẫn còn mở.
