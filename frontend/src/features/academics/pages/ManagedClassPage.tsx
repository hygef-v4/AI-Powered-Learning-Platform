"use client";

import type {CandidateLoader} from "../hooks/useCandidates";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { useState } from "react";
import { Button, PageHeader, Pill, Section, Tabs, type TabItem } from "@/shared/ui";
import { initials } from "@/shared/layout/navigation";
import { ClassInfoForm } from "../components/ClassInfoForm";
import { AssignTeacherDialog, ClassStateActions } from "../components/ClassDialogs";
import { EnrollmentPanel } from "../components/EnrollmentPanel";
import { formatDate } from "../model/format";
import { CLASS_STATUS_LABELS, type ClassInfoInput, type ClassStatus, type CourseClassView, type EnrollmentRowResult, type EnrollmentView, type PersonView } from "../model/types";

export type ManagedClassActions = {
  onSaveInfo: (input: ClassInfoInput) => Promise<void>;
  onAssign: (teacherId: string | null, version: number) => Promise<void>;
  onChangeStatus: (status: ClassStatus, version: number) => Promise<void>;
  onEnrollmentStatus: (accountId: string, status: "ACTIVE" | "REMOVED") => Promise<void>;
  onEnroll: (accountId: string) => Promise<void>;
  onBatch: (input: string) => Promise<EnrollmentRowResult[]>;
};
type Tab = "info" | "students";
const TONES: Record<ClassStatus, "success" | "warning" | "danger"> = { OPEN: "success", DRAFT: "warning", ARCHIVED: "danger" };

/**
 * Chi tiết lớp của Chủ nhiệm môn, cùng bố cục Chi tiết môn: tiêu đề là tên lớp, tab Thông tin (thẻ sửa ngay bên trái; cột
 * phải có Trạng thái, Giảng viên) và tab Sinh viên.
 */
export function ManagedClassPage({ value, students, teachers, candidates, actions, loadTeachers, loadCandidates }: {
  loadTeachers?:CandidateLoader; loadCandidates?:CandidateLoader; value: CourseClassView; students: EnrollmentView[]; teachers: PersonView[]; candidates: PersonView[]; onBack: () => void; actions: ManagedClassActions;
}) {
  const [tab, openTab] = useUrlState<Tab>("tab", "info", ["info", "students"]);
  const [assigning, setAssigning] = useState(false);
  const [tabActions, setTabActions] = useState<HTMLElement | null>(null);
  const archived = value.status === "ARCHIVED";
  const tabs: TabItem<Tab>[] = [
    { key: "info", label: "Thông tin" },
    { key: "students", label: "Sinh viên", count: students.filter((student) => student.status === "ACTIVE").length },
  ];
  return <>
    <PageHeader backHref="/manager/classes" title={<span className="min-w-0 break-words [overflow-wrap:anywhere]">{value.code}</span>}
      breadcrumb={[{ label: "Lớp học", href: "/manager/classes" }, { label: value.code }]} />
    {/* Nút thêm sinh viên của tab Sinh viên nằm cùng hàng tab, bên phải. */}
    <div className="relative">
      <Tabs items={tabs} value={tab} onChange={openTab} label="Chi tiết lớp" testIdPrefix="class-tab-" />
      <div ref={setTabActions} className="absolute right-0 bottom-1.5" />
    </div>
    {tab === "info" ? <div role="tabpanel" aria-label="Thông tin" className="grid items-start gap-4 lg:grid-cols-[1fr_280px]">
      <ClassInfoForm value={value} onSave={actions.onSaveInfo}/>
      <div className="flex flex-col gap-4">
        <Section title="Trạng thái">
          <dl className="flex flex-col gap-2.5 text-[13px]">
            <div className="flex items-center justify-between"><dt className="text-muted">Trạng thái</dt><dd><Pill tone={TONES[value.status]}>{CLASS_STATUS_LABELS[value.status]}</Pill></dd></div>
            <div className="flex items-center justify-between"><dt className="text-muted">Ngày tạo</dt><dd className="text-ink">{formatDate(value.createdAt)}</dd></div>
          </dl>
          <div className="flex flex-col gap-2"><ClassStateActions value={value} onChange={actions.onChangeStatus}/></div>
        </Section>
        <Section title="Giảng viên" actions={!archived && <Button size="sm" onClick={() => setAssigning(true)} data-testid="class-assign-teacher">{value.teacher ? "Đổi giảng viên" : "Phân công"}</Button>}>
          {value.teacher ? <div className="flex items-center gap-3">
            <span aria-hidden="true" className="flex size-10 shrink-0 items-center justify-center rounded-full bg-accent-soft font-semibold text-accent">{initials(value.teacher.name)}</span>
            <div className="min-w-0"><p className="text-sm font-semibold">{value.teacher.name}</p><p className="break-all text-xs text-muted">{value.teacher.email}</p></div>
          </div> : <div><Pill>Chưa phân công</Pill></div>}
        </Section>
      </div>
    </div> : <div role="tabpanel" aria-label="Sinh viên">
      <EnrollmentPanel loadCandidates={loadCandidates} students={students} editable={!archived} includeRemoved candidates={candidates} onStatusChange={actions.onEnrollmentStatus} onEnroll={actions.onEnroll} onBatch={actions.onBatch} actionsHost={tabActions}/>
    </div>}
    {assigning && !archived && <AssignTeacherDialog loadTeachers={loadTeachers} value={value} teachers={teachers} onAssign={actions.onAssign} onClose={() => setAssigning(false)}/>}
  </>;
}
