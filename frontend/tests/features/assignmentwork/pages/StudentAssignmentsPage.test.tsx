import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { StudentAssignmentsPage } from "@/features/assignmentwork/pages/StudentAssignmentsPage";
import { workApi } from "@/features/assignmentwork/api";
import type { StudentRow } from "@/features/assignmentwork/model/types";

const state = vi.hoisted(() => ({ search: "" }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }), useSearchParams: () => new URLSearchParams(state.search) }));
vi.mock("@/features/assignmentwork/api", async (original) => ({ ...await original<typeof import("@/features/assignmentwork/api")>(), workApi: { list: vi.fn() } }));

const row = (patch: Partial<StudentRow>): StudentRow => ({
  assignmentId: "a", classId: "c1", classCode: "SE1901", className: "Kiến trúc phần mềm", scope: "CLASS", title: "Bài", submissionMode: "INDIVIDUAL",
  gradingMode: "GRADED", diagramEnabled: false, status: "OPEN", opensAt: null, closesAt: null, lateUntil: null, maxAttempts: 1,
  myStatus: "NOT_STARTED", attemptCount: 0, submittedCount: 0, inProgressAttemptId: null, lastSubmittedAt: null, group: null, ...patch,
});

beforeEach(() => {
  state.search = "";
  vi.mocked(workApi.list).mockResolvedValue([
    row({ assignmentId: "a1", title: "Sơ đồ lớp" }),
    row({ assignmentId: "a2", title: "Ca kiểm thử", classId: "c2", classCode: "SE1903", className: "Kiểm thử phần mềm" }),
  ]);
});

it("trang chung có ô lọc lớp cạnh tiêu đề; chọn một lớp thì chỉ còn bài của lớp đó", async () => {
  render(<StudentAssignmentsPage />);
  expect(await screen.findByText("Sơ đồ lớp")).toBeInTheDocument();
  const filter = screen.getByTestId("assignment-class-filter");
  expect(filter).toHaveTextContent("Tất cả lớp");
  fireEvent.click(within(filter).getByRole("menuitem", { name: /SE1903/, hidden: true }));
  expect(screen.queryByText("Sơ đồ lớp")).not.toBeInTheDocument();
  expect(screen.getByText("Ca kiểm thử")).toBeInTheDocument();
  expect(filter).toHaveTextContent("Kiểm thử phần mềm");
});

it("trang bài tập của một lớp (?classId=) không có ô lọc lớp", async () => {
  state.search = "classId=c1";
  render(<StudentAssignmentsPage />);
  await screen.findByText("Sơ đồ lớp");
  expect(screen.queryByTestId("assignment-class-filter")).not.toBeInTheDocument();
});
