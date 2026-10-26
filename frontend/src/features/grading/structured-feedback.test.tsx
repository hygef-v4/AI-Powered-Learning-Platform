import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { GradingWorkspacePage } from "./GradingWorkspacePage";
import { StudentGradePanel } from "./StudentGradePanel";
import { GradingFeedbackSummary } from "./StructuredFeedbackSection";
import type { Detail, Rubric, StructuredFeedback } from "./types";

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

vi.mock("./api", () => ({
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
    expect(document.getElementById("submission-block-part-1")).not.toBeNull();
    expect(
      screen.getByText(/Tham chiếu đoạn: missing-block-id \(hiển thị dạng văn bản, không có neo trực tiếp\)/),
    ).toBeInTheDocument();

    // Full marks item shows "Gợi ý thêm (không trừ điểm)" label
    expect(screen.getByText(/Gợi ý thêm \(không trừ điểm\):/)).toBeInTheDocument();
  });

  it("applies structured feedback to teacher draft, warns on score change, blocks finalize until confirmed/edited, and saves/clears cleanly", async () => {
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

    render(<GradingWorkspacePage evaluationId="eval-1" />);
    const applyBtn = await screen.findByRole("button", { name: "Áp dụng vào bản nháp" });
    fireEvent.click(applyBtn);

    // Apply populates draft only; does not auto-save and preserves teacher adjustment (-0.25)
    expect(mocks.save).not.toHaveBeenCalled();
    expect(
      screen.getByText("Tóm tắt nhận xét cấu trúc (Làm tốt · Cần sửa)"),
    ).toBeInTheDocument();
    expect(screen.getByDisplayValue("-0.25")).toBeInTheDocument();

    // Acknowledge the AI review flag on item 2 first
    const ackCheckbox = screen.getByRole("checkbox", {
      name: "Đã kiểm tra và xác nhận giải thích: Bội số quan hệ",
    });
    fireEvent.click(ackCheckbox);
    expect(ackCheckbox).toBeChecked();

    // Now change item 2's score from 0.5 to 0.75 without editing its explanation
    const scoreInput2 = screen.getByRole("spinbutton", { name: "Điểm: Bội số quan hệ" });
    fireEvent.change(scoreInput2, { target: { value: "0.75" } });

    // Score-change review warning appears immediately
    expect(
      await screen.findByText(/Điểm mục này đã thay đổi sau khi tạo nhận xét \(0\.50 → 0\.75\)/),
    ).toBeInTheDocument();

    // Save the draft with the unresolved score mismatch (allowed for DRAFT)
    mocks.save.mockImplementation(async (_id, body) => ({
      ...baseDetail.evaluation,
      version: 2,
      score: 1.5,
      rubricItemScores: body.itemScoresByRubric,
      structuredFeedback: body.structuredFeedback,
      reviewedItemScores: body.reviewedItemScores,
      acknowledgedReviewItems: body.acknowledgedReviewItems,
    }));
    fireEvent.click(screen.getByRole("button", { name: "Lưu nháp / thay đổi" }));
    await waitFor(() => expect(mocks.save).toHaveBeenCalledTimes(1));

    // Attempt to finalize while score mismatch is unresolved -> server rejects with STRUCTURED_FEEDBACK_SCORE_MISMATCH
    mocks.finalize.mockResolvedValue({
      items: [
        {
          evaluationId: "eval-1",
          outcome: "FAILED",
          code: "STRUCTURED_FEEDBACK_SCORE_MISMATCH",
          newVersion: null,
        },
      ],
    });
    fireEvent.click(screen.getByRole("button", { name: "Chốt điểm" }));
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(
      await screen.findByText(
        "Có mục đã thay đổi điểm nhưng chưa cập nhật hoặc xác nhận lại giải thích nhận xét.",
      ),
    ).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole("button", { name: "Đóng" })[0]!);

    // Teacher edits the explanation for item 2 to match the new 0.75 score
    const reasonTextarea2 = screen.getByRole("textbox", { name: "Giải thích điểm: Bội số quan hệ" });
    fireEvent.change(reasonTextarea2, {
      target: { value: "Đạt 0.75/1.00 điểm sau khi giảng viên kiểm tra trực tiếp sơ đồ" },
    });

    // Score mismatch warning disappears after teacher edits explanation
    expect(
      screen.queryByText(/Điểm mục này đã thay đổi sau khi tạo nhận xét/),
    ).not.toBeInTheDocument();

    // Save updated explanation and verify payload
    fireEvent.click(screen.getByRole("button", { name: "Lưu nháp / thay đổi" }));
    await waitFor(() => expect(mocks.save).toHaveBeenCalledTimes(2));
    const secondSaveBody = mocks.save.mock.calls[1]![1];
    expect(secondSaveBody.structuredFeedback.items[1].reason).toBe(
      "Đạt 0.75/1.00 điểm sau khi giảng viên kiểm tra trực tiếp sơ đồ",
    );
    expect(secondSaveBody.reviewedItemScores.r1.i2).toBe("0.75");

    // Explicit clear removes structuredFeedback with clearStructuredFeedback=true
    fireEvent.click(screen.getByRole("button", { name: "Xóa nhận xét cấu trúc" }));
    fireEvent.click(screen.getByRole("button", { name: "Lưu nháp / thay đổi" }));
    await waitFor(() => expect(mocks.save).toHaveBeenCalledTimes(3));
    expect(mocks.save.mock.calls[2]![1].clearStructuredFeedback).toBe(true);
    expect(mocks.save.mock.calls[2]![1].structuredFeedback).toBeUndefined();
  });

  it("renders sanitized published StudentGradePanel with rubric items, optional full-mark advice, and member override notice without exposing teacher review flags", async () => {
    mocks.attempt.mockResolvedValue({
      assignmentId: "a1",
      attemptId: "att-1",
      score: 1.25,
      maxScore: 2,
      rubricScore: 1.5,
      memberOverride: true,
      feedback: "Nhận xét chung đã duyệt",
      publishedAt: "2026-10-26T04:00:00Z",
      updated: false,
      rubricChecks: "{}",
      structuredSummary: {
        strengths: ["Xác định đúng thực thể"],
        improvements: ["Bổ sung bội số"],
        nextSteps: ["Xem lại bài giảng chương 3"],
      },
      rubricItems: [
        {
          rubricId: "r1",
          partId: "part-1",
          criterionTitle: "Mô hình hóa",
          itemId: "i1",
          itemText: "Xác định thực thể",
          awardedPoints: 1,
          maxPoints: 1,
          evidence: "Có đủ LopHoc và HocSinh",
          reason: "Đạt tối đa 1.00/1.00 điểm",
          suggestion: "Có thể đặt tên thuộc tính rõ nghĩa hơn",
        },
        {
          rubricId: "r1",
          partId: "part-1",
          criterionTitle: "Mô hình hóa",
          itemId: "i2",
          itemText: "Bội số quan hệ",
          awardedPoints: 0.5,
          maxPoints: 1,
          evidence: "Thiếu ký hiệu 1..*",
          reason: "Đạt 0.50/1.00 điểm do thiếu bội số ở một đầu",
          suggestion: "Thêm 1..* vào đầu HocSinh",
        },
      ],
    });

    const { container } = render(<StudentGradePanel attemptId="att-1" />);
    expect(await screen.findByText(/1\.25 \/ 2\.00/)).toBeInTheDocument();
    expect(screen.getByText(/Điểm theo rubric: 1\.50 \/ 2\.00/)).toBeInTheDocument();
    expect(
      screen.getByText(/Điểm cá nhân đã được giảng viên điều chỉnh riêng/),
    ).toBeInTheDocument();
    expect(screen.getByText("Xác định đúng thực thể")).toBeInTheDocument();
    expect(screen.getByText(/Mô hình hóa: Xác định thực thể/)).toBeInTheDocument();
    expect(screen.getByText(/Gợi ý thêm \(không trừ điểm\):/)).toBeInTheDocument();
    expect(screen.getByText(/Cách cải thiện:/)).toBeInTheDocument();

    // Never expose teacher review controls or internal flags
    expect(container.textContent).not.toContain("Cần giảng viên kiểm tra");
    expect(container.textContent).not.toContain("requiresReview");
    expect(screen.queryByRole("checkbox")).toBeNull();
  });
});
