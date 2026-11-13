# Kế hoạch sửa U04, U13, U14 sau kiểm thử hộp đen

Nhánh thực hiện: `fix-u04-u13-u14`. Ngày: 2026-10-23. Workspace: `E:/GitHub/AI-Powered-Learning-Platform`.

Đây là kế hoạch Code Generation Part 1 bổ sung cho các unit đã có thiết kế; chưa sửa application code trước khi duyệt kế hoạch. Không khởi động lại Inception, không đổi kiến trúc hoặc chia lại unit. Báo cáo đầu vào: `construction/remediation/u04-u13-u14/verification/u04-u13-u14-blackbox-2026-10-23.md`; snapshot: `E:/FPTUni/sem9_fa26/test_report/reports/blackbox-2026-10-23/checklist-results.json` tại thư mục bằng chứng ngoài Git.

## 1. Phạm vi và bằng chứng

- [x] Xác nhận đang ở đúng nhánh người dùng yêu cầu; giữ toàn bộ thay đổi chưa commit và Git index.
- [x] Đọc CLAUDE.md, các rule common, code-generation và cấu hình extension hiện có.
- [x] Đối chiếu 3 ca Lỗi, 17 ca Bỏ qua và business rules hiện hành.
- [x] Truy đường upload: `frontend/src/features/files/api.ts` dùng XHR nhưng không gửi header CSRF; `frontend/src/shared/api/client.ts` gửi header cho fetch. Backend vẫn yêu cầu CSRF ở `identity/web/IdentityHttpSecurity.java`.
- [x] Truy U04-09: `frontend/src/features/subjects/SubjectClassesTable.tsx` chỉ có caption ẩn với nội dung ngắn, không có hướng dẫn hiển thị như checklist.
- [x] Truy U14-02: `Announcement.clean` từ chối chuỗi có thẻ HTML. `BR-U14-02` ghi không HTML thô và hiển thị Markdown đã làm sạch; renderer hiện có `skipHtml` + `rehype-sanitize`. Đây là điểm cần chốt hành vi, không phải bằng chứng script đã chạy.
- [x] Truy blocker chốt điểm: `GradingService.requireCurrent`, `EvaluationRepositoryAdapter`, `EvaluationJpaEntity` và UI gửi version. Chưa khẳng định nguyên nhân cuối; cần tái hiện bằng JPA/transaction thật trước sửa.
- [x] Người dùng duyệt kế hoạch: [Answer]: a, xác nhận bằng “tôi chọn rồi”. Thực hiện phương án A.

## 2. Thứ tự thực hiện sau duyệt

### Bước 1 — U13: sửa upload, giữ bảo vệ CSRF

- [x] Thêm regression cho multipart upload tại `frontend/src/features/files/api.test.ts`: cookie CSRF được đọc/giải mã và gửi qua header X-XSRF-TOKEN, credentials, progress, abort và response lỗi.
- [x] Tách helper CSRF dùng chung trong `frontend/src/shared/api/` nếu cần; dùng lại trong `client.ts` và `features/files/api.ts`, không sao chép quy tắc cookie khác nhau.
- [x] Sửa XHR gửi CSRF token trước `send(FormData)`; không tự đặt Content-Type làm mất multipart boundary; giữ tiến trình và nút Hủy.
- [x] Nếu xử lý phiên hết hạn cần retry, chỉ thực hiện theo hợp đồng refresh hiện có và chỉ khi chắc chắn upload chưa commit; không tự retry request đã thành công/không rõ kết quả hoặc đã bị người dùng hủy.
- [x] Chạy regression API client và FileUploader; xác minh backend tiếp tục từ chối request thiếu/sai CSRF. Không whitelist upload khỏi CSRF, không sửa chính sách cookie/CORS để né lỗi.
- [x] Chạy upload PDF, DOCX, PPTX và PNG đổi đuôi PDF trên stack local; kiểm chính sách loại/dung lượng và lỗi có thể sửa được.

Truy vết: UC59,61; BR-U13-10,30,40; U03 file policy; U01 CSRF.

### Bước 2 — U04: hướng dẫn bảng lớp

- [x] Bổ sung dòng hiển thị `Chỉ xem. Lớp do Chủ nhiệm môn quản lý.` gần bảng trong `SubjectClassesTable.tsx` hoặc container `SubjectClassesTab.tsx` đang sở hữu bảng.
- [x] Giữ bảng chỉ đọc, số đếm và bộ lọc; không thêm link mở lớp cho Admin. Kiểm bằng UI cùng fixture lớp Nháp/Đang mở/Lưu trữ.

Truy vết: UC18; BR-U04-05; ca U04-09. Thay đổi copy nhỏ không cần một test chỉ lặp lại nguyên văn implementation; dùng checks/UI hiện có.

### Bước 3 — U14: chốt và sửa hành vi Markdown

Phương án đề xuất A trong file câu hỏi: nhận payload hỗn hợp của checklist, loại HTML/script khỏi nội dung lưu và giữ phần Markdown hợp lệ. Tiêu đề vẫn là văn bản thuần. Không cho HTML thô đi tới renderer hoặc email.

- [x] Tạo regression ở `backend/src/test/java/edu/aiplatform/announcements/` cho tạo/sửa, payload Markdown + script, HTML event handlers, link nguy hiểm, nội dung rỗng sau làm sạch và biên 200/5000.
- [x] Sửa validation/sanitization tại `backend/src/main/java/edu/aiplatform/announcements/domain/Announcement.java` và helper boundary nếu cần. Dùng bộ phân tích/làm sạch HTML đáng tin cậy; không dùng regex tự chế làm ranh giới bảo mật. Xác minh dependency thực có và tài liệu chính thức trước khi thêm thư viện nếu cần; ghi rõ thay đổi dependency.
- [x] Giới hạn độ dài đầu vào vẫn được kiểm trước làm sạch để không nhận payload quá lớn; giữ Markdown hợp lệ. Tạo và sửa phải dùng cùng quy tắc.
- [x] Giữ `AnnouncementMarkdown.tsx` với `skipHtml`, sanitizer, URL allowlist và chặn ảnh như lớp bảo vệ khi hiển thị. Bổ sung regression renderer có ý nghĩa cho script/HTML/link độc hại nếu coverage hiện tại thiếu.
- [x] Làm rõ BR-U14-02 trong tài liệu để thống nhất lựa chọn. Không phát thông báo/email mới khi sửa.

Phương án B nếu người dùng chọn giữ rule từ chối: không thay đổi chính sách nhận HTML của backend; chỉnh thông báo lỗi/ca kiểm cho đúng rule và chỉ cập nhật artifact sau khi kiểm lại hành vi đã thống nhất. Không âm thầm đánh Đạt ca cũ có mong đợi khác.

Truy vết: UC63,64; BR-U14-02,04,12; U14-02.

### Bước 4 — Gỡ blocker U09 phục vụ test U14

Đây là thay đổi liên unit có phạm vi hẹp, cần thiết cho U14-12/13; không bỏ kiểm version hoặc chấp nhận overwrite.

- [x] Tái hiện chuỗi đọc evaluation → lưu nháp → đọc lại → chốt → đọc lại → công bố bằng repository/JPA PostgreSQL thật. Xác minh đọc không làm đổi version và command trả đúng version đã commit.
- [x] Thêm regression sát nguyên nhân tại `backend/src/test/java/edu/aiplatform/grading/infrastructure/` và/hoặc application; không chỉ mock các bước tăng version của JPA.
- [x] Sửa phần được regression chứng minh trong `EvaluationJpaEntity`, `EvaluationRepositoryAdapter`, `GradingService`, query service hoặc UI version lifecycle; không sửa cả các lớp khi chưa có bằng chứng cần thiết.
- [x] Nếu sửa UI `useGradingWorkspace.ts`/`BatchActionDialog.tsx`, kiểm bản nháp được giữ khi conflict, reload lấy version mới, và save/finalize/publish dùng version mới. Trả kết quả batch theo từng mục, không tự retry ghi sau conflict.
- [x] Regression stale version vẫn nhận 409; audit/event không phát cho transaction thất bại; thông báo công bố chỉ đến đúng Sinh viên và không chứa điểm.

Truy vết: BR-U09-40,71; BR-U14-10,11,12; U14-12/13.

### Bước 5 — Kiểm các ca đã Bỏ qua

- [x] Kiểm lại U13-01,04,05,06,09,10,12,14,15,16,18,20,22 sau upload: extraction, PDF preview, phiên bản, DOCX/PPTX download, nội dung file, phạm vi, AI selector và token tải 5 phút. Sửa tiếp lỗi cùng luồng nếu tái hiện được, kèm regression theo nguyên nhân. U13-05/10/15/22 vẫn Bỏ qua có ghi chú phần chưa xác minh; U13-20 hoàn tất nhánh AI sau lưu trữ ngày 24/10.
- [x] Chuẩn bị nhóm QA và dữ liệu công bố điểm để chạy đủ U14-12/13; không sửa quyền hoặc đánh Đạt bằng mock thay test giao diện.
- [x] Kiểm reconnect ở cấp session nếu công cụ browser hỗ trợ. Nếu không, bổ sung test component/transport sự kiện reconnect và báo đúng phạm vi; không coi đó là bằng chứng đã ngắt mạng toàn trình duyệt 30 giây.
- [x] Kiểm U14-15 với Clock điều khiển trong test backend: bài ngưng không nhắc, hạn cũ không nhắc lặp, đổi hạn có đúng một lần mới và không gửi nhắc cho Quiz. Không đổi đồng hồ hoặc cập nhật tay DB live.
- [x] Ghi riêng kết quả test tự động và E2E thực; ca cần chờ hai ngày hoặc điều kiện công cụ chưa hỗ trợ vẫn phải ghi lý do rõ. Không thêm endpoint test hoặc chức năng chỉnh thời gian vào production. Báo cáo remediation-2026-10-24 ghi riêng 6 ca E2E chưa đủ điều kiện và 35 test backend skip môi trường.

### Bước 6 — Hồi quy và cập nhật artifact

- [x] Chạy bộ test frontend bị tác động, typecheck, lint, production build; chạy các test backend liên quan rồi Maven verify với PostgreSQL integration sẵn có. Ghi mọi skip môi trường, không cộng thành pass.
- [x] Rebuild/restart riêng dịch vụ local cần cho E2E, giữ volume/dữ liệu và thay đổi Docker của người dùng; không down -v, reset DB hoặc đổi credentials.
- [x] Chạy lại ba ca Lỗi và các ca bị chặn; mỗi lần cập nhật artifact có kết quả thực tế, bước tái hiện và ghi chú. Các giới hạn native PDF, signed URL, offline 30 giây và chờ 2 ngày được giữ Bỏ qua, không cộng thành Đạt.
- [x] Reload artifact, kiểm mọi trạng thái và ghi chú đã lưu; giữ snapshot trước/sau riêng, không xóa bằng chứng ban đầu. Đã đọc đủ 50 ca: 44 Đạt, 0 Lỗi, 6 Bỏ qua, không ca trống; snapshot checklist-results-after.json và ảnh artifact-u13-after.png.
- [x] Cập nhật code summary, báo cáo kiểm thử, audit và state của lát cắt này; không tự đánh toàn unit/global stage hoàn thành. Báo cáo hiện ở `../verification/u04-u13-u14-remediation-2026-10-24.md`, code summary ở `../implementation/u04-u13-u14-remediation-code-summary.md`; xem `../README.md` cho lịch sử sắp xếp tài liệu.

## 3. Ràng buộc và extension

- Security 03/09/12: không log token, OTP, cookie hoặc credentials; không đổi chính sách đăng nhập hay cấu hình hardening. Infrastructure/security-header deployment ngoài phạm vi là N/A.
- Security 04/05/08/15: giữ CSP, validation, CSRF, kiểm quyền phía server và fail closed; payload nguy hiểm không được render/đưa vào email. Bằng chứng runtime sẽ bổ sung ở bước thực thi; chưa chứng nhận compliant ở mức production.
- Resiliency 04/06: dùng build/healthcheck hiện có khi restart local, không thay kiến trúc triển khai. Rule 10: giữ timeout/cancel/failure handling, không retry vô hạn.
- PBT đã disabled trong state; không bật thêm hoặc yêu cầu opt-in mới cho bugfix này. Các extension ngoài tập rule đồ án đã chốt là N/A.
- Không commit, push, merge, sửa nhánh khác, publish hoặc đổi dữ liệu để giả tạo test pass.

## 4. Điều kiện bàn giao

Có regression chứng minh lỗi trước sửa và pass sau sửa cho CSRF, sanitization/policy đã chốt và blocker version; code đổi ít nhất cần thiết; build/check phù hợp pass; artifact và ghi chú phản ánh đúng phạm vi thực kiểm. Những giới hạn công cụ hoặc chờ thời gian không được coi là lỗi ứng dụng cần thêm tính năng để lách.

## Bổ sung trong retest — U13-04

- [x] Xác nhận UI giữ PENDING trong khi DB của PDF scan đã NO_TEXT; useMaterialResource không có polling/SSE, thay đổi nhìn thấy trước đó do refetch khi thêm tệp.
- [x] Regression không thao tác UI sau tải: test đỏ trước sửa, xanh sau sửa; thêm kiểm tra dừng timer khi unmount.
- [x] Thêm polling 5 giây chỉ khi còn học liệu ACTIVE/PENDING; không chồng request, dừng khi hoàn tất/lỗi hoặc đang mở popup/thao tác reorder; reload callback ổn định.
- [x] MaterialList/Preview 13/13 pass; typecheck và lint vùng đổi pass.
- [x] Rebuild frontend và kiểm riêng chuyển PENDING → terminal khi không có thao tác bổ sung.

## Bổ sung — người dùng kiểm PDF trên Chrome ngày 24/10

- [x] Đối chiếu xác nhận và ảnh: xem/tải PDF thường và scan ở hai vai trò; người dùng xác nhận chuyển trang 2 thành công. Chốt U13-05/10/15 dựa trên kiểm thủ công của người dùng kết hợp các nhánh đã chạy trước.
- [x] Kiểm metadata gốc: hai fixture có Title “untitled”; đối chiếu mã nguồn Chromium ưu tiên metadata Title. Không sửa application hoặc thay nội dung PDF để xử lý tên do chính fixture đặt.
- [x] Reload artifact, xác nhận ghi chú cập nhật và lưu snapshot user-retest riêng; tổng mới47 Đạt, 0 Lỗi, 3 Bỏ qua, giữ lịch sử44/0/6. Đã đọc lại 50 ca và lưu checklist-results-user-retest.json cùng artifact-u13-user-retest.png.

## Bổ sung — người dùng kiểm URL tải ngày 24/10

- [x] Ghi nhận A dùng URL gốc thành công, B mở ngay nhận404 và A mở lại sau6phút nhận404 cùng mã DOWNLOAD_TOKEN_NOT_FOUND. Chốt U13-22 với nguồn bằng chứng là người dùng; không log token.
- [x] Reload trạng thái/ghi chú, lưu checklist-results-user-url-retest.json và artifact-u13-complete.png. Tổng48 Đạt, 0 Lỗi, 2 Bỏ qua; U13 đủ22/22. Giữ các snapshot trước và ghi hướng dẫn cho hai ca U14 còn thiếu, không coi hướng dẫn là đã kiểm.

## Bổ sung — người dùng xác nhận U14-08 ngày 24/10

- [x] Nhận xác nhận “u14-08 đã ok”; đánh Đạt và ghi rõ bằng chứng là người dùng kiểm thủ công, phân biệt với test component/phiên agent.
- [x] Reload trạng thái và ghi chú; lưu snapshot offline-retest riêng và ảnh U14. Tổng mới49 Đạt, 0 Lỗi, 1 Bỏ qua (U14-15). Đã đọc đủ50 ca sau tải lại; checklist-results-user-offline-retest.json và artifact-u14-offline-retest.png được lưu.

## Bổ sung — U14-15 kiểm thủ công ngày 24/10

- [x] Ghi nhận nhắc đúng24h trước hạn, sau đó dời hạn khoảng5phút và nhận thêm một nhắc đúng mốc mới. Ghi rõ biến thể +5phút, không khẳng định đã chờ +2ngày như dữ liệu gốc.
- [x] Người dùng xác nhận Ngưng giao trước mốc nhắc, qua mốc không có thông báo. Chốt hai Mong đợi U14-15 với biến thể đổi hạn +5phút thực tế; ghi rõ +2ngày E2E chưa chạy và scanner9/9 tự động có kiểm +2ngày. Không sửa bước gốc hoặc giả định đã chờ hai ngày.
- [x] Reload trạng thái/ghi chú cuối, lưu checklist-results-final.json và artifact-u14-final.png. Đã xác nhận đủ50 ca Đạt,0 Lỗi,0 Bỏ qua và ghi chú U14-15 nêu rõ biến thể +5phút/nguồn người dùng; dữ liệu gốc +2ngày không bị khẳng định đã chạy E2E.
