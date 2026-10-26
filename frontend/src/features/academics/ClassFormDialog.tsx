"use client";

import {useCandidates,type CandidateLoader} from "./useCandidates";
import { useId, useState, type FormEvent } from "react";
import type { CreateClassInput, PersonView, SubjectView } from "./types";
import { Icon, Modal, operationError } from "./ui";
import styles from "./academics.module.css";

export function ClassFormDialog({ subject, teachers, onSubmit, onClose, loadTeachers }: { subject: SubjectView; loadTeachers?:CandidateLoader; teachers: PersonView[]; onSubmit: (value: CreateClassInput) => Promise<void>; onClose: () => void }) {
  const id = useId();
  const [values, setValues] = useState({ code: "", name: "", term: "", description: "", teacherId: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [teacherQuery,setTeacherQuery]=useState("");
  const options=useCandidates(teachers,teacherQuery,loadTeachers);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    if (!values.code.trim() || !values.name.trim() || !values.term.trim()) { setError("Vui lòng điền mã lớp, tên lớp và học kỳ."); return; }
    setBusy(true); setError("");
    try { await onSubmit({ subjectId: subject.id, code: values.code.trim(), name: values.name.trim(), term: values.term.trim(), description: values.description.trim(), teacherId: values.teacherId || null }); onClose(); }
    catch (cause) { setError(operationError(cause)); } finally { setBusy(false); }
  }
  return <Modal title="Tạo lớp" description={`Môn: ${subject.code} · ${subject.name}`} onClose={onClose} busy={busy}
    footer={<><button type="submit" form={`${id}-form`} disabled={busy} className={styles.primaryButton} data-testid="class-form-submit"><Icon name="plus"/>{busy ? "Đang tạo…" : "Tạo lớp"}</button></>}>
    <form id={`${id}-form`} onSubmit={submit} className={styles.formGrid}>
      {([ ["code", "Mã lớp", 30], ["term", "Học kỳ", 20], ["name", "Tên lớp", 200] ] as const).map(([key, label, length]) => <div key={key} className={`${styles.formField} ${key === "name" ? styles.wide : ""}`}><label htmlFor={`${id}-${key}`}>{label} <span className={styles.required}>*</span></label><input id={`${id}-${key}`} className={styles.field} maxLength={length} required value={values[key]} disabled={busy} onChange={(event) => setValues({ ...values, [key]: event.target.value })} data-testid={`class-form-${key}`}/></div>)}
      <div className={`${styles.formField} ${styles.wide}`}><label htmlFor={`${id}-description`}>Mô tả</label><textarea id={`${id}-description`} className={styles.textarea} maxLength={2000} value={values.description} disabled={busy} onChange={(event) => setValues({ ...values, description: event.target.value })}/></div>
      <div className={`${styles.formField} ${styles.wide}`}><label htmlFor={`${id}-teacher`}>Giảng viên</label>{loadTeachers && <input aria-label="Tìm giảng viên tạo lớp" placeholder="Tìm giảng viên" className={styles.field} value={teacherQuery} onChange={e=>setTeacherQuery(e.target.value)} disabled={busy}/>}{options.error && <p role="alert">{options.error}</p>}<select id={`${id}-teacher`} value={values.teacherId} disabled={busy} className={styles.field} onChange={(event) => setValues({ ...values, teacherId: event.target.value })}><option value="">Chưa phân công</option>{options.rows.map((teacher) => <option key={teacher.id} value={teacher.id}>{teacher.name}</option>)}</select></div>
      {error && <p role="alert" className={`${styles.error} ${styles.wide}`}>{error}</p>}
    </form>
  </Modal>;
}
