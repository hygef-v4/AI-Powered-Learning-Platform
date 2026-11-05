"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Alert, Button, TableFrame, Td, Th } from "@/shared/ui";
import { Pagination } from "@/shared/ui/Pagination";
import { listBackupHistory } from "../api";
import type { BackupHistoryPage, BackupRunItem, BackupSectionView } from "../model/types";

const states: Record<BackupRunItem["state"], string> = {
  CREATED: "Đang chờ", RUNNING: "Đang chạy", SUCCESS: "Thành công", FAILED: "Thất bại",
};
const dateTime = (value: string | null) => value
  ? new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", dateStyle: "short", timeStyle: "short" }).format(new Date(value))
  : "—";
const bytes = (value: number | null) => value === null ? "—" : `${new Intl.NumberFormat("vi-VN").format(value)} B`;

/** Operational metadata is independent of the calendar's available history. */
export function BackupStatus({ backup }: { backup: BackupSectionView }) {
  const schedule = backup.schedule.data;
  const latest = backup.latest.data;
  return (
    <section aria-label="Trạng thái sao lưu" className="space-y-3 border-t border-line pt-3 text-xs">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="font-semibold">{!schedule ? "Không khả dụng" : schedule.enabled ? "Sao lưu đã bật" : "Sao lưu chưa bật"}</p>
        <Link href="/admin/settings" className="font-semibold text-primary-ink underline underline-offset-2">Cài đặt sao lưu</Link>
      </div>
      {schedule?.enabled && (
        <p className="text-muted">Hằng ngày {schedule.dailyAt ?? "—"} ({schedule.timeZone}) · Lần tới: {dateTime(schedule.nextPlannedAt)}</p>
      )}
      <dl className="grid grid-cols-[auto_minmax(0,1fr)] gap-x-3 gap-y-1.5 [&_dd]:break-words [&_dd]:text-right">
        <dt>Lần gần nhất</dt><dd>{backup.latest.availability === "UNAVAILABLE" ? "Không khả dụng" : latest ? states[latest.state] : "Chưa có lần sao lưu nào"}</dd>
        {latest && <>
          <dt>Bắt đầu</dt><dd>{dateTime(latest.startedAt)}</dd>
          <dt>Kết thúc</dt><dd>{dateTime(latest.completedAt)}</dd>
          <dt>Dung lượng</dt><dd>{bytes(latest.byteSize)}</dd>
        </>}
        <dt>Thành công gần nhất</dt>
        <dd>{backup.latestSuccessfulAt.availability === "UNAVAILABLE" ? "Không khả dụng" : backup.latestSuccessfulAt.data ? dateTime(backup.latestSuccessfulAt.data) : "Chưa có bản sao lưu thành công"}</dd>
      </dl>
      {latest?.safeError && <p className="break-words text-danger" role="status">{latest.safeError}</p>}
      <p className="text-muted">Thất bại trong 7 ngày: {backup.failuresLast7Days.data?.count ?? "Không khả dụng"}</p>
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
      {open ? "Ẩn lịch sử sao lưu" : "Xem lịch sử sao lưu"}
    </Button>
    {open && <section id="backup-history" aria-label="Lịch sử sao lưu" className="min-w-0 space-y-3">
      {result.error ? <Alert tone="danger" action={<Button size="sm" onClick={() => setAttempt(attempt + 1)}>Thử lại</Button>}>Không tải được lịch sử sao lưu.</Alert>
        : !result.data ? <p role="status">Đang tải lịch sử sao lưu…</p>
          : <>
            {result.data.items.length === 0 ? <p>Chưa có lịch sử sao lưu.</p> : <TableFrame>
              <caption className="sr-only">Các lần sao lưu</caption>
              <thead><tr><Th>Ngày</Th><Th>Trạng thái</Th><Th>Bắt đầu / kết thúc</Th><Th>Dung lượng</Th><Th>Lỗi</Th></tr></thead>
              <tbody>{result.data.items.map((run) => <tr key={run.scheduledFor}>
                <Td>{run.scheduledFor}</Td><Td>{states[run.state]}</Td>
                <Td>{dateTime(run.startedAt)} / {dateTime(run.completedAt)}</Td><Td>{bytes(run.byteSize)}</Td>
                <Td className="max-w-64 whitespace-normal break-words">{run.safeError ?? "—"}</Td>
              </tr>)}</tbody>
            </TableFrame>}
            {result.data.totalElements > 0 && <Pagination page={page} pageSize={20} total={result.data.totalElements} onPageChange={setPage} itemLabel="lần sao lưu" />}
          </>}
    </section>}
  </>;
}
