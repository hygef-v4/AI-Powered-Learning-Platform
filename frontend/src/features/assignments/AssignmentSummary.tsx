"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { assignmentsApi } from "./api";
import type { AssignmentStatus } from "./types";

/** Số bài tập của môn (UC 34): dòng trên thẻ môn (Manager Dashboard) hoặc thẻ trên My Subject; gắn qua SubjectSummarySection. */
export function AssignmentSummary({ subjectId, variant }: { subjectId: string; variant: "inline" | "card" }) {
  const [counts, setCounts] = useState<Partial<Record<AssignmentStatus, number>> | null>(null);
  useEffect(() => {
    const controller = new AbortController();
    assignmentsApi.listForSubject(subjectId, { includeClosed: true }, controller.signal).then(
      (page) => { if (!controller.signal.aborted) setCounts(page.counts); },
      () => { if (!controller.signal.aborted) setCounts(null); },
    );
    return () => controller.abort();
  }, [subjectId]);
  if (!counts) return null;
  const open = counts.OPEN ?? 0;
  const drafts = (counts.DRAFT ?? 0) + (counts.REVIEWED ?? 0);
  const total = Object.values(counts).reduce((sum, value) => sum + (value ?? 0), 0);
  if (variant === "inline") {
    return <p className="text-xs text-muted" data-testid="assignment-summary">Bài tập: {open} đang mở · {drafts} nháp</p>;
  }
  return (
    <section className="flex flex-col gap-1 rounded-2xl border border-line bg-white p-5" data-testid="assignment-summary">
      <h2 className="text-[15px] font-bold text-ink">Bài tập của môn</h2>
      <p className="text-2xl font-bold text-ink">{total}</p>
      <p className="text-xs text-muted">{open} đang mở · {drafts} nháp</p>
      <Link href={`/manager/assignments?subjectId=${encodeURIComponent(subjectId)}`} className="mt-1 text-xs font-semibold text-primary">Xem bài tập của môn</Link>
    </section>
  );
}
