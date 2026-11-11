# Sửa điểm sau công bố và chấm AI chạy nền — Kế hoạch triển khai

> **For agentic workers:** Đã triển khai inline bằng `superpowers:executing-plans` và ponytail theo phê duyệt của người dùng. Không dùng subagent; không commit/push/deploy. Xem bằng chứng xác minh và giới hạn runtime bên dưới.

**Goal:** Giảng viên lưu/chỉnh sửa điểm đến khi lớp ARCHIVED; tự chọn lần công bố đầu; điểm đã công bố cập nhật sau mỗi lần lưu. Chấm AI chạy nền, không bắt đứng chờ ở màn hình chấm.

**Architecture:** Tái sử dụng Evaluation, optimistic version, proposal/task queue và polling hiện tại. Giữ PUBLISHED khi lưu sửa điểm đã công bố; bỏ FINALIZED khỏi luồng mới. AI chỉ tạo đề xuất lưu bền vững, không tự ghi điểm/công bố hoặc ghi đè form khi trả kết quả muộn.

**Tech Stack:** Java/Spring, PostgreSQL JSONB, React/Next.js, Vitest/Testing Library, JUnit/Mockito và các primitive UI hiện có. Không thêm dependency.

**Spec:** Các mục “Quyết định nghiệp vụ” và “Tiêu chí nghiệm thu” trong chính tài liệu này; xác nhận của người dùng ngày 2026-11-11.

**Baseline đã đọc:** `2444c3f4` trên `fix/u05-u09-u15`, working tree sạch trước khi viết tài liệu. Đọc lại diff nếu HEAD thay đổi trước triển khai.

## Quyết định nghiệp vụ

1. Bỏ nút và bước **Chốt điểm**. Luồng chính gồm **Lưu** và **Công bố điểm**.
2. Chưa công bố: lưu thành DRAFT, sinh viên chưa thấy điểm/nhận xét.
3. Công bố trực tiếp bản DRAFT hợp lệ, không cần finalize trung gian.
4. Đã công bố: vẫn sửa được; mỗi lần lưu thành công cập nhật điểm, rubric, nhận xét và minh chứng sinh viên nhìn thấy. Không trả trạng thái về DRAFT và không làm điểm biến mất.
5. “Ngay mỗi lần lưu” gồm lưu chủ động và tự lưu nền 30 giây. Thay đổi chưa lưu chưa hiện cho sinh viên; chỉ cập nhật nguyên vẹn sau transaction thành công.
6. Chỉ cho ghi khi lớp OPEN, đúng quyền giảng viên hiện có và bản chấm còn hiện hành. Hết hạn/đóng nhận bài không khóa sửa điểm. Lớp ARCHIVED chỉ đọc; lớp DRAFT không có luồng chấm bài hợp lệ. ClassStatus hiện chỉ có DRAFT, OPEN, ARCHIVED; không thêm trạng thái CLOSED cho lớp.
7. Giữ audit, người sửa, thời gian, phiên bản và ghi chú tùy chọn; không đưa lại bảng lịch sử sửa điểm lên UI.
8. Giữ quy tắc nộp lại hiện hành: bản chấm bị SUPERSEDED hoặc đổi receipt không được sửa/áp dụng đề xuất cũ. Cho sửa điểm sau công bố không làm sống lại điểm của bài nộp đã mất hiệu lực.
9. **Yêu cầu AI mới nhất thay thế yêu cầu đứng chờ trước đó:** gửi yêu cầu rồi tiếp tục thao tác, chấm bài khác hoặc chuyển màn hình; backend tiếp tục xử lý.

## Phương án đã cân nhắc

| Phương án | Đánh đổi | Kết luận |
|---|---|---|
| Lưu sửa điểm PUBLISHED và sinh viên thấy ngay sau lưu | Ít thay đổi dữ liệu, cần nói rõ tác dụng của tự lưu | Người dùng đã chọn |
| Giữ một bản công bố riêng, sửa nháp rồi công bố lại | Cần snapshot/revision và thêm thao tác công bố lại | Không chọn, không triển khai |
| Chấm AI bằng request HTTP chạy lâu hoặc popup khóa màn hình | Giảng viên phải đứng chờ; dễ mất kết nối | Không chọn; giữ task queue bất đồng bộ |

**Phương án đã duyệt và triển khai:** AI hoàn tất chỉ báo có đề xuất, dùng lại “Xem đề xuất AI” và “Áp dụng vào bản nháp”. Chỉ hành động áp dụng của giảng viên mới điền form; không tự áp dụng/công bố. Điều này tránh kết quả về muộn ghi đè điểm đang sửa, đặc biệt khi điểm đã công bố sẽ cập nhật sau lưu. Không thêm một quy trình duyệt riêng.

## Giao diện dự kiến

| Tình huống | Thao tác chính | Sinh viên thấy |
|---|---|---|
| Chưa chấm/chưa lưu | Lưu, Chấm bằng AI | Chưa có điểm |
| Đã lưu, chưa công bố | Lưu thay đổi, Công bố điểm | Chưa có điểm |
| Đã công bố | Lưu thay đổi, Chấm bằng AI | Bản đã lưu mới nhất |
| Đang nhập sửa điểm đã công bố | Nhãn Chưa lưu; lưu tay hoặc tự lưu 30 giây | Bản lưu trước đó |
| Lớp ARCHIVED | Chỉ xem | Điểm đã công bố hiện hành |

- Với điểm đã công bố, dùng một câu ngắn: “Thay đổi sẽ hiển thị cho sinh viên sau khi lưu.”
- Không còn nút Chốt điểm hoặc nút Cập nhật công bố. Công bố lặp lại cùng bản trả kết quả đã công bố, không lỗi và không gửi thông báo trùng.
- Nút AI có trạng thái gọn “AI đang chấm…” cho đúng bài đang xử lý; chỉ chặn gửi trùng bài đó, không khóa form, lưu, chuyển sinh viên hoặc sidebar.
- Khi gửi thành công: toast “Đã gửi chấm AI. Bạn có thể tiếp tục làm việc.” Khi xong: báo đúng bài/sinh viên, không tự mở/đổi màn hình và không làm mất nội dung đang nhập.

## Phát hiện từ code hiện tại

- `Evaluation.requireMutableGrade()` khóa FINALIZED/PUBLISHED; `saveGrade()` luôn chuyển về DRAFT; `publishGrade()` chỉ nhận FINALIZED. Chỉ bỏ disabled ở frontend sẽ không sửa được nghiệp vụ.
- `GradingService.finalizeEvaluation()` đang chứa kiểm tra xác nhận minh chứng/điểm AI. Phải đưa điều kiện bắt buộc này vào đường công bố trực tiếp, không bỏ mất khi bỏ finalize.
- `requireCurrent()` bỏ kiểm tra receipt nhóm khi có official grade; khi cho chỉnh sửa lại phải kiểm receipt của bản nhóm trước mọi lần ghi.
- `GradingProposalTransactions` chỉ cho target PENDING/DRAFT ở khâu tạo, ghi MCP và callback; cần đồng nhất để chấm lại bài PUBLISHED không bị STALE sai.
- `useGradingWorkspace.requestProposal()` dùng busy chung; page có AiProgressModal và tự apply khi READY. Đây là phần khiến trải nghiệm vẫn phải chờ dù backend đã dùng task queue.
- `useGradingProposals` polling theo trang, dừng khi unmount; kết quả được lưu server nên không mất, nhưng cần khôi phục/theo dõi khi giảng viên chuyển bài.
- `AiProposalPanel` đã có “Áp dụng vào bản nháp”, trạng thái PENDING/READY/FAILED/STALE/REJECTED và nút kiểm tra lại; tái sử dụng các phần này.

## Global Constraints

- Người dùng đã duyệt triển khai source/test/tài liệu; không sửa dữ liệu điểm thật, commit/push, gọi AI trả phí hoặc deploy.
- Giữ scope R3/R4, owner/enrollment, validation rubric/điểm/minh chứng, receipt, optimistic version và audit.
- Không đổi policy nộp lại; không tự công bố điểm PENDING/DRAFT qua autosave, AI hoặc MCP.
- Không lưu riêng snapshot điểm công bố hoặc thêm bảng/queue/SSE/WebSocket mới.
- Security và Resiliency giữ phạm vi đã duyệt; PBT tiếp tục tắt.

## Review Focus

1. Lưu điểm đã công bố phải giữ PUBLISHED và không lộ form chưa lưu: Task 1–3.
2. Hai giảng viên/tab cùng sửa không ghi đè nhau; lưu/công bố nhóm nguyên vẹn và giữ member override: Task 2.
3. Lớp bị ARCHIVED hoặc sinh viên nộp lại trong lúc AI chạy phải chặn ghi/áp dụng muộn: Task 1, 2, 4.
4. Chuyển bài, refresh, lỗi mạng/polling không áp dụng kết quả sang sai sinh viên hoặc báo sai rằng task đã thất bại: Task 4–5.
5. Bỏ finalize không bỏ kiểm tra xác nhận AI, không hỏng bản FINALIZED cũ, batch, MCP, export hoặc phân bố điểm: Task 1–3, 6.

## Task 1 — Cho sửa điểm hợp lệ và bỏ khóa theo công bố

**Files:**
- Modify: `backend/src/main/java/edu/aiplatform/grading/model/Evaluation.java`, `EvaluationStatus.java`.
- Modify: `backend/src/main/java/edu/aiplatform/grading/service/GradingAccess.java`.
- Test: `backend/src/test/java/edu/aiplatform/grading/model/EvaluationTest.java`, `service/GradingAccessTest.java`.
- Test persistence: `backend/src/test/java/edu/aiplatform/grading/integration/GradingPersistenceTest.java`.

**Interfaces:** Giữ `saveGrade(...)`, `publishGrade(...)`, `checkVersion(...)`; không đổi kiểu điểm hay hợp đồng lưu hiện có. Đọc trạng thái lớp qua `IClassAccess.getClassRef(...)` có sẵn. Nếu cần trả quyền sửa cho UI, thêm `editable` vào `GradingViews.EvaluationDetail`, không đoán quyền chỉ từ state.

- [x] Viết RED: lưu PUBLISHED giữ PUBLISHED; lưu DRAFT vẫn riêng tư; nhận xét-only và điểm cập nhật đầy đủ; OPEN cho sửa quá hạn bài; ARCHIVED/DRAFT từ chối ghi, đọc hợp lệ vẫn được.
- [x] Sửa guard ghi ở backend; phân biệt quyền sửa hiện tại với trạng thái đã hiển thị cho sinh viên. Khi không lấy được trạng thái lớp, không mặc định cho ghi.
- [x] Giữ FINALIZED trong khả năng đọc dữ liệu cũ: coi là đã lưu chưa công bố, được sửa/công bố trực tiếp; không sinh FINALIZED mới ở luồng mới. Không xóa enum hoặc đổi hàng loạt dữ liệu cũ.
- [x] Chạy test model/access và round-trip persistence cho DRAFT, legacy FINALIZED, PUBLISHED, SUPERSEDED; xác nhận không cần migration schema trên thiết kế này.

## Task 2 — Công bố trực tiếp, cập nhật điểm đã công bố và bài nhóm

**Files:**
- Modify: `backend/src/main/java/edu/aiplatform/grading/service/GradingService.java`, `GradingCommands.java` nếu cần validation.
- Modify: `backend/src/main/java/edu/aiplatform/grading/controller/EvaluationController.java` chỉ cho tương thích finalize cũ.
- Review/modify: `backend/src/main/java/edu/aiplatform/grading/service/GradebookService.java`, `SubmissionReviewService.java` và `dto/GradingViews.java`.
- Test: `service/GradingServiceTest.java`, `GroupEvaluationTest.java`, `GradingBatchTest.java`, `GradebookServiceTest.java`, `GradeDistributionPrivacyTest.java`, `GradebookExporterTest.java` dưới thư mục test grading hiện có.

**Interfaces:** Giữ `:publish`, save/member-save và ItemVersion; publish chấp nhận DRAFT/legacy FINALIZED hợp lệ. Trả ALREADY_PUBLISHED không gây side effect cho công bố lặp. Nếu giữ endpoint `:finalize` để tương thích, nó chỉ kiểm tra bản đã lưu, không khóa; frontend mới không gọi endpoint này.

- [x] RED: công bố DRAFT không qua finalize; thiếu xác nhận AI vẫn bị chặn ở cả publish và save PUBLISHED; lặp publish không gửi thông báo trùng; save PUBLISHED cập nhật student result và phân bố đúng dữ liệu đã commit.
- [x] Chuyển kiểm tra cần thiết từ finalize sang mọi đường làm điểm hiển thị cho sinh viên: publish lần đầu và save/member-save của bản PUBLISHED. Lưu sửa PUBLISHED không đổi publishedAt lần công bố đầu. Audit lưu người/thời điểm sửa; không phát thông báo “công bố điểm” sau mỗi autosave. Không đánh dấu “đã cập nhật sau công bố” chỉ vì có lịch sử sửa nháp trước công bố.
- [x] Cập nhật bài nhóm/member trong cùng transaction; giữ override cá nhân, version lock, receipt mới nhất và quy tắc SUPERSEDED/reset khi nộp lại. Không cho lỗi giữa chừng làm một phần thành viên nhận điểm mới.
- [x] GREEN: kiểm tra student endpoints, slot result, phân bố và export; không làm lộ DRAFT, không đưa dữ liệu AI chưa áp dụng vào kết quả sinh viên. Quyền scope lớp/môn giữ nguyên.

## Task 3 — Gọn thao tác Lưu/Công bố, cho sửa PUBLISHED

**Files:**
- Modify: `frontend/src/features/grading/hooks/useGradingWorkspace.ts`, `pages/GradingWorkspacePage.tsx`.
- Modify: `components/BatchActionDialog.tsx`, `GradebookBatchControls.tsx`, `MemberContributionPanel.tsx`, `ClassGradebookTab.tsx`.
- Modify: `model/gradeActions.ts`, `model/types.ts`, `model/format.ts`, `api/index.ts`, `pages/SubmissionDetailPage.tsx` theo caller thực tế.
- Test: `frontend/tests/features/grading/components/manual-workflow.test.tsx`, `batch-action-feedback.test.tsx`, `model/gradeActions.test.ts`, `hooks/preserve-grading.test.tsx`, `pages/workspace.test.tsx`.

**Interfaces:** Gỡ finalize khỏi UI/API caller mới; chỉnh disabled theo writable/current/ARCHIVED, không theo PUBLISHED. Nhãn “Chưa lưu” là trạng thái form cục bộ, không đổi trạng thái hiển thị cho sinh viên.

- [x] RED: PUBLISHED vẫn nhập/lưu/chấm lại AI được; không có nút Chốt điểm; batch publish gọi một luồng trực tiếp; ARCHIVED chỉ đọc.
- [x] Sửa các caller, filter, nhãn và batch; xóa fallback publish → finalize → publish. Giữ xử lý version conflict, busy riêng khi save và lỗi từng item.
- [x] Giữ debounce 30 giây, Save/Ctrl+S tức thì, không reload/mất focus; thêm câu nhắc ngắn cho điểm đã công bố. Làm rõ lưu nút nào có chuyển bài, không tự đổi bài khi chỉ lưu thay đổi.
- [x] GREEN: bài cá nhân và nhóm, thao tác qua danh sách và workspace, student result cập nhật sau load/refetch kế tiếp. “Cập nhật sau lưu” không đồng nghĩa push thời gian thực vào tab sinh viên đang mở; chưa thêm realtime.

## Task 4 — Đồng nhất AI backend cho chấm lại và bảo vệ callback muộn

**Files:**
- Modify: `backend/src/main/java/edu/aiplatform/grading/service/GradingProposalTransactions.java`.
- Review/modify: `GradingProposalService.java`, `GradingMcpTools.java`, `GradingMcpBatchService.java`, `GradingMcpQueryService.java` trong cùng thư mục service.
- Test: `GradingProposalServiceTest.java`, `GradingProposalResultHandlerTest.java`, `GradingMcpToolsTest.java`, `GradingMcpReplayTest.java`, `GroupEvaluationTest.java` trong test service hiện có.

**Interfaces:** Giữ task queue và requestRef idempotency; proposal luôn gắn evaluationId, submissionHash và rubricFingerprint. READY lưu đề xuất, không gọi save/publish điểm.

- [x] RED: tạo/nhận proposal cho PUBLISHED đang writable; job không chết vì target đã công bố; đổi receipt, supersede, archive hoặc mất quyền trước callback không được ghi/áp dụng sai.
- [x] Sửa mọi allowlist target PENDING/DRAFT và điều kiện callback hiện có, giữ check hash/rubric/quyền/lớp. Giữ giới hạn và validation AI; MCP không có quyền lưu/công bố điểm chính thức.
- [x] Bảo đảm job tiếp tục sau khi frontend unmount; requestRef retry không tạo thêm task/tính phí lần nữa. Trong một phiên UI không gửi lại cùng evaluation khi còn proposal PENDING đang theo dõi; không mở rộng thành hệ thống khóa toàn lớp.
- [x] GREEN: READY/FAILED/STALE/REJECTED, token/quota/timeout thực từ provider, không suy diễn hết token từ lỗi schema hoặc tổng điểm.

## Task 5 — Chấm AI nền trên UI, không ghi đè form

**Files:**
- Modify: `frontend/src/features/grading/pages/GradingWorkspacePage.tsx`, `hooks/useGradingWorkspace.ts`, `hooks/useGradingProposals.ts`.
- Modify: `frontend/src/features/grading/components/GradingWorkspaceHeader.tsx`, `AiProposalPanel.tsx`.
- Create if needed: `frontend/src/features/grading/components/GradingAiTaskTracker.tsx` và `model/gradingAiTasks.ts`; gắn tracker vào `frontend/src/app/teaching/layout.tsx` trong layout đã xác thực.
- Reuse: `frontend/src/shared/ui/Toast.tsx`; không tạo modal/toast mới hoặc framework task chung.
- Test: `frontend/tests/features/grading/pages/workspace.test.tsx`, `hooks/preserve-grading.test.tsx`, `components/ai-source-integration.test.tsx`; tạo `hooks/background-ai.test.tsx` nếu cần tách test vòng đời tracker.

**Interfaces:** `requestProposal(...)` không dùng busy toàn form; dùng trạng thái gửi riêng theo evaluation. Tracker chỉ theo dõi ID task/proposal của người dùng hiện tại, không lưu bài làm/điểm vào browser storage. Server là nguồn trạng thái bền vững.

- [x] RED: trả PENDING rồi vẫn nhập/lưu/chuyển sinh viên được; không có popup khóa. READY không auto apply, auto save, auto publish, chuyển trang hoặc sửa form của bài khác.
- [x] Bỏ AiProgressModal và auto-apply trên READY khỏi page; dùng nút/trạng thái gọn, existing panel/Apply. Chống double click và callback của request cũ cập nhật state màn hình mới.
- [x] Theo dõi các task đã gửi ở layout giảng dạy để chuyển bài không mất thông báo. Mỗi evaluation đang theo dõi có một chủ sở hữu polling; page/hook đọc chung trạng thái thay vì tạo thêm timer cho cùng task. Chỉ báo hoàn tất một lần/đúng bài; khi rời cả khu vực giảng dạy hoặc đóng tab, backend vẫn chạy, quay lại lấy trạng thái từ server. Nếu dùng sessionStorage để phục hồi danh sách theo dõi, chỉ giữ ID, key theo tài khoản và xóa khi logout/đổi tài khoản.
- [x] Giữ retry/backoff/budget polling, không polling trùng vô hạn; timeout/mất kết nối của browser chỉ báo chưa lấy được trạng thái, không đổi task server sang FAILED hoặc tự gửi job mới. Tái sử dụng Kiểm tra lại.
- [x] GREEN: từ bài A sang B, sửa B trong khi A READY không đổi B; trở lại A nhìn thấy kết quả; refresh khi PENDING; lỗi mạng rồi phục hồi; nhấn Apply rõ ràng mới đổi form. Điểm đã công bố chỉ đổi sau lần lưu thành công tiếp theo.

## Task 6 — Kiểm thử liên luồng và cập nhật tài liệu

**Files:**
- Update spec: `aidlc-docs/construction/u09-assignment-grading/functional-design/business-rules.md`, `business-logic-model.md`, `domain-entities.md`.
- Update: mô tả workflow MCP liên quan GRADE_WITH_RUBRIC nếu còn chỉ dẫn chốt điểm; `aidlc-docs/aidlc-state.md`, `aidlc-docs/audit.md`.
- Create evidence only after implementation: `aidlc-docs/construction/build-and-test/2026-11-11-editable-grades-and-background-ai.md`.

- [x] Chạy các test mới RED trước, GREEN sau từng task; kiểm tra caller còn FINALIZED/finalize/hasOfficialGrade trước khi kết luận đã bỏ khóa. Không xóa các dấu trạng thái phục vụ dữ liệu cũ hoặc superseded.
- [x] Chạy `npm test -- tests/features/grading tests/features/assignmentwork --maxWorkers=1`, typecheck và lint scope thay đổi; thêm layout/identity tests nếu sửa layout/auth lifecycle.
- [x] Chạy Maven grading/architecture và persistence PostgreSQL thực; thêm scope identity/academics/notifications nếu chỉnh các unit đó. Chạy production frontend build và required backend checks theo CI; không chấp nhận test DB bị skip.
- [x] UI kiểm chứng bằng fixture/tài khoản test: chưa công bố → lưu → sinh viên chưa thấy → công bố → sửa/lưu → sinh viên thấy điểm mới → chấm AI nền/chuyển bài → áp dụng đúng bản → ARCHIVED chặn ghi. Không đổi điểm thật để test.
- [x] Ghi test counts, hash đã kiểm và giới hạn thực tế. Chỉ commit/push/deploy khi người dùng yêu cầu.

## Tiêu chí nghiệm thu

- Không còn bước Chốt điểm trong luồng mới. Bản FINALIZED cũ vẫn đọc, sửa/công bố được khi đúng điều kiện.
- Công bố lần đầu là hành động của giảng viên; chấm thủ công/AI/lưu nháp không làm sinh viên thấy bài chưa công bố.
- Sau công bố, sửa điểm/nhận xét và lưu thành công vẫn PUBLISHED; sinh viên thấy nội dung mới khi tải/refetch, không bị mất điểm tạm thời.
- Khi lớp OPEN, hết hạn bài vẫn sửa được. ARCHIVED không ghi được qua UI, API, batch hoặc MCP/callback.
- AI được gửi đi và chạy tiếp khi chuyển bài. Thao tác khác không phải chờ task READY; không modal khóa, không auto-apply/công bố, không nhầm sinh viên và không mất form.
- Điểm nhóm/member, dữ liệu cũ, version conflict, resubmission, export, phân bố và kiểm soát AI được test bao phủ.

## Giới hạn và trạng thái

Đã triển khai đủ sáu task theo phê duyệt, gồm Apply thủ công/thông báo. Backend verify và frontend test/build đều qua; xem [bằng chứng xác minh](../../build-and-test/2026-11-11-editable-grades-and-background-ai.md). Không thay đổi điểm thật, gọi AI trả phí hoặc commit/push/deploy. Kiểm duyệt tự động chặn dừng/khởi động lại backend local với lý do “blocked by policy”; không tiến trình nào bị dừng bởi lệnh bị chặn. Cần restart backend thủ công để nạp mã mới; chưa nghiệm thu browser trên backend vừa cập nhật. Test UI dùng fixture và workflow tích hợp dùng PostgreSQL thực. Không thêm realtime/hủy task hoặc thay chính sách giữ điểm nhóm official khi nộp lại.

## Implementation ledger — 2026-11-11

- Subsequent authorization: “Giúp tôi commit rõ ràng”. Commit source/test riêng với tài liệu; không push/deploy. Backend được người dùng restart thủ công; nghiệm thu UI local sau đó đã qua cho sửa điểm công bố, sinh viên xem chi tiết, AI nền và sửa đồng thời, Apply, tự lưu30 giây không reload. Trả điểm/nhận xét gốc; đề xuất AI test giữ trong lịch sử. Các ghi chú runtime bị chặn ở trên mô tả thời điểm xác minh ban đầu.

- Authorization: người dùng yêu cầu `$ponytail:ponytail triển khai theo kế hoạch`, bao gồm đề xuất Apply rõ ràng/AI nền đã trình bày. Không dùng subagent; giữ các sửa tài liệu chưa commit.
- Ruling: dùng workspace hiện tại vì nhánh fix đã có tài liệu kế hoạch đang sửa; không tạo worktree hay đổi nhánh — tránh chia đôi trạng thái được duyệt.
- Ruling: tái sử dụng proposal/task queue; chuyển polling hiện có sang module gradingAiTasks và hook subscribe chung, không tạo backend task/SSE/framework mới. Không dùng browser storage; refresh khôi phục từ server khi mở bài, theo dõi/thông báo tồn tại trong layout giảng dạy.
- Ruling: baseline2444c3f4/U12 giữ nguyên điểm nhóm official khi nộp lại (BR-U09-50 được duyệt 2026-11-10). Giữ chính sách đó, không reset hoặc tạo vòng chấm mới; mọi sửa vẫn kiểm receipt hiện hành. Bản nhóm giữ receipt cũ không được sửa nếu receipt đã đổi.
- Ruling: xuất bảng điểm cho giảng viên phải gồm DRAFT đã lưu vì FINALIZED không còn là bước mới. Giữ student endpoints chỉ đọc PUBLISHED; không buộc công bố để xuất nháp. RED xác nhận file xuất nháp từng bị trống điểm.
- RED: editable model/access8 cases có3 fail/1 error; direct publication service test lỗi; UI2 failures/39; tracker chưa có module; account cleanup race1 fail/3; staff draft export1 fail.
- GREEN trung gian:10 model/access/service cases;207/208 connected frontend (assert nút chốt cũ sau đó được sửa); full frontend956/134 files; final62 UI/background cases sau sửa race và batch pending.
- Full backend lần đầu vượt heap256MB sau1285 cases không lỗi; đổi giới hạn tiến trình test sang768MB, không đổi cấu hình app. Lần kế2146 cases có3 assert cũ về xuất FINALIZED/khóa điểm tích hợp; đã cập nhật theo nghiệp vụ mới và chạy lại đầy đủ.
- Security/Resiliency: giữ phân quyền, fail-closed class state, receipt/version, đánh giá AI và isolation theo tài khoản; callback/cleanup từ tài khoản cũ không xóa cache tài khoản mới. PBT giữ tắt.
- Xác minh cuối: Maven verify2146 test,0 fail/error/skip và đóng gói JAR; required integration/traced-suite guard qua. Frontend toàn bộ956 test/134 file; sau các bổ sung cuối,211 test/25 file liên luồng và39 test workspace qua (các số đếm có giao nhau). TypeScript, scoped ESLint, production build và whitespace đều qua. Baseline2444c3f4, changes chưa commit.
- Kiểm tra provider/quota/error và vòng đời UI bằng fixture; không thực hiện AI trả phí hay sửa điểm thật. Backend local restart bị kiểm duyệt tự động từ chối; đây là giới hạn nghiệm thu runtime, không phải test/build lỗi.
