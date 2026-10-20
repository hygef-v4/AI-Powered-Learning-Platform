"use client";
import Link from "next/link";
import { useCallback } from "react";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
export function SubmissionsLink({
  assignmentId,
  classId,
}: {
  assignmentId: string;
  classId?: string;
}) {
  return classId ? <ClassSubmissionsLink classId={classId} assignmentId={assignmentId} /> : null;
}
function ClassSubmissionsLink({
  assignmentId,
  classId,
}: {
  assignmentId: string;
  classId: string;
}) {
  const load = useCallback(
    () => gradingApi.submissions(classId, assignmentId),
    [classId, assignmentId],
  );
  const { value, error } = useResource(load);
  return (
    <Link
      className="text-primary"
      href={`/teaching/classes/${classId}/assignments/${assignmentId}/submissions`}
    >
      {value
        ? `${value.summary.submitted ?? 0} / ${value.summary.total ?? 0} nộp · ${value.summary.PENDING ?? 0} chờ chấm`
        : error
          ? "Bài nộp chưa khả dụng"
          : "Xem bài nộp"}
    </Link>
  );
}
