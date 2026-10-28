"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import type { DocumentPart } from "@/shared/document";
import { Section } from "@/shared/ui";
import { workApi } from "./api";
import { formatPoints, resultByPart, useAttemptResult } from "./attemptResult";

/**
 * Thẻ Kết quả trên Assignment Detail: chỉ điểm tổng và điểm từng bài (nhánh con trực tiếp của gốc tài liệu); nhận xét chi
 * tiết xem ở Lịch sử nộp bài.
 */
export function ResultSummaryCard({ attemptId, historyHref }: { attemptId: string; historyHref: string }) {
  const { result, failed, loading } = useAttemptResult(attemptId);
  const [parts, setParts] = useState<{ attemptId: string; parts: DocumentPart[] } | null>(null);
  useEffect(() => {
    const controller = new AbortController();
    workApi.attempt(attemptId, controller.signal).then(
      (attempt) => { if (!controller.signal.aborted) setParts({ attemptId, parts: attempt.parts }); },
      () => {},
    );
    return () => controller.abort();
  }, [attemptId]);
  const groups = result ? resultByPart(result.items, parts?.attemptId === attemptId ? parts.parts : undefined) : [];

  return (
    <Section title="Kết quả" actions={<Link href={historyHref} className="text-[13px] font-semibold text-primary-ink hover:underline">Xem chi tiết</Link>}>
      {failed ? <p role="alert" className="text-[13px] text-danger">Không tải được kết quả.</p>
        : loading ? <p role="status" className="text-[13px] text-muted">Đang tải kết quả…</p>
        : result?.score == null ? <p className="text-[13px] text-muted">Chưa có điểm công bố.</p>
        : (
          <div className="flex flex-col gap-4 sm:flex-row sm:items-stretch">
            <div className="flex shrink-0 flex-col justify-center rounded-xl bg-shell px-6 py-4 text-center">
              <span className="text-xs text-muted">Tổng điểm</span>
              <span className="mt-1 font-display text-[34px] leading-none tracking-[-0.02em] text-ink tabular-nums">{formatPoints(result.score)}</span>
              <span className="mt-1 text-xs text-muted tabular-nums">/ {formatPoints(result.maxScore)}</span>
            </div>
            {groups.length > 0 && (
              <ul className="flex min-w-0 flex-1 flex-col">
                {groups.map((group) => (
                  <li key={group.key} className="flex items-center justify-between gap-4 border-b border-line py-2.5 last:border-b-0">
                    <span className="min-w-0 truncate text-[13.5px] text-ink">{group.title}</span>
                    <span className="shrink-0 text-[13.5px] tabular-nums">
                      <span className="font-semibold text-ink">{formatPoints(group.awarded)}</span>
                      <span className="text-muted"> / {formatPoints(group.max)}</span>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
    </Section>
  );
}
