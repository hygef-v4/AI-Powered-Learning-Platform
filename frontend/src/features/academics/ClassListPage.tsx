"use client";

import { useState } from "react";
import { Button, ClickableRow, Icon, PageHeader, PageNav, Pill, TableFrame, Td, TextInput, Th, cx } from "@/shared/ui";
import { statusLabels, type ClassStatus, type CourseClassView, type SubjectView } from "./types";

const PAGE_SIZE = 20;
const TABS: { value: ClassStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "Tất cả" },
  { value: "OPEN", label: statusLabels.OPEN },
  { value: "ARCHIVED", label: statusLabels.ARCHIVED },
];
const TONES: Record<ClassStatus, "success" | "warning" | "danger"> = { OPEN: "success", DRAFT: "warning", ARCHIVED: "danger" };

/**
 * Lớp của môn (Chủ nhiệm môn), cùng bố cục tab Lớp học ở Chi tiết môn của Admin: một thẻ có tab trạng thái và ô tìm theo
 * mã hoặc tên lớp, bảng tiêu đề tô nền; khác ở chỗ bấm dòng để mở chi tiết lớp. Môn là môn đang chọn trên sidebar
 * (`?subjectId=`), không đổi môn ngay trên trang; Tạo lớp ở đầu trang.
 */
export function ClassListPage({ classes, subjects, initialSubjectId, onOpen, onCreate }: { classes: CourseClassView[]; subjects: SubjectView[]; initialSubjectId?: string; onOpen: (id: string) => void; onCreate: (subject: SubjectView) => void }) {
  const subjectId = initialSubjectId ?? subjects[0]?.id ?? "";
  const [status, setStatus] = useState<ClassStatus | "ALL">("ALL");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const selected = subjects.find((subject) => subject.id === subjectId);
  const q = query.trim().toLocaleLowerCase("vi");
  const filtered = classes.filter((course) => course.subject.id === subjectId
    && (status === "ALL" || course.status === status)
    && (!q || `${course.code} ${course.name}`.toLocaleLowerCase("vi").includes(q)));
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const current = Math.min(page, totalPages - 1);
  const rows = filtered.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE);

  return <>
    <PageHeader title="Lớp học" actions={
      <Button variant="primary" icon="add" disabled={!selected} onClick={() => selected && onCreate(selected)} data-testid="class-create">Tạo lớp</Button>
    } />
    <section aria-label="Danh sách lớp" className="flex flex-col gap-3.5 rounded-xl border border-line bg-surface p-5">
      <div className="flex flex-wrap items-center gap-3">
        <div role="tablist" aria-label="Trạng thái lớp" className="flex flex-wrap gap-1 rounded-xl bg-shell p-1">
          {TABS.map((tab) => <button key={tab.value} type="button" role="tab" aria-selected={status === tab.value}
            data-testid={`class-filter-${tab.value}`} onClick={() => { setStatus(tab.value); setPage(0); }}
            className={cx("rounded-[7px] px-2.5 py-1.5 text-xs font-semibold transition-colors duration-200",
              status === tab.value ? "bg-surface text-ink shadow-sm" : "text-muted hover:text-ink")}>
            {tab.label}
          </button>)}
        </div>
        <div role="search" className="ml-auto w-full sm:w-72">
          <TextInput type="search" icon="search" aria-label="Lọc theo mã hoặc tên lớp" placeholder="Lọc theo mã hoặc tên lớp"
            value={query} maxLength={200} data-testid="class-search" onChange={(event) => { setQuery(event.target.value); setPage(0); }} />
        </div>
      </div>
      <TableFrame filledHead className="[&_table]:min-w-[720px] [&_table]:table-fixed">
        <colgroup><col className="w-[130px]" /><col /><col className="w-[110px]" /><col className="w-[24%]" /><col className="w-[130px]" /><col className="w-[48px]" /></colgroup><thead><tr><Th>Mã lớp</Th><Th>Tên lớp</Th><Th>Học kỳ</Th><Th>Giảng viên</Th><Th>Trạng thái</Th><Th className="w-10"><span className="sr-only">Chi tiết</span></Th></tr></thead>
        <tbody>
          {rows.map((course) => <ClickableRow key={course.id} onClick={() => onOpen(course.id)}>
            <Td className="font-mono font-semibold">{course.code}</Td>
            <Td><button type="button" aria-label={`Xem lớp ${course.code}`} data-testid={`class-open-${course.id}`}
              onClick={(event) => { event.stopPropagation(); onOpen(course.id); }} className="text-left font-semibold text-ink hover:text-primary-ink">{course.name}</button></Td>
            <Td>{course.term}</Td>
            <Td>{course.teacher?.name ?? <Pill>Chưa phân công</Pill>}</Td>
            <Td><Pill tone={TONES[course.status]}>{statusLabels[course.status]}</Pill></Td>
            <Td className="text-faint"><Icon name="chevron_right" size={18} /></Td>
          </ClickableRow>)}
          {rows.length === 0 && <tr><Td colSpan={6} className="py-10 text-center! text-muted">Không có lớp phù hợp.</Td></tr>}
        </tbody>
      </TableFrame>
      <PageNav page={current} size={PAGE_SIZE} total={filtered.length} onPage={setPage} />
    </section>
  </>;
}
