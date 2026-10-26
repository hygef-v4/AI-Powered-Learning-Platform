"use client";

import { useEffect, useState } from "react";
import { StatTile } from "@/shared/ui/StatTile";
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
    <StatTile testId="assignment-summary" label="Bài tập" value={total} meta={`${open} đang mở · ${drafts} nháp`}
      href={`/manager/assignments?subjectId=${encodeURIComponent(subjectId)}`} />
  );
}
