"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Alert, Button, TableFrame, Td, Th } from "@/shared/ui";
import { Pagination } from "@/shared/ui/Pagination";
import { listBackupHistory } from "../api";
import type { BackupHistoryPage, BackupRunItem, BackupSectionView } from "../model/types";
import { msg } from "@/i18n/msg";

const BACKUP_STATE_LABELS: Record<BackupRunItem["state"], string> = {
  CREATED: msg("admindashboard.components.BackupStatus.dangCho"), RUNNING: msg("admindashboard.components.BackupStatus.dangChay"), SUCCESS: msg("admindashboard.components.BackupStatus.thanhCong"), FAILED: msg("admindashboard.components.BackupStatus.thatBai"),
};
const formatDateTime = (value: string | null) => value
  ? new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", dateStyle: "short", timeStyle: "short" }).format(new Date(value))
  : "—";
const formatBytes = (value: number | null) => value === null ? "—" : `${new Intl.NumberFormat("vi-VN").format(value)} B`;

/** Operational metadata is independent of the calendar's available history. */
export function BackupStatus({ backup }: { backup: BackupSectionView }) {
  const schedule = backup.schedule.data;
  const latest = backup.latest.data;
  return (
    <section aria-label={msg("admindashboard.components.BackupStatus.trangThaiSaoLuu")} className="space-y-3 border-t border-line pt-3 text-xs">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="font-semibold">{!schedule ? msg("admindashboard.components.BackupStatus.khongKhaDung") : schedule.enabled ? msg("admindashboard.components.BackupStatus.saoLuuDaBat") : msg("admindashboard.components.BackupStatus.saoLuuChuaBat")}</p>
        <Link href="/admin/settings" className="font-semibold text-primary-ink underline underline-offset-2">{msg("admindashboard.components.BackupStatus.caiDatSaoLuu")}</Link>
      </div>
      {schedule?.enabled && (
        <p className="text-muted">{msg("admindashboard.components.BackupStatus.hangNgay")}{schedule.dailyAt ?? "—"} ({schedule.timeZone}{msg("admindashboard.components.BackupStatus.lanToi")}{formatDateTime(schedule.nextPlannedAt)}</p>
      )}
      <dl className="grid grid-cols-[auto_minmax(0,1fr)] gap-x-3 gap-y-1.5 [&_dd]:break-words [&_dd]:text-right">
        <dt>{msg("admindashboard.components.BackupStatus.lanGanNhat")}</dt><dd>{backup.latest.availability === "UNAVAILABLE" ? msg("admindashboard.components.BackupStatus.khongKhaDung") : latest ? BACKUP_STATE_LABELS[latest.state] : msg("admindashboard.components.BackupStatus.chuaCoLanSaoLuuNao")}</dd>
        {latest && <>
          <dt>{msg("admindashboard.components.BackupStatus.batDau")}</dt><dd>{formatDateTime(latest.startedAt)}</dd>
          <dt>{msg("admindashboard.components.BackupStatus.ketThuc")}</dt><dd>{formatDateTime(latest.completedAt)}</dd>
          <dt>{msg("admindashboard.components.BackupStatus.dungLuong")}</dt><dd>{formatBytes(latest.byteSize)}</dd>
        </>}
        <dt>{msg("admindashboard.components.BackupStatus.thanhCongGanNhat")}</dt>
        <dd>{backup.latestSuccessfulAt.availability === "UNAVAILABLE" ? msg("admindashboard.components.BackupStatus.khongKhaDung") : backup.latestSuccessfulAt.data ? formatDateTime(backup.latestSuccessfulAt.data) : msg("admindashboard.components.BackupStatus.chuaCoBanSaoLuuThanh")}</dd>
      </dl>
      {latest?.safeError && <p className="break-words text-danger" role="status">{latest.safeError}</p>}
      <p className="text-muted">{msg("admindashboard.components.BackupStatus.thatBaiTrong7Ngay")}{backup.failuresLast7Days.data?.count ?? msg("admindashboard.components.BackupStatus.khongKhaDung")}</p>
      <BackupHistory />
    </section>
  );
}

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
      {open ? msg("admindashboard.components.BackupStatus.anLichSuSaoLuu") : msg("admindashboard.components.BackupStatus.xemLichSuSaoLuu")}
    </Button>
    {open && <section id="backup-history" aria-label={msg("admindashboard.components.BackupStatus.lichSuSaoLuu")} className="min-w-0 space-y-3">
      {result.error ? <Alert tone="danger" action={<Button size="sm" onClick={() => setAttempt(attempt + 1)}>{msg("admindashboard.components.BackupStatus.thuLai")}</Button>}>{msg("admindashboard.components.BackupStatus.khongTaiDuocLichSuSao")}</Alert>
        : !result.data ? <p role="status">{msg("admindashboard.components.BackupStatus.dangTaiLichSuSaoLuu")}</p>
          : <>
            {result.data.items.length === 0 ? <p>{msg("admindashboard.components.BackupStatus.chuaCoLichSuSaoLuu")}</p> : <TableFrame>
              <caption className="sr-only">{msg("admindashboard.components.BackupStatus.cacLanSaoLuu")}</caption>
              <thead><tr><Th>{msg("admindashboard.components.BackupStatus.ngay")}</Th><Th>{msg("admindashboard.components.BackupStatus.trangThai")}</Th><Th>{msg("admindashboard.components.BackupStatus.batDauKetThuc")}</Th><Th>{msg("admindashboard.components.BackupStatus.dungLuong")}</Th><Th>{msg("admindashboard.components.BackupStatus.loi")}</Th></tr></thead>
              <tbody>{result.data.items.map((run) => <tr key={run.scheduledFor}>
                <Td>{run.scheduledFor}</Td><Td>{BACKUP_STATE_LABELS[run.state]}</Td>
                <Td>{formatDateTime(run.startedAt)} / {formatDateTime(run.completedAt)}</Td><Td>{formatBytes(run.byteSize)}</Td>
                <Td className="max-w-64 whitespace-normal break-words">{run.safeError ?? "—"}</Td>
              </tr>)}</tbody>
            </TableFrame>}
            {result.data.totalElements > 0 && <Pagination page={page} pageSize={20} total={result.data.totalElements} onPageChange={setPage} itemLabel={msg("admindashboard.components.BackupStatus.lanSaoLuu")} />}
          </>}
    </section>}
  </>;
}
