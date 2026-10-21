import { apiFetch } from "@/shared/api/client";
import type { DocumentModel, RubricCriterion } from "@/shared/document";
import type {
  AiDraft,
  AssignmentDetail,
  AssignmentPage,
  AssignmentStatus,
  CopySource,
  DetailsInput,
  DiagramView,
  GradingMode,
  ImportPreview,
  Material,
  RubricDetail,
  ScheduleInput,
  SubmissionMode,
} from "./types";

const base = "/api/v1";
const enc = encodeURIComponent;

function write<T>(path: string, method: string, body?: unknown) {
  return apiFetch<T>(base + path, { method, body: body === undefined ? undefined : JSON.stringify(body) });
}

export type ListFilter = { status?: AssignmentStatus[]; includeClosed?: boolean; q?: string; gradingMode?: GradingMode };

function listQuery(filter: ListFilter = {}) {
  const params = new URLSearchParams();
  filter.status?.forEach((status) => params.append("status", status));
  if (filter.includeClosed) params.set("includeClosed", "true");
  if (filter.q) params.set("q", filter.q);
  if (filter.gradingMode) params.set("gradingMode", filter.gradingMode);
  const text = params.toString();
  return text ? `?${text}` : "";
}

export type CreateInput = { submissionMode: SubmissionMode; gradingMode: GradingMode; diagramEnabled: boolean; title?: string };

export const assignmentsApi = {
  listForClass: (classId: string, filter?: ListFilter, signal?: AbortSignal) =>
    apiFetch<AssignmentPage>(`${base}/classes/${enc(classId)}/assignments${listQuery(filter)}`, { signal }),
  listForSubject: (subjectId: string, filter?: ListFilter, signal?: AbortSignal) =>
    apiFetch<AssignmentPage>(`${base}/subjects/${enc(subjectId)}/assignments${listQuery(filter)}`, { signal }),
  createForClass: (classId: string, input: CreateInput) =>
    write<AssignmentDetail>(`/classes/${enc(classId)}/assignments`, "POST", input),
  createForSubject: (subjectId: string, input: CreateInput) =>
    write<AssignmentDetail>(`/subjects/${enc(subjectId)}/assignments`, "POST", input),
  copySources: (classId: string, signal?: AbortSignal) =>
    apiFetch<CopySource[]>(`${base}/classes/${enc(classId)}/assignment-copy-sources`, { signal }),
  copy: (classId: string, sourceAssignmentId: string) =>
    write<AssignmentDetail>(`/classes/${enc(classId)}/assignments:copy`, "POST", { sourceAssignmentId }),
  detail: (id: string, signal?: AbortSignal) => apiFetch<AssignmentDetail>(`${base}/assignments/${enc(id)}`, { signal }),
  updateDetails: (id: string, version: number, input: DetailsInput) =>
    write<AssignmentDetail>(`/assignments/${enc(id)}`, "PATCH", { ...input, version }),
  saveSkeleton: (id: string, version: number, skeleton: DocumentModel, options: { instructions?: string; fromDocx?: boolean } = {}) =>
    write<AssignmentDetail>(`/assignments/${enc(id)}/skeleton`, "PUT", { version, skeleton, ...options }),
  remove: (id: string, version: number) => write<void>(`/assignments/${enc(id)}?version=${version}`, "DELETE"),
  review: (id: string, version: number) => write<AssignmentDetail>(`/assignments/${enc(id)}/review`, "POST", { version }),
  publish: (id: string, version: number, input: ScheduleInput) =>
    write<AssignmentDetail>(`/assignments/${enc(id)}/publish`, "POST", { ...input, version }),
  changeSchedule: (id: string, version: number, input: ScheduleInput) =>
    write<AssignmentDetail>(`/assignments/${enc(id)}/schedule`, "PATCH", { ...input, version }),
  retire: (id: string, version: number, reason: string) =>
    write<AssignmentDetail>(`/assignments/${enc(id)}/retire`, "POST", { version, reason }),
  clone: (id: string) => write<AssignmentDetail>(`/assignments/${enc(id)}/clone`, "POST"),
  newVersion: (id: string) => write<AssignmentDetail>(`/assignments/${enc(id)}/versions`, "POST"),
  rubric: (id: string, partId: string, signal?: AbortSignal) =>
    apiFetch<RubricDetail>(`${base}/assignments/${enc(id)}/parts/${enc(partId)}/rubric`, { signal }),
  saveRubric: (id: string, partId: string, version: number, criteria: RubricCriterion[]) =>
    write<RubricDetail>(`/assignments/${enc(id)}/parts/${enc(partId)}/rubric`, "PUT", { version, criteria }),
  materials: (id: string, signal?: AbortSignal) =>
    apiFetch<Material[]>(`${base}/assignments/${enc(id)}/selectable-materials`, { signal }),
  diagram: (id: string, blockId: string, signal?: AbortSignal) =>
    apiFetch<DiagramView>(`${base}/assignments/${enc(id)}/diagrams/${enc(blockId)}`, { signal }),
  importDocx: (id: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return apiFetch<ImportPreview>(`${base}/assignments/${enc(id)}/skeleton:import-docx`, { method: "POST", body: form });
  },
  requestAiDraft: (id: string, input: { request: string; materialIds: string[]; source: "SHARED" | "PERSONAL"; provider?: string; requestRef: string }) =>
    write<AiDraft>(`/assignments/${enc(id)}/ai-drafts`, "POST", input),
  aiDraft: (id: string, draftId: string, signal?: AbortSignal) =>
    apiFetch<AiDraft>(`${base}/assignments/${enc(id)}/ai-drafts/${enc(draftId)}`, { signal }),
  applyAiDraft: (id: string, draftId: string, version: number, replaceInstructions: boolean) =>
    write<AssignmentDetail>(`/assignments/${enc(id)}/ai-drafts/${enc(draftId)}:apply`, "POST", { version, replaceInstructions }),
  discardAiDraft: (id: string, draftId: string) => write<AiDraft>(`/assignments/${enc(id)}/ai-drafts/${enc(draftId)}:discard`, "POST"),
};

export function imageUrlFor(assignmentId: string) {
  return (blockId: string) => `${base}/assignments/${enc(assignmentId)}/blocks/${enc(blockId)}/image`;
}

export function docxUrl(assignmentId: string, includeRubric: boolean) {
  return `${base}/assignments/${enc(assignmentId)}/docx?includeRubric=${includeRubric}`;
}
