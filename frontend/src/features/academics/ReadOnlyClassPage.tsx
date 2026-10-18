"use client";

import { useEffect, useRef, useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { studentClassSections, teacherClassTabs } from "./slots";
import { StudentDetailDialog } from "./ClassDialogs";
import { EnrollmentPanel } from "./EnrollmentPanel";
import { EmptyState, PageHeading, StatusBadge, Tabs } from "./ui";
import type { CourseClassView, EnrollmentView } from "./types";
import styles from "./academics.module.css";

export function ReadOnlyClassPage({ audience, value, students = [], onBack, loadStudent }: { audience: "student" | "teacher"; value: CourseClassView; students?: EnrollmentView[]; loadStudent?:(classId:string,accountId:string,signal?:AbortSignal)=>Promise<EnrollmentView>; onBack: () => void }) {
  const [tab, setTab] = useState<"info" | "students" | "materials" | "evals" | "gradebook">("info");
  const [selected, setSelected] = useState<EnrollmentView | null>(null);
  const [detailError,setDetailError]=useState("");
  const [detailBusy,setDetailBusy]=useState(false);
  const detailRequest=useRef<AbortController|null>(null);
  useEffect(()=>()=>detailRequest.current?.abort(),[]);
  async function selectStudent(student:EnrollmentView){detailRequest.current?.abort();const request=new AbortController();detailRequest.current=request;setDetailError("");setDetailBusy(true);try{const detail=loadStudent?await loadStudent(value.id,student.accountId,request.signal):student;if(!request.signal.aborted)setSelected(detail);}catch{if(!request.signal.aborted)setDetailError("Không tải được thông tin sinh viên.");}finally{if(!request.signal.aborted)setDetailBusy(false);}}
  const activeStudents = students.filter((student) => student.status === "ACTIVE");
  if (audience === "student" && value.status !== "OPEN") return <><PageHeading title="Chi tiết lớp" onBack={onBack}/><section className={styles.panel}><EmptyState title="Lớp hiện không khả dụng" description="Quay lại danh sách lớp để xem các lớp đang học."/></section></>;
  const tabs = [{ key: "info" as const, label: "Thông tin lớp" }, { key: "students" as const, label: "Sinh viên", count: activeStudents.length },
    { key: "materials" as const, label: "Học liệu" }, { key: "evals" as const, label: "Đánh giá" }, { key: "gradebook" as const, label: "Sổ điểm" }];
  return <>
    <PageHeading title={<>{value.subject.name}<StatusBadge status={value.status}/></>} subtitle={`${audience === "student" ? "Lớp của tôi" : "Lớp được phân công"} › ${value.subject.code} · ${value.code}`} onBack={onBack}/>
    {audience === "teacher" && <Tabs items={tabs} value={tab} onChange={setTab} label="Nội dung lớp"/>}
    {tab === "info" && <section className={styles.panel}><h2>Thông tin lớp</h2><dl className={styles.readList}>
      <dt>Môn</dt><dd>{value.subject.code} · {value.subject.name}</dd><dt>Mã lớp</dt><dd className={styles.code}>{value.code}</dd><dt>Học kỳ</dt><dd>{value.term}</dd><dt>Giảng viên</dt><dd>{value.teacher ? `${value.teacher.name} · ${value.teacher.email}` : "Chưa phân công"}</dd><dt>Trạng thái</dt><dd><StatusBadge status={value.status}/></dd><dt>Mô tả</dt><dd>{value.description || "Chưa có mô tả."}</dd>
    </dl></section>}
    {audience === "teacher" && tab === "info" && <SlotOutlet slot={teacherClassTabs} props={{classId:value.id,tab:"info" as const}}/>}
    {audience === "student" && <SlotOutlet slot={studentClassSections} props={{ classId: value.id }}/>}
    {audience === "teacher" && tab === "students" && <><EnrollmentPanel students={activeStudents} onView={student=>void selectStudent(student)}/><SlotOutlet slot={teacherClassTabs} props={{ classId: value.id, tab: "students" as const }}/></>}
    {audience === "teacher" && (tab === "materials" || tab === "evals" || tab === "gradebook") && <SlotOutlet slot={teacherClassTabs} props={{ classId: value.id, tab }}/>}
    {detailError && <p role="alert">{detailError}</p>}{detailBusy && <p role="status">Đang tải sinh viên…</p>}
    {selected && <StudentDetailDialog classId={value.id} student={selected} onClose={() => setSelected(null)}/>}
  </>;
}
