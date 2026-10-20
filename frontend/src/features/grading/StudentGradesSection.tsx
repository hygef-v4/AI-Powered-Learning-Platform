"use client";
import { useCallback } from "react";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
import { points, statusLabels } from "./types";
export function StudentGradesSection({
  classId,
  studentId,
}: {
  classId: string;
  studentId: string;
}) {
  const load = useCallback(() => gradingApi.student(classId, studentId), [classId, studentId]);
  const { value, error } = useResource(load);
  return (
    <section className="my-4">
      <h3 className="font-bold">Bài nộp và điểm</h3>
      {error && <p role="alert">{error}</p>}
      {value?.grades.map((g, i) => (
        <p key={g.assignmentId}>
          Bài {i + 1}: {points(g.score, g.maxScore)} · {statusLabels[g.status] ?? g.status}
        </p>
      ))}
    </section>
  );
}
