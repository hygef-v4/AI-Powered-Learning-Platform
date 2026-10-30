"use client";

import type {CandidateLoader} from "../hooks/useCandidates";
import { useSearchParams } from "next/navigation";
import { useState } from "react";
import { Button, PageHeader, Pill, Section, showToast, Switch, Tabs, type TabItem } from "@/shared/ui";
import { initials } from "@/shared/layout/navigation";
import { ClassInfoForm } from "../components/ClassInfoForm";
import { AssignTeacherDialog, ClassStateActions } from "../components/ClassDialogs";
import { EnrollmentPanel } from "../components/EnrollmentPanel";
import { formatDate, operationError } from "../components/ui";
import { statusLabels, type ClassInfoInput, type ClassStatus, type CourseClassView, type EnrollmentRowResult, type EnrollmentView, type PersonView } from "../model/types";

export type ManagedClassActions = {
  onSaveInfo: (input: ClassInfoInput) => Promise<void>;
  onAssign: (teacherId: string | null, version: number) => Promise<void>;
  onChangeStatus: (status: ClassStatus, version: number) => Promise<void>;
  onDistribution: (enabled: boolean, version: number) => Promise<void>;
  onEnrollmentStatus: (accountId: string, status: "ACTIVE" | "REMOVED") => Promise<void>;
  onEnroll: (accountId: string) => Promise<void>;
  onBatch: (input: string) => Promise<EnrollmentRowResult[]>;
};
type Tab = "info" | "students";
const TONES: Record<ClassStatus, "success" | "warning" | "danger"> = { OPEN: "success", DRAFT: "warning", ARCHIVED: "danger" };

/**
 * Chi tiết lớp của Chủ nhiệm môn, cùng bố cục Chi tiết môn: tiêu đề là tên lớp, tab Thông tin (thẻ sửa ngay bên trái; cột
 * phải có Trạng thái, Giảng viên, Phân bố điểm) và tab Sinh viên.
 */
export function ManagedClassPage({ value, students, teachers, candidates, actions, loadTeachers, loadCandidates }: {
  loadTeachers?:CandidateLoader; loadCandidates?:CandidateLoader; value: CourseClassView; students: EnrollmentView[]; teachers: PersonView[]; candidates: PersonView[]; onBack: () => void; actions: ManagedClassActions;
}) {
  // Tab theo `?tab=` để sidebar của lớp mở đúng tab; bấm tab trên trang (màn hẹp) thì ghi lại vào URL.
  const params = useSearchParams();
  const urlTab: Tab = params?.get("tab") === "students" ? "students" : "info";
  const [tab, setTab] = useState<Tab>(urlTab);
  const [seenUrlTab, setSeenUrlTab] = useState(urlTab);
  if (urlTab !== seenUrlTab) {
    setSeenUrlTab(urlTab);
    setTab(urlTab);
  }
  const openTab = (next: Tab) => {
    setTab(next);
    const url = new URL(window.location.href);
    url.searchParams.set("tab", next);
    window.history.replaceState(window.history.state, "", url);
  };
  const [assigning, setAssigning] = useState(false);
  const [tabActions, setTabActions] = useState<HTMLElement | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const archived = value.status === "ARCHIVED";
  async function setDistribution(enabled: boolean) {
    if (busy || archived) return;
    setBusy(true); setError("");
    try { await actions.onDistribution(enabled, value.version); if (enabled) showToast("Đã bật phân bố điểm"); else showToast("Đã tắt phân bố điểm", "danger"); }
    catch (cause) { setError(operationError(cause)); } finally { setBusy(false); }
  }
  const tabs: TabItem<Tab>[] = [
    { key: "info", label: "Thông tin" },
    { key: "students", label: "Sinh viên", count: students.filter((student) => student.status === "ACTIVE").length },
  ];
  return <>
    <PageHeader backHref="/manager/classes" title={<span className="min-w-0 break-words [overflow-wrap:anywhere]">{value.name}</span>}
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
            <div className="flex items-center justify-between"><dt className="text-muted">Trạng thái</dt><dd><Pill tone={TONES[value.status]}>{statusLabels[value.status]}</Pill></dd></div>
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
        <Section title="Phân bố điểm">
          <Switch checked={value.showGradeDistribution} ariaLabel="Cho sinh viên xem phân bố điểm" label="Cho sinh viên xem" disabled={archived || busy}
            onChange={(next) => void setDistribution(next)} />
          {error && <p role="alert" className="text-sm text-danger">{error}</p>}
        </Section>
      </div>
    </div> : <div role="tabpanel" aria-label="Sinh viên">
      <EnrollmentPanel loadCandidates={loadCandidates} students={students} editable={!archived} includeRemoved candidates={candidates} onStatusChange={actions.onEnrollmentStatus} onEnroll={actions.onEnroll} onBatch={actions.onBatch} actionsHost={tabActions}/>
    </div>}
    {assigning && !archived && <AssignTeacherDialog loadTeachers={loadTeachers} value={value} teachers={teachers} onAssign={actions.onAssign} onClose={() => setAssigning(false)}/>}
  </>;
}
