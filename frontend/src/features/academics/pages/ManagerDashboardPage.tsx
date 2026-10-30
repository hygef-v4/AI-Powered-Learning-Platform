"use client";

import { useId, useState } from "react";
import { Field, LockedInput, PageHeader, Section, Segmented } from "@/shared/ui";
import type { ManagedSubject } from "../api";
import { CourseCard, CourseGrid } from "../components/CourseCard";
import type { CourseClassView, SubjectView } from "../model/types";

/** Số lớp đang hoạt động (đang mở) của một môn. */
function openClasses(subject: SubjectView, classes: CourseClassView[]) {
  return classes.filter((course) => course.subject.id === subject.id && course.status === "OPEN").length;
}

type SubjectTab = "ACTIVE" | "ARCHIVED";

/**
 * Môn được phân công (Chủ nhiệm môn): tab Hoạt động / Lưu trữ theo trạng thái môn, lưới thẻ dọc chỉ có mã môn ở trên và tên
 * môn ở dưới; bấm thẻ để mở Thông tin môn.
 */
export function ManagerDashboardPage({ subjects, onSubject }: { subjects: ManagedSubject[]; onSubject: (id: string) => void }) {
  const [tab, setTab] = useState<SubjectTab>("ACTIVE");
  const archived = subjects.filter((subject) => subject.status === "ARCHIVED").length;
  const count = (value: SubjectTab) => (value === "ARCHIVED" ? archived : subjects.length - archived);
  const shown = subjects.filter((subject) => (subject.status === "ARCHIVED") === (tab === "ARCHIVED"));
  return <>
    <PageHeader title="Môn được phân công" />
    <Segmented as="tab" ariaLabel="Lọc môn" value={tab} onChange={setTab} className="self-start"
      options={([{ value: "ACTIVE", label: "Hoạt động" }, { value: "ARCHIVED", label: "Lưu trữ" }] as const).map((option) => ({
        value: option.value, label: <>{option.label}<span className="ml-1 tabular-nums text-muted">{count(option.value)}</span></>,
      }))} />
    <CourseGrid empty={shown.length === 0 ? (tab === "ARCHIVED" ? "Chưa có môn lưu trữ." : "Bạn chưa được giao quản lý môn.") : null}>
      {shown.map((subject) => (
        <CourseCard key={subject.id} code={subject.code} name={subject.name} band={subject.code} icon="menu_book"
          onOpen={() => onSubject(subject.id)} label={`Mở môn ${subject.code}`} testId={`subject-row-${subject.id}`} />
      ))}
    </CourseGrid>
  </>;
}

/**
 * Thông tin môn của Chủ nhiệm môn: một thẻ như Thông tin môn học bên quản trị (mã môn, tên môn, mô tả) nhưng chỉ xem, thêm ô
 * Số lớp là số lớp đang hoạt động. Không có ô số liệu khác; đổi môn bằng ô chọn môn trên sidebar.
 */
export function MySubjectPage({ subject, classes }: { subject: SubjectView; classes: CourseClassView[] }) {
  const id = useId();
  return <>
    <PageHeader title="Thông tin môn" />
    <Section title="Thông tin môn học">
      <div className="grid min-w-0 gap-4 sm:grid-cols-[1fr_2fr_1fr]">
        <Field label="Mã môn" htmlFor={`${id}-code`}><LockedInput id={`${id}-code`} value={subject.code} /></Field>
        <Field label="Tên môn học" htmlFor={`${id}-name`}><LockedInput id={`${id}-name`} value={subject.name} /></Field>
        <Field label="Số lớp" htmlFor={`${id}-classes`}><LockedInput id={`${id}-classes`} value={String(openClasses(subject, classes))} /></Field>
        <div className="sm:col-span-3">
          <Field label="Mô tả" htmlFor={`${id}-description`}>
            <p id={`${id}-description`} className="min-h-28 rounded-lg border border-line bg-sunken px-3 py-2 text-[13.5px] whitespace-pre-line text-body [overflow-wrap:anywhere]">
              {subject.description?.trim() || <span className="text-muted">Chưa có mô tả.</span>}
            </p>
          </Field>
        </div>
      </div>
    </Section>
  </>;
}
