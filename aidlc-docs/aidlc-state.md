# AI-DLC State Tracking

## System test WF03/WF04: sửa defect còn lại và bổ sung BR · 2026-11-25

Người dùng: "sửa các lỗi còn lại, sửa các br trong aidlc-docs tương ứng còn thiếu".

- [x] KAN-225 (U07): bài Luyện tập không bắt buộc rubric khi hoàn tất hoặc phát hành; thẻ rubric ghi dở vẫn bị chặn. Sửa `RubricService.listReadinessIssues` và `listRubricIssues` ở `AssignmentFormPage`.
- [x] KAN-231 (U10): đề xuất AI ghi `resolvedSource`, `resolvedProvider`, `resolvedModel` ngay sau khi gửi task (và khi tạo lại theo tài liệu, hoặc bù lúc đối soát task); `AiDraftView` trả nguồn thực dùng và thêm trường `model`; `AiDrafts` hiện nguồn kèm model.
- [x] KAN-223 (U10, U11, U08): dịch "Sai", "Xem", "Xong" trên màn quiz và trạng thái AI; lỗi phát hành dịch theo `field` và `code` (`describeViolation`), Quiz chưa có câu đổi mã thành `ITEMS_REQUIRED`; ghi chú độ tin cậy hoạt động dựng ở giao diện (`describeReliability`).
- [x] KAN-227 (U08): thẻ nhận xét AI nêu cả khóa API cá nhân và agent MCP.
- [x] KAN-229 (U11): bảng lượt làm của giảng viên nới cột Lượt, Điểm cao nhất, Rời trang và cho tiêu đề xuống dòng.
- [x] BR: BR-U07-32, BR-U08-11, BR-U08-41, BR-U08-53, BR-U10-15, BR-U10-21, BR-U10-40, BR-U11-10 ghi theo quyết định system test 2026-11-25 (KAN-224, KAN-230, WF04-006).
- [x] Kiểm tra: 431 test backend (quizzes, assignments, quizpractice) đạt, có test mới cho KAN-225 và KAN-231; frontend 355 test (quizzes, quizpractice, assignmentwork, shared, i18n) cùng test assignments đạt, typecheck và lint sạch. Phải `npm install` lại vì `node_modules` thiếu `next-intl`.
- [x] Kiểm trên systest (người dùng: "Kiểm trên systest"), build lại backend, worker, frontend từ mã đang sửa, giao diện tiếng Anh:
  - KAN-225: lần kiểm đầu lộ lỗ hổng, bài Luyện tập chỉ có một thẻ 0 điểm vẫn hoàn tất được (tổng 0 nên bỏ qua kiểm thẻ). Đã sửa `RubricService` và `listRubricIssues` để vẫn kiểm thẻ đã ghi, thêm test; build lại rồi kiểm: bài không rubric hoàn tất và phát hành được (UI hiện Finalized, Schedule), thẻ 0 điểm bị chặn (409 `ASSIGNMENT_NOT_READY`, UI báo trên thẻ).
  - KAN-231: yêu cầu AI không chọn nguồn trả `PERSONAL` · `OPENAI` · `gpt-6-luna` ngay khi PENDING và khi READY; khung đề xuất hiện "Personal OpenAI · gpt-6-luna".
  - KAN-223: "View" ở danh sách Quiz practice, "Correct 2 · Wrong 1" và nhãn "Wrong" ở kết quả, nút "Done" khi sửa đề xuất, lỗi phát hành "The quiz is not ready to publish…" và "A quiz needs at least 1 question.", ghi chú hoạt động tiếng Anh; không còn câu tiếng Việt ở hộp thoại lượt làm.
  - KAN-227: thẻ nhận xét AI hiện "Feedback uses your personal API key or a connected MCP agent" kèm link AI connection.
  - KAN-229: tiêu đề Attempts, Highest score, Left page nằm trong ô, không chồng nhau ("Highest score" xuống dòng).
- [x] Jira (người dùng: "Cập nhật Jira và sheet"): KAN-223, KAN-225, KAN-227, KAN-229, KAN-231 có comment kết quả retest và chuyển Done.
- [x] Bằng chứng (người dùng: "các ảnh chụp bằng chứng đang bị mờ…", chọn "Round 2 + chụp lại Round 1"): chạy lại các bước Round 1 trên dữ liệu mới, chụp PNG 2880x1800 (1440x900, mật độ 2x) vào `.tmp/systemtest/proof-2026-11-25/system test proof/`. Round 1 (recaptured 2026-11-25) chụp lại 47/51 ảnh cùng tên; 4 ảnh lỗi đã sửa thay bằng Round 2: WF03 3 ảnh (KAN-225, KAN-227), WF04 7 ảnh (KAN-223, KAN-229, KAN-231). Mỗi thư mục có `_README.txt`. Script: `.tmp/systemtest/wf0304/shoot*.mjs`, `wf04lib.mjs`.
- [x] Sheet Report-5.3: Round 2 Passed 2026-11-25 cho WF03-001, WF03-002, WF04-001, WF04-002, WF04-004, WF04-005, WF04-006; cột Note thêm dòng Round 2 kèm tên thư mục bằng chứng. Các ca khác giữ Pending.
- [x] Dịch nốt (người dùng: "sửa nốt các chỗ chưa dịch còn lại"), chỉ sửa frontend, không đổi API:
  - Tiêu đề bảng "MSSV", "STT" viết cứng ở 7 bảng (lượt làm quiz, ghi danh, sổ điểm, nhóm lớp, nhập tài khoản, danh sách tài khoản, Log) dùng khóa i18n ("Student ID", "No.").
  - Thông báo trên chuông dịch theo `type` (`notifications/model/labels.ts`, `describeNotification`); câu tiếng Việt trùng câu U14 lưu, loại lạ giữ câu server.
  - Hai cảnh báo nguồn tài liệu của đề xuất AI dịch theo câu cố định (`describeSourceWarning`), áp cả đề xuất cũ.
  - Số theo ngôn ngữ giao diện (`i18n/number.ts`: `NUMBER_LOCALE`, `formatNumber`): điểm quiz, token, dung lượng tệp, biểu đồ dashboard.
  - Số ít, số nhiều tiếng Anh bằng ICU plural cho "rubric(s) have point errors", "document(s) selected".
- [x] Kiểm tra: frontend 1239 test đạt (thêm test thông báo, cảnh báo AI, plural, định dạng số), typecheck và lint sạch. Build lại frontend systest, kiểm giao diện tiếng Anh: chuông "A new quiz is available", điểm "3.5 / 4", cột "Student ID", "1 rubric has point errors", "1 document selected", cảnh báo AI tiếng Anh. Ảnh ở `.tmp/systemtest/proof-2026-11-25/_i18n-verify/`.
- [ ] Còn tồn ngoài giao diện: nội dung câu hỏi do AI viết và email thông báo vẫn tiếng Việt (prompt và email ở server). KAN-226 cần xóa tay trên Jira; người dùng tự đẩy ảnh lên Drive. Đã chia thành các commit nhỏ trên nhánh `system-test` (người dùng: "chia thành các commit nhỏ lên branch"), chưa push.

## U02: Nhật ký chi tiết hơn, bỏ ghi tra cứu audit · 2026-11-25

Người dùng: "kiểm tra phần Log và làm chi tiết hơn cho tôi (chẳng hạn tôi thấy Truy cập bị từ chối, đối tượng là lớp nhưng không biết là lớp nào, lý do cũng đang quá chung chung. Ngoài ra chỉ cần log các sự kiện cần thiết và quan trọng, không log hành động tra cứu audit".

- [x] Nguyên nhân: U05 ghi `ACCESS_DENIED` với loại `COURSE_CLASS` nhưng bộ tra tên chỉ hiểu `CLASS`; thao tác theo môn ghi id môn dưới loại lớp; details rỗng nên không biết thao tác bị chặn; giao diện rút lý do còn 2–3 chữ.
- [x] Backend: bỏ ghi `AUDIT_QUERIED` (bỏ tham số `recordView` khỏi `IAuditQuery` và API), ẩn các dòng cũ; tra tên lớp cho `COURSE_CLASS`; `AcademicsService` ghi kèm `action`, `actorRole`, `audience`, `subjectId` khi bị từ chối và ghi loại `SUBJECT` cho `MANAGED_SUBJECT_VIEW`, `CLASS_CREATED`.
- [x] Frontend màn Log: dòng phụ "thao tác bị chặn" dưới Hành động, lý do thành câu đầy đủ (kèm vai trò khi sai vai trò), bấm STT mở dòng chi tiết (mã đối tượng, thao tác, vai trò, màn, mã lý do, khóa details khác). Chuỗi vi/en dưới `audit.model.details`.
- [x] Kiểm tra: 246 test backend (Audit*, Academics*, ContractStubsTest, AdminDashboard*) đạt; frontend 26 test audit, 4 test i18n, typecheck và lint sạch.
- [x] Đợt 2 ("làm tiếp vai trò lúc đó cho nhóm, chấm điểm và quiz"): thêm `AuditDetails.describeDenial` trong `audit.contract` (khóa `action`, `actorRole`); dùng ở `GroupAccess` (lớp và đề xuất nhóm), `GradingAccess`, `QuizAudit`, `PracticeAccess` và `AcademicsService`. Màn Log đã hiện sẵn "Vai trò lúc đó" nên không đổi frontend.
- [x] Kiểm tra đợt 2: 864 test backend (audit, U05, chấm điểm, nhóm, quiz, luyện tập, `ArchitectureTest`) không lỗi; 4 test integration cần Docker không chạy được vì Docker chưa bật.
- [ ] Chưa chạy lại trên backend local (đang chạy bản cũ ở 8080) nên chưa xem màn thật; các dòng cũ đã ghi trước khi sửa vẫn thiếu thao tác bị chặn. Chưa commit.

## Chấm điểm bài cá nhân và bài nhóm · Round 1 · 2026-11-24

- [x] Tạo fixture PE với Context Diagram, Use Case Diagram và Conceptual ERD theo COMET. Chạy seed và kiểm tra batch thực qua API bằng `scripts/e2e/wf01-wf02-grading.mjs`.
- [x] Đọc snapshot và lưu đề xuất qua MCP bằng `scripts/e2e/wf01-mcp-client.mjs`.
- [x] Phiên Orca GPT-6 Luna High chạy UI trên dữ liệu demo. 8/8 ca Passed sau kiểm tra lại OpenAI, lịch sử MCP và giới hạn điểm thành viên.
- Report 5.3, Round 1: WF01 5 Passed, WF02 3 Passed. Tổng workbook 14 Passed, 0 Failed, 110 Pending trên 124 ca.
- Workbook: https://docs.google.com/spreadsheets/d/1kTzIL-Xg6MdJg0Ms5vnCae-lfVvYeA9U/edit?gid=1203805996
- Bằng chứng và báo cáo: https://drive.google.com/drive/folders/108mLgtbeBSNVGBNO7IsfgQCT5MDu8lez

## System test xác thực trên ứng dụng local · 2026-11-24

Người dùng: "giúp tôi check system test authentication system test", rồi "giúp tôi test nốt, tôi đả mở container mailpit".

- [x] 179 test identity trong repo đạt (web security, JWT, phiên và chặn tần suất trên Redis thật, OTP, tài khoản trên Postgres thật).
- [x] System test qua `localhost:3000`, 19/19 đạt: 401 khi chưa đăng nhập, sai mật khẩu trả `AUTH_FAILED`, cookie `access_token`/`refresh_token` HttpOnly và SameSite, thiếu CSRF bị 403, phân quyền admin, refresh xoay cookie, đăng xuất vô hiệu cookie cũ, dùng lại refresh token: 409 trong 20 giây và thu hồi phiên sau 20 giây (BR-U01-46).
- [x] System test OTP qua Mailpit, 16/16 đạt: kích hoạt (sai OTP, OTP dùng lại, mật khẩu yếu, không lộ tài khoản tồn tại) và quên mật khẩu (giãn cách 60 giây, mật khẩu cũ hết hiệu lực, phiên cũ bị thu hồi).
- [x] Sửa `scripts/run-backend.bat`: thêm `SMTP_FROM=no-reply@platform.local` như `infra/docker-compose.local.yml`. Trước đó backend chạy bằng script này không gửi được OTP vì `SmtpMailAdapter` từ chối khi thiếu người gửi, job `OTP_DELIVERY` thử lại mãi.
- [x] Đưa hai script vào repo: `scripts/e2e/auth.mjs` (25 ca, gồm đổi mật khẩu khi đang đăng nhập ST-WF07-005) và `scripts/e2e/auth-otp.mjs` (16 ca), đọc tài khoản và mật khẩu từ biến môi trường, không in bí mật; chạy lại trên local đều đạt. README e2e có mục U01.
- [x] System test giao diện thật WF07 Round 1 (ST-WF07-001 đến 006) trên localhost:3000 bằng trình duyệt Orca, đạt 6/6; ảnh bằng chứng theo từng bước đưa lên Drive thư mục system test proof; trạng thái Round 1 cập nhật vào sheet. Không đổi mã nguồn.
- [x] `scripts/e2e/lib.mjs`: `Session` giữ cookie theo tên và đường dẫn như trình duyệt, thêm `copyCookies`. Trước đó jar chỉ theo tên nên lệnh xóa cookie `refresh_token` ở đường dẫn cũ làm mất cookie hợp lệ và refresh trả 401.
- [ ] Các script e2e cũ (U02, U06, U12) chưa chạy lại sau khi đổi `lib.mjs` vì cần stack Docker `8088`. DB local còn tài khoản test `otp.*@school.test`. Chưa commit.


## Sửa rủi ro MCP, đợt 1 · 2026-11-23

Người dùng: "giúp tôi làm lần lượt từng bước" theo danh sách 11 vấn đề MCP đã rà soát. Đợt 1 làm bước 1 đến 3.

- [x] Bước 1, chẩn đoán MCP từ chối mọi lời gọi trên máy local. Nguyên nhân là chuyển tiếp cổng Docker Desktop của Redis và RabbitMQ hỏng sau khi container khởi động lại lúc 19:25 (Redis trong mạng Docker trả PONG, từ máy vào bị đóng kết nối). Khởi động lại hai container thì MCP chạy lại. Không phải lỗi code.
- [x] Bước 2, system test gọi `/mcp` qua HTTP trong `PlatformWorkflowIntegrationTest`: 401 khi thiếu token, 403 khi có `Origin`, token chỉ đọc gọi được công cụ đọc nhưng không thấy và không gọi được công cụ ghi, lời gọi thứ 61 trả `-32029`, thân yêu cầu quá 1 MB. Redis của bộ test nay bắt mật khẩu như môi trường thật.
- [x] Mục 1: Redis lỗi vẫn từ chối (BR-U06-53) nhưng trả mã riêng `-32030`, log ERROR kèm nguyên nhân, audit `RATE_LIMIT_UNAVAILABLE`. Hướng dẫn `docs/guides/mcp-clients.md` thêm mã mới.
- [x] Mục 2: `McpController` xác thực trước, rồi mới đọc thân yêu cầu tối đa 1 MB; không còn nhận `@RequestBody String`.
- [x] Mục 3: `read_authorized_submission` gắn `untrustedContent: true`.
- [x] Kiểm tra: 112 test MCP (unit và integration) và 39 test `PlatformWorkflowIntegrationTest` đạt. Trên backend local đã khởi động lại: gọi MCP thật được, 401 với thân 3 MB trong 0,01 giây, 403 khi có `Origin`.
- [x] Mục 4: sửa agent chỉ được hạ quyền; READ_ONLY nâng lên READ_WRITE trả `MCP_ACCESS_UPGRADE_NOT_ALLOWED`. Giao diện khóa lựa chọn "Đọc và ghi" và hướng dẫn tạo agent mới.
- [x] Mục 5, người dùng chọn cách B: bỏ xem lại token. Xóa endpoint `GET /api/v1/me/mcp-credentials/{id}/token`, hàm `reveal`, việc lưu bản mã, trường `revealable` và ô xem token trên giao diện. Migration `V20261123_1200__drop_mcp_token_ciphertext.sql` xóa ba cột bản mã; token đang dùng vẫn xác thực bằng hash. Cập nhật BR-U06-30/31, D05, hướng dẫn MCP, tài liệu thiết kế và test bảo mật U06.
- [x] Kiểm tra: 175 test backend (MCP, credential, controller, repository, `PlatformWorkflowIntegrationTest`) và 54 test frontend AI/i18n đạt; typecheck và lint frontend sạch.
- [x] Khởi động lại backend local: migration `20261123.1200` và `20261123.1210` áp dụng thành công; token đang dùng vẫn gọi MCP được.
- [x] Mục 9: xóa `AdminAiMcpTools`, `AdminDashboardMcpTools`, `AdminAiMcpToolsTest` và `McpInput.requireAdmin`; Admin vốn bị chặn ở `MCP_SELF`, xác thực token và registry.
- [x] Mục 8: SQL của `McpWriteRequestService` chuyển sang `McpWriteRequestRepository` (JdbcClient), giữ khóa advisory và transaction.
- [x] Mục 7: `McpWriteRequestCleanupScanner` (worker, mỗi phút) xóa bản ghi chống ghi trùng cũ hơn 7 ngày; migration `V20261123_1210` cấp quyền delete cho role runtime. Xác thực chỉ ghi `last_used_at` khi giá trị cũ quá 1 phút. Bảng audit và thống kê sử dụng giữ nguyên vì là dữ liệu đối soát và quota.
- [x] Mục 10: `scripts/test-check-mcp.py` chạy đạt 2/2; giữ lại vì kiểm tra công cụ chẩn đoán `check-mcp.ps1` cho người dùng, khác phạm vi system test.
- [x] Kiểm tra: 204 test backend (MCP, admin dashboard, `ArchitectureTest`, `PlatformWorkflowIntegrationTest`) đạt.
- [ ] Mục 6 (giới hạn tần suất theo IP) để cho reverse proxy khi triển khai, không sửa code. Chưa commit.

## Gom báo cáo kiểm thử theo ngày trong build-and-test · 2026-11-23

Người dùng: "nhóm lại các thư mục ngày tháng này trong build and test folder lại cho gọn". Chỉ đổi vị trí tài liệu, không đổi nội dung báo cáo.

- [x] 123 mục có tiền tố ngày (tệp và thư mục bằng chứng) chuyển vào 24 thư mục `YYYY-MM-DD/` ngay dưới `construction/build-and-test/`. Gốc thư mục từ 144 mục còn 45: 24 thư mục ngày, tài liệu hiện hành (hướng dẫn build và test, `build-and-test-summary.md`, báo cáo xác minh theo unit, `ai-grading-test-report.md` cùng thư mục hình) và `2026-11-08-integration-test-cases.json`.
- [x] `2026-11-08-integration-test-cases.json` giữ ở gốc vì `scripts/assert-required-backend-tests.ps1` (chạy trong CI) đọc tệp này theo đường dẫn.
- [x] Sửa 183 tệp: 558 liên kết và 52 đường dẫn dạng chữ, gồm `aidlc-state.md`, `README.md`, các `as-built-baseline.md`, kế hoạch và các báo cáo bị chuyển (liên kết tương đối thêm một cấp). Kiểm tra: 224 tệp đang được git theo dõi đều còn nguyên, không liên kết nào trỏ vào `build-and-test` bị hỏng, tổng số liên kết hỏng sẵn có không đổi (110 trước và sau).
- [x] Không sửa `audit.md` và `history/` vì là bản ghi lịch sử: các đường dẫn cũ trong đó trỏ tới vị trí trước ngày 2026-11-23. Quy tắc tìm tệp: tên tệp bắt đầu bằng ngày nằm trong thư mục cùng tên ngày.
- [ ] Chưa commit. Các nhánh khác đang sửa tài liệu trong `build-and-test` có thể xung đột đường dẫn khi merge.

## U09: Chi phí trên mỗi bài chấm hoàn tất cho eval chấm AI · 2026-11-23

Người dùng chọn hai việc từ khóa Introduction to evals ("làm việc 1,2"), sau đó giữ nguyên judge GEval và yêu cầu hoàn lại phần verdict. Chỉ đổi `scripts/ai-grading-eval`, không đổi backend hay API.

- [x] Báo cáo thêm `summary.cost`: token và chi phí trên mỗi ca hoàn tất (tổng lệch trong `--score-tolerance` so với tham chiếu), chia toàn bộ usage đã ghi cho số ca hoàn tất. Đọc usage từ bản ghi Java; `--input-price` và `--output-price` (USD mỗi triệu token) cho ra số tiền.
- [x] Cập nhật [báo cáo kiểm thử AI chấm điểm](construction/build-and-test/ai-grading-test-report.md) lượt 3 với token và thời gian: 416.997 token cho 20 ca, 27.800 token mỗi ca hoàn tất (15/20), trung vị 56 giây mỗi lần chấm. Tính từ `provider-result.json`, chưa quy ra tiền.
- [x] Báo cáo kiểm thử AI chấm điểm thêm độ lệch điểm theo lượt (sai số trung bình 0,148 → 0,065 → 0,165), kết quả theo từng đề, nguyên nhân hai ca lệch còn lại ở lượt 3 (`06-prompt-injection`, `08-wrong-actors`) và nhóm nguyên nhân của năm ca lệch. Tính từ các bảng phụ lục và bằng chứng đã commit.
- [x] Báo cáo kiểm thử AI chấm điểm thêm phần A: độ chính xác theo mục (225/234, 96,2%), hiệu quả cờ xem lại (2/9 mục lệch), token và thời gian ba lượt (lượt 1, 2 lấy từ `.tmp` cục bộ), môi trường kiểm thử, mục 5 giới hạn và mục 6 kết luận (bản nháp chờ nhóm xác nhận).
- [x] Sửa nhận định trong báo cáo kiểm thử AI chấm điểm: lượt 3 không cho thấy cải thiện so với lượt 2 (Sol vẫn 18/20, Opus chưa so sánh được, điểm lệch lớn hơn, hai ca workshop xấu đi); chỉ tốt hơn lượt 1. Thêm bảng ca đổi điểm giữa lượt 2 và lượt 3.
- [x] Chạy lặp `swd-workshop-missing-fk` (Luna `medium`, 3 lần mỗi prompt lượt 2 và lượt 3, 124.866 token): cùng một prompt ra 7,00 đến 9,25, trung bình 8,38 (lượt 2) và 8,31 (lượt 3). Rút lại nhận định prompt lượt 3 làm ca này xấu đi; cập nhật mục 4, 5, 6 và lưu bằng chứng ở [thư mục chạy lặp](construction/build-and-test/2026-11-23/2026-11-23-u09-ai-grading-rerun-evidence/). Chưa chạy lặp `swd-workshop-complete`.
- [x] Chạy lặp thêm ba ca (`03-one-normal-flow`, `05-off-topic`, `swd-workshop-complete`; Luna `medium`, 3 lần mỗi prompt lượt 2 và lượt 3, 374.382 token): lượt 3 đúng hơn ở hai ca nhắm tới (`05` 3/3 so với trigger sai 3/3; `03` 2/3 so với 0/3), hai prompt dao động như nhau ở `swd-workshop-complete` (mất 1,50 ở `s-authorization`). Sửa kết luận 4, khuyến nghị và giới hạn trong báo cáo kiểm thử; bằng chứng ở thư mục chạy lặp. 16 ca còn lại chưa chạy lặp.
- [x] Báo cáo kiểm thử AI chấm điểm thêm 7 hình: sơ đồ quy trình (Mermaid, đã render kiểm tra bằng mermaid-cli) và 6 biểu đồ matplotlib (kết quả theo đề, mục rubric và cờ, ba lượt, độ lệch điểm, chạy lặp, token và thời gian). Số liệu giữ nguyên như bảng; script vẽ và ảnh nằm ở `construction/build-and-test/ai-grading-test-report-charts/`.
- [x] Viết lại báo cáo kiểm thử AI chấm điểm cho gọn và dễ hiểu: thêm Tóm tắt và khung Thuật ngữ, gộp mục 1 và 2, gộp hai bảng ca lệch thành một, chuyển các bảng chi tiết xuống Phụ lục A, đưa khuyến nghị về mục 6, bỏ ghi chú sửa nội bộ và giọng ngôi thứ nhất. Số liệu không đổi; thân báo cáo còn khoảng một nửa độ dài.
- [x] Judge `correct/incorrect/unknown` đã làm rồi gỡ theo quyết định giữ GEval. Phán quyết có thể làm lại sau khi có nhãn của giảng viên để so hai kiểu.
- [x] Pytest của bộ eval 19/19 (18 cũ, thêm 1 cho chi phí). Chưa chạy lại trên lượt ghi thật. Chưa commit.

## U07/U03/U06: Tạo đề bằng AI nhận ảnh WebP · 2026-11-23

Người dùng: "sửa phần tạo đề ai hỗ trợ up ảnh webp đi". Chỉ mở danh sách định dạng, không đổi API hay schema.

- [x] Tệp nguồn soạn bài bằng AI (`ASSIGNMENT_SOURCE`) nhận thêm WEBP (`PurposePolicy`). Ảnh WebP được gửi thẳng cho model đọc ảnh qua `AssignmentAiDraftService.SOURCE_IMAGE_TYPES` (dùng chung với `AssignmentSourceImageSource`), `AiTaskService.IMAGE_TYPES` và ô tải tệp của `AiReviseChat`. Gemini, OpenAI và Anthropic đều nhận WebP, gateway chỉ truyền nguyên `mediaType`.
- [x] Cập nhật BR-U06-54 và mô tả `ASSIGNMENT_SOURCE` của U07. Giữ báo cáo lịch sử nguyên văn.
- [x] Kiểm thử backend: PurposePolicyTest 4, AssignmentSourceImageSourceTest 5 (có ca WebP cho tạo và sửa đề), AssignmentAiDraftServiceTest 16, AiTaskFlowTest 39, đều đạt.
- [ ] Chưa thử tải ảnh WebP thật trên giao diện; backend đang chạy cần khởi động lại để nhận thay đổi.

## U09: AI điền thẳng đề xuất vào form chấm điểm · 2026-11-23

Người dùng: bấm chấm bằng AI xong thì điểm và nhận xét nên điền luôn vào form, có nút từ chối đề xuất, không phải kéo xuống lịch sử để bấm áp dụng. Chỉ frontend, không đổi API hay schema.

- [x] Đề xuất READY do giáo viên vừa yêu cầu tự điền vào form (`useGradingWorkspace`). Form đang có chỉnh sửa chưa lưu thì không ghi đè, thanh trên form hỏi Áp dụng/Bỏ qua.
- [x] Thanh `AiDraftBanner` trên form: Giữ đề xuất / Hoàn tác (khôi phục form trước khi AI điền) / Chi tiết. Bảng so sánh `AiProposalPanel` chuyển lên ngay dưới thanh. Autosave vẫn chặn khi nháp AI chưa được giữ.
- [x] Hoàn tác hỏi xác nhận khi giáo viên đã sửa tay sau lúc AI điền (tránh mất chỉnh sửa). Ô điểm và nhận xét chung do AI điền có nhãn ✨ AI, mất khi sửa ô đó.
- [x] Nút Chấm bằng AI kẹt "AI đang chấm…" khi task kẹt ở QUEUED: thêm nút **Hủy chấm AI** trên thanh công cụ, gọi API hủy sẵn có `POST /ai/tasks/{id}:cancel` rồi tải lại danh sách. Chỉ hủy được task QUEUED; task RUNNING đã gửi nhà cung cấp, backend tự đóng (UNKNOWN) khi hết hạn gọi.
- [x] ESLint, tsc đạt; Vitest grading + i18n 234/234 (sửa 1 test cũ, thêm 4 test mới). Chưa kiểm tra trên trình duyệt thật. Làm trên nhánh `audit-staging`.

## SEO cho alapedu.online — 2026-11-22

Người dùng: "tôi muốn làm seo cho trang web"; chọn nền tảng kỹ thuật + landing page, đã có Search Console, nhánh mới từ staging. [Kế hoạch](construction/plans/code-generation-plan/2026-11-22-seo.md), [kết quả](construction/build-and-test/2026-11-22/2026-11-22-seo-results.md). Nhánh `feat/seo` từ `origin/staging@47837e52`, worktree `.claude/worktrees/seo`.

- [x] Requirements Analysis (Standard) và Workflow Planning: SEO-FR1–FR9; chỉ Code Generation và Build and Test.
- [x] Người dùng duyệt kế hoạch (2026-11-22).
- [x] Code Generation: 11/11 bước. Landing `/` dựng phía máy chủ, robots/sitemap động, noindex khu vực đăng nhập, canonical, JSON-LD, meta Search Console, staging `X-Robots-Tag` và `SEO_INDEXABLE=false`.
- [x] Build and Test: ESLint, tsc đạt; Vitest 1.203/1.203; dev server kiểm robots, sitemap, meta, giao diện desktop/375 px. `next build` dừng ở cảnh báo lỗ hổng của Next.js 16.4.3 sau khi biên dịch — chờ người dùng quyết định.

Người dùng yêu cầu commit cục bộ, push và mở PR vào staging (2026-11-22). Không tự merge; chưa deploy.

## Đồng bộ AI-DLC và năm luồng nghiệp vụ · 2026-11-22

- [x] Đối chiếu baseline `8afa00b4`, cập nhật nội dung hiện hành về học liệu, Quiz, tạo đề, phản hồi luyện tập, chấm và công bố điểm. Bảng hoạt động, năm tệp Draw.io, bản gộp và năm PNG nằm trong [Main Business Flows](inception/main-business-flows/README.md).
- [x] Cập nhật [luồng tạo và phát hành U07](construction/u07-assignment-authoring/functional-design/business-logic-model.md), [quy tắc U09](construction/u09-assignment-grading/functional-design/business-rules.md) và [hướng dẫn kiểm thử CI](construction/build-and-test/build-instructions.md). Giữ các báo cáo lịch sử theo đúng phạm vi và thời điểm đã kiểm thử.

## U07: Tạo đề AI theo cấu trúc PE · 2026-11-22

- [x] Đối chiếu 14 đề SWR302 và 6 đề SWD392 với prompt và luồng tạo đề hiện tại. 22 test backend tạo đề đạt ở lượt audit.
- [x] Lập [kế hoạch code generation](construction/plans/code-generation-plan/2026-11-22-u07-pe-assignment-generation.md) cho prompt AUTO, điểm từng câu, xem trước cảnh báo và hồi quy trước/sau.
- [x] Theo kế hoạch đã duyệt, bổ sung hướng dẫn AUTO, giữ điểm từng câu từ rubric mẫu và yêu cầu xác nhận trên UI khi có cảnh báo nguồn.
- [x] Kiểm 159 test backend, 35 test tích hợp, 92 test frontend và hai ca trên Chrome local với API mock. Đối chiếu Luna medium trên sáu brief. Prompt cuối sinh sáu kết quả đủ điểm, gồm một đề cần xem do mâu thuẫn mẫu. [Báo cáo U07](construction/build-and-test/2026-11-22/2026-11-22-u07-pe-assignment-evaluation.md)
- [x] Đo Luna medium trên 3 lượt cùng 6 đề. 18/18 đúng schema, 15/18 đúng tổng, 2/3 lượt phát hiện mâu thuẫn. 15 đề đúng điểm giữ nguyên giới hạn rubric khi dựng prompt U09. Ba bài nộp đối chứng đạt 4/4, 3/4 và 3/4 đúng mục cần đổi điểm.
- [x] Thêm tên hai đầu cạnh cho U09, xem trước bắt buộc với đề tạo từ mẫu và cấu hình điểm từng câu. 229 test backend và 94 test frontend đạt. Luna giữ đúng điểm ở 17/17 phản hồi đọc được khi chốt điểm, một phản hồi có dữ liệu dư sau JSON. Ba đối chứng U09 giữ ID cũ và đạt điểm kỳ vọng.
- [x] Đo Luna medium khi để trống điểm từng câu. 14/18 đúng tổng và số câu theo brief, 2 sai tổng bị chặn và 2 phản hồi lỗi JSON. 12 test backend và 3 test frontend liên quan đạt.
- [x] Thêm số câu tùy chọn và xem trước khi model phát hiện mâu thuẫn với lời nhắc. 32 test backend, 96 test frontend và luồng Chrome với API giả lập đạt. Luna medium báo đúng cả hai ca 4/5 câu. Một ca đối chứng sai tổng bị chặn.
- [ ] Kiểm tra Gemini live qua gateway U06.

## U09: Siết cách chấm từng mục rubric · 2026-11-22

- [x] Tái hiện mất 1,50 điểm phân quyền ở cả ba lượt ca thiếu FK với prompt cũ dù bằng chứng sequence giữ nguyên.
- [x] Prompt đọc đúng phạm vi mục, nhánh sequence, mức điểm một phần và kiểm nhất quán giữa điểm với lý do. 57 test backend liên quan đạt.
- [x] Prompt chốt cho ca thiếu FK 9,25/10 và ca thực sự thiếu phân quyền 8,50/10. Ca lạc đề vẫn lệch 0,25 ở trigger.
- [x] Báo cáo cục bộ đổi tên thành `test-ai-grading-test.md`. Có cột riêng và lệnh đánh giá cùng bản ghi cho Opus 5.5.
- [x] Chạy Opus 5.5 trên 20 ca lượt 3 (15/20 đạt).
- [x] Lưu báo cáo kiểm thử thành một file [ai-grading-test-report.md](construction/build-and-test/ai-grading-test-report.md). Lưu 83 file bằng chứng lượt 3 trong `construction/build-and-test/2026-11-22/2026-11-22-u09-ai-grading-test-evidence/`. Bản ghi các lượt trước giữ trong `.tmp/` cục bộ.
- [ ] Kiểm độ ổn định rộng hơn với prompt chốt.

## U09: Luna chấm và Sol đánh giá · 2026-11-22

- [x] Codex judge GPT-6.2 Sol dùng reasoning effort `high`. Ghi cấu hình vào metadata. Kiểm thử Python có 18 ca đạt.
- [x] GPT-6 Luna `medium` trả 20 phản hồi hợp lệ. Sol `high` đánh giá đủ ba metric trên cả 20, với 15 ca đạt cả ba ngưỡng 0,80.
- [x] Gộp kết quả vào `U09-Luna-Sol-Report.md` trong thư mục chạy cục bộ. Bộ dữ liệu có nhãn tổng hợp của nhóm.
- [ ] Đánh giá có điểm giảng viên và đo độ ổn định nhiều lượt.

## U09: Bộ đề PE cho bảo vệ · 2026-11-22

- [x] Tạo 12 ca đối chứng SWR302 và SWD392 theo schema fixture hiện có. Giữ 8 ca badminton-booking.
- [x] Soạn 4 đề độc lập và phiếu trống cho 20 bài. Điểm giảng viên chờ thu.
- [x] Adapter Python đọc được cả 20 ca hồi quy. Java harness có 24 test đạt, Python có 17 test đạt. PowerShell replay đúng đề đạt và từ chối kết quả của đề khác.
- [ ] Chạy model trên các đề PE mới sau khi chốt dữ liệu. Chưa gọi nhà cung cấp trong đợt này.

[Dataset và hướng dẫn kiểm thử](construction/build-and-test/u09-pe-defense-dataset.md)

## U03 — RabbitMQ không đọc được JobMessage sau Spring Boot 3.5.16 — 2026-11-19

Nhánh `fix/amqp-trusted-packages` từ `staging@2433c57b`. Chưa commit.

- [x] Nguyên nhân: spring-amqp 3.2.12 chỉ tin `java.util`/`java.lang`; `Jackson2JsonMessageConverter` trong `jobs/config/AmqpMessageConfig.java` từ chối `edu.aiplatform.jobs.contract.JobMessage`, nên mọi job qua RabbitMQ (OTP, email, AI, Drive, backup) hỏng.
- [x] Sửa: khai báo trusted package `edu.aiplatform.jobs.contract` (chứa `JobMessage`, `DomainEvent`); không tin toàn bộ (`*`).
- [x] Test: `AmqpMessageConfigTest` (round-trip `JobMessage`, chặn lớp ngoài package); test này lỗi đúng thông báo production khi bỏ bản sửa. 116 test jobs/notifications liên quan pass.

## U06/U07/U10/U11 — áp dụng quy ước mã nguồn — 2026-11-18

Rà soát 15 unit theo [code-conventions](construction/code-conventions.md), sau đó sửa nhóm vi phạm rõ trên nhánh `staging`. Chưa commit.

- [x] Rà soát: không còn JPA; interface, dependency service, tên file và test đạt ở cả 15 unit.
- [x] U10/U11: bỏ tiền tố `Jdbc` của 4 repository và 3 integration test.
- [x] U06: `McpInput` dùng tên động từ (`readText`, `readUuid`, `requireTeaching`, `createInvalidArguments`…); xuống dòng các lớp MCP.
- [x] Contract: `IAiTask.loadTask`, `IAssignmentQuery.loadAssignment`, `IQuizQuery.loadQuiz` thay cho `get`; cập nhật caller, stub, test.
- [x] U07/U10: xuống dòng, tách method trong các lớp MCP; giữ tên tool, schema, chuỗi.
- [x] Build and Test: test-compile đạt; 593 test liên quan 0 lỗi. Chưa chạy `mvn verify` toàn bộ.
- [ ] Còn lại (vi phạm nhẹ, chưa sửa): method chỉ là danh từ ở U03/U07/U08/U10/U11, biến `data`/`temp` ở U03/U09, `SessionProvider.tsx` nằm trong `hooks/` (U01).

## U04/U13/U14 — chuyển JPA sang JDBC — 2026-11-18

Người dùng yêu cầu tạo nhánh mới từ staging để chuyển U04, U13 (module học liệu) và U14 từ JPA sang Spring JDBC. Nhánh `refactor/jdbc-u04-u13-u14` từ `staging@2e179598`. Ba commit mã cục bộ (`418a42ae` U13, `503cf463` U14, `a4bd6934` U04) và một commit tài liệu; chưa push.

- [x] Workspace Detection: brownfield, tiếp tục từ state hiện hành; tạo nhánh.
- [x] Requirements Analysis (Standard): [câu hỏi](inception/requirements/2026-11-18-u04-u13-u14-jdbc-questions.md) đã trả lời (Q1 chỉ sửa ba unit, giữ hạ tầng JPA chung; Q2–Q4 theo khuyến nghị); [yêu cầu JD-01…JD-09](inception/requirements/2026-11-18-u04-u13-u14-jdbc.md). Đã duyệt 2026-11-18.
- [x] Workflow Planning: chỉ Code Generation và Build and Test; thứ tự U13 → U14 → U04. Đã duyệt 2026-11-18.
- [x] Code Generation Part 1: [kế hoạch 6 bước](construction/plans/code-generation-plan/2026-11-18-u04-u13-u14-jdbc.md). Đã duyệt 2026-11-18.
- [x] Code Generation Part 2: U13 module, U14, U04; giữ hạ tầng JPA chung. Cập nhật tài liệu unit và [persistence-conventions](construction/persistence-conventions.md). Người dùng duyệt 2026-11-18T14:44:22Z.
- [x] Build and Test: `mvn -B verify` 2572 test, 0 fail, 0 error, 1 skipped. [Kết quả](construction/build-and-test/2026-11-18/2026-11-18-u04-u13-u14-jdbc-results.md). Thêm dòng vào [build-and-test-summary](construction/build-and-test/build-and-test-summary.md), sửa ghi chú JPA trong integration-test-instructions. Người dùng duyệt 2026-11-18T14:45:06Z.
- [x] Operations: placeholder, không có bước triển khai. Sau đó người dùng yêu cầu E2E và PR: E2E `u04-u13-u14` 40/40 trên stack `jdbc1118` với bản vá tạm cho lỗi RabbitMQ của staging `3ed52391` (không commit); mở PR vào staging, không merge, không deploy.

## Sửa ký pháp class diagram theo UML — 2026-11-18

Người dùng gửi trang hướng dẫn UML class diagram của draw.io, yêu cầu review rồi sửa `docs/class-diagrams.drawio`. Chỉ sửa sơ đồ và README; không đổi mã; chưa commit.

- [x] Review: 106 dependency, 12 realization, 3 association, 2 composition; interface gắn nhầm «service»; thiếu `IJobHandler`.
- [x] Sửa bằng script đối chiếu field Java (chỉ field cấp lớp): 83 dependency thành association có hướng, 2 thành aggregation (`AiTaskResultHandlerRegistry`, `McpToolRegistry`) có `1`/`0..*`; 21 giữ dependency (repository → entity, registry → tool cụ thể, `JobDispatcher` → `OtpMailHandler`); bỏ dependency trùng `Evaluation` → `EvaluationHistory`; `GroupDocument` ◆ `GroupSection` thành composition; 4 interface đổi sang «interface»; thêm `IJobHandler` và realization từ `OtpMailHandler`, `AiTaskJobHandler`. Giữ nguyên vị trí hộp và điểm gấp đường.
- [x] Nối lại đường cho gọn: định tuyến vuông góc ưu tiên đường thẳng rồi 1–2 lần gập, không đi xuyên hộp, giữ vị trí hộp; tổng số điểm gập giảm từ 404 xuống 72. Bỏ 6 dependency "dispatches" từ `McpToolRegistry` tới tool cụ thể vì mã chỉ gọi qua `IMcpToolProvider`. Trang Authentication còn 30 chỗ cắt nhau do 5 service dùng chung nhiều store.
- [x] Render 6 trang bằng draw.io CLI để kiểm; README ghi quy ước mới.
- [x] Làm thử bố cục theo cột trên trang Grading: hộp rộng cố định, chữ ký phương thức xuống dòng sau tham số, 3 cột Evaluation/Proposal/Practice; 10 điểm gập, 2 chỗ cắt.
- [x] Trang Grading đổi sang đường thẳng (nối chéo như ảnh mẫu): 26/26 đường là một đoạn thẳng, cách hộp khác ít nhất 12px, 3 chỗ cắt; dịch `IGradingProposalRepository`, `GradingProposalRepository`, `GradingProposal` sang phải để không bị `GradingProposalTransactions` chắn.
- [x] Trang Grading chỉ giữ class chính: bỏ `IEvaluationRepository`, `IGradingProposalRepository` (service nối thẳng repository), `IAiTask` thay bằng `AiTaskService`, bỏ `GradingBatchService`, `AttemptRepository`; còn 15 class, 19 đường thẳng, 3 chỗ cắt.
- [x] Trang Grading xếp theo tầng như ảnh mẫu: hàng controller, hàng service, hàng repository, hàng entity; 19 đường thẳng, 1 chỗ cắt; nhãn hai đầu đặt trên đường ngang, tên quan hệ ở giữa.
- [x] Trang Grading đổi sang nối vuông góc kiểu ảnh mẫu: hộp thẳng hàng nối thẳng; nhiều class cùng nối vào một class gộp chung thanh ngang rồi một đường vào đích (`EvaluationRepository`, `GradingProposalRepository`); `EvaluationController` tách nhánh từ một thân; 1 chỗ cắt.
- [x] Gỡ rối đoạn service → repository trang Grading: `EvaluationRepository` dưới `GradingService`/`GradingProposalTransactions` (gộp một thanh), `GradingProposalRepository` dưới `GradingProposalService` (đường thẳng), hai đường chéo cột là nét chéo thẳng vào cạnh bên repository (theo yêu cầu người dùng), 1 chỗ cắt.
- [x] Áp cho 5 trang còn lại: bỏ interface và lớp phụ, mỗi tầng một hàng, nối vuông góc gộp thanh. Authoring 1 chỗ cắt, Workspace 0, AI API 3, MCP 0, Authentication 19 (4 service dùng chung 5 store); Authentication giữ class có trong sequence diagram cộng `OtpService`.
- [x] Cắt gọn cả 6 trang theo tiêu chí chỉ giữ class có trong sequence của phần đó cộng entity chính: Authentication 10 class (4 chỗ cắt), Authoring 8 (0), Workspace 10 (0), Grading 13 (0), AI API 11 (3), MCP 9 (0).
- [x] Rà độ dễ đọc các đường nối trên ảnh draw.io của 6 trang. Authentication có 4 điểm giao ở các bus service → store, AI API có 2 vị trí giao giữa các bus vào registry và repository. Nhãn bội số sát khung ở Authoring và Workspace. Grading và MCP không có chỗ nối cần ưu tiên. Chỉ ghi nhận kết quả review, chưa chỉnh sơ đồ.
- [x] Chỉnh đường nối trang Authentication theo review: đưa bus SessionStore xuống dưới, căn thẳng các nhánh vào SessionStore và AccountRepository, đánh dấu ba nút nhập bus bằng chấm đen. Điểm giao giảm 4 → 3, cả ba có dấu nhảy rõ trên ảnh xuất draw.io. Đã kiểm tra đủ 10 class, 13 quan hệ và nội dung năm trang còn lại khớp nguyên văn.
- [x] Đối chiếu UML 2.5.1, hướng dẫn draw.io và mã Java cho trang Authentication. Xác nhận 12 association có hướng từ field được giữ và 1 dependency AccountRepository → Account. Thêm 13 nhãn động từ, bội số 0..* ở đầu lớp tham chiếu và 1 ở đầu đối tượng được giữ; hiện bội số một lần ở mỗi đầu bus dùng chung. Đã kiểm ảnh xuất, tuyến nối và năm trang còn lại giữ nguyên.
- [x] Rà 56 quan hệ của 5 trang còn lại theo Java, Spring và migration. Thêm nhãn và bội số ở hai đầu association, aggregation và composition. Authoring: Assignment → AssignmentRubric đổi sang composition 1 → 0..*, dời nhãn khỏi viền. Workspace: GroupDocument → GroupSection sửa bội số 1..* thành 0..* và dời nhãn. Grading: giữ Evaluation–GradingProposal 1 → 0..*, giãn khoảng nối Practice Feedback. AI API: đổi hai đầu xuất phát của AiTaskService, giảm 2 → 1 điểm giao nhìn thấy và tăng dấu nhảy. Hai registry dùng aggregation tới các bean cụ thể. Đã kiểm đủ 5 ảnh render và xác nhận Authentication khớp nguyên văn.
- [x] Căn 69 nhãn quan hệ theo mẫu trên cả 6 trang. 40 nhãn nằm giữa nét dọc, 29 nhãn đặt phía trên đoạn ngang. Đã kiểm bằng ảnh draw.io và đối chiếu xác nhận chỉ đổi vị trí nhãn quan hệ.
- [x] Đối chiếu tên 154 mục phương thức trên 6 trang với khai báo Java hiện tại. 120 trùng tên, 34 mục lệch ở 21 lớp Java. Số mục lệch theo trang là Authentication 5, Authoring 10, Workspace 9, Grading 2, AI API 5 và MCP 3. Bảng tên thay thế có dẫn dòng mã trong audit.md. Chưa sửa sơ đồ hay mã nguồn.
- [x] Cập nhật đủ 34 tên phương thức trên 6 trang và bổ sung pwdStamp vào SessionStore.createSession. Đối chiếu lại 154/154 tên với Java. Chỉ thay nội dung ô phương thức, xuống dòng ba chỗ cần chỗ trống. Đã xem 6 ảnh xuất và xác nhận kích thước, vị trí hộp và đường nối giữ nguyên.
- [ ] Quyết định bỏ lifeline `RequestThrottle` trong sequence Login cho khớp class diagram.
- [x] Xuất 6 ảnh class diagram và 1 ảnh kiến trúc từ draw.io. Thay đúng 7 hình trong bản Word hiện tại, kiểm 47 trang bằng Word và đối chiếu bản tải lại từ Drive khớp byte với bản đã kiểm. Giữ nguyên file ID và link.
- [x] Cập nhật quy ước sơ đồ lớp trong skill comet-uml-drawio của Codex và Claude. Hai bản đồng nhất và qua quick_validate.py.

## Đồng bộ sequence diagram với mã nguồn — 2026-11-18

- [x] Rà 15 trang theo Java. Sửa các lời gọi chính của Authentication, Authoring, Workspace và Grading. Bổ sung `attachTask`, `insertRevision` và `prepareProposal`. Ghi `release` ở phần luồng thay thế của bài nhóm vì thao tác này xóa bản nháp. Luồng kích hoạt và đặt lại mật khẩu chỉ lấy vé sau khi mật khẩu đạt yêu cầu.
- [x] Bỏ 14 lifeline Database và 39 cặp gọi/trả SQL khỏi draw.io. Repo hoặc store kết thúc luồng lưu trữ. Giữ Redis ở Login và Logout. Đồng bộ JSON và README.
- [x] Kiểm lớp đích, tên và số tham số của 153 lời gọi Java. Bộ kiểm UML báo 0 lỗi, 0 cảnh báo. Đã xem ảnh xuất draw.io của 15 trang.
- [x] Xuất 15 ảnh PNG từ sequence-diagrams.drawio, thay hình trong bản Word Report 4 trên Drive và giữ link hiện có. Hình chấm điểm MCP cùng trang với tiêu đề, báo cáo còn 46 trang và số trang trong mục lục đã đồng bộ. Bản tải lại khớp SHA-256.

- [x] Thêm 234 thanh kích hoạt nền trắng viền đen trên 15 sequence diagram. Đầu mũi tên chạm cạnh thanh, các nhãn và thứ tự message giữ nguyên. Nới một số cột để nhãn không chạm thanh. Đã kiểm đủ 15 ảnh xuất draw.io.

- [x] Sửa 4 điểm ký pháp và luồng Java trên 7 sequence diagram. Login chỉ tra email khi IP được phép. Ba trang AI API vẽ job RabbitMQ nhận bất đồng bộ, External MCP sửa guard tiếp tục lặp, hai trang OTP đổi ref sang ghi chú. Giữ thanh kích hoạt trắng viền đen, đồng bộ JSON và kiểm 7 ảnh xuất.

- [x] Hiện lifeline RabbitMQ trên ba trang AI API. Nối trực tiếp thông điệp bất đồng bộ AI_TASK vào thanh kích hoạt của AiTaskJobHandler. Đồng bộ JSON, giữ thanh nền trắng và kiểm ba ảnh xuất.

- [x] Xuất lại 15 ảnh sequence có thanh kích hoạt trắng, gồm ba luồng RabbitMQ chỉ vẽ thông điệp AI_TASK. Thay vào bản Word Report 4 trên Drive, giữ cùng ID và link. Đối chiếu 46 trang, 31 trang ngoài phạm vi khớp từng pixel và bản tải lại khớp SHA-256.

## Soát Report 4 và viết lại mô tả kiến trúc — 2026-11-18

Người dùng yêu cầu kiểm toàn bộ Report 4 (file Word trên Drive), viết lại mô tả Software Architecture, rồi kiểm thêm phần III và IV. Chỉ thêm tài liệu đề xuất; không sửa mã, không sửa file Word; chưa commit.

- [x] Đọc phần chữ của Report 4 và đối chiếu với mã, migration Flyway, `application.yml`.
- [x] `docs/report4-diagram-specs/report4-revisions-2026-11-18.md`: văn bản mới cho I.1 (đoạn mô tả và bảng thành phần theo sơ đồ đã vẽ lại); bảng chỗ cần sửa ở III/IV kèm câu thay và bằng chứng.
- [x] `docs/report4-diagram-specs/report4-sections-III-IV-2026-11-18.md`: nội dung III và IV viết lại ngắn gọn, giữ đúng tiêu đề và cột bảng của report, đã sửa các chỗ lệch mã.
- [x] Áp III/IV vào file Word và thêm dòng Change Log 18/11/26; tải lên Drive làm phiên bản mới cùng link (2026-11-18 07:06Z, 4.683.131 byte, khớp bản local).
- [ ] Dán phần mô tả I.1 được đề xuất vào Word khi được yêu cầu.
- [x] Thay hình kiến trúc trong Report 4 bằng bản draw.io hiện tại.
- [ ] Viết lại I.3 Database Design từ migration (12/24 bảng không tồn tại, các bảng còn lại sai cột).
- [ ] Kiểm hình class/sequence trong Word (bản xuất chữ không có ảnh).

## Vẽ lại sơ đồ kiến trúc tổng thể theo mã nguồn — 2026-11-17

Người dùng yêu cầu kiểm `docs/high-level-software-architecture.drawio` so với mã: thiếu đường trả về (quy ước nét liền là request, nét đứt là response), API Client dùng thư viện gì, vì sao Worker trỏ thẳng Repository. Chỉ sửa sơ đồ và tài liệu; không đổi mã; chưa commit.

- [x] Đối chiếu mã: API Client là wrapper `fetch` (`frontend/src/shared/api/client.ts`, CSRF, refresh 401) cùng `EventSource` cho SSE, không dùng axios; Worker (`JobListener`, profile `worker`) gọi `*JobHandler` nằm ở tầng service, handler mới gọi repository và adapter; chuỗi bảo mật `/mcp` riêng, Bearer kiểm trong `McpController`; Drive, mail, AI, Redis, RabbitMQ đi qua các gói `integration`.
- [x] Vẽ lại: mỗi lời gọi đồng bộ có cặp request/response; thêm Integration Adapters, Worker đi qua Service Layer; publish sau commit và RabbitMQ giao job là thông điệp một chiều; thêm chú giải; render kiểm bằng draw.io CLI, không có nhãn đè đường.
- [x] `docs/report4-diagram-specs/README.md`: ghi quy ước và bố cục mới.
- [x] Thêm message ngắn cạnh mọi đường nối (theo `JobListener`: xong thì ack, lỗi thì nack và đưa lại hàng đợi); khung External Services rộng thêm để nhãn không đè viền; generator tự kiểm nhãn không đè đường, không đè nhau.
- [ ] Xuất lại ảnh sơ đồ trong tệp Word Report 4 (ngoài repo).
- [ ] Người dùng duyệt sơ đồ và quyết định commit.

## Đồng bộ tài liệu AI-DLC với mã nguồn — 2026-11-17

Người dùng yêu cầu kiểm tra đồng bộ tài liệu AI-DLC với mã trên `staging@706e46f1`, rồi yêu cầu sửa tài liệu. Chỉ sửa Markdown; không đổi mã, test, schema hay cấu hình; chưa commit.

- [x] Kiểm tra bằng script: api-inventory khớp 347/347 mapping của 50 controller; đường dẫn trong tài liệu hiện hành còn tồn tại; migration mới nhất `v20261116.1010` đã ghi.
- [x] U11: `attemptNo`, số lượt và lượt `IN_PROGRESS` tính theo (Quiz, người học, lớp) như migration `V20261113_1000__scope_quiz_attempts_to_class.sql` (domain-entities, BR-U11-10, code-summary).
- [x] current-code-baseline: thêm migration trên và 21 lớp test còn thiếu, delta 2026-11-17.
- [x] code-summary U02, U03, U08, U12: bỏ component đã xóa (`AuditDetailDialog`, `SettingGroupTable`, `DelegatedSectionCard`, `ReceiptPage`, `ClassAssignmentsCard`, `ClassCardAssignments`, `GroupEditor`, `GroupProgressView`, `GroupCountCard`, `SectionEditorDialog`), ghi component hiện có.
- [x] U03: P16, P17, logical-components và BR-U03-93 nêu khóa backup tùy chọn.
- [x] U14: chuông thông báo chỉ hỏi lại số chưa đọc khi mất SSE và tab đang hiện.
- [ ] Người dùng duyệt tài liệu và quyết định commit.

Không sửa: chú thích `frontend/src/i18n/LocaleGate.tsx` (mã nguồn); các báo cáo, kế hoạch có ngày và bản tóm tắt remediation U04/U13/U14 trỏ tới tệp đã xóa (tài liệu lịch sử). Kiểm tra dựa trên tên, đường dẫn và endpoint; chưa đọc lại toàn bộ business rule của 15 unit.

## Script deploy tự khởi động lại nginx khi cấu hình nginx đổi — 2026-11-16

Phát hiện khi đưa PR 101 lên staging: `platform-deploy` không tạo lại container nginx, nên thay đổi trong `infra/nginx` không được nạp và `nginx -t` trên container cũ báo `unknown log format "timed"`; phải restart tay. Người dùng đồng ý sửa ("Có, sửa đi"). Nhánh `fix/deploy-restart-nginx` từ `origin/staging@e2014177`, worktree riêng.

- [x] Requirements Analysis (Minimal) và Workflow Planning: một thay đổi trong script deploy, chỉ Code Generation và Build and Test.
- [x] Code Generation: `switch_to` so `infra/nginx` giữa bản đang chạy và bản mới, khác thì `compose restart nginx` sau `compose up`; áp cho cả lúc quay về. Thêm test, cập nhật runbook mục 10.
- [x] Build and Test: `test-platform-deploy.sh` 16/16 (chạy trong thư mục tạm trên VPS vì máy phát triển không có `jq`, `flock`). Chưa chạy với Docker và nginx thật.
- [x] Người dùng yêu cầu commit cục bộ (2026-11-16).
- [x] Người dùng yêu cầu push và mở PR vào staging (2026-11-16). Không tự merge.
- [x] PR 102 đã merge vào staging (`706e46f1`, 2026-11-17).

Sau khi bản này lên VPS phải cài lại `/usr/local/bin/platform-deploy` và `/usr/local/bin/platform-staging-deploy` một lần nữa. Security/Resiliency trong phạm vi đã duyệt; PBT tắt.

## Khắc phục báo cáo phi chức năng — 2026-11-17

Người dùng yêu cầu kiểm tra NFR, sau đó “sửa toàn bộ cho tôi” và “tiếp tục”. [Báo cáo gốc](construction/build-and-test/2026-11-16/2026-11-16-non-functional-review.md), [kế hoạch](construction/plans/code-generation-plan/2026-11-16-nfr-remediation.md), [kết quả kiểm chứng](construction/build-and-test/2026-11-17/2026-11-17-nfr-remediation-results.md).

- [x] Workspace/Requirements/Planning: brownfield, dùng lại yêu cầu và thiết kế; phạm vi F01–F08 được người dùng yêu cầu thực hiện. Không thêm use case/API/schema.
- [x] Code Generation: 9/9 bước kế hoạch hoàn tất; health Drive không chặn, DB timeout, giảm tranh khóa Quiz, giới hạn worker, restore deadline/cleanup, image digest pins và đồng bộ tài liệu key/RPO.
- [x] Build and Test: backend 2.570 tests, 0 failures/errors, 1 skip AI evaluation; guard 7 suite PostgreSQL và 58 ca truy vết qua; frontend liên quan 66/66; deploy harness 15/15; restore harness 4/4; ShellCheck/image pins/Compose config qua. Build Dockerfile backend và restore fixture bằng image đó trên PostgreSQL thật qua.
- [x] Review: giữ nguyên công việc performance/security đồng thời; kiểm diff và tài liệu. Security/Resiliency tuân thủ trong phạm vi đã duyệt, PBT tắt.

Đóng 8/8 lỗi đã báo ở mã nguồn/cấu hình/tài liệu. Chưa nghiệm thu p95 tại tải mục tiêu, RPO/RTO dữ liệu production, accessibility toàn hành trình hoặc soak/chaos dài hạn. Chưa deploy/commit/push phần NFR trong lượt này; không truy cập hay thay dữ liệu production.

Kiểm tiếp theo yêu cầu đưa lên staging: [readiness 17/11](construction/build-and-test/2026-11-17/2026-11-17-nfr-staging-readiness.md). Có thể chuẩn bị PR; local chậm staging 4 commit, mô phỏng gộp có hai xung đột tài liệu state/audit, mã nguồn không xung đột. CI staging `706e46f1` xanh cả 4 job; deploy harness mới 16/16 qua; frontend Docker build và smoke 384 MiB/0,5 CPU qua. Cần đồng bộ tài liệu, commit và CI PR trước merge; chưa thao tác phát hành.

Người dùng đã yêu cầu đồng bộ, commit, mở PR và chờ CI ngày 17/11. Đã tạo nhánh `codex/nfr-remediation` trên staging `706e46f1`, gộp đủ nội dung state/audit, giữ mã NFR đã kiểm thử. Đang thực hiện commit/PR và theo dõi CI theo [kế hoạch](construction/plans/code-generation-plan/2026-11-16-nfr-remediation.md); chưa được yêu cầu merge/deploy.

## Sửa các điểm trong báo cáo hiệu năng production — 2026-11-16

Người dùng yêu cầu kiểm tra hiệu năng production, rồi yêu cầu "sửa toàn bộ". [Báo cáo](construction/build-and-test/2026-11-16/2026-11-16-performance-report.md); [kế hoạch](construction/plans/code-generation-plan/2026-11-16-performance-fixes.md).

- [x] Workspace Detection: brownfield, dùng lại tài liệu reverse engineering.
- [x] Requirements Analysis (Minimal): phạm vi là mục 6.1–6.5 của báo cáo.
- [x] Workflow Planning: chỉ Code Generation và Build and Test; các giai đoạn khác bỏ qua vì không có thành phần, API hay schema mới.
- [x] Người dùng duyệt kế hoạch, chọn phương án deploy A.
- [x] Code Generation: 13/13 bước. Bước 7 chỉ áp cho 502/503; Redis `maxmemory` 96 MB (staging giới hạn 128 MB).
- [x] Build and Test: [kết quả](construction/build-and-test/2026-11-16/2026-11-16-performance-fixes-results.md). Frontend ESLint, TypeScript, Vitest 1.160/1.160, production build qua; test script deploy 15/15; nginx, Postgres, Redis kiểm trong container tạm.
- [x] Người dùng yêu cầu commit cục bộ (2026-11-16).
- [x] Người dùng yêu cầu mở PR (2026-11-16): push nhánh, PR vào staging. Không tự merge.

Chưa kiểm: `docker compose config` và cả stack chạy cùng nhau (Docker ở máy không chạy); template nginx của production và staging (cần chứng chỉ); thời gian khởi động backend với 2 CPU; các trang sau đăng nhập trên trình duyệt. Phát hiện mới ngoài phạm vi: `src/i18n/msg.ts` đóng gói cả ba ngôn ngữ (643 KB) vào mọi trang. Security/Resiliency trong phạm vi đã duyệt; PBT tắt.

## Kiểm tra staging trước khi lên main và đồng bộ tài liệu vận hành — 2026-11-16

Người dùng yêu cầu kiểm tra `staging@d8ac7d21` trước khi đưa lên main, rồi yêu cầu sửa tài liệu. Nhánh `docs/sync-ops-docs-staging` từ `origin/staging`; chỉ sửa tài liệu, không đổi mã, test, schema hay cấu hình.

- [x] Kiểm tra: CI của `d8ac7d21` qua cả bốn job; staging trên VPS chạy `abca8520` (khác HEAD ở hai script e2e và audit.md), mọi dịch vụ khỏe, 6 giờ không có dòng ERROR ở backend. Không chạy lại test ở máy.
- [x] Tài liệu: runbook deploy-vps mục 6 (Drive cá nhân qua OAuth, thứ tự chọn nơi lưu, khóa backup tùy chọn), restore-isolated (archive không mã hóa, thư mục backup của Drive cá nhân), deploy-staging, README, requirements (FR-006, FR-013, FR-038, NFR-002), code-summary của U03, current-code-baseline (delta 2026-11-16).
- [ ] Trước khi gộp vào main, người vận hành kiểm `.env` production: mã mới dùng Drive khi có biến `GOOGLE_DRIVE_OAUTH_*`, kể cả khi `U03_REQUIRE_DRIVE=false`; tệp production đang ở đĩa VPS không tự chuyển sang Drive; production và staging cần `GOOGLE_DRIVE_FOLDER_NAME` khác nhau. Phiên này không đọc được `.env` production nên chưa xác nhận.
- [x] Người dùng duyệt tài liệu và quyết định commit, push, mở PR vào staging: commit `d19f0329`, đã có trên staging.

`nfr-design-patterns.md` và `logical-components.md` của U03 đã sửa ngày 2026-11-17. Chưa cập nhật: chú thích trong `frontend/src/i18n/LocaleGate.tsx` còn ghi máy chủ dựng chữ bằng tiếng Việt trong khi mặc định là tiếng Anh (mã nguồn, ngoài phạm vi lượt này).

## Sửa các điểm rà soát toàn dự án trên nền staging — 2026-11-16

Người dùng yêu cầu rà soát toàn dự án (2026-11-15), rồi chọn phương án 1: chỉ mang lên staging những điểm staging còn thiếu. Nhánh `chi-cuongg/audit-project-bugs-docs` trên `origin/staging@f1c351dd`, đã gộp `origin/staging@5f9c0173`. [Kế hoạch và phạm vi](construction/plans/code-generation-plan/2026-11-16-review-fixes-on-staging.md); [kết quả và giới hạn](construction/build-and-test/2026-11-16/2026-11-16-review-fixes-on-staging-results.md).

- [x] Workspace Detection: brownfield, dùng lại tài liệu reverse engineering.
- [x] Requirements Analysis (Minimal): phạm vi là các điểm của báo cáo rà soát còn tồn tại trên staging.
- [x] Workflow Planning: chỉ Code Generation và Build and Test; các giai đoạn khác bỏ qua vì không có thành phần hay quy tắc nghiệp vụ mới.
- [x] Code Generation: 7/7 bước, gồm bước gộp lại staging. Quét bù nhắc hạn 24 giờ (U14); migration bỏ trần 10 của số thứ tự lượt và bỏ bảng `group_peer_reviews`; frontend bỏ `GRADE_LOCKED` và nhãn đánh giá chéo; OpenAPI grading và tài liệu đồng bộ.
- [x] Build and Test trên bản đã gộp: backend `mvn -B verify` có Docker 2.546 test, 0 thất bại, 0 lỗi, 1 bỏ qua; bộ kiểm bắt buộc của CI qua; migration áp trên PostgreSQL thật tới `v20261116.1010`; frontend ESLint, TypeScript, Vitest 1.159/1.159 và production build qua.
- [x] Người dùng duyệt mã và quyết định push, mở PR vào staging: PR #97 đã gộp vào staging (`abca8520`, 2026-11-16).

Bản sửa đầu trên `3b86e57d` có thêm chặn nộp lại khi đã có điểm công bố, trạng thái `CANCELLED` của lượt làm và hộp xác nhận công bố. Các phần đó không mang lên vì staging đã có quy tắc chỉ chấm khi bài đã đóng (`fe492df5`); bản đầy đủ giữ ở nhánh cục bộ `backup/review-fixes-full-2026-11-15`. Chưa kiểm trên trình duyệt, chưa push hay deploy. Security/Resiliency giữ phạm vi đã duyệt, PBT tắt.

## Rà soát MSSV trên danh sách và bộ chọn sinh viên — 2026-11-16

- [x] Đối chiếu yêu cầu SC-10/SC-11, component hiện có và đường truyền `studentCode`.
- [x] Xác minh 8 nhóm vị trí thiếu mã; lập [kết quả/phạm vi](inception/requirements/2026-11-16-student-code-ui-completeness.md).
- [x] Người dùng quyết định [duyệt phạm vi hay chỉ rà soát](inception/requirements/2026-11-16-student-code-ui-completeness-questions.md).
- [x] Sau khi duyệt: lập kế hoạch Code Generation, sửa và kiểm thử.

Người dùng chọn “Duyệt phạm vi, tiếp tục sửa và kiểm thử”. Đã sửa đủ 8 nhóm, thêm hai trường DTO nullable và nhãn dùng chung. [Kế hoạch/kết quả](construction/plans/code-generation-plan/2026-11-16-student-code-ui-completeness.md): frontend 338/338, backend groups/groupdocs 141/141; ESLint, TypeScript và diff check đạt; review độc lập không có finding. Backend đã restart, `/health` UP; frontend `/login` 200. Đã kiểm tra browser đăng nhập giảng viên/sinh viên rộng/hẹp: [kết quả UI](construction/build-and-test/2026-11-16/2026-11-16-student-code-ui-browser-results.md). Lượt UI đầu MSSV local đều null, ca có mã dùng fixture GET đã gỡ. Theo yêu cầu tiếp theo, đã backfill 6 sinh viên seed local bằng HE190001–HE190006, kiểm tra SQL/unique/idempotence đạt; API và browser Nhóm của tôi hiển thị 5 mã thật của lớp, không có fixture. Giao mục, duyệt đề xuất/lời mời và chi tiết quiz còn thiếu dữ liệu/trạng thái phù hợp, không tính browser PASS. Có thông báo CSP inline style trên quiz chưa xác định nguồn. Chưa chạy production build/module ngoài phạm vi. Giữ nguyên ba báo cáo AUDIT chưa theo dõi; không commit/push. Security/Resiliency compliant trong phạm vi; PBT disabled.


Theo ảnh phản hồi tiếp theo, đã tách cột MSSV/Email và cân chữ/độ rộng trong bảng Sinh viên (ClassGroupsPanel). Groups 50/50 test, ESLint, TypeScript đạt; browser dữ liệu thật ở 1920/1440/390px xác minh 6 cột, tìm kiếm, empty colspan và link ID. Màn hẹp cuộn trong bảng, không tràn trang. Xem phần bổ sung cuối báo cáo browser. Không đổi backend/database/commit/push.

Người dùng tiếp tục yêu cầu “giúp tôi commit”: cho phép commit local phần source, test và tài liệu MSSV trong phiên; ba báo cáo AUDIT gốc vẫn không đưa vào Git, không push. Backfill HE19 ở DB local và ảnh/script dưới .tmp không thuộc nội dung commit.

Người dùng yêu cầu “tạo pr lên staging”: cho phép publish nhánh audit-staging và tạo PR mới vào staging cho phần MSSV; PR audit trước đã được merge. Không tự merge PR. Mô tả PR nêu kết quả kiểm thử và các ca browser còn thiếu dữ liệu; backfill DB local không thuộc PR.

## Audit staging và sửa lỗi quan trọng (AUD-001..008) — 2026-11-16

User yêu cầu audit nhánh staging, tạo nhánh mới để audit/sửa lỗi, chưa commit/push.
Brownfield, nhánh `audit-staging` từ staging@f1c351dd, worktree riêng của Orca. Đã commit cục bộ thành 8 commit (không push).
Điều phối theo ORCA MASTER WORKFLOW v2.1: Sonnet điều phối, Codex GPT-6.1 Sol làm Developer
và Tester độc lập (4 vòng QA, worktree QA riêng đã xóa), Opus 5.5 High quyết định thiết kế
và duyệt cuối. Ghi chú làm việc `AUDIT-FINDINGS.md`, `AUDIT-DEVELOPER-REPORT.md`,
`AUDIT-006-007-REPORT.md` chỉ nằm ở thư mục gốc worktree, chưa theo dõi và không đưa vào commit
(`AUDIT-DEVELOPER-REPORT.md` đã cũ: AUD-004 sau đó có thêm bản nháp sessionStorage). Báo cáo QA
và báo cáo Opus lưu ngoài repo, trên máy của user.

- [x] AUD-001/002 (high): race session/đổi mật khẩu và refresh. Dấu mốc mật khẩu trên session,
  thao tác Lua nguyên tử, thử lại có giới hạn khi refresh/restamp. QA vòng 2 FAIL (2 bug
  interleave + JSON null), sửa, QA vòng 4 PASS (110 test, gồm Redis 7.4 thật và rollback
  PostgreSQL). Opus duyệt cuối: APPROVE WITH CONDITIONS.
- [x] AUD-003 (high): giới hạn 100 trang và ngân sách giải nén chung cho Draw.io. QA PASS.
- [x] AUD-004 (high): giữ bản nháp khi rời trang lúc lưu lỗi (sessionStorage). QA vòng 1 FAIL,
  sửa, QA vòng 2/4 PASS.
- [x] AUD-005 (high): restore-isolated.sh sai main class, dọn khóa/dump khi lỗi. QA PASS
  (chưa restore bằng image thật).
- [x] AUD-006 (medium): khóa cấp quiz cho sửa/bắt đầu/retire. QA PASS, kể cả PostgreSQL thật.
- [x] AUD-007 (medium): gộp tham chiếu ảnh nhóm sau khi lưu. QA PASS.
- [x] AUD-008 (medium): task AI CANCELLED vào danh sách phục hồi callback. QA PASS.
- [x] Opus 5.5 High kiểm chứng toàn bộ (2026-11-16): backend `mvn test` BUILD SUCCESS, 2539
  test, 0 lỗi, 1 bỏ qua (GradingAiEvaluationTest cần ai.eval.mode); frontend tsc, eslint,
  vitest 156 file/1147 test đều qua; `next build` qua trên ổ F: (trên ổ C: lỗi do liên kết
  node_modules khác ổ, không phải mã); test restore qua và fail trên bản gốc như mong đợi.
  Kết luận READY WITH CONDITIONS, không có lỗi chặn commit.
- [x] Opus sửa mục review (1): lưu lúc rời trang ghi vào `Map<attemptId, Promise>` cấp module, mở
  lại cùng attempt chờ lần lưu đó xong rồi mới tải, nên lần lưu sau dùng phiên bản mới. Test vitest
  "waits for a pending departure save…" đỏ trước (gửi version 1), xanh sau; `workspace-ui` +
  `assignmentwork` 18/18 qua.
- [x] Opus sửa mục review (2): CI job `deploy-script` shellcheck thêm `restore-isolated.sh`,
  `test-restore-isolated.sh` và chạy `sh infra/scripts/test-restore-isolated.sh`; YAML parse được,
  test restore chạy cục bộ qua. shellcheck không có trên máy: NOT TESTED, CI sẽ chạy lần đầu.
- [x] Opus sửa mục review (4): bỏ tiền tố `qa` ở 3 test `SessionStoreIntegrationTest`;
  `mvn test -Dtest=SessionStoreIntegrationTest` 24/24 qua (Redis thật). Thêm: tsc, eslint file
  đã sửa đều qua.
- [ ] Còn mở (nợ đã ghi nhận): (3) lock bắt đầu attempt (`for update` trên dòng quiz) làm các lượt
  bắt đầu cùng quiz xếp hàng; có thể đổi sang `for share` cho `lockStart`. Không đổi trong đợt này.
- [ ] NOT TESTED: trình duyệt thật cho AUD-004/007, restore bằng image thật, shellcheck,
  deploy nhiều instance/lệch phiên bản, Redis sập/cluster, tải, tài khoản ACTIVE có
  `password_changed_at` null (rơi về luật `issuedAt`, không chống race AUD-001).
- [x] Đã commit cục bộ 8 commit theo nhóm (identity, Draw.io, assignmentwork, infra, quiz, groupdocs, AI, docs), không dòng đồng tác giả Claude theo yêu cầu. [x] Đã push `audit-staging` và mở [PR #92](https://github.com/hygef-v4/AI-Powered-Learning-Platform/pull/92) vào staging (8 commit, MERGEABLE); không merge/deploy. [ ] CI và merge do người dùng quyết định. Điều kiện: không commit `AUDIT-*.md` gốc, stage theo
  từng đường dẫn, deploy mọi backend cùng lúc, giữ Redis một node và ≥ 6.0. Gợi ý chia 8
  commit: identity, Draw.io, assignmentwork, infra restore, quiz, groupdocs, AI, tài liệu.

Quy tắc Security/Resiliency tuân thủ trong phạm vi; PBT disabled. Không sửa dữ liệu thật,
không commit/push/merge/deploy.

## Ngày tháng, kiểm API key và mở chấm sau đóng bài — 2026-11-15

User duyệt đề xuất audit bằng “ok giúp tôi sửa theo đề xuất $ponytail:ponytail”.
Brownfield, nhánh `fix/date-ai-grading-window` từ staging@7d0905d4; giữ audit WIP trước đó.
[Yêu cầu](inception/requirements/2026-11-15-date-ai-grading-window.md),
[kế hoạch](construction/plans/code-generation-plan/2026-11-15-date-ai-grading-window.md),
[kết quả](construction/build-and-test/2026-11-15/2026-11-15-date-ai-grading-window.md).

- [x] DateInput dd/MM/yyyy chung; ISO/giờ Việt Nam giữ nguyên, không dependency mới.
- [x] Probe AI đủ budget, typed output-limit không báo đỏ; giữ kiểm phản hồi tạo/chấm.
- [x] Guard backend manual/AI/member/finalize/publish/batch/MCP và UI chỉ đọc trước đóng.
- [x] Đóng sớm có xác nhận/quyền/version/audit; tự đóng sau hạn cuối +30 giây. Chốt
  bài cá nhân/nhóm đã lưu cùng transaction, khóa đúng thứ tự và rollback khi lỗi.
- [x] Frontend 1135/1135 và final academics37/37, lint/isolated typecheck/build qua;
  config gốc khôi phục đúng byte. Backend scoped tests và final integration282/282,
  5 required PostgreSQL suites/58 traced cases qua. Business rules/API/manifest cập nhật.
- [ ] Nghiệm thu browser/key thật, tải lớn và deploy ngoài phạm vi lần thực hiện này.

Security/Resiliency tuân thủ trong phạm vi thay đổi, PBT disabled. Không sửa dữ liệu
học tập thật, restart backend/worker, commit/push/merge/deploy. Cần dùng backend và
worker mã mới trước nghiệm thu; giữ cache Next cũ, chỉ kiểm build bằng thư mục riêng.

### Bàn giao commit cục bộ — ngày tháng/AI/đóng nhận bài

Người dùng yêu cầu “giúp tôi commit”. Chia thay đổi thành ba commit chức năng
(DateInput, AI probe, grading/closure) và một commit tài liệu/kiểm chứng. Mã nguồn
không đổi sau các kiểm thử đã ghi; diff whitespace và guard 5 suite PostgreSQL/58
kịch bản kiểm lại trước commit đều qua. Không chạy lại toàn bộ test/build. Chưa
push, merge, deploy hay gọi provider thật; giới hạn nghiệm thu giữ như báo cáo.

### Bàn giao PR ngày tháng/AI/đóng nhận bài vào staging

User yêu cầu “giúp tôi tạo pr lên staging”. Nhánh `fix/date-ai-grading-window` đã có
merge staging, không thiếu commit origin/staging tại thời điểm chuẩn bị PR. Kiểm
sau merge: HttpProviderGateway36, AiConnection11, GradingService38 qua (85 tổng),
diff whitespace sạch. Bàn giao bằng push nhánh và PR base staging; không merge hay
auto-merge. Các kết quả toàn frontend/integration/build ở trên thuộc đợt trước merge;
CI cần kiểm toàn bộ head PR. Không nghiệm thu key thật/browser hoặc tải lớn.

- [x] Push nhánh và mở [PR #91](https://github.com/hygef-v4/AI-Powered-Learning-Platform/pull/91)
  vào staging. GitHub connector bị 403 khi tạo PR; GitHub CLI đã đăng nhập tạo thành
  công. Không merge hoặc bật auto-merge; trạng thái CI xem trên PR.

## Đồng bộ tài liệu theo staging — 2026-11-15

Đối chiếu `staging@7e0e8375` theo các điểm trong báo cáo của người dùng. Phạm vi là tài liệu AI-DLC.

- [x] Cập nhật yêu cầu, use case, story và thiết kế liên quan đến công bố/sửa điểm, đề mẫu cấp môn, nộp lại và lịch sử, API key chỉ ghi, phát hành Quiz.
- [x] Ghi quyết định khi nhóm nộp lại tại BR-U09-50 và BR-U12-42. Giữ điểm và mức đã công bố cùng biên nhận đã chấm, kể cả dữ liệu FINALIZED cũ. Chỉ bản chưa chính thức được đặt lại theo BR-U09-50.
- [x] Bổ sung [truy vết](inception/reverse-engineering/doc-code-traceability.md), [bảng kê source](inception/reverse-engineering/current-code-baseline.md) và [bằng chứng kiểm thử](construction/build-and-test/build-and-test-summary.md).
- [x] Đối chiếu tĩnh 66 use case, 50 controller/346 mapping, 43 migration và 5 suite PostgreSQL bắt buộc. Kiểm tra liên kết mới và diff Markdown. Kết quả test ứng dụng giữ phạm vi từng lần chạy đã ghi.

## U09 — giữ bản nháp khi refresh lỗi, 2026-11-15

Sửa P2 đã xác nhận trong review PR #87 theo yêu cầu người dùng, tiếp tục thiết kế
chấm cá nhân cạnh bài nhóm đã duyệt. Nhánh `fix/grading-member-drafts`.
[Kế hoạch](construction/plans/code-generation-plan/2026-11-15-member-draft-refresh-fix.md),
[kết quả](construction/build-and-test/2026-11-15/2026-11-15-member-draft-refresh-fix.md).

- [x] Phạm vi/diagnosis: form bị unmount do `members: []` sau save nhóm/refresh lỗi.
- [x] Code Generation: test RED rồi giữ members và khóa `editable` trong hook.
- [x] Kiểm chứng: 306/306 test trong 33 file liên quan qua; ESLint sạch; typecheck
  src/tests qua bằng config tạm loại cache Next cũ. Typecheck mặc định còn lỗi cache.
- [x] Tự review và tài liệu. Security/Resiliency giữ phạm vi đã duyệt; PBT tắt.

Không đổi backend/schema. Commit cục bộ theo yêu cầu tiếp theo; chưa push/merge/deploy. Không mở lại requirement
hoặc design vì đây là sửa lỗi so với yêu cầu giữ draft hiện có.

## U04/U13/U14 — áp dụng quy ước mã nguồn, 2026-11-15

Refactor đặt tên và trình bày theo [code-conventions.md](construction/code-conventions.md) sau lần rà soát cùng ngày; không đổi hành vi, URL, JSON, schema. Nhánh `fix/code-conventions-u04-u13-u14`. Brownfield, dùng lại tài liệu reverse engineering hiện hành. [Câu hỏi xác nhận](inception/requirements/2026-11-15-u04-u13-u14-conventions-questions.md).

- [x] Workspace Detection: brownfield, bỏ qua Reverse Engineering.
- [x] Requirements Analysis (Minimal): Q1–Q4 = A, Q5 = nhánh `fix/code-conventions-u04-u13-u14` (2026-11-15T09:27:37Z); [yêu cầu CC-01…06](inception/requirements/2026-11-15-u04-u13-u14-conventions.md); người dùng duyệt 2026-11-15T09:29:55Z.
- [x] User Stories: SKIP (không ảnh hưởng người dùng).
- [x] Workflow Planning: [execution plan](inception/plans/2026-11-15-u04-u13-u14-conventions-execution-plan.md); chạy Code Generation (một kế hoạch gộp) và Build and Test, bỏ qua các stage khác. người dùng duyệt 2026-11-15T09:29:55Z.
- [x] Code Generation: Part 1 [kế hoạch 12 bước](construction/plans/code-generation-plan/2026-11-15-u04-u13-u14-code-conventions-code-generation-plan.md) người dùng duyệt 2026-11-15T09:35:28Z; Part 2 xong 12/12 bước, tóm tắt [U04](construction/u04-subject-administration/code/2026-11-15-code-conventions-summary.md), [U13](construction/u13-reference-materials/code/2026-11-15-code-conventions-summary.md), [U14](construction/u14-announcements-and-notifications/code/2026-11-15-code-conventions-summary.md). Backend 493 test, 0 lỗi (40 bỏ qua do không có Docker); frontend tsc, ESLint sạch, Vitest bốn feature 178/178. Chưa commit; người dùng duyệt code 2026-11-15T11:23:27Z.
- [x] Build and Test: [kết quả](construction/build-and-test/2026-11-15/2026-11-15-u04-u13-u14-code-conventions-results.md); dựng lại Docker stack `platform` theo yêu cầu (8 service healthy); `mvn -o -B clean verify` 2.426 test, 0 failure/error/skip, 5 suite PostgreSQL bắt buộc qua; frontend lint, tsc, build qua; Vitest 1.075/1.092, 17 test timeout do tải máy, chạy lại qua (3 file cần `--testTimeout=60000`). E2E `u04-u13-u14` trên stack cô lập `conv041314`: 40/40 qua (2026-11-15T12:19:55Z). Người dùng duyệt 2026-11-15T12:21:14Z.
- [x] Operations: giai đoạn giữ chỗ, không có bước nào; triển khai theo quy trình merge vào `staging` hiện có. Đã commit cục bộ thành 10 commit trên nhánh `fix/code-conventions-u04-u13-u14` (2026-11-15), chưa push.
- Extension: Security và Resiliency giữ phạm vi đã duyệt, PBT tắt.

## U05 (kèm U04) — Học kỳ của lớp, 2026-11-14

Lớp có Ngày bắt đầu bắt buộc; học kỳ (SP/SU/FA + 2 số năm, thêm `-BL5` nếu bắt đầu tháng 4/8/12) do hệ thống tính và dùng để lọc lớp. Không có chức năng quản lý học kỳ. Mã lớp duy nhất theo môn và học kỳ. Quyết định D-U04-05 thay D-U04-04. Nhánh hiện tại `fix/term`. Brownfield, dùng lại tài liệu reverse engineering hiện hành. [Câu hỏi xác nhận](inception/requirements/2026-11-14-class-term-questions.md); [yêu cầu CT-01…09](inception/requirements/2026-11-14-class-term.md).

- [x] Workspace Detection: brownfield, bỏ qua Reverse Engineering.
- [x] Requirements Analysis (Standard): CT-01…09, G1–G4; người dùng duyệt 2026-11-14T15:48:47Z.
- [x] User Stories: SKIP (mở rộng nhỏ trên luồng lớp có sẵn, không thêm persona).
- [x] Workflow Planning: [execution plan](inception/plans/2026-11-14-class-term-execution-plan.md), người dùng duyệt 2026-11-14T15:51:26Z. Chạy Functional Design U05 (kèm U04), Code Generation, Build and Test; bỏ qua Application Design, Units Generation, NFR Requirements/Design, Infrastructure Design.
- [x] Functional Design U05 (kèm U04): [kế hoạch 6 bước, 6 câu hỏi](construction/plans/functional-design-plan/2026-11-14-u05-class-term-functional-design-plan.md), A cả 6 câu (2026-11-14T15:54:26Z). Đã viết BR-U05-61…72 (sửa BR-U05-11/13), F17–F20, `ClassTermPolicy`, frontend học kỳ; phần U04 (`SubjectClassSummary.term`) và U07 (`CopySource.term`). Security/Resiliency tuân thủ. Người dùng duyệt 2026-11-14T16:00:54Z.
- [x] Code Generation: Part 1 [kế hoạch 20 bước](construction/plans/code-generation-plan/2026-11-14-u05-class-term-code-generation-plan.md) người dùng duyệt 2026-11-14T16:05:52Z; Part 2 xong 20/20 bước (2026-11-15T02:21:55Z), [tóm tắt](construction/u05-class-enrollment/code/2026-11-14-class-term-summary.md). Backend 2426 test qua (gồm integration test có Docker), frontend tsc/eslint sạch, Vitest 540 test phần liên quan qua (timeout 60 giây). Chưa commit. Người dùng duyệt mã 2026-11-15T02:29:40Z.
- [x] Build and Test: [kết quả](construction/build-and-test/2026-11-15/2026-11-15-class-term-results.md). Backend `mvn -o -B verify` 2.426 test qua (gồm PostgreSQL); frontend lint/tsc sạch, build qua, Vitest 1.082/1.092 khi chạy cả bộ lúc máy tải nặng (10 test timeout 5 giây, chạy lại riêng hoặc với timeout 60 giây thì qua). Stack riêng `classterm`: API 21/21 qua, thử trên trình duyệt với Chủ nhiệm môn, giảng viên, sinh viên. Chưa commit. Người dùng duyệt 2026-11-15T03:05:31Z. Kiểm bổ sung 2026-11-15: thêm Admin, popup thông báo, trang Bài tập sinh viên, Đề có sẵn; e2e `u04-u13-u14` 40/40, các bộ khác lỗi không liên quan học kỳ (`ASSIGNMENT_NOT_FINALIZED` ở script U12, cấu hình AI, thứ tự chạy). Đã sửa script U12 thêm bước Hoàn tất: `u12b` 36/36, `u12c` 20/20, `u12` 58/59 (dữ liệu lần chạy trước).
- [x] Operations: giai đoạn giữ chỗ, không có bước nào; triển khai theo quy trình merge vào `staging` hiện có.
- Extension: Security và Resiliency giữ phạm vi đã duyệt, PBT tắt.

## U01 (kèm U05/U09/U11/U12) — Mã số sinh viên, 2026-11-13

Thêm MSSV (2 chữ cái in hoa + 6 chữ số) cho tài khoản `STUDENT`, ghi danh bằng MSSV. Không có chức năng đổi mã (chỉ sửa khi tài khoản còn `PENDING`), không có bộ lọc sinh viên chưa có mã (người dùng thu hẹp phạm vi 2026-11-13). Ngày 2026-11-14 bỏ thêm chức năng gán MSSV cho tài khoản cũ (F16), cột vẫn cho phép trống. Nhánh hiện tại `fix/student-code`. Brownfield, dùng lại tài liệu reverse engineering hiện hành. [Câu hỏi xác nhận](inception/requirements/2026-11-13-student-code-questions.md); [yêu cầu SC-01…11](inception/requirements/2026-11-13-student-code.md).

- Operations: giai đoạn giữ chỗ, không có bước nào; triển khai theo quy trình merge vào `staging` hiện có.

- [x] Workspace Detection: brownfield, bỏ qua Reverse Engineering.
- [x] Requirements Analysis (Standard): người dùng duyệt 2026-11-13; G1 đổi thành SC-11 (sinh viên thấy MSSV của mình và người khác), G2–G4 giữ.
- [x] User Stories: bỏ qua (thuộc tính mới cho vai trò đã có, không thêm persona).
- [x] Workflow Planning: [execution plan](inception/plans/2026-11-13-student-code-execution-plan.md), người dùng duyệt 2026-11-13 sau khi thu hẹp phạm vi. Chạy Functional Design U01 (kèm U05/U09/U11/U12), Code Generation, Build and Test; bỏ qua Application Design, Units Generation, NFR Requirements/Design, Infrastructure Design.
- [x] Functional Design U01 (kèm U05/U09/U11/U12): [kế hoạch 9 bước, 8 câu hỏi](construction/plans/functional-design-plan/2026-11-13-u01-student-code-functional-design-plan.md), A cả 8 câu. Đã viết BR-U01-100…115 (kèm F16 gán MSSV hàng loạt; chặn đổi role vào/ra STUDENT theo quyết định 15:50), BR-U05-56…60, BR-U09-73…75, BR-U11-42, BR-U12-97…98; Security/Resiliency tuân thủ. Người dùng duyệt 2026-11-13T15:53:39Z.
- [x] Code Generation: Part 1 [kế hoạch 20 bước](construction/plans/code-generation-plan/2026-11-13-u01-student-code-code-generation-plan.md) người dùng duyệt 2026-11-13T15:56:03Z; Part 2 xong 20/20 bước (2026-11-13T16:50:20Z), [tóm tắt](construction/u01-account-and-access/code/2026-11-13-student-code-summary.md). Backend 805 test qua (2 lỗi integration vì thiếu Docker), frontend tsc/eslint sạch, Vitest 1043 test qua (5 test timeout khi chạy tải nặng, chạy lại riêng thì qua). Chưa commit. Người dùng duyệt mã 2026-11-13T16:52:38Z.
- [x] Build and Test: [kết quả](construction/build-and-test/2026-11-14/2026-11-14-student-code-results.md). Backend mvn verify 2.366 test qua (gồm PostgreSQL); frontend lint/tsc sạch, Vitest 1042/1045 (3 timeout khi chạy cả bộ, chạy lại riêng qua), build qua; thử trên stack riêng `studentcode` với Admin, Chủ nhiệm môn, sinh viên. Đã thử bổ sung bảng điểm/xuất, chấm bài, quiz, nhóm, màn hẹp, chế độ tối; sửa 3 chỗ frontend (header chấm bài, dòng thành viên đề xuất, bảng điểm màn hẹp). Người dùng duyệt 2026-11-14T10:43:59Z. Đã commit, merge origin/staging, push; e2e u04-u13-u14 40/40; PR #84 vào staging (https://github.com/hygef-v4/AI-Powered-Learning-Platform/pull/84), chưa merge.
- Extension: Security và Resiliency giữ phạm vi đã duyệt, PBT tắt.

## U06 — Guardrail hai tầng cho prompt AI, PR #77 vào staging, 2026-11-12

Nhánh `feat/ai-prompt-guardrail` từ `main@8923d45d`. Guardrail Tầng 1 (quy tắc/regex) và Tầng 2 (RBAC, máy trạng thái, phân tích trích dẫn, chính sách logic, không regex) chạy tuần tự, fail-closed, trả `400 AI_PROMPT_REJECTED` trước khi lưu draft, giữ quota hay gọi AI. [Thiết kế và vị trí file](construction/u06-ai-connections/code/ai-prompt-guardrail.md); [kế hoạch và kết quả](construction/plans/code-generation-plan/2026-11-11-ai-prompt-guardrail.md).

- [x] Workspace Detection: brownfield, dùng lại tài liệu reverse engineering
- [x] Requirements Analysis (Standard): đặc tả của người dùng đủ; quyết định stack Java/Spring Boot và vị trí ở U06 ghi trong kế hoạch
- [x] Workflow Planning: chỉ Code Generation và Build and Test; các giai đoạn khác bỏ qua
- [x] Code Generation: contract + 8 lớp service, nối vào soạn/sửa đề, soạn quiz, tạo lại quiz, `AiTaskService`; `UntrustedPrompt` cho 5 prompt builder
- [x] Build and Test: toàn bộ `mvn verify` backend 2.220 test qua, 0 lỗi
- [x] Người dùng duyệt mã (2026-11-11)
- [x] Trình duyệt: yêu cầu injection ở "Sửa đề bằng AI" bị chặn 400, hiện câu báo thân thiện, không tạo draft/AI task (2026-11-12)
- [x] Commit, merge staging, push; PR #77 vào staging, chưa merge

Không đổi schema, migration, cấu hình, dependency hay frontend. Security Baseline: SECURITY-03 (log không chứa nội dung), 05 (kiểm đầu vào), 08 (RBAC phòng thủ thêm), 15 (fail-closed) tuân thủ; các quy tắc khác N/A. Resiliency không bị ảnh hưởng. PBT tắt.

## U07 — Nhập đề từ ảnh/PDF thành khung đề chỉnh sửa được — Code Generation, 2026-11-11

Mở rộng nhập DOCX của U07 sang ảnh và PDF scan, dùng AI đọc ảnh (U06) để dựng khối đề. Brownfield, dùng lại tài liệu reverse engineering hiện hành. [Câu hỏi xác nhận](inception/requirements/2026-11-11-exam-image-import-questions.md) (A cả 10 câu); [yêu cầu](inception/requirements/2026-11-11-exam-image-import.md).

- [x] Workspace Detection: brownfield, bỏ qua Reverse Engineering.
- [x] Requirements Analysis: FR-IMP-01…08, người dùng duyệt 2026-11-11T07:28:36Z.
- [x] User Stories: bỏ qua (một vai trò, mở rộng US-ASM-004).
- [x] Workflow Planning: [execution plan](inception/plans/2026-11-11-exam-image-import-execution-plan.md), người dùng duyệt. Chạy Functional Design (U07 kèm delta U06/U03), Code Generation, Build and Test; bỏ qua Application Design, Units Generation, NFR Requirements, NFR Design, Infrastructure Design.
- [x] Functional Design U07 (kèm delta U06/U03): [kế hoạch](construction/plans/functional-design-plan/2026-11-11-u07-exam-image-import-functional-design-plan.md) (A cả 6 câu); BR-U07-87…101, BR-U06-54…60, BR-U03-99…100 trong functional-design của từng unit; người dùng duyệt.
- [x] Code Generation Part 1: [kế hoạch 22 bước + nhánh](construction/plans/code-generation-plan/2026-11-11-u07-exam-image-import-code-generation-plan.md), người dùng duyệt.
- [x] Code Generation Part 2: 22/22 bước xong trên nhánh `feat/exam-image-import` (worktree `.claude/worktrees/exam-image-import`, `--no-track` từ `origin/staging@e78ac7b9`), người dùng duyệt 2026-11-11T13:20:00Z. [Tóm tắt và điều chỉnh](construction/u07-assignment-authoring/code/2026-11-11-exam-image-import-summary.md). Test theo phạm vi: U03 25/25, U06 64/64, U07 79/79, ArchitectureTest 3/3, frontend tsc/eslint sạch, Vitest assignments 61/61. Chưa commit.
- [x] Build and Test (người dùng duyệt 2026-11-11T16:35:00Z): test tự động và thử trên trình duyệt với Gemini thật đã xong, [kết quả](construction/build-and-test/2026-11-11/2026-11-11-exam-image-import-verification.md). Đã sửa bind `AiProperties.Provider`, lỗi chọn tệp trong popup, schema để Gemini trả đủ nội dung, hình trong màn xem trước (BR-U03-101) và `placement` của `:apply`. Backend mvn verify 2201/2201, frontend Vitest 969/969, tsc, eslint sạch. Chưa commit.
- [x] Operations: giai đoạn giữ chỗ, không có bước nào; triển khai theo quy trình merge vào `staging` hiện có.
- Extension: Security và Resiliency bật trong phạm vi đã duyệt, PBT tắt (giữ bảng Extension Configuration).

Tạo worktree riêng vì thư mục chính có thay đổi chưa commit của phiên khác.

## U05/U08/U14 — tổng quan lớp sinh viên, 2026-11-11

Thay Thông báo gần đây và thẻ cần làm chia đôi bằng một thẻ Tổng quan bài tập đủ chiều rộng, đồng bộ giảng viên: số Chưa làm/Đang làm/Đã nộp mở đúng tab của lớp; giữ loại bài và hạn nộp, thẻ co theo nội dung. Bài nhóm đã nộp được loại khỏi helper upcoming dùng chung. Thông báo vẫn có trong menu, không thêm lịch/số buổi/điểm nháp. [Kế hoạch](construction/plans/code-generation-plan/2026-11-11-student-class-overview.md), [kiểm tra](construction/build-and-test/2026-11-11/2026-11-11-student-class-overview.md).

Full frontend972/972,138 tệp; kiểm cuối7/7, ESLint/TypeScript/build32 trang qua. UI local student2 trong profile tách riêng,1440/390 sáng/tối; số2/1/3 khớp tab Bài tập, menu thông báo còn, không tràn ngang. Không đổi backend/migration hay ghi bài nộp/điểm; chưa test VPS/thiết bị thật. Đã commit riêng `97ebab28` theo yêu cầu tiếp theo; giữ thay đổi bảng điểm trong commit riêng, chưa push/deploy. Security/Resiliency giữ phạm vi đã duyệt, PBT disabled.

## U09 — bảng điểm dạng ma trận, 2026-11-11

Đã triển khai trên `fix/u05-u09-u15`: nút Bảng điểm ở trang Chấm điểm mở ma trận sinh viên × assignment, lọc một bài/toàn bộ, tìm kiếm/phân trang và xuất XLSX/CSV cùng cấu trúc. Điểm0 và điểm riêng thành viên nhóm được giữ, chưa chấm để trống; XLSX dùng ô số và cố định header/cột định danh. [Kế hoạch](construction/plans/code-generation-plan/2026-11-11-gradebook-matrix.md), [kiểm tra/giới hạn](construction/build-and-test/2026-11-11/2026-11-11-gradebook-matrix.md).

Frontend970/970, backend full verify2.153/2.153 và guard57 ca qua; test bổ sung cuối Assembly/Exporter/Controller19/19, giao diện cuối19/19; lint/TypeScript/build qua. UI local đã kiểm màn rộng/hẹp, sáng/tối, lọc/tìm và popup xuất. Writer mới tạo XLSX được đọc lại xác nhận kiểu số/freeze/filter; download live vẫn theo cấu trúc cũ vì backend hiện chạy chưa nạp mã mới. Không restart trái lệnh automatic approval review trước đó. Đã commit riêng `7466539b` theo yêu cầu tiếp theo, chưa push/deploy; không sửa điểm hay dữ liệu workbook mẫu. Trước commit chạy lại26 test frontend liên quan và19 test backend export/assembly/controller, đều qua. Security/Resiliency giữ phạm vi đã duyệt, PBT tắt.

## U12 — Sinh viên tự chia nhóm, giảng viên duyệt — Build and Test, 2026-11-11

Tính năng mới mở rộng U12: sinh viên tự lập đề xuất nhóm trong lớp, giảng viên duyệt thì đề xuất thành nhóm chính thức. Brownfield, dùng lại tài liệu reverse engineering hiện hành. [Câu hỏi xác nhận](inception/requirements/2026-11-11-student-self-grouping-questions.md) (A cả 9 câu); [yêu cầu SG-01…09](inception/requirements/2026-11-11-student-self-grouping.md). Nhánh `feat/student-self-grouping` (`--no-track` từ `origin/staging@e78ac7b9`), worktree `../AI-Powered-Learning-Platform-self-grouping`.

- [x] Workspace Detection: brownfield, bỏ qua Reverse Engineering.
- [x] Requirements Analysis: người dùng duyệt 2026-11-11. Security/Resiliency giữ phạm vi, PBT tắt.
- [x] User Stories: [kế hoạch](inception/plans/2026-11-11-student-self-grouping-story-plan.md) A cả 4 câu; US-GRP-007…010, người dùng duyệt 2026-11-11.
- [x] Workflow Planning: [kế hoạch thực hiện](inception/plans/2026-11-11-student-self-grouping-execution-plan.md), người dùng duyệt 2026-11-11. Chạy Functional Design U12, Code Generation, Build and Test; bỏ qua Application Design, Units Generation, NFR Requirements/Design, Infrastructure Design.
- [x] Functional Design U12: [kế hoạch](construction/plans/functional-design-plan/2026-11-11-u12-student-self-grouping-functional-design-plan.md) A cả 9 câu; BR-U12-70…96, F16–F24, người dùng duyệt 2026-11-11.
- [x] Code Generation U12: [kế hoạch 19 bước](construction/plans/code-generation-plan/2026-11-11-u12-student-self-grouping-code-generation-plan.md) xong 19/19, người dùng duyệt 2026-11-11. Backend 54 test liên quan qua (gồm PostgreSQL); frontend 45 test nhóm, tsc, eslint qua. Chưa thử trên trình duyệt, chưa chạy toàn bộ suite.
- [x] Build and Test: [kết quả](construction/build-and-test/2026-11-11/2026-11-11-student-self-grouping-results.md). Backend `mvn -B verify` 2.203 test qua; frontend lint, typecheck, 978/978 test, build qua; thử hai vai trò trên stack riêng `selfgroup`. Người dùng duyệt 2026-11-11; đã commit trên nhánh, chưa push.

## UI/UX đợt 2 — kiểm báo cáo ngày 2026-11-10, đã commit, chưa push, 2026-11-11

Nhánh `fix/ui-ux-review-2` từ `origin/staging@353383f2`. Đối chiếu từng mục của báo cáo đánh giá UI/UX với mã sau PR #64: mục 3 (bảng Bài tập dạng thẻ, cột lớp), 4 (thanh đồng hồ dính, `aria-live`, đáp án 44px), 6 (ngăn kéo `inert`, Esc), 8 (toast lỗi), "Practice", liên kết bỏ qua, Đăng xuất trong menu tài khoản, `Menu` phím mũi tên, tiêu đề popup 22px đã sửa trước đó.

- [x] Sidebar: mục đang chọn `bg-primary-soft`, hover token mới `--c-sidebar-hover` (sáng `#f4f4f5`, tối `#202b39`); bỏ bóng nâu; mũi tên mở nhánh 40px trên màn cảm ứng, liên kết cha chừa lề phải
- [x] Ô nhập 16px dưới 640px (quy tắc chung trong `globals.css`, ô OTP chữ to giữ cỡ)
- [x] Lề `main` và 4 thanh đầu trang dính dùng chung `.main-gutter`/`.main-bleed`; đã đo trên trình duyệt: thanh trùng hai mép `main`
- [x] Công tắc tắt dùng `--c-field` (3,33:1), cả `Switch` và bản trong Thông báo
- [x] Trang chấm bài: `window.confirm` thành popup "Bỏ thay đổi"
- [x] Quiz: "Chưa lưu được" màu cảnh báo, popup khi thoát quiz có giới hạn giờ, dải số câu cuộn ngang ở trên câu hỏi khi hẹp hơn `lg`
- [x] Hạn nộp tương đối ("còn 5 giờ", cảnh báo dưới 24 giờ) ở trang Bài tập của sinh viên; thời gian thông báo dạng tương đối; `shared/dateTime.ts` cố định giờ Việt Nam thay 3 chỗ `toLocaleString` theo giờ máy
- [x] `InfoTip` đóng bằng Esc, rê chuột vào được; `Tabs` có Home/End, `aria-controls`/`tabpanel` ở trang lớp và môn; bảng lượt làm quiz mở được bằng bàn phím
- [x] Câu chữ: "Không có … phù hợp.", "…", "Nhập đúng email do trường cấp.", hằng số `LOAD_FAILED_MESSAGE`; trình xem học liệu `100dvh`
- [ ] Chưa làm: `useAuthoringGuard` (nút Back cần trả lời đồng bộ), thang cỡ chữ/bo góc/z-index, `Overlay`/`Textarea` dùng chung, gộp `Pagination`/`PageNav`, gộp 11 hàm ngày giờ, kiểm thử ảnh chụp, lint chặn giá trị lẻ; hành vi ô chọn môn/lớp trên sidebar chờ người dùng quyết định

Kiểm tra: `tsc --noEmit`, `eslint` qua; Vitest 133 tệp, 950 test qua. Trình duyệt (stack Docker local): sidebar hai theme, ô nhập 16px ở 375px, thanh dính trang soạn bài. Chưa thử trên iPhone thật. Không đổi backend, API hay schema; đã commit trên nhánh, chưa push. Security/Resiliency giữ phạm vi đã duyệt, PBT tắt.

## Sửa 22 điểm UI/UX — build và kiểm thử xong, đã commit, chưa push, 2026-11-10

Người dùng yêu cầu sửa toàn bộ 22 điểm của đợt rà soát UI/UX, chọn phương án A cho cả 9 câu hỏi, duyệt kế hoạch 21 bước và duyệt mã. Nhánh `fix/ui-ux-remediation` tạo từ `staging` `062a43f5`. Chỉ sửa `frontend/` và `PRODUCT.md`; không đổi backend, API, schema. [Yêu cầu và quyết định](inception/requirements/2026-11-10-ui-ux-remediation-questions.md); [kế hoạch và điều chỉnh](construction/plans/code-generation-plan/2026-11-10-ui-ux-remediation.md); [tóm tắt thay đổi](construction/ui-ux-remediation/code/summary.md); [kết quả build và kiểm thử](construction/build-and-test/2026-11-10/2026-11-10-ui-ux-remediation-results.md).

- [x] Workspace Detection: brownfield, dùng lại tài liệu reverse engineering hiện có.
- [x] Requirements Analysis: 22 yêu cầu, 9 quyết định (đều A).
- [x] Workflow Planning: chỉ chạy Code Generation và Build and Test; các giai đoạn khác bỏ qua.
- [x] Code Generation: 21/21 bước, người dùng đã duyệt.
- [x] Build and Test: TypeScript và ESLint sạch; Vitest 886/886 ca trong 128 file, ba lần liên tiếp; production build thành công; trình duyệt xác nhận màn đăng nhập và trang 404.
- [ ] Operations: giai đoạn giữ chỗ, chờ người dùng xác nhận.

Chưa kiểm tra trên trình duyệt các màn sau đăng nhập (chỉ có kiểm thử tự động trong jsdom). Đã commit trên nhánh theo yêu cầu sau đó của người dùng; chưa push. Security và Resiliency tuân thủ trong phạm vi đã duyệt, các quy tắc khác N/A; PBT tắt.
## Đồng bộ tài liệu với code staging — 2026-11-10

Đối chiếu `staging@062a43f5` với aidlc-docs và sửa các chỗ lệch do commit code chưa kèm tài liệu: U07 sửa đề bằng AI (`REVISE`, `AI_DRAFT_STALE`), U08 bỏ kiểm nộp và trang biên nhận, U09 phần bỏ trống 0 điểm và điều hướng bản nộp nhóm, U10 số câu theo độ khó, U06 model Gemini chọn sẵn, cây điều hướng môn/lớp (U05/U01), log lỗi Admin môn học (U04). Bảng kê source trong current-code-baseline được bổ sung 15 mục, bỏ 2 liên kết hỏng; API inventory khớp 239 mapping. [Báo cáo](construction/build-and-test/2026-11-10/2026-11-10-aidlc-code-sync-gaps.md).

- [x] Đối chiếu commit code sau các đợt sync 2026-11-07 và 2026-11-09
- [x] Sửa functional-design và as-built của unit bị lệch
- [x] Cập nhật current-code-baseline, api-inventory, build-and-test-summary
- [x] Kiểm liên kết cục bộ của các file đã sửa
- [x] Đợt 2 sau merge tới `staging@1e77eb79`: PR #60 (đề mẫu của môn, nộp lại không giới hạn, đánh số câu hỏi, popup tạo bài) vào U07, U08, U09, U05; BR-U07-80…86, BR-U08-70…74; api-inventory 241 mapping, baseline thêm 6 mục
- [ ] Inception (FR-027, US-ASM-009, số lượt nộp bài có chấm) còn mô tả hành vi cũ; chờ xác nhận yêu cầu trước khi sửa

Chỉ sửa Markdown; không chạy application test, không đổi code/schema/config, không commit hay deploy. Thay đổi chưa commit ở `AssignmentFormPage` nằm ngoài phạm vi. Security/Resiliency giữ phạm vi đã duyệt, PBT disabled.

## U08 — Thẻ Góp ý AI trong Lịch sử nộp bài, 2026-11-09

Đã triển khai trên `fix/student-ai-grading` và đồng bộ UC 41, FR-030, US-ASM-012 cùng tài liệu U08. Thẻ luôn hiện theo lượt Practice đã nộp, dùng cột nhận xét trên màn hình rộng và chuyển vào đầu nội dung bài làm trên màn hình hẹp. [Kế hoạch và kết quả kiểm tra](construction/plans/code-generation-plan/2026-11-09-student-ai-feedback-card.md).

Đã xác nhận 71 kiểm thử trong phạm vi Assignment Work và trình xem tài liệu, lint, TypeScript trong production build và build. Lượt chạy toàn bộ Vitest bị dừng sau lỗi `ENOENT` ở thư mục tạm của sandbox. Lượt chạy phạm vi liên quan dùng thư mục tạm trong workspace. Build hoàn tất với cảnh báo tải font Google. Chưa xác nhận trên trình duyệt hoặc với nhà cung cấp AI trực tiếp. Security và Resiliency giữ phạm vi đã duyệt. PBT tắt theo cấu hình dự án.

## Integration-test delivery — regressions verified, publication ready, 2026-11-09

Initial commits15400ac2/8d53a1c6 pushed normally. Staging fb20c6bc merged as a55f8426; later staging198b796f merged as65f86a6c. Complete first-merge checks: backend2,028/2,028 plus57-case guard; frontend build/lint/typecheck and800/800 tests. Latest U12 increment:95/95 backend, current frontend build/typecheck/scoped lint and15/15 tests. Keep those scopes separate; no summed or fresh-final-full-run claim.

All168 actual API/proxy/SSE E2E assertions passed against `65f86a6c432c63e318737cef88195ad5ffd30ab1` with local Mailpit OTP/SMTP and isolated services. Only the task stack/frontend stopped; original7 platform container IDs preserved. [Delivery results](construction/build-and-test/2026-11-09/2026-11-09-integration-test-delivery-results.md) and [evidence](construction/build-and-test/2026-11-09/2026-11-09-integration-test-delivery-evidence.json) record exact snapshots, hashes and earlier resource-sensitive timeouts. No assertion/timeout or production behavior was changed to obtain a pass.

Final evidence is ready for commit/push, then the authorized PR to staging. PR/CI publication state is recorded externally on GitHub and in the final response; no PR merge or deployment is authorized. Existing security/resiliency retained; PBT disabled. The57-case workbook remains unchanged and separately delivered. Prior no-commit/no-push statements below are historical.

## Prioritized integration completion — verified, 2026-11-09

Task8 is complete on integration-test. Eight additional methods extend the composed suite from16 to24. Final sequential backend verify passed2,028/2,028 with zero failures/errors/skips at2026-11-09T11:18:00+07 and packaged the executable JAR. All seven unchanged local latency checks passed; earlier failed attempts remain preserved. Current required guard passed57 mapped report cases/62 executions, with all four negative QA fixtures rejected. Independent review's quota-clock and grading-lock findings were addressed; no deferred findings remain.

The English integration report now contains57 representative cases across15 units:49 original case cells/styles/execution dates and the Cover revision preserved, eight cases appended. Source template/native styles/drawings/widths/hidden groups remain intact. Five affected unit tabs and summaries/trace/header views were visually reviewed; eleven unchanged unit/security sheets compared with the baseline. Reopened formula recalculation passed. Scenario/evidence/results and Task8 ledger contain source hashes, workbook hash, actual boundaries and remaining per-unit exclusions.

Only the composed backend test source changed from the historical1,031-file source snapshot. No production source/schema/dependency/frontend change, live provider call, commit, push, PR or merge. Reports5.3–5.5 remain untouched. External mail is controlled; deployed browser/proxy/provider acceptance and exhaustive requirements coverage remain outside this report. Existing security/resiliency retained; PBT disabled. Historical49-case results below describe the preceding phase.

## Integration tests and English report — design review, 2026-11-08

User selected scope B: representative automated integration coverage across all15 units, delivered in groups, plus an English report preserving `Report-5.2_Integration Test_Template.xlsx` styling. Created `integration-test` from freshly fetched `origin/staging`321f8892 with a clean starting checkout. [Scope answer and design review](inception/requirements/2026-11-08-integration-test-questions.md); [concrete design](inception/application-design/2026-11-08-integration-test-design.md).

- [x] Workspace detection and branch setup.
- [x] Confirm all15-unit scope and inspect initial requirements/test/template structure.
- [x] Write implementation-aware, requirement-grounded design with per-unit coverage targets and existing evidence candidates.
- [x] Independent builder check: no important unanswered planning questions.
- [x] User approval of the concrete design (question2=A), including planning and sequential implementation.
- [x] Detailed implementation plan and per-group execution:16 new real-component integration methods, production source unchanged.
- [x] Fresh full backend verification:2,020 passed, zero failures/errors/skips;49 traced report cases resolve to54 passing executions. Guard and four negative checks passed; independent review found no actionable findings.
- [x] English report authored/recalculated/exported/reopened and all19 sheets visually reviewed;49 cases, accurate execution statistics, traceable methods/requirements and preserved template styling/native parts.

Initial search found24 Testcontainers/composed-context candidates; classification inspected actual collaborators rather than filenames. New composed tests use production owners, disposable PostgreSQL/Redis/Rabbit and current Flyway. Existing focused suites are reused with explicit mocked/persistence-only boundaries. [Plan](construction/plans/code-generation-plan/2026-11-08-integration-test.md), [results/gaps](construction/build-and-test/2026-11-08/2026-11-08-integration-test-results.md), [scenario manifest](construction/build-and-test/2026-11-08-integration-test-cases.json) and [source-hashed execution evidence](construction/build-and-test/2026-11-08/2026-11-08-integration-test-evidence.json). Final Maven verify completed19:47:22+07 on2026-11-08. The early overlapping-compile baseline was invalidated and is not acceptance evidence. Existing Maven/CI entry point, public contracts, migrations, dependency versions and production persistence choices preserved. Final English workbook has19 sheets and49 Passed Round1 cases; later rounds remain Pending. It was reopened, visually reviewed and checked for formula reactivity, source/template SHA, style/native-part preservation and navigation. Existing security/resiliency scope retained; PBT disabled. Branch remains uncommitted/unpushed; no PR, deployment, live data/provider writes or full-requirements acceptance claim.

## fix/remove-term delivery and staging merge — 2026-11-08

User explicitly authorized scoped commits, push, staging→current-branch merge and E2E. Six scoped commits pushed to origin/fix/remove-term through fc31721b, then origin/staging528cc50e merged at6007c5fb. Only audit history conflicted; all lines from both branches preserved. Test-only integration repairs committed at44b5781d: restore three needed imports removed by staging cleanup and align the staff-email E2E oracle with BR-U14-21/D-U14-09. Verified merge/test integration through44b5781d has also been normally pushed to the same branch. No postmerge production behavior change.

Fresh premerge complete backend2,015/2,015 and frontend805/805 passed. Postmerge frontend affected suites286/286, scoped lint and build31pages passed. Isolated API E2E168/168 (40 U04/U13/U14,16 U02,58 U12,35 U12b,19 U12c) passed with actual Nginx/auth/OTP/worker/Mailpit and separate data. First full postmerge backend2,003/2,004 passed, one load-sensitive U14 p95 assertion368.3049ms exceeded300ms during concurrent E2E; unchanged isolated benchmark then passed120.9493ms. Final complete retry without E2E load **2,004/2,004**, zero failures/errors/skips, BUILD SUCCESS17:21:26+07; announcements p95 **83.9347ms**, required PostgreSQL31/7/10/1 passed. All1,625 source/config hashes match final verification; see [delivery plan and evidence](construction/plans/code-generation-plan/2026-11-08-remove-term-staging-e2e.md).

E2E resources stopped with volumes retained; existing platform stack preserved. API E2E does not close browser/proxy deployment capacity/provider acceptance or all requirements gates. Report drafting remains paused; historical draft contents preserved. Existing security/resiliency retained; PBT disabled.

## U04/U13/U14 expanded clause remediation — 2026-11-08

Continued the user's explicit expanded review request on `fix/remove-term`, using current `aidlc-docs` and the settled reconciliation decisions. [Current clause map, evidence and open acceptance gates](construction/plans/code-generation-plan/2026-11-08-u04-u13-u14-clause-closure.md). Fixed demonstrated missing U13 current-version provenance, U04 null archive-result handling and blocked normal lifecycle transaction, missing module denial audit, swallowed GROUP submission dependency failure, and reminder truncation after500 candidates. Added real owner PostgreSQL/security/JWT/audit composition, Redis token expiry, Rabbit two-hub delivery, parser and local performance tests without new dependencies or a persistence migration.

Fresh full backend verify **2,010 passed**, zero failures/errors/skips; required PostgreSQL31/7/10/1 and migration2/1 passed. Later test-only content/snapshot refinements **6/6**, module transaction/retention refinements **3/3** and final multirow CAS rollback/retry **1/1** passed separately. Fresh affected frontend **233/233**, TypeScript/scoped lint/production build31pages passed. Full backend verification predates the late test-only refinements, and is not represented as a later rerun. Independent review of the production changes and late rollback/retention/CAS oracles found no remaining actionable finding.

Local measured p95/fanout/51MB parser evidence is explicitly scoped. Browser/proxy/provider acceptance, representative concurrent load, complete adversarial parser isolation and universal caller-owned lifecycle deadlines remain open; this is not100% requirements acceptance. Prior minimal-slice sections below remain historical. Report drafting remains paused; staged drafts preserved; no commit/push/deploy/live DB/provider writes. Existing security/resiliency scope retained; PBT disabled.

## U04/U13/U14 confirmed requirement baseline — 2026-11-08

Decisions1–11 are confirmed in [reconciliation questions](inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md). Current requirements synchronized for U04 cascade/class-count/close behavior, fixed PDF/DOCX/PPTX/XLSX MATERIAL formats, XLSX AI extraction, archived staff reference access and staff email default-on with saved preferences preserved. User approved the [concrete test-remediation plan](construction/plans/code-generation-plan/2026-11-08-u04-u13-u14-requirements-tests.md) at question12=a. Approved U04/U13/U14 test additions and archive popup explanation implemented and independently reviewed. Fresh full backend verify1,879/1,879, required PostgreSQL31/7/10/1 plus migration1/2 passed; final fixture-only material query file15/15 passed. Frontend affected feature suite231/231 and final detail-page file13/13 passed, including new0/7 count assertions; TypeScript/scoped lint/build31pages passed. Initial frontend timeouts resolved on unchanged reruns. The plan contains the requirement/assertion map and explicit remaining contract/integration/NFR gates. Historical reports/plans unchanged; report drafting remains paused. Existing branch/user changes preserved; no commit/push/deploy/live writes.

## Class semester removal — implemented 2026-11-07

Implemented D-U04-04 on user-requested `fix/remove-term` from staging c9b7ce96: removed semester from U04/U05 request/response/domain/contracts/JDBC/frontend payloads, fixtures and E2E seeds; synchronized current semester requirements. New Flyway migration `V20261107_2200__remove_class_term.sql` drops only the old class column; applied migrations unchanged. [Decisions](inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md), [implementation plan and evidence](construction/plans/code-generation-plan/2026-11-07-remove-class-term.md).

Fresh backend Maven verify1,846 pass, zero failures/errors/skips; required PostgreSQL guard31+2+10+1 passed. New migration suite2/2 passed, including seeded upgrade/fresh install/replay and JDBC CRUD with retained class/enrollment data. Frontend focused149/149, TypeScript/scoped lint/production build31pages passed; E2E script syntax passed. RED evidence: frontend3 expected failures and backend12 expected failures before removal. Independent review: no remaining actionable findings. No live migration/database write, commit/push/deploy or report drafting; existing staged drafts preserved. This completes only the approved semester slice, not all U04/U13/U14 coverage remediation. Existing Security/Resiliency scope retained; PBT disabled.

## Current documentation/source index — 2026-11-07

Source snapshot `f68a6ec5` on `fix/u05-u09-u15`; documentation synchronized by the user's request. [Source inventory](inception/reverse-engineering/current-code-baseline.md), [API mappings](inception/reverse-engineering/api-inventory.md), [services](inception/application-design/services.md), [contract types](inception/application-design/component-methods.md) and 15 as-built summaries reflect current file/type ownership. Mail/deploy/AI-key/source behavior and required-test guidance were updated from source; [results and verification limits](construction/build-and-test/2026-11-07/2026-11-07-aidlc-code-sync-results.md).

No application/config/test changes or fresh application run during this sync. The earlier 1,830-backend/799-frontend evidence predates the staging merge and is not current-HEAD acceptance. Dated sections below are historical records of their original scope, including statements about commit/deployment status at that time. Existing security/resiliency configuration remains; PBT disabled. No commit, push or deployment.

## Continuous deployment to the VPS — 2026-11-06

User-chosen pull model: GitHub-hosted runners cannot reach the VPS, which blocks international inbound traffic, and a self-hosted runner cannot reach the Actions broker. `platform-deploy` (systemd timer, 2 min) asks the GitHub API for the latest successful `ci.yml` push run on `main`. For each new SHA it pulls images, dumps the database (keeps 5), checks out the SHA, sets `IMAGE_TAG`, runs `up -d` and gates on container health plus HTTPS 200. On failure it rolls back automatically and skips that SHA. Manual `rollback`, `status` and `unskip` commands are available. Database migrations are not reversed; the runbook covers dump restore. The RabbitMQ healthcheck timeout was raised: the 7 s ping made it falsely unhealthy, which would block every restart. CI gains a `deploy-script` job and drops the placeholder deploy job. Fake-command tests 11/11 passed, shellcheck clean. Installed and enabled on the VPS; a same-SHA deploy passed in 28 s. [Plan and results](construction/plans/code-generation-plan/2026-11-06-continuous-deployment.md), runbook section 10. Not yet seen: a real deploy from a new `main` commit; no commit.

## Any email domain — 2026-11-06

At the user's request, BR-U01-03 no longer restricts the domain. Admin create, CSV import, OTP requests and the bootstrap Admin accept any well-formed email of at most 254 characters. Uniqueness and normalization are unchanged, and there is still no self-registration. `U01_ALLOWED_EMAIL_DOMAINS` and `EMAIL_DOMAIN_NOT_ALLOWED` were removed. Schema unchanged; `school_email` is kept as the field name. Requirements, US-IAM-001, U01 design docs, code summary, main business flows, E2E README and deploy runbook were synced. Backend 1,776 passed, zero skips, required PostgreSQL suites passed; frontend identity 38 passed, typecheck and lint passed. [Plan and results](construction/plans/code-generation-plan/2026-11-06-any-email-domain.md). No commit or deployment.

## Brevo HTTPS API mail transport — 2026-11-06

Requested because the production VPS provider blocks all outbound SMTP while `api.brevo.com:443` is reachable. `MAIL_TRANSPORT=brevo-api` with `BREVO_API_KEY` selects the new `BrevoApiMailAdapter`. `smtp` stays the default, and Mailpit stays the local default. An unknown value stops startup. The fixed endpoint is used; logs and errors carry only the HTTP status and Brevo error code. Compose, `.env.example` and the deploy runbook have been updated. Fresh full backend run: 1,773 passed, zero skips; required PostgreSQL suites passed. [Plan and results](construction/plans/code-generation-plan/2026-11-06-brevo-api-mail.md). Not yet done: real Brevo call, image build and deployment, commit. Security/resiliency scope retained; PBT disabled.

## Staging pull request delivery scope — 2026-11-06

User requested a PR from fix/u05-u09-u15 to staging, authorizing the needed push. Refreshed refs: staging is an ancestor, the feature branch was synchronized and no open PR existed for it. Reviewed scope includes U05/U09/U15 readability/simplification, U09 JDBC persistence, global AI-DLC JDBC policy and U05 repository names. PR evidence distinguishes the full JDBC 1,830-backend/799-frontend checks from the latest naming-only 200 Academics/Architecture run; no product changes or redundant runtime tests for delivery metadata. Document no-op save/version distinction and remaining JPA owners; create the PR for review without merging/deploying or claiming CI success in advance.

## U05 repository names simplified — 2026-11-06

At the user's screenshot/naming request, removed Jdbc from the three U05 concrete repository class/file names: AcademicsReadRepository, CourseClassRepository and EnrollmentRepository. Updated constructor/method-reference/test callers and the test dependency field, current U05 summary and shared naming conventions; interfaces and NamedParameterJdbcTemplate remain unchanged. No named qualifier references found. Verified all five Java files differ only by the requested identifier replacements, preserving SQL/transactions/API and behavior text. Fresh compilation of 718 main/246 test files and focused Academics/Architecture run passed all 200 cases, zero failures/errors/skips; stale old-name class output absent, whitespace check passed. [Plan and evidence](construction/plans/code-generation-plan/2026-11-06-u05-repository-names.md). Other units, frontend, runtime restart and automatic commit/push outside this naming-only scope. Security/resiliency retained; PBT disabled.

## JDBC local commit scope — 2026-11-06

User requested clear local commits for the verified JDBC work. Delivery grouping: one U09 JDBC code/test/required-DB-guard commit, followed by one project-wide AI-DLC policy/implementation/verification documentation commit. U05 remains JDBC unchanged; U04/U13/U14 remain separately documented JPA migration debt. Reviewed scoped diff and whitespace; 1,572 source/config hashes match the passing 1,830-backend/799-frontend verification, so no redundant runtime rerun. Exclude ignored scratch logs, credentials and build artifacts; retain running local apps. No push, remote write, merge, deployment or Git configuration change authorized by this request.

## JDBC backend/frontend restarted and freshly tested — 2026-11-06

At the user's explicit restart/test request, stopped only verified app PIDs, rebuilt the standard backend JAR and restarted backend :8080 (PID 50084, local,worker) plus frontend :3000 (listener 3184, launcher 43712). Fresh normal-target full Maven verify: 1,830 tests/236 suites, zero failures/errors/skips, executable repackage successful; the earlier thin-JAR limitation is resolved. Required PostgreSQL suites 31 + 2 + 10 + 1 passed. Frontend 799 tests/116 files, full lint/typecheck/production build and final post-restart typecheck passed. Backend health/liveness/readiness UP/200; frontend root/login 200; protected API via frontend and directly 401. Live Flyway validated 32 migrations with none pending. Restored next-env.d.ts/AGENTS.md generated changes; all 1,572 source/config hashes match the pre-operation snapshot, preserving pending JDBC changes. Existing Docker dependencies healthy, Mailpit restarted, old Docker worker retained stopped. Apps left running; no authenticated UI/provider/load acceptance, product-source/schema/dependency edit, commit or push. [Fresh results and limits](construction/build-and-test/2026-11-06/2026-11-06-jdbc-restart-test-results.md). Security/resiliency scope retained; PBT disabled.

## U05/U09 JDBC transition and project-wide persistence decision — 2026-11-06

User discarded the previous JPA migration and explicitly chose U05/U09 JDBC-only code plus JDBC as the AI-DLC persistence standard for all units. Verified a clean checkout before this task; prior discarded changes were not restored. U05 remains JDBC; U09 evaluation/proposal adapters now use NamedParameterJdbcTemplate with plain JSON row mapping; visibility stays JdbcTemplate. Public interfaces/API/schema/Flyway unchanged; row/advisory locks, explicit version/CAS and caller rollback protected. New scoped architecture rule rejects production JPA/Hibernate in U05/U09. Baseline 209 behavior/other-rule cases passed with JDBC rule RED; the same 210 passed after migration; expanded focused run 446 passed including nine full-migration PostgreSQL cases and shared JpaTransactionManager rollback. Final isolated full Maven verify passed: 1,830 tests/236 suites, zero failures/errors/skips, executable JAR packaged; required PostgreSQL guard 31 + 2 + 10 + 1 passed. Independent review complete: U04 @Version documentation contradiction fixed and explicit no-op save/version characterization added. Standard repackage was blocked by the running JAR; executable artifact is under .tmp/jdbc-transition-20261106/target, and the normal backend/target JAR remains thin until rebuilt when unlocked. 1,008 source/config hashes matched final verification, no new broken documentation links. [Plan](construction/plans/code-generation-plan/2026-11-06-u05-u09-jdbc.md), [policy and remaining unit gaps](construction/persistence-conventions.md), [evidence](construction/build-and-test/2026-11-06/2026-11-06-u05-u09-jdbc-results.md). U04/U13/U14 still JPA pending separate code migrations; global JPA infrastructure retained for them. No frontend/source changes outside this scope, commit/push or live restart/migration. Security/resiliency retained; PBT disabled.

## Local commit delivery — 2026-11-06

User requested one local commit of the verified U05/U09/U15 formatting, behavior-preserving simplification, characterization tests and AI-DLC documentation; explicitly **no push**. Source/config hashes match the successful backend 1,822 and frontend 799 test/lint/typecheck/build verification. This delivery authorization supersedes the earlier no-commit scope; no source change, test rerun, remote write, merge or deployment is included.

## Local backend/frontend restart and tests — 2026-11-06

At the user's request, reran backend verify: **1,822 passed, zero failures/errors/skips**, JAR packaged; required PostgreSQL guard **31 + 2 passed**. Frontend: **799 passed / 116 files**, full lint/typecheck/production build passed; generated config restored from snapshots and typechecked again. Started backend on :8080 with local,worker and frontend dev Webpack on :3000, restarted existing Mailpit and retained the stopped old Docker worker. HTTP health for both services UP/200, login 200 and unauthenticated protected API via frontend 401. Source/config hashes unchanged. [Evidence and limits](construction/build-and-test/2026-11-06/2026-11-06-restart-test-results.md). Processes left running; no dependency/source edit, new live migration, authenticated UI mutation, commit/push or deployment. Existing security/resiliency scope retained; PBT disabled.

## U05/U09/U15 simplification completed — 2026-11-06

Implemented the approved [plan](construction/plans/code-generation-plan/2026-11-06-u05-u09-u15-simplification.md) on the existing formatted checkout: inline U05's single-use mutation helper, share U09's identical batch-size guards and consolidate U15 source-to-section mapping. Exactly seven production/three test files changed versus the pre-refactor snapshot; frontend, contracts and config unchanged. Added 18 characterization cases green before production edits; focused U05/U09/U15 runs pass before and after. Independent read-only review found no actionable findings. Final backend verify: **1,822 passed, zero failures/errors/skips**, executable JAR built; required real PostgreSQL guard **31 + 2 passed**. Initial full attempt failed because Docker Desktop was stopped; started it and reran successfully without source changes. [Evidence and limits](construction/build-and-test/2026-11-06/2026-11-06-u05-u09-u15-simplification-results.md). No commit/push/PR, live migration, app restart or frontend/manual UI/provider rerun. Security/resiliency scope retained; PBT disabled.

## U05/U09/U15 simplification plan — 2026-11-06

Created a [scoped refactor plan](construction/plans/code-generation-plan/2026-11-06-u05-u09-u15-simplification.md) at the user's planning-only request. Source review identified one single-caller U05 mutation helper, three identical U09 batch-size guards, and two identical U15 source-to-section mappings. Plan preserves API/DTOs, transaction/lock/error order, audit/events and dashboard deadlines/cancellation; protects all three changes with existing tests plus characterization only where needed. The prior 71-file formatting work remains the baseline. Implementation and fresh runtime tests are pending review; no source code edited for this plan. Security/resiliency scope retained; PBT remains disabled.

## U05/U09/U15 Java readability formatting — 2026-11-06

Completed the requested formatting in `academics`, `grading`, `admindashboard`: 71 Java files changed, all 85 checked; inline returns 37 to zero. Preserved exact source tokens/comments/literals and existing wildcard imports, with CRLF/LF normalization only for Git baseline comparison. Formatter second run changes zero files, whitespace diff check passes, fresh isolated JDK 17 compilation succeeds for 720 main + 246 test source files. Shared conventions now explicitly separate method bodies from declarations and wrap long calls. [Plan](construction/plans/code-generation-plan/2026-11-06-u05-u09-u15-format.md), [evidence and memory limits](construction/build-and-test/2026-11-06/2026-11-06-u05-u09-u15-format-results.md). Frontend/runtime tests not rerun; no behavior change, commit/push, restart or migration. Existing security/resiliency scope unchanged.

## U05/U09/U15 staging PR and CI delivery — 2026-11-06

User authorized publishing the current `fix/u05-u09-u15` branch as a PR into `staging` and checking CI before any PR merge. Initial remote head `b466400b` matches the clean local checkout; GitHub comparison is ahead by three commits, behind zero, and no open PR exists for this head/base. Reuse the unchanged source's [verified merge results](construction/build-and-test/2026-11-06/2026-11-06-merge-staging-u05-u09-u15.md): backend 1,804 / frontend 799 passing, lint/typecheck/build passing and required real PostgreSQL guard passing. Commit only this delivery record before publishing; no new source changes or local test rerun needed. GitHub PR creation triggers the existing backend/frontend CI jobs; check the final PR head and report its live results. Images/deploy do not run on this PR. No PR merge, auto-merge or deployment authorized; existing security/resiliency scope retained and PBT disabled.

## Merge staging into U05/U09/U15 — 2026-11-06

Resolved the user's existing local merge of `staging` at `b91413ca` into `fix/u05-u09-u15` at `2440cbe4`. Six conflicts resolved by retaining staging behavior and the branch's convention names; all incoming staged changes preserved. Fixed the directly related inherited `@NotNull source` mismatch after reproducing HTTP 400 for single/batch automatic AI requests; added seven HTTP cases and synchronized grading OpenAPI. Independent reviews found no remaining introduced or follow-up issue. Final backend verify **1,804 passed, zero failures/errors/skips**, required PostgreSQL guard **31 + 2 passed**, JAR built. Frontend **799 passed / 116 files**, lint/typecheck/production build passed; generated configuration restored and typechecked again. Deliver through the authorized local merge commit; [plan, evidence and memory constraints](construction/build-and-test/2026-11-06/2026-11-06-merge-staging-u05-u09-u15.md). No push, live migration, application restart or deployment; manual UI/provider acceptance not repeated. Existing security/resiliency scope retained; PBT disabled.

## Wildcard import preference — 2026-11-05

Delivery follow-up: user authorized a descriptive local commit of the convention refactor and compact-import follow-up on the current branch. No push requested; this supersedes the prior no-commit scope. Reuse the recorded full verification and subsequent import-only compilation; no further source changes.

User requested compact imports after the convention refactor. The canonical convention now permits Java wildcard/static wildcard imports, including contract types; keep explicit imports when needed to disambiguate names. Restored original import blocks in 71 changed U05/U09/U15 Java files (883 fewer import statements), retaining all naming changes and all bytes outside those import blocks. Backend source and all 244 test source files compile successfully (`mvn -DskipTests test-compile`); full runtime suites were not repeated for this import-only follow-up. The earlier full test/build results below describe the preceding refactor state. [Follow-up evidence](construction/build-and-test/2026-11-05/2026-11-05-u05-u09-u15-conventions-results.md#wildcard-import-follow-up--2026-11-05). No behavior, public contract, frontend, schema, dependency, security/resiliency scope, commit or push change; PBT remains disabled.

## U05/U09/U15 shared convention refactor — 2026-11-05

Implemented the [convention plan](construction/plans/code-generation-plan/2026-11-05-u05-u09-u15-conventions.md) from `3bfc2fb8` in the existing `fix/u05-u09-u15` checkout. Concrete dependencies identify their service/repository roles; internal actions use verb-object names, predicates/guards have appropriate names, fixed constants use UPPER_SNAKE_CASE, four React modules use descriptive PascalCase and scoped Java wildcard imports are expanded. All affected callers/imports/mocks updated. Preserve public contracts, HTTP/JSON/DB/event/MCP identifiers, hook return keys, authorization, transactions, grade locks/resubmission and rendering. JSON regression protects against new bean fields. Independent review: no Critical/Important defect; its minor naming correction applied. Final backend **1,783 passed, zero failures/errors/skips**, required PostgreSQL guard **31 + 2 passed**, JAR built; frontend **776 passed**, full lint/typecheck/production build passed. Generated configuration restored and typecheck passed again. [Evidence, compatibility exceptions and memory-constrained verification](construction/build-and-test/2026-11-05/2026-11-05-u05-u09-u15-conventions-results.md). No commit/push, migration, live grade write, restart or deployment; manual browser/provider acceptance not repeated. Existing security/resiliency scope retained; PBT disabled. Build/test stage complete; no operations change requested.

## Shared code conventions — 2026-11-05

- [x] Review existing naming guidance and approved proposal; scope is documentation for all units, not a U09 code rename.
- [x] Publish [shared code conventions](construction/code-conventions.md) covering Java/TypeScript names, service dependencies, action methods, files, framework/contract exceptions and refactor verification.
- [x] Link README and CLAUDE.md to the canonical convention document.
- [x] Verify documentation references, relative migration link, Markdown fences and whitespace diff. Runtime/build/tests are not required for this documentation-only change. No source rename, new tooling or contract change; implementation/design stages skipped as unnecessary. Existing extension scope retained; runtime security/resiliency checks N/A.

## U05/U09/U15 follow-up implementation — 2026-11-05

Delivery follow-up: user requested an English commit, push of the existing feature branch and PR into `staging`, with CI verification. Current PR head and remote check results are tracked on GitHub and reported with delivery; no merge/deployment requested. This supersedes the implementation-only no-commit scope below.

Implemented the [follow-up plan](construction/plans/code-generation-plan/2026-11-05-u05-u09-u15-followup.md) in existing `fix/u05-u09-u15`: independent/bounded U15 loading and cancellation, one-student gradebook lookup, typed submission adapter, rubric scoring in the model, and U05 presentation helper/import cleanup. Independent review found one stale-loading P2; regression reproduced and fixed, no other confirmed source finding. Fresh backend **1,782 passed/zero skipped**, required PostgreSQL guard passed; frontend **776 passed**, full lint/TypeScript/production build passed. [Evidence, environment setup and limits](construction/build-and-test/2026-11-05/2026-11-05-u05-u09-u15-followup-results.md). Manual Orca browser acceptance deferred because `orca tab list` could not resolve the executable. Public contracts, grade locks/resubmission, authorization, unit folders and DB schema unchanged. No commit/push/deployment. Existing security/resiliency scope retained; PBT disabled.

## U05/U09/U15 audit remediation — 2026-11-04

Follow-up: on user request, started the existing Frontend :3000 and Backend :8080 Orca terminals; login returned HTTP 200 and backend health UP. Local backend includes its worker; the old Docker worker was stopped to avoid duplicate task consumers. User subsequently requested a descriptive local commit in English; no push/merge/deployment requested. Keep the verified cleanup as one atomic commit because navigation/scoring imports and workspace regression changes share files.

Implemented the [cleanup plan](construction/plans/code-generation-plan/2026-11-04-u05-u09-u15-cleanup.md) on existing `fix/u05-u09-u15` from `45baad9b`: preserve grading navigation filters, guard save-next when the current evaluation leaves its queue, batch/index gradebook reads and load export sources once, explain conflicts by business code, move pure scoring helpers into the feature model, and remove one duplicated U05 permission check. U15 source unchanged; regression included. Full backend 1,771 passed/zero skipped with required PostgreSQL suites, frontend 760 passed, final lint/TypeScript/production build passed. Independent review rechecked its one corrected P2 and found no remaining actionable introduced finding. [Evidence and limits](construction/build-and-test/2026-11-04/2026-11-04-u05-u09-u15-cleanup-results.md). No DB migration, live grade write, application restart, commit or push. Manual browser acceptance deferred because `orca` is unavailable; automated UI/export tests passed. Existing security/resiliency scope retained; PBT disabled.

## U04/U13/U14 fixes and formatting — 2026-11-02

U13 accepts single-video YouTube links with playlist context and stores the canonical URL. U14 exposes role-specific settings for every role; new staff email is opt-in and the Admin backup alert remains required. U04/U13/U14 source/tests were formatted with syntax-equivalence checks, in separate commits from the fixes. The user subsequently authorized merging staging, verification, pushing the feature branch and creating [PR #44 into staging](https://github.com/hygef-v4/AI-Powered-Learning-Platform/pull/44). Merge `1ef38be7` preserves staging's newer YouTube/UI behavior. Fresh local backend: 1,764 passed, zero skipped, including required PostgreSQL suites (31 + 2). Frontend: 743 passed, lint/typecheck/production build passed. Isolated API E2E: 40/40 passed after worker readiness; its Docker project and volumes were removed. The U04 mutation/readback test now awaits its async React updates. [Plan](construction/plans/code-generation-plan/2026-11-02-u04-u13-u14.md), [verification and limits](construction/build-and-test/2026-11-02/2026-11-02-u04-u13-u14-results.md), [live PR checks](https://github.com/hygef-v4/AI-Powered-Learning-Platform/pull/44/checks). CI is checked against the latest PR head before handoff. No PR merge or deployment. Existing security/resiliency scope retained; PBT disabled. Unrelated report DOCX retained outside the commits.

## Dedicated MCP documentation — 2026-11-02

Implemented the requested /ai/mcp-guide and compact Agent MCP help link, available even on credential errors. Four client configurations/English copy prompt reused, SSR origin hydration fixed, 29 provider tool names covered with role/write effects, usage/review/apply/rubric limits and troubleshooting. U06 synced. Fresh AI/MCP tests 39 pass after fixing the single duplicate-copy failure in a full 727-case run (726 other cases passed); TypeScript/focused ESLint/final Next 16.4.3 production build passed. Desktop/mobile light/dark screenshots inspected with zero page overflow; independent review no Critical/Important findings. Synced existing lockfile dependencies and restarted frontend only; generated tsconfig/AGENTS changes restored. No backend/DB changes, live MCP business write, commit or push. [Plan](construction/plans/code-generation-plan/2026-11-02-mcp-documentation-page.md), [evidence and limits](construction/build-and-test/2026-11-02/2026-11-02-mcp-documentation-results.md). Security/resiliency scope retained; PBT disabled.

## Finalized grade lock — 2026-11-02

Latest user decision: "tôi nghĩ để cái cũ đi nộp lại thì bỏ điểm cũ" supersedes retaining prior official grading rounds. Keep manual/AI/member editing locked after FINALIZED/PUBLISHED, but resubmission invalidates previous grades: individual SUPERSEDED, group reset existing root/member to PENDING. Removed the unexecuted group-round migration and snapshot/current-flag changes; no DB schema/index change. Changed-receipt reload resets old local marks/proposal selection. [Decision](inception/requirements/2026-11-02-locked-grades.md), [plan](construction/plans/code-generation-plan/2026-11-02-locked-grades.md), [results](construction/build-and-test/2026-11-02/2026-11-02-locked-grades-results.md). Fresh full backend 1,741 pass/zero failures/errors/skips, required PostgreSQL suites pass; grading frontend 118 pass, TypeScript/scoped lint/production build pass. Reviewer rechecked receipt-reload fix: no remaining Critical/Important findings. Prior 1,743/710 evidence applies to the superseded implementation only. No live migration/restart/commit/push. Security/resiliency scope unchanged; PBT disabled.

## Staging check fixes — 2026-11-02

Findings of the `staging` check, logged as KAN-178 to KAN-182 and fixed on `fix/staging-check-2026-11-02`: `next` 16.4.3; `echarts` and `uuid` overrides under `pptx-preview`; class quiz sidebar without `classId`; audit labels; leftover cleanup. Frontend `npm ci`, lint, typecheck, 700 tests and build passed; `npm audit --omit=dev` found 0 vulnerabilities. Backend code unchanged. Charts with no explicit series color show no bars in `pptx-preview`, with `echarts` 5 and 6 alike. Not pushed or deployed.

## U05/U09/U15 defect remediation — 2026-11-02

Implemented KAN-153–157 on `fix/u05-u09-u15` from clean staging `86b16d92`: preserve partial points/adjustments and AI feedback/review gates; lock manual/restore enrollment targets before subjects; support authorized SUBJECT grading context; restore backup operational metadata and inline paginated history. [Plan](construction/plans/code-generation-plan/2026-11-02-u05-u09-u15-defects.md), [verification and limits](construction/build-and-test/2026-11-02/2026-11-02-u05-u09-u15-defects-results.md). Backend 411 distinct tests/zero skips with real PostgreSQL, frontend 189/189, typecheck/focused lint/final production build passed. Full lint has one unrelated existing warning. Fresh reviewer rechecked the corrected legacy metadata issue, no remaining Critical/Important findings. Actual admin dashboard checked; no live grade write, backend restart, broad role/mobile/load acceptance or full-project rerun. Security/resiliency scope retained; PBT disabled. User requested a local commit after verification; no push/merge/deployment.

## Quiz rework — 2026-11-01

User requested a roadmap and implementation of all seven Quiz items with documentation sync. Recommended options were first applied as defaults; on 2026-11-02 the user decided Question 2 option B and kept the other answers. Implemented on `feat/quiz-rework-2026-11-01`: removed Quiz REVIEWED and quiz material attachment; generated papers by tag rules; duplicate control; teacher proposals with Subject Manager approval; leave signal flags and a configurable suggested penalty that only the teacher applies or dismisses per attempt; one question per page in results. Also rendered the never-mounted attempts slot found during browser testing. [Roadmap](construction/plans/code-generation-plan/2026-11-01-quiz-rework.md), [results and limits](construction/build-and-test/2026-11-01/2026-11-01-quiz-rework-results.md), [assessment](inception/requirements/2026-11-01-quiz-feedback-assessment.md), [questions](inception/requirements/2026-11-01-quiz-rework-questions.md). Backend clean verify 1,712 passed, zero skipped, required suites passed; frontend 677 passed, typecheck and build passed, lint zero errors. API end-to-end 56 checks and the penalty browser flow passed on an isolated, since removed Docker stack. BF-06 PNG not redrawn; real mobile and load not measured. Security/resiliency scope retained; PBT disabled. Merged into `staging` through PR #36; not deployed. End-to-end on the merged code passed, using a fake school AI key on the test database for U06.

## U06/U12 review feedback — 2026-11-02

Triage of the 2026-11-02 review feedback for the units Võ Minh Tài owns (U02, U06, U12); Quiz, Assignment, Grading and materials items belong to U07–U11/U13 owners and were not changed. Implemented as additions outside the frozen business rules (FB-U06-01, FB-U12-01..04 in `construction/plans/code-generation-plan/2026-11-02-feedback-additions.md`): U06 default AI source; U12 leader finalize with a responsible member, section history with restore, DOCX/Draw.io import mapped by section title without AI, peer review; image upload per BR-U12-35. Take-over was added then removed (conflicts with BR-U12-33); the frozen `business-rules.md` files were restored; user guide `docs/guides/user-guide-audit-ai-group.md`. Peer scores are informational; weighting into the final grade (document/peer/presentation) needs U07/U09. Bilingual UI not started (cross-unit decision). [Plan](construction/plans/code-generation-plan/2026-11-02-u06-u12-feedback.md), code summaries U06/U12. Branch `feat/u06-u12-feedback-2026-11-02`.

## Server-managed MCP workflows — 2026-11-01

Implemented full workflow delivery via read_agent_workflow (AUTHOR_ASSIGNMENT/GRADE_WITH_RUBRIC), role-filtered READ access and deterministic content versions. Full source lives in backend classpath resources; local skills are optional wrappers, no user download required. Capabilities/requirements guide discovery; QUIZ/BANK remain separate. Current U06/U07/U09 rules and guides synchronized. [Requirements](inception/requirements/2026-11-01-server-agent-workflows.md), [plan](construction/plans/code-generation-plan/2026-11-01-server-agent-workflows.md), [evidence/limits](construction/build-and-test/2026-11-01/2026-11-01-server-agent-workflows-results.md). Backend focused/architecture: 39 tests passed, zero skipped; hook checks 14 passed; package and required jar resource checks passed. Existing MCP capability call advertises new tool; direct authenticated RPC fetched both complete workflows without local skills. Backend reloaded; clients with stale tool metadata need refresh/new session. No provider call, assignment/grade write, private config installation, commit or push.

## External-agent workflow — 2026-11-01

Implemented the approved agent-first flow: conversation/clarifications in the external agent, exact UI review links, contiguous current-context read enforcement in Redis before MCP grading writes, and optional skill guidance available through capabilities/requirements. No mandatory platform initiation form or session table. Current U06/U07/U09 business rules synchronized. [Requirements](inception/requirements/2026-11-01-external-agent-workflow.md), [plan](construction/plans/code-generation-plan/2026-11-01-external-agent-workflow.md), [results/limits](construction/build-and-test/2026-11-01/2026-11-01-external-agent-workflow-results.md). Backend affected/architecture: 67 passed, zero skipped; frontend affected: 38 passed; build/typecheck passed; lint zero errors/two existing warnings. Real Redis script and existing older proposal link checked through Orca; backend restarted, health UP. No real model quality or all-client acceptance claim; no global install, commit or push.

## MCP quality remediation — 2026-11-01

Latest setup scope: only Codex, Claude Code, OpenCode and Antigravity; user requested removing Gemini CLI. Codex is now the default in both guide and token dialog. Gemini API provider unchanged. Focused 18 tests and typecheck passed; no external config removal.

Setup follow-up: added copyable English agent setup prompt and preview in the shared guide/token dialog, preserving client selection and other servers. Token is included only in the explicit token dialog, with disclosure. Focused 18 tests, typecheck, targeted lint and production build passed; Orca guide checked. No real client setup or secret copy performed.

Implemented strict complete AI proposal validation with legacy replay compatibility, shared API/MCP assignment context and policy, metadata-only discovery, server-sourced rubric limit warnings, four-client application/file-based setup and two repository-local skills with optional Gemini hook adapters. User requested no terminal setup instructions; guide follows that preference. [Plan](construction/plans/code-generation-plan/2026-11-01-mcp-quality-remediation.md), [results and limits](construction/build-and-test/2026-11-01/2026-11-01-mcp-quality-remediation-results.md). Backend 1,630 runtime cases plus 2 separately rerun architecture cases passed; frontend full 644 plus final targeted setup regressions passed. No global installation, provider call, new token, commit or push. Actual teacher-scored quality, authenticated acceptance in all external clients and load latency remain unmeasured. Existing security/resiliency scope retained; PBT disabled.

## Back navigation and page selections — 2026-11-01

Implemented the user's request for Back to restore the actual previous page/tab. Shared history-aware links retain fallback destinations and navigation guards; page tabs and relevant selections/filters live in URL parameters. Next native-history integration verified after correcting the internal-state bypass. Full frontend: 641 tests/96 files passed; build/typecheck passed; lint zero errors/two existing warnings. Orca verified grading → submissions → Back restores tab/student/filter, and same-page tab Back/Forward restores the visible tab. [Plan](construction/plans/code-generation-plan/2026-11-01-back-navigation-state.md), [results/limits](construction/build-and-test/2026-11-01/2026-11-01-back-navigation-state-results.md). No backend/data changes, commit or push.

## Neutral white palette follow-up — 2026-11-01

User found the Slate Blue light surfaces cold and requested a white background matching the supplied reference. Light canvas is now #FAFAFA; sidebar/topbar/cards white; text, borders, sunken surfaces and shadows neutral gray. Blue remains the control/selection accent; dark palette unchanged from the preceding trial. PRODUCT.md updated. Fourteen representative contrast pairs passed (minimum 4.75:1); Orca confirmed the actual canvas/sidebar colors and no horizontal overflow, and the rendered page was inspected. CSS-only follow-up; no new tests or behavior changes. No commit/push.

## Slate Blue palette trial — 2026-11-01

User requested trying Slate Blue after reporting discomfort with the warm theme. Shared light tokens now use cool neutral surfaces, white cards and muted blue accents; dark tokens use slate surfaces and pale blue accents. Removed the oversized shell watermark, softened shadows and aligned the wordmark badge/login seal with the theme. Small brush logo brand colors retained. PRODUCT.md records this as a trial. Production build/typecheck, two shared suites (9 tests) and targeted ESLint passed. Fourteen representative palette contrast pairs passed (minimum 4.67:1); Orca inspected light/dark and confirmed no horizontal overflow at 390 px. No backend/behavior changes, new dependencies, commit or push. Full visual/role coverage not rerun.

## Gradebook and toolbar bug fixes — 2026-11-01

Implemented: historical rubric object envelopes now read alongside current arrays without rewriting stored data; editor tools have visible enabled hover/focus feedback and pointer cursors; assignment header dropdown stacks above the toolbar. [Plan](construction/plans/code-generation-plan/2026-11-01-gradebook-toolbar-fixes.md) and [results/limits](construction/build-and-test/2026-11-01/2026-11-01-gradebook-toolbar-fixes-results.md). Fresh backend assignment/grading: 175 passed/zero skipped; frontend focused: 77 passed; typecheck/targeted lint/build passed. Backend restarted; same gradebook API returns 200, and Orca confirmed grading/progress data and menu visibility. Existing pending changes preserved; no persisted data rewrite, commit or push.

## Assignment distribution visibility — 2026-11-01

Implemented: anonymous grade-distribution visibility moved from class management to the assigned teacher's assignment settings, separately for each class. Defaults off; existing score publication and privacy guards remain. [Execution plan](construction/plans/code-generation-plan/2026-11-01-assignment-distribution-visibility.md) and [fresh verification/limits](construction/build-and-test/2026-11-01/2026-11-01-assignment-distribution-visibility-results.md). Backend: 174 distinct focused tests passed, including actual PostgreSQL races and architecture. Frontend: 615 tests passed; build/typecheck passed; lint zero errors/two existing warnings. Local backend restarted with additive migration; Orca verified saving and persistence, leaving the test preference off. Pending scoped UI changes preserved. Security/resiliency scope retained; PBT disabled. No commit or push.

## Scoped workspace UI — 2026-11-01

Grading follow-up: blank answer space grows to fit the corresponding rubric when desktop side comments are taller. Resize/font/content updates recalculate it; mobile removes the imposed min-height. Focused tests and lint passed; Orca measured matching desktop section/card heights.

Follow-up: widen the shared assignment authoring/grading workspaces as well; document body text is 16 px with 28 px line height, and wide paper uses smaller side padding. Grading now identifies actual students from all pages of the existing submissions API and offers searchable student navigation while preserving unsaved-work confirmation. Related tests: 155/155 pass; build/typecheck and focused ESLint pass. Orca confirmed the student name and search menu at 390 px, and the author paper expands to 950 px plus a 300 px rubric rail at 1600 px, with 16 px body text and no page overflow. No backend changes, commit or push.

After discarding the previous remediation, the user requested only a wider student answer workspace and inline AI grading review. Changes are limited to an opt-in full-width editor and student shell width, plus replacing the grading AI dialog with a page section. Verified: 49 focused tests pass; production build/typecheck and focused ESLint pass. Orca measured 1110 px paper at 1440 px viewport, no student page overflow at 390 px, and inline AI review with zero dialogs at 603 px. No provider calls, backend/data changes, commit or push.

## Font trial — 2026-11-01

At the user's request, Plus Jakarta Sans now replaces Be Vietnam Pro in the frontend as a reversible trial. Six official self-hosted WOFF2 subsets and their OFL license were added; unused Be Vietnam Pro assets were removed. Orca verified Vietnamese normal/bold/italic loading and no document overflow at 390/1280 px in both themes. Frontend production build and lint passed; two existing lint warnings remain. The user subsequently requested a commit; no push was requested.

## Development UI follow-up — 2026-11-01

Disabled the Next.js on-screen development indicator reported on the mobile assignment page. Orca no longer lists the Dev Tools controls; production build passed. Plus Jakarta Sans was researched as an alternative typeface, with the existing user-confirmed Be Vietnam Pro choice retained pending selection.

## UI remediation — 2026-11-01

User requested a plan and implementation for observed UI defects. [Execution plan](inception/plans/2026-11-01-ui-remediation.md) tracks the fixes and checks. Scope is frontend presentation based on the existing visual audit and four screenshots; no backend, schema, or AI provider changes.

Completed: 599 frontend tests pass; lint 0 errors/2 pre-existing warnings; typecheck and production build pass. Orca confirmed dropdown layering, no document-level horizontal scroll at 390 px in the inspected AI tabs, and the add-key dialog fits the 390 × 844 px viewport. Broader visual regression across every role and task-running AI states remains unchecked.

After the dark-mode select report, added theme foreground/background to native select options. Fresh production build and lint passed; the full test suite passed 599/599 with four workers. The default worker configuration timed out once on the subject detail mutation test in two runs; that test passed alone. Orca measured the corrected option CSS colors but cannot capture the native popup surface.

## Current source/documentation baseline — 2026-10-30

- Project origin: Greenfield, started 2026-09-12; current task: package/file moves only, per the latest user clarification; original implementations retained.
- Current source: branch `aidlc-docs`; package/file relocation included in this commit, after documentation commit `11df6307`. Code baseline `5a76e428`; 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Earlier snapshots below remain historical evidence.
- Current scoped stage: move-only scope correction and fresh local Build and Test complete; current Construction documentation synchronized. Earlier remediation/doc sync preserved. Student MCP remains UC41; Admin MCP denied; no deployment. Work executed directly without subagents.
- Source-observed means code exists; full application acceptance, fresh CI/runtime/provider/security/performance/restore effectiveness remain pending.
- [Execution plan](inception/plans/2026-10-30-aidlc-doc-sync-plan.md), [traceability](inception/reverse-engineering/doc-code-traceability.md), [decision register](inception/reverse-engineering/decision-register.md).

## Unit Progress (current source snapshot)

| Unit | Current responsibility | Source | Fresh acceptance |
|---|---|---|---|
| U01 | Account & Access | [source-observed](construction/u01-account-and-access/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U02 | Audit | [source-observed](construction/u02-audit-job-and-outbox/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U03 | Job and Settings | [source-observed](construction/u03-job-and-settings/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U04 | Subject Administration | [source-observed](construction/u04-subject-administration/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U05 | Class & Enrollment | [source-observed](construction/u05-class-enrollment/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U06 | AI Connections | [source-observed](construction/u06-ai-connections/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U07 | Assignment Authoring | [source-observed](construction/u07-assignment-authoring/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U08 | Assignment Work | [source-observed](construction/u08-assignment-work/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U09 | Assignment Grading | [source-observed](construction/u09-assignment-grading/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U10 | Quiz Authoring | [source-observed](construction/u10-quiz-authoring/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U11 | Quiz Practice | [source-observed](construction/u11-quiz-practice/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U12 | Group Assignment | [source-observed](construction/u12-group-assignment/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U13 | Reference Materials | [source-observed](construction/u13-reference-materials/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U14 | Announcements & Notifications | [source-observed](construction/u14-announcements-and-notifications/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U15 | Admin Dashboard | [source-observed](construction/u15-admin-dashboard/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |

## Extension Configuration

| Extension | Enabled | Decided At |
|---|---|---|
| Security Baseline | Yes | Requirements Analysis; scope retained |
| Resiliency Baseline | Yes | Requirements Analysis; scope retained |
| Property-Based Testing | No | Requirements Analysis; unchanged |

Security scope: SECURITY-03/04/05/08/09/12/15. Resiliency scope: RESILIENCY-04/06/10. Other controls N/A outside approved project scope. Automated authorization/privacy/resiliency regressions passed within the approved remediation scope; this does not certify every runtime control. AI API keys are write-only (D15 closed 2026-11-11); only MCP tokens can be shown again.

## Historical lifecycle and approvals

[Full pre-sync state, approvals, dated test counts and pending issues](history/aidlc-state-before-2026-10-30-sync.md). This archive preserves the previous content including date-specific implementation notes; stale unit names/statuses there are historical. Audit remains append-only in [audit.md](audit.md). No historical approval is converted into fresh full-UC acceptance.

## Documentation verification

- [x] Six synchronization tasks complete; code-derived deltas D01–D14 addressed, additional D15 key reveal recorded.
- [x] Current Markdown links, OpenAPI parse/local references/operation IDs/path parameters, draw.io XML/page consistency, 12 refreshed PNG exports and visual inspection.
- [x] Independent read-only review findings corrected; source application/config/DB unchanged.
- [ ] Fresh application/runtime/provider/security/performance/restore acceptance remains pending; no such success claimed.

[Sync report](construction/build-and-test/2026-10-30/2026-10-30-documentation-sync-report.md).

## Rule/use-case/test audit — 2026-10-30

- [x] Read assertions/fixtures and code for all15units, with per-UC evidence/gap rows in three appendices.
- [x] Fresh backend1372pass/99skip; frontend597pass/94files after restoring locked dependencies. No source/manifest/lockfile changes.
- [x] Subsequent remediation ran all backend cases: 1,611 passed/0 skipped; required PostgreSQL guard passed. The earlier 1,372/99 audit run remains historical.
- [x] Approved U03 P1 fixes, false-positive fixtures, scoped missing assertions, current BR/UC drift and Student/Admin MCP correction implemented and verified. Broader acceptance gaps remain explicit in the remediation report.

[Consolidated audit](construction/build-and-test/2026-10-30/2026-10-30-unit-test-business-rule-audit.md). Passing suites do not certify full business-rule/UC acceptance.

## Remediation closure — 2026-10-30

[Results and practical limits](construction/build-and-test/2026-10-30/2026-10-30-remediation-student-mcp-results.md). Backend 1,611 pass/0 skip; frontend 599 pass/94 files; required DB guard, lint/typecheck/build passed. BF-06/BF-08 exports inspected. Migration tested in disposable databases only. No commit/push/live migration/deploy.

## Code structure review and refactor proposal — 2026-10-30

- User requested a whole-project review with `ponytail-audit` and `clean-code`, including AI-DLC, and a simple refactor plan. Scope is review/planning, not implementation.
- Source reviewed at HEAD `5a76e428`; working tree initially clean. [Structure audit](inception/reverse-engineering/2026-10-30-code-structure-audit.md) and [proposed refactor plan](inception/plans/2026-10-30-code-structure-refactor-plan.md) prepared for review. Preserve feature/unit ownership; normalize internal controller/service/repository/model and frontend API/model/components/pages/hooks.
- Fresh frontend: 599 passed across 94 files; lint exit 0 with two existing warnings; typecheck passed. Focused backend architecture run did not reach tests because the workspace offline dependency cache was incomplete. Earlier 1,611-pass evidence remains historical, not a fresh result of this review.
- No application/test/config/dependency/DB changes; no commit/push/deploy. Security/resiliency scope retained; PBT disabled. Refactor execution is proposed and pending review; no construction approval is inferred from “Làm tiếp” during the review.

## Withdrawn broader refactor snapshot — 2026-10-30

This section records the superseded working-tree implementation. All non-move edits below were undone after the latest user clarification; use the move-only result at the end of this document.

- Explicit approval: “Giúp tôi refactor lại dự án theo kế hoạch”. Executed directly without subagents in the existing checkout, preserving prior documentation work.
- All 15 backend units and 18 frontend features now follow the approved structure. Admin-only unreachable MCP implementations removed; architecture guard hardened; U05/document/editor/AI-draft responsibilities separated; U05 list lookups batched; frontend public imports stabilized.
- Fresh final backend `mvn clean verify`: 1,614 passed, 0 failures/errors/skips; required PostgreSQL guard passed. Frontend: 601 passed/94 files; lint/typecheck/production build passed. Public unit contracts, dependency versions/lockfiles, migrations and infrastructure config unchanged.
- Current source inventory, all 15 Construction as-built/logical-component documents, services and test guidance synchronized. Earlier reports remain dated evidence, not a fresh acceptance claim.
- [Results and limits](construction/build-and-test/2026-10-30/2026-10-30-code-structure-refactor-results.md); [execution ledger](construction/plans/code-generation-plan/2026-10-30-code-structure-refactor-progress.md). No commit, push, deployment or live migration. Browser/provider/load/security/restore acceptance remains unverified.

## Current move-only result — 2026-10-30

User clarified that only moving files/packages is wanted. All original 1,501 source/resource files are retained; 1,066 relocated, with necessary package/import/path updates. Broader splits, cleanup, optimizations, new public barrels and extra tests were undone. Original class/test bodies are retained, with only ArchitectureTest layer names and required-suite FQNs adapted.

Fresh backend clean verify: 1,611 passed/0 skipped, required PostgreSQL guard passed. Frontend: 599 passed/94 files; typecheck/build passed; lint exit 0 with two existing unused-import warnings. No staged changes, commit, push, history rewrite or deployment.

[Current results and limits](construction/build-and-test/2026-10-30/2026-10-30-package-moves-only-results.md). Security/resiliency scope unchanged; PBT remains disabled. Existing full runtime/provider/E2E acceptance gaps remain outside this move-only change.

## Fresh verification after git mv — 2026-10-30 17:11 +07:00

HEAD `11df6307` contains documentation only; code remains staged as 1,066 renames and 86 reference updates. Fresh backend clean verify: 1,611 tests passed, 0 failures/errors/skips, JAR built; required PostgreSQL suites passed. Frontend: 599 tests passed/94 files, production build/typecheck passed; lint exit 0 with two existing unused-import warnings. Logs: `.tmp/2026-10-30-after-gitmv-*.log`. No source fix, commit, push or deploy. Existing browser/provider/load/restore acceptance limits remain unchanged.

## Commit authorization — 2026-10-30 17:13 +07:00

User requested “giúp tôi commit”. Include the verified file/package relocation and latest verification/audit notes in this commit. Pre-commit comparison confirms all 1,501 original source/resource files retained, 908 Java bodies and 506 TypeScript/TSX ASTs unchanged except necessary package/import/reference updates. Backend/frontend source matches the tested tree; test/build evidence above applies. No push, deployment or history rewrite.

## U09 scoped structure cleanup — 2026-11-06

**Subsequent full verification requested and completed:** final isolated Maven verify passed 1,830 tests with zero failures/errors/skips and packaged the executable JAR; all four required PostgreSQL suites passed. Frontend 799 tests/116 files, full lint, production build and typecheck passed. [Fresh results, environment corrections and limits](construction/build-and-test/2026-11-06/2026-11-06-u09-structure-full-verification.md). No application/config/dependency changes from verification; temporary backend POM removed. No commit, push, restart or deployment.

- [x] User approved two concrete review recommendations with “ok giúp tôi sửa”; implemented directly without subagents.
- [x] Moved frontend error formatter/test into model; updated all six external consumers. Renamed two concrete JDBC repositories without Adapter suffix; updated integration wiring and current documentation.
- [x] Fresh frontend 134 tests/16 files, typecheck and scoped lint passed. Backend compiled all main/test sources; final repository/PostgreSQL/architecture run passed 19 tests with zero skips after starting Docker Desktop.
- [x] Exact move/rename comparisons and whitespace check passed; no business logic, SQL, contracts, dependencies or schema changes.

[Execution and verification details](construction/plans/code-generation-plan/2026-11-06-u09-structure-cleanup.md). No full-suite/production-build/manual acceptance rerun, application restart, commit, push or deployment; Docker Desktop remains running. Security/resiliency scope retained; PBT disabled.

## Bug remediation and doc sync — 2026-11-01

Commit `1cd58c02` on local branch `fix/review-bugs-2026-11-01` fixes 26 review bugs plus the DOCX preview CSP issue. Business rules, frontend components, API inventory, contracts and decision register were synchronized to the new behavior; summary in [review-bugs remediation](construction/remediation/review-bugs-2026-11-01/README.md). Frontend lint/typecheck/605 tests/build passed; backend compiled and unit-tested without Maven (environment-caused failures only). `mvn verify` and Testcontainers suites not run; nothing pushed.

## Excel import / YouTube playback — 2026-11-09

- [x] Workspace Detection: brownfield; baseline/source inspected; initial tree clean.
- [x] Requirements: scoped document created; user requested continuation with “continue”.
- [x] Workflow Planning: [execution plan and approval](inception/plans/2026-11-09-excel-youtube-execution-plan.md).
- [ ] Plan approval before application changes.
- [ ] Implementation and fresh verification.

CSV-only import confirmed; YouTube runtime cause unconfirmed. No application changes or fresh tests. Security/Resiliency scope retained; disabled PBT skipped.

## Excel imports — implementation, 2026-11-09

- [x] User approved the plan with “a”; later explicitly canceled YouTube work and requested every Admin/Manager CSV import use Excel.
- [x] U01 account import: POI XLSX reader with shared validation, preview/commit, limits and legacy CSV API.
- [x] U05 enrollment: XLSX email reader, existing text API and server checks, Excel template.
- [x] U10 question import: Excel-only picker/labels and real XLSX template; existing parser retained.
- [x] Fresh frontend 814 tests/119 files, related backend 39 tests/0 skipped, lint and production build passed. Standalone typecheck pending final confirmation after stale generated receipt types were regenerated by build.
- [ ] Final diff, typecheck and result report.

No YouTube/CSP changes, migration, dependency addition, commit, push or deployment. Security/Resiliency scope retained; disabled PBT skipped.

## Excel imports — final verification, 2026-11-09

- [x] Standalone typecheck passed after production build regenerated stale Next types.
- [x] Final related backend 39/0 skipped; frontend 814/119 files; full lint and production build passed.
- [x] Diff reviewed and documentation synchronized; [results and limits](construction/build-and-test/2026-11-09/2026-11-09-excel-import-results.md).

Approved Excel scope complete. Live browser import, full backend/DB/performance and deployment remain outside the completed verification. Backend not restarted; no commit/push/deploy. YouTube work canceled by user.

## Materials and quiz UI proposal — 2026-11-09

- [x] Read-only scoped code/DOM/API audit of materials, quiz authoring, AI material selection and draft dispatch.
- [x] Prepared [UI proposal and findings](inception/requirements/2026-11-09-material-quiz-ui-proposal.md).
- [ ] User chooses Disabled behavior and approves design before implementation planning.

Current live quiz has three YouTube sources without text and no AI drafts; no provider failure claimed. No application/test/config changes from this proposal. Existing Excel diff retained. No task creation, production writes, test/build run or deployment in this scope.

## Materials / inline quiz authoring — completed, 2026-11-09

- [x] User approved implementation; default Disabled behavior adopted. Final visible status copy is Vietnamese Bật/Tắt.
- [x] Material visibility plus direct/token-redemption access, formatted material/quiz instructions and student view; JDBC touched repository with two safe migrations.
- [x] Inline manual/bank/AI authoring, source dropdown, persistent truncation warnings, empty-source rejection, retry/input/navigation protection.
- [x] Four Important edit-loss review fixes and list marker gutter follow-up corrected.
- [x] Backend clean533/0skip plus overlapping focused104/119/10 and required guard passed; backend JAR packaged.
- [x] Frontend836/124files, full lint/build/typecheck passed. Gutter final82/10files+build/typecheck; Vietnamese copy final55/8files passed.
- [x] [Final results and applying migrations](construction/build-and-test/2026-11-09/2026-11-09-material-quiz-ui-results.md), current contracts/docs and plan checks synchronized.

Manual UI/mobile/provider acceptance remains with user by explicit request. No live migration, application restart, commit, push or deployment. Existing Excel diff retained. Security/Resiliency scope retained; PBT disabled. Backend update needs Flyway material2200/quiz2201 before testing full behavior on running app.

## Quiz AI HTTP source follow-up — 2026-11-09

- [x] Fixed stale mandatory source validation in shared quiz/bank HTTP parser; absent/null source reaches existing automatic selection.
- [x] Regression red reproduced; backend50/0skip and JAR packaging passed; frontend payload2 passed.
- [x] [Fix evidence and limits](construction/build-and-test/2026-11-09/2026-11-09-quiz-http-auto-source-fix.md).

No additional migration. Restart backend using updated artifact for user acceptance. Live provider/UI acceptance remains unperformed. Previous changes and extension configuration retained.

## Live AI worker diagnosis — 2026-11-09

- [x] Matched failed quiz task to personal Gemini model and persisted HTTP503; current connection probe passed later.
- [x] Found duplicate older Docker worker and stopped only that container; current local,worker backend remains active with four AI consumers and empty queue.
- [x] [Runtime evidence and remaining provider limit](construction/build-and-test/2026-11-09/2026-11-09-ai-worker-runtime-diagnosis.md).

No source/config/schema change or real AI retry submitted. Previous FAILED draft remains unchanged; user can submit a new request. Upstream provider availability remains unverified.

## Current-change documentation reconciliation — 2026-11-09

- [x] Compared implementation with requirements/stories, U01/U05/U10/U13 current behavior and U03/U06/U11 cross-unit contracts.
- [x] Synchronized19 Markdown files;22 added local links resolve and scoped diff whitespace passed. Historical evidence retained.
- [x] [Document map and verification limits](construction/build-and-test/2026-11-09/2026-11-09-aidlc-doc-sync.md).

Documentation-only; no application test rerun or new code/schema/config/runtime change. Manual UI and successful real AI generation remain unverified. Material/version JDBC conversion does not include module persistence. Security/Resiliency scope retained; PBT disabled.

## AI draft source label fix — 2026-11-09

- [x] AUTO/null provenance now displays Hệ thống tự chọn nguồn AI; PERSONAL without provider displays API cá nhân; SHARED/MCP/provider labels retained.
- [x] Regression red3 cases, green37tests/2files; scoped ESLint and whitespace passed. U10 frontend documentation synchronized.

No backend restart/migration/provider call or manual UI test. Source selection behavior unchanged.

## AI quiz review follow-up — 2026-11-09

- [x] Xem/Thu gọn reflects actual open draft; unsaved edits guarded.
- [x] Optional prompt with selected source documents, server default instructions and same-call relevance warning persisted.
- [x] Chấp nhận tất cả uses current edited/nondropped questions, validation, busy lock and existing versioned accept API.
- [x] Frontend66/5files, scoped lint/typecheck; backend98/5suites0skip and JAR package passed; affected docs synchronized.
- [x] [Evidence and applying](construction/build-and-test/2026-11-09/2026-11-09-ai-quiz-review-followup.md).

Restart current backend/worker for new behavior; no additional migration. No live provider/UI acceptance. Old drafts not reassessed; model semantic relevance can be wrong. Prior source-label fix and earlier changes retained.

## Admin subject-list runtime recovery — 2026-11-09

- [x] Read Frontend terminal in Orca as requested, checked backend/metadata and separated proxy/CSP/session evidence.
- [x] Added server-only unexpected-U04 exception logging; existing error/API/auth behavior retained. Controller/service54 and isolated PostgreSQL31 passed; frontend27/3files passed.
- [x] Restarted current local API/worker after zero-running-AI check; health200. User confirmed “Đã tải được môn học”.
- [x] [Evidence and limits](construction/build-and-test/2026-11-09/2026-11-09-admin-subjects-runtime-diagnosis.md).

No proven original500 root cause, CSP relaxation, data/migration/auth change or browser UI test. Runtime recovery is confirmed by user; diagnostic logging retained for recurrence. Security/Resiliency retained; PBT disabled.

## AI source grounding enforcement — 2026-11-10

- [x] Mismatch output no longer becomes READY; empty/PENDING plus warning, one source-only retry, second mismatch fails closed.
- [x] Current rights/version/source/text/quota, row lock, atomic job/task metadata, rollback-safe failure, after-commit and owner-only recovery verified.
- [x] Final backend103/6suites0skip/package; frontend44/2files; scoped diff and29 added links passed.
- [x] U10/current stories synchronized; [results and limits](construction/build-and-test/2026-11-10/2026-11-10-ai-source-grounding-fix.md).
- [x] Gracefully restarted current local API/worker after zero-running-task check; PID55012, health200.

No additional migration or real provider/UI acceptance. Extra retry costs at most one additional task; AI semantic checks are fallible. Old incorrect READY proposals remain historical and must be discarded/recreated. No commit/push/deploy.

## Local commit delivery — 2026-11-10

- [x] User accepted current behavior and requested clear commits.
- [x] 5d628228: Excel account/enrollment/question imports and real XLSX templates.
- [x] b1729529: material visibility/formatting/JDBC and inline quiz/AI source grounding, safeguards and tests.
- [x] 0df689df: subject server error diagnostics, generic client response retained.
- [x] Final AI-DLC documentation commit carries scoped plans, requirements, evidence and this receipt.
- [x] Fresh backend239/19suites0skip plus separate subject54/2suites0skip; full frontend849/124files, full lint and typecheck passed.

Logs .tmp/commit-final-backend-tests.log, commit-final-subject-tests.log, commit-final-frontend-tests.log, commit-final-frontend-lint.log and commit-final-frontend-typecheck.log. No source/runtime change, push, PR or deployment in this delivery turn; no new real-provider/UI acceptance. Focused backend scope only. Security/Resiliency retained; PBT disabled.

## PR preparation — 2026-11-10

User requested a PR before merge into staging. Source headfb8846ad has four feature/documentation commits; current origin/staginga35d9854 is5commits ahead and feature4ahead. Merge preview clean; no existing open head/base PR. Publish the current branch via normal fast-forward push and create PR targeting staging; do not merge or enable auto-merge. Only this preparation receipt changes after verified source: backend239 plus subject54, frontend849/124files, lint/typecheck passed. GitHub CI provides merged-tree validation. Live PR URL/status is reported separately from GitHub; no deployment is triggered by this PR under current CI configuration. Security/Resiliency retained; PBT disabled.

## PR59 conflict resolution / merge authorization — 2026-11-10

User explicitly requested resolving PR59 and merging into staging. Latest staging9a07e1e introduces U08 feedback/U12 changes. Only audit.md had a textual conflict; preserve both histories, do not drop either side. Application changes merged automatically. Refresh checks on the resolution head and merge with exact-head protection only after CI passes; preserve feature commits/branch and do not deploy production. Previous PR-only authorization is superseded for this merge. Security/Resiliency retained; PBT disabled.


## Grading remediation continuation — 2026-11-10

- [x] Restored the 37-file Antigravity stash; completed the five-wave scope sequentially, without subagents.
- [x] Official group-grade preservation, member history/transfer/adjustments (including legacy data), AI validation, group evidence/freshness, rubric/deadline/quiz fixes, query batching, student feedback, batch controls and bounded polling.
- [x] Requirements/stories, affected unit docs, OpenAPI and the original/continuation plans synchronized.
- [x] Final backend 2,095 tests with zero failures/errors/skips and executable JAR; required five-suite/57-case guard passed. Frontend 870 tests/127 files, full lint/typecheck/build passed. Source snapshot unchanged.

[Results, evidence and applying migrations](construction/build-and-test/2026-11-10/2026-11-10-grading-remediation-results.md). Original stash retained; changes unstaged, no commit/push/deploy. Existing browser was at login; no authenticated UI/mobile/provider acceptance or live grade write. No running API/worker restart or live migration. Update the local backend through the existing Flyway startup process to apply the two new migrations. Security/Resiliency retained; PBT disabled.


## Grading UI screenshot follow-up — 2026-11-10

- [x] Measured toolbar-relative score/comment offset, readable auto-growing feedback, clean evidence links, dated successful proposal cards; latest failure notice retained outside history. Internal diagnostics preserved.
- [x] Full frontend873/127files, lint/typecheck/build; desktop/mobile real-component fixture inspected. No authenticated session/provider/live grade write or backend change/restart.

[Results and acceptance](construction/build-and-test/2026-11-10/2026-11-10-grading-ui-feedback-polish.md). Existing work retained, no subagents/commit/push. Security/Resiliency retained; PBT disabled.


## AI grading action and reported rejection — 2026-11-10

- [x] Larger36px AI grading action with permanent label; narrow read-only toolbar wraps. Regression206/20files, scoped lint, typecheck/build and synthetic desktop/mobile fixture passed.
- [x] Read-only runtime diagnosis: two Gemini tasks READY then grading REJECTED. Output deliberately cleared after handling; exact invalid field remains unproven. No backend fix or successful new provider run claimed.

[Evidence and limits](construction/build-and-test/2026-11-10/2026-11-10-grading-ai-button-and-error.md). No subagents/live grade/provider retry/application restart/commit/push. Security/Resiliency retained; PBT disabled.


## AI grading rejection fixed — 2026-11-10

- [x] Reproduced zero-score/full-credit-check contradiction with controlled Gemini request; provider output now omits redundant checks and existing normalization derives them. Full MCP schema/strict validation retained.
- [x] Red/green regression, connected210/38classes0skip, final14 regression, Maven verify/JAR and live Gemini READY with16 feedback items passed. Local API/worker refreshed; grade fingerprint/version unchanged in final verification.
- [x] Opened new proposal in a separate authenticated Chrome tab; original tab preserved. Two provider calls; no grade apply/save/finalize/publish, migration, commit/push or subagents.

[Results and limits](construction/build-and-test/2026-11-10/2026-11-10-grading-ai-rejection-fix.md). Other models were not tested live; whole-backend suite not repeated. Security/Resiliency retained; PBT disabled.


## Grading feedback formatting — 2026-11-10

- [x] General paragraph action/readable type; strengths/improvements as separate editable bulleted ideas with retained newlines and add/remove/limit controls. Blank draft rows excluded only at save.
- [x] Grading/document210/20files, final grading154/17files, scoped lint, typecheck/build and synthetic desktop/mobile inspection passed.

[Evidence and acceptance](construction/build-and-test/2026-11-10/2026-11-10-grading-feedback-format.md). No live grade save, provider call, backend/runtime change, commit/push or subagents. Security/Resiliency retained; PBT disabled.


## Repeated grade actions — 2026-11-10

- [x] Shared state eligibility for both batch selectors; readable completed/race feedback and safe draft-only publish fallback. Backend guards retained.
- [x] Grading167/18files, scoped lint, typecheck/build; authenticated gradebook shows published student with all batch actions disabled.

[Results and limits](construction/build-and-test/2026-11-10/2026-11-10-grading-repeat-actions.md). No live finalize/publish, backend/runtime change, migration, commit/push or subagents. Security/Resiliency retained; PBT disabled.


## Grading local commit delivery — 2026-11-10

- [x] User requested clear local commits, explicitly no push. Five groups: rubric/deadline, quiz/activity, grading/group/AI backend, grading UI, then AI-DLC records.
- [x] Fresh full backend2096 tests0skip/JAR and five-suite/57-case guard; full frontend891/128files, lint/typecheck passed. Source unchanged during packaging.
- [x] Source commits51428543,66af2cd5,27ec9f0a,8f0ac9ea; this final documentation commit completes the series. Original Antigravity stash retained.

[Commit receipt and limits](construction/build-and-test/2026-11-10/2026-11-10-grading-local-commits.md). No push, GitHub CI, deployment, runtime restart, provider call, live grade write or subagents in this turn. Security/Resiliency retained; PBT disabled.


## Staging merge resolution — 2026-11-10

- [x] Resolve the existing merge of staging57b0bb8d, retaining both audit histories exactly.
- [x] Align the published-feedback test fixture with the new deadline-gated history behavior.
- [x] Verify the merged source and complete the authorized local merge delivery; no push. Full backend verify2110 tests0fail0error0skip/JAR; frontend908 tests/129files, full lint/typecheck passed.

Existing Security/Resiliency scope retained; PBT disabled. No production behavior changed by manual resolution. No UI/provider acceptance, frontend production build, deployment, live migration or application restart in this merge task.


## Authorized staging-to-main release — 2026-11-10

User authorized publishing a staging-to-main PR and merging after CI passes. Application source is the verified7fbf5703 tree; this checkpoint only records authorization. The GitHub PR/workflow and final chat response carry the release outcome without adding another source commit after verification. Existing Security/Resiliency scope retained; PBT disabled.

## Gradebook export naming — 2026-11-10

- [x] Scoped workspace/export review and requirements documented in inception/requirements/2026-11-10-gradebook-export.md.
- [x] User approved implementation; safe assignment filenames shared by API and UI. Backend14 tests, frontend183 tests plus final35-case subset, typecheck/scoped lint passed. Evidence: construction/build-and-test/2026-11-10/2026-11-10-gradebook-export.md. No live Excel/browser acceptance, full suite/build, restart, deployment or commit/push. Security/Resiliency retained; PBT disabled.
- [x] Screenshot follow-up: Vietnamese headers/statuses/late flags/time/sheet, active-assignment export default, single-assignment title and ID-free aggregate filenames. Backend15/15, frontend35/35, typecheck/lint passed. Local API/worker refreshed; two pending staging migrations applied on startup. Real local browser downloads and XLSX XML assertions passed for assignment5rows and aggregate20rows/4assignments. User's original tab retained. No VPS release, grade write, commit/push or subagents; current evidence/limits in the same report.

## Diagram-only AI grading consistency — 2026-11-10

- [x] Reproduced erroneous empty detection for real compressed Draw.io pages; grading now reuses the shared bounded document parser. Live heading/rubric snapshot mapping matches2+4+4=10.
- [x] Full backend2116 tests/253suites0fail0error0skip/JAR and required guard passed. One successful live Gemini retry gives READY8.50 with all3 sections recognized; persisted grade remains DRAFT0.00/version1 and submission hash unchanged.

Evidence: construction/build-and-test/2026-11-10/2026-11-10-grading-diagram-parity.md. No grade save/finalize/publish, assignment/submission edit, migration, VPS update, commit/push or subagents. Qualitative scoring still needs teacher review. Existing Security/Resiliency retained; PBT disabled.

## Accurate Vietnamese AI errors — 2026-11-10

- [x] Translate actual grading validation failures and old proposal messages without exposing uppercase codes; preserve precise provider reasons and internal server diagnostics.
- [x] Recognize explicit token-truncation metadata for3 providers with charged usage retained; distinguish known quota/rate errors without guessing from429 alone.
- [x] Final343 connected tests0fail0error0skip and authenticated browser inspection pass. Local API/worker refreshed; original browser form retained and saved grade/version/hash unchanged. No paid model request, grade write, migration, commit/push or VPS update.

Evidence and limits: construction/build-and-test/2026-11-10/2026-11-10-grading-ai-errors.md. Existing diagram fix retained. Security/Resiliency retained; PBT disabled.


## Deterministic AI grading totals — 2026-11-10

- [x] Provider schema/prompt delegate totals to existing backend calculation; complete MCP contract and strict item/check/evidence validation retained.
- [x] Final349 connected tests0fail0error0skip; local API/worker refreshed, health200. One live proposal READY7.50 matches12 item scores and section totals2+3+2.5 in storage/UI. Saved grade/state/version/hash unchanged.

Evidence: construction/build-and-test/2026-11-10/2026-11-10-grading-derived-total.md. Original tab retained; separate test tab shows unsaved proposal. No official grade action, commit/push or VPS update. Other providers not tested live; full suite/JAR not repeated. Security/Resiliency retained; PBT disabled.


## Compact optional manual grading feedback — 2026-11-10

- [x] Remove grade-history/paragraph-split/whole-feedback-clear UI; optional change note collapsed. Manual strengths/improvements and per-item comments/evidence available without AI; blank manual fields accepted, AI completeness/review checks retained.
- [x] Inline required adjustment reason and readable fallback; autosave pauses after errors and resumes on edits.
- [x] Final173 frontend/204 backend tests, TypeScript/scoped lint and45 final-polish cases pass. Local API/worker refreshed and health200; DOM checks pass. Browser-test accidental local draft mutation restored to exact7.40 score/non-audit contents via normal saves; version/history correction retained, no finalize/publish.

Evidence and limits: construction/build-and-test/2026-11-10/2026-11-10-manual-feedback-parity.md. No commit/push, VPS release, migration, provider call or subagents. Full build/suites/mobile visual verification not repeated; screenshot follow-ups timed out. Security/Resiliency retained; PBT disabled.


## Student assignment context columns — 2026-11-10

- [x] Class and subject codes shown across3 tabs; fixed widths/overflow and correct empty/loading spans. Existing filtering/navigation retained.
- [x] Assignment-work23 tests plus final4 page cases, TypeScript/scoped lint and whitespace pass. No API/data change, account switch, commit/push or deployment. Prior grading work preserved.

Evidence: construction/build-and-test/2026-11-10/2026-11-10-student-assignment-context.md. Live student/mobile visual acceptance and full build not repeated. Security/Resiliency retained; PBT disabled.


## Quiet30-second grading autosave and one optional note — 2026-11-10

- [x] Mock diagnosis confirms the old1500ms global busy transition replaced score editors without an actual evaluation reload.30-second idle background save retains inputs/focus; view-only renders do not reset the timer.
- [x] Single-flight/revision/version handling preserves newer edits; explicit Save/Ctrl+S, error pause and group/stale/locked-grade guards retained. One optional change note replaces required adjustment reason, with user-authorized domain rule update and legacy-note preservation.
- [x] Final176 frontend/206 backend cases, final13 hook cases, TypeScript/scoped lint/whitespace pass. Local API/worker refreshed, health200. All grade mutation tests mocked; no live grade edits, commit/push or VPS deploy.

Evidence: construction/build-and-test/2026-11-10/2026-11-10-grading-quiet-autosave.md. Prior pending changes retained. Full build/real multi-session/mobile visual checks not repeated. Security/Resiliency retained; PBT disabled.

## Student published result entry — 2026-11-10

- [x] Confirmed local published8/10 is hidden behind a newer draft plus pre-deadline history filtering. Detail now retains submitted result and a published-only detail link; read-only viewer admits current grading candidate before deadline without older history/drafts. Existing backend ownership/publication controls retained.
- [x] Connected26 frontend/9 GradebookService cases and final13 affected page cases pass; TypeScript/scoped lint/whitespace pass. No live grade writes, student-account switch, subagents, commit/push or deployment. Prior pending changes retained.

Evidence: construction/build-and-test/2026-11-10/2026-11-10-student-published-result-entry.md. Student live visual acceptance/full build not performed. Security/Resiliency retained; PBT disabled.

## Local commits and fresh verification — 2026-11-10

- [x] ee0a3838: compact optional manual feedback, unified optional note and quiet30-second autosave with edit/version preservation.
- [x]23f9309a: student assignment class/subject columns and regression coverage.
- [x]4873b931: student published result and detail access alongside newer drafts, preserving private-grade controls.
- [x] Fresh202 frontend/206 backend architecture cases, TypeScript/scoped lint/whitespace checks pass; cumulative audit/state recorded separately. No push/deploy, live grade mutation or subagents. Full production build/live student visual acceptance not repeated.

## Editable published grades and background AI — implemented, 2026-11-11

- [x] User selects immediate student-visible corrections after every successful save, and editing until class ARCHIVED including past assignment deadlines. No separate published snapshot or republish step.
- [x] Latest AI clarification supersedes the earlier wait-on-screen wording: background task must not block grading/navigation. Reviewed model/save/publication, group and proposal callback guards and current busy/modal/auto-apply behavior on2444c3f4.
- [x] User approved the six-task plan with `$ponytail:ponytail triển khai theo kế hoạch`, including explicit Apply/notification. Implemented direct publication/legacy FINALIZED compatibility, preserving PUBLISHED after saves, AI review safeguards, current receipt/version rules and background result isolation/recovery. Removed finalize buttons; retained quiet30-second autosave.
- [x] Full Maven verify2146 tests with no failures/errors/skips and required integration/traced-suite guard pass. Full frontend956 tests/134 files pass; final connected211 tests/25 files and workspace39 tests pass after subsequent additions (overlapping counts). TypeScript, scoped ESLint, production frontend build and whitespace pass. No live grade edits, paid model calls, subagents, commit/push or deploy.
- [x] User restarted the local backend; direct Orca UI acceptance passed for published corrections8→8.25, student visibility/detail access, background AI across navigation, concurrent manual edits preserved when AI completes, explicit Apply without automatic saving and quiet30-second autosave without reload. Restored saved8.00 and original feedback. New AI proposals remain in history. Archived-class/group UI acceptance was not performed; automated coverage remains. The earlier rejected restart command stopped/restarted no process.

Plan: construction/plans/code-generation-plan/2026-11-11-editable-grades-and-background-ai.md. Evidence: construction/build-and-test/2026-11-11/2026-11-11-editable-grades-and-background-ai.md. Security/Resiliency compliant for existing scope; PBT disabled. Existing U12 preservation of official group grades on resubmission remains, with stale-receipt edits denied. Student tabs see saved corrections on load/refetch; no realtime push added.

User subsequently authorized descriptive local commits. Source/tests and documentation recorded separately; fresh precommit frontend grading/assignment-work run passed211 tests/25 files. No push or deployment.

## Ba lỗi UI/UX staging — 2026-11-11

Người dùng duyệt "sửa 3 lỗi trên". Baseline origin/staging ce40e447; thực hiện tại worktree riêng `.claude/worktrees/ui-ux-staging`, nhánh `codex/ui-ux-staging`.

- [x] Workspace Detection, yêu cầu và kế hoạch sửa giới hạn; dùng lại brownfield context.
- [x] Pagination/PageNav xuống dòng; Switch có accessible name từ nhãn; Field nối lỗi tới control và giữ focus khi lỗi thay đổi.
- [x] 332 test/51 file, TypeScript, ESLint, diff check qua. Production build exit 0, có cảnh báo font bị chặn và 27 standalone traced files ở ngoài root do junction dependency.
- [x] Ghi giới hạn: chưa kiểm tra layout trên browser/mobile, chưa xác nhận standalone deployment; không commit/push/deploy hay thay checkout gốc.

[Kế hoạch](construction/plans/code-generation-plan/2026-11-11-three-ui-fixes.md) và [kết quả](construction/build-and-test/2026-11-11/2026-11-11-three-ui-fixes.md). Security/Resiliency giữ phạm vi đã duyệt, PBT disabled. Các stage thiết kế mới/stories/hạ tầng/operations không cần cho ba lỗi hiện hữu.

### Staging readiness — ba lỗi UI/UX, 2026-11-11

- [x] Fetch origin/staging vẫn cùng ce40e447; review không thấy blocker.
- [x] Full frontend 963 test/135 file qua; Docker build sạch cảnh báo font/tracing và image smoke test health UP/login/icon 200.
- [x] Đủ điều kiện commit/tạo PR staging; merge sau CI PR. Chưa commit/push/merge; chưa xác minh browser/mobile thực. Chi tiết trong báo cáo ba lỗi UI/UX ở trên.

### Cho phép tạo PR — ba lỗi UI/UX, 2026-11-11

Người dùng yêu cầu "tạo pr vào staging cho tôi". Cho phép commit/push nhánh `codex/ui-ux-staging` và tạo PR base `staging`; không bao gồm merge. Source là bản đã qua 963 test/135 file và Docker smoke. Trạng thái phát hành cuối theo PR GitHub và phản hồi giao hàng.

## Teacher class overview — 2026-11-11

- [x] User approved replacing submission-progress tabs/recent announcements on teacher class info with compact read-only class facts, three assignment summary links and nearest3 upcoming deadlines. Student class info and announcement routes preserved. Reused assignment list/submission summaries; no full gradebook load, new API or schedule scope.
- [x] RED/GREEN:264 related tests/31 files pass; final targeted34/2 pass after link fix. TypeScript, scoped ESLint and whitespace pass. Local browser verified1440px/390px, light/dark, correct counts and actionable filtered gradebook link. No class/grade writes. Security/Resiliency retained; PBT disabled.
- Limits: numbers refresh on page reload; summary links select the first assignment with matching work. Production build/backend suite not repeated for this frontend-only change; no commit/push/deploy. Plan/evidence: construction/plans/code-generation-plan/2026-11-11-teacher-class-overview.md.

- Subsequent user request authorizes local commit, push and PR to staging. Fresh34 targeted tests and TypeScript pass; fetched staging has no divergence from the implementation baseline and merge preview has no conflict. Publish the overview changes only; no automatic merge/deployment.

## Gradebook bulk selection in the table — 2026-11-11

- [x] Approved proposal implemented inline: no duplicate selection list/toggle, row/header mixed-state checkboxes,20 rows/page, explicit all-filtered selection capped100. Counts reflect action eligibility. Selection survives page changes but resets for assignment/search/status/mode. Group roots selected once; member grades remain separately accessible.
- [x] RED4 new tests, GREEN189 grading tests/20 files; final targeted20/2 pass, TypeScript/scoped lint pass. Local5-student/group-root UI verified at1440px/390px, light/dark, no document overflow; no AI/publish/data writes.30/101 scenarios covered with fixtures.
- No backend/API/dependency, production build, commit/push/deploy. Security/Resiliency retained, PBT off. Plan/evidence: construction/plans/code-generation-plan/2026-11-11-gradebook-bulk-selection.md.

- Approved follow-up: global announcement navigation/title is “Tất cả thông báo”; nested class navigation/title remains “Thông báo lớp”. URLs/filtering/authorization unchanged.11 announcement tests and scoped lint pass; local browser verifies both destinations and titles. Checkbox cursors also verified as pointer/enabled and not-allowed/disabled. Changes remain uncommitted.

- Shared cursor follow-up: base CSS applies pointer to enabled native/custom interactive controls and not-allowed to disabled, preserving text/drag/pending utility overrides. Actual group choices and14 browser style probes pass;324 related tests/44 files, lint and production build/typecheck pass. No backend/data changes, commit/push. Not every screen/state was manually exercised.

- Subsequent user authorization: “giúp tôi commit”. Record bulk-selection, announcement labels, shared cursors and documentation in separate local commits;15 fresh precommit tests/3 files pass. No push/merge/deploy.

- PR74 CI follow-up: feature-registration test retained the old global announcement labels. Reproduced RED locally and updated all3 role expectations to the approved label; fixed-class labels remain unchanged. Full frontend967 tests/136 files and scoped lint pass. User authorizes pushing the fix to staging to repair PR74; no merge/deploy requested. Evidence: .tmp/pr74-red.log, pr74-frontend-full.log.

### PR delivery — ba lỗi UI/UX

Tích hợp staging `1f713615`; giữ cả hai lịch sử audit/state. Fresh full frontend
963 test/135 file, ESLint/TypeScript, Docker build và runtime smoke qua. Người dùng
cho phép push/tạo PR vào staging; không tự merge PR. Xem PR GitHub để biết trạng thái CI.

## Whole-project bug review — 2026-11-12

- [x] Reviewed checkout 65fcc764 on feat/ai-prompt-guardrail with existing AI-DLC conventions and module context.
- [x] Full backend verify 2,342 tests/no skips and required integration guard; frontend 1,018 tests, lint, production build and clean-cache typecheck passed. MCP hook 14 scenarios and deploy ShellCheck passed.
- [x] Confirmed four defects with local probes: quiz exit after failed save (P1), quota fallback transaction rollback, quiz resumption in archived prior class, aggregate AI timeout (P2). Findings are reported, not fixed.
- [x] Vietnamese report and evidence: construction/build-and-test/2026-11-12/2026-11-12-project-bug-review.md. Plan: construction/plans/2026-11-12-project-bug-review.md.

No application source edits, live data/provider writes, restart, subagents, commit/push/deploy. Stale generated route types moved to .tmp before rebuilding. Live browser/provider/production/load checks not performed; Python fixture unavailable and deploy shell harness not rerun. Security/Resiliency retained with recovery defects reported, PBT disabled. Concurrent audit additions preserved.

## Whole-project review fixes — 2026-11-13

- [x] Fixed BUG-01–04 from the 12/11 review: failed quiz exit save, quota rollback fallback, class-scoped retake attempts, aggregate AI retry timeout.
- [x] User policy: retain old work in archived class; new class gets independent attempts and limits. Enrollment restriction unchanged.
- [x] Final isolated backend 88 tests/no skips, PostgreSQL migration; frontend quiz 35 tests, TypeScript/scoped lint passed.
- Evidence: construction/build-and-test/2026-11-13/2026-11-13-review-fixes-results.md. No live migration/commit/push/deploy; unrelated concurrent work preserved.

## Group controls and AI authoring — 2026-11-13

- [x] User approved seven previously reviewed proposals with “giúp tôi sửa theo đề xuất $ponytail:ponytail”. Implemented multi-select student enrollment (200-row existing batch limit), filtered group-member selection, explicit leader-only action, Đề mẫu navigation, informative release confirmation, private image reference sources for AI generation/revision and document-native formatting.
- [x] Full backend verify 2349 tests without failures/errors/skips; required integration guard and 57 traced cases pass. Final cap delta 107 academics/identity tests pass. Full frontend 1024 tests/148 files, final two enrollment cases, scoped lint, isolated typecheck and production build pass.
- [x] Browser checks teacher/manager UI at local mobile viewport; no post-change data writes or paid AI call. Running backend still advertises old source types and needs reload. Original tsconfig restored after isolated build. Automatic review blocked deletion of stale generated type directory; no retry/deletion performed. No commit/push/deploy.

Plan/evidence: construction/plans/code-generation-plan/2026-11-13-group-and-ai-authoring.md and construction/build-and-test/2026-11-13/2026-11-13-group-and-ai-authoring.md. Existing security/resiliency scope retained; PBT disabled.

## Kế hoạch đính kèm trong khung chat AI — 2026-11-13

- [x] Đối chiếu AiReviseChat, sourceUploads, API signed FileRef, IArtifact và thư viện trích xuất hiện có; giữ nguyên WIP trước đó.
- [x] Lập kế hoạch hai giai đoạn theo yêu cầu “giúp tôi lập plan trển khai”: menu ba nguồn, tệp mặc định tham khảo/DOCX đổi vai trò, paste ảnh, kéo thả và giữ nội dung khi gửi lỗi; sau đó nhập trang HTTPS công khai thành nguồn TXT riêng tư.
- [ ] Triển khai giai đoạn 1 khi người dùng yêu cầu thực hiện.
- [ ] Chứng minh đường kết nối URL chống SSRF/rebinding/redirect trước khi bật giai đoạn 2; chưa suy ra endpoint mạng được phép triển khai từ yêu cầu lập kế hoạch.

Kế hoạch: construction/plans/code-generation-plan/2026-11-13-ai-chat-attachments.md. Chưa sửa source, chạy test mới, commit/push hoặc deploy. Không subagent. Giữ Security/Resiliency hiện hữu; phần mạng có review bổ sung, PBT vẫn tắt.


## AI chat files — implemented 2026-11-13

- [x] User approved phase 1 only and explicitly deferred links. Three-action menu, DOCX role switching, image paste, file drop, shared bounded/cancellable upload queue, visible cards and input/recovery safeguards implemented. Existing WIP retained; no new backend/API/dependency.
- [x] Full frontend 1038 tests/150 files, full lint, isolated TypeScript and production build pass; original tsconfig/next config restored. Final verification ran sequentially with two workers after Windows virtual-memory exhaustion in a concurrent run. Manager UI validates DOCX/reference/template transitions and TXT drop; PNG thumbnail renders but running backend still uses old image policy. No model call or assignment edits; fixture attachments removed from composer.
- [ ] Deferred by user: all URL/link work. Not required for this phase's completion.

Plan/evidence: construction/plans/code-generation-plan/2026-11-13-ai-chat-attachments.md and construction/build-and-test/2026-11-13/2026-11-13-ai-chat-files.md. Backend reload and page refresh needed for live image acceptance. Scoped blob-preview design false-positive exemption recorded; no global exemption. No commit/push/deploy, no subagents.

### Runtime UI recheck — 2026-11-13

- [x] User explicitly authorized restart. Existing backend terminal restarted through scripts/run-backend.bat; new local API and worker started on 8080. Old Docker worker stopped to prevent stale-code job execution.
- [x] Fresh manager/teacher UI accepts PNG/JPEG as Sẵn sàng; manager DOCX role switching, text+image paste, file drop, size/type/count validation, retry/remove and close/reopen preservation pass. Previous running-backend image-policy limitation is resolved.
- [x] Removed test attachments/text from composers and restored teacher route. No assignment text edits, model-provider request, commit/push/deploy. Browser-event paste/drop simulation; existing CSP style/SSE disconnect logs remain outside this check's scope.

### Live AI check — 2026-11-13

- [x] Explicit user authorization “Gọi ai thực tế đi”: two real PERSONAL/GEMINI gemini-3.5-flash-lite calls through teacher chat UI in a new QA draft. Image-only and TXT-only markers correctly read; apply and reload persistence pass.
- [x] Follow-up yields one question, two five-point rubric criteria, VALID total ten points. Draft f0643cc5-f9a5-4947-8eea-5056564bffa8 remains unpublished for review; screenshot .tmp/ai-live-provider-result.png.
- [ ] Content-quality finding: initial call omitted requested rubric and emitted Markdown; second call fixed rubric/markers but still omitted requested native bold. No source-code fix claimed; no further calls or commit/push/deploy.

### Local commits and staging check — 2026-11-13

- [x] User authorized local commits and integration check against staging. Recorded class enrollment/group controls and AI source/chat authoring as separate feature commits, with plans and verification in a documentation commit.
- [x] Fresh precommit checks: 217 frontend tests/31 files and 131 targeted backend tests pass; diff whitespace check passes. No code changes since earlier lint/typecheck/build and live UI checks.
- [x] Fetched origin/staging and merged into fix/u05-u09-u15: Already up to date; no conflicts or merge commit needed. Staging contains no commits missing from this branch.
- [ ] Push/PR/deploy not requested in this interaction. Live AI rubric/formatting findings remain recorded; no claim they were fixed by committing.

## Student assignment visibility diagnosis — 2026-11-13

- [x] User requested checking a screenshot: notification opens group assessment, class assignment tabs show zero items. Reproduced via actual StudentAssignmentsPage with a mocked API row: OPEN visible, CLOSED/unsubmitted missing from every tab (one passing control, one failing symptom).
- [x] Located frontend tab partition gap and misleading closed-group status label. Backend list/detail intentionally include CLOSED rows; deadline scanner can close previously published work. Diagnostic failing harness moved out of the discovered test tree.
- [ ] Await full deployed assignment link to verify that record's status/class; not claiming the screenshot's production root cause is conclusively verified.
- [ ] Product fix not requested/applied in this diagnosis turn. Proposed discoverable closed/unsubmitted category plus matching labels; preserve actionable class-summary predicates and authorization.

Evidence: construction/build-and-test/2026-11-13/2026-11-13-student-assignment-visibility.md. Only diagnosis documents changed; no commit/push/deploy.

### Approved visibility repair — 2026-11-13

- [x] User authorized the proposed repair with Ponytail. Added Đã đóng tab for CLOSED/MISSED rows without an individual/group submission, with existing Hết hạn label shared by list and detail. Closed submitted rows remain in Đã nộp; actionable todo predicates, API, schedules and permissions unchanged.
- [x] RED: nine new regression failures, seven passing controls before source edits. GREEN: 40 affected tests and full frontend 1047 tests/150 files; full ESLint and isolated TypeScript pass. URL focus/filter/pagination and closed submitted group histories covered.
- [x] Current U08 frontend doc and evidence updated. Normal typecheck still fails on old generated Next routes; no cache deletion or permanent config change, temporary check config removed.
- [ ] Deploy-record confirmation and browser acceptance not performed; no link provided. No backend change or new build/backend tests required for this frontend classification delta. No commit/push/deploy.

## Member contribution levels — 2026-11-13

- [x] Read existing member grading UI/API/model/JSON persistence; prepared concrete five-level assessment and in-product scoring guidance.
- [x] User subsequently approved implementation of inception/requirements/2026-11-13-member-contribution-levels.md. Optional persisted level; teacher decides score; no automatic conversion. See implementation results below.
- Initial design-only stage changed no source/tests; implementation results below supersede that stage. No commit/push/deploy. Existing security/resiliency scope retained; PBT disabled.

### Member contribution levels implemented — 2026-11-13

- [x] User authorized implementation of the reviewed five-level contribution proposal; direct execution, no subagents. Persisted nullable enum in existing evaluation JSONB/member-save transaction, previous/current level audit, private published student view, reset on resubmission and retained independent assessment on group corrections.
- [x] Shared Select and collapsed grading guide; no automatic score conversion, saved personal reason restored, initial level unassessed. Source and tests follow existing module boundaries, no new dependency/schema migration.
- [x] Backend55/55; frontend grading199/199 plus API4/4 (three overlap); scoped lint and isolated source/test TypeScript pass. Normal TypeScript fails only on old generated Next route validators. Temporary typecheck config removed, production config untouched.
- [x] User will perform UI acceptance. No browser check, production build, live grade changes, AI call, backend restart, commit/push/deploy. Backend must load new code before testing the new strict request field.

Plan: construction/plans/code-generation-plan/2026-11-13-member-contribution-levels.md. Results: construction/build-and-test/2026-11-13/2026-11-13-member-contribution-levels.md. Security/Resiliency scope retained, PBT disabled.

### Group grading side panel — implemented 2026-11-13

- [x] User approved the concrete two-column UI proposal. Added optional DocumentEditor sidePanel, group rubric/member tabs and responsive modeless drawer using the existing1280px breakpoint.
- [x] Member selector/previous/next and actual sectionAuthors links in both directions; multiple authors retained, unknown contribution never inferred. Persistent per-member draft components, version-safe refresh/conflict handling and additional unsaved guards; group autosave excludes member grades.
- [x] RED3 new failures/44 controls; frontend full1056/151files; final scoped50/50 after local polish, lint/isolated TypeScript pass. Normal stale Next validator issue remains outside this delta; temporary config removed.
- [x] Live teacher local UI1440/390: two columns/sticky panel, correct member, jump in page/step modes, draft retained across member switch and drawer Escape/reopen. Removed temporary unsaved QA note; restored554x930 viewport. No actual grade save, publish or AI call.
- No backend delta/restart, production build, commit/push/deploy. Existing five-level feature WIP preserved; no subagents. Security/Resiliency retained, PBT disabled.

Plan: construction/plans/code-generation-plan/2026-11-13-group-grading-side-panel.md. Results: construction/build-and-test/2026-11-13/2026-11-13-group-grading-side-panel.md.

### Local commit delivery — member grading

- [x] User authorized clear local commits. Separated optional contribution-level data/API/published-student visibility from group-side-panel UI and unsaved-draft safeguards; plans/evidence/audit recorded in a documentation commit.
- [x] Fresh precommit backend55/55 and frontend grading/shared document/submission-history276/276 across26 files pass; whitespace/index checks pass. No source edits since verification, no log/screenshot/temp config added to git.
- No push, merge or deploy requested/performed. Earlier UI/typecheck/full-suite evidence and production-build limitation remain recorded in the reports.

## Staging readiness — 2026-11-16

- [x] HEAD matches freshly fetched origin/staging at d8ac7d21; no merge conflicts found. Exact-SHA CI backend/frontend/deploy-script/images succeeded.
- [x] Report: construction/build-and-test/2026-11-16/2026-11-16-staging-readiness.md. Local frontend dependency installation incomplete and Docker engine stopped; no live deployment verification, push or deploy.


## Góp ý giảng viên và Syllabus SWR302/SWD392 — 2026-11-20

- [x] Rà soát chỉ đọc theo góp ý giảng viên và Syllabus FLM SWD392 (ID 14179), SWR302 (ID 14176). Kết quả: inception/requirements/2026-11-20-lecturer-feedback-syllabus.md.
- [x] Người dùng chọn mục 2, 3, 4, 5, 8. Đã cập nhật tài liệu: định vị không áp quy trình riêng, AS-IS theo Syllabus, giữ D01, MCP/AI không cần GPU và công thức chi phí (requirements.md §1, §2.3, §2.4, REL-005), trạng thái 2026-11-20 trong gap analysis 2026-11-12.
- [ ] Mục 8 (loại sơ đồ Context/DFD/Communication): Requirements Analysis nháp và câu hỏi Q1–Q4 tại inception/requirements/2026-11-20-diagram-types*.md. Chờ người dùng trả lời và GVHD duyệt; chưa sửa code.
- [ ] Report 1 trên Drive chưa sửa; dùng mục 2 của tài liệu 2026-11-20 để cập nhật.

Chỉ sửa tài liệu, không chạy test, không commit/push/deploy. Security/Resiliency giữ nguyên, PBT tắt.

## Cải thiện độ chính xác AI chấm bài — kế hoạch 2026-11-20

- [x] Lập kế hoạch theo yêu cầu "lập plan hoàn chỉnh để cải thiện độ chính xác AI chấm bài hiện tại": construction/plans/code-generation-plan/2026-11-20-ai-grading-accuracy.md. Dựa trên kết quả đánh giá 2026-11-13/15 và đọc code tĩnh; tìm 9 nguyên nhân gốc R1–R9 (mã block không gửi kèm nội dung, sơ đồ gửi hai lần, mã block sai làm hỏng cả đề xuất, thiếu system boundary…).
- [x] Người dùng duyệt làm giai đoạn 1–3 trước. Đã làm: mã block trong prompt chấm, mã block sai không còn làm hỏng đề xuất, bỏ gửi sơ đồ lần hai, tóm tắt sơ đồ có hình chữ nhật và quan hệ "trong", chỉ dẫn loại sơ đồ/chuẩn ký pháp/rubric mơ hồ/sơ đồ bị cắt/lạc đề. Test liên quan 86/86, unit test các unit bị ảnh hưởng 755 đạt, 8 bỏ qua.
- [x] Đo live 2026-11-20 (gemini-3.5-flash-lite, badminton-booking, cũ 7d51a16f và mới): không cải thiện, bài 08 tụt khi có mã block (G1); bài 04 sai ở cả hai bản (G2). Kết quả: construction/build-and-test/2026-11-20/2026-11-20-ai-grading-accuracy-results.md.
- [x] Sửa G1 (bằng chứng phải đối chiếu) và G3 (cấu hình suy nghĩ theo provider; Gemini mặc định không gửi tham số, 0 lần bị cắt khi đo). 926 unit test đạt. Kết quả mục 4–5 file kết quả 2026-11-20.
- [x] G4 (làm tròn điểm) và G2 (nét liền) đã sửa 2026-11-21; 928 unit test đạt, live bài 04 đúng 3,75, replay xác nhận G4.
- [x] Chạy lại cả bộ 2026-11-21: 9,46/10, sai số điểm 2,3%, kappa 0,84; G5, G6 mới phát hiện.
- [x] 2026-11-21: giữ chỉ dẫn G6, bỏ chỉ dẫn độ dài G5, bộ đánh giá lưu phần chữ bị cắt, RubricEditor gợi ý tiêu chí chỉ nêu trường hợp bị trừ (vi/en/ja). Backend 496, frontend 149 test đạt.
- [x] Commit local 2026-11-21 theo yêu cầu (chưa push): d8f69175, 43857601, 3aa94c57, fc8ec0cf và commit tài liệu.
- [ ] Còn: chẩn đoán G5 (chưa bắt được lần bị cắt), rubric mẫu cho môn (cần app chạy), giai đoạn 0 (bộ SWR302/COMET), 4.1–4.2, kiểm OpenAI/Claude khi có key, integration test (Docker tắt). Chưa commit.

Đã gọi Gemini thật bằng key của người dùng (khoảng 80 lời gọi, bản ghi trong backend/target/ai-eval, không vào git). Không commit/push/deploy.

### U09 — rà soát actor phụ trong đề xuất AI (2026-11-21)

- [x] Sửa prompt để đối chiếu actor theo vai trò, xử lý điểm thành phần theo rubric và đánh dấu trường hợp mơ hồ.
- [x] Backend kiểm tra mục chữ ghi rõ tên actor phụ bắt buộc với bảng đặc tả cùng phần. Đề xuất tối đa khi thiếu tên được đánh dấu cần giảng viên xem ở cả provider và MCP.
- [x] Test hồi quy tái hiện lỗi. Trên mã cuối, 96 ca grading đạt. Phát lại 10 phản hồi Gemini hợp lệ theo hai rubric, cả 5 lượt đề xuất tối đa sai actor phụ được đánh dấu. Prompt mới chưa được gọi lại qua Gemini.
- [x] Chạy toàn backend trước bước bổ sung xử lý rubric có ví dụ: 2.583 đạt, một bỏ qua và ca đo `U04_SCOPE` vượt p95 (23,80 ms so với 20 ms). Chạy lại ca đo cùng 96 ca grading trên mã cuối, tất cả đạt. `U04_SCOPE` đạt 3,65 ms khi chạy lại.

### U09 — phục hồi yêu cầu chấm AI (2026-11-21)

- [x] Kế hoạch thực hiện grading trước theo [kế hoạch U09](construction/plans/code-generation-plan/2026-11-21-u09-proposal-recovery.md). QA Orca độc lập xác nhận hai cửa sổ gián đoạn và rà soát bản sửa backend.
- [x] Gửi lại cùng mã và snapshot khôi phục đề xuất còn chờ. Gắn tác vụ trong transaction tạo tác vụ và giữ quota, theo khóa đánh giá. Báo snapshot hết hiệu lực và từ chối bản MCP gửi lại sai định dạng trước khi xác nhận replay.
- [x] Backend 157 test đạt qua hai nhóm. Ca đồng thời với PostgreSQL và RabbitMQ ghi nhận một đề xuất, một tác vụ, một khoản giữ quota và một message. Xác nhận hoàn tác task/quota khi transaction gián đoạn. Hồ sơ trong `backend/target/grading-proposal-recovery-verified.log` và `backend/target/grading-proposal-transaction-guards.log`.
- [x] Frontend giữ mã khi gửi lại chấm lẻ và theo lô trong cùng màn hình. Kiểm tra trực tiếp 223 test grading, TypeScript và ESLint các file đổi đạt. Worker Orca tái hiện 6 ca sai mã trước sửa và chạy lại 1.171 test frontend thành công với 4 worker.
- [ ] Chạy hành trình trình duyệt qua stack cô lập và gọi lại provider thật.

### U09 — xử lý phản hồi AI thiếu trường nhận xét (2026-11-21)

- [x] Năm ca thiếu trường nullable bị từ chối trước sửa, đều đạt sau khi sửa schema provider và chuẩn hóa. Bằng chứng thiếu cần giảng viên xem. Sai kiểu dữ liệu vẫn bị từ chối.
- [x] 255 test grading, gateway và MCP đạt. Phát lại 24 bản ghi Gemini, 17 hợp lệ về định dạng và 7 lỗi provider đã ghi. Kết quả trong `backend/target/grading-format-verified.log` và `backend/target/grading-format-replay`.
- [ ] Gọi provider thật trên bản sửa và khởi động lại backend Orca.

## Bỏ tiếng Nhật — 2026-11-21

- [x] Bỏ locale `ja` (locales.ts, msg.ts, xóa `messages/ja.json`); cookie cũ `ja` quay về tiếng Anh. Lựa chọn ngôn ngữ đề AI chỉ còn VI/EN/AUTO (frontend dựng từ LOCALES; backend `normalizeOptions` từ chối `JA`, prompt bỏ nhánh Japanese).
- [x] Cập nhật README, requirements (§FR-006, NFR-002), current-code-baseline; báo cáo lịch sử giữ nguyên.
- [x] Test: frontend 157 tệp, 1.161 test đạt; backend assignments + ArchitectureTest 166 đạt.
- [x] Theo yêu cầu, thay dữ liệu mẫu tiếng Trung trong `AssignmentAiRevisionTest` bằng tiếng Việt cùng nghĩa (`vietnameseSkeleton`).
- [x] Theo yêu cầu, xóa con dấu trang trí chữ Hán (`HankoMark`) khỏi `Logo.tsx` và màn đăng nhập (`AuthLayout`); còn giấy nhớ và bút chì. Frontend identity + shared 202 test đạt, eslint không lỗi. Chưa xem trên trình duyệt.

## Độ tin cậy của rubric và bằng chứng chấm AI

- [x] U09 ưu tiên điều kiện bắt buộc và tùy chọn của rubric khi đề xuất điểm. Khi cách tính điểm chưa rõ, prompt yêu cầu giảng viên xem lại.
- [x] Đề xuất qua provider và MCP được đánh dấu cần xem khi dẫn bằng chứng sang phần khác đã xác định. Điểm đề xuất giữ nguyên. Test kiểm tra gửi lại cùng mã, đề xuất khác nội dung và dữ liệu lịch sử.
- [x] Ba ca hồi quy thất bại trước bản sửa và đạt sau bản sửa. Backend Java 17 đạt 2.581/2.582 test, test backup còn lại đạt khi chạy riêng với UTF-8. 71 test giao diện duyệt điểm đạt.
- [x] Replay 24 phản hồi giữ nguyên kết quả điểm. Phép đo prompt mới qua Gemini đang chờ khóa trong phiên cục bộ. Cơ chế xác thực, phân quyền, ghi nhận và phục hồi nền hiện có được giữ cho phạm vi U09. Không bổ sung triển khai hoặc thay đổi cấu hình.

## Sửa 19 phát hiện audit mã nguồn (2026-11-21)

- [x] Tái hiện, sửa và kiểm lại 19 mục theo [kế hoạch](construction/plans/code-generation-plan/2026-11-21-audit-remediation.md).
- [x] Kiểm bản ghép với 249 test backend được chọn, 1.186 test frontend, TypeScript, ESLint và 20 ca deploy/restore giả lập. Dữ liệu kiểm thử tổng hợp.
- [ ] Nghiệm thu browser, AI provider trực tiếp và triển khai. Chưa commit, push hoặc deploy lượt này.

### Kiểm tra Gemini trực tiếp sau đợt sửa audit

- [x] Gemini 3.5 Flash Lite trả 6/6 phản hồi hợp lệ trên 01-complete và 08-wrong-actors. Bài đầy đủ đạt 4,00 trong cả ba lượt. Bài actor sai đạt 2,25 đến 2,50 trên 4,00, kỳ vọng 2,80 từ bộ tổng hợp. Mục actor dao động 0,00 đến 0,12 và không có cờ cần kiểm tra.

## U10 — audit và sửa tạo câu hỏi bằng AI (2026-11-22)

- [x] Chỉ tạo lại theo tài liệu khi câu hỏi lệch nguồn. Yêu cầu lệch tài liệu giữ câu đã tạo kèm cảnh báo, không gọi AI lần hai.
- [x] Lượt tạo lại theo nguồn không đòi version quiz khớp; quiz còn soạn được là đủ, như lượt đầu.
- [x] Schema kết quả ép loại câu và độ khó người soạn đã chọn.
- [x] 190 test của package `quizzes` và 35 test `PlatformWorkflowIntegrationTest` (Postgres Testcontainers) đạt.
- [ ] Đo 50 câu bằng lời gọi provider thật, nhất là Claude với trần 8.192 token và timeout 120 giây. Mẫu Codex gpt-6-luna cho cận dưới 6.134 token o200k.

## U09 — hồi quy prompt cho điểm từng luồng và trigger (2026-11-22)

- [x] Prompt kiểm tra trigger có khởi tạo đúng use case. Điểm từng phần có phân bổ rõ được cộng vào `itemScoresByRubric` theo bằng chứng của từng luồng, đồng nhất với lý do.
- [x] Hai kiểm tra prompt thất bại trên bản trước khi sửa. Lượt cuối đạt 57/57 test backend, gồm tạo prompt, schema, harness và validator. Phạm vi security và resiliency đã phê duyệt được giữ. PBT đã tắt.
- [x] Luna medium cho bài lạc đề 0,50/4 và giữ 4,00/4 ở bài đầy đủ. Cả hai đạt ngưỡng theo Sol high trên cùng dữ liệu và schema của lượt trước.
- [x] Chạy đủ 20 ca với prompt hiện tại. Luna trả 20 kết quả hợp lệ, 15/20 khớp điểm tổng của nhóm và Sol đạt ngưỡng cả ba chỉ số ở 18/20 ca.
- [x] Opus 5.5 `high` đánh giá đủ 20 bản ghi Luna của lượt 3: 15/20 đạt cả ba chỉ số. Hai judge cùng đạt 15 ca, chỉ Sol đạt 3 ca (`01-complete`, `03-one-normal-flow`, `05-off-topic`, Opus cho chỉ số hữu ích 0,70), cùng dưới ngưỡng 2 ca workshop.
- [ ] Luna vẫn cho 0 thay vì 0,25 ở mục một luồng. Lượt đầy đủ cũng ghi nhận lệch bội số và kiểm tra quyền ở hai ca workshop. Lượt 2 còn 17 ca chưa có Opus.

## Tối ưu luồng MCP (2026-11-24)

- [x] Xóa `McpServer.cut()` và `TRUNCATED` không còn nơi gọi.
- [x] `JsonSchemaChecker.validateToolArguments` giữ schema đã biên dịch cho tool MCP. Schema output AI theo từng task vẫn biên dịch mỗi lần để map không tăng mãi.
- [x] `read_grading_context` đo kích thước tăng dần theo block thay vì tuần tự hóa lại cả trang. Thêm overload nhận ngân sách byte.
- [x] `read_grading_batch` truyền phần ngân sách 180 KB còn lại cho từng item. Item sau nhận trang ngắn hơn kèm `continuation`; không vừa thì trả `MCP_BATCH_RESULT_LIMIT`.
- [x] Test MCP và grading MCP đạt. Toàn bộ backend 2663 test còn 1 lỗi có sẵn trên HEAD (`GradingProposalValidatorTest.onlyHoldsFullCreditForExplicitActorLists[2]`), không liên quan.
- [ ] Chưa đo thời gian trước và sau trên evaluation nhiều block; chưa chạy E2E WF01/WF02.
- [x] Chạy luồng MCP thật trên backend worktree (cổng 8081, DB local): tạo đề nhóm nháp, replay và xung đột requestRef; đọc ngữ cảnh và ghi đề xuất chấm bài nhóm `GROUP_DOCUMENT`, replay, sai fingerprint; điểm chính thức giữ `PUBLISHED`.
- [ ] `read_grading_batch` 6 bài thật: 4 OK (bài cuối nhận trang ngắn kèm continuation), 2 trả `MCP_BATCH_RESULT_LIMIT` vì kích thước sau escape trong envelope vượt 240 KB dù ngân sách thô còn đủ. Chưa sửa.

## Ổn định luồng MCP (2026-11-24)

- [x] Ngân sách `read_grading_batch` tính thêm hệ số phình do escape vào envelope, giữ kiểm tra 240 KB làm chốt chặn. Test với item nhiều dấu ngoặc kép.
- [x] `McpServer` ghi audit `INVALID_ARGS` khi `arguments` không phải object hoặc sai schema (chỉ mã lý do, không ghi thông điệp validator).
- [x] `scripts/mcp-smoke.py` và `scripts/test-mcp-smoke.py`: smoke test tạo đề, replay, xung đột requestRef, đọc ngữ cảnh, lô, sai fingerprint, ghi đề xuất và replay. Chạy thật trên backend worktree: 11/11 PASS.
- [x] Đo lại lô 6 bài thật: hệ số phình chỉ 1,14; bài 3–4 bị `MCP_BATCH_RESULT_LIMIT` vì phần cố định mỗi trang (~28 KB rubric, schema, ngữ cảnh đề) lớn hơn ngân sách còn ~25,8 KB. Hành vi đúng; không phải do escape như giả thuyết trước.
- [ ] Đề xuất sau: không lặp phần cố định giống nhau giữa các item cùng bài tập trong một lô.
- [x] Sửa `GradingSecondaryActorCheck.NON_MANDATORY`: thêm `(?U)` để `\b` nhận chữ tiếng Việt; tiêu chí "(ví dụ …)" không còn bị giữ duyệt nhầm. Toàn bộ backend 2664 test đạt, 0 lỗi.
- [x] Frontend: `vitest.config.mts` đặt `testTimeout` 15 giây. Năm test UI nặng (grading workspace, bulk selection, authoring flow) chỉ mất 0,25–1,2 giây khi chạy riêng nhưng vượt 5 giây khi 24 worker jsdom chạy song song. Toàn bộ 1213 test đạt hai lần liên tiếp; typecheck và lint sạch.
- [x] E2E trên backend worktree (`local,worker`, cổng 8081): `wf01-wf02-grading.mjs` PASS (2 ca), `wf01-mcp-client.mjs read` đọc đủ 7 block, `record` trả đề xuất READY.
- [x] Đã xóa 2 đề nháp thử `[MCP test]`/`[MCP smoke]` qua API của giảng viên. Ba đề xuất chấm thử trên bài nhóm SE_JS không có API xóa nên được giữ lại; không đổi điểm chính thức.
