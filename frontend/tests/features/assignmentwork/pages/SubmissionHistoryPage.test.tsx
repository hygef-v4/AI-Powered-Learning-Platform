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

describe("SubmissionHistoryPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("chuyển lượt trên thanh công cụ: lượt trước, lượt sau, chọn nhanh; quay lại về đúng tab của bài", async () => {
    vi.spyOn(workApi, "detail").mockResolvedValue({ title: "Bài", gradingMode: "GRADED" } as StudentDetail);
    vi.spyOn(workApi, "history").mockResolvedValue([
      summary({ attemptId: "a2", attemptNo: 2, late: true, submittedAt: "2026-10-26T12:30:00Z" }),
      summary({ attemptId: "a1", attemptNo: 1 }),
    ]);
    const load = vi.spyOn(workApi, "attempt").mockImplementation((id) => Promise.resolve(attempt(id)));

    render(<SubmissionHistoryPage assignmentId="x" />);

    const picker = await screen.findByRole("combobox", { name: "Chọn lượt" });
    // Chỉ ghi thời gian, mới nhất trước.
    const options = within(picker).getAllByRole("option");
    expect(options.map((o) => o.getAttribute("value"))).toEqual(["a2", "a1"]);
    expect(options.map((o) => o.textContent)).not.toContainEqual(expect.stringMatching(/nộp/i));
    // Mở sẵn lượt đã nộp đầu tiên trong danh sách (lượt 2); lượt sau không còn.
    expect(picker).toHaveValue("a2");
    expect(screen.getByRole("button", { name: "Lượt sau" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Lượt trước" }));
    await waitFor(() => expect(load).toHaveBeenLastCalledWith("a1", expect.anything()));
    // Header như trang làm bài: tên bài, chuyển cách xem ở giữa, Tải DOCX trong menu "…"; thanh công cụ chỉ còn chuyển lượt.
    const bar = screen.getByRole("banner");
    expect(within(bar).getByRole("heading", { level: 1 })).toHaveTextContent("Bài");
    expect(within(bar).getByTestId("view-slot")).toBeInTheDocument();
    expect(within(bar).getByText("Tải DOCX")).toBeInTheDocument();
    const toolbar = screen.getByRole("toolbar", { name: "Thanh công cụ" });
    expect(within(toolbar).queryByText("Tải DOCX")).toBeNull();
    expect(within(toolbar).queryByLabelText("Tải DOCX")).toBeNull();
  });
});

describe("SubmissionHistoryPage chỉ có lượt đã nộp", () => {
  afterEach(() => vi.restoreAllMocks());

  it("lượt đang làm không hiện trong lịch sử; cạnh tên bài không ghi gì", async () => {
    vi.spyOn(workApi, "detail").mockResolvedValue({ title: "Bài", gradingMode: "GRADED" } as StudentDetail);
    vi.spyOn(workApi, "history").mockResolvedValue([
      summary({ attemptId: "a3", attemptNo: 3, status: "IN_PROGRESS", submittedAt: null }),
      summary({ attemptId: "a2", attemptNo: 2 }),
      summary({ attemptId: "a1", attemptNo: 1 }),
    ]);
    const load = vi.spyOn(workApi, "attempt").mockImplementation((id) => Promise.resolve(attempt(id)));
    render(<SubmissionHistoryPage assignmentId="x" />);
    const picker = await screen.findByRole("combobox", { name: "Chọn lượt" });
    expect(within(picker).getAllByRole("option").map((o) => o.getAttribute("value"))).toEqual(["a2", "a1"]);
    expect(picker).toHaveValue("a2");
    expect(load).not.toHaveBeenCalledWith("a3", expect.anything());
    const heading = within(screen.getByRole("banner")).getByRole("heading", { level: 1 });
    expect(heading.parentElement?.children).toHaveLength(1);
  });

  it("chưa nộp lượt nào thì báo chưa nộp", async () => {
    vi.spyOn(workApi, "detail").mockResolvedValue({ title: "Bài", gradingMode: "GRADED" } as StudentDetail);
    vi.spyOn(workApi, "history").mockResolvedValue([summary({ attemptId: "a1", status: "IN_PROGRESS", submittedAt: null })]);
    render(<SubmissionHistoryPage assignmentId="x" />);
    expect(await screen.findByText("Bạn chưa nộp lượt nào.")).toBeInTheDocument();
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
