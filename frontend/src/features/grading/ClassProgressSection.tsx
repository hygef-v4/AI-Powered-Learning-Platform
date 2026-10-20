"use client";
import Link from "next/link";
import { useCallback } from "react";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
export function ClassProgressSection({ classId }: { classId: string }) {
  const load = useCallback(async () => {
    const [progress, gradebook] = await Promise.all([
      gradingApi.progress(classId),
      gradingApi.gradebook(classId),
    ]);
    return { progress, assignments: gradebook.assignments };
  }, [classId]);
  const { value, error } = useResource(load);
  return (
    <section className="mt-5 rounded-xl border bg-white p-5">
      <h2 className="text-lg font-bold">Tiến độ nộp bài</h2>
      {error && <p role="alert">{error}</p>}
      {value &&
        value.assignments.map((a) => {
          const p = value.progress[a.id];
          return (
            <div key={a.id} className="my-3 flex items-center justify-between gap-4">
              <Link
                className="text-primary"
                href={`/teaching/classes/${classId}/assignments/${a.id}/submissions`}
              >
                {a.title}
              </Link>
              <span>
                {p
                  ? `${p.summary.submitted ?? 0} / ${p.summary.total ?? 0} đã nộp · ${p.summary.PENDING ?? 0} chờ chấm`
                  : "Chưa có dữ liệu"}
              </span>
            </div>
          );
        })}
      {value && !value.assignments.length && <p>Chưa có bài Graded trong lớp.</p>}
    </section>
  );
}
