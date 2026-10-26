"use client";

import { useEffect, useState } from "react";
import { formatDate } from "./format";
import { Alert, Button, Dialog, Pill, Section, Select, TableFrame, Td, Th } from "@/shared/ui";
import { getGroupAssignments, getProgress, releaseSection } from "./api";
import { personLabel } from "./GroupEditor";
import type { GroupAssignment, GroupProgress, GroupSetView, SectionProgress } from "./types";

/** Tiến độ bài nhóm theo nhóm (t02e, BR-U12-52) và nhả mục (t02h). */
export function GroupProgressView({ classId, set, onBack }: { classId: string; set: GroupSetView; onBack: () => void }) {
  const [assignments, setAssignments] = useState<GroupAssignment[] | null>(null);
  const [selected, setSelected] = useState("");
  const [rows, setRows] = useState<GroupProgress[] | null>(null);
  const [releasing, setReleasing] = useState<{ row: GroupProgress; section: SectionProgress } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reload, setReload] = useState(0);
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
        if (active) setError("Không tải được tiến độ.");
      },
    );
    return () => {
      active = false;
    };
  }, [classId, selected, reload]);

  const release = async () => {
    if (!releasing?.row.documentId) return;
    try {
      await releaseSection(releasing.row.documentId, releasing.section.id);
      setReloadAndClose();
    } catch {
      setError("Không nhả được mục, thử lại sau.");
      setReleasing(null);
    }
  };
  const setReloadAndClose = () => {
    setReleasing(null);
    setReload((r) => r + 1);
  };

  return (
    <Section
      title="Tiến độ bài nhóm"
      icon="monitoring"
      actions={
        <Button size="sm" icon="arrow_back" onClick={onBack}>
          Danh sách nhóm
        </Button>
      }
    >
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {assignments && assignments.length === 0 && <p className="text-[13px] text-muted">Lớp chưa có bài nhóm.</p>}
      {assignments && assignments.length > 0 && (
        <Select aria-label="Bài nhóm" value={selected} onChange={(e) => setSelected(e.target.value)} wrapperClassName="max-w-[420px]">
          {assignments.map((a) => (
            <option key={a.assignmentId} value={a.assignmentId}>
              {a.title}
              {a.closesAt ? ` · hạn ${formatDate(a.closesAt)}` : ""}
            </option>
          ))}
        </Select>
      )}
      {rows && (
        <TableFrame>
          <thead>
            <tr>
              <Th>Nhóm</Th>
              <Th>Trưởng nhóm</Th>
              <Th>Mục xong</Th>
              <Th>Trạng thái</Th>
              <Th>Mục đang giữ</Th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => {
              const total = row.done + row.claimed + row.open;
              const status = row.submitted ? (row.late ? "Đã nộp · trễ" : "Đã nộp") : row.done + row.claimed > 0 ? "Đang làm" : "Chưa bắt đầu";
              return (
                <tr key={row.groupId}>
                  <Td className="font-semibold">{row.groupName}</Td>
                  <Td>{personLabel(people.get(row.leaderId ?? ""))}</Td>
                  <Td>
                    {row.done} / {total}
                  </Td>
                  <Td>
                    <Pill tone={row.submitted ? "success" : row.done + row.claimed > 0 ? "info" : "neutral"}>{status}</Pill>
                  </Td>
                  <Td>
                    <div className="flex flex-col gap-1">
                      {row.sections.filter((s) => s.status === "CLAIMED").map((s) => (
                        <span key={s.id} className="flex items-center gap-2 text-xs">
                          {s.title} · {personLabel(people.get(s.holderId ?? ""))}
                          <Button size="sm" variant="ghost" onClick={() => setReleasing({ row, section: s })}>
                            Nhả mục
                          </Button>
                        </span>
                      ))}
                      {row.claimed === 0 && <span className="text-xs text-muted">—</span>}
                    </div>
                  </Td>
                </tr>
              );
            })}
          </tbody>
        </TableFrame>
      )}
      <Dialog
        open={releasing !== null}
        title={`Nhả mục ${releasing?.section.title ?? ""}?`}
        icon="warning"
        tone="warning"
        onClose={() => setReleasing(null)}
        footer={
          <>
            <Button variant="danger" onClick={() => void release()}>
              Nhả mục
            </Button>
          </>
        }
      >
        <p>
          {releasing?.row.groupName} · người giữ {personLabel(people.get(releasing?.section.holderId ?? ""))}.
        </p>
        <Alert tone="warning">
          Bản nháp chưa Xong của người đang giữ sẽ bị bỏ. Mục về Xong nếu đã có nội dung đã Xong, không thì về Mở.
        </Alert>
      </Dialog>
    </Section>
  );
}
