"use client";
import { useState } from "react";
import { Dialog } from "@/shared/ui/Dialog";
import { Button } from "@/shared/ui/Button";
import { announcementsApi, announcementError } from "./api";
import type { Announcement } from "./types";
import styles from "./announcements.module.css";
export function AnnouncementEditorDialog({ item, onClose, onSaved }: { item: Announcement; onClose: () => void; onSaved: () => void }) {
  const [title, setTitle] = useState(item.title); const [body, setBody] = useState(item.body); const [busy, setBusy] = useState(false); const [error, setError] = useState("");
  async function save() { setBusy(true); setError(""); try { await announcementsApi.edit(item.id, title.trim(), body.trim(), item.version); onSaved(); onClose(); } catch (e) { setError(announcementError(e)); } finally { setBusy(false); } }
  return <Dialog open title="Sửa thông báo" onClose={onClose} dismissible={!busy} size="lg" footer={<><Button onClick={onClose} disabled={busy}>Hủy</Button><Button data-testid="announcement-edit-save" variant="primary" onClick={() => void save()} disabled={busy || !title.trim() || !body.trim()}>Lưu thay đổi</Button></>}>
    <div className={styles.form}><label>Tiêu đề *<input data-testid="announcement-edit-title" maxLength={200} value={title} disabled={busy} onChange={e => setTitle(e.target.value)} /></label><label>Nội dung (Markdown) *<textarea data-testid="announcement-edit-body" maxLength={5000} value={body} disabled={busy} onChange={e => setBody(e.target.value)} /></label></div>
    <p className={styles.muted}>Không đổi được lựa chọn Quan trọng / Gửi email sau khi đăng. Sinh viên không nhận thông báo mới khi bạn sửa.</p>{error && <p role="alert" className={styles.error}>{error} Đóng hộp thoại và tải lại nếu thông báo đã thay đổi.</p>}
  </Dialog>;
}
export function DeleteAnnouncementDialog({ item, onClose, onSaved }: { item: Announcement; onClose: () => void; onSaved: () => void }) {
  const [busy, setBusy] = useState(false); const [error, setError] = useState("");
  async function remove() { setBusy(true); try { await announcementsApi.remove(item.id, item.version); onSaved(); onClose(); } catch (e) { setError(announcementError(e)); } finally { setBusy(false); } }
  return <Dialog open title="Xóa thông báo?" onClose={onClose} dismissible={!busy} tone="danger" footer={<><Button onClick={onClose} disabled={busy}>Hủy</Button><Button data-testid="announcement-delete-confirm" variant="danger" onClick={() => void remove()} disabled={busy}>Xóa</Button></>}><p>“{item.title}” sẽ bị xóa khỏi lớp {item.className}.</p>{error && <p role="alert" className={styles.error}>{error}</p>}</Dialog>;
}
