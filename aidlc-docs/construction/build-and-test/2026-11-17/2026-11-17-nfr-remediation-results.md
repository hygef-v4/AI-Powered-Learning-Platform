# Kết quả khắc phục yêu cầu phi chức năng

Ngày 17/11/2026 (giờ Việt Nam). Đã sửa **8/8 phát hiện F01–F08** của [báo cáo ngày 16/11](../2026-11-16/2026-11-16-non-functional-review.md) và kiểm chứng tại máy phát triển. Kết quả này đóng các lỗi mã nguồn/cấu hình/tài liệu đã nêu; chưa xác nhận toàn bộ chỉ tiêu NFR trên production.

## Phạm vi và baseline

- Thực hiện theo yêu cầu “sửa toàn bộ cho tôi”, sau đó “tiếp tục”; [kế hoạch](../../plans/code-generation-plan/2026-11-16-nfr-remediation.md).
- Baseline kiểm cuối: `e7110051046c17e6ef49cababd608baeea39274d` cộng các thay đổi NFR chưa commit trong workspace. Branch tại lúc kiểm: `perf/production-fixes`. Các thay đổi performance/security từ công việc khác được giữ nguyên, không tính là phần sửa của báo cáo này.
- Backend hiện dùng Spring Boot 3.5.16; Maven 3.9.16, JVM chạy test 21.0.8, Java release 17. Dockerfile backend được build lại bằng JDK 17 của image đã pin.
- Không đổi API/DTO/schema, không truy cập VPS, không ghi dữ liệu production, không commit/push/deploy trong lượt NFR này.

## Trạng thái từng phát hiện

| Mã | Mức | Thay đổi và bằng chứng | Kết quả |
|---|---|---|---|
| F01 | P1 | Drive health trả cache ngay, một probe nền, ngân sách báo suy giảm 2 giây, cache 60 giây; không tạo thêm probe khi I/O chưa dừng. Unit test dependency chậm, cache và không cấu hình Drive; integration health cũng qua. | Đã sửa |
| F02 | P1 | Pool tối đa 10 connection; chờ pool 3 giây, connect 5 giây, socket 30 giây, SQL 15 giây, lock 2 giây. PostgreSQL thật xác nhận hủy truy vấn, rollback và dùng lại connection; lỗi timeout/lock trả lỗi an toàn. Flyway/pg_dump dùng connection riêng. | Đã sửa |
| F03 | P2 | Bắt đầu Quiz dùng `FOR SHARE`, giữ advisory lock theo người học. Test PostgreSQL chứng minh sinh viên khác không bị tuần tự hóa, cùng sinh viên và sửa Quiz vẫn chờ đúng. | Đã sửa |
| F04 | P2 | Executor hữu hạn theo queue: triggered 2, AI 4, các queue còn lại 1. Hết capacity thì requeue message gốc qua listener; handler không chịu interrupt vẫn giữ slot/idempotency tới khi kết thúc. Test saturation và queue email độc lập qua. | Đã sửa |
| F05 | P2 | Restore có deadline readiness 60 giây, kiểm container đã dừng, dọn tệp nhạy cảm và container/volume tạm khi lỗi. Bốn ca shell harness và restore với image/PostgreSQL thật qua. | Đã sửa |
| F06 | P2 | Pin 10 base/service image bằng digest registry, lưu lock manifest; kiểm 13 reference ở Dockerfiles/Compose và PostgreSQL của restore; thêm guard vào CI. Backend image build thành công. | Đã sửa |
| F07 | P2 | Checklist bảo mật tuân thủ D15: institutional API key chỉ ghi, không kiểm theo hành vi reveal cũ; phân biệt contract MCP readback. | Đã sửa |
| F08 | P3 | Runbook tính RPO từ thời điểm sự cố đến snapshot có thể khôi phục; không cộng thêm chu kỳ backup. RTO tính tới khi dịch vụ được kiểm chứng phục hồi. | Đã sửa |

Chi tiết cấu hình, cách cập nhật image và lệnh kiểm restore: [NFR controls](../../../../docs/runbooks/nfr-controls.md), [restore runbook](../../../../docs/runbooks/restore-isolated.md).

## Kết quả kiểm thử

| Kiểm tra | Kết quả quan sát |
|---|---|
| Backend `mvn verify`, có Docker/Testcontainers | **2.570 tests, 0 failures, 0 errors, 1 skipped**, BUILD SUCCESS; 7 phút 25 giây. Test bỏ qua: `GradingAiEvaluationTest`, chưa bật property `ai.eval.mode`; không gọi AI thật. |
| Guard test bắt buộc của CI | 7 suite PostgreSQL bắt buộc không skip; các ca truy vết yêu cầu đạt. Thêm guard cho database timeout và Quiz concurrency. |
| Frontend liên quan | **66/66** test ở 6 file: proxy, API client/download, document, nonced styles và Dialog. Đây là kiểm hồi quy có chọn lọc, không phải full frontend build/a11y audit. |
| Script deploy | **15/15** ca qua trong môi trường Linux tạm có Bash, util-linux, jq, findutils/coreutils. |
| Restore shell harness | **4/4**: decrypt failure, PostgreSQL thoát, readiness timeout, success. |
| ShellCheck 0.10.0 | Qua cả 5 script deploy/restore/test liên quan. |
| Image pin guard | Qua 13 reference và PostgreSQL restore. |
| Docker Compose | `config --quiet` qua production/local/staging với biến môi trường giả; không khởi chạy stack hoặc đọc secret production. |
| Backend Dockerfile | Build thành công image `platform-backend:nfr-review-20261117`, image ID `sha256:09d8d1933dbb5fa2df53f64946f9776e5944b5a8883831e2b05492eb249f5ff0`. |
| Restore thực bằng image trên | Qua: dump raw, schema fixture, JSON, điểm `8.25`, số dòng. Script restore báo **5 giây** lúc 09:52:50; fixture tổng hợp nhỏ, không phải đo RTO production. Container và anonymous volume thử nghiệm đã được dọn. |

Full backend verify chạy trên mã production cuối. Sau đó chỉ chuẩn hóa trình bày hai file test health/worker; chạy lại riêng 16 test đó để xác minh. Các cảnh báo jsdom `scrollTo` ở test frontend không gây failure.

## Cách chạy lại và bằng chứng local

Từ thư mục `backend`, lệnh đã dùng trên máy này:

```powershell
& 'D:/m2/wrapper/dists/apache-maven-3.9.16/56ba1f9f/bin/mvn.cmd' -o -B -s ../scripts/maven-local-cache-settings.xml '-Dmaven.repo.local=D:/m2/repository' verify
```

Máy có Maven/dependency network chuẩn có thể dùng `mvn -B verify`. Sau đó, từ root:

```powershell
./scripts/assert-required-backend-tests.ps1
node scripts/check-image-pins.mjs
docker build -t platform-backend:nfr-review backend
```

Trên Linux có Docker:

```sh
bash infra/deploy/test-platform-deploy.sh
sh infra/scripts/test-restore-isolated.sh
sh infra/scripts/test-restore-container.sh platform-backend:nfr-review
```

Log local (không đưa vào Git): `.tmp/nfr-final-backend.log`, `.tmp/nfr-timeout-rerun.log`, `.tmp/nfr-format-tests.log`, `.tmp/nfr-deploy-harness.log`, `.tmp/nfr-backend-image.log`, `.tmp/nfr-final-restore.log`; XML test ở `backend/target/surefire-reports`. Runtime-only image dựng từ JAR đã verify cũng đã kiểm restore qua trước đó; kết quả chính ở bảng dùng image dựng từ Dockerfile đầy đủ.

## Giới hạn nghiệm thu còn lại

1. Chưa chạy benchmark API ở tải mục tiêu dưới 100 người đồng thời/dưới 1.000 tài khoản để xác nhận p95 ≤500 ms; kiểm khóa/timeouts không thay benchmark end-to-end.
2. Restore đã kiểm đường thực thi bằng dữ liệu giả nhỏ, dạng raw. Chưa diễn tập backup mã hóa với dữ liệu và khóa production, chưa đo RPO/RTO toàn dịch vụ hoặc khôi phục liên kết tệp Drive. Bộ test backup trong backend kiểm crypto/sai khóa/archive bị sửa.
3. Dialog/keyboard/CSP có test hồi quy; chưa audit accessibility toàn bộ hành trình với screen reader/trình duyệt thật.
4. Chưa chạy soak/chaos dài hạn hoặc cả stack production. Pin digest không thay quét lỗ hổng định kỳ, và không đảm bảo build byte-for-byte khi OS package được tải từ repository bên ngoài.
5. Thay đổi hiện ở workspace; giới hạn pool/SQL và worker mới chỉ có hiệu lực trên môi trường triển khai sau khi phát hành/tái tạo backend/worker theo quy trình hiện hữu.

## Tuân thủ AI-DLC trong phạm vi sửa

| Rule đang bật | Trạng thái trong phạm vi |
|---|---|
| SECURITY-03 | Compliant: giữ thông báo lỗi an toàn, không ghi SQL/credential/provider exception thô trong phản hồi mới. |
| SECURITY-04 | Compliant: giữ CSP/header hiện hữu; test proxy/document/nonce liên quan qua. |
| SECURITY-05 | Compliant: giữ validation API; tham số deadline restore kiểm số nguyên dương. |
| SECURITY-08 | Compliant: giữ quyền truy cập và contract; backend verify gồm các test quyền hiện hữu. |
| SECURITY-09 | Compliant: giữ runtime non-root, cấu hình timeout hữu hạn và kiểm cấu hình deployment. |
| SECURITY-12 | Compliant: checklist API key write-only; tệp key tạm của restore được dọn. Không thay cơ chế xác thực. |
| SECURITY-15 | Compliant: timeout/lock có phản hồi lỗi an toàn, saturation không ghi thất bại nghiệp vụ giả; kiểm rollback/recovery qua. |
| RESILIENCY-04 | Compliant: giữ quy trình triển khai/rollback; harness 15/15 qua. Không triển khai trong lượt này. |
| RESILIENCY-06 | Compliant: health Drive không chặn request, probe hữu hạn và cache có trạng thái suy giảm. |
| RESILIENCY-10 | Compliant trong phạm vi đã duyệt: cách ly capacity theo queue, timeout/backpressure và idempotency. |
| PBT | Disabled theo cấu hình đã chốt; không tự mở rộng phạm vi. |

Các cơ chế hạ tầng/HA mới ngoài phạm vi đã duyệt là N/A cho thay đổi này. Không có finding F01–F08 còn mở ở mức mã nguồn/cấu hình/tài liệu; các giới hạn nghiệm thu ở trên vẫn cần bằng chứng môi trường đại diện.
