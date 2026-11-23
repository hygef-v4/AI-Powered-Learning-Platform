# Restart backend/frontend và kiểm thử sau chuyển JDBC — 2026-11-06

Người dùng yêu cầu: “giúp tôi restart fe,be chạy test”. Phạm vi: kiểm thử code hiện tại, rebuild JAR chuẩn sau khi dừng process cũ, restart backend/frontend và HTTP smoke; giữ nguyên source/dependency và các thay đổi JDBC chưa commit.

## Tiến độ

- [x] Xác định đúng backend :8080 và frontend :3000 của workspace, kiểm Docker dependencies và chụp snapshot đầu vào.
- [x] Dừng đúng process cũ, fresh backend verify đạt 1.830 test/236 suite, zero failures/errors/skips; required PostgreSQL guard 31 + 2 + 10 + 1 đạt. JAR chuẩn đã repackage thành công, không còn là thin JAR.
- [x] Frontend 799 test/116 file đạt; lint, typecheck và production build đạt. Khôi phục next-env.d.ts sau build và AGENTS.md do Next dev tự sinh; final typecheck sau restart đạt.
- [x] Backend mới PID 50084 trên :8080, frontend listener PID 3184/launcher 43712 trên :3000. Flyway validate 32 migration/no pending; health/liveness/readiness UP; frontend root/login 200, protected API trực tiếp và qua FE trả 401 như kỳ vọng.
- [x] 1.572 file source/config khớp snapshot; git diff --check đạt, frontend không có diff mới. Kết quả/state/audit đã cập nhật.

Trạng thái: hoàn thành, 2026-11-06T09:47:58Z. Không dùng số test cũ làm bằng chứng cho lượt này. Không commit/push hoặc thay schema/dependency. Security/resiliency scope giữ nguyên, PBT disabled. Log/snapshot tại `.tmp/restart-jdbc-20261106`.

## Kết quả mới

| Kiểm tra | Kết quả |
|---|---|
| Backend full Maven verify | BUILD SUCCESS, 1.830 test trong 236 suite, zero failures/errors/skips; JAR chuẩn repackage thành công |
| Required PostgreSQL guard | U04 31 + notifications 2 + U09 version/transaction 10 + visibility 1, tất cả thực chạy/không skip |
| Frontend Vitest | 799 test / 116 file đạt, exit 0 |
| Frontend lint/typecheck/build | Tất cả exit 0; final typecheck sau khôi phục config và restart cũng exit 0 |
| Backend runtime | PID 50084, port 8080, profiles local,worker; health/liveness/readiness HTTP 200 và UP |
| Frontend runtime | Listener PID 3184, launcher PID 43712, port 3000; `/` và `/login` HTTP 200 |
| Proxy/auth smoke | Anonymous GET `/api/v1/manager/classes` trực tiếp BE và qua FE đều 401 như kỳ vọng |
| Live Flyway | Validate 32 migration; schema up to date, không chạy migration mới |
| Dependencies | PostgreSQL/Redis/RabbitMQ/Mailpit/nginx hiện hữu healthy; Docker worker cũ vẫn stopped để giữ worker local hiện tại |
| Giữ nguyên đầu vào | 1.572 hash source/config khớp snapshot, pending JDBC changes được giữ nguyên; không sửa dependency/schema |

Standard JAR `backend/target/platform-backend-0.1.0-SNAPSHOT.jar` đã là executable Boot JAR, không còn thin JAR từ lượt trước. Manifest và việc không đóng gói bốn class JPA U09 cũ đã được kiểm tra. Không cần dùng artifact tách output của lượt JDBC migration để khởi động lần này.

Next build thay next-env.d.ts; Next dev đổi cấp heading trong AGENTS.md. Đã khôi phục đúng snapshot hai file sau bước tương ứng; tsconfig giữ nguyên. Không đưa thay đổi tự sinh vào source diff. Vitest có cảnh báo jsdom chưa hỗ trợ navigation/scrollTo như các lần trước; test vẫn kết thúc exit 0, không coi stderr này là failure của ứng dụng.

Logs: backend-verify.log, frontend-test.log, frontend-lint.log, frontend-typecheck.log, frontend-build.log, frontend-typecheck-final.log, backend.out/err.log, frontend.out/err.log. `http-smoke.json` ghi bảy kiểm tra HTTP; `baseline.json` giữ hash đầu vào. Tất cả thuộc scratch directory nói trên, không commit.

## Phạm vi và giới hạn

Đã thực hiện automated tests và smoke HTTP không đăng nhập, không ghi dữ liệu nghiệp vụ có xác thực. Chưa nghiệm thu UI sau đăng nhập, AI/provider thật hoặc load test. App được để chạy cho người dùng; chưa commit/push, merge hoặc deploy. Security/resiliency scope hiện hữu giữ nguyên, không thay auth/privacy/transaction policy; PBT disabled.
