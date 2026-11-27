import type { DiagramType, DocumentModel, DocumentPart, ValidationIssue } from "@/shared/document";
import type { Tone } from "@/shared/ui";
import { msg } from "@/i18n/msg";

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
  /** Học kỳ của lớp (D-U04-05); có thể thiếu ở dữ liệu cũ. */
  classTerm?: string | null;
  subjectCode: string | null;
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
  /** {@code changedSinceSubmit}: nhóm sửa sau lần nộp cuối, bài lại là Đang làm. */
  group: { groupDocumentId: string | null; submitted: boolean; submittedAt: string | null; changedSinceSubmit?: boolean } | null;
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
  subjectCode: string | null;
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
/** Góp ý Practice; điểm là AI ước tính theo rubric (yêu cầu 2026-11-27), chỉ có ở phần có rubric tính điểm. */
export type FeedbackBody = {
  label: string;
  summary: string;
  strengths: string[];
  suggestions: string[];
  parts: { partId: string; comment: string; score?: number; maxScore?: number }[];
  score?: number;
  maxScore?: number;
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
  NOT_STARTED: msg("assignmentwork.model.types.notStarted"), IN_PROGRESS: msg("assignmentwork.model.types.inProgress"), SUBMITTED: msg("assignmentwork.model.types.submitted"), SUBMITTED_LATE: msg("assignmentwork.model.types.submittedLate"), MISSED: msg("assignmentwork.model.types.expired"), GROUP: msg("assignmentwork.model.types.groupAssignment"),
};

/** Chưa làm xám, Đang làm vàng, Đã nộp xanh lá, Nộp trễ tím, Hết hạn đỏ. */
export const myStatusTones: Record<MyStatus, Tone> = {
  NOT_STARTED: "neutral", IN_PROGRESS: "warning", SUBMITTED: "success", SUBMITTED_LATE: "accent", MISSED: "danger", GROUP: "accent",
};

export const submitModeLabels: Record<SubmitMode, string> = {
  MANUAL: msg("assignmentwork.model.types.submittedManually"), AUTO_DEADLINE: msg("assignmentwork.model.types.autoSubmittedWhenSubmissionsClosed"), AUTO_RETIRED: msg("assignmentwork.model.types.autoSubmittedWhenWithdrawn"),
};

export const blockedLabels: Record<string, string> = {
  GROUP_ASSIGNMENT: msg("assignmentwork.model.types.groupAssignmentsAreDoneIn"),
  SUBMISSION_CLOSED: msg("assignmentwork.model.types.submissionsForThisAssignmentHave"),
  ATTEMPTS_EXHAUSTED: msg("assignmentwork.model.types.noAttemptsLeft"),
};
