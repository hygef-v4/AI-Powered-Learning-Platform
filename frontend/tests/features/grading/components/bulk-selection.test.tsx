import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { GradebookBatchControls } from "@/features/grading/components/GradebookBatchControls";
const mocks = vi.hoisted(() => ({ listSubmissions: vi.fn(), dialog: vi.fn() }));
const router = vi.hoisted(() => ({ push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router }));
vi.mock("@/features/grading/api", () => ({ gradingApi: mocks }));
vi.mock("@/features/grading/components/BatchActionDialog", () => ({ BatchActionDialog: (props: unknown) => { mocks.dialog(props); return <div>Confirm batch</div>; } }));
const rows = Array.from({ length: 30 }, (_, i) => ({
  student: { accountId: `s${i}`, name: `Student ${i}`, email: `s${i}@school.test` },
  evaluationId: `e${i}`, status: i < 10 ? "PUBLISHED" : "DRAFT", version: i + 1,
  score: 5, maxScore: 10, late: false,
}));
const props = { classId: "c", assignmentId: "a", onDone: vi.fn(), query: "", status: "", group: false };
beforeEach(() => {
  vi.resetAllMocks();
  mocks.listSubmissions.mockImplementation((_c, _a, _status, page) => Promise.resolve({ rows: rows.slice((page - 1) * 20, page * 20), total: rows.length }));
});
afterEach(cleanup);
it("selects a page, all30 matching submissions, and passes only eligible IDs/versions to publication", async () => {
  render(<GradebookBatchControls {...props} />);
  fireEvent.click(await screen.findByRole("checkbox", { name: "Chọn tất cả trên trang" }));
  expect(screen.getByText("Đã chọn 20 bài")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Chọn toàn bộ 30 bài phù hợp" }));
  expect(screen.getByRole("button", { name: "Chấm bằng AI (30)" })).toBeEnabled();
  fireEvent.click(screen.getByRole("button", { name: "Công bố điểm (20)" }));
  const action = mocks.dialog.mock.lastCall![0];
  expect(action.action).toBe("publish");
  expect(action.items).toEqual(rows.slice(10).map(row => ({ evaluationId: row.evaluationId, version: row.version })));
  expect(screen.queryByRole("button", { name: "Thao tác hàng loạt" })).not.toBeInTheDocument();
});
it("retains page selections, shows mixed state, and clears on filter/assignment changes", async () => {
  const view = render(<GradebookBatchControls {...props} />);
  fireEvent.click(await screen.findByRole("checkbox", { name: "Chọn Student 0" }));
  expect(screen.getByRole("checkbox", { name: "Chọn tất cả trên trang" })).toBePartiallyChecked();
  fireEvent.click(screen.getByRole("button", { name: "Trang sau" }));
  expect(screen.getByText("Đã chọn 1 bài")).toBeInTheDocument();
  expect(screen.getByRole("checkbox", { name: "Chọn Student 20" })).not.toBeChecked();
  view.rerender(<GradebookBatchControls {...props} query="Student 2" />);
  await screen.findByRole("checkbox", { name: "Chọn Student 2" });
  expect(screen.queryByText("Đã chọn 1 bài")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("checkbox", { name: "Chọn tất cả trên trang" }));
  expect(screen.getByText("Đã chọn 11 bài")).toBeInTheDocument();
  view.rerender(<GradebookBatchControls {...props} assignmentId="b" />);
  await screen.findByRole("checkbox", { name: "Chọn Student 0" });
  expect(screen.queryByText("Đã chọn 11 bài")).not.toBeInTheDocument();
});
it("disables unsubmitted rows and never lets select-all exceed100", async () => {
  mocks.listSubmissions.mockResolvedValue({ rows: [
    ...Array.from({ length: 101 }, (_, i) => ({ ...rows[0]!, student: { ...rows[0]!.student, name: `Person ${i}` }, evaluationId: `id${i}` })),
    { ...rows[0]!, student: { ...rows[0]!.student, name: "Unsubmitted" }, evaluationId: null, status: "NOT_SUBMITTED" },
  ], total: 102 });
  render(<GradebookBatchControls {...props} query="Unsubmitted" />);
  expect(await screen.findByRole("checkbox", { name: "Chọn Unsubmitted" })).toBeDisabled();
  cleanup();
  render(<GradebookBatchControls {...props} />);
  fireEvent.click(await screen.findByRole("checkbox", { name: "Chọn tất cả trên trang" }));
  expect(screen.getByRole("button", { name: "Chọn toàn bộ 101 bài phù hợp" })).toBeDisabled();
  expect(screen.getByText("Tối đa 100 bài mỗi lần.")).toBeInTheDocument();
  for (let page = 2; page <= 5; page++) {
    fireEvent.click(screen.getByRole("button", { name: "Trang sau" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Chọn tất cả trên trang" }));
  }
  expect(screen.getByText("Đã chọn 100 bài")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Trang sau" }));
  expect(screen.getByRole("checkbox", { name: "Chọn Person 100" })).toBeDisabled();
});
it("renders one group root per row and handles a failed directory without actions", async () => {
  mocks.listSubmissions.mockResolvedValue({ rows: [rows[0]], total: 1 });
  render(<GradebookBatchControls {...props} group />);
  await screen.findByRole("checkbox", { name: "Chọn Student 0" });
  expect(screen.getByRole("columnheader", { name: "Nhóm" })).toBeInTheDocument();
  expect(screen.getAllByRole("checkbox")).toHaveLength(2);
  cleanup();
  mocks.listSubmissions.mockRejectedValue(new Error("offline"));
  render(<GradebookBatchControls {...props} />);
  await waitFor(() => expect(screen.getByRole("alert")).toBeInTheDocument());
  expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
});

it("bấm cả dòng là mở trang chấm; không còn dòng đếm số sinh viên", async () => {
  render(<GradebookBatchControls {...props} />);
  fireEvent.click(await screen.findByText("Student 12"));
  expect(router.push).toHaveBeenCalledWith(expect.stringContaining("e12"));
  expect(screen.queryByText(/Hiển thị/)).not.toBeInTheDocument();
});
