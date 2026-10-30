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
  it("focus toggle and document menu do not modify the draft; deadline has a date", async () => {
    render(<AssignmentWorkspacePage attemptId="a" />);
    const focus = await screen.findByRole("button", { name: "Chế độ tập trung" });
    expect(screen.getByText(/Hạn nộp:/)).toHaveTextContent("30/10/2026");
    expect(screen.getByText(/Hạn nộp:/)).toHaveTextContent("đến hạn nộp");
    expect(screen.getByRole("region", { name: "Lưu và nộp bài" })).toHaveClass("sticky", "bottom-0");
    expect(screen.getByRole("button", { name: "Lưu bài" })).toBeDisabled();
    fireEvent.click(focus);
    expect(screen.getByRole("button", { name: "Thoát tập trung" })).toHaveAttribute("aria-pressed", "true");
    expect(document.querySelector('[data-workspace-focus="true"]')).not.toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Thoát tập trung" }));
    expect(document.querySelector('[data-workspace-focus="true"]')).toBeNull();
    fireEvent.click(screen.getByText("Tệp DOCX"));
    expect(screen.getByRole("link", { name: "Tải DOCX" })).toHaveAttribute("href", "/export.docx");
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
    expect(screen.getByRole("region", { name: "Lưu và nộp bài" })).toHaveTextContent("Có thay đổi chưa lưu");
    fireEvent.click(screen.getByTestId("open-submit"));
    expect(workApi.submit).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("confirm-submit"));
    await waitFor(() => expect(workApi.save).toHaveBeenCalled());
    expect(workApi.submit).not.toHaveBeenCalled();
    const savedContent = vi.mocked(workApi.save).mock.calls[0]![2];
    expect(savedContent.blocks[1]).toMatchObject({ content: { runs: [{ text: "Bài làm mới" }] } });
    finish({ attemptId: "a", contentVersion: 2, lastSavedAt: "2026-10-25T01:00:00Z", content: savedContent });
    await waitFor(() => expect(workApi.submit).toHaveBeenCalledWith("a", 2));
    await waitFor(() => expect(push).toHaveBeenCalledWith("/attempts/a/receipt"));
  });

  it("submitted attempt remains read-only", async () => {
    vi.mocked(workApi.attempt).mockResolvedValue({ ...attempt, editable: false, status: "SUBMITTED", submittedAt: "2026-10-25T01:00:00Z" });
    render(<AssignmentWorkspacePage attemptId="a" />);
    expect(await screen.findByRole("button", { name: "Xem biên nhận" })).toBeInTheDocument();
    expect(screen.queryByRole("region", { name: "Lưu và nộp bài" })).toBeNull();
    expect(screen.queryByTestId("open-submit")).toBeNull();
    expect(screen.queryByRole("textbox")).toBeNull();
    const editor = screen.getByTestId("document-editor");
    expect(within(editor).queryByRole("button", { name: "Thêm sơ đồ cho câu này" })).toBeNull();
  });
});
