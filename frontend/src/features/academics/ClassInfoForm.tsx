"use client";

import { useId, useState, type FormEvent } from "react";
import type { ClassInfoInput, CourseClassView } from "./types";
import { formatDate, Icon, operationError } from "./ui";
import styles from "./academics.module.css";

export function ClassInfoForm(props: { value: CourseClassView; onSave: (value: ClassInfoInput) => Promise<void> }) {
  return <InfoEditor key={`${props.value.id}-${props.value.version}`} {...props}/>;
}
function InfoEditor({ value, onSave }: { value: CourseClassView; onSave: (value: ClassInfoInput) => Promise<void> }) {
  const id = useId();
  const original = { name: value.name, term: value.term, description: value.description };
  const [fields, setFields] = useState(original);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);
  const archived = value.status === "ARCHIVED";
  async function save(event: FormEvent) {
    event.preventDefault();
    if (busy || archived) return;
    if (!fields.name.trim() || !fields.term.trim()) { setError("Vui lòng điền tên lớp và học kỳ."); return; }
    setBusy(true); setError(""); setSaved(false);
    try { await onSave({ name: fields.name.trim(), description: fields.description.trim(), term: fields.term.trim(), version: value.version }); setSaved(true); }
    catch (cause) { setError(operationError(cause)); } finally { setBusy(false); }
  }
  return <form className={styles.panel} onSubmit={save} aria-label="Thông tin lớp">
    <div className={styles.formGrid}><div className={styles.formField}><label htmlFor={`${id}-code`}>Mã lớp</label><input id={`${id}-code`} className={`${styles.field} ${styles.code}`} value={value.code} disabled/></div>
      <div className={styles.formField}><label htmlFor={`${id}-term`}>Học kỳ <span className={styles.required}>*</span></label><input id={`${id}-term`} className={styles.field} required maxLength={20} disabled={archived || busy} value={fields.term} onChange={(event) => setFields({ ...fields, term: event.target.value })}/></div>
      <div className={`${styles.formField} ${styles.wide}`}><label htmlFor={`${id}-name`}>Tên lớp <span className={styles.required}>*</span></label><input id={`${id}-name`} className={styles.field} required maxLength={200} disabled={archived || busy} value={fields.name} onChange={(event) => setFields({ ...fields, name: event.target.value })}/></div>
      <div className={`${styles.formField} ${styles.wide}`}><label htmlFor={`${id}-description`}>Mô tả <span className={styles.muted}>(tùy chọn)</span></label><textarea id={`${id}-description`} className={styles.textarea} maxLength={2000} disabled={archived || busy} value={fields.description} onChange={(event) => setFields({ ...fields, description: event.target.value })}/></div>
    </div><div className={styles.meta}><span>Sinh viên <strong>{value.activeStudentCount} đang học · {value.removedStudentCount} đã gỡ</strong></span><span>Ngày tạo <strong>{formatDate(value.createdAt)}</strong></span></div>
    {error && <p role="alert" className={styles.error}>{error}</p>}{saved && <p role="status" className={styles.success}>Đã lưu thay đổi.</p>}
    {archived ? <p className={styles.hint}>Lớp đã lưu trữ. Thông tin chỉ được xem.</p> : <div className={styles.formActions}><button type="button" className={styles.button} disabled={busy} onClick={() => { setFields(original); setError(""); setSaved(false); }}>Hủy</button><button type="submit" className={styles.primaryButton} disabled={busy} data-testid="class-info-save"><Icon name="save"/>{busy ? "Đang lưu…" : "Lưu thay đổi"}</button></div>}
  </form>;
}
