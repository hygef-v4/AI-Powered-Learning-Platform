"use client";

import { useEffect, useState } from "react";
import { ButtonLink, PageHeader, Pill, Section } from "@/shared/ui";
import { exportUrl, workApi } from "../api";
import { errorMessage, formatDateTime } from "../model/format";
import { submitModeLabels, type Receipt } from "../model/types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

/** Biên nhận sau khi nộp (BR-U08-22): mã lượt, lượt thứ mấy, thời điểm nộp, trễ hay không, mã băm. */
export function ReceiptPage({ attemptId }: { attemptId: string }) {
  const [receipt, setReceipt] = useState<Receipt | null>(null);
  const [error, setError] = useState<unknown>(null);
  useEffect(() => {
    const controller = new AbortController();
    workApi.receipt(attemptId, controller.signal).then((value) => { if (!controller.signal.aborted) setReceipt(value); },
      (e) => { if (!controller.signal.aborted) setError(e); });
    return () => controller.abort();
  }, [attemptId]);
  if (error) return <PageLoadError>{errorMessage(error)}</PageLoadError>;
  if (!receipt) return <PageLoading label="Đang tải biên nhận…" />;
  const history = `/my-assignments/${encodeURIComponent(receipt.assignmentId)}/history?classId=${encodeURIComponent(receipt.classId)}`;
  return (
    <div className="flex flex-col gap-4">
      <PageHeader title="Đã nộp bài" subtitle={`${receipt.title} · lượt ${receipt.attemptNo} / ${receipt.maxAttempts}`} />
      <Section title="Biên nhận" icon="receipt_long">
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-[13px]" data-testid="receipt">
          <dt className="text-muted">Mã biên nhận</dt><dd className="font-mono text-xs break-all">{receipt.receiptHash}</dd>
          <dt className="text-muted">Mã lượt</dt><dd className="font-mono text-xs">{receipt.attemptId}</dd>
          <dt className="text-muted">Thời gian nộp</dt><dd>{formatDateTime(receipt.submittedAt)} (giờ Việt Nam)</dd>
          <dt className="text-muted">Tình trạng</dt><dd>{receipt.late ? <Pill tone="warning">Trễ</Pill> : <Pill tone="success">Đúng hạn</Pill>} <span className="text-xs text-muted">{submitModeLabels[receipt.submitMode]}</span></dd>
          <dt className="text-muted">Nội dung</dt><dd>{receipt.blockCount} khối của bạn · {receipt.diagramCount} sơ đồ</dd>
        </dl>
        <div className="flex flex-wrap gap-2">
          <a href={exportUrl(receipt.attemptId)} className="inline-flex items-center gap-1.5 rounded-lg border border-line-strong bg-surface px-3.5 py-2 text-[13px] font-semibold text-ink hover:bg-shell">Tải DOCX</a>
          <ButtonLink href={history}>Xem lịch sử nộp bài</ButtonLink>
          <ButtonLink href="/my-assignments" variant="primary">Về danh sách bài tập</ButtonLink>
        </div>
      </Section>
    </div>
  );
}
