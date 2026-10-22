"use client";

import { useRouter } from "next/navigation";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { ClickableRow, Pill, TableFrame, Td, Th } from "@/shared/ui";
import { formatDateTime } from "./format";
import { assignmentRowActions } from "./slots";
import { gradingLabels, originLabels, statusLabels, statusTones, submissionLabels, type AssignmentRow } from "./types";

/** Bảng bài tập (F1, F2): tên, hình thức, chế độ (hai cột riêng), phạm vi, trạng thái, hạn; lối vào bài nộp do U09 gắn. */
export function AssignmentTable({ items, classId, showScope }: { items: AssignmentRow[]; classId?: string; showScope: boolean }) {
  const router = useRouter();
  if (items.length === 0) return <p className="py-6 text-center text-sm text-muted">Chưa có bài tập nào.</p>;
  return (
    <TableFrame>
      <thead>
        <tr>
          <Th>Bài tập</Th>
          <Th>Hình thức</Th>
          <Th>Chế độ</Th>
          {showScope && <Th>Phạm vi</Th>}
          <Th>Trạng thái</Th>
          <Th>Hạn nộp</Th>
          {classId && <Th>Bài nộp</Th>}
        </tr>
      </thead>
      <tbody>
        {items.map((row) => (
          <ClickableRow key={row.id} data-testid={`assignment-row-${row.id}`}
            onClick={() => router.push(`/assignments/${encodeURIComponent(row.id)}${classId ? `?classId=${encodeURIComponent(classId)}` : ""}`)}>
            <Td>
              <p className="font-semibold">{row.title}</p>
              <p className="flex flex-wrap items-center gap-1 text-[11px] text-muted">
                {row.versionNo > 1 && <span>Version {row.versionNo}</span>}
                {row.origin !== "MANUAL" && row.origin !== "CLONE" && <span>· {originLabels[row.origin]}</span>}
              </p>
            </Td>
            <Td>{submissionLabels[row.submissionMode]}</Td>
            <Td><Pill tone={row.gradingMode === "GRADED" ? "info" : "accent"}>{gradingLabels[row.gradingMode]}</Pill></Td>
            {showScope && <Td>{row.scope === "SUBJECT" ? <Pill tone="info">Của môn</Pill> : <Pill>Của lớp</Pill>}</Td>}
            <Td><Pill tone={statusTones[row.status]}>{statusLabels[row.status]}</Pill></Td>
            <Td className="whitespace-nowrap text-xs">{formatDateTime(row.closesAt)}</Td>
            {classId && (
              <Td onClick={(event) => event.stopPropagation()}>
                <SlotOutlet slot={assignmentRowActions} props={{ assignmentId: row.id, classId }} />
              </Td>
            )}
          </ClickableRow>
        ))}
      </tbody>
    </TableFrame>
  );
}
