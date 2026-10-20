"use client";
import Link from "next/link";
import { useCallback, useState } from "react";
import { Pagination } from "@/shared/ui/Pagination";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
import { points, statusLabels } from "./types";
import { GradebookExportDialog } from "./GradebookExportDialog";
export function ClassGradebookTab({ classId }: { classId: string }) {
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(1);
  const [exporting, setExporting] = useState(false);
  const load = useCallback(() => gradingApi.gradebook(classId, query, page), [classId, query, page]);
  const resource = useResource(load);
  const data = resource.value;
  return (
    <section className="space-y-4">
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold">Sổ điểm</h2>
          <p>
            {data ? `${data.total} sinh viên · ${data.assignments.length} bài Graded` : "Đang tải…"}
          </p>
        </div>
        <button
          disabled={!data}
          onClick={() => setExporting(true)}
          className="rounded-lg bg-primary px-4 py-2 text-white"
        >
          Xuất sổ điểm
        </button>
      </header>
      <label>
        Tìm sinh viên{" "}
        <input
          className="rounded-lg border p-2"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setPage(1);
          }}
          placeholder="Tên hoặc email"
        />
      </label>
      {resource.error && <p role="alert">{resource.error}</p>}
      {data && (
        <>
          <div className="overflow-auto rounded-xl border border-line bg-white">
            <table className="w-full text-left">
              <thead className="bg-slate-50">
                <tr>
                  <th className="p-3">Sinh viên</th>
                  {data.assignments.map((assignment) => (
                    <th key={assignment.id} className="min-w-44 p-3">
                      <Link
                        className="text-primary"
                        href={`/teaching/classes/${classId}/assignments/${assignment.id}/submissions`}
                      >
                        {assignment.title}
                      </Link>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data.rows.map((row) => (
                  <tr key={row.student.accountId} className="border-t border-line">
                    <td className="p-3">
                      <strong>{row.student.name}</strong>
                      <p className="text-sm text-slate-500">{row.student.email}</p>
                    </td>
                    {row.grades.map((grade) => (
                      <td key={grade.assignmentId} className="p-3">
                        {grade.evaluationId ? (
                          <Link
                            href={`/teaching/evaluations/${grade.evaluationId}`}
                            className="font-semibold text-primary"
                          >
                            {points(grade.score, grade.maxScore)}
                          </Link>
                        ) : (
                          "—"
                        )}
                        <p className="text-sm text-slate-500">
                          {statusLabels[grade.status] ?? grade.status}
                          {grade.late && " · Trễ"}
                        </p>
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
            {!data.rows.length && <p className="p-6">Không có sinh viên phù hợp.</p>}
          </div>
          <Pagination
            page={page - 1}
            pageSize={20}
            total={data.total}
            onPageChange={(p) => setPage(p + 1)}
          />
        </>
      )}
      <p className="text-sm text-slate-600">
        Chỉ bài Graded và lượt được chấm. Không cộng tổng điểm hoặc trọng số; không bao gồm Practice
        và Quiz.
      </p>
      {exporting && data && (
        <GradebookExportDialog
          classId={classId}
          assignments={data.assignments}
          onClose={() => setExporting(false)}
        />
      )}
    </section>
  );
}
