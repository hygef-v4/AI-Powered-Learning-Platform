import { apiDownload, apiFetch } from "@/shared/api/client";
import type { ActivityEventInput } from "@/shared/activity/useActivityTracker";
import type { DocxPreview } from "@/shared/document";
import type {
  ApplyResult,
  DocumentBlock,
  DocumentImport,
  PeerGiven,
  PeerReviewView,
  Receipt,
  RevisionView,
  SectionView,
  WorkspaceView,
} from "../model/types";

const doc = (id: string) => `/api/v1/group-docs/${encodeURIComponent(id)}`;
const section = (id: string, sectionId: string) => `${doc(id)}/sections/${encodeURIComponent(sectionId)}`;

export const getMyGroupDoc = (assignmentId: string) =>
  apiFetch<WorkspaceView>(`/api/v1/assignments/${encodeURIComponent(assignmentId)}/my-group-doc`);

export const getWorkspace = (id: string) => apiFetch<WorkspaceView>(doc(id));

export const claimSection = (id: string, sectionId: string) =>
  apiFetch<SectionView>(`${section(id, sectionId)}:claim`, { method: "POST" });

export const assignSection = (id: string, sectionId: string, assigneeId: string, replaceHolder: boolean) =>
  apiFetch<SectionView>(`${section(id, sectionId)}:assign`, { method: "POST", body: JSON.stringify({ assigneeId, replaceHolder }) });

export const releaseSection = (id: string, sectionId: string) =>
  apiFetch<SectionView>(`${section(id, sectionId)}:release`, { method: "POST" });

export const saveDraft = (id: string, sectionId: string, version: number, blocks: DocumentBlock[]) =>
  apiFetch<SectionView>(`${section(id, sectionId)}/draft`, { method: "PUT", body: JSON.stringify({ version, blocks }) });

export const finishSection = (id: string, sectionId: string) =>
  apiFetch<SectionView>(`${section(id, sectionId)}:done`, { method: "POST" });

export const lockSection = (id: string, sectionId: string) =>
  apiFetch<SectionView>(`${section(id, sectionId)}:lock`, { method: "POST" });

export const unlockSection = (id: string, sectionId: string) =>
  apiFetch<SectionView>(`${section(id, sectionId)}:unlock`, { method: "POST" });

export const getRevisions = (id: string, sectionId: string) => apiFetch<RevisionView[]>(`${section(id, sectionId)}/revisions`);

export const restoreRevision = (id: string, sectionId: string, revisionId: string, version: number) =>
  apiFetch<SectionView>(`${section(id, sectionId)}:restore`, { method: "POST", body: JSON.stringify({ revisionId, version }) });

/** Người giữ nhập DOCX vào mục; chỉ xem trước, trình soạn chèn rồi tự lưu. */
export const previewSectionDocx = async (id: string, sectionId: string, file: File): Promise<DocxPreview> => {
  const form = new FormData();
  form.append("file", file);
  const r = await apiFetch<{ document: DocxPreview["document"]; warnings: DocxPreview["warnings"]; images: number; diagrams: number }>(
    `${section(id, sectionId)}/docx:preview`,
    { method: "POST", body: form },
  );
  return { document: r.document, warnings: r.warnings, imageCount: r.images, diagramCount: r.diagrams };
};

/** Trưởng nhóm nhập DOCX và/hoặc Draw.io cho cả tài liệu: xem trước theo mục (FB-U12-03). */
export const previewImport = (id: string, files: { docx?: File | null; drawio?: File | null }) => {
  const form = new FormData();
  if (files.docx) form.append("docx", files.docx);
  if (files.drawio) form.append("drawio", files.drawio);
  return apiFetch<DocumentImport>(`${doc(id)}/import:preview`, { method: "POST", body: form });
};

export const applyImport = (id: string, sections: { sectionId: string; version: number; blocks: DocumentBlock[] }[]) =>
  apiFetch<ApplyResult>(`${doc(id)}/import:apply`, { method: "POST", body: JSON.stringify({ sections }) });

export const getPeerReviews = (id: string) => apiFetch<PeerReviewView>(`${doc(id)}/peer-reviews`);

export const savePeerReviews = (id: string, reviews: PeerGiven[]) =>
  apiFetch<PeerReviewView>(`${doc(id)}/peer-reviews`, { method: "PUT", body: JSON.stringify({ reviews }) });

export const submitDocument = (id: string, version: number) =>
  apiFetch<Receipt>(`${doc(id)}/submit`, { method: "POST", body: JSON.stringify({ version }) });

export const downloadDocx = (id: string, submitted: boolean) => apiDownload(`${doc(id)}/export.docx?submitted=${submitted}`);

export const eventsUrl = (id: string) => `${doc(id)}/events`;

/** Ghi nhận hoạt động khi bài bật giám sát (U12 nhận như U08); `keepalive` cho lần gửi lúc rời trang. */
export const sendActivity = (id: string, events: ActivityEventInput[], keepalive: boolean) =>
  apiFetch<void>(`${doc(id)}/activity`, { method: "POST", body: JSON.stringify({ events }), keepalive });

/** Ảnh trong tài liệu nhóm mở theo id block; backend chuyển tới link tải ngắn hạn. */
export const groupDocImageUrl = (id: string) => (blockId: string) =>
  `${doc(id)}/blocks/${encodeURIComponent(blockId)}/image`;
