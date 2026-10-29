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
  const load = useCallback(async () => {
    const [row, gradebook] = await Promise.all([
      gradingApi.student(classId, studentId),
      gradingApi.gradebook(classId),
    ]);
    return { grades: row.grades, assignments: gradebook.assignments };
  }, [classId, studentId]);
  const { value, error } = useResource(load);
  return (
    <section className="my-4">
      <h3 className="font-bold">Bài nộp và điểm</h3>
      {error && <p role="alert">{error}</p>}
      {!value && !error && <p role="status">Đang tải bài nộp và điểm…</p>}
      {value && <>
        <p className="my-3">Đã nộp {value.grades.filter(grade => grade.submittedAt !== null).length} / {value.assignments.length} bài tính điểm · Nộp trễ {value.grades.filter(grade => grade.submittedAt !== null && grade.late).length}</p>
        <div className="overflow-x-auto"><table className="w-full table-fixed text-left text-sm">
          <colgroup><col /><col className="w-[120px]" /><col className="w-[150px]" /><col className="w-[110px]" /></colgroup><thead><tr><th scope="col" className="p-2">Bài</th><th scope="col" className="p-2">Hình thức</th><th scope="col" className="p-2">Nộp</th><th scope="col" className="p-2">Điểm</th></tr></thead>
          <tbody>{value.assignments.map(assignment => {
            const grade = value.grades.find(item => item.assignmentId === assignment.id);
            return <tr key={assignment.id} className="border-t">
              <th scope="row" className="p-2 font-medium">{assignment.title}</th>
              <td className="p-2">{assignment.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"}</td>
              <td className="p-2">{grade?.submittedAt ? <>{grade.late ? "Trễ" : "Đúng hạn"}<br/>{new Date(grade.submittedAt).toLocaleString("vi-VN")}</> : grade?.status === "IN_PROGRESS" ? "Đang làm" : "Chưa nộp"}</td>
              <td className="p-2">{points(grade?.score, grade?.maxScore ?? assignment.maxScore)}<br/>{statusLabels[grade?.status ?? "NOT_SUBMITTED"] ?? grade?.status}</td>
            </tr>;
          })}</tbody>
        </table></div>
        {!value.assignments.length && <p>Chưa có bài tính điểm trong lớp.</p>}
      </>}
    </section>
  );
}
