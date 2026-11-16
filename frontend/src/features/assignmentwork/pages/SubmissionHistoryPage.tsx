"use client";

import { useCallback, useEffect, useRef, useState, type CSSProperties, type ReactNode } from "react";
import { useSearchParams } from "next/navigation";
import { DocumentEditor, ToolbarButton, outlineOf, publishOutline, type DocumentComment } from "@/shared/document";
import { Alert, Card, Menu, MenuItem, ScoreMeter, TotalScoreCard } from "@/shared/ui";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { attemptImageUrl, exportUrl, workApi } from "../api";
import { formatPoints, resultByPart, useAttemptResult, type ResultPart } from "../model/attemptResult";
import { errorMessage, formatDateTime, historyOpen, historyVersions } from "../model/format";
import { PracticeFeedbackPanel } from "../components/PracticeFeedbackPanel";
import { studentGradeDistributions, type AttemptResult } from "../slots";
import { PublishedFeedback, PublishedRubricItems } from "../components/PublishedFeedback";
import type { Attempt, AttemptSummary, StudentDetail } from "../model/types";
import { NOOP } from "@/shared/noop";
import { WorkspaceTopBar } from "@/shared/layout/WorkspaceTopBar";
import { msg } from "@/i18n/msg";

function attemptTime(a: AttemptSummary) {
  return formatDateTime(a.submittedAt ?? a.lastSavedAt ?? a.startedAt);
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
        // Lịch sử chỉ gồm các lượt đã nộp; lượt đang làm không hiện (lỗi 2026-11-05). Bài cá nhân (yêu cầu 2026-11-10):
        // lịch sử mở sau hạn nộp; bản đang được chấm vẫn mở riêng để xem điểm đã công bố trước hạn.
        // Điểm và nhận xét chưa công bố do API kết quả giữ kín.
        const all = list.filter((a) => a.status === "SUBMITTED").sort((a, b) => b.attemptNo - a.attemptNo);
        const submitted = value.gradingMode === "PRACTICE" ? all
          : historyOpen(value, all.length > 0) ? historyVersions(all) : all.filter((a) => a.graded);
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
    publishOutline({ assignmentId, backHref, backLabel: msg("assignmentwork.pages.SubmissionHistoryPage.baiTap"), entries: outlineOf(blocks) });
  }, [assignmentId, attempt, classId, detail, general, selected]);
  useEffect(() => () => publishOutline(null), []);

  const current = attempts?.find((a) => a.attemptId === selected) ?? null;
  const { result } = useAttemptResult(current?.graded ? current.attemptId : null);
  const shownResult = result?.score != null && attempt?.attemptId === selected ? result : null;

  if (!classId) return <Alert tone="danger" role="alert">{msg("assignmentwork.pages.SubmissionHistoryPage.thieuLopCuaBai")}</Alert>;
  if (error) return <PageLoadError>{errorMessage(error)}</PageLoadError>;
  if (!detail || !attempts) return <PageLoading label={msg("assignmentwork.pages.SubmissionHistoryPage.dangTaiLichSuNopBai")} />;

  // Quay lại nằm ở sidebar mục lục của bài (về trang Bài tập, đúng tab đang chứa bài).
  const shell = (bar: ReactNode, body: ReactNode) => (
    <div data-wide-workspace className="flex flex-col gap-4" style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}>
      {bar}
      {body}
    </div>
  );
  if (attempts.length === 0) {
    return shell(<WorkspaceTopBar barRef={headerRef} title={detail.title} setViewSlot={setViewSlot} />,
      <Card as="p" padding="none" className="p-8 text-center text-[13.5px] text-muted">
        {historyOpen(detail, true) ? msg("assignmentwork.pages.SubmissionHistoryPage.banChuaNopBaiNao") : msg("assignmentwork.pages.SubmissionHistoryPage.lichSuNopBaiHienSau")}
      </Card>);
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
            ASSIGNMENT: msg("assignmentwork.pages.SubmissionHistoryPage.toanBai"),
            ...Object.fromEntries(shown.parts.map((part) => [part.partId, part.title])),
          }}
        />
      ),
    });
  }

  const bar = (
    <WorkspaceTopBar barRef={headerRef} title={detail.title} setViewSlot={setViewSlot}
      actions={current && (
        <Menu label="" icon="more_horiz" ariaLabel={msg("assignmentwork.pages.SubmissionHistoryPage.thaoTacKhac")} align="right">
          <MenuItem icon="download" onClick={() => { window.location.href = exportUrl(current.attemptId); }}>{msg("assignmentwork.pages.SubmissionHistoryPage.taiDocx")}</MenuItem>
        </Menu>
      )} />
  );
  const kind = (
    <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
      <p className="text-[13px] text-muted" data-testid="assignment-kind">
        {detail.gradingMode === "PRACTICE" ? msg("assignmentwork.pages.SubmissionHistoryPage.luyenTap") : detail.submissionMode === "GROUP" ? msg("assignmentwork.pages.SubmissionHistoryPage.nhom") : msg("assignmentwork.pages.SubmissionHistoryPage.caNhan")}
      </p>
    </div>
  );

  return shell(bar, <>
    {!shown || !current ? <PageLoading label={msg("assignmentwork.pages.SubmissionHistoryPage.dangTaiBaiNop")} /> : (
      <DocumentEditor fullWidth document={shown.content} onChange={NOOP} mode="student" readOnly diagramEnabled={false}
        imageUrl={attemptImageUrl(current.attemptId)}
        emptyLeafLabel={msg("assignmentwork.pages.SubmissionHistoryPage.chuaLamPhanNay")}
        comments={comments}
        titleField={{ text: detail.title, readOnly: true, onText: NOOP }}
        header={kind}
        viewSlot={viewSlot}
        // Chuyển qua lại giữa các bài nộp; chỉ có khi có hơn một bản (bài cá nhân: bản đúng hạn và bản nộp muộn).
        toolbarCenter={ordered.length < 2 ? undefined : <>
          <ToolbarButton icon="chevron_left" title={msg("assignmentwork.pages.SubmissionHistoryPage.baiNopTruoc")} aria-label={msg("assignmentwork.pages.SubmissionHistoryPage.baiNopTruoc")} disabled={index < 0 || index >= ordered.length - 1}
            onClick={() => setSelected(ordered[index + 1]!.attemptId)} />
          <select aria-label={msg("assignmentwork.pages.SubmissionHistoryPage.chonBaiNop")} value={selected ?? ""} onChange={(e) => setSelected(e.target.value)}
            className="h-7 rounded-md border border-line bg-surface px-2 font-mono text-xs text-ink outline-none focus:border-focus">
            {ordered.map((a) => (
              <option key={a.attemptId} value={a.attemptId}>
                {detail.gradingMode === "PRACTICE" ? attemptTime(a) : `${a.late ? msg("assignmentwork.pages.SubmissionHistoryPage.banNopMuon") : msg("assignmentwork.pages.SubmissionHistoryPage.banDungHan")} – ${attemptTime(a)}`}
              </option>
            ))}
          </select>
          <ToolbarButton icon="chevron_right" title={msg("assignmentwork.pages.SubmissionHistoryPage.baiNopSau")} aria-label={msg("assignmentwork.pages.SubmissionHistoryPage.baiNopSau")} disabled={index <= 0}
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

function TotalCard({ result }: { result: AttemptResult }) {
  return (
    <TotalScoreCard score={result.score} maxScore={result.maxScore} format={formatPoints}>
      <div className="mt-3"><PublishedFeedback feedback={result.feedback} summary={result.summary} /></div>
      {result.memberOverride && <p className="mt-2 text-xs text-muted">{msg("assignmentwork.pages.SubmissionHistoryPage.diemCaNhanDaDuocGiang")}</p>}
    </TotalScoreCard>
  );
}

/** Thẻ điểm của một phần: tên phần, điểm đạt / tối đa và thanh tỉ lệ. */
function PartScoreCard({ part }: { part: ResultPart }) {
  return (
    <Card as="article" padding="none" lifted className="px-4 py-3">
      <p className="flex items-baseline justify-between gap-3">
        <span className="min-w-0 truncate text-[13px] font-semibold text-ink" title={part.title}>{part.title}</span>
        <span className="shrink-0 tabular-nums">
          <span className="font-display text-[20px] leading-none text-ink">{formatPoints(part.awarded)}</span>
          <span className="text-[12.5px] text-muted"> / {formatPoints(part.max)}</span>
        </span>
      </p>
      <ScoreMeter awarded={part.awarded} max={part.max} />
      <PublishedRubricItems items={part.items} />
    </Card>
  );
}
