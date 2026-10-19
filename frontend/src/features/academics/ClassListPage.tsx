"use client";

import { useState } from "react";
import { EmptyState, Icon, PageHeading, StatusBadge } from "./ui";
import { statusLabels, type ClassStatus, type CourseClassView, type SubjectView } from "./types";
import styles from "./academics.module.css";

export function ClassListPage({ classes, subjects, initialSubjectId, onSubjectChange, onOpen, onCreate }: { classes: CourseClassView[]; subjects: SubjectView[]; initialSubjectId?: string; onSubjectChange?: (id: string) => void; onOpen: (id: string) => void; onCreate: (subject: SubjectView) => void }) {
  const [subjectId, setSubjectId] = useState(initialSubjectId ?? subjects[0]?.id ?? "");
  const [status, setStatus] = useState<ClassStatus | "ALL">("ALL");
  const [term, setTerm] = useState("");
  const [query, setQuery] = useState("");
  const [page,setPage]=useState(1);
  const selected = subjects.find((subject) => subject.id === subjectId);
  const scoped = classes.filter((course) => course.subject.id === subjectId);
  const filtered = scoped.filter((course) => (status === "ALL" || course.status === status) && (!term || course.term === term) &&
    `${course.code} ${course.name} ${course.teacher?.name ?? ""}`.toLocaleLowerCase("vi").includes(query.toLocaleLowerCase("vi").trim()));
  return <>
    <PageHeading title="Lớp của môn"><select aria-label="Môn học" className={styles.select} value={subjectId} onChange={(event) => { setSubjectId(event.target.value); setTerm(""); onSubjectChange?.(event.target.value); }} data-testid="class-subject-filter">{subjects.map((subject) => <option key={subject.id} value={subject.id}>{subject.code} · {subject.name}</option>)}</select><button className={styles.primaryButton} disabled={!selected} onClick={() => selected && onCreate(selected)} data-testid="class-create"><Icon name="plus"/>Tạo lớp</button></PageHeading>
    <section className={styles.panel} aria-label="Danh sách lớp">
      <div className={styles.toolbar}><div className={styles.segmented} aria-label="Lọc trạng thái">{(["ALL", "DRAFT", "OPEN", "ARCHIVED"] as const).map((key) => <button key={key} className={status === key ? styles.selected : ""} aria-pressed={status === key} onClick={() => setStatus(key)} data-testid={`class-filter-${key}`}>{key === "ALL" ? "Tất cả" : `${statusLabels[key]} · ${scoped.filter((course) => course.status === key).length}`}</button>)}</div><div className={styles.filters}>
        <select aria-label="Học kỳ" className={styles.select} value={term} onChange={(event) => setTerm(event.target.value)} data-testid="class-term-filter"><option value="">Học kỳ: Tất cả</option>{[...new Set(scoped.map((course) => course.term))].sort().map((value) => <option key={value}>{value}</option>)}</select>
        <label className={styles.search}><Icon name="search"/><input type="search" aria-label="Tìm mã lớp, tên, giảng viên" placeholder="Tìm mã lớp, tên, giảng viên" value={query} onChange={(event) => {setQuery(event.target.value);setPage(1);}} data-testid="class-search"/></label>
      </div></div>
      <div className={styles.tableWrap}><table className={styles.table}><thead><tr><th scope="col">Mã lớp</th><th scope="col">Tên lớp</th><th scope="col">Học kỳ</th><th scope="col">Giảng viên</th><th scope="col">Sinh viên</th><th scope="col">Trạng thái</th><th scope="col"><span className="sr-only">Chi tiết</span></th></tr></thead><tbody>{filtered.slice((Math.min(page,Math.max(1,Math.ceil(filtered.length/20)))-1)*20,Math.min(page,Math.max(1,Math.ceil(filtered.length/20)))*20).map((course) => <tr key={course.id} onClick={() => onOpen(course.id)} style={{ cursor: "pointer" }}>
        <td className={styles.code}>{course.code}</td><td><button className={styles.rowLink} aria-label={`Xem lớp ${course.code}`} onClick={(event) => { event.stopPropagation(); onOpen(course.id); }} data-testid={`class-open-${course.id}`}>{course.name}</button></td><td>{course.term}</td><td>{course.teacher?.name ?? <em className={styles.muted}>Chưa phân công</em>}</td><td>{course.activeStudentCount}</td><td><StatusBadge status={course.status}/></td><td><Icon name="chevron"/></td>
      </tr>)}</tbody></table></div>{!filtered.length && <EmptyState title="Không tìm thấy lớp phù hợp" description="Thử đổi bộ lọc hoặc từ khóa tìm kiếm."/>}
{filtered.length>20 && <nav aria-label="Phân trang"><button disabled={page<=1} onClick={()=>setPage(v=>Math.max(1,v-1))}>Trang trước</button><span>Trang {Math.min(page,Math.ceil(filtered.length/20))} / {Math.ceil(filtered.length/20)}</span><button disabled={page>=Math.ceil(filtered.length/20)} onClick={()=>setPage(v=>v+1)}>Trang sau</button></nav>}
    </section>
  </>;
}
