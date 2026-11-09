// Kiểu dữ liệu ngân hàng câu hỏi (U10, UC 46, 47), khớp DTO của QuestionController.

import type { SourceChoice } from "@/shared/ai";
import type { FormattedText } from "@/shared/document/formattedText";

export type QuestionType = "MCQ_SINGLE" | "MCQ_MULTI";
export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type QuestionStatus = "DRAFT" | "PENDING" | "REJECTED" | "ACTIVE" | "RETIRED";

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

/** Câu gần giống trong ngân hàng, chỉ để cảnh báo (BR-U10-08). */
export type SimilarQuestion = { id: string; stem: string; status: QuestionStatus; percent: number };

/** Lần duyệt gần nhất của câu đề xuất (BR-U10-09). */
export type ReviewInfo = { decision: "APPROVED" | "REJECTED"; reason: string | null; reviewedAt: string };

export type QuestionDetail = {
  question: QuestionView;
  versions: VersionEntry[];
  usedInQuizzes: number;
  draftId: string | null;
  similar: SimilarQuestion[];
  review: ReviewInfo | null;
  /** Quiz sinh đề đang mở thiếu câu sau khi ngưng câu này (BR-U10-17). */
  quizzesShortOfQuestions: string[];
};

/** Câu giảng viên đề xuất ở một lớp (BR-U10-09). */
export type ProposalView = { question: QuestionView; review: ReviewInfo | null; similar: SimilarQuestion[] };

export type BulkItemResult = { id: string; ok: boolean; code: string | null; message: string | null };
export type BulkResult = { items: BulkItemResult[]; succeeded: number; failed: number };
export type BulkItem = { id: string; version: number };

export type DuplicateGroup = { questions: SimilarQuestion[] };

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
  version: number;
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
export type QuizStatus = "DRAFT" | "OPEN" | "RETIRED";
export type QuizPaperMode = "FIXED" | "GENERATED";
export type QuizOrigin = "MANUAL" | "CLONE" | "NEW_VERSION" | "CLASS_COPY";

/** Mức trừ gợi ý theo số lần rời trang; giảng viên quyết định có trừ hay không (BR-U10-15). */
export type LeavePenalty = { freeLeaves: number; pointsPerLeave: number; maxPenalty: number | null; minAwaySeconds: number };

export type QuizSettings = {
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  timeLimitMinutes: number | null;
  maxAttempts: number | null;
  showScoreAfterSubmit: boolean;
  showCorrectAnswers: boolean;
  leavePenalty: LeavePenalty | null;
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
  instructionsDocument?: FormattedText | null;
  settings: QuizSettings;
  activityMonitoringEnabled: boolean;
  activityNotice: string | null;
  paperMode: QuizPaperMode;
  status: QuizStatus;
  publishedAt: string | null;
  retiredAt: string | null;
  retiredReason: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type QuizItemView = { position: number; points: number; inline: boolean; question: QuestionView };

/** Một quy tắc sinh đề (BR-U10-17). */
export type DrawRule = { position: number; tag: string; difficulty: Difficulty | null; questionCount: number; pointsEach: number };
export type DrawRuleInput = Omit<DrawRule, "position">;

export type BankTag = { tag: string; activeCount: number };

export type RuleShortage = { position: number; tag: string; difficulty: Difficulty | null; required: number; available: number };

export type PaperPreview = {
  items: { rulePosition: number; points: number; question: QuestionView }[];
  maxScore: number;
  shortages: RuleShortage[];
};

export type QuizDetail = {
  quiz: QuizInfo;
  items: QuizItemView[];
  drawRules: DrawRule[];
  maxScore: number;
  canManage: boolean;
};

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
  instructionsDocument?: FormattedText | null;
  settings: QuizSettings;
  activityMonitoringEnabled: boolean;
  activityNotice: string | null;
};

export type QuizItemInput = { questionId: string; points: number | null };

export type RandomPreview = { questions: QuestionView[]; matching: number; freeSlots: number };

// --- Đề xuất AI, MCP và tài liệu nguồn cho AI (giai đoạn 4)

export type SelectableMaterial = { materialId: string; title: string; hasText: boolean };

export type AiDraftStatus = "PENDING" | "READY" | "ACCEPTED" | "DISCARDED" | "FAILED";

export type DraftQuestion = { content: QuestionContent; violations: Violation[] };

export type AiDraft = {
  id: string;
  target: "QUIZ" | "BANK";
  quizId: string | null;
  subjectId: string;
  origin: "AI_TASK" | "MCP";
  /** Task AI của U06; trống với đề xuất từ MCP. */
  taskId: string | null;
  status: AiDraftStatus;
  questions: DraftQuestion[];
  requestedCount: number | null;
  source: string | null;
  provider: string | null;
  materials: { id: string; title: string | null }[];
  sourceWarnings?: string[];
  agentLabel: string | null;
  baseVersion: number | null;
  failureCode: string | null;
  failureMessage: string | null;
  createdBy: string;
  createdAt: string;
  decidedAt: string | null;
};

/** Yêu cầu AI soạn câu (BR-U10-40): 1–50 câu, loại và độ khó tùy chọn, tối đa 10 tài liệu đã có chữ. */
export type AiDraftRequest = {
  instructions: string;
  count: number;
  type: QuestionType | null;
  difficulty: Difficulty | null;
  materialIds: string[];
};

/** Yêu cầu kèm nguồn đã chọn ở `AiSourcePicker` của U06 (M2). */
/** {@code choice} bỏ trống: hệ thống tự chọn nguồn AI (yêu cầu 2026-11-05). */
export type AiDraftInput = AiDraftRequest & { choice?: SourceChoice };

export type BankAcceptResult = { draft: AiDraft; questionIds: string[] };
