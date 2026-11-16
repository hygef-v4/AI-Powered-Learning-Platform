import { fetchApi } from "@/shared/api/client";
import type { EmailPreferences, NotificationFeed } from "../model/types";
const root = "/api/v1/me";
export const notificationsApi = {
  countUnread: (signal?: AbortSignal) =>
    fetchApi<{ count: number }>(`${root}/notifications/unread-count`, { signal }),
  listNotifications: (unread: boolean, page = 1, signal?: AbortSignal) =>
    fetchApi<NotificationFeed>(
      `${root}/notifications?${new URLSearchParams({ unread: String(unread), page: String(page), size: "20" })}`,
      { signal },
    ),
  markRead: (id: string) =>
    fetchApi<void>(`${root}/notifications/${encodeURIComponent(id)}:read`, { method: "POST" }),
  markAllRead: () => fetchApi<void>(`${root}/notifications:read-all`, { method: "POST" }),
  loadPreferences: (signal?: AbortSignal) =>
    fetchApi<EmailPreferences>(`${root}/notification-preferences`, { signal }),
  savePreferences: (values: EmailPreferences) =>
    fetchApi<EmailPreferences>(`${root}/notification-preferences`, {
      method: "PUT",
      body: JSON.stringify(values),
    }),
};
export function sanitizeNotificationLink(link: string) {
  return /^\/(?!\/)/.test(link) && !/[\\\u0000-\u0020]/.test(link) ? link : null;
}
