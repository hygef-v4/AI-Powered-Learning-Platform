"use client";

import { useCallback, useEffect, useRef, useState, type CSSProperties, type ReactNode } from "react";
import { useSearchParams } from "next/navigation";
import { DocumentEditor, ToolbarButton, outlineOf, publishOutline, type DocumentComment } from "@/shared/document";
import { Alert, Menu, MenuItem } from "@/shared/ui";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { attemptImageUrl, exportUrl, workApi } from "../api";
import { formatPoints, resultByPart, useAttemptResult, type ResultPart } from "../model/attemptResult";
import { errorMessage, formatDateTime } from "../model/format";
import { PracticeFeedbackPanel } from "../components/PracticeFeedbackPanel";
import { studentGradeDistributions, type AttemptResult } from "../slots";
import { PublishedFeedback, PublishedRubricItems } from "../components/PublishedFeedback";
import type { Attempt, AttemptSummary, StudentDetail } from "../model/types";

function attemptTime(a: AttemptSummary) {
  return formatDateTime(a.submittedAt ?? a.lastSavedAt ?? a.startedAt);
}

const NOOP = () => {};

/**
 * Thanh trên cùng giống trang làm bài (yêu cầu 2026-11-05): tên bài và trạng thái bên trái, chỗ đặt nút Cả trang / Từng
 * phần ở giữa, thao tác bên phải.
 */
function TopBar({ barRef, title, status, setViewSlot, actions }: {
  barRef: (element: HTMLElement | null) => void;
  title: string;
  status?: ReactNode;
  setViewSlot: (element: HTMLElement | null) => void;
  actions?: ReactNode;
}) {
  return (
    <header ref={barRef} className="sticky top-14 z-30 -mx-4 -mt-8 grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-x-4 gap-y-2 border-b border-line bg-[var(--c-topbar)] px-4 py-3 backdrop-blur-md sm:-mx-8 sm:px-8 md:-mt-10">
      <div className="flex min-w-0 flex-1 items-center gap-x-3">
        <h1 className="min-w-0 truncate text-[15px] font-semibold text-ink" title={title}>{title}</h1>
        {status}
      </div>
      <div ref={setViewSlot} data-testid="view-slot" className="flex justify-center" />
      <div className="flex flex-wrap items-center justify-end gap-2">{actions}</div>
    </header>
  );
}

/**
 * #41 Submission History (F8, UC 40): bài nộp hiện như trang tài liệu chỉ đọc (kiểu Docs ở chế độ chỉ xem):
 * giữa thanh công cụ, thẳng giữa trang giấy, là nút lượt trước / lượt sau và ô chọn lượt theo thời gian (mới nhất trước);
 * nút tải DOCX ở bên phải. Bên lề hiện góp ý AI của lượt luyện tập hoặc tổng điểm và điểm từng phần của bài đã chấm.
 * `?attempt=` mở sẵn một lượt; quay lại về đúng tab của bài trên trang Bài tập (`focus`).
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
  const [viewSlot, setViewSlot] = useState<HTMLElement | null>(null);
  const [headerHeight, setHeaderHeight] = useState(64);
  const headerObserver = useRef<ResizeObserver | null>(null);
  const headerRef = useCallback((element: HTMLElement | null) => {
    headerObserver.current?.disconnect();
    headerObserver.current = null;
    if (!element || typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(() => setHeaderHeight(element.offsetHeight));
    observer.observe(element);
    headerObserver.current = observer;
  }, []);

  useEffect(() => {
    if (!classId) return;
    const controller = new AbortController();
    Promise.all([workApi.detail(assignmentId, classId, controller.signal), workApi.history(assignmentId, classId, controller.signal)])
      .then(([value, list]) => {
        if (controller.signal.aborted) return;
        setDetail(value);
        // Lịch sử chỉ gồm các lượt đã nộp; lượt đang làm không hiện (lỗi 2026-11-05).
        const submitted = list.filter((a) => a.status === "SUBMITTED");
        setAttempts(submitted);
        setSelected(submitted.find((a) => a.attemptId === wanted)?.attemptId ?? submitted[0]?.attemptId ?? null);
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

  // Sidebar mục lục của bài như trang làm bài; lối về trang Bài tập (yêu cầu 2026-11-05).
  useEffect(() => {
    if (!detail || !classId) return;
    const focus = `focus=${encodeURIComponent(assignmentId)}`;
    const backHref = general ? `/my-assignments?${focus}` : `/my-assignments?classId=${encodeURIComponent(classId)}&${focus}`;
    const blocks = attempt && attempt.attemptId === selected ? attempt.content.blocks : [];
    publishOutline({ assignmentId, backHref, backLabel: "Bài tập", entries: outlineOf(blocks) });
  }, [assignmentId, attempt, classId, detail, general, selected]);
  useEffect(() => () => publishOutline(null), []);

  const current = attempts?.find((a) => a.attemptId === selected) ?? null;
  const { result } = useAttemptResult(current?.graded ? current.attemptId : null);
  const shownResult = result?.score != null && attempt?.attemptId === selected ? result : null;

  if (!classId) return <Alert tone="danger" role="alert">Thiếu lớp của bài.</Alert>;
  if (error) return <PageLoadError>{errorMessage(error)}</PageLoadError>;
  if (!detail || !attempts) return <PageLoading label="Đang tải lịch sử nộp bài…" />;

  // Quay lại nằm ở sidebar mục lục của bài (về trang Bài tập, đúng tab đang chứa bài).
  const shell = (bar: ReactNode, body: ReactNode) => (
    <div data-wide-workspace className="flex flex-col gap-4" style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}>
      {bar}
      {body}
    </div>
  );
  if (attempts.length === 0) {
    return shell(<TopBar barRef={headerRef} title={detail.title} setViewSlot={setViewSlot} />,
      <p className="rounded-xl border border-line bg-surface p-8 text-center text-[13.5px] text-muted">Bạn chưa nộp lượt nào.</p>);
  }

  // Mới nhất trước: ‹ về lượt trước (cũ hơn), › sang lượt sau (mới hơn).
  const ordered = [...attempts].sort((a, b) => b.attemptNo - a.attemptNo);
  const index = ordered.findIndex((a) => a.attemptId === selected);
  const shown = attempt && attempt.attemptId === selected ? attempt : null;
  const comments = shown && shownResult ? scoreCards(shownResult, shown) : [];
  if (shown && detail.gradingMode === "PRACTICE" && current?.status === "SUBMITTED") {
    comments.push({
      key: `practice-feedback-${shown.attemptId}`,
      anchorBlockId: null,
      content: (
        <PracticeFeedbackPanel
          attemptId={shown.attemptId}
          partTitles={{
            ASSIGNMENT: "Toàn bài",
            ...Object.fromEntries(shown.parts.map((part) => [part.partId, part.title])),
          }}
        />
      ),
    });
  }

  const bar = (
    <TopBar barRef={headerRef} title={detail.title} setViewSlot={setViewSlot}
      actions={current && (
        <Menu label="" icon="more_horiz" ariaLabel="Thao tác khác" align="right">
          <MenuItem icon="download" onClick={() => { window.location.href = exportUrl(current.attemptId); }}>Tải DOCX</MenuItem>
        </Menu>
      )} />
  );
  const kind = (
    <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
      <p className="text-[13px] text-muted" data-testid="assignment-kind">
        {detail.gradingMode === "PRACTICE" ? "Luyện tập" : detail.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"}
      </p>
    </div>
  );

  return shell(bar, <>
    {!shown || !current ? <PageLoading label="Đang tải bài nộp…" /> : (
      <DocumentEditor fullWidth document={shown.content} onChange={NOOP} mode="student" readOnly diagramEnabled={false}
        imageUrl={attemptImageUrl(current.attemptId)}
        emptyLeafLabel="Chưa làm phần này."
        comments={comments}
        titleField={{ text: detail.title, readOnly: true, onText: NOOP }}
        header={kind}
        viewSlot={viewSlot}
        // Thanh công cụ chỉ còn chuyển qua lại giữa các lượt.
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
      />
    )}
    {shownResult && studentGradeDistributions.entries().map(({ key, component: Distribution }) =>
      <Distribution key={key} classId={classId} assignmentId={assignmentId} />)}
    {current?.warnings.length ? <Alert tone="warning">{current.warnings.map((w) => w.message).join("; ")}</Alert> : null}
  </>);
}

/**
 * Bên lề hiển thị điểm và nhận xét đã công bố: đầu tài liệu là thẻ tổng điểm; mỗi phần (nhánh con trực tiếp của gốc) một thẻ điểm gắn vào tiêu đề
 * của phần, kèm minh chứng và gợi ý theo rubric.
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
      <div className="mt-3"><PublishedFeedback feedback={result.feedback} summary={result.summary} /></div>
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
      <PublishedRubricItems items={part.items} />
    </article>
  );
}
