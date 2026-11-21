import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { workApi } from "@/features/assignmentwork/api";
import { StudentAssignmentDetailPage } from "@/features/assignmentwork/pages/StudentAssignmentDetailPage";
import { attemptResultSources } from "@/features/assignmentwork/slots";
import type { Attempt, AttemptSummary, StudentDetail } from "@/features/assignmentwork/model/types";

const mocks = vi.hoisted(() => ({ push: vi.fn() }));

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams("classId=c1&from=all"),
  useRouter: () => ({ push: mocks.push }),
}));
vi.mock("@/features/assignmentwork/hooks/useStudentAssignments", () => ({
  useStudentAssignments: () => ({ rows: [] }), resolveRowStatus: vi.fn(),
}));

const submitted = { attemptId: "submitted", attemptNo: 2, status: "SUBMITTED", graded: true } as AttemptSummary;
const draft = { attemptId: "draft", attemptNo: 3, status: "IN_PROGRESS", graded: false } as AttemptSummary;
const detail = {
  title: "Đặt sân", scope: "CLASS", gradingMode: "GRADED", submissionMode: "INDIVIDUAL", latestAttempt: draft,
  inProgressAttemptId: "draft", closesAt: "2026-11-14T00:00:00Z", serverTime: "2026-11-10T00:00:00Z",
  referenceMaterialIds: [], partCount: 3,
} as unknown as StudentDetail;

describe("student published result entry", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    mocks.push.mockClear();
  });

  it("resumes a subject assignment draft through start before opening the editor", async () => {
    vi.spyOn(workApi, "detail").mockResolvedValue({ ...detail, scope: "SUBJECT" });
    vi.spyOn(workApi, "history").mockResolvedValue([draft]);
    const start = vi.spyOn(workApi, "start").mockResolvedValue({ attemptId: "draft" } as Attempt);

    render(<StudentAssignmentDetailPage assignmentId="assignment" />);
    fireEvent.click(await screen.findByRole("button", { name: "Tiếp tục làm bài" }));

    await waitFor(() => expect(mocks.push).toHaveBeenCalledWith("/attempts/draft"));
    expect(start).toHaveBeenCalledWith("assignment", "c1");
  });

  it.each([true, false])("keeps the submitted result beside a newer draft, published=%s", async (published) => {
    vi.spyOn(workApi, "detail").mockResolvedValue(detail);
    vi.spyOn(workApi, "history").mockResolvedValue([draft, submitted]);
    vi.spyOn(workApi, "attempt").mockResolvedValue({ parts: [] } as unknown as Attempt);
    const load = vi.fn().mockResolvedValue({ score: published ? 8 : null, maxScore: 10, items: [] });
    vi.spyOn(attemptResultSources, "entries").mockReturnValue([{ key: "test", load }]);

    render(<StudentAssignmentDetailPage assignmentId="assignment" />);

    expect(await screen.findByText(published ? "Tổng điểm" : "Chưa có điểm công bố.")).toBeInTheDocument();
    expect(load).toHaveBeenCalledWith("submitted");
    expect(load).not.toHaveBeenCalledWith("draft");
    expect(screen.getByRole("link", { name: "Tiếp tục làm bài" })).toHaveAttribute("href", "/attempts/draft");
    expect(screen.queryByRole("link", { name: "Xem lịch sử nộp bài" })).not.toBeInTheDocument();
    if (published) {
      expect(screen.getByRole("link", { name: "Xem chi tiết chấm điểm" })).toHaveAttribute(
        "href", "/my-assignments/assignment/history?classId=c1&from=all&attempt=submitted",
      );
    } else {
      expect(screen.queryByRole("link", { name: "Xem chi tiết chấm điểm" })).not.toBeInTheDocument();
    }
  });
});
