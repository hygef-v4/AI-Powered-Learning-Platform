import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { StudentAssignmentsPage } from "@/features/assignmentwork/pages/StudentAssignmentsPage";
import { workApi } from "@/features/assignmentwork/api";
import type { StudentRow } from "@/features/assignmentwork/model/types";

const state = vi.hoisted(() => ({ search: "" }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn(), back: vi.fn() }), useSearchParams: () => new URLSearchParams(window.location.search) }));
vi.mock("@/features/assignmentwork/api", async (original) => ({ ...await original<typeof import("@/features/assignmentwork/api")>(), workApi: { list: vi.fn() } }));

const row = (patch: Partial<StudentRow>): StudentRow => ({
  assignmentId: "a", classId: "c1", classCode: "SE1901", subjectCode: "SWD392", scope: "CLASS", title: "Bài", submissionMode: "INDIVIDUAL",
  gradingMode: "GRADED", diagramEnabled: false, status: "OPEN", opensAt: null, closesAt: null, lateUntil: null, maxAttempts: 1,
  myStatus: "NOT_STARTED", attemptCount: 0, submittedCount: 0, inProgressAttemptId: null, lastSubmittedAt: null, group: null, ...patch,
});

beforeEach(() => {
  state.search = "";
  window.history.replaceState(null, "", "/my-assignments");
  vi.mocked(workApi.list).mockResolvedValue([
    row({ assignmentId: "a1", title: "Sơ đồ lớp" }),
    row({ assignmentId: "a2", title: "Ca kiểm thử", classId: "c2", classCode: "SE1903", subjectCode: "SWT301" }),
  ]);
});

it("trang chung có ô lọc lớp cạnh tiêu đề; chọn một lớp thì chỉ còn bài của lớp đó", async () => {
  render(<StudentAssignmentsPage />);
  expect(await screen.findByText("Sơ đồ lớp")).toBeInTheDocument();
  expect(screen.getByRole("columnheader", { name: "Lớp" })).toBeInTheDocument();
  expect(screen.getByRole("columnheader", { name: "Môn học" })).toBeInTheDocument();
  const firstRow = within(screen.getByTestId("student-row-a1"));
  expect(firstRow.getByRole("cell", { name: "SE1901" })).toBeInTheDocument();
  expect(firstRow.getByRole("cell", { name: "SWD392" })).toBeInTheDocument();
  const secondRow = within(screen.getByTestId("student-row-a2"));
  expect(secondRow.getByRole("cell", { name: "SE1903" })).toBeInTheDocument();
  expect(secondRow.getByRole("cell", { name: "SWT301" })).toBeInTheDocument();
  const filter = screen.getByTestId("assignment-class-filter");
  expect(filter).toHaveTextContent("Tất cả lớp");
  fireEvent.click(within(filter).getByRole("menuitem", { name: /SE1903/, hidden: true }));
  expect(screen.queryByText("Sơ đồ lớp")).not.toBeInTheDocument();
  expect(screen.getByText("Ca kiểm thử")).toBeInTheDocument();
  expect(filter).toHaveTextContent("SWT301");
});

it("trang bài tập của một lớp (?classId=) không có ô lọc lớp", async () => {
  state.search = "classId=c1";
  window.history.replaceState(null, "", `?${state.search}`);
  render(<StudentAssignmentsPage />);
  await screen.findByText("Sơ đồ lớp");
  expect(screen.queryByTestId("assignment-class-filter")).not.toBeInTheDocument();
});


it("restores list tab, kind, class, search and page on remount without a history entry per search", async () => {
  vi.mocked(workApi.list).mockResolvedValue(Array.from({ length: 25 }, (_, index) => row({
    assignmentId: String(index), title: "Nhóm " + index, submissionMode: "GROUP", submittedCount: 1,
  })));
  window.history.replaceState(null, "", "/my-assignments?tab=submitted&mode=GROUP&classFilter=c1&q=Nh%C3%B3m&page=1");
  const view = render(<StudentAssignmentsPage />);
  expect(await screen.findByText("Nhóm 20")).toBeInTheDocument();
  expect(screen.queryByText("Nhóm 0")).not.toBeInTheDocument();
  expect(screen.getByRole("tab", { name: /Đã nộp/ })).toHaveAttribute("aria-selected", "true");
  expect(within(screen.getByTestId("student-row-20")).getByRole("cell", { name: "SWD392" })).toBeInTheDocument();
  expect(screen.getByLabelText("Loại bài")).toHaveValue("GROUP");
  const historyLength = window.history.length;
  fireEvent.change(screen.getByLabelText("Tìm bài tập"), { target: { value: "Nhóm 2" } });
  expect(window.history.length).toBe(historyLength);
  expect(new URLSearchParams(window.location.search).get("page")).toBe("0");
  view.unmount();
  render(<StudentAssignmentsPage />);
  expect(await screen.findByText("Nhóm 2")).toBeInTheDocument();
  expect(screen.getByLabelText("Tìm bài tập")).toHaveValue("Nhóm 2");
  expect(screen.getByLabelText("Loại bài")).toHaveValue("GROUP");
});

it("ignores invalid list values and retains focus-based tab selection", async () => {
  vi.mocked(workApi.list).mockResolvedValue([row({ assignmentId: "done", myStatus: "SUBMITTED", submittedCount: 1, title: "Đã làm" })]);
  window.history.replaceState(null, "", "/my-assignments?tab=invalid&mode=invalid&page=-2&focus=done");
  render(<StudentAssignmentsPage />);
  expect(await screen.findByText("Đã làm")).toBeInTheDocument();
  expect(screen.getByRole("tab", { name: /Đã nộp/ })).toHaveAttribute("aria-selected", "true");
  expect(screen.getByLabelText("Loại bài")).toHaveValue("");
});

it("bài đang sửa để nộp lại chỉ nằm ở Đang làm, không lẫn vào Đã nộp", async () => {
  vi.mocked(workApi.list).mockResolvedValue([
    row({ assignmentId: "done", myStatus: "SUBMITTED", submittedCount: 1, title: "Đã xong" }),
    row({ assignmentId: "redo", myStatus: "IN_PROGRESS", submittedCount: 1, inProgressAttemptId: "t", title: "Đang sửa" }),
  ]);
  window.history.replaceState(null, "", "/my-assignments?tab=submitted");
  render(<StudentAssignmentsPage />);
  expect(await screen.findByText("Đã xong")).toBeInTheDocument();
  expect(screen.queryByText("Đang sửa")).not.toBeInTheDocument();
  expect(screen.getByRole("tab", { name: /Đã nộp/ })).toHaveTextContent("1");
  expect(screen.getByRole("tab", { name: /Đang làm/ })).toHaveTextContent("1");
});

it.each([
  { status: "CLOSED", myStatus: "GROUP", submissionMode: "GROUP", group: null },
  { status: "CLOSED", myStatus: "GROUP", submissionMode: "GROUP", group: { groupDocumentId: "d", submitted: false, submittedAt: null } },
  { status: "CLOSED", myStatus: "MISSED", submissionMode: "INDIVIDUAL", group: null },
  { status: "OPEN", myStatus: "MISSED", submissionMode: "INDIVIDUAL", group: null },
] satisfies Partial<StudentRow>[])("bài chưa nộp $status/$myStatus xuất hiện trong Đã đóng", async patch => {
  vi.mocked(workApi.list).mockResolvedValue([row({ ...patch, title: "Bệnh viện thông minh" })]);
  window.history.replaceState(null, "", "/my-assignments?classId=c1&tab=closed");
  render(<StudentAssignmentsPage />);
  const closedTab = await screen.findByRole("tab", { name: /Đã đóng/ });
  expect(await screen.findByRole("link", { name: "Bệnh viện thông minh" })).toHaveAttribute(
    "href", "/my-assignments/a?classId=c1",
  );
  expect(closedTab).toHaveAttribute("aria-selected", "true");
  expect(closedTab).toHaveTextContent("1");
  expect(within(screen.getByTestId("student-row-a")).getByText("Hết hạn")).toBeInTheDocument();
  for (const name of [/^Chưa làm/, /^Đang làm/, /^Đã nộp/]) {
    expect(screen.getByRole("tab", { name })).toHaveTextContent("0");
  }
});

it("bài nhóm đã đóng nhưng đã nộp vẫn chỉ ở Đã nộp, kể cả có bản sửa sau lần nộp", async () => {
  vi.mocked(workApi.list).mockResolvedValue([row({
    title: "Nhóm đã nộp", submissionMode: "GROUP", myStatus: "GROUP", status: "CLOSED",
    group: { groupDocumentId: "d", submitted: true, submittedAt: "2026-11-12T00:00:00Z", changedSinceSubmit: true },
  })]);
  window.history.replaceState(null, "", "/my-assignments?tab=submitted");
  render(<StudentAssignmentsPage />);
  expect(await screen.findByText("Nhóm đã nộp")).toBeInTheDocument();
  expect(screen.getByRole("tab", { name: /^Đã nộp/ })).toHaveTextContent("1");
  expect(screen.getByRole("tab", { name: /Đã đóng/ })).toHaveTextContent("0");
  expect(within(screen.getByTestId("student-row-a")).getByText("Đã nộp")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("tab", { name: /Đã đóng/ }));
  expect(screen.queryByTestId("student-row-a")).not.toBeInTheDocument();
  expect(screen.getByText("Không có bài đã đóng chưa nộp.")).toBeInTheDocument();
});

it("quay về từ bài đã đóng tự chọn tab chứa bài đó", async () => {
  vi.mocked(workApi.list).mockResolvedValue([row({ title: "Bài hết hạn", status: "CLOSED", myStatus: "MISSED" })]);
  window.history.replaceState(null, "", "/my-assignments?classId=c1&focus=a");
  render(<StudentAssignmentsPage />);
  expect(await screen.findByText("Bài hết hạn")).toBeInTheDocument();
  expect(screen.getByRole("tab", { name: /Đã đóng/ })).toHaveAttribute("aria-selected", "true");
});

it("Đã đóng giữ lọc và phân trang trong URL khi mở lại", async () => {
  vi.mocked(workApi.list).mockResolvedValue(Array.from({ length: 25 }, (_, index) => row({
    assignmentId: String(index), title: "Hết hạn " + index, status: "CLOSED", myStatus: "GROUP", submissionMode: "GROUP",
  })));
  window.history.replaceState(null, "", "/my-assignments?tab=closed&mode=GROUP&classFilter=c1&q=H%E1%BA%BFt&page=1");
  const view = render(<StudentAssignmentsPage />);
  expect(await screen.findByText("Hết hạn 20")).toBeInTheDocument();
  expect(screen.queryByText("Hết hạn 0")).not.toBeInTheDocument();
  expect(screen.getByRole("tab", { name: /Đã đóng/ })).toHaveTextContent("25");
  view.unmount();
  render(<StudentAssignmentsPage />);
  expect(await screen.findByText("Hết hạn 20")).toBeInTheDocument();
  expect(screen.getByLabelText("Loại bài")).toHaveValue("GROUP");
  expect(screen.getByLabelText("Tìm bài tập")).toHaveValue("Hết");
});
