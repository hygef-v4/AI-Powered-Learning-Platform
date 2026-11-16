"use client";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Icon } from "@/shared/ui/Icon";
import { handleTabListKey } from "@/shared/ui/tabKeys";
import { notificationsApi, sanitizeNotificationLink } from "../api";
import { EmailPreferencePanel } from "./EmailPreferencePanel";
import type { NotificationItem } from "../model/types";
import styles from "../styles/notifications.module.css";
import { formatDateTime, formatRelativeTime } from "@/shared/dateTime";
import { msg } from "@/i18n/msg";
/**
 * Popup thông báo: tiêu đề kèm nút cài đặt mở tùy chọn email theo vai trò trong popup; hàng tab
 * Tất cả / Chưa đọc mang luôn nút đánh dấu đã đọc. Làm mới (SSE, định kỳ, sau khi đọc) giữ danh sách cũ của cùng bộ lọc cho tới khi
 * có dữ liệu mới, nên danh sách không nháy trống rồi hiện lại (yêu cầu 2026-10-29).
 */
export function NotificationPopup({
  count,
  revision,
  onChanged,
  onClose,
}: {
  count: number;
  revision: number;
  onChanged: () => void;
  onClose: () => void;
}) {
  const router = useRouter();
  const [unread, setUnread] = useState(false);
  const [settings, setSettings] = useState(false);
  const [busy, setBusy] = useState(false);
  const [mutationError, setMutationError] = useState("");
  const more = useRef<HTMLButtonElement>(null);
  const panel = useRef<HTMLElement>(null);
  const gear = useRef<HTMLButtonElement>(null);
  const back = useRef<HTMLButtonElement>(null);
  const filter = String(unread);
  const base = `${filter}:${revision}`;
  const [pagination, setPagination] = useState({ base: "", page: 1 });
  const page = pagination.base === base ? pagination.page : 1;
  function setPage(value: number | ((page: number) => number)) {
    setPagination((previous) => ({
      base,
      page: typeof value === "number" ? value : value(previous.base === base ? previous.page : 1),
    }));
  }
  const key = `${base}:${page}`;
  const [result, setResult] = useState<{
    filter: string;
    base: string;
    key: string;
    items: NotificationItem[];
    hasMore: boolean;
    error?: string;
  }>({ filter: "", base: "", key: "", items: [], hasMore: false });
  // Trang 1 thay cả danh sách; trang sau nối thêm. Lỗi chỉ giữ hàng của cùng bộ lọc, không lộ hàng của tab kia.
  useEffect(() => {
    const controller = new AbortController();
    void notificationsApi
      .listNotifications(unread, page, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted)
          setResult((previous) => ({
            filter,
            base,
            key,
            items:
              page > 1 && previous.base === base
                ? [
                    ...previous.items,
                    ...data.items.filter((i) => !previous.items.some((p) => p.id === i.id)),
                  ]
                : data.items,
            hasMore: data.hasMore,
          }));
      })
      .catch(() => {
        if (!controller.signal.aborted)
          setResult((previous) => ({
            filter,
            base,
            key,
            items: previous.filter === filter ? previous.items : [],
            hasMore: false,
            error: msg("notifications.components.NotificationPopup.couldNotLoadNotifications"),
          }));
      });
    return () => controller.abort();
  }, [unread, page, filter, base, key]);
  useEffect(() => {
    panel.current?.focus();
  }, []);
  const loading = result.key !== key;
  const items = result.filter === filter ? result.items : [];
  useEffect(() => {
    const node = more.current;
    if (!node || !result.hasMore || loading || typeof IntersectionObserver === "undefined") return;
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((e) => e.isIntersecting))
        setPagination((previous) => ({
          base,
          page: (previous.base === base ? previous.page : 1) + 1,
        }));
    });
    observer.observe(node);
    return () => observer.disconnect();
  }, [result.hasMore, result.key, loading, base]);
  async function handleOpen(item: NotificationItem) {
    const link = sanitizeNotificationLink(item.link);
    if (!link) {
      setMutationError(msg("notifications.components.NotificationPopup.invalidLink"));
      return;
    }
    setBusy(true);
    try {
      await notificationsApi.markRead(item.id);
      onChanged();
      onClose();
      router.push(link);
    } catch {
      setMutationError(msg("notifications.components.NotificationPopup.couldNotMarkAsRead"));
    } finally {
      setBusy(false);
    }
  }
  async function handleMarkAllRead() {
    setBusy(true);
    try {
      await notificationsApi.markAllRead();
      setPage(1);
      onChanged();
    } catch {
      setMutationError(msg("notifications.components.NotificationPopup.couldNotMarkAllAs"));
    } finally {
      setBusy(false);
    }
  }
  function handleShowSettings(value: boolean) {
    setSettings(value);
    requestAnimationFrame(() => (value ? back : gear).current?.focus());
  }
  return (
    <section
      id="notification-popup"
      ref={panel}
      tabIndex={-1}
      role="dialog"
      aria-label={msg("notifications.components.NotificationPopup.notifications")}
      className={styles.popup}
      onKeyDown={(e) => {
        if (e.key !== "Escape") return;
        if (settings) {
          e.stopPropagation();
          handleShowSettings(false);
        } else onClose();
      }}
    >
      <header className={styles.heading}>
        {settings ? (
          <>
            <button
              ref={back}
              type="button"
              aria-label={msg("notifications.components.NotificationPopup.backToNotifications")}
              title={msg("notifications.components.NotificationPopup.backToNotifications")}
              className={styles.iconButton}
              onClick={() => handleShowSettings(false)}
            >
              <Icon name="arrow_back" size={18} />
            </button>
            <h2>{msg("notifications.components.NotificationPopup.settings")}</h2>
          </>
        ) : (
          <>
            <h2>{msg("notifications.components.NotificationPopup.notifications")}</h2>
            <button
              ref={gear}
              type="button"
              data-testid="notification-settings"
              aria-label={msg("notifications.components.NotificationPopup.notificationSettings")}
              title={msg("notifications.components.NotificationPopup.notificationSettings")}
              className={styles.iconButton}
              onClick={() => handleShowSettings(true)}
            >
              <Icon name="settings" size={19} />
            </button>
          </>
        )}
      </header>
      <div hidden={!settings} className={styles.scroll}>
        <EmailPreferencePanel />
      </div>
      <div hidden={settings} className={styles.listView}>
        <div className={styles.tabbar}>
          <div role="tablist" aria-label={msg("notifications.components.NotificationPopup.filterNotifications")} onKeyDown={handleTabListKey} className={styles.tabs}>
            <button
              role="tab"
              aria-selected={!unread}
              tabIndex={unread ? -1 : 0}
              onClick={() => {
                setUnread(false);
                setPage(1);
              }}
            >
              {msg("notifications.components.NotificationPopup.all")}</button>
            <button
              role="tab"
              aria-selected={unread}
              tabIndex={unread ? 0 : -1}
              onClick={() => {
                setUnread(true);
                setPage(1);
              }}
            >
              {msg("notifications.components.NotificationPopup.unread")}{count})
            </button>
          </div>
          <button
            data-testid="notifications-read-all"
            type="button"
            className={styles.linkButton}
            disabled={busy || !count}
            onClick={() => void handleMarkAllRead()}
          >
            <Icon name="check" size={15} />
            {msg("notifications.components.NotificationPopup.markAsRead")}</button>
        </div>
        <div className={styles.scroll} aria-busy={loading}>
          {mutationError && (
            <p role="alert" className={styles.error}>
              {mutationError}
            </p>
          )}
          {result.filter === filter && result.error && (
            <p role="alert" className={styles.error}>
              {result.error}{" "}
              <button
                onClick={() => {
                  setPage(1);
                  onChanged();
                }}
              >
                {msg("notifications.components.NotificationPopup.retry")}</button>
            </p>
          )}
          {items.map((item) => (
            <button
              data-testid="notification-item"
              key={item.id}
              className={`${styles.item} ${item.readAt ? "" : styles.unread}`}
              disabled={busy}
              onClick={() => void handleOpen(item)}
            >
              <span className={styles.tile}>
                <Icon name="notifications" size={19} />
              </span>
              <span className={styles.text}>
                <strong>{item.title}</strong>
                <p>{item.summary}</p>
                <p><time dateTime={item.createdAt} title={formatDateTime(item.createdAt)}>{formatRelativeTime(item.createdAt)}</time></p>
              </span>
              {!item.readAt && <span className={styles.dot} />}
            </button>
          ))}
          {loading && !items.length ? (
            <p role="status" className={styles.empty}>
              {msg("notifications.components.NotificationPopup.loading")}</p>
          ) : (
            !loading &&
            !items.length &&
            !(result.filter === filter && result.error) && (
              <p className={styles.empty}>
                {unread ? msg("notifications.components.NotificationPopup.youHaveReadAllNotifications") : msg("notifications.components.NotificationPopup.noNotificationsYet")}
              </p>
            )
          )}
          {result.filter === filter && result.hasMore && (
            <button
              ref={more}
              disabled={loading}
              className={styles.loadMore}
              onClick={() => setPage((p) => p + 1)}
            >
              {loading ? msg("notifications.components.NotificationPopup.loading") : msg("notifications.components.NotificationPopup.loadMore")}
            </button>
          )}
        </div>
      </div>
    </section>
  );
}
