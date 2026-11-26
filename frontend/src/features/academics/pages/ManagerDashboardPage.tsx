"use client";

import { useUrlState } from "@/shared/navigation/useUrlState";
import { PageHeader, Section, Segmented } from "@/shared/ui";
import type { ManagedSubject } from "../api";
import { CourseCard, CourseGrid } from "../components/CourseCard";
import { SubjectClassesOverview } from "../components/SubjectClassesOverview";
import { msg } from "@/i18n/msg";

type SubjectTab = "ACTIVE" | "ARCHIVED";

/**
 * Môn được phân công (Chủ nhiệm môn): tab Hoạt động / Lưu trữ theo trạng thái môn, lưới thẻ dọc chỉ có mã môn ở trên và tên
 * môn ở dưới; bấm thẻ để mở Thông tin môn.
 */
export function ManagerDashboardPage({ subjects, onSubject }: { subjects: ManagedSubject[]; onSubject: (id: string) => void }) {
  const [tab, setTab] = useUrlState<SubjectTab>("subjectStatus", "ACTIVE", ["ACTIVE", "ARCHIVED"]);
  const archived = subjects.filter((subject) => subject.status === "ARCHIVED").length;
  // Đếm môn lưu trữ hoặc nhóm còn lại để hiển thị bộ lọc.
  const count = (value: SubjectTab) => (value === "ARCHIVED" ? archived : subjects.length - archived);
  const shown = subjects.filter((subject) => (subject.status === "ARCHIVED") === (tab === "ARCHIVED"));
  return <>
    <PageHeader title={msg("academics.pages.ManagerDashboardPage.assignedSubjects")} />
    <Segmented as="tab" ariaLabel={msg("academics.pages.ManagerDashboardPage.filterSubjects")} value={tab} onChange={setTab} className="self-start"
      options={([{ value: "ACTIVE", label: msg("academics.pages.ManagerDashboardPage.active") }, { value: "ARCHIVED", label: msg("academics.pages.ManagerDashboardPage.archived") }] as const).map((option) => ({
        value: option.value, label: <>{option.label}<span className="ml-1 tabular-nums text-muted">{count(option.value)}</span></>,
      }))} />
    <CourseGrid empty={shown.length === 0 ? (tab === "ARCHIVED" ? msg("academics.pages.ManagerDashboardPage.noArchivedSubjectsYet") : msg("academics.pages.ManagerDashboardPage.youHaveNotBeenAssigned")) : null}>
      {shown.map((subject) => (
        <CourseCard key={subject.id} code={subject.code} name={subject.name} band={subject.code} icon="menu_book"
          onOpen={() => onSubject(subject.id)} label={msg("academics.pages.ManagerDashboardPage.openSubject", { code: String(subject.code) })} testId={`subject-row-${subject.id}`} />
      ))}
    </CourseGrid>
  </>;
}

/** Số lớp theo trạng thái do API môn trả (`classCounts`), không cần tải danh sách lớp. */
function describeClassCounts(subject: ManagedSubject) {
  const open = subject.classCounts?.OPEN ?? 0;
  const archived = subject.classCounts?.ARCHIVED ?? 0;
  return archived > 0 ? msg("academics.pages.ManagerDashboardPage.openArchived", { open: String(open), archived: String(archived) }) : msg("academics.pages.ManagerDashboardPage.open", { open: String(open) });
}

/**
 * Thông tin môn của Chủ nhiệm môn, cùng bố cục Thông tin lớp của Giảng viên: thẻ chỉ xem (mã môn, tên môn, số lớp, trạng
 * thái, mô tả) rồi Tổng quan lớp của môn bên dưới. Đổi môn bằng ô chọn môn trên sidebar.
 */
export function MySubjectPage({ subject }: { subject: ManagedSubject }) {
  const description = subject.description?.trim() ?? "";
  return <>
    <PageHeader title={msg("academics.pages.ManagerDashboardPage.subjectInformation")} />
    <div className="flex flex-col gap-4">
      <Section title={msg("academics.pages.ManagerDashboardPage.subjectInformation")}>
        <dl className="grid min-w-0 gap-x-6 gap-y-4 sm:grid-cols-2 lg:grid-cols-4">
          {[
            [msg("academics.pages.ManagerDashboardPage.subjectCode"), subject.code],
            [msg("academics.pages.ManagerDashboardPage.subjectName"), subject.name],
            [msg("academics.pages.ManagerDashboardPage.classes"), describeClassCounts(subject)],
            [msg("academics.pages.ManagerDashboardPage.status"), subject.status === "ARCHIVED" ? msg("academics.pages.ManagerDashboardPage.archived") : msg("academics.pages.ManagerDashboardPage.active")],
          ].map(([label, content]) => (
            <div key={label} className="min-w-0">
              <dt className="mb-1 text-xs text-muted">{label}</dt>
              <dd className="break-words text-sm font-medium text-ink">{content}</dd>
            </div>
          ))}
        </dl>
        {description && <p className="text-sm whitespace-pre-line text-body [overflow-wrap:anywhere]">{description}</p>}
      </Section>
      <SubjectClassesOverview subjectId={subject.id} />
    </div>
  </>;
}
