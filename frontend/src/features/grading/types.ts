export type Decimal = string | number;
export type GradeStatus = "PENDING" | "DRAFT" | "FINALIZED" | "PUBLISHED" | "SUPERSEDED";
export type Checks = Record<string, string[]>;
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
export type DocumentModel = {
  blocks: { id: string; type: string; content: Record<string, unknown> }[];
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
  adjustment: Decimal;
  adjustmentReason: string | null;
  feedback: string | null;
  publishedAt: string | null;
  changes: { actorId: string; at: string; before: Decimal; after: Decimal; reason: string }[];
};
export type Detail = {
  evaluation: Evaluation;
  document: DocumentModel;
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
};
export type Person = { accountId: string; name: string; email: string };
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
  title: string;
  ownerName: string;
};
export type SubmissionPage = {
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
};
export type Proposal = {
  id: string;
  versionNo: number;
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
    feedback?: string;
    sections?: { partId: string; feedback: string }[];
  };
};
export type AiRequest = {
  source: "SHARED" | "PERSONAL";
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
};
export const statusLabels: Record<string, string> = {
  PENDING: "Chờ chấm",
  DRAFT: "Đã chấm (nháp)",
  FINALIZED: "Đã chốt",
  PUBLISHED: "Đã công bố",
  SUPERSEDED: "Lượt cũ",
  NOT_SUBMITTED: "Chưa nộp",
  IN_PROGRESS: "Đang làm",
};
export function points(score: Decimal | null | undefined, max: Decimal | null | undefined) {
  return score == null ? "—" : `${Number(score).toFixed(2)} / ${Number(max ?? 0).toFixed(2)}`;
}

export const activityLabels: Record<string, string> = {
  DISABLED: "Không bật theo dõi hoạt động",
  NO_DATA: "Chưa có dữ liệu hoạt động",
  AVAILABLE: "Có dữ liệu hoạt động",
  EXPIRED: "Dữ liệu hoạt động đã hết thời hạn lưu",
  UNAVAILABLE: "Tạm thời không đọc được dữ liệu hoạt động",
  PER_MEMBER: "Hoạt động theo từng thành viên",
};

export type GradingFormValues = {
  checks: Checks;
  adjustment: string;
  adjustmentReason: string;
  feedback: string;
  changeReason: string;
  usedProposalId: string | null;
};
