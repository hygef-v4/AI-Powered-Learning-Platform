"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, Button, ButtonLink, PageHeader, Pill, Section } from "@/shared/ui";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { workApi } from "./api";
import { errorMessage, formatDateTime } from "./format";
import { ResultSummaryCard } from "./ResultSummaryCard";
import { assignmentMaterialSections, groupWorkEntries } from "./slots";
import { myStatusLabels, myStatusTones, type AttemptSummary, type StudentDetail } from "./types";
import { rowStatus, useStudentAssignments } from "./useStudentAssignments";

/** Loại bài như trên bảng Bài tập: luyện tập là loại riêng, còn lại theo cá nhân hay nhóm. */
function kindLabel(detail: StudentDetail) {
  if (detail.gradingMode === "PRACTICE") return "Luyện tập";
  return detail.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân";
}

/**
 * #39 Assignment Detail (F2, UC 38): trang thông tin trước khi làm bài, đúng bốn mục: loại bài, trạng thái (lấy từ dòng của bài
 * trên bảng Bài tập, kể cả tiến độ bài nhóm), hạn nộp (bài đã quá hạn mà còn cho nộp trễ thì là hạn nộp trễ) và số bài (số
 * nhánh con trực tiếp của gốc tài liệu có rubric ở chính nó hoặc con cháu); thêm lời báo theo dõi nếu có, học liệu tham khảo, kết quả đã chấm. Thẻ Làm bài ghi lượt đã dùng / tối đa cạnh tiêu đề. Không hiện đề và rubric; đề chỉ mở khi bấm Bắt đầu hoặc Tiếp
 * tục. Quay lại về trang Bài tập chung nếu mở từ đó (`from=all`), không thì về Bài tập của lớp.
 */
export function StudentAssignmentDetailPage({ assignmentId }: { assignmentId: string }) {
  const router = useRouter();
  const params = useSearchParams();
  const classId = params.get("classId") ?? "";
  const general = params.get("from") === "all";
  const [detail, setDetail] = useState<StudentDetail | null>(null);
  // Các lượt làm của mình (mới nhất trước); không tải được thì chỉ hiện lượt mới nhất trong chi tiết bài.
  const [attempts, setAttempts] = useState<AttemptSummary[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [starting, setStarting] = useState(false);
  // Trạng thái lấy đúng như trên bảng Bài tập (cùng nguồn dữ liệu, có tiến độ bài nhóm).
  const { rows } = useStudentAssignments(classId || undefined);
  const row = rows?.find((item) => item.assignmentId === assignmentId);

  useEffect(() => {
    if (!classId) return;
    const controller = new AbortController();
    workApi.detail(assignmentId, classId, controller.signal).then(
      (value) => { if (!controller.signal.aborted) setDetail(value); },
      (e) => { if (!controller.signal.aborted) setError(e); },
    );
    workApi.history(assignmentId, classId, controller.signal).then(
      (value) => { if (!controller.signal.aborted) setAttempts(value); },
      () => {},
    );
    return () => controller.abort();
  }, [assignmentId, classId]);

  async function start() {
    setStarting(true);
    try {
      const attempt = await workApi.start(assignmentId, classId);
      router.push(`/attempts/${encodeURIComponent(attempt.attemptId)}`);
    } catch (e) {
      setError(e);
      setStarting(false);
    }
  }

  if (!classId) return <Alert tone="danger" role="alert">Thiếu lớp của bài.</Alert>;
  if (error && !detail) return <PageLoadError>{errorMessage(error)}</PageLoadError>;
  if (!detail) return <PageLoading label="Đang tải bài tập…" />;

  // Quay lại về danh sách kèm `focus`: danh sách tự mở đúng tab của bài theo trạng thái mới nhất lúc quay về
  // (đang làm mà đã hết hạn thì bài đã tự nộp, sẽ nằm ở tab Đã nộp).
  const focus = `focus=${encodeURIComponent(assignmentId)}`;
  const back = general ? `/my-assignments?${focus}` : `/my-assignments?classId=${encodeURIComponent(classId)}&${focus}`;
  const history = `/my-assignments/${encodeURIComponent(assignmentId)}/history?classId=${encodeURIComponent(classId)}${general ? "&from=all" : ""}`;
  const pastDeadline = !!detail.closesAt && new Date(detail.closesAt).getTime() < new Date(detail.serverTime).getTime();
  const submitted = !!row && (row.submittedCount > 0 || !!row.group?.submitted);
  // Hạn nộp trễ chỉ có nghĩa với bài chưa nộp mà đã quá hạn nộp.
  const lateDeadline = pastDeadline && !!detail.lateUntil && !submitted;
  const status = row ? rowStatus(row) : null;
  const latest = detail.latestAttempt;
  const statusKey = (a: { status: string; late: boolean }) => (a.status === "IN_PROGRESS" ? "IN_PROGRESS" : a.late ? "SUBMITTED_LATE" : "SUBMITTED");
  // Danh sách lượt: đủ các lượt khi tải được lịch sử, không thì lượt mới nhất.
  const rowsOfAttempts = attempts && attempts.length > 0
    ? [...attempts].sort((a, b) => b.attemptNo - a.attemptNo)
    : latest ? [{ ...latest, attemptNo: null }] : [];
  return (
    <div className="flex flex-col gap-4">
      <PageHeader backHref={back} title={detail.title} breadcrumb={[{ label: "Bài tập", href: back }, { label: detail.title }]} />
      {error !== null && <Alert tone="danger" role="alert">{errorMessage(error)}</Alert>}
      <div className="grid grid-cols-1 items-start gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="flex min-w-0 flex-col gap-4">
          <Section title="Thông tin bài">
            <dl className="grid gap-x-6 gap-y-3 text-[13px] sm:grid-cols-2" data-testid="assignment-info">
              <div><dt className="text-xs text-muted">Loại</dt><dd className="mt-0.5 font-semibold text-ink">{kindLabel(detail)}</dd></div>
              <div><dt className="text-xs text-muted">Trạng thái</dt><dd className="mt-0.5">{status ? <Pill tone={status.tone}>{status.label}</Pill> : <span className="text-muted">—</span>}</dd></div>
              <div>
                <dt className="text-xs text-muted">{lateDeadline ? "Hạn nộp trễ" : "Hạn nộp"}</dt>
                <dd className="mt-0.5 font-mono font-semibold text-ink">{formatDateTime(lateDeadline ? detail.lateUntil : detail.closesAt)}</dd>
              </div>
              <div><dt className="text-xs text-muted">Số bài</dt><dd className="mt-0.5 font-semibold text-ink">{detail.partCount}</dd></div>
            </dl>
            {detail.activityMonitoringEnabled && detail.activityNotice && (
              <Alert tone="info" title="Bài có ghi nhận hoạt động">{detail.activityNotice}</Alert>
            )}
          </Section>
          <SlotOutlet slot={assignmentMaterialSections} props={{ classId, materialIds: detail.referenceMaterialIds }} />
          {latest?.graded && <ResultSummaryCard attemptId={latest.attemptId} />}
        </div>
        <Section title="Làm bài" actions={<span className="text-[13px] text-muted tabular-nums" title="Lượt đã dùng / tối đa">{detail.attemptsUsed}/{detail.maxAttempts ?? 1}</span>}>
          {rowsOfAttempts.length > 0 && (
            // Mỗi lượt một dòng; bấm để mở lịch sử nộp bài đúng lượt đó (lượt đang làm thì mở trang làm bài).
            <ul aria-label="Các lượt làm" className="-mx-2 flex flex-col">
              {rowsOfAttempts.map((a) => {
                const key = statusKey(a);
                const href = a.status === "IN_PROGRESS" ? `/attempts/${encodeURIComponent(a.attemptId)}` : `${history}&attempt=${encodeURIComponent(a.attemptId)}`;
                return (
                  <li key={a.attemptId}>
                    <Link href={href} className="row-interactive flex items-center justify-between gap-3 rounded-lg px-2 py-2 text-[13px] hover:bg-shell">
                      <span className="flex min-w-0 flex-col">
                        {a.attemptNo !== null && <span className="font-semibold text-ink">Lượt {a.attemptNo}</span>}
                        <span className="font-mono text-xs text-muted">{formatDateTime(a.submittedAt ?? a.lastSavedAt ?? a.startedAt)}</span>
                      </span>
                      <Pill tone={myStatusTones[key]}>{myStatusLabels[key]}</Pill>
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
          {detail.submissionMode === "GROUP" ? (
            <SlotOutlet slot={groupWorkEntries} props={{ assignmentId, classId }} />
          ) : detail.inProgressAttemptId ? (
            <ButtonLink variant="primary" href={`/attempts/${encodeURIComponent(detail.inProgressAttemptId)}`}>Tiếp tục làm bài</ButtonLink>
          ) : detail.canStart ? (
            // Chỉ khi còn hạn nộp (hoặc còn hạn nộp trễ) và còn lượt.
            <Button variant="primary" onClick={() => void start()} disabled={starting} data-testid="start-attempt">Bắt đầu làm bài</Button>
          ) : null}
          <ButtonLink href={history}>Xem lịch sử nộp bài</ButtonLink>
        </Section>
      </div>
    </div>
  );
}
