"use client";

import { useEffect, useId, useRef, useState } from "react";
import { AiSourcePicker, AiTaskStatus, PROVIDER_LABEL, type AiProvider, type SourceChoice } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, cx, Dialog, Field, Icon, Pill, Select, TextInput } from "@/shared/ui";
import { DIFFICULTY_LABELS, errorMessage, failureLabel, formatDateTime, TYPE_LABELS } from "./labels";
import { McqEditor } from "./McqEditor";
import { formOf, toContent, validateForm, type FormErrors, type QuestionForm } from "./questionForm";
import type {
  AiDraft, AiDraftRequest, Difficulty, QuestionContent, QuestionType, SelectableMaterial, Violation,
} from "./types";

const TEXTAREA = "w-full rounded-[9px] border border-line-strong px-3 py-2 text-[13px] text-ink focus-visible:outline-primary";
const POLL_MS = 3000;
const MAX_COUNT = 50;
const MAX_MATERIALS = 10;
/** Trần token đầu ra của một câu (giống backend `QuizDraftPrompt.maxOutputTokens`), để popup U06 hiện ước tính. */
const TOKENS_PER_QUESTION = 600;

/** Cách gọi API của một đích (ngân hàng môn hoặc một Quiz); dialog không cần biết đích là gì. */
export type DraftTargetApi = {
  title: string;
  /** Dòng "tác vụ · đích" trên popup Chọn nguồn AI của U06. */
  taskLabel: string;
  acceptNote: string;
  loadMaterials: (signal: AbortSignal) => Promise<SelectableMaterial[]>;
  list: (signal: AbortSignal) => Promise<AiDraft[]>;
  request: (input: AiDraftRequest & { choice: SourceChoice }) => Promise<AiDraft>;
  get: (draftId: string) => Promise<AiDraft>;
  /** Ném lỗi khi không chấp nhận được (409, 422…); thành công thì dialog đóng. */
  accept: (draftId: string, questions: QuestionContent[]) => Promise<void>;
  discard: (draftId: string) => Promise<AiDraft>;
};

type EntryState = "open" | "accepted" | "dropped";
export type ProposalEntry = { form: QuestionForm; state: EntryState; editing: boolean; errors: FormErrors; serverViolations: Violation[] };

const entriesOf = (draft: AiDraft): ProposalEntry[] => draft.questions.map((q) => ({
  form: formOf(q.content), state: "open", editing: false, errors: {}, serverViolations: q.violations,
}));

/**
 * Popup "Tạo nháp bằng AI" (F4, F8): gửi yêu cầu, chờ AI, rồi sửa, bỏ, chấp nhận từng câu (BR-U10-41).
 * Mở lại thì hiện đề xuất đang chờ hoặc đã xong gần nhất; AI không tự thêm câu.
 */
export function AiDraftDialog({ api, initialDraftId, onClose, onAccepted }: {
  api: DraftTargetApi;
  initialDraftId?: string | null;
  onClose: () => void;
  onAccepted: (count: number) => void;
}) {
  const [draft, setDraft] = useState<AiDraft | null>(null);
  const [entries, setEntries] = useState<ProposalEntry[]>([]);
  const [mode, setMode] = useState<"loading" | "form" | "draft">("loading");
  const [error, setError] = useState<string | null>(null);
  const [problems, setProblems] = useState<Violation[]>([]);
  const [busy, setBusy] = useState(false);
  // M1: form đã kiểm chờ chọn nguồn; `picking` thì thay popup U10 bằng AiSourcePicker để hai Dialog không tranh focus.
  const [request, setRequest] = useState<AiDraftRequest | null>(null);
  const [picking, setPicking] = useState(false);
  const shown = useRef<AiDraft | null>(null);

  /** Chỉ dựng lại danh sách câu khi đổi đề xuất hoặc trạng thái, để không mất phần người soạn đang sửa. */
  const showDraft = (next: AiDraft | null) => {
    const current = shown.current;
    if (next && (current?.id !== next.id || current.status !== next.status)) setEntries(entriesOf(next));
    if (!next) setEntries([]);
    shown.current = next;
    setDraft(next);
    setMode(next ? "draft" : "form");
  };

  useEffect(() => {
    const controller = new AbortController();
    const start = initialDraftId
      ? api.get(initialDraftId).then((found): AiDraft | null => found)
      : api.list(controller.signal).then((list) => list.find((d) => d.status === "PENDING" || d.status === "READY") ?? null);
    start
      .then((found) => {
        if (controller.signal.aborted) return;
        showDraft(found);
      })
      .catch((failure: unknown) => {
        if (controller.signal.aborted) return;
        setError(errorMessage(failure));
        setMode("form");
      });
    return () => controller.abort();
    // Chỉ chạy khi mở popup; api đổi theo phiên bản quiz không cần tải lại.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialDraftId]);

  const pendingId = draft?.status === "PENDING" ? draft.id : null;
  useEffect(() => {
    if (!pendingId) return;
    let active = true;
    const timer = setTimeout(() => {
      api.get(pendingId)
        .then((next) => active && showDraft(next))
        .catch((failure: unknown) => active && setError(errorMessage(failure)));
    }, POLL_MS);
    return () => {
      active = false;
      clearTimeout(timer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pendingId, draft]);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    setProblems([]);
    try {
      await action();
    } catch (failure) {
      const problem = failure instanceof ApiError ? (failure.problem as { violations?: Violation[] }) : {};
      setProblems(problem.violations ?? []);
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };

  const send = (choice: SourceChoice) => request && void run(async () => {
    try {
      showDraft(await api.request({ ...request, choice }));
      setRequest(null);
    } finally {
      setPicking(false);
    }
  });
  const refresh = (draftId: string) => {
    api.get(draftId).then(showDraft).catch((failure: unknown) => setError(errorMessage(failure)));
  };

  const accepted = entries.filter((e) => e.state === "accepted");
  const acceptAll = () => draft && void run(async () => {
    await api.accept(draft.id, accepted.map((e) => toContent(e.form)));
    onAccepted(accepted.length);
  });
  const discard = () => draft && void run(async () => {
    await api.discard(draft.id);
    showDraft(null);
  });

  const footer = mode === "form" ? (
    <Button onClick={onClose} disabled={busy}>Đóng</Button>
  ) : (
    <>
      <Button onClick={onClose} disabled={busy}>Đóng</Button>
      {draft && draft.status !== "PENDING" && (
        <Button onClick={() => showDraft(null)} disabled={busy} data-testid="ai-draft-new">Yêu cầu mới</Button>
      )}
      {draft?.status === "READY" && (
        <>
          <Button variant="danger" onClick={discard} disabled={busy} data-testid="ai-draft-discard">Bỏ đề xuất</Button>
          <Button variant="primary" onClick={acceptAll} disabled={busy || accepted.length === 0} data-testid="ai-draft-accept">
            Lưu {accepted.length} câu đã chấp nhận
          </Button>
        </>
      )}
    </>
  );

  if (picking && request) {
    return (
      <AiSourcePicker open task={api.taskLabel} estimatedTokens={TOKENS_PER_QUESTION * request.count} busy={busy}
        onCancel={() => setPicking(false)} onSubmit={send} />
    );
  }

  return (
    <Dialog open onClose={onClose} dismissible={!busy} size="lg" icon="smart_toy" title={api.title} footer={footer}>
      <div className="flex flex-col gap-4" data-testid="ai-draft-dialog">
        {error && (
          <Alert tone="danger" role="alert">
            {error}
            {problems.length > 0 && <ul className="mt-1 list-disc pl-4">{problems.map((p, i) => <li key={i}>{p.message}</li>)}</ul>}
          </Alert>
        )}
        {mode === "loading" && <p role="status" className="py-6 text-center text-sm text-muted">Đang tải…</p>}
        {mode === "form" && (
          <DraftRequestForm api={api} busy={busy} initial={request}
            onSubmit={(values) => {
              setRequest(values);
              setPicking(true);
            }} />
        )}
        {mode === "draft" && draft && (
          <DraftView draft={draft} entries={entries} onEntries={setEntries} note={api.acceptNote}
            onTaskFinished={() => refresh(draft.id)} />
        )}
      </div>
    </Dialog>
  );
}

/**
 * Yêu cầu, số câu, loại, độ khó, tài liệu đã có chữ (BR-U10-40). Nguồn AI chọn ở popup U06 sau khi bấm "Gửi yêu cầu";
 * hủy popup thì form mở lại với `initial`.
 */
function DraftRequestForm({ api, busy, initial, onSubmit }: {
  api: DraftTargetApi;
  busy: boolean;
  initial: AiDraftRequest | null;
  onSubmit: (values: AiDraftRequest) => void;
}) {
  const id = useId();
  const [instructions, setInstructions] = useState(initial?.instructions ?? "");
  const [count, setCount] = useState(initial ? String(initial.count) : "5");
  const [type, setType] = useState<QuestionType | "">(initial?.type ?? "");
  const [difficulty, setDifficulty] = useState<Difficulty | "">(initial?.difficulty ?? "");
  const [materialIds, setMaterialIds] = useState<string[]>(initial?.materialIds ?? []);
  const [materials, setMaterials] = useState<SelectableMaterial[] | null>(null);
  const [materialsError, setMaterialsError] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    const controller = new AbortController();
    api.loadMaterials(controller.signal)
      .then((list) => !controller.signal.aborted && setMaterials(list))
      .catch(() => !controller.signal.aborted && setMaterialsError(true));
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const submit = () => {
    const found: Record<string, string> = {};
    const n = Number(count);
    if (!instructions.trim() || instructions.trim().length > 2000) found.instructions = "Nhập yêu cầu, tối đa 2.000 ký tự";
    if (!Number.isInteger(n) || n < 1 || n > MAX_COUNT) found.count = "Từ 1 đến 50 câu";
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    onSubmit({ instructions: instructions.trim(), count: n, type: type || null, difficulty: difficulty || null, materialIds });
  };

  const toggle = (materialId: string) => setMaterialIds((current) => current.includes(materialId)
    ? current.filter((m) => m !== materialId) : [...current, materialId]);

  return (
    <fieldset disabled={busy} className="flex flex-col gap-3" data-testid="ai-draft-form">
      <Field label="Yêu cầu" htmlFor={`${id}-instructions`} required error={errors.instructions}
        hint="Ví dụ: chủ đề, chương, kiểu câu cần soạn. Không đưa thông tin sinh viên.">
        <textarea id={`${id}-instructions`} rows={3} maxLength={2000} value={instructions} className={TEXTAREA}
          onChange={(event) => setInstructions(event.target.value)} data-testid="ai-draft-instructions" />
      </Field>
      <div className="grid gap-3 sm:grid-cols-3">
        <Field label="Số câu" htmlFor={`${id}-count`} required error={errors.count}>
          <TextInput id={`${id}-count`} inputMode="numeric" value={count} invalid={!!errors.count}
            onChange={(event) => setCount(event.target.value)} data-testid="ai-draft-count" />
        </Field>
        <Field label="Loại câu" htmlFor={`${id}-type`}>
          <Select id={`${id}-type`} value={type} onChange={(event) => setType(event.target.value as QuestionType | "")}>
            <option value="">Tùy nội dung</option>
            {(Object.keys(TYPE_LABELS) as QuestionType[]).map((t) => <option key={t} value={t}>{TYPE_LABELS[t]}</option>)}
          </Select>
        </Field>
        <Field label="Độ khó" htmlFor={`${id}-difficulty`}>
          <Select id={`${id}-difficulty`} value={difficulty} onChange={(event) => setDifficulty(event.target.value as Difficulty | "")}>
            <option value="">Phân bổ hợp lý</option>
            {(Object.keys(DIFFICULTY_LABELS) as Difficulty[]).map((d) => <option key={d} value={d}>{DIFFICULTY_LABELS[d]}</option>)}
          </Select>
        </Field>
      </div>
      <div className="flex flex-col gap-1.5">
        <p className="text-xs font-semibold text-body">Tài liệu tham khảo <span className="font-normal text-muted">· tối đa 10, chỉ học liệu đã trích chữ</span></p>
        {materialsError && <p className="text-[11px] text-muted">Chưa lấy được danh sách tài liệu; vẫn gửi được yêu cầu không kèm tài liệu.</p>}
        {materials?.length === 0 && <p className="text-[11px] text-muted">Chưa có học liệu nào.</p>}
        <ul className="flex flex-col gap-1" aria-label="Tài liệu tham khảo">
          {materials?.map((m) => {
            const checked = materialIds.includes(m.materialId);
            const blocked = !m.hasText || (!checked && materialIds.length >= MAX_MATERIALS);
            return (
              <li key={m.materialId}>
                <label className={cx("inline-flex items-center gap-2 text-[13px]", blocked && !checked && "text-faint")}>
                  <input type="checkbox" checked={checked} disabled={blocked && !checked} onChange={() => toggle(m.materialId)}
                    data-testid="ai-draft-material" />
                  {m.title}{!m.hasText && " (chưa trích được chữ)"}
                </label>
              </li>
            );
          })}
        </ul>
      </div>
      <p className="text-[11px] text-muted">Bấm “Gửi yêu cầu” để chọn nguồn AI; hết hạn mức hay lỗi nguồn sẽ không tự đổi sang nguồn khác.</p>
      <div className="flex justify-end">
        <Button variant="primary" icon="smart_toy" onClick={submit} data-testid="ai-draft-submit">Gửi yêu cầu</Button>
      </div>
    </fieldset>
  );
}

/** Đề xuất đang chờ, lỗi hoặc đã xong; với đề xuất xong thì sửa, bỏ, chấp nhận từng câu. */
function DraftView({ draft, entries, onEntries, note, onTaskFinished }: {
  draft: AiDraft;
  entries: ProposalEntry[];
  onEntries: (entries: ProposalEntry[]) => void;
  note: string;
  onTaskFinished: () => void;
}) {
  const origin = draft.origin === "MCP" ? `Agent: ${draft.agentLabel ?? "Agent"}`
    : draft.source === "SHARED" ? "API của trường" : `${PROVIDER_LABEL[draft.provider as AiProvider] ?? draft.provider} cá nhân`;
  const update = (index: number, change: Partial<ProposalEntry>) =>
    onEntries(entries.map((entry, i) => (i === index ? { ...entry, ...change } : entry)));
  const accept = (index: number) => {
    const entry = entries[index];
    if (!entry) return;
    const errors = validateForm(entry.form);
    update(index, Object.keys(errors).length > 0 ? { errors, editing: true } : { errors, state: "accepted", editing: false });
  };
  const acceptedCount = entries.filter((e) => e.state === "accepted").length;

  return (
    <div className="flex flex-col gap-3" data-testid="ai-draft-view">
      <p className="text-[12px] text-muted">
        {origin} · gửi lúc {formatDateTime(draft.createdAt)}
        {draft.materials.length > 0 && <> · tài liệu: {draft.materials.map((m) => m.title).join(", ")}</>}
      </p>
      {draft.status === "PENDING" && (
        <>
          <Alert tone="info" role="status">AI đang soạn đề xuất… Bạn có thể đóng popup, đề xuất vẫn được giữ.</Alert>
          {/* M3: trạng thái task của U06 (cho hủy khi còn chờ); xong thì tải lại đề xuất ngay. */}
          {draft.taskId && <AiTaskStatus taskId={draft.taskId} onFinished={onTaskFinished} />}
        </>
      )}
      {draft.status === "FAILED" && (
        <div data-testid="ai-draft-failed">
          <Alert tone="danger" role="alert">{failureLabel(draft.failureCode, draft.failureMessage)}</Alert>
        </div>
      )}
      {draft.status === "READY" && (
        <>
          <p className="text-[13px] font-semibold text-ink">Đề xuất ({acceptedCount} đã chấp nhận / {entries.length})</p>
          <ol className="flex flex-col gap-3">
            {entries.map((entry, index) => (
              <ProposalCard key={index} index={index} entry={entry} onChange={(change) => update(index, change)}
                onAccept={() => accept(index)} />
            ))}
          </ol>
          <p className="text-[11px] text-muted">{note}</p>
        </>
      )}
    </div>
  );
}

function ProposalCard({ index, entry, onChange, onAccept }: {
  index: number;
  entry: ProposalEntry;
  onChange: (change: Partial<ProposalEntry>) => void;
  onAccept: () => void;
}) {
  const { form, state } = entry;
  return (
    <li className={cx("rounded-[11px] border p-3", state === "accepted" ? "border-success/40 bg-success-soft/40"
      : state === "dropped" ? "border-line bg-shell opacity-60" : "border-line")} data-testid="ai-draft-question">
      <div className="mb-2 flex flex-wrap items-center gap-2">
        <span className="text-[12px] font-semibold text-body">Câu {index + 1} · {TYPE_LABELS[form.type]}</span>
        {state === "accepted" && <Pill tone="success">Đã chấp nhận</Pill>}
        {state === "dropped" && <Pill tone="neutral">Đã bỏ</Pill>}
        <span className="ml-auto flex gap-1.5">
          {state !== "dropped" && (
            <Button size="sm" onClick={() => onChange({ editing: !entry.editing, state: "open" })} data-testid="ai-draft-edit">
              {entry.editing ? "Xong" : "Sửa"}
            </Button>
          )}
          <Button size="sm" onClick={() => onChange({ state: state === "dropped" ? "open" : "dropped", editing: false })}
            data-testid="ai-draft-drop">{state === "dropped" ? "Lấy lại" : "Bỏ"}</Button>
          {state === "open" && <Button size="sm" variant="primary" onClick={onAccept} data-testid="ai-draft-accept-one">Chấp nhận</Button>}
        </span>
      </div>
      {entry.editing ? (
        <McqEditor value={form} onChange={(next) => onChange({ form: next })} errors={entry.errors} />
      ) : (
        <>
          <p className="whitespace-pre-wrap text-[13px] text-ink">{form.stem}</p>
          <ul className="mt-1.5 flex flex-col gap-0.5">
            {form.options.map((option) => {
              const correct = form.correctOptionIds.includes(option.id);
              return (
                <li key={option.id} className={cx("flex items-center gap-1.5 text-[12.5px]", correct ? "font-semibold text-success" : "text-body")}>
                  {correct ? <Icon name="check_circle" size={15} /> : <span aria-hidden className="inline-block size-[15px] text-center">·</span>}
                  {option.text}{correct && <span className="sr-only"> (đáp án đúng)</span>}
                </li>
              );
            })}
          </ul>
          {form.explanation && <p className="mt-1.5 text-[12px] text-muted">Giải thích: {form.explanation}</p>}
          {entry.serverViolations.length > 0 && state === "open" && (
            <p className="mt-1.5 text-[11px] text-danger">Cần sửa: {entry.serverViolations.map((v) => v.message).join("; ")}</p>
          )}
        </>
      )}
    </li>
  );
}
