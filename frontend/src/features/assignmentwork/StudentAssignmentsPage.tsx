"use client";

import Link from "next/link";
import { useState, type CSSProperties } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, ButtonLink, EmptyState, Icon, PageHeader, Pill, Segmented, Select } from "@/shared/ui";
import { DeadlineChip } from "./DeadlineChip";
import { errorMessage, formatDateTime } from "./format";
import { studentAssignmentResults } from "./slots";
import { myStatusLabels, myStatusTones, type StudentRow } from "./types";
import { upcoming, useStudentAssignments } from "./useStudentAssignments";

type Tab = "upcoming" | "submitted" | "all";

function detailHref(row: StudentRow) {
  return `/my-assignments/${encodeURIComponent(row.assignmentId)}?classId=${encodeURIComponent(row.classId)}`;
}

/** #38 Student Assignments (F1, UC 37): bài của các lớp đang học dạng danh sách dòng, sắp theo hạn; điểm do U09 gắn. */
export function StudentAssignmentsPage() {
  const router = useRouter();
  const params = useSearchParams();
  const classId = params.get("classId") ?? undefined;
  const [tab, setTab] = useState<Tab>("upcoming");
  const [mode, setMode] = useState("");
  const { rows, error, loading } = useStudentAssignments(classId);

  const classes = [...new Map((rows ?? []).map((row) => [row.classId, `${row.classCode} · ${row.className}`])).entries()];
  const inTab = (row: StudentRow, key: Tab) => key === "all" || (key === "upcoming" ? upcoming(row) : row.submittedCount > 0 || !!row.group?.submitted);
  const visible = (rows ?? []).filter((row) => inTab(row, tab) && (!mode || row.submissionMode === mode || row.gradingMode === mode));
  const count = (key: Tab) => (rows ?? []).filter((row) => inTab(row, key)).length;

  return (
    <>
      <PageHeader title="Bài tập" subtitle={rows ? `${count("upcoming")} bài cần làm` : undefined} />
      <div className="flex flex-wrap items-center gap-2">
        <Segmented as="tab" ariaLabel="Nhóm bài" value={tab} onChange={setTab} options={[
          { value: "upcoming", label: `Cần làm · ${count("upcoming")}` },
          { value: "submitted", label: `Đã nộp · ${count("submitted")}` },
          { value: "all", label: "Tất cả" },
        ]} />
        <div className="ml-auto flex flex-wrap gap-2">
          <Select aria-label="Lớp" value={classId ?? ""} wrapperClassName="w-52"
            onChange={(e) => router.push(e.target.value ? `/my-assignments?classId=${encodeURIComponent(e.target.value)}` : "/my-assignments")}>
            <option value="">Mọi lớp</option>
            {classes.map(([id, label]) => <option key={id} value={id}>{label}</option>)}
          </Select>
          <Select aria-label="Loại bài" value={mode} wrapperClassName="w-40" onChange={(e) => setMode(e.target.value)}>
            <option value="">Mọi loại</option>
            <option value="INDIVIDUAL">Cá nhân</option>
            <option value="GROUP">Nhóm</option>
            <option value="GRADED">Tính điểm</option>
            <option value="PRACTICE">Luyện tập</option>
          </Select>
        </div>
      </div>
      {loading && <p role="status" className="text-sm text-muted">Đang tải bài tập…</p>}
      {error !== null && <Alert tone="danger" role="alert">{errorMessage(error)}</Alert>}
      {rows && visible.length === 0 && <EmptyState title={tab === "upcoming" ? "Không còn bài cần làm" : "Không có bài tập nào"} />}
      {rows && visible.length > 0 && (
        <ul className="stagger flex flex-col" aria-label="Danh sách bài tập">
          {visible.map((row, index) => (
            <li key={`${row.classId}-${row.assignmentId}`} style={{ "--i": Math.min(index, 8) } as CSSProperties}
              className="group relative -mx-3 flex flex-wrap items-center gap-x-4 gap-y-2 rounded-lg border-b border-line px-3 py-3.5 transition-colors duration-200 hover:bg-surface"
              data-testid={`student-row-${row.assignmentId}`}>
              <span className="w-[104px] shrink-0">
                {upcoming(row) ? <DeadlineChip closesAt={row.closesAt} /> : <Pill tone={myStatusTones[row.myStatus]}>{myStatusLabels[row.myStatus]}</Pill>}
              </span>
              <Link href={detailHref(row)} className="min-w-0 flex-1 after:absolute after:inset-0 after:content-['']">
                <span className="block truncate font-semibold text-ink">{row.title}</span>
                <span className="block truncate text-[12.5px] text-muted">
                  {row.classCode}{row.scope === "SUBJECT" ? " · Của môn" : ""} · {row.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"}
                  {row.gradingMode === "PRACTICE" ? " · Luyện tập" : ""}
                  {row.submittedCount > 0 ? ` · Đã nộp ${row.submittedCount} lượt` : ""}
                  {row.myStatus === "GROUP" && row.group ? (row.group.submitted ? " · Nhóm đã nộp" : " · Nhóm đang làm") : ""}
                </span>
              </Link>
              <span className="font-mono text-xs text-muted">{formatDateTime(row.closesAt)}</span>
              {row.gradingMode === "GRADED" && (
                <span className="relative z-10"><SlotOutlet slot={studentAssignmentResults} props={{ assignmentId: row.assignmentId, classId: row.classId }} /></span>
              )}
              {row.inProgressAttemptId ? (
                <span className="relative z-10"><ButtonLink size="sm" variant="primary" href={`/attempts/${encodeURIComponent(row.inProgressAttemptId)}`}>Tiếp tục</ButtonLink></span>
              ) : (
                <Icon name="chevron_right" size={16} className="text-muted transition-transform duration-200 group-hover:translate-x-0.5 group-hover:text-ink" />
              )}
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
