import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { workApi } from "./api";
import { SubmissionHistoryPage } from "./SubmissionHistoryPage";
import type { Attempt, AttemptSummary, StudentDetail } from "./types";

vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams("classId=c1") }));

const summary = (patch: Partial<AttemptSummary>): AttemptSummary => ({
  attemptId: "a1", attemptNo: 1, status: "SUBMITTED", startedAt: "2026-10-25T12:00:00Z", deadlineAt: "2026-10-26T12:00:00Z",
  lastSavedAt: null, submittedAt: "2026-10-25T12:30:00Z", late: false, submitMode: "MANUAL", receiptHash: "h".repeat(64),
  graded: false, feedbackCount: 0, warnings: [], ...patch,
});

describe("SubmissionHistoryPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("mỗi lượt đã nộp ghi đúng hạn hay trễ và cách nộp", async () => {
    vi.spyOn(workApi, "detail").mockResolvedValue({ title: "Bài", gradingMode: "GRADED" } as StudentDetail);
    vi.spyOn(workApi, "history").mockResolvedValue([
      summary({ attemptId: "a2", attemptNo: 2, late: true, submitMode: "AUTO_DEADLINE", graded: true }),
      summary({ attemptId: "a1", attemptNo: 1 }),
      summary({ attemptId: "a3", attemptNo: 3, status: "IN_PROGRESS", submittedAt: null, submitMode: null }),
    ]);
    vi.spyOn(workApi, "attempt").mockReturnValue(new Promise<Attempt>(() => {}));

    render(<SubmissionHistoryPage assignmentId="x" />);

    expect(await screen.findByText("Trễ · Tự nộp khi hết hạn")).toBeInTheDocument();
    expect(screen.getByText("Đúng hạn · Nộp tay")).toBeInTheDocument();
    expect(screen.queryAllByText(/Đúng hạn|Trễ ·/)).toHaveLength(2);
  });
});
