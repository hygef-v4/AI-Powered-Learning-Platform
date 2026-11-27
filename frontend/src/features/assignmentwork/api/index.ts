import { buildDiagramPngsRequest, downloadApiFileToDisk, fetchApi } from "@/shared/api/client";
import { renderMissingDiagramPngs } from "@/shared/document/diagramPngRenderer";
import type { ActivityEventInput } from "@/shared/activity/useActivityTracker";
import type { DocumentBlock, DocumentModel } from "@/shared/document";
import type { Attempt, AttemptDiagram, AttemptSummary, Feedback, ImportPreview, Receipt, Saved, StudentDetail, StudentRow } from "../model/types";

const base = "/api/v1";
const enc = encodeURIComponent;

function writeJson<T>(path: string, method: string, body?: unknown, init: RequestInit = {}) {
  return fetchApi<T>(base + path, { ...init, method, body: body === undefined ? undefined : JSON.stringify(body) });
}

export const workApi = {
  list: (classId?: string, signal?: AbortSignal) =>
    fetchApi<StudentRow[]>(`${base}/me/assignments${classId ? `?classId=${enc(classId)}` : ""}`, { signal }),
  detail: (assignmentId: string, classId: string, signal?: AbortSignal) =>
    fetchApi<StudentDetail>(`${base}/me/assignments/${enc(assignmentId)}?classId=${enc(classId)}`, { signal }),
  history: (assignmentId: string, classId: string, signal?: AbortSignal) =>
    fetchApi<AttemptSummary[]>(`${base}/assignments/${enc(assignmentId)}/attempts/mine?classId=${enc(classId)}`, { signal }),
  start: (assignmentId: string, classId: string) => writeJson<Attempt>(`/assignments/${enc(assignmentId)}/attempts`, "POST", { classId }),
  attempt: (attemptId: string, signal?: AbortSignal) => fetchApi<Attempt>(`${base}/attempts/${enc(attemptId)}`, { signal }),
  save: (attemptId: string, contentVersion: number, content: DocumentModel, keepalive = false) =>
    writeJson<Saved>(`/attempts/${enc(attemptId)}/content`, "PUT", { contentVersion, content }, { keepalive }),
  submit: (attemptId: string, contentVersion: number) => writeJson<Receipt>(`/attempts/${enc(attemptId)}/submit`, "POST", { contentVersion }),
  importDocx: (attemptId: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return fetchApi<ImportPreview>(`${base}/attempts/${enc(attemptId)}/docx:preview`, { method: "POST", body: form });
  },
  feedback: (attemptId: string, signal?: AbortSignal) => fetchApi<Feedback[]>(`${base}/attempts/${enc(attemptId)}/feedback`, { signal }),
  requestFeedback: (attemptId: string, requestRef: string) =>
    writeJson<Feedback>(`/attempts/${enc(attemptId)}/feedback`, "POST", { requestRef }),
  diagram: (attemptId: string, blockId: string, signal?: AbortSignal) =>
    fetchApi<AttemptDiagram>(`${base}/attempts/${enc(attemptId)}/diagrams/${enc(blockId)}`, { signal }),
  activity: (attemptId: string, events: ActivityEventInput[], keepalive: boolean) =>
    writeJson<{ accepted: number }>(`/attempts/${enc(attemptId)}/activity`, "POST", { events }, { keepalive }),
};

export function attemptImageUrl(attemptId: string) {
  return (blockId: string) => `${base}/attempts/${enc(attemptId)}/blocks/${enc(blockId)}/image`;
}

/** {@code blocks}: bài làm đang hiện; sơ đồ chưa có ảnh lưu sẵn được trình duyệt dựng ảnh trước khi tải (yêu cầu 2026-11-27). */
export async function downloadAttemptDocx(attemptId: string, blocks: DocumentBlock[] = []) {
  return downloadApiFileToDisk(`${base}/attempts/${enc(attemptId)}/export.docx`, "bai-lam.docx",
    buildDiagramPngsRequest(await renderMissingDiagramPngs(blocks)));
}
