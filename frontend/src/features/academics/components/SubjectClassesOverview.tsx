"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Button, Section } from "@/shared/ui";
import { academicsApi } from "../api";
import { formatOperationError } from "../model/format";
import type { CourseClassView } from "../model/types";
import { msg } from "@/i18n/msg";

type OverviewState = { subjectId: string; classes?: CourseClassView[]; error?: string };

/** Lớp đang mở của môn (một trang đủ cho một môn; danh sách đầy đủ ở mục Lớp học). */
async function listOpenClasses(subjectId: string, signal: AbortSignal) {
  return academicsApi.listClasses("manager", { subjectId, status: "OPEN", size: 100 }, signal);
}

/**
 * Tổng quan lớp trên Thông tin môn, cùng kiểu Tổng quan bài tập trên Thông tin lớp của Giảng viên: hàng số liệu (lớp đang mở,
 * sinh viên, lớp chưa có giảng viên) rồi danh sách lớp đang mở.
 */
export function SubjectClassesOverview({ subjectId }: { subjectId: string }) {
  const [state, setState] = useState<OverviewState>({ subjectId });
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const request = new AbortController();
    listOpenClasses(subjectId, request.signal).then(
      (classes) => setState({ subjectId, classes }),
      (error: unknown) => {
        if (!request.signal.aborted) setState({ subjectId, error: formatOperationError(error) });
      },
    );
    return () => request.abort();
  }, [subjectId, attempt]);

  const current = state.subjectId === subjectId ? state : { subjectId };
  const classesHref = `/manager/classes?subjectId=${encodeURIComponent(subjectId)}`;
  const classes = current.classes ?? [];
  const stats = [
    { label: msg("academics.components.SubjectClassesOverview.openClasses"), count: classes.length },
    { label: msg("academics.components.SubjectClassesOverview.students"), count: classes.reduce((sum, course) => sum + course.activeStudentCount, 0) },
    { label: msg("academics.components.SubjectClassesOverview.classesWithoutALecturer"), count: classes.filter((course) => !course.teacher).length },
  ];

  return (
    <Section title={msg("academics.components.SubjectClassesOverview.classOverview")}>
      {current.error ? (
        <div className="flex flex-col items-start gap-3">
          <p role="alert" className="text-sm text-danger">{current.error}</p>
          <Button size="sm" variant="secondary" onClick={() => setAttempt((value) => value + 1)}>{msg("academics.components.SubjectClassesOverview.tryAgain")}</Button>
        </div>
      ) : !current.classes ? (
        <p role="status" className="text-sm text-muted">{msg("academics.components.SubjectClassesOverview.loadingOverview")}</p>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            {stats.map((item) => (
              <Link key={item.label} href={classesHref} aria-label={`${item.count} ${item.label}`}
                className="row-interactive flex items-baseline gap-3 rounded-lg px-2 py-3 hover:bg-shell">
                <span className="text-2xl font-semibold tabular-nums text-ink">{item.count}</span>
                <span className="text-sm text-body">{item.label}</span>
              </Link>
            ))}
          </div>
          <div className="border-t border-line pt-5">
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
              <h3 className="text-sm font-semibold text-ink">{msg("academics.components.SubjectClassesOverview.openClasses")}</h3>
              <Link href={classesHref} className="text-[13px] font-semibold text-primary-ink hover:underline">{msg("academics.components.SubjectClassesOverview.allClasses")}</Link>
            </div>
            {classes.length === 0 ? <p className="text-sm text-muted">{msg("academics.components.SubjectClassesOverview.noOpenClassesYet")}</p> : (
              <ul className="divide-y divide-line">
                {classes.map((course) => (
                  <li key={course.id} data-testid="subject-open-class">
                    <Link href={`/manager/classes/${encodeURIComponent(course.id)}`}
                      className="row-interactive -mx-2 flex flex-wrap items-center justify-between gap-x-6 gap-y-2 rounded-lg px-2 py-3 hover:bg-shell">
                      <span className="min-w-0 flex-1 basis-48">
                        <span className="block break-words text-sm font-semibold text-ink">{course.code}</span>
                        <span className="text-xs text-muted">{course.teacher?.name ?? msg("academics.components.SubjectClassesOverview.noLecturerAssigned")}</span>
                      </span>
                      <span className="text-[13px] tabular-nums text-muted">{course.activeStudentCount} {msg("academics.components.SubjectClassesOverview.students2")}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </Section>
  );
}
