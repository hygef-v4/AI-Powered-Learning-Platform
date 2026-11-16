import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
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

beforeEach(() => { sessionStorage.clear(); vi.clearAllMocks(); vi.mocked(workApi.attempt).mockResolvedValue(attempt); });

describe("assignment workspace UI", () => {
  it.each([false, true])("flushes dirty content on in-app unmount (save failure: %s)", async (fails) => {
    vi.mocked(workApi.save).mockImplementation(async (_id, _version, content) => {
      if (fails) throw new Error("offline");
      return { attemptId: "a", contentVersion: 2, lastSavedAt: "2026-10-25T01:00:00Z", content };
    });
    const { unmount } = render(<AssignmentWorkspacePage attemptId="a" />);
    const field = await screen.findByRole("textbox", { name: "Đoạn văn" });
    field.textContent = "Bài làm mới";
    fireEvent.input(field);
    expect(workApi.save).not.toHaveBeenCalled();

    unmount();

    await waitFor(() => expect(workApi.save).toHaveBeenCalledTimes(1));
    expect(workApi.save).toHaveBeenCalledWith("a", 1, expect.objectContaining({
      blocks: expect.arrayContaining([expect.objectContaining({
        id: "answer", content: { runs: [{ text: "Bài làm mới" }] },
      })]),
    }), true);
    expect(workApi.submit).not.toHaveBeenCalled();
  });

  it("recovers dirty content after a failed departure save and return", async () => {
    vi.mocked(workApi.save).mockRejectedValue(new Error("offline"));
    const { unmount } = render(<AssignmentWorkspacePage attemptId="a" />);
    const field = await screen.findByRole("textbox", { name: "Đoạn văn" });
    field.textContent = "Unsaved departure draft";
    fireEvent.input(field);
    unmount();
    await waitFor(() => expect(workApi.save).toHaveBeenCalledTimes(1));

    render(<AssignmentWorkspacePage attemptId="a" />);

    expect(await screen.findByRole("textbox", { name: "Đoạn văn" }))
      .toHaveTextContent("Unsaved departure draft");
  });

  it("clears a recovered draft after it saves successfully", async () => {
    sessionStorage.setItem("assignment-work-draft:a", JSON.stringify({
      contentVersion: 1, content: attempt.content,
    }));
    vi.mocked(workApi.save).mockImplementation(async (_id, _version, content) => ({
      attemptId: "a", contentVersion: 2, lastSavedAt: "2026-10-25T01:00:00Z", content,
    }));
    render(<AssignmentWorkspacePage attemptId="a" />);
    await screen.findByRole("textbox", { name: "Đoạn văn" });

    fireEvent(window, new Event("pagehide"));

    await waitFor(() => expect(sessionStorage.getItem("assignment-work-draft:a")).toBeNull());
    expect(screen.getByTestId("save-state")).not.toHaveTextContent("Có thay đổi chưa lưu");
  });

  it("does not clear a newer departure draft when an older save completes", async () => {
    let finish!: (value: Saved) => void;
    vi.mocked(workApi.save).mockReturnValue(new Promise((resolve) => { finish = resolve; }));
    const { unmount } = render(<AssignmentWorkspacePage attemptId="a" />);
    const field = await screen.findByRole("textbox", { name: "Đoạn văn" });
    field.textContent = "First departure draft";
    fireEvent.input(field);
    unmount();
    const content = vi.mocked(workApi.save).mock.calls[0]![2];
    const newer = JSON.stringify({ contentVersion: 1, content: { blocks: [] } });
    sessionStorage.setItem("assignment-work-draft:a", newer);

    await act(async () => {
      finish({ attemptId: "a", contentVersion: 2, lastSavedAt: "2026-10-25T01:00:00Z", content });
    });

    expect(sessionStorage.getItem("assignment-work-draft:a")).toBe(newer);
  });

  it("waits for a pending departure save before reopening, so the next save uses the new version", async () => {
    let finish!: (value: Saved) => void;
    let committed = false;
    vi.mocked(workApi.save)
      .mockReturnValueOnce(new Promise((resolve) => { finish = resolve; }))
      .mockImplementation(async (_id, version, content) => ({
        attemptId: "a", contentVersion: version + 1, lastSavedAt: "2026-10-25T02:00:00Z", content,
      }));
    const { unmount } = render(<AssignmentWorkspacePage attemptId="a" />);
    const field = await screen.findByRole("textbox", { name: "Đoạn văn" });
    field.textContent = "Saved while leaving";
    fireEvent.input(field);
    unmount();
    const departed = vi.mocked(workApi.save).mock.calls[0]![2];
    vi.mocked(workApi.attempt).mockImplementation(async () => committed
      ? { ...attempt, contentVersion: 2, content: departed } : attempt);

    render(<AssignmentWorkspacePage attemptId="a" />);
    await act(async () => {
      committed = true;
      finish({ attemptId: "a", contentVersion: 2, lastSavedAt: "2026-10-25T01:00:00Z", content: departed });
    });
    const returned = await screen.findByRole("textbox", { name: "Đoạn văn" });
    expect(returned).toHaveTextContent("Saved while leaving");
    returned.textContent = "Typed after returning";
    fireEvent.input(returned);
    fireEvent(window, new Event("pagehide"));

    await waitFor(() => expect(workApi.save).toHaveBeenCalledTimes(2));
    expect(vi.mocked(workApi.save).mock.calls[1]![1]).toBe(2);
  });

  it.each([
    [2, "IN_PROGRESS"], [1, "SUBMITTED"],
  ] as const)("keeps server content when version/status is %s/%s", async (contentVersion, status) => {
    const content = structuredClone(attempt.content);
    content.blocks[1] = { id: "answer", origin: "STUDENT", type: "PARAGRAPH", content: {
      runs: [{ text: "Older cached draft" }],
    } };
    sessionStorage.setItem("assignment-work-draft:a", JSON.stringify({ contentVersion: 1, content }));
    vi.mocked(workApi.attempt).mockResolvedValue({ ...attempt, contentVersion, status });
    render(<AssignmentWorkspacePage attemptId="a" />);

    expect(await screen.findByText("Trả lời")).toBeInTheDocument();
    expect(screen.queryByText("Older cached draft")).toBeNull();
    expect(sessionStorage.getItem("assignment-work-draft:a")).toBeNull();
    expect(workApi.save).not.toHaveBeenCalled();
  });

  it("opens the server content when a cached draft is malformed", async () => {
    sessionStorage.setItem("assignment-work-draft:a", "invalid JSON");
    render(<AssignmentWorkspacePage attemptId="a" />);
    expect(await screen.findByRole("textbox", { name: "Đoạn văn" })).toHaveTextContent("Trả lời");
  });

  it("still attempts departure saving when browser storage is unavailable", async () => {
    vi.mocked(workApi.save).mockRejectedValue(new Error("offline"));
    const { unmount } = render(<AssignmentWorkspacePage attemptId="a" />);
    const field = await screen.findByRole("textbox", { name: "Đoạn văn" });
    field.textContent = "New answer";
    fireEvent.input(field);
    const store = vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new DOMException("Quota exceeded", "QuotaExceededError");
    });
    try {
      unmount();
      await waitFor(() => expect(workApi.save).toHaveBeenCalledTimes(1));
    } finally {
      store.mockRestore();
    }
  });

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
