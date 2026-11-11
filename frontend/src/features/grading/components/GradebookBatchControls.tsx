"use client";
import Link from "next/link";
import { useState } from "react";
import { Button, Icon, Pill, TableFrame, Td, Th } from "@/shared/ui";
import { Pagination } from "@/shared/ui/Pagination";
import { useSubmissionDirectory } from "../hooks/useSubmissionDirectory";
import { BatchActionDialog } from "./BatchActionDialog";
import { buildGradeActionItems } from "../model/gradeActions";
import { GRADE_STATUS_LABELS, type ItemVersion, type SubmissionRow } from "../model/types";
import { buildEvaluationHref } from "../model/navigation";

type Props = {
  classId: string; assignmentId: string; onDone: () => void;
  query?: string; status?: string; group?: boolean;
};
const PAGE_SIZE = 20;
const MAX_BATCH = 100;
const formatStatus = (status: string) => status === "FINALIZED" ? "DRAFT" : status === "IN_PROGRESS" ? "NOT_SUBMITTED" : status;
const canSelect = (row: SubmissionRow) => !!row.evaluationId && ["PENDING", "DRAFT", "FINALIZED", "PUBLISHED"].includes(row.status);

export function GradebookBatchControls(props: Props) {
  const directory = useSubmissionDirectory(props.classId, props.assignmentId);
  return <SubmissionTable key={JSON.stringify([props.classId, props.assignmentId, props.query, props.status])} {...props} directory={directory} />;
}

function SubmissionTable({ onDone, query = "", status = "", group = false, directory }: Props & { directory: ReturnType<typeof useSubmissionDirectory> }) {
  const { value, error, refresh } = directory;
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [page, setPage] = useState(0);
  const [action, setAction] = useState<{ kind: "ai" | "publish"; items: ItemVersion[] } | null>(null);
  const search = query.trim().toLocaleLowerCase("vi");
  const rows = (value ?? []).filter(row => (!status || formatStatus(row.status) === status)
    && `${row.student.name} ${row.student.email}`.toLocaleLowerCase("vi").includes(search));
  const currentPage = Math.min(page, Math.max(0, Math.ceil(rows.length / PAGE_SIZE) - 1));
  const shown = rows.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);
  const eligible = rows.filter(canSelect);
  const pageItems = shown.filter(canSelect);
  const selectedRows = eligible.filter(row => selected.has(row.evaluationId!));
  const selectedCount = selectedRows.length;
  const allPageSelected = pageItems.length > 0 && pageItems.every(row => selected.has(row.evaluationId!));
  const somePageSelected = pageItems.some(row => selected.has(row.evaluationId!));
  const pageFits = new Set([...selected, ...pageItems.map(row => row.evaluationId!)]).size <= MAX_BATCH;
  const aiItems = buildGradeActionItems(selectedRows, "ai");
  const publishItems = buildGradeActionItems(selectedRows, "publish");

  function toggleRow(id: string, checked: boolean) {
    setSelected(current => {
      const next = new Set(current);
      if (checked && next.size < MAX_BATCH) next.add(id); else next.delete(id);
      return next;
    });
  }
  function togglePage(checked: boolean) {
    setSelected(current => {
      const next = new Set(current);
      pageItems.forEach(row => { if (checked) next.add(row.evaluationId!); else next.delete(row.evaluationId!); });
      return next.size <= MAX_BATCH ? next : current;
    });
  }

  if (error) return <div role="alert" className="flex flex-wrap items-center gap-2 text-sm text-danger">
    {error}<Button size="sm" onClick={() => void refresh().catch(() => {})}>Thử lại</Button>
  </div>;
  if (!value) return <p role="status" className="text-sm text-muted">Đang tải bài nộp…</p>;

  return <>
    {selectedCount > 0 && <div role="toolbar" aria-label="Thao tác bài đã chọn"
      className="sticky top-14 z-10 flex flex-wrap items-center gap-2 rounded-lg border border-line bg-surface p-3">
      <span className="mr-auto text-sm font-semibold text-ink" aria-live="polite">Đã chọn {selectedCount} bài</span>
      <Button size="sm" disabled={!aiItems.length} onClick={() => setAction({ kind: "ai", items: aiItems })}>Chấm bằng AI ({aiItems.length})</Button>
      <Button size="sm" disabled={!publishItems.length} onClick={() => setAction({ kind: "publish", items: publishItems })}>Công bố điểm ({publishItems.length})</Button>
      <Button size="sm" variant="ghost" onClick={() => setSelected(new Set())}>Bỏ chọn</Button>
    </div>}
    {allPageSelected && selectedCount < eligible.length && <div className="flex flex-wrap items-center gap-2 text-sm">
      <Button size="sm" variant="secondary" disabled={eligible.length > MAX_BATCH}
        onClick={() => setSelected(new Set(eligible.map(row => row.evaluationId!)))}>
        Chọn toàn bộ {eligible.length} bài phù hợp
      </Button>
      {eligible.length > MAX_BATCH && <span className="text-muted">Tối đa 100 bài mỗi lần.</span>}
    </div>}
    {selectedCount === MAX_BATCH && <p className="text-xs text-muted">Tối đa 100 bài mỗi lần.</p>}
    <TableFrame filledHead className="[&_table]:min-w-[560px] [&_table]:table-fixed">
      <caption className="sr-only">{group ? "Bài nộp của nhóm" : "Bài nộp của sinh viên"}</caption>
      <colgroup><col className="w-11" /><col /><col className="w-24" /><col className="w-36" /><col className="w-11" /></colgroup>
      <thead><tr>
        <Th><input type="checkbox" aria-label="Chọn tất cả trên trang" className="size-4 accent-primary"
          checked={allPageSelected} disabled={!pageItems.length || (!allPageSelected && !pageFits)}
          ref={node => { if (node) node.indeterminate = somePageSelected && !allPageSelected; }}
          onChange={event => togglePage(event.target.checked)} /></Th>
        <Th>{group ? "Nhóm" : "Sinh viên"}</Th><Th>Điểm</Th><Th>Trạng thái</Th><Th><span className="sr-only">Thao tác</span></Th>
      </tr></thead>
      <tbody>{shown.map(row => {
        const state = formatStatus(row.status);
        const href = canSelect(row) ? buildEvaluationHref(row.evaluationId!) : null;
        const label = group ? `Xem bản nộp chung của ${row.student.name}` : state === "PENDING" ? "Chấm bài" : state === "DRAFT" ? "Tiếp tục chấm" : "Xem điểm";
        return <tr key={row.student.accountId}>
          <Td><input type="checkbox" aria-label={`Chọn ${row.student.name}`} className="size-4 accent-primary"
            checked={!!row.evaluationId && selected.has(row.evaluationId)}
            disabled={!canSelect(row) || (selectedCount >= MAX_BATCH && !selected.has(row.evaluationId!))}
            onChange={event => toggleRow(row.evaluationId!, event.target.checked)} /></Td>
          <Td className="min-w-0">
            <strong className="block break-words text-ink">{row.student.name}</strong>
            {row.student.email && <p className="truncate text-xs text-muted">{row.student.email}</p>}
          </Td>
          <Td className="font-semibold whitespace-nowrap tabular-nums">{row.score == null ? "—" : `${Number(row.score)} / ${Number(row.maxScore ?? 10)}`}</Td>
          <Td><div className="flex flex-wrap gap-1.5">
            <Pill tone={state === "PUBLISHED" ? "success" : state === "DRAFT" ? "info" : state === "PENDING" ? "warning" : "neutral"}>{GRADE_STATUS_LABELS[state] ?? state}</Pill>
            {row.late && <Pill tone="danger">Trễ</Pill>}
          </div></Td>
          <Td>{href && <Link href={href} aria-label={label} title={label}
            className="inline-flex size-8 items-center justify-center rounded-lg text-muted hover:bg-primary-soft hover:text-primary-ink"><Icon name="chevron_right" size={20} /></Link>}</Td>
        </tr>;
      })}
        {!shown.length && <tr><Td colSpan={5} className="py-8 text-center! text-muted">Không có bài phù hợp.</Td></tr>}
      </tbody>
    </TableFrame>
    <Pagination page={currentPage} pageSize={PAGE_SIZE} total={rows.length} itemLabel={group ? "nhóm" : "sinh viên"} onPageChange={setPage} />
    {action && <BatchActionDialog action={action.kind} items={action.items} onClose={() => setAction(null)} onDone={() => {
      setSelected(new Set());
      void refresh().catch(() => {});
      onDone();
    }} />}
  </>;
}
