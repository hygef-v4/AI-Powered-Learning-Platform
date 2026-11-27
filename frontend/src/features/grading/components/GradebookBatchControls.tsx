"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert, Button, ClickableRow, Icon, Pill, TableFrame, Td, Th } from "@/shared/ui";
import { Pagination } from "@/shared/ui/Pagination";
import { useSubmissionDirectory } from "../hooks/useSubmissionDirectory";
import { BatchActionDialog } from "./BatchActionDialog";
import { buildGradeActionItems } from "../model/gradeActions";
import { GRADE_STATUS_LABELS, type ItemVersion, type SubmissionRow } from "../model/types";
import { buildEvaluationHref } from "../model/navigation";
import { matchesPersonQuery } from "@/shared/studentCode";
import { PersonSubline } from "@/shared/ui/PersonSubline";
import { msg } from "@/i18n/msg";

type Props = {
  classId: string; assignmentId: string; onDone: () => void;
  query?: string; status?: string; group?: boolean;
};
const PAGE_SIZE = 20;
const MAX_BATCH = 100;
// Quy đổi trạng thái cũ về các nhóm mà bộ lọc sổ điểm dùng.
const formatStatus = (status: string) => status === "FINALIZED" ? "DRAFT" : status === "IN_PROGRESS" ? "NOT_SUBMITTED" : status;
// Chỉ cho chọn hàng có bản chấm và trạng thái hỗ trợ thao tác theo lô.
const canSelect = (row: SubmissionRow) => !!row.evaluationId && ["PENDING", "DRAFT", "FINALIZED", "PUBLISHED"].includes(row.status);

// Tải danh sách bài nộp và đặt lại bảng chọn khi phạm vi hoặc bộ lọc đổi.
export function GradebookBatchControls(props: Props) {
  const directory = useSubmissionDirectory(props.classId, props.assignmentId);
  return <SubmissionTable key={JSON.stringify([props.classId, props.assignmentId, props.query, props.status])} {...props} directory={directory} />;
}

// Gắn kết quả danh sách với các hàng chọn, điều hướng và giới hạn thao tác của lớp.
function SubmissionTable({ onDone, query = "", status = "", group = false, directory }: Props & { directory: ReturnType<typeof useSubmissionDirectory> }) {
  const { value, error, refresh } = directory;
  const readOnly = directory.editable === false;
  const router = useRouter();
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [page, setPage] = useState(0);
  const [action, setAction] = useState<{ kind: "ai" | "publish"; items: ItemVersion[] } | null>(null);
  const search = query.trim().toLocaleLowerCase("vi");
  const rows = (value ?? []).filter(row => (!status || formatStatus(row.status) === status)
    // Bài nhóm: tìm theo tên nhóm; bài cá nhân: theo tên, email hoặc MSSV sinh viên.
    && (group ? row.student.name.toLocaleLowerCase("vi").includes(search) : matchesPersonQuery(row.student, search)));
  const currentPage = Math.min(page, Math.max(0, Math.ceil(rows.length / PAGE_SIZE) - 1));
  const shown = rows.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);
  const eligible = readOnly ? [] : rows.filter(canSelect);
  const pageItems = readOnly ? [] : shown.filter(canSelect);
  const selectedRows = eligible.filter(row => selected.has(row.evaluationId!));
  const selectedCount = selectedRows.length;
  const allPageSelected = pageItems.length > 0 && pageItems.every(row => selected.has(row.evaluationId!));
  const somePageSelected = pageItems.some(row => selected.has(row.evaluationId!));
  const pageFits = new Set([...selected, ...pageItems.map(row => row.evaluationId!)]).size <= MAX_BATCH;
  const aiItems = buildGradeActionItems(selectedRows, "ai");
  const publishItems = buildGradeActionItems(selectedRows, "publish");

  // Cập nhật tập ID được chọn mà vẫn áp dụng giới hạn số bản chấm trong lô.
  function toggleRow(id: string, checked: boolean) {
    setSelected(current => {
      const next = new Set(current);
      if (checked && next.size < MAX_BATCH) next.add(id); else next.delete(id);
      return next;
    });
  }
  // Áp lựa chọn cho các hàng của trang đang xem, giữ tập chọn ở trang khác.
  function togglePage(checked: boolean) {
    setSelected(current => {
      const next = new Set(current);
      pageItems.forEach(row => { if (checked) next.add(row.evaluationId!); else next.delete(row.evaluationId!); });
      return next.size <= MAX_BATCH ? next : current;
    });
  }

  if (error) return <div role="alert" className="flex flex-wrap items-center gap-2 text-sm text-danger">
    {error}<Button size="sm" onClick={() => void refresh().catch(() => {})}>{msg("grading.components.GradebookBatchControls.retry")}</Button>
  </div>;
  if (!value) return <p role="status" className="text-sm text-muted">{msg("grading.components.GradebookBatchControls.loadingSubmissions")}</p>;

  return <>
    {readOnly && <Alert tone="info">{directory.readOnlyReason || msg("grading.components.GradebookBatchControls.thisAssignmentOnlyAllowsViewing")}</Alert>}
    {selectedCount > 0 && <div role="toolbar" aria-label={msg("grading.components.GradebookBatchControls.actionsOnSelectedSubmissions")}
      className="sticky top-14 z-10 flex flex-wrap items-center gap-2 rounded-lg border border-line bg-surface p-3">
      <span className="mr-auto text-sm font-semibold text-ink" aria-live="polite">{msg("grading.components.GradebookBatchControls.selected")}{selectedCount} {msg("grading.components.GradebookBatchControls.submissions")}</span>
      <Button size="sm" disabled={!aiItems.length} onClick={() => setAction({ kind: "ai", items: aiItems })}>{msg("grading.components.GradebookBatchControls.gradeWithAi")}{aiItems.length})</Button>
      <Button size="sm" disabled={!publishItems.length} onClick={() => setAction({ kind: "publish", items: publishItems })}>{msg("grading.components.GradebookBatchControls.publishGrades")}{publishItems.length})</Button>
      <Button size="sm" variant="ghost" onClick={() => setSelected(new Set())}>{msg("grading.components.GradebookBatchControls.deselect")}</Button>
    </div>}
    {allPageSelected && selectedCount < eligible.length && <div className="flex flex-wrap items-center gap-2 text-sm">
      <Button size="sm" variant="secondary" disabled={eligible.length > MAX_BATCH}
        onClick={() => setSelected(new Set(eligible.map(row => row.evaluationId!)))}>
        {msg("grading.components.GradebookBatchControls.selectAll")}{eligible.length} {msg("grading.components.GradebookBatchControls.matchingSubmissions")}</Button>
      {eligible.length > MAX_BATCH && <span className="text-muted">{msg("grading.components.GradebookBatchControls.upTo100SubmissionsAt")}</span>}
    </div>}
    {selectedCount === MAX_BATCH && <p className="text-xs text-muted">{msg("grading.components.GradebookBatchControls.upTo100SubmissionsAt")}</p>}
    <TableFrame filledHead className="[&_table]:min-w-[560px] [&_table]:table-fixed">
      <caption className="sr-only">{group ? msg("grading.components.GradebookBatchControls.groupSubmissions") : msg("grading.components.GradebookBatchControls.studentSubmissions")}</caption>
      <colgroup><col className="w-11" /><col /><col className="w-24" /><col className="w-36" /><col className="w-11" /></colgroup>
      <thead><tr>
        <Th><input type="checkbox" aria-label={msg("grading.components.GradebookBatchControls.selectAllOnPage")} className="size-4 accent-primary"
          checked={allPageSelected} disabled={!pageItems.length || (!allPageSelected && !pageFits)}
          ref={node => { if (node) node.indeterminate = somePageSelected && !allPageSelected; }}
          onChange={event => togglePage(event.target.checked)} /></Th>
        <Th>{group ? msg("grading.components.GradebookBatchControls.group") : msg("grading.components.GradebookBatchControls.student")}</Th><Th>{msg("grading.components.GradebookBatchControls.score")}</Th><Th>{msg("grading.components.GradebookBatchControls.status")}</Th><Th><span className="sr-only">{msg("grading.components.GradebookBatchControls.actions")}</span></Th>
      </tr></thead>
      <tbody>{shown.map(row => {
        const state = formatStatus(row.status);
        const href = canSelect(row) ? buildEvaluationHref(row.evaluationId!) : null;
        const label = group ? msg("grading.components.GradebookBatchControls.viewTheSharedSubmissionOf", { name: String(row.student.name) }) : readOnly ? msg("grading.components.GradebookBatchControls.viewWork") : state === "PENDING" ? msg("grading.components.GradebookBatchControls.grade") : state === "DRAFT" ? msg("grading.components.GradebookBatchControls.continueGrading") : msg("grading.components.GradebookBatchControls.viewGrade");
        const cells = <>
          <Td onClick={(event) => event.stopPropagation()}><input type="checkbox" aria-label={msg("grading.components.GradebookBatchControls.select", { name: String(row.student.name) })} className="size-4 accent-primary"
            checked={!!row.evaluationId && selected.has(row.evaluationId)}
            disabled={readOnly || !canSelect(row) || (selectedCount >= MAX_BATCH && !selected.has(row.evaluationId!))}
            onChange={event => toggleRow(row.evaluationId!, event.target.checked)} /></Td>
          <Td className="min-w-0">
            <strong className="block break-words text-ink">{row.student.name}</strong>
            <PersonSubline className="truncate" studentCode={row.student.studentCode} email={row.student.email} />
          </Td>
          <Td className="font-semibold whitespace-nowrap tabular-nums">{row.score == null ? "—" : `${Number(row.score)} / ${Number(row.maxScore ?? 10)}`}</Td>
          <Td><div className="flex flex-wrap gap-1.5">
            <Pill tone={state === "PUBLISHED" ? "success" : state === "DRAFT" ? "info" : state === "PENDING" ? "warning" : "neutral"}>{GRADE_STATUS_LABELS[state] ?? state}</Pill>
            {row.late && <Pill tone="danger">{msg("grading.components.GradebookBatchControls.late")}</Pill>}
            {row.leftClass && <Pill tone="neutral">{msg("grading.components.GradebookBatchControls.leftClass")}</Pill>}
          </div></Td>
          <Td>{href && <Link href={href} aria-label={label} title={label}
            onClick={(event) => event.stopPropagation()}
            className="inline-flex size-8 items-center justify-center rounded-lg text-muted hover:bg-primary-soft hover:text-primary-ink"><Icon name="chevron_right" size={20} /></Link>}</Td>
        </>;
        // Bấm cả dòng là mở trang chấm.
        return href
          ? <ClickableRow key={row.student.accountId} onClick={() => router.push(href)}>{cells}</ClickableRow>
          : <tr key={row.student.accountId}>{cells}</tr>;
      })}
        {!shown.length && <tr><Td colSpan={5} className="py-8 text-center! text-muted">{msg("grading.components.GradebookBatchControls.noMatchingSubmissions")}</Td></tr>}
      </tbody>
    </TableFrame>
    <Pagination page={currentPage} pageSize={PAGE_SIZE} total={rows.length} summary={false} onPageChange={setPage} />
    {action && !readOnly && <BatchActionDialog action={action.kind} items={action.items} onClose={() => setAction(null)} onDone={() => {
      setSelected(new Set());
      void refresh().catch(() => {});
      onDone();
    }} />}
  </>;
}
