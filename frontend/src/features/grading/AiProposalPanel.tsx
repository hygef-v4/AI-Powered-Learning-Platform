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
    <section aria-label="Đề xuất AI" className="rounded-xl border border-line bg-white p-4 sm:p-5">
      <h2 className="font-bold">Đề xuất AI</h2>
      <p className="mt-1 text-sm leading-relaxed text-muted">AI chỉ đề xuất. Bạn kiểm tra rubric và lưu nháp trước khi chốt.</p>
      <button
        className="mt-3 rounded-lg bg-primary px-4 py-2 text-white disabled:opacity-40"
        disabled={disabled || proposals.some((proposal) => proposal.status === "PENDING")}
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
      {proposals.length === 0 && <p className="mt-3 text-sm text-muted">Chưa có đề xuất. Bạn có thể tự chấm theo rubric hoặc yêu cầu AI hỗ trợ.</p>}
      {proposals.map((p, index) => (
        <article key={p.id} className="mt-3 rounded-lg border border-line bg-shell/40 p-3 text-sm">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <strong className={p.status === "FAILED" || p.status === "REJECTED" ? "text-danger" : "text-ink"}>
              {({ PENDING: "Đang xử lý", READY: "Sẵn sàng để xem", FAILED: "Không tạo được đề xuất", STALE: "Đề xuất đã cũ", REJECTED: "Đề xuất không hợp lệ" } as Record<string, string>)[p.status] ?? "Chưa khả dụng"}
            </strong>
            <span className="text-xs text-muted">Lần {p.versionNo} · {p.origin === "MCP" ? (p.agentLabel ?? "Agent") : p.source === "PERSONAL" ? "API cá nhân" : "API trường"}</span>
          </div>
          {p.provider && <p className="mt-1 text-xs text-muted">{p.provider}{p.model && ` · ${p.model}`}</p>}
          {p.status === "PENDING" && <p role="status" className="mt-2 text-muted">AI đang đọc bài và rubric. Bạn vẫn có thể chấm thủ công trong lúc chờ.</p>}
          {(p.status === "FAILED" || p.status === "REJECTED") && <div className="mt-2 rounded-lg bg-danger/5 p-3">
            <p>{p.safeError && !/^AI task (failed|unavailable or failed)$/.test(p.safeError) ? p.safeError : "Tác vụ AI không thành công. Kiểm tra kết nối hoặc thử lại bằng nguồn AI đã cấu hình."}</p>
            <p className="mt-1 text-muted">Bạn vẫn có thể chấm thủ công theo rubric. Điểm chưa bị thay đổi.</p>
            <a href="/ai/connection" className="mt-2 inline-block font-semibold text-primary underline underline-offset-2">Kiểm tra kết nối AI</a>
          </div>}
          {p.status === "STALE" && <p className="mt-2 text-muted">Bài nộp hoặc rubric đã thay đổi. Hãy yêu cầu một đề xuất mới.</p>}
          {p.proposal && <>
            <p className="mt-2 font-semibold">Điểm AI đề xuất: {Number(p.proposal.score).toFixed(2)}</p>
            {p.proposal.feedback && <p className="mt-2 whitespace-pre-wrap leading-relaxed">{p.proposal.feedback}</p>}
            {!!p.proposal.sections?.length && <details className="mt-2"><summary className="cursor-pointer text-primary">Nhận xét từng phần</summary>
              {p.proposal.sections.map((section, sectionIndex) => <p key={section.partId} className="mt-2"><strong>Phần {sectionIndex + 1}:</strong> {section.feedback}</p>)}
            </details>}
          </>}
          {index > 0 && <p className="mt-1 text-xs text-muted">Đề xuất trước đó</p>}
          <button className="mt-3 rounded-lg border border-line-strong bg-white px-3 py-2 font-semibold text-body disabled:cursor-not-allowed disabled:opacity-40"
            disabled={disabled || p.status !== "READY" || !p.proposal}
            onClick={() => p.proposal && onApply(p.proposal.checks, p.proposal.feedback ?? "", p.id)}>Dùng đề xuất</button>
        </article>
      ))}
    </section>
  );
}
