import { apiFetch } from "@/shared/api/client";
import type { Announcement, AnnouncementFeed, AnnouncementInput } from "./types";
const enc = encodeURIComponent;
export const announcementsApi = {
  list: (classId = "", page = 1, size = 20, signal?: AbortSignal) => apiFetch<AnnouncementFeed>(`/api/v1/me/announcements?${new URLSearchParams({ ...(classId ? { classId } : {}), page: String(page), size: String(size) })}`, { signal }),
  create: (classId: string, input: AnnouncementInput) => apiFetch<Announcement>(`/api/v1/classes/${enc(classId)}/announcements`, { method: "POST", body: JSON.stringify(input) }),
  edit: (id: string, title: string, body: string, version: number) => apiFetch<Announcement>(`/api/v1/announcements/${enc(id)}`, { method: "PATCH", body: JSON.stringify({ title, body, version }) }),
  remove: (id: string, version: number) => apiFetch<void>(`/api/v1/announcements/${enc(id)}?version=${version}`, { method: "DELETE" }),
};
export function announcementError(error: unknown) { return error instanceof Error ? error.message : "Không thể tải dữ liệu, vui lòng thử lại."; }
