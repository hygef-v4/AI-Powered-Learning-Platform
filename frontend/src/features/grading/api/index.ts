import { apiFetch, apiDownloadFile } from "@/shared/api/client";
import type { NavigationFilters } from "../model/navigation";
import type {
  AiRequest,
  AssignmentContext,
  AiEstimate,
  BatchResult,
  Detail,
  Evaluation,
  GradebookPage,
  GradebookRow,
  ItemVersion,
  Proposal,
  Save,
  StudentGrade,
  SubmissionPage,
} from "../model/types";

const API_ROOT = "/api/v1";

export type GradeDistributionVisibility = { enabled: boolean; version: number; canEdit: boolean };

const buildVisibilityPath = (classId: string, assignmentId: string) =>
  `/classes/${encodeURIComponent(classId)}/assignments/${encodeURIComponent(assignmentId)}/grade-distribution-visibility`;

const buildQuery = (values: Record<string, string | number | undefined>) => {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined) params.set(key, String(value));
  }
  return params.toString();
};

const writeRequest = <T>(path: string, body: unknown, method = "POST") =>
  apiFetch<T>(API_ROOT + path, { method, body: JSON.stringify(body) });

export const gradingApi = {
  loadAssignmentContext: (assignmentId: string) =>
    apiFetch<AssignmentContext>(`${API_ROOT}/assignments/${encodeURIComponent(assignmentId)}`),
  listSubmissions: (classId: string, assignmentId: string, status = "ALL", page = 1, late?: string) => {
    const params = buildQuery({ status, page, size: 20, late: late || undefined });
    return apiFetch<SubmissionPage>(
      `${API_ROOT}/classes/${classId}/assignments/${assignmentId}/submissions?${params}`,
    );
  },
  loadSubmissionProgress: (classId: string) =>
    apiFetch<Record<string, SubmissionPage>>(`${API_ROOT}/classes/${classId}/submission-progress`),

  loadEvaluation: (id: string, filters: NavigationFilters = {}) => {
    const params = buildQuery(filters);
    return apiFetch<Detail>(`${API_ROOT}/evaluations/${id}${params ? `?${params}` : ""}`);
  },
  saveEvaluation: (id: string, body: Save) => writeRequest<Evaluation>(`/evaluations/${id}`, body, "PUT"),
  finalizeGrades: (items: ItemVersion[]) => writeRequest<BatchResult>("/evaluations:finalize", { items }),
  publishGrades: (items: ItemVersion[]) => writeRequest<BatchResult>("/evaluations:publish", { items }),
  saveMemberGrade: (id: string, accountId: string, version: number, score: string, reason: string | null) =>
    writeRequest<Evaluation>(`/evaluations/${id}/members/${accountId}`, { version, score, reason }, "PUT"),

  loadTask: (id: string) =>
    apiFetch<{ state: string; safeError: string | null }>(
      `${API_ROOT}/ai/tasks/${encodeURIComponent(id)}`,
    ),
  listProposals: (id: string) => apiFetch<Proposal[]>(`${API_ROOT}/evaluations/${id}/ai-proposals`),
  requestProposal: (id: string, input: AiRequest) =>
    writeRequest<Proposal>(`/evaluations/${id}/ai-proposals`, input),
  requestProposals: (items: ItemVersion[], request: AiRequest) =>
    writeRequest<BatchResult>("/evaluations:ai-proposals", { items, request }),
  estimateProposalTokens: (items: ItemVersion[]) =>
    writeRequest<AiEstimate>("/evaluations:ai-proposal-estimate", { items }),

  loadGradebook: (classId: string, q = "", page = 1, size = 20) =>
    apiFetch<GradebookPage>(`${API_ROOT}/classes/${classId}/gradebook?${buildQuery({ q, page, size })}`),
  exportGrades: (classId: string, format: string, assignmentId?: string) =>
    apiDownloadFile(`${API_ROOT}/classes/${classId}/gradebook/export?${buildQuery({ format, assignmentId })}`),
  loadStudentGrades: (classId: string, studentId: string) =>
    apiFetch<GradebookRow>(`${API_ROOT}/classes/${classId}/students/${studentId}/grades`),
  listMyGrades: (classId: string) => apiFetch<StudentGrade[]>(`${API_ROOT}/me/grades?classId=${classId}`),
  loadAttemptGrade: (attemptId: string) => apiFetch<StudentGrade>(`${API_ROOT}/me/grades/attempts/${attemptId}`),

  loadDistributionVisibility: (classId: string, assignmentId: string, signal?: AbortSignal) =>
    apiFetch<GradeDistributionVisibility>(
      API_ROOT + buildVisibilityPath(classId, assignmentId),
      { signal },
    ),
  saveDistributionVisibility: (
    classId: string,
    assignmentId: string,
    enabled: boolean,
    version: number,
  ) =>
    writeRequest<GradeDistributionVisibility>(
      buildVisibilityPath(classId, assignmentId),
      { enabled, version },
      "PUT",
    ),
  loadMyGradeDistribution: (classId: string, assignmentId: string) =>
    apiFetch<{ label: string; count: number }[]>(
      `${API_ROOT}/me/grade-distributions?${buildQuery({ classId, assignmentId })}`,
    ),
  loadClassGradeDistribution: (classId: string, assignmentId: string) =>
    apiFetch<{ label: string; count: number }[]>(
      `${API_ROOT}/classes/${classId}/grade-distribution?${buildQuery({ assignmentId })}`,
    ),
};
