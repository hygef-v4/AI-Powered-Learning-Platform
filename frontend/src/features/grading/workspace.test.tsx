import { render, screen, fireEvent, waitFor, cleanup } from "@testing-library/react";
import { afterEach, beforeEach, describe, it, expect, vi } from "vitest";
import { GradingWorkspacePage } from "./GradingWorkspacePage";
import { ApiError } from "@/shared/api/client";
import type { Detail } from "./types";
const mocks = vi.hoisted(() => ({
  detail: vi.fn(),
  save: vi.fn(),
  proposals: vi.fn(),
  push: vi.fn(),
}));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: mocks.push }) }));
vi.mock("./api", () => ({ gradingApi: mocks }));
const detail: Detail = {
  evaluation: {
    id: "e",
    classId: "c",
    assignmentId: "a",
    attemptId: "t",
    groupDocumentId: null,
    accountId: "s",
    kind: "ATTEMPT",
    submissionHash: "hash",
    state: "DRAFT",
    score: 0,
    maxScore: 10,
    version: 2,
    rubricChecks: "{}",
    adjustment: 0,
    adjustmentReason: null,
    feedback: "original",
    publishedAt: null,
    changes: [],
  },
  document: { blocks: [] },
  rubrics: [],
  navigationIds: ["e", "next"],
  activity: null,
  activityState: "DISABLED",
  members: [],
  memberActivities: [],
  sectionAuthors: [],
  current: true,
};
beforeEach(() => {
  vi.resetAllMocks();
  mocks.detail.mockResolvedValue(detail);
  mocks.proposals.mockResolvedValue([]);
});
afterEach(cleanup);
describe("workspace save", () => {
  it("sends version and checklist, then moves to the next submission only after saving", async () => {
    mocks.save.mockResolvedValue({ ...detail.evaluation, version: 3 });
    render(<GradingWorkspacePage evaluationId="e" />);
    const feedback = await screen.findByRole("textbox", { name: "Nhận xét" });
    fireEvent.change(feedback, { target: { value: "reviewed" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu nháp / thay đổi" }));
    await waitFor(() => expect(mocks.push).toHaveBeenCalledWith("/teaching/evaluations/next"));
    expect(mocks.save.mock.calls[0]![1]).toMatchObject({
      version: 2,
      checkedItemsByRubric: {},
      feedback: "reviewed",
    });
    expect(mocks.save.mock.calls[0]![1]).not.toHaveProperty("score");
  });
  it("keeps unsaved feedback and stays on the submission after conflict", async () => {
    mocks.save.mockRejectedValue(
      new ApiError({ status: 409, title: "Conflict", code: "GRADE_VERSION_CONFLICT" }),
    );
    render(<GradingWorkspacePage evaluationId="e" />);
    const feedback = await screen.findByRole("textbox", { name: "Nhận xét" });
    fireEvent.change(feedback, { target: { value: "unsaved" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu nháp / thay đổi" }));
    await screen.findByRole("alert");
    expect(feedback).toHaveValue("unsaved");
    expect(mocks.push).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Công bố điểm" })).toBeDisabled();
  });
});


it("shows a live score and named sections while AI is unavailable, without saving any grade", async () => {
  mocks.detail.mockResolvedValue({ ...detail, document: { blocks: [{ id: "part", type: "HEADING", content: { text: "Câu 1: Class Diagram" } }] },
    rubrics: [{ rubricId: "r", partId: "part", maxPoints: 10, criteria: [{ criterionId: "c", title: "Kiến trúc", items: [{ itemId: "i", text: "Đúng quan hệ", points: 2.5 }] }] }] });
  mocks.proposals.mockResolvedValue([{ id: "p", versionNo: 1, origin: "AI_TASK", status: "FAILED", safeError: "AI task failed" }]);
  render(<GradingWorkspacePage evaluationId="e" />);
  fireEvent.click(await screen.findByRole("checkbox", { name: /Đúng quan hệ/ }));
  expect(screen.getByLabelText("Điểm tạm tính")).toHaveTextContent("2.50 / 10.00");
  expect(screen.getByRole("region", { name: "Thao tác chấm bài" })).toHaveClass("sticky");
  expect(screen.getByRole("button", { name: "Lưu nháp / thay đổi" })).toBeEnabled();
  expect(screen.getByRole("button", { name: "Chốt điểm" })).toBeDisabled();
  expect(mocks.save).not.toHaveBeenCalled();
});
