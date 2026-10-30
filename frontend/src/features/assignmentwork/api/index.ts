import { apiFetch } from "@/shared/api/client";
import type { ActivityEventInput } from "@/shared/activity/useActivityTracker";
import type { DocumentModel } from "@/shared/document";
import type { Attempt, AttemptDiagram, AttemptSummary, Feedback, ImportPreview, Receipt, Saved, StudentDetail, StudentRow } from "../model/types";

const base = "/api/v1";
const enc = encodeURIComponent;

function write<T>(path: string, method: string, body?: unknown, init: RequestInit = {}) {
  return apiFetch<T>(base + path, { ...init, method, body: body === undefined ? undefined : JSON.stringify(body) });
}

export const workApi = {
  list: (classId?: string, signal?: AbortSignal) =>
    apiFetch<StudentRow[]>(`${base}/me/assignments${classId ? `?classId=${enc(classId)}` : ""}`, { signal }),
  detail: (assignmentId: string, classId: string, signal?: AbortSignal) =>
    apiFetch<StudentDetail>(`${base}/me/assignments/${enc(assignmentId)}?classId=${enc(classId)}`, { signal }),
  history: (assignmentId: string, classId: string, signal?: AbortSignal) =>
    apiFetch<AttemptSummary[]>(`${base}/assignments/${enc(assignmentId)}/attempts/mine?classId=${enc(classId)}`, { signal }),
  start: (assignmentId: string, classId: string) => write<Attempt>(`/assignments/${enc(assignmentId)}/attempts`, "POST", { classId }),
  attempt: (attemptId: string, signal?: AbortSignal) => apiFetch<Attempt>(`${base}/attempts/${enc(attemptId)}`, { signal }),
  save: (attemptId: string, contentVersion: number, content: DocumentModel, keepalive = false) =>
    write<Saved>(`/attempts/${enc(attemptId)}/content`, "PUT", { contentVersion, content }, { keepalive }),
  submit: (attemptId: string, contentVersion: number) => write<Receipt>(`/attempts/${enc(attemptId)}/submit`, "POST", { contentVersion }),
  receipt: (attemptId: string, signal?: AbortSignal) => apiFetch<Receipt>(`${base}/attempts/${enc(attemptId)}/receipt`, { signal }),
  importDocx: (attemptId: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return apiFetch<ImportPreview>(`${base}/attempts/${enc(attemptId)}/docx:preview`, { method: "POST", body: form });
  },
  feedback: (attemptId: string, signal?: AbortSignal) => apiFetch<Feedback[]>(`${base}/attempts/${enc(attemptId)}/feedback`, { signal }),
  requestFeedback: (attemptId: string, provider: string, requestRef: string) =>
    write<Feedback>(`/attempts/${enc(attemptId)}/feedback`, "POST", { provider, requestRef }),
  diagram: (attemptId: string, blockId: string, signal?: AbortSignal) =>
    apiFetch<AttemptDiagram>(`${base}/attempts/${enc(attemptId)}/diagrams/${enc(blockId)}`, { signal }),
  activity: (attemptId: string, events: ActivityEventInput[], keepalive: boolean) =>
    write<{ accepted: number }>(`/attempts/${enc(attemptId)}/activity`, "POST", { events }, { keepalive }),
};

export function attemptImageUrl(attemptId: string) {
  return (blockId: string) => `${base}/attempts/${enc(attemptId)}/blocks/${enc(blockId)}/image`;
}

export function exportUrl(attemptId: string) {
  return `${base}/attempts/${enc(attemptId)}/export.docx`;
}
