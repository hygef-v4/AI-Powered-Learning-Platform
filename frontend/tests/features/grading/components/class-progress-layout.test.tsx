import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { ClassProgressSection } from "@/features/grading/components/ClassProgressSection";
import type { AssignmentRow } from "@/features/assignments/model/types";

const mocks = vi.hoisted(() => ({ listForClass: vi.fn(), loadSubmissionProgress: vi.fn() }));
vi.mock("@/features/grading/api", () => ({ gradingApi: mocks }));
vi.mock("@/features/assignments/api", () => ({ assignmentsApi: mocks }));
afterEach(() => { cleanup(); vi.restoreAllMocks(); });
beforeEach(() => {
  vi.spyOn(Date, "now").mockReturnValue(Date.parse("2026-11-11T00:00:00Z"));
  mocks.listForClass.mockResolvedValue({ items: [] });
  mocks.loadSubmissionProgress.mockResolvedValue({});
});
function assignment(id: string, closesAt: string | null, extra: Partial<AssignmentRow> = {}) {
  return { id, title: id, status: "OPEN", closesAt, submissionMode: "INDIVIDUAL", gradingMode: "GRADED", ...extra };
}

it("uses server submission counts, including group roots and legacy saved grades", async () => {
  mocks.listForClass.mockResolvedValue({ items: [assignment("open", null)] });
  mocks.loadSubmissionProgress.mockResolvedValue({
    individual: { summary: { PENDING: 3, DRAFT: 2, FINALIZED: 1, PUBLISHED: 5 } },
    group: { summary: { PENDING: 1, DRAFT: 1, FINALIZED: 0, total: 2 } },
  });
  render(<ClassProgressSection classId="class-1" />);
  expect(await screen.findByRole("link", { name: "1 Bài đang mở" })).toHaveAttribute("href", "/teaching/classes/class-1?tab=evals&assignmentState=open");
  expect(screen.getByRole("link", { name: "4 Bài nộp chờ chấm" })).toHaveAttribute("href", "/teaching/classes/class-1?tab=gradebook&assignment=individual&gradebookStatus=PENDING");
  expect(screen.getByRole("link", { name: "4 Bài đã chấm chưa công bố" })).toHaveAttribute("href", "/teaching/classes/class-1?tab=gradebook&assignment=individual&gradebookStatus=DRAFT");
  expect(screen.queryByRole("tab")).not.toBeInTheDocument();
});
it("shows the nearest three future OPEN deadlines, including practice", async () => {
  mocks.listForClass.mockResolvedValue({ items: [
    assignment("fourth", "2026-11-20T00:00:00Z"), assignment("second", "2026-11-13T00:00:00Z", { submissionMode: "GROUP" }),
    assignment("first", "2026-11-12T00:00:00Z", { gradingMode: "PRACTICE" }), assignment("third", "2026-11-14T00:00:00Z"),
    assignment("past", "2026-11-10T00:00:00Z"), assignment("no deadline", null),
    assignment("closed", "2026-11-12T00:00:00Z", { status: "CLOSED" }), assignment("invalid", "invalid"),
  ] });
  render(<ClassProgressSection classId="class-1" />);
  await screen.findByRole("link", { name: "7 Bài đang mở" });
  expect(screen.getAllByTestId("upcoming-assignment").map(x => x.textContent)).toEqual([
    expect.stringContaining("first"), expect.stringContaining("second"), expect.stringContaining("third"),
  ]);
  expect(screen.getByRole("link", { name: /first.*Luyện tập/ })).toHaveAttribute("href", "/assignments/first?classId=class-1");
  expect(screen.queryByText("fourth")).not.toBeInTheDocument();
});
it("keeps loading and errors distinct from zero", async () => {
  mocks.listForClass.mockReturnValue(new Promise(() => {}));
  const first = render(<ClassProgressSection classId="class-1" />);
  expect(screen.getByRole("status")).toHaveTextContent("Đang tải tổng quan");
  expect(screen.queryByText("0")).not.toBeInTheDocument();
  first.unmount();
  mocks.listForClass.mockRejectedValue(new Error("offline"));
  render(<ClassProgressSection classId="class-2" />);
  expect(await screen.findByRole("alert")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Thử tải lại" })).toBeInTheDocument();
});
it("renders an empty state without inventing deadlines", async () => {
  render(<ClassProgressSection classId="class-1" />);
  expect(await screen.findByRole("link", { name: "0 Bài đang mở" })).toBeInTheDocument();
  expect(screen.getByText("Chưa có hạn nộp sắp tới.")).toBeInTheDocument();
});
