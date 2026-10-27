"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Button, PageHeader, Pill, Section, showToast, Tabs, type TabItem } from "@/shared/ui";
import { getSubject, getSubjectClasses } from "./api";
import { ManagerCard } from "./ManagerCard";
import { ManagerChangeDialog } from "./ManagerChangeDialog";
import type { SearchSubjectManagers } from "./managerSearch";
import { SubjectInfoTab } from "./SubjectInfoTab";
import { SubjectStatusActions } from "./SubjectStatusActions";
import type { SubjectDetail } from "./types";
import { SubjectClassesTab } from "./SubjectClassesTab";
import { SubjectMetaCard } from "./SubjectMetaCard";
import { isSubjectAccessError, SubjectReadError } from "./SubjectReadError";
import { useSubjectRead } from "./useSubjectRead";
import { PageLoading } from "@/shared/ui/PageStatus";

type Props = { subjectId: string; searchManagers?: SearchSubjectManagers };
type Tab = "information" | "classes";

export function SubjectDetailPage(props: Props) {
  // Đổi ID phải bỏ draft/callback/loading cũ của cả cây detail, không chỉ request đọc.
  return <SubjectDetailContent key={props.subjectId} {...props} />;
}

/**
 * Chi tiết môn học (UC 17–20), cùng bố cục Chi tiết tài khoản: thẻ thông tin sửa ngay bên trái (mã môn khóa), cột phải
 * có thẻ Trạng thái (nút lưu trữ / mở lại) và Chủ nhiệm môn; lưu xong báo nổi ở góc phải.
 */
function SubjectDetailContent({ subjectId, searchManagers }: Props) {
  const [tab, setTab] = useState<Tab>("information");
  const [mode, setMode] = useState<"manager" | "status" | null>(null);
  const [mutationAccessError, setMutationAccessError] = useState<unknown>(null);
  // Lưu xong sau khi đã rời môn này (đổi môn khi request còn chờ) thì không báo gì: thông báo nổi là toàn cục.
  const alive = useRef(true);
  useEffect(() => {
    alive.current = true;
    return () => { alive.current = false; };
  }, []);
  const load = useCallback(() => getSubject(subjectId), [subjectId]);
  const subject = useSubjectRead(`subject:${subjectId}`, load);
  const loadClasses = useCallback(() => getSubjectClasses(subjectId), [subjectId]);
  const classes = useSubjectRead(`classes:${subjectId}`, loadClasses, subject.data !== null && tab === "classes");
  const accessError = mutationAccessError ?? (isSubjectAccessError(classes.error) ? classes.error : null);
  function reload() {
    setMode(null);
    setMutationAccessError(null);
    subject.reload();
    classes.reload();
  }
  function saved(message: string, tone: "success" | "danger" = "success") {
    if (!alive.current) return;
    showToast(message, tone);
    reload();
  }
  if (subject.error !== null || accessError !== null) return <>
    <PageHeader title="Môn học" backHref="/admin/subjects" />
    <SubjectReadError error={subject.error ?? accessError} onRetry={reload} />
  </>;
  if (!subject.data) return <PageLoading label="Đang tải môn học…" />;
  const data = subject.data;
  const archived = data.status === "ARCHIVED";
  const base = { subject: data, onReload: reload, onAccessError: setMutationAccessError };
  const tabs: TabItem<Tab>[] = [
    { key: "information", label: "Thông tin" },
    { key: "classes", label: "Lớp học", count: data.classCount ?? undefined },
  ];
  return <>
    <PageHeader backHref="/admin/subjects" title={<><span className="min-w-0 break-words [overflow-wrap:anywhere]">{data.name}</span><Pill tone={archived ? "danger" : "success"}>{archived ? "Lưu trữ" : "Hoạt động"}</Pill></>}
      breadcrumb={[{ label: "Môn học", href: "/admin/subjects" }, { label: data.code }]} />
    <Tabs items={tabs} value={tab} onChange={setTab} label="Nội dung môn học" testIdPrefix="subjects-tab-" />
    {tab === "information" ? <div role="tabpanel" aria-label="Thông tin" className="grid items-start gap-4 lg:grid-cols-[1fr_280px]">
      <SubjectInfoTab key={data.version} {...base} onSaved={() => saved("Đã lưu thay đổi")} />
      <div className="flex flex-col gap-4">
        <SubjectMetaCard subject={data} disabled={mode !== null} onStatus={() => setMode("status")} />
        <ManagerCard subject={data} disabled={mode !== null} onChange={archived ? undefined : () => setMode("manager")} />
      </div>
    </div> : <div role="tabpanel" aria-label="Lớp học" className="flex flex-col gap-3">
      {classes.loading && <p role="status" className="py-8 text-center text-sm text-muted">Đang tải lớp học…</p>}
      {classes.error !== null && <SubjectReadError error={classes.error} onRetry={classes.reload} />}
      {classes.data?.availability === "UNAVAILABLE" && <Section title="Danh sách lớp"><p className="text-sm text-muted">Dữ liệu lớp hiện không khả dụng.</p><Button onClick={classes.reload} data-testid="subjects-classes-retry">Thử lại</Button></Section>}
      {classes.data?.availability === "AVAILABLE" && <SubjectClassesTab items={classes.data.items} />}
    </div>}
    {mode === "manager" && <ManagerChangeDialog {...base} searchManagers={searchManagers} onSaved={(next: SubjectDetail) => next.managerId === null ? saved("Đã bỏ chủ nhiệm môn", "danger") : saved("Đã đổi chủ nhiệm môn")} onClose={() => setMode(null)} />}
    {mode === "status" && <SubjectStatusActions {...base} onSaved={(next: SubjectDetail) => next.status === "ARCHIVED" ? saved("Đã lưu trữ môn học", "danger") : saved("Đã mở lại môn học")} onClose={() => setMode(null)} />}
  </>;
}
