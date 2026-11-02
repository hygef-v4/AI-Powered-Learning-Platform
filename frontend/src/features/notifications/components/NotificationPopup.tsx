"use client";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Icon } from "@/shared/ui/Icon";
import { notificationsApi, safeNotificationLink } from "../api";
import { EmailPreferencePanel } from "./EmailPreferencePanel";
import type { NotificationItem } from "../model/types";
import styles from "../styles/notifications.module.css";
/**
 * Popup thông báo: tiêu đề kèm nút cài đặt (bánh răng, chỉ Sinh viên có tùy chọn email) mở trang Cài đặt riêng trong popup; hàng tab
 * Tất cả / Chưa đọc mang luôn nút đánh dấu đã đọc. Làm mới (SSE, định kỳ, sau khi đọc) giữ danh sách cũ của cùng bộ lọc cho tới khi
 * có dữ liệu mới, nên danh sách không nháy trống rồi hiện lại (yêu cầu 2026-10-29).
 */
export function NotificationPopup({ count, student, revision, onChanged, onClose }: { count: number; student: boolean; revision: number; onChanged: () => void; onClose: () => void }) {
  const router = useRouter(); const [unread, setUnread] = useState(false); const [settings, setSettings] = useState(false); const [busy, setBusy] = useState(false); const [mutationError, setMutationError] = useState(""); const more = useRef<HTMLButtonElement>(null); const panel = useRef<HTMLElement>(null); const gear = useRef<HTMLButtonElement>(null); const back = useRef<HTMLButtonElement>(null);
  const filter = String(unread); const base = `${filter}:${revision}`; const [pagination, setPagination] = useState({ base: "", page: 1 }); const page = pagination.base === base ? pagination.page : 1;
  function setPage(value: number | ((page: number) => number)) { setPagination(previous => ({ base, page: typeof value === "number" ? value : value(previous.base === base ? previous.page : 1) })); }
  const key = `${base}:${page}`; const [result, setResult] = useState<{ filter: string; base: string; key: string; items: NotificationItem[]; hasMore: boolean; error?: string }>({ filter: "", base: "", key: "", items: [], hasMore: false });
  // Trang 1 thay cả danh sách; trang sau nối thêm. Lỗi chỉ giữ hàng của cùng bộ lọc, không lộ hàng của tab kia.
  useEffect(() => { const controller = new AbortController(); void notificationsApi.list(unread, page, controller.signal).then(data => { if (!controller.signal.aborted) setResult(previous => ({ filter, base, key, items: page > 1 && previous.base === base ? [...previous.items, ...data.items.filter(i => !previous.items.some(p => p.id === i.id))] : data.items, hasMore: data.hasMore })); }).catch(() => { if (!controller.signal.aborted) setResult(previous => ({ filter, base, key, items: previous.filter === filter ? previous.items : [], hasMore: false, error: "Không thể tải thông báo." })); }); return () => controller.abort(); }, [unread, page, filter, base, key]);
  useEffect(() => { panel.current?.focus(); }, []);
  const loading = result.key !== key; const items = result.filter === filter ? result.items : [];
  useEffect(() => { const node = more.current; if (!node || !result.hasMore || loading || typeof IntersectionObserver === "undefined") return; const observer = new IntersectionObserver(entries => { if (entries.some(e => e.isIntersecting)) setPagination(previous => ({ base, page: (previous.base === base ? previous.page : 1) + 1 })); }); observer.observe(node); return () => observer.disconnect(); }, [result.hasMore, result.key, loading, base]);
  async function open(item: NotificationItem) { const link = safeNotificationLink(item.link); if (!link) { setMutationError("Liên kết không hợp lệ."); return; } setBusy(true); try { await notificationsApi.read(item.id); onChanged(); onClose(); router.push(link); } catch { setMutationError("Chưa đánh dấu đã đọc được. Hãy thử lại."); } finally { setBusy(false); } }
  async function allRead() { setBusy(true); try { await notificationsApi.readAll(); setPage(1); onChanged(); } catch { setMutationError("Không thể đánh dấu tất cả đã đọc."); } finally { setBusy(false); } }
  function showSettings(value: boolean) { setSettings(value); requestAnimationFrame(() => (value ? back : gear).current?.focus()); }
  return <section id="notification-popup" ref={panel} tabIndex={-1} role="dialog" aria-label="Thông báo" className={styles.popup} onKeyDown={e => { if (e.key !== "Escape") return; if (settings) { e.stopPropagation(); showSettings(false); } else onClose(); }}>
    <header className={styles.heading}>
      {settings ? <><button ref={back} type="button" aria-label="Quay lại thông báo" title="Quay lại thông báo" className={styles.iconButton} onClick={() => showSettings(false)}><Icon name="arrow_back" size={18} /></button><h2>Cài đặt</h2></>
        : <><h2>Thông báo</h2>{student && <button ref={gear} type="button" data-testid="notification-settings" aria-label="Cài đặt thông báo" title="Cài đặt thông báo" className={styles.iconButton} onClick={() => showSettings(true)}><Icon name="settings" size={19} /></button>}</>}
    </header>
    {student && <div hidden={!settings} className={styles.scroll}><EmailPreferencePanel /></div>}
    <div hidden={settings} className={styles.listView}>
      <div className={styles.tabbar}>
        <div role="tablist" aria-label="Lọc thông báo" className={styles.tabs}><button role="tab" aria-selected={!unread} onClick={() => { setUnread(false); setPage(1); }}>Tất cả</button><button role="tab" aria-selected={unread} onClick={() => { setUnread(true); setPage(1); }}>Chưa đọc ({count})</button></div>
        <button data-testid="notifications-read-all" type="button" className={styles.linkButton} disabled={busy || !count} onClick={() => void allRead()}><Icon name="check" size={15} />Đánh dấu đã đọc</button>
      </div>
      <div className={styles.scroll} aria-busy={loading}>{mutationError && <p role="alert" className={styles.error}>{mutationError}</p>}{result.filter === filter && result.error && <p role="alert" className={styles.error}>{result.error} <button onClick={() => { setPage(1); onChanged(); }}>Thử lại</button></p>}{items.map(item => <button data-testid="notification-item" key={item.id} className={`${styles.item} ${item.readAt ? "" : styles.unread}`} disabled={busy} onClick={() => void open(item)}><span className={styles.tile}><Icon name="notifications" size={19} /></span><span className={styles.text}><strong>{item.title}</strong><p>{item.summary}</p><p>{new Date(item.createdAt).toLocaleString("vi-VN")}</p></span>{!item.readAt && <span className={styles.dot} />}</button>)}{loading && !items.length ? <p role="status" className={styles.empty}>Đang tải…</p> : !loading && !items.length && !(result.filter === filter && result.error) && <p className={styles.empty}>{unread ? "Bạn đã đọc tất cả thông báo." : "Chưa có thông báo."}</p>}{result.filter === filter && result.hasMore && <button ref={more} disabled={loading} className={styles.loadMore} onClick={() => setPage(p => p + 1)}>{loading ? "Đang tải…" : "Tải thêm"}</button>}</div>
    </div>
  </section>;
}
