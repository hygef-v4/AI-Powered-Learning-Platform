import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AssignmentWorkspacePage } from "@/features/assignmentwork/pages/AssignmentWorkspacePage";
import { workApi } from "@/features/assignmentwork/api";
import type { Attempt, Saved } from "@/features/assignmentwork/model/types";

const { push } = vi.hoisted(() => ({ push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
vi.mock("@/features/assignmentwork/api", () => ({
  workApi: { attempt: vi.fn(), save: vi.fn(), submit: vi.fn() },
  attemptImageUrl: () => () => "/image", exportUrl: () => "/export.docx",
}));
const attempt: Attempt = {
  attemptId: "a", assignmentId: "assignment", classId: "class", title: "Bài kiểm tra", attemptNo: 1, maxAttempts: 1,
  status: "IN_PROGRESS", content: { blocks: [
    { id: "q1", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Câu 1" } },
    { id: "answer", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Trả lời" }] } },
  ] }, contentVersion: 1, startedAt: "2026-10-25T00:00:00Z", deadlineAt: "2026-10-30T00:00:00Z",
  lastSavedAt: null, submittedAt: null, late: false, submitMode: null, receiptHash: null, warnings: [],
  instructions: "Thời gian 85 phút", gradingMode: "GRADED", submissionMode: "INDIVIDUAL", diagramEnabled: true,
  requiredDiagrams: {}, parts: [], maxPoints: 10, activityMonitoringEnabled: false, activityNotice: null,
  closesAt: null, lateUntil: null, editable: true, serverTime: "2026-10-25T00:00:00Z",
};

beforeEach(() => { vi.clearAllMocks(); vi.mocked(workApi.attempt).mockResolvedValue(attempt); });

describe("assignment workspace UI", () => {
  it("looks like the teacher preview: top bar with title, status and view switch; menu does not modify the draft", async () => {
    render(<AssignmentWorkspacePage attemptId="a" />);
    const bar = await screen.findByRole("banner");
    expect(within(bar).getByRole("heading", { level: 1 })).toHaveTextContent(attempt.title);
    expect(within(bar).getByText(/^Còn /)).toBeInTheDocument();
    expect(bar.querySelector("span[title^='Hạn nộp:']")?.getAttribute("title")).toContain("30/10/2026");
    // Cạnh tên bài chỉ có thời gian còn lại, không ghi lượt.
    expect(within(bar).queryByText(/^Lượt /)).toBeNull();
    expect(screen.getByTestId("view-slot")).toBeInTheDocument();
    expect(screen.getByTestId("assignment-kind")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Chế độ tập trung" })).toBeNull();
    fireEvent.click(within(bar).getByLabelText("Thao tác khác"));
    expect(within(bar).getByText("Tải DOCX")).toBeInTheDocument();
    expect(workApi.save).not.toHaveBeenCalled();
    expect(workApi.submit).not.toHaveBeenCalled();
  });

  it("submit still confirms and waits for the newest draft to save", async () => {
    let finish!: (value: Saved) => void;
    vi.mocked(workApi.save).mockReturnValue(new Promise((resolve) => { finish = resolve; }));
    vi.mocked(workApi.submit).mockResolvedValue({ attemptId: "a" } as Awaited<ReturnType<typeof workApi.submit>>);
    render(<AssignmentWorkspacePage attemptId="a" />);
    const field = await screen.findByRole("textbox", { name: "Đoạn văn" });
    field.textContent = "Bài làm mới";
    fireEvent.input(field);
    expect(screen.getByTestId("save-state")).toHaveTextContent("Có thay đổi chưa lưu");
    fireEvent.click(screen.getByTestId("open-submit"));
    expect(workApi.submit).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("confirm-submit"));
    await waitFor(() => expect(workApi.save).toHaveBeenCalled());
    expect(workApi.submit).not.toHaveBeenCalled();
    const savedContent = vi.mocked(workApi.save).mock.calls[0]![2];
    expect(savedContent.blocks[1]).toMatchObject({ content: { runs: [{ text: "Bài làm mới" }] } });
    finish({ attemptId: "a", contentVersion: 2, lastSavedAt: "2026-10-25T01:00:00Z", content: savedContent });
    await waitFor(() => expect(workApi.submit).toHaveBeenCalledWith("a", 2));
    await waitFor(() => expect(push).toHaveBeenCalledWith("/my-assignments/assignment?classId=class"));
  });

  it("submitted attempt remains read-only", async () => {
    vi.mocked(workApi.attempt).mockResolvedValue({ ...attempt, editable: false, status: "SUBMITTED", submittedAt: "2026-10-25T01:00:00Z" });
    render(<AssignmentWorkspacePage attemptId="a" />);
    expect(await screen.findByText(/Đã nộp lúc/)).toBeInTheDocument();
    expect(screen.queryByRole("region", { name: "Lưu và nộp bài" })).toBeNull();
    expect(screen.queryByTestId("open-submit")).toBeNull();
    expect(screen.queryByRole("textbox")).toBeNull();
    const editor = screen.getByTestId("document-editor");
    expect(within(editor).queryByRole("button", { name: "Thêm sơ đồ cho câu này" })).toBeNull();
  });
});
