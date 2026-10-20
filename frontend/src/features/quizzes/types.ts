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
