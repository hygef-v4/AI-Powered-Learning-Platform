import { LOAD_FAILED_MESSAGE } from "@/shared/api/messages";
import { fetchApi } from "@/shared/api/client";
import type { Announcement, AnnouncementFeed, AnnouncementInput } from "../model/types";
const encodePathSegment = encodeURIComponent;
export const announcementsApi = {
  listAnnouncements: (classId = "", page = 1, size = 20, signal?: AbortSignal) =>
    fetchApi<AnnouncementFeed>(
      `/api/v1/me/announcements?${new URLSearchParams({ ...(classId ? { classId } : {}), page: String(page), size: String(size) })}`,
      { signal },
    ),
  createAnnouncement: (classId: string, input: AnnouncementInput) =>
    fetchApi<Announcement>(`/api/v1/classes/${encodePathSegment(classId)}/announcements`, {
      method: "POST",
      body: JSON.stringify(input),
    }),
  editAnnouncement: (id: string, title: string, body: string, version: number) =>
    fetchApi<Announcement>(`/api/v1/announcements/${encodePathSegment(id)}`, {
      method: "PATCH",
      body: JSON.stringify({ title, body, version }),
    }),
  deleteAnnouncement: (id: string, version: number) =>
    fetchApi<void>(`/api/v1/announcements/${encodePathSegment(id)}?version=${version}`, { method: "DELETE" }),
};
export function describeAnnouncementError(error: unknown) {
  return error instanceof Error ? error.message : LOAD_FAILED_MESSAGE;
}
