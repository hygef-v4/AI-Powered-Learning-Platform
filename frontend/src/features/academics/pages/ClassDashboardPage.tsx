"use client";

import { useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { PageHeader, Segmented } from "@/shared/ui";
import { CourseCard, CourseGrid } from "../components/CourseCard";
import { classCardSections } from "../slots";
import type { ClassStatus, CourseClassView } from "../model/types";

/**
 * Lớp của tôi (Sinh viên) và Lớp được phân công (Giảng viên): tab theo trạng thái lớp rồi lưới thẻ dọc, mỗi thẻ chỉ có mã lớp
 * ở trên và tên môn ở dưới; sinh viên thấy thêm số bài cần làm. Tab Sắp mở chỉ hiện khi còn lớp nháp; sinh viên không vào được
 * lớp đã kết thúc.
 */
export function ClassDashboardPage({ audience, classes, onOpen }: { audience: "student" | "teacher"; classes: CourseClassView[]; onOpen: (id: string) => void }) {
  const [status, setStatus] = useState<ClassStatus>("OPEN");
  const student = audience === "student";
  const count = (value: ClassStatus) => classes.filter((course) => course.status === value).length;
  const tabs: { value: ClassStatus; label: string }[] = student
    ? [{ value: "OPEN", label: "Đang học" }, { value: "ARCHIVED", label: "Đã kết thúc" }]
    : [...(count("DRAFT") > 0 ? [{ value: "DRAFT" as const, label: "Sắp mở" }] : []), { value: "OPEN", label: "Đang dạy" }, { value: "ARCHIVED", label: "Kết thúc" }];
  const visible = classes.filter((course) => course.status === status).sort((a, b) => a.code.localeCompare(b.code));
  const empty = status === "ARCHIVED" ? "Chưa có lớp đã kết thúc." : student ? "Bạn chưa học lớp nào." : "Chưa có lớp trong mục này.";

  return <>
    <PageHeader title={student ? "Lớp của tôi" : "Lớp được phân công"} />
    <Segmented as="tab" ariaLabel="Lọc lớp" value={status} onChange={setStatus} className="self-start"
      options={tabs.map((tab) => ({ value: tab.value, label: <>{tab.label}<span className="ml-1 tabular-nums text-muted">{count(tab.value)}</span></> }))} />
    <CourseGrid empty={visible.length === 0 ? empty : null}>
      {visible.map((course) => {
        const openable = !(student && course.status === "ARCHIVED");
        return (
          <CourseCard key={course.id} code={course.code} name={course.name} band={course.subject.code} icon="class"
            extra={student && course.status === "OPEN" ? <SlotOutlet slot={classCardSections} props={{ classId: course.id }} /> : undefined}
            onOpen={openable ? () => onOpen(course.id) : undefined} label={`Xem lớp ${course.code}`} testId={`dashboard-open-${course.id}`} />
        );
      })}
    </CourseGrid>
  </>;
}
