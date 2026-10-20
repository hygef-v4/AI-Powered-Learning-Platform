import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { getAttempt, getClassAttempts, getPracticeHistory, getPracticeQuiz, getResult, saveAnswers, startAttempt, submitAttempt } from "./api";
import { QuizPracticeDetailPage } from "./QuizPracticeDetailPage";
import { QuizPracticeHistoryPage } from "./QuizPracticeHistoryPage";
import { QuizResultPage } from "./QuizResultPage";
import { AUTOSAVE_DELAY_MS, QuizTakingPage } from "./QuizTakingPage";
import { QuizAttemptsSection } from "./TeacherQuizSections";
import type { PracticeQuizDetail, ResultView, TakingView } from "./types";

vi.mock("./api", () => ({
  getPracticeHistory: vi.fn(),
  getPracticeQuiz: vi.fn(),
  startAttempt: vi.fn(),
  getAttempt: vi.fn(),
  saveAnswers: vi.fn(),
  submitAttempt: vi.fn(),
  getResult: vi.fn(),
  getClassAttempts: vi.fn(),
  getTeacherAttempt: vi.fn(),
}));

const push = vi.fn();
const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push, replace }) }));

const TAKING: TakingView = {
  attemptId: "a1", quizId: "z1", classId: "c1", title: "Luyện tập", attemptNo: 1, startedAt: "2026-10-20T08:00:00Z",
  deadlineAt: null, serverTime: new Date().toISOString(), answerVersion: 0, answers: {},
  questions: [
    { questionId: "q1", type: "MCQ_SINGLE", stem: "Thủ đô?", points: 1, options: [{ optionId: "1", text: "Hà Nội" }, { optionId: "2", text: "Huế" }] },
    { questionId: "q2", type: "MCQ_MULTI", stem: "Số chẵn?", points: 2, options: [{ optionId: "1", text: "2" }, { optionId: "2", text: "3" }] },
  ],
};
const DETAIL: PracticeQuizDetail = {
  quizId: "z1", classId: "c1", classCode: "SE1801", className: "Lớp", title: "Luyện tập", instructions: null, status: "OPEN",
  questionCount: 2, maxScore: 3, timeLimitMinutes: null, attemptsUsed: 2, maxAttempts: 2, attemptsLeft: 0, bestScore: 2,
  showScoreAfterSubmit: true, showCorrectAnswers: true, activityNotice: null, referenceMaterialIds: [],
  attempts: [{ attemptId: "a2", attemptNo: 2, status: "SUBMITTED", startedAt: "2026-10-20T08:00:00Z", submittedAt: "2026-10-20T08:05:00Z", score: 2, maxScore: 3 }],
  inProgressAttemptId: null, canStart: false, blockedReason: "Bạn đã dùng hết 2/2 lượt làm quiz này.",
};
const RESULT: ResultView = {
  attemptId: "a1", quizId: "z1", classId: "c1", title: "Luyện tập", attemptNo: 1, startedAt: "2026-10-20T08:00:00Z",
  submittedAt: "2026-10-20T08:05:00Z", submitMode: "MANUAL", showScore: false, showAnswers: false, score: null, maxScore: null,
  correctCount: 1, wrongCount: 1, blankCount: 0, canRetry: true,
  questions: [{ questionId: "q1", type: "MCQ_SINGLE", stem: "Thủ đô?", options: [{ optionId: "1", text: "Hà Nội" }], selectedOptionIds: ["1"],
    correct: true, points: null, maxPoints: null, correctOptionIds: null, explanation: null }],
};

afterEach(() => {
  vi.clearAllMocks();
  vi.useRealTimers();
});

describe("QuizPracticeHistoryPage (UC 49)", () => {
  it("quiz chưa làm có nút Bắt đầu mở lượt mới", async () => {
    vi.mocked(getPracticeHistory).mockResolvedValue([{ quizId: "z1", classId: "c1", classCode: "SE1801", className: "Lớp",
      title: "Luyện tập", status: "OPEN", timeLimitMinutes: null, attemptsUsed: 0, maxAttempts: 2, bestScore: null, lastScore: null,
      maxScore: 3, lastAttemptAt: null, inProgressAttemptId: null, canStart: true }]);
    vi.mocked(startAttempt).mockResolvedValue(TAKING);
    render(<QuizPracticeHistoryPage />);
    expect(await screen.findByText("Chưa làm")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("practice-history-start"));
    await waitFor(() => expect(push).toHaveBeenCalledWith("/quiz-attempts/a1"));
    expect(startAttempt).toHaveBeenCalledWith("z1", "c1");
  });
});

describe("QuizPracticeDetailPage (UC 50)", () => {
  it("hết lượt thì ẩn nút và nêu lý do", async () => {
    vi.mocked(getPracticeQuiz).mockResolvedValue(DETAIL);
    render(<QuizPracticeDetailPage quizId="z1" classId="c1" />);
    expect(await screen.findByText("Bạn đã dùng hết 2/2 lượt làm quiz này.")).toBeInTheDocument();
    expect(screen.queryByTestId("practice-detail-start")).not.toBeInTheDocument();
    expect(screen.getByTestId("practice-detail-attempts")).toHaveTextContent("2 / 2 lượt");
  });
});

describe("QuizTakingPage (UC 51)", () => {
  beforeEach(() => {
    vi.mocked(getAttempt).mockResolvedValue(TAKING);
  });

  it("tự lưu 2 giây sau lần chọn cuối kèm answerVersion", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.mocked(saveAnswers).mockResolvedValue({ answerVersion: 1, savedAt: "2026-10-20T08:01:00Z" });
    render(<QuizTakingPage attemptId="a1" />);
    fireEvent.click(await screen.findByTestId("quiz-taking-option-1"));
    expect(screen.getByTestId("quiz-taking-save-state")).toHaveTextContent("Chưa lưu");
    expect(saveAnswers).not.toHaveBeenCalled();

    await act(async () => {
      vi.advanceTimersByTime(AUTOSAVE_DELAY_MS + 10);
    });
    await waitFor(() => expect(saveAnswers).toHaveBeenCalledWith("a1", { q1: ["1"] }, 0));
    expect(await screen.findByText("Đã tự động lưu")).toBeInTheDocument();
  });

  it("xung đột answerVersion thì tải lại lượt", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.mocked(saveAnswers).mockRejectedValue(new ApiError({ status: 409, code: "ANSWER_VERSION_CONFLICT" }));
    render(<QuizTakingPage attemptId="a1" />);
    fireEvent.click(await screen.findByTestId("quiz-taking-option-2"));
    await act(async () => {
      vi.advanceTimersByTime(AUTOSAVE_DELAY_MS + 10);
    });
    await waitFor(() => expect(getAttempt).toHaveBeenCalledTimes(2));
  });

  it("popup nộp đếm câu chưa trả lời rồi nộp và chuyển sang kết quả", async () => {
    vi.mocked(saveAnswers).mockResolvedValue({ answerVersion: 1, savedAt: "2026-10-20T08:01:00Z" });
    vi.mocked(submitAttempt).mockResolvedValue({ ...RESULT, showScore: true });
    render(<QuizTakingPage attemptId="a1" />);
    fireEvent.click(await screen.findByTestId("quiz-taking-option-1"));
    fireEvent.click(screen.getByTestId("quiz-taking-submit"));
    expect(screen.getByTestId("quiz-taking-confirm")).toHaveTextContent("Còn 1 câu chưa trả lời: câu 2");

    fireEvent.click(screen.getByTestId("quiz-taking-confirm-submit"));
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/quiz-attempts/a1/result"));
    expect(saveAnswers).toHaveBeenCalledWith("a1", { q1: ["1"] }, 0);
    expect(submitAttempt).toHaveBeenCalledWith("a1");
  });

  it("hết giờ thì tự nộp", async () => {
    vi.mocked(getAttempt).mockResolvedValue({ ...TAKING, deadlineAt: new Date(Date.now() + 1500).toISOString(), serverTime: new Date().toISOString() });
    vi.mocked(submitAttempt).mockResolvedValue(RESULT);
    render(<QuizTakingPage attemptId="a1" />);
    expect(await screen.findByTestId("quiz-taking-timer")).toBeInTheDocument();
    await waitFor(() => expect(submitAttempt).toHaveBeenCalledWith("a1"), { timeout: 4000 });
    expect(replace).toHaveBeenCalledWith("/quiz-attempts/a1/result");
  });

  it("lượt đã nộp thì chuyển sang kết quả", async () => {
    vi.mocked(getAttempt).mockRejectedValue(new ApiError({ status: 409, code: "ATTEMPT_SUBMITTED" }));
    render(<QuizTakingPage attemptId="a1" />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/quiz-attempts/a1/result"));
  });
});

describe("QuizResultPage (UC 52)", () => {
  it("quiz tắt hiện điểm và đáp án thì chỉ báo đã nộp", async () => {
    vi.mocked(getResult).mockResolvedValue(RESULT);
    render(<QuizResultPage attemptId="a1" />);
    expect(await screen.findByTestId("result-hidden")).toBeInTheDocument();
    expect(screen.queryByTestId("result-score")).not.toBeInTheDocument();
    expect(screen.getByText("Quiz này không hiện đáp án sau khi nộp.")).toBeInTheDocument();
    expect(screen.getByTestId("result-retry")).toBeInTheDocument();
  });

  it("quiz hiện đáp án thì có nhãn Đáp án đúng và Bạn chọn", async () => {
    vi.mocked(getResult).mockResolvedValue({ ...RESULT, showScore: true, showAnswers: true, score: 1, maxScore: 3, canRetry: false,
      questions: [{ ...RESULT.questions[0]!, selectedOptionIds: ["1"], correctOptionIds: ["1"], explanation: "Vì vậy" }] });
    render(<QuizResultPage attemptId="a1" />);
    expect(await screen.findByTestId("result-score")).toHaveTextContent("1 / 3");
    expect(screen.getByText("Đáp án đúng")).toBeInTheDocument();
    expect(screen.getByText("Bạn chọn")).toBeInTheDocument();
    expect(screen.queryByTestId("result-retry")).not.toBeInTheDocument();
  });
});

describe("QuizAttemptsSection (UC 42)", () => {
  it("giảng viên xem số sinh viên đã làm; không có lớp thì không hiện", async () => {
    vi.mocked(getClassAttempts).mockResolvedValue({ quizId: "z1", classId: "c1", maxAttempts: 2, studentsAttempted: 1, totalStudents: 30,
      students: [{ accountId: "s1", displayName: "Sinh viên A", email: "a@fpt.edu.vn", attempts: 1, bestScore: 2, lastScore: 2,
        lastSubmittedAt: "2026-10-20T08:05:00Z", rows: [] }] });
    const { container } = render(<QuizAttemptsSection quizId="z1" />);
    expect(container).toBeEmptyDOMElement();
    render(<QuizAttemptsSection quizId="z1" classId="c1" />);
    expect(await screen.findByTestId("quiz-attempts-summary")).toHaveTextContent("1 / 30 sinh viên đã làm");
    expect(getClassAttempts).toHaveBeenCalledWith("c1", "z1");
  });
});
