"use client";
import { PageHeader } from "@/shared/ui";
import { ModuleBoard } from "./ModuleBoard";
import { useMaterialResource } from "./useMaterialResource";

type SubjectOption = { id: string; code: string; name: string; status?: string };

/** Học liệu của môn (Chủ nhiệm môn), chia theo module; môn đang mở theo `?subjectId=` (ô chọn môn ở sidebar của môn). */
export function SubjectMaterialListPage({ initialSubjectId = "" }: { initialSubjectId?: string }) {
  const subjects = useMaterialResource<SubjectOption[]>("/api/v1/manager/subjects");
  const subject = subjects.data?.find((s) => s.id === initialSubjectId) ?? subjects.data?.[0];
  if (subject) return <ModuleBoard key={subject.id} header scope={{ kind: "subject", subjectId: subject.id }} readOnly={subject.status === "ARCHIVED"} />;
  return <>
    <PageHeader title="Học liệu" />
    {subjects.loading && <p role="status" className="py-10 text-center text-sm text-muted">Đang tải môn học…</p>}
    {subjects.error && <p role="alert" className="text-sm text-danger">{subjects.error}</p>}
    {subjects.data && <p className="py-10 text-center text-sm text-muted">Bạn chưa được phân công môn học.</p>}
  </>;
}

/** Tab Học liệu của lớp dạy: module của môn, giảng viên thêm học liệu và Quiz của lớp vào module. */
export function ClassMaterialsTab({ classId }: { classId: string }) {
  const info = useMaterialResource<{ status: string }>(`/api/v1/teaching/classes/${encodeURIComponent(classId)}`);
  return <ModuleBoard key={classId} scope={{ kind: "class", classId }} readOnly={info.data?.status === "ARCHIVED"} />;
}
