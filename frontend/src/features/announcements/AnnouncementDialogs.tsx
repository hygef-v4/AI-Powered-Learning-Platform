"use client";
import { useEffect, useId, useMemo, useRef, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { Button, ClassLabel, Dialog, Field, Icon, TextInput, cx, showToast } from "@/shared/ui";
import { announcementsApi, announcementError } from "./api";
import type { Announcement, ClassOption } from "./types";

const AREA = "min-h-36 w-full rounded-lg border border-field px-3 py-2 text-[13.5px] leading-relaxed text-ink outline-none focus:border-focus disabled:opacity-60";
const CHECK = "flex items-center gap-2 text-[13.5px] text-ink [&:has(input:disabled)]:text-muted";

/**
 * Popup Tạo thông báo. Trong một lớp (`fixedClassId`) đăng vào lớp đó; ở trang chung có ô Lớp ngay dưới Tiêu đề để chọn nhiều
 * lớp (tìm, chọn tất cả, danh sách cuộn nên nhiều lớp không vỡ). Đăng lần lượt từng lớp; lớp lỗi được giữ lại để thử lại.
 */
export function CreateAnnouncementDialog({ classes, fixedClassId, onClose, onCreated }: {
  classes: ClassOption[]; fixedClassId?: string; onClose: () => void; onCreated: () => void;
}) {
  const id = useId();
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [important, setImportant] = useState(false);
  const [email, setEmail] = useState(false);
  const [chosen, setChosen] = useState<string[]>(fixedClassId ? [fixedClassId] : []);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const ready = !!title.trim() && !!body.trim() && chosen.length > 0;

  async function submit() {
    setBusy(true);
    setError("");
    const input = { title: title.trim(), body: body.trim(), important, sendEmail: important && email };
    const failed: string[] = [];
    let message = "";
    for (const classId of chosen) {
      try { await announcementsApi.create(classId, input); }
      catch (e) { failed.push(classId); message = announcementError(e); }
    }
    setBusy(false);
    if (failed.length < chosen.length) onCreated();
    if (!failed.length) { showToast("Đã đăng thông báo"); onClose(); return; }
    setChosen(failed);
    const names = failed.map((classId) => classes.find((c) => c.classId === classId)?.code ?? classId).join(", ");
    setError(fixedClassId ? message : `Chưa đăng được ở ${names}. ${message}`);
  }

  return (
    <Dialog open title="Tạo thông báo" icon="campaign" onClose={onClose} dismissible={!busy} size="lg"
      footer={<Button variant="primary" onClick={() => void submit()} disabled={busy || !ready} data-testid="announcement-create-submit">Đăng</Button>}>
      <Field label="Tiêu đề" htmlFor={`${id}-title`} required>
        <TextInput id={`${id}-title`} maxLength={200} value={title} disabled={busy} autoFocus onChange={(e) => setTitle(e.target.value)} data-testid="announcement-create-title" />
      </Field>
      {!fixedClassId && <ClassPicker classes={classes} chosen={chosen} disabled={busy} onChange={setChosen} />}
      <Field label="Nội dung" htmlFor={`${id}-body`} required>
        <textarea id={`${id}-body`} maxLength={5000} value={body} disabled={busy} onChange={(e) => setBody(e.target.value)} className={AREA} data-testid="announcement-create-body" />
      </Field>
      <div className="flex flex-wrap gap-x-6 gap-y-2">
        <label className={CHECK}><input type="checkbox" className="size-4 accent-[var(--c-primary)]" checked={important} disabled={busy} data-testid="announcement-important"
          onChange={(e) => { setImportant(e.target.checked); if (!e.target.checked) setEmail(false); }} />Quan trọng</label>
        <label className={CHECK}><input type="checkbox" className="size-4 accent-[var(--c-primary)]" checked={email} disabled={!important || busy} data-testid="announcement-email"
          onChange={(e) => setEmail(e.target.checked)} />Gửi email</label>
      </div>
      {error && <p role="alert" className="text-[13px] text-danger">{error}</p>}
    </Dialog>
  );
}

/**
 * Ô chọn nhiều lớp cùng kiểu ô chọn giảng viên: gõ mã hoặc tên lớp, gợi ý nổi đè lên phần bên dưới (vẽ ở body, bám theo ô
 * nhập) nên popup không đổi bố cục dù có bao nhiêu lớp. Bấm một lớp thì tích / bỏ tích mà danh sách vẫn mở; đầu danh sách có
 * Chọn tất cả theo kết quả đang hiện. Ô không gõ thì chỉ ghi tóm tắt các lớp đã chọn.
 */
function ClassPicker({ classes, chosen, disabled, onChange }: { classes: ClassOption[]; chosen: string[]; disabled: boolean; onChange: (ids: string[]) => void }) {
  const id = useId();
  const anchorRef = useRef<HTMLDivElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [place, setPlace] = useState<{ top: number; left: number; width: number } | null>(null);
  useEffect(() => {
    if (!open) return;
    const update = () => {
      const rect = anchorRef.current?.getBoundingClientRect();
      if (rect) setPlace({ top: rect.bottom + 4, left: rect.left, width: rect.width });
    };
    const frame = requestAnimationFrame(update);
    window.addEventListener("resize", update);
    window.addEventListener("scroll", update, true);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("resize", update);
      window.removeEventListener("scroll", update, true);
    };
  }, [open]);
  const shown = useMemo(() => {
    const q = query.toLocaleLowerCase("vi").trim();
    return q ? classes.filter((c) => `${c.code} ${c.name}`.toLocaleLowerCase("vi").includes(q)) : classes;
  }, [classes, query]);
  const allShown = shown.length > 0 && shown.every((c) => chosen.includes(c.classId));
  const toggle = (classId: string) => onChange(chosen.includes(classId) ? chosen.filter((x) => x !== classId) : [...chosen, classId]);
  const toggleShown = () => onChange(allShown ? chosen.filter((x) => !shown.some((c) => c.classId === x))
    : [...chosen, ...shown.map((c) => c.classId).filter((x) => !chosen.includes(x))]);
  const codes = classes.filter((c) => chosen.includes(c.classId)).map((c) => c.code);
  const summary = !codes.length ? "" : codes.length === classes.length && classes.length > 1 ? `Tất cả lớp (${codes.length})`
    : codes.length <= 3 ? codes.join(", ") : `${codes.slice(0, 3).join(", ")} +${codes.length - 3}`;
  const close = () => { setOpen(false); setQuery(""); };
  const row = (key: string, checked: boolean, onToggle: () => void, content: ReactNode, strong = false) => (
    <button key={key} type="button" role="option" aria-selected={checked} onMouseDown={(event) => event.preventDefault()} onClick={onToggle}
      className={cx("flex w-full items-center gap-2.5 px-3 py-2 text-left text-[13px] text-ink hover:bg-shell", strong && "border-b border-line font-semibold")}>
      <span aria-hidden className={cx("flex size-4 shrink-0 items-center justify-center rounded border",
        checked ? "border-primary bg-primary text-on-primary" : "border-line-strong bg-surface")}>
        {checked && <Icon name="check" size={12} />}
      </span>
      {content}
    </button>
  );
  return (
    <div className="flex min-w-0 flex-col gap-1.5" data-testid="announcement-classes">
      <label htmlFor={`${id}-input`} className="text-[13px] font-semibold text-ink">Lớp <span className="text-danger">*</span></label>
      <div ref={anchorRef}>
        <TextInput id={`${id}-input`} icon="search" role="combobox" aria-expanded={open} aria-controls={`${id}-list`} aria-autocomplete="list" autoComplete="off"
          maxLength={100} disabled={disabled} placeholder={open ? "Gõ mã hoặc tên lớp" : "Chọn lớp"} data-testid="announcement-classes-input"
          value={open ? query : summary} title={codes.join(", ")}
          onFocus={() => setOpen(true)}
          onChange={(event) => { setQuery(event.target.value); setOpen(true); }}
          onKeyDown={(event) => { if (event.key === "Escape" && open) { event.stopPropagation(); close(); } }}
          onBlur={(event) => { if (!listRef.current?.contains(event.relatedTarget as Node | null)) close(); }}
          suffix={<span className="text-xs text-muted tabular-nums">{chosen.length}/{classes.length}</span>} />
      </div>
      {open && !disabled && typeof document !== "undefined" && createPortal(
        <div ref={listRef} id={`${id}-list`} role="listbox" aria-multiselectable aria-label="Lớp"
          style={place ? { top: place.top, left: place.left, width: place.width } : { visibility: "hidden" }}
          className="fixed z-[105] max-h-64 overflow-auto overscroll-contain rounded-lg border border-line bg-surface shadow-[var(--shadow-float)]">
          {shown.length > 1 && row("all", allShown, toggleShown, <span className="flex-1">{query ? "Chọn các lớp đang tìm" : "Chọn tất cả"}</span>, true)}
          {shown.map((c) => row(c.classId, chosen.includes(c.classId), () => toggle(c.classId),
            <ClassLabel code={c.code} name={c.name} />))}
          {!shown.length && <p className="px-3 py-2 text-xs text-muted">Không có lớp phù hợp</p>}
        </div>, document.body)}
    </div>
  );
}

export function AnnouncementEditorDialog({ item, onClose, onSaved }: { item: Announcement; onClose: () => void; onSaved: () => void }) {
  const id = useId();
  const [title, setTitle] = useState(item.title);
  const [body, setBody] = useState(item.body);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function save() {
    setBusy(true);
    setError("");
    try { await announcementsApi.edit(item.id, title.trim(), body.trim(), item.version); showToast("Đã lưu thay đổi"); onSaved(); onClose(); }
    catch (e) { setError(announcementError(e)); }
    finally { setBusy(false); }
  }
  return (
    <Dialog open title="Sửa thông báo" icon="edit" onClose={onClose} dismissible={!busy} size="lg"
      footer={<Button data-testid="announcement-edit-save" variant="primary" onClick={() => void save()} disabled={busy || !title.trim() || !body.trim()}>Lưu</Button>}>
      <Field label="Tiêu đề" htmlFor={`${id}-title`} required>
        <TextInput id={`${id}-title`} data-testid="announcement-edit-title" maxLength={200} value={title} disabled={busy} onChange={(e) => setTitle(e.target.value)} />
      </Field>
      <Field label="Nội dung" htmlFor={`${id}-body`} required>
        <textarea id={`${id}-body`} data-testid="announcement-edit-body" maxLength={5000} value={body} disabled={busy} onChange={(e) => setBody(e.target.value)} className={AREA} />
      </Field>
      {error && <p role="alert" className="text-[13px] text-danger">{error}</p>}
    </Dialog>
  );
}

export function DeleteAnnouncementDialog({ item, onClose, onSaved }: { item: Announcement; onClose: () => void; onSaved: () => void }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function remove() {
    setBusy(true);
    try { await announcementsApi.remove(item.id, item.version); showToast("Đã xóa thông báo", "danger"); onSaved(); onClose(); }
    catch (e) { setError(announcementError(e)); }
    finally { setBusy(false); }
  }
  return (
    <Dialog open title="Xóa thông báo?" onClose={onClose} dismissible={!busy} tone="danger"
      footer={<Button data-testid="announcement-delete-confirm" variant="danger" onClick={() => void remove()} disabled={busy}>Xóa</Button>}>
      <p>Bạn có chắc muốn xóa thông báo “{item.title}” không?</p>
      {error && <p role="alert" className="text-[13px] text-danger">{error}</p>}
    </Dialog>
  );
}
