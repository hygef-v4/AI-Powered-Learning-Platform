"use client";

import { useUrlState } from "@/shared/navigation/useUrlState";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { PageHeader, Segmented, TermFilter } from "@/shared/ui";
import { CourseCard, CourseGrid } from "../components/CourseCard";
import { classCardSections } from "../slots";
import type { ClassStatus, CourseClassView } from "../model/types";
import { compareTermsNewestFirst, listTermOptions } from "../model/term";
import { msg } from "@/i18n/msg";

/**
 * Lớp của tôi (Sinh viên) và Lớp được phân công (Giảng viên): tab theo trạng thái lớp rồi lưới thẻ dọc, mỗi thẻ chỉ có mã lớp
 * ở trên và tên môn ở dưới; sinh viên thấy thêm số bài cần làm. Tab Sắp mở chỉ hiện khi còn lớp nháp; sinh viên không vào được
 * lớp đã kết thúc. Ô Học kỳ (URL `classTerm`) lấy danh sách từ mọi lớp của người dùng; số trên tab tính theo học kỳ đang
 * chọn; thẻ ghi `mã môn · học kỳ` (D-U04-05, FD Q4).
 */
export function ClassDashboardPage({ audience, classes, onOpen }: { audience: "student" | "teacher"; classes: CourseClassView[]; onOpen: (id: string) => void }) {
  const [status, setStatus] = useUrlState<ClassStatus>("classStatus", "OPEN", audience === "student" ? ["OPEN", "ARCHIVED"] : ["OPEN", "DRAFT", "ARCHIVED"]);
  const terms = listTermOptions(classes);
  const [termValue, setTerm] = useUrlState<string>("classTerm", "", undefined, "replace");
  const term = terms.includes(termValue) ? termValue : "";
  const inTerm = term ? classes.filter((course) => course.term === term) : classes;
  const student = audience === "student";
  const count = (value: ClassStatus) => inTerm.filter((course) => course.status === value).length;
  const tabs: { value: ClassStatus; label: string }[] = student
    ? [{ value: "OPEN", label: msg("academics.pages.ClassDashboardPage.dangHoc") }, { value: "ARCHIVED", label: msg("academics.pages.ClassDashboardPage.daKetThuc") }]
    : [...(classes.some((course) => course.status === "DRAFT") ? [{ value: "DRAFT" as const, label: msg("academics.pages.ClassDashboardPage.sapMo") }] : []), { value: "OPEN", label: msg("academics.pages.ClassDashboardPage.dangDay") }, { value: "ARCHIVED", label: msg("academics.pages.ClassDashboardPage.ketThuc") }];
  const visible = inTerm.filter((course) => course.status === status)
    .sort((a, b) => a.code.localeCompare(b.code) || compareTermsNewestFirst(a.term, b.term));
  const empty = status === "ARCHIVED" ? msg("academics.pages.ClassDashboardPage.chuaCoLopDaKetThuc") : student ? msg("academics.pages.ClassDashboardPage.banChuaHocLopNao") : msg("academics.pages.ClassDashboardPage.chuaCoLopTrongMucNay");

  return <>
    <PageHeader title={student ? msg("academics.pages.ClassDashboardPage.lopCuaToi") : msg("academics.pages.ClassDashboardPage.lopDuocPhanCong")} />
    <div className="flex flex-wrap items-center gap-3">
    <Segmented as="tab" ariaLabel={msg("academics.pages.ClassDashboardPage.locLop")} value={status} onChange={setStatus}
      options={tabs.map((tab) => ({ value: tab.value, label: <>{tab.label}<span className="ml-1 tabular-nums text-muted">{count(tab.value)}</span></> }))} />
      <div className="w-full sm:ml-auto sm:w-44"><TermFilter options={terms} value={term} onChange={setTerm} testId="class-term-filter" /></div>
    </div>
    <CourseGrid empty={visible.length === 0 ? empty : null}>
      {visible.map((course) => {
        const openable = !(student && course.status === "ARCHIVED");
        return (
          <CourseCard key={course.id} code={`${course.subject.code} · ${course.term}`} name={course.code} band={course.subject.code} icon="class"
            extra={student && course.status === "OPEN" ? <SlotOutlet slot={classCardSections} props={{ classId: course.id }} /> : undefined}
            onOpen={openable ? () => onOpen(course.id) : undefined} label={msg("academics.pages.ClassDashboardPage.xemLop", { code: String(course.code) })} testId={`dashboard-open-${course.id}`} />
        );
      })}
    </CourseGrid>
  </>;
}
