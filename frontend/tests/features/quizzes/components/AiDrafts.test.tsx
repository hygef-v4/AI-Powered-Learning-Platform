import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import {
  acceptBankDraft, acceptQuizDraft, getBankDrafts, getQuizDraft, getQuizDrafts, getQuizMaterials, getSubjectMaterials, requestBankDraft, requestQuizDraft,
} from "@/features/quizzes/api";
import { QuestionAiDraftDialog, QuizAiDraftPanel } from "@/features/quizzes/components/AiDraftTargets";
import type { AiDraft, QuestionContent, QuizDetail } from "@/features/quizzes/model/types";

vi.mock("@/features/quizzes/api", () => ({
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
  it("allows a document-only request without typing instructions", async () => {
    vi.mocked(getBankDrafts).mockResolvedValue([]);
    vi.mocked(getSubjectMaterials).mockResolvedValue([{ materialId: "m1", title: "Nguồn", hasText: true }]);
    vi.mocked(requestBankDraft).mockResolvedValue(draft({ status: "PENDING", questions: [] }));
    render(<QuestionAiDraftDialog subjectId="s1" onClose={vi.fn()} onAccepted={vi.fn()} />);
    fireEvent.click(await screen.findByText("Chọn tài liệu nguồn"));
    fireEvent.click(await screen.findByRole("checkbox", { name: "Nguồn" }));
    fireEvent.click(screen.getByTestId("ai-draft-submit"));
    await waitFor(() => expect(requestBankDraft).toHaveBeenCalledWith("s1", expect.objectContaining({
      instructions: "", materialIds: ["m1"],
    })));
  });

  it("accepts all remaining valid proposals in one request, preserving edits and dropped questions", async () => {
    vi.mocked(getBankDrafts).mockResolvedValue([draft()]);
    vi.mocked(acceptBankDraft).mockResolvedValue({ draft: draft({ status: "ACCEPTED" }), questionIds: ["q1"] });
    const onAccepted = vi.fn();
    render(<QuestionAiDraftDialog subjectId="s1" onClose={vi.fn()} onAccepted={onAccepted} />);
    const cards = await screen.findAllByTestId("ai-draft-question");
    fireEvent.click(within(nth(cards, 1)).getByTestId("ai-draft-drop"));
    fireEvent.click(within(nth(cards, 0)).getByTestId("ai-draft-edit"));
    fireEvent.change(screen.getByTestId("mcq-editor-stem"), { target: { value: "Câu đã sửa" } });
    fireEvent.click(screen.getByRole("button", { name: /Chấp nhận tất cả/ }));
    await waitFor(() => expect(acceptBankDraft).toHaveBeenCalledWith("s1", "d1", [
      expect.objectContaining({ stem: "Câu đã sửa" }),
    ]));
    expect(onAccepted).toHaveBeenCalledWith(1);
  });

  it("blocks bulk acceptance and opens invalid questions for correction", async () => {
    vi.mocked(getBankDrafts).mockResolvedValue([draft()]);
    render(<QuestionAiDraftDialog subjectId="s1" onClose={vi.fn()} onAccepted={vi.fn()} />);
    fireEvent.click(await screen.findByRole("button", { name: /Chấp nhận tất cả/ }));
    expect(await screen.findByTestId("mcq-editor")).toBeInTheDocument();
    expect(acceptBankDraft).not.toHaveBeenCalled();
  });

  it("locks proposal edits while bulk acceptance is pending", async () => {
    vi.mocked(getBankDrafts).mockResolvedValue([draft({ questions: [{ content: content(), violations: [] }] })]);
    let finish!: (result: Awaited<ReturnType<typeof acceptBankDraft>>) => void;
    vi.mocked(acceptBankDraft).mockReturnValue(new Promise((resolve) => { finish = resolve; }));
    const onAccepted = vi.fn();
    render(<QuestionAiDraftDialog subjectId="s1" onClose={vi.fn()} onAccepted={onAccepted} />);
    fireEvent.click(await screen.findByRole("button", { name: "Chấp nhận tất cả" }));
    expect(screen.getByTestId("ai-draft-edit")).toBeDisabled();
    expect(screen.getByTestId("ai-draft-drop")).toBeDisabled();
    expect(screen.getByTestId("ai-draft-accept-all")).toBeDisabled();
    finish({ draft: draft({ status: "ACCEPTED" }), questionIds: ["q1"] });
    await waitFor(() => expect(onAccepted).toHaveBeenCalledWith(1));
    expect(acceptBankDraft).toHaveBeenCalledTimes(1);
  });

  it.each([
    { source: "AUTO", provider: null, label: "Hệ thống tự chọn nguồn AI" },
    { source: null, provider: null, label: "Hệ thống tự chọn nguồn AI" },
    { source: "PERSONAL", provider: null, label: "API cá nhân" },
    { source: "PERSONAL", provider: "GEMINI", label: "Gemini cá nhân" },
    { source: "SHARED", provider: null, label: "API của trường" },
  ])("shows $label for source $source and provider $provider", async ({ source, provider, label }) => {
    vi.mocked(getBankDrafts).mockResolvedValue([draft({ source, provider })]);
    render(<QuestionAiDraftDialog subjectId="s1" onClose={vi.fn()} onAccepted={vi.fn()} />);
    const view = await screen.findByTestId("ai-draft-view");
    expect(view).toHaveTextContent(label);
    expect(view).not.toHaveTextContent("null cá nhân");
  });

  it("restores the same request after AI returns a failed draft", async () => {
    vi.mocked(getBankDrafts).mockResolvedValue([]);
    vi.mocked(getSubjectMaterials).mockResolvedValue([{ materialId: "m1", title: "Chương 1", hasText: true }]);
    vi.mocked(requestBankDraft).mockResolvedValue(draft({ status: "FAILED", failureCode: "AI_TASK_FAILED" }));
    render(<QuestionAiDraftDialog subjectId="s1" onClose={vi.fn()} onAccepted={vi.fn()} />);
    fireEvent.change(await screen.findByTestId("ai-draft-instructions"), { target: { value: "Yêu cầu cần thử lại" } });
    fireEvent.change(screen.getByTestId("ai-draft-count"), { target: { value: "3" } });
    fireEvent.click(screen.getByText("Chọn tài liệu nguồn"));
    fireEvent.click(await screen.findByRole("checkbox", { name: "Chương 1" }));
    fireEvent.click(screen.getByTestId("ai-draft-submit"));
    fireEvent.click(await screen.findByTestId("ai-draft-new"));
    expect(await screen.findByTestId("ai-draft-instructions")).toHaveValue("Yêu cầu cần thử lại");
    expect(screen.getByTestId("ai-draft-count")).toHaveValue("3");
    expect(screen.getByRole("button", { name: "Bỏ Chương 1" })).toBeInTheDocument();
  });

  it("gửi yêu cầu ngay, hệ thống tự chọn nguồn AI, chỉ kèm tài liệu đã có chữ", async () => {
    vi.mocked(getBankDrafts).mockResolvedValue([]);
    vi.mocked(getSubjectMaterials).mockResolvedValue([
      { materialId: "m1", title: "Chương 1", hasText: true },
      { materialId: "m2", title: "Video", hasText: false },
    ]);
    vi.mocked(requestBankDraft).mockResolvedValue(draft({ status: "PENDING", questions: [], taskId: "t1" }));
    render(<QuestionAiDraftDialog subjectId="s1" onClose={vi.fn()} onAccepted={vi.fn()} />);

    const boxes = await screen.findAllByTestId("ai-draft-material");
    expect(nth(boxes, 1)).toBeDisabled();
    fireEvent.click(screen.getByTestId("ai-draft-submit"));
    expect(await screen.findByText("Nhập yêu cầu hoặc chọn tài liệu nguồn, tối đa 2.000 ký tự")).toBeInTheDocument();
    expect(screen.queryByTestId("ai-source-picker")).not.toBeInTheDocument();

    fireEvent.click(nth(boxes, 0));
    fireEvent.change(screen.getByTestId("ai-draft-instructions"), { target: { value: "Ôn tập chương 1" } });
    fireEvent.change(screen.getByTestId("ai-draft-count"), { target: { value: "3" } });
    fireEvent.click(screen.getByTestId("ai-draft-submit"));
    // Không còn popup chọn nguồn (yêu cầu 2026-11-05).
    expect(screen.queryByTestId("ai-source-picker")).not.toBeInTheDocument();

    await waitFor(() => expect(requestBankDraft).toHaveBeenCalledWith("s1", {
      instructions: "Ôn tập chương 1", count: 3, type: null, difficulty: null, materialIds: ["m1"],
    }));
    expect(await screen.findByText(/AI đang soạn đề xuất/)).toBeInTheDocument();
    expect(screen.getByTestId("ai-task-status")).toHaveTextContent("t1");
  });

  it("gửi lỗi thì vẫn ở form với nội dung đã nhập", async () => {
    vi.mocked(getBankDrafts).mockResolvedValue([]);
    vi.mocked(getSubjectMaterials).mockResolvedValue([]);
    vi.mocked(requestBankDraft).mockRejectedValue(new Error("Không có nguồn AI dùng được"));
    render(<QuestionAiDraftDialog subjectId="s1" onClose={vi.fn()} onAccepted={vi.fn()} />);
    fireEvent.change(await screen.findByTestId("ai-draft-instructions"), { target: { value: "Chương 2" } });
    fireEvent.change(screen.getByTestId("ai-draft-count"), { target: { value: "7" } });
    fireEvent.click(screen.getByTestId("ai-draft-submit"));

    await waitFor(() => expect(requestBankDraft).toHaveBeenCalled());
    expect(await screen.findByTestId("ai-draft-instructions")).toHaveValue("Chương 2");
    expect(screen.getByTestId("ai-draft-count")).toHaveValue("7");
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

    expect(await screen.findByTestId("ai-draft-failed")).toHaveTextContent("AI chưa sẵn sàng");
    expect(screen.getByTestId("ai-draft-new")).toBeInTheDocument();
  });
});

describe("QuizAiDraftPanel (F8, F13)", () => {
  const quiz = { quiz: { id: "z1", version: 4 }, items: [], maxScore: 0, canManage: true } as unknown as QuizDetail;

  it("toggles the displayed proposal with View and Collapse", async () => {
    const proposal = draft({ target: "QUIZ", quizId: "z1" });
    vi.mocked(getQuizDrafts).mockResolvedValue([proposal]);
    vi.mocked(getQuizDraft).mockResolvedValue(proposal);
    render(<QuizAiDraftPanel inline detail={quiz} disabled={false} onConflict={vi.fn()} onQuizChange={vi.fn()} />);
    fireEvent.click(await screen.findByRole("button", { name: "Xem" }));
    expect(await screen.findByTestId("ai-draft-view")).toBeInTheDocument();
    fireEvent.click(await screen.findByRole("button", { name: "Thu gọn" }));
    expect(screen.queryByTestId("ai-draft-view")).not.toBeInTheDocument();
    fireEvent.click(await screen.findByRole("button", { name: "Xem" }));
    expect(await screen.findByTestId("ai-draft-view")).toBeInTheDocument();
  });

  it("keeps unsaved edits when collapse or another proposal is canceled", async () => {
    const proposal = draft({ target: "QUIZ", quizId: "z1" });
    vi.mocked(getQuizDrafts).mockResolvedValue([proposal, { ...proposal, id: "d2" }]);
    vi.mocked(getQuizDraft).mockResolvedValue(proposal);
    render(<QuizAiDraftPanel inline detail={quiz} disabled={false} onConflict={vi.fn()} onQuizChange={vi.fn()} />);
    fireEvent.click(nth(await screen.findAllByRole("button", { name: "Xem" }), 0));
    fireEvent.click(nth(await screen.findAllByTestId("ai-draft-edit"), 0));
    fireEvent.change(screen.getByTestId("mcq-editor-stem"), { target: { value: "Giữ nội dung" } });
    // Thu gọn khi còn chỉnh sửa: popup xác nhận hiện ra; đóng popup thì giữ nguyên nội dung đang sửa.
    fireEvent.click(screen.getByRole("button", { name: "Thu gọn" }));
    expect(screen.getByRole("dialog", { name: "Bỏ các chỉnh sửa đề xuất chưa lưu?" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getByTestId("mcq-editor-stem")).toHaveValue("Giữ nội dung");
    fireEvent.click(screen.getByRole("button", { name: "Xem" }));
    expect(screen.getByRole("dialog", { name: "Bỏ các chỉnh sửa chưa lưu để xem đề xuất khác?" })).toBeInTheDocument();
    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getByTestId("mcq-editor-stem")).toHaveValue("Giữ nội dung");
    expect(getQuizDraft).toHaveBeenCalledTimes(1);
  });

  it("discards unsaved edits only after the confirmation dialog is accepted", async () => {
    const proposal = draft({ target: "QUIZ", quizId: "z1" });
    vi.mocked(getQuizDrafts).mockResolvedValue([proposal]);
    vi.mocked(getQuizDraft).mockResolvedValue(proposal);
    render(<QuizAiDraftPanel inline detail={quiz} disabled={false} onConflict={vi.fn()} onQuizChange={vi.fn()} />);
    fireEvent.click(await screen.findByRole("button", { name: "Xem" }));
    fireEvent.click(nth(await screen.findAllByTestId("ai-draft-edit"), 0));
    fireEvent.change(screen.getByTestId("mcq-editor-stem"), { target: { value: "Sẽ bỏ" } });
    fireEvent.click(screen.getByRole("button", { name: "Thu gọn" }));
    fireEvent.click(screen.getByRole("button", { name: "Bỏ chỉnh sửa" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.queryByTestId("mcq-editor-stem")).not.toBeInTheDocument();
  });

  it("accepts all quiz proposals using the current quiz version", async () => {
    const proposal = draft({ target: "QUIZ", quizId: "z1", questions: [{ content: content(), violations: [] }] });
    vi.mocked(getQuizDrafts).mockResolvedValue([proposal]);
    vi.mocked(getQuizDraft).mockResolvedValue(proposal);
    vi.mocked(acceptQuizDraft).mockResolvedValue(quiz);
    const onQuizChange = vi.fn();
    render(<QuizAiDraftPanel inline detail={quiz} disabled={false} onConflict={vi.fn()} onQuizChange={onQuizChange} />);
    fireEvent.click(await screen.findByRole("button", { name: "Xem" }));
    fireEvent.click(await screen.findByRole("button", { name: /Chấp nhận tất cả/ }));
    await waitFor(() => expect(acceptQuizDraft).toHaveBeenCalledWith("z1", "d1", [content()], 4));
    expect(onQuizChange).toHaveBeenCalledWith(quiz);
  });

  it("authors AI requests inline and keeps selected documents after a request error", async () => {
    vi.mocked(getQuizDrafts).mockResolvedValue([]);
    vi.mocked(getQuizMaterials).mockResolvedValue([{ materialId: "m1", title: "Chương 1.pdf", hasText: true }]);
    vi.mocked(requestQuizDraft).mockRejectedValue(new Error("offline"));
    render(<QuizAiDraftPanel inline requestOpen detail={quiz} disabled={false} onConflict={vi.fn()} onQuizChange={vi.fn()} />);
    fireEvent.change(await screen.findByTestId("ai-draft-instructions"), { target: { value: "Soạn theo chương 1" } });
    fireEvent.click(screen.getByText("Chọn tài liệu nguồn"));
    fireEvent.click(await screen.findByRole("checkbox", { name: "Chương 1.pdf" }));
    fireEvent.click(screen.getByTestId("ai-draft-submit"));
    await waitFor(() => expect(requestQuizDraft).toHaveBeenCalledWith("z1", expect.objectContaining({ materialIds: ["m1"] }), 4));
    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(screen.getByTestId("ai-draft-instructions")).toHaveValue("Soạn theo chương 1");
    expect(screen.getByRole("button", { name: "Bỏ Chương 1.pdf" })).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("shows source truncation warnings when reopening an existing AI proposal", async () => {
    const proposal = draft({ target: "QUIZ", quizId: "z1", sourceWarnings: ["Chương 2: nội dung bị cắt do giới hạn."] });
    vi.mocked(getQuizDrafts).mockResolvedValue([proposal]);
    vi.mocked(getQuizDraft).mockResolvedValue(proposal);
    render(<QuizAiDraftPanel inline detail={quiz} disabled={false} onConflict={vi.fn()} onQuizChange={vi.fn()} />);
    fireEvent.click(await screen.findByTestId("quiz-ai-draft-view"));
    expect(await screen.findByText("Chương 2: nội dung bị cắt do giới hạn.")).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

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
