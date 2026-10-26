"use client";

import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { classCardSections } from "./slots";
import { useState } from "react";
import { EmptyState, Icon, PageHeading, StatusBadge } from "./ui";
import type { ClassStatus, CourseClassView } from "./types";
import styles from "./academics.module.css";

export function ClassDashboardPage({ audience, classes, onOpen }: { audience: "student" | "teacher"; classes: CourseClassView[]; onOpen: (id: string) => void }) {
  const [status, setStatus] = useState<ClassStatus>("OPEN");
  const tabs: { status: ClassStatus; label: string }[] = audience === "student" ? [{ status: "OPEN", label: "Đang học" }, { status: "ARCHIVED", label: "Đã kết thúc" }] : [{ status: "DRAFT", label: "Sắp mở" }, { status: "OPEN", label: "Đang dạy" }, { status: "ARCHIVED", label: "Đã kết thúc" }];
  const visible = classes.filter((course) => course.status === status);
  return <>
    <PageHeading title={audience === "student" ? "Lớp của tôi" : "Lớp được phân công"}/>
    <div><div className={styles.segmented} role="group" aria-label="Lọc lớp">{tabs.map((tab) => <button key={tab.status} className={status === tab.status ? styles.selected : ""} aria-pressed={status === tab.status} onClick={() => setStatus(tab.status)}>{tab.label} · {classes.filter((course) => course.status === tab.status).length}</button>)}</div></div>
    <div className={styles.cardGrid}>{visible.map((course) => <article key={course.id} className={styles.classCard}>
      <div className={styles.cardTop}><span className={styles.subjectCode}>{course.subject.code} · <span>{course.code}</span></span>{course.status !== "OPEN" && <StatusBadge status={course.status}/>}</div>
      <h2>{course.subject.name}</h2>
      <div className={styles.cardMeta}>{course.teacher?.name ?? "Chưa phân công"}<span aria-hidden>·</span>{course.term}</div>
      {audience === "student" && <SlotOutlet slot={classCardSections} props={{classId:course.id}}/>}
      <div className={styles.cardBottom}><span className={`${styles.muted} ${styles.small}`}>{audience === "teacher" ? `${course.activeStudentCount} sinh viên` : course.name}</span>{audience === "student" && course.status === "ARCHIVED" ? null : <button className={`${styles.textButton} ${styles.cardLink}`} aria-label={`Xem lớp ${course.code}`} onClick={() => onOpen(course.id)} data-testid={`dashboard-open-${course.id}`}>Vào lớp<Icon name="chevron"/></button>}</div>
    </article>)}</div>{!visible.length && <section className={styles.panel}><EmptyState title="Chưa có lớp trong mục này"/></section>}
  </>;
}
