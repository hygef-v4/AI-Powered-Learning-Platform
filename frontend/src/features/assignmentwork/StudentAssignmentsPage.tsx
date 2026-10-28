"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, ClickableRow, Icon, PageHeader, PageNav, Pill, Select, TableFrame, Td, Th, cx } from "@/shared/ui";
import { errorMessage, formatDateTime } from "./format";
import { studentAssignmentResults } from "./slots";
import type { StudentRow } from "./types";
import { doing, rowStatus, upcoming, useStudentAssignments } from "./useStudentAssignments";

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

/** Số lượt còn được làm của bài; bài không ghi số lượt tối đa thì được một lượt (như khi bắt đầu làm). */
function attemptsLeft(row: StudentRow) {
  return `Còn ${Math.max(0, (row.maxAttempts ?? 1) - row.attemptCount)} lượt`;
}

/** Mở bài từ trang chung thì đánh dấu `from=all` để nút quay lại về trang chung, không về trang bài tập của lớp. */
function detailHref(row: StudentRow, general: boolean) {
  return `/my-assignments/${encodeURIComponent(row.assignmentId)}?classId=${encodeURIComponent(row.classId)}${general ? "&from=all" : ""}`;
}

/**
 * #38 Student Assignments (F1, UC 37): bài của các lớp đang học (hoặc một lớp khi mở từ sidebar của lớp, `?classId=`), cùng bố
 * cục bảng Lớp của môn: một thẻ có tab Chưa làm / Đang làm / Đã nộp và ô lọc loại bài (Cá nhân / Nhóm / Luyện tập); bảng tiêu đề tô nền,
 * bấm dòng để mở bài. Các tab cùng độ rộng cột; cột thứ năm là Lượt làm (số lượt còn lại), riêng Đã nộp là Điểm. Bài đang làm
 * (kể cả bài nhóm nhóm đang làm) có nhãn Đang làm màu vàng; bài nhóm hiện trạng thái theo tiến độ của nhóm;
 * điểm do U09 gắn.
 */
export function StudentAssignmentsPage() {
  const router = useRouter();
  const params = useSearchParams();
  const classId = params.get("classId") ?? undefined;
  // Quay về từ một bài (`focus`): mở tab đang chứa bài đó theo dữ liệu mới tải; bấm tab thì theo lựa chọn.
  const focus = params.get("focus");
  const [chosenTab, setTab] = useState<Tab | null>(null);
  const [mode, setMode] = useState("");
  const [page, setPage] = useState(0);
  const { rows, error, loading } = useStudentAssignments(classId);

  const inTab = (row: StudentRow, key: Tab) =>
    key === "upcoming" ? upcoming(row) && !doing(row) : key === "doing" ? doing(row) : row.submittedCount > 0 || !!row.group?.submitted;
  const focused = focus ? rows?.find((row) => row.assignmentId === focus) : undefined;
  const tab: Tab = chosenTab ?? TABS.find((item) => focused && inTab(focused, item.value))?.value ?? "upcoming";
  const visible = (rows ?? []).filter((row) => inTab(row, tab) && (!mode || kindOf(row) === mode));
  const count = (key: Tab) => (rows ?? []).filter((row) => inTab(row, key)).length;
  const totalPages = Math.max(1, Math.ceil(visible.length / PAGE_SIZE));
  const current = Math.min(page, totalPages - 1);
  const shown = visible.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE);

  return (
    <>
      <PageHeader title="Bài tập" subtitle={rows ? `${count("upcoming")} bài chưa làm` : undefined} />
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
        </div>
        {/* Cột cố định độ rộng để hai tab Chưa làm / Đã nộp thẳng cột nhau. */}
        <TableFrame filledHead className="[&_table]:min-w-[760px] [&_table]:table-fixed">
          <colgroup>
            <col />
            <col className="w-[110px]" />
            <col className="w-[150px]" />
            <col className="w-[130px]" />
            <col className="w-[140px]" />
            <col className="w-[48px]" />
          </colgroup>
          <thead>
            <tr>
              <Th>Bài tập</Th><Th>Loại</Th><Th>Hạn nộp</Th><Th>Trạng thái</Th><Th>{tab === "submitted" ? "Điểm" : "Lượt làm"}</Th>
              <Th><span className="sr-only">Mở bài</span></Th>
            </tr>
          </thead>
          <tbody>
            {shown.map((row) => (
              <ClickableRow key={`${row.classId}-${row.assignmentId}`} onClick={() => router.push(detailHref(row, !classId))} data-testid={`student-row-${row.assignmentId}`}>
                <Td>
                  <Link href={detailHref(row, !classId)} onClick={(e) => e.stopPropagation()} className="block truncate font-semibold text-ink hover:text-primary-ink">{row.title}</Link>
                </Td>
                <Td className="whitespace-nowrap">{KIND_LABELS[kindOf(row)]}</Td>
                <Td className="whitespace-nowrap">
                  <span className="font-mono text-xs text-muted">{formatDateTime(row.closesAt)}</span>
                </Td>
                <Td><Pill tone={rowStatus(row).tone}>{rowStatus(row).label}</Pill></Td>
                {tab !== "submitted" ? (
                  <Td className="whitespace-nowrap">{attemptsLeft(row)}</Td>
                ) : (
                  <Td>
                    {row.gradingMode === "GRADED" ? <SlotOutlet slot={studentAssignmentResults} props={{ assignmentId: row.assignmentId, classId: row.classId }} /> : <span className="text-muted">—</span>}
                  </Td>
                )}
                <Td className="text-faint"><Icon name="chevron_right" size={18} /></Td>
              </ClickableRow>
            ))}
            {loading && !rows && <tr><Td colSpan={6} className="py-10 text-center! text-muted"><span role="status">Đang tải bài tập…</span></Td></tr>}
            {rows && shown.length === 0 && (
              <tr><Td colSpan={6} className="py-10 text-center! text-muted">{tab === "upcoming" ? "Không còn bài chưa làm." : tab === "doing" ? "Không có bài đang làm." : "Chưa nộp bài nào."}</Td></tr>
            )}
          </tbody>
        </TableFrame>
        <PageNav page={current} size={PAGE_SIZE} total={visible.length} onPage={setPage} />
      </section>
    </>
  );
}
