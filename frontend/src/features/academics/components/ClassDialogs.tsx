"use client";

import { type CandidateLoader } from "../hooks/useCandidates";
import { useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Button, Dialog, showToast } from "@/shared/ui";
import { studentDetailSections } from "../slots";
import { PersonPicker } from "./PersonPicker";
import { Avatar, formatDate, Modal, operationError } from "./ui";
import type { ClassStatus, CourseClassView, EnrollmentView, PersonView } from "../model/types";
import styles from "../academics.module.css";

export class ReopenConflictError extends Error {
  constructor(public readonly students: string[]) { super("Không thể mở lại lớp vì có sinh viên đang ở lớp khác cùng môn."); }
}

/**
 * Nút đổi trạng thái lớp trong thẻ Trạng thái (Mở lớp / Mở lại lớp, Lưu trữ lớp) và popup xác nhận chỉ có tiêu đề và nút,
 * như popup Lưu trữ môn học; lỗi (sinh viên ở lớp khác) hiện trong popup.
 */
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
    try {
      await onChange(target, value.version);
      if (target === "ARCHIVED") showToast("Đã lưu trữ lớp", "danger"); else showToast("Đã mở lớp");
      setTarget(null);
    }
    catch (cause) { setError(operationError(cause)); if (cause instanceof ReopenConflictError) setConflicts(cause.students); }
    finally { setBusy(false); }
  }
  function choose(status: ClassStatus) { setTarget(status); setError(""); setConflicts([]); }
  return <>
    {value.status !== "OPEN" && <Button variant="primary" icon="lock_open" onClick={() => choose("OPEN")} data-testid="class-state-open">{value.status === "ARCHIVED" ? "Mở lại lớp" : "Mở lớp"}</Button>}
    {value.status !== "ARCHIVED" && <Button variant="danger" icon="archive" onClick={() => choose("ARCHIVED")} data-testid="class-state-archive">Lưu trữ lớp</Button>}
    <Dialog open={target !== null} title={`${label}?`} icon={open ? "lock_open" : "archive"} tone={open ? "primary" : "danger"} onClose={() => setTarget(null)} dismissible={!busy}
      footer={<Button variant={open ? "primary" : "danger"} className="w-full" disabled={busy} onClick={() => void confirm()} data-testid="class-state-confirm">{open ? (value.status === "ARCHIVED" ? "Mở lại" : "Mở lớp") : "Lưu trữ"}</Button>}>
      <p>Bạn có chắc muốn {label.toLocaleLowerCase("vi")} {value.name} không?</p>
      {error ? <div role="alert" className="text-sm text-danger">{error}{conflicts.length > 0 && <ul className="mt-1 list-disc pl-5">{conflicts.map((name, index) => <li key={`${name}-${index}`}>{name}</li>)}</ul>}</div> : null}
    </Dialog>
  </>;
}

/**
 * Popup Đổi giảng viên, cùng kiểu Đổi chủ nhiệm môn: một ô chọn mặc định là giảng viên hiện tại; bỏ chọn (x) là bỏ phân
 * công. Chỉ Lưu được khi đã đổi.
 */
export function AssignTeacherDialog({ value, teachers, onAssign, onClose, loadTeachers }: { value: CourseClassView; teachers: PersonView[]; loadTeachers?: CandidateLoader; onAssign: (teacherId: string | null, version: number) => Promise<void>; onClose: () => void }) {
  const [selected, setSelected] = useState<PersonView | null>(value.teacher);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const unchanged = (selected?.id ?? "") === (value.teacher?.id ?? "");
  async function assign() {
    if (busy || unchanged) return;
    setBusy(true); setError("");
    try { await onAssign(selected?.id ?? null, value.version); if (selected) showToast("Đã đổi giảng viên"); else showToast("Đã bỏ phân công giảng viên", "danger"); onClose(); }
    catch (cause) { setError(operationError(cause)); } finally { setBusy(false); }
  }
  return <Dialog open title="Đổi giảng viên" size="lg" onClose={onClose} dismissible={!busy}
    footer={<Button variant="primary" disabled={busy || unchanged} onClick={() => void assign()} data-testid="class-teacher-save">Lưu</Button>}>
    <PersonPicker label="Giảng viên" value={selected} onChange={setSelected} people={teachers} loader={loadTeachers} clearable disabled={busy} placeholder="Chưa phân công" />
    {error && <p role="alert" className="text-sm text-danger">{error}</p>}
  </Dialog>;
}

export function StudentDetailDialog({ classId, student, onClose }: { classId: string; student: EnrollmentView; onClose: () => void }) {
  return <Modal title="Thông tin sinh viên" onClose={onClose}>
    <div className={styles.person}><Avatar name={student.name}/><div><strong>{student.name}</strong><small>{student.email}</small></div></div>
    <dl className={styles.readList} style={{ marginTop: 22 }}><dt>Ngày ghi danh</dt><dd>{formatDate(student.enrolledAt)}</dd><dt>Trạng thái</dt><dd>Đang học</dd></dl>
    <SlotOutlet slot={studentDetailSections} props={{ classId, studentId: student.accountId }}/>
  </Modal>;
}
