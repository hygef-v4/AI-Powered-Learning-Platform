# U09 — Implementation and verification

Ngày: 2026-10-20. Branch: `grading-gradebook`, baseline `6341aed2`.

Đã triển khai mã backend/frontend cho UC42–45 dựa trên bộ thiết kế `u09-assignment-grading` và các public contract hiện có. Chưa nghiệm thu tích hợp production: U06/U07/U08 chủ yếu còn stub, U12 chưa có provider và môi trường hiện tại không truy cập được Docker. Các gate dưới đây phải được hoàn tất trước khi triển khai staging có U09.

## Phạm vi và bằng chứng

| Use case / quy tắc | Mã chính | Bằng chứng tự động |
|---|---|---|
| UC42: bài GRADED, roster, trạng thái/trễ hạn, scope theo lớp/môn | `SubmissionReviewService`, `SubmissionDetailPage`, `SubmissionViewer` | `GradingPolicyTest`, `GradingAccessTest`, `manual-workflow.test.tsx`, `SubmissionImageServiceTest` |
| UC44: checklist server, điều chỉnh/lý do, version, chốt/công bố, lịch sử | `Evaluation`, `GradingService`, `GradingBatchService`, `GradingWorkspacePage` | `EvaluationTest`, `GradingServiceTest`, `GradingCommandsTest`, `GradingBatchTest`, `workspace.test.tsx` |
| UC43: proposal AI/MCP; nguồn tường minh; correlation, snapshot, schema | `GradingProposalTransactions`, `GradingProposalService`, `GradingProposalValidator`, `GradingMcpTools` | `GradingProposalValidatorTest`, `GradingProposalResultHandlerTest`, `GradingProposalServiceTest`, `GradingMcpToolsTest`; adapter U06 thật chưa nghiệm thu |
| UC45: sổ điểm, điểm đã công bố của chính sinh viên, phân bố 20/5, export | `GradebookService`, `GradeDistributionCalculator`, `GradebookExporter`, gradebook/student slots | `GradebookServiceTest`, `GradeDistributionCalculatorTest`, `GradebookExporterTest`, `download.test.ts` |
| Lifecycle cùng transaction nộp bài, nộp lại và nhóm | `EvaluationLifecycleService`, JPA adapters, migration 1500 | `EvaluationLifecycleServiceTest`, `GroupEvaluationTest`, `EvaluationMappingTest`; PostgreSQL gate chưa chạy |
| Ranh giới module | Chỉ import public contract của unit khác; registration qua slots | `ArchitectureTest` |

Đường dẫn backend trong bảng nằm dưới `backend/src/main/java/edu/aiplatform/grading`, test dưới `backend/src/test/java/edu/aiplatform/grading` (riêng policy test thuộc identity). Frontend nằm dưới `frontend/src/features/grading`.

## Quyết định triển khai

- Chỉ Assignment `GRADED`; không cộng Quiz/Practice hay tạo tổng điểm lớp. Mọi mutation kiểm tra quyền và version lại. Student chỉ đọc điểm `PUBLISHED` của mình khi còn enrollment. Bài nộp mới chưa công bố không hiện nhầm điểm cũ; điểm cũ vẫn đọc qua endpoint attempt lịch sử.
- Checklist được tính bằng `BigDecimal` trên **rubric snapshot đã đóng băng**, thay vì gọi `IRubric.score` với rubric sống. Không thay public contract U07. `maxScore` và rubric được chụp lúc submit.
- Listener dùng `MANDATORY`; batch dùng transaction riêng từng item, dedup ID, trả partial outcome. Unique/FK/CHECK thật có trong migration; không tạo bảng giả trong production.
- Bài nhóm dùng điểm chung mặc định, giữ chỉnh riêng qua `memberOverride`; nộp lại reset PENDING và lưu lịch sử. Hoạt động được hiển thị riêng từng thành viên, không suy luận gian lận/điểm đóng góp.
- Nguồn nhóm stub của U09 trả 503 khi U12 chưa có, thay vì trả mẫu/danh sách rỗng làm giả trạng thái thành công. `ConditionalOnMissingBean` vẫn cho adapter U12 thật thay thế.
- U06 `inputVersion` là số thứ tự proposal; hash đầy đủ và rubric snapshot lưu riêng. Proposal commit trước dispatch. Callback lấy khóa evaluation trước khi đọc proposal; scalar correlation tránh stale entity trong persistence context. Polling đối chiếu task terminal để xử lý failure callback đến sớm. Không fallback nguồn; batch dừng dispatch tiếp khi nguồn lỗi.
- MCP chỉ có tool ghi proposal, kiểm tra version/hash/scope/actor của U06; không có tool lưu điểm chính thức/chốt/công bố. U09 không tự quản API key, quota hay gọi provider.
- CSV/XLSX không chứa điểm/feedback nháp, không lưu file. XLSX dùng Java ZIP với inline string cells, thay POI vì không có dependency offline; không phát formula. Đã kiểm tra cấu trúc ZIP/XML; chưa mở bằng Excel/LibreOffice thật.
- Bài làm có renderer chỉ đọc cho text/list/table, ảnh qua download token U03 sau khi kiểm tra file thuộc snapshot đã được cấp quyền. SVG preview chỉ dùng whitelist và ảnh inert; XML gốc chỉ là văn bản. Nếu không có `svgPreview`, vẫn xem được XML; dựng preview từ XML cần DocumentViewer U07.
- Route lớp dùng slug `[id]` đang có, tránh xung đột Next.js với `[classId]`.
- Sự kiện `grade.published` không chứa điểm/feedback/bài làm. Dùng U03 after-commit best effort; không cam kết durable outbox/exactly-once.

## Contract và vận hành

HTTP contract: `docs/contracts/openapi/grading.yaml` (18 operations). Cookie session và CSRF theo U01. Các request ghi điểm không nhận tổng điểm từ client. Lỗi version/state/snapshot trả 409; ngoài scope trả 404; provider không sẵn sàng phải fail closed.

Migration: `backend/src/main/resources/db/migration/grading/V20260925_1500__create_grading_tables.sql`.

**Gate bắt buộc:** schema hiện thiếu migrations U07 `assignments` và U08 `assignment_attempts`. Migration U09 có FK thật tới hai bảng này, do đó **full startup/migrate sẽ thất bại nếu chạy nhánh hiện tại trước khi có prerequisite thật**. Không bỏ FK hoặc thêm bảng giả để vượt gate. Fixture `backend/src/test/resources/grading/prerequisites.sql` chỉ phục vụ test cô lập; không được dùng để triển khai.

Khi prerequisites được bàn giao:

1. Bật PostgreSQL/Docker, chạy `GradingPersistenceTest` rồi toàn bộ `mvn -f backend/pom.xml verify`. Kiểm migration mới/nâng cấp, `ddl-auto=validate`, JPA optimistic/pessimistic locking, app không có DELETE.
2. Chạy profile `platform.contract-stubs.enabled=false` cùng U06/U07/U08 thật: submit → listener → manual/proposal → DRAFT → FINALIZED → PUBLISHED → Student → CSV/XLSX. Kiểm rollback submit, hai tab cùng version, resubmit đồng thời callback/chốt, retry dispatch và đổi giáo viên/enrollment.
3. Với U12: kiểm membership/section authors, group resubmit, điểm member và callback cũ. Contract `GroupSubmissionStatus` hiện không có membership/trạng thái đang làm trước submit, nên chưa thể dựng tiến độ từng sinh viên trước submit từ contract này. Không tự sửa contract liên unit.
4. Kiểm U14 nhận event thật, thu hồi quyền download token, mở export trên ứng dụng bảng tính, preview XML không có SVG, keyboard/focus và đối chiếu UI t16/t17/t18/t19 bằng trình duyệt.
5. Đo NFR trên dữ liệu thật: submissions p95 ≤500 ms/200 SV; gradebook p95 ≤1 s/200×30; export ≤10 s; batch 200 p95 ≤2 s. Chưa có kết quả benchmark; unit tests không chứng minh NFR.

## Kiểm tra và review

Kết quả cuối ngày 2026-10-20:

| Kiểm tra | Kết quả |
|---|---|
| Backend `mvn -o -f backend/pom.xml test` | **719 tests: 657 passed, 0 failed/error, 62 skipped**, BUILD SUCCESS. Trong đó 3 test PostgreSQL mới của U09 bị skip vì Docker không khả dụng. Có `ArchitectureTest`, test MCP và regression cho nguồn nhóm thiếu phải trả 503. |
| Frontend `npm --prefix frontend test` | **223 passed**, 45 test files, exit 0 |
| Frontend lint / typecheck | Cả hai exit 0 |
| OpenAPI | Parse YAML và resolve toàn bộ local schema refs, 18 operations |
| `git diff --check` | Qua; các file mới đã đọc kiểm tra UTF-8 |
| Backend `mvn -o ... verify` | Tests qua nhưng **đóng gói executable JAR bị chặn**: plugin repackage thiếu dependency chưa có trong Maven cache offline (`spring-boot-buildpack-platform`, `spring-boot-loader-tools`...). Chưa xác minh executable JAR. |
| Frontend `npm run build` | Compilation/TypeScript qua; **production build chưa hoàn tất** do EPERM mkdir/copy trong `.next` khi prerender/standalone. Hai lần thử đều gặp giới hạn quyền ở thư mục output. |
| Database, end-to-end, benchmark, visual/browser QA | Chưa nghiệm thu; các gate đã liệt kê ở trên |

Lệnh backend dùng Maven wrapper-cache có sẵn, JDK21 biên dịch release17 và startup ByteBuddy agent để Mockito không cần dynamic attach trong sandbox. Vitest dùng TEMP/TMP trong workspace để tránh thư mục temp child-process không tồn tại. Không thay pom hoặc cấu hình production để bỏ qua kiểm tra.

Testcontainers bị skip không tính là kiểm tra PostgreSQL thành công. Requirement owner và independent test-case owner của AI-DLC chưa được chỉ định; review code độc lập không thay thế hai vai trò này.

Review độc lập đã phát hiện và được xử lý: subject-scoped group submissions, nguồn metadata nhóm trong gradebook/export, race dispatch/callback, renderer bảng/ảnh và giới hạn idempotency reference batch. Chưa tự commit/push/merge.

## Clean code refactor — 2026-10-20

Phạm vi chỉ U09 và route của U09. Không thay API, migration hay public contract liên unit.

- Java: format 4 spaces, một câu lệnh/một dòng, import tường minh, tên biến theo nghiệp vụ; các JSON/XML string dài được tách thành chuỗi nối với nội dung không đổi. TypeScript/JSX format 2 spaces, print width 100; tách khai báo state.
- `GradebookService`: tách dựng cột, đọc tiến độ cá nhân/nhóm, chọn evaluation hiện tại, dựng ô điểm và dòng export. Không gộp hai quy tắc chọn điểm Student/distribution vốn khác nhau.
- `GradingService`/`Evaluation`: đặt tên rõ cho kiểm tra lần nộp mới nhất và thao tác nhóm; enum nội bộ thay operation string. Kế thừa điểm nhóm, chỉnh riêng và reset khi nộp lại nằm trong domain. Giữ nguyên trường snapshot/JSON để tương thích API và persistence.
- `GradingProposalTransactions`/`GradingProposalValidator`: tách kiểm tra nguồn, idempotency, tạo proposal và từng bước kiểm tra payload. Giữ nguyên thứ tự validation, transaction annotations và callback correlation.
- Workspace: tách `useGradingWorkspace` (form/save/conflict/điều hướng), `useGradingProposals` (polling), `GradingForm` và các panel hiển thị. Reload sau 409 giữ draft đang sửa; polling không ghi đè form; chỉ chuyển bài sau save thành công.
- Production U09 backend/frontend không còn dòng trên 200 ký tự (trước refactor lần lượt 118/56 dòng). Đây là chỉ dấu trình bày, không phải thước đo đầy đủ về chất lượng code.

Đã so sánh snapshot trước/sau và review độc lập: không phát hiện P1/P2 hồi quy; public contract giữ nguyên token code sau khi bỏ qua import/format/comment. Kiểm tra cuối: backend **719 total, 657 passed, 62 skipped, 0 failures/errors**, frontend **223 passed / 45 files**; lint và typecheck exit 0. Các test tích hợp cần Docker bị skip; migration/provider/NFR/production packaging gates ở phần trước vẫn chưa được nghiệm thu. Không thêm test chỉ để kiểm tra việc tách hàm/format và không tự commit/push.

## Clean Code review follow-up - 2026-10-20

The requested Clean Code skill was applied to the existing uncommitted U09 implementation. This follow-up keeps the earlier refactor and limits new production edits to four files:

- `GradingMcpTools`: private methods isolate argument/reference validation and ATTEMPT/GROUP_DOCUMENT lookup; field allowlists are named constants. Public tool metadata/schema, validation precedence, default Agent label, error codes/messages and transaction delegation are preserved.
- `GradingProposalService` and `GradingBatchService`: evaluation/task/outcome variables now state their roles; dispatch, deduplication, source blocking and per-item transaction behavior are unchanged.
- `ClassGradebookTab`: query, assignment and grade names replace terse locals; output and request parameters are unchanged.

Five retained public-seam characterization cases were added to `GradingMcpToolsTest` and passed before production edits. Final full backend: 724 total, 662 passed, 62 skipped, zero failures/errors. Full frontend: 223 passed / 45 files; lint and typecheck passed. String literals in all four changed production files were compared with the session baseline and are unchanged. Offline executable JAR packaging remains blocked by missing cached plugin dependencies. Database/provider/NFR acceptance gates above remain open.

Detailed scope, baseline and evidence: `../../plans/2026-10-20-u09-clean-code-review.md`. No commit or push.

Frontend build in this follow-up failed with TurbopackInternalError while processing unchanged `features/academics/academics.module.css`: the PostCSS child node process exited before connection. Production build remains unverified; this is distinct from the prior EPERM evidence. No build configuration or unrelated CSS was changed.

## Build repair - 2026-10-20

The prior packaging blockers have been addressed. Full backend `verify` with the optional `scripts/maven-local-cache-settings.xml` profile packages the executable JAR and reports 724 total tests: 662 passed, 62 skipped, zero failures/errors. The profile references the repository ID already recorded in this machine's cache, rather than changing versions or checksum rules.

Frontend `npm --prefix frontend run build` now runs the small build launcher: Webpack, with a default single libuv filesystem thread on Windows. This resolves the reproduced Turbopack child-connection failure and the Windows standalone-copy EPERM in this environment. Final build passed including standalone packaging on default Node 25.8.1, using NEXT_DIST_DIR only to preserve old output. Be Vietnam Pro is bundled locally with unchanged bytes/ranges/weights/fallback and 10 preloads, removing build-time Google DNS dependency; Docker copies public assets. Frontend 223 tests, lint and typecheck passed. No dependency version, U09 contract or grading logic changed.

Standalone startup logged Ready; its process was stopped after verification. HTTP requests were blocked by sandbox socket permission, so HTTP/browser and Docker image checks are not claimed. Existing skipped DB/provider and NFR gates remain open. Scope, repeatable commands and evidence: `../../plans/2026-10-20-u09-build-repair.md`. No commit/push.

## Renderer và nguồn AI U06 — 2026-10-22

Theo yêu cầu người dùng, sửa U09 trên nhánh grading-gradebook tại baseline fa1c0f06, dùng Clean Code và regression trước production edits. U15 không nằm trong phạm vi.

- SubmissionViewer chỉ lấy nội dung text/runs thay vì duyệt mọi thuộc tính object; dùng RunsView/alignClass dùng chung để hiển thị chữ đậm/nghiêng/màu/highlight, căn lề và cấp danh sách. Metadata không xuất hiện như nội dung bài làm. Vẫn đọc plain text/run-array/cell legacy; luồng ảnh qua evaluation/fileId và toàn bộ SVG sanitization/XML fallback giữ nguyên.
- AiProposalPanel và BatchActionDialog dùng AiSourcePicker thật của U06, đọc /api/v1/ai/sources khi mở, hiện nguồn/model/quota/key hint theo provider, không chọn sẵn, không gửi nguồn unavailable. Cancel không dispatch. Batch khóa gửi/đóng trong lúc chờ, hiện outcome/lỗi và chỉ retry khi người dùng chọn lại; không tự fallback nguồn.
- Giữ request DTO: SHARED chỉ source/requestRef như trước; PERSONAL thêm provider. Giữ items/evaluationId/version và requestRef mới mỗi lần yêu cầu. AI vẫn chỉ tạo proposal; không tự apply/lưu/chốt/công bố. Backend, HTTP/OpenAPI, shared/provider files, schema, dependency/lockfile và U15 không sửa. Helper aiRequest đổi input nội bộ sang SourceChoice; đã kiểm toàn bộ callers nội bộ.

RED: 5 regression mới thất bại trên code production cũ, 4 test hiện hữu đạt. GREEN focused ban đầu 12; bổ sung kiểm SHARED payload, disabled workspace và finalize/publish để full suite cuối 446 tests/78 files đạt (tăng 8 test so baseline438). Lint/typecheck và production build đạt. Build dùng NEXT_DIST_DIR=.next-u09-picker-20261022 để giữ output cũ; tsconfig do Next tự sửa được khôi phục từ snapshot trước build. Lượt typecheck đầu thiếu package của staging mới; npm ci theo lockfile giải quyết, không đổi dependency version. Mock integration test đã sửa để giữ ApiError export thật sau khi GREEN đầu phát hiện mock thiếu.

Logs ignored: .tmp/u09-picker-red.log, u09-picker-green.log, u09-picker-full-tests.log, u09-picker-lint.log, u09-picker-typecheck.log và u09-picker-build.log. Backend không chạy lại vì source/contract không đổi. Chưa gọi AI/MCP thật hoặc chạy browser E2E trong lượt này; các gate DB/session/NFR và U15/U03 còn mở. Không commit/push/Jira mutation.
