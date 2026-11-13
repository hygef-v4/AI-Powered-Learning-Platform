import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useState } from "react";
import { GradingWorkspacePage } from "@/features/grading/pages/GradingWorkspacePage";
import { GradingFeedbackSummary, RubricItemFeedback } from "@/features/grading/components/StructuredFeedbackSection";
import type { Detail, Rubric, StructuredFeedback } from "@/features/grading/model/types";

const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  detail: vi.fn(),
  proposals: vi.fn(),
  save: vi.fn(),
  finalize: vi.fn(),
  attempt: vi.fn(),
  assignmentContext: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mocks.push }),
}));

vi.mock("@/features/grading/api", () => ({
  gradingApi: {
    loadEvaluation: mocks.detail,
    listProposals: mocks.proposals,
    saveEvaluation: mocks.save,
    finalizeGrades: mocks.finalize,
    loadAttemptGrade: mocks.attempt,
    loadAssignmentContext: mocks.assignmentContext,
  },
}));

const rubrics: Rubric[] = [
  {
    rubricId: "r1",
    partId: "part-1",
    maxPoints: 2,
    criteria: [
      {
        criterionId: "c1",
        title: "Mô hình hóa",
        items: [
          { itemId: "i1", text: "Xác định thực thể", points: 1 },
          { itemId: "i2", text: "Bội số quan hệ", points: 1 },
        ],
      },
    ],
  },
];

const sampleStructuredFeedback: StructuredFeedback = {
  schemaVersion: 1,
  summary: {
    strengths: ["Xác định đầy đủ các thực thể cốt lõi"],
    improvements: ["Thiếu bội số ở liên kết LopHoc - HocSinh"],
    nextSteps: ["Bổ sung ký hiệu 1..* trên sơ đồ lớp"],
  },
  items: [
    {
      rubricId: "r1",
      itemId: "i1",
      evidence: "Đã nêu đủ thực thể LopHoc và HocSinh kèm khóa chính",
      reason: "Đạt tối đa 1.00/1.00 điểm theo tiêu chí",
      suggestion: "Gợi ý thêm (không trừ điểm): thống nhất đặt tên thuộc tính theo camelCase",
      evidenceBlockId: "part-1",
      requiresReview: false,
      reviewReason: null,
    },
    {
      rubricId: "r1",
      itemId: "i2",
      evidence: null,
      reason: "Đạt 0.50/1.00 điểm do một phần sơ đồ nhúng ảnh mờ chưa đọc rõ bội số",
      suggestion: "Ghi rõ 1..* ở đầu HocSinh",
      evidenceBlockId: "missing-block-id",
      requiresReview: true,
      reviewReason: "Ảnh sơ đồ mờ chưa trích xuất hết ký hiệu bội số",
    },
  ],
};

it("expands all feedback fields to fit long text and shrinks them after editing", () => {
  let contentHeight = 220;
  const scrollHeight = vi.spyOn(HTMLTextAreaElement.prototype, "scrollHeight", "get").mockImplementation(() => contentHeight);
  const renderFeedback = (reason: string) => <RubricItemFeedback rubricId="r1" itemId="i1" itemText="Tiêu chí"
    awardedPoints={0} maxPoints={1} editable onChangeItem={vi.fn()}
    feedbackItem={{ ...sampleStructuredFeedback.items[0]!, reason }} />;
  try {
    const { rerender } = render(renderFeedback("Nhận xét dài ".repeat(30)));
    for (const field of screen.getAllByRole("textbox")) {
      expect(field).toHaveStyle({ height: "220px" });
      expect(field).toHaveClass("overflow-hidden", "resize-none");
    }
    contentHeight = 90;
    rerender(renderFeedback("Nhận xét ngắn"));
    expect(screen.getByRole("textbox", { name: "Giải thích điểm: Tiêu chí" })).toHaveStyle({ height: "90px" });
  } finally {
    scrollHeight.mockRestore();
  }
});

const baseDetail: Detail = {
  evaluation: {
    id: "eval-1",
    assignmentId: "a1",
    classId: "c1",
    attemptId: "att-1",
    groupDocumentId: null,
    accountId: "stu-1",
    kind: "ATTEMPT",
    submissionHash: "hash-1",
    state: "DRAFT",
    score: 1.5,
    maxScore: 2,
    version: 1,
    rubricChecks: "{}",
    rubricItemScores: { r1: { i1: 1, i2: 0.5 } },
    adjustment: -0.25,
    adjustmentReason: "Nộp trễ",
    feedback: "Nhận xét nháp ban đầu",
    publishedAt: null,
    changes: [],
  },
  document: {
    blocks: [
      { id: "part-1", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Câu 1: Thiết kế lớp" } },
      { id: "block-text-1", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{text: "Nội dung bài làm của học sinh"}] } },
    ],
  },
  rubrics,
  navigationIds: ["eval-1"],
  activityState: "DISABLED",
  activity: null,
  members: [],
  memberActivities: [],
  sectionAuthors: [],
  current: true,
};

beforeEach(() => {
  vi.clearAllMocks();
  mocks.assignmentContext.mockResolvedValue({
    id: "a1",
    scope: "CLASS",
    classId: "c1",
    title: "Bài tập phân tích thiết kế",
    ownerName: "GV Nguyễn Văn A",
  });
});

afterEach(() => {
  cleanup();
});

describe("Feedback summary without duplicated improvement guidance", () => {
  it("shows strengths and issues but hides historical next steps", () => {
    render(<GradingFeedbackSummary summary={sampleStructuredFeedback.summary} />);
    expect(screen.getByText("✓ Làm tốt")).toBeInTheDocument();
    expect(screen.getByText("! Cần sửa")).toBeInTheDocument();
    expect(screen.queryByText(/Gợi ý tiếp theo/)).not.toBeInTheDocument();
    expect(screen.queryByText(sampleStructuredFeedback.summary.nextSteps[0]!)).not.toBeInTheDocument();
  });

  it("edits the two summary fields while preserving historical next-step data", () => {
    const onChange = vi.fn();
    render(<GradingFeedbackSummary summary={sampleStructuredFeedback.summary} editable onChange={onChange} />);
    expect(screen.queryByLabelText("Gợi ý tiếp theo")).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Điểm cần cải thiện, ý 1"), { target: { value: "Thiếu multiplicity" } });
    expect(onChange).toHaveBeenCalledWith({
      ...sampleStructuredFeedback.summary,
      improvements: ["Thiếu multiplicity"],
    });
  });

  it("does not render an empty summary for historical next-step-only data", () => {
    const { container } = render(<GradingFeedbackSummary summary={{
      strengths: [], improvements: [], nextSteps: ["Historical advice"],
    }} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("adds separate summary ideas and preserves line breaks while editing", () => {
    function Summary() {
      const [summary, setSummary] = useState(sampleStructuredFeedback.summary);
      return <GradingFeedbackSummary summary={summary} editable onChange={setSummary} />;
    }
    render(<Summary />);
    fireEvent.click(screen.getByRole("button", { name: "Thêm ý: Điểm cần cải thiện" }));
    const second = screen.getByRole("textbox", { name: "Điểm cần cải thiện, ý 2" });
    fireEvent.change(second, { target: { value: "Dòng đầu.\n\nDòng tiếp theo.\n" } });
    expect(second).toHaveValue("Dòng đầu.\n\nDòng tiếp theo.\n");
    expect(second).toHaveAttribute("maxlength", "500");
    fireEvent.click(screen.getByRole("button", { name: "Xóa ý 1: Điểm cần cải thiện" }));
    expect(screen.getByRole("textbox", { name: "Điểm cần cải thiện, ý 1" })).toHaveValue("Dòng đầu.\n\nDòng tiếp theo.\n");
  });

  it("limits summary lists to five ideas and disables edits in read-only state", () => {
    render(<GradingFeedbackSummary summary={{ ...sampleStructuredFeedback.summary, improvements: ["a", "b", "c", "d", "e"] }}
      editable disabled onChange={vi.fn()} />);
    expect(screen.getByRole("button", { name: "Thêm ý: Điểm cần cải thiện" })).toBeDisabled();
    expect(screen.getByRole("textbox", { name: "Điểm cần cải thiện, ý 1" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Xóa ý 1: Điểm cần cải thiện" })).toBeDisabled();
  });
});

describe("Structured Grading Feedback — Teacher Workspace & Student View", () => {
  it("adds optional manual summary and per-item feedback without requiring AI or creating feedback on open", async () => {
    mocks.detail.mockResolvedValue(baseDetail);
    mocks.proposals.mockResolvedValue([]);
    mocks.save.mockResolvedValue({ ...baseDetail.evaluation, version: 2 });
    render(<GradingWorkspacePage evaluationId="eval-1" />);
    const summary = await screen.findByText("Điểm mạnh và điểm cần cải thiện");
    expect(summary.closest("details")).not.toHaveAttribute("open");
    fireEvent.click(summary);
    const firstItem = within(screen.getByTestId("rubric-item-i1"));
    fireEvent.click(firstItem.getByText("Nhận xét & minh chứng"));
    expect(mocks.save).not.toHaveBeenCalled();
    expect(firstItem.getByRole("textbox", { name: "Giải thích điểm: Xác định thực thể" })).toHaveValue("");
    expect(firstItem.getByRole("link", { name: "Đi tới đoạn bài làm" })).toHaveAttribute("href", "#submission-block-part-1");
    fireEvent.change(screen.getByRole("textbox", { name: "Điểm mạnh, ý 1" }), { target: { value: "Nắm được các lớp chính" } });
    fireEvent.change(firstItem.getByRole("textbox", { name: "Minh chứng: Xác định thực thể" }), { target: { value: "Có lớp LopHoc" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
    await waitFor(() => expect(mocks.save).toHaveBeenCalled());
    const saved = mocks.save.mock.calls[0]![1].structuredFeedback;
    expect(saved.summary).toEqual({ strengths: ["Nắm được các lớp chính"], improvements: [], nextSteps: [] });
    expect(saved.items).toHaveLength(1);
    expect(saved.items[0]).toMatchObject({ rubricId: "r1", itemId: "i1", evidence: "Có lớp LopHoc", reason: "", requiresReview: false });
    expect(saved.items.some((item: { itemId: string }) => item.itemId === "i2")).toBe(false);
    expect(screen.queryByRole("button", { name: "Xóa nhận xét cấu trúc" })).not.toBeInTheDocument();
  });

  it("saves only nonblank summary ideas while retaining multiline text and historical next steps", async () => {
    mocks.detail.mockResolvedValue({ ...baseDetail, evaluation: { ...baseDetail.evaluation, structuredFeedback: sampleStructuredFeedback } });
    mocks.proposals.mockResolvedValue([]);
    mocks.save.mockResolvedValue({ ...baseDetail.evaluation, version: 2 });
    render(<GradingWorkspacePage evaluationId="eval-1" />);
    fireEvent.click(await screen.findByRole("button", { name: "Thêm ý: Điểm cần cải thiện" }));
    fireEvent.change(screen.getByLabelText("Điểm cần cải thiện, ý 1"), { target: { value: "Ý chính.\nChi tiết minh chứng." } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
    await waitFor(() => expect(mocks.save).toHaveBeenCalled());
    expect(mocks.save.mock.calls[0]![1].structuredFeedback).toEqual({
      ...sampleStructuredFeedback,
      summary: { ...sampleStructuredFeedback.summary, improvements: ["Ý chính.\nChi tiết minh chứng."] },
    });
  });
  it("renders AI structured feedback preview with block anchors, plain-text fallback for missing blocks, optional full-mark advice, and review flags", async () => {
    mocks.detail.mockResolvedValue(baseDetail);
    mocks.proposals.mockResolvedValue([
      {
        id: "prop-structured",
        versionNo: 1,
        status: "READY",
        origin: "AI_TASK",
        source: "PERSONAL",
        provider: "GEMINI",
        model: "gemini-2.5-flash",
        proposal: {
          checks: { r1: ["i1"] },
          itemScoresByRubric: { r1: { i1: 1, i2: 0.5 } },
          score: 1.5,
          feedback: "Nhận xét văn bản tương thích cũ",
          structuredFeedback: sampleStructuredFeedback,
        },
      },
    ]);

    render(<GradingWorkspacePage evaluationId="eval-1" />);
    fireEvent.click(await screen.findByRole("button", { name: "Xem đề xuất AI" }));

    expect(await screen.findByText("Tóm tắt nhận xét cấu trúc từ AI")).toBeInTheDocument();
    expect(screen.getByText("Xác định đầy đủ các thực thể cốt lõi")).toBeInTheDocument();
    expect(screen.getByText("Thiếu bội số ở liên kết LopHoc - HocSinh")).toBeInTheDocument();
    expect(screen.queryByText("Bổ sung ký hiệu 1..* trên sơ đồ lớp")).not.toBeInTheDocument();
    expect(screen.getByText("Ghi rõ 1..* ở đầu HocSinh")).toBeInTheDocument();
    expect(screen.getByText("Cách cải thiện:")).toBeInTheDocument();

    // Prominent review flag count banner
    expect(screen.getByText(/Có 1 mục AI đánh dấu cần giảng viên kiểm tra thủ công/)).toBeInTheDocument();
    expect(screen.getByText(/Ảnh sơ đồ mờ chưa trích xuất hết ký hiệu bội số/)).toBeInTheDocument();

    // Known block anchor is rendered as a navigable link; unknown block reference is plain text
    const validAnchor = within(screen.getByRole("region", { name: "Chấm bằng AI" }))
      .getByRole("link", { name: "Đi tới đoạn bài làm" });
    expect(validAnchor).toHaveAttribute("href", "#submission-block-part-1");
    expect(document.querySelector('[data-block-id="part-1"]')).not.toBeNull();
    expect(
      screen.getByText("Đoạn tham chiếu không còn trong bài nộp."),
    ).toBeInTheDocument();
    expect(screen.queryByText(/missing-block-id/)).not.toBeInTheDocument();

    // Full marks item shows "Gợi ý thêm (không trừ điểm)" label
    expect(screen.getByText(/Gợi ý thêm \(không trừ điểm\):/)).toBeInTheDocument();
  });

  it("allows changing rubric score without requiring explanation or review blocker", async () => {
    mocks.detail.mockResolvedValue({ ...baseDetail, evaluation: { ...baseDetail.evaluation,
      rubricItemScores: { r1: { i1: 1, i2: 0.5 } }, structuredFeedback: sampleStructuredFeedback,
      reviewedItemScores: { r1: { i1: 1, i2: 0.5 } }, acknowledgedReviewItems: { r1: ["i2"] },
    } });
    mocks.proposals.mockResolvedValue([]);
    render(<GradingWorkspacePage evaluationId="e" />);
    const item = await screen.findByRole("checkbox", { name: "Xác định thực thể" });
    expect(screen.getByRole("checkbox", { name: /Đã kiểm tra và xác nhận giải thích/ })).toBeInTheDocument();
    fireEvent.click(item);
    expect(screen.getByRole("checkbox", { name: /Đã kiểm tra và xác nhận giải thích/ })).toBeInTheDocument();
  });
  it("applies AI partial scores and structured feedback without review blocker", async () => {
    mocks.detail.mockResolvedValue(baseDetail);
    mocks.proposals.mockResolvedValue([
      {
        id: "prop-structured",
        versionNo: 1,
        status: "READY",
        origin: "AI_TASK",
        source: "PERSONAL",
        proposal: {
          checks: { r1: ["i1"] },
          itemScoresByRubric: { r1: { i1: 1, i2: 0.5 } },
          score: 1.5,
          feedback: "Nhận xét chung từ AI",
          structuredFeedback: sampleStructuredFeedback,
        },
      },
    ]);

    render(<GradingWorkspacePage evaluationId="e" />);
    fireEvent.click(await screen.findByRole("button", { name: "Xem đề xuất AI" }));
    fireEvent.click(await screen.findByRole("button", { name: "Áp dụng vào bản nháp" }));

    expect(mocks.save).not.toHaveBeenCalled();
    expect(screen.queryByText("Làm tốt · Cần sửa")).not.toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: /Đã kiểm tra và xác nhận giải thích/ })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Xác định thực thể" })).toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Bội số quan hệ" })).not.toBeChecked();

    mocks.save.mockImplementation(async () => ({ ...baseDetail.evaluation, version: 2, score: 1 }));
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
    await waitFor(() => expect(mocks.save).toHaveBeenCalledTimes(1));
    const body = mocks.save.mock.calls[0]![1];
    expect(body).toMatchObject({
      checkedItemsByRubric: { r1: ["i1"] },
      itemScoresByRubric: { r1: { i1: 1, i2: 0.5 } },
      feedback: "Nhận xét chung từ AI",
      usedProposalId: "prop-structured",
    });
    expect(body.structuredFeedback).toEqual(sampleStructuredFeedback);
  });

});
