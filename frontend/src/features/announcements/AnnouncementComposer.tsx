"use client";
import { useState, type FormEvent } from "react";
import { Button } from "@/shared/ui/Button";
import { announcementsApi, announcementError } from "./api";
import type { ClassOption } from "./types";
import styles from "./announcements.module.css";
export function AnnouncementComposer({ selected, onCreated }: { selected: ClassOption; onCreated: () => void }) {
  const [title, setTitle] = useState(""); const [body, setBody] = useState(""); const [important, setImportant] = useState(false); const [email, setEmail] = useState(false); const [busy, setBusy] = useState(false); const [error, setError] = useState("");
  async function submit(event: FormEvent) { event.preventDefault(); setBusy(true); setError(""); try { await announcementsApi.create(selected.classId, { title: title.trim(), body: body.trim(), important, sendEmail: important && email }); setTitle(""); setBody(""); setImportant(false); setEmail(false); onCreated(); } catch (e) { setError(announcementError(e)); } finally { setBusy(false); } }
  return <aside className={`${styles.card} ${styles.composer}`}><h2>Thông báo mới · {selected.code}</h2><form className={styles.form} onSubmit={submit}>
    <label>Tiêu đề *<input data-testid="announcement-create-title" required maxLength={200} value={title} disabled={busy} onChange={e => setTitle(e.target.value)} /></label>
    <label>Nội dung (Markdown) *<textarea data-testid="announcement-create-body" required maxLength={5000} value={body} disabled={busy} onChange={e => setBody(e.target.value)} /></label>
    <p className={styles.muted}>Gửi tới {selected.studentCount} sinh viên · tối đa 5.000 ký tự</p>
    <label className={styles.check}><input data-testid="announcement-important" type="checkbox" checked={important} disabled={busy} onChange={e => { setImportant(e.target.checked); if (!e.target.checked) setEmail(false); }} />Thông báo quan trọng</label>
    <label className={styles.check}><input data-testid="announcement-email" type="checkbox" checked={email} disabled={!important || busy} onChange={e => setEmail(e.target.checked)} />Gửi email cho sinh viên</label>
    <p className={styles.muted}>Email gửi theo tùy chọn của người nhận. Hai lựa chọn này chỉ có khi tạo, không đổi được khi sửa.</p>
    {error && <p role="alert" className={styles.error}>{error}</p>}<Button data-testid="announcement-create-submit" type="submit" variant="primary" disabled={busy || !title.trim() || !body.trim()}>{busy ? "Đang đăng…" : "Đăng thông báo"}</Button>
  </form></aside>;
}
