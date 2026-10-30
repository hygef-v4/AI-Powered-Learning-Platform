import { apiFetch } from "@/shared/api/client";
import type { EmailPreferences, NotificationFeed } from "../model/types";
const root = "/api/v1/me";
export const notificationsApi = {
  count: (signal?: AbortSignal) => apiFetch<{ count: number }>(`${root}/notifications/unread-count`, { signal }),
  list: (unread: boolean, page = 1, signal?: AbortSignal) => apiFetch<NotificationFeed>(`${root}/notifications?${new URLSearchParams({ unread: String(unread), page: String(page), size: "20" })}`, { signal }),
  read: (id: string) => apiFetch<void>(`${root}/notifications/${encodeURIComponent(id)}:read`, { method: "POST" }),
  readAll: () => apiFetch<void>(`${root}/notifications:read-all`, { method: "POST" }),
  preferences: (signal?: AbortSignal) => apiFetch<EmailPreferences>(`${root}/notification-preferences`, { signal }),
  savePreferences: (values: EmailPreferences) => apiFetch<EmailPreferences>(`${root}/notification-preferences`, { method: "PUT", body: JSON.stringify(values) }),
};
export function safeNotificationLink(link: string) { return /^\/(?!\/)/.test(link) && !/[\\\u0000-\u0020]/.test(link) ? link : null; }
