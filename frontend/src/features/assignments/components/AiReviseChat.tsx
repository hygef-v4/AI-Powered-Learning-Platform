"use client";

import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import { Icon, IconButton, cx } from "@/shared/ui";
import { assignmentsApi } from "../api";
import { errorMessage, requestRef } from "../model/format";
import type { AiDraft } from "../model/types";

const POLL_MS = 2000;
const MAX_REQUEST = 4000;
const MAX_WARNINGS = 3;
const MAX_WAIT_MS = 3 * 60 * 1000;

type Message = { id: number; role: "user" | "ai"; text: string; tone?: "danger" };

/**
 * Khung chat nhỏ sửa đề đang có bằng AI (yêu cầu 2026-11-09): máy chủ gửi AI toàn bộ đề và rubric đã lưu, AI trả bản
 * đã sửa và trang ghi thẳng lên đề; Ctrl+Z quay lại bản trước. Mỗi yêu cầu sửa trên bản mới nhất của đề.
 */
export function AiReviseChat({ assignmentId, prepare, onApplied }: {
  assignmentId: string;
  /** Lưu mọi thay đổi đang chờ, trả version mới nhất; lỗi thì ném kèm lý do. */
  prepare: () => Promise<number>;
  onApplied: () => Promise<void>;
}) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const nextId = useRef(0);
  const alive = useRef(true);
  const list = useRef<HTMLDivElement>(null);

  useEffect(() => {
    alive.current = true;
    return () => { alive.current = false; };
  }, []);

  useEffect(() => {
    if (list.current) list.current.scrollTop = list.current.scrollHeight;
  }, [messages, busy]);

  function say(role: Message["role"], text: string, tone?: Message["tone"]) {
    setMessages((current) => [...current, { id: nextId.current++, role, text, tone }]);
  }

  async function waitForResult(draft: AiDraft): Promise<AiDraft> {
    let current = draft;
    const deadline = Date.now() + MAX_WAIT_MS;
    while (current.status === "PENDING" && alive.current) {
      if (Date.now() > deadline) {
        void assignmentsApi.discardAiDraft(assignmentId, current.draftId).catch(() => {});
        return { ...current, status: "FAILED", safeError: "AI phản hồi quá lâu." };
      }
      await new Promise((resolve) => setTimeout(resolve, POLL_MS));
      current = await assignmentsApi.aiDraft(assignmentId, current.draftId);
    }
    return current;
  }

  async function send(event?: FormEvent) {
    event?.preventDefault();
    const text = input.trim();
    if (!text || busy) return;
    setInput("");
    say("user", text);
    setBusy(true);
    let draft: AiDraft | null = null;
    try {
      await prepare();
      draft = await waitForResult(await assignmentsApi.requestAiRevision(assignmentId, text, requestRef()));
      if (!alive.current) return;
      if (draft.status !== "READY" || !draft.proposal) {
        say("ai", `${draft.safeError ?? "AI không trả kết quả."} Đề chưa thay đổi.`, "danger");
        return;
      }
      if (draft.proposal.unchanged) {
        say("ai", "AI chưa thay đổi gì. Hãy nói rõ hơn, ví dụ: \"Thêm 10 câu hỏi về thiết kế cơ sở dữ liệu, mỗi câu 10 điểm\".");
        void assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
        return;
      }
      const validation = draft.proposal.validation;
      if (validation?.status === "INVALID") {
        say("ai", `Không áp dụng được: ${validation.issues.map((issue) => issue.message).join(" ")}`, "danger");
        void assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
        return;
      }
      const version = await prepare();
      await assignmentsApi.applyAiDraft(assignmentId, draft.draftId, version, true, true);
      await onApplied();
      const warnings = validation?.issues.filter((issue) => issue.severity === "WARNING") ?? [];
      const shown = warnings.slice(0, MAX_WARNINGS).map((issue) => `Lưu ý: ${issue.message}`);
      if (warnings.length > MAX_WARNINGS) shown.push(`…và ${warnings.length - MAX_WARNINGS} lưu ý khác.`);
      say("ai", ["Đã sửa đề theo yêu cầu. Nhấn Ctrl+Z để quay lại bản trước.", ...shown].join("\n"));
    } catch (error) {
      if (!alive.current) return;
      say("ai", `${errorMessage(error)} Đề chưa thay đổi.`, "danger");
      if (draft?.status === "READY") void assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
    } finally {
      if (alive.current) setBusy(false);
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault();
      void send();
    }
  }

  return (
    <section aria-label="Sửa đề bằng AI" data-testid="ai-revise-chat"
      className="flex flex-col gap-2 rounded-xl border border-line bg-surface p-3 shadow-sm">
      <div className="flex items-center gap-2">
        <Icon name="smart_toy" size={16} className="text-primary" />
        <p className="text-[13px] font-semibold text-ink">Sửa đề bằng AI</p>
      </div>
      {(messages.length > 0 || busy) && (
        <div ref={list} role="log" aria-live="polite" className="flex max-h-56 flex-col gap-1.5 overflow-y-auto">
          {messages.map((message) => (
            <p key={message.id} className={cx("max-w-[90%] whitespace-pre-line rounded-lg px-2.5 py-1.5 text-xs",
              message.role === "user" ? "self-end bg-primary text-on-primary"
                : message.tone === "danger" ? "self-start bg-danger-soft text-danger" : "self-start bg-sunken text-ink")}>
              {message.text}
            </p>
          ))}
          {busy && <p role="status" className="self-start rounded-lg bg-sunken px-2.5 py-1.5 text-xs text-muted">AI đang sửa đề…</p>}
        </div>
      )}
      <form onSubmit={(event) => void send(event)} className="flex items-end gap-1.5">
        <textarea aria-label="Yêu cầu sửa đề" data-testid="ai-revise-input" rows={2} maxLength={MAX_REQUEST}
          value={input} disabled={busy} onChange={(event) => setInput(event.target.value)} onKeyDown={handleKeyDown}
          placeholder="Ví dụ: dịch toàn bộ đề sang tiếng Anh"
          className="min-h-9 flex-1 resize-none rounded-lg border border-field bg-surface px-2.5 py-1.5 text-xs text-ink outline-none focus:outline-2 focus:outline-focus disabled:opacity-60" />
        <IconButton type="submit" icon="arrow_forward" label="Gửi yêu cầu" data-testid="ai-revise-send"
          disabled={busy || !input.trim()} className="bg-primary text-on-primary hover:bg-primary-strong hover:text-on-primary" />
      </form>
    </section>
  );
}
