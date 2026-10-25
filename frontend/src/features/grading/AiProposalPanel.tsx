"use client";
import { useState } from "react";
import type { AiRequest, Proposal, Checks } from "./types";
import { AiSourcePicker, type SourceChoice } from "@/shared/ai";

/** Preserve the U09 request DTO; shared provider choice remains implicit as before. */
export function aiRequest(choice: SourceChoice): AiRequest {
  return {
    source: choice.source,
    ...(choice.source === "PERSONAL" && choice.provider ? { provider: choice.provider } : {}),
    requestRef: crypto.randomUUID(),
  };
}
export function AiProposalPanel({
  proposals,
  onApply,
  onRequest,
  disabled = false,
}: {
  evaluationId: string;
  proposals: Proposal[];
  onApply: (checks: Checks, feedback: string, id: string) => void;
  onRequest: (input: AiRequest) => void;
  disabled?: boolean;
}) {
  const [choosingSource, setChoosingSource] = useState(false);
  return (
    <section aria-label="Đề xuất AI" className="my-5 rounded-xl border border-line p-4">
      <h2 className="font-bold">Đề xuất AI</h2>
      <p>AI chỉ đề xuất. Bạn kiểm tra rubric và lưu nháp trước khi chốt.</p>
      <button
        className="mt-3 rounded-lg bg-primary px-4 py-2 text-white disabled:opacity-40"
        disabled={disabled}
        onClick={() => setChoosingSource(true)}
      >
        Yêu cầu đề xuất mới
      </button>
      {choosingSource && (
        <AiSourcePicker
          open
          task="Đề xuất chấm bài đang xem"
          busy={disabled}
          onCancel={() => setChoosingSource(false)}
          onSubmit={(choice) => {
            if (disabled) return;
            setChoosingSource(false);
            onRequest(aiRequest(choice));
          }}
        />
      )}
      {proposals.map((p) => (
        <article key={p.id} className="mt-3 rounded border p-3">
          <strong>
            #{p.versionNo} · {p.origin === "MCP" ? (p.agentLabel ?? "Agent") : (p.source ?? "API")}{" "}
            · {p.status}
          </strong>
          <p>
            {p.provider}
            {p.model && ` · ${p.model}`}
          </p>
          <p>{p.proposal?.feedback ?? p.safeError}</p>
          {p.proposal?.sections?.map((s) => (
            <p key={s.partId}>
              <strong>{s.partId}:</strong> {s.feedback}
            </p>
          ))}
          <button
            className="mt-2 rounded border px-3 py-1 disabled:opacity-40"
            disabled={disabled || p.status !== "READY" || !p.proposal}
            onClick={() =>
              p.proposal && onApply(p.proposal.checks, p.proposal.feedback ?? "", p.id)
            }
          >
            Dùng đề xuất
          </button>
        </article>
      ))}
    </section>
  );
}
