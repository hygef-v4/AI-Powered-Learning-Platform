"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { useSession } from "@/features/identity/hooks/session";
import { Icon } from "@/shared/ui/Icon";
import { notificationsApi } from "../api";
import { NotificationPopup } from "./NotificationPopup";
import styles from "../styles/notifications.module.css";
import { msg } from "@/i18n/msg";
export function NotificationBell() {
  const session = useSession();
  return session.status === "authenticated" ? (
    <Bell key={`${session.profile.accountId}:${session.profile.role}`} />
  ) : null;
}
function Bell() {
  const [count, setCount] = useState(0);
  const [failed, setFailed] = useState(false);
  const [open, setOpen] = useState(false);
  const [revision, setRevision] = useState(0);
  const host = useRef<HTMLDivElement>(null);
  const button = useRef<HTMLButtonElement>(null);
  const handleRefresh = useCallback(() => setRevision((r) => r + 1), []);
  useEffect(() => {
    const controller = new AbortController();
    void notificationsApi
      .countUnread(controller.signal)
      .then((data) => {
        setCount(data.count);
        setFailed(false);
      })
      .catch(() => {
        if (!controller.signal.aborted) setFailed(true);
      });
    return () => controller.abort();
  }, [revision]);
  useEffect(() => {
    let stopped = false;
    let stream: EventSource | undefined;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let attempts = 0;
    function connect() {
      if (stopped || typeof EventSource === "undefined") return;
      stream = new EventSource("/api/v1/me/notifications/events", { withCredentials: true });
      const changed = () => {
        attempts = 0;
        handleRefresh();
      };
      stream.onmessage = changed;
      stream.addEventListener("notification-changed", changed);
      stream.onerror = () => {
        stream?.close();
        handleRefresh();
        if (!stopped)
          timer = setTimeout(connect, Math.min(30000, 1000 * 2 ** Math.min(attempts++, 5)));
      };
    }
    connect();
    const fallback = setInterval(handleRefresh, 30000);
    return () => {
      stopped = true;
      stream?.close();
      if (timer) clearTimeout(timer);
      clearInterval(fallback);
    };
  }, [handleRefresh]);
  useEffect(() => {
    if (!open) return;
    const outside = (e: PointerEvent) => {
      if (e.target instanceof Node && !host.current?.contains(e.target)) setOpen(false);
    };
    document.addEventListener("pointerdown", outside);
    return () => document.removeEventListener("pointerdown", outside);
  }, [open]);
  function handleClose() {
    setOpen(false);
    button.current?.focus();
  }
  return (
    <div ref={host} className={styles.host}>
      <button
        ref={button}
        data-testid="notification-bell"
        type="button"
        aria-label={
          failed ? msg("notifications.components.NotificationBell.thongBaoChuaTaiDuocSo") : msg("notifications.components.NotificationBell.thongBaoChuaDoc", { count: String(count) })
        }
        aria-expanded={open}
        aria-controls={open ? "notification-popup" : undefined}
        className={styles.bell}
        onClick={() => {
          setOpen((value) => !value);
          handleRefresh();
        }}
      >
        <Icon name="notifications" />
        {count > 0 && <span className={styles.count}>{count > 99 ? "99+" : count}</span>}
      </button>
      {open && (
        <NotificationPopup
          count={count}
          revision={revision}
          onChanged={handleRefresh}
          onClose={handleClose}
        />
      )}
    </div>
  );
}
