"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Alert, Button, Dialog, Pill, Select } from "@/shared/ui";
import { workApi } from "./api";
import { errorMessage, formatDateTime, requestRef } from "./format";
import type { Feedback } from "./types";

const POLL_MS = 3000;

/**
 * Góp ý AI của bài Practice (F9, F10, UC 41), mở từ nút trên thanh công cụ của Lịch sử nộp bài: danh sách góp ý và chỗ gửi
 * yêu cầu mới bằng API cá nhân; nhãn "AI ước tính, không phải điểm chính thức". Không có ước tính theo rubric vì người học
 * không xem rubric (yêu cầu 2026-10-21).
 */
export function PracticeFeedbackDialog({ attemptId, partTitles, onClose }: {
  attemptId: string;
  partTitles?: Record<string, string>;
  onClose: () => void;
}) {
  const [items, setItems] = useState<Feedback[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [tick, setTick] = useState(0);
  const [provider, setProvider] = useState("GEMINI");
  const [busy, setBusy] = useState(false);
  const [ref, setRef] = useState(requestRef);

  useEffect(() => {
    const controller = new AbortController();
    workApi.feedback(attemptId, controller.signal).then((list) => { if (!controller.signal.aborted) setItems(list); },
      (e) => { if (!controller.signal.aborted) setError(e); });
    return () => controller.abort();
  }, [attemptId, tick]);

  const pending = items?.some((item) => item.status === "PENDING") ?? false;
  useEffect(() => {
    if (!pending) return;
    const timer = setTimeout(() => setTick((value) => value + 1), POLL_MS);
    return () => clearTimeout(timer);
  }, [pending, items]);

  const ask = useCallback(async () => {
    setBusy(true);
    setError(null);
    try {
      await workApi.requestFeedback(attemptId, provider, ref);
      setRef(requestRef());
      setTick((value) => value + 1);
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }, [attemptId, provider, ref]);

  return (
    <Dialog open size="lg" title="Góp ý AI" icon="smart_toy" onClose={onClose} dismissible={!busy}
      footer={<>
        <Select aria-label="Nhà cung cấp" value={provider} onChange={(e) => setProvider(e.target.value)} className="w-36">
          <option value="GEMINI">Gemini</option>
          <option value="OPENAI">OpenAI</option>
          <option value="CLAUDE">Claude</option>
        </Select>
        <Button variant="primary" icon="smart_toy" disabled={busy || pending} data-testid="ask-feedback" onClick={() => void ask()}>
          {items?.length ? "Nhận góp ý lại" : "Nhận góp ý AI"}
        </Button>
      </>}>
      <div className="flex flex-col gap-3">
        {error !== null && <Alert tone="danger" role="alert">{errorMessage(error)}</Alert>}
        {pending && <p role="status" className="text-xs text-muted">AI đang góp ý…</p>}
        {!items && error === null && <p role="status" className="text-sm text-muted">Đang tải góp ý…</p>}
        {items && items.length === 0 && <p className="text-sm text-muted">Chưa có góp ý.</p>}
        {items?.map((item) => (
          <article key={item.feedbackId} className="flex flex-col gap-1.5 rounded-xl border border-line p-3">
            <p className="flex flex-wrap items-center gap-1.5 text-[11px] text-muted">
              {item.status === "READY" && <Pill tone="warning">AI ước tính, không phải điểm chính thức</Pill>}
              {item.status === "PENDING" && <Pill tone="info">Đang chờ</Pill>}
              {item.status === "FAILED" && <Pill tone="danger">Không nhận được góp ý</Pill>}
              {item.origin === "MCP" ? <Pill tone="accent">Agent</Pill> : <Pill>{String(item.provenance.provider ?? "API cá nhân")}</Pill>}
              {formatDateTime(item.completedAt ?? item.createdAt)}
            </p>
            {item.status === "FAILED" && <p className="text-xs text-danger">{item.safeError ?? "Không nhận được góp ý"}</p>}
            {item.status === "READY" && item.feedback && (
              <>
                <p className="text-[13px] text-ink">{item.feedback.summary}</p>
                {item.feedback.strengths.length > 0 && <div><p className="text-xs font-semibold text-success">Điểm mạnh</p><ul className="list-disc pl-5 text-[13px]">{item.feedback.strengths.map((s, i) => <li key={i}>{s}</li>)}</ul></div>}
                {item.feedback.suggestions.length > 0 && <div><p className="text-xs font-semibold text-warning">Cần cải thiện</p><ul className="list-disc pl-5 text-[13px]">{item.feedback.suggestions.map((s, i) => <li key={i}>{s}</li>)}</ul></div>}
                {item.feedback.parts.filter((part) => part.comment).length > 0 && (
                  <div>
                    <p className="text-xs font-semibold text-body">Theo từng phần</p>
                    <ul className="list-disc pl-5 text-[13px]">
                      {item.feedback.parts.filter((part) => part.comment).map((part, i) => <li key={i}>{partTitles?.[part.partId] ? `${partTitles[part.partId]}: ` : ""}{part.comment}</li>)}
                    </ul>
                  </div>
                )}
              </>
            )}
          </article>
        ))}
        <p className="text-xs text-muted">Dùng khóa API của bạn. Chưa có khóa? <Link href="/ai/connection" className="font-semibold text-primary-ink">Kết nối AI</Link></p>
      </div>
    </Dialog>
  );
}
