"use client";
import { useCallback } from "react";
import { PublishedFeedback, PublishedRubricItems } from "@/features/assignmentwork/components/PublishedFeedback";
import { formatPoints } from "@/features/assignmentwork/model/attemptResult";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";
import { Button, Section } from "@/shared/ui";
import { CONTRIBUTION_LEVELS } from "../model/contributionLevels";

type Props = { classId: string; assignmentId: string };

/**
 * Thẻ Kết quả bài nhóm trên Assignment Detail, cùng dáng thẻ Kết quả bài cá nhân: tổng điểm, điểm từng mục (theo
 * `partTitles`, thứ tự mục), rồi nhận xét đã duyệt và phân bố điểm. Chưa công bố điểm thì không hiện gì.
 */
export function StudentPublishedFeedback({ classId, assignmentId, partTitles }: Props & { partTitles?: Readonly<Record<string, string>> }) {
  const load = useCallback(() => gradingApi.listMyGrades(classId), [classId]);
  const { value, error, refresh } = useResource(load);
  const grade = value?.find(g => g.assignmentId === assignmentId);
  if (error) return <p role="alert">{error} <Button size="sm" onClick={() => void refresh().catch(() => {})}>Thử lại</Button></p>;
  if (!grade || grade.score == null) return null;
  const items = (grade.rubricItems ?? []).map(item => ({ ...item, awarded: Number(item.awardedPoints), max: Number(item.maxPoints) }));
  const order = Object.keys(partTitles ?? {});
  const parts = new Map<string, { title: string; awarded: number; max: number }>();
  for (const item of items) {
    const part = parts.get(item.partId) ?? { title: partTitles?.[item.partId] ?? item.criterionTitle, awarded: 0, max: 0 };
    part.awarded += item.awarded;
    part.max += item.max;
    parts.set(item.partId, part);
  }
  const rank = (id: string) => (order.indexOf(id) < 0 ? Infinity : order.indexOf(id));
  const rows = [...parts.entries()].sort(([a], [b]) => rank(a) - rank(b));
  const contribution = CONTRIBUTION_LEVELS.find(level => level.value === grade.contributionLevel);
  return <Section title="Kết quả">
    <div className="flex flex-col gap-4 sm:flex-row sm:items-stretch">
      <div className="flex shrink-0 flex-col justify-center rounded-xl bg-shell px-6 py-4 text-center">
        <span className="text-xs text-muted">Tổng điểm</span>
        <span className="mt-1 font-display text-[34px] leading-none tracking-[-0.02em] text-ink tabular-nums">{formatPoints(grade.score)}</span>
        <span className="mt-1 text-xs text-muted tabular-nums">/ {formatPoints(grade.maxScore)}</span>
      </div>
      {rows.length > 0 && <ul className="flex min-w-0 flex-1 flex-col">
        {rows.map(([id, part]) => <li key={id} className="flex items-center justify-between gap-4 border-b border-line py-2.5 last:border-b-0">
          <span className="min-w-0 truncate text-[13.5px] text-ink">{part.title}</span>
          <span className="shrink-0 text-[13.5px] tabular-nums">
            <span className="font-semibold text-ink">{formatPoints(part.awarded)}</span>
            <span className="text-muted"> / {formatPoints(part.max)}</span>
          </span>
        </li>)}
      </ul>}
    </div>
    {grade.memberOverride && <p className="text-xs text-muted">Điểm cá nhân đã được giảng viên điều chỉnh riêng.</p>}
    {(contribution || grade.contributionReason) && <div className="space-y-1 text-sm">
      {contribution && <p><strong>Mức đóng góp: {contribution.label}</strong></p>}
      {grade.contributionReason && <p className="whitespace-pre-wrap wrap-break-word">{grade.contributionReason}</p>}
    </div>}
    <PublishedFeedback feedback={grade.feedback} summary={grade.structuredSummary ?? null} />
    {items.length > 0 && <PublishedRubricItems items={items} />}
    <StudentGradeDistribution classId={classId} assignmentId={assignmentId} />
  </Section>;
}

export function StudentGradeDistribution({ classId, assignmentId }: Props) {
  const load = useCallback(() => gradingApi.loadMyGradeDistribution(classId, assignmentId), [classId, assignmentId]);
  const { value, error, refresh } = useResource(load);
  if (error) return <p role="alert">Không tải được phân bố điểm. <Button size="sm" onClick={() => void refresh().catch(() => {})}>Thử lại</Button></p>;
  if (!value?.length) return null;
  return <section aria-label="Phân bố điểm" className="space-y-2 text-sm">
    <h3 className="font-semibold text-ink">Phân bố điểm của lớp</h3>
    <ul className="flex flex-wrap gap-x-5 gap-y-2 text-muted">
      {value.map(bucket => <li key={bucket.label}>{bucket.label}: <strong className="text-ink">{bucket.count}</strong></li>)}
    </ul>
  </section>;
}
