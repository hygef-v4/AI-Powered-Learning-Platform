"use client";

import { useEffect, useId, useRef, type ReactNode } from "react";
import { ApiError } from "@/shared/api/client";
import { statusLabels, type ClassStatus } from "./types";
import styles from "./academics.module.css";

export type IconName = "school" | "grid" | "book" | "users" | "folder" | "file" | "arrow" | "chevron" | "search" | "plus" | "close" | "lock" | "save" | "archive" | "person" | "info" | "check" | "clock";
const paths: Record<IconName, ReactNode> = {
  school: <><path d="m2 9 10-5 10 5-10 5L2 9Z"/><path d="M6 11v6c4 3 8 3 12 0v-6M22 9v7"/></>,
  grid: <><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>,
  book: <><path d="M12 5v15M3 4c4-1 7 0 9 2 2-2 5-3 9-2v15c-4-1-7 0-9 2-2-2-5-3-9-2V4Z"/></>,
  users: <><circle cx="9" cy="7" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3M17 4a3 3 0 0 1 0 6M21 21v-3a6 6 0 0 0-4-5"/></>,
  folder: <path d="M3 6h7l2 3h9l-3 11H3V6Zm0 3h8"/>,
  file: <><path d="M6 3h8l4 4v14H6V3Zm8 0v5h4M9 12h6M9 16h6"/></>,
  arrow: <path d="m12 5-7 7 7 7M5 12h15"/>,
  chevron: <path d="m9 5 7 7-7 7"/>,
  search: <><circle cx="10" cy="10" r="6"/><path d="m15 15 5 5"/></>,
  plus: <path d="M12 5v14M5 12h14"/>,
  close: <path d="m6 6 12 12M18 6 6 18"/>,
  lock: <><rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3"/></>,
  save: <><path d="M4 3h13l3 3v15H4V3Z"/><path d="M8 3v6h8V3M8 21v-8h8v8"/></>,
  archive: <><rect x="3" y="3" width="18" height="5" rx="1"/><path d="M5 8v13h14V8M10 12h4"/></>,
  person: <><circle cx="12" cy="7" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/></>,
  info: <><circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7v1"/></>,
  check: <path d="m5 12 4 4L19 6"/>,
  clock: <><circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/></>,
};
export function Icon({ name, className = "" }: { name: IconName; className?: string }) {
  return <svg aria-hidden="true" className={`${styles.icon} ${className}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>;
}
export function Avatar({ name }: { name: string }) {
  const initials = name.split(/\s+/).filter(Boolean).slice(-2).map((word) => word[0]).join("");
  return <span className={styles.avatar} aria-hidden="true">{initials}</span>;
}
export function StatusBadge({ status }: { status: ClassStatus }) {
  return <span className={`${styles.badge} ${styles[status.toLowerCase() as "draft" | "open" | "archived"]}`}>{statusLabels[status]}</span>;
}
export function EmptyState({ title = "Chưa có dữ liệu", description }: { title?: string; description?: string }) {
  return <div className={styles.empty}><Icon name="folder"/><strong>{title}</strong>{description && <p>{description}</p>}</div>;
}
export function PageHeading({ title, subtitle, onBack, children }: { title: ReactNode; subtitle?: string; onBack?: () => void; children?: ReactNode }) {
  return <div className={styles.pageHeading}><div className={styles.headingText}>{onBack && <button className={styles.iconButton} onClick={onBack} aria-label="Quay lại" data-testid="class-back"><Icon name="arrow"/></button>}<div><h1>{title}</h1>{subtitle && <p>{subtitle}</p>}</div></div><div className={styles.actions}>{children}</div></div>;
}
export function Tabs<T extends string>({ items, value, onChange, label }: { items: { key: T; label: string; count?: number }[]; value: T; onChange: (key: T) => void; label: string }) {
  return <div className={styles.tabs} role="tablist" aria-label={label}>{items.map((item) => <button key={item.key} role="tab" aria-selected={value === item.key} className={value === item.key ? styles.activeTab : ""} onClick={() => onChange(item.key)} data-testid={`class-tab-${item.key}`}>{item.label}{item.count !== undefined && <span className={styles.count}>{item.count}</span>}</button>)}</div>;
}
export function Modal({ title, description, onClose, children, footer, busy = false }: { title: string; description?: string; onClose: () => void; children: ReactNode; footer?: ReactNode; busy?: boolean }) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (typeof dialog.showModal === "function") dialog.showModal();
    else dialog.setAttribute("open", "");
    return () => { if (typeof dialog.close === "function") dialog.close(); };
  }, []);
  return <dialog ref={ref} className={styles.modal} aria-labelledby={titleId} aria-busy={busy} onCancel={(event) => { event.preventDefault(); if (!busy) onClose(); }}>
    <header className={styles.modalHeader}><div><h2 id={titleId}>{title}</h2>{description && <p>{description}</p>}</div><button className={styles.iconButton} type="button" aria-label="Đóng" onClick={onClose} disabled={busy} data-testid="class-dialog-close"><Icon name="close"/></button></header>
    <div className={styles.modalBody}>{children}</div>{footer && <footer className={styles.modalFooter}>{footer}</footer>}
  </dialog>;
}
export function operationError(cause: unknown) {
  if (cause instanceof ApiError && cause.problem.status === 409) return "Dữ liệu đã thay đổi. Vui lòng tải lại trước khi tiếp tục.";
  // Nội dung server chưa kiểm chứng không được hiển thị SQL/stack trace cho người dùng.
  return cause instanceof ApiError ? "Không thể hoàn tất thao tác. Vui lòng thử lại." : cause instanceof Error ? cause.message : "Không thể hoàn tất thao tác. Vui lòng thử lại.";
}
export function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : date.toLocaleDateString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" });
}
