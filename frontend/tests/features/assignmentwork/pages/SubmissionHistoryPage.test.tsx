import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { workApi } from "@/features/assignmentwork/api";
import { resultByPart } from "@/features/assignmentwork/model/attemptResult";
import { SubmissionHistoryPage } from "@/features/assignmentwork/pages/SubmissionHistoryPage";
import type { Attempt, AttemptSummary, StudentDetail } from "@/features/assignmentwork/model/types";

vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams("classId=c1") }));

const summary = (patch: Partial<AttemptSummary>): AttemptSummary => ({
  attemptId: "a1", attemptNo: 1, status: "SUBMITTED", startedAt: "2026-10-25T12:00:00Z", deadlineAt: "2026-10-26T12:00:00Z",
  lastSavedAt: null, submittedAt: "2026-10-25T12:30:00Z", late: false, submitMode: "MANUAL", receiptHash: "h".repeat(64),
  graded: false, feedbackCount: 0, warnings: [], ...patch,
});

const attempt = (attemptId: string) => ({ attemptId, content: { blocks: [] }, parts: [] }) as unknown as Attempt;

/** Bài cá nhân đã qua hạn nộp đúng giờ (lịch sử mở). */
const pastDeadline = { title: "Bài", gradingMode: "GRADED", closesAt: "2026-10-26T00:00:00Z", lateUntil: "2026-10-28T00:00:00Z",
  serverTime: "2026-10-27T00:00:00Z" } as StudentDetail;

describe("SubmissionHistoryPage (yêu cầu 2026-11-10)", () => {
  afterEach(() => vi.restoreAllMocks());

  it("bài cá nhân: nộp lại là ghi đè, lịch sử chỉ có bản đúng hạn và bản nộp muộn; chuyển qua lại trên thanh công cụ", async () => {
    vi.spyOn(workApi, "detail").mockResolvedValue(pastDeadline);
    vi.spyOn(workApi, "history").mockResolvedValue([
      summary({ attemptId: "a4", attemptNo: 4, status: "IN_PROGRESS", submittedAt: null }),
      summary({ attemptId: "a3", attemptNo: 3, late: true, submittedAt: "2026-10-26T12:30:00Z" }),
      summary({ attemptId: "a2", attemptNo: 2, late: true, submittedAt: "2026-10-26T10:00:00Z" }),
      summary({ attemptId: "a1", attemptNo: 1 }),
    ]);
    const load = vi.spyOn(workApi, "attempt").mockImplementation((id) => Promise.resolve(attempt(id)));

    render(<SubmissionHistoryPage assignmentId="x" />);

    const picker = await screen.findByRole("combobox", { name: "Chọn bài nộp" });
    const options = within(picker).getAllByRole("option");
    expect(options.map((o) => o.getAttribute("value"))).toEqual(["a3", "a1"]);
    expect(options[0]).toHaveTextContent(/^Bản nộp muộn – /);
    expect(options[1]).toHaveTextContent(/^Bản đúng hạn – /);
    expect(options.map((o) => o.textContent)).not.toContainEqual(expect.stringMatching(/lần|lượt/i));
    expect(picker).toHaveValue("a3");
    expect(load).not.toHaveBeenCalledWith("a4", expect.anything());
    expect(screen.getByRole("button", { name: "Bài nộp sau" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Bài nộp trước" }));
    await waitFor(() => expect(load).toHaveBeenLastCalledWith("a1", expect.anything()));
    // Header như trang làm bài: tên bài, chuyển cách xem ở giữa, Tải DOCX trong menu "…".
    const bar = screen.getByRole("banner");
    expect(within(bar).getByRole("heading", { level: 1 })).toHaveTextContent("Bài");
    expect(within(bar).getByTestId("view-slot")).toBeInTheDocument();
    expect(within(bar).getByText("Tải DOCX")).toBeInTheDocument();
    const toolbar = screen.getByRole("toolbar", { name: "Thanh công cụ" });
    expect(within(toolbar).queryByText("Tải DOCX")).toBeNull();
  });

  it("không có bản nộp muộn thì chỉ một bản, không có ô chọn", async () => {
    vi.spyOn(workApi, "detail").mockResolvedValue(pastDeadline);
    vi.spyOn(workApi, "history").mockResolvedValue([summary({ attemptId: "a2", attemptNo: 2 }), summary({ attemptId: "a1", attemptNo: 1 })]);
    const load = vi.spyOn(workApi, "attempt").mockImplementation((id) => Promise.resolve(attempt(id)));
    render(<SubmissionHistoryPage assignmentId="x" />);
    await waitFor(() => expect(load).toHaveBeenCalledWith("a2", expect.anything()));
    expect(screen.queryByRole("combobox", { name: "Chọn bài nộp" })).toBeNull();
  });

  it("chưa qua hạn nộp đúng giờ thì chưa có lịch sử", async () => {
    vi.spyOn(workApi, "detail").mockResolvedValue({ ...pastDeadline, serverTime: "2026-10-25T23:00:00Z" });
    vi.spyOn(workApi, "history").mockResolvedValue([summary({ attemptId: "a1" })]);
    const load = vi.spyOn(workApi, "attempt");
    render(<SubmissionHistoryPage assignmentId="x" />);
    expect(await screen.findByText("Lịch sử nộp bài hiện sau khi đã nộp và qua hạn nộp.")).toBeInTheDocument();
    expect(load).not.toHaveBeenCalled();
  });

  it("bài luyện tập: đủ các lượt đã nộp, chỉ ghi giờ, không ghi lượt", async () => {
    vi.spyOn(workApi, "detail").mockResolvedValue({ ...pastDeadline, gradingMode: "PRACTICE", serverTime: "2026-10-25T23:00:00Z" });
    vi.spyOn(workApi, "history").mockResolvedValue([summary({ attemptId: "a2", attemptNo: 2 }), summary({ attemptId: "a1", attemptNo: 1 })]);
    vi.spyOn(workApi, "attempt").mockImplementation((id) => Promise.resolve(attempt(id)));
    render(<SubmissionHistoryPage assignmentId="x" />);
    const picker = await screen.findByRole("combobox", { name: "Chọn bài nộp" });
    const options = within(picker).getAllByRole("option");
    expect(options.map((o) => o.getAttribute("value"))).toEqual(["a2", "a1"]);
    expect(options.map((o) => o.textContent)).not.toContainEqual(expect.stringMatching(/lượt/i));
  });

  it("chưa nộp bài nào thì báo chưa nộp", async () => {
    vi.spyOn(workApi, "detail").mockResolvedValue(pastDeadline);
    vi.spyOn(workApi, "history").mockResolvedValue([summary({ attemptId: "a1", status: "IN_PROGRESS", submittedAt: null })]);
    render(<SubmissionHistoryPage assignmentId="x" />);
    expect(await screen.findByText("Bạn chưa nộp bài nào.")).toBeInTheDocument();
  });
});

describe("resultByPart", () => {
  it("gom điểm các mục rubric về nhánh con trực tiếp của gốc tài liệu, theo thứ tự trong tài liệu", () => {
    const item = (partId: string, awarded: number, max: number) => ({ rubricId: partId, partId, criterionTitle: "", itemText: partId, awarded, max, evidence: null, reason: null, suggestion: null });
    const parts = [
      { partId: "h11", headingBlockId: "h11", ancestorHeadingIds: ["c1"], title: "Chương 1 › 1.1" },
      { partId: "h12", headingBlockId: "h12", ancestorHeadingIds: ["c1"], title: "Chương 1 › 1.2" },
      { partId: "c2", headingBlockId: "c2", ancestorHeadingIds: [], title: "Chương 2" },
    ];
    expect(resultByPart([item("c2", 1, 2), item("h11", 2, 3), item("h12", 1, 1)], parts)).toEqual([
      expect.objectContaining({ title: "Chương 1", awarded: 3, max: 4 }),
      expect.objectContaining({ title: "Chương 2", awarded: 1, max: 2 }),
    ]);
  });
});
