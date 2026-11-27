"use client";

import { AiChatAttachments } from "./AiChatAttachments";
import { AiRequestLength, MAX_AI_REQUEST_LENGTH } from "./AiRequestLength";
import { describeAiFailure } from "@/shared/api/errorMessages";
import { useAiChatAttachments } from "../hooks/useAiChatAttachments";
import { useEffect, useLayoutEffect, useRef, useState, type FormEvent, type KeyboardEvent, type ClipboardEvent, type DragEvent } from "react";
import { AiSourcePicker, AiTaskStatus, PROVIDER_LABEL, type SourceChoice } from "@/shared/ai";
import { Button, Dialog, Icon, IconButton, MenuItem, cx, showErrorToast, type IconName } from "@/shared/ui";
import { assignmentsApi } from "../api";
import { describeError, requestRef } from "../model/format";
import type { AiDraft, Material } from "../model/types";
import { msg } from "@/i18n/msg";
import { formatDate } from "@/shared/dateTime";

const POLL_MS = 2000;
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
  recover?: () => void;
};
type TemplateOption = { assignmentId: string; title: string; subjectTemplate: boolean; createdAt: string };
/** Menu của nút "+": danh sách gốc, hoặc danh sách đề có sẵn / học liệu để chọn. */
type AttachMenu = "root" | "templates" | "materials";

/** Composer chỉ quản lý tệp của assignment hiện tại; đổi bài hủy các callback cũ. */
export function AiReviseChat(props: { assignmentId: string; prepare: () => Promise<number>; onApplied: () => Promise<void> }) {
  return <AiReviseChatPanel key={props.assignmentId} {...props} />;
}

function AiReviseChatPanel({ assignmentId, prepare, onApplied }: {
  assignmentId: string; prepare: () => Promise<number>; onApplied: () => Promise<void>;
}) {
  const [open, setOpen] = useState(false);
  /** Đã mở lần nào chưa: lần đầu mở mới tải danh sách đề có sẵn, học liệu, giới hạn tệp. */
  const [opened, setOpened] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const requestTooLong = input.length > MAX_AI_REQUEST_LENGTH;
  const [busy, setBusy] = useState(false);
  const [sourcePicker, setSourcePicker] = useState<"send" | "choose" | null>(null);
  const [nextSource, setNextSource] = useState<SourceChoice | null>(null);
  const [previousSource, setPreviousSource] = useState<SourceChoice | null>(null);
  const [taskId, setTaskId] = useState<string | null>(null);
  const [menu, setMenu] = useState<AttachMenu | null>(null);
  const [query, setQuery] = useState("");
  const [templates, setTemplates] = useState<TemplateOption[]>([]);
  const [materials, setMaterials] = useState<Material[]>([]);
  const [chosen, setChosen] = useState<string[]>([]);
  const attachments = useAiChatAttachments(assignmentId, showErrorToast, opened);
  const { sources, template, templateId } = attachments;
  const [dragging, setDragging] = useState(false);
  const dragDepth = useRef(0);
  const submitting = useRef(false);
  const pendingRequest = useRef<{ key: string; ref: string } | null>(null);
  const nextId = useRef(0);
  const alive = useRef(true);
  const list = useRef<HTMLDivElement>(null);
  const referenceInput = useRef<HTMLInputElement>(null);
  const attach = useRef<HTMLDivElement>(null);
  const field = useRef<HTMLTextAreaElement>(null);
  const composerBusy = busy || sourcePicker !== null;

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
    if (open) {
      closeChat();
      return;
    }
    setOpened(true);
    setOpen(true);
  }

  function showMenu(next: AttachMenu | null) {
    setQuery("");
    setMenu(next);
  }

  function addMessage(message: Omit<Message, "id">) {
    setMessages((current) => [...current, { ...message, id: nextId.current++ }]);
  }

  const pickedTemplate = templates.find(item => item.assignmentId === templateId);
  const pickedMaterials = chosen.flatMap(id => materials.filter(item => item.materialId === id));
  const hasTemplate = Boolean(template || pickedTemplate);
  const hasReferences = pickedMaterials.length > 0 || sources.uploads.length > 0;

  function handleSend(event?: FormEvent) {
    event?.preventDefault();
    if (sourcePicker || submitting.current || !input.trim() || requestTooLong) return;
    if (attachments.pending) { showErrorToast(msg("assignments.components.AiReviseChat.waitForUploadsToFinish")); return; }
    setMenu(null);
    setSourcePicker("send");
  }

  function handleChooseSource(choice: SourceChoice) {
    if (sourcePicker === "choose") {
      setNextSource(choice);
      setSourcePicker(null);
    } else {
      // An unconfirmed request can be retried with the same selected source and request reference.
      if (nextSource) setNextSource(choice);
      void sendMessage(choice);
    }
  }

  async function sendMessage(choice: SourceChoice) {
    setSourcePicker(null);
    const text = input.trim();
    if (!text || submitting.current || requestTooLong) return;
    if (attachments.pending) { showErrorToast(msg("assignments.components.AiReviseChat.waitForUploadsToFinish")); return; }
    const snapshot = attachments.snapshot();
    const materialIds = [...chosen];
    const templateName = template ? template.file.name : pickedTemplate?.title;
    const references = [...pickedMaterials.map(item => item.title), ...sources.uploads.map(item => item.file.name)];
    const payload = {
      source: choice.source,
      provider: choice.source === "PERSONAL" ? choice.provider ?? undefined : undefined,
      ...(hasTemplate || hasReferences ? {
        formatText: template?.text,
        templateAssignmentId: template ? undefined : templateId || undefined,
        materialIds,
        sourceFileRefs: sources.fileRefs,
      } : {}),
    };
    const key = JSON.stringify([text, payload]);
    if (pendingRequest.current?.key !== key) pendingRequest.current = { key, ref: requestRef() };
    const ref = pendingRequest.current.ref;
    const recover = () => { setInput(text); attachments.restore(snapshot); setChosen(materialIds); };
    submitting.current = true;
    setBusy(true);
    setMenu(null);
    let draft: AiDraft | null = null;
    let accepted = false;
    let applied = false;
    let applying = false;
    try {
      await prepare();
      if (!alive.current) return;
      const requested = await assignmentsApi.requestAiRevision(assignmentId, text, ref, payload);
      if (!alive.current) return;
      accepted = true;
      pendingRequest.current = null;
      setNextSource(null);
      setPreviousSource(choice);
      addMessage({ role: "user", text, template: templateName, references });
      setInput("");
      attachments.clear();
      setChosen([]);
      if (requested.status === "PENDING") setTaskId(requested.taskId);
      draft = await waitForResult(assignmentId, requested, () => alive.current);
      if (!alive.current) return;
      if (draft.status !== "READY" || !draft.proposal) {
        const failure = draft.safeError ? describeAiFailure(draft.safeError)
          : msg("assignments.components.AiReviseChat.theAiReturnedNoResult");
        addMessage({ role: "ai", text: msg("assignments.components.AiReviseChat.theAssignmentWasNotChanged", { value: failure }), tone: "danger", recover });
        return;
      }
      if (draft.proposal.unchanged) {
        addMessage({ role: "ai", text: msg("assignments.components.AiReviseChat.theAiDidNotChange"), recover });
        void assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
        return;
      }
      const validation = draft.proposal.validation;
      if (validation?.status === "INVALID") {
        const detail = validation.issues.map(issue => issue.message).join(" ");
        addMessage({ role: "ai", text: msg("assignments.components.AiReviseChat.couldNotApply", {
          value: describeAiFailure(`[AI_OUTPUT_INVALID] ${detail}`),
        }), tone: "danger", recover });
        void assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
        return;
      }
      const version = await prepare();
      if (!alive.current) return;
      applying = true;
      await assignmentsApi.applyAiDraft(assignmentId, draft.draftId, version, true, true);
      applied = true;
      await onApplied();
      if (!alive.current) return;
      const warnings = validation?.issues.filter(issue => issue.severity === "WARNING") ?? [];
      const shown = warnings.slice(0, MAX_WARNINGS).map(issue => msg("assignments.components.AiReviseChat.note", { message: String(issue.message) }));
      if (warnings.length > MAX_WARNINGS) shown.push(msg("assignments.components.AiReviseChat.andMoreNotes", { value: String(warnings.length - MAX_WARNINGS) }));
      addMessage({ role: "ai", text: [msg("assignments.components.AiReviseChat.theAssignmentWasEditedAs"), ...shown].join("\n") });
    } catch (error) {
      if (!alive.current) return;
      addMessage({ role: "ai", text: applied ? msg("assignments.components.AiReviseChat.theAssignmentWasUpdatedBut")
        : applying ? msg("assignments.components.AiReviseChat.couldNotConfirmTheResult", { value: String(describeError(error)) })
        : `${describeError(error)} ${accepted ? msg("assignments.components.AiReviseChat.theAssignmentWasNotChanged2") : msg("assignments.components.AiReviseChat.theRequestWasNotConfirmed")}`,
        tone: "danger", recover: accepted && !applying ? recover : undefined });
      if (!applying && draft?.status === "READY") void assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
    } finally {
      submitting.current = false;
      if (alive.current) {
        setBusy(false);
        setTaskId(null);
      }
    }
  }

  function addFiles(files: File[]) {
    if (!composerBusy && !attachments.processing) attachments.addFiles(files);
  }

  function pasteImages(event: ClipboardEvent<HTMLTextAreaElement>) {
    const files = Array.from(event.clipboardData.items ?? []).filter(item => item.kind === "file")
      .map(item => item.getAsFile()).filter((file): file is File => !!file && file.type.startsWith("image/"));
    if (!files.length) return;
    event.preventDefault();
    if (composerBusy) return;
    if (attachments.processing) showErrorToast(msg("assignments.components.AiReviseChat.waitUntilTheFileRole"));
    else addFiles(files);
    const text = event.clipboardData.getData("text/plain");
    if (text) {
      const start = event.currentTarget.selectionStart;
      const end = event.currentTarget.selectionEnd;
      setInput(current => current.slice(0, start) + text + current.slice(end));
    }
  }

  function dragFiles(event: DragEvent<HTMLElement>, entering: boolean) {
    if (!Array.from(event.dataTransfer.types).includes("Files")) return;
    event.preventDefault();
    if (composerBusy || attachments.processing) return;
    dragDepth.current = Math.max(0, dragDepth.current + (entering ? 1 : -1));
    setDragging(dragDepth.current > 0);
  }

  function dropFiles(event: DragEvent<HTMLElement>) {
    if (!Array.from(event.dataTransfer.types).includes("Files")) return;
    event.preventDefault();
    dragDepth.current = 0;
    setDragging(false);
    const items = Array.from(event.dataTransfer.items ?? []);
    let hasDirectory = false;
    const files = items.length ? items.flatMap(item => {
      if (item.webkitGetAsEntry?.()?.isDirectory) { hasDirectory = true; return []; }
      const file = item.kind === "file" ? item.getAsFile() : null;
      return file ? [file] : [];
    }) : Array.from(event.dataTransfer.files);
    if (hasDirectory) showErrorToast(msg("assignments.components.AiReviseChat.chooseSpecificFilesDoNot"));
    addFiles(files);
  }

  function closeChat() {
    setOpen(false);
    setSourcePicker(null);
    setMenu(null);
    dragDepth.current = 0;
    setDragging(false);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault();
      handleSend();
    }
    if (event.key === "Escape") closeChat();
  }

  const needle = query.trim().toLocaleLowerCase("vi");
  const matches = (title: string) => !needle || title.toLocaleLowerCase("vi").includes(needle);
  const templateChoices = templates.filter((item) => item.assignmentId !== templateId && matches(item.title));
  const materialChoices = materials.filter((item) => !chosen.includes(item.materialId) && matches(item.title));
  const sourceLabel = msg("assignments.components.AiReviseChat.changeAiSource");
  const nextSourceName = nextSource?.source === "SHARED" ? msg("shared.ai.AiSourcePicker.schoolApi")
    : nextSource?.provider ? PROVIDER_LABEL[nextSource.provider] : null;
  const sourceHint = nextSourceName ? msg("assignments.components.AiReviseChat.nextAiSource", { source: nextSourceName }) : undefined;

  return (
    <>
      {/* Popup giữ nguyên khi đóng (chỉ ẩn) để cuộc trò chuyện và lượt AI đang chạy không mất. */}
      <section id="ai-revise-panel" aria-label={msg("assignments.components.AiReviseChat.editAssignmentWithAi")} data-testid="ai-revise-chat" hidden={!open}
        onDragEnter={event => dragFiles(event, true)} onDragLeave={event => dragFiles(event, false)}
        onDragOver={event => { if (Array.from(event.dataTransfer.types).includes("Files")) { event.preventDefault(); event.dataTransfer.dropEffect = composerBusy ? "none" : "copy"; } }}
        onDrop={dropFiles}
        className={cx("fixed right-4 bottom-20 z-40 w-[min(22rem,calc(100vw-2rem))] flex-col rounded-2xl border",
          "border-line bg-surface shadow-[var(--shadow-float)] h-[min(30rem,calc(100dvh-9.5rem))]",
          open && "flex animate-pop")}>
        {dragging && <div role="status" className="pointer-events-none absolute inset-0 z-20 flex items-center justify-center rounded-2xl border-2 border-dashed border-primary bg-surface/95 text-sm font-semibold text-primary-ink">{msg("assignments.components.AiReviseChat.dropFilesToAttach")}</div>}
        <header className="flex shrink-0 items-center gap-2 border-b border-line py-2 pr-2 pl-3.5">
          <Icon name="smart_toy" size={17} className="text-primary" />
          <h2 className="flex-1 text-[13.5px] font-semibold text-ink">{msg("assignments.components.AiReviseChat.editAssignmentWithAi")}</h2>
          <IconButton icon="close" label={msg("assignments.components.AiReviseChat.closeAiAssistant")} size={28} onClick={closeChat} />
        </header>
        <div ref={list} role="log" aria-live="polite" className="flex min-h-0 flex-1 flex-col gap-1.5 overflow-y-auto px-3.5 py-3">
          {messages.map((message) => (
            <div key={message.id} className={cx("max-w-[90%] rounded-lg px-2.5 py-1.5 text-xs",
              message.role === "user" ? "self-end bg-primary text-on-primary"
                : message.tone === "danger" ? "self-start bg-danger-soft text-danger" : "self-start bg-sunken text-ink")}>
              <p className="whitespace-pre-line">{message.text}</p>
              {message.recover && <button type="button" disabled={composerBusy || !!input || hasTemplate || hasReferences}
                onClick={message.recover} className="mt-1 text-[11px] font-semibold underline disabled:opacity-40">{msg("assignments.components.AiReviseChat.restoreRequest")}</button>}
              {(message.template || (message.references?.length ?? 0) > 0) && (
                <ul className="mt-1.5 flex flex-col gap-0.5 border-t border-white/30 pt-1.5 text-[11px]" aria-label={msg("assignments.components.AiReviseChat.attachedFiles")}>
                  {message.template && <li><b>{msg("assignments.components.AiReviseChat.template")}</b> {message.template}</li>}
                  {(message.references?.length ?? 0) > 0 && <li><b>{msg("assignments.components.AiReviseChat.references")}</b> {message.references!.join(", ")}</li>}
                </ul>
              )}
            </div>
          ))}
          {busy && (taskId
            ? <AiTaskStatus key={taskId} taskId={taskId} cancellable={false} intervalMs={500} />
            : <p role="status" className="self-start rounded-lg bg-sunken px-2.5 py-1.5 text-xs text-muted">{msg("assignments.components.AiReviseChat.theAiIsEditingThe")}</p>)}
        </div>
        <div className="flex shrink-0 flex-col gap-2 border-t border-line px-3 py-2.5">
          <AiChatAttachments attachments={attachments} templateName={template?.file.name ?? pickedTemplate?.title}
            materials={pickedMaterials} disabled={composerBusy} onUnchoose={id => setChosen(current => current.filter(value => value !== id))} />
          <form onSubmit={handleSend} aria-busy={busy || sourcePicker === "send"} className="flex items-end gap-1.5">
            <div ref={attach} className="relative shrink-0">
              <IconButton icon="add" label={msg("assignments.components.AiReviseChat.addATemplateOrReferences")} data-testid="ai-revise-attach" disabled={composerBusy || attachments.processing}
                aria-haspopup="menu" aria-expanded={menu !== null} onClick={() => showMenu(menu ? null : "root")}
                className="border border-line-strong" />
              {menu && (
                <div role="menu" aria-label={msg("assignments.components.AiReviseChat.attachedFiles")} data-testid="ai-revise-attach-menu"
                  onKeyDown={(event) => { if (event.key === "Escape") { event.stopPropagation(); setMenu(null); } }}
                  className="absolute bottom-full left-0 z-10 mb-1.5 flex max-h-52 w-56 animate-pop flex-col overflow-hidden rounded-xl border border-line bg-surface p-1 shadow-[var(--shadow-float)]">
                  {menu === "root" ? (
                    <>
                      <MenuItem icon={TEMPLATE_ICON} className="w-full" onClick={() => showMenu("templates")}>{msg("assignments.components.AiReviseChat.chooseAnExistingTemplate")}</MenuItem>
                      <MenuItem icon={REFERENCE_ICON} className="w-full" onClick={() => showMenu("materials")}>{msg("assignments.components.AiReviseChat.chooseSubjectMaterials")}</MenuItem>
                      <MenuItem icon="upload_file" className="w-full" disabled={!sources.canUpload} onClick={() => { setMenu(null); referenceInput.current?.click(); }}>{msg("assignments.components.AiReviseChat.uploadFromComputer")}</MenuItem>
                    </>
                  ) : (
                    <>
                      <div className="flex shrink-0 items-center gap-1 border-b border-line px-1.5 pb-1">
                        <IconButton icon="arrow_back" label={msg("assignments.components.AiReviseChat.back")} size={26} onClick={() => showMenu("root")} />
                        <input autoFocus value={query} onChange={(event) => setQuery(event.target.value)}
                          aria-label={menu === "templates" ? msg("assignments.components.AiReviseChat.findTemplates") : msg("assignments.components.AiReviseChat.findReferences")} placeholder={msg("assignments.components.AiReviseChat.searchByName")}
                          className="h-7 min-w-0 flex-1 bg-transparent text-xs text-ink outline-none placeholder:text-muted" />
                      </div>
                      {/* Danh sách chỉ chứa nguồn có sẵn; tải từ máy nằm ở menu gốc. */}
                      <div className="flex min-h-0 flex-col overflow-y-auto">
                        {menu === "templates" && templateChoices.map((item) => (
                          <MenuItem key={item.assignmentId} icon={TEMPLATE_ICON} className="w-full"
                            onClick={() => { attachments.chooseExisting(item.assignmentId); setMenu(null); }}><span className="min-w-0 flex-1 truncate">{item.title}</span>{" "}
                            <span className="shrink-0 text-[11px] text-muted">{formatDate(item.createdAt)}</span></MenuItem>
                        ))}
                        {menu === "materials" && materialChoices.map((item) => (
                          <MenuItem key={item.materialId} icon={REFERENCE_ICON} className="w-full"
                            onClick={() => {
                              setChosen((current) => (current.includes(item.materialId) ? current : [...current, item.materialId].slice(0, MAX_MATERIALS)));
                              setMenu(null);
                            }}><span className="min-w-0 flex-1 truncate">{item.title}</span></MenuItem>
                        ))}
                      </div>
                    </>
                  )}
                </div>
              )}
            </div>
            <IconButton icon="swap_horiz" label={sourceLabel} aria-haspopup="dialog" disabled={composerBusy}
              aria-description={sourceHint} title={sourceHint ?? sourceLabel}
              className={cx("border border-line-strong", nextSource && "bg-primary-soft text-primary-ink")}
              onClick={() => {
                setMenu(null);
                setSourcePicker("choose");
              }} />
            <textarea ref={field} aria-label={msg("assignments.components.AiReviseChat.editRequest")} data-testid="ai-revise-input" rows={1}
              aria-describedby={input.length > 0 ? "revision-ai-request-length" : undefined}
              aria-invalid={requestTooLong || undefined}
              value={input} disabled={composerBusy} onPaste={pasteImages} onChange={(event) => setInput(event.target.value)} onKeyDown={handleKeyDown}
              placeholder={msg("assignments.components.AiReviseChat.exampleTranslateTheWholeAssignment")}
              className={cx("min-h-8 min-w-0 flex-1 resize-none rounded-lg border bg-surface px-2.5 py-1.5 text-xs leading-4 text-ink outline-none focus:outline-2 focus:outline-focus disabled:opacity-60",
                requestTooLong ? "border-danger" : "border-field")} />
            <IconButton type="submit" icon="arrow_forward" label={msg("assignments.components.AiReviseChat.sendRequest")} data-testid="ai-revise-send"
              disabled={composerBusy || !input.trim() || attachments.pending || requestTooLong} className="bg-primary text-on-primary hover:bg-primary-strong hover:text-on-primary" />
          </form>
          {input.length > 0 && <AiRequestLength id="revision-ai-request-length" length={input.length} />}
          <input ref={referenceInput} type="file" multiple accept=".docx,.pdf,.txt,.png,.jpg,.jpeg,.webp" hidden aria-label={msg("assignments.components.AiReviseChat.uploadAFileFromComputer")}
            data-testid="ai-revise-reference-file"
            onChange={event => { addFiles(Array.from(event.target.files ?? [])); event.target.value = ""; }} />
        </div>
      </section>
      {sourcePicker && <AiSourcePicker open hideUntilSelectionNeeded hideNotes requireConfirmation={sourcePicker === "choose"}
        nextChoice={nextSource} submitNextChoiceIfAvailable={sourcePicker === "send"}
        previousChoice={sourcePicker === "choose" ? previousSource : null}
        confirmLabel={sourcePicker === "choose" ? msg("assignments.components.AiReviseChat.useSourceForNextRequest") : undefined}
        task={msg("assignments.components.AiReviseChat.editAssignmentWithAi")}
        onCancel={() => setSourcePicker(null)} onSubmit={handleChooseSource} />}
      <Dialog open={attachments.replacement !== null} title={msg("assignments.components.AiReviseChat.replaceTemplate")} onClose={attachments.cancelReplacement}
        footer={<Button variant="primary" onClick={() => void attachments.confirmReplacement()}>{msg("assignments.components.AiReviseChat.replaceTemplate2")}</Button>}>
        <p>{msg("assignments.components.AiReviseChat.theNewTemplateReplacesThe")}</p>
      </Dialog>
      <button type="button" data-testid="ai-revise-toggle" aria-expanded={open} aria-controls="ai-revise-panel" onClick={toggle}
        aria-label={open ? msg("assignments.components.AiReviseChat.closeAiAssistant") : msg("assignments.components.AiReviseChat.editAssignmentWithAi")} title={open ? msg("assignments.components.AiReviseChat.closeAiAssistant") : msg("assignments.components.AiReviseChat.editAssignmentWithAi")}
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
      return { ...current, status: "FAILED", safeError: msg("assignments.components.AiReviseChat.theAiTookTooLong") };
    }
    await new Promise((resolve) => setTimeout(resolve, POLL_MS));
    current = await assignmentsApi.aiDraft(assignmentId, current.draftId);
  }
  return current;
}
