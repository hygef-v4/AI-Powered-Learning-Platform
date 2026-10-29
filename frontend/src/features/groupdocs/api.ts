import { apiDownload, apiFetch } from "@/shared/api/client";
import type { ActivityEventInput } from "@/shared/activity/useActivityTracker";
import type { DocumentBlock, Receipt, SectionView, WorkspaceView } from "./types";

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

export const submitDocument = (id: string, version: number) =>
  apiFetch<Receipt>(`${doc(id)}/submit`, { method: "POST", body: JSON.stringify({ version }) });

export const downloadDocx = (id: string, submitted: boolean) => apiDownload(`${doc(id)}/export.docx?submitted=${submitted}`);

export const eventsUrl = (id: string) => `${doc(id)}/events`;

/** Ghi nhận hoạt động khi bài bật giám sát (U12 nhận như U08); `keepalive` cho lần gửi lúc rời trang. */
export const sendActivity = (id: string, events: ActivityEventInput[], keepalive: boolean) =>
  apiFetch<void>(`${doc(id)}/activity`, { method: "POST", body: JSON.stringify({ events }), keepalive });
