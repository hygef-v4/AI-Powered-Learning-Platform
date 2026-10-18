"use client";

import type { ReactNode } from "react";
import { dashboardNavEntries } from "./slots";
import { Avatar, Icon, type IconName } from "./ui";
import type { Audience } from "./types";
import styles from "./academics.module.css";

export type AcademicsView = "overview" | "subject" | "classes" | "teaching" | "my-classes";
const labels = { manager: "Chủ nhiệm môn", teacher: "Giảng viên", student: "Sinh viên" };
const navigation: Record<Audience, { key: AcademicsView; label: string; icon: IconName }[]> = {
  manager: [{ key: "overview", label: "Tổng quan", icon: "grid" }, { key: "subject", label: "Môn học của tôi", icon: "book" }, { key: "classes", label: "Lớp học", icon: "users" }, { key: "teaching", label: "Lớp được phân công", icon: "school" }],
  teacher: [{ key: "teaching", label: "Lớp được phân công", icon: "school" }],
  student: [{ key: "my-classes", label: "Lớp của tôi", icon: "school" }],
};
/** Chỉ trình bày actor đã được phía tích hợp xác thực; audience không cấp quyền cho API. */
export function AcademicsShell({ audience, userName, active, onNavigate, children, previewControls }: {
  audience: Audience; userName: string; active: AcademicsView; onNavigate: (view: AcademicsView) => void; children: ReactNode; previewControls?: ReactNode;
}) {
  const role = audience === "manager" ? "SUBJECT_MANAGER" : audience === "teacher" ? "TEACHER" : "STUDENT";
  const entries = dashboardNavEntries.entries().filter((entry) => entry.roles.includes(role) && entry.dashboard === (audience === "manager" ? "manager" : "class"));
  return <div className={styles.root}>
    {previewControls}
    <header className={styles.header}><div className={styles.brand}><span className={styles.brandMark}><Icon name="school"/></span>Nền tảng học tập</div><div className={styles.user}><Avatar name={userName}/><div><strong>{userName}</strong><small>{labels[audience]}</small></div></div></header>
    <div className={styles.layout}><aside className={styles.sidebar}><nav className={styles.nav} aria-label={`Menu ${labels[audience]}`}>
      {navigation[audience].map((item) => <button key={item.key} onClick={() => onNavigate(item.key)} className={active === item.key ? styles.activeNav : ""} aria-current={active === item.key ? "page" : undefined} data-testid={`academics-nav-${item.key}`}><Icon name={item.icon}/>{item.label}</button>)}
      {entries.length > 0 && <span className={styles.navLabel}>Truy cập nhanh</span>}
      {entries.map((entry) => <a href={entry.href} key={entry.key}><Icon name="file"/>{entry.title}</a>)}
    </nav></aside><main className={styles.main}>{children}</main></div><footer className={styles.footer}>© 2026 Nền tảng học tập</footer>
  </div>;
}
