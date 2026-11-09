import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import {
  addInlineQuestion, createSubjectQuiz, getBankQuestionsForQuiz, getClassQuizzes, getManagedSubjects, getQuiz, getSubjectQuizzes, previewRandomItems,
  previewPaper, proposeQuestion, publishQuiz, reopenQuiz, saveDrawRules, saveQuizItems, updateQuizDetails,
} from "@/features/quizzes/api";
import { BankQuestionPicker } from "@/features/quizzes/components/BankQuestionPicker";
import { ClassQuizzesSection } from "@/features/quizzes/components/ClassQuizzesSection";
import { settingsFormOf, settingsInput } from "@/features/quizzes/components/QuizDetailParts";
import { QuizDetailPage } from "@/features/quizzes/pages/QuizDetailPage";
import { SubjectQuizListPage } from "@/features/quizzes/pages/SubjectQuizListPage";
import { quizDetailSections } from "@/features/quizzes/slots";
import type { QuestionView, QuizDetail, QuizInfo, QuizRow } from "@/features/quizzes/model/types";

vi.mock("@/features/quizzes/api", () => ({
  getManagedSubjects: vi.fn(),
  getSubjectQuizzes: vi.fn(),
  createSubjectQuiz: vi.fn(),
  getClassQuizzes: vi.fn(),
  createClassQuiz: vi.fn(),
  getCopySources: vi.fn(),
  copyQuizToClass: vi.fn(),
  getQuiz: vi.fn(),
  updateQuizDetails: vi.fn(),
  saveQuizItems: vi.fn(),
  previewRandomItems: vi.fn(),
  getBankQuestionsForQuiz: vi.fn(),
  addInlineQuestion: vi.fn(),
  updateInlineQuestion: vi.fn(),
  saveDrawRules: vi.fn(),
  getBankTags: vi.fn(async () => [{ tag: "Chương 1", activeCount: 12 }]),
  previewPaper: vi.fn(),
  proposeQuestion: vi.fn(),
  publishQuiz: vi.fn(),
  retireQuiz: vi.fn(),
  reopenQuiz: vi.fn(),
  cloneQuiz: vi.fn(),
  createQuizVersion: vi.fn(),
  deleteQuiz: vi.fn(),
  getQuizMaterials: vi.fn(() => Promise.resolve([])),
  getQuizDrafts: vi.fn(() => Promise.resolve([])),
}));

const push = vi.fn();
const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push, replace }), useSearchParams: () => new URLSearchParams(window.location.search) }));
vi.mock("@/features/materials/components/ModulePlace", () => ({ useModuleScope: () => null, useModulePlace: () => ({ reload: () => {} }), ModuleNeighbours: () => null }));

const QUESTION: QuestionView = {
  id: "q1", lineageId: "q1", versionNo: 2, subjectId: "s1", type: "MCQ_SINGLE", stem: "Thủ đô của Việt Nam?",
  options: [{ id: "1", text: "Hà Nội" }, { id: "2", text: "Huế" }], correctOptionIds: ["1"], explanation: null,
  defaultPoints: 1, difficulty: "EASY", tags: [], status: "ACTIVE", activatedAt: "2026-10-20T08:00:00Z",
  createdBy: "u1", createdAt: "2026-10-20T08:00:00Z", updatedAt: "2026-10-20T08:00:00Z", version: 1,
};
const INFO: QuizInfo = {
  id: "z1", scope: "CLASS", classId: "c1", subjectId: "s1", sourceQuizId: null, origin: "MANUAL", versionNo: 1, title: "Luyện tập",
  instructions: null, settings: { shuffleQuestions: false, shuffleOptions: false, timeLimitMinutes: null, maxAttempts: 1,
    showScoreAfterSubmit: true, showCorrectAnswers: false, leavePenalty: null },
  activityMonitoringEnabled: false, activityNotice: null, paperMode: "FIXED", status: "DRAFT", publishedAt: null, retiredAt: null,
  retiredReason: null, createdAt: "2026-10-20T08:00:00Z", updatedAt: "2026-10-20T08:00:00Z", version: 3,
};
const detail = (quiz: Partial<QuizInfo> = {}, canManage = true): QuizDetail => ({
  quiz: { ...INFO, ...quiz }, items: [{ position: 1, points: 2, inline: false, question: QUESTION }], drawRules: [], maxScore: 2, canManage,
});
const row = (overrides: Partial<QuizRow> = {}): QuizRow => ({
  id: "z1", scope: "CLASS", classId: "c1", subjectId: "s1", versionNo: 1, title: "Luyện tập", status: "DRAFT", itemCount: 3,
  maxScore: 3, timeLimitMinutes: null, updatedAt: "2026-10-20T08:00:00Z", readOnly: false, ...overrides,
});

afterEach(() => vi.clearAllMocks());

describe("SubjectQuizListPage", () => {
  beforeEach(() => {
    vi.mocked(getManagedSubjects).mockResolvedValue([{ id: "s1", code: "SWT301", name: "Kiểm thử", status: "ACTIVE" }]);
    vi.mocked(getSubjectQuizzes).mockResolvedValue({ items: [row({ scope: "SUBJECT", classId: null, status: "OPEN" })], total: 1, page: 0, size: 20 });
  });

  it("lọc theo trạng thái và tạo quiz mở Quiz Detail", async () => {
    render(<SubjectQuizListPage />);
    expect(await screen.findByText("Luyện tập")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("quiz-list-tab-OPEN"));
    await waitFor(() => expect(getSubjectQuizzes).toHaveBeenLastCalledWith("s1", expect.objectContaining({ status: "OPEN", page: 0 })));

    vi.mocked(createSubjectQuiz).mockResolvedValue(detail({ id: "z9", scope: "SUBJECT" }));
    fireEvent.click(screen.getByTestId("quiz-list-create"));
    fireEvent.change(screen.getByTestId("create-quiz-title"), { target: { value: "Ôn tập" } });
    fireEvent.click(screen.getByTestId("create-quiz-submit"));
    await waitFor(() => expect(push).toHaveBeenCalledWith("/quizzes/z9"));
    expect(createSubjectQuiz).toHaveBeenCalledWith("s1", "Ôn tập");
  });
});


it("restores quiz status tab after remount and ignores invalid status", async () => {
  vi.mocked(getManagedSubjects).mockResolvedValue([{ id: "s1", code: "SWT301", name: "Kiểm thử", status: "ACTIVE" }]);
  vi.mocked(getSubjectQuizzes).mockResolvedValue({ items: [], total: 0, page: 0, size: 20 });
  const view = render(<SubjectQuizListPage />);
  fireEvent.click(await screen.findByTestId("quiz-list-tab-OPEN"));
  view.unmount();
  const restored = render(<SubjectQuizListPage />);
  expect(await screen.findByTestId("quiz-list-tab-OPEN")).toHaveAttribute("aria-selected", "true");
  window.history.replaceState(null, "", "?subjectId=s1&status=unknown");
  restored.rerender(<SubjectQuizListPage />);
  expect(screen.getByTestId("quiz-list-tab-ALL")).toHaveAttribute("aria-selected", "true");
});

describe("QuizDetailPage", () => {
  it("keeps formatted description including blank paragraphs when only settings change", () => {
    const document = { blocks: [
      { type: "PARAGRAPH" as const, align: "LEFT" as const, indent: 0, runs: [{ text: "Mô tả", bold: true }] },
      { type: "PARAGRAPH" as const, align: "LEFT" as const, indent: 0, runs: [{ text: "" }] },
    ] };
    const form = settingsFormOf({ ...INFO, instructions: "Mô tả\n", instructionsDocument: document });
    const result = settingsInput({ ...form, timeLimit: "10" });
    expect(result.input?.instructionsDocument).toEqual(document);
  });

  it("locks mode switching until an inline save finishes", async () => {
    let finish!: (value: QuizDetail) => void;
    vi.mocked(getQuiz).mockResolvedValue(detail());
    vi.mocked(addInlineQuestion).mockImplementation(() => new Promise((resolve) => { finish = resolve; }));
    render(<QuizDetailPage id="z1" />);
    fireEvent.click(await screen.findByLabelText("Thêm câu hỏi"));
    fireEvent.click(screen.getByTestId("quiz-detail-add-inline"));
    fireEvent.change(screen.getByTestId("mcq-editor-stem"), { target: { value: "Câu hỏi mới" } });
    fireEvent.change(screen.getByTestId("mcq-editor-option-0"), { target: { value: "A" } });
    fireEvent.change(screen.getByTestId("mcq-editor-option-1"), { target: { value: "B" } });
    fireEvent.click(screen.getByTestId("mcq-editor-correct-0"));
    fireEvent.click(screen.getByTestId("inline-question-save"));
    await waitFor(() => expect(addInlineQuestion).toHaveBeenCalled());
    expect(screen.getByRole("button", { name: "Ngân hàng" })).toBeDisabled();
    expect(screen.getByTestId("quiz-detail-edit")).toBeDisabled();
    finish(detail({ version: 4 }));
    await waitFor(() => expect(screen.queryByTestId("mcq-editor-stem")).not.toBeInTheDocument());
  });

  it("authors and edits inside the question section without a dialog", async () => {
    vi.mocked(getQuiz).mockResolvedValue({ ...detail(), items: [{ ...detail().items[0]!, inline: true }] });
    render(<QuizDetailPage id="z1" />);
    fireEvent.click(await screen.findByTestId("quiz-item-edit-0"));
    const card = screen.getByTestId("quiz-item");
    expect(within(card).getByTestId("mcq-editor-stem")).toHaveValue(QUESTION.stem);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Hủy soạn câu" }));
    fireEvent.click(screen.getByLabelText("Thêm câu hỏi"));
    fireEvent.click(screen.getByTestId("quiz-detail-add-inline"));
    const section = screen.getByRole("heading", { name: "Câu hỏi" }).closest("section")!;
    expect(within(section).getByTestId("mcq-editor-stem")).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("keeps an unsaved inline question when changing modes is canceled", async () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
    vi.mocked(getQuiz).mockResolvedValue(detail());
    render(<QuizDetailPage id="z1" />);
    fireEvent.click(await screen.findByLabelText("Thêm câu hỏi"));
    fireEvent.click(screen.getByTestId("quiz-detail-add-inline"));
    fireEvent.change(screen.getByTestId("mcq-editor-stem"), { target: { value: "Câu đang soạn" } });
    fireEvent.click(screen.getByRole("button", { name: "Ngân hàng" }));
    expect(confirm).toHaveBeenCalled();
    expect(screen.getByTestId("mcq-editor-stem")).toHaveValue("Câu đang soạn");
    const leaving = new Event("beforeunload", { cancelable: true });
    expect(window.dispatchEvent(leaving)).toBe(false);
    confirm.mockReturnValue(true);
    fireEvent.click(screen.getByRole("button", { name: "Hủy soạn câu" }));
    expect(screen.queryByTestId("mcq-editor-stem")).not.toBeInTheDocument();
    confirm.mockRestore();
  });

  it("keeps the inline form and shows the error when saving fails", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail());
    vi.mocked(addInlineQuestion).mockRejectedValue(new Error("offline"));
    render(<QuizDetailPage id="z1" />);
    fireEvent.click(await screen.findByLabelText("Thêm câu hỏi"));
    fireEvent.click(screen.getByTestId("quiz-detail-add-inline"));
    fireEvent.change(screen.getByTestId("mcq-editor-stem"), { target: { value: "Câu hỏi mới" } });
    fireEvent.change(screen.getByTestId("mcq-editor-option-0"), { target: { value: "A" } });
    fireEvent.change(screen.getByTestId("mcq-editor-option-1"), { target: { value: "B" } });
    fireEvent.click(screen.getByTestId("mcq-editor-correct-0"));
    fireEvent.click(screen.getByTestId("inline-question-save"));
    await waitFor(() => expect(addInlineQuestion).toHaveBeenCalledWith("z1", expect.objectContaining({ stem: "Câu hỏi mới" }), 3));
    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(screen.getByTestId("mcq-editor-stem")).toHaveValue("Câu hỏi mới");
    expect(screen.getByTestId("inline-question-save")).toBeEnabled();
  });

  it("lưu điểm câu kèm version của quiz", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail());
    vi.mocked(saveQuizItems).mockResolvedValue(detail({ version: 4 }));
    render(<QuizDetailPage id="z1" />);

    fireEvent.change(await screen.findByTestId("quiz-item-points-0"), { target: { value: "2,5" } });
    expect(screen.getByTestId("quiz-detail-publish")).toBeDisabled();
    fireEvent.click(screen.getByTestId("quiz-detail-save"));

    await waitFor(() => expect(saveQuizItems).toHaveBeenCalledWith("z1", [{ questionId: "q1", points: 2.5 }], 3));
    await waitFor(() => expect(screen.queryByTestId("quiz-detail-save")).not.toBeInTheDocument());
  });

  it("điểm sai thì báo đúng lỗi, không gọi API kể cả khi cấu hình cũng đổi", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail());
    render(<QuizDetailPage id="z1" />);

    fireEvent.change(await screen.findByTestId("quiz-item-points-0"), { target: { value: "0" } });
    fireEvent.click(screen.getByRole("switch", { name: "Đảo thứ tự câu" }));
    fireEvent.click(screen.getByTestId("quiz-detail-save"));

    expect(await screen.findByRole("alert")).toHaveTextContent("Điểm mỗi câu phải lớn hơn 0, tối đa 2 chữ số thập phân.");
    expect(screen.queryByText(/Hệ thống tạm bận/)).not.toBeInTheDocument();
    expect(updateQuizDetails).not.toHaveBeenCalled();
    expect(saveQuizItems).not.toHaveBeenCalled();
  });

  it("phát hành lỗi thì hiện danh sách cần sửa; xung đột thì tải lại", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail());
    vi.mocked(publishQuiz).mockRejectedValue(new ApiError({ status: 400, code: "QUIZ_PUBLISH_FAILED",
      violations: [{ field: "items[1]", code: "QUESTION_NOT_ACTIVE", message: "Câu 1 lấy từ ngân hàng đã ngưng dùng" }] } as never));
    render(<QuizDetailPage id="z1" />);

    fireEvent.click(await screen.findByTestId("quiz-detail-publish"));
    fireEvent.click(await screen.findByTestId("quiz-detail-publish-confirm"));
    expect(await screen.findByText("Câu 1 lấy từ ngân hàng đã ngưng dùng")).toBeInTheDocument();

    vi.mocked(publishQuiz).mockRejectedValue(new ApiError({ status: 409, code: "QUIZ_VERSION_CONFLICT" }));
    fireEvent.click(screen.getByTestId("quiz-detail-publish"));
    fireEvent.click(await screen.findByTestId("quiz-detail-publish-confirm"));
    await waitFor(() => expect(getQuiz).toHaveBeenCalledTimes(2));
  });

  it("thêm câu từ ngân hàng sau khi phát hành lỗi thì xóa danh sách lỗi cũ", async () => {
    vi.mocked(getQuiz).mockResolvedValue({ ...detail(), items: [], maxScore: 0 });
    vi.mocked(publishQuiz).mockRejectedValue(new ApiError({ status: 400, code: "QUIZ_PUBLISH_FAILED",
      violations: [{ field: "items", code: "ITEMS_EMPTY", message: "Quiz cần ít nhất 1 câu" }] } as never));
    vi.mocked(getBankQuestionsForQuiz).mockResolvedValue({ items: [{ id: "q1", lineageId: "q1", versionNo: 2, type: "MCQ_SINGLE",
      stem: "Thủ đô của Việt Nam?", difficulty: "EASY", tags: [], status: "ACTIVE", hasDraft: false, updatedAt: "2026-10-20T08:00:00Z", version: 1 }],
      total: 1, page: 0, size: 50 });
    vi.mocked(saveQuizItems).mockResolvedValue(detail({ version: 4 }));
    render(<QuizDetailPage id="z1" />);

    fireEvent.click(await screen.findByTestId("quiz-detail-publish"));
    fireEvent.click(await screen.findByTestId("quiz-detail-publish-confirm"));
    expect(await screen.findByTestId("quiz-detail-violations")).toHaveTextContent("Quiz cần ít nhất 1 câu");

    fireEvent.click(screen.getByTestId("quiz-detail-add-bank"));
    fireEvent.click(await screen.findByLabelText("Chọn câu Thủ đô của Việt Nam?"));
    fireEvent.click(screen.getByTestId("bank-picker-add"));

    await waitFor(() => expect(saveQuizItems).toHaveBeenCalled());
    await waitFor(() => expect(screen.queryByTestId("quiz-detail-violations")).not.toBeInTheDocument());
  });

  it("quiz nháp phát hành thẳng sau khi xác nhận, không còn bước duyệt", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail());
    vi.mocked(publishQuiz).mockResolvedValue(detail({ status: "OPEN", publishedAt: "2026-10-20T09:00:00Z", version: 4 }));
    render(<QuizDetailPage id="z1" />);
    fireEvent.click(await screen.findByTestId("quiz-detail-publish"));
    expect(await screen.findByTestId("quiz-detail-publish-summary")).toHaveTextContent("Quiz có 1 câu");
    expect(publishQuiz).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("quiz-detail-publish-confirm"));
    expect(await screen.findByText("Đang mở")).toBeInTheDocument();
    expect(publishQuiz).toHaveBeenCalledWith("z1", 3);
    expect(screen.getByTestId("quiz-detail-edit")).toBeInTheDocument();
    expect(screen.queryByTestId("quiz-detail-save")).not.toBeInTheDocument();
  });

  it("sinh đề: lưu quy tắc theo thẻ, ẩn danh sách câu, xem thử đề mẫu", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail());
    const generated: QuizDetail = { ...detail({ paperMode: "GENERATED", version: 4 }), items: [], maxScore: 2,
      drawRules: [{ position: 1, tag: "Chương 1", difficulty: null, questionCount: 4, pointsEach: 0.5 }] };
    vi.mocked(saveDrawRules).mockResolvedValue(generated);
    vi.mocked(previewPaper).mockResolvedValue({ items: [{ rulePosition: 1, points: 0.5, question: QUESTION }], maxScore: 2,
      shortages: [{ position: 1, tag: "Chương 1", difficulty: null, required: 4, available: 1 }] });
    render(<QuizDetailPage id="z1" />);

    fireEvent.change(await screen.findByTestId("draw-rules-mode"), { target: { value: "GENERATED" } });
    fireEvent.click(screen.getByTestId("draw-rule-add"));
    fireEvent.change(screen.getByTestId("draw-rule-tag-0"), { target: { value: "Chương 1" } });
    expect(await screen.findByText("Kho có 12 câu đang dùng mang thẻ này")).toBeInTheDocument();
    fireEvent.change(screen.getByTestId("draw-rule-count-0"), { target: { value: "4" } });
    fireEvent.change(screen.getByTestId("draw-rule-points-0"), { target: { value: "0,5" } });
    expect(screen.getByTestId("draw-rules-total")).toHaveTextContent("4 câu · 2 điểm");
    fireEvent.click(screen.getByTestId("draw-rules-save"));

    await waitFor(() => expect(saveDrawRules).toHaveBeenCalledWith("z1", "GENERATED",
      [{ tag: "Chương 1", difficulty: null, questionCount: 4, pointsEach: 0.5 }], 3));
    await waitFor(() => expect(screen.queryByTestId("quiz-item")).not.toBeInTheDocument());
    fireEvent.click(await screen.findByTestId("draw-rules-preview"));
    expect(await screen.findByTestId("paper-preview")).toHaveTextContent("cần 4 câu, kho chỉ cấp được 1 câu");
    fireEvent.click(screen.getAllByRole("button", { name: "Đóng" })[0]!);
    fireEvent.click(screen.getByTestId("quiz-detail-publish"));
    expect(await screen.findByTestId("quiz-detail-publish-summary")).toHaveTextContent("một đề mới gồm 4 câu");
  });

  it("hiện phần lượt làm của sinh viên theo lớp của quiz; quiz của môn mở từ lớp thì theo lớp đó", async () => {
    const seen: { quizId: string; classId?: string }[] = [];
    quizDetailSections.register({ key: "test-attempts", order: 1, component: (props) => { seen.push(props); return null; } });
    vi.mocked(getQuiz).mockResolvedValue(detail());
    const { unmount } = render(<QuizDetailPage id="z1" />);
    await screen.findByTestId("quiz-detail-publish");
    expect(seen.at(-1)).toEqual({ quizId: "z1", classId: "c1" });
    unmount();
    vi.mocked(getQuiz).mockResolvedValue(detail({ scope: "SUBJECT", classId: null, status: "OPEN" }, false));
    render(<QuizDetailPage id="z1" classId="c9" />);
    await screen.findByText("Đang mở");
    expect(seen.at(-1)).toEqual({ quizId: "z1", classId: "c9" });
  });

  it("giảng viên đề xuất câu riêng vào ngân hàng; quiz của môn thì không có nút này", async () => {
    const inline = { ...detail(), items: [{ position: 1, points: 2, inline: true, question: QUESTION }] };
    vi.mocked(getQuiz).mockResolvedValue(inline);
    vi.mocked(proposeQuestion).mockResolvedValue({ question: { ...QUESTION, status: "PENDING" }, review: null, similar: [] });
    const { unmount } = render(<QuizDetailPage id="z1" />);
    fireEvent.click(await screen.findByTestId("quiz-item-propose-0"));
    await waitFor(() => expect(proposeQuestion).toHaveBeenCalledWith("c1", QUESTION));
    unmount();

    vi.mocked(getQuiz).mockResolvedValue({ ...inline, quiz: { ...INFO, scope: "SUBJECT", classId: null } });
    render(<QuizDetailPage id="z1" />);
    expect(await screen.findByTestId("quiz-item-edit-0")).toBeInTheDocument();
    expect(screen.queryByTestId("quiz-item-propose-0")).not.toBeInTheDocument();
  });

  it("cấu hình trừ điểm cần bật ghi nhận hoạt động và gửi đúng dữ liệu", async () => {
    const monitored = detail({ activityMonitoringEnabled: true, activityNotice: "Có ghi nhận" });
    vi.mocked(getQuiz).mockResolvedValue(monitored);
    vi.mocked(updateQuizDetails).mockResolvedValue(monitored);
    render(<QuizDetailPage id="z1" />);
    fireEvent.click(await screen.findByRole("switch", { name: "Gợi ý trừ điểm khi rời trang" }));
    fireEvent.change(screen.getByTestId("quiz-settings-free-leaves"), { target: { value: "1" } });
    fireEvent.change(screen.getByTestId("quiz-settings-points-per-leave"), { target: { value: "0,5" } });
    fireEvent.change(screen.getByTestId("quiz-settings-max-penalty"), { target: { value: "3" } });
    fireEvent.click(screen.getByTestId("quiz-detail-save"));
    await waitFor(() => expect(updateQuizDetails).toHaveBeenCalled());
    expect(vi.mocked(updateQuizDetails).mock.calls[0]?.[1].settings.leavePenalty)
      .toEqual({ freeLeaves: 1, pointsPerLeave: 0.5, maxPenalty: 3, minAwaySeconds: 5 });
  });

  it("quiz đã lưu trữ mở lại được, không sửa hay lưu trữ lần nữa", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail({ status: "RETIRED", publishedAt: "2026-10-20T09:00:00Z", retiredAt: "2026-10-21T09:00:00Z" }));
    vi.mocked(reopenQuiz).mockResolvedValue(detail({ status: "OPEN", publishedAt: "2026-10-20T09:00:00Z", version: 4 }));
    render(<QuizDetailPage id="z1" />);
    const reopen = await screen.findByTestId("quiz-detail-reopen");
    expect(screen.queryByTestId("quiz-detail-edit")).not.toBeInTheDocument();
    // Sau khi mở lại, trang tải lại quiz từ máy chủ.
    vi.mocked(getQuiz).mockResolvedValue(detail({ status: "OPEN", publishedAt: "2026-10-20T09:00:00Z", version: 4 }));
    fireEvent.click(reopen);
    await waitFor(() => expect(reopenQuiz).toHaveBeenCalledWith("z1", 3));
    expect(await screen.findByTestId("quiz-detail-edit")).toBeInTheDocument();
  });

  it("giảng viên xem quiz của môn từ lớp thì chỉ xem", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail({ scope: "SUBJECT", classId: null, status: "OPEN" }, false));
    render(<QuizDetailPage id="z1" classId="c1" />);
    expect(await screen.findByTestId("quiz-item-points-0")).toBeDisabled();
    expect(getQuiz).toHaveBeenCalledWith("z1", "c1");
    expect(screen.queryByTestId("quiz-detail-edit")).not.toBeInTheDocument();
    expect(screen.queryByTestId("quiz-detail-archive")).not.toBeInTheDocument();
    expect(screen.queryByTestId("quiz-detail-more")).not.toBeInTheDocument();
    expect(screen.getByTestId("quiz-item-points-0")).toBeDisabled();
  });
});

describe("BankQuestionPicker", () => {
  it("chọn tay rồi thêm các câu đã chọn", async () => {
    vi.mocked(getBankQuestionsForQuiz).mockResolvedValue({ items: [{ id: "b1", lineageId: "b1", versionNo: 1, type: "MCQ_SINGLE",
      stem: "Câu ngân hàng", difficulty: "EASY", tags: [], status: "ACTIVE", hasDraft: false, updatedAt: "2026-10-20T08:00:00Z", version: 1 }], total: 1, page: 0, size: 50 });
    const onAdd = vi.fn().mockResolvedValue(undefined);
    render(<BankQuestionPicker quizId="z1" freeSlots={199} onClose={vi.fn()} onAdd={onAdd} />);

    fireEvent.click(await screen.findByLabelText("Chọn câu Câu ngân hàng"));
    fireEvent.click(screen.getByTestId("bank-picker-add"));
    await waitFor(() => expect(onAdd).toHaveBeenCalledWith(["b1"]));
  });

  it("bốc ngẫu nhiên hiện số câu phù hợp, bỏ từng câu trước khi lưu", async () => {
    vi.mocked(getBankQuestionsForQuiz).mockResolvedValue({ items: [], total: 0, page: 0, size: 50 });
    vi.mocked(previewRandomItems).mockResolvedValue({ questions: [QUESTION, { ...QUESTION, id: "q2", stem: "Câu hai" }], matching: 7, freeSlots: 199 });
    const onAdd = vi.fn().mockResolvedValue(undefined);
    render(<BankQuestionPicker quizId="z1" freeSlots={199} onClose={vi.fn()} onAdd={onAdd} />);

    fireEvent.click(screen.getByTestId("bank-picker-tab-random"));
    fireEvent.change(screen.getByTestId("bank-picker-count"), { target: { value: "2" } });
    fireEvent.click(screen.getByTestId("bank-picker-draw"));
    expect(await screen.findByText(/Có 7 câu phù hợp/)).toBeInTheDocument();
    expect(previewRandomItems).toHaveBeenCalledWith("z1", 2, null, null);
    fireEvent.click(screen.getAllByText("Bỏ")[0]!);
    fireEvent.click(screen.getByTestId("bank-picker-add"));
    await waitFor(() => expect(onAdd).toHaveBeenCalledWith(["q2"]));
  });
});

describe("ClassQuizzesSection", () => {
  it("quiz của môn mở chỉ xem kèm lớp", async () => {
    vi.mocked(getClassQuizzes).mockResolvedValue([row(), row({ id: "z2", scope: "SUBJECT", classId: null, status: "OPEN", readOnly: true, title: "Quiz ôn tập môn" })]);
    render(<ClassQuizzesSection classId="c1" />);
    const rows = await screen.findAllByTestId("class-quizzes-row");
    expect(screen.getByText("Của môn")).toBeInTheDocument();
    fireEvent.click(rows[1]!);
    expect(push).toHaveBeenCalledWith("/quizzes/z2?classId=c1");
    fireEvent.click(rows[0]!);
    expect(push).toHaveBeenLastCalledWith("/quizzes/z1");
  });
});

describe("QuizSettingsPanel", () => {
  it("ghi nhận hoạt động luôn bật được (dữ liệu giữ cố định 1 ngày) và cần lời báo", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail());
    vi.mocked(updateQuizDetails).mockResolvedValue(detail({ activityMonitoringEnabled: true, activityNotice: "Ghi lại" }));
    render(<QuizDetailPage id="z1" />);
    const toggle = await screen.findByRole("switch", { name: "Ghi nhận hoạt động" });
    expect(toggle).toBeEnabled();
    fireEvent.click(toggle);
    fireEvent.click(screen.getByTestId("quiz-detail-save"));
    expect(await screen.findByText("Cần lời báo cho sinh viên khi bật ghi nhận hoạt động")).toBeInTheDocument();
    expect(screen.getByText("Cấu hình chưa hợp lệ.")).toBeInTheDocument();
    expect(screen.queryByText(/Hệ thống tạm bận/)).not.toBeInTheDocument();
    expect(updateQuizDetails).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("quiz-settings-activity-notice"), { target: { value: "Ghi lại" } });
    fireEvent.click(screen.getByTestId("quiz-detail-save"));
    await waitFor(() => expect(updateQuizDetails).toHaveBeenCalledWith("z1",
      expect.objectContaining({ activityMonitoringEnabled: true, activityNotice: "Ghi lại" }), 3));
  });

  it("các công tắc có tên truy cập", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail());
    vi.mocked(saveQuizItems).mockResolvedValue(detail());
    render(<QuizDetailPage id="z1" />);
    for (const name of ["Hiện điểm sau khi nộp", "Đảo thứ tự câu", "Đảo thứ tự lựa chọn", "Ghi nhận hoạt động"]) {
      expect(await screen.findByRole("switch", { name })).toBeInTheDocument();
    }
  });
});

describe("settingsInput (BR-U10-15)", () => {
  it("kiểm thời gian, số lượt và chuyển sang dữ liệu gửi API", () => {
    const form = settingsFormOf(INFO);
    expect(settingsInput({ ...form, timeLimit: "301", maxAttempts: "0" }).errors).toMatchObject({ timeLimit: expect.any(String), maxAttempts: expect.any(String) });
    const { input } = settingsInput({ ...form, timeLimit: "45", maxAttempts: "", instructions: "  " });
    expect(input?.settings).toMatchObject({ timeLimitMinutes: 45, maxAttempts: null });
    expect(input?.instructions).toBeNull();
  });
});

beforeEach(() => { window.history.replaceState(null, "", "/manager?subjectId=s1"); });
