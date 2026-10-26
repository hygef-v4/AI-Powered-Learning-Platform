"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Alert, Button, Section } from "@/shared/ui";
import { assignmentsApi, type ListFilter } from "./api";
import { AssignmentFilters, emptyFilter, isFiltered, toListFilter, type FilterState } from "./AssignmentFilters";
import { AssignmentTable } from "./AssignmentTable";
import { CreateAssignmentDialog } from "./CreateAssignmentDialog";
import { errorMessage } from "./format";
import { useAssignmentPage } from "./useAssignmentPage";

export function useDebounced<T>(value: T, ms = 300): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), ms);
    return () => clearTimeout(timer);
  }, [value, ms]);
  return debounced;
}

/** Phần Bài tập trong tab Đánh giá của Teacher Class Detail (F1, UC 34); gắn qua TeacherClassTab. */
export function ClassAssignmentsSection({ classId, tab }: { classId: string; tab: string }) {
  return tab === "evals" ? <ClassAssignments classId={classId} /> : null;
}

function ClassAssignments({ classId }: { classId: string }) {
  const router = useRouter();
  const [filter, setFilter] = useState<FilterState>(emptyFilter);
  const [creating, setCreating] = useState(false);
  const debounced = useDebounced(filter);
  const load = useCallback((value: ListFilter, signal: AbortSignal) => assignmentsApi.listForClass(classId, value, signal), [classId]);
  const page = useAssignmentPage(load, toListFilter(debounced));
  return (
    <Section title="Bài tập" icon="assignment"
      actions={<Button size="sm" variant="primary" icon="add" onClick={() => setCreating(true)}
        disabled={!page.data || page.data.ownerArchived} data-testid="class-assignment-create">Tạo bài tập</Button>}>
      <AssignmentFilters value={filter} onChange={setFilter} />
      {page.loading && !page.data && <p role="status" className="text-sm text-muted">Đang tải bài tập…</p>}
      {page.error !== null && <Alert tone="danger" role="alert" action={<Button size="sm" onClick={page.reload}>Thử lại</Button>}>{errorMessage(page.error)}</Alert>}
      {page.data && (
        <>
          <AssignmentTable items={page.data.items} classId={classId} showScope filtered={isFiltered(debounced)} />
          {page.data.hiddenCount > 0 && <p className="text-xs text-muted">Đang ẩn {page.data.hiddenCount} bài Đã đóng hoặc Ngưng giao.</p>}
        </>
      )}
      {creating && page.data && (
        <CreateAssignmentDialog target={{ kind: "class", classId, groupSupported: page.data.groupSupported }}
          onClose={() => setCreating(false)}
          onCreated={(created, startWithAi) => router.push(`/assignments/${encodeURIComponent(created.id)}?classId=${encodeURIComponent(classId)}${startWithAi ? "&startAI=1" : ""}`)} />
      )}
    </Section>
  );
}
