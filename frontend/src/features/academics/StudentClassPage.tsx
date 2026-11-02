"use client";

import { useId } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Field, LockedInput, PageHeader, Pill, Section } from "@/shared/ui";
import { initials } from "@/shared/layout/navigation";
import { studentClassAside, studentClassBoard, studentClassSections } from "./slots";
import type { CourseClassView } from "./types";

/**
 * Lớp của tôi (Sinh viên), cùng bố cục Chi tiết lớp của Chủ nhiệm môn nhưng chỉ xem: tiêu đề là tên lớp, thẻ Thông tin lớp bên
 * trái, cột phải có Giảng viên và Nhóm của tôi; bên dưới là hàng hai thẻ chia đôi (Thông báo gần đây, Bài cần làm). `?tab=materials`
 * (mục Học liệu trên sidebar) mở danh sách học liệu của lớp.
 */
export function StudentClassPage({ value, tab }: { value: CourseClassView; tab: "info" | "materials" }) {
  const id = useId();
  return (
    <>
      <PageHeader
        title={tab === "materials" ? "Học liệu" : "Thông tin lớp"}
      />
      {tab === "materials" ? (
        <div role="region" aria-label="Học liệu" className="flex flex-col gap-4">
          <SlotOutlet slot={studentClassSections} props={{ classId: value.id }} />
        </div>
      ) : (
        <>
        <div className="grid gap-4 lg:grid-cols-[1fr_300px]">
          <Section title="Thông tin lớp">
            <div className="grid min-w-0 gap-4 sm:grid-cols-2">
              <Field label="Mã lớp" htmlFor={`${id}-code`}><LockedInput id={`${id}-code`} value={value.code} /></Field>
              <Field label="Mã môn" htmlFor={`${id}-subject`}><LockedInput id={`${id}-subject`} value={value.subject.code} /></Field>
              <Field label="Tên lớp" htmlFor={`${id}-name`}><LockedInput id={`${id}-name`} value={value.name} /></Field>
              <Field label="Học kỳ" htmlFor={`${id}-term`}><LockedInput id={`${id}-term`} value={value.term} /></Field>
              <div className="sm:col-span-2">
                <Field label="Mô tả" htmlFor={`${id}-description`}>
                  {/* Cùng khung ô Mô tả bên Chủ nhiệm môn (cao tối thiểu như textarea ở đó). */}
                  <p id={`${id}-description`} className="min-h-28 rounded-lg border border-line bg-sunken px-3 py-2 text-[13.5px] whitespace-pre-line text-body [overflow-wrap:anywhere]">
                    {value.description?.trim() || <span className="text-muted">Chưa có mô tả.</span>}
                  </p>
                </Field>
              </div>
            </div>
          </Section>
          {/* Cột phải cao bằng thẻ Thông tin lớp: Giảng viên gọn theo nội dung, Nhóm của tôi giãn phần còn lại. */}
          <div className="flex flex-col gap-4">
            <Section title="Giảng viên">
              {value.teacher ? (
                <div className="flex items-center gap-3">
                  <span aria-hidden="true" className="flex size-10 shrink-0 items-center justify-center rounded-full bg-accent-soft font-semibold text-accent">{initials(value.teacher.name)}</span>
                  <div className="min-w-0"><p className="text-sm font-semibold">{value.teacher.name}</p><p className="break-all text-xs text-muted">{value.teacher.email}</p></div>
                </div>
              ) : <div><Pill>Chưa phân công</Pill></div>}
            </Section>
            <SlotOutlet slot={studentClassAside} props={{ classId: value.id }} />
          </div>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <SlotOutlet slot={studentClassBoard} props={{ classId: value.id }} />
        </div>
        </>
      )}
    </>
  );
}
