import { buildDiagramPngsRequest, downloadApi, fetchApi } from "@/shared/api/client";
import { renderMissingDiagramPngs } from "@/shared/document/diagramPngRenderer";
import type { ActivityEventInput } from "@/shared/activity/useActivityTracker";
import type { DocxPreview } from "@/shared/document";
import type { DocumentBlock, Receipt, RevisionView, SectionView, WorkspaceView } from "../model/types";

const buildDocPath = (id: string) => `/api/v1/group-docs/${encodeURIComponent(id)}`;
const buildSectionPath = (id: string, sectionId: string) => `${buildDocPath(id)}/sections/${encodeURIComponent(sectionId)}`;

export const getMyGroupDoc = (assignmentId: string) =>
  fetchApi<WorkspaceView>(`/api/v1/assignments/${encodeURIComponent(assignmentId)}/my-group-doc`);

export const getWorkspace = (id: string, signal?: AbortSignal) =>
  fetchApi<WorkspaceView>(buildDocPath(id), { cache: "no-store", signal });

export const claimSection = (id: string, sectionId: string) =>
  fetchApi<SectionView>(`${buildSectionPath(id, sectionId)}:claim`, { method: "POST" });

export const assignSection = (id: string, sectionId: string, assigneeId: string, replaceHolder: boolean) =>
  fetchApi<SectionView>(`${buildSectionPath(id, sectionId)}:assign`, { method: "POST", body: JSON.stringify({ assigneeId, replaceHolder }) });

export const releaseSection = (id: string, sectionId: string) =>
  fetchApi<SectionView>(`${buildSectionPath(id, sectionId)}:release`, { method: "POST" });

export const saveDraft = (id: string, sectionId: string, version: number, blocks: DocumentBlock[]) =>
  fetchApi<SectionView>(`${buildSectionPath(id, sectionId)}/draft`, { method: "PUT", body: JSON.stringify({ version, blocks }) });

export const finishSection = (id: string, sectionId: string) =>
  fetchApi<SectionView>(`${buildSectionPath(id, sectionId)}:done`, { method: "POST" });

/** Lịch sử mục chỉ xem, mới nhất trước (BR-U12-35). */
export const getRevisions = (id: string, sectionId: string) => fetchApi<RevisionView[]>(`${buildSectionPath(id, sectionId)}/revisions`);

/** Người giữ nhập DOCX vào mục; chỉ xem trước, trình soạn chèn rồi tự lưu. */
export const previewSectionDocx = async (id: string, sectionId: string, file: File): Promise<DocxPreview> => {
  const form = new FormData();
  form.append("file", file);
  const r = await fetchApi<{ document: DocxPreview["document"]; warnings: DocxPreview["warnings"]; images: number; diagrams: number }>(
    `${buildSectionPath(id, sectionId)}/docx:preview`,
    { method: "POST", body: form },
  );
  return { document: r.document, warnings: r.warnings, imageCount: r.images, diagramCount: r.diagrams };
};

export const submitDocument = (id: string, version: number) =>
  fetchApi<Receipt>(`${buildDocPath(id)}/submit`, { method: "POST", body: JSON.stringify({ version }) });

/** {@code blocks}: tài liệu đang hiện; sơ đồ chưa có ảnh lưu sẵn được trình duyệt dựng ảnh trước khi tải (yêu cầu 2026-11-27). */
export const downloadDocx = async (id: string, submitted: boolean, blocks: DocumentBlock[] = []) =>
  downloadApi(`${buildDocPath(id)}/export.docx?submitted=${submitted}`, buildDiagramPngsRequest(await renderMissingDiagramPngs(blocks)));

export const buildEventsUrl = (id: string) => `${buildDocPath(id)}/events`;

/** Ghi nhận hoạt động khi bài bật giám sát (U12 nhận như U08); `keepalive` cho lần gửi lúc rời trang. */
export const sendActivity = (id: string, events: ActivityEventInput[], keepalive: boolean) =>
  fetchApi<void>(`${buildDocPath(id)}/activity`, { method: "POST", body: JSON.stringify({ events }), keepalive });

/** Ảnh trong tài liệu nhóm mở theo id block; backend chuyển tới link tải ngắn hạn. */
export const groupDocImageUrl = (id: string) => (blockId: string) =>
  `${buildDocPath(id)}/blocks/${encodeURIComponent(blockId)}/image`;
