import { cleanup, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { ClassTodoCard } from "@/features/assignmentwork/components/ClassTodoCard";
import { workApi } from "@/features/assignmentwork/api";
import type { StudentRow } from "@/features/assignmentwork/model/types";
import { studentClassBoard } from "@/features/academics/slots";
import "@/features/register";

vi.mock("@/features/assignmentwork/api", () => ({ workApi: { list: vi.fn() } }));
vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams() }));
const row = (id: string, overrides: Partial<StudentRow> = {}): StudentRow => ({
  assignmentId: id, classId: "class-1", title: id, status: "OPEN", myStatus: "NOT_STARTED",
  gradingMode: "GRADED", submissionMode: "INDIVIDUAL", submittedCount: 0,
  closesAt: "2099-01-01T00:00:00Z", ...overrides,
} as StudentRow);
beforeEach(() => vi.clearAllMocks());
afterEach(cleanup);

it("replaces recent announcements with the assignment overview and matches personal/group progress", async () => {
  vi.mocked(workApi.list).mockResolvedValue([
    row("new"), row("doing", { myStatus: "IN_PROGRESS" }),
    row("submitted", { myStatus: "SUBMITTED", submittedCount: 1 }),
    row("practice", { gradingMode: "PRACTICE" }),
    row("group-done", { submissionMode: "GROUP", myStatus: "GROUP", group: { submitted: true } as StudentRow["group"] }),
    row("group-doing", { submissionMode: "GROUP", myStatus: "GROUP", group: { submitted: false, groupDocumentId: "doc" } as StudentRow["group"] }),
    row("closed", { status: "CLOSED" }),
  ]);
  render(<ClassTodoCard classId="class-1" />);
  expect(await screen.findByRole("link", { name: "2 Chưa làm" })).toHaveAttribute("href", "/my-assignments?classId=class-1&tab=upcoming");
  expect(screen.getByRole("link", { name: "2 Đang làm" })).toHaveAttribute("href", "/my-assignments?classId=class-1&tab=doing");
  expect(screen.getByRole("link", { name: "2 Đã nộp" })).toHaveAttribute("href", "/my-assignments?classId=class-1&tab=submitted");
  expect(screen.getByRole("heading", { name: "Tổng quan bài tập" })).toBeInTheDocument();
  const tabs = screen.getByRole("tablist", { name: "Loại bài" });
  expect(within(tabs).getByRole("tab", { name: /Bài nhóm\s*1/ })).toBeInTheDocument();
  expect(studentClassBoard.entries().map(entry => entry.key)).toEqual(["assignments"]);
  expect(workApi.list).toHaveBeenCalledTimes(1);
});

it("does not show fake zero statistics while loading or on error, and supports an empty class", async () => {
  vi.mocked(workApi.list).mockReturnValue(new Promise(() => {}));
  render(<ClassTodoCard classId="class-1" />);
  expect(screen.getByRole("status")).toHaveTextContent("Đang tải");
  expect(screen.queryByRole("link", { name: "0 Chưa làm" })).not.toBeInTheDocument();
  cleanup(); vi.mocked(workApi.list).mockRejectedValue(new Error("offline"));
  render(<ClassTodoCard classId="class-1" />);
  expect(await screen.findByRole("alert")).toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "0 Chưa làm" })).not.toBeInTheDocument();
  cleanup(); vi.mocked(workApi.list).mockResolvedValue([]);
  render(<ClassTodoCard classId="class-1" />);
  expect(await screen.findByRole("link", { name: "0 Chưa làm" })).toBeInTheDocument();
  expect(screen.getByText("Không còn bài cá nhân cần làm.")).toBeInTheDocument();
});
