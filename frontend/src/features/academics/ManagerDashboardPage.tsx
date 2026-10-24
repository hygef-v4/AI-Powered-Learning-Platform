import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { subjectSummarySections } from "./slots";
import { EmptyState, Icon, PageHeading } from "./ui";
import type { CourseClassView, SubjectView } from "./types";
import styles from "./academics.module.css";

export function ManagerDashboardPage({ subjects, classes, teachingCount = 0, onSubject, onTeaching }: { subjects: SubjectView[]; classes: CourseClassView[]; teachingCount?: number; onSubject: (id: string) => void; onTeaching: () => void }) {
  return <><PageHeading title="Tổng quan" subtitle="Các môn bạn phụ trách"/>
    {teachingCount > 0 && <div className={styles.notice}><Icon name="info"/><div>Bạn đang được phân công dạy <strong>{teachingCount} lớp</strong>. <button className={styles.textButton} onClick={onTeaching}>Mở Lớp được phân công<Icon name="chevron"/></button></div></div>}
    <div className={styles.cardGrid}>{subjects.map((subject) => { const rows = classes.filter((course) => course.subject.id === subject.id); return <article key={subject.id} className={styles.subjectCard}><span className={styles.brandMark}><Icon name="book"/></span><h2>{subject.code}</h2><p className={styles.muted}>{subject.name}</p><div className={styles.statRow}>{([ ["DRAFT", "Nháp"], ["OPEN", "Đang mở"], ["ARCHIVED", "Lưu trữ"] ] as const).map(([status, label]) => <div key={status}><small>{label}</small><strong>{rows.filter((row) => row.status === status).length}</strong></div>)}</div><SlotOutlet slot={subjectSummarySections} props={{subjectId:subject.id,variant:"inline" as const}}/><button className={styles.textButton} style={{ marginTop: 22 }} onClick={() => onSubject(subject.id)}>Truy cập quản lý môn<Icon name="chevron"/></button></article>; })}</div>
    {!subjects.length && <section className={styles.panel}><EmptyState title="Bạn chưa được giao quản lý môn"/></section>}
  </>;
}
export function MySubjectPage({ subject, classes, onBack, onClasses }: { subject: SubjectView; classes: CourseClassView[]; onBack: () => void; onClasses: () => void }) {
  const rows = classes.filter((course) => course.subject.id === subject.id);
  return <>
    <PageHeading title={subject.name} subtitle={`Môn học của tôi › ${subject.code}`} onBack={onBack}><button className={styles.primaryButton} onClick={onClasses}><Icon name="users"/>Lớp của môn</button></PageHeading>
    <section className={styles.panel}>
      <h2>Thông tin môn</h2>
      <dl className={styles.readList}><dt>Mã môn</dt><dd className={styles.code}>{subject.code}</dd><dt>Tên môn</dt><dd>{subject.name}</dd><dt>Mô tả</dt><dd>{subject.description || "Chưa có mô tả."}</dd></dl>
      <p className={styles.hint}>Tên và mô tả môn do Quản trị viên quản lý.</p>
      <div className={styles.statRow}>{([ ["DRAFT", "Lớp nháp"], ["OPEN", "Lớp đang mở"], ["ARCHIVED", "Lớp lưu trữ"] ] as const).map(([status, label]) => <div key={status}><strong>{rows.filter((row) => row.status === status).length}</strong><small>{label}</small></div>)}</div>
    </section>
    <SlotOutlet slot={subjectSummarySections} props={{subjectId:subject.id,variant:"card" as const}}/>
  </>;
}
