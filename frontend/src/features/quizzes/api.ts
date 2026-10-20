import { apiFetch } from "@/shared/api/client";
import type { ImportResult, ManagedSubject, QuestionContent, QuestionDetail, QuestionPage, QuestionQuery } from "./types";

const enc = encodeURIComponent;
const questionPath = (id: string) => `/api/v1/questions/${enc(id)}`;
const bankPath = (subjectId: string) => `/api/v1/subjects/${enc(subjectId)}/questions`;

/** Chỉ gửi đúng các trường nội dung; server vẫn tự chặn trường lạ. */
function contentBody(content: QuestionContent) {
  const { type, stem, options, correctOptionIds, explanation, defaultPoints, difficulty, tags } = content;
  return {
    type,
    stem,
    options: options.map(({ id, text }) => ({ id, text })),
    correctOptionIds,
    explanation,
    defaultPoints,
    difficulty,
    tags,
  };
}

export function getManagedSubjects(signal?: AbortSignal): Promise<ManagedSubject[]> {
  return apiFetch<ManagedSubject[]>("/api/v1/manager/subjects", { signal });
}

export function getQuestions(subjectId: string, query: QuestionQuery = {}, signal?: AbortSignal): Promise<QuestionPage> {
  const params = new URLSearchParams();
  for (const key of ["type", "difficulty", "status", "page", "size"] as const) {
    if (query[key] !== undefined) params.set(key, String(query[key]));
  }
  if (query.q?.trim()) params.set("q", query.q.trim());
  return apiFetch<QuestionPage>(`${bankPath(subjectId)}${params.size ? `?${params}` : ""}`, { signal });
}

export function getQuestion(id: string, signal?: AbortSignal): Promise<QuestionDetail> {
  return apiFetch<QuestionDetail>(questionPath(id), { signal });
}

export function createQuestion(subjectId: string, content: QuestionContent): Promise<QuestionDetail> {
  return apiFetch<QuestionDetail>(bankPath(subjectId), { method: "POST", body: JSON.stringify(contentBody(content)) });
}

export function updateQuestion(id: string, content: QuestionContent, version: number): Promise<QuestionDetail> {
  return apiFetch<QuestionDetail>(questionPath(id), {
    method: "PATCH",
    body: JSON.stringify({ ...contentBody(content), version }),
  });
}

/** Sửa câu đang dùng: mở (hoặc lấy lại) bản nháp mới cùng lineage (BR-U10-04). */
export function openDraft(id: string): Promise<QuestionDetail> {
  return apiFetch<QuestionDetail>(`${questionPath(id)}/versions`, { method: "POST" });
}

export function activateQuestion(id: string, version: number): Promise<QuestionDetail> {
  return apiFetch<QuestionDetail>(`${questionPath(id)}/activate`, { method: "POST", body: JSON.stringify({ version }) });
}

export function retireQuestion(id: string, version: number): Promise<QuestionDetail> {
  return apiFetch<QuestionDetail>(`${questionPath(id)}/retire`, { method: "POST", body: JSON.stringify({ version }) });
}

export function deleteQuestion(id: string, version: number): Promise<void> {
  return apiFetch<void>(`${questionPath(id)}?version=${version}`, { method: "DELETE" });
}

/** Nhập tệp: `dryRun` chỉ kiểm từng dòng, không có thì ghi các dòng hợp lệ (D3). */
export function importQuestions(subjectId: string, file: File, dryRun: boolean): Promise<ImportResult> {
  const body = new FormData();
  body.append("file", file);
  return apiFetch<ImportResult>(`${bankPath(subjectId)}:import${dryRun ? "?dryRun=true" : ""}`, { method: "POST", body });
}
