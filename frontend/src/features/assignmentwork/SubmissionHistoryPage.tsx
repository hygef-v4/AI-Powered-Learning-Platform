"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { DocumentEditor, ToolbarButton, type DocumentComment } from "@/shared/document";
import { Alert, Icon, PageHeader, Pill } from "@/shared/ui";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { attemptImageUrl, exportUrl, workApi } from "./api";
import { formatPoints, resultByPart, useAttemptResult, type ResultPart } from "./attemptResult";
import { errorMessage, formatDateTime } from "./format";
import { PracticeFeedbackDialog } from "./PracticeFeedbackPanel";
import type { AttemptResult } from "./slots";
import { myStatusLabels, myStatusTones, type Attempt, type AttemptSummary, type StudentDetail } from "./types";

/** Trạng thái của một lượt: Đang làm (vàng), Đã nộp (xanh lá), Nộp trễ (tím), cùng màu với bảng Bài tập. */
function attemptStatus(a: AttemptSummary) {
  const key = a.status === "IN_PROGRESS" ? "IN_PROGRESS" : a.late ? "SUBMITTED_LATE" : "SUBMITTED";
  return { label: myStatusLabels[key], tone: myStatusTones[key] };
}

function attemptTime(a: AttemptSummary) {
  return formatDateTime(a.submittedAt ?? a.lastSavedAt ?? a.startedAt);
}

const NOOP = () => {};

/**
 * #41 Submission History (F8, UC 40): bài nộp hiện như trang tài liệu chỉ đọc (kiểu Docs ở chế độ chỉ xem):
 * giữa thanh công cụ, thẳng giữa trang giấy, là nút lượt trước / lượt sau và ô chọn lượt theo thời gian (mới nhất trước);
 * nút tải DOCX ở bên phải. Bên lề chỉ có điểm: tổng điểm ở đầu, điểm của từng bài gắn vào tiêu đề của bài; học sinh
 * không xem chi tiết các mục rubric. Bài luyện tập có nút Góp ý AI trên thanh công cụ (mở hộp thoại góp ý). `?attempt=` mở sẵn một lượt; quay lại về
 * đúng tab của bài trên trang Bài tập (`focus`).
 */
export function SubmissionHistoryPage({ assignmentId }: { assignmentId: string }) {
  const params = useSearchParams();
  const classId = params.get("classId") ?? "";
  const general = params.get("from") === "all";
  const wanted = params.get("attempt");
  const [detail, setDetail] = useState<StudentDetail | null>(null);
  const [attempts, setAttempts] = useState<AttemptSummary[] | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [aiOpen, setAiOpen] = useState(false);

  useEffect(() => {
    if (!classId) return;
    const controller = new AbortController();
    Promise.all([workApi.detail(assignmentId, classId, controller.signal), workApi.history(assignmentId, classId, controller.signal)])
      .then(([value, list]) => {
        if (controller.signal.aborted) return;
        setDetail(value);
        setAttempts(list);
        setSelected(list.find((a) => a.attemptId === wanted)?.attemptId
          ?? list.find((a) => a.status === "SUBMITTED")?.attemptId ?? list[0]?.attemptId ?? null);
      }, (e) => { if (!controller.signal.aborted) setError(e); });
    return () => controller.abort();
  }, [assignmentId, classId, wanted]);

  useEffect(() => {
    if (!selected) return;
    const controller = new AbortController();
    workApi.attempt(selected, controller.signal).then((value) => { if (!controller.signal.aborted) setAttempt(value); },
      (e) => { if (!controller.signal.aborted) setError(e); });
    return () => controller.abort();
  }, [selected]);

  const current = attempts?.find((a) => a.attemptId === selected) ?? null;
  const { result } = useAttemptResult(current?.graded ? current.attemptId : null);
  const shownResult = result?.score != null && attempt?.attemptId === selected ? result : null;

  if (!classId) return <Alert tone="danger" role="alert">Thiếu lớp của bài.</Alert>;
  if (error) return <PageLoadError>{errorMessage(error)}</PageLoadError>;
  if (!detail || !attempts) return <PageLoading label="Đang tải lịch sử nộp bài…" />;

  const query = `classId=${encodeURIComponent(classId)}${general ? "&from=all" : ""}`;
  const detailHref = `/my-assignments/${encodeURIComponent(assignmentId)}?${query}`;
  const focus = `focus=${encodeURIComponent(assignmentId)}`;
  // Quay lại về trang Bài tập (chung hay của lớp), mở đúng tab đang chứa bài.
  const back = general ? `/my-assignments?${focus}` : `/my-assignments?classId=${encodeURIComponent(classId)}&${focus}`;
  const header = (
    <PageHeader backHref={back} title="Lịch sử nộp bài"
      breadcrumb={[{ label: "Bài tập", href: back }, { label: detail.title, href: detailHref }, { label: "Lịch sử nộp bài" }]} />
  );
  if (attempts.length === 0) {
    return <div className="flex flex-col gap-4">{header}<p className="rounded-xl border border-line bg-surface p-8 text-center text-[13.5px] text-muted">Bạn chưa có lượt làm nào.</p></div>;
  }

  // Mới nhất trước: ‹ về lượt trước (cũ hơn), › sang lượt sau (mới hơn).
  const ordered = [...attempts].sort((a, b) => b.attemptNo - a.attemptNo);
  const index = ordered.findIndex((a) => a.attemptId === selected);
  const status = current ? attemptStatus(current) : null;
  const shown = attempt && attempt.attemptId === selected ? attempt : null;
  const comments = shown && shownResult ? scoreCards(shownResult, shown) : [];
  // Bài luyện tập đã nộp: nút Góp ý AI trên thanh công cụ.
  const practiceReady = detail.gradingMode === "PRACTICE" && current?.status === "SUBMITTED";

  return (
    <div className="flex flex-col gap-4">
      {header}
      {!shown || !current ? <PageLoading label="Đang tải bài nộp…" /> : (
        <DocumentEditor document={shown.content} onChange={NOOP} mode="student" readOnly diagramEnabled={false}
          imageUrl={attemptImageUrl(current.attemptId)}
          emptyLeafLabel="Chưa làm phần này."
          comments={comments}
          header={
            <div className="mb-5 border-b border-line pb-4">
              <h2 className="font-display text-[28px] leading-tight tracking-[-0.02em] text-ink">{detail.title}</h2>
              <p className="mt-1.5 flex flex-wrap items-center gap-2 text-[13px] text-muted">
                <span className="font-mono">{attemptTime(current)}</span>
                {status && <Pill tone={status.tone}>{status.label}</Pill>}
              </p>
            </div>
          }
          toolbarCenter={<>
            <ToolbarButton icon="chevron_left" title="Lượt trước" aria-label="Lượt trước" disabled={index < 0 || index >= ordered.length - 1}
              onClick={() => setSelected(ordered[index + 1]!.attemptId)} />
            <select aria-label="Chọn lượt" value={selected ?? ""} onChange={(e) => setSelected(e.target.value)}
              className="h-7 rounded-md border border-line bg-surface px-2 font-mono text-xs text-ink outline-none focus:border-focus">
              {ordered.map((a) => <option key={a.attemptId} value={a.attemptId}>{attemptTime(a)}</option>)}
            </select>
            <ToolbarButton icon="chevron_right" title="Lượt sau" aria-label="Lượt sau" disabled={index <= 0}
              onClick={() => setSelected(ordered[index - 1]!.attemptId)} />
          </>}
          toolbarExtra={<>
            {practiceReady && <ToolbarButton icon="smart_toy" title="Góp ý AI" aria-label="Góp ý AI" onClick={() => setAiOpen(true)} />}
            <a href={exportUrl(current.attemptId)} title="Tải DOCX" aria-label="Tải DOCX"
              className="inline-flex h-7 min-w-7 items-center justify-center rounded-md px-1.5 text-body hover:bg-sunken">
              <Icon name="download" size={18} />
            </a>
          </>}
        />
      )}
      {current?.warnings.length ? <Alert tone="warning">{current.warnings.map((w) => w.message).join("; ")}</Alert> : null}
      {aiOpen && current && practiceReady && (
        <PracticeFeedbackDialog attemptId={current.attemptId} onClose={() => setAiOpen(false)}
          partTitles={shown ? Object.fromEntries(shown.parts.map((part) => [part.partId, part.title])) : undefined} />
      )}
    </div>
  );
}

/**
 * Bên lề chỉ có điểm: đầu tài liệu là thẻ tổng điểm; mỗi phần (nhánh con trực tiếp của gốc) một thẻ điểm gắn vào tiêu đề
 * của phần. Tạm thời không có nhận xét; học sinh không thấy các mục rubric.
 */
function scoreCards(result: AttemptResult, attempt: Attempt): DocumentComment[] {
  const blockIds = new Set(attempt.content.blocks.map((block) => block.id));
  return [
    { key: "total", anchorBlockId: null, content: <TotalCard result={result} /> },
    ...resultByPart(result.items, attempt.parts).map((part): DocumentComment => ({
      key: `part-${part.key}`,
      anchorBlockId: blockIds.has(part.key) ? part.key : null,
      content: <PartScoreCard part={part} />,
    })),
  ];
}

/** Thanh tỉ lệ điểm đạt / điểm tối đa. */
function Meter({ awarded, max }: { awarded: number | null; max: number | null }) {
  const ratio = awarded != null && max ? Math.min(1, Math.max(0, awarded / max)) : 0;
  return (
    <span aria-hidden className="mt-2.5 block h-1 overflow-hidden rounded-full bg-sunken">
      <span className="block h-full rounded-full bg-primary" style={{ width: `${ratio * 100}%` }} />
    </span>
  );
}

function TotalCard({ result }: { result: AttemptResult }) {
  return (
    <article className="rounded-xl border border-line bg-surface px-4 py-3.5 shadow-[var(--shadow-lift)]">
      <p className="text-xs font-medium text-muted">Tổng điểm</p>
      <p className="mt-1 flex items-baseline gap-1.5 tabular-nums">
        <span className="font-display text-[30px] leading-none tracking-[-0.02em] text-ink">{formatPoints(result.score)}</span>
        <span className="text-[13px] text-muted">/ {formatPoints(result.maxScore)}</span>
      </p>
      <Meter awarded={result.score} max={result.maxScore} />
      {result.memberOverride && <p className="mt-2 text-xs text-muted">Điểm cá nhân đã được giảng viên điều chỉnh riêng.</p>}
    </article>
  );
}

/** Thẻ điểm của một phần: tên phần, điểm đạt / tối đa và thanh tỉ lệ. */
function PartScoreCard({ part }: { part: ResultPart }) {
  return (
    <article className="rounded-xl border border-line bg-surface px-4 py-3 shadow-[var(--shadow-lift)]">
      <p className="flex items-baseline justify-between gap-3">
        <span className="min-w-0 truncate text-[13px] font-semibold text-ink" title={part.title}>{part.title}</span>
        <span className="shrink-0 tabular-nums">
          <span className="font-display text-[20px] leading-none text-ink">{formatPoints(part.awarded)}</span>
          <span className="text-[12.5px] text-muted"> / {formatPoints(part.max)}</span>
        </span>
      </p>
      <Meter awarded={part.awarded} max={part.max} />
    </article>
  );
}
