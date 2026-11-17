"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { Button, PageHeader, Section, showToast, tabId, Tabs, type TabItem } from "@/shared/ui";
import { loadSubject, listSubjectClasses } from "../api";
import { ManagerCard } from "../components/ManagerCard";
import { ManagerChangeDialog } from "../components/ManagerChangeDialog";
import type { SearchSubjectManagers } from "../model/managerSearch";
import { SubjectInfoTab } from "../components/SubjectInfoTab";
import { SubjectStatusActions } from "../components/SubjectStatusActions";
import type { SubjectDetail } from "../model/types";
import { SubjectClassesTab } from "../components/SubjectClassesTab";
import { SubjectMetaCard } from "../components/SubjectMetaCard";
import { isSubjectAccessError, SubjectReadError } from "../components/SubjectReadError";
import { useSubjectRead } from "../hooks/useSubjectRead";
import { PageLoading } from "@/shared/ui/PageStatus";
import { msg } from "@/i18n/msg";

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
  const [tab, setTab] = useUrlState<Tab>("tab", "information", ["information", "classes"]);
  const [mode, setMode] = useState<"manager" | "status" | null>(null);
  const [mutationAccessError, setMutationAccessError] = useState<unknown>(null);
  // Lưu xong sau khi đã rời môn này (đổi môn khi request còn chờ) thì không báo gì: thông báo nổi là toàn cục.
  const alive = useRef(true);
  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
    };
  }, []);
  const load = useCallback(() => loadSubject(subjectId), [subjectId]);
  const subject = useSubjectRead(`subject:${subjectId}`, load);
  const loadClasses = useCallback(() => listSubjectClasses(subjectId), [subjectId]);
  const classes = useSubjectRead(
    `classes:${subjectId}`,
    loadClasses,
    subject.data !== null && tab === "classes",
  );
  const accessError =
    mutationAccessError ?? (isSubjectAccessError(classes.error) ? classes.error : null);
  function handleReload() {
    setMode(null);
    setMutationAccessError(null);
    subject.reload();
    classes.reload();
  }
  function handleSaved(message: string, tone: "success" | "danger" = "success") {
    if (!alive.current) return;
    showToast(message, tone);
    handleReload();
  }
  if (subject.error !== null || accessError !== null)
    return (
      <>
        <PageHeader title={msg("subjects.pages.SubjectDetailPage.subjects")} backHref="/admin/subjects" />
        <SubjectReadError error={subject.error ?? accessError} onRetry={handleReload} />
      </>
    );
  if (!subject.data) return <PageLoading label={msg("subjects.pages.SubjectDetailPage.loadingSubjects")} />;
  const data = subject.data;
  const archived = data.status === "ARCHIVED";
  const base = { subject: data, onReload: handleReload, onAccessError: setMutationAccessError };
  const tabs: TabItem<Tab>[] = [
    { key: "information", label: msg("subjects.pages.SubjectDetailPage.information") },
    { key: "classes", label: msg("subjects.pages.SubjectDetailPage.classes"), count: data.classCount ?? undefined },
  ];
  return (
    <>
      <PageHeader
        backHref="/admin/subjects"
        title={<span className="min-w-0 break-words [overflow-wrap:anywhere]">{data.name}</span>}
        breadcrumb={[{ label: msg("subjects.pages.SubjectDetailPage.subjects"), href: "/admin/subjects" }, { label: data.code }]}
      />
      <Tabs
        items={tabs}
        value={tab}
        onChange={setTab}
        label={msg("subjects.pages.SubjectDetailPage.subjectContent")}
        testIdPrefix="subjects-tab-"
        panelId="subject-tab-panel"
      />
      {tab === "information" ? (
        <div
          role="tabpanel"
          id="subject-tab-panel"
          aria-labelledby={tabId("subject-tab-panel", tab)}
          className="grid items-start gap-4 lg:grid-cols-[1fr_280px]"
        >
          <SubjectInfoTab key={data.version} {...base} onSaved={() => handleSaved(msg("subjects.pages.SubjectDetailPage.changesSaved"))} />
          <div className="flex flex-col gap-4">
            <SubjectMetaCard
              subject={data}
              disabled={mode !== null}
              onStatus={() => setMode("status")}
            />
            <ManagerCard
              subject={data}
              disabled={mode !== null}
              onChange={archived ? undefined : () => setMode("manager")}
            />
          </div>
        </div>
      ) : (
        <div role="tabpanel" id="subject-tab-panel" aria-labelledby={tabId("subject-tab-panel", tab)} className="flex flex-col gap-3">
          {classes.loading && (
            <p role="status" className="py-8 text-center text-sm text-muted">
              {msg("subjects.pages.SubjectDetailPage.loadingClasses")}</p>
          )}
          {classes.error !== null && (
            <SubjectReadError error={classes.error} onRetry={classes.reload} />
          )}
          {classes.data?.availability === "UNAVAILABLE" && (
            <Section title={msg("subjects.pages.SubjectDetailPage.classList")}>
              <p className="text-sm text-muted">{msg("subjects.pages.SubjectDetailPage.classDataIsCurrentlyUnavailable")}</p>
              <Button onClick={classes.reload} data-testid="subjects-classes-retry">
                {msg("subjects.pages.SubjectDetailPage.retry")}</Button>
            </Section>
          )}
          {classes.data?.availability === "AVAILABLE" && (
            <SubjectClassesTab items={classes.data.items} />
          )}
        </div>
      )}
      {mode === "manager" && (
        <ManagerChangeDialog
          {...base}
          searchManagers={searchManagers}
          onSaved={(next: SubjectDetail) =>
            next.managerId === null
              ? handleSaved(msg("subjects.pages.SubjectDetailPage.subjectManagerRemoved"), "danger")
              : handleSaved(msg("subjects.pages.SubjectDetailPage.subjectManagerChanged"))
          }
          onClose={() => setMode(null)}
        />
      )}
      {mode === "status" && (
        <SubjectStatusActions
          {...base}
          onSaved={(next: SubjectDetail) =>
            next.status === "ARCHIVED"
              ? handleSaved(msg("subjects.pages.SubjectDetailPage.subjectArchived"), "danger")
              : handleSaved(msg("subjects.pages.SubjectDetailPage.subjectReopened"))
          }
          onClose={() => setMode(null)}
        />
      )}
    </>
  );
}
