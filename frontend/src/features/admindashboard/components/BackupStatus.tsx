"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Alert, Button, TableFrame, Td, Th } from "@/shared/ui";
import { Pagination } from "@/shared/ui/Pagination";
import { listBackupHistory } from "../api";
import type { BackupHistoryPage, BackupRunItem, BackupSectionView } from "../model/types";
import { msg } from "@/i18n/msg";
import { formatNumber } from "@/i18n/number";

const BACKUP_STATE_LABELS: Record<BackupRunItem["state"], string> = {
  CREATED: msg("admindashboard.components.BackupStatus.pending"), RUNNING: msg("admindashboard.components.BackupStatus.running"), SUCCESS: msg("admindashboard.components.BackupStatus.succeeded"), FAILED: msg("admindashboard.components.BackupStatus.failed"),
};
// Định dạng thời điểm sao lưu theo giờ Việt Nam hoặc hiện dấu gạch nếu chưa có.
const formatDateTime = (value: string | null) => value
  ? new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", dateStyle: "short", timeStyle: "short" }).format(new Date(value))
  : "—";
// Hiện kích thước theo đơn vị byte, giữ dấu gạch cho giá trị chưa biết.
const formatBytes = (value: number | null) => value === null ? "—" : `${formatNumber(value)} B`;

/** Operational metadata is independent of the calendar's available history. */
export function BackupStatus({ backup }: { backup: BackupSectionView }) {
  const schedule = backup.schedule.data;
  const latest = backup.latest.data;
  return (
    <section aria-label={msg("admindashboard.components.BackupStatus.backupStatus")} className="space-y-3 border-t border-line pt-3 text-xs">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="font-semibold">{!schedule ? msg("admindashboard.components.BackupStatus.unavailable") : schedule.enabled ? msg("admindashboard.components.BackupStatus.backupEnabled") : msg("admindashboard.components.BackupStatus.backupNotEnabled")}</p>
        <Link href="/admin/settings" className="font-semibold text-primary-ink underline underline-offset-2">{msg("admindashboard.components.BackupStatus.backupSettings")}</Link>
      </div>
      {schedule?.enabled && (
        <p className="text-muted">{msg("admindashboard.components.BackupStatus.dailyAt")}{schedule.dailyAt ?? "—"} ({schedule.timeZone}{msg("admindashboard.components.BackupStatus.nextRun")}{formatDateTime(schedule.nextPlannedAt)}</p>
      )}
      <dl className="grid grid-cols-[auto_minmax(0,1fr)] gap-x-3 gap-y-1.5 [&_dd]:break-words [&_dd]:text-right">
        <dt>{msg("admindashboard.components.BackupStatus.lastRun")}</dt><dd>{backup.latest.availability === "UNAVAILABLE" ? msg("admindashboard.components.BackupStatus.unavailable") : latest ? BACKUP_STATE_LABELS[latest.state] : msg("admindashboard.components.BackupStatus.noBackupsYet")}</dd>
        {latest && <>
          <dt>{msg("admindashboard.components.BackupStatus.start")}</dt><dd>{formatDateTime(latest.startedAt)}</dd>
          <dt>{msg("admindashboard.components.BackupStatus.end")}</dt><dd>{formatDateTime(latest.completedAt)}</dd>
          <dt>{msg("admindashboard.components.BackupStatus.size")}</dt><dd>{formatBytes(latest.byteSize)}</dd>
        </>}
        <dt>{msg("admindashboard.components.BackupStatus.lastSuccess")}</dt>
        <dd>{backup.latestSuccessfulAt.availability === "UNAVAILABLE" ? msg("admindashboard.components.BackupStatus.unavailable") : backup.latestSuccessfulAt.data ? formatDateTime(backup.latestSuccessfulAt.data) : msg("admindashboard.components.BackupStatus.noSuccessfulBackupYet")}</dd>
      </dl>
      {latest?.safeError && <p className="break-words text-danger" role="status">{latest.safeError}</p>}
      <p className="text-muted">{msg("admindashboard.components.BackupStatus.failuresIn7Days")}{backup.failuresLast7Days.data?.count ?? msg("admindashboard.components.BackupStatus.unavailable")}</p>
      <BackupHistory />
    </section>
  );
}

// Chỉ tải từng trang lịch sử khi phần chi tiết được mở và cho thử lại khi lỗi.
function BackupHistory() {
  const [open, setOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState<{ data: BackupHistoryPage | null; error: boolean }>({ data: null, error: false });
  useEffect(() => {
    if (!open) return;
    const controller = new AbortController();
    queueMicrotask(async () => {
      if (controller.signal.aborted) return;
      setResult({ data: null, error: false });
      try {
        const data = await listBackupHistory(page, 20, controller.signal);
        if (!controller.signal.aborted) setResult({ data, error: false });
      } catch {
        if (!controller.signal.aborted) setResult({ data: null, error: true });
      }
    });
    return () => controller.abort();
  }, [open, page, attempt]);
  return <>
    <Button size="sm" variant="secondary" aria-expanded={open} aria-controls="backup-history" onClick={() => setOpen(!open)}>
      {open ? msg("admindashboard.components.BackupStatus.hideBackupHistory") : msg("admindashboard.components.BackupStatus.viewBackupHistory")}
    </Button>
    {open && <section id="backup-history" aria-label={msg("admindashboard.components.BackupStatus.backupHistory")} className="min-w-0 space-y-3">
      {result.error ? <Alert tone="danger" action={<Button size="sm" onClick={() => setAttempt(attempt + 1)}>{msg("admindashboard.components.BackupStatus.retry")}</Button>}>{msg("admindashboard.components.BackupStatus.couldNotLoadBackupHistory")}</Alert>
        : !result.data ? <p role="status">{msg("admindashboard.components.BackupStatus.loadingBackupHistory")}</p>
          : <>
            {result.data.items.length === 0 ? <p>{msg("admindashboard.components.BackupStatus.noBackupHistoryYet")}</p> : <TableFrame>
              <caption className="sr-only">{msg("admindashboard.components.BackupStatus.backupRuns")}</caption>
              <thead><tr><Th>{msg("admindashboard.components.BackupStatus.date")}</Th><Th>{msg("admindashboard.components.BackupStatus.status")}</Th><Th>{msg("admindashboard.components.BackupStatus.startEnd")}</Th><Th>{msg("admindashboard.components.BackupStatus.size")}</Th><Th>{msg("admindashboard.components.BackupStatus.error")}</Th></tr></thead>
              <tbody>{result.data.items.map((run) => <tr key={run.scheduledFor}>
                <Td>{run.scheduledFor}</Td><Td>{BACKUP_STATE_LABELS[run.state]}</Td>
                <Td>{formatDateTime(run.startedAt)} / {formatDateTime(run.completedAt)}</Td><Td>{formatBytes(run.byteSize)}</Td>
                <Td className="max-w-64 whitespace-normal break-words">{run.safeError ?? "—"}</Td>
              </tr>)}</tbody>
            </TableFrame>}
            {result.data.totalElements > 0 && <Pagination page={page} pageSize={20} total={result.data.totalElements} onPageChange={setPage} itemLabel={msg("admindashboard.components.BackupStatus.backupRuns2")} />}
          </>}
    </section>}
  </>;
}
