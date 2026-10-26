"use client";

import Link from "next/link";
import { Pill, Section } from "@/shared/ui";
import { DeadlineChip } from "./DeadlineChip";
import { formatDateTime } from "./format";
import { myStatusLabels, myStatusTones } from "./types";
import { upcoming, useStudentAssignments } from "./useStudentAssignments";

/** "Bài tập sắp đến hạn" trên Student Class Detail (F1); gắn qua StudentClassSection của U05. */
export function ClassAssignmentsCard({ classId }: { classId: string }) {
  const { rows } = useStudentAssignments(classId);
  if (!rows) return null;
  const soon = rows.filter(upcoming).slice(0, 5);
  return (
    <Section title="Cần làm"
      actions={<Link href={`/my-assignments?classId=${encodeURIComponent(classId)}`} className="text-[13px] font-semibold text-primary-ink">Tất cả bài của lớp</Link>}>
      {soon.length === 0 ? <p className="text-sm text-muted">Không còn bài cần làm.</p> : (
        <ul className="-my-1 flex flex-col">
          {soon.map((row) => (
            <li key={row.assignmentId} className="border-b border-line last:border-b-0">
              <Link href={`/my-assignments/${encodeURIComponent(row.assignmentId)}?classId=${encodeURIComponent(classId)}`}
                className="-mx-2 flex items-center gap-3 rounded-lg px-2 py-3 transition-colors hover:bg-shell">
                <DeadlineChip closesAt={row.closesAt} />
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-[13.5px] font-semibold text-ink">{row.title}</span>
                  <span className="text-xs text-muted">
                    {row.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"}{row.gradingMode === "PRACTICE" ? " · Luyện tập" : ""} · <span className="font-mono">{formatDateTime(row.closesAt)}</span>
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
