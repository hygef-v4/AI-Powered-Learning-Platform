"use client";

import { useId, useState, type FormEvent } from "react";
import type { CreateClassInput, PersonView, SubjectView } from "./types";
import { Icon, Modal, operationError } from "./ui";
import styles from "./academics.module.css";

export function ClassFormDialog({ subject, teachers, onSubmit, onClose }: { subject: SubjectView; teachers: PersonView[]; onSubmit: (value: CreateClassInput) => Promise<void>; onClose: () => void }) {
  const id = useId();
  const [values, setValues] = useState({ code: "", name: "", term: "", description: "", teacherId: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    if (!values.code.trim() || !values.name.trim() || !values.term.trim()) { setError("Vui lòng điền mã lớp, tên lớp và học kỳ."); return; }
    setBusy(true); setError("");
    try { await onSubmit({ subjectId: subject.id, code: values.code.trim(), name: values.name.trim(), term: values.term.trim(), description: values.description.trim(), teacherId: values.teacherId || null }); onClose(); }
    catch (cause) { setError(operationError(cause)); } finally { setBusy(false); }
  }
  return <Modal title="Tạo lớp" description={`Môn: ${subject.code} · ${subject.name}`} onClose={onClose} busy={busy}
    footer={<><button type="button" disabled={busy} className={styles.button} onClick={onClose}>Hủy</button><button type="submit" form={`${id}-form`} disabled={busy} className={styles.primaryButton} data-testid="class-form-submit"><Icon name="plus"/>{busy ? "Đang tạo…" : "Tạo lớp"}</button></>}>
    <form id={`${id}-form`} onSubmit={submit} className={styles.formGrid}>
      {([ ["code", "Mã lớp", 30], ["term", "Học kỳ", 20], ["name", "Tên lớp", 200] ] as const).map(([key, label, length]) => <div key={key} className={`${styles.formField} ${key === "name" ? styles.wide : ""}`}><label htmlFor={`${id}-${key}`}>{label} <span className={styles.required}>*</span></label><input id={`${id}-${key}`} className={styles.field} maxLength={length} required value={values[key]} disabled={busy} onChange={(event) => setValues({ ...values, [key]: event.target.value })} data-testid={`class-form-${key}`}/>{key === "code" && <p className={styles.hint}>Duy nhất trong môn, không đổi sau khi tạo.</p>}</div>)}
      <div className={`${styles.formField} ${styles.wide}`}><label htmlFor={`${id}-description`}>Mô tả <span className={styles.muted}>(tùy chọn)</span></label><textarea id={`${id}-description`} className={styles.textarea} maxLength={2000} value={values.description} disabled={busy} onChange={(event) => setValues({ ...values, description: event.target.value })}/></div>
      <div className={`${styles.formField} ${styles.wide}`}><label htmlFor={`${id}-teacher`}>Giảng viên <span className={styles.muted}>(tùy chọn)</span></label><select id={`${id}-teacher`} value={values.teacherId} disabled={busy} className={styles.field} onChange={(event) => setValues({ ...values, teacherId: event.target.value })}><option value="">Chưa phân công</option>{teachers.map((teacher) => <option key={teacher.id} value={teacher.id}>{teacher.name}</option>)}</select><p className={styles.hint}>Bắt buộc trước khi mở lớp.</p></div>
      <div className={`${styles.notice} ${styles.wide}`}><Icon name="info"/><p>Lớp mới ở trạng thái <strong>Nháp</strong>: sinh viên chưa thấy lớp cho tới khi bạn bấm Mở lớp.</p></div>
      {error && <p role="alert" className={`${styles.error} ${styles.wide}`}>{error}</p>}
    </form>
  </Modal>;
}
