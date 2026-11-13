# Báo cáo triển khai — Sổ điểm và bố cục Thông tin lớp (U09)

- **Ngày thực hiện**: 2026-10-25
- **Trạng thái**: **Code complete / chờ người dùng test UI**
- **Nhánh / HEAD**: `mcp-test` @ `ea0d2c8e`
- **Tài liệu kế hoạch**: [`aidlc-docs/construction/plans/code-generation-plan/2026-10-25-u09-gradebook-class-layout-code-generation-plan.md`](../plans/code-generation-plan/2026-10-25-u09-gradebook-class-layout-code-generation-plan.md)
- **Hướng dẫn kiểm thử thủ công (40 ca UI)**: [`aidlc-docs/construction/build-and-test/2026-10-25-u09-gradebook-class-layout-manual-test-guide.md`](./2026-10-25-u09-gradebook-class-layout-manual-test-guide.md) — **tất cả 40 ca đang ở trạng thái `Chưa test` để người dùng trực tiếp nghiệm thu**.

---

## 1. Tóm tắt thay đổi mã nguồn

### 1.1. Sổ điểm (`ClassGradebookTab` & `GradebookExportDialog`)
- **Mặc định chế độ `Theo bài tập` (`focused`)**:
  - Hiển thị bộ chọn `Bài tập` với tên bài đầy đủ (dùng `assignment.id` làm định danh, hỗ trợ trùng tiêu đề hoặc tiêu đề rất dài) và tiêu đề bài đang chọn kèm liên kết `Xem danh sách bài nộp`.
  - Tự động chọn bài đầu tiên nếu chưa chọn hoặc nếu bài đang chọn không còn trong dữ liệu mới tải về.
  - Bảng 4 cột gọn: `Sinh viên`, `Điểm`, `Trạng thái`, `Thao tác`. Ô điểm và trạng thái được ánh xạ theo `assignmentId` (qua `Map`), không phụ thuộc thứ tự phần tử trong mảng `grades`.
  - Phân biệt rõ điểm `0.00 / max` thực tế với trạng thái chưa có đánh giá (`—`), chỉ hiển thị liên kết chấm/xem điểm khi có `evaluationId`.
- **Chế độ `Tổng hợp` (`matrix`)**:
  - Chuyển qua lại bằng cụm nút `Theo bài tập` / `Tổng hợp` (`aria-pressed`), giữ nguyên từ khóa tìm kiếm sinh viên và trang hiện tại; đổi từ khóa tìm kiếm sẽ đưa về trang 1; đổi `classId` sẽ đặt lại trạng thái theo lớp mới.
  - Bảng tổng hợp giới hạn vùng cuộn riêng (`max-h-[65vh] overflow-auto`), cột `Sinh viên` cố định bên trái (`sticky left-0`, ~`220px`) và tiêu đề cố định phía trên (`sticky top-0`) với nền đặc và `z-index` phân tầng rõ ràng; mỗi cột bài rộng cố định `160px`, tiêu đề có nhãn thứ tự `Bài 1`, `Bài 2`, … giới hạn 2 dòng (`line-clamp-2`).
  - Bổ sung nút đóng/mở `Danh mục bài tập (N)` ngay phía trên bảng tổng hợp để truy cập đầy đủ tên bài dài và liên kết bằng chuột, bàn phím hoặc màn hình cảm ứng (không phụ thuộc hover `title`).
- **Xuất sổ điểm (`GradebookExportDialog`)**:
  - Giữ nguyên bộ chọn phạm vi (`Toàn bộ bài Graded` hoặc `1 bài tập`) và định dạng (`CSV` / `XLSX`), bổ sung ghi chú rõ ràng rằng phạm vi xuất áp dụng cho toàn bộ sinh viên trong lớp theo lựa chọn trong hộp thoại, không bị cắt theo ô tìm kiếm hay trang đang xem.

### 1.2. Bố cục Thông tin lớp cho giảng viên (`ReadOnlyClassPage`, `academics.module.css`, `TeacherGroupWidgets`, `ClassProgressSection`)
- **Giữ nguyên kiến trúc Slot liên đơn vị (`teacherClassTabs`) và giao diện sinh viên (`audience="student"`)**:
  - `academics` không import trực tiếp `grading` hay `groups`; `SlotOutlet` dùng chung không bị gắn logic cụ thể của từng unit.
  - Trong `ReadOnlyClassPage` khi `audience === "teacher" && tab === "info"`, sử dụng lưới `.teacherOverviewGrid` (`data-teacher-overview="true"`) đặt các vùng theo `data-class-overview-region`:
    - Hàng tóm tắt (4 thẻ trên desktop):
      1. `Sinh viên` (`data-class-overview-region="students"`): số sinh viên đang hoạt động (`ACTIVE`), không cộng sinh viên đã gỡ (`REMOVED`).
      2. `Nhóm` (`data-class-overview-region="groups"`, `GroupCountCard` với `variant="overview"`): hiển thị `Đang tải…` hoặc `Không khả dụng` khi chưa có dữ liệu/lỗi (không bịa `0`), và hiển thị số nhóm cùng số sinh viên chưa có nhóm khi tải xong.
      3. `Bài đang mở` (`data-class-overview-region="open"`): tổng số bài đang mở, số bài hết hạn hôm nay, và ghi chú `{hiddenCount} bài ẩn theo quyền` nếu `openAssignments.hiddenCount > 0`; hiển thị `Đang tải…` / `Không khả dụng` khi đang tải hoặc lỗi.
      4. `Chờ chấm` (`data-class-overview-region="pending"`): tổng số bài nộp ở trạng thái `PENDING` từ `progress` summary; hiển thị `Đang tải…` / `Không khả dụng` khi đang tải hoặc lỗi.
    - Thân trang trên desktop (`> 1100px`):
      - Cột trái 1/3 (`data-class-overview-region="metadata"`): danh sách thông tin lớp gọn (`Môn`, `Mã lớp`, `Học kỳ`, `Giảng viên`, `Trạng thái`, `Mô tả` có nút `Xem đầy đủ` / `Thu gọn` khi văn bản dài hơn 140 ký tự, hiển thị thuần văn bản an toàn).
      - Cột phải 2/3 (`data-class-overview-region="progress"`): tiến độ nộp bài và phân bố điểm đã công bố.
    - Trên màn hình hẹp / di động (`<= 1100px` và `<= 720px`): tự động xếp theo thứ tự `summary cards` → `progress` → `metadata`.

### 1.3. Tiến độ nộp bài & Phân bố điểm tải lười (`ClassProgressSection`)
- **Sắp xếp & thu gọn danh sách tiến độ**:
  - Ưu tiên bài còn `PENDING > 0` lên trước, sau đó theo hạn nộp (`deadline`) gần nhất, bài không có hạn nộp xếp cuối, tie-break ổn định theo `id`.
  - Mặc định hiển thị tối đa `5` bài, có nút `Xem tất cả (N)` / `Thu gọn` (`aria-expanded`).
  - Hiển thị đúng đơn vị `nhóm` (bài `GROUP`) hoặc `sinh viên` (bài `INDIVIDUAL`), liên kết `Xem bài nộp`, thanh `<progress>` theo màu chủ đạo (`accent-primary`), xử lý an toàn mẫu số `total === 0` (`0 / 0 ... (0%)`, `max={1}`, `value={0}`, không sinh `NaN`/`Infinity`/`100%`) và hiển thị `Chưa có dữ liệu` khi chưa có bản ghi tiến độ.
- **Phân bố điểm đã công bố (`Phân bố điểm đã công bố`)**:
  - Mặc định đóng (`aria-expanded="false"`), **không** gọi API `gradingApi.classDistribution` khi mới mở tab Thông tin lớp.
  - Chỉ khi người dùng bấm mở `Phân bố điểm đã công bố`, hệ thống mới hiển thị bộ chọn `Bài tập đã công bố` (`aria-label="Bài tập xem phân bố điểm"`) và gọi `classDistribution` cho đúng 1 bài được chọn.
  - Khi đổi bài hoặc đổi lớp, dữ liệu cũ được xóa ngay lập tức; nếu mảng trả về rỗng `[]` (chưa đủ dữ liệu hoặc bị ẩn theo ngưỡng riêng tư), hiển thị thông báo `Chưa đủ dữ liệu công bố hoặc ẩn theo ngưỡng riêng tư để hiển thị phân bố điểm.`.

---

## 2. Danh sách file thay đổi / tạo mới

### File mã nguồn đã cập nhật
1. `frontend/src/features/grading/ClassGradebookTab.tsx`
2. `frontend/src/features/grading/GradebookExportDialog.tsx`
3. `frontend/src/features/grading/ClassProgressSection.tsx`
4. `frontend/src/features/grading/register.ts`
5. `frontend/src/features/groups/TeacherGroupWidgets.tsx`
6. `frontend/src/features/groups/register.ts`
7. `frontend/src/features/academics/ReadOnlyClassPage.tsx`
8. `frontend/src/features/academics/academics.module.css`

### File kiểm thử tự động mới / cập nhật
9. `frontend/src/features/grading/gradebook-layout.test.tsx` *(mới — 6 kịch bản kiểm thử sổ điểm)*
10. `frontend/src/features/grading/class-progress-layout.test.tsx` *(mới — 4 kịch bản kiểm thử tiến độ & phân bố điểm)*
11. `frontend/src/features/academics/academics.test.tsx` *(cập nhật — thêm kiểm thử bố cục tổng quan lớp của teacher, mô tả dài và bảo toàn view student)*
12. `frontend/src/features/groups/TeacherGroupWidgets.test.tsx` *(cập nhật — thêm kiểm thử `GroupCountCard` chế độ `overview` khi đang tải và sau khi tải xong)*

### File tài liệu AI-DLC cập nhật
13. `aidlc-docs/construction/plans/code-generation-plan/2026-10-25-u09-gradebook-class-layout-code-generation-plan.md`
14. `aidlc-docs/construction/build-and-test/2026-10-25-u09-gradebook-class-layout-manual-test-guide.md`
15. `aidlc-docs/construction/build-and-test/2026-10-25-u09-gradebook-class-layout-implementation-report.md` *(file này)*
16. `aidlc-docs/aidlc-state.md`
17. `aidlc-docs/audit.md`

---

## 3. Kết quả kiểm tra tự động (Automated Verification)

| Kiểm tra | Lệnh thực thi | Kết quả | Exit code |
|---|---|---|---|
| Baseline trước khi sửa (`ea0d2c8e`) | `npm --prefix frontend test -- src/features/grading src/features/academics src/features/groups --maxWorkers=2` | `17/17` file đạt (`115/115` tests) | `0` |
| Kiểm thử các unit thay đổi (`grading`, `academics`, `groups`) | `npm --prefix frontend test -- src/features/grading src/features/academics src/features/groups --maxWorkers=2` | `19/19` file đạt (`127/127` tests, +12 tests mới) | `0` |
| Toàn bộ bộ kiểm thử frontend | `npm --prefix frontend test -- --maxWorkers=2` | `89/89` file đạt (`544/544` tests, 0 lỗi) | `0` |
| ESLint | `npm --prefix frontend run lint` | Đạt (0 lỗi, 0 cảnh báo) | `0` |
| TypeScript (`tsc --noEmit`) | `npm --prefix frontend run typecheck` | Đạt (0 lỗi) | `0` |
| Build production cô lập (không ghi đè `.next` của dev server) | `$env:NEXT_DIST_DIR=".next-build-u09-overview"; $env:NODE_OPTIONS='--require ".../limit-cpus.cjs"'; npm --prefix frontend run build` | Đạt (biên dịch và sinh 28/28 route thành công; đã dọn `.next-build-u09-overview` và khôi phục `tsconfig.json`) | `0` |
| Kiểm tra định dạng diff / conflict markers | `git diff --check` | Đạt (không có conflict marker hoặc lỗi khoảng trắng) | `0` |

> **Ghi chú kỹ thuật về bản build cô lập trên Windows 24 luồng CPU**: Khi chạy `next build` mặc định, Next.js 16 tự khởi tạo 23 tiến trình worker song song (`availableParallelism - 1`) khiến Windows báo lỗi giới hạn tài nguyên hệ thống (`os error 1450` / V8 zone allocation OOM). Việc truyền preload giới hạn `os.availableParallelism = () => 3` qua `NODE_OPTIONS` trong phiên lệnh build giúp Next.js thu thập dữ liệu trang bằng 2 worker ổn định và hoàn tất với `exit code 0` mà không cần sửa cấu hình dự án.

---

## 4. Trạng thái Git & Xử lý xung đột tài liệu

- **Trạng thái Git ban đầu & cuối**:
  - Nhánh `mcp-test`, `HEAD` tại `ea0d2c8e`.
  - Các file thông báo 4 giây đã được stage từ trước (`frontend/src/features/grading/BatchActionDialog.tsx`, `frontend/src/features/grading/ai-source-integration.test.tsx`, `frontend/src/features/grading/batch-action-feedback.test.tsx`, `aidlc-docs/aidlc-state.md`, `aidlc-docs/construction/build-and-test/2026-10-25-u09-feedback-results.md`, `aidlc-docs/construction/plans/2026-10-25-u09-feedback-class-layout.md`) được **bảo toàn nguyên vẹn trong Git index**.
  - Không thực hiện bất kỳ lệnh `git add`, `git commit`, `git reset`, `git checkout`, `git stash`, `git merge` hay `git push` nào.
- **Xử lý `aidlc-docs/audit.md` (`UU`)**:
  - Khi nhận bàn giao, `aidlc-docs/audit.md` có đánh dấu unmerged (`UU`) ở cuối file giữa 2 mục lịch sử PR22 (`2026-10-25T08:00:00Z` / `2026-10-25T09:00:00Z`) và 2 mục lịch sử U09 feedback từ stash.
  - Đã hợp nhất an toàn trên working tree bằng cách giữ nguyên toàn bộ cả hai khối lịch sử theo đúng trình tự thời gian, xóa các dòng marker `<<<<<<<` / `=======` / `>>>>>>>`, và chỉ ghi nối tiếp (append) nhật ký bàn giao & hoàn tất mới ở cuối file; không can thiệp vào Git index.

---

## 5. Giới hạn & Lưu ý khi người dùng test UI

1. **Chưa chạy kiểm thử giao diện thủ công**: Theo đúng yêu cầu bàn giao, Gemini chỉ thực hiện code và kiểm tra tự động; toàn bộ 40 ca kiểm thử trong guide đều để `Chưa test` chờ bạn trực tiếp kiểm tra.
2. **Dữ liệu lớp `U05-A` (`4d18e255-1789-45d6-8fbb-351e2188f672`)**:
   - Lớp hiện có 1 sinh viên đang học và nhiều bài tập tên dài. Các ca cần >20 sinh viên để chuyển trang (`GB07`) hoặc cần đủ số lượng bài đã công bố vượt ngưỡng riêng tư để vẽ biểu đồ phân bố điểm (`CL09`) sẽ phụ thuộc vào dữ liệu thực tế của lớp.
   - Bài QA `826f5c29-b5d1-4a32-9999-9313c242d8d3` đã ở trạng thái `PUBLISHED` (`10.00`, version `7`). Bài gốc `7ca08c41-e9d4-4738-946d-24c049e3d1e5` được giữ nguyên ở trạng thái `DRAFT` (`0.00`, version `7`). Gemini không ghi đè hay thay đổi dữ liệu DB.

---

## 6. Hướng dẫn mở ứng dụng và bắt đầu test UI

1. **Kiểm tra tiến trình đang chạy**:
   - Backend + worker hiện đang chạy tại cổng `8080` (terminal `term_b21e6d47-b4d9-4b91-9a56-94c2f7cf2d16`). Nếu cần khởi động lại: chạy `.\scripts\run-backend.bat` từ thư mục gốc dự án (không bật Docker worker cũ song song).
   - Frontend Next.js dev server đang chạy tại cổng `3000` (terminal `term_0feceaf6-77ba-420a-b701-347e593d6a20`). Nếu cần khởi động lại: chạy `npm --prefix frontend run dev`.
2. **Truy cập trực tiếp trên trình duyệt**:
   - Thông tin lớp: `http://localhost:3000/teaching/classes/4d18e255-1789-45d6-8fbb-351e2188f672?tab=info`
   - Sổ điểm: `http://localhost:3000/teaching/classes/4d18e255-1789-45d6-8fbb-351e2188f672?tab=gradebook`
   - Bài QA: `http://localhost:3000/teaching/evaluations/826f5c29-b5d1-4a32-9999-9313c242d8d3`
3. **Tài liệu hướng dẫn test chi tiết (40 ca)**:
   - Mở [`aidlc-docs/construction/build-and-test/2026-10-25-u09-gradebook-class-layout-manual-test-guide.md`](./2026-10-25-u09-gradebook-class-layout-manual-test-guide.md).
