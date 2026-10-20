"use client";
import { useState } from "react";
import type { AiRequest, Proposal, Checks } from "./types";

export function AiSourceFields({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="block">
      Nguồn AI
      <select
        className="ml-2 rounded border p-2"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      >
        <option value="">Chọn nguồn AI</option>
        <option value="SHARED">API của trường · trường chịu chi phí</option>
        <option value="GEMINI">Gemini cá nhân · bạn chịu chi phí</option>
        <option value="OPENAI">OpenAI cá nhân · bạn chịu chi phí</option>
        <option value="CLAUDE">Claude cá nhân · bạn chịu chi phí</option>
      </select>
      <small className="mt-2 block text-slate-600">
        Chưa đọc được hạn mức/token hiện tại. Nguồn cá nhân cần API key đã cấu hình. Hết quota sẽ
        không tự đổi nguồn.
      </small>
    </label>
  );
}
export function aiRequest(source: string): AiRequest {
  return source === "SHARED"
    ? { source: "SHARED", requestRef: crypto.randomUUID() }
    : {
        source: "PERSONAL",
        provider: source as AiRequest["provider"],
        requestRef: crypto.randomUUID(),
      };
}
export function AiProposalPanel({
  evaluationId,
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
  const [source, setSource] = useState("");
  return (
    <section aria-label="Đề xuất AI" className="my-5 rounded-xl border border-line p-4">
      <h2 className="font-bold">Đề xuất AI</h2>
      <p>AI chỉ đề xuất. Bạn kiểm tra rubric và lưu nháp trước khi chốt.</p>
      <AiSourceFields value={source} onChange={setSource} />
      <button
        className="mt-3 rounded-lg bg-primary px-4 py-2 text-white disabled:opacity-40"
        disabled={disabled || !source}
        onClick={() => onRequest(aiRequest(source))}
      >
        Yêu cầu đề xuất mới
      </button>
      <span className="sr-only">{evaluationId}</span>
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
