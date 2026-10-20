// Kiểu dữ liệu ngân hàng câu hỏi (U10, UC 46, 47), khớp DTO của QuestionController.

export type QuestionType = "MCQ_SINGLE" | "MCQ_MULTI";
export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type QuestionStatus = "DRAFT" | "ACTIVE" | "RETIRED";

export type QuestionOption = { id: string; text: string };

/** Nội dung soạn được của một phiên bản câu (BR-U10-02, 03). */
export type QuestionContent = {
  type: QuestionType;
  stem: string;
  options: QuestionOption[];
  correctOptionIds: string[];
  explanation: string | null;
  defaultPoints: number;
  difficulty: Difficulty;
  tags: string[];
};

export type QuestionView = QuestionContent & {
  id: string;
  lineageId: string;
  versionNo: number;
  subjectId: string;
  status: QuestionStatus;
  activatedAt: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type VersionEntry = { id: string; versionNo: number; status: QuestionStatus; createdAt: string; activatedAt: string | null };

export type QuestionDetail = { question: QuestionView; versions: VersionEntry[]; usedInQuizzes: number; draftId: string | null };

export type QuestionRow = {
  id: string;
  lineageId: string;
  versionNo: number;
  type: QuestionType;
  stem: string;
  difficulty: Difficulty;
  tags: string[];
  status: QuestionStatus;
  hasDraft: boolean;
  updatedAt: string;
};

export type QuestionPage = { items: QuestionRow[]; total: number; page: number; size: number };

export type QuestionQuery = {
  type?: QuestionType;
  difficulty?: Difficulty;
  status?: QuestionStatus;
  q?: string;
  page?: number;
  size?: number;
};

export type Violation = { field: string; code: string; message: string };

export type ImportRow = { row: number; stem: string; optionCount: number; valid: boolean; reasons: Violation[] };

export type ImportResult = { dryRun: boolean; total: number; valid: number; invalid: number; created: number; rows: ImportRow[] };

/** Môn Chủ nhiệm môn đang quản lý (GET /api/v1/manager/subjects của U05). */
export type ManagedSubject = { id: string; code: string; name: string; status?: string };

// --- Quiz (UC 48)

export type QuizScope = "CLASS" | "SUBJECT";
export type QuizStatus = "DRAFT" | "REVIEWED" | "OPEN" | "RETIRED";
export type QuizOrigin = "MANUAL" | "CLONE" | "NEW_VERSION" | "CLASS_COPY";

export type QuizSettings = {
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  timeLimitMinutes: number | null;
  maxAttempts: number | null;
  showScoreAfterSubmit: boolean;
  showCorrectAnswers: boolean;
};

export type QuizInfo = {
  id: string;
  scope: QuizScope;
  classId: string | null;
  subjectId: string;
  sourceQuizId: string | null;
  origin: QuizOrigin;
  versionNo: number;
  title: string;
  instructions: string | null;
  settings: QuizSettings;
  activityMonitoringEnabled: boolean;
  activityNotice: string | null;
  referenceMaterialIds: string[];
  status: QuizStatus;
  publishedAt: string | null;
  retiredAt: string | null;
  retiredReason: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type QuizItemView = { position: number; points: number; inline: boolean; question: QuestionView };

export type QuizDetail = { quiz: QuizInfo; items: QuizItemView[]; maxScore: number; canManage: boolean };

export type QuizRow = {
  id: string;
  scope: QuizScope;
  classId: string | null;
  subjectId: string;
  versionNo: number;
  title: string;
  status: QuizStatus;
  itemCount: number;
  maxScore: number;
  timeLimitMinutes: number | null;
  updatedAt: string;
  readOnly: boolean;
};

export type QuizPage = { items: QuizRow[]; total: number; page: number; size: number };

export type QuizDetailsInput = {
  title: string;
  instructions: string | null;
  settings: QuizSettings;
  activityMonitoringEnabled: boolean;
  activityNotice: string | null;
  referenceMaterialIds: string[];
};

export type QuizItemInput = { questionId: string; points: number | null };

export type RandomPreview = { questions: QuestionView[]; matching: number; freeSlots: number };
