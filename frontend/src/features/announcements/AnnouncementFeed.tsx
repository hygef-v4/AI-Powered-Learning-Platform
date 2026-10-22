"use client";
import { useState } from "react";
import { Button } from "@/shared/ui/Button";
import { AnnouncementMarkdown } from "./AnnouncementMarkdown";
import { AnnouncementEditorDialog, DeleteAnnouncementDialog } from "./AnnouncementDialogs";
import type { Announcement } from "./types";
import styles from "./announcements.module.css";
export function AnnouncementFeed({ items, onChanged }: { items: Announcement[]; onChanged: () => void }) {
  const [editing, setEditing] = useState<Announcement | null>(null); const [deleting, setDeleting] = useState<Announcement | null>(null);
  return <div className={styles.feed}>{!items.length && <p className={styles.empty}>Chưa có thông báo trong các lớp này.</p>}{items.map(item => <article data-testid="announcement-card" className={styles.card} key={item.id}>
    <div className={styles.byline}><span className={styles.avatar}>{item.authorName?.slice(0, 1).toUpperCase() || "GV"}</span><div><strong>{item.authorName}</strong><div className={styles.muted}>{item.className} · {new Date(item.createdAt).toLocaleString("vi-VN")}{item.updatedAt !== item.createdAt && " · đã chỉnh sửa"}</div></div></div>
    <div className={styles.title}><h2>{item.title}{item.important && <span className={styles.badge}>Quan trọng</span>}</h2>{item.canManage && <div className={styles.actions}><Button size="sm" data-testid={`announcement-edit-${item.id}`} onClick={() => setEditing(item)}>Sửa</Button><Button size="sm" variant="ghost" data-testid={`announcement-delete-${item.id}`} onClick={() => setDeleting(item)}>Xóa</Button></div>}</div><AnnouncementMarkdown body={item.body} />
  </article>)}{editing && <AnnouncementEditorDialog key={editing.id} item={editing} onClose={() => setEditing(null)} onSaved={onChanged} />}{deleting && <DeleteAnnouncementDialog key={deleting.id} item={deleting} onClose={() => setDeleting(null)} onSaved={onChanged} />}</div>;
}
