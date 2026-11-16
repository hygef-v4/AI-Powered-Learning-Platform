"use client";

import Link from "next/link";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { useRouter, useSearchParams } from "next/navigation";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, Card, ClassFilter, ClickableRow, cx, Icon, PageHeader, Pagination, Pill, Segmented, Select, TableFrame, TableSkeletonRows, Td, TextInput, Th } from "@/shared/ui";
import { compareTermsNewestFirst } from "@/features/academics/model/term";
import { formatTimeUntil } from "@/shared/dateTime";
import { errorMessage, formatDateTime } from "../model/format";
import { studentAssignmentResults } from "../slots";
import type { StudentRow } from "../model/types";
import { doing, isClosedUnsubmitted, rowStatus, upcoming, useStudentAssignments } from "../hooks/useStudentAssignments";
import { msg } from "@/i18n/msg";

type Tab = "upcoming" | "doing" | "submitted" | "closed";

const PAGE_SIZE = 20;
const TABS: { value: Tab; label: string }[] = [
  { value: "upcoming", label: msg("assignmentwork.pages.StudentAssignmentsPage.chuaLam") },
  { value: "doing", label: msg("assignmentwork.pages.StudentAssignmentsPage.dangLam") },
  { value: "submitted", label: msg("assignmentwork.pages.StudentAssignmentsPage.daNop") },
  { value: "closed", label: msg("assignmentwork.pages.StudentAssignmentsPage.daDong") },
];

type Kind = "INDIVIDUAL" | "GROUP" | "PRACTICE";
const KIND_LABELS: Record<Kind, string> = { INDIVIDUAL: msg("assignmentwork.pages.StudentAssignmentsPage.caNhan"), GROUP: msg("assignmentwork.pages.StudentAssignmentsPage.nhom"), PRACTICE: msg("assignmentwork.pages.StudentAssignmentsPage.luyenTap") };

/** Loại bài hiển thị: bài luyện tập (không chấm điểm) là một loại riêng, còn lại theo cá nhân hay nhóm. */
function kindOf(row: StudentRow): Kind {
  return row.gradingMode === "PRACTICE" ? "PRACTICE" : row.submissionMode;
}

/** Thời gian còn lại tới hạn nộp, dưới ngày giờ; còn dưới 24 giờ thì màu cảnh báo. */
function DueIn({ closesAt }: { closesAt: string | null }) {
  const due = formatTimeUntil(closesAt);
  if (!due) return null;
  return (
    <span className={cx("block text-xs", due.urgent ? "font-semibold text-warning" : "text-muted")} data-testid="assignment-due-in">
      {due.text}
    </span>
  );
}

/** Mở bài từ trang chung thì đánh dấu `from=all` để nút quay lại về trang chung, không về trang bài tập của lớp. */
function detailHref(row: StudentRow, general: boolean) {
  return `/my-assignments/${encodeURIComponent(row.assignmentId)}?classId=${encodeURIComponent(row.classId)}${general ? "&from=all" : ""}`;
}

/**
 * #38 Student Assignments (F1, UC 37): bài của các lớp đang học (hoặc một lớp khi mở từ sidebar của lớp, `?classId=`), cùng bố
 * cục bảng Lớp học: một thẻ có tab Chưa làm / Đang làm / Đã nộp / Đã đóng, ô lọc loại bài (Cá nhân / Nhóm / Luyện tập) và ô tìm theo tên bài; bảng tiêu đề tô nền,
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
  const [chosenTab, setTab] = useUrlState<Tab | "">("tab", "", ["", "upcoming", "doing", "submitted", "closed"]);
  // `?mode=GROUP` (link Bài nhóm trên trang Nhóm của tôi) mở sẵn ô lọc loại bài.
  const [mode, setMode] = useUrlState<string>("mode", "", ["", "INDIVIDUAL", "GROUP", "PRACTICE"], "replace");
  const [query, setQuery] = useUrlState<string>("q", "", undefined, "replace");
  const [pageValue, setPageValue] = useUrlState<string>("page", "0", undefined, "replace");
  const page = /^\d+$/.test(pageValue) && Number.isSafeInteger(Number(pageValue)) ? Number(pageValue) : 0;
  const setPage = (value: number) => setPageValue(String(value));
  // Trang chung (không có `?classId=`) lọc theo lớp ngay trên trang, không đổi sang sidebar của lớp.
  const [classFilter, setClassFilter] = useUrlState<string>("classFilter", "", undefined, "replace");
  const { rows: allRows, error, loading } = useStudentAssignments(classId);
  const classes = [...new Map((allRows ?? []).map((row) => [row.classId, { classId: row.classId, code: row.classCode, subjectCode: row.subjectCode, term: row.classTerm ?? null }])).values()]
    .sort((a, b) => a.code.localeCompare(b.code, "vi") || compareTermsNewestFirst(a.term ?? "", b.term ?? ""));
  const rows = allRows && classFilter ? allRows.filter((row) => row.classId === classFilter) : allRows;

  const inTab = (row: StudentRow, key: Tab) => {
    // Mỗi bài một tab: đang làm (kể cả sửa để nộp lại) thì chỉ ở Đang làm, không ở Đã nộp.
    if (key === "closed") return isClosedUnsubmitted(row);
    return key === "upcoming" ? upcoming(row) && !doing(row) : key === "doing" ? doing(row) : !doing(row) && (row.submittedCount > 0 || !!row.group?.submitted);
  };
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
      <PageHeader title={msg("assignmentwork.pages.StudentAssignmentsPage.baiTap")} actions={!classId && (
        <ClassFilter classes={classes} value={classFilter} onChange={(value) => { setClassFilter(value); setPage(0); }} testId="assignment-class-filter" />
      )} />
      {error !== null && <Alert tone="danger" role="alert">{errorMessage(error)}</Alert>}
      <Card as="section" padding="lg" className="flex flex-col gap-3.5" aria-label={msg("assignmentwork.pages.StudentAssignmentsPage.danhSachBaiTap")}>
        <div className="flex flex-wrap items-center gap-3">
          <Segmented
            as="tab"
            ariaLabel={msg("assignmentwork.pages.StudentAssignmentsPage.nhomBai")}
            value={tab}
            onChange={(value) => { setTab(value); setPage(0); }}
            options={TABS.map((item) => ({ ...item, count: rows ? count(item.value) : undefined }))}
          />
          <Select aria-label={msg("assignmentwork.pages.StudentAssignmentsPage.loaiBai")} value={mode} wrapperClassName="ml-auto w-40" onChange={(e) => { setMode(e.target.value); setPage(0); }}>
            <option value="">{msg("assignmentwork.pages.StudentAssignmentsPage.moiLoai")}</option>
            {(Object.keys(KIND_LABELS) as Kind[]).map((kind) => <option key={kind} value={kind}>{KIND_LABELS[kind]}</option>)}
          </Select>
          <div role="search" className="w-full sm:w-64">
            <TextInput type="search" icon="search" aria-label={msg("assignmentwork.pages.StudentAssignmentsPage.timBaiTap")} placeholder={msg("assignmentwork.pages.StudentAssignmentsPage.timBaiTap")} value={query} maxLength={200}
              onChange={(e) => { setQuery(e.target.value); setPage(0); }} />
          </div>
        </div>
        {/* Cột cố định độ rộng để hai tab Chưa làm / Đã nộp thẳng cột nhau; trên điện thoại mỗi bài là một thẻ. */}
        <TableFrame filledHead stackOnMobile className="sm:[&_table]:min-w-[1000px] [&_table]:table-fixed">
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
              <Th>{msg("assignmentwork.pages.StudentAssignmentsPage.baiTap")}</Th><Th>{msg("assignmentwork.pages.StudentAssignmentsPage.lop")}</Th><Th>{msg("assignmentwork.pages.StudentAssignmentsPage.monHoc")}</Th><Th>{msg("assignmentwork.pages.StudentAssignmentsPage.loai")}</Th><Th>{msg("assignmentwork.pages.StudentAssignmentsPage.hanNop")}</Th><Th>{msg("assignmentwork.pages.StudentAssignmentsPage.trangThai")}</Th>{tab === "submitted" && <Th>{msg("assignmentwork.pages.StudentAssignmentsPage.diem")}</Th>}
              <Th><span className="sr-only">{msg("assignmentwork.pages.StudentAssignmentsPage.moBai")}</span></Th>
            </tr>
          </thead>
          <tbody>
            {shown.map((row) => (
              <ClickableRow key={`${row.classId}-${row.assignmentId}`} onClick={() => router.push(detailHref(row, !classId))} data-testid={`student-row-${row.assignmentId}`}>
                <Td>
                  <Link href={detailHref(row, !classId)} onClick={(e) => e.stopPropagation()} className="block truncate font-semibold text-ink hover:text-primary-ink">{row.title}</Link>
                </Td>
                <Td label={msg("assignmentwork.pages.StudentAssignmentsPage.lop")} className="truncate" title={row.classCode || undefined}>{row.classCode || "—"}</Td>
                <Td label={msg("assignmentwork.pages.StudentAssignmentsPage.monHoc")} className="truncate" title={row.subjectCode || undefined}>{row.subjectCode || "—"}</Td>
                <Td label={msg("assignmentwork.pages.StudentAssignmentsPage.loai")} className="whitespace-nowrap">{KIND_LABELS[kindOf(row)]}</Td>
                <Td label={msg("assignmentwork.pages.StudentAssignmentsPage.hanNop")} className="whitespace-nowrap">
                  <span className="font-mono text-xs text-muted">{formatDateTime(row.closesAt)}</span>
                  {(tab === "upcoming" || tab === "doing") && <DueIn closesAt={row.closesAt} />}
                </Td>
                <Td label={msg("assignmentwork.pages.StudentAssignmentsPage.trangThai")}><Pill tone={rowStatus(row).tone}>{rowStatus(row).label}</Pill></Td>
                {tab === "submitted" && (
                  <Td label={msg("assignmentwork.pages.StudentAssignmentsPage.diem")}>
                    {row.gradingMode === "GRADED" ? <SlotOutlet slot={studentAssignmentResults} props={{ assignmentId: row.assignmentId, classId: row.classId }} /> : <span className="text-muted">—</span>}
                  </Td>
                )}
                <Td hideWhenStacked className="text-faint"><Icon name="chevron_right" size={18} /></Td>
              </ClickableRow>
            ))}
            {loading && !rows && <TableSkeletonRows columns={columnCount} label={msg("assignmentwork.pages.StudentAssignmentsPage.dangTaiBaiTap")} />}
            {rows && shown.length === 0 && (
              <tr><Td colSpan={columnCount} className="py-10 text-center! text-muted">{tab === "upcoming" ? msg("assignmentwork.pages.StudentAssignmentsPage.khongConBaiChuaLam") : tab === "doing" ? msg("assignmentwork.pages.StudentAssignmentsPage.khongCoBaiDangLam") : tab === "closed" ? msg("assignmentwork.pages.StudentAssignmentsPage.khongCoBaiDaDongChua") : msg("assignmentwork.pages.StudentAssignmentsPage.chuaNopBaiNao")}</Td></tr>
            )}
          </tbody>
        </TableFrame>
        <Pagination page={current} pageSize={PAGE_SIZE} total={visible.length} onPageChange={setPage} summary={false} />
      </Card>
    </>
  );
}
