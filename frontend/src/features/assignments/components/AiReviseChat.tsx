"use client";

import { useEffect, useLayoutEffect, useRef, useState, type FormEvent, type KeyboardEvent, type ReactNode } from "react";
import { Icon, IconButton, cx, showErrorToast, type IconName } from "@/shared/ui";
import { assignmentsApi } from "../api";
import { errorMessage, requestRef } from "../model/format";
import type { AiDraft, Material } from "../model/types";
import { readTemplateFile, useSourceUploads, type TemplateFile } from "./sourceUploads";

const POLL_MS = 2000;
const MAX_REQUEST = 4000;
const MAX_WARNINGS = 3;
const MAX_WAIT_MS = 3 * 60 * 1000;
const MAX_MATERIALS = 10;
/** Ô nhắn cao tối đa 6 dòng (dòng 16px, đệm và viền 14px); dài hơn thì cuộn trong ô. */
const INPUT_MAX_HEIGHT = 6 * 16 + 14;
/** Biểu tượng phân biệt hai loại tệp gửi kèm, dùng cả ở menu "+" và ở hàng thẻ. */
const TEMPLATE_ICON: IconName = "assignment";
const REFERENCE_ICON: IconName = "menu_book";

type Message = {
  id: number;
  role: "user" | "ai";
  text: string;
  tone?: "danger";
  /** Mẫu đề và tài liệu tham khảo gửi kèm yêu cầu này. */
  template?: string;
  references?: string[];
};
type TemplateOption = { assignmentId: string; title: string; subjectTemplate: boolean };
/** Menu của nút "+": danh sách gốc, hoặc danh sách đề có sẵn / học liệu để chọn. */
type AttachMenu = "root" | "templates" | "materials";

/**
 * Trợ lý AI sửa đề đang có (yêu cầu 2026-11-09), là popup nổi ở góc phải dưới trang soạn, mở / đóng bằng một nút tròn chỉ
 * có biểu tượng (yêu cầu 2026-11-12). Máy chủ gửi AI toàn bộ đề và rubric đã lưu, AI trả bản đã sửa và trang ghi thẳng lên
 * đề; Ctrl+Z quay lại bản trước. Tệp gửi kèm thêm bằng nút "+" cạnh ô nhắn, menu chỉ có hai dòng: Mẫu đề (AI làm theo bố
 * cục; chỉ một mẫu, đã có thì dòng này khóa và ghi "Đã có mẫu đề") và Tài liệu tham khảo (AI lấy nội dung). Bấm một dòng thì
 * ra danh sách để chọn, dòng đầu của danh sách là tải tệp lên. Có tệp thì hiện một hàng thẻ nhỏ có tên ngay trên ô nhắn,
 * không ghi chữ đầu hàng: mẫu đề đứng đầu, rồi các tài liệu tham khảo, phân biệt bằng biểu tượng. Gửi xong thì bỏ các tệp.
 */
export function AiReviseChat({ assignmentId, prepare, onApplied }: {
  assignmentId: string;
  /** Lưu mọi thay đổi đang chờ, trả version mới nhất; lỗi thì ném kèm lý do. */
  prepare: () => Promise<number>;
  onApplied: () => Promise<void>;
}) {
  const [open, setOpen] = useState(false);
  /** Đã mở lần nào chưa: lần đầu mở mới tải danh sách đề có sẵn, học liệu, giới hạn tệp. */
  const [opened, setOpened] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [menu, setMenu] = useState<AttachMenu | null>(null);
  const [query, setQuery] = useState("");
  const [templates, setTemplates] = useState<TemplateOption[]>([]);
  const [templateId, setTemplateId] = useState("");
  const [template, setTemplate] = useState<TemplateFile | null>(null);
  const [materials, setMaterials] = useState<Material[]>([]);
  const [chosen, setChosen] = useState<string[]>([]);
  const sources = useSourceUploads(assignmentId, showErrorToast, opened);
  const nextId = useRef(0);
  const alive = useRef(true);
  const list = useRef<HTMLDivElement>(null);
  const templateInput = useRef<HTMLInputElement>(null);
  const referenceInput = useRef<HTMLInputElement>(null);
  const attach = useRef<HTMLDivElement>(null);
  const field = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    alive.current = true;
    return () => { alive.current = false; };
  }, []);

  useEffect(() => {
    if (list.current) list.current.scrollTop = list.current.scrollHeight;
  }, [messages, busy, open]);

  useEffect(() => {
    if (open) field.current?.focus();
  }, [open]);

  // Ô nhắn một dòng, gõ xuống dòng thì cao dần theo chữ tới 6 dòng rồi mới cuộn (yêu cầu 2026-11-12).
  useLayoutEffect(() => {
    const node = field.current;
    if (!node) return;
    node.style.height = "auto";
    const height = Math.min(node.scrollHeight + 2, INPUT_MAX_HEIGHT);
    node.style.height = `${height}px`;
    node.style.overflowY = node.scrollHeight + 2 > INPUT_MAX_HEIGHT ? "auto" : "hidden";
  }, [input, open]);

  useEffect(() => {
    if (!opened) return;
    const controller = new AbortController();
    assignmentsApi.listAiTemplates(assignmentId, controller.signal, true).then(
      (found) => { if (!controller.signal.aborted) setTemplates(found); },
      () => {},
    );
    assignmentsApi.materials(assignmentId, controller.signal).then(
      (found) => { if (!controller.signal.aborted) setMaterials(found.filter((item) => item.hasText)); },
      () => {},
    );
    return () => controller.abort();
  }, [assignmentId, opened]);

  // Menu của nút "+": bấm ra ngoài thì đóng.
  useEffect(() => {
    if (!menu) return;
    const close = (event: MouseEvent) => {
      if (!attach.current?.contains(event.target as Node)) setMenu(null);
    };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, [menu]);

  function toggle() {
    setOpened(true);
    setOpen((value) => !value);
    setMenu(null);
  }

  function showMenu(next: AttachMenu | null) {
    setQuery("");
    setMenu(next);
  }

  function say(message: Omit<Message, "id">) {
    setMessages((current) => [...current, { ...message, id: nextId.current++ }]);
  }

  async function readTemplate(file: File) {
    setTemplateId("");
    setTemplate({ file, status: "READING" });
    const read = await readTemplateFile(assignmentId, file);
    if (alive.current) setTemplate((current) => (current?.file === file ? read : current));
  }

  const pickedTemplate = templates.find((item) => item.assignmentId === templateId);
  const pickedMaterials = chosen.flatMap((id) => materials.filter((item) => item.materialId === id));
  const hasTemplate = Boolean(template || pickedTemplate);
  const hasReferences = pickedMaterials.length > 0 || sources.uploads.length > 0;

  async function send(event?: FormEvent) {
    event?.preventDefault();
    const text = input.trim();
    if (!text || busy) return;
    if (sources.pending || (template && template.status !== "READY")) {
      showErrorToast("Hãy đợi tải xong hoặc gỡ các tệp bị lỗi trước khi gửi.");
      return;
    }
    const templateName = template ? template.file.name : pickedTemplate?.title;
    const references = [...pickedMaterials.map((item) => item.title), ...sources.uploads.map((item) => item.file.name)];
    const attachments = {
      formatText: template?.text,
      templateAssignmentId: template ? undefined : templateId || undefined,
      materialIds: chosen,
      sourceFileRefs: sources.fileRefs,
    };
    const attached = Boolean(templateName) || references.length > 0;
    setInput("");
    setMenu(null);
    say({ role: "user", text, template: templateName, references });
    setBusy(true);
    let draft: AiDraft | null = null;
    try {
      await prepare();
      const requested = attached
        ? await assignmentsApi.requestAiRevision(assignmentId, text, requestRef(), attachments)
        : await assignmentsApi.requestAiRevision(assignmentId, text, requestRef());
      // Máy chủ đã nhận tệp đính kèm: bỏ khỏi ô, yêu cầu sau không tự gửi lại.
      setTemplate(null);
      setTemplateId("");
      setChosen([]);
      sources.clear();
      draft = await waitForResult(assignmentId, requested, () => alive.current);
      if (!alive.current) return;
      if (draft.status !== "READY" || !draft.proposal) {
        say({ role: "ai", text: `${draft.safeError ?? "AI không trả kết quả."} Đề chưa thay đổi.`, tone: "danger" });
        return;
      }
      if (draft.proposal.unchanged) {
        say({ role: "ai", text: "AI chưa thay đổi gì. Hãy nói rõ hơn, ví dụ: \"Thêm 10 câu hỏi về thiết kế cơ sở dữ liệu, mỗi câu 10 điểm\"." });
        void assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
        return;
      }
      const validation = draft.proposal.validation;
      if (validation?.status === "INVALID") {
        say({ role: "ai", text: `Không áp dụng được: ${validation.issues.map((issue) => issue.message).join(" ")}`, tone: "danger" });
        void assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
        return;
      }
      const version = await prepare();
      await assignmentsApi.applyAiDraft(assignmentId, draft.draftId, version, true, true);
      await onApplied();
      const warnings = validation?.issues.filter((issue) => issue.severity === "WARNING") ?? [];
      const shown = warnings.slice(0, MAX_WARNINGS).map((issue) => `Lưu ý: ${issue.message}`);
      if (warnings.length > MAX_WARNINGS) shown.push(`…và ${warnings.length - MAX_WARNINGS} lưu ý khác.`);
      say({ role: "ai", text: ["Đã sửa đề theo yêu cầu. Nhấn Ctrl+Z để quay lại bản trước.", ...shown].join("\n") });
    } catch (error) {
      if (!alive.current) return;
      say({ role: "ai", text: `${errorMessage(error)} Đề chưa thay đổi.`, tone: "danger" });
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
    if (event.key === "Escape") setOpen(false);
  }

  const needle = query.trim().toLocaleLowerCase("vi");
  const matches = (title: string) => !needle || title.toLocaleLowerCase("vi").includes(needle);
  const templateChoices = templates.filter((item) => item.assignmentId !== templateId && matches(item.title));
  const materialChoices = materials.filter((item) => !chosen.includes(item.materialId) && matches(item.title));

  return (
    <>
      {/* Popup giữ nguyên khi đóng (chỉ ẩn) để cuộc trò chuyện và lượt AI đang chạy không mất. */}
      <section id="ai-revise-panel" aria-label="Sửa đề bằng AI" data-testid="ai-revise-chat" hidden={!open}
        className={cx("fixed right-4 bottom-20 z-40 w-[min(22rem,calc(100vw-2rem))] flex-col rounded-2xl border",
          "border-line bg-surface shadow-[var(--shadow-float)] h-[min(30rem,calc(100dvh-9.5rem))]",
          open && "flex animate-pop")}>
        <header className="flex shrink-0 items-center gap-2 border-b border-line py-2 pr-2 pl-3.5">
          <Icon name="smart_toy" size={17} className="text-primary" />
          <h2 className="flex-1 text-[13.5px] font-semibold text-ink">Sửa đề bằng AI</h2>
          <IconButton icon="close" label="Đóng trợ lý AI" size={28} onClick={() => setOpen(false)} />
        </header>
        <div ref={list} role="log" aria-live="polite" className="flex min-h-0 flex-1 flex-col gap-1.5 overflow-y-auto px-3.5 py-3">
          {messages.map((message) => (
            <div key={message.id} className={cx("max-w-[90%] rounded-lg px-2.5 py-1.5 text-xs",
              message.role === "user" ? "self-end bg-primary text-on-primary"
                : message.tone === "danger" ? "self-start bg-danger-soft text-danger" : "self-start bg-sunken text-ink")}>
              <p className="whitespace-pre-line">{message.text}</p>
              {(message.template || (message.references?.length ?? 0) > 0) && (
                <ul className="mt-1.5 flex flex-col gap-0.5 border-t border-white/30 pt-1.5 text-[11px]" aria-label="Tệp gửi kèm">
                  {message.template && <li><b>Mẫu đề:</b> {message.template}</li>}
                  {(message.references?.length ?? 0) > 0 && <li><b>Tài liệu tham khảo:</b> {message.references!.join(", ")}</li>}
                </ul>
              )}
            </div>
          ))}
          {busy && <p role="status" className="self-start rounded-lg bg-sunken px-2.5 py-1.5 text-xs text-muted">AI đang sửa đề…</p>}
        </div>
        <div className="flex shrink-0 flex-col gap-2 border-t border-line px-3 py-2.5">
          {/* Một hàng thẻ nhỏ, chỉ hiện khi có tệp: mẫu đề đứng đầu, rồi tài liệu tham khảo; phân biệt bằng biểu tượng. */}
          {(hasTemplate || hasReferences) && (
            <ul aria-label="Tệp gửi kèm yêu cầu" data-testid="ai-revise-attachments" className="flex max-h-[3.25rem] flex-wrap gap-1 overflow-y-auto">
              {template && (
                <Chip kind="template" name={template.file.name} disabled={busy}
                  state={template.status === "READING" ? "…" : undefined} error={template.status === "ERROR" ? template.error : undefined}
                  onRetry={() => void readTemplate(template.file)} onRemove={() => setTemplate(null)} />
              )}
              {!template && pickedTemplate && (
                <Chip kind="template" name={pickedTemplate.title} disabled={busy} onRemove={() => setTemplateId("")} />
              )}
              {pickedMaterials.map((item) => (
                <Chip key={item.materialId} kind="reference" name={item.title} disabled={busy}
                  onRemove={() => setChosen((current) => current.filter((id) => id !== item.materialId))} />
              ))}
              {sources.uploads.map((item) => (
                <Chip key={item.id} kind="reference" name={item.file.name} disabled={busy}
                  state={item.status === "UPLOADING" ? `${item.percent}%` : item.status === "READING" ? "…" : undefined}
                  error={item.status === "ERROR" ? item.error : undefined}
                  onRetry={() => sources.retry(item)} onRemove={() => sources.remove(item)} />
              ))}
            </ul>
          )}
          <form onSubmit={(event) => void send(event)} className="flex items-end gap-1.5">
            <div ref={attach} className="relative shrink-0">
              <IconButton icon="add" label="Thêm mẫu đề hoặc tài liệu tham khảo" data-testid="ai-revise-attach" disabled={busy}
                aria-haspopup="menu" aria-expanded={menu !== null} onClick={() => showMenu(menu ? null : "root")}
                className="border border-line-strong" />
              {menu && (
                <div role="menu" aria-label="Tệp gửi kèm" data-testid="ai-revise-attach-menu"
                  onKeyDown={(event) => { if (event.key === "Escape") { event.stopPropagation(); setMenu(null); } }}
                  className="absolute bottom-full left-0 z-10 mb-1.5 flex max-h-52 w-52 animate-pop flex-col overflow-hidden rounded-xl border border-line bg-surface py-1 shadow-[var(--shadow-float)]">
                  {menu === "root" ? (
                    <>
                      <MenuRow icon={TEMPLATE_ICON} disabled={hasTemplate} onClick={() => showMenu("templates")}>
                        {hasTemplate ? "Đã có mẫu đề" : "Mẫu đề"}
                      </MenuRow>
                      <MenuRow icon={REFERENCE_ICON} onClick={() => showMenu("materials")}>Tài liệu tham khảo</MenuRow>
                    </>
                  ) : (
                    <>
                      <div className="flex shrink-0 items-center gap-1 border-b border-line px-1.5 pb-1">
                        <IconButton icon="arrow_back" label="Quay lại" size={26} onClick={() => showMenu("root")} />
                        <input autoFocus value={query} onChange={(event) => setQuery(event.target.value)}
                          aria-label={menu === "templates" ? "Tìm mẫu đề" : "Tìm tài liệu tham khảo"} placeholder="Tìm theo tên"
                          className="h-7 min-w-0 flex-1 bg-transparent text-xs text-ink outline-none placeholder:text-muted" />
                      </div>
                      {/* Danh sách cuộn trong khung menu; dòng đầu là tải tệp lên. */}
                      <div className="flex min-h-0 flex-col overflow-y-auto">
                        {menu === "templates" ? (
                          <MenuRow icon="upload_file" onClick={() => { setMenu(null); templateInput.current?.click(); }}>Tải tệp lên</MenuRow>
                        ) : (
                          <MenuRow icon="upload_file" disabled={!sources.canUpload}
                            onClick={() => { setMenu(null); referenceInput.current?.click(); }}>Tải tệp lên</MenuRow>
                        )}
                        {menu === "templates" && templateChoices.map((item) => (
                          <MenuRow key={item.assignmentId} icon={TEMPLATE_ICON}
                            onClick={() => { setTemplate(null); setTemplateId(item.assignmentId); setMenu(null); }}>{item.title}</MenuRow>
                        ))}
                        {menu === "materials" && materialChoices.map((item) => (
                          <MenuRow key={item.materialId} icon={REFERENCE_ICON}
                            onClick={() => {
                              setChosen((current) => (current.includes(item.materialId) ? current : [...current, item.materialId].slice(0, MAX_MATERIALS)));
                              setMenu(null);
                            }}>{item.title}</MenuRow>
                        ))}
                      </div>
                    </>
                  )}
                </div>
              )}
            </div>
            <textarea ref={field} aria-label="Yêu cầu sửa đề" data-testid="ai-revise-input" rows={1} maxLength={MAX_REQUEST}
              value={input} disabled={busy} onChange={(event) => setInput(event.target.value)} onKeyDown={handleKeyDown}
              placeholder="Ví dụ: dịch toàn bộ đề sang tiếng Anh"
              className="min-h-8 flex-1 resize-none rounded-lg border border-field bg-surface px-2.5 py-1.5 text-xs leading-4 text-ink outline-none focus:outline-2 focus:outline-focus disabled:opacity-60" />
            <IconButton type="submit" icon="arrow_forward" label="Gửi yêu cầu" data-testid="ai-revise-send"
              disabled={busy || !input.trim()} className="bg-primary text-on-primary hover:bg-primary-strong hover:text-on-primary" />
          </form>
          <input ref={templateInput} type="file" accept=".docx" hidden aria-label="Tải mẫu đề lên" data-testid="ai-revise-template-file"
            onChange={(event) => {
              const file = event.target.files?.[0];
              event.target.value = "";
              if (file) void readTemplate(file);
            }} />
          <input ref={referenceInput} type="file" multiple accept={sources.accept} hidden aria-label="Tải tài liệu tham khảo lên"
            data-testid="ai-revise-reference-file"
            onChange={(event) => { sources.add(event.target.files); event.target.value = ""; }} />
        </div>
      </section>
      <button type="button" data-testid="ai-revise-toggle" aria-expanded={open} aria-controls="ai-revise-panel" onClick={toggle}
        aria-label={open ? "Đóng trợ lý AI" : "Sửa đề bằng AI"} title={open ? "Đóng trợ lý AI" : "Sửa đề bằng AI"}
        className={cx("fixed right-4 bottom-5 z-40 inline-flex size-12 items-center justify-center rounded-full bg-primary",
          "text-on-primary shadow-[var(--shadow-float)] transition-colors hover:bg-primary-strong",
          "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus")}>
        <Icon name={open ? "close" : "smart_toy"} size={22} />
        {busy && !open && <span aria-hidden className="absolute top-1 right-1 size-2.5 animate-pulse rounded-full border-2 border-primary bg-on-primary" />}
      </button>
    </>
  );
}

/** Chờ AI trả kết quả; quá 3 phút thì bỏ bản nháp và coi là lỗi. {@code alive}: trang còn mở. */
async function waitForResult(assignmentId: string, draft: AiDraft, alive: () => boolean): Promise<AiDraft> {
  let current = draft;
  const deadline = Date.now() + MAX_WAIT_MS;
  while (current.status === "PENDING" && alive()) {
    if (Date.now() > deadline) {
      void assignmentsApi.discardAiDraft(assignmentId, current.draftId).catch(() => {});
      return { ...current, status: "FAILED", safeError: "AI phản hồi quá lâu." };
    }
    await new Promise((resolve) => setTimeout(resolve, POLL_MS));
    current = await assignmentsApi.aiDraft(assignmentId, current.draftId);
  }
  return current;
}

/** Thẻ nhỏ của một tệp gửi kèm: biểu tượng nói loại (mẫu đề hay tài liệu tham khảo), tên, nút gỡ; lỗi thì viền đỏ, bấm tên để thử lại. */
function Chip({ kind, name, state, error, disabled, onRemove, onRetry }: {
  kind: "template" | "reference";
  name: string;
  state?: string;
  error?: string;
  disabled?: boolean;
  onRemove: () => void;
  onRetry?: () => void;
}) {
  const role = kind === "template" ? "Mẫu đề" : "Tài liệu tham khảo";
  return (
    <li title={error ? `${role}: ${name} (${error})` : `${role}: ${name}`}
      className={cx("inline-flex h-[22px] max-w-full items-center gap-1 rounded-md border pr-0.5 pl-1.5 text-[11px]",
        error ? "border-danger bg-danger-soft text-danger" : kind === "template" ? "border-primary bg-primary-soft text-primary-ink" : "border-line bg-sunken text-ink")}>
      <Icon name={kind === "template" ? TEMPLATE_ICON : REFERENCE_ICON} size={12} className="shrink-0" />
      <span className="sr-only">{role}: </span>
      {error && onRetry ? (
        <button type="button" aria-label={`Thử lại ${name}`} onClick={onRetry} className="max-w-[9rem] min-w-0 truncate underline">{name}</button>
      ) : (
        <span className="max-w-[9rem] min-w-0 truncate">{name}</span>
      )}
      {state && <span role="status" className="shrink-0 text-muted tabular-nums">{state}</span>}
      <button type="button" aria-label={`Gỡ ${name}`} disabled={disabled} onClick={onRemove}
        className="flex size-4 shrink-0 items-center justify-center rounded text-muted hover:text-danger disabled:opacity-50">
        <Icon name="close" size={11} />
      </button>
    </li>
  );
}

function MenuRow({ icon, disabled, onClick, children }: {
  icon: IconName;
  disabled?: boolean;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button type="button" role="menuitem" disabled={disabled} onClick={onClick}
      className="flex w-full shrink-0 items-center gap-2 px-3 py-1.5 text-left text-xs text-ink hover:bg-canvas disabled:cursor-not-allowed disabled:text-faint">
      <Icon name={icon} size={14} className="shrink-0 text-muted" />
      <span className="min-w-0 flex-1 truncate">{children}</span>
    </button>
  );
}
