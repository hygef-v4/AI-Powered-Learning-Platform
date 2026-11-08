"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Alert, Pill, Select, TableFrame, Td, Th } from "@/shared/ui";
import { getGroupAssignments, getProgress } from "../api";
import { formatDate } from "../model/format";
import { personLabel } from "../model/groupDrafts";
import type { GroupAssignment, GroupProgress, GroupSetView } from "../model/types";

/**
 * Tiến độ bài nhóm cho giảng viên (BR-U12-52): mỗi nhóm có trưởng nhóm, số mục Xong, trạng thái nộp, mục đang soạn và
 * lối vào tài liệu nhóm (chỉ xem, nhả mục ở đó). Lớp chưa có bài nhóm thì không hiện.
 */
export function GroupProgressCard({ classId, set }: { classId: string; set: GroupSetView }) {
  const [assignments, setAssignments] = useState<GroupAssignment[] | null>(null);
  const [selected, setSelected] = useState("");
  const [rows, setRows] = useState<GroupProgress[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const people = new Map(set.groups.flatMap((g) => g.members.map((m) => [m.accountId, m] as const)));

  useEffect(() => {
    let active = true;
    getGroupAssignments(classId).then(
      (list) => {
        if (!active) return;
        setAssignments(list);
        const open = list.find((a) => a.status === "OPEN") ?? list[0];
        if (open) setSelected(open.assignmentId);
      },
      () => {
        if (active) setError("Không tải được danh sách bài nhóm.");
      },
    );
    return () => {
      active = false;
    };
  }, [classId]);

  useEffect(() => {
    if (!selected) return;
    let active = true;
    getProgress(classId, selected).then(
      (list) => {
        if (active) setRows(list);
      },
      () => {
        if (active) setError("Không tải được tiến độ bài nhóm.");
      },
    );
    return () => {
      active = false;
    };
  }, [classId, selected]);

  if (assignments !== null && assignments.length === 0) return null;

  return (
    <section aria-label="Tiến độ bài nhóm" className="flex min-w-0 flex-col gap-3 rounded-xl border border-line bg-surface p-4">
      <div className="flex flex-wrap items-center gap-3">
        <h2 className="font-display text-[15px] font-bold text-ink">Tiến độ bài nhóm</h2>
        {assignments && assignments.length > 1 && (
          <Select aria-label="Bài nhóm" value={selected} onChange={(e) => setSelected(e.target.value)} wrapperClassName="ml-auto max-w-[360px]">
            {assignments.map((a) => (
              <option key={a.assignmentId} value={a.assignmentId}>
                {a.title}
                {a.closesAt ? ` · hạn ${formatDate(a.closesAt)}` : ""}
              </option>
            ))}
          </Select>
        )}
        {assignments?.length === 1 && <span className="text-[13px] text-muted">{assignments[0]!.title}</span>}
      </div>
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {rows && (
        <TableFrame>
          <thead>
            <tr>
              <Th>Nhóm</Th>
              <Th>Trưởng nhóm</Th>
              <Th>Mục Xong</Th>
              <Th>Trạng thái</Th>
              <Th>Đang soạn</Th>
              <Th aria-label="Thao tác" />
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => {
              const total = row.done + row.claimed + row.open;
              const started = row.done + row.claimed > 0;
              const status = row.submitted ? (row.late ? "Đã nộp · trễ" : "Đã nộp") : started ? "Đang làm" : "Chưa bắt đầu";
              const holding = row.sections.filter((s) => s.status === "CLAIMED");
              return (
                <tr key={row.groupId}>
                  <Td className="font-semibold">{row.groupName}</Td>
                  <Td>{personLabel(people.get(row.leaderId ?? ""))}</Td>
                  <Td>
                    {row.done} / {total}
                  </Td>
                  <Td>
                    <Pill tone={row.submitted ? "success" : started ? "info" : "neutral"}>{status}</Pill>
                  </Td>
                  <Td className="text-xs">
                    {holding.length === 0 ? "—" : holding.map((s) => `${s.title} · ${personLabel(people.get(s.holderId ?? ""))}`).join("; ")}
                  </Td>
                  <Td>
                    {row.documentId && (
                      <Link href={`/group-docs/${encodeURIComponent(row.documentId)}`} className="text-[13px] font-semibold text-primary-ink underline">
                        Xem tài liệu
                      </Link>
                    )}
                  </Td>
                </tr>
              );
            })}
          </tbody>
        </TableFrame>
      )}
    </section>
  );
}
