"use client";
import Link from "next/link";
import { useCallback } from "react";
import { assignmentsApi } from "@/features/assignments/api";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
export function ClassProgressSection({ classId }: { classId: string }) {
  const load = useCallback(async () => {
    const [progress, gradebook, openAssignments] = await Promise.all([
      gradingApi.progress(classId),
      gradingApi.gradebook(classId),
      assignmentsApi.listForClass(classId, { status: ["OPEN"] }),
    ]);
    const today = new Date().toLocaleDateString("vi-VN");
    const dueToday = openAssignments.items.filter(assignment => assignment.closesAt &&
      new Date(assignment.closesAt).toLocaleDateString("vi-VN") === today).length;
    return { progress, assignments: gradebook.assignments, openCount: openAssignments.items.length, dueToday };
  }, [classId]);
  const { value, error } = useResource(load);
  return (
    <section className="mt-5 rounded-xl border bg-white p-5">
      <h2 className="text-lg font-bold">Tiến độ nộp bài</h2>
      {error && <p role="alert">{error}</p>}
      {value && <dl className="my-4 grid gap-3 sm:grid-cols-2">
        <div className="rounded-lg border p-3"><dt>Bài đang mở</dt><dd className="text-2xl font-bold">{value.openCount}</dd><dd className="text-sm">{value.dueToday} bài hết hạn hôm nay</dd></div>
        <div className="rounded-lg border p-3"><dt>Chờ chấm</dt><dd className="text-2xl font-bold">{Object.values(value.progress).reduce((total, page) => total + (page.summary.PENDING ?? 0), 0)}</dd></div>
      </dl>}
      {value &&
        value.assignments.map((a) => {
          const p = value.progress[a.id];
          const total = p?.summary.total ?? 0;
          const submitted = p?.summary.submitted ?? 0;
          const percent = total > 0 ? Math.round(100 * submitted / total) : 0;
          return (
            <div key={a.id} className="my-3 rounded-lg border p-3">
              <Link
                className="text-primary"
                href={`/teaching/classes/${classId}/assignments/${a.id}/submissions`}
              >
                {a.title}
              </Link>
              <p className="mt-1 text-sm">{a.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"} · Graded · {p?.deadline ? `Hạn: ${new Date(p.deadline).toLocaleString("vi-VN")}` : "Không có hạn nộp"}</p>
              <p className="mt-2 text-sm">
                {p
                  ? `${submitted} / ${total} ${a.submissionMode === "GROUP" ? "nhóm" : "sinh viên"} đã nộp (${percent}%) · ${p.summary.PENDING ?? 0} chờ chấm`
                  : "Chưa có dữ liệu"}
              </p>
              {p && <progress className="mt-2 w-full" aria-label={`Tiến độ nộp ${a.title}`} max={Math.max(1, total)} value={submitted}/>}
              {(p?.summary.PUBLISHED ?? 0) > 0 && <PublishedDistribution classId={classId} assignmentId={a.id}/>}
            </div>
          );
        })}
      {value && !value.assignments.length && <p>Chưa có bài Graded trong lớp.</p>}
    </section>
  );
}

function PublishedDistribution({ classId, assignmentId }: { classId: string; assignmentId: string }) {
  const load = useCallback(() => gradingApi.classDistribution(classId, assignmentId), [classId, assignmentId]);
  const { value, error } = useResource(load);
  return <section className="mt-3">
    <h3 className="font-semibold">Phân bố điểm đã công bố</h3>
    {error && <p role="alert">{error}</p>}
    {!value && !error && <p role="status">Đang tải phân bố điểm…</p>}
    {value?.length === 0 && <p className="text-sm">Chưa đủ dữ liệu để hiển thị phân bố điểm.</p>}
    {value && value.length > 0 && <ul className="mt-2 space-y-2">{value.map(bucket => <li key={bucket.label} className="flex items-center gap-3 text-sm">
      <span className="w-20">{bucket.label}</span>
      <progress className="flex-1" aria-label={`Mức điểm ${bucket.label}`} max={Math.max(1, ...value.map(item => item.count))} value={bucket.count}/>
      <span>{bucket.count} sinh viên</span>
    </li>)}</ul>}
  </section>;
}
