"use client";
import { useCallback } from "react";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";
import { formatPoints, GRADE_STATUS_LABELS } from "../model/types";
import { formatDateTime } from "@/shared/dateTime";
import { msg } from "@/i18n/msg";
// Ghép điểm sinh viên được chọn với thông tin bài tập trong lớp.
export function StudentGradesSection({
  classId,
  studentId,
}: {
  classId: string;
  studentId: string;
}) {
  // Đọc hàng điểm và danh mục bài cùng lúc để hiển thị tên bài bên cạnh điểm.
  const load = useCallback(async () => {
    const [row, gradebook] = await Promise.all([
      gradingApi.loadStudentGrades(classId, studentId),
      gradingApi.loadGradebook(classId),
    ]);
    return { grades: row.grades, assignments: gradebook.assignments };
  }, [classId, studentId]);
  const { value, error } = useResource(load);
  return (
    <section className="my-4">
      <h3 className="font-bold">{msg("grading.components.StudentGradesSection.submissionsAndScores")}</h3>
      {error && <p role="alert">{error}</p>}
      {!value && !error && <p role="status">{msg("grading.components.StudentGradesSection.loadingSubmissionsAndScores")}</p>}
      {value && <>
        <p className="my-3">{msg("grading.components.StudentGradesSection.submitted")}{value.grades.filter(grade => grade.submittedAt !== null).length} / {value.assignments.length} {msg("grading.components.StudentGradesSection.gradedAssignmentsLate")}{value.grades.filter(grade => grade.submittedAt !== null && grade.late).length}</p>
        <div className="overflow-x-auto"><table className="w-full table-fixed text-left text-sm">
          <colgroup><col /><col className="w-[120px]" /><col className="w-[150px]" /><col className="w-[110px]" /></colgroup><thead><tr><th scope="col" className="p-2">{msg("grading.components.StudentGradesSection.assignment")}</th><th scope="col" className="p-2">{msg("grading.components.StudentGradesSection.format")}</th><th scope="col" className="p-2">{msg("grading.components.StudentGradesSection.submitted2")}</th><th scope="col" className="p-2">{msg("grading.components.StudentGradesSection.score")}</th></tr></thead>
          <tbody>{value.assignments.map(assignment => {
            const grade = value.grades.find(item => item.assignmentId === assignment.id);
            return <tr key={assignment.id} className="border-t">
              <th scope="row" className="p-2 font-medium">{assignment.title}</th>
              <td className="p-2">{assignment.submissionMode === "GROUP" ? msg("grading.components.StudentGradesSection.group") : msg("grading.components.StudentGradesSection.individual")}</td>
              <td className="p-2">{grade?.submittedAt ? <>{grade.late ? msg("grading.components.StudentGradesSection.late") : msg("grading.components.StudentGradesSection.onTime")}<br/>{formatDateTime(grade.submittedAt)}</> : msg("grading.components.StudentGradesSection.notSubmitted")}</td>
              <td className="p-2">{formatPoints(grade?.score, grade?.maxScore ?? assignment.maxScore)}<br/>{GRADE_STATUS_LABELS[grade?.status ?? "NOT_SUBMITTED"] ?? grade?.status}</td>
            </tr>;
          })}</tbody>
        </table></div>
        {!value.assignments.length && <p>{msg("grading.components.StudentGradesSection.noGradedAssignmentsInThis")}</p>}
      </>}
    </section>
  );
}
