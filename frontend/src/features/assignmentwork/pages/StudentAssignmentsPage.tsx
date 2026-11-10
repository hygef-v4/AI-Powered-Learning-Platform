"use client";

import Link from "next/link";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { useRouter, useSearchParams } from "next/navigation";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, ClassFilter, ClickableRow, Icon, PageHeader, PageNav, Pill, Select, TableFrame, Td, TextInput, Th, cx } from "@/shared/ui";
import { errorMessage, formatDateTime } from "../model/format";
import { studentAssignmentResults } from "../slots";
import type { StudentRow } from "../model/types";
import { doing, rowStatus, upcoming, useStudentAssignments } from "../hooks/useStudentAssignments";

type Tab = "upcoming" | "doing" | "submitted";

const PAGE_SIZE = 20;
const TABS: { value: Tab; label: string }[] = [
  { value: "upcoming", label: "Chưa làm" },
  { value: "doing", label: "Đang làm" },
  { value: "submitted", label: "Đã nộp" },
];

type Kind = "INDIVIDUAL" | "GROUP" | "PRACTICE";
const KIND_LABELS: Record<Kind, string> = { INDIVIDUAL: "Cá nhân", GROUP: "Nhóm", PRACTICE: "Luyện tập" };

/** Loại bài hiển thị: bài luyện tập (không chấm điểm) là một loại riêng, còn lại theo cá nhân hay nhóm. */
function kindOf(row: StudentRow): Kind {
  return row.gradingMode === "PRACTICE" ? "PRACTICE" : row.submissionMode;
}

/** Mở bài từ trang chung thì đánh dấu `from=all` để nút quay lại về trang chung, không về trang bài tập của lớp. */
function detailHref(row: StudentRow, general: boolean) {
  return `/my-assignments/${encodeURIComponent(row.assignmentId)}?classId=${encodeURIComponent(row.classId)}${general ? "&from=all" : ""}`;
}

/**
 * #38 Student Assignments (F1, UC 37): bài của các lớp đang học (hoặc một lớp khi mở từ sidebar của lớp, `?classId=`), cùng bố
 * cục bảng Lớp học: một thẻ có tab Chưa làm / Đang làm / Đã nộp, ô lọc loại bài (Cá nhân / Nhóm / Luyện tập) và ô tìm theo tên bài; bảng tiêu đề tô nền,
 * bấm dòng để mở bài. Các tab cùng độ rộng cột, hiện lớp và môn học; riêng Đã nộp có thêm Điểm. Bài đang làm
 * (kể cả bài nhóm nhóm đang làm) có nhãn Đang làm màu vàng; bài nhóm hiện trạng thái theo tiến độ của nhóm;
 * điểm do U09 gắn.
 */
export function StudentAssignmentsPage() {
  const router = useRouter();
  const params = useSearchParams();
  const classId = params.get("classId") ?? undefined;
  // Quay về từ một bài (`focus`): mở tab đang chứa bài đó theo dữ liệu mới tải; bấm tab thì theo lựa chọn.
  const focus = params.get("focus");
  const [chosenTab, setTab] = useUrlState<Tab | "">("tab", "", ["", "upcoming", "doing", "submitted"]);
  // `?mode=GROUP` (link Bài nhóm trên trang Nhóm của tôi) mở sẵn ô lọc loại bài.
  const [mode, setMode] = useUrlState<string>("mode", "", ["", "INDIVIDUAL", "GROUP", "PRACTICE"], "replace");
  const [query, setQuery] = useUrlState<string>("q", "", undefined, "replace");
  const [pageValue, setPageValue] = useUrlState<string>("page", "0", undefined, "replace");
  const page = /^\d+$/.test(pageValue) && Number.isSafeInteger(Number(pageValue)) ? Number(pageValue) : 0;
  const setPage = (value: number) => setPageValue(String(value));
  // Trang chung (không có `?classId=`) lọc theo lớp ngay trên trang, không đổi sang sidebar của lớp.
  const [classFilter, setClassFilter] = useUrlState<string>("classFilter", "", undefined, "replace");
  const { rows: allRows, error, loading } = useStudentAssignments(classId);
  const classes = [...new Map((allRows ?? []).map((row) => [row.classId, { classId: row.classId, code: row.classCode, subjectCode: row.subjectCode }])).values()]
    .sort((a, b) => a.code.localeCompare(b.code, "vi"));
  const rows = allRows && classFilter ? allRows.filter((row) => row.classId === classFilter) : allRows;

  const inTab = (row: StudentRow, key: Tab) =>
    key === "upcoming" ? upcoming(row) && !doing(row) : key === "doing" ? doing(row) : row.submittedCount > 0 || !!row.group?.submitted;
  const focused = focus ? rows?.find((row) => row.assignmentId === focus) : undefined;
  const tab: Tab = chosenTab || TABS.find((item) => focused && inTab(focused, item.value))?.value || "upcoming";
  const q = query.toLocaleLowerCase("vi").trim();
  const visible = (rows ?? []).filter((row) => inTab(row, tab) && (!mode || kindOf(row) === mode)
    && row.title.toLocaleLowerCase("vi").includes(q));
  const count = (key: Tab) => (rows ?? []).filter((row) => inTab(row, key)).length;
  const totalPages = Math.max(1, Math.ceil(visible.length / PAGE_SIZE));
  const current = Math.min(page, totalPages - 1);
  const shown = visible.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE);
  const columnCount = tab === "submitted" ? 8 : 7;

  return (
    <>
      <PageHeader title="Bài tập" actions={!classId && (
        <ClassFilter classes={classes} value={classFilter} onChange={(value) => { setClassFilter(value); setPage(0); }} testId="assignment-class-filter" />
      )} />
      {error !== null && <Alert tone="danger" role="alert">{errorMessage(error)}</Alert>}
      <section aria-label="Danh sách bài tập" className="flex flex-col gap-3.5 rounded-xl border border-line bg-surface p-5">
        <div className="flex flex-wrap items-center gap-3">
          <div role="tablist" aria-label="Nhóm bài" className="flex flex-wrap gap-1 rounded-xl bg-shell p-1">
            {TABS.map((item) => (
              <button key={item.value} type="button" role="tab" aria-selected={tab === item.value}
                onClick={() => { setTab(item.value); setPage(0); }}
                className={cx("rounded-[7px] px-2.5 py-1.5 text-xs font-semibold transition-colors duration-200",
                  tab === item.value ? "bg-surface text-ink shadow-sm" : "text-muted hover:text-ink")}>
                {item.label}{rows && <span className="ml-1 tabular-nums text-muted">{count(item.value)}</span>}
              </button>
            ))}
          </div>
          <Select aria-label="Loại bài" value={mode} wrapperClassName="ml-auto w-40" onChange={(e) => { setMode(e.target.value); setPage(0); }}>
            <option value="">Mọi loại</option>
            {(Object.keys(KIND_LABELS) as Kind[]).map((kind) => <option key={kind} value={kind}>{KIND_LABELS[kind]}</option>)}
          </Select>
          <div role="search" className="w-full sm:w-64">
            <TextInput type="search" icon="search" aria-label="Tìm bài tập" placeholder="Tìm bài tập" value={query} maxLength={200}
              onChange={(e) => { setQuery(e.target.value); setPage(0); }} />
          </div>
        </div>
        {/* Cột cố định độ rộng để hai tab Chưa làm / Đã nộp thẳng cột nhau. */}
        <TableFrame filledHead className="[&_table]:min-w-[1000px] [&_table]:table-fixed">
          <colgroup>
            <col />
            <col className="w-[110px]" />
            <col className="w-[110px]" />
            <col className="w-[110px]" />
            <col className="w-[150px]" />
            <col className="w-[130px]" />
            {/* Không còn cột Lượt làm (yêu cầu 2026-11-10): tab Đã nộp mới có cột Điểm. */}
            {tab === "submitted" && <col className="w-[140px]" />}
            <col className="w-[48px]" />
          </colgroup>
          <thead>
            <tr>
              <Th>Bài tập</Th><Th>Lớp</Th><Th>Môn học</Th><Th>Loại</Th><Th>Hạn nộp</Th><Th>Trạng thái</Th>{tab === "submitted" && <Th>Điểm</Th>}
              <Th><span className="sr-only">Mở bài</span></Th>
            </tr>
          </thead>
          <tbody>
            {shown.map((row) => (
              <ClickableRow key={`${row.classId}-${row.assignmentId}`} onClick={() => router.push(detailHref(row, !classId))} data-testid={`student-row-${row.assignmentId}`}>
                <Td>
                  <Link href={detailHref(row, !classId)} onClick={(e) => e.stopPropagation()} className="block truncate font-semibold text-ink hover:text-primary-ink">{row.title}</Link>
                </Td>
                <Td className="truncate" title={row.classCode || undefined}>{row.classCode || "—"}</Td>
                <Td className="truncate" title={row.subjectCode || undefined}>{row.subjectCode || "—"}</Td>
                <Td className="whitespace-nowrap">{KIND_LABELS[kindOf(row)]}</Td>
                <Td className="whitespace-nowrap">
                  <span className="font-mono text-xs text-muted">{formatDateTime(row.closesAt)}</span>
                </Td>
                <Td><Pill tone={rowStatus(row).tone}>{rowStatus(row).label}</Pill></Td>
                {tab === "submitted" && (
                  <Td>
                    {row.gradingMode === "GRADED" ? <SlotOutlet slot={studentAssignmentResults} props={{ assignmentId: row.assignmentId, classId: row.classId }} /> : <span className="text-muted">—</span>}
                  </Td>
                )}
                <Td className="text-faint"><Icon name="chevron_right" size={18} /></Td>
              </ClickableRow>
            ))}
            {loading && !rows && <tr><Td colSpan={columnCount} className="py-10 text-center! text-muted"><span role="status">Đang tải bài tập…</span></Td></tr>}
            {rows && shown.length === 0 && (
              <tr><Td colSpan={columnCount} className="py-10 text-center! text-muted">{tab === "upcoming" ? "Không còn bài chưa làm." : tab === "doing" ? "Không có bài đang làm." : "Chưa nộp bài nào."}</Td></tr>
            )}
          </tbody>
        </TableFrame>
        <PageNav page={current} size={PAGE_SIZE} total={visible.length} onPage={setPage} />
      </section>
    </>
  );
}
