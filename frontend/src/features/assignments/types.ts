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
  activityRetentionConfigured: boolean;
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
  status: string;
  assignments: AssignmentRow[];
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

export type AiProposalSection = {
  level: number;
  title: string;
  guidance: string;
  rubric: { title: string; description: string; kind: "TEXT" | "DIAGRAM"; items: { text: string; points?: number }[] }[];
};

export type AiDraft = {
  draftId: string;
  assignmentId: string;
  origin: "AI_TASK" | "MCP";
  status: "PENDING" | "READY" | "APPLIED" | "DISCARDED" | "FAILED";
  taskId: string | null;
  proposal: { instructions: string; sections: AiProposalSection[] } | null;
  provenance: Record<string, unknown>;
  safeError: string | null;
  baseVersion: number;
  createdAt: string;
  decidedAt: string | null;
};

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
  maxAttempts: number;
  confirmUngroupedStudents?: boolean;
};

export const statusLabels: Record<AssignmentStatus, string> = {
  DRAFT: "Nháp", REVIEWED: "Đã duyệt", SCHEDULED: "Đã lên lịch", OPEN: "Đang mở", CLOSED: "Đã đóng", RETIRED: "Ngưng giao",
};

export const statusTones: Record<AssignmentStatus, Tone> = {
  DRAFT: "neutral", REVIEWED: "info", SCHEDULED: "accent", OPEN: "success", CLOSED: "warning", RETIRED: "danger",
};

export const gradingLabels: Record<GradingMode, string> = { GRADED: "Graded", PRACTICE: "Practice" };
export const submissionLabels: Record<SubmissionMode, string> = { INDIVIDUAL: "Cá nhân", GROUP: "Nhóm" };
export const originLabels: Record<AssignmentOrigin, string> = {
  MANUAL: "Tạo mới", CLONE: "Nhân bản", NEW_VERSION: "Version mới", CLASS_COPY: "Copy từ lớp khác",
};
