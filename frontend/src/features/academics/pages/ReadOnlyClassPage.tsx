"use client";

import { useSearchParams } from "next/navigation";
import { useCallback, useId, useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { teacherClassInfoFields, teacherClassTabs } from "../slots";
import { StudentClassPage } from "./StudentClassPage";
import { EnrollmentPanel } from "../components/EnrollmentPanel";
import { Field, LockedInput, PageHeader, Section } from "@/shared/ui";
import { EmptyState, PageHeading, Tabs } from "../components/ui";
import type { CourseClassView, EnrollmentView } from "../model/types";

import styles from "../academics.module.css";

type ClassTab = "info" | "students" | "materials" | "evals" | "gradebook";
const CLASS_TABS: readonly ClassTab[] = ["info", "students", "materials", "evals", "gradebook"];

/** Tab theo `?tab=` (sidebar của lớp, nút Mở tab Sinh viên ở Kiểm tra sẵn sàng bài nhóm); thiếu hay sai thì là Thông tin lớp. */
function tabOf(wanted: string | null | undefined): ClassTab {
  return CLASS_TABS.find((t) => t === wanted) ?? "info";
}

export function ReadOnlyClassPage({
  audience,
  value,
  students,
  onBack,
}: {
  audience: "student" | "teacher";
  value: CourseClassView;
  students?: EnrollmentView[];
  onBack: () => void;
}) {
  const params = useSearchParams();
  const urlTab = tabOf(params?.get("tab"));
  const [tab, setTab] = useState<ClassTab>(urlTab);
  // Bấm mục trên sidebar của lớp thì `?tab=` đổi: mở theo.
  const [seenUrlTab, setSeenUrlTab] = useState(urlTab);
  if (urlTab !== seenUrlTab) {
    setSeenUrlTab(urlTab);
    setTab(urlTab);
  }
  // Giữ tab trên URL để tải lại hoặc gửi link vẫn mở đúng tab.
  const openTab = (next: ClassTab) => {
    setTab(next);
    const url = new URL(window.location.href);
    url.searchParams.set("tab", next);
    window.history.replaceState(window.history.state, "", url);
  };
  const [groupNames, setGroupNames] = useState<{ classId: string; names: Record<string, string>; leaders: string[] } | null>(null);
  const fieldId = useId();
  // Ô trên dòng tiêu đề cho nút của tab đang mở (unit gắn vào bằng portal).
  const [headerActions, setHeaderActions] = useState<HTMLElement | null>(null);
  // U12 dựng bảng sinh viên kèm nhóm thì bảng sinh viên chỉ xem của trang ẩn đi.
  const [groupTable, setGroupTable] = useState(false);
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
        <PageHeading title="Chi tiết lớp" onBack={onBack} />
        <section className={styles.panel}>
          <EmptyState
            title="Lớp hiện không khả dụng"
          />
        </section>
      </>
    );
  }

  if (audience === "student") return <StudentClassPage value={value} tab={tab === "materials" ? "materials" : "info"} />;

  const tabs = [
    { key: "info" as const, label: "Thông tin lớp" },
    { key: "students" as const, label: "Sinh viên", count: studentSummaryCount },
    { key: "materials" as const, label: "Học liệu" },
    { key: "evals" as const, label: "Bài tập" },
    { key: "gradebook" as const, label: "Chấm điểm" },
  ];

  return (
    <>
      {/* Tiêu đề là tên trang đang mở; quay về danh sách lớp bằng sidebar của lớp. */}
      <PageHeader
        title={tabs.find((t) => t.key === tab)?.label}
        actions={<div ref={setHeaderActions} className="flex flex-wrap items-center gap-2" />} />
      {/* Màn rộng đã có các tab này trên sidebar của lớp. */}
      <div className="md:hidden"><Tabs items={tabs} value={tab} onChange={openTab} label="Nội dung lớp" /></div>
      {(
        // Đổi tab thì dựng lại khung này để nội dung mới mờ vào, khớp với gạch chân đang trượt.
        <div key={tab} role="tabpanel" aria-label={tabs.find((t) => t.key === tab)?.label} className={styles.tabPanel}>
        {tab === "info" && (
          // Thẻ Thông tin lớp như trang lớp của sinh viên, thêm ô Sinh viên (đang học) và Học liệu (U13, đang hoạt động); bên dưới
          // hàng thẻ chia đôi do các unit gắn vào (Thông báo gần đây, Tiến độ nộp bài). Không có ô số liệu.
          <div className="flex flex-col gap-4" data-teacher-overview="true">
            <Section title="Thông tin lớp">
              {/* Hai cột như thẻ của sinh viên, thêm cột thứ ba bên phải cho Sinh viên và Học liệu. */}
              <div className="grid min-w-0 gap-4 sm:grid-cols-2 lg:grid-flow-col lg:grid-cols-3 lg:grid-rows-2">
                <Field label="Mã lớp" htmlFor={`${fieldId}-code`}><LockedInput id={`${fieldId}-code`} value={value.code} /></Field>
                <Field label="Tên lớp" htmlFor={`${fieldId}-name`}><LockedInput id={`${fieldId}-name`} value={value.name} /></Field>
                <Field label="Mã môn" htmlFor={`${fieldId}-subject`}><LockedInput id={`${fieldId}-subject`} value={value.subject.code} /></Field>
                <Field label="Học kỳ" htmlFor={`${fieldId}-term`}><LockedInput id={`${fieldId}-term`} value={value.term} /></Field>
                <Field label="Sinh viên" htmlFor={`${fieldId}-students`}><LockedInput id={`${fieldId}-students`} value={String(studentSummaryCount)} /></Field>
                <SlotOutlet slot={teacherClassInfoFields} props={{ classId: value.id }} />
              </div>
              <div>
                  <Field label="Mô tả" htmlFor={`${fieldId}-description`}>
                    <p id={`${fieldId}-description`} className="min-h-28 rounded-lg border border-line bg-sunken px-3 py-2 text-[13.5px] whitespace-pre-line text-body [overflow-wrap:anywhere]">
                      {normalizedDescription || <span className="text-muted">Chưa có mô tả.</span>}
                    </p>
                  </Field>
              </div>
            </Section>
            <div className="grid items-start gap-4 lg:grid-cols-2">
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
