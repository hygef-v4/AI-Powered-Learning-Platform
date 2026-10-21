"use client";

import Link from "next/link";
import { Pill, Section } from "@/shared/ui";
import { formatDateTime } from "./format";
import { myStatusLabels, myStatusTones } from "./types";
import { upcoming, useStudentAssignments } from "./useStudentAssignments";

/** "Bài tập sắp đến hạn" trên Student Class Detail (F1); gắn qua StudentClassSection của U05. */
export function ClassAssignmentsCard({ classId }: { classId: string }) {
  const { rows } = useStudentAssignments(classId);
  if (!rows) return null;
  const soon = rows.filter(upcoming).slice(0, 5);
  return (
    <Section title="Bài tập sắp đến hạn" icon="assignment"
      actions={<Link href={`/my-assignments?classId=${encodeURIComponent(classId)}`} className="text-xs font-semibold text-primary">Xem tất cả bài của lớp</Link>}>
      {soon.length === 0 ? <p className="text-sm text-muted">Không có bài sắp đến hạn.</p> : (
        <ul className="flex flex-col gap-1.5">
          {soon.map((row) => (
            <li key={row.assignmentId}>
              <Link href={`/my-assignments/${encodeURIComponent(row.assignmentId)}?classId=${encodeURIComponent(classId)}`}
                className="flex items-center gap-2 rounded-lg border border-line px-3 py-2 hover:bg-canvas">
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-[13px] font-semibold text-ink">{row.title}</span>
                  <span className="text-[11px] text-muted">
                    {row.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"} · {row.gradingMode === "GRADED" ? "Graded" : "Practice"} · Hạn {formatDateTime(row.closesAt)}
                  </span>
                </span>
                <Pill tone={myStatusTones[row.myStatus]}>{myStatusLabels[row.myStatus]}</Pill>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </Section>
  );
}

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
