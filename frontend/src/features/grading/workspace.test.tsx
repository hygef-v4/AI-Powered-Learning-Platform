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
  task: vi.fn(),
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
  fireEvent.click(await screen.findByRole("button", { name: "Đủ điểm: Đúng quan hệ" }));
  expect(screen.getByLabelText("Điểm tạm tính")).toHaveTextContent("2.50 / 10.00");
  expect(screen.getByRole("region", { name: "Thao tác chấm bài" })).toHaveClass("sticky");
  expect(screen.getByRole("button", { name: "Lưu nháp / thay đổi" })).toBeEnabled();
  expect(screen.getByRole("button", { name: "Chốt điểm" })).toBeDisabled();
  expect(mocks.save).not.toHaveBeenCalled();
});


it("loads and saves fractional item points as an additive DTO, never a client total", async () => {
  const partialDetail = { ...detail, evaluation: { ...detail.evaluation, rubricItemScores: { r: { i: 0.5 } } },
    rubrics: [{ rubricId: "r", partId: "q", maxPoints: 1, criteria: [{ criterionId: "c", title: "Thiết kế", items: [{ itemId: "i", text: "Đạt một phần", points: 1 }] }] }] };
  mocks.detail.mockResolvedValue(partialDetail);
  mocks.save.mockResolvedValue({ ...partialDetail.evaluation, version: 3, score: 0.75 });
  render(<GradingWorkspacePage evaluationId="e" />);
  const field = await screen.findByRole("spinbutton", { name: "Điểm: Đạt một phần" });
  expect(field).toHaveValue(0.5);
  fireEvent.change(field, { target: { value: "0.75" } });
  fireEvent.click(screen.getByRole("button", { name: "Lưu nháp / thay đổi" }));
  await waitFor(() => expect(mocks.save).toHaveBeenCalled());
  expect(mocks.save.mock.calls[0]![1]).toMatchObject({ version: 2, itemScoresByRubric: { r: { i: "0.75" } } });
  expect(mocks.save.mock.calls[0]![1]).not.toHaveProperty("score");
});

it("keeps partial edits when the running backend lacks item-score support", async () => {
  mocks.detail.mockResolvedValue({ ...detail,
    rubrics: [{ rubricId: "r", partId: "q", maxPoints: 1, criteria: [{ criterionId: "c", title: "Thiết kế", items: [{ itemId: "i", text: "Đạt một phần", points: 1 }] }] }] });
  render(<GradingWorkspacePage evaluationId="e" />);
  const field = await screen.findByRole("spinbutton", { name: "Điểm: Đạt một phần" });
  fireEvent.change(field, { target: { value: "0.5" } });
  fireEvent.click(screen.getByRole("button", { name: "Lưu nháp / thay đổi" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Máy chủ chưa hỗ trợ lưu điểm lẻ");
  expect(field).toHaveValue(0.5);
  expect(mocks.save).not.toHaveBeenCalled();
});

it("recovers the actual task error when an older backend only returns AI task failed", async () => {
  mocks.proposals.mockResolvedValue([{ id: "p", taskId: "task", versionNo: 2, status: "FAILED", origin: "AI_TASK", safeError: "AI task failed" }]);
  mocks.task.mockResolvedValue({ state: "FAILED", safeError: "Yêu cầu chấm không hợp lệ (HTTP 400)" });
  render(<GradingWorkspacePage evaluationId="e" />);
  expect(await screen.findByText("Yêu cầu chấm không hợp lệ (HTTP 400)")).toBeInTheDocument();
  expect(mocks.task).toHaveBeenCalledWith("task");
  expect(screen.getByRole("button", { name: "Lưu nháp / thay đổi" })).toBeEnabled();
});

describe("AI proposal comparison and draft application", () => {
  const sampleRubrics = [
    {
      rubricId: "r1",
      partId: "part-1",
      maxPoints: 3,
      criteria: [
        {
          criterionId: "c1",
          title: "Kiến trúc",
          items: [
            { itemId: "i1", text: "Thực thể chính", points: 1 },
            { itemId: "i2", text: "Quan hệ lớp", points: 2 },
          ],
        },
      ],
    },
  ];

  it("compares current fractional and legacy scores against AI proposal, separates teacher adjustment, and applies only to local draft", async () => {
    mocks.detail.mockResolvedValue({
      ...detail,
      evaluation: {
        ...detail.evaluation,
        maxScore: 3,
        rubricChecks: JSON.stringify({ r1: ["i2"] }),
        rubricItemScores: { r1: { i1: 0.5, i2: 2 } },
        adjustment: -0.5,
        adjustmentReason: "Nộp muộn",
      },
      document: { blocks: [{ id: "part-1", type: "HEADING", content: { text: "Câu 1: Class Diagram" } }] },
      rubrics: sampleRubrics,
    });
    mocks.proposals.mockResolvedValue([
      {
        id: "prop-latest",
        versionNo: 2,
        status: "READY",
        origin: "AI_TASK",
        source: "PERSONAL",
        provider: "GEMINI",
        model: "gemini-3.5-flash-lite",
        proposal: {
          checks: { r1: ["i1", "i2"] },
          itemScoresByRubric: { r1: { i1: 1, i2: 2 } },
          score: 3,
          feedback: "Nhận xét chung tiếng Việt",
          sections: [{ partId: "part-1", feedback: "Tốt phần 1" }],
        },
      },
      {
        id: "prop-old-ready",
        versionNo: 1,
        status: "READY",
        origin: "AI_TASK",
        source: "PERSONAL",
        proposal: {
          checks: { r1: ["i2"] },
          score: 2,
          feedback: "Bản cũ",
        },
      },
    ]);

    render(<GradingWorkspacePage evaluationId="e" />);
    expect(await screen.findByText(/Điểm đang nhập: 2\.50/)).toBeInTheDocument();
    expect(screen.getByText(/AI đề xuất: 3\.00/)).toBeInTheDocument();
    expect(screen.getByText(/Điều chỉnh giảng viên: -0\.50/)).toBeInTheDocument();
    expect(screen.getByText(/Lệch \+0\.50/)).toBeInTheDocument();

    const applyBtn = screen.getByRole("button", { name: "Áp dụng vào bản nháp" });
    fireEvent.click(applyBtn);
    expect(mocks.save).not.toHaveBeenCalled();
    expect(screen.getByRole("spinbutton", { name: "Điểm: Thực thể chính" })).toHaveValue(1);
    expect(screen.getByRole("textbox", { name: "Nhận xét" })).toHaveValue("Nhận xét chung tiếng Việt");
  });

  it("shows explicit invalid comparison without NaN when current input is invalid", async () => {
    mocks.detail.mockResolvedValue({
      ...detail,
      evaluation: { ...detail.evaluation, maxScore: 3, rubricItemScores: { r1: { i1: 0.5, i2: 2 } } },
      rubrics: sampleRubrics,
    });
    mocks.proposals.mockResolvedValue([
      {
        id: "prop-latest",
        versionNo: 1,
        status: "READY",
        origin: "AI_TASK",
        proposal: {
          checks: { r1: ["i1", "i2"] },
          score: 3,
          feedback: "OK",
        },
      },
    ]);

    const { container } = render(<GradingWorkspacePage evaluationId="e" />);
    const input = await screen.findByRole("spinbutton", { name: "Điểm: Thực thể chính" });
    fireEvent.change(input, { target: { value: "9" } });
    expect(screen.getByText(/Điểm đang nhập chưa hợp lệ/)).toBeInTheDocument();
    expect(container.textContent).not.toContain("NaN");
  });

  it("collapses empty activity and history into compact disclosures while keeping populated history open", async () => {
    const { unmount } = render(<GradingWorkspacePage evaluationId="e" />);
    const activityHeading = await screen.findByRole("heading", { name: "Hoạt động khi làm bài" });
    const historyHeading = screen.getByRole("heading", { name: "Lịch sử sửa điểm" });
    expect(activityHeading.closest("details")).not.toHaveAttribute("open");
    expect(historyHeading.closest("details")).not.toHaveAttribute("open");
    expect(screen.getByText(/nền tảng không kết luận gian lận/)).toBeInTheDocument();
    unmount();

    mocks.detail.mockResolvedValue({
      ...detail,
      evaluation: {
        ...detail.evaluation,
        changes: [{ actorId: "t", at: "2026-10-25T10:00:00Z", before: 8, after: 9, reason: "Cộng điểm sơ đồ" }],
      },
    });
    render(<GradingWorkspacePage evaluationId="e" />);
    const populatedHistoryHeading = await screen.findByRole("heading", { name: "Lịch sử sửa điểm" });
    expect(populatedHistoryHeading.closest("details")).toHaveAttribute("open");
    expect(screen.getByText(/Cộng điểm sơ đồ/)).toBeInTheDocument();
  });
});
