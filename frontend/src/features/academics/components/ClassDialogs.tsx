"use client";

import { type CandidateLoader } from "../hooks/useCandidates";
import { useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Button, Dialog, showToast } from "@/shared/ui";
import { studentDetailSections } from "../slots";
import { PersonPicker } from "./PersonPicker";
import { Avatar } from "./Avatar";
import { formatDate, formatOperationError } from "../model/format";
import type { ClassStatus, CourseClassView, EnrollmentView, PersonView } from "../model/types";
import styles from "../styles/academics.module.css";
import { msg } from "@/i18n/msg";

export class ReopenConflictError extends Error {
  // Giữ danh sách sinh viên bị xung đột để hộp thoại có thể hiển thị khi mở lại lớp.
  constructor(public readonly students: string[]) { super(msg("academics.components.ClassDialogs.cannotReopenTheClassBecause")); }
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
  const label = open ? value.status === "ARCHIVED" ? msg("academics.components.ClassDialogs.reopenClass") : msg("academics.components.ClassDialogs.openClass") : msg("academics.components.ClassDialogs.archiveClass");
  // Gửi trạng thái đích cùng version và hiển thị danh sách xung đột nếu mở lại thất bại.
  async function confirm() {
    if (!target || busy) return;
    setBusy(true); setError(""); setConflicts([]);
    try {
      await onChange(target, value.version);
      if (target === "ARCHIVED") showToast(msg("academics.components.ClassDialogs.classArchived"), "danger"); else showToast(msg("academics.components.ClassDialogs.classOpened"));
      setTarget(null);
    }
    catch (cause) { setError(formatOperationError(cause)); if (cause instanceof ReopenConflictError) setConflicts(cause.students); }
    finally { setBusy(false); }
  }
  // Đổi lựa chọn trong hộp thoại và xóa lỗi của lần thử trước.
  function choose(status: ClassStatus) { setTarget(status); setError(""); setConflicts([]); }
  return <>
    {value.status !== "OPEN" && <Button variant="primary" icon="lock_open" onClick={() => choose("OPEN")} data-testid="class-state-open">{value.status === "ARCHIVED" ? msg("academics.components.ClassDialogs.reopenClass") : msg("academics.components.ClassDialogs.openClass")}</Button>}
    {value.status !== "ARCHIVED" && <Button variant="danger" icon="archive" onClick={() => choose("ARCHIVED")} data-testid="class-state-archive">{msg("academics.components.ClassDialogs.archiveClass")}</Button>}
    <Dialog open={target !== null} title={`${label}?`} icon={open ? "lock_open" : "archive"} tone={open ? "primary" : "danger"} onClose={() => setTarget(null)} dismissible={!busy}
      footer={<Button variant={open ? "primary" : "danger"} className="w-full" disabled={busy} onClick={() => void confirm()} data-testid="class-state-confirm">{open ? (value.status === "ARCHIVED" ? msg("academics.components.ClassDialogs.reopen") : msg("academics.components.ClassDialogs.openClass")) : msg("academics.components.ClassDialogs.archive")}</Button>}>
      <p>{msg("academics.components.ClassDialogs.areYouSureYouWant")}{label.toLocaleLowerCase("vi")} {msg("academics.components.ClassDialogs.class")}{value.code} {msg("academics.components.ClassDialogs.text")}</p>
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
  // Gửi tài khoản được chọn hoặc null để bỏ phân công, kèm version đang xem.
  async function assign() {
    if (busy || unchanged) return;
    setBusy(true); setError("");
    try { await onAssign(selected?.id ?? null, value.version); if (selected) showToast(msg("academics.components.ClassDialogs.lecturerChanged")); else showToast(msg("academics.components.ClassDialogs.lecturerUnassigned"), "danger"); onClose(); }
    catch (cause) { setError(formatOperationError(cause)); } finally { setBusy(false); }
  }
  return <Dialog open title={msg("academics.components.ClassDialogs.changeLecturer")} size="lg" onClose={onClose} dismissible={!busy}
    footer={<Button variant="primary" disabled={busy || unchanged} onClick={() => void assign()} data-testid="class-teacher-save">{msg("academics.components.ClassDialogs.save")}</Button>}>
    <PersonPicker label={msg("academics.components.ClassDialogs.lecturer")} value={selected} onChange={setSelected} people={teachers} loader={loadTeachers} clearable disabled={busy} placeholder={msg("academics.components.ClassDialogs.notAssigned")} />
    {error && <p role="alert" className="text-sm text-danger">{error}</p>}
  </Dialog>;
}

// Hiện thông tin tài khoản và ghi danh của sinh viên đã chọn trong lớp.
export function StudentDetailDialog({ classId, student, onClose }: { classId: string; student: EnrollmentView; onClose: () => void }) {
  return <Dialog open title={msg("academics.components.ClassDialogs.studentInformation")} onClose={onClose}>
    <div className={styles.person}><Avatar name={student.name}/><div><strong>{student.name}</strong><small>{student.email}</small></div></div>
    <dl className={styles.readList} style={{ marginTop: 22 }}><dt>{msg("academics.components.ClassDialogs.enrollmentDate")}</dt><dd>{formatDate(student.enrolledAt)}</dd><dt>{msg("academics.components.ClassDialogs.status")}</dt><dd>{msg("academics.components.ClassDialogs.studying")}</dd></dl>
    <SlotOutlet slot={studentDetailSections} props={{ classId, studentId: student.accountId }}/>
  </Dialog>;
}
