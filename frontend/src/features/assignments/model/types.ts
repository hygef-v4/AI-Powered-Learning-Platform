import type { Align, Run } from "@/shared/document/types";
import type { DiagramType, DocumentModel, RubricCriterion, ValidationIssue } from "@/shared/document";
import type { Tone } from "@/shared/ui";

export type AssignmentScope = "CLASS" | "SUBJECT";
export type AssignmentStatus = "DRAFT" | "REVIEWED" | "SCHEDULED" | "OPEN" | "CLOSED" | "RETIRED";
export type SubmissionMode = "INDIVIDUAL" | "GROUP";
export type GradingMode = "GRADED" | "PRACTICE";
export type AssignmentOrigin = "MANUAL" | "CLONE" | "NEW_VERSION" | "CLASS_COPY";

export type AssignmentRow = {
  id: string;
  scope: AssignmentScope;
  classId: string | null;
  subjectId: string;
  title: string;
  submissionMode: SubmissionMode;
  gradingMode: GradingMode;
  diagramEnabled: boolean;
  status: AssignmentStatus;
  versionNo: number;
  origin: AssignmentOrigin;
  opensAt: string | null;
  closesAt: string | null;
  lateUntil: string | null;
  maxAttempts: number | null;
  updatedAt: string;
  readOnly: boolean;
};

export type AssignmentPage = {
  items: AssignmentRow[];
  hiddenCount: number;
  counts: Partial<Record<AssignmentStatus, number>>;
  groupSupported: boolean;
  ownerArchived: boolean;
};

export type PartView = {
  partId: string;
  headingBlockId: string | null;
  ancestorHeadingIds: string[];
  title: string;
  rubricId: string | null;
  criteriaCount: number;
  totalPoints: number;
  rubricComplete: boolean;
  locked: boolean;
};

export type Permissions = {
  canManage: boolean;
  canEditContent: boolean;
  canChangeSchedule: boolean;
  canRetire: boolean;
  canDelete: boolean;
  canReview: boolean;
  canPublish: boolean;
  canClone: boolean;
  canCreateVersion: boolean;
  ownerArchived: boolean;
};

export type AssignmentDetail = {
  maxAiGradingRubricItems?: number;
  id: string;
  scope: AssignmentScope;
  classId: string | null;
  subjectId: string;
  ownerName: string;
  sourceAssignmentId: string | null;
  origin: AssignmentOrigin;
  versionNo: number;
  submissionMode: SubmissionMode;
  diagramEnabled: boolean;
  gradingMode: GradingMode;
  title: string;
  instructions: string;
  skeleton: DocumentModel;
  parts: PartView[];
  totalPoints: number;
  requiredDiagrams: Partial<Record<DiagramType, number>>;
  referenceMaterialIds: string[];
  activityMonitoringEnabled: boolean;
  activityNotice: string | null;
  status: AssignmentStatus;
  opensAt: string | null;
  closesAt: string | null;
  lateUntil: string | null;
  maxAttempts: number | null;
  retiredReason: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
  permissions: Permissions;
  groupSupported: boolean;
  warnings: ValidationIssue[];
};

export type RubricDetail = {
  rubricId: string;
  assignmentId: string;
  partId: string;
  partTitle: string;
  criteria: RubricCriterion[];
  totalPoints: number;
  locked: boolean;
  complete: boolean;
  diagramEnabled: boolean;
  editable: boolean;
  assignmentVersion: number;
};

export type CopySource = {
  classId: string;
  code: string;
  name: string;
  status: string | null;
  assignments: AssignmentRow[];
  /** SUBJECT: bài khác của cùng môn (classId là mã môn); CLASS: một lớp mình dạy. */
  kind?: "CLASS" | "SUBJECT";
};

export type DiagramView = {
  assignmentId: string;
  blockId: string;
  assignmentTitle: string;
  diagramType: DiagramType;
  alt: string;
  xml: string;
  svgPreview: string | null;
};

export type AiTextFormat = Partial<Omit<Run, "text">> & { align?: Align; runs?: Run[] };

export type AiProposalSection = {
  titleFormat?: AiTextFormat;
  guidanceFormat?: AiTextFormat;
  level: number;
  title: string;
  guidance: string;
  rubric: { title: string; description: string; kind: "TEXT" | "DIAGRAM"; items: { text: string; points?: number }[] }[];
};

export type AiDraftValidation = {
  status: "VALID" | "NEEDS_REVIEW" | "INVALID";
  questionCount: number;
  rubricLeafCount: number;
  totalPoints: number;
  issues: { severity: "ERROR" | "WARNING"; code: string; path: string; message: string }[];
};

export type AiSourcePreview = { fileName: string; previewText: string; extractedChars: number; truncated: boolean };

export type AiDraft = {
  draftId: string;
  assignmentId: string;
  origin: "AI_TASK" | "MCP";
  status: "PENDING" | "READY" | "APPLIED" | "DISCARDED" | "FAILED";
  taskId: string | null;
  /** {@code unchanged}: bản sửa đề bằng AI không đổi gì so với đề đã gửi. */
  proposal: { title: string; businessContextFormat?: AiTextFormat; businessContext: string; instructions: string; sections: AiProposalSection[]; validation?: AiDraftValidation;
    unchanged?: boolean } | null;
  provenance: Record<string, unknown>;
  safeError: string | null;
  baseVersion: number;
  createdAt: string;
  decidedAt: string | null;
};

/** Cảnh báo của bản nhập đề (BR-U07-89, 94, 96). */
export type ImportWarning = {
  kind: "ILLEGIBLE" | "HEADER_FOOTER_REMOVED" | "ROTATED_PAGE" | "LOW_RESOLUTION" | "FIGURE_CROP_FAILED" | "OTHER";
  page?: number;
  message: string;
};

/** Đề xuất của bản nháp nhập đề từ ảnh/PDF (BR-U07-97). */
export type ImportProposal = {
  kind: "IMPORT";
  title: string;
  document: DocumentModel;
  warnings: ImportWarning[];
  counts: { headings: number; tables: number; images: number; illegible: number };
  /** Id khối ảnh → link tải 5 phút của hình đã cắt, chỉ để xem trước (máy chủ cấp mỗi lần đọc bản nháp). */
  previews?: Record<string, string>;
};

/** Bản nháp nhập đề: cùng API bản nháp AI, đề xuất có {@code kind: "IMPORT"}. */
export type ImportDraft = Omit<AiDraft, "proposal"> & { proposal: ImportProposal | null };

export type ImportPlacement = "REPLACE" | "APPEND";

export type ImportPreview = {
  document: DocumentModel;
  warnings: ValidationIssue[];
  headingCount: number;
  imageCount: number;
  diagramCount: number;
};

export type Material = { materialId: string; title: string; hasText: boolean };

export type DetailsInput = Partial<{
  title: string;
  instructions: string;
  diagramEnabled: boolean;
  gradingMode: GradingMode;
  requiredDiagrams: Partial<Record<DiagramType, number>>;
  referenceMaterialIds: string[];
  activityMonitoringEnabled: boolean;
  activityNotice: string;
}>;

export type ScheduleInput = {
  opensAt?: string | null;
  openNow?: boolean;
  closesAt: string;
  lateUntil?: string | null;
  /** Chỉ bài Luyện tập; bài có chấm gửi null (yêu cầu 2026-11-10). */
  maxAttempts: number | null;
  confirmUngroupedStudents?: boolean;
};

export const statusLabels: Record<AssignmentStatus, string> = {
  // Không còn bước duyệt (yêu cầu 2026-10-22): bài đã duyệt từ trước hiện như bản nháp.
  DRAFT: "Nháp", REVIEWED: "Hoàn tất", SCHEDULED: "Đã lên lịch", OPEN: "Đang mở", CLOSED: "Đã đóng", RETIRED: "Ngưng giao",
};

export const statusTones: Record<AssignmentStatus, Tone> = {
  DRAFT: "neutral", REVIEWED: "info", SCHEDULED: "accent", OPEN: "success", CLOSED: "warning", RETIRED: "danger",
};

export const gradingLabels: Record<GradingMode, string> = { GRADED: "Graded", PRACTICE: "Practice" };
export const submissionLabels: Record<SubmissionMode, string> = { INDIVIDUAL: "Cá nhân", GROUP: "Nhóm" };
export const originLabels: Record<AssignmentOrigin, string> = {
  MANUAL: "Tạo mới", CLONE: "Nhân bản", NEW_VERSION: "Version mới", CLASS_COPY: "Copy từ bài khác",
};
