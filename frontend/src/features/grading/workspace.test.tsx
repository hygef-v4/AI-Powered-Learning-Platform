import { render, screen, fireEvent, waitFor, cleanup, act } from "@testing-library/react";
import { afterEach, beforeEach, describe, it, expect, vi } from "vitest";
import { GradingWorkspacePage } from "./GradingWorkspacePage";
import { ApiError } from "@/shared/api/client";
import type { Detail } from "./types";
const mocks = vi.hoisted(() => ({
  detail: vi.fn(),
  save: vi.fn(),
  proposals: vi.fn(),
  request: vi.fn(),
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
  vi.useRealTimers();
  vi.resetAllMocks();
  mocks.detail.mockResolvedValue(detail);
  mocks.proposals.mockResolvedValue([]);
});
afterEach(() => {
  cleanup();
  vi.useRealTimers();
});
/** Đề xuất AI mở bằng nút trên thanh công cụ của tài liệu. */
function openAi() {
  fireEvent.click(screen.getByRole("button", { name: "Chấm bằng AI" }));
}
describe("workspace save", () => {
  it("sends version and checklist, then moves to the next submission only after saving", async () => {
    mocks.save.mockResolvedValue({ ...detail.evaluation, version: 3 });
    render(<GradingWorkspacePage evaluationId="e" />);
    const feedback = await screen.findByRole("textbox", { name: "Nhận xét gửi sinh viên" });
    fireEvent.change(feedback, { target: { value: "reviewed" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu nháp / thay đổi" }));
    await waitFor(() => expect(mocks.push).toHaveBeenCalledWith("/teaching/evaluations/next"));
    expect(mocks.save.mock.calls[0]![1]).toMatchObject({
      version: 2,
      checkedItemsByRubric: {},
      adjustment: "0",
      adjustmentReason: null,
      feedback: "reviewed",
    });
    expect(mocks.save.mock.calls[0]![1]).not.toHaveProperty("score");
  });
  it("keeps unsaved feedback and stays on the submission after conflict", async () => {
    mocks.save.mockRejectedValue(
      new ApiError({ status: 409, title: "Conflict", code: "GRADE_VERSION_CONFLICT" }),
    );
    render(<GradingWorkspacePage evaluationId="e" />);
    const feedback = await screen.findByRole("textbox", { name: "Nhận xét gửi sinh viên" });
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
  expect(screen.getByRole("region", { name: "Rubric: Câu 1: Class Diagram" })).toHaveTextContent("Đạt 2.50");
  expect(screen.queryByRole("spinbutton")).not.toBeInTheDocument();
  expect(screen.queryByText(/Điều chỉnh/)).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Lưu nháp / thay đổi" })).toBeEnabled();
  expect(screen.getByRole("button", { name: "Chốt điểm" })).toBeDisabled();
  expect(mocks.save).not.toHaveBeenCalled();
});


it("shows a saved fractional point as a note and ticking the item saves full points", async () => {
  const partialDetail = { ...detail, evaluation: { ...detail.evaluation, rubricItemScores: { r: { i: 0.5 } } },
    rubrics: [{ rubricId: "r", partId: "q", maxPoints: 1, criteria: [{ criterionId: "c", title: "Thiết kế", items: [{ itemId: "i", text: "Đạt một phần", points: 1 }] }] }] };
  mocks.detail.mockResolvedValue(partialDetail);
  mocks.save.mockResolvedValue({ ...partialDetail.evaluation, version: 3, score: 1 });
  render(<GradingWorkspacePage evaluationId="e" />);
  const item = await screen.findByRole("checkbox", { name: /Đạt một phần/ });
  expect(item).not.toBeChecked();
  expect(screen.getByText(/Điểm lẻ đã lưu: 0.50 \/ 1.00/)).toBeInTheDocument();
  fireEvent.click(item);
  expect(item).toBeChecked();
  fireEvent.click(screen.getByRole("button", { name: "Lưu nháp / thay đổi" }));
  await waitFor(() => expect(mocks.save).toHaveBeenCalled());
  expect(mocks.save.mock.calls[0]![1]).toMatchObject({ version: 2, checkedItemsByRubric: { r: ["i"] }, itemScoresByRubric: { r: { i: 1 } } });
  expect(mocks.save.mock.calls[0]![1]).not.toHaveProperty("score");
});

it("unticking an item gives it zero and a backend without item scores only gets the checklist", async () => {
  mocks.detail.mockResolvedValue({ ...detail, evaluation: { ...detail.evaluation, rubricChecks: JSON.stringify({ r: ["i"] }) },
    rubrics: [{ rubricId: "r", partId: "q", maxPoints: 1, criteria: [{ criterionId: "c", title: "Thiết kế", items: [{ itemId: "i", text: "Đạt một phần", points: 1 }] }] }] });
  mocks.save.mockResolvedValue({ ...detail.evaluation, version: 3 });
  render(<GradingWorkspacePage evaluationId="e" />);
  const item = await screen.findByRole("checkbox", { name: /Đạt một phần/ });
  expect(item).toBeChecked();
  fireEvent.click(item);
  expect(screen.getByLabelText("Điểm tạm tính")).toHaveTextContent("0.00 / 10.00");
  fireEvent.click(screen.getByRole("button", { name: "Lưu nháp / thay đổi" }));
  await waitFor(() => expect(mocks.save).toHaveBeenCalled());
  expect(mocks.save.mock.calls[0]![1]).toMatchObject({ checkedItemsByRubric: { r: [] } });
  expect(mocks.save.mock.calls[0]![1]).not.toHaveProperty("itemScoresByRubric");
});

it("recovers the actual task error when an older backend only returns AI task failed", async () => {
  mocks.proposals.mockResolvedValue([{ id: "p", taskId: "task", versionNo: 2, status: "FAILED", origin: "AI_TASK", safeError: "AI task failed" }]);
  mocks.task.mockResolvedValue({ state: "FAILED", safeError: "Yêu cầu chấm không hợp lệ (HTTP 400)" });
  render(<GradingWorkspacePage evaluationId="e" />);
  await screen.findByRole("button", { name: "Chấm bằng AI" });
  openAi();
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

  it("compares the current checklist with the AI proposal and applies only to the local draft", async () => {
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
    expect(await screen.findByText("Có đề xuất AI")).toBeInTheDocument();
    openAi();
    const dialog = screen.getByRole("dialog", { name: "Chấm bằng AI" });
    expect(dialog).toHaveTextContent(/Điểm đang chấm: 2\.50/);
    expect(dialog).toHaveTextContent(/AI đề xuất: 3\.00/);
    expect(dialog).not.toHaveTextContent(/Điều chỉnh/);
    expect(dialog).toHaveTextContent(/Lệch \+0\.50/);

    fireEvent.click(screen.getByRole("button", { name: "Áp dụng vào bản nháp" }));
    expect(mocks.save).not.toHaveBeenCalled();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: /Thực thể chính/ })).toBeChecked();
    expect(screen.getByRole("textbox", { name: "Nhận xét gửi sinh viên" })).toHaveValue("Nhận xét chung tiếng Việt");
  });

  it("shows the AI comparison without NaN when a fractional point was saved before", async () => {
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

    render(<GradingWorkspacePage evaluationId="e" />);
    await screen.findByRole("checkbox", { name: /Thực thể chính/ });
    openAi();
    expect(screen.getByRole("dialog", { name: "Chấm bằng AI" })).toHaveTextContent(/Điểm đang chấm: 2\.50/);
    expect(document.body.textContent).not.toContain("NaN");
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

describe("AI proposal polling resilience and progress states", () => {
  const rubricFixture = [
    {
      rubricId: "r1",
      partId: "part-1",
      maxPoints: 1,
      criteria: [
        {
          criterionId: "c1",
          title: "Kiến trúc",
          items: [{ itemId: "i1", text: "Thực thể chính", points: 1 }],
        },
      ],
    },
  ];

  it("transitions QUEUED -> RUNNING -> READY task with PENDING proposal -> READY proposal while preserving a manual tick", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    mocks.detail.mockResolvedValue({
      ...detail,
      evaluation: { ...detail.evaluation, maxScore: 1, rubricItemScores: {} },
      rubrics: rubricFixture,
    });
    mocks.proposals
      .mockResolvedValueOnce([
        { id: "p1", versionNo: 1, status: "PENDING", taskState: "QUEUED", origin: "AI_TASK", source: "PERSONAL" },
      ])
      .mockResolvedValueOnce([
        { id: "p1", versionNo: 1, status: "PENDING", taskState: "RUNNING", origin: "AI_TASK", source: "PERSONAL" },
      ])
      .mockResolvedValueOnce([
        { id: "p1", versionNo: 1, status: "PENDING", taskState: "READY", origin: "AI_TASK", source: "PERSONAL" },
      ])
      .mockResolvedValue([
        {
          id: "p1",
          versionNo: 1,
          status: "READY",
          origin: "AI_TASK",
          source: "PERSONAL",
          proposal: {
            checks: { r1: ["i1"] },
            itemScoresByRubric: { r1: { i1: 1 } },
            score: 1,
            feedback: "Đề xuất hoàn tất",
          },
        },
      ]);

    render(<GradingWorkspacePage evaluationId="e" />);
    await act(async () => {});
    const scoreInput = screen.getByRole("checkbox", { name: /Thực thể chính/ });
    fireEvent.click(scoreInput);
    expect(scoreInput).toBeChecked();
    openAi();

    expect(screen.getByText("Đang chờ xử lý")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Áp dụng vào bản nháp" })).toBeDisabled();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.getByText("AI đang chấm bài")).toBeInTheDocument();
    expect(scoreInput).toBeChecked();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.getByText("Đã nhận kết quả AI, đang xử lý phản hồi")).toBeInTheDocument();
    expect(scoreInput).toBeChecked();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.getByText("Sẵn sàng để xem")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Áp dụng vào bản nháp" })).toBeEnabled();
    expect(scoreInput).toBeChecked();
    expect(mocks.save).not.toHaveBeenCalled();
  });

  it("recovers after a temporary network error without spamming duplicate error alerts", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    mocks.proposals
      .mockResolvedValueOnce([
        { id: "p1", versionNo: 1, status: "PENDING", taskState: "RUNNING", origin: "AI_TASK" },
      ])
      .mockRejectedValueOnce(new Error("Temporary network error"))
      .mockResolvedValue([
        {
          id: "p1",
          versionNo: 1,
          status: "READY",
          origin: "AI_TASK",
          proposal: { checks: {}, score: 8, feedback: "Đã hồi phục" },
        },
      ]);

    render(<GradingWorkspacePage evaluationId="e" />);
    await act(async () => {});
    openAi();
    expect(screen.getByText("AI đang chấm bài")).toBeInTheDocument();

    // First poll fails with transient error -> no error alert spam, schedules retry.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();

    // Backoff poll succeeds -> proposal becomes READY.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.getByText("Sẵn sàng để xem")).toBeInTheDocument();
    expect(screen.getByText("Đã hồi phục")).toBeInTheDocument();
  });

  it("stops polling on 403 error and surfaces the error once", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    mocks.proposals
      .mockResolvedValueOnce([
        { id: "p1", versionNo: 1, status: "PENDING", taskState: "RUNNING", origin: "AI_TASK" },
      ])
      .mockRejectedValueOnce(new ApiError({ status: 403, title: "Forbidden", code: "FORBIDDEN" }));

    render(<GradingWorkspacePage evaluationId="e" />);
    await act(async () => {});
    openAi();
    expect(screen.getByText("AI đang chấm bài")).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.getByRole("alert")).toBeInTheDocument();
    const callsAfter403 = mocks.proposals.mock.calls.length;

    await act(async () => {
      await vi.advanceTimersByTimeAsync(15000);
    });
    expect(mocks.proposals).toHaveBeenCalledTimes(callsAfter403);
  });

  it("surfaces delayed status and Kiểm tra lại button after repeated poll failures, resuming existing request without a new AI job", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    mocks.proposals
      .mockResolvedValueOnce([
        { id: "p1", versionNo: 1, status: "PENDING", taskState: "RUNNING", origin: "AI_TASK" },
      ])
      .mockRejectedValueOnce(new Error("Poll timeout 1"))
      .mockRejectedValueOnce(new Error("Poll timeout 2"))
      .mockRejectedValueOnce(new Error("Poll timeout 3"))
      .mockRejectedValueOnce(new Error("Poll timeout 4"))
      .mockResolvedValue([
        {
          id: "p1",
          versionNo: 1,
          status: "READY",
          origin: "AI_TASK",
          proposal: { checks: {}, score: 9, feedback: "Hoàn tất sau khi kiểm tra lại" },
        },
      ]);

    render(<GradingWorkspacePage evaluationId="e" />);
    await act(async () => {});
    openAi();
    expect(screen.getByText("AI đang chấm bài")).toBeInTheDocument();

    for (const waitMs of [3000, 3000, 6000, 12000]) {
      await act(async () => {
        await vi.advanceTimersByTimeAsync(waitMs);
      });
    }

    expect(screen.getByText(/Phản hồi AI đang mất nhiều thời gian hơn dự kiến/)).toBeInTheDocument();
    const recheckBtn = screen.getByRole("button", { name: "Kiểm tra lại" });

    await act(async () => {
      fireEvent.click(recheckBtn);
    });
    expect(screen.getByText("Sẵn sàng để xem")).toBeInTheDocument();
    expect(screen.getByText("Hoàn tất sau khi kiểm tra lại")).toBeInTheDocument();
    expect(mocks.request).not.toHaveBeenCalled();
  });

  it("cleans up polling timers on unmount", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    mocks.proposals.mockResolvedValue([
      { id: "p1", versionNo: 1, status: "PENDING", taskState: "QUEUED", origin: "AI_TASK" },
    ]);

    const { unmount } = render(<GradingWorkspacePage evaluationId="e" />);
    await act(async () => {});
    openAi();
    expect(screen.getByText("Đang chờ xử lý")).toBeInTheDocument();
    expect(mocks.proposals).toHaveBeenCalledTimes(1);

    unmount();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(15000);
    });
    expect(mocks.proposals).toHaveBeenCalledTimes(1);
  });
});
