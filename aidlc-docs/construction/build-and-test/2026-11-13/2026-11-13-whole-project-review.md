# Báo cáo code, kiến trúc, bug và UI/UX — 13/11/2026

## 1. Kết luận

**Dự án có nền tảng tốt nhưng chưa hoàn toàn clean, còn bug và chưa thể coi UI/UX đã tối ưu.** Đã tái hiện 3 lỗi chức năng bằng component test cô lập: 2 lỗi mất nội dung chưa lưu và 1 lỗi đếm thiếu dữ liệu. Ngoài ra còn rủi ro cấu hình stub phân quyền cho phép mặc định khi thiếu implementation thật.

Kiến trúc modular monolith theo 15 unit phù hợp với cấu trúc nghiệp vụ hiện tại. Chưa có bằng chứng cần chia microservice hoặc viết lại toàn bộ. Ưu tiên sửa tính toàn vẹn dữ liệu và cơ chế thất bại an toàn, sau đó mới giảm độ phức tạp của các file lớn.

| Mã | Mức | Phát hiện | Bằng chứng |
|---|---|---|---|
| BUG-01 | P1 | Rời màn chấm điểm qua liên kết trong ứng dụng làm mất thay đổi chưa tự lưu | Tái hiện component + vòng đời unmount |
| BUG-02 | P1 | Nội dung nhập thêm trong lúc bấm “Xong” bài nhóm không được lưu nhưng mục vẫn hoàn thành | Tái hiện với request lưu bị trì hoãn |
| BUG-03 | P2 | Tổng quan môn đếm thiếu khi có hơn 100 lớp đang mở | Tái hiện fixture 101 lớp |
| RISK-01 | P2 | Thiếu bean phân quyền thật có thể kích hoạt stub luôn cho phép | Code/config và test stub hiện hữu |
| DEBT-01 | P3 | Persistence chưa thống nhất JDBC; Hibernate/JPA còn lộ ra ngoài repository | Code và quyết định persistence của dự án |
| DEBT-02 | P3 | Một số component/service gánh nhiều trách nhiệm, khó kiểm soát trạng thái | Inventory và các luồng mất dữ liệu đã tái hiện |
| HYGIENE-01 | P4 | Hai file cache Vitest dưới node_modules đang được Git theo dõi | `git ls-files` |

P1: nên sửa trước đợt phát hành tiếp theo vì có thể mất công việc của người dùng. P2: cần xử lý sớm, có điều kiện kích hoạt cụ thể. P3/P4: nợ kỹ thuật hoặc cải thiện, không đồng nghĩa ứng dụng đang hỏng.

## 2. Baseline và phạm vi

- Nhánh `feat/ai-prompt-guardrail`, HEAD lúc bắt đầu `af245ac3`, bao gồm các thay đổi chưa commit. Có phiên khác sửa backend trong lúc review.
- Inventory: **768 file Java production, 479 file frontend TS/TSX/CSS, 277 class test backend, 146 file test frontend**. Không tính dependency và output build là code ứng dụng.
- Đã quét toàn cây source/config, đọc các luồng rủi ro và chạy bộ test hiện hữu. Đây là review theo luồng và rủi ro, **không phải cam kết đã đọc từng dòng của 1.247 file production**.
- Các nhóm được đối chiếu: U01 identity/phân quyền; U02 audit; U03 file/job/settings/backup; U04–U05 môn/lớp/ghi danh; U06 AI/quota/retry; U07–U09 đề/bài làm/chấm; U10–U11 quiz; U12 bài nhóm; U13 học liệu; U14 thông báo; U15 dashboard. Kiểm tra sâu tập trung ở lưu dữ liệu, chuyển trạng thái, scope lớp, boundary và UI dùng chung.
- Dùng AI-DLC context, code conventions và persistence conventions hiện hữu. Không thay đổi yêu cầu nghiệp vụ hay mã ứng dụng trong đợt review này.

## 3. Lỗi cần sửa

### BUG-01 — P1: Mất thay đổi chấm điểm khi đi về bảng điểm

**Vị trí:** `frontend/src/features/grading/hooks/useGradingWorkspace.ts:55`, `:131`; `frontend/src/features/grading/pages/GradingWorkspacePage.tsx:214`; `frontend/src/features/assignments/components/assignmentNav.tsx:57`.

Hook chỉ chặn đóng/reload tab qua `beforeunload`, và hỏi xác nhận với điều hướng bài trước/bài sau thông qua `navigateEvaluation`. Link “Chấm điểm” ở sidebar là `BackLink` bình thường, không gọi guard. Bộ tự lưu đợi 30 giây sau lần sửa cuối và hủy timer khi unmount.

Tình huống: giảng viên sửa nhận xét/điểm, rồi bấm link về bảng điểm trước 30 giây. Client navigation không phát `beforeunload`; form bị unmount, timer bị hủy, không có request save. Mở lại bài nhận dữ liệu cũ.

**Kiểm chứng:** probe sửa nhận xét, phát click liên kết có thể hủy, xác nhận click không bị chặn và không có dialog, mô phỏng route unmount, tiến thời gian thêm 31 giây. Không gọi `saveEvaluation`; render lại nhận `original`. Đây là component/lifecycle reproduction, chưa phải browser E2E.

**Hướng sửa:** một guard thống nhất cho mọi đường rời workspace, gồm link sidebar, logo, điều hướng lịch sử và router; cho phép lưu thành công hoặc xác nhận bỏ thay đổi trước khi rời. Giữ cơ chế tự lưu 30 giây và single-flight hiện có. Thêm regression cho chính link “Chấm điểm”, không chỉ nút bài trước/bài sau.

### BUG-02 — P1: Bấm “Xong” có thể bỏ mất nội dung vừa gõ thêm

**Vị trí:** `frontend/src/features/groupdocs/pages/GroupSectionEditorPage.tsx:114`, `:196`, `:213`, `:295`.

`done()` và `finishAll()` chờ một lần `save()` rồi gọi `finishSection`. Trong khi chờ, editor vẫn `readOnly={!editable}`; `busy/saving` không khóa editor. `save()` có giữ dirty nếu draft đã thay đổi trong lúc request chạy, nhưng caller không kiểm tra dirty đó trước khi hoàn thành mục. Reload sau hoàn thành loại mục khỏi danh sách đang giữ.

Tình huống: nhập đoạn A → bấm “Xong” → mạng đang chờ lưu A → nhập tiếp B → request A thành công → mục chuyển DONE và rời trang. B chưa từng được gửi lên server.

**Kiểm chứng:** trì hoãn response `/draft`, xác nhận ô vẫn `contenteditable=true`, gõ nội dung mới, rồi trả thành công. Chỉ có một request draft chứa A; endpoint `:done` được gọi và router về tài liệu nhóm; B không nằm trong bất kỳ payload lưu nào.

**Hướng sửa:** khi bắt đầu hoàn thành, khóa đúng vùng soạn liên quan cho tới khi kết thúc; hoặc lưu theo revision và chỉ hoàn thành khi revision mới nhất đã được xác nhận. Đồng bộ single-flight/version giữa autosave, lưu khi rời trang, “Xong” và “Hoàn thành tất cả”.

### BUG-03 — P2: Số liệu tổng quan môn bị giới hạn ở 100 lớp

**Vị trí:** `frontend/src/features/academics/components/SubjectClassesOverview.tsx:13`, `:37`; `frontend/src/features/academics/api/index.ts:31`; backend `AcademicsService.java:67`.

Overview chỉ gọi trang 1 với `size:100`, rồi lấy `classes.length`, tổng `activeStudentCount` và số lớp chưa có giảng viên làm số liệu toàn môn. API thực sự phân trang, không trả tất cả lớp.

**Kiểm chứng:** fixture có 101 lớp mở, mỗi lớp 10 sinh viên. Màn hình hiện **100 lớp / 1.000 sinh viên**, thay vì 101 / 1.010; API chỉ được gọi một lần. Thẻ thông tin môn dùng `classCounts` độc lập còn có thể hiển thị số khác ngay trên cùng màn hình.

**Hướng sửa:** lấy số liệu qua API aggregate theo môn/trạng thái; danh sách có phân trang hoặc chỉ rõ “100 lớp đầu”. Nếu tạm tải đủ trang, cần dùng helper chung và giới hạn rõ chi phí tải. `ClassNavTree` cũng chỉ lấy 100 lớp (`ClassNavContexts.tsx:162`), cần kiểm tra cùng đợt để lớp ngoài trang đầu vẫn tìm được trong menu.

## 4. Kiến trúc và độ clean

### RISK-01 — P2: Stub phân quyền fail-open

`IdentityContractStubs.java:29–36` dùng `matchIfMissing=true`; khi không có bean thật, `IAuthorization` trả `AuthorizationDecision.allow()`. Các stub scope môn/lớp cũng trả `true`. Quét thấy 14 cấu hình stub mặc định bật; cấu hình ứng dụng/infra không đặt `platform.contract-stubs.enabled=false`.

`ContractStubsTest.stubsAllowPermissionsAndReturnEmptyLists` còn xác nhận hành vi cho phép đó. Rủi ro xuất hiện khi bean thật bị bỏ sót/đổi điều kiện kích hoạt: app có thể tiếp tục chạy với quyền quá rộng thay vì lỗi startup. **Chưa chứng minh hệ thống đang chạy thực tế sử dụng stub hoặc đang bị vượt quyền.**

Nên tắt stub mặc định trong runtime và chỉ bật rõ ràng ở test/dev; thêm full-context assertion bảo đảm các contract quan trọng dùng implementation thật. Không xóa hàng loạt stub khi chưa xác định các test fixture đang phụ thuộc.

### Điểm đang tốt

- Có 15 logical unit và rule ArchUnit kiểm tên interface/ràng buộc truy cập nội bộ theo unit.
- Frontend `shared` không import `features` trong phép quét import trực tiếp.
- Có API client chung, refresh session single-flight, CSRF; validation/authorization và regression theo nghiệp vụ đã được đầu tư.
- Các luồng dữ liệu dùng version/CAS, transaction, job/retry và outbox thay vì chỉ dựa vào UI.
- Production frontend build, typecheck và ESLint qua trong lượt này. Test xanh là nền tảng tốt nhưng ba probe mới cho thấy vẫn còn khoảng trống về tương tác bất đồng bộ.

### Đính chính báo cáo kiến trúc ngày 12/11

`ArchitectureTest.UNIT_OF_PACKAGE` và `unit-of-work.md` quy định `groups/groupdocs` cùng U12; `files/jobs/settings/backup` cùng U03. Do đó import nội bộ giữa các package này **không tự động vi phạm quy tắc “unit khác chỉ dùng contract”**. Không giữ ARCH-02/ARCH-04 cũ như hai lỗi boundary đã được xác nhận. Coupling nội bộ vẫn có thể giảm khi chỉnh sửa use case, nhưng cần đánh giá theo trách nhiệm và chiều phụ thuộc thực tế.

### Nợ kỹ thuật còn lại

- **DEBT-01:** U04, phần module của U13 và U14 còn JPA, đúng khoảng cách đã ghi trong `persistence-conventions.md`. Có 12 file import JPA trực tiếp; thêm `SubjectCommandService.java:73` bắt exception Hibernate bằng tên đầy đủ, `NotificationExceptionHandler.java:3` import `PersistenceException`. Nên chuyển theo unit với test transaction/lock/version; chưa loại bỏ transaction manager/ORM chung khi các owner còn dùng.
- **DEBT-02:** `DocumentEditor.tsx` khoảng 1.968 dòng, `AssignmentFormPage.tsx` 867 dòng; nhiều service tài liệu/AI khoảng 600–800 dòng. Không coi số dòng tự nó là bug. Cần tách phần orchestration lưu/điều hướng, parser/serializer và UI thuần ở nơi trách nhiệm đang trộn, ưu tiên khu vực đã phát sinh race. Một số dependency service còn tên ngắn như `quota`, `usage`, `tasks`, chưa thống nhất quy ước tên; áp dụng dần khi chạm code.
- **HYGIENE-01:** Git đang track `backend/node_modules/.vitest-cache/CACHEDIR.TAG` và `_metadata.json`. Nên bỏ theo dõi và bổ sung ignore phù hợp, không cần thay hành vi ứng dụng.

## 5. Đánh giá UI/UX

**Chưa thể kết luận “đã tối ưu”.** Hai lỗi mất dữ liệu là vấn đề trải nghiệm lớn hơn các thay đổi thẩm mỹ. Sai số tổng quan làm giảm độ tin cậy thông tin.

| Khía cạnh | Đánh giá từ code/test | Việc nên làm |
|---|---|---|
| Bảo toàn công việc | Guard và autosave chưa đồng nhất giữa màn chấm, quiz, bài nhóm | Sửa BUG-01/02; test latency/error/navigation trong một ma trận chung |
| Loading/error/retry | Có `PageLoading`, `PageLoadError`, toast lỗi bền và nút thử lại ở nhiều màn | Kiểm thống nhất việc giữ form khi lỗi, tránh rời trang mà chỉ còn lỗi ở component đã unmount |
| Bàn phím/accessibility | Dialog có focus trap/restore, menu hỗ trợ phím, skip link; Field nối lỗi, Switch có tên; nhiều bảng có button/link trong ô | Browser audit với Tab/Shift+Tab/Escape và screen reader; chưa chứng nhận WCAG |
| Responsive | Bảng bọc overflow, pagination wrap, mobile drawer và target cảm ứng trong shared UI | Kiểm màn soạn/chấm có nhiều nút ở 320/390/768/1440px; chưa xác nhận ảnh chụp/layout thực trong lượt này |
| Điều hướng/số liệu | Context sidebar và URL filter có sẵn; overview/menu vẫn có trần 100 lớp | Aggregate/count + danh sách phân trang/tìm kiếm đúng scope |
| Hiệu năng | Có lazy import viewer; vài route tải tuần tự mọi trang lớp/sinh viên rồi lọc phía client | Đo request count, thời gian tương tác với dữ liệu lớn; chuyển phân trang/search về server khi cần |
| Hướng dẫn nhập liệu | `Field` nhận `hint` nhưng chủ ý không render; một số caller còn truyền hướng dẫn về định dạng/thẻ/giới hạn | Rà lại từng hướng dẫn thiết yếu; bỏ prop chết hoặc đặt hướng dẫn tại control. Đây là đề xuất, không coi lựa chọn tối giản hiện hữu là bug tự thân |

Không có trình duyệt khả dụng qua Computer Use trong phiên này (`iab` unavailable; inventory chỉ có MCP Apps, không có tab web). Vì vậy không gắn nhãn “đã kiểm mobile/light-dark thực tế”, không đưa điểm Lighthouse/Core Web Vitals hay tỷ lệ usability giả định.

## 6. Kiểm chứng

| Kiểm tra mới trong lượt này | Kết quả |
|---|---|
| Frontend full suite | 1.020 test / 146 file qua |
| Frontend ESLint | Qua |
| Frontend production build | Qua |
| TypeScript sau build | Qua |
| Probe BUG-01/03 | 2/2 xác nhận hành vi lỗi |
| Probe BUG-02 | 1/1 xác nhận hành vi lỗi |
| MCP hooks | 14 tình huống qua |
| Backend verify + required integration guard | Đang tổng hợp kết quả chạy riêng |
| ShellCheck lượt này | Chưa chạy: automatic approval review từ chối mount script vào image bên thứ ba; không có binary local |
| Browser E2E/mobile, load test, provider AI thật | Chưa thực hiện |

Các probe là characterization test: **PASS nghĩa là tái hiện thành công bug, không phải bug đã được sửa**. Không cộng 3 probe vào 1.020 regression test như bằng chứng sản phẩm đã đạt.

Lượt backend đầu gặp `FileAlreadyExistsException` tại output build đang được ghi đồng thời. Đã chuyển sang snapshot riêng. Lượt snapshot đầu thiếu mount fixture `../infra/postgres/init/01-roles.sh`; đây là lỗi setup của review, không phải lỗi sản phẩm. Lượt chạy lại dùng snapshot backend và infra chỉ đọc, database Testcontainers riêng. Các lỗi phân giải dependency/hoist của probe ban đầu cũng đã sửa trong harness trước khi lấy kết quả cuối.

Bằng chứng local nằm trong `.tmp/review-20261113-*`: `frontend.log`, `lint.log`, `build.log`, `typecheck.log`, `probes.log`, `group-probe.log`, `hooks.log`, `inventory.json`, backend logs và snapshot/report. Các file `.tmp` là bằng chứng local, không mặc định nằm trong commit.

## 7. Lỗi đã báo trước và trạng thái hiện tại

| Lỗi ngày 12/11 | Đối chiếu source hiện tại |
|---|---|
| Thoát quiz dù lưu thất bại | Nút thoát đã kiểm kết quả `flush()`; regression nằm trong suite mới |
| Fallback quota làm rollback transaction | Có `QuotaUnavailableException` và `noRollbackFor` giới hạn đúng loại lỗi nghiệp vụ; lỗi DB không bị nuốt chung |
| Lượt quiz ở lớp cũ chặn lớp mới | Đã thêm scope class cho find/count/history và migration unique constraint; cần áp migration trong đợt triển khai được duyệt |
| Timeout tổng cắt chuỗi AI retry/fallback | Có ngân sách theo số connection, retry/backoff; dispatcher gọi timeout theo message; có xử lý interrupt |

Không báo lại bốn lỗi này như bốn lỗi mới còn nguyên. Tuy nhiên source đang được sửa đồng thời; kết quả build/test gắn với snapshot và kiểm tra delta được ghi bên dưới, không suy ra runtime local/VPS đã cập nhật. Các bản sửa Gemini thinking/diagram mới có trong source nhưng lượt này không gọi provider thật để đánh giá chất lượng chấm.

## 8. Thứ tự xử lý đề xuất

1. Sửa BUG-01 và BUG-02, thêm test điều hướng/race trên đúng component và browser E2E trước phát hành.
2. Tắt stub phân quyền ở runtime, bổ sung startup assertion; sửa số liệu và menu khi quá 100 lớp.
3. Chạy ma trận UI bằng trình duyệt: bốn role, desktop/mobile, sáng/tối, mạng chậm/mất mạng, xung đột phiên, bàn phím.
4. Refactor phần lưu/hoàn thành/điều hướng thành cơ chế dùng chung có owner rõ; tiếp tục JDBC theo unit; dọn cache Git.
5. Đo hiệu năng với dữ liệu đủ lớn và xác minh AI/provider thực tế trong đợt kiểm thử riêng.

## 9. Giới hạn và AI-DLC

Không sửa điểm, bài nộp, tài khoản hay dữ liệu nghiệp vụ thật; không gọi AI trả phí, restart ứng dụng, commit/push/deploy. Không kiểm production, CVE online, SMTP/Drive thật, tải đồng thời dài hạn, hay bảo đảm mọi bug đã được tìm thấy.

Security scope giữ SECURITY-03/04/05/08/09/12/15; RISK-01 là khoảng hở liên quan SECURITY-08/15 và chưa đủ điều kiện chứng nhận fail-closed. Source/test cung cấp kiểm tra một phần cho các control còn lại, không chứng nhận cấu hình vận hành. Resiliency giữ RESILIENCY-04/06/10: logic timeout/retry được review; deploy/health vận hành chưa xác minh ở production. Control ngoài phạm vi giữ N/A; PBT disabled. Không mở giai đoạn triển khai hay tuyên bố toàn bộ extension compliant chỉ vì đã hoàn thành báo cáo.
