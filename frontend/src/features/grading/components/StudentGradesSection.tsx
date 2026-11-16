"use client";
import { useCallback } from "react";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";
import { formatPoints, GRADE_STATUS_LABELS } from "../model/types";
import { formatDateTime } from "@/shared/dateTime";
import { msg } from "@/i18n/msg";
export function StudentGradesSection({
  classId,
  studentId,
}: {
  classId: string;
  studentId: string;
}) {
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
      <h3 className="font-bold">{msg("grading.components.StudentGradesSection.baiNopVaDiem")}</h3>
      {error && <p role="alert">{error}</p>}
      {!value && !error && <p role="status">{msg("grading.components.StudentGradesSection.dangTaiBaiNopVaDiem")}</p>}
      {value && <>
        <p className="my-3">{msg("grading.components.StudentGradesSection.daNop")}{value.grades.filter(grade => grade.submittedAt !== null).length} / {value.assignments.length} {msg("grading.components.StudentGradesSection.baiTinhDiemNopTre")}{value.grades.filter(grade => grade.submittedAt !== null && grade.late).length}</p>
        <div className="overflow-x-auto"><table className="w-full table-fixed text-left text-sm">
          <colgroup><col /><col className="w-[120px]" /><col className="w-[150px]" /><col className="w-[110px]" /></colgroup><thead><tr><th scope="col" className="p-2">{msg("grading.components.StudentGradesSection.bai")}</th><th scope="col" className="p-2">{msg("grading.components.StudentGradesSection.hinhThuc")}</th><th scope="col" className="p-2">{msg("grading.components.StudentGradesSection.nop")}</th><th scope="col" className="p-2">{msg("grading.components.StudentGradesSection.diem")}</th></tr></thead>
          <tbody>{value.assignments.map(assignment => {
            const grade = value.grades.find(item => item.assignmentId === assignment.id);
            return <tr key={assignment.id} className="border-t">
              <th scope="row" className="p-2 font-medium">{assignment.title}</th>
              <td className="p-2">{assignment.submissionMode === "GROUP" ? msg("grading.components.StudentGradesSection.nhom") : msg("grading.components.StudentGradesSection.caNhan")}</td>
              <td className="p-2">{grade?.submittedAt ? <>{grade.late ? msg("grading.components.StudentGradesSection.tre") : msg("grading.components.StudentGradesSection.dungHan")}<br/>{formatDateTime(grade.submittedAt)}</> : msg("grading.components.StudentGradesSection.chuaNop")}</td>
              <td className="p-2">{formatPoints(grade?.score, grade?.maxScore ?? assignment.maxScore)}<br/>{GRADE_STATUS_LABELS[grade?.status ?? "NOT_SUBMITTED"] ?? grade?.status}</td>
            </tr>;
          })}</tbody>
        </table></div>
        {!value.assignments.length && <p>{msg("grading.components.StudentGradesSection.chuaCoBaiTinhDiemTrong")}</p>}
      </>}
    </section>
  );
}
