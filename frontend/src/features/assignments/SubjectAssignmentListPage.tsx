"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { apiFetch } from "@/shared/api/client";
import { Alert, Button, PageHeader } from "@/shared/ui";
import { assignmentsApi, type ListFilter } from "./api";
import { ALL_ASSIGNMENTS, AssignmentListCard } from "./AssignmentListCard";
import { CreateAssignmentDialog } from "./CreateAssignmentDialog";
import { errorMessage } from "./format";
import { useAssignmentPage } from "./useAssignmentPage";

type ManagedSubject = { id: string; code: string; name: string };

/**
 * #25 Assignment List (F2): bài của môn cho Chủ nhiệm môn, cùng kiểu trang Bài tập của sinh viên; nút Tạo bài tập trên đầu
 * trang, ngoài thẻ danh sách. Đổi môn bằng ô chọn môn trên sidebar (`?subjectId=`).
 */
export function SubjectAssignmentListPage() {
  const router = useRouter();
  const params = useSearchParams();
  const [subjects, setSubjects] = useState<ManagedSubject[] | null>(null);
  const [subjectsError, setSubjectsError] = useState<unknown>(null);
  const [creating, setCreating] = useState(false);

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
  const page = useAssignmentPage(load, ALL_ASSIGNMENTS);

  return (
    <div className="flex flex-col gap-4">
      <PageHeader title="Bài tập"
        actions={<Button variant="primary" icon="add" disabled={!subjectId || !page.data || page.data.ownerArchived}
          onClick={() => setCreating(true)} data-testid="subject-assignment-create">Tạo bài tập</Button>} />
      {subjectsError !== null && <Alert tone="danger" role="alert">{errorMessage(subjectsError)}</Alert>}
      {subjects && subjects.length === 0 && <Alert tone="info">Bạn chưa được giao quản lý môn nào.</Alert>}
      {subjectId && <AssignmentListCard page={page} />}
      {creating && subjectId && (
        <CreateAssignmentDialog target={{ kind: "subject", subjectId }} onClose={() => setCreating(false)}
          onCreated={(created) => router.push(`/assignments/${encodeURIComponent(created.id)}`)} />
      )}
    </div>
  );
}
