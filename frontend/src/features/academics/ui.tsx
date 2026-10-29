"use client";

import { Tabs as SharedTabs } from "@/shared/ui/Tabs";
import { useEffect, useId, useRef, type ReactNode } from "react";
import { ApiError } from "@/shared/api/client";
import { Icon as SharedIcon, type IconName as SharedIconName } from "@/shared/ui/Icon";
import { Pill } from "@/shared/ui/Pill";
import { EmptyState as SharedEmptyState } from "@/shared/ui/EmptyState";
import type { Tone } from "@/shared/ui/tones";
import { statusLabels, type ClassStatus } from "./types";
import styles from "./academics.module.css";

export type IconName = "school" | "grid" | "book" | "users" | "folder" | "file" | "arrow" | "chevron" | "search" | "plus" | "close" | "lock" | "save" | "archive" | "person" | "info" | "check" | "clock";

const ICONS: Record<IconName, SharedIconName> = {
  school: "school", grid: "space_dashboard", book: "menu_book", users: "groups", folder: "folder", file: "description",
  arrow: "arrow_back", chevron: "chevron_right", search: "search", plus: "add", close: "close", lock: "lock",
  save: "save", archive: "archive", person: "person", info: "info", check: "check", clock: "schedule",
};

export function Icon({ name, className = "" }: { name: IconName; className?: string }) {
  return <SharedIcon name={ICONS[name]} size={16} className={`${styles.icon} ${className}`} />;
}

export function Avatar({ name }: { name: string }) {
  const initials = name.split(/\s+/).filter(Boolean).slice(-2).map((word) => word[0]).join("");
  return <span className={styles.avatar} aria-hidden="true">{initials}</span>;
}

const STATUS_TONES: Record<ClassStatus, Tone> = { DRAFT: "neutral", OPEN: "success", ARCHIVED: "danger" };

export function StatusBadge({ status }: { status: ClassStatus }) {
  return <Pill tone={STATUS_TONES[status]}>{statusLabels[status]}</Pill>;
}

export function EmptyState({ title = "Chưa có dữ liệu", description }: { title?: string; description?: string }) {
  return <SharedEmptyState title={title} description={description} />;
}

/** Đầu trang: nút quay lại nhỏ phía trên, tiêu đề chữ đậm, mô tả, thao tác căn đáy bên phải. */
export function PageHeading({ title, subtitle, onBack, children }: { title: ReactNode; subtitle?: string; onBack?: () => void; children?: ReactNode }) {
  return <header className={styles.pageHeading}>
    <div className={styles.headingText}>
      {(onBack || subtitle) && <div className={styles.trail}>
        {onBack && <button className={styles.backButton} onClick={onBack} aria-label="Quay lại" data-testid="class-back"><SharedIcon name="arrow_back" size={14}/></button>}
        {subtitle && <p>{subtitle}</p>}
      </div>}
      <h1>{title}</h1>
    </div>
    {children && <div className={styles.actions}>{children}</div>}
  </header>;
}

export function Tabs<T extends string>({ items, value, onChange, label }: { items: { key: T; label: string; count?: number }[]; value: T; onChange: (key: T) => void; label: string }) {
  return <SharedTabs items={items} value={value} onChange={onChange} label={label} testIdPrefix="class-tab-" />;
}

/** Popup của nhóm màn lớp; đóng bằng X hoặc Esc nên footer chỉ còn nút chính. */
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
    <header className={styles.modalHeader}><div><h2 id={titleId}>{title}</h2>{description && <p>{description}</p>}</div><button className={styles.closeButton} type="button" aria-label="Đóng" onClick={onClose} disabled={busy} data-testid="class-dialog-close"><SharedIcon name="close" size={16}/></button></header>
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
