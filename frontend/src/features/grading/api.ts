import { apiFetch, apiDownload } from "@/shared/api/client";
import type {
  AiRequest,
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
} from "./types";
const root = "/api/v1";
const query = (values: Record<string, string | number | undefined>) => {
  const q = new URLSearchParams();
  Object.entries(values).forEach(([k, v]) => {
    if (v !== undefined) q.set(k, String(v));
  });
  return q.toString();
};
const write = <T>(path: string, body: unknown, method = "POST") =>
  apiFetch<T>(root + path, { method, body: JSON.stringify(body) });
export const gradingApi = {
  submissions: (classId: string, assignmentId: string, status = "ALL", page = 1, late?: string) =>
    apiFetch<SubmissionPage>(
      `${root}/classes/${classId}/assignments/${assignmentId}/submissions?${query({ status, page, size: 20, late: late || undefined })}`,
    ),
  detail: (id: string) => apiFetch<Detail>(`${root}/evaluations/${id}`),
  save: (id: string, body: Save) => write<Evaluation>(`/evaluations/${id}`, body, "PUT"),
  finalize: (items: ItemVersion[]) => write<BatchResult>("/evaluations:finalize", { items }),
  publish: (items: ItemVersion[]) => write<BatchResult>("/evaluations:publish", { items }),
  proposals: (id: string) => apiFetch<Proposal[]>(`${root}/evaluations/${id}/ai-proposals`),
  request: (id: string, input: AiRequest) =>
    write<Proposal>(`/evaluations/${id}/ai-proposals`, input),
  requestBatch: (items: ItemVersion[], request: AiRequest) =>
    write<BatchResult>("/evaluations:ai-proposals", { items, request }),
  gradebook: (classId: string, q = "", page = 1) =>
    apiFetch<GradebookPage>(`${root}/classes/${classId}/gradebook?${query({ q, page, size: 20 })}`),
  export: (classId: string, format: string, assignmentId?: string) =>
    apiDownload(`${root}/classes/${classId}/gradebook/export?${query({ format, assignmentId })}`),
  progress: (classId: string) =>
    apiFetch<Record<string, SubmissionPage>>(`${root}/classes/${classId}/submission-progress`),
  student: (classId: string, studentId: string) =>
    apiFetch<GradebookRow>(`${root}/classes/${classId}/students/${studentId}/grades`),
  myGrades: (classId: string) => apiFetch<StudentGrade[]>(`${root}/me/grades?classId=${classId}`),
  attempt: (attemptId: string) => apiFetch<StudentGrade>(`${root}/me/grades/attempts/${attemptId}`),
  distribution: (classId: string, assignmentId: string) =>
    apiFetch<{ label: string; count: number }[]>(
      `${root}/me/grade-distributions?${query({ classId, assignmentId })}`,
    ),
  member: (id: string, accountId: string, version: number, score: string, reason: string | null) =>
    write<Evaluation>(`/evaluations/${id}/members/${accountId}`, { version, score, reason }, "PUT"),
};
