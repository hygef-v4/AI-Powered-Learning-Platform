import { apiDownloadFile, apiFetch } from "@/shared/api/client";
import type { DocumentModel, RubricCriterion } from "@/shared/document";
import type {
  AiDraft,
  AiSourcePreview,
  AssignmentDetail,
  AssignmentPage,
  AssignmentStatus,
  CopySource,
  DetailsInput,
  DiagramView,
  GradingMode,
  ImportDraft,
  ImportPlacement,
  ImportPreview,
  Material,
  RubricDetail,
  ScheduleInput,
  SubmissionMode,
} from "../model/types";

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
  /** {@code modes}: loại của bài copy (cá nhân / nhóm, tính điểm / luyện tập); bỏ trống thì giữ loại của bài nguồn. */
  copy: (classId: string, sourceAssignmentId: string, title?: string, modes?: CopyModes) =>
    write<AssignmentDetail>(`/classes/${enc(classId)}/assignments:copy`, "POST", { sourceAssignmentId, title, ...modes }),
  subjectCopySources: (subjectId: string, signal?: AbortSignal) =>
    apiFetch<CopySource[]>(`${base}/subjects/${enc(subjectId)}/assignments/copy-sources`, { signal }),
  copyToSubject: (subjectId: string, sourceAssignmentId: string, title?: string, modes?: CopyModes) =>
    write<AssignmentDetail>(`/subjects/${enc(subjectId)}/assignments/copies`, "POST", { sourceAssignmentId, title, ...modes }),
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
  /** Tài liệu tham khảo cho Tạo từ AI trong popup tạo bài, lúc bài chưa có (yêu cầu 2026-11-10). */
  classMaterials: (classId: string, signal?: AbortSignal) =>
    apiFetch<Material[]>(`${base}/classes/${enc(classId)}/assignment-materials`, { signal }),
  subjectMaterials: (subjectId: string, signal?: AbortSignal) =>
    apiFetch<Material[]>(`${base}/subjects/${enc(subjectId)}/assignments/materials`, { signal }),
  materials: (id: string, signal?: AbortSignal) =>
    apiFetch<Material[]>(`${base}/assignments/${enc(id)}/selectable-materials`, { signal }),
  diagram: (id: string, blockId: string, signal?: AbortSignal) =>
    apiFetch<DiagramView>(`${base}/assignments/${enc(id)}/diagrams/${enc(blockId)}`, { signal }),
  importDocx: (id: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return apiFetch<ImportPreview>(`${base}/assignments/${enc(id)}/skeleton:import-docx`, { method: "POST", body: form });
  },
  requestAiDraft: (id: string, input: { request: string; materialIds: string[]; sourceFileRefs: string[];
    options: { profile: "AUTO" | "GENERAL" | "SWR" | "SWD"; language: "VI" | "EN" | "AUTO"; questionCount?: number; targetPoints?: number };
    /** Bỏ trống: hệ thống tự chọn nguồn; PERSONAL không kèm provider: chỉ API cá nhân. */
    source?: "SHARED" | "PERSONAL"; provider?: string; requestRef: string; formatText?: string;
    templateAssignmentId?: string }) =>
    write<AiDraft>(`/assignments/${enc(id)}/ai-drafts`, "POST", input),
  /** AI sửa đề đang có theo yêu cầu; máy chủ gửi kèm toàn bộ đề và rubric đã lưu (yêu cầu 2026-11-09). */
  requestAiRevision: (id: string, request: string, requestRef: string,
    /** Tệp gửi kèm (yêu cầu 2026-11-12): mẫu đề (chữ của DOCX tải lên hoặc một đề cũ) và tài liệu tham khảo. */
    attachments?: { formatText?: string; templateAssignmentId?: string; materialIds?: string[]; sourceFileRefs?: string[] }) =>
    write<AiDraft>(`/assignments/${enc(id)}/ai-drafts`, "POST", { request, mode: "REVISE", requestRef, ...attachments }),
  aiDraft: (id: string, draftId: string, signal?: AbortSignal) =>
    apiFetch<AiDraft>(`${base}/assignments/${enc(id)}/ai-drafts/${enc(draftId)}`, { signal }),
  importDraft: (id: string, draftId: string, signal?: AbortSignal) =>
    apiFetch<ImportDraft>(`${base}/assignments/${enc(id)}/ai-drafts/${enc(draftId)}`, { signal }),
  /** Đề mẫu của môn (bài do chủ nhiệm môn tạo) để chọn khi soạn bằng AI; {@code own}: thêm các đề cũ mình quản lý được. */
  listAiTemplates: (id: string, signal?: AbortSignal, own = false) =>
    apiFetch<{ assignmentId: string; title: string; subjectTemplate: boolean }[]>(
      `${base}/assignments/${enc(id)}/ai-drafts/templates${own ? "?own=true" : ""}`, { signal }),
  previewAiSources: (id: string, sourceFileRefs: string[], signal?: AbortSignal) =>
    apiFetch<AiSourcePreview[]>(`${base}/assignments/${enc(id)}/ai-drafts/sources:preview`, {
      method: "POST", body: JSON.stringify({ sourceFileRefs }), signal,
    }),
  /** {@code placement} chỉ dùng cho bản nhập đề từ ảnh/PDF: thay toàn bộ hay chèn vào cuối khung đề (BR-U07-98). */
  applyAiDraft: (id: string, draftId: string, version: number, replaceInstructions: boolean, replaceTitle: boolean,
    placement?: ImportPlacement) =>
    write<AssignmentDetail>(`/assignments/${enc(id)}/ai-drafts/${enc(draftId)}:apply`, "POST",
      { version, replaceInstructions, replaceTitle, ...(placement ? { placement } : {}) }),
  /** Nhập đề cũ từ ảnh/PDF (tối đa 5 ảnh hoặc 1 PDF): tạo bản nháp chờ AI đọc (BR-U07-87). */
  startImport: (id: string, files: File[], requestRef: string) => {
    const form = new FormData();
    files.forEach((file) => form.append("files", file));
    form.append("requestRef", requestRef);
    return apiFetch<ImportDraft>(`${base}/assignments/${enc(id)}/imports`, { method: "POST", body: form });
  },
  discardAiDraft: (id: string, draftId: string) => write<AiDraft>(`/assignments/${enc(id)}/ai-drafts/${enc(draftId)}:discard`, "POST"),
};

export function imageUrlFor(assignmentId: string) {
  return (blockId: string) => `${base}/assignments/${enc(assignmentId)}/blocks/${enc(blockId)}/image`;
}

export type CopyModes = Pick<CreateInput, "submissionMode" | "gradingMode">;

/** Tệp DOCX của đề hoặc của rubric: hai tệp riêng (yêu cầu 2026-10-22). */
export function docxUrl(assignmentId: string, what: "skeleton" | "rubric") {
  return `${base}/assignments/${enc(assignmentId)}/${what === "rubric" ? "rubric/docx" : "docx"}`;
}

/**
 * Tải tệp DOCX của đề hoặc rubric về máy (yêu cầu 2026-11-12): gọi qua apiFetch nên phiên hết hạn được làm mới, lỗi trả về
 * dạng ApiError để trang báo, không còn chuyển cả trang sang địa chỉ tệp (lỗi là mất trang soạn).
 */
export async function downloadDocx(assignmentId: string, what: "skeleton" | "rubric", fallbackName: string) {
  const { blob, filename } = await apiDownloadFile(docxUrl(assignmentId, what));
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename ?? fallbackName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
