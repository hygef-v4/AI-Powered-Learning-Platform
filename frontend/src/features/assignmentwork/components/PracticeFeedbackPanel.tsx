"use client";

import { useEffect, useId, useState } from "react";
import Link from "next/link";
import { Alert, Button, Card, Icon, Pill } from "@/shared/ui";
import { workApi } from "../api";
import { describeError, formatDateTime, requestRef } from "../model/format";
import type { Feedback } from "../model/types";
import { msg } from "@/i18n/msg";

const POLL_MS = 3000;
const MAX_POLLS = 60;

/** Góp ý AI theo lượt Practice đã nộp (F9, F10, UC 41). */
export function PracticeFeedbackPanel({ attemptId, partTitles }: {
  attemptId: string;
  partTitles: Record<string, string>;
}) {
  const headingId = useId();
  const [items, setItems] = useState<Feedback[] | null>(null);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [requestError, setRequestError] = useState<unknown>(null);
  const [refreshCount, setRefreshCount] = useState(0);
  const [isRequesting, setIsRequesting] = useState(false);
  const [feedbackRequestRef, setFeedbackRequestRef] = useState(requestRef);

  useEffect(() => {
    const controller = new AbortController();
    workApi.feedback(attemptId, controller.signal).then((list) => {
      if (controller.signal.aborted) return;
      setItems(list);
      setLoadError(null);
    }, (error) => {
      if (!controller.signal.aborted) setLoadError(error);
    });
    return () => controller.abort();
  }, [attemptId, refreshCount]);

  const hasPending = items?.some((item) => item.status === "PENDING") ?? false;
  useEffect(() => {
    if (!hasPending || loadError !== null || refreshCount >= MAX_POLLS) return;
    const timer = setTimeout(() => setRefreshCount((value) => value + 1), POLL_MS);
    return () => clearTimeout(timer);
  }, [hasPending, items, loadError, refreshCount]);

  async function handleRequestFeedback() {
    setIsRequesting(true);
    setRequestError(null);
    try {
      setRefreshCount(0);
      const result = await workApi.requestFeedback(attemptId, feedbackRequestRef);
      setItems((previous) => [result, ...(previous ?? []).filter((item) => item.feedbackId !== result.feedbackId)]);
      setFeedbackRequestRef(requestRef());
    } catch (error) {
      setRequestError(error);
    } finally {
      setIsRequesting(false);
    }
  }

  return (
    <Card as="section" lifted className="flex min-w-0 flex-col gap-3" aria-labelledby={headingId}>
      <h3 id={headingId} className="flex items-center gap-2 text-sm font-semibold text-ink">
        <Icon name="smart_toy" className="text-primary-ink" /> {msg("assignmentwork.components.PracticeFeedbackPanel.gopYAi")}</h3>
      {loadError !== null && (
        <div className="space-y-2">
          <Alert tone="danger" role="alert">{describeError(loadError)}</Alert>
          <Button size="sm" onClick={() => setRefreshCount((value) => value + 1)}>{msg("assignmentwork.components.PracticeFeedbackPanel.taiLaiGopY")}</Button>
        </div>
      )}
      {requestError !== null && <Alert tone="danger" role="alert">{describeError(requestError)}</Alert>}
      {(isRequesting || hasPending) && <p role="status" className="text-xs text-muted">{msg("assignmentwork.components.PracticeFeedbackPanel.aiDangGopY")}</p>}
      {!items && loadError === null && <p role="status" className="text-sm text-muted">{msg("assignmentwork.components.PracticeFeedbackPanel.dangTaiGopY")}</p>}
      {items && items.length === 0 && <p className="text-sm text-muted">{msg("assignmentwork.components.PracticeFeedbackPanel.chuaCoGopY")}</p>}
      {items?.map((item) => (
        <article key={item.feedbackId} className="flex flex-col gap-2 rounded-lg border border-line p-3 wrap-anywhere">
          <p className="flex flex-wrap items-center gap-1.5 text-xs text-muted">
            {item.status === "READY" && <Pill tone="warning">{msg("assignmentwork.components.PracticeFeedbackPanel.gopYThamKhao")}</Pill>}
            {item.status === "PENDING" && <Pill tone="info">{msg("assignmentwork.components.PracticeFeedbackPanel.dangCho")}</Pill>}
            {item.status === "FAILED" && <Pill tone="danger">{msg("assignmentwork.components.PracticeFeedbackPanel.khongNhanDuocGopY")}</Pill>}
            {item.origin === "MCP" ? <Pill tone="accent">Agent</Pill> : <Pill>{String(item.provenance.provider ?? msg("assignmentwork.components.PracticeFeedbackPanel.apiCaNhan"))}</Pill>}
            {formatDateTime(item.completedAt ?? item.createdAt)}
          </p>
          {item.status === "FAILED" && <p className="text-xs text-danger">{item.safeError ?? msg("assignmentwork.components.PracticeFeedbackPanel.khongNhanDuocGopY")}</p>}
          {item.status === "READY" && item.feedback && (
            <>
              <div>
                <p className="text-xs font-semibold text-ink">{msg("assignmentwork.components.PracticeFeedbackPanel.nhanXetChung")}</p>
                <p className="text-[13px] text-body">{item.feedback.summary}</p>
              </div>
              {item.feedback.strengths.length > 0 && (
                <div>
                  <p className="text-xs font-semibold text-success">{msg("assignmentwork.components.PracticeFeedbackPanel.diemManh")}</p>
                  <ul className="list-disc pl-5 text-[13px]">
                    {item.feedback.strengths.map((strength, i) => <li key={i}>{strength}</li>)}
                  </ul>
                </div>
              )}
              {item.feedback.suggestions.length > 0 && (
                <div>
                  <p className="text-xs font-semibold text-warning">{msg("assignmentwork.components.PracticeFeedbackPanel.canCaiThien")}</p>
                  <ul className="list-disc pl-5 text-[13px]">
                    {item.feedback.suggestions.map((suggestion, i) => <li key={i}>{suggestion}</li>)}
                  </ul>
                </div>
              )}
              {item.feedback.parts.filter((part) => part.comment).length > 0 && (
                <div>
                  <p className="text-xs font-semibold text-body">{msg("assignmentwork.components.PracticeFeedbackPanel.theoTungPhan")}</p>
                  <ul className="list-disc pl-5 text-[13px]">
                    {item.feedback.parts.filter((part) => part.comment).map((part, i) => (
                      <li key={i}>{partTitles[part.partId] ? `${partTitles[part.partId]}: ` : ""}{part.comment}</li>
                    ))}
                  </ul>
                </div>
              )}
            </>
          )}
        </article>
      ))}
      {hasPending && refreshCount >= MAX_POLLS && <p role="status" className="text-xs text-muted">
        {msg("assignmentwork.components.PracticeFeedbackPanel.aiVanDangXuLy")}<Button size="sm" onClick={() => setRefreshCount(0)}>{msg("assignmentwork.components.PracticeFeedbackPanel.kiemTraLaiGopY")}</Button>
      </p>}
      <p className="text-xs text-muted">
        {msg("assignmentwork.components.PracticeFeedbackPanel.banCoTheQuanLyKhoa")}{" "}
        <Link href="/ai/connection" className="font-semibold text-primary-ink">{msg("assignmentwork.components.PracticeFeedbackPanel.ketNoiAi")}</Link>.
      </p>
      <Button
        variant="primary" icon="smart_toy" disabled={!items || isRequesting || hasPending}
        data-testid="ask-feedback" onClick={() => void handleRequestFeedback()}
      >
        {items?.length ? msg("assignmentwork.components.PracticeFeedbackPanel.nhanGopYLai") : msg("assignmentwork.components.PracticeFeedbackPanel.nhanGopYAi")}
      </Button>
    </Card>
  );
}
