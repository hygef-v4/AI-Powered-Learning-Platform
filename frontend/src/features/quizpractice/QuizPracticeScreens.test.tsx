import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import {
  getAttempt, getClassAttempts, getPracticeHistory, getPracticeQuiz, getResult, getTeacherAttempt, recordQuizActivity, saveAnswers, startAttempt,
  submitAttempt,
} from "./api";
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
  recordQuizActivity: vi.fn(() => Promise.resolve()),
}));

const push = vi.fn();
const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push, replace }) }));
vi.mock("@/features/identity/session", () => ({ useSession: () => ({ status: "authenticated", profile: { role: "STUDENT" } }) }));
vi.mock("@/features/materials/ModulePlace", () => ({ useModuleScope: () => null, useModulePlace: () => ({ reload: () => {} }), ModuleNeighbours: () => null }));

const TAKING: TakingView = {
  attemptId: "a1", quizId: "z1", classId: "c1", title: "Luyện tập", attemptNo: 1, startedAt: "2026-10-20T08:00:00Z",
  deadlineAt: null, serverTime: new Date().toISOString(), answerVersion: 0, answers: {}, activityMonitoringEnabled: false,
  activityNotice: null,
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

  const historyRow = (classId: string, classCode: string) => ({ quizId: "z1", classId, classCode, className: "Lớp",
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
    expect(await screen.findByTestId("quiz-taking-save-state")).toHaveTextContent("Lưu chưa được");
    expect(submitAttempt).not.toHaveBeenCalled();
    expect(replace).not.toHaveBeenCalled();
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

describe("Chi tiết lượt của giảng viên (BR-U11-31)", () => {
  it("hiện dòng thời gian hoạt động của U08", async () => {
    vi.mocked(getClassAttempts).mockResolvedValue({ quizId: "z1", classId: "c1", maxAttempts: 2, studentsAttempted: 1, totalStudents: 30,
      students: [{ accountId: "s1", displayName: "Sinh viên A", email: "a@fpt.edu.vn", attempts: 1, bestScore: 2, lastScore: 2,
        lastSubmittedAt: "2026-10-20T08:05:00Z",
        rows: [{ attemptId: "a1", attemptNo: 1, status: "SUBMITTED", startedAt: "2026-10-20T08:00:00Z",
          submittedAt: "2026-10-20T08:05:00Z", score: 2, maxScore: 3 }] }] });
    vi.mocked(getTeacherAttempt).mockResolvedValue({ result: { ...RESULT, showScore: true, score: 2, maxScore: 3 }, displayName: "Sinh viên A",
      activity: { policyState: "AVAILABLE", awayCount: 2, knownAwayDuration: "PT1M5S", intervals: [], unknownGaps: 0,
        reliabilityNote: "Chỉ để tham khảo" } });
    render(<QuizAttemptsSection quizId="z1" classId="c1" />);
    fireEvent.click(await screen.findByTestId("quiz-attempts-row"));
    fireEvent.click(await screen.findByTestId("quiz-attempts-open"));

    expect(await screen.findByTestId("teacher-attempt-activity")).toHaveTextContent("Rời trang 2 lần");
    expect(getTeacherAttempt).toHaveBeenCalledWith("c1", "a1");
  });
});
