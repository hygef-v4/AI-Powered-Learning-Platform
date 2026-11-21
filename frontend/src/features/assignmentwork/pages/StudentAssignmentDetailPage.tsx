"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, Button, ButtonLink, PageHeader, Pill, Section } from "@/shared/ui";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { workApi } from "../api";
import { describeError, formatDateTime, canOpenHistory } from "../model/format";
import { ResultSummaryCard } from "../components/ResultSummaryCard";
import { assignmentMaterialSections, groupProgressSections, groupWorkEntries } from "../slots";
import { myStatusLabels, myStatusTones, type AttemptSummary, type StudentDetail } from "../model/types";
import { resolveRowStatus, useStudentAssignments } from "../hooks/useStudentAssignments";
import { msg } from "@/i18n/msg";

/** Loại bài như trên bảng Bài tập: luyện tập là loại riêng, còn lại theo cá nhân hay nhóm. */
function describeKind(detail: StudentDetail) {
  if (detail.gradingMode === "PRACTICE") return msg("assignmentwork.pages.StudentAssignmentDetailPage.practice");
  return detail.submissionMode === "GROUP" ? msg("assignmentwork.pages.StudentAssignmentDetailPage.group") : msg("assignmentwork.pages.StudentAssignmentDetailPage.individual");
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

  if (!classId) return <Alert tone="danger" role="alert">{msg("assignmentwork.pages.StudentAssignmentDetailPage.theAssignmentsClassIsMissing")}</Alert>;
  if (error && !detail) return <PageLoadError>{describeError(error)}</PageLoadError>;
  if (!detail) return <PageLoading label={msg("assignmentwork.pages.StudentAssignmentDetailPage.loadingAssignment")} />;

  // Quay lại về danh sách kèm `focus`: danh sách tự mở đúng tab của bài theo trạng thái mới nhất lúc quay về
  // (đang làm mà đã hết hạn thì bài đã tự nộp, sẽ nằm ở tab Đã nộp).
  const focus = `focus=${encodeURIComponent(assignmentId)}`;
  const back = general ? `/my-assignments?${focus}` : `/my-assignments?classId=${encodeURIComponent(classId)}&${focus}`;
  const history = `/my-assignments/${encodeURIComponent(assignmentId)}/history?classId=${encodeURIComponent(classId)}${general ? "&from=all" : ""}`;
  const pastDeadline = !!detail.closesAt && new Date(detail.closesAt).getTime() < new Date(detail.serverTime).getTime();
  const submitted = !!row && (row.submittedCount > 0 || !!row.group?.submitted);
  // Hạn nộp trễ chỉ có nghĩa với bài chưa nộp mà đã quá hạn nộp.
  const lateDeadline = pastDeadline && !!detail.lateUntil && !submitted;
  const status = row ? resolveRowStatus(row) : null;
  const latest = detail.latestAttempt;
  const resolveStatusKey = (a: { status: string; late: boolean }) => (a.status === "IN_PROGRESS" ? "IN_PROGRESS" : a.late ? "SUBMITTED_LATE" : "SUBMITTED");
  // Bài luyện tập: mỗi lượt một dòng, có bộ đếm lượt (chỉ ở trang này). Bài cá nhân (yêu cầu 2026-11-10): chỉ một bài
  // nộp, nộp lại là ghi đè bản cũ, không đếm số lần: một dòng là bản đang sửa nếu có, không thì bản nộp mới nhất.
  const practice = detail.gradingMode === "PRACTICE";
  const newestFirst = attempts && attempts.length > 0 ? [...attempts].sort((a, b) => b.attemptNo - a.attemptNo) : null;
  const gradedAttempt = newestFirst?.find((a) => a.status === "SUBMITTED" && a.graded)
    ?? (latest?.graded ? latest : null);
  // Thẻ Làm bài chỉ liệt kê bài đã nộp; bài đang làm dở mở bằng nút Tiếp tục làm bài.
  const submittedOnly = newestFirst?.filter((a) => a.status !== "IN_PROGRESS") ?? null;
  const latestSubmitted = latest && latest.status !== "IN_PROGRESS" ? latest : null;
  const rowsOfAttempts = practice
    ? submittedOnly ?? (latestSubmitted ? [{ ...latestSubmitted, attemptNo: null }] : [])
    : (submittedOnly ? submittedOnly.slice(0, 1) : latestSubmitted ? [latestSubmitted] : []).map((a) => ({ ...a, attemptNo: null }));
  return (
    <div className="flex flex-col gap-4">
      <PageHeader backHref={back} title={detail.title} breadcrumb={[{ label: msg("assignmentwork.pages.StudentAssignmentDetailPage.assignment"), href: back }, { label: detail.title }]} />
      {error !== null && <Alert tone="danger" role="alert">{describeError(error)}</Alert>}
      <div className="grid grid-cols-1 items-start gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="flex min-w-0 flex-col gap-4">
          <Section title={msg("assignmentwork.pages.StudentAssignmentDetailPage.assignmentInformation")}>
            <dl className="grid gap-x-6 gap-y-3 text-[13px] sm:grid-cols-2" data-testid="assignment-info">
              <div><dt className="text-xs text-muted">{msg("assignmentwork.pages.StudentAssignmentDetailPage.type")}</dt><dd className="mt-0.5 font-semibold text-ink">{describeKind(detail)}</dd></div>
              <div><dt className="text-xs text-muted">{msg("assignmentwork.pages.StudentAssignmentDetailPage.status")}</dt><dd className="mt-0.5">{status ? <Pill tone={status.tone}>{status.label}</Pill> : <span className="text-muted">—</span>}</dd></div>
              <div>
                <dt className="text-xs text-muted">{lateDeadline ? msg("assignmentwork.pages.StudentAssignmentDetailPage.lateDeadline") : msg("assignmentwork.pages.StudentAssignmentDetailPage.deadline")}</dt>
                <dd className="mt-0.5 font-mono font-semibold text-ink">{formatDateTime(lateDeadline ? detail.lateUntil : detail.closesAt)}</dd>
              </div>
              <div><dt className="text-xs text-muted">{msg("assignmentwork.pages.StudentAssignmentDetailPage.work")}</dt><dd className="mt-0.5 font-semibold text-ink">{detail.partCount}</dd></div>
            </dl>
            {detail.activityMonitoringEnabled && detail.activityNotice && (
              <Alert tone="info" title={msg("assignmentwork.pages.StudentAssignmentDetailPage.activityIsMonitored")}>{detail.activityNotice}</Alert>
            )}
          </Section>
          {/* Bài nhóm: thẻ Tiến độ ngay dưới Thông tin; nhóm nộp rồi thì thành thẻ Kết quả (khi đã có điểm). */}
          {detail.submissionMode === "GROUP" && <SlotOutlet slot={groupProgressSections} props={{ assignmentId, classId }} />}
          <SlotOutlet slot={assignmentMaterialSections} props={{ classId, materialIds: detail.referenceMaterialIds }} />
          {detail.submissionMode !== "GROUP" && gradedAttempt && <ResultSummaryCard attemptId={gradedAttempt.attemptId}
            detailsHref={`${history}&attempt=${encodeURIComponent(gradedAttempt.attemptId)}`} />}
        </div>
        {/* Chỉ bài luyện tập có bộ đếm lượt; bài cá nhân, nhóm không đếm số lần nộp (yêu cầu 2026-11-10). */}
        <Section title={msg("assignmentwork.pages.StudentAssignmentDetailPage.doAssignment")} actions={practice
          ? <span className="text-[13px] text-muted tabular-nums" title={msg("assignmentwork.pages.StudentAssignmentDetailPage.attemptsUsedMax")}>{detail.attemptsUsed}/{detail.maxAttempts ?? 1} {msg("assignmentwork.pages.StudentAssignmentDetailPage.attempts")}</span>
          : undefined}>
          {rowsOfAttempts.length > 0 && (
            // Mỗi lượt một dòng; bấm để mở lịch sử nộp bài đúng lượt đó (lượt đang làm thì mở trang làm bài).
            <ul aria-label={practice ? msg("assignmentwork.pages.StudentAssignmentDetailPage.attempts2") : msg("assignmentwork.pages.StudentAssignmentDetailPage.submission")} className="-mx-2 flex flex-col">
              {rowsOfAttempts.map((a) => {
                const key = resolveStatusKey(a);
                const href = a.status === "IN_PROGRESS" ? `/attempts/${encodeURIComponent(a.attemptId)}` : `${history}&attempt=${encodeURIComponent(a.attemptId)}`;
                return (
                  <li key={a.attemptId}>
                    <Link href={href} className="row-interactive flex items-center justify-between gap-3 rounded-lg px-2 py-2 text-[13px] hover:bg-shell">
                      <span className="flex min-w-0 flex-col">
                        {practice && a.attemptNo !== null && <span className="font-semibold text-ink">{msg("assignmentwork.pages.StudentAssignmentDetailPage.attempt")}{a.attemptNo}</span>}
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
          ) : detail.inProgressAttemptId && detail.scope === "CLASS" ? (
            <ButtonLink variant="primary" href={`/attempts/${encodeURIComponent(detail.inProgressAttemptId)}`}>{msg("assignmentwork.pages.StudentAssignmentDetailPage.continueWorking")}</ButtonLink>
          ) : detail.inProgressAttemptId || detail.canStart ? (
            // Còn hạn nộp (hoặc hạn nộp trễ); bài luyện tập còn lượt. Bài cá nhân đã nộp thì sửa bài đã nộp rồi nộp lại (ghi đè).
            <Button variant="primary" onClick={() => void start()} disabled={starting} data-testid="start-attempt">
              {detail.inProgressAttemptId
                ? msg("assignmentwork.pages.StudentAssignmentDetailPage.continueWorking")
                : !practice && detail.attemptsUsed > 0
                  ? msg("assignmentwork.pages.StudentAssignmentDetailPage.editAndResubmit")
                  : msg("assignmentwork.pages.StudentAssignmentDetailPage.startWorking")}
            </Button>
          ) : null}
          {/* Bài cá nhân, nhóm: lịch sử chỉ có khi đã nộp và qua hạn nộp đúng giờ (yêu cầu 2026-11-10). */}
          {canOpenHistory(detail, submitted || !!attempts?.some((a) => a.status === "SUBMITTED")) && (
            <ButtonLink href={history}>{msg("assignmentwork.pages.StudentAssignmentDetailPage.viewSubmissionHistory")}</ButtonLink>
          )}
        </Section>
      </div>
    </div>
  );
}
