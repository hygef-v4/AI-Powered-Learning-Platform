import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { ClassGradebookTab } from "@/features/grading/components/ClassGradebookTab";

const mocks = vi.hoisted(() => ({ loadGradebook: vi.fn(), exportGrades: vi.fn() }));
vi.mock("@/features/grading/api", () => ({ gradingApi: mocks }));
vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams(window.location.search), useRouter: () => ({ push: vi.fn() }) }));
const assignments = [
  { id: "a", title: "Kiến trúc", maxScore: 10, submissionMode: "INDIVIDUAL" },
  { id: "b", title: "Bài nhóm", maxScore: 20, submissionMode: "GROUP" },
];
const data = { assignments, total: 21, page: 1, size: 20, rows: [
  { student: { accountId: "s", name: "An", email: "an@school.test", studentCode: "SE000001" }, grades: [
    { assignmentId: "b", evaluationId: "eb", status: "PUBLISHED", score: "8.25", maxScore: 20, memberOverride: true },
    { assignmentId: "a", evaluationId: "ea", status: "DRAFT", score: "0.00", maxScore: 10 },
  ] },
  { student: { accountId: "t", name: "Bình", email: "binh@school.test" }, grades: [
    { assignmentId: "a", evaluationId: "pending", status: "PENDING", score: "9.00", maxScore: 10 },
  ] },
] };
beforeEach(() => {
  vi.resetAllMocks();
  window.history.replaceState(null, "", "/teaching/classes/c?tab=gradebook&gradebookView=matrix");
  mocks.loadGradebook.mockResolvedValue(data);
});
afterEach(cleanup);
it("renders students once and aligns grades by assignment ID, preserving zero and member scores", async () => {
  render(<ClassGradebookTab classId="c" />);
  const row = (await screen.findByText("An")).closest("tr")!;
  expect(within(row).getAllByRole("cell").map(x => x.textContent)).toEqual(["1", "SE000001", "AnSE000001an@school.test", "0", "8.25"]);
  expect(within(row).getByRole("link", { name: /Kiến trúc.*An/ })).toHaveAttribute("href", "/teaching/evaluations/ea");
  expect(screen.getByText("Bình").closest("tr")).not.toHaveTextContent("9");
  expect(screen.queryByText("Chốt điểm")).not.toBeInTheDocument();
});
it("filters columns, exports all learners for the selected assignment and preserves filename", async () => {
  mocks.exportGrades.mockResolvedValue({ blob: new Blob(["xlsx"]), filename: "Bài nhóm.xlsx" });
  URL.createObjectURL = vi.fn(() => "blob:test"); URL.revokeObjectURL = vi.fn();
  const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(function(this: HTMLAnchorElement) { expect(this.download).toBe("Bài nhóm.xlsx"); });
  render(<ClassGradebookTab classId="c" />);
  await screen.findByText("An");
  fireEvent.change(screen.getByRole("combobox", { name: "Bài tập" }), { target: { value: "b" } });
  expect(screen.queryByRole("columnheader", { name: /Kiến trúc/ })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Xuất bảng điểm" }));
  const dialog = screen.getByRole("dialog", { name: "Xuất bảng điểm" });
  expect(within(dialog).getByRole("combobox", { name: /Phạm vi/ })).toHaveValue("b");
  fireEvent.click(within(dialog).getByRole("button", { name: "Xuất tệp" }));
  await waitFor(() => expect(mocks.exportGrades).toHaveBeenCalledWith("c", "xlsx", "b"));
  click.mockRestore();
});
it("paginates and searches using the existing gradebook API, and handles errors", async () => {
  render(<ClassGradebookTab classId="c" />);
  await screen.findByText("An");
  fireEvent.click(screen.getByRole("button", { name: "Trang sau" }));
  await waitFor(() => expect(mocks.loadGradebook).toHaveBeenLastCalledWith("c", "", 2));
  fireEvent.change(screen.getByRole("searchbox", { name: "Tìm sinh viên" }), { target: { value: "An" } });
  await waitFor(() => expect(mocks.loadGradebook).toHaveBeenLastCalledWith("c", "An", 1));
  cleanup(); mocks.loadGradebook.mockRejectedValue(new Error("offline"));
  render(<ClassGradebookTab classId="c" />);
  expect(await screen.findByRole("alert")).toBeInTheDocument();
});
