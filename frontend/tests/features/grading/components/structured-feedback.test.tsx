import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { GradingWorkspacePage } from "@/features/grading/pages/GradingWorkspacePage";
import { GradingFeedbackSummary } from "@/features/grading/components/StructuredFeedbackSection";
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
    detail: mocks.detail,
    proposals: mocks.proposals,
    save: mocks.save,
    finalize: mocks.finalize,
    attempt: mocks.attempt,
    assignmentContext: mocks.assignmentContext,
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
      { id: "part-1", type: "HEADING", content: { text: "Câu 1: Thiết kế lớp" } },
      { id: "block-text-1", type: "PARAGRAPH", content: { text: "Nội dung bài làm của học sinh" } },
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
    fireEvent.change(screen.getByLabelText("Cần sửa"), { target: { value: "Thiếu multiplicity" } });
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
});

describe("Structured Grading Feedback — Teacher Workspace & Student View", () => {
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
    fireEvent.click(await screen.findByRole("button", { name: "Chấm bằng AI" }));

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
    const validAnchor = screen.getByRole("link", { name: "Đi tới đoạn bài làm (part-1)" });
    expect(validAnchor).toHaveAttribute("href", "#submission-block-part-1");
    expect(document.querySelector('[data-block-id="part-1"]')).not.toBeNull();
    expect(
      screen.getByText(/Tham chiếu đoạn: missing-block-id \(hiển thị dạng văn bản, không có neo trực tiếp\)/),
    ).toBeInTheDocument();

    // Full marks item shows "Gợi ý thêm (không trừ điểm)" label
    expect(screen.getByText(/Gợi ý thêm \(không trừ điểm\):/)).toBeInTheDocument();
  });

  it("áp dụng đề xuất AI chỉ lấy checklist (điểm lẻ thành chưa đạt), không đưa nhận xét của AI vào bản nháp", async () => {
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
    fireEvent.click(await screen.findByRole("button", { name: "Chấm bằng AI" }));
    fireEvent.click(await screen.findByRole("button", { name: "Áp dụng vào bản nháp" }));

    expect(mocks.save).not.toHaveBeenCalled();
    expect(screen.queryByText("Làm tốt · Cần sửa")).not.toBeInTheDocument();
    expect(screen.queryByRole("checkbox", { name: /Đã kiểm tra và xác nhận giải thích/ })).not.toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Xác định thực thể" })).toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Bội số quan hệ" })).not.toBeChecked();

    mocks.save.mockImplementation(async () => ({ ...baseDetail.evaluation, version: 2, score: 1 }));
    fireEvent.click(screen.getByRole("button", { name: "Lưu nháp / thay đổi" }));
    await waitFor(() => expect(mocks.save).toHaveBeenCalledTimes(1));
    const body = mocks.save.mock.calls[0]![1];
    expect(body).toMatchObject({
      checkedItemsByRubric: { r1: ["i1"] },
      itemScoresByRubric: { r1: { i1: 1, i2: 0 } },
      feedback: "Nhận xét nháp ban đầu",
      usedProposalId: "prop-structured",
    });
    expect(body.structuredFeedback).toBeUndefined();
  });

});
