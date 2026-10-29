# U09 UI Bugfix Implementation Plan

> Người dùng duyệt triển khai bằng “giúp tôi fix bug”. Thực hiện tuần tự; code và regression tests đã hoàn tất, runtime UI acceptance còn chờ.

**Goal:** Sửa các điểm U09 trong image(3).png và image(4).png: hai khung ứng dụng, UUID trong nội dung AI, thiếu tên bài/lớp và filter chưa đồng bộ.

**Architecture:** Giữ một AuthenticatedLayout tại teaching/layout.tsx. Tải metadata riêng bằng GET chi tiết bài tập sẵn có. Dùng Field/Select dùng chung; không đổi backend/contract.

**Tech stack:** Next.js App Router, React, TypeScript, Tailwind, Vitest, Testing Library.

**Spec:** Hai ảnh người dùng cung cấp và các yêu cầu/acceptance trong tài liệu này.

## 1. Kết quả kiểm tra

Ngày 2026-10-25. Nhánh mcp-test, HEAD b3068845. Working tree sạch lúc bắt đầu. Đã đọc nội dung trích xuất từ đúng hai ảnh và đối chiếu source; chưa tái hiện qua browser ở lượt này.

| Điểm báo lỗi | Bằng chứng source hiện tại | Ưu tiên |
|---|---|---|
| Header/sidebar lặp | app/teaching/evaluations/[evaluationId]/page.tsx:1-9 bọc AuthenticatedLayout, trong khi teaching/layout.tsx:8 đã bọc cùng roles. | P1 |
| Profile/count/SSE lặp | Mỗi layout tạo SessionProvider/AppShell; mỗi NotificationBell tạo EventSource riêng. Có cơ sở từ code, chưa đo số kết nối runtime. | Cùng P1 |
| UUID ở phần AI | AiProposalPanel.tsx:52 có span sr-only chứa evaluationId: đã ẩn bằng mắt nhưng vẫn ở DOM/trợ năng. Không kết luận còn hiện bằng mắt như báo cáo cũ. | P2 |
| Thiếu tên bài/lớp | SubmissionDetailPage.tsx:38 chỉ ghi “Bài nộp”; SubmissionPage không có metadata ngữ cảnh. | P2 |
| Filter khác UI chung | Hai select ở SubmissionDetailPage.tsx:68,89 dùng style riêng, dù shared/ui/inputs.tsx có Select. Native select không phải lỗi chức năng. | P3 |

Các đường dẫn không ghi prefix ở trên thuộc frontend/src/features/grading, riêng route thuộc frontend/src/app.

Điểm ngoài báo cáo cần phân biệt: AiProposalPanel.tsx:66 còn render sections[].partId. Đây là ID phần bài, khác evaluationId; nếu runtime in UUID tại đây thì ghi nhận riêng và kiểm nguồn tiêu đề phần trước khi mở rộng phạm vi.

## 2. Yêu cầu và giới hạn

- Một shell/header/sidebar; giữ parent guard TEACHER, SUBJECT_MANAGER và authorization backend.
- Không render evaluationId như nội dung, kể cả sr-only; giữ ID trong URL, callback, request.
- Khi metadata có: tiêu đề “Bài nộp — {title}”, dòng lớp “Lớp: {ownerName}”. Title rỗng dùng “Bài tập chưa có tiêu đề”; ownerName rỗng dùng “Chưa có thông tin lớp”. Không fallback sang UUID.
- Khi đang tải metadata: giữ “Bài nộp”, báo tải ngữ cảnh riêng. Metadata lỗi: “Không tải được thông tin bài tập/lớp.” và retry riêng, không chặn danh sách có quyền truy cập.
- Chỉ hiển thị metadata khi id, scope CLASS và classId khớp route. Không hiện ngữ cảnh sai lớp hay response cũ sau đổi route.
- Filter dùng Field/Select chung, giữ options/query/reset page về 1/reset selection.
- Giữ nguyên tính điểm, draft/finalize/publish, optimistic version, AI source/payload. Không thêm dependency, endpoint, schema/migration.

### Nguồn metadata đã xác minh

GET /api/v1/assignments/{assignmentId} có id/scope/classId/title/ownerName trong AssignmentDetail. Backend AssignmentSupport.java:171 ghép ownerName CLASS từ mã + tên lớp, không phải tên giáo viên. Vì vậy chỉ cần một GET, không cần tải thêm class detail hoặc lấy tên từ danh sách có phân trang.

Tại boundary U09 thêm gradingApi.assignmentContext(assignmentId: string): Promise<AssignmentContext>, dùng shared apiFetch. Kiểu cục bộ AssignmentContext gồm id: string, scope: "CLASS" | "SUBJECT", classId: string | null, title: string, ownerName: string. Đây là projection response có sẵn, không sửa backend hoặc import nội bộ module khác.

## 3. Review focus

- Mở link trực tiếp/hết phiên/sai role vẫn bị chặn: task 1 + UI acceptance.
- UUID mất ở DOM/trợ năng nhưng callback vẫn dùng đúng ID: task 2.
- Tên dài/trống/HTML render như text và màn hình 390 px: task 3/4.
- Response metadata cũ về sau khi đổi route không hiện tên sai: task 3.
- Metadata lỗi không phá danh sách; filter/pagination không tải lại metadata: task 3/4.

## 4. Triển khai tuần tự

### Task 1 — Bỏ shell lồng nhau

Files: sửa frontend/src/app/teaching/evaluations/[evaluationId]/page.tsx; thêm frontend/src/features/grading/route-shell.test.tsx.

- [x] Test RED render teaching layout cùng page đã await params; mock shell có thể đếm, mock workspace. Assert đúng một shell, trước sửa phải thất bại vì hai wrapper.
- [x] Bỏ import/wrapper AuthenticatedLayout tại page; giữ GradingWorkspacePage và evaluationId.
- [x] Test GREEN một shell, đúng evaluationId, parent vẫn nhận đủ hai roles. Không chỉ test workspace độc lập.
- [ ] Runtime production: đếm shell và SSE đang hoạt động, kiểm cleanup khi rời/chuyển bài; không assert tổng request bằng 1 trong StrictMode dev/reconnect.

### Task 2 — Bỏ UUID khỏi nội dung AI

Files: frontend/src/features/grading/AiProposalPanel.tsx, manual-workflow.test.tsx, ai-source-integration.test.tsx.

- [x] Test RED evaluationId dạng UUID không có trong container.textContent; hiện sẽ fail vì sr-only span.
- [x] Bỏ span. Giữ prop contract/call sites hiện tại, không destructure prop khi không còn dùng trong component.
- [x] Test GREEN: request vẫn có source/provider/requestRef như cũ; apply đúng checks/feedback/proposal ID; proposal chưa READY vẫn khóa.


### Task 3 — Bổ sung tên bài/lớp

Files trong frontend/src/features/grading: sửa api.ts, types.ts, SubmissionDetailPage.tsx; thêm useSubmissionContext.ts và submissions.test.tsx.

Interface: useSubmissionContext(classId: string, assignmentId: string) trả { value: AssignmentContext | null; error: string; refresh: () => Promise<void> }.

- [x] Test RED tiêu đề có tên bài/lớp; metadata lỗi vẫn hiện rows; retry chỉ tải metadata; đổi filter không tải metadata lại.
- [x] Thêm gradingApi.assignmentContext dùng apiFetch và encode assignmentId. Tải theo classId/assignmentId, clear context cũ khi key đổi, generation guard bỏ response stale/unmount; catch lỗi, không retry vô hạn.
- [x] Validate response id/scope/classId trước hiển thị; mismatch báo lỗi ngữ cảnh và không gắn tên nhầm lớp.
- [x] Hiện title/lớp/loading/fallback theo mục 2; giữ link quay lớp và mô tả Practice/Quiz.
- [x] Test GREEN tên dài/rỗng/HTML, pending, 403/404/503, route đổi khi request cũ chưa hoàn tất. Shared client vẫn xử lý 401; metadata không cấp quyền cho API danh sách.

### Task 4 — Đồng bộ filter và hồi quy

Files: SubmissionDetailPage.tsx, submissions.test.tsx; dùng shared/ui Field/Select có sẵn, không sửa component chung nếu không cần.

- [x] Dùng Field/Select với id/htmlFor cho “Trạng thái”, “Nộp bài”; giữ keyboard/focus và values/options.
- [x] Test đổi status/late gửi đúng query, reset page/selected; pagination đúng; chưa nộp không chọn được; batch giữ evaluationId/version.
- [x] Test metadata không refetch theo filter/page, không bị xóa khi list refresh lỗi.
- [ ] UI desktop/390 px: title/filter không tràn, bảng cuộn trong khung, keyboard chọn option được.


## 5. Verification

Baseline thực chạy, không phải nghiệm thu lỗi:

- Lần đầu: 11 notification tests đạt; 3 suites grading lỗi import ENOENT trong cache tạm Vitest, chưa chạy test body.
- Chạy lại với TEMP/TMP riêng writable và --maxWorkers=1, Node v25.8.1: **4 suites / 27 tests passed**, exit 0, 6.46 giây.
- Suites: grading/workspace.test.tsx, grading/manual-workflow.test.tsx, grading/ai-source-integration.test.tsx, notifications/Notifications.test.tsx.
- Chưa viết regression mới, chưa chạy full suite/build/browser trong lượt lập plan.

Sau sửa, chạy từ frontend; dùng TEMP/TMP writable ổn định nếu cache tiếp tục lỗi:

1. npm test -- src/features/grading src/features/notifications/Notifications.test.tsx --maxWorkers=1
2. npm run typecheck
3. npm run lint
4. npm test -- --maxWorkers=1
5. npm run build
6. UI production: Teacher/Subject Manager có quyền → lớp → assignment → bài nộp → Chấm / xem; mở link trực tiếp, đổi bài/quay lại. Đếm shell/profile/count/SSE, kiểm title/context/filters. Anonymous/Student bị chặn. Không cần gọi AI trả phí hoặc công bố điểm để xác minh các lỗi UI này.

Nghiệm thu khi regression và kiểm tra trên đạt, browser xác nhận. Runtime không sẵn có phải ghi “chưa kiểm thử UI”, không đổi verdict thành Pass.

## 6. AI-DLC và bàn giao

Tiếp tục Construction đã có, dùng remediation plan theo yêu cầu; không reset stages/stories toàn dự án. Trình tự: task 1 → 2 → 3 → 4 → automated verification → browser acceptance. Không cần thiết kế hệ thống/schema/infrastructure mới. Đây là text workflow thay cho sơ đồ không cần thiết.

Security reduced subset: SECURITY-05/08/12 giữ shared API, scope và parent guard; SECURITY-15 và RESILIENCY-10 yêu cầu lỗi metadata được cô lập, loại response stale. SECURITY-03/04/09, RESILIENCY-04/06: N/A với kế hoạch UI, không đổi vận hành. Các rule ngoài reduced subset N/A theo state; PBT disabled, không bật extension mới. Đây là đối chiếu thiết kế, chưa chứng nhận runtime.

Rủi ro chính: mất guard khi bỏ wrapper, ngữ cảnh sai lớp/stale, regression filter/selection; các task có gate tương ứng. Có thể chia hai commit khi được yêu cầu: shell + UUID; context + filters. Không tự commit/push/deploy. Rollback bằng revert từng commit frontend; không có migration.

Self-review: đủ mọi điểm trong hai ảnh; phân biệt UUID sr-only với lỗi nhìn thấy cũ; nguồn title/lớp đã kiểm backend; baseline không thay regression/UI acceptance. Không mở rộng sửa partId khi chưa có bằng chứng.
