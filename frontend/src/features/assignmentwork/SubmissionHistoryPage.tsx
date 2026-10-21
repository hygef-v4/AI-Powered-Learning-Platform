"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { DocumentViewer } from "@/shared/document";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, PageHeader, Pill, Section, cx } from "@/shared/ui";
import { attemptImageUrl, exportUrl, workApi } from "./api";
import { errorMessage, formatDateTime } from "./format";
import { PracticeFeedbackPanel } from "./PracticeFeedbackPanel";
import { submissionResultSections } from "./slots";
import { submitModeLabels, type Attempt, type AttemptSummary, type StudentDetail } from "./types";

/** #41 Submission History (F8, UC 40): các lượt của mình, nội dung chỉ đọc, biên nhận, góp ý AI của bài Practice. */
export function SubmissionHistoryPage({ assignmentId }: { assignmentId: string }) {
  const classId = useSearchParams().get("classId") ?? "";
  const [detail, setDetail] = useState<StudentDetail | null>(null);
  const [attempts, setAttempts] = useState<AttemptSummary[] | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    if (!classId) return;
    const controller = new AbortController();
    Promise.all([workApi.detail(assignmentId, classId, controller.signal), workApi.history(assignmentId, classId, controller.signal)])
      .then(([value, list]) => {
        if (controller.signal.aborted) return;
        setDetail(value);
        setAttempts(list);
        setSelected(list.find((a) => a.status === "SUBMITTED")?.attemptId ?? list[0]?.attemptId ?? null);
      }, (e) => { if (!controller.signal.aborted) setError(e); });
    return () => controller.abort();
  }, [assignmentId, classId]);

  useEffect(() => {
    if (!selected) return;
    const controller = new AbortController();
    workApi.attempt(selected, controller.signal).then((value) => { if (!controller.signal.aborted) setAttempt(value); },
      (e) => { if (!controller.signal.aborted) setError(e); });
    return () => controller.abort();
  }, [selected]);

  if (!classId) return <Alert tone="danger" role="alert">Thiếu lớp của bài.</Alert>;
  if (error) return <Alert tone="danger" role="alert">{errorMessage(error)}</Alert>;
  if (!detail || !attempts) return <p role="status" className="text-sm text-muted">Đang tải lịch sử nộp bài…</p>;
  const current = attempts.find((a) => a.attemptId === selected) ?? null;
  const detailHref = `/my-assignments/${encodeURIComponent(assignmentId)}?classId=${encodeURIComponent(classId)}`;

  return (
    <div className="flex flex-col gap-4">
      <PageHeader backHref={detailHref} title="Lịch sử nộp bài"
        breadcrumb={[{ label: "Bài tập", href: "/my-assignments" }, { label: detail.title, href: detailHref }, { label: "Lịch sử nộp bài" }]} />
      <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr)_300px]">
        <div className="flex min-w-0 flex-col gap-4">
          {current && attempt?.attemptId === current.attemptId ? (
            <Section title={`Bài nộp · lượt ${current.attemptNo}`} icon="description"
              description={current.submittedAt ? `${formatDateTime(current.submittedAt)} · ${current.receiptHash?.slice(0, 12) ?? ""}` : "Đang làm"}
              actions={<a href={exportUrl(current.attemptId)} className="text-xs font-semibold text-primary">Tải DOCX</a>}>
              {current.warnings.length > 0 && <Alert tone="warning" title={current.submitMode ? submitModeLabels[current.submitMode] : undefined}>
                {current.warnings.map((w) => w.message).join("; ")}</Alert>}
              <DocumentViewer document={attempt.content} imageUrl={attemptImageUrl(current.attemptId)} markTeacherBlocks />
            </Section>
          ) : <p role="status" className="text-sm text-muted">{attempts.length === 0 ? "Bạn chưa có lượt nào." : "Đang tải lượt…"}</p>}
          {current?.graded && <SlotOutlet slot={submissionResultSections} props={{ attemptId: current.attemptId }} />}
          {detail.gradingMode === "PRACTICE" && current?.status === "SUBMITTED" && (
            <PracticeFeedbackPanel attemptId={current.attemptId}
              partTitles={attempt ? Object.fromEntries(attempt.parts.map((part) => [part.partId, part.title])) : undefined} />
          )}
        </div>
        <Section title="Các lượt nộp" icon="history">
          <ul className="flex flex-col gap-1.5">
            {attempts.map((a) => (
              <li key={a.attemptId}>
                <button type="button" onClick={() => setSelected(a.attemptId)}
                  className={cx("flex w-full flex-col items-start rounded-lg border px-3 py-2 text-left", a.attemptId === selected ? "border-primary bg-primary-soft" : "border-line hover:bg-canvas")}>
                  <span className="flex items-center gap-1.5 text-[13px] font-semibold text-ink">
                    Lượt {a.attemptNo}
                    {a.status === "IN_PROGRESS" ? <Pill tone="info">Đang làm</Pill> : a.late ? <Pill tone="warning">Trễ</Pill> : <Pill tone="success">Đã nộp</Pill>}
                  </span>
                  <span className="text-[11px] text-muted">{a.submittedAt ? formatDateTime(a.submittedAt) : "Chưa nộp"}</span>
                  {detail.gradingMode === "GRADED" && a.status === "SUBMITTED" && <span className="text-[11px] text-muted">{a.graded ? "Được chấm" : "Không chấm"}</span>}
                  {detail.gradingMode === "PRACTICE" && a.status === "SUBMITTED" && <span className="text-[11px] text-muted">{a.feedbackCount > 0 ? "Đã có góp ý AI" : "Chưa có góp ý AI"}</span>}
                </button>
              </li>
            ))}
          </ul>
        </Section>
      </div>
    </div>
  );
}
