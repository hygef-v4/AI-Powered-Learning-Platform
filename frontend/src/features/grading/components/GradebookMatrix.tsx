"use client";

import Link from "next/link";
import { createPortal } from "react-dom";
import { useCallback, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { Button, ButtonLink, Select, TableFrame, Td, TextInput, Th } from "@/shared/ui";
import { Pagination } from "@/shared/ui/Pagination";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";
import { GRADE_STATUS_LABELS, type GradebookPage } from "../model/types";
import { buildEvaluationHref } from "../model/navigation";
import { GradebookExportDialog } from "./GradebookExportDialog";

export function GradebookMatrix({ classId, headerActions }: { classId: string; headerActions?: HTMLElement | null }) {
  const params = useSearchParams();
  const [assignmentId, setAssignmentId] = useUrlState<string>("matrixAssignment", "");
  const [query, setQuery] = useUrlState<string>("matrixQuery", "", undefined, "replace");
  const [pageValue, setPage] = useUrlState<string>("matrixPage", "1", undefined, "replace");
  const page = /^[1-9]\d*$/.test(pageValue) && Number.isSafeInteger(Number(pageValue)) ? Number(pageValue) : 1;
  const [search, setSearch] = useState("");
  useEffect(() => {
    const timer = setTimeout(() => setSearch(query.trim()), 300);
    return () => clearTimeout(timer);
  }, [query]);
  const load = useCallback(() => gradingApi.loadGradebook(classId, search, page), [classId, search, page]);
  const resource = useResource(load);
  const [kept, setKept] = useState<GradebookPage | null>(null);
  if (resource.value && resource.value !== kept) setKept(resource.value);
  const data = resource.value ?? kept;
  const [exporting, setExporting] = useState(false);
  const assignments = data?.assignments ?? [];
  const selectedId = assignments.some(item => item.id === assignmentId) ? assignmentId : "";
  const columns = assignments.filter(item => !selectedId || item.id === selectedId);
  const backParams = new URLSearchParams(params?.toString());
  ["gradebookView", "matrixAssignment", "matrixQuery", "matrixPage"].forEach(key => backParams.delete(key));
  const backHref = `/teaching/classes/${encodeURIComponent(classId)}?${backParams}`;
  const exportButton = <Button icon="download" disabled={!data || !assignments.length} onClick={() => setExporting(true)}>Xuất bảng điểm</Button>;

  return <div className="flex min-w-0 flex-col gap-4">
    {headerActions ? createPortal(exportButton, headerActions) : <div className="flex justify-end">{exportButton}</div>}
    <div className="flex flex-wrap items-center gap-3">
      <ButtonLink href={backHref} back icon="arrow_back">Quay lại chấm điểm</ButtonLink>
      <Select aria-label="Bài tập" value={selectedId} wrapperClassName="min-w-0 flex-1 sm:max-w-sm"
        onChange={event => setAssignmentId(event.target.value)}>
        <option value="">Toàn bộ bài tính điểm</option>
        {assignments.map(item => <option key={item.id} value={item.id}>{item.title}</option>)}
      </Select>
      <div className="w-full sm:ml-auto sm:w-64">
        <TextInput type="search" icon="search" aria-label="Tìm sinh viên" placeholder="Tìm tên, email hoặc MSSV" value={query}
          onChange={event => { setQuery(event.target.value); setPage("1"); }} />
      </div>
    </div>
    {resource.error && <div role="alert" className="flex flex-wrap items-center gap-2 text-sm text-danger">
      {resource.error}<Button size="sm" onClick={() => void resource.refresh().catch(() => {})}>Thử tải lại</Button>
    </div>}
    {!data && !resource.error && <p role="status" className="text-sm text-muted">Đang tải bảng điểm…</p>}
    {data && !columns.length && <p className="text-sm text-muted">Chưa có bài tính điểm trong lớp.</p>}
    {data && columns.length > 0 && <>
      <TableFrame filledHead className="max-h-[68vh] rounded-xl border border-line bg-surface [&_table]:w-max [&_table]:min-w-full [&_thead_th]:sticky [&_thead_th]:top-0 [&_thead_th]:z-20">
        <caption className="sr-only">Điểm theo sinh viên và bài tập</caption>
        <thead><tr>
          <Th className="left-0 z-30! w-12">STT</Th>
          {/* Màn hẹp: MSSV gộp vào ô Sinh viên để các cột cố định không chiếm hết bề ngang. */}
          <Th className="left-12 z-30! hidden w-28 sm:table-cell">MSSV</Th>
          <Th className="left-12 z-30! min-w-44 sm:left-40 sm:min-w-60">Sinh viên</Th>
          {columns.map(column => <Th key={column.id} className="whitespace-normal! normal-case! tracking-normal!">
            <span className="block w-44 break-words" title={column.title}>{column.title}</span>
            <span className="mt-1 block text-xs font-normal text-muted">/ {Number(column.maxScore)} điểm</span>
          </Th>)}
        </tr></thead>
        <tbody>{data.rows.map((row, index) => <tr key={row.student.accountId}>
          <Td className="sticky left-0 z-10 w-12 bg-surface tabular-nums">{(page - 1) * 20 + index + 1}</Td>
          <Td className="sticky left-12 z-10 hidden w-28 bg-surface font-medium text-body sm:table-cell" data-testid="gradebook-student-code">{row.student.studentCode}</Td>
          <Td className="sticky left-12 z-10 min-w-44 bg-surface sm:left-40 sm:min-w-60">
            <strong className="block text-sm text-ink">{row.student.name}</strong>
            {row.student.studentCode && <span className="block text-xs font-semibold text-body sm:hidden">{row.student.studentCode}</span>}
            <span className="block max-w-40 break-all text-xs text-muted sm:max-w-none">{row.student.email}</span>
          </Td>
          {columns.map(column => {
            const grade = row.grades.find(item => item.assignmentId === column.id);
            const saved = grade && ["DRAFT", "FINALIZED", "PUBLISHED"].includes(grade.status);
            const points = saved && grade.score != null ? String(Number(grade.score)) : "—";
            const status = GRADE_STATUS_LABELS[grade?.status ?? "NOT_SUBMITTED"] ?? "Chưa nộp";
            return <Td key={column.id} className="text-center! font-semibold tabular-nums" title={status}>
              {grade?.evaluationId && grade.status !== "SUPERSEDED" ? <Link href={buildEvaluationHref(grade.evaluationId)}
                aria-label={`${column.title} — ${row.student.name}: ${points} (${status})`}
                className="inline-flex min-h-9 min-w-9 items-center justify-center rounded-lg px-2 text-primary-ink hover:bg-primary-soft hover:underline">{points}</Link> : points}
            </Td>;
          })}
        </tr>)}
          {!data.rows.length && <tr><Td colSpan={columns.length + 3} className="py-8 text-center! text-muted">Không có sinh viên phù hợp.</Td></tr>}
        </tbody>
      </TableFrame>
      <Pagination page={page - 1} pageSize={20} total={data.total} itemLabel="sinh viên" onPageChange={next => setPage(String(next + 1))} />
    </>}
    {exporting && data && <GradebookExportDialog classId={classId} assignments={assignments} initialAssignmentId={selectedId} onClose={() => setExporting(false)} />}
  </div>;
}
