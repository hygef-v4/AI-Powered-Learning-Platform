"use client";

import { useId, useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { studentDetailSections } from "./slots";
import { Avatar, formatDate, Icon, Modal, operationError } from "./ui";
import type { ClassStatus, CourseClassView, EnrollmentView, PersonView } from "./types";
import styles from "./academics.module.css";

export class ReopenConflictError extends Error {
  constructor(public readonly students: string[]) { super("Không thể mở lại lớp vì có sinh viên đang ở lớp khác cùng môn."); }
}
export function ClassStateActions({ value, onChange }: { value: CourseClassView; onChange: (status: ClassStatus, version: number) => Promise<void> }) {
  const [target, setTarget] = useState<ClassStatus | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [conflicts, setConflicts] = useState<string[]>([]);
  const open = target === "OPEN";
  const label = open ? value.status === "ARCHIVED" ? "Mở lại lớp" : "Mở lớp" : "Lưu trữ lớp";
  async function confirm() {
    if (!target || busy) return;
    setBusy(true); setError(""); setConflicts([]);
    try { await onChange(target, value.version); setTarget(null); }
    catch (cause) { setError(operationError(cause)); if (cause instanceof ReopenConflictError) setConflicts(cause.students); }
    finally { setBusy(false); }
  }
  function choose(status: ClassStatus) { setTarget(status); setError(""); setConflicts([]); }
  return <>
    {value.status !== "OPEN" && <button className={styles.primaryButton} onClick={() => choose("OPEN")} data-testid="class-state-open"><Icon name="check"/>{value.status === "ARCHIVED" ? "Mở lại lớp" : "Mở lớp"}</button>}
    {value.status !== "ARCHIVED" && <button className={styles.dangerButton} onClick={() => choose("ARCHIVED")} data-testid="class-state-archive"><Icon name="archive"/>Lưu trữ lớp</button>}
    {target && <Modal title={label} onClose={() => setTarget(null)} busy={busy} footer={<><button disabled={busy} className={styles.button} onClick={() => setTarget(null)}>Hủy</button><button disabled={busy || (open && !value.teacher)} className={open ? styles.primaryButton : styles.dangerButton} onClick={() => void confirm()} data-testid="class-state-confirm">{busy ? "Đang xử lý…" : label}</button></>}>
      <p>{open ? "Sinh viên đã ghi danh sẽ truy cập được nội dung lớp khi lớp được mở." : "Lớp chuyển sang chỉ xem. Ghi danh, bài làm và điểm được giữ nguyên."}</p>
      {open && !value.teacher && <p role="alert" className={styles.error}>Cần phân công giảng viên trước khi mở lớp.</p>}
      {error && <div role="alert" className={styles.error}>{error}{conflicts.length > 0 && <ul>{conflicts.map((name) => <li key={name}>{name}</li>)}</ul>}</div>}
    </Modal>}
  </>;
}
export function AssignTeacherDialog({ value, teachers, onAssign, onClose }: { value: CourseClassView; teachers: PersonView[]; onAssign: (teacherId: string | null, version: number) => Promise<void>; onClose: () => void }) {
  const id = useId();
  const [selected, setSelected] = useState(value.teacher?.id ?? "");
  const [query, setQuery] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function assign() {
    if (busy || (value.status === "OPEN" && !selected)) return;
    setBusy(true); setError("");
    try { await onAssign(selected || null, value.version); onClose(); } catch (cause) { setError(operationError(cause)); } finally { setBusy(false); }
  }
  return <Modal title="Phân công giảng viên" description={`${value.subject.code} · ${value.code}`} onClose={onClose} busy={busy} footer={<><button disabled={busy} className={styles.button} onClick={onClose}>Hủy</button><button disabled={busy || (value.status === "OPEN" && !selected)} className={styles.primaryButton} onClick={() => void assign()} data-testid="class-teacher-save">{busy ? "Đang lưu…" : "Lưu phân công"}</button></>}>
    <label className={styles.search}><Icon name="search"/><input aria-label="Tìm giảng viên" placeholder="Tìm theo tên hoặc email" value={query} disabled={busy} onChange={(event) => setQuery(event.target.value)}/></label>
    <div className={styles.choiceList} role="radiogroup" aria-label="Chọn giảng viên">
      <label className={styles.choice}><span className={styles.muted}>Chưa phân công</span><input type="radio" name={id} aria-label="Chưa phân công" value="" disabled={busy || value.status === "OPEN"} checked={!selected} onChange={() => setSelected("")}/></label>
      {teachers.filter((teacher) => `${teacher.name} ${teacher.email}`.toLocaleLowerCase("vi").includes(query.toLocaleLowerCase("vi"))).map((teacher) => <label className={styles.choice} key={teacher.id}><span className={styles.person}><Avatar name={teacher.name}/><span><strong>{teacher.name}</strong><small>{teacher.email}</small></span></span><input type="radio" name={id} checked={selected === teacher.id} value={teacher.id} aria-label={teacher.name} disabled={busy} onChange={() => setSelected(teacher.id)}/></label>)}
    </div><p className={styles.hint}>Lớp đang mở phải có giảng viên chính. Phân công mới thay thế quyền dạy của giảng viên hiện tại.</p>
    {error && <p role="alert" className={styles.error}>{error}</p>}
  </Modal>;
}
export function StudentDetailDialog({ classId, student, onClose }: { classId: string; student: EnrollmentView; onClose: () => void }) {
  return <Modal title="Thông tin sinh viên" onClose={onClose} footer={<button className={styles.button} onClick={onClose}>Đóng</button>}>
    <div className={styles.person}><Avatar name={student.name}/><div><strong>{student.name}</strong><small>{student.email}</small></div></div>
    <dl className={styles.readList} style={{ marginTop: 22 }}><dt>Ngày ghi danh</dt><dd>{formatDate(student.enrolledAt)}</dd><dt>Trạng thái</dt><dd>Đang học</dd></dl>
    <SlotOutlet slot={studentDetailSections} props={{ classId, studentId: student.accountId }}/>
  </Modal>;
}
