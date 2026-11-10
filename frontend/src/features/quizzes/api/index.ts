import { apiFetch } from "@/shared/api/client";
import type {
  AiDraft, AiDraftInput, BankAcceptResult, BankTag, BulkItem, BulkResult, Difficulty, DrawRuleInput, DuplicateGroup,
  ImportResult, ManagedSubject, PaperPreview, ProposalView, QuestionContent, QuestionDetail, QuestionPage, QuestionQuery,
  QuizDetail, QuizDetailsInput, QuizItemInput, QuizPage, QuizPaperMode, QuizRow, QuizStatus, RandomPreview,
  SelectableMaterial,
} from "../model/types";

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

export function approveQuestion(id: string, version: number): Promise<QuestionDetail> {
  return apiFetch<QuestionDetail>(`${questionPath(id)}/approve`, { method: "POST", body: JSON.stringify({ version }) });
}

export function rejectQuestion(id: string, reason: string, version: number): Promise<QuestionDetail> {
  return apiFetch<QuestionDetail>(`${questionPath(id)}/reject`, { method: "POST", body: JSON.stringify({ reason, version }) });
}

/** Thao tác hàng loạt trên ngân hàng: câu lỗi không chặn các câu khác. */
export function bulkQuestions(subjectId: string, action: "activate" | "approve" | "reject", items: BulkItem[], reason?: string): Promise<BulkResult> {
  const body = action === "reject" ? { items: items.map(({ id, version }) => ({ id, version })), reason } : { items: items.map(({ id, version }) => ({ id, version })) };
  return apiFetch<BulkResult>(`${bankPath(subjectId)}:${action}`, { method: "POST", body: JSON.stringify(body) });
}

export function getDuplicateQuestions(subjectId: string, signal?: AbortSignal): Promise<DuplicateGroup[]> {
  return apiFetch<DuplicateGroup[]>(`${bankPath(subjectId)}:duplicates`, { signal });
}

// --- Giảng viên đề xuất câu vào ngân hàng (BR-U10-09)

const proposalPath = (classId: string) => `/api/v1/classes/${enc(classId)}/question-proposals`;

export function getProposals(classId: string, signal?: AbortSignal): Promise<ProposalView[]> {
  return apiFetch<ProposalView[]>(proposalPath(classId), { signal });
}

export function proposeQuestion(classId: string, content: QuestionContent): Promise<ProposalView> {
  return apiFetch<ProposalView>(proposalPath(classId), { method: "POST", body: JSON.stringify(contentBody(content)) });
}

export function resubmitProposal(classId: string, id: string, content: QuestionContent, version: number): Promise<ProposalView> {
  return apiFetch<ProposalView>(`${proposalPath(classId)}/${enc(id)}`, { method: "PATCH", body: JSON.stringify({ ...contentBody(content), version }) });
}

export function withdrawProposal(classId: string, id: string, version: number): Promise<void> {
  return apiFetch<void>(`${proposalPath(classId)}/${enc(id)}?version=${version}`, { method: "DELETE" });
}

/** Nhập tệp: `dryRun` chỉ kiểm từng dòng, không có thì ghi các dòng hợp lệ (D3). */
export function importQuestions(subjectId: string, file: File, dryRun: boolean): Promise<ImportResult> {
  const body = new FormData();
  body.append("file", file);
  return apiFetch<ImportResult>(`${bankPath(subjectId)}:import${dryRun ? "?dryRun=true" : ""}`, { method: "POST", body });
}

// --- Quiz (UC 48)

const quizPath = (id: string) => `/api/v1/quizzes/${enc(id)}`;

function post<T>(path: string, body?: unknown): Promise<T> {
  return apiFetch<T>(path, { method: "POST", ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
}

function queryString(values: Record<string, string | number | undefined | null>): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined && value !== null && String(value).trim() !== "") params.set(key, String(value).trim());
  }
  return params.size ? `?${params}` : "";
}

export function getSubjectQuizzes(subjectId: string, query: { status?: QuizStatus; q?: string; page?: number; size?: number } = {},
  signal?: AbortSignal): Promise<QuizPage> {
  return apiFetch<QuizPage>(`/api/v1/subjects/${enc(subjectId)}/quizzes${queryString(query)}`, { signal });
}

export function createSubjectQuiz(subjectId: string, title: string): Promise<QuizDetail> {
  return post<QuizDetail>(`/api/v1/subjects/${enc(subjectId)}/quizzes`, { title });
}

export function getClassQuizzes(classId: string, signal?: AbortSignal): Promise<QuizRow[]> {
  return apiFetch<QuizRow[]>(`/api/v1/classes/${enc(classId)}/quizzes`, { signal });
}

export function createClassQuiz(classId: string, title: string): Promise<QuizDetail> {
  return post<QuizDetail>(`/api/v1/classes/${enc(classId)}/quizzes`, { title });
}

export function getCopySources(classId: string): Promise<QuizRow[]> {
  return apiFetch<QuizRow[]>(`/api/v1/classes/${enc(classId)}/quiz-copy-sources`);
}

export function copyQuizToClass(classId: string, sourceQuizId: string): Promise<QuizDetail> {
  return post<QuizDetail>(`/api/v1/classes/${enc(classId)}/quizzes:copy`, { sourceQuizId });
}

export function getQuiz(id: string, classId?: string | null, signal?: AbortSignal): Promise<QuizDetail> {
  return apiFetch<QuizDetail>(`${quizPath(id)}${queryString({ classId })}`, { signal });
}

export function updateQuizDetails(id: string, input: QuizDetailsInput, version: number): Promise<QuizDetail> {
  const { title, instructions, instructionsDocument, settings, activityMonitoringEnabled, activityNotice } = input;
  return apiFetch<QuizDetail>(quizPath(id), {
    method: "PATCH",
    body: JSON.stringify({ title, instructions, instructionsDocument, settings, activityMonitoringEnabled, activityNotice, version }),
  });
}

export function saveQuizItems(id: string, items: QuizItemInput[], version: number): Promise<QuizDetail> {
  return apiFetch<QuizDetail>(`${quizPath(id)}/items`, {
    method: "PUT",
    body: JSON.stringify({ items: items.map(({ questionId, points }) => ({ questionId, points })), version }),
  });
}

export function previewRandomItems(id: string, count: number, difficulty: Difficulty | null, tag: string | null): Promise<RandomPreview> {
  return post<RandomPreview>(`${quizPath(id)}/items:random-preview`, { count, difficulty, tag });
}

export function getBankQuestionsForQuiz(id: string, query: { difficulty?: Difficulty; tag?: string; q?: string; page?: number; size?: number } = {},
  signal?: AbortSignal): Promise<QuestionPage> {
  return apiFetch<QuestionPage>(`${quizPath(id)}/bank-questions${queryString(query)}`, { signal });
}

export function addInlineQuestion(id: string, content: QuestionContent, version: number): Promise<QuizDetail> {
  return post<QuizDetail>(`${quizPath(id)}/inline-questions`, { ...contentBody(content), version });
}

export function updateInlineQuestion(id: string, questionId: string, content: QuestionContent, version: number): Promise<QuizDetail> {
  return apiFetch<QuizDetail>(`${quizPath(id)}/inline-questions/${enc(questionId)}`, {
    method: "PATCH",
    body: JSON.stringify({ ...contentBody(content), version }),
  });
}

export function deleteInlineQuestion(id: string, questionId: string, version: number): Promise<QuizDetail> {
  return apiFetch<QuizDetail>(`${quizPath(id)}/inline-questions/${enc(questionId)}?version=${version}`, { method: "DELETE" });
}

/** Lưu chế độ đề và quy tắc sinh đề (BR-U10-17). */
export function saveDrawRules(id: string, paperMode: QuizPaperMode, rules: DrawRuleInput[], version: number): Promise<QuizDetail> {
  return apiFetch<QuizDetail>(`${quizPath(id)}/draw-rules`, {
    method: "PUT",
    body: JSON.stringify({
      paperMode, version,
      rules: rules.map(({ tag, difficulty, questionCount, pointsEach }) => ({ tag, difficulty, questionCount, pointsEach })),
    }),
  });
}

export function getBankTags(id: string, signal?: AbortSignal): Promise<BankTag[]> {
  return apiFetch<BankTag[]>(`${quizPath(id)}/bank-tags`, { signal });
}

/** Bốc thử một đề mẫu theo quy tắc đã lưu; không lưu gì. */
export function previewPaper(id: string): Promise<PaperPreview> {
  return post<PaperPreview>(`${quizPath(id)}/paper-preview`);
}

export function publishQuiz(id: string, version: number): Promise<QuizDetail> {
  return post<QuizDetail>(`${quizPath(id)}/publish`, { version });
}

export function retireQuiz(id: string, reason: string, version: number): Promise<QuizDetail> {
  return post<QuizDetail>(`${quizPath(id)}/retire`, { reason, version });
}

/** Mở lại Quiz đã lưu trữ. */
export function reopenQuiz(id: string, version: number): Promise<QuizDetail> {
  return post<QuizDetail>(`${quizPath(id)}/reopen`, { version });
}

export function cloneQuiz(id: string): Promise<QuizDetail> {
  return post<QuizDetail>(`${quizPath(id)}/clone`);
}

export function createQuizVersion(id: string): Promise<QuizDetail> {
  return post<QuizDetail>(`${quizPath(id)}/versions`);
}

export function deleteQuiz(id: string, version: number): Promise<void> {
  return apiFetch<void>(`${quizPath(id)}?version=${version}`, { method: "DELETE" });
}

// --- Đề xuất AI, tài liệu nguồn cho AI (F4, F8, H8)

const bankDraftPath = (subjectId: string) => `/api/v1/subjects/${enc(subjectId)}/question-ai-drafts`;
const quizDraftPath = (quizId: string) => `${quizPath(quizId)}/ai-drafts`;

/** `requestRef` mới mỗi lần bấm gửi để server bỏ yêu cầu gửi trùng. */
function draftBody(input: AiDraftInput) {
  const { instructions, count, type, difficulty, materialIds, choice } = input;
  return {
    instructions, count, type, difficulty, materialIds, source: choice?.source ?? null,
    provider: choice?.source === "PERSONAL" ? choice.provider : null, requestRef: crypto.randomUUID(),
  };
}

export function getQuizMaterials(quizId: string, signal?: AbortSignal): Promise<SelectableMaterial[]> {
  return apiFetch<SelectableMaterial[]>(`${quizPath(quizId)}/reference-materials`, { signal });
}

export function getSubjectMaterials(subjectId: string, signal?: AbortSignal): Promise<SelectableMaterial[]> {
  return apiFetch<SelectableMaterial[]>(`/api/v1/subjects/${enc(subjectId)}/reference-materials`, { signal });
}

export function getBankDrafts(subjectId: string, signal?: AbortSignal): Promise<AiDraft[]> {
  return apiFetch<AiDraft[]>(bankDraftPath(subjectId), { signal });
}

export function requestBankDraft(subjectId: string, input: AiDraftInput): Promise<AiDraft> {
  return post<AiDraft>(bankDraftPath(subjectId), draftBody(input));
}

export function getBankDraft(subjectId: string, draftId: string, signal?: AbortSignal): Promise<AiDraft> {
  return apiFetch<AiDraft>(`${bankDraftPath(subjectId)}/${enc(draftId)}`, { signal });
}

export function acceptBankDraft(subjectId: string, draftId: string, questions: QuestionContent[]): Promise<BankAcceptResult> {
  return post<BankAcceptResult>(`${bankDraftPath(subjectId)}/${enc(draftId)}:accept`, { questions: questions.map(contentBody) });
}

export function discardBankDraft(subjectId: string, draftId: string): Promise<AiDraft> {
  return post<AiDraft>(`${bankDraftPath(subjectId)}/${enc(draftId)}:discard`);
}

export function getQuizDrafts(quizId: string, signal?: AbortSignal): Promise<AiDraft[]> {
  return apiFetch<AiDraft[]>(quizDraftPath(quizId), { signal });
}

export function requestQuizDraft(quizId: string, input: AiDraftInput, version: number): Promise<AiDraft> {
  return post<AiDraft>(quizDraftPath(quizId), { ...draftBody(input), version });
}

export function getQuizDraft(quizId: string, draftId: string, signal?: AbortSignal): Promise<AiDraft> {
  return apiFetch<AiDraft>(`${quizDraftPath(quizId)}/${enc(draftId)}`, { signal });
}

export function acceptQuizDraft(quizId: string, draftId: string, questions: QuestionContent[], version: number): Promise<QuizDetail> {
  return post<QuizDetail>(`${quizDraftPath(quizId)}/${enc(draftId)}:accept`, { questions: questions.map(contentBody), version });
}

export function discardQuizDraft(quizId: string, draftId: string): Promise<AiDraft> {
  return post<AiDraft>(`${quizDraftPath(quizId)}/${enc(draftId)}:discard`);
}
