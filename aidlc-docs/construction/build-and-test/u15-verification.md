# U15 — build, test và acceptance gates

Scope: current-contract slice A, ngày 2026-10-20. Đây là supplement cho brownfield Construction, không thay kết quả hoặc gate unit khác và không đánh dấu global Build & Test hoàn tất. Xem [handoff](../u15-admin-dashboard/code/implementation-summary.md) và [plan](../plans/code-generation-plan/u15-admin-dashboard-code-generation-plan.md).

## Commands đã chạy

Chạy tại repository root, PowerShell, Java 21.0.12.1/Maven 3.9.12 (pom target Java17), Node/npm và dependencies hiện có. Không cài thêm package. Maven dùng local-cache profile và ByteBuddy agent đã có của workspace; nếu máy khác đủ network/cache có thể dùng Maven launcher thông thường.

```powershell
$u15Maven = 'C:/Users/Quang Hung/.m2/wrapper/dists/apache-maven-3.9.12/59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798/bin/mvn.cmd'
& $u15Maven -o -s scripts/maven-local-cache-settings.xml -f backend/pom.xml '-DargLine=-javaagent:F:/code/git/AI-Powered-Learning-Platform/.tmp/byte-buddy-agent.jar' '-Dtest=edu.aiplatform.admindashboard.**,AdminDashboardPolicyTest,AuthServiceTest,ArchitectureTest' test
& $u15Maven -o -s scripts/maven-local-cache-settings.xml -f backend/pom.xml '-DargLine=-javaagent:F:/code/git/AI-Powered-Learning-Platform/.tmp/byte-buddy-agent.jar' verify
$env:TEMP = (Resolve-Path .tmp/u05-clean-code-temp).Path
$env:TMP = $env:TEMP
npm --prefix frontend test -- src/features/admindashboard --maxWorkers=1
npm --prefix frontend test -- --maxWorkers=1
npm --prefix frontend run lint
npm --prefix frontend run typecheck
$env:NEXT_DIST_DIR = '.next-verify-u15'
npm --prefix frontend run build
```

Scratch paths phải tồn tại và agent JAR phải có trước lệnh. Dùng process-local TEMP/TMP do hạn chế temp directory của sandbox. Snapshot tsconfig trước build và restore đúng nội dung snapshot sau build vì Next tự thêm generated type paths; chạy typecheck lại. Không bỏ compiler/linter/test rule. Lượt này đã restore, không còn diff tsconfig.

## Evidence

- Backend full sau sửa review: 755 total / 693 passed / 62 skipped / 0 failures/errors, JAR `backend/target/platform-backend-0.1.0-SNAPSHOT.jar`; report `backend/target/surefire-reports`.
- Focused backend 49 passed gồm 31 U15/policy và 18 AuthService/Architecture. Three range-audit cases thay three range-parser cases, nên tổng không tăng sau review fix.
- Frontend focused 21 passed / 5 files; full 253 passed / 52 files; lint/typecheck/build pass. Build route table có `/admin`; output `frontend/.next-verify-u15`.
- Local logs `.tmp/u15/`: baseline-backend/ baseline-frontend, backend-red/api-red/frontend-red/home-red, review-red (3 assertion failures 403 vs 400), review-green, final-backend, frontend-focused-final, final-frontend, lint, typecheck-final và build. Scratch không được commit.
- Parsed OpenAPI YAML 3.1 với Python/PyYAML hiện có và resolve toàn bộ local `$ref`; không chạy external OpenAPI conformance validator. Whitespace và diff owner scope kiểm qua Git.
- Independent review P2 guard-order đã sửa bằng RED/GREEN + full verify; không finding cụ thể khác. Frontend production không đổi sau successful build, chỉ thêm test public UI và chạy lại tests/lint/typecheck.

## Tester/integration matrix — pending

Môi trường dùng thật U01/U02/U03/U04/U05, contract stubs disabled; khởi tạo migrations theo tài liệu owner. Không thay DB dùng chung hoặc tự bật stub để nghiệm thu. Requirement/code/test-case owners cần độc lập, chưa gán owner tự động.

| Scenario | Kết quả cần chứng minh |
|---|---|
| ADMIN ACTIVE login/current session | Redirect `/admin`; API 200; số liệu khớp DB, không contact/ID/archive |
| Non-ADMIN, revoked role, inactive/deleted account | Không đọc source; 401/403 theo session/current authorization; audit DENIED được persisted |
| Non-ADMIN với page=-1 hoặc size=0/101 | 403 và deny audit; ADMIN nhận 400, không gọi history provider |
| Nguồn count lỗi/null/timeout | Section UNAVAILABLE; nguồn khỏe giữ số thật, empty hợp lệ khác lỗi |
| Backup CREATED/RUNNING/FAILED/SUCCESS, no runs, purged | Đúng trạng thái/metadata; không suy purged từ tuổi, bản SUCCESS mới nhất có thể quá 7 ngày |
| Settings disabled/thiếu giờ/thay đổi | Schedule dự kiến đúng timezone; tôn trọng cache30s/stale policy U03 |
| History paging/error/close/reopen/session expired | Không trộn page hoặc phản hồi cũ; retry mới; 401/403 xóa metrics/dialog; focus return |
| Source khỏe, tải đại diện | Đo healthy p95 <= 1 giây, có workload/environment/dataset và percentiles |
| Source chậm, queue đầy, bỏ qua interrupt | Deadline 2 giây từ submit + overhead; pool8/queue32 hữu hạn; query/connection release được đo riêng |
| Full application startup/browser | JWT/filter chain/headers/real providers; prerequisite U07/U08 migrations của U09 được xử lý bởi owner |

U03 metadata C01/C02 vẫn pending: startedAt null, latestSuccessfulAt/failuresLast7Days UNAVAILABLE. Bổ sung qua contract handoff riêng rồi kiểm lại đúng cửa sổ thời gian và retention. Chưa full UC66 acceptance, chưa NFR certification, chưa triển khai Operations/Jira Done.
