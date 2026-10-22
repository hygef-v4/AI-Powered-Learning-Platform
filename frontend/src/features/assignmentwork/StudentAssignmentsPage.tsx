"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, ButtonLink, ClickableRow, PageHeader, Pill, Select, TableFrame, Td, Th, cx } from "@/shared/ui";
import { errorMessage, formatDateTime } from "./format";
import { studentAssignmentResults } from "./slots";
import { myStatusLabels, myStatusTones, type StudentRow } from "./types";
import { upcoming, useStudentAssignments } from "./useStudentAssignments";

type Tab = "upcoming" | "submitted" | "all";

function detailHref(row: StudentRow) {
  return `/my-assignments/${encodeURIComponent(row.assignmentId)}?classId=${encodeURIComponent(row.classId)}`;
}

/** #38 Student Assignments (F1, UC 37): bài của các lớp đang học, sắp theo hạn; điểm do U09 gắn. */
export function StudentAssignmentsPage() {
  const router = useRouter();
  const params = useSearchParams();
  const classId = params.get("classId") ?? undefined;
  const [tab, setTab] = useState<Tab>("upcoming");
  const [mode, setMode] = useState("");
  const [grading, setGrading] = useState("");
  const { rows, error, loading } = useStudentAssignments(classId);

  const classes = [...new Map((rows ?? []).map((row) => [row.classId, `${row.classCode} · ${row.className}`])).entries()];
  const visible = (rows ?? []).filter((row) =>
    (tab === "all" || (tab === "upcoming" ? upcoming(row) : row.submittedCount > 0 || row.group?.submitted))
    && (!mode || row.submissionMode === mode) && (!grading || row.gradingMode === grading));

  return (
    <div className="flex flex-col gap-4">
      <PageHeader title="Bài tập" subtitle="Bài tập của các lớp bạn đang học. Quiz luyện tập ở mục Luyện tập Quiz." />
      <div className="flex flex-wrap items-center gap-2">
        <div role="tablist" aria-label="Nhóm bài" className="flex gap-1 rounded-[10px] bg-shell p-1">
          {([["upcoming", "Sắp đến hạn"], ["submitted", "Đã nộp"], ["all", "Tất cả"]] as const).map(([key, label]) => (
            <button key={key} type="button" role="tab" aria-selected={tab === key} onClick={() => setTab(key)}
              className={cx("rounded-lg px-3 py-1 text-xs font-semibold", tab === key ? "bg-white text-ink shadow-sm" : "text-muted")}>{label}</button>
          ))}
        </div>
        <Select aria-label="Lớp" value={classId ?? ""} wrapperClassName="w-56"
          onChange={(e) => router.push(e.target.value ? `/my-assignments?classId=${encodeURIComponent(e.target.value)}` : "/my-assignments")}>
          <option value="">Mọi lớp</option>
          {classes.map(([id, label]) => <option key={id} value={id}>{label}</option>)}
        </Select>
        <Select aria-label="Hình thức" value={mode} wrapperClassName="w-36" onChange={(e) => setMode(e.target.value)}>
          <option value="">Mọi hình thức</option><option value="INDIVIDUAL">Cá nhân</option><option value="GROUP">Nhóm</option>
        </Select>
        <Select aria-label="Chế độ" value={grading} wrapperClassName="w-36" onChange={(e) => setGrading(e.target.value)}>
          <option value="">Mọi chế độ</option><option value="GRADED">Graded</option><option value="PRACTICE">Practice</option>
        </Select>
      </div>
      {loading && <p role="status" className="text-sm text-muted">Đang tải bài tập…</p>}
      {error !== null && <Alert tone="danger" role="alert">{errorMessage(error)}</Alert>}
      {rows && visible.length === 0 && <p className="py-8 text-center text-sm text-muted">Không có bài tập nào.</p>}
      {rows && visible.length > 0 && (
        <TableFrame>
          <thead>
            <tr><Th>Bài tập</Th><Th>Hình thức</Th><Th>Chế độ</Th><Th>Hạn nộp</Th><Th>Trạng thái</Th><Th>Điểm</Th><Th /></tr>
          </thead>
          <tbody>
            {visible.map((row) => (
              <ClickableRow key={`${row.classId}-${row.assignmentId}`} onClick={() => router.push(detailHref(row))} data-testid={`student-row-${row.assignmentId}`}>
                <Td>
                  <p className="font-semibold">{row.title}</p>
                  <p className="text-[11px] text-muted">{row.classCode}{row.scope === "SUBJECT" ? " · Của môn" : ""}</p>
                </Td>
                <Td>{row.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"}</Td>
                <Td><Pill tone={row.gradingMode === "GRADED" ? "info" : "accent"}>{row.gradingMode === "GRADED" ? "Graded" : "Practice"}</Pill></Td>
                <Td className="text-xs whitespace-nowrap">{formatDateTime(row.closesAt)}</Td>
                <Td>
                  <Pill tone={myStatusTones[row.myStatus]}>
                    {row.myStatus === "SUBMITTED" || row.myStatus === "SUBMITTED_LATE" ? `${myStatusLabels[row.myStatus]} ${row.submittedCount} lượt` : myStatusLabels[row.myStatus]}
                  </Pill>
                  {row.myStatus === "GROUP" && row.group && <span className="ml-1 text-[11px] text-muted">{row.group.submitted ? "Nhóm đã nộp" : "Nhóm đang làm"}</span>}
                </Td>
                <Td onClick={(e) => e.stopPropagation()}>
                  {row.gradingMode === "PRACTICE" ? <span className="text-xs text-muted">Không tính điểm</span>
                    : <SlotOutlet slot={studentAssignmentResults} props={{ assignmentId: row.assignmentId, classId: row.classId }} />}
                </Td>
                <Td onClick={(e) => e.stopPropagation()}>
                  {row.inProgressAttemptId ? <ButtonLink size="sm" variant="primary" href={`/attempts/${encodeURIComponent(row.inProgressAttemptId)}`}>Tiếp tục</ButtonLink>
                    : <ButtonLink size="sm" href={detailHref(row)}>{upcoming(row) && row.myStatus === "NOT_STARTED" ? "Làm bài" : "Xem"}</ButtonLink>}
                </Td>
              </ClickableRow>
            ))}
          </tbody>
        </TableFrame>
      )}
    </div>
  );
}
