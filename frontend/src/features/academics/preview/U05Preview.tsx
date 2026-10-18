"use client";

import { useState } from "react";
import { AcademicsShell, type AcademicsView } from "../AcademicsShell";
import { ClassListPage } from "../ClassListPage";
import { ClassDashboardPage } from "../ClassDashboardPage";
import { ClassFormDialog } from "../ClassFormDialog";
import { ManagedClassPage, type ManagedClassActions } from "../ManagedClassPage";
import { ReadOnlyClassPage } from "../ReadOnlyClassPage";
import { ManagerDashboardPage, MySubjectPage } from "../ManagerDashboardPage";
import { ReopenConflictError } from "../ClassDialogs";
import { parseEnrollmentList } from "../enrollmentList";
import type { Audience, CourseClassView, EnrollmentRowResult, EnrollmentView, SubjectView } from "../types";
import { initialClasses, initialEnrollments, studentCandidates, subjects, teachers } from "./fixtures";
import styles from "../academics.module.css";

/** Adapter minh họa trong bộ nhớ, chỉ được mount ở route development đã guard phía server. */
export function U05Preview() {
  const [audience, setAudience] = useState<Audience>("manager");
  const [view, setView] = useState<AcademicsView>("classes");
  const [classId, setClassId] = useState<string | null>(null);
  const [subjectId, setSubjectId] = useState(subjects[0]!.id);
  const [classes, setClasses] = useState(initialClasses);
  const [enrollments, setEnrollments] = useState(initialEnrollments);
  const [creating, setCreating] = useState<SubjectView | null>(null);
  const rows = classes.map((course) => ({ ...course, activeStudentCount: (enrollments[course.id] ?? []).filter((student) => student.status === "ACTIVE").length, removedStudentCount: (enrollments[course.id] ?? []).filter((student) => student.status === "REMOVED").length }));
  const selected = rows.find((course) => course.id === classId);
  const subject = subjects.find((item) => item.id === subjectId)!;
  const taught = rows.filter((course) => course.teacher?.id === (audience === "manager" ? teachers[3]!.id : teachers[0]!.id));
  const studying = rows.filter((course) => (enrollments[course.id] ?? []).some((student) => student.accountId === "student-1" && student.status === "ACTIVE"));
  function navigate(next: AcademicsView) { setClassId(null); setView(next); }
  function update(current: CourseClassView, patch: Partial<CourseClassView>, expectedVersion: number) {
    if (expectedVersion !== current.version) throw new Error("Dữ liệu đã thay đổi. Vui lòng tải lại.");
    setClasses((all) => all.map((course) => course.id === current.id ? { ...course, ...patch, version: course.version + 1 } : course));
  }
  function hasOtherClass(accountId: string, current: CourseClassView) {
    return rows.some((course) => course.id !== current.id && course.subject.id === current.subject.id && course.status !== "ARCHIVED" && (enrollments[course.id] ?? []).some((student) => student.accountId === accountId && student.status === "ACTIVE"));
  }
  function enroll(accountId: string, current: CourseClassView) {
    if (current.status === "ARCHIVED") throw new Error("Lớp đã lưu trữ, không thể thay đổi ghi danh.");
    const person = studentCandidates.find((candidate) => candidate.id === accountId);
    if (!person) throw new Error("Không tìm thấy sinh viên.");
    if (hasOtherClass(accountId, current)) throw new Error("Sinh viên đang ở lớp khác cùng môn.");
    const existing = (enrollments[current.id] ?? []).find((student) => student.accountId === accountId);
    if (existing?.status === "ACTIVE") throw new Error("Sinh viên đã có trong lớp.");
    const row: EnrollmentView = { accountId, name: person.name, email: person.email, status: "ACTIVE", enrolledAt: "2026-10-18" };
    setEnrollments((all) => ({ ...all, [current.id]: [...(all[current.id] ?? []).filter((student) => student.accountId !== accountId), row] }));
  }
  const actions: ManagedClassActions | null = selected ? {
    async onSaveInfo(input) { update(selected, { name: input.name, description: input.description, term: input.term }, input.version); },
    async onAssign(teacherId, version) {
      if (selected.status === "OPEN" && !teacherId) throw new Error("Lớp đang mở phải có giảng viên.");
      update(selected, { teacher: teachers.find((teacher) => teacher.id === teacherId) ?? null }, version);
    },
    async onChangeStatus(status, version) {
      if (status === "OPEN") {
        if (!selected.teacher) throw new Error("Cần phân công giảng viên trước khi mở lớp.");
        const conflicts = (enrollments[selected.id] ?? []).filter((student) => student.status === "ACTIVE" && hasOtherClass(student.accountId, selected));
        if (conflicts.length) throw new ReopenConflictError(conflicts.map((student) => student.name));
      }
      update(selected, { status }, version);
    },
    async onDistribution(enabled, version) { update(selected, { showGradeDistribution: enabled }, version); },
    async onEnrollmentStatus(accountId, status) {
      if (status === "ACTIVE") { enroll(accountId, selected); return; }
      setEnrollments((all) => ({ ...all, [selected.id]: (all[selected.id] ?? []).map((student) => student.accountId === accountId ? { ...student, status } : student) }));
    },
    async onEnroll(accountId) { enroll(accountId, selected); },
    async onBatch(input) {
      const results: EnrollmentRowResult[] = [];
      for (const row of parseEnrollmentList(input)) {
        const person = studentCandidates.find((candidate) => candidate.email === row.email);
        const current = person && (enrollments[selected.id] ?? []).find((student) => student.accountId === person.id);
        const result = row.error ?? (!person ? "NOT_FOUND" : current?.status === "ACTIVE" ? "ALREADY_ENROLLED" : hasOtherClass(person.id, selected) ? "IN_OTHER_CLASS" : current ? "RESTORED" : "ENROLLED");
        if (person && (result === "ENROLLED" || result === "RESTORED")) enroll(person.id, selected);
        results.push({ line: row.line, email: row.email, result });
      }
      return results;
    },
  } : null;
  const userName = audience === "student" ? studentCandidates[0]!.name : audience === "teacher" ? teachers[0]!.name : teachers[3]!.name;
  return <AcademicsShell audience={audience} userName={userName} active={view} onNavigate={navigate} previewControls={<div className={styles.previewBar}>
    <strong>Bản xem trước U05</strong><span>Dữ liệu minh họa · Thay đổi chỉ giữ trong phiên xem</span><label>Vai trò xem trước<select aria-label="Vai trò xem trước" value={audience} onChange={(event) => { const next = event.target.value as Audience; setAudience(next); setClassId(null); setView(next === "manager" ? "classes" : next === "teacher" ? "teaching" : "my-classes"); }}><option value="manager">Chủ nhiệm môn</option><option value="teacher">Giảng viên</option><option value="student">Sinh viên</option></select></label>
  </div>}>
    {selected && actions ? audience === "manager" && view === "classes" ? <ManagedClassPage key={selected.id} value={selected} students={enrollments[selected.id] ?? []} teachers={teachers} candidates={studentCandidates} onBack={() => setClassId(null)} actions={actions}/> : <ReadOnlyClassPage key={selected.id} audience={audience === "student" ? "student" : "teacher"} value={selected} students={enrollments[selected.id] ?? []} onBack={() => setClassId(null)}/> : <>
      {view === "classes" && <ClassListPage classes={rows} subjects={subjects} initialSubjectId={subjectId} onSubjectChange={setSubjectId} onOpen={setClassId} onCreate={setCreating}/>}
      {(view === "teaching" || view === "my-classes") && <ClassDashboardPage key={view} audience={view === "teaching" ? "teacher" : "student"} classes={view === "teaching" ? taught : studying} onOpen={setClassId}/>}
      {view === "overview" && <ManagerDashboardPage subjects={subjects} classes={rows} teachingCount={taught.filter((course) => course.status === "OPEN").length} onSubject={(id) => { setSubjectId(id); navigate("subject"); }} onTeaching={() => navigate("teaching")}/>}
      {view === "subject" && <><div><select className={styles.select} aria-label="Chọn môn phụ trách" value={subjectId} onChange={(event) => setSubjectId(event.target.value)}>{subjects.map((item) => <option key={item.id} value={item.id}>{item.code} · {item.name}</option>)}</select></div><MySubjectPage subject={subject} classes={rows} onBack={() => navigate("overview")} onClasses={() => navigate("classes")}/></>}
    </>}
    {creating && <ClassFormDialog subject={creating} teachers={teachers} onClose={() => setCreating(null)} onSubmit={async (input) => {
      if (classes.some((course) => course.subject.id === input.subjectId && course.code === input.code)) throw new Error("Mã lớp đã tồn tại trong môn.");
      const now = new Date().toISOString();
      setClasses((all) => [...all, { id: crypto.randomUUID(), subject: creating, code: input.code, name: input.name, description: input.description, term: input.term, teacher: teachers.find((teacher) => teacher.id === input.teacherId) ?? null, status: "DRAFT", activeStudentCount: 0, removedStudentCount: 0, createdAt: now, showGradeDistribution: false, version: 1 }]);
    }}/>}
  </AcademicsShell>;
}
