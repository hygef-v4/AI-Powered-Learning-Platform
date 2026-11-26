"use client";

import { useUrlState } from "@/shared/navigation/useUrlState";
import { useSearchParams } from "next/navigation";
import { useCallback, useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { teacherClassTabs } from "../slots";
import { StudentClassPage } from "./StudentClassPage";
import { EnrollmentPanel } from "../components/EnrollmentPanel";
import { EmptyState, PageHeader, Section, Tabs } from "@/shared/ui";
import { CLASS_STATUS_LABELS, type CourseClassView, type EnrollmentView } from "../model/types";

import styles from "../styles/academics.module.css";
import { msg } from "@/i18n/msg";

type ClassTab = "info" | "students" | "materials" | "evals" | "gradebook";
const CLASS_TABS: readonly ClassTab[] = ["info", "students", "materials", "evals", "gradebook"];

// Dựng trang lớp chỉ xem, đồng bộ tab với URL và nạp dữ liệu phụ theo tab.
export function ReadOnlyClassPage({
  audience,
  value,
  students,
}: {
  audience: "student" | "teacher";
  value: CourseClassView;
  students?: EnrollmentView[];
}) {
  const [tab, openTab] = useUrlState<ClassTab>("tab", "info", CLASS_TABS);
  const params = useSearchParams();
  const [groupNames, setGroupNames] = useState<{ classId: string; names: Record<string, string>; leaders: string[] } | null>(null);
  // Ô trên dòng tiêu đề cho nút của tab đang mở (unit gắn vào bằng portal).
  const [headerActions, setHeaderActions] = useState<HTMLElement | null>(null);
  // U12 dựng bảng sinh viên kèm nhóm thì bảng sinh viên chỉ xem của trang ẩn đi.
  const [groupTable, setGroupTable] = useState(false);
  // Gắn kết quả nhóm vào ID lớp để kết quả cũ không hiển thị khi đổi lớp.
  const onGroupNamesLoaded = useCallback(
    (names: Record<string, string>, leaders: string[] = []) => setGroupNames({ classId: value.id, names, leaders }),
    [value.id],
  );
  const enrollmentRows = students ?? [];
  const activeStudents = enrollmentRows.filter((student) => student.status === "ACTIVE");
  const studentSummaryCount = students !== undefined ? activeStudents.length : value.activeStudentCount;
  const normalizedDescription = (value.description ?? "").trim();

  if (audience === "student" && value.status !== "OPEN") {
    return (
      <>
        <PageHeader title={msg("academics.pages.ReadOnlyClassPage.classDetails")} backHref="/classes" />
        <section className={styles.panel}>
          <EmptyState
            title={msg("academics.pages.ReadOnlyClassPage.thisClassIsCurrentlyUnavailable")}
          />
        </section>
      </>
    );
  }

  if (audience === "student") return <StudentClassPage value={value} tab={tab === "materials" ? "materials" : "info"} />;

  const tabs = [
    { key: "info" as const, label: msg("academics.pages.ReadOnlyClassPage.classInformation") },
    { key: "students" as const, label: msg("academics.pages.ReadOnlyClassPage.students"), count: studentSummaryCount },
    { key: "materials" as const, label: msg("academics.pages.ReadOnlyClassPage.learningMaterials") },
    { key: "evals" as const, label: msg("academics.pages.ReadOnlyClassPage.assignments") },
    { key: "gradebook" as const, label: msg("academics.pages.ReadOnlyClassPage.grading") },
  ];

  return (
    <>
      {/* Tiêu đề là tên trang đang mở; quay về danh sách lớp bằng sidebar của lớp. */}
      <PageHeader
        title={tab === "gradebook" && params?.get("gradebookView") === "matrix" ? msg("academics.pages.ReadOnlyClassPage.gradebook") : tabs.find((t) => t.key === tab)?.label}
        actions={<div ref={setHeaderActions} className="flex flex-wrap items-center gap-2" />} />
      {/* Màn rộng đã có các tab này trên sidebar của lớp. */}
      <div className="md:hidden"><Tabs items={tabs} value={tab} onChange={openTab} label={msg("academics.pages.ReadOnlyClassPage.classContent")} testIdPrefix="class-tab-" /></div>
      {(
        // Đổi tab thì dựng lại khung này để nội dung mới mờ vào, khớp với gạch chân đang trượt.
        <div key={tab} role="tabpanel" aria-label={tabs.find((t) => t.key === tab)?.label} className={styles.tabPanel}>
        {tab === "info" && (
          <div className="flex flex-col gap-4" data-teacher-overview="true">
            <Section title={msg("academics.pages.ReadOnlyClassPage.classInformation")}>
              <dl className="grid min-w-0 gap-x-6 gap-y-4 sm:grid-cols-2 lg:grid-cols-5">
                {[
                  [msg("academics.pages.ReadOnlyClassPage.subject"), `${value.subject.code} · ${value.subject.name}`],
                  [msg("academics.pages.ReadOnlyClassPage.classCode"), value.code],
                  [msg("academics.pages.ReadOnlyClassPage.term"), value.term],
                  [msg("academics.pages.ReadOnlyClassPage.students"), String(studentSummaryCount)],
                  [msg("academics.pages.ReadOnlyClassPage.status"), CLASS_STATUS_LABELS[value.status]],
                ].map(([label, content]) => (
                  <div key={label} className="min-w-0">
                    <dt className="mb-1 text-xs text-muted">{label}</dt>
                    <dd className="break-words text-sm font-medium text-ink">{content}</dd>
                  </div>
                ))}
              </dl>
              {normalizedDescription && <p className="text-sm whitespace-pre-line text-body [overflow-wrap:anywhere]">{normalizedDescription}</p>}
            </Section>
            <div>
              <SlotOutlet slot={teacherClassTabs} props={{ classId: value.id, tab: "info" as const }} />
            </div>
          </div>
        )}
        {tab === "students" && (
          // Có U12 thì thẻ Nhóm cạnh bảng sinh viên (việc với nhóm làm trong popup); không thì bảng sinh viên chỉ xem.
          <div className="flex flex-col gap-4">
            <SlotOutlet
              slot={teacherClassTabs}
              props={{
                classId: value.id, tab: "students" as const, students: activeStudents, onGroupNamesLoaded, onStudentTable: setGroupTable,
              }}
            />
            {!groupTable && <EnrollmentPanel
              students={activeStudents}
              groupNames={groupNames?.classId === value.id ? groupNames.names : undefined}
              groupLeaders={groupNames?.classId === value.id ? groupNames.leaders : undefined}
            />}
          </div>
        )}
        {(tab === "materials" || tab === "evals" || tab === "gradebook") && (
          <SlotOutlet slot={teacherClassTabs} props={{ classId: value.id, tab, headerActions }} />
        )}
        </div>
      )}
    </>
  );
}
