"use client";

import {useCallback, useEffect, useState} from "react";
import {ApiError} from "@/shared/api/client";
import {Alert} from "@/shared/ui/Alert";
import {Button} from "@/shared/ui/Button";
import {PageHeader} from "@/shared/ui/PageHeader";
import {getAdminDashboard} from "./api";
import {BackupHistoryDialog} from "./BackupHistoryDialog";
import {BackupSection} from "./BackupSection";
import {formatInstant} from "./BackupRunTable";
import {CountCard, formatCount} from "./CountCard";
import {StatusBreakdown} from "./StatusBreakdown";
import type {AdminDashboardView} from "./types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

export function AdminDashboardPage() {
  const [generation, setGeneration] = useState(0);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [result, setResult] = useState<{generation: number; data?: AdminDashboardView; error?: unknown} | null>(null);
  useEffect(() => {
    const controller = new AbortController();
    getAdminDashboard(controller.signal).then(data => {
      if (!controller.signal.aborted) setResult({generation, data});
    }, error => {
      if (!controller.signal.aborted) setResult({generation, error});
    });
    return () => controller.abort();
  }, [generation]);
  const onDenied = useCallback((error: ApiError) => {
    setHistoryOpen(false);
    setResult({generation, error});
  }, [generation]);
  const current = result?.generation === generation ? result : null;
  const data = current?.data;
  const denied = current?.error instanceof ApiError && [401, 403].includes(current.error.problem.status);
  function reload() { setHistoryOpen(false); setGeneration(value => value + 1); }

  return <div className="flex flex-col gap-7">
    <PageHeader title="Tổng quan" subtitle={data ? `Số liệu lúc ${formatInstant(data.calculatedAt)}` : undefined}
      actions={data && <Button icon="refresh" onClick={reload} data-testid="dashboard-refresh">Tải lại</Button>}/>
    {!current && <PageLoading/>}
    {current?.error !== undefined && (denied
      ? <Alert tone="danger" role="alert">Không có quyền xem tổng quan hệ thống.</Alert>
      : <PageLoadError onRetry={reload}>Không tải được tổng quan hệ thống.</PageLoadError>)}
    {data && <>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <CountCard title="Tài khoản" href="/admin/accounts" value={data.accounts.data?.total ?? null} detail={data.accounts.data ? `${formatCount(data.accounts.data.activeTotal)} đang hoạt động` : undefined} testId="dashboard-accounts"/>
        <CountCard title="Môn học" href="/admin/subjects" value={data.subjects.data ? data.subjects.data.ACTIVE + data.subjects.data.ARCHIVED : null}
          detail={data.subjects.data ? `${formatCount(data.subjects.data.ACTIVE)} hoạt động · ${formatCount(data.subjects.data.ARCHIVED)} lưu trữ` : undefined} testId="dashboard-subjects"/>
        <CountCard title="Lớp học" value={data.classes.data ? data.classes.data.DRAFT + data.classes.data.OPEN + data.classes.data.ARCHIVED : null}
          detail={data.classes.data ? `${formatCount(data.classes.data.OPEN)} đang mở · ${formatCount(data.classes.data.DRAFT)} nháp · ${formatCount(data.classes.data.ARCHIVED)} lưu trữ` : undefined} testId="dashboard-classes"/>
        <CountCard title="Ghi danh đang hoạt động" value={data.activeEnrollments.data}
          detail={data.classes.data ? `Lớp đang mở: ${formatCount(data.classes.data.OPEN)}` : undefined} testId="dashboard-enrollments"/>
      </div>
      <BackupSection value={data.backup} onHistory={() => setHistoryOpen(true)}/>
      <StatusBreakdown value={data}/>
    </>}
    {data && historyOpen && <BackupHistoryDialog onClose={() => setHistoryOpen(false)} onDenied={onDenied}/>}
  </div>;
}
