"use client";

import { AiChatAttachments } from "./AiChatAttachments";
import { useAiChatAttachments } from "../hooks/useAiChatAttachments";
import { useEffect, useLayoutEffect, useRef, useState, type FormEvent, type KeyboardEvent, type ReactNode, type ClipboardEvent, type DragEvent } from "react";
import { Button, Dialog, Icon, IconButton, cx, showErrorToast, type IconName } from "@/shared/ui";
import { assignmentsApi } from "../api";
import { errorMessage, requestRef } from "../model/format";
import type { AiDraft, Material } from "../model/types";

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
  recover?: () => void;
};
type TemplateOption = { assignmentId: string; title: string; subjectTemplate: boolean };
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
  const [busy, setBusy] = useState(false);
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
    setOpen(value => !value);
    dragDepth.current = 0;
    setDragging(false);
    setMenu(null);
  }

  function showMenu(next: AttachMenu | null) {
    setQuery("");
    setMenu(next);
  }

  function say(message: Omit<Message, "id">) {
    setMessages((current) => [...current, { ...message, id: nextId.current++ }]);
  }

  const pickedTemplate = templates.find(item => item.assignmentId === templateId);
  const pickedMaterials = chosen.flatMap(id => materials.filter(item => item.materialId === id));
  const hasTemplate = Boolean(template || pickedTemplate);
  const hasReferences = pickedMaterials.length > 0 || sources.uploads.length > 0;

  async function send(event?: FormEvent) {
    event?.preventDefault();
    const text = input.trim();
    if (!text || submitting.current) return;
    if (attachments.pending) { showErrorToast("Hãy đợi tải xong hoặc gỡ các tệp bị lỗi trước khi gửi."); return; }
    const snapshot = attachments.snapshot();
    const materialIds = [...chosen];
    const templateName = template ? template.file.name : pickedTemplate?.title;
    const references = [...pickedMaterials.map(item => item.title), ...sources.uploads.map(item => item.file.name)];
    const payload = { formatText: template?.text, templateAssignmentId: template ? undefined : templateId || undefined,
      materialIds, sourceFileRefs: sources.fileRefs };
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
      const requested = hasTemplate || hasReferences
        ? await assignmentsApi.requestAiRevision(assignmentId, text, ref, payload)
        : await assignmentsApi.requestAiRevision(assignmentId, text, ref);
      if (!alive.current) return;
      accepted = true;
      pendingRequest.current = null;
      say({ role: "user", text, template: templateName, references });
      setInput("");
      attachments.clear();
      setChosen([]);
      draft = await waitForResult(assignmentId, requested, () => alive.current);
      if (!alive.current) return;
      if (draft.status !== "READY" || !draft.proposal) {
        say({ role: "ai", text: `${draft.safeError ?? "AI không trả kết quả."} Đề chưa thay đổi.`, tone: "danger", recover });
        return;
      }
      if (draft.proposal.unchanged) {
        say({ role: "ai", text: "AI chưa thay đổi gì. Hãy nói rõ hơn yêu cầu sửa đề.", recover });
        void assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
        return;
      }
      const validation = draft.proposal.validation;
      if (validation?.status === "INVALID") {
        say({ role: "ai", text: `Không áp dụng được: ${validation.issues.map(issue => issue.message).join(" ")}`, tone: "danger", recover });
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
      const shown = warnings.slice(0, MAX_WARNINGS).map(issue => `Lưu ý: ${issue.message}`);
      if (warnings.length > MAX_WARNINGS) shown.push(`…và ${warnings.length - MAX_WARNINGS} lưu ý khác.`);
      say({ role: "ai", text: ["Đã sửa đề theo yêu cầu. Nhấn Ctrl+Z để quay lại bản trước.", ...shown].join("\n") });
    } catch (error) {
      if (!alive.current) return;
      say({ role: "ai", text: applied ? "Đề đã được cập nhật nhưng chưa tải lại được. Vui lòng tải lại trang."
        : applying ? `${errorMessage(error)} Chưa xác nhận được kết quả áp dụng. Hãy tải lại đề để kiểm tra trước khi thử lại.`
        : `${errorMessage(error)} ${accepted ? "Đề chưa thay đổi." : "Yêu cầu chưa được xác nhận; nội dung và tệp vẫn được giữ để thử lại."}`,
        tone: "danger", recover: accepted && !applying ? recover : undefined });
      if (!applying && draft?.status === "READY") void assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
    } finally {
      submitting.current = false;
      if (alive.current) setBusy(false);
    }
  }

  function addFiles(files: File[]) {
    if (!busy && !attachments.processing) attachments.addFiles(files);
  }

  function pasteImages(event: ClipboardEvent<HTMLTextAreaElement>) {
    const files = Array.from(event.clipboardData.items ?? []).filter(item => item.kind === "file")
      .map(item => item.getAsFile()).filter((file): file is File => !!file && file.type.startsWith("image/"));
    if (!files.length) return;
    event.preventDefault();
    if (busy) return;
    if (attachments.processing) showErrorToast("Đợi chuyển vai trò tệp xong rồi dán ảnh.");
    else addFiles(files);
    const text = event.clipboardData.getData("text/plain");
    if (text) {
      const start = event.currentTarget.selectionStart;
      const end = event.currentTarget.selectionEnd;
      setInput(current => (current.slice(0, start) + text + current.slice(end)).slice(0, MAX_REQUEST));
    }
  }

  function dragFiles(event: DragEvent<HTMLElement>, entering: boolean) {
    if (!Array.from(event.dataTransfer.types).includes("Files")) return;
    event.preventDefault();
    if (busy || attachments.processing) return;
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
    if (hasDirectory) showErrorToast("Chọn tệp cụ thể, không kéo thả thư mục.");
    addFiles(files);
  }

  function closeChat() { setOpen(false); dragDepth.current = 0; setDragging(false); }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault();
      void send();
    }
    if (event.key === "Escape") closeChat();
  }

  const needle = query.trim().toLocaleLowerCase("vi");
  const matches = (title: string) => !needle || title.toLocaleLowerCase("vi").includes(needle);
  const templateChoices = templates.filter((item) => item.assignmentId !== templateId && matches(item.title));
  const materialChoices = materials.filter((item) => !chosen.includes(item.materialId) && matches(item.title));

  return (
    <>
      {/* Popup giữ nguyên khi đóng (chỉ ẩn) để cuộc trò chuyện và lượt AI đang chạy không mất. */}
      <section id="ai-revise-panel" aria-label="Sửa đề bằng AI" data-testid="ai-revise-chat" hidden={!open}
        onDragEnter={event => dragFiles(event, true)} onDragLeave={event => dragFiles(event, false)}
        onDragOver={event => { if (Array.from(event.dataTransfer.types).includes("Files")) { event.preventDefault(); event.dataTransfer.dropEffect = busy ? "none" : "copy"; } }}
        onDrop={dropFiles}
        className={cx("fixed right-4 bottom-20 z-40 w-[min(22rem,calc(100vw-2rem))] flex-col rounded-2xl border",
          "border-line bg-surface shadow-[var(--shadow-float)] h-[min(30rem,calc(100dvh-9.5rem))]",
          open && "flex animate-pop")}>
        {dragging && <div role="status" className="pointer-events-none absolute inset-0 z-20 flex items-center justify-center rounded-2xl border-2 border-dashed border-primary bg-surface/95 text-sm font-semibold text-primary-ink">Thả tệp để đính kèm</div>}
        <header className="flex shrink-0 items-center gap-2 border-b border-line py-2 pr-2 pl-3.5">
          <Icon name="smart_toy" size={17} className="text-primary" />
          <h2 className="flex-1 text-[13.5px] font-semibold text-ink">Sửa đề bằng AI</h2>
          <IconButton icon="close" label="Đóng trợ lý AI" size={28} onClick={closeChat} />
        </header>
        <div ref={list} role="log" aria-live="polite" className="flex min-h-0 flex-1 flex-col gap-1.5 overflow-y-auto px-3.5 py-3">
          {messages.map((message) => (
            <div key={message.id} className={cx("max-w-[90%] rounded-lg px-2.5 py-1.5 text-xs",
              message.role === "user" ? "self-end bg-primary text-on-primary"
                : message.tone === "danger" ? "self-start bg-danger-soft text-danger" : "self-start bg-sunken text-ink")}>
              <p className="whitespace-pre-line">{message.text}</p>
              {message.recover && <button type="button" disabled={busy || !!input || hasTemplate || hasReferences}
                onClick={message.recover} className="mt-1 text-[11px] font-semibold underline disabled:opacity-40">Khôi phục yêu cầu</button>}
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
          <AiChatAttachments attachments={attachments} templateName={template?.file.name ?? pickedTemplate?.title}
            materials={pickedMaterials} disabled={busy} onUnchoose={id => setChosen(current => current.filter(value => value !== id))} />
          <form onSubmit={(event) => void send(event)} className="flex items-end gap-1.5">
            <div ref={attach} className="relative shrink-0">
              <IconButton icon="add" label="Thêm mẫu đề hoặc tài liệu tham khảo" data-testid="ai-revise-attach" disabled={busy || attachments.processing}
                aria-haspopup="menu" aria-expanded={menu !== null} onClick={() => showMenu(menu ? null : "root")}
                className="border border-line-strong" />
              {menu && (
                <div role="menu" aria-label="Tệp gửi kèm" data-testid="ai-revise-attach-menu"
                  onKeyDown={(event) => { if (event.key === "Escape") { event.stopPropagation(); setMenu(null); } }}
                  className="absolute bottom-full left-0 z-10 mb-1.5 flex max-h-52 w-52 animate-pop flex-col overflow-hidden rounded-xl border border-line bg-surface py-1 shadow-[var(--shadow-float)]">
                  {menu === "root" ? (
                    <>
                      <MenuRow icon={TEMPLATE_ICON} onClick={() => showMenu("templates")}>Chọn đề mẫu có sẵn</MenuRow>
                      <MenuRow icon={REFERENCE_ICON} onClick={() => showMenu("materials")}>Chọn học liệu của môn</MenuRow>
                      <MenuRow icon="upload_file" disabled={!sources.canUpload} onClick={() => { setMenu(null); referenceInput.current?.click(); }}>Tải từ máy</MenuRow>
                    </>
                  ) : (
                    <>
                      <div className="flex shrink-0 items-center gap-1 border-b border-line px-1.5 pb-1">
                        <IconButton icon="arrow_back" label="Quay lại" size={26} onClick={() => showMenu("root")} />
                        <input autoFocus value={query} onChange={(event) => setQuery(event.target.value)}
                          aria-label={menu === "templates" ? "Tìm mẫu đề" : "Tìm tài liệu tham khảo"} placeholder="Tìm theo tên"
                          className="h-7 min-w-0 flex-1 bg-transparent text-xs text-ink outline-none placeholder:text-muted" />
                      </div>
                      {/* Danh sách chỉ chứa nguồn có sẵn; tải từ máy nằm ở menu gốc. */}
                      <div className="flex min-h-0 flex-col overflow-y-auto">
                        {menu === "templates" && templateChoices.map((item) => (
                          <MenuRow key={item.assignmentId} icon={TEMPLATE_ICON}
                            onClick={() => { attachments.chooseExisting(item.assignmentId); setMenu(null); }}>{item.title}</MenuRow>
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
              value={input} disabled={busy} onPaste={pasteImages} onChange={(event) => setInput(event.target.value)} onKeyDown={handleKeyDown}
              placeholder="Ví dụ: dịch toàn bộ đề sang tiếng Anh"
              className="min-h-8 flex-1 resize-none rounded-lg border border-field bg-surface px-2.5 py-1.5 text-xs leading-4 text-ink outline-none focus:outline-2 focus:outline-focus disabled:opacity-60" />
            <IconButton type="submit" icon="arrow_forward" label="Gửi yêu cầu" data-testid="ai-revise-send"
              disabled={busy || !input.trim() || attachments.pending} className="bg-primary text-on-primary hover:bg-primary-strong hover:text-on-primary" />
          </form>
          <input ref={referenceInput} type="file" multiple accept=".docx,.pdf,.txt,.png,.jpg,.jpeg" hidden aria-label="Tải tệp từ máy"
            data-testid="ai-revise-reference-file"
            onChange={event => { addFiles(Array.from(event.target.files ?? [])); event.target.value = ""; }} />
        </div>
      </section>
      <Dialog open={attachments.replacement !== null} title="Thay mẫu đề?" onClose={attachments.cancelReplacement}
        footer={<Button variant="primary" onClick={() => void attachments.confirmReplacement()}>Thay mẫu đề</Button>}>
        <p>Mẫu mới sẽ thay mẫu đang chọn sau khi đọc thành công. Các tài liệu tham khảo khác được giữ nguyên.</p>
      </Dialog>
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
