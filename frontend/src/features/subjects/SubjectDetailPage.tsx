"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Button, PageHeader, Pill, Section } from "@/shared/ui";
import { getSubject, getSubjectClasses } from "./api";
import { ManagerCard } from "./ManagerCard";
import { ManagerChangeDialog } from "./ManagerChangeDialog";
import type { SearchSubjectManagers } from "./managerSearch";
import { SubjectInfoTab } from "./SubjectInfoTab";
import { SubjectStatusActions, SubjectStatusButton } from "./SubjectStatusActions";
import type { SubjectDetail } from "./types";
import { SubjectClassesTab } from "./SubjectClassesTab";
import { SubjectMetaCard } from "./SubjectMetaCard";
import { isSubjectAccessError, SubjectReadError } from "./SubjectReadError";
import { useSubjectRead } from "./useSubjectRead";

type Props = { subjectId: string; searchManagers?: SearchSubjectManagers };
export function SubjectDetailPage(props: Props) {
  // Đổi ID phải bỏ draft/callback/loading cũ của cả cây detail, không chỉ request đọc.
  return <SubjectDetailContent key={props.subjectId} {...props} />;
}

function SubjectDetailContent({ subjectId, searchManagers }: Props) {
  const [tab, setTab] = useState<"information" | "classes">("information");
  const [mode, setMode] = useState<"edit" | "manager" | "status" | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [mutationAccessError, setMutationAccessError] = useState<unknown>(null);
  const focusTarget = useRef<string | null>(null);
  const load = useCallback(() => getSubject(subjectId), [subjectId]);
  const subject = useSubjectRead(`subject:${subjectId}`, load);
  useEffect(() => {
    if (!subject.loading && mode === null && focusTarget.current) {
      document.getElementById(subject.error === null ? focusTarget.current : "subject-save-notice")?.focus();
      focusTarget.current = null;
    }
  }, [subject.loading, subject.error, mode]);
  const loadClasses = useCallback(() => getSubjectClasses(subjectId), [subjectId]);
  const classes = useSubjectRead(`classes:${subjectId}`, loadClasses, subject.data !== null && tab === "classes");
  const accessError = mutationAccessError ?? (isSubjectAccessError(classes.error) ? classes.error : null);
  function reload() {
    if (mode !== null) focusTarget.current = triggerId();
    setMode(null);
    setMutationAccessError(null);
    subject.reload();
    classes.reload();
  }
  function triggerId() { return mode === "edit" ? "subject-edit-open" : mode === "manager" ? "subject-manager-open" : "subject-status-open"; }
  function open(next: "edit" | "manager" | "status") { setNotice(null); setMode(next); }
  function close() { focusTarget.current = triggerId(); setMode(null); }
  function saved(snapshot: SubjectDetail) {
    focusTarget.current = triggerId();
    setNotice(`Đã lưu thay đổi môn ${snapshot.code} (version ${snapshot.version}).`);
    reload();
  }
  const noticeElement = notice && <p id="subject-save-notice" tabIndex={-1} role="status" className="text-sm text-body">{notice}</p>;
  if (subject.error !== null || accessError !== null) return <>
    <PageHeader title="Môn học" backHref="/admin/subjects" />
    {noticeElement}
    <SubjectReadError error={subject.error ?? accessError} onRetry={reload} />
  </>;
  if (!subject.data) return <><PageHeader title="Môn học" backHref="/admin/subjects" />{noticeElement}<p role="status" className="py-8 text-center text-sm text-muted">Đang tải môn học…</p></>;
  const data = subject.data;
  const mutationProps = { subject: data, onSaved: saved, onReload: reload, onAccessError: setMutationAccessError };
  return <>
    <PageHeader backHref="/admin/subjects" title={<><span className="min-w-0 break-words [overflow-wrap:anywhere]">{data.name}</span><Pill tone={data.status === "ACTIVE" ? "success" : "neutral"}>{data.status === "ACTIVE" ? "Hoạt động" : "Lưu trữ"}</Pill></>}
      breadcrumb={[{ label: "Môn học", href: "/admin/subjects" }, { label: data.code }]}
      actions={<SubjectStatusButton archived={data.status === "ARCHIVED"} disabled={mode !== null} onClick={() => open("status")} />} />
    {noticeElement}
    <div role="tablist" aria-label="Nội dung môn học" className="flex gap-2 border-b border-line pb-2">
      <button type="button" role="tab" disabled={mode !== null} id="subject-information-tab" aria-controls="subject-information-panel" aria-selected={tab === "information"} tabIndex={tab === "information" ? 0 : -1} onClick={() => setTab("information")}
        onKeyDown={(event) => { if (event.key === "ArrowRight" || event.key === "ArrowLeft") { event.preventDefault(); setTab("classes"); document.getElementById("subject-classes-tab")?.focus(); } }}
        data-testid="subjects-tab-information" className="rounded px-3 py-2 text-sm font-semibold aria-selected:bg-primary-soft aria-selected:text-primary">Thông tin</button>
      <button type="button" role="tab" disabled={mode !== null} id="subject-classes-tab" aria-controls="subject-classes-panel" aria-selected={tab === "classes"} tabIndex={tab === "classes" ? 0 : -1} onClick={() => setTab("classes")}
        onKeyDown={(event) => { if (event.key === "ArrowRight" || event.key === "ArrowLeft") { event.preventDefault(); setTab("information"); document.getElementById("subject-information-tab")?.focus(); } }}
        data-testid="subjects-tab-classes" className="rounded px-3 py-2 text-sm font-semibold aria-selected:bg-primary-soft aria-selected:text-primary">Lớp học ({data.classCount ?? "—"})</button>
    </div>
    {tab === "information" ? <div role="tabpanel" id="subject-information-panel" aria-labelledby="subject-information-tab" className="grid items-start gap-4 lg:grid-cols-[minmax(0,1fr)_300px]">
      <SubjectInfoTab {...mutationProps} editing={mode === "edit"} disabled={mode !== null} onEdit={() => open("edit")} onCancel={close} />
      <div className="flex flex-col gap-4"><ManagerCard subject={data} disabled={mode !== null} onChange={data.status === "ARCHIVED" ? undefined : () => open("manager")} /><SubjectMetaCard subject={data} /></div>
    </div> : <div role="tabpanel" id="subject-classes-panel" aria-labelledby="subject-classes-tab" className="flex flex-col gap-3">
      {classes.loading && <p role="status" className="py-8 text-center text-sm text-muted">Đang tải lớp học…</p>}
      {classes.error !== null && <SubjectReadError error={classes.error} onRetry={classes.reload} />}
      {classes.data?.availability === "UNAVAILABLE" && <Section title="Danh sách lớp"><p className="text-sm text-muted">Dữ liệu lớp hiện không khả dụng.</p><Button onClick={classes.reload} data-testid="subjects-classes-retry">Thử lại</Button></Section>}
      {classes.data?.availability === "AVAILABLE" && <SubjectClassesTab items={classes.data.items} />}
    </div>}
    {mode === "manager" && <ManagerChangeDialog {...mutationProps} searchManagers={searchManagers} onClose={close} />}
    {mode === "status" && <SubjectStatusActions {...mutationProps} onClose={close} />}
  </>;
}
