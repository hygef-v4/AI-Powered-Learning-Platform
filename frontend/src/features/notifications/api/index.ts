import { apiFetch } from "@/shared/api/client";
import type { EmailPreferences, NotificationFeed } from "../model/types";
const root = "/api/v1/me";
export const notificationsApi = {
  countUnread: (signal?: AbortSignal) =>
    apiFetch<{ count: number }>(`${root}/notifications/unread-count`, { signal }),
  listNotifications: (unread: boolean, page = 1, signal?: AbortSignal) =>
    apiFetch<NotificationFeed>(
      `${root}/notifications?${new URLSearchParams({ unread: String(unread), page: String(page), size: "20" })}`,
      { signal },
    ),
  markRead: (id: string) =>
    apiFetch<void>(`${root}/notifications/${encodeURIComponent(id)}:read`, { method: "POST" }),
  markAllRead: () => apiFetch<void>(`${root}/notifications:read-all`, { method: "POST" }),
  loadPreferences: (signal?: AbortSignal) =>
    apiFetch<EmailPreferences>(`${root}/notification-preferences`, { signal }),
  savePreferences: (values: EmailPreferences) =>
    apiFetch<EmailPreferences>(`${root}/notification-preferences`, {
      method: "PUT",
      body: JSON.stringify(values),
    }),
};
export function sanitizeNotificationLink(link: string) {
  return /^\/(?!\/)/.test(link) && !/[\\\u0000-\u0020]/.test(link) ? link : null;
}
