"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { diagramTypeLabels, type DiagramType } from "@/shared/document";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, Button, ButtonLink, PageHeader, Pill, Section } from "@/shared/ui";
import { workApi } from "./api";
import { errorMessage, formatDateTime } from "./format";
import { assignmentMaterialSections, groupWorkEntries, submissionResultSections } from "./slots";
import { blockedLabels, myStatusLabels, type StudentDetail } from "./types";

/**
 * #39 Assignment Detail (F2, UC 38): trang thông tin trước khi làm bài (yêu cầu 2026-10-21 thay BR-U08-03). Chỉ hiện
 * thông tin bài (hình thức, chế độ, lịch, số lượt, sơ đồ cần có, lời báo theo dõi), không hiện đề và rubric; đề chỉ mở
 * khi bấm Bắt đầu hoặc Tiếp tục.
 */
export function StudentAssignmentDetailPage({ assignmentId }: { assignmentId: string }) {
  const router = useRouter();
  const classId = useSearchParams().get("classId") ?? "";
  const [detail, setDetail] = useState<StudentDetail | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [starting, setStarting] = useState(false);

  useEffect(() => {
    if (!classId) return;
    const controller = new AbortController();
    workApi.detail(assignmentId, classId, controller.signal).then(
      (value) => { if (!controller.signal.aborted) setDetail(value); },
      (e) => { if (!controller.signal.aborted) setError(e); },
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
  if (error && !detail) return <Alert tone="danger" role="alert">{errorMessage(error)}</Alert>;
  if (!detail) return <p role="status" className="text-sm text-muted">Đang tải bài tập…</p>;

  const history = `/my-assignments/${encodeURIComponent(assignmentId)}/history?classId=${encodeURIComponent(classId)}`;
  const required = Object.entries(detail.requiredDiagrams) as [DiagramType, number][];
  const attemptsLeft = Math.max(0, (detail.maxAttempts ?? 1) - detail.attemptsUsed);
  const latest = detail.latestAttempt;
  return (
    <div className="flex flex-col gap-4">
      <PageHeader backHref={`/my-assignments?classId=${encodeURIComponent(classId)}`}
        title={<>{detail.title}<Pill tone={detail.gradingMode === "GRADED" ? "primary" : "accent"}>{detail.gradingMode === "GRADED" ? "Graded" : "Practice"}</Pill><Pill>{detail.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"}</Pill></>}
        breadcrumb={[{ label: "Bài tập", href: "/my-assignments" }, { label: detail.title }]} />
      {error !== null && <Alert tone="danger" role="alert">{errorMessage(error)}</Alert>}
      <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div className="flex min-w-0 flex-col gap-4">
          <Section title="Thông tin bài" icon="info">
            <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-[13px]" data-testid="assignment-info">
              <dt className="text-muted">Lớp</dt><dd>{detail.classCode} · {detail.className}{detail.scope === "SUBJECT" ? " (bài của môn)" : ""}</dd>
              <dt className="text-muted">Hình thức</dt><dd>{detail.submissionMode === "GROUP" ? "Bài nhóm" : "Bài cá nhân"}</dd>
              <dt className="text-muted">Chế độ</dt><dd>{detail.gradingMode === "GRADED" ? "Graded" : "Practice · không tính điểm, có góp ý AI sau khi nộp"}</dd>
              <dt className="text-muted">Số phần cần làm</dt><dd>{detail.partCount}</dd>
              {required.length > 0 && (<><dt className="text-muted">Sơ đồ cần có</dt><dd>{required.map(([type, count]) => `${count} ${diagramTypeLabels[type]}`).join(", ")}</dd></>)}
              <dt className="text-muted">Mở lúc</dt><dd>{formatDateTime(detail.opensAt)}</dd>
              <dt className="text-muted">Hạn nộp</dt><dd>{formatDateTime(detail.closesAt)}</dd>
              <dt className="text-muted">Nộp trễ</dt><dd>{detail.lateUntil ? `đến ${formatDateTime(detail.lateUntil)}` : "Không cho phép"}</dd>
              <dt className="text-muted">Số lượt</dt><dd>Đã dùng {detail.attemptsUsed} / {detail.maxAttempts ?? 1} · còn {attemptsLeft}</dd>
            </dl>
            {detail.activityMonitoringEnabled && detail.activityNotice && (
              <Alert tone="info" title="Bài có ghi nhận hoạt động">{detail.activityNotice}</Alert>
            )}
          </Section>
          <SlotOutlet slot={assignmentMaterialSections} props={{ classId, materialIds: detail.referenceMaterialIds }} />
          {latest?.graded && <SlotOutlet slot={submissionResultSections} props={{ attemptId: latest.attemptId }} />}
        </div>
        <div className="flex flex-col gap-4">
          <Section title="Làm bài" icon="assignment">
            {latest && (
              <p className="text-xs text-body">
                Lượt gần nhất: lượt {latest.attemptNo} · {latest.status === "IN_PROGRESS" ? myStatusLabels.IN_PROGRESS
                  : latest.late ? myStatusLabels.SUBMITTED_LATE : myStatusLabels.SUBMITTED}
                {latest.submittedAt ? ` lúc ${formatDateTime(latest.submittedAt)}` : ""}
              </p>
            )}
            {detail.submissionMode === "GROUP" ? (
              <SlotOutlet slot={groupWorkEntries} props={{ assignmentId, classId }} />
            ) : detail.inProgressAttemptId ? (
              <div className="flex flex-col gap-1">
                <ButtonLink variant="primary" href={`/attempts/${encodeURIComponent(detail.inProgressAttemptId)}`}>
                  Tiếp tục lượt {latest?.attemptNo ?? ""}
                </ButtonLink>
                {latest?.lastSavedAt && <p className="text-[11px] text-muted">Bạn có một lượt làm dở, tự lưu lúc {formatDateTime(latest.lastSavedAt)}</p>}
              </div>
            ) : detail.canStart ? (
              <Button variant="primary" onClick={() => void start()} disabled={starting} data-testid="start-attempt">Bắt đầu làm bài</Button>
            ) : (
              <p className="text-xs text-muted">{blockedLabels[detail.startBlockedReason ?? ""] ?? "Chưa làm được bài này"}</p>
            )}
            <ButtonLink href={history}>Xem lịch sử nộp bài</ButtonLink>
          </Section>
          {detail.gradingMode === "PRACTICE" && (
            <Section title="Góp ý AI" icon="smart_toy">
              <p className="text-xs text-body">Sau khi nộp, bạn nhận góp ý bằng API cá nhân (hoặc agent MCP tự gọi công cụ). Góp ý chỉ là ước tính, không phải điểm chính thức.</p>
            </Section>
          )}
        </div>
      </div>
    </div>
  );
}
