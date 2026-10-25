# U09 UI Polish — Build, Test, and Live UI Acceptance Results

**Date**: 2026-10-25

**Plan**: [`aidlc-docs/construction/plans/code-generation-plan/2026-10-25-u09-ui-polish-code-generation-plan.md`](../plans/code-generation-plan/2026-10-25-u09-ui-polish-code-generation-plan.md)
**Branch / Commit**: `mcp-test` (`HEAD` `f4a19cf6`, dirty working tree preserved)

---

## 1. Summary of Frontend Changes

1. **Container-Aware Two-Column Workspace & Compact Sticky Toolbar**
   - [`frontend/src/app/globals.css`](../../../frontend/src/app/globals.css): Scoped container query rules (`[data-workspace-main="true"]` with `container-type: inline-size`) that activate a two-column grading grid (`minmax(400px, 1.18fr) minmax(320px, 1fr)`) at `@container (min-width: 736px)` and a ~60/40 split (`minmax(400px, 3fr) minmax(320px, 2fr)`) at `@container (min-width: 960px)`, while stacking cleanly below `736px`.
   - [`frontend/src/features/grading/GradingWorkspacePage.tsx`](../../../frontend/src/features/grading/GradingWorkspacePage.tsx): Compact sticky toolbar (`46px` height at desktop viewport, `top-16` offset below app header) combining live provisional total (`Điểm tạm tính`), saved/dirty status, navigation (`← Bài trước` / `Bài sau →`), and save/finalize/publish actions on a single primary row. Workflow instructions moved to a `<details>` disclosure (`Quy trình chấm`) above the sticky bar. Wired `rubrics`, `currentValues`, and `partTitles` into `AiProposalPanel`.
   - [`frontend/src/features/grading/GradingForm.tsx`](../../../frontend/src/features/grading/GradingForm.tsx): Reduced vertical padding and spacing while preserving teacher adjustment and official grade-change reason rules.

2. **Compact Rubric Scoring & Accessible Full-Credit Action**
   - [`frontend/src/features/grading/RubricChecklistPanel.tsx`](../../../frontend/src/features/grading/RubricChecklistPanel.tsx):
     - Compact criterion rows keeping the Vietnamese criterion label on the left and score input (`step="0.01"`), maximum (`/ X.XX`), and full-credit button on the right on wide rows (`sm:flex-row`), stacking cleanly on narrow screens.
     - Replaced the ambiguous full-credit checkbox with an accessible `Đủ điểm` / `✓ Đủ điểm` button (`aria-pressed`, `aria-label="Đủ điểm: <item.text>"`) that awards the item's maximum score via the existing score/check update path.
     - Collapsible rubric sections (default open) displaying the part title and live subtotal/max (`X.XX / Y.YY điểm`) while collapsed.
     - When a collapsed section contains an invalid score, renders an explicit alert banner (`Phần này có mục điểm chưa hợp lệ.`) and a keyboard-accessible recovery button (`Mở mục có điểm chưa hợp lệ`) that expands the section and focuses the first invalid input.

3. **AI Proposal Presentation, Live Comparison, and Draft-Only Application**
   - [`frontend/src/features/grading/AiProposalPanel.tsx`](../../../frontend/src/features/grading/AiProposalPanel.tsx):
     - Status badges rendered with `Pill` tones and icon+text (`✓ Sẵn sàng để xem`, `⏳ Đang tạo đề xuất`, `✕ Không tạo được đề xuất`, `! Đề xuất đã cũ`, `− Đã bỏ qua`), keeping model/source as secondary metadata.
     - Latest proposal expanded by default; older `READY` / `STALE` proposals collapsible via a disclosure button (`aria-expanded`), while older `PENDING` / `FAILED` / `REJECTED` proposals remain expanded so error diagnostics and `Kiểm tra kết nối AI` are never hidden.
     - Live comparison on the latest proposal showing `Điểm đang nhập` vs `AI đề xuất` at rubric subtotal (`Lệch ...` / `Khớp điểm rubric`), named section, and item levels, with changed items highlighted.
     - Teacher adjustment (`Điều chỉnh giảng viên: ...`) displayed separately from rubric subtotal comparison.
     - Invalid current inputs render `Điểm đang nhập chưa hợp lệ` without `NaN` or false zero fallback; missing AI item IDs render `Chưa có dữ liệu`.
     - Apply action renamed `Áp dụng vào bản nháp` with helper note (`Chỉ điền vào bản nháp trên biểu mẫu; bạn có thể sửa lại trước khi lưu.`), updating local draft state and `usedProposalId` without auto-saving.

4. **Submissions List & Secondary Review Panels**
   - [`frontend/src/features/grading/SubmissionDetailPage.tsx`](../../../frontend/src/features/grading/SubmissionDetailPage.tsx):
     - Compact 4-counter summary bar (`Đã nộp`, `Đang làm`, `Chưa nộp`, `Nộp trễ`).
     - Status column rendered with semantic `Pill` tones (`PENDING` warning, `DRAFT` info, `FINALIZED` primary, `PUBLISHED` success, `SUPERSEDED` neutral) and single-line tabular scores (`tabular-nums whitespace-nowrap`).
     - Bulk action buttons (`AI đề xuất`, `Chốt điểm`, `Công bố điểm`, `Bỏ chọn`) appear only when at least one eligible row is selected; when none are selected, shows `Đã chọn 0 bài · Tích chọn bài nộp để thao tác hàng loạt`.
     - State-specific row links: `Chấm bài` (`PENDING`), `Tiếp tục chấm` (`DRAFT`), `Xem điểm` (`FINALIZED` / `PUBLISHED` / `SUPERSEDED`).
   - [`frontend/src/features/grading/GradingReviewPanels.tsx`](../../../frontend/src/features/grading/GradingReviewPanels.tsx):
     - `ActivityReview` and `GradeHistory` wrapped in `<details open={hasData}>` so empty states collapse into compact one-line disclosures while populated activity/history opens by default.

---

## 2. Automated Verification Results

| Check | Command | Result | Details |
|---|---|---|---|
| Scoped Grading Tests | `npm --prefix frontend test -- src/features/grading --maxWorkers=2` | **PASS** (exit 0) | `6 passed (6)` files, `49 passed (49)` tests (`partial-scores.test.tsx`, `manual-workflow.test.tsx`, `workspace.test.tsx`, `submissions.test.tsx`, `ai-source-integration.test.tsx`, `route-shell.test.tsx`) |
| Full Frontend Test Suite | `npm --prefix frontend test -- --maxWorkers=2` | **PASS** (exit 0) | `86 passed (86)` files, `523 passed (523)` tests |
| ESLint | `npm --prefix frontend run lint` | **PASS** (exit 0) | 0 errors, 0 warnings |
| TypeScript Typecheck | `npm --prefix frontend run typecheck` | **PASS** (exit 0) | `tsc --noEmit` clean |
| Isolated Production Build | `$env:NEXT_DIST_DIR=".next-build"; npm --prefix frontend run build` | **PASS** (exit 0) | Next.js 16.3.8 compiled & generated all 28 static/dynamic routes without touching active `.next` dev output; cleaned up `.next-build` and restored `tsconfig.json` afterward |
| Git Diff Whitespace Check | `git diff --check` | **PASS** (exit 0) | No whitespace or conflict marker errors |

---

## 3. Live Orca Browser UI Acceptance Evidence

### 3.1 Measured Layout & Responsive Container Behavior
- **Active Orca Desktop Viewport** (`http://localhost:3000/teaching/evaluations/826f5c29-b5d1-4a32-9999-9313c242d8d3`):
  - `innerWidth = 1289` CSS px, `innerHeight = 921` CSS px (`devicePixelRatio = 1`).
  - Workspace grid width with left sidebar open: `974` CSS px -> **2 columns** (`575px` submission viewer left, `383px` grading form right, `16px` gap).
  - Sticky toolbar height: `46` CSS px (`<= 104` CSS px target), `top: 64px` (`top-16`), `hasHorizontalOverflow = false`.
- **Focus Mode (`Chế độ tập trung`, measured in the same desktop viewport)**:
  - Hides shell sidebar (`data-workspace-focus="true"`), expanding workspace grid width to `1210` CSS px -> **2 columns** (`716px` submission viewer left, `478px` grading form right, `16px` gap, ~60/40 split), `hasHorizontalOverflow = false`.
- **Container-Query Verification at `762px` and `358px` Content Widths**:
  - *Note on Orca window resizing*: Orca's client-hosted browser CLI does not expose a native window/viewport resize command (`orca exec` does not support viewport resizing). Therefore, in addition to live desktop (`974px`) and Focus Mode (`1210px`), we measured `[data-workspace-main="true"]` directly at `762px` (the exact container width at the `1077px` viewport) and `358px` (the content width inside a `390px` mobile viewport with `16px` horizontal padding):
    - At `762px` container width: **2 columns** (`404px` left, `342px` right, `16px` gap; `scrollWidth = 762px`, no overflow).
    - At `358px` container width: **1 column** (`358px` stacked width; `scrollWidth = 358px`, no overflow).
  - These checks constrain the workspace container only. They do not change the viewport media queries, app shell, device behavior or browser zoom. Actual 390px mobile viewport, 1440px viewport and 200% browser zoom acceptance remain unverified; the observed container results must not be reported as equivalent full-page tests.

### 3.2 Keyboard, Partial Score, and Collapsed Section Validation Flow (QA Evaluation `826f5c29-b5d1-4a32-9999-9313c242d8d3`)
- Focused first item spinbutton (`Điểm: Xác định đầy đủ các thực thể chính...`), pressed `Tab` -> focus moved directly to `BUTTON` `Đủ điểm: Xác định đầy đủ các thực thể chính...` (`aria-pressed="false"`).
- Pressed `Enter` on `Đủ điểm` -> button updated to `✓ Đủ điểm` (`aria-pressed="true"`), item value became `1`, and live total updated to `10.00 / 10.00`.
- Entered invalid score `1.5` (above max `1.00`), collapsed `Câu 1: Sơ đồ lớp (Class Diagram)` (`aria-expanded="false"`):
  - Collapsed header remained visible with `Phần này có mục điểm chưa hợp lệ.` and `Mở mục có điểm chưa hợp lệ`.
  - Clicking `Lưu nháp / thay đổi` while invalid was blocked with alert `Kiểm tra điểm từng mục: từ 0 đến điểm tối đa, với tối đa 2 chữ số thập phân.` without losing the draft.
  - Focused `Mở mục có điểm chưa hợp lệ` and pressed `Enter` -> section expanded (`aria-expanded="true"`) and focus landed directly on `score-input-c66fc2ef-6ab4-4fb7-ad3a-f745bb2f1221-d9d2254e-8817-4e11-83ce-6597e3d8b995` (`aria-invalid="true"`).

### 3.3 AI Comparison & Apply-to-Draft -> Edit -> Save -> Reload Flow (QA Evaluation `826f5c29-b5d1-4a32-9999-9313c242d8d3`)
- Existing READY proposal (`6e4eea09-6d45-49cc-bccd-c6f8318d03bd`, `Lần 1 · API cá nhân`, `GEMINI · gemini-3.5-flash-lite`) displayed:
  - `✓ Sẵn sàng để xem`, `Điểm AI đề xuất: 10.00`, `Điểm đang nhập: 9.50`, `AI đề xuất: 10.00`, `Lệch +0.50`.
  - Named section comparison (`Câu 1: Sơ đồ lớp (Class Diagram)` `Đang nhập: 2.50 · AI: 3.00 / 3.00`) and highlighted item delta (`Đang nhập: 0.50 · AI: 1.00 / 1.00  +0.50`).
- Clicked `Áp dụng vào bản nháp`:
  - Populated local draft (`Điểm tạm tính 10.00 / 10.00`, `Có thay đổi chưa lưu`) without saving to server.
- Edited first item back to `0.5` (`Điểm tạm tính 9.50 / 10.00`), clicked `Lưu nháp / thay đổi`, and reloaded the page:
  - Reloaded state confirmed `firstValReloaded = "0.5"`, `totalReloaded = "9.50 / 10.00"`, `Điểm đã lưu: 9.50 / 10.00`.

### 3.4 Submissions List Flow (`aaf2897a-1deb-4987-8349-f1783b6610bc`)
- Verified compact counters (`Đã nộp 1`, `Đang làm 0`, `Chưa nộp 0`, `Nộp trễ 0`), `Đã chấm (nháp)` info pill badge, single-line score `9.50 / 10.00`, and link label `Tiếp tục chấm`.
- With 0 rows selected: bulk action buttons were hidden (`Đã chọn 0 bài · Tích chọn bài nộp để thao tác hàng loạt`).
- Selecting the row checkbox revealed `AI đề xuất`, `Chốt điểm`, `Công bố điểm`, and `Bỏ chọn`; clicking `Bỏ chọn` hid the bulk actions again.

### 3.5 Protected Evaluation Integrity & Older Proposal Diagnostics (`7ca08c41-e9d4-4738-946d-24c049e3d1e5`)
- Verified in read-only mode via `/api/v1/evaluations/7ca08c41-e9d4-4738-946d-24c049e3d1e5`:
  - `state: "DRAFT"`, `score: 0`, `version: 7` (untouched; no writes, saves, finalizations, or publications performed).
- Verified `Đề xuất AI` on `7ca08c41-e9d4-4738-946d-24c049e3d1e5`:
  - Latest proposal (`Lần 3 · API cá nhân`, `✓ Sẵn sàng để xem`, `10.00`) expanded at top with `Lệch +10.00` comparison against `0.00` draft.
  - Older failed proposals (`Lần 2 · API cá nhân` and `Lần 1 · API cá nhân`, `✕ Không tạo được đề xuất`) remained expanded with full safe diagnostics (`Nhà cung cấp AI từ chối yêu cầu (HTTP 400): model hoặc tham số không hợp lệ`) and `Kiểm tra kết nối AI` link.

### 3.6 Inspected Screenshot Inventory (`.tmp/u09-ui-polish/`)
- `.tmp/u09-ui-polish/01-qa-workspace-overview.png` — Two-column workspace, compact sticky bar (`46px`), compact rubric rows with `0.5 / 1.00` and `Đủ điểm` / `✓ Đủ điểm`.
- `.tmp/u09-ui-polish/02-qa-focus-mode.png` — Focus Mode (`Chế độ tập trung`) expanding workspace grid to `1210px` (`716px` + `478px`).
- `.tmp/u09-ui-polish/03-qa-ai-comparison.png` — `Đề xuất AI` ready badge, `Điểm đang nhập: 9.50` vs `AI đề xuất: 10.00` (`Lệch +0.50`), and per-section/per-item comparison.
- `.tmp/u09-ui-polish/04-qa-bottom-panels.png` — Collapsible AI feedback disclosures, `Áp dụng vào bản nháp`, and collapsed empty `Hoạt động khi làm bài` / `Lịch sử sửa điểm` disclosures.
- `.tmp/u09-ui-polish/05-qa-collapsed-invalid-error.png` — Collapsed rubric section with invalid score alert banner and `Mở mục có điểm chưa hợp lệ` focus button.
- `.tmp/u09-ui-polish/06-qa-submissions-list-unselected.png` — Submissions list with compact counters, status badge, tabular score, `Tiếp tục chấm`, and hidden bulk actions when unselected.
- `.tmp/u09-ui-polish/07-qa-submissions-list-selected.png` — Submissions list with row selected revealing `AI đề xuất`, `Chốt điểm`, `Công bố điểm`, and `Bỏ chọn`.
- `.tmp/u09-ui-polish/08-protected-eval-readonly-ai-history.png` — Protected evaluation `7ca08c41-e9d4-4738-946d-24c049e3d1e5` (`0.00 / 10.00` untouched) showing `Lần 3` READY comparison.
- `.tmp/u09-ui-polish/09-protected-eval-older-failed-proposals.png` — Protected evaluation showing older `Lần 2` and `Lần 1` `FAILED` proposals kept expanded with error diagnostics.
