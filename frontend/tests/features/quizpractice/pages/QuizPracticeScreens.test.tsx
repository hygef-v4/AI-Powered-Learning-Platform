import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import {
  decideAttemptPenalty, getAttempt, getClassAttempts, getPracticeHistory, getPracticeQuiz, getResult, getTeacherAttempt, recordQuizActivity, saveAnswers,
  startAttempt, submitAttempt,
} from "@/features/quizpractice/api";
import { QuizPracticeDetailPage } from "@/features/quizpractice/pages/QuizPracticeDetailPage";
import { QuizPracticeHistoryPage } from "@/features/quizpractice/pages/QuizPracticeHistoryPage";
import { QuizResultPage } from "@/features/quizpractice/pages/QuizResultPage";
import { AUTOSAVE_DELAY_MS, QuizTakingPage } from "@/features/quizpractice/pages/QuizTakingPage";
import { QuizAttemptsSection } from "@/features/quizpractice/components/TeacherQuizSections";
import type { PracticeQuizDetail, ResultView, TakingView } from "@/features/quizpractice/model/types";

vi.mock("@/features/quizpractice/api", () => ({
  getPracticeHistory: vi.fn(),
  getPracticeQuiz: vi.fn(),
  startAttempt: vi.fn(),
  getAttempt: vi.fn(),
  saveAnswers: vi.fn(),
  submitAttempt: vi.fn(),
  getResult: vi.fn(),
  getClassAttempts: vi.fn(),
  getTeacherAttempt: vi.fn(),
  recordQuizActivity: vi.fn(() => Promise.resolve()),
  decideAttemptPenalty: vi.fn(),
}));

const push = vi.fn();
const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push, replace }), useSearchParams: () => new URLSearchParams(window.location.search) }));
vi.mock("@/features/identity/hooks/session", () => ({ useSession: () => ({ status: "authenticated", profile: { role: "STUDENT" } }) }));
vi.mock("@/features/materials/components/ModulePlace", () => ({ useModuleScope: () => null, useModulePlace: () => ({ reload: () => {} }), ModuleNeighbours: () => null }));

const TAKING: TakingView = {
  attemptId: "a1", quizId: "z1", classId: "c1", title: "Luyện tập", attemptNo: 1, startedAt: "2026-10-20T08:00:00Z",
  deadlineAt: null, serverTime: new Date().toISOString(), answerVersion: 0, answers: {}, activityMonitoringEnabled: false,
  activityNotice: null, leavePenalty: null,
  questions: [
    { questionId: "q1", type: "MCQ_SINGLE", stem: "Thủ đô?", points: 1, options: [{ optionId: "1", text: "Hà Nội" }, { optionId: "2", text: "Huế" }] },
    { questionId: "q2", type: "MCQ_MULTI", stem: "Số chẵn?", points: 2, options: [{ optionId: "1", text: "2" }, { optionId: "2", text: "3" }] },
  ],
};
const DETAIL: PracticeQuizDetail = {
  quizId: "z1", classId: "c1", classCode: "SE1801", subjectCode: "SWD392", title: "Luyện tập", instructions: null, status: "OPEN",
  questionCount: 2, maxScore: 3, timeLimitMinutes: null, attemptsUsed: 2, maxAttempts: 2, attemptsLeft: 0, bestScore: 2,
  showScoreAfterSubmit: true, showCorrectAnswers: true, activityNotice: null, leavePenalty: null,
  attempts: [{ attemptId: "a2", attemptNo: 2, status: "SUBMITTED", startedAt: "2026-10-20T08:00:00Z", submittedAt: "2026-10-20T08:05:00Z", score: 2, maxScore: 3, leaveCount: null, activitySignal: null, suggestedPenalty: null, penaltyDecision: null }],
  inProgressAttemptId: null, canStart: false, blockedReason: "Bạn đã dùng hết 2/2 lượt làm quiz này.",
};
const RESULT: ResultView = {
  attemptId: "a1", quizId: "z1", classId: "c1", title: "Luyện tập", attemptNo: 1, startedAt: "2026-10-20T08:00:00Z",
  submittedAt: "2026-10-20T08:05:00Z", submitMode: "MANUAL", showScore: false, showAnswers: false, score: null, maxScore: null,
  correctCount: 1, wrongCount: 1, blankCount: 0, canRetry: true, penalty: null,
  questions: [{ questionId: "q1", type: "MCQ_SINGLE", stem: "Thủ đô?", options: [{ optionId: "1", text: "Hà Nội" }], selectedOptionIds: ["1"],
    correct: true, points: null, maxPoints: null, correctOptionIds: null, explanation: null }],
};

afterEach(() => {
  vi.clearAllMocks();
  vi.useRealTimers();
});

describe("QuizPracticeHistoryPage (UC 49)", () => {
  it("quiz chưa làm có nút Bắt đầu mở lượt mới", async () => {
    vi.mocked(getPracticeHistory).mockResolvedValue([{ quizId: "z1", classId: "c1", classCode: "SE1801", subjectCode: "SWD392",
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
  it("hết lượt thì chỉ ẩn nút làm bài, không hiện cảnh báo", async () => {
    vi.mocked(getPracticeQuiz).mockResolvedValue(DETAIL);
    render(<QuizPracticeDetailPage quizId="z1" classId="c1" />);
    expect(await screen.findByTestId("practice-detail-info")).toBeInTheDocument();
    expect(screen.queryByText("Bạn đã dùng hết 2/2 lượt làm quiz này.")).not.toBeInTheDocument();
    expect(screen.queryByTestId("practice-detail-start")).not.toBeInTheDocument();
    expect(screen.queryByText("Practice")).not.toBeInTheDocument();
    // Thẻ Làm bài vẫn còn, ghi lượt đã dùng cạnh tiêu đề; bảng Kết quả không còn cột Thời gian.
    expect(screen.getByRole("heading", { name: "Làm bài" })).toBeInTheDocument();
    expect(screen.getByTestId("practice-detail-attempts")).toHaveTextContent("2/2");
    expect(screen.queryByRole("columnheader", { name: "Thời gian" })).not.toBeInTheDocument();
    expect(screen.getByTestId("practice-detail-attempt")).toHaveTextContent("2 / 3");
  });

  const historyRow = (classId: string, classCode: string) => ({ quizId: "z1", classId, classCode, subjectCode: "SWD392",
    title: "Luyện tập", status: "OPEN" as const, timeLimitMinutes: null, attemptsUsed: 0, maxAttempts: 2, bestScore: null,
    lastScore: null, maxScore: 3, lastAttemptAt: null, inProgressAttemptId: null, canStart: true });

  it("link thông báo thiếu classId: một lớp thì chuyển sang link có lớp", async () => {
    vi.mocked(getPracticeHistory).mockResolvedValue([historyRow("c1", "SE1801"), { ...historyRow("c2", "SE1802"), quizId: "z9" }]);
    render(<QuizPracticeDetailPage quizId="z1" classId={null} />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/quiz-practice/z1?classId=c1"));
    expect(getPracticeQuiz).not.toHaveBeenCalled();
  });

  it("quiz của môn ở nhiều lớp thì cho chọn lớp; không có lớp thì báo", async () => {
    vi.mocked(getPracticeHistory).mockResolvedValue([historyRow("c1", "SE1801"), historyRow("c2", "SE1802")]);
    const { unmount } = render(<QuizPracticeDetailPage quizId="z1" classId={null} />);
    const classes = await screen.findAllByTestId("practice-detail-class");
    expect(classes).toHaveLength(2);
    fireEvent.click(classes[1]!);
    expect(push).toHaveBeenCalledWith("/quiz-practice/z1?classId=c2");
    unmount();

    vi.mocked(getPracticeHistory).mockResolvedValue([]);
    render(<QuizPracticeDetailPage quizId="z1" classId={null} />);
    expect(await screen.findByText("Quiz không có trong lớp bạn đang học hoặc đã ngưng.")).toBeInTheDocument();
  });
});

describe("QuizTakingPage (UC 51)", () => {
  beforeEach(() => {
    vi.mocked(getAttempt).mockResolvedValue(TAKING);
  });

  it("chỉ gửi sự kiện hoạt động khi lượt bật ghi nhận, kèm lời báo", async () => {
    const { unmount } = render(<QuizTakingPage attemptId="a1" />);
    await screen.findByTestId("quiz-taking-option-1");
    expect(screen.queryByTestId("quiz-taking-activity-notice")).not.toBeInTheDocument();
    expect(recordQuizActivity).not.toHaveBeenCalled();
    unmount();

    vi.mocked(getAttempt).mockResolvedValue({ ...TAKING, activityMonitoringEnabled: true, activityNotice: "Ghi lại khi rời trang" });
    render(<QuizTakingPage attemptId="a1" />);
    expect(await screen.findByTestId("quiz-taking-activity-notice")).toHaveTextContent("Ghi lại khi rời trang");
    await waitFor(() => expect(recordQuizActivity).toHaveBeenCalledWith("a1",
      [expect.objectContaining({ kind: "SESSION_START", sequence: 0 })], false));
  });

  it("ô chọn đáp án có tên là nội dung lựa chọn", async () => {
    render(<QuizTakingPage attemptId="a1" />);
    expect(await screen.findByRole("radio", { name: "Hà Nội" })).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: "Huế" })).toBeInTheDocument();
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
    await waitFor(() => expect(saveAnswers).toHaveBeenCalledWith("a1", { q1: ["1"] }, 0, false));
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
    expect(saveAnswers).toHaveBeenCalledWith("a1", { q1: ["1"] }, 0, false);
    expect(submitAttempt).toHaveBeenCalledWith("a1");
  });

  it("lần lưu sau chờ lần lưu đang chạy xong rồi gửi answerVersion mới", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    let finishFirst: (value: { answerVersion: number; savedAt: string }) => void = () => {};
    vi.mocked(saveAnswers)
      .mockImplementationOnce(() => new Promise((resolve) => { finishFirst = resolve; }))
      .mockResolvedValueOnce({ answerVersion: 2, savedAt: "2026-10-20T08:01:05Z" });
    render(<QuizTakingPage attemptId="a1" />);
    fireEvent.click(await screen.findByTestId("quiz-taking-option-1"));
    await act(async () => {
      vi.advanceTimersByTime(AUTOSAVE_DELAY_MS + 10);
    });
    await waitFor(() => expect(saveAnswers).toHaveBeenCalledTimes(1));

    fireEvent.click(screen.getByTestId("quiz-taking-option-2"));
    await act(async () => {
      vi.advanceTimersByTime(AUTOSAVE_DELAY_MS + 10);
    });
    expect(saveAnswers).toHaveBeenCalledTimes(1);

    await act(async () => {
      finishFirst({ answerVersion: 1, savedAt: "2026-10-20T08:01:00Z" });
    });
    await waitFor(() => expect(saveAnswers).toHaveBeenCalledTimes(2));
    expect(saveAnswers).toHaveBeenLastCalledWith("a1", { q1: ["2"] }, 1, false);
    expect(await screen.findByText("Đã tự động lưu")).toBeInTheDocument();
  });

  it("nộp bài chờ lần lưu đang chạy xong", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    let finishSave: (value: { answerVersion: number; savedAt: string }) => void = () => {};
    vi.mocked(saveAnswers).mockImplementationOnce(() => new Promise((resolve) => { finishSave = resolve; }));
    vi.mocked(submitAttempt).mockResolvedValue(RESULT);
    render(<QuizTakingPage attemptId="a1" />);
    fireEvent.click(await screen.findByTestId("quiz-taking-option-1"));
    await act(async () => {
      vi.advanceTimersByTime(AUTOSAVE_DELAY_MS + 10);
    });
    await waitFor(() => expect(saveAnswers).toHaveBeenCalledTimes(1));

    fireEvent.click(screen.getByTestId("quiz-taking-submit"));
    fireEvent.click(screen.getByTestId("quiz-taking-confirm-submit"));
    await act(async () => {
      await Promise.resolve();
    });
    expect(submitAttempt).not.toHaveBeenCalled();

    await act(async () => {
      finishSave({ answerVersion: 1, savedAt: "2026-10-20T08:01:00Z" });
    });
    await waitFor(() => expect(submitAttempt).toHaveBeenCalledWith("a1"));
    expect(saveAnswers).toHaveBeenCalledTimes(1);
  });

  it("rời trang hoặc trang bị ẩn thì lưu ngay bằng keepalive", async () => {
    vi.mocked(saveAnswers)
      .mockResolvedValueOnce({ answerVersion: 1, savedAt: "2026-10-20T08:01:00Z" })
      .mockResolvedValueOnce({ answerVersion: 2, savedAt: "2026-10-20T08:01:05Z" });
    render(<QuizTakingPage attemptId="a1" />);
    fireEvent.click(await screen.findByTestId("quiz-taking-option-1"));
    await act(async () => {
      window.dispatchEvent(new Event("pagehide"));
    });
    await waitFor(() => expect(saveAnswers).toHaveBeenCalledWith("a1", { q1: ["1"] }, 0, true));

    fireEvent.click(screen.getByTestId("quiz-taking-option-2"));
    const visibility = vi.spyOn(document, "visibilityState", "get").mockReturnValue("hidden");
    try {
      await act(async () => {
        document.dispatchEvent(new Event("visibilitychange"));
      });
      await waitFor(() => expect(saveAnswers).toHaveBeenLastCalledWith("a1", { q1: ["2"] }, 1, true));
    } finally {
      visibility.mockRestore();
    }
    expect(saveAnswers).toHaveBeenCalledTimes(2);
  });

  it("không lưu được câu trả lời thì không nộp tay", async () => {
    vi.mocked(saveAnswers).mockRejectedValue(new ApiError({ status: 503, code: "SYSTEM_BUSY", detail: "Hệ thống tạm bận" }));
    render(<QuizTakingPage attemptId="a1" />);
    fireEvent.click(await screen.findByTestId("quiz-taking-option-1"));
    fireEvent.click(screen.getByTestId("quiz-taking-submit"));
    fireEvent.click(screen.getByTestId("quiz-taking-confirm-submit"));

    await waitFor(() => expect(saveAnswers).toHaveBeenCalledTimes(1));
    expect(await screen.findByTestId("quiz-taking-save-state")).toHaveTextContent("Chưa lưu được");
    expect(submitAttempt).not.toHaveBeenCalled();
    expect(replace).not.toHaveBeenCalled();
  });

  it("lưu lỗi thì có nút Thử lại gửi lại câu trả lời đang chờ", async () => {
    vi.mocked(saveAnswers).mockRejectedValueOnce(new ApiError({ status: 503, code: "SYSTEM_BUSY", detail: "Hệ thống tạm bận" }));
    vi.mocked(saveAnswers).mockResolvedValue({ answerVersion: 1, savedAt: "2026-10-20T08:01:00Z" });
    render(<QuizTakingPage attemptId="a1" />);
    fireEvent.click(await screen.findByTestId("quiz-taking-option-1"));
    expect(screen.queryByTestId("quiz-taking-save-retry")).not.toBeInTheDocument();

    fireEvent.click(await screen.findByTestId("quiz-taking-save-retry", undefined, { timeout: 4000 }));
    await waitFor(() => expect(saveAnswers).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.getByTestId("quiz-taking-save-state")).toHaveTextContent("Đã tự động lưu"));
    expect(screen.queryByTestId("quiz-taking-save-retry")).not.toBeInTheDocument();
  });

  it("đọc mốc thời gian còn lại cho trình đọc màn hình; quiz không giới hạn giờ thì không đọc gì", async () => {
    vi.mocked(getAttempt).mockResolvedValue({ ...TAKING, deadlineAt: new Date(Date.now() + 50_000).toISOString(), serverTime: new Date().toISOString() });
    const timed = render(<QuizTakingPage attemptId="a1" />);
    expect(await screen.findByTestId("quiz-taking-time-milestone")).toHaveTextContent("Còn dưới 1 phút.");
    expect(screen.getByTestId("quiz-taking-time-milestone")).toHaveAttribute("aria-live", "assertive");
    timed.unmount();

    vi.mocked(getAttempt).mockResolvedValue(TAKING);
    render(<QuizTakingPage attemptId="a1" />);
    expect(await screen.findByTestId("quiz-taking-time-milestone")).toBeEmptyDOMElement();
  });

  it("thoát quiz có giới hạn giờ thì hỏi trước và báo đồng hồ vẫn chạy; quiz không giới hạn thì thoát ngay", async () => {
    vi.mocked(getAttempt).mockResolvedValue({ ...TAKING, deadlineAt: new Date(Date.now() + 600_000).toISOString(), serverTime: new Date().toISOString() });
    const timed = render(<QuizTakingPage attemptId="a1" />);
    fireEvent.click(await screen.findByTestId("quiz-taking-exit"));
    expect(await screen.findByTestId("quiz-taking-exit-notice")).toHaveTextContent("đồng hồ vẫn chạy");
    expect(push).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("quiz-taking-confirm-exit"));
    await waitFor(() => expect(push).toHaveBeenCalledWith("/quiz-practice"));
    timed.unmount();
    push.mockClear();

    vi.mocked(getAttempt).mockResolvedValue(TAKING);
    render(<QuizTakingPage attemptId="a1" />);
    fireEvent.click(await screen.findByTestId("quiz-taking-exit"));
    await waitFor(() => expect(push).toHaveBeenCalledWith("/quiz-practice"));
    expect(screen.queryByTestId("quiz-taking-exit-notice")).not.toBeInTheDocument();
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

  it("lượt tự nộp ghi rõ lý do, lượt nộp tay thì không", async () => {
    vi.mocked(getResult).mockResolvedValue({ ...RESULT, submitMode: "AUTO_RETIRED" });
    const { unmount } = render(<QuizResultPage attemptId="a1" />);
    expect(await screen.findByText(/Tự nộp khi quiz bị ngưng/)).toBeInTheDocument();
    unmount();

    vi.mocked(getResult).mockResolvedValue({ ...RESULT, submitMode: "AUTO_TIME_LIMIT" });
    const timed = render(<QuizResultPage attemptId="a1" />);
    expect(await screen.findByText(/Tự nộp khi hết giờ/)).toBeInTheDocument();
    timed.unmount();

    vi.mocked(getResult).mockResolvedValue(RESULT);
    render(<QuizResultPage attemptId="a1" />);
    await screen.findByTestId("result-hidden");
    expect(screen.queryByText(/Tự nộp/)).not.toBeInTheDocument();
  });
});

describe("QuizAttemptsSection (UC 42)", () => {
  it("giảng viên xem bảng lượt làm (tiêu đề không kèm số, không có dòng tóm tắt); không có lớp thì không hiện", async () => {
    vi.mocked(getClassAttempts).mockResolvedValue({ quizId: "z1", classId: "c1", maxAttempts: 2, studentsAttempted: 1, totalStudents: 30,
      students: [{ accountId: "s1", displayName: "Sinh viên A", email: "a@fpt.edu.vn", attempts: 1, bestScore: 2, lastScore: 2,
        lastSubmittedAt: "2026-10-20T08:05:00Z", rows: [], lastLeaveCount: null, signalWarning: false, penaltyPending: false }] });
    const { container } = render(<QuizAttemptsSection quizId="z1" />);
    expect(container).toBeEmptyDOMElement();
    render(<QuizAttemptsSection quizId="z1" classId="c1" />);
    const row = await screen.findByTestId("quiz-attempts-row");
    expect(row).toHaveTextContent("Sinh viên A");
    expect(screen.getByRole("heading", { name: "Lượt làm của sinh viên" })).toBeInTheDocument();
    expect(screen.queryByText(/sinh viên đã làm/)).not.toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Điểm cao nhất" })).toBeInTheDocument();
    expect(getClassAttempts).toHaveBeenCalledWith("c1", "z1");
  });
});

describe("Gợi ý trừ điểm khi rời trang (BR-U11-33)", () => {
  const pending = { ...RESULT, showScore: true, showAnswers: true, score: 8, maxScore: 10,
    penalty: { rawScore: 8, suggestedPenalty: 1.5, penaltyPoints: 0, leaveCount: 4, decisionPending: true, decision: null, decisionReason: null } };
  const row = { attemptId: "a1", attemptNo: 1, status: "SUBMITTED" as const, startedAt: "2026-10-20T08:00:00Z",
    submittedAt: "2026-10-20T08:05:00Z", score: 8, maxScore: 10, leaveCount: 4, activitySignal: "OK" as const, suggestedPenalty: 1.5, penaltyDecision: null };
  const applied = (points: number, reason: string) => ({ result: { ...pending, score: 8 - points,
    penalty: { ...pending.penalty, penaltyPoints: points, decisionPending: false, decision: "APPLIED" as const, decisionReason: reason } },
    displayName: "Sinh viên A", activity: null, activitySignal: "OK" as const });

  it("sinh viên thấy điểm chưa bị trừ khi chờ quyết định, rồi thấy điểm trừ và lý do", async () => {
    vi.mocked(getResult).mockResolvedValue({ ...pending, penalty: { ...pending.penalty, suggestedPenalty: null } });
    const { unmount } = render(<QuizResultPage attemptId="a1" />);
    expect(await screen.findByTestId("result-penalty")).toHaveTextContent("Rời trang 4 lần · chưa bị trừ điểm, giảng viên sẽ xem xét.");
    expect(screen.getByTestId("result-score")).toHaveTextContent("8 / 10");
    unmount();
    vi.mocked(getResult).mockResolvedValue(applied(1.5, "Rời nhiều").result);
    render(<QuizResultPage attemptId="a1" />);
    expect(await screen.findByTestId("result-penalty")).toHaveTextContent("Điểm gốc 8 · rời trang 4 lần · giảng viên trừ 1,5 điểm: Rời nhiều");
  });

  const openAttempt = async () => {
    vi.mocked(getClassAttempts).mockResolvedValue({ quizId: "z1", classId: "c1", maxAttempts: 2, studentsAttempted: 1, totalStudents: 30,
      students: [{ accountId: "s1", displayName: "Sinh viên A", email: "a@fpt.edu.vn", attempts: 1, bestScore: 8, lastScore: 8,
        lastSubmittedAt: "2026-10-20T08:05:00Z", rows: [row], lastLeaveCount: 4, signalWarning: false, penaltyPending: true }] });
    vi.mocked(getTeacherAttempt).mockResolvedValue({ result: pending, displayName: "Sinh viên A", activity: null, activitySignal: "OK" });
    render(<QuizAttemptsSection quizId="z1" classId="c1" />);
    expect(await screen.findByTestId("quiz-attempts-leaves")).toHaveTextContent("4 lần");
    expect(screen.getByTestId("quiz-attempts-penalty-pending")).toHaveTextContent("Chờ quyết định");
    fireEvent.click(screen.getByTestId("quiz-attempts-row"));
    fireEvent.click(await screen.findByTestId("quiz-attempts-open"));
    expect(await screen.findByTestId("teacher-attempt-penalty")).toHaveTextContent("gợi ý trừ 1,5 điểm");
    expect(screen.getByTestId("teacher-attempt-penalty-state")).toHaveTextContent("Chưa quyết định, điểm chưa bị trừ.");
  };

  it("giảng viên chọn số điểm khác mức gợi ý và trừ kèm lý do", async () => {
    vi.mocked(decideAttemptPenalty).mockResolvedValue(applied(1, "Rời nhiều"));
    await openAttempt();
    expect(screen.getByTestId("teacher-attempt-penalty-points")).toHaveValue("1.5");
    expect(screen.getByTestId("teacher-attempt-penalty-apply")).toBeDisabled();
    expect(screen.getByTestId("teacher-attempt-penalty-dismiss")).toBeDisabled();
    fireEvent.change(screen.getByTestId("teacher-attempt-penalty-reason"), { target: { value: " Rời nhiều " } });
    fireEvent.change(screen.getByTestId("teacher-attempt-penalty-points"), { target: { value: "9" } });
    expect(screen.getByTestId("teacher-attempt-penalty-apply")).toBeDisabled();
    fireEvent.change(screen.getByTestId("teacher-attempt-penalty-points"), { target: { value: "1" } });
    fireEvent.click(screen.getByTestId("teacher-attempt-penalty-apply"));
    await waitFor(() => expect(decideAttemptPenalty).toHaveBeenCalledWith("c1", "a1", { decision: "APPLIED", points: 1, reason: "Rời nhiều" }));
    expect(await screen.findByTestId("teacher-attempt-penalty-state")).toHaveTextContent("Đã trừ 1 điểm: Rời nhiều");
    expect(screen.getByTestId("teacher-attempt-penalty-reason")).toHaveValue("");
  });

  it("giảng viên quyết định không trừ kèm lý do", async () => {
    vi.mocked(decideAttemptPenalty).mockResolvedValue({ result: { ...pending, penalty: { ...pending.penalty, decisionPending: false, decision: "DISMISSED", decisionReason: "Lỗi máy" } },
      displayName: "Sinh viên A", activity: null, activitySignal: "OK" });
    await openAttempt();
    fireEvent.change(screen.getByTestId("teacher-attempt-penalty-reason"), { target: { value: "Lỗi máy" } });
    fireEvent.click(screen.getByTestId("teacher-attempt-penalty-dismiss"));
    await waitFor(() => expect(decideAttemptPenalty).toHaveBeenCalledWith("c1", "a1", { decision: "DISMISSED", reason: "Lỗi máy" }));
    expect(await screen.findByTestId("teacher-attempt-penalty-state")).toHaveTextContent("Đã quyết định không trừ: Lỗi máy");
  });

  it("lượt không có mức gợi ý thì không có phần quyết định", async () => {
    vi.mocked(getClassAttempts).mockResolvedValue({ quizId: "z1", classId: "c1", maxAttempts: 2, studentsAttempted: 1, totalStudents: 30,
      students: [{ accountId: "s1", displayName: "Sinh viên A", email: "a@fpt.edu.vn", attempts: 1, bestScore: 8, lastScore: 8,
        lastSubmittedAt: "2026-10-20T08:05:00Z", rows: [{ ...row, suggestedPenalty: 0 }], lastLeaveCount: 0, signalWarning: false, penaltyPending: false }] });
    vi.mocked(getTeacherAttempt).mockResolvedValue({ result: { ...pending, penalty: { ...pending.penalty, suggestedPenalty: 0, decisionPending: false } },
      displayName: "Sinh viên A", activity: null, activitySignal: "OK" });
    render(<QuizAttemptsSection quizId="z1" classId="c1" />);
    fireEvent.click(await screen.findByTestId("quiz-attempts-row"));
    expect(screen.queryByText(/Gợi ý trừ/)).toBeNull();
    fireEvent.click(await screen.findByTestId("quiz-attempts-open"));
    expect(await screen.findByTestId("teacher-attempt-summary")).toBeInTheDocument();
    expect(screen.queryByTestId("teacher-attempt-penalty")).toBeNull();
  });

  it("báo quy tắc trước và trong khi làm bài", async () => {
    const rule = { freeLeaves: 0, pointsPerLeave: 1, maxPenalty: null, minAwaySeconds: 0 };
    vi.mocked(getPracticeQuiz).mockResolvedValue({ ...DETAIL, activityNotice: "Có ghi nhận", leavePenalty: rule });
    const { unmount } = render(<QuizPracticeDetailPage quizId="z1" classId="c1" />);
    expect(await screen.findByTestId("practice-detail-penalty-rule")).toHaveTextContent("Mỗi lần rời trang có thể bị trừ 1 điểm");
    unmount();
    vi.mocked(getAttempt).mockResolvedValue({ ...TAKING, activityMonitoringEnabled: true, activityNotice: "Có ghi nhận", leavePenalty: rule });
    render(<QuizTakingPage attemptId="a1" />);
    expect(await screen.findByTestId("quiz-taking-penalty-rule")).toHaveTextContent("Mỗi lần rời trang có thể bị trừ 1 điểm");
  });
});

describe("Chi tiết lượt của giảng viên (BR-U11-31)", () => {
  it("hiện dòng thời gian hoạt động của U08", async () => {
    vi.mocked(getClassAttempts).mockResolvedValue({ quizId: "z1", classId: "c1", maxAttempts: 2, studentsAttempted: 1, totalStudents: 30,
      students: [{ accountId: "s1", displayName: "Sinh viên A", email: "a@fpt.edu.vn", attempts: 1, bestScore: 2, lastScore: 2,
        lastSubmittedAt: "2026-10-20T08:05:00Z",
        rows: [{ attemptId: "a1", attemptNo: 1, status: "SUBMITTED", startedAt: "2026-10-20T08:00:00Z",
          submittedAt: "2026-10-20T08:05:00Z", score: 2, maxScore: 3, leaveCount: 2, activitySignal: "SIGNAL_LOST", suggestedPenalty: null, penaltyDecision: null }], lastLeaveCount: 2, signalWarning: true, penaltyPending: false }] });
    vi.mocked(getTeacherAttempt).mockResolvedValue({ result: { ...RESULT, showScore: true, score: 2, maxScore: 3 }, displayName: "Sinh viên A",
      activity: { policyState: "AVAILABLE", awayCount: 2, knownAwayDuration: "PT1M5S", intervals: [], unknownGaps: 0,
        reliabilityNote: "Chỉ để tham khảo" }, activitySignal: "SIGNAL_LOST" });
    render(<QuizAttemptsSection quizId="z1" classId="c1" />);
    fireEvent.click(await screen.findByTestId("quiz-attempts-row"));
    fireEvent.click(await screen.findByTestId("quiz-attempts-open"));

    expect(await screen.findByTestId("teacher-attempt-activity")).toHaveTextContent("Rời trang 2 lần");
    expect(getTeacherAttempt).toHaveBeenCalledWith("c1", "a1");
  });
});
