"use client";
import { useState } from "react";
import { Button } from "@/shared/ui";
import { useSubmissionDirectory } from "../hooks/useSubmissionDirectory";
import { BatchActionDialog } from "./BatchActionDialog";
import { buildGradeActionItems } from "../model/gradeActions";
import { GRADE_STATUS_LABELS } from "../model/types";

type Props = { classId: string; assignmentId: string; onDone: () => void };
export function GradebookBatchControls(props: Props) {
  const [open, setOpen] = useState(false);
  return <div>
    <Button size="sm" aria-expanded={open} onClick={() => setOpen(!open)}>Thao tác hàng loạt</Button>
    {open && <BatchSelection {...props} />}
  </div>;
}

function BatchSelection({ classId, assignmentId, onDone }: Props) {
  const { value, error, refresh } = useSubmissionDirectory(classId, assignmentId);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [action, setAction] = useState<"ai" | "finalize" | "publish" | null>(null);
  const rows = value?.filter(row => row.evaluationId) ?? [];
  const selectedRows = rows.filter(row => selected.has(row.evaluationId!));
  const items = selectedRows
    .map(row => ({ evaluationId: row.evaluationId!, version: row.version }));
  const disabled = !items.length || items.length > 100 || !!error;
  return <section aria-label="Chọn bài hàng loạt" className="mt-3 space-y-3 rounded-xl border border-line p-3">
    {error && <p role="alert">{error} <Button size="sm" onClick={() => void refresh().catch(() => {})}>Thử lại</Button></p>}
    {!value && !error && <p role="status">Đang tải bài nộp…</p>}
    {value && !rows.length && <p className="text-sm text-muted">Chưa có bài nộp để chọn.</p>}
    <ul className="max-h-52 space-y-2 overflow-y-auto text-sm">
      {rows.map(row => <li key={row.evaluationId}>
        <label className="flex items-center gap-2">
          <input type="checkbox" aria-label={`Chọn ${row.student.name}`} checked={selected.has(row.evaluationId!)}
            onChange={event => setSelected(current => {
              const next = new Set(current);
              if (event.target.checked) next.add(row.evaluationId!); else next.delete(row.evaluationId!);
              return next;
            })} className="size-4 accent-primary" />
          {row.student.name}
          <span className="ml-auto text-xs text-muted">{row.status === "FINALIZED" ? "Đã chốt" : GRADE_STATUS_LABELS[row.status] ?? row.status}</span>
        </label>
      </li>)}
    </ul>
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-xs text-muted">Đã chọn {items.length} / 100 bài</span>
      <Button size="sm" disabled={disabled || !buildGradeActionItems(selectedRows, "ai").length} onClick={() => setAction("ai")}>Chấm AI đã chọn</Button>
      <Button size="sm" disabled={disabled || !buildGradeActionItems(selectedRows, "finalize").length} onClick={() => setAction("finalize")}>Chốt điểm đã chọn</Button>
      <Button size="sm" disabled={disabled || !buildGradeActionItems(selectedRows, "publish").length} onClick={() => setAction("publish")}>Công bố đã chọn</Button>
    </div>
    {action && <BatchActionDialog action={action} items={buildGradeActionItems(selectedRows, action)} onClose={() => setAction(null)} onDone={() => {
      void refresh().catch(() => {});
      onDone();
    }} />}
  </section>;
}
