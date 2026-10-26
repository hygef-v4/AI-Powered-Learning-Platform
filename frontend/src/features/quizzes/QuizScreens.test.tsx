import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import {
  createSubjectQuiz, getBankQuestionsForQuiz, getClassQuizzes, getManagedSubjects, getQuiz, getSubjectQuizzes, previewRandomItems,
  publishQuiz, reviewQuiz, saveQuizItems, updateQuizDetails,
} from "./api";
import { BankQuestionPicker } from "./BankQuestionPicker";
import { ClassQuizzesSection } from "./ClassQuizzesSection";
import { settingsFormOf, settingsInput } from "./QuizDetailParts";
import { QuizDetailPage } from "./QuizDetailPage";
import { SubjectQuizListPage } from "./SubjectQuizListPage";
import type { QuestionView, QuizDetail, QuizInfo, QuizRow } from "./types";

vi.mock("./api", () => ({
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
  reviewQuiz: vi.fn(),
  publishQuiz: vi.fn(),
  retireQuiz: vi.fn(),
  cloneQuiz: vi.fn(),
  createQuizVersion: vi.fn(),
  deleteQuiz: vi.fn(),
  getQuizMaterials: vi.fn(() => Promise.resolve([])),
  getQuizDrafts: vi.fn(() => Promise.resolve([])),
}));

const push = vi.fn();
const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push, replace }), useSearchParams: () => new URLSearchParams("subjectId=s1") }));

const QUESTION: QuestionView = {
  id: "q1", lineageId: "q1", versionNo: 2, subjectId: "s1", type: "MCQ_SINGLE", stem: "Thủ đô của Việt Nam?",
  options: [{ id: "1", text: "Hà Nội" }, { id: "2", text: "Huế" }], correctOptionIds: ["1"], explanation: null,
  defaultPoints: 1, difficulty: "EASY", tags: [], status: "ACTIVE", activatedAt: "2026-10-20T08:00:00Z",
  createdBy: "u1", createdAt: "2026-10-20T08:00:00Z", updatedAt: "2026-10-20T08:00:00Z", version: 1,
};
const INFO: QuizInfo = {
  id: "z1", scope: "CLASS", classId: "c1", subjectId: "s1", sourceQuizId: null, origin: "MANUAL", versionNo: 1, title: "Luyện tập",
  instructions: null, settings: { shuffleQuestions: false, shuffleOptions: false, timeLimitMinutes: null, maxAttempts: 1,
    showScoreAfterSubmit: true, showCorrectAnswers: false },
  activityMonitoringEnabled: false, activityNotice: null, referenceMaterialIds: [], status: "DRAFT", publishedAt: null, retiredAt: null,
  retiredReason: null, createdAt: "2026-10-20T08:00:00Z", updatedAt: "2026-10-20T08:00:00Z", version: 3,
};
const detail = (quiz: Partial<QuizInfo> = {}, canManage = true): QuizDetail => ({
  quiz: { ...INFO, ...quiz }, items: [{ position: 1, points: 2, inline: false, question: QUESTION }], maxScore: 2, canManage,
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

describe("QuizDetailPage", () => {
  it("lưu điểm câu kèm version của quiz", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail());
    vi.mocked(saveQuizItems).mockResolvedValue(detail({ version: 4 }));
    render(<QuizDetailPage id="z1" />);

    fireEvent.change(await screen.findByTestId("quiz-item-points-0"), { target: { value: "2,5" } });
    expect(screen.getByTestId("quiz-detail-review")).toBeDisabled();
    fireEvent.click(screen.getByTestId("quiz-detail-save"));

    await waitFor(() => expect(saveQuizItems).toHaveBeenCalledWith("z1", [{ questionId: "q1", points: 2.5 }], 3));
    expect(await screen.findByText("Đã lưu quiz.")).toBeInTheDocument();
  });

  it("điểm sai thì báo đúng lỗi, không gọi API kể cả khi cấu hình cũng đổi", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail());
    render(<QuizDetailPage id="z1" />);

    fireEvent.change(await screen.findByTestId("quiz-item-points-0"), { target: { value: "0" } });
    fireEvent.change(screen.getByLabelText(/Tên quiz/), { target: { value: "Luyện tập 2" } });
    fireEvent.click(screen.getByTestId("quiz-detail-save"));

    expect(await screen.findByRole("alert")).toHaveTextContent("Điểm mỗi câu phải lớn hơn 0, tối đa 2 chữ số thập phân.");
    expect(screen.queryByText(/Hệ thống tạm bận/)).not.toBeInTheDocument();
    expect(updateQuizDetails).not.toHaveBeenCalled();
    expect(saveQuizItems).not.toHaveBeenCalled();
  });

  it("duyệt lỗi thì hiện danh sách cần sửa; xung đột thì tải lại", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail());
    vi.mocked(reviewQuiz).mockRejectedValue(new ApiError({ status: 400, code: "QUIZ_REVIEW_FAILED",
      violations: [{ field: "items[1]", code: "QUESTION_NOT_ACTIVE", message: "Câu 1 lấy từ ngân hàng đã ngưng dùng" }] } as never));
    render(<QuizDetailPage id="z1" />);

    fireEvent.click(await screen.findByTestId("quiz-detail-review"));
    expect(await screen.findByText("Câu 1 lấy từ ngân hàng đã ngưng dùng")).toBeInTheDocument();

    vi.mocked(reviewQuiz).mockRejectedValue(new ApiError({ status: 409, code: "QUIZ_VERSION_CONFLICT" }));
    fireEvent.click(screen.getByTestId("quiz-detail-review"));
    await waitFor(() => expect(getQuiz).toHaveBeenCalledTimes(2));
  });

  it("thêm câu từ ngân hàng sau khi duyệt lỗi thì xóa danh sách lỗi cũ", async () => {
    vi.mocked(getQuiz).mockResolvedValue({ ...detail(), items: [], maxScore: 0 });
    vi.mocked(reviewQuiz).mockRejectedValue(new ApiError({ status: 400, code: "QUIZ_REVIEW_FAILED",
      violations: [{ field: "items", code: "ITEMS_EMPTY", message: "Quiz cần ít nhất 1 câu" }] } as never));
    vi.mocked(getBankQuestionsForQuiz).mockResolvedValue({ items: [{ id: "q1", lineageId: "q1", versionNo: 2, type: "MCQ_SINGLE",
      stem: "Thủ đô của Việt Nam?", difficulty: "EASY", tags: [], status: "ACTIVE", hasDraft: false, updatedAt: "2026-10-20T08:00:00Z" }],
      total: 1, page: 0, size: 50 });
    vi.mocked(saveQuizItems).mockResolvedValue(detail({ version: 4 }));
    render(<QuizDetailPage id="z1" />);

    fireEvent.click(await screen.findByTestId("quiz-detail-review"));
    expect(await screen.findByTestId("quiz-detail-violations")).toHaveTextContent("Quiz cần ít nhất 1 câu");

    fireEvent.click(screen.getByTestId("quiz-detail-add-bank"));
    fireEvent.click(await screen.findByLabelText("Chọn câu Thủ đô của Việt Nam?"));
    fireEvent.click(screen.getByTestId("bank-picker-add"));

    expect(await screen.findByText("Đã thêm 1 câu từ ngân hàng.")).toBeInTheDocument();
    expect(screen.queryByTestId("quiz-detail-violations")).not.toBeInTheDocument();
  });

  it("quiz đã duyệt có nút phát hành", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail({ status: "REVIEWED" }));
    vi.mocked(publishQuiz).mockResolvedValue(detail({ status: "OPEN", publishedAt: "2026-10-20T09:00:00Z", version: 4 }));
    render(<QuizDetailPage id="z1" />);
    fireEvent.click(await screen.findByTestId("quiz-detail-publish"));
    expect(await screen.findByText("Đã phát hành quiz.")).toBeInTheDocument();
    expect(publishQuiz).toHaveBeenCalledWith("z1", 3);
    expect(screen.queryByTestId("quiz-detail-save")).not.toBeInTheDocument();
  });

  it("giảng viên xem quiz của môn từ lớp thì chỉ xem", async () => {
    vi.mocked(getQuiz).mockResolvedValue(detail({ scope: "SUBJECT", classId: null, status: "OPEN" }, false));
    render(<QuizDetailPage id="z1" classId="c1" />);
    expect(await screen.findByText("Quiz của môn: giảng viên lớp chỉ xem.")).toBeInTheDocument();
    expect(getQuiz).toHaveBeenCalledWith("z1", "c1");
    expect(screen.queryByTestId("quiz-detail-more")).not.toBeInTheDocument();
    expect(screen.getByTestId("quiz-item-points-0")).toBeDisabled();
  });
});

describe("BankQuestionPicker", () => {
  it("chọn tay rồi thêm các câu đã chọn", async () => {
    vi.mocked(getBankQuestionsForQuiz).mockResolvedValue({ items: [{ id: "b1", lineageId: "b1", versionNo: 1, type: "MCQ_SINGLE",
      stem: "Câu ngân hàng", difficulty: "EASY", tags: [], status: "ACTIVE", hasDraft: false, updatedAt: "2026-10-20T08:00:00Z" }], total: 1, page: 0, size: 50 });
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
    expect(await screen.findByText("Dữ liệu hoạt động được giữ 1 ngày rồi tự xóa.")).toBeInTheDocument();
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
