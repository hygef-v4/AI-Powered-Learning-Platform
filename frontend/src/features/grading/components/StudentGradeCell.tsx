"use client";
import { useCallback } from "react";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";

/** Ô Điểm trên bảng Bài tập của học sinh: chỉ con số điểm đã công bố (bỏ số 0 thừa); chưa có thì dấu gạch. */
export function StudentGradeCell({
  classId,
  assignmentId,
}: {
  classId: string;
  assignmentId: string;
}) {
  // Tải danh sách điểm của sinh viên đang đăng nhập cho lớp chứa ô điểm.
  const load = useCallback(() => gradingApi.listMyGrades(classId), [classId]);
  const { value, error } = useResource(load);
  const grade = value?.find((g) => g.assignmentId === assignmentId);
  return error || grade?.score == null
    ? <span className="text-muted" title={error || undefined}>—</span>
    : <span className="font-semibold text-ink tabular-nums">{String(Number(Number(grade.score).toFixed(2)))}</span>;
}
