// Kiểu dữ liệu Quiz Practice (U11, UC 49–52), khớp DTO của QuizPracticeController, TeacherQuizAttemptController.

export type QuizStatus = "DRAFT" | "REVIEWED" | "OPEN" | "RETIRED";
export type QuestionType = "MCQ_SINGLE" | "MCQ_MULTI";
export type AttemptStatus = "IN_PROGRESS" | "SUBMITTED";
export type SubmitMode = "MANUAL" | "AUTO_TIME_LIMIT" | "AUTO_RETIRED";

export type PracticeQuizRow = {
  quizId: string;
  classId: string;
  classCode: string;
  className: string;
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
};

export type PracticeQuizDetail = {
  quizId: string;
  classId: string;
  classCode: string;
  className: string;
  title: string;
  instructions: string | null;
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
  referenceMaterialIds: string[];
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
};

export type SaveResult = { answerVersion: number; savedAt: string };

export type ResultQuestion = {
  questionId: string;
  type: QuestionType;
  stem: string;
  options: OptionView[];
  selectedOptionIds: string[];
  correct: boolean;
  points: number | null;
  maxPoints: number | null;
  correctOptionIds: string[] | null;
  explanation: string | null;
};

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
  correctCount: number;
  wrongCount: number;
  blankCount: number;
  questions: ResultQuestion[];
  canRetry: boolean;
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
};

export type ClassAttempts = {
  quizId: string;
  classId: string;
  maxAttempts: number | null;
  studentsAttempted: number;
  totalStudents: number;
  students: StudentAttempts[];
};

export type TeacherAttemptDetail = { result: ResultView; displayName: string | null; activity: unknown | null };
