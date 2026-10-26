import type { DiagramType, DocumentModel, DocumentPart, ValidationIssue } from "@/shared/document";
import type { Tone } from "@/shared/ui";

export type AssignmentStatus = "DRAFT" | "REVIEWED" | "SCHEDULED" | "OPEN" | "CLOSED" | "RETIRED";
export type GradingMode = "GRADED" | "PRACTICE";
export type SubmissionMode = "INDIVIDUAL" | "GROUP";
export type AttemptStatus = "IN_PROGRESS" | "SUBMITTED";
export type SubmitMode = "MANUAL" | "AUTO_DEADLINE" | "AUTO_RETIRED";
export type MyStatus = "NOT_STARTED" | "IN_PROGRESS" | "SUBMITTED" | "SUBMITTED_LATE" | "MISSED" | "GROUP";
export type SubmissionWindow = "ON_TIME" | "LATE" | "CLOSED";

export type StudentRow = {
  assignmentId: string;
  classId: string;
  classCode: string;
  className: string;
  scope: "CLASS" | "SUBJECT";
  title: string;
  submissionMode: SubmissionMode;
  gradingMode: GradingMode;
  diagramEnabled: boolean;
  status: AssignmentStatus;
  opensAt: string | null;
  closesAt: string | null;
  lateUntil: string | null;
  maxAttempts: number | null;
  myStatus: MyStatus;
  attemptCount: number;
  submittedCount: number;
  inProgressAttemptId: string | null;
  lastSubmittedAt: string | null;
  group: { groupDocumentId: string | null; submitted: boolean; submittedAt: string | null } | null;
};

export type AttemptSummary = {
  attemptId: string;
  attemptNo: number;
  status: AttemptStatus;
  startedAt: string;
  deadlineAt: string;
  lastSavedAt: string | null;
  submittedAt: string | null;
  late: boolean;
  submitMode: SubmitMode | null;
  receiptHash: string | null;
  graded: boolean;
  feedbackCount: number;
  warnings: ValidationIssue[];
};

/** Trang thông tin trước khi làm bài: không có đề và rubric (yêu cầu 2026-10-21). */
export type StudentDetail = {
  assignmentId: string;
  classId: string;
  classCode: string;
  className: string;
  scope: "CLASS" | "SUBJECT";
  title: string;
  submissionMode: SubmissionMode;
  gradingMode: GradingMode;
  diagramEnabled: boolean;
  status: AssignmentStatus;
  opensAt: string | null;
  closesAt: string | null;
  lateUntil: string | null;
  maxAttempts: number | null;
  partCount: number;
  maxPoints: number;
  requiredDiagrams: Partial<Record<DiagramType, number>>;
  activityMonitoringEnabled: boolean;
  activityNotice: string | null;
  referenceMaterialIds: string[];
  attemptsUsed: number;
  latestAttempt: AttemptSummary | null;
  inProgressAttemptId: string | null;
  canStart: boolean;
  startBlockedReason: string | null;
  window: SubmissionWindow;
  serverTime: string;
};

export type Attempt = {
  attemptId: string;
  assignmentId: string;
  classId: string;
  title: string;
  attemptNo: number;
  maxAttempts: number;
  status: AttemptStatus;
  content: DocumentModel;
  contentVersion: number;
  startedAt: string;
  deadlineAt: string;
  lastSavedAt: string | null;
  submittedAt: string | null;
  late: boolean;
  submitMode: SubmitMode | null;
  receiptHash: string | null;
  warnings: ValidationIssue[];
  instructions: string;
  gradingMode: GradingMode;
  submissionMode: SubmissionMode;
  diagramEnabled: boolean;
  requiredDiagrams: Partial<Record<DiagramType, number>>;
  parts: DocumentPart[];
  maxPoints: number;
  activityMonitoringEnabled: boolean;
  activityNotice: string | null;
  closesAt: string | null;
  lateUntil: string | null;
  editable: boolean;
  serverTime: string;
  /** Kết quả kiểm nộp của bản đang lưu, cùng luật lúc nộp (server có bản đề gốc); lượt đã nộp thì rỗng. */
  submitIssues?: ValidationIssue[];
};

export type Saved = { attemptId: string; contentVersion: number; lastSavedAt: string; content: DocumentModel; submitIssues?: ValidationIssue[] };

export type Receipt = {
  attemptId: string;
  assignmentId: string;
  classId: string;
  title: string;
  attemptNo: number;
  maxAttempts: number;
  submittedAt: string;
  late: boolean;
  submitMode: SubmitMode;
  receiptHash: string;
  blockCount: number;
  diagramCount: number;
};

/** Góp ý AI: không có ước tính theo rubric (người học không xem rubric); bản cũ có thể còn trường rubric, không hiện. */
export type FeedbackBody = {
  label: string;
  summary: string;
  strengths: string[];
  suggestions: string[];
  parts: { partId: string; comment: string }[];
};

export type Feedback = {
  feedbackId: string;
  attemptId: string;
  origin: "AI_TASK" | "MCP";
  status: "PENDING" | "READY" | "FAILED";
  taskId: string | null;
  feedback: FeedbackBody | null;
  provenance: Record<string, unknown>;
  safeError: string | null;
  createdAt: string;
  completedAt: string | null;
};

export type ImportPreview = { document: DocumentModel; warnings: ValidationIssue[]; imageCount: number; diagramCount: number };

export type AttemptDiagram = { attemptId: string; blockId: string; title: string; diagramType: DiagramType; alt: string; xml: string; svgPreview: string | null };

export const myStatusLabels: Record<MyStatus, string> = {
  NOT_STARTED: "Chưa làm", IN_PROGRESS: "Đang làm", SUBMITTED: "Đã nộp", SUBMITTED_LATE: "Nộp trễ", MISSED: "Hết hạn", GROUP: "Bài nhóm",
};

export const myStatusTones: Record<MyStatus, Tone> = {
  NOT_STARTED: "neutral", IN_PROGRESS: "info", SUBMITTED: "success", SUBMITTED_LATE: "warning", MISSED: "danger", GROUP: "accent",
};

export const submitModeLabels: Record<SubmitMode, string> = {
  MANUAL: "Nộp tay", AUTO_DEADLINE: "Tự nộp khi hết hạn", AUTO_RETIRED: "Tự nộp khi ngưng giao",
};

export const blockedLabels: Record<string, string> = {
  GROUP_ASSIGNMENT: "Bài nhóm làm trong tài liệu nhóm",
  SUBMISSION_CLOSED: "Bài đã hết hạn nhận bài",
  ATTEMPTS_EXHAUSTED: "Đã hết lượt",
};
