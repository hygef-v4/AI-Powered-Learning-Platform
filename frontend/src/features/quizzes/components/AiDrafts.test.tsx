import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import {
  acceptBankDraft, acceptQuizDraft, getBankDrafts, getQuizDraft, getQuizDrafts, getQuizMaterials, getSubjectMaterials, requestBankDraft,
} from "../api";
import { MaterialRefPicker, QuestionAiDraftDialog, QuizAiDraftPanel } from "./AiDraftTargets";
import type { AiDraft, QuestionContent, QuizDetail } from "../model/types";

vi.mock("../api", () => ({
  getSubjectMaterials: vi.fn(),
  getQuizMaterials: vi.fn(),
  getBankDrafts: vi.fn(),
  requestBankDraft: vi.fn(),
  getBankDraft: vi.fn(),
  acceptBankDraft: vi.fn(),
  discardBankDraft: vi.fn(),
  getQuizDrafts: vi.fn(),
  requestQuizDraft: vi.fn(),
  getQuizDraft: vi.fn(),
  acceptQuizDraft: vi.fn(),
  discardQuizDraft: vi.fn(),
}));

// Popup chọn nguồn và trạng thái task của U06 có test riêng; ở đây chỉ cần biết U10 mở chúng và nhận lựa chọn.
vi.mock("@/shared/ai", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/shared/ai")>()),
  AiSourcePicker: ({ task, estimatedTokens, onCancel, onSubmit }: {
    task: string; estimatedTokens?: number; onCancel: () => void;
    onSubmit: (choice: { source: "SHARED" | "PERSONAL"; provider: "GEMINI" | null }) => void;
  }) => (
    <div data-testid="ai-source-picker">
      <span>{task} · ~{estimatedTokens}</span>
      <button type="button" onClick={() => onSubmit({ source: "PERSONAL", provider: "GEMINI" })}>pick-gemini</button>
      <button type="button" onClick={() => onSubmit({ source: "SHARED", provider: null })}>pick-shared</button>
      <button type="button" onClick={onCancel}>pick-cancel</button>
    </div>
  ),
  AiTaskStatus: ({ taskId }: { taskId: string }) => <span data-testid="ai-task-status">{taskId}</span>,
}));

const content = (overrides: Partial<QuestionContent> = {}): QuestionContent => ({
  type: "MCQ_SINGLE", stem: "2 + 2 = ?", options: [{ id: "1", text: "3" }, { id: "2", text: "4" }], correctOptionIds: ["2"],
  explanation: "Cộng", defaultPoints: 1, difficulty: "EASY", tags: [], ...overrides,
});

const draft = (overrides: Partial<AiDraft> = {}): AiDraft => ({
  id: "d1", target: "BANK", quizId: null, subjectId: "s1", origin: "AI_TASK", taskId: null, status: "READY",
  questions: [
    { content: content(), violations: [] },
    { content: content({ stem: "Câu lỗi", options: [{ id: "1", text: "A" }, { id: "2", text: "A" }], correctOptionIds: ["1"] }),
      violations: [{ field: "options", code: "OPTION_DUPLICATE", message: "Các lựa chọn không được trùng nội dung" }] },
  ],
  requestedCount: 2, source: "SHARED", provider: null, materials: [], agentLabel: null, baseVersion: null, failureCode: null,
  failureMessage: null, createdBy: "u1", createdAt: "2026-10-22T08:00:00Z", decidedAt: null, ...overrides,
});

/** Phần tử thứ `index`; báo lỗi rõ khi danh sách ngắn hơn mong đợi. */
function nth(items: HTMLElement[], index: number): HTMLElement {
  const item = items[index];
  if (!item) throw new Error(`Không có phần tử thứ ${index}`);
  return item;
}

afterEach(() => vi.clearAllMocks());

describe("QuestionAiDraftDialog (F4)", () => {
  it("gửi yêu cầu qua popup chọn nguồn của U06, chỉ kèm tài liệu đã có chữ", async () => {
    vi.mocked(getBankDrafts).mockResolvedValue([]);
    vi.mocked(getSubjectMaterials).mockResolvedValue([
      { materialId: "m1", title: "Chương 1", hasText: true },
      { materialId: "m2", title: "Video", hasText: false },
    ]);
    vi.mocked(requestBankDraft).mockResolvedValue(draft({ status: "PENDING", questions: [], taskId: "t1" }));
    render(<QuestionAiDraftDialog subjectId="s1" onClose={vi.fn()} onAccepted={vi.fn()} />);

    const boxes = await screen.findAllByTestId("ai-draft-material");
    expect(nth(boxes, 1)).toBeDisabled();
    fireEvent.click(nth(boxes, 0));
    fireEvent.click(screen.getByTestId("ai-draft-submit"));
    expect(await screen.findByText("Nhập yêu cầu, tối đa 2.000 ký tự")).toBeInTheDocument();
    expect(screen.queryByTestId("ai-source-picker")).not.toBeInTheDocument();

    fireEvent.change(screen.getByTestId("ai-draft-instructions"), { target: { value: "Ôn tập chương 1" } });
    fireEvent.change(screen.getByTestId("ai-draft-count"), { target: { value: "3" } });
    fireEvent.click(screen.getByTestId("ai-draft-submit"));
    expect(await screen.findByTestId("ai-source-picker")).toHaveTextContent("Sinh câu hỏi · Ngân hàng câu hỏi của môn · ~1800");
    expect(screen.queryByTestId("ai-draft-dialog")).not.toBeInTheDocument();
    expect(requestBankDraft).not.toHaveBeenCalled();
    fireEvent.click(screen.getByText("pick-gemini"));

    await waitFor(() => expect(requestBankDraft).toHaveBeenCalledWith("s1", {
      instructions: "Ôn tập chương 1", count: 3, type: null, difficulty: null, materialIds: ["m1"],
      choice: { source: "PERSONAL", provider: "GEMINI" },
    }));
    expect(await screen.findByText(/AI đang soạn đề xuất/)).toBeInTheDocument();
    expect(screen.getByTestId("ai-task-status")).toHaveTextContent("t1");
  });

  it("hủy popup chọn nguồn thì quay lại form với nội dung đã nhập", async () => {
    vi.mocked(getBankDrafts).mockResolvedValue([]);
    vi.mocked(getSubjectMaterials).mockResolvedValue([]);
    render(<QuestionAiDraftDialog subjectId="s1" onClose={vi.fn()} onAccepted={vi.fn()} />);
    fireEvent.change(await screen.findByTestId("ai-draft-instructions"), { target: { value: "Chương 2" } });
    fireEvent.change(screen.getByTestId("ai-draft-count"), { target: { value: "7" } });
    fireEvent.click(screen.getByTestId("ai-draft-submit"));
    fireEvent.click(await screen.findByText("pick-cancel"));

    expect(await screen.findByTestId("ai-draft-instructions")).toHaveValue("Chương 2");
    expect(screen.getByTestId("ai-draft-count")).toHaveValue("7");
    expect(requestBankDraft).not.toHaveBeenCalled();
  });

  it("sửa, bỏ, chấp nhận từng câu rồi lưu đúng các câu đã chấp nhận", async () => {
    vi.mocked(getBankDrafts).mockResolvedValue([draft()]);
    vi.mocked(acceptBankDraft).mockResolvedValue({ draft: draft({ status: "ACCEPTED" }), questionIds: ["q9"] });
    const onAccepted = vi.fn();
    render(<QuestionAiDraftDialog subjectId="s1" onClose={vi.fn()} onAccepted={onAccepted} />);

    const cards = await screen.findAllByTestId("ai-draft-question");
    expect(within(nth(cards, 1)).getByText(/Cần sửa: Các lựa chọn không được trùng nội dung/)).toBeInTheDocument();
    fireEvent.click(within(nth(cards, 1)).getByTestId("ai-draft-accept-one"));
    expect(within(nth(screen.getAllByTestId("ai-draft-question"), 1)).getByTestId("mcq-editor")).toBeInTheDocument();
    fireEvent.click(within(nth(screen.getAllByTestId("ai-draft-question"), 1)).getByTestId("ai-draft-drop"));

    fireEvent.click(within(nth(screen.getAllByTestId("ai-draft-question"), 0)).getByTestId("ai-draft-edit"));
    fireEvent.change(screen.getByTestId("mcq-editor-stem"), { target: { value: "2 + 2 bằng mấy?" } });
    fireEvent.click(within(nth(screen.getAllByTestId("ai-draft-question"), 0)).getByTestId("ai-draft-accept-one"));
    expect(screen.getByText("Đề xuất (1 đã chấp nhận / 2)")).toBeInTheDocument();

    fireEvent.click(screen.getByTestId("ai-draft-accept"));
    await waitFor(() => expect(acceptBankDraft).toHaveBeenCalledWith("s1", "d1", [
      expect.objectContaining({ stem: "2 + 2 bằng mấy?", correctOptionIds: ["2"] }),
    ]));
    expect(onAccepted).toHaveBeenCalledWith(1);
  });

  it("hiện lý do khi đề xuất lỗi", async () => {
    vi.mocked(getBankDrafts).mockResolvedValue([]);
    vi.mocked(getSubjectMaterials).mockResolvedValue([]);
    vi.mocked(requestBankDraft).mockResolvedValue(draft({ status: "FAILED", questions: [], failureCode: "AI_TASK_FAILED",
      failureMessage: "AI chưa sẵn sàng" }));
    render(<QuestionAiDraftDialog subjectId="s1" onClose={vi.fn()} onAccepted={vi.fn()} />);
    fireEvent.change(await screen.findByTestId("ai-draft-instructions"), { target: { value: "x" } });
    fireEvent.click(screen.getByTestId("ai-draft-submit"));
    fireEvent.click(await screen.findByText("pick-shared"));

    expect(await screen.findByTestId("ai-draft-failed")).toHaveTextContent("AI chưa sẵn sàng");
    expect(screen.getByTestId("ai-draft-new")).toBeInTheDocument();
  });
});

describe("QuizAiDraftPanel (F8, F13)", () => {
  const quiz = { quiz: { id: "z1", version: 4 }, items: [], maxScore: 0, canManage: true } as unknown as QuizDetail;

  it("đề xuất từ agent chấp nhận kèm version; 409 báo tải lại", async () => {
    const mcp = draft({ target: "QUIZ", quizId: "z1", origin: "MCP", agentLabel: "Claude Desktop" });
    vi.mocked(getQuizDrafts).mockResolvedValue([mcp]);
    vi.mocked(getQuizDraft).mockResolvedValue(mcp);
    vi.mocked(acceptQuizDraft).mockRejectedValue(new ApiError({ status: 409, code: "QUIZ_VERSION_CONFLICT" }));
    const onConflict = vi.fn();
    render(<QuizAiDraftPanel detail={quiz} disabled={false} onConflict={onConflict} onQuizChange={vi.fn()} />);

    expect(await screen.findByText("Agent: Claude Desktop")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("quiz-ai-draft-view"));
    const cards = await screen.findAllByTestId("ai-draft-question");
    fireEvent.click(within(nth(cards, 0)).getByTestId("ai-draft-accept-one"));
    fireEvent.click(screen.getByTestId("ai-draft-accept"));

    await waitFor(() => expect(acceptQuizDraft).toHaveBeenCalledWith("z1", "d1", [expect.objectContaining({ stem: "2 + 2 = ?" })], 4));
    expect(onConflict).toHaveBeenCalled();
    expect(await screen.findByText(/Quiz đã được thay đổi ở phiên khác/)).toBeInTheDocument();
  });
});

describe("MaterialRefPicker (H8)", () => {
  it("giới hạn 10 học liệu và giữ học liệu không còn truy cập được", async () => {
    const list = Array.from({ length: 11 }, (_, i) => ({ materialId: `m${i}`, title: `Tài liệu ${i}`, hasText: true }));
    vi.mocked(getQuizMaterials).mockResolvedValue(list);
    const chosen = [...list.slice(0, 9).map((m) => m.materialId), "gone"];
    const onChange = vi.fn();
    render(<MaterialRefPicker quizId="z1" value={chosen} onChange={onChange} readOnly={false} />);

    const boxes = await screen.findAllByTestId("material-ref-option");
    expect(nth(boxes, 10)).toBeDisabled();
    expect(screen.getByText("Học liệu không còn truy cập được")).toBeInTheDocument();
    fireEvent.click(nth(boxes, 0));
    expect(onChange).toHaveBeenCalledWith(chosen.filter((id) => id !== "m0"));
  });
});
