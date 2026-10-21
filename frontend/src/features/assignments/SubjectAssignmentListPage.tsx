"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { apiFetch } from "@/shared/api/client";
import { Alert, Button, PageHeader, Section, Select } from "@/shared/ui";
import { assignmentsApi, type ListFilter } from "./api";
import { AssignmentFilters, emptyFilter, toListFilter, type FilterState } from "./AssignmentFilters";
import { AssignmentTable } from "./AssignmentTable";
import { useDebounced } from "./ClassAssignmentsSection";
import { CreateAssignmentDialog } from "./CreateAssignmentDialog";
import { errorMessage } from "./format";
import { useAssignmentPage } from "./useAssignmentPage";

type ManagedSubject = { id: string; code: string; name: string };

/** #25 Assignment List (F2): bài của môn cho Chủ nhiệm môn. */
export function SubjectAssignmentListPage() {
  const router = useRouter();
  const params = useSearchParams();
  const [subjects, setSubjects] = useState<ManagedSubject[] | null>(null);
  const [subjectsError, setSubjectsError] = useState<unknown>(null);
  const [filter, setFilter] = useState<FilterState>(emptyFilter);
  const [creating, setCreating] = useState(false);
  const debounced = useDebounced(filter);

  useEffect(() => {
    const controller = new AbortController();
    apiFetch<ManagedSubject[]>("/api/v1/manager/subjects", { signal: controller.signal }).then(
      (list) => { if (!controller.signal.aborted) setSubjects(list); },
      (error) => { if (!controller.signal.aborted) setSubjectsError(error); },
    );
    return () => controller.abort();
  }, []);

  const subjectId = params.get("subjectId") ?? subjects?.[0]?.id ?? null;
  const load = useCallback((value: ListFilter, signal: AbortSignal) =>
    subjectId ? assignmentsApi.listForSubject(subjectId, value, signal) : Promise.resolve({ items: [], hiddenCount: 0, counts: {}, groupSupported: false, ownerArchived: false }),
  [subjectId]);
  const page = useAssignmentPage(load, toListFilter(debounced));

  return (
    <div className="flex flex-col gap-4">
      <PageHeader title="Bài tập của môn"
        subtitle="Bài cá nhân do Chủ nhiệm môn soạn cho cả môn, giao cho mọi lớp đang mở của môn theo một lịch chung; giảng viên lớp chỉ xem và chấm. Bài nhóm chỉ có ở bài của lớp."
        actions={<>
          {subjects && subjects.length > 0 && (
            <Select aria-label="Môn học" value={subjectId ?? ""} wrapperClassName="w-60"
              onChange={(event) => router.push(`/manager/assignments?subjectId=${encodeURIComponent(event.target.value)}`)}>
              {subjects.map((subject) => <option key={subject.id} value={subject.id}>{subject.code} · {subject.name}</option>)}
            </Select>
          )}
          <Button variant="primary" icon="add" disabled={!subjectId || !page.data || page.data.ownerArchived}
            onClick={() => setCreating(true)} data-testid="subject-assignment-create">Tạo bài tập</Button>
        </>} />
      {subjectsError !== null && <Alert tone="danger" role="alert">{errorMessage(subjectsError)}</Alert>}
      {subjects && subjects.length === 0 && <Alert tone="info">Bạn chưa được giao quản lý môn nào.</Alert>}
      {subjectId && (
        <Section>
          <AssignmentFilters value={filter} onChange={setFilter} />
          {page.loading && !page.data && <p role="status" className="text-sm text-muted">Đang tải bài tập…</p>}
          {page.error !== null && <Alert tone="danger" role="alert" action={<Button size="sm" onClick={page.reload}>Thử lại</Button>}>{errorMessage(page.error)}</Alert>}
          {page.data && <AssignmentTable items={page.data.items} showScope={false} />}
          {page.data && (
            <p className="text-xs text-muted">
              {page.data.hiddenCount > 0 ? `Đang ẩn ${page.data.hiddenCount} bài Đã đóng hoặc Ngưng giao. ` : ""}Quiz của môn xem ở mục Quiz.
            </p>
          )}
        </Section>
      )}
      {creating && subjectId && (
        <CreateAssignmentDialog target={{ kind: "subject", subjectId }} onClose={() => setCreating(false)}
          onCreated={(created, { importDocx }) => router.push(`/assignments/${encodeURIComponent(created.id)}${importDocx ? "?import=docx" : ""}`)} />
      )}
    </div>
  );
}
