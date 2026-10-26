"use client";

import type {CandidateLoader} from "./useCandidates";
import { useState } from "react";
import { ClassInfoForm } from "./ClassInfoForm";
import { AssignTeacherDialog, ClassStateActions } from "./ClassDialogs";
import { EnrollmentPanel } from "./EnrollmentPanel";
import { Avatar, Icon, Modal, operationError, PageHeading, StatusBadge, Tabs } from "./ui";
import type { ClassInfoInput, ClassStatus, CourseClassView, EnrollmentRowResult, EnrollmentView, PersonView } from "./types";
import styles from "./academics.module.css";

export type ManagedClassActions = {
  onSaveInfo: (input: ClassInfoInput) => Promise<void>;
  onAssign: (teacherId: string | null, version: number) => Promise<void>;
  onChangeStatus: (status: ClassStatus, version: number) => Promise<void>;
  onDistribution: (enabled: boolean, version: number) => Promise<void>;
  onEnrollmentStatus: (accountId: string, status: "ACTIVE" | "REMOVED") => Promise<void>;
  onEnroll: (accountId: string) => Promise<void>;
  onBatch: (input: string) => Promise<EnrollmentRowResult[]>;
};
export function ManagedClassPage({ value, students, teachers, candidates, onBack, actions, loadTeachers, loadCandidates }: {
  loadTeachers?:CandidateLoader; loadCandidates?:CandidateLoader; value: CourseClassView; students: EnrollmentView[]; teachers: PersonView[]; candidates: PersonView[]; onBack: () => void; actions: ManagedClassActions;
}) {
  const [tab, setTab] = useState<"info" | "students">("info");
  const [assigning, setAssigning] = useState(false);
  const [confirmDistribution, setConfirmDistribution] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const archived = value.status === "ARCHIVED";
  async function setDistribution(enabled: boolean) {
    if (busy || archived) return;
    setBusy(true); setError("");
    try { await actions.onDistribution(enabled, value.version); setConfirmDistribution(false); }
    catch (cause) { setError(operationError(cause)); } finally { setBusy(false); }
  }
  return <>
    <PageHeading title={<>{value.subject.code} · {value.code}<StatusBadge status={value.status}/></>} subtitle="Lớp của môn" onBack={onBack}>
      {!archived && <button className={styles.button} onClick={() => setAssigning(true)} data-testid="class-assign-teacher"><Icon name="person"/>Phân công giảng viên</button>}
      <ClassStateActions value={value} onChange={actions.onChangeStatus}/>
    </PageHeading>
    <Tabs items={[{ key: "info", label: "Thông tin" }, { key: "students", label: "Sinh viên", count: students.filter((student) => student.status === "ACTIVE").length }]} value={tab} onChange={setTab} label="Chi tiết lớp"/>
    {tab === "info" ? <div className={styles.detailGrid}><ClassInfoForm value={value} onSave={actions.onSaveInfo}/><div className={styles.stack}>
      <section className={styles.panel}><h2>Giảng viên</h2>{value.teacher ? <div className={styles.person}><Avatar name={value.teacher.name}/><div><strong>{value.teacher.name}</strong><small>{value.teacher.email}</small></div></div> : <p className={styles.muted}>Chưa phân công giảng viên.</p>}{!archived && <button className={styles.textButton} style={{ marginTop: 18 }} onClick={() => setAssigning(true)}><Icon name="person"/>{value.teacher ? "Đổi giảng viên" : "Phân công giảng viên"}</button>}</section>
      <section className={styles.panel}><h2>Sinh viên được xem</h2><div className={styles.switchRow}><span>Cho sinh viên xem phân bố điểm</span><button type="button" className={styles.switch} role="switch" aria-label="Cho sinh viên xem phân bố điểm" aria-checked={value.showGradeDistribution} disabled={archived || busy} onClick={() => { setError(""); if (value.showGradeDistribution) void setDistribution(false); else setConfirmDistribution(true); }} data-testid="class-grade-distribution"/></div>{error && !confirmDistribution && <p role="alert" className={styles.error}>{error}</p>}</section>
    </div></div> : <EnrollmentPanel loadCandidates={loadCandidates} students={students} editable={!archived} includeRemoved candidates={candidates} onStatusChange={actions.onEnrollmentStatus} onEnroll={actions.onEnroll} onBatch={actions.onBatch}/>}
    {assigning && !archived && <AssignTeacherDialog loadTeachers={loadTeachers} value={value} teachers={teachers} onAssign={actions.onAssign} onClose={() => setAssigning(false)}/>}
    {confirmDistribution && <Modal title="Bật phân bố điểm?" onClose={() => setConfirmDistribution(false)} busy={busy} footer={<><button disabled={busy} className={styles.primaryButton} onClick={() => void setDistribution(true)}>Xác nhận bật</button></>}><p>Sinh viên xem được phân bố điểm ẩn danh của các bài đã công bố.</p>{error && <p role="alert" className={styles.error}>{error}</p>}</Modal>}
  </>;
}
