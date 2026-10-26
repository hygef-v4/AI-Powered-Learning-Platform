"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Alert, Button, Dialog, Field, Pill, Section, Select } from "@/shared/ui";
import { workApi } from "./api";
import { errorMessage, formatDateTime, requestRef } from "./format";
import type { Feedback } from "./types";

const POLL_MS = 3000;

/**
 * Góp ý AI của bài Practice (F9, F10, UC 41): dùng API cá nhân; nhãn "AI ước tính, không phải điểm chính thức". Không
 * có ước tính theo rubric vì người học không xem rubric (yêu cầu 2026-10-21).
 */
export function PracticeFeedbackPanel({ attemptId, partTitles }: { attemptId: string; partTitles?: Record<string, string> }) {
  const [items, setItems] = useState<Feedback[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [asking, setAsking] = useState(false);
  const [tick, setTick] = useState(0);

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

  const refresh = useCallback(() => setTick((value) => value + 1), []);

  return (
    <Section title="Góp ý AI" icon="smart_toy"
      actions={<Button size="sm" onClick={() => setAsking(true)} disabled={pending} data-testid="ask-feedback">{items?.length ? "Nhận góp ý lại" : "Nhận góp ý AI"}</Button>}>
      {error !== null && <Alert tone="danger" role="alert">{errorMessage(error)}</Alert>}
      {pending && <p role="status" className="text-xs text-muted">AI đang góp ý…</p>}
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
      {asking && <AskDialog attemptId={attemptId} onClose={() => setAsking(false)} onDone={() => { setAsking(false); refresh(); }} />}
    </Section>
  );
}

function AskDialog({ attemptId, onClose, onDone }: { attemptId: string; onClose: () => void; onDone: () => void }) {
  const [provider, setProvider] = useState("GEMINI");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ref] = useState(requestRef);
  return (
    <Dialog open title="Nhận góp ý AI" icon="smart_toy" onClose={onClose} dismissible={!busy}
      footer={<>
        <Button variant="primary" disabled={busy} data-testid="confirm-feedback" onClick={async () => {
          setBusy(true);
          setError(null);
          try {
            await workApi.requestFeedback(attemptId, provider, ref);
            onDone();
          } catch (e) {
            setError(errorMessage(e));
            setBusy(false);
          }
        }}>Gửi</Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <Field label="API cá nhân" hint="Bạn chịu chi phí của khóa API.">
        <Select aria-label="Nhà cung cấp" value={provider} onChange={(e) => setProvider(e.target.value)}>
          <option value="GEMINI">Gemini</option>
          <option value="OPENAI">OpenAI</option>
          <option value="CLAUDE">Claude</option>
        </Select>
      </Field>
      <p className="text-xs text-muted">Chưa có khóa? <Link href="/ai/connection" className="font-semibold text-primary-ink">Kết nối AI</Link></p>
    </Dialog>
  );
}
