"use client";

import { useCallback, useState } from "react";
import { useRouter } from "next/navigation";
import { createPortal } from "react-dom";
import { Button } from "@/shared/ui";
import { assignmentsApi, type ListFilter } from "./api";
import { ALL_ASSIGNMENTS, AssignmentListCard } from "./AssignmentListCard";
import { CreateAssignmentDialog } from "./CreateAssignmentDialog";
import { useAssignmentPage } from "./useAssignmentPage";

/**
 * Tab Bài tập của Teacher Class Detail (F1, UC 34), gắn qua TeacherClassTab: nút Tạo bài tập trên dòng tiêu đề trang lớp;
 * danh sách cùng kiểu trang Bài tập của môn, thêm cột Phạm vi và Bài nộp.
 */
export function ClassAssignmentsSection({ classId, tab, headerActions }: { classId: string; tab: string; headerActions?: HTMLElement | null }) {
  return tab === "evals" ? <ClassAssignments classId={classId} headerActions={headerActions} /> : null;
}

function ClassAssignments({ classId, headerActions }: { classId: string; headerActions?: HTMLElement | null }) {
  const router = useRouter();
  const [creating, setCreating] = useState(false);
  const load = useCallback((value: ListFilter, signal: AbortSignal) => assignmentsApi.listForClass(classId, value, signal), [classId]);
  const page = useAssignmentPage(load, ALL_ASSIGNMENTS);
  const create = (
    <Button variant="primary" icon="add" onClick={() => setCreating(true)}
      disabled={!page.data || page.data.ownerArchived} data-testid="class-assignment-create">Tạo bài tập</Button>
  );
  return (
    <div className="flex flex-col gap-3">
      {/* Nút lên dòng tiêu đề trang lớp để bảng nằm sát tiêu đề như trang Bài tập của sinh viên. */}
      {headerActions ? createPortal(create, headerActions) : <div className="flex justify-end">{create}</div>}
      <AssignmentListCard page={page} classId={classId} />
      {creating && page.data && (
        <CreateAssignmentDialog target={{ kind: "class", classId, groupSupported: page.data.groupSupported }}
          onClose={() => setCreating(false)}
          onCreated={(created) => router.push(`/assignments/${encodeURIComponent(created.id)}?classId=${encodeURIComponent(classId)}`)} />
      )}
    </div>
  );
}
