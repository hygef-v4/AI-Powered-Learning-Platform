"use client";

import {useEffect, useState} from "react";
import {ApiError} from "@/shared/api/client";
import {Alert} from "@/shared/ui/Alert";
import {Button} from "@/shared/ui/Button";
import {Dialog} from "@/shared/ui/Dialog";
import {Pagination} from "@/shared/ui/Pagination";
import {getBackupHistory} from "./api";
import {BackupRunTable} from "./BackupRunTable";
import type {BackupHistoryPage} from "./types";

export function BackupHistoryDialog({onClose, onDenied}: {onClose: () => void; onDenied?: (error: ApiError) => void}) {
  const [page, setPage] = useState(0);
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState<{page: number; attempt: number; data?: BackupHistoryPage; error?: unknown} | null>(null);
  useEffect(() => {
    const controller = new AbortController();
    getBackupHistory(page, 20, controller.signal).then(data => {
      if (!controller.signal.aborted) setResult({page, attempt, data});
    }, error => {
      if (controller.signal.aborted) return;
      setResult({page, attempt, error});
      if (error instanceof ApiError && [401, 403].includes(error.problem.status)) onDenied?.(error);
    });
    return () => controller.abort();
  }, [page, attempt, onDenied]);
  const current = result?.page === page && result.attempt === attempt ? result : null;
  const denied = current?.error instanceof ApiError && [401, 403].includes(current.error.problem.status);
  return <Dialog open title="Lịch sử backup" size="lg" onClose={onClose}>
    <p className="text-xs text-muted">Mỗi lần chạy theo lịch hằng ngày · giữ bản thành công 7 ngày và ít nhất bản thành công gần nhất.</p>
    {!current && <p role="status">Đang tải lịch sử…</p>}
    {current?.error !== undefined && <Alert tone="danger" role="alert" action={!denied && <Button onClick={() => setAttempt(value => value + 1)}>Thử lại</Button>}>
      {denied ? "Không có quyền xem lịch sử backup." : "Không tải được lịch sử backup."}
    </Alert>}
    {current?.data && <>
      {current.data.items.length === 0 ? <p>Chưa có lần backup nào.</p> : <BackupRunTable runs={current.data.items}/>}
      <Pagination page={page} pageSize={20} total={current.data.totalElements} onPageChange={setPage} testId="backup-history-pagination"/>
    </>}
  </Dialog>;
}
