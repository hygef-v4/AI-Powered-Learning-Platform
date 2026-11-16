import type { SubmissionDocumentDto } from "./submissionDocument";
import type { ContributionLevel } from "./contributionLevels";
import { msg } from "@/i18n/msg";

export type Decimal = string | number;
export type GradeStatus = "PENDING" | "DRAFT" | "FINALIZED" | "PUBLISHED" | "SUPERSEDED";
export type Checks = Record<string, string[]>;
export type ItemScores = Record<string, Record<string, Decimal>>;
export type StructuredFeedbackSummary = {
  strengths: string[];
  improvements: string[];
  nextSteps: string[];
};
export type StructuredFeedbackItem = {
  rubricId: string;
  itemId: string;
  evidence: string | null;
  reason: string | null;
  suggestion: string | null;
  evidenceBlockId: string | null;
  requiresReview: boolean;
  reviewReason: string | null;
};
export type StructuredFeedback = {
  schemaVersion: number;
  summary: StructuredFeedbackSummary;
  items: StructuredFeedbackItem[];
};
export type StudentRubricItem = {
  rubricId: string;
  partId: string;
  criterionTitle: string;
  itemId: string;
  itemText: string;
  awardedPoints: Decimal;
  maxPoints: Decimal;
  evidence: string | null;
  reason: string | null;
  suggestion: string | null;
};
export type Rubric = {
  rubricId: string;
  partId: string;
  maxPoints: Decimal;
  criteria: {
    criterionId: string;
    title: string;
    items: { itemId: string; text: string; points: Decimal }[];
  }[];
};
export type Evaluation = {
  id: string;
  assignmentId: string;
  classId: string;
  attemptId: string | null;
  groupDocumentId: string | null;
  accountId: string | null;
  kind: "ATTEMPT" | "GROUP_DOCUMENT" | "MEMBER";
  submissionHash: string;
  state: GradeStatus;
  score: Decimal | null;
  maxScore: Decimal;
  version: number;
  rubricChecks: string;
  rubricItemScores?: ItemScores;
  structuredFeedback?: StructuredFeedback | null;
  reviewedItemScores?: ItemScores;
  acknowledgedReviewItems?: Checks;
  memberOverride?: boolean;
  contributionLevel?: ContributionLevel | null;
  adjustment: Decimal;
  adjustmentReason: string | null;
  feedback: string | null;
  publishedAt: string | null;
  changes: {
    actorId: string;
    at: string;
    before: Decimal;
    after: Decimal;
    reason: string;
    previousItemScores?: ItemScores;
    previousStructuredFeedback?: StructuredFeedback | null;
  }[];
};
export type Detail = {
  editable?: boolean;
  readOnlyReason?: string | null;
  evaluation: Evaluation;
  document: SubmissionDocumentDto;
  rubrics: Rubric[];
  navigationIds: string[];
  activityState: string;
  activity: null | {
    awayCount: number;
    knownAwayDuration: string;
    unknownGaps: number;
    reliabilityNote: string;
  };
  members: Evaluation[];
  memberActivities: { student: Person; activity: Detail["activity"]; state: string }[];
  sectionAuthors: { partId: string; accountId: string }[];
  current: boolean;
  groupEvaluationId?: string | null;
};
export type Person = { accountId: string; name: string; email: string; studentCode?: string | null };
export type SubmissionRow = {
  student: Person;
  evaluationId: string | null;
  attemptId: string | null;
  status: string;
  score: Decimal | null;
  maxScore: Decimal | null;
  late: boolean;
  submittedAt: string | null;
  attemptCount: number;
  version: number;
};
export type AssignmentContext = {
  id: string;
  scope: "CLASS" | "SUBJECT";
  classId: string | null;
  subjectId?: string | null;
  title: string;
  ownerName: string;
};
export type SubmissionPage = {
  editable?: boolean;
  readOnlyReason?: string | null;
  rows: SubmissionRow[];
  total: number;
  page: number;
  size: number;
  summary: Record<string, number>;
  deadline: string | null;
};
export type ItemVersion = { evaluationId: string; version: number };
export type BatchResult = {
  items: {
    evaluationId: string;
    outcome: string;
    code: string | null;
    newVersion: number | null;
  }[];
};
export type AiEstimate = { estimatedTokens: number; items: number };
export type Save = {
  version: number;
  checkedItemsByRubric: Checks;
  adjustment: string;
  adjustmentReason: string | null;
  feedback: string;
  changeReason: string | null;
  usedProposalId: string | null;
  itemScoresByRubric?: ItemScores;
  structuredFeedback?: StructuredFeedback | null;
  clearStructuredFeedback?: boolean;
  acknowledgedReviewItems?: Checks;
  reviewedItemScores?: ItemScores;
};
export type Proposal = {
  id: string;
  versionNo: number;
  createdAt?: string | null;
  taskId?: string | null;
  taskState?: "QUEUED" | "RUNNING" | "READY" | "FAILED" | "UNKNOWN" | "CANCELLED" | string | null;
  status: string;
  origin: string;
  agentLabel?: string | null;
  provider?: string | null;
  source?: string;
  model?: string | null;
  submissionHash?: string;
  safeError?: string | null;
  proposal?: {
    checks: Checks;
    score: Decimal;
    itemScoresByRubric?: ItemScores;
    feedback?: string;
    sections?: { partId: string; feedback: string }[];
    structuredFeedback?: StructuredFeedback | null;
  };
};
export type AiRequest = {
  /** Bỏ trống: hệ thống tự chọn nguồn. */
  source?: "SHARED" | "PERSONAL";
  provider?: "GEMINI" | "OPENAI" | "CLAUDE";
  requestRef: string;
};
export type GradeCell = {
  assignmentId: string;
  evaluationId: string | null;
  status: string;
  score: Decimal | null;
  maxScore: Decimal;
  late: boolean;
  submittedAt: string | null;
  feedback: string | null;
};
export type GradebookRow = { student: Person; grades: GradeCell[] };
export type GradebookPage = {
  rows: GradebookRow[];
  assignments: { id: string; title: string; maxScore: Decimal; submissionMode: "INDIVIDUAL" | "GROUP" }[];
  total: number;
  page: number;
  size: number;
};
export type StudentGrade = {
  assignmentId: string;
  attemptId: string | null;
  score: Decimal | null;
  maxScore: Decimal | null;
  feedback: string | null;
  publishedAt: string | null;
  updated: boolean;
  rubricChecks: string | null;
  structuredSummary?: StructuredFeedbackSummary | null;
  rubricItems?: StudentRubricItem[];
  rubricScore?: Decimal | null;
  memberOverride?: boolean;
  contributionLevel?: ContributionLevel | null;
  contributionReason?: string | null;
};
export const GRADE_STATUS_LABELS: Record<string, string> = {
  PENDING: msg("grading.model.types.awaitingGrading"),
  DRAFT: msg("grading.model.types.graded"),
  FINALIZED: msg("grading.model.types.graded"),
  PUBLISHED: msg("grading.model.types.published"),
  SUPERSEDED: msg("grading.model.types.oldSubmission"),
  NOT_SUBMITTED: msg("grading.model.types.notSubmitted"),
  IN_PROGRESS: msg("grading.model.types.notSubmitted"),
};
export function formatPoints(score: Decimal | null | undefined, max: Decimal | null | undefined) {
  return score == null ? "—" : `${Number(score).toFixed(2)} / ${Number(max ?? 0).toFixed(2)}`;
}

export const ACTIVITY_LABELS: Record<string, string> = {
  DISABLED: msg("grading.model.types.activityMonitoringOff"),
  NO_DATA: msg("grading.model.types.noActivityDataYet"),
  AVAILABLE: msg("grading.model.types.activityDataAvailable"),
  EXPIRED: msg("grading.model.types.activityDataRetentionPeriodHas"),
  UNAVAILABLE: msg("grading.model.types.activityDataTemporarilyUnavailable"),
  PER_MEMBER: msg("grading.model.types.activityByMember"),
};

export type GradingFormValues = {
  checks: Checks;
  itemScores?: ItemScores;
  structuredFeedback?: StructuredFeedback | null;
  clearStructuredFeedback?: boolean;
  acknowledgedReviewItems?: Checks;
  reviewedItemScores?: ItemScores;
  adjustment: string;
  feedback: string;
  changeReason: string;
  usedProposalId: string | null;
};
