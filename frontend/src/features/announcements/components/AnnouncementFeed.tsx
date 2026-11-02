"use client";
import { useState } from "react";
import { IconButton, Pill } from "@/shared/ui";
import { AnnouncementMarkdown } from "./AnnouncementMarkdown";
import { AnnouncementEditorDialog, DeleteAnnouncementDialog } from "./AnnouncementDialogs";
import type { Announcement } from "../model/types";

/**
 * Danh sách thông báo trong một thẻ, ngăn nhau bằng đường kẻ. Mỗi thông báo: (mã lớp khi xem nhiều lớp), tiêu đề và nhãn Quan
 * trọng, nội dung đầy đủ, người đăng và thời gian; người đăng sửa, xóa bằng hai nút cuối hàng tiêu đề.
 */
export function AnnouncementFeed({ items, classCodes, onChanged }: { items: Announcement[]; classCodes?: Record<string, string>; onChanged: () => void }) {
  const [editing, setEditing] = useState<Announcement | null>(null);
  const [deleting, setDeleting] = useState<Announcement | null>(null);
  return (
    <section aria-label="Danh sách thông báo" className="overflow-hidden rounded-xl border border-line bg-surface">
      {!items.length && <p className="px-6 py-12 text-center text-[13.5px] text-muted">Chưa có thông báo.</p>}
      {items.map((item) => (
        <article key={item.id} data-testid="announcement-card" className="flex flex-col gap-2 border-t border-line px-6 py-5 first:border-t-0">
          <div className="flex items-start gap-3">
            <div className="flex min-w-0 flex-1 flex-wrap items-center gap-x-2 gap-y-1">
              {classCodes && <span className="shrink-0 rounded-md border border-line-strong px-1.5 py-px text-[11.5px] font-semibold text-muted" title={item.className}>
                {classCodes[item.classId] ?? item.className}
              </span>}
              <h2 className="min-w-0 text-[15.5px] font-bold text-ink [overflow-wrap:anywhere]">{item.title}</h2>
              {item.important && <Pill tone="warning">Quan trọng</Pill>}
            </div>
            {item.canManage && <div className="-mt-1 -mr-2 flex shrink-0 items-center">
              <IconButton icon="edit" label={`Sửa ${item.title}`} onClick={() => setEditing(item)} data-testid={`announcement-edit-${item.id}`} />
              <IconButton icon="trash" label={`Xóa ${item.title}`} className="text-danger! hover:bg-danger-soft" onClick={() => setDeleting(item)}
                data-testid={`announcement-delete-${item.id}`} />
            </div>}
          </div>
          <AnnouncementMarkdown body={item.body} />
          <p className="text-[12.5px] text-muted">{item.authorName} · {new Date(item.createdAt).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" })}</p>
        </article>
      ))}
      {editing && <AnnouncementEditorDialog key={editing.id} item={editing} onClose={() => setEditing(null)} onSaved={onChanged} />}
      {deleting && <DeleteAnnouncementDialog key={deleting.id} item={deleting} onClose={() => setDeleting(null)} onSaved={onChanged} />}
    </section>
  );
}
