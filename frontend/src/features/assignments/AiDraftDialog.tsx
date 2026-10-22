"use client";

import { useEffect, useRef, useState } from "react";
import { Alert, Button, Dialog, Field, Pill, Select } from "@/shared/ui";
import { assignmentsApi } from "./api";
import { errorMessage, requestRef } from "./format";
import type { AiDraft, Material } from "./types";

type Props = {
  assignmentId: string;
  version: number;
  currentPartCount: number;
  onClose: () => void;
  onApplied: () => void;
};

const POLL_MS = 2000;

/** t13b Tạo nháp bằng AI (F6; BR-U07-50, 51): đề xuất xem trước, Áp dụng thay khung, Bỏ thì bài không đổi. */
export function AiDraftDialog({ assignmentId, version, currentPartCount, onClose, onApplied }: Props) {
  const [request, setRequest] = useState("");
  const [materials, setMaterials] = useState<Material[]>([]);
  const [chosen, setChosen] = useState<string[]>([]);
  const [source, setSource] = useState<"SHARED" | "PERSONAL">("SHARED");
  const [provider, setProvider] = useState("GEMINI");
  const [draft, setDraft] = useState<AiDraft | null>(null);
  const [replaceInstructions, setReplaceInstructions] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const ref = useRef(requestRef());

  useEffect(() => {
    const controller = new AbortController();
    assignmentsApi.materials(assignmentId, controller.signal).then((list) => {
      if (!controller.signal.aborted) setMaterials(list.filter((m) => m.hasText));
    }, () => {});
    return () => controller.abort();
  }, [assignmentId]);

  useEffect(() => {
    if (!draft || draft.status !== "PENDING") return;
    const timer = setTimeout(() => {
      assignmentsApi.aiDraft(assignmentId, draft.draftId).then(setDraft, (e) => setError(errorMessage(e)));
    }, POLL_MS);
    return () => clearTimeout(timer);
  }, [assignmentId, draft]);

  async function send() {
    setBusy(true);
    setError(null);
    try {
      setDraft(await assignmentsApi.requestAiDraft(assignmentId, {
        request, materialIds: chosen, source, provider: source === "PERSONAL" ? provider : undefined, requestRef: ref.current,
      }));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function apply() {
    if (!draft) return;
    setBusy(true);
    setError(null);
    try {
      await assignmentsApi.applyAiDraft(assignmentId, draft.draftId, version, replaceInstructions);
      onApplied();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function discard() {
    if (draft && (draft.status === "READY" || draft.status === "PENDING")) {
      await assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
    }
    onClose();
  }

  const ready = draft?.status === "READY" && draft.proposal;
  return (
    <Dialog open size="lg" icon="smart_toy" title="Tạo nháp bằng AI" onClose={() => void discard()} dismissible={!busy}
      footer={ready ? <>
        <Button onClick={() => void discard()} disabled={busy}>Bỏ</Button>
        <Button variant="primary" onClick={() => void apply()} disabled={busy} data-testid="ai-apply">Áp dụng</Button>
      </> : <>
        <Button onClick={() => void discard()} disabled={busy}>Hủy</Button>
        <Button variant="primary" onClick={() => void send()} disabled={busy || !request.trim() || draft?.status === "PENDING"} data-testid="ai-send">
          Tạo đề xuất
        </Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {!ready && (
        <>
          <Field label="Yêu cầu" required htmlFor="ai-request" hint="Chủ đề, mức độ, số phần, gợi ý về rubric.">
            <textarea id="ai-request" rows={4} maxLength={4000} value={request} onChange={(e) => setRequest(e.target.value)}
              className="w-full rounded-[9px] border border-line-strong px-3 py-2 text-[13px] outline-none focus:outline-2 focus:outline-primary" />
          </Field>
          <Field label="Tài liệu tham khảo" hint="Chỉ học liệu đã trích được chữ.">
            {materials.length === 0 ? <p className="text-xs text-muted">Chưa có học liệu dùng được.</p> : (
              <div className="flex max-h-32 flex-col gap-1 overflow-y-auto">
                {materials.map((m) => (
                  <label key={m.materialId} className="flex items-center gap-2 text-[13px]">
                    <input type="checkbox" checked={chosen.includes(m.materialId)}
                      onChange={(e) => setChosen((c) => e.target.checked ? [...c, m.materialId].slice(0, 10) : c.filter((id) => id !== m.materialId))} />
                    {m.title}
                  </label>
                ))}
              </div>
            )}
          </Field>
          <Field label="Nguồn AI">
            <div className="flex flex-wrap items-center gap-2">
              <Select aria-label="Nguồn AI" value={source} wrapperClassName="w-56" onChange={(e) => setSource(e.target.value as "SHARED" | "PERSONAL")}>
                <option value="SHARED">API chung (quota giảng viên)</option>
                <option value="PERSONAL">API cá nhân</option>
              </Select>
              {source === "PERSONAL" && (
                <Select aria-label="Nhà cung cấp" value={provider} wrapperClassName="w-36" onChange={(e) => setProvider(e.target.value)}>
                  <option value="GEMINI">Gemini</option>
                  <option value="OPENAI">OpenAI</option>
                  <option value="CLAUDE">Claude</option>
                </Select>
              )}
            </div>
          </Field>
          {draft?.status === "PENDING" && <p role="status" className="text-sm text-muted">AI đang soạn đề xuất…</p>}
          {draft?.status === "FAILED" && <Alert tone="warning">{draft.safeError ?? "AI không trả kết quả."} Bạn vẫn soạn thủ công được.</Alert>}
        </>
      )}
      {ready && draft.proposal && (
        <div className="flex flex-col gap-3" data-testid="ai-proposal">
          {currentPartCount > 0 && <Alert tone="warning">Áp dụng sẽ thay toàn bộ khung tài liệu hiện tại ({currentPartCount} mục).</Alert>}
          <label className="flex items-center gap-2 text-[13px]">
            <input type="checkbox" checked={replaceInstructions} onChange={(e) => setReplaceInstructions(e.target.checked)} />
            Thay cả hướng dẫn chung
          </label>
          {draft.proposal.instructions && <p className="rounded-lg bg-canvas p-3 text-[13px] whitespace-pre-wrap">{draft.proposal.instructions}</p>}
          <ol className="flex flex-col gap-2">
            {draft.proposal.sections.map((section, index) => (
              <li key={index} style={{ marginLeft: `${(section.level - 1) * 16}px` }} className="rounded-lg border border-line p-2">
                <p className="text-[13px] font-semibold text-ink">{section.title}</p>
                {section.guidance && <p className="text-xs text-body">{section.guidance}</p>}
                {section.rubric.length > 0 && (
                  <p className="mt-1 text-[11px] text-muted">
                    Rubric gợi ý: {section.rubric.map((c) => `${c.title} (${c.items.reduce((s, i) => s + (i.points ?? 0), 0)} điểm)`).join(", ")}
                  </p>
                )}
              </li>
            ))}
          </ol>
          <p className="flex flex-wrap items-center gap-1 text-[11px] text-muted">
            Nguồn: <Pill>{String(draft.provenance.origin ?? "AI")}</Pill>
            {typeof draft.provenance.provider === "string" && <Pill>{draft.provenance.provider}</Pill>}
            {typeof draft.provenance.model === "string" && <Pill>{draft.provenance.model}</Pill>}
            {typeof draft.provenance.agentLabel === "string" && <Pill>{draft.provenance.agentLabel}</Pill>}
          </p>
        </div>
      )}
    </Dialog>
  );
}
