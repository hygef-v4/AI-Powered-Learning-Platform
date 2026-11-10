// Kiểu dữ liệu Quiz Practice (U11, UC 49–52), khớp DTO của QuizPracticeController, TeacherQuizAttemptController.

import type { ActivitySummary } from "@/shared/activity/ActivityTimeline";
import type { FormattedText } from "@/shared/document/formattedText";

export type QuizStatus = "DRAFT" | "OPEN" | "RETIRED";
export type ActivitySignal = "DISABLED" | "OK" | "NO_SIGNAL" | "SIGNAL_LOST" | "UNAVAILABLE";
export type LeavePenalty = { freeLeaves: number; pointsPerLeave: number; maxPenalty: number | null; minAwaySeconds: number };
export type QuestionType = "MCQ_SINGLE" | "MCQ_MULTI";
export type AttemptStatus = "IN_PROGRESS" | "SUBMITTED";
export type SubmitMode = "MANUAL" | "AUTO_TIME_LIMIT" | "AUTO_RETIRED";

export type PracticeQuizRow = {
  quizId: string;
  classId: string;
  classCode: string;
  subjectCode: string | null;
  title: string;
  status: QuizStatus;
  timeLimitMinutes: number | null;
  attemptsUsed: number;
  maxAttempts: number | null;
  bestScore: number | null;
  lastScore: number | null;
  maxScore: number;
  lastAttemptAt: string | null;
  inProgressAttemptId: string | null;
  canStart: boolean;
};

export type AttemptRow = {
  attemptId: string;
  attemptNo: number;
  status: AttemptStatus;
  startedAt: string;
  submittedAt: string | null;
  score: number | null;
  maxScore: number | null;
  /** Chỉ có ở danh sách của giảng viên (BR-U11-32). */
  leaveCount: number | null;
  activitySignal: ActivitySignal | null;
  suggestedPenalty: number | null;
  penaltyDecision: PenaltyDecision | null;
};

export type PracticeQuizDetail = {
  quizId: string;
  classId: string;
  classCode: string;
  subjectCode: string | null;
  title: string;
  instructions: string | null;
  instructionsDocument?: FormattedText | null;
  status: QuizStatus;
  questionCount: number;
  maxScore: number;
  timeLimitMinutes: number | null;
  attemptsUsed: number;
  maxAttempts: number | null;
  attemptsLeft: number | null;
  bestScore: number | null;
  showScoreAfterSubmit: boolean;
  showCorrectAnswers: boolean;
  activityNotice: string | null;
  leavePenalty: LeavePenalty | null;
  attempts: AttemptRow[];
  inProgressAttemptId: string | null;
  canStart: boolean;
  blockedReason: string | null;
};

export type OptionView = { optionId: string; text: string };

export type TakingQuestion = { questionId: string; type: QuestionType; stem: string; points: number; options: OptionView[] };

export type TakingView = {
  attemptId: string;
  quizId: string;
  classId: string;
  title: string;
  attemptNo: number;
  startedAt: string;
  deadlineAt: string | null;
  serverTime: string;
  answerVersion: number;
  questions: TakingQuestion[];
  answers: Record<string, string[]>;
  /** Theo ảnh chụp của lượt (K4). */
  activityMonitoringEnabled: boolean;
  activityNotice: string | null;
  leavePenalty: LeavePenalty | null;
};

export type SaveResult = { answerVersion: number; savedAt: string };

export type ResultQuestion = {
  questionId: string;
  type: QuestionType;
  stem: string;
  options: OptionView[];
  selectedOptionIds: string[];
  /** null khi Quiz không hiện cả điểm lẫn đáp án (BR-U11-20). */
  correct: boolean | null;
  points: number | null;
  maxPoints: number | null;
  correctOptionIds: string[] | null;
  explanation: string | null;
};

export type PenaltyDecision = "APPLIED" | "DISMISSED";

/** Điểm gốc và quyết định trừ điểm của lượt (BR-U11-33); `suggestedPenalty` chỉ giảng viên thấy. */
export type PenaltyView = {
  rawScore: number;
  suggestedPenalty: number | null;
  penaltyPoints: number;
  leaveCount: number | null;
  decisionPending: boolean;
  decision: PenaltyDecision | null;
  decisionReason: string | null;
};

export type PenaltyDecisionInput = { decision: PenaltyDecision; points?: number; reason: string };

export type ResultView = {
  attemptId: string;
  quizId: string;
  classId: string;
  title: string;
  attemptNo: number;
  startedAt: string;
  submittedAt: string;
  submitMode: SubmitMode;
  showScore: boolean;
  showAnswers: boolean;
  score: number | null;
  maxScore: number | null;
  /** null khi Quiz không hiện cả điểm lẫn đáp án (BR-U11-20). */
  correctCount: number | null;
  wrongCount: number | null;
  blankCount: number;
  questions: ResultQuestion[];
  canRetry: boolean;
  penalty: PenaltyView | null;
};

export type StudentAttempts = {
  accountId: string;
  displayName: string | null;
  email: string | null;
  attempts: number;
  bestScore: number | null;
  lastScore: number | null;
  lastSubmittedAt: string | null;
  rows: AttemptRow[];
  lastLeaveCount: number | null;
  signalWarning: boolean;
  penaltyPending: boolean;
};

export type ClassAttempts = {
  quizId: string;
  classId: string;
  maxAttempts: number | null;
  studentsAttempted: number;
  totalStudents: number;
  students: StudentAttempts[];
};

export type TeacherAttemptDetail = { result: ResultView; displayName: string | null; activity: ActivitySummary | null; activitySignal: ActivitySignal | null };
