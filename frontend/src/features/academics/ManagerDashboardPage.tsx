import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { subjectSummarySections } from "./slots";
import { EmptyState, Icon, PageHeading } from "./ui";
import type { CourseClassView, SubjectView } from "./types";
import styles from "./academics.module.css";

export function ManagerDashboardPage({ subjects, classes, teachingCount = 0, onSubject, onTeaching }: { subjects: SubjectView[]; classes: CourseClassView[]; teachingCount?: number; onSubject: (id: string) => void; onTeaching: () => void }) {
  return <><PageHeading title="Tổng quan"/>
    {teachingCount > 0 && <div className={styles.notice}><Icon name="info"/><div>Bạn đang được phân công dạy <strong>{teachingCount} lớp</strong>. <button className={styles.textButton} onClick={onTeaching}>Mở Lớp được phân công<Icon name="chevron"/></button></div></div>}
    <div className={styles.cardGrid}>{subjects.map((subject) => { const rows = classes.filter((course) => course.subject.id === subject.id); return <article key={subject.id} className={styles.subjectCard}><h2>{subject.code}</h2><p className={styles.subjectName}>{subject.name}</p><div className={styles.statRow}>{([ ["DRAFT", "Nháp"], ["OPEN", "Đang mở"], ["ARCHIVED", "Lưu trữ"] ] as const).map(([status, label]) => <div key={status}><small>{label}</small><strong>{rows.filter((row) => row.status === status).length}</strong></div>)}</div><SlotOutlet slot={subjectSummarySections} props={{subjectId:subject.id,variant:"inline" as const}}/><button className={styles.textButton} style={{ marginTop: 18, alignSelf: "flex-start" }} onClick={() => onSubject(subject.id)}>Mở môn<Icon name="chevron"/></button></article>; })}</div>
    {!subjects.length && <section className={styles.panel}><EmptyState title="Bạn chưa được giao quản lý môn"/></section>}
  </>;
}
/** Thông tin môn của Chủ nhiệm môn: chọn môn gọn trên tiêu đề, dải số lớp, rồi lưới ô số liệu do các unit gắn vào. */
export function MySubjectPage({ subject, subjects, classes, onSwitch, onClasses }: { subject: SubjectView; subjects?: SubjectView[]; classes: CourseClassView[]; onSwitch?: (id: string) => void; onBack?: () => void; onClasses: () => void }) {
  const rows = classes.filter((course) => course.subject.id === subject.id);
  return <>
    <header className={styles.pageHeading}>
      <div className={styles.headingText}>
        {subjects && subjects.length > 1 && onSwitch
          ? <label className={styles.subjectSwitch}><select aria-label="Môn học" value={subject.id} onChange={(event) => onSwitch(event.target.value)}>
            {subjects.map((item) => <option key={item.id} value={item.id}>{item.code} · {item.name}</option>)}
          </select></label>
          : <span className={styles.subjectCode}>{subject.code}</span>}
        <h1>{subject.name}</h1>
        {subject.description && <p className={styles.lede}>{subject.description}</p>}
      </div>
      <div className={styles.actions}><button className={styles.primaryButton} onClick={onClasses}><Icon name="users"/>Lớp của môn</button></div>
    </header>
    <div className={styles.statRow}>{([ ["OPEN", "Lớp đang mở"], ["DRAFT", "Lớp nháp"], ["ARCHIVED", "Lớp lưu trữ"] ] as const).map(([status, label]) => <div key={status}><strong>{rows.filter((row) => row.status === status).length}</strong><small>{label}</small></div>)}</div>
    <div className={styles.bento}><SlotOutlet slot={subjectSummarySections} props={{subjectId:subject.id,variant:"card" as const}}/></div>
  </>;
}
