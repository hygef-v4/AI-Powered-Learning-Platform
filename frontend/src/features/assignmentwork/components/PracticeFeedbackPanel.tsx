"use client";

import { useCallback, useEffect, useId, useState, type ReactNode } from "react";
import Link from "next/link";
import { describeAiFailure } from "@/shared/api/errorMessages";
import type { DocumentComment, DocumentPart } from "@/shared/document";
import { Alert, Button, Card, Icon, Pill, cx } from "@/shared/ui";
import { workApi } from "../api";
import { formatPoints } from "../model/attemptResult";
import { describeError, formatDateTime, requestRef } from "../model/format";
import type { Feedback } from "../model/types";
import { msg } from "@/i18n/msg";

const POLL_MS = 3000;
const MAX_POLLS = 60;

/** Trạng thái góp ý AI của một lượt Practice đã nộp (F9, F10, UC 41); {@code attemptId} null thì không tải gì. */
export type PracticeFeedbackState = {
  /** Góp ý đang hiện: bản sẵn sàng mới nhất, không có thì bản mới nhất (đang chờ hoặc lỗi). */
  current: Feedback | null;
  loaded: boolean;
  loadError: unknown;
  requestError: unknown;
  busy: boolean;
  stalled: boolean;
  /** Chỉ xin được khi lượt chưa có góp ý nào dùng được (yêu cầu 2026-11-27: bỏ "Nhận góp ý lại"). */
  canRequest: boolean;
  reload: () => void;
  request: () => void;
};

export function usePracticeFeedback(attemptId: string | null): PracticeFeedbackState {
  const [items, setItems] = useState<{ attemptId: string; list: Feedback[] } | null>(null);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [requestError, setRequestError] = useState<unknown>(null);
  const [refreshCount, setRefreshCount] = useState(0);
  const [isRequesting, setIsRequesting] = useState(false);
  const [ref, setRef] = useState(requestRef);

  useEffect(() => {
    if (!attemptId) return;
    const controller = new AbortController();
    workApi.feedback(attemptId, controller.signal).then((list) => {
      if (controller.signal.aborted) return;
      setItems({ attemptId, list });
      setLoadError(null);
    }, (error) => {
      if (!controller.signal.aborted) setLoadError(error);
    });
    return () => controller.abort();
  }, [attemptId, refreshCount]);

  const list = items && items.attemptId === attemptId ? items.list : null;
  const hasPending = list?.some((item) => item.status === "PENDING") ?? false;
  useEffect(() => {
    if (!hasPending || loadError !== null || refreshCount >= MAX_POLLS) return;
    const timer = setTimeout(() => setRefreshCount((value) => value + 1), POLL_MS);
    return () => clearTimeout(timer);
  }, [hasPending, list, loadError, refreshCount]);

  const request = useCallback(() => {
    if (!attemptId) return;
    setIsRequesting(true);
    setRequestError(null);
    setRefreshCount(0);
    workApi.requestFeedback(attemptId, ref).then((result) => {
      setItems((previous) => ({ attemptId, list: [result, ...(previous?.attemptId === attemptId ? previous.list : [])
        .filter((item) => item.feedbackId !== result.feedbackId)] }));
      setRef(requestRef());
    }, setRequestError).finally(() => setIsRequesting(false));
  }, [attemptId, ref]);

  const newest = [...(list ?? [])].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  const ready = newest.find((item) => item.status === "READY" && item.feedback);
  const current = hasPending ? newest.find((item) => item.status === "PENDING")! : ready ?? newest[0] ?? null;
  return {
    current, loaded: list !== null, loadError, requestError,
    busy: isRequesting || hasPending,
    stalled: hasPending && refreshCount >= MAX_POLLS,
    canRequest: list !== null && !ready && !hasPending && !isRequesting,
    // Đọc lỗi thì đọc lại; đã ngừng hỏi lại tự động thì bắt đầu lại từ đầu.
    reload: () => setRefreshCount((value) => (value >= MAX_POLLS ? 0 : value + 1)),
    request,
  };
}

/** Một thẻ bên lề cho một câu hỏi (nhánh ngoài cùng), giống thẻ điểm rubric của bài đã chấm. */
type PartFeedback = { key: string; title: string; score: number | null; max: number | null; comments: { title: string | null; text: string }[] };

function groupFeedbackByPart(feedback: NonNullable<Feedback["feedback"]>, parts: DocumentPart[]): PartFeedback[] {
  const partOf = new Map(parts.map((part) => [part.partId, part]));
  const order = new Map<string, number>();
  parts.forEach((part, index) => {
    const key = part.ancestorHeadingIds[0] ?? part.partId;
    if (!order.has(key)) order.set(key, index);
  });
  const groups = new Map<string, PartFeedback>();
  for (const entry of feedback.parts) {
    if (!entry.comment.trim() && entry.score == null) continue;
    const part = partOf.get(entry.partId);
    const key = part ? part.ancestorHeadingIds[0] ?? part.partId : entry.partId;
    const [top, ...rest] = (part?.title ?? msg("assignmentwork.components.PracticeFeedbackPanel.wholeAssignment")).split(" › ");
    const group: PartFeedback = groups.get(key) ?? { key, title: top!, score: null, max: null, comments: [] };
    if (entry.score != null && entry.maxScore != null) {
      group.score = (group.score ?? 0) + entry.score;
      group.max = (group.max ?? 0) + entry.maxScore;
    }
    if (entry.comment.trim()) group.comments.push({ title: rest.length ? rest.join(" › ") : null, text: entry.comment });
    groups.set(key, group);
  }
  return [...groups.values()].sort((a, b) => (order.get(a.key) ?? Infinity) - (order.get(b.key) ?? Infinity));
}

/**
 * Thẻ bên lề của góp ý AI (yêu cầu 2026-11-27): thẻ Tổng quan gọn ở đầu cột, rồi mỗi câu hỏi một thẻ gắn vào tiêu đề câu với
 * điểm và nhận xét của câu.
 */
export function practiceFeedbackCards(state: PracticeFeedbackState, parts: DocumentPart[], blockIds: Set<string>): DocumentComment[] {
  const cards: DocumentComment[] = [{ key: "practice-overall", anchorBlockId: null, content: <PracticeOverallCard state={state} /> }];
  const body = state.current?.status === "READY" ? state.current.feedback : null;
  if (!body) return cards;
  return [...cards, ...groupFeedbackByPart(body, parts).map((part) => ({
    key: `practice-part-${part.key}`, anchorBlockId: blockIds.has(part.key) ? part.key : null, content: <PracticePartCard part={part} />,
  }))];
}

/** Thanh tỉ lệ điểm màu mực cam (màu của AI). */
function ScoreBar({ score, max, className }: { score: number; max: number; className?: string }) {
  const ratio = max > 0 ? Math.min(1, Math.max(0, score / max)) : 0;
  return (
    <span aria-hidden className={cx("block h-1.5 overflow-hidden rounded-full bg-sunken", className)}>
      <span className="block h-full rounded-full bg-orange" style={{ width: `${ratio * 100}%` }} />
    </span>
  );
}

type ListKey = "strengths" | "suggestions";
/** Nhận xét chung dài hơn mức này thì thu gọn còn 3 dòng, có nút Xem thêm. */
const SUMMARY_CLAMP_CHARS = 180;

/**
 * Thẻ Tổng quan gọn ở đầu cột bên lề (yêu cầu 2026-11-27: đẹp và không chiếm chỗ): dòng đầu là tên thẻ, nhãn AI và điểm
 * ước tính; thanh tỉ lệ mảnh; nhận xét chung thu gọn 3 dòng; Điểm mạnh và Cần cải thiện là hai nút có số lượng, bấm để mở
 * danh sách (mặc định thu gọn để các thẻ câu đứng sát câu của nó). Chưa có góp ý thì trạng thái và nút Nhận góp ý AI.
 */
export function PracticeOverallCard({ state }: { state: PracticeFeedbackState }) {
  const headingId = useId();
  const listId = useId();
  const [open, setOpen] = useState<ListKey | null>(null);
  const [expanded, setExpanded] = useState(false);
  const item = state.current;
  const body = item?.status === "READY" ? item.feedback : null;
  const scored = body?.score != null && body.maxScore != null;

  const header = (
    <header className="flex items-start justify-between gap-3">
      <div className="flex min-w-0 flex-col gap-1">
        <h3 id={headingId} className="text-[13px] font-semibold text-ink">{msg("assignmentwork.components.PracticeFeedbackPanel.overall")}</h3>
        <span title={msg("assignmentwork.components.PracticeFeedbackPanel.aiEstimateNotOfficial")} className="w-fit">
          <Pill className="h-5 bg-orange-soft px-2 text-[11px] text-orange"><Icon name="smart_toy" size={12} />{msg("assignmentwork.components.PracticeFeedbackPanel.aiEstimate")}</Pill>
        </span>
      </div>
      {scored && (
        <p className="flex shrink-0 items-baseline gap-1 tabular-nums">
          <span className="font-display text-[30px] leading-none font-extrabold tracking-[-0.03em] text-ink">{formatPoints(body!.score)}</span>
          <span className="text-[13px] text-muted">/ {formatPoints(body!.maxScore)}</span>
        </p>
      )}
    </header>
  );
  const errors = <>
    {state.loadError !== null && (
      <div className="flex flex-col gap-2">
        <Alert tone="danger" role="alert">{describeError(state.loadError)}</Alert>
        <Button size="sm" onClick={state.reload}>{msg("assignmentwork.components.PracticeFeedbackPanel.reloadFeedback")}</Button>
      </div>
    )}
    {state.requestError !== null && <Alert tone="danger" role="alert">{describeError(state.requestError)}</Alert>}
  </>;

  if (!body) {
    let status: ReactNode = null;
    if (!state.loaded && state.loadError === null) {
      status = <p role="status" className="text-[13px] text-muted">{msg("assignmentwork.components.PracticeFeedbackPanel.loadingFeedback")}</p>;
    } else if (state.busy) {
      status = <p role="status" className="text-[13px] text-muted">{msg("assignmentwork.components.PracticeFeedbackPanel.theAiIsWritingFeedback")}</p>;
    } else if (item?.status === "FAILED") {
      status = <Alert tone="danger">{describeAiFailure(item.safeError, msg("assignmentwork.components.PracticeFeedbackPanel.noFeedbackReceived"))}</Alert>;
    } else if (state.loaded) {
      status = <p className="text-[13px] text-muted">{msg("assignmentwork.components.PracticeFeedbackPanel.noFeedbackYet")}</p>;
    }
    return (
      <Card as="section" lifted padding="none" className="flex min-w-0 flex-col gap-3 px-4 py-3.5" aria-labelledby={headingId}>
        {header}
        {errors}
        {status}
        {state.stalled && (
          <p role="status" className="flex flex-wrap items-center gap-2 text-xs text-muted">
            {msg("assignmentwork.components.PracticeFeedbackPanel.theAiIsStillProcessing")}
            <Button size="sm" onClick={state.reload}>{msg("assignmentwork.components.PracticeFeedbackPanel.checkFeedbackAgain")}</Button>
          </p>
        )}
        <Button variant="primary" size="sm" icon="smart_toy" disabled={!state.canRequest} data-testid="ask-feedback" onClick={state.request}>
          {msg("assignmentwork.components.PracticeFeedbackPanel.getAiFeedback")}
        </Button>
        <p className="text-xs leading-relaxed text-muted">
          {msg("assignmentwork.components.PracticeFeedbackPanel.youCanManageYourPersonal")}{" "}
          <Link href="/ai/connection" className="font-semibold text-primary-ink">{msg("assignmentwork.components.PracticeFeedbackPanel.aiConnection")}</Link>.
        </p>
      </Card>
    );
  }

  const lists: { key: ListKey; icon: "check_circle" | "edit"; tone: string; label: string; items: string[] }[] = [
    { key: "strengths", icon: "check_circle", tone: "text-success", label: msg("assignmentwork.components.PracticeFeedbackPanel.strengths"), items: body.strengths },
    { key: "suggestions", icon: "edit", tone: "text-warning", label: msg("assignmentwork.components.PracticeFeedbackPanel.toImprove"), items: body.suggestions },
  ];
  const shown = lists.find((list) => list.key === open);
  const long = body.summary.length > SUMMARY_CLAMP_CHARS;
  return (
    <Card as="section" lifted padding="none" className="flex min-w-0 flex-col gap-3 px-4 py-3.5" aria-labelledby={headingId}>
      {header}
      {scored && <ScoreBar score={body.score!} max={body.maxScore!} className="-mt-1" />}
      {errors}
      <div className="flex flex-col items-start gap-1">
        <p className={cx("text-[13px] leading-relaxed text-body wrap-anywhere", long && !expanded && "line-clamp-3")}>{body.summary}</p>
        {long && (
          <button type="button" aria-expanded={expanded} onClick={() => setExpanded((value) => !value)}
            className="text-xs font-semibold text-primary-ink underline-offset-2 hover:underline">
            {expanded ? msg("assignmentwork.components.PracticeFeedbackPanel.showLess") : msg("assignmentwork.components.PracticeFeedbackPanel.showMore")}
          </button>
        )}
      </div>
      {lists.some((list) => list.items.length > 0) && (
        <div className="flex flex-col gap-2 border-t border-line pt-3">
          <div className="flex flex-wrap gap-1.5">
            {lists.map((list) => (
              <button key={list.key} type="button" disabled={list.items.length === 0} aria-expanded={open === list.key} aria-controls={listId}
                onClick={() => setOpen((value) => (value === list.key ? null : list.key))}
                className={cx("flex flex-1 items-center gap-1.5 rounded-lg border px-2 py-1.5 text-xs font-semibold whitespace-nowrap transition-colors disabled:opacity-40",
                  open === list.key ? "border-line-strong bg-sunken text-ink" : "border-line text-body hover:bg-sunken")}>
                <Icon name={list.icon} size={14} className={cx("shrink-0", list.tone)} />
                <span>{list.label}</span>
                <span className="ml-auto shrink-0 rounded-full bg-sunken px-1.5 text-[11px] tabular-nums text-muted">{list.items.length}</span>
              </button>
            ))}
          </div>
          {shown && (
            <ul id={listId} aria-label={shown.label} className="flex animate-fade flex-col gap-1.5 text-[13px] leading-relaxed text-body">
              {shown.items.map((text, i) => (
                <li key={i} className="relative pl-3.5 wrap-anywhere before:absolute before:top-[0.65em] before:left-0.5 before:size-1 before:rounded-full before:bg-line-strong">{text}</li>
              ))}
            </ul>
          )}
        </div>
      )}
      <p className="text-xs text-muted">
        {item!.origin === "MCP" ? "Agent" : String(item!.provenance.provider ?? msg("assignmentwork.components.PracticeFeedbackPanel.personalApi"))}
        {" · "}{formatDateTime(item!.completedAt ?? item!.createdAt)}
      </p>
    </Card>
  );
}

/** Thẻ của một câu hỏi: tên câu và điểm cùng một dòng, thanh tỉ lệ mảnh, nhận xét của AI (không liệt kê tiêu chí rubric). */
function PracticePartCard({ part }: { part: PartFeedback }) {
  const scored = part.score != null && part.max != null;
  return (
    <Card as="article" padding="none" lifted className="flex flex-col gap-2 px-3.5 py-3">
      <div className="flex items-baseline justify-between gap-3">
        <h4 className="min-w-0 truncate text-[13px] font-semibold text-ink" title={part.title}>{part.title}</h4>
        {scored
          ? <span className="shrink-0 text-[13px] tabular-nums"><span className="font-bold text-ink">{formatPoints(part.score)}</span><span className="text-muted"> / {formatPoints(part.max)}</span></span>
          : <Icon name="smart_toy" size={15} className="shrink-0 text-muted" />}
      </div>
      {scored && <ScoreBar score={part.score!} max={part.max!} className="h-1" />}
      {part.comments.map((comment, i) => (
        <p key={i} className="text-[13px] leading-relaxed text-body wrap-anywhere">
          {comment.title && <span className="font-semibold text-ink">{comment.title}: </span>}{comment.text}
        </p>
      ))}
    </Card>
  );
}
