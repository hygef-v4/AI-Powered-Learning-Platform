# Ngày tháng, kiểm API key và điều kiện mở chấm — 2026-11-15

Nhánh `fix/date-ai-grading-window`, từ `staging@7d0905d4`. Người dùng duyệt đề xuất
audit bằng “ok giúp tôi sửa theo đề xuất $ponytail:ponytail”.
[Yêu cầu](../../inception/requirements/2026-11-15-date-ai-grading-window.md),
[kế hoạch](../plans/code-generation-plan/2026-11-15-date-ai-grading-window.md).

## Kết quả thực hiện

- DateInput chung hiện/nhận `dd/MM/yyyy`, kiểm ngày thật, năm nhuận, min/max và hỗ trợ
  lịch native/bàn phím; ISO API giữ nguyên. Áp dụng tạo/sửa lớp, audit, lịch sử AI,
  lịch giao bài và hạn tự chia nhóm. Giờ dùng helper Việt Nam hiện có. Không migration.
- Probe kết nối AI tăng ngân sách tối đa từ 5 lên 1024 token; typed output-limit được
  coi là kết nối thành công và giữ usage. Sai key/model/quota/rate/timeout vẫn lỗi.
  Phản hồi cắt token hoặc kết thúc bất thường vẫn bị từ chối khi tạo đề/chấm bài;
  từ chối an toàn được phân loại trước, không biến thành probe thành công.
- Backend chặn chấm tay/AI/member/finalize/công bố/batch/MCP khi assignment chưa
  CLOSED/RETIRED. Dữ liệu phụ thuộc thiếu/lỗi không được cho phép ghi. API detail,
  danh sách và MCP progress có editability/reason; UI chỉ đọc có lý do, khóa batch.
- Nút Đóng nhận bài cho bài CLASS đang OPEN, xác nhận, kiểm giảng viên/lớp/version,
  audit `ASSIGNMENT_CLOSED`; giữ hạn gốc. Tự đóng sau hạn cuối +30 giây qua cùng
  lifecycle. Khóa assignment trước attempt/document/section cho cả thao tác người
  học lẫn auto-submit. CLOSED và chốt dữ liệu cùng transaction, lỗi rollback.
- Lượt cá nhân đang làm chốt nội dung đã lưu. Nhóm chốt draft CLAIMED thành DONE và
  lịch sử; nhóm đã nộp tay giữ nguyên receipt/bản nộp để chấm. Nội dung nháp chốt sau
  đó vẫn giữ trong tài liệu/lịch sử. Không thêm mở lại hoặc tùy chọn thời gian mới.

## Kiểm chứng backend

- RED: guard chấm trước đóng thất bại đúng tại kỳ vọng không ném lỗi trước khi thêm
  guard; probe/date có các vòng RED/GREEN riêng. Không dùng provider thật.
- Đợt rộng: 510 test assignments/assignmentwork/grading/groupdocs và ArchitectureTest
  qua; sau sửa lock-order nền và giữ draft nhóm, 101 test liên quan qua. Các đợt này
  chồng lặp, không cộng thành tổng test độc lập.
- Sau tách validation read-only khỏi khóa ghi: 61/61 qua, gồm GroupSectionService,
  GroupDocImportService, GroupSubmissionService, GroupDocumentService, PostgreSQL
  closing (3) và PlatformWorkflowIntegrationTest (26). Log local `.tmp/date-ai-backend-final.log`.
- Thêm kiểm thử thực trên composition production: lỗi insert evaluation khi đóng sớm
  rollback assignment OPEN, attempt IN_PROGRESS, audit và evaluation; đóng lại chốt
  nguyên content, giữ hạn và khóa sửa. 1/1 qua, log `.tmp/date-ai-close-retry.log`.
- PostgreSQL xác nhận FOR SHARE chặn close đến khi lưu commit; deadline lateUntil và
  ranh giới inclusive 30 giây đúng. Quyền/version/lớp, batch, terminal status và
  unavailable dependency có regression test. AI gateway/probe 45/45 qua.
- Kiểm cuối 21 suite tích hợp có trong manifest: **282/282**, không failure/error/skip,
  gồm PlatformWorkflow 27 và RequirementsCoverage 122. Script
  `scripts/assert-required-backend-tests.ps1` qua cả 5 suite PostgreSQL bắt buộc và
  **58/58** kịch bản truy vết. Log `.tmp/date-ai-required-integration.log` và
  `.tmp/date-ai-integration-guard.log`. Đây là kiểm theo phạm vi, không phải toàn bộ
  backend test suite.

Maven chạy offline với heap 256 MB và 2 processor; Vitest chạy một worker tuần tự.
Lần chạy nhiều worker song song trước đó hết bộ nhớ đã dừng; lỗi không được tính là
kết quả kiểm chứng. Không sửa policy hay dependency để né lỗi.

## Frontend và bàn giao

- Toàn bộ Vitest: **1135/1135 test, 156/156 file** qua với
  `npm test -- --maxWorkers=1 --no-file-parallelism`. jsdom cảnh báo navigation/scrollTo
  như các suite cũ, không có failure/skip. Log `.tmp/date-ai-frontend-tests.log`.
- Sau sửa fixture ngày rỗng từ null về chuỗi rỗng theo type hiện có: riêng academics
  **37/37** qua. Không đổi hợp đồng startsOn để chiều fixture.
- `npm run lint` toàn bộ qua. `npx tsc --noEmit` với config tạm chỉ src/tests và
  next-env qua, không dùng các route validator cũ trong `.next`.
- Production `npm run build` (webpack) qua, gồm TypeScript và static/dynamic routes,
  dùng `NEXT_DIST_DIR=.next-date-ai-grading-check`. Lần đầu bắt lỗi fixture null ở
  trên; đã sửa và chạy lại thành công. Log `.tmp/date-ai-frontend-build.log`.
- Kiểm hash xác nhận tsconfig.json và next-env.d.ts khôi phục đúng byte; config tạm
  đã xóa. Không xóa cache Next cũ hay đổi config/dependency sản phẩm.
- Đã đồng bộ tên hai kịch bản nộp lại trước đóng trong manifest CI hiện hành, thêm
  IT-U07-004 cho rollback khi đóng sớm. Kết quả kiểm guard tích hợp ghi bên dưới.

## Giới hạn

Chưa gọi provider bằng API key thật, nghiệm thu lịch native trực tiếp trên browser,
test nhiều người dùng live hoặc tải lớn. Không ghi điểm/dữ liệu học tập thật, không
restart backend/worker đang dùng, commit/push/merge/deploy. Cần chạy backend/worker
bằng mã mới trước nghiệm thu; không để worker cũ tiếp tục dùng quy tắc đóng trước đây.
Security/Resiliency giữ phạm vi đã duyệt; PBT disabled. Ba phần date/AI/UI được làm
song song theo dispatching skill, root tích hợp và review transaction/lock/data loss.

Kiểm các ràng buộc liên quan: Security đạt trong phạm vi quyền/version, fail-closed,
không gửi dữ liệu học tập trong probe hoặc lộ key. Resiliency đạt trong phạm vi
thứ tự khóa, rollback và retry đã kiểm thử; không chứng nhận tải lớn. PBT tắt nên
không áp dụng. Diff whitespace và JSON manifest hợp lệ; không sửa báo cáo lịch sử
để biến chúng thành kết quả kiểm thử hiện tại.

## Bàn giao commit cục bộ

Người dùng yêu cầu “giúp tôi commit”. Giữ nguyên mã đã kiểm thử, chia theo date,
AI probe, grading/closure và tài liệu. Kiểm lại diff whitespace và guard bắt buộc:
5 suite PostgreSQL, 58 kịch bản qua; dùng kết quả ứng dụng ở trên, không chạy lại
test/build không cần thiết. Không push/merge/deploy hoặc mở rộng kiểm thử live.

## Kiểm trước PR vào staging

User tiếp tục yêu cầu tạo PR. Nhánh đã có commit merge staging; sau fetch không
thiếu commit từ origin/staging. Vì merge cập nhật AI gateway, chạy lại
HttpProviderGatewayTest36, AiConnectionServiceTest11, GradingServiceTest38:
**85/85 qua**, không failure/error/skip. Log `.tmp/date-ai-pre-pr-tests.log`.
Các kết quả toàn frontend/integration/build ở trên được giữ đúng phạm vi trước
merge; không thay bằng tuyên bố full-head acceptance. CI cần kiểm head PR.

Đã push và mở [PR #91](https://github.com/hygef-v4/AI-Powered-Learning-Platform/pull/91)
vào staging qua GitHub CLI sau khi connector tạo PR trả 403. Không merge/auto-merge;
CI và nghiệm thu live giữ giới hạn nêu trên. Tài liệu bàn giao không đổi source.
