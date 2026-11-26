"use client";

import { useUrlState } from "@/shared/navigation/useUrlState";
import { Button, Card, ClickableRow, Icon, PageHeader, Pagination, Pill, Segmented, TableFrame, Td, TermFilter, TextInput, Th } from "@/shared/ui";
import { listTermOptions } from "../model/term";
import { CLASS_STATUS_LABELS, type ClassStatus, type CourseClassView, type SubjectView } from "../model/types";
import { msg } from "@/i18n/msg";

const PAGE_SIZE = 20;
const TABS: { value: ClassStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: msg("academics.pages.ClassListPage.all") },
  { value: "OPEN", label: CLASS_STATUS_LABELS.OPEN },
  { value: "ARCHIVED", label: CLASS_STATUS_LABELS.ARCHIVED },
];
const TONES: Record<ClassStatus, "success" | "warning" | "danger"> = { OPEN: "success", DRAFT: "warning", ARCHIVED: "danger" };

/**
 * Lớp của môn (Chủ nhiệm môn), cùng bố cục tab Lớp học ở Chi tiết môn của Admin: một thẻ có tab trạng thái và ô tìm theo
 * mã hoặc tên lớp, bảng tiêu đề tô nền; khác ở chỗ bấm dòng để mở chi tiết lớp. Môn là môn đang chọn trên sidebar
 * (`?subjectId=`), không đổi môn ngay trên trang; Tạo lớp ở đầu trang. Ô Học kỳ (URL `classTerm`) lấy danh sách từ mọi lớp
 * của môn, không phụ thuộc tab; giá trị lạ coi như Tất cả (D-U04-05, FD Q4).
 */
export function ClassListPage({ classes, subjects, initialSubjectId, onOpen, onCreate }: { classes: CourseClassView[]; subjects: SubjectView[]; initialSubjectId?: string; onOpen: (id: string) => void; onCreate: (subject: SubjectView) => void }) {
  const subjectId = initialSubjectId ?? subjects[0]?.id ?? "";
  const [status, setStatus] = useUrlState<ClassStatus | "ALL">("classStatus", "ALL", ["ALL", "OPEN", "ARCHIVED"]);
  const [query, setQuery] = useUrlState<string>("classQuery", "", undefined, "replace");
  const [pageValue, setPageValue] = useUrlState<string>("classPage", "0", undefined, "replace");
  const page = /^\d+$/.test(pageValue) && Number.isSafeInteger(Number(pageValue)) ? Number(pageValue) : 0;
  // Đổi trang bằng giá trị chuỗi dùng trong trạng thái URL.
  const setPage = (page: number) => setPageValue(String(page));
  const subjectClasses = classes.filter((course) => course.subject.id === subjectId);
  const terms = listTermOptions(subjectClasses);
  const [termValue, setTerm] = useUrlState<string>("classTerm", "", undefined, "replace");
  const term = terms.includes(termValue) ? termValue : "";
  const selected = subjects.find((subject) => subject.id === subjectId);
  const q = query.trim().toLocaleLowerCase("vi");
  const filtered = subjectClasses.filter((course) => (!term || course.term === term)
    && (status === "ALL" || course.status === status)
    && (!q || course.code.toLocaleLowerCase("vi").includes(q)));
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const current = Math.min(page, totalPages - 1);
  const rows = filtered.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE);

  return <>
    <PageHeader title={msg("academics.pages.ClassListPage.classes")} actions={
      <Button variant="primary" icon="add" disabled={!selected} onClick={() => selected && onCreate(selected)} data-testid="class-create">{msg("academics.pages.ClassListPage.createClass")}</Button>
    } />
    <Card as="section" padding="lg" className="flex flex-col gap-3.5" aria-label={msg("academics.pages.ClassListPage.classList")}>
      <div className="flex flex-wrap items-center gap-3">
        <Segmented
          as="tab"
          ariaLabel={msg("academics.pages.ClassListPage.classStatus")}
          value={status}
          onChange={(value) => { setStatus(value); setPage(0); }}
          options={TABS.map((tab) => ({ ...tab, testId: `class-filter-${tab.value}` }))}
        />
        <div className="w-full sm:w-44"><TermFilter options={terms} value={term} onChange={(value) => { setTerm(value); setPage(0); }} testId="class-term-filter" /></div>
        <div role="search" className="ml-auto w-full sm:w-72">
          <TextInput type="search" icon="search" aria-label={msg("academics.pages.ClassListPage.filterByClassCode")} placeholder={msg("academics.pages.ClassListPage.filterByClassCode")}
            value={query} maxLength={200} data-testid="class-search" onChange={(event) => { setQuery(event.target.value); setPage(0); }} />
        </div>
      </div>
      <TableFrame filledHead className="[&_table]:min-w-[640px] [&_table]:table-fixed">
        <colgroup><col className="w-[28%]" /><col className="w-[110px]" /><col /><col className="w-[130px]" /><col className="w-[48px]" /></colgroup><thead><tr><Th>{msg("academics.pages.ClassListPage.classCode")}</Th><Th>{msg("academics.pages.ClassListPage.term")}</Th><Th>{msg("academics.pages.ClassListPage.lecturer")}</Th><Th>{msg("academics.pages.ClassListPage.status")}</Th><Th className="w-10"><span className="sr-only">{msg("academics.pages.ClassListPage.details")}</span></Th></tr></thead>
        <tbody>
          {rows.map((course) => <ClickableRow key={course.id} onClick={() => onOpen(course.id)}>
            <Td><button type="button" aria-label={msg("academics.pages.ClassListPage.viewClass", { code: String(course.code) })} data-testid={`class-open-${course.id}`}
              onClick={(event) => { event.stopPropagation(); onOpen(course.id); }} className="text-left font-mono font-semibold text-ink hover:text-primary-ink">{course.code}</button></Td>
            <Td className="font-mono text-body">{course.term}</Td>
            <Td>{course.teacher?.name ?? <Pill>{msg("academics.pages.ClassListPage.notAssigned")}</Pill>}</Td>
            <Td><Pill tone={TONES[course.status]}>{CLASS_STATUS_LABELS[course.status]}</Pill></Td>
            <Td className="text-faint"><Icon name="chevron_right" size={18} /></Td>
          </ClickableRow>)}
          {rows.length === 0 && <tr><Td colSpan={5} className="py-10 text-center! text-muted">{msg("academics.pages.ClassListPage.noMatchingClasses")}</Td></tr>}
        </tbody>
      </TableFrame>
      <Pagination page={current} pageSize={PAGE_SIZE} total={filtered.length} onPageChange={setPage} summary={false} />
    </Card>
  </>;
}
