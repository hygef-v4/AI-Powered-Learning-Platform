"use client";

import { upcoming, useStudentAssignments } from "./useStudentAssignments";

/** Dòng trên thẻ lớp ở Class Dashboard (F1); gắn qua ClassCardSection của U05. */
export function ClassCardAssignments({ classId }: { classId: string }) {
  const { rows } = useStudentAssignments(classId);
  if (!rows) return null;
  const soon = rows.filter(upcoming).length;
  const open = rows.filter((row) => row.status === "OPEN").length;
  return (
    <p className="text-xs text-muted" data-testid="class-card-assignments">
      {soon > 0 ? `${soon} bài sắp đến hạn` : open > 0 ? `${open} bài đang mở` : "Chưa có bài"}
    </p>
  );
}
