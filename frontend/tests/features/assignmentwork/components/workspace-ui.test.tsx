import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AUTOSAVE_MS, AssignmentWorkspacePage } from "@/features/assignmentwork/pages/AssignmentWorkspacePage";
import { workApi } from "@/features/assignmentwork/api";
import { ApiError } from "@/shared/api/client";
import type { Attempt, Saved } from "@/features/assignmentwork/model/types";

const { push } = vi.hoisted(() => ({ push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
vi.mock("@/features/assignmentwork/api", async (loadOriginal) => ({
  ...await loadOriginal<typeof import("@/features/assignmentwork/api")>(),
  workApi: { attempt: vi.fn(), save: vi.fn(), submit: vi.fn() },
  attemptImageUrl: () => () => "/image",
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
  it("autosaves edits made while the previous autosave is in flight", async () => {
    vi.useFakeTimers();
    let finish!: (value: Saved) => void;
    vi.mocked(workApi.save)
      .mockReturnValueOnce(new Promise((resolve) => { finish = resolve; }))
      .mockImplementation(async (_id, version, content) => ({
        attemptId: "a", contentVersion: version + 1, lastSavedAt: attempt.serverTime, content,
      }));
    const { unmount } = render(<AssignmentWorkspacePage attemptId="a" />);
    try {
      await act(async () => {});
      const field = screen.getByRole("textbox", { name: "Đoạn văn" });
      field.textContent = "First autosave";
      fireEvent.input(field);
      await act(() => vi.advanceTimersByTimeAsync(AUTOSAVE_MS));
      expect(workApi.save).toHaveBeenCalledTimes(1);
      const firstContent = vi.mocked(workApi.save).mock.calls[0]![2];

      field.textContent = "Edited during autosave";
      fireEvent.input(field);
      await act(async () => {
        finish({
          attemptId: "a", contentVersion: 2, lastSavedAt: attempt.serverTime, content: firstContent,
        });
      });
      await act(() => vi.advanceTimersByTimeAsync(AUTOSAVE_MS));

      expect(workApi.save).toHaveBeenNthCalledWith(2, "a", 2, expect.objectContaining({
        blocks: expect.arrayContaining([expect.objectContaining({
          id: "answer", content: { runs: [{ text: "Edited during autosave" }] },
        })]),
      }), false);
    } finally {
      unmount();
      vi.useRealTimers();
    }
  });

  it.each([false, true])("autosaves corrections around a validation failure (edited in flight: %s)", async (editInFlight) => {
    vi.useFakeTimers();
    let fail!: (error: Error) => void;
    vi.mocked(workApi.save)
      .mockReturnValueOnce(new Promise((_resolve, reject) => { fail = reject; }))
      .mockImplementation(async (_id, version, content) => ({
        attemptId: "a", contentVersion: version + 1, lastSavedAt: attempt.serverTime, content,
      }));
    const { unmount } = render(<AssignmentWorkspacePage attemptId="a" />);
    try {
      await act(async () => {});
      const field = screen.getByRole("textbox", { name: "Đoạn văn" });
      field.textContent = "Invalid answer";
      fireEvent.input(field);
      await act(() => vi.advanceTimersByTimeAsync(AUTOSAVE_MS));
      const correctAnswer = () => {
        field.textContent = "Corrected answer";
        fireEvent.input(field);
      };
      if (editInFlight) correctAnswer();
      await act(async () => { fail(new ApiError({ status: 400, title: "Invalid draft" })); });
      expect(screen.getByRole("alert")).toBeInTheDocument();
      if (!editInFlight) {
        await act(() => vi.advanceTimersByTimeAsync(AUTOSAVE_MS));
        expect(workApi.save).toHaveBeenCalledTimes(1);
        correctAnswer();
      }
      await act(() => vi.advanceTimersByTimeAsync(AUTOSAVE_MS));

      expect(workApi.save).toHaveBeenNthCalledWith(2, "a", 1, expect.objectContaining({
        blocks: expect.arrayContaining([expect.objectContaining({
          id: "answer", content: { runs: [{ text: "Corrected answer" }] },
        })]),
      }), false);
    } finally {
      unmount();
      vi.useRealTimers();
    }
  });

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

  it("refreshes an expired session and downloads the DOCX with its server filename", async () => {
    vi.useFakeTimers();
    const fetchFile = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response("docx", { headers: {
        "Content-Disposition": "attachment; filename*=UTF-8''B%C3%A0i%20l%C3%A0m.docx",
      } }));
    vi.stubGlobal("fetch", fetchFile);
    const createObjectURL = vi.fn(() => "blob:attempt");
    const revokeObjectURL = vi.fn();
    vi.stubGlobal("URL", class extends URL {
      static override createObjectURL = createObjectURL;
      static override revokeObjectURL = revokeObjectURL;
    });
    const downloaded: string[] = [];
    const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(function (this: HTMLAnchorElement) {
      downloaded.push(this.download);
    });
    try {
      render(<AssignmentWorkspacePage attemptId="a" />);
      await act(async () => {});
      fireEvent.click(screen.getByLabelText("Thao tác khác"));
      fireEvent.click(screen.getByRole("menuitem", { name: "Tải DOCX" }));
      await act(async () => {});

      expect(fetchFile.mock.calls.map(([path]) => path)).toEqual([
        "/api/v1/attempts/a/export.docx", "/api/v1/auth/refresh", "/api/v1/attempts/a/export.docx",
      ]);
      expect(downloaded).toEqual(["Bài làm.docx"]);
      expect(screen.getByRole("textbox", { name: "Đoạn văn" })).toHaveTextContent("Trả lời");
      await act(() => vi.advanceTimersByTimeAsync(1000));
      expect(revokeObjectURL).toHaveBeenCalledWith("blob:attempt");
    } finally {
      click.mockRestore();
      vi.unstubAllGlobals();
      vi.useRealTimers();
    }
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
