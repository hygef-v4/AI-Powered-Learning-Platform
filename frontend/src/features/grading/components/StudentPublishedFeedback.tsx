"use client";
import { useCallback } from "react";
import { PublishedFeedback, PublishedRubricItems } from "@/features/assignmentwork/components/PublishedFeedback";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";
import { Button } from "@/shared/ui";

type Props = { classId: string; assignmentId: string };

export function StudentPublishedFeedback({ classId, assignmentId }: Props) {
  const load = useCallback(() => gradingApi.listMyGrades(classId), [classId]);
  const { value, error, refresh } = useResource(load);
  const grade = value?.find(g => g.assignmentId === assignmentId);
  if (error) return <p role="alert">{error} <Button size="sm" onClick={() => void refresh().catch(() => {})}>Thử lại</Button></p>;
  if (!grade || grade.score == null) return null;
  return <section aria-label="Kết quả chấm bài nhóm" className="space-y-3 rounded-xl border border-line bg-surface p-4">
    <h3 className="font-semibold text-ink">Điểm đã công bố: {Number(grade.score)} / {Number(grade.maxScore)}</h3>
    {grade.memberOverride && <p className="text-xs text-muted">Điểm cá nhân đã được giảng viên điều chỉnh riêng.</p>}
    <PublishedFeedback feedback={grade.feedback} summary={grade.structuredSummary ?? null} />
    <PublishedRubricItems items={(grade.rubricItems ?? []).map(item => ({
      ...item, awarded: Number(item.awardedPoints), max: Number(item.maxPoints),
    }))} />
    <StudentGradeDistribution classId={classId} assignmentId={assignmentId} />
  </section>;
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
