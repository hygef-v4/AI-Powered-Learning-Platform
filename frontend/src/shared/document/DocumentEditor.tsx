"use client";

import {
  Fragment,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
  type TextareaHTMLAttributes,
} from "react";
import { Icon, cx, type IconName } from "@/shared/ui";
import { CommentRail, useWideLayout, type DocumentComment } from "./CommentRail";
import { DiagramEditor } from "./DiagramEditor";
import { BlockView, HEADING_CLASS, PARAGRAPH_CLASS, alignClass, runClass, runStyle, type ImageUrl } from "./DocumentViewer";
import {
  blockText,
  computeParts,
  diagramTypeLabels,
  documentTree,
  headingLabels,
  headingLevel,
  newBlock,
  newId,
  runsText,
  sectionsOf,
  type Align,
  type BlockOrigin,
  type DiagramType,
  type DocSection,
  type DocumentBlock,
  type DocumentModel,
  type HeadingStyle,
  type ListItem,
  type Run,
  type TableCell,
  type ValidationIssue,
} from "./types";

type Mode = "author" | "student";

type Props = {
  document: DocumentModel;
  onChange: (next: DocumentModel) => void;
  /** author: người soạn khung; student: người làm bài, block của người soạn bị khóa. */
  mode: Mode;
  diagramEnabled: boolean;
  readOnly?: boolean;
  imageUrl?: ImageUrl;
  /** Tải ảnh qua U03 ({@code DOCUMENT_IMAGE}), trả {@code fileRef} để gắn khi lưu. */
  uploadImage?: (file: File) => Promise<string>;
  issues?: ValidationIssue[];
  /** Phần đầu trang giấy, trên các block (tên bài, hướng dẫn). */
  header?: ReactNode;
  /** Nút thêm ở cuối thanh công cụ; có nút thì thanh công cụ vẫn hiện khi chỉ đọc. */
  toolbarExtra?: ReactNode;
  /** Ghi chú bên lề gắn với block (rubric của từng mục), kiểu comment Google Docs. */
  comments?: DocumentComment[];
  activeComment?: string | null;
  /** Tiêu đề của phần đang chọn: viền phần đó. */
  highlightBlockId?: string | null;
  onFocusBlock?: (blockId: string) => void;
  /** Chế độ người học, chỉ đọc: chữ hiện ở phần chưa có bài làm; không có thì để trống. */
  emptyLeafLabel?: string;
};

type StyleValue = "P" | "UL" | "OL" | HeadingStyle;
type Flag = "bold" | "italic" | "underline" | "strike" | "code";
type Flags = Omit<Run, "text">;
/** Nơi đặt con trỏ sau khi tài liệu đổi: ô nhập đầu, cuối, hoặc ô thứ {@code field} của block. */
type FocusTarget = { id: string; at: "start" | "end"; field?: number };
type CellFocus = { blockId: string; row: number; col: number };

const STYLE_LABELS: Record<StyleValue, string> = { P: "Văn bản thường", UL: "Danh sách chấm", OL: "Danh sách số", ...headingLabels };
const AUTHOR_STYLES: StyleValue[] = ["P", "TITLE", "SUBTITLE", "H1", "H2", "H3", "H4", "H5", "H6"];

const FLAGS: { key: Flag; label: string; title: string; className: string }[] = [
  { key: "bold", label: "B", title: "Đậm (Ctrl+B)", className: "font-bold" },
  { key: "italic", label: "I", title: "Nghiêng (Ctrl+I)", className: "font-serif italic" },
  { key: "underline", label: "U", title: "Gạch chân (Ctrl+U)", className: "underline" },
  { key: "strike", label: "S", title: "Gạch ngang", className: "line-through" },
  { key: "code", label: "</>", title: "Mã", className: "font-mono text-[11px]" },
];

const TEXT_COLORS = ["#202124", "#5F6368", "#C5221F", "#E37400", "#188038", "#1A73E8", "#9334E6", "#B80672"];
const HIGHLIGHTS = ["#FFF475", "#CCFF90", "#A7FFEB", "#AECBFA", "#D7AEFB", "#FDCFE8", "#E8EAED"];
/** Nền của nhánh theo độ sâu (nhánh con đậm hơn nhánh cha); phần lá nền trắng có viền. */
const BRANCH_BG = ["#eef2f8", "#e1e8f3", "#d4ddee", "#c8d3e8"];
const ALIGNS: { value: Align; title: string; glyph: GlyphName }[] = [
  { value: "LEFT", title: "Căn trái", glyph: "alignLeft" },
  { value: "CENTER", title: "Căn giữa", glyph: "alignCenter" },
  { value: "RIGHT", title: "Căn phải", glyph: "alignRight" },
  { value: "JUSTIFY", title: "Căn đều", glyph: "justify" },
];
const IMAGE_TYPES = ".png,.jpg,.jpeg,.gif,.svg";
const HISTORY_GAP_MS = 700;

function flagsOf(run: Run | undefined): Flags {
  const { text: _text, ...rest } = run ?? { text: "" };
  void _text;
  return rest;
}

function runsOf(block: DocumentBlock | undefined): Run[] {
  if (block?.type === "PARAGRAPH") return block.content.runs;
  if (block?.type === "LIST") return block.content.items.flatMap((item) => item.runs);
  return [];
}

function styleOf(block: DocumentBlock | undefined): StyleValue | "" {
  if (block?.type === "PARAGRAPH") return "P";
  if (block?.type === "HEADING") return block.content.style;
  if (block?.type === "LIST") return block.content.ordered ? "OL" : "UL";
  return "";
}

function alignOf(block: DocumentBlock | undefined): Align | undefined {
  return block?.type === "PARAGRAPH" || block?.type === "HEADING" || block?.type === "LIST" ? block.content.align : undefined;
}

function paragraph(origin: BlockOrigin, text: string, flags: Flags = {}): DocumentBlock {
  return { id: newId(), origin, type: "PARAGRAPH", content: { runs: [{ ...flags, text }] } };
}

/** Đổi kiểu block chữ (đoạn, tiêu đề, danh sách), giữ id, nguồn, chữ, định dạng và căn lề. */
export function convertBlock(block: DocumentBlock, style: StyleValue): DocumentBlock {
  const { id, origin } = block;
  const flags = flagsOf(runsOf(block)[0]);
  const align = alignOf(block);
  const keep = align ? { align } : {};
  const text = block.type === "LIST" ? block.content.items.map((item) => runsText(item.runs)).join("\n") : blockText(block);
  if (style === "P") return { id, origin, type: "PARAGRAPH", content: { runs: [{ ...flags, text }], ...keep } };
  if (style === "UL" || style === "OL") {
    if (block.type === "LIST") return { ...block, content: { ...block.content, ordered: style === "OL" } };
    return { id, origin, type: "LIST", content: { ordered: style === "OL", items: text.split("\n").map((line) => ({ level: 0, runs: [{ ...flags, text: line }] })), ...keep } };
  }
  return { id, origin, type: "HEADING", content: { style, text: text.replace(/\s*\n\s*/g, " ").slice(0, 500), ...keep } };
}

function mapRuns(block: DocumentBlock, change: (run: Run) => Run): DocumentBlock {
  if (block.type === "PARAGRAPH") return { ...block, content: { ...block.content, runs: block.content.runs.map(change) } };
  if (block.type === "LIST") return { ...block, content: { ...block.content, items: block.content.items.map((item) => ({ ...item, runs: item.runs.map(change) })) } };
  return block;
}

/** Bật hoặc tắt một định dạng cho cả đoạn hoặc cả danh sách. */
export function toggleFlag(block: DocumentBlock, flag: Flag): DocumentBlock {
  const runs = runsOf(block);
  const active = runs.length > 0 && runs.every((run) => run[flag]);
  return mapRuns(block, (run) => ({ ...run, [flag]: !active || undefined }));
}

/** Đặt màu chữ hoặc màu tô nền cho cả đoạn; {@code undefined} là bỏ màu. */
export function setRunColor(block: DocumentBlock, key: "color" | "highlight", value: string | undefined): DocumentBlock {
  return mapRuns(block, (run) => ({ ...run, [key]: value }));
}

export function setAlign(block: DocumentBlock, align: Align): DocumentBlock {
  const value = align === "LEFT" ? undefined : align;
  if (block.type === "PARAGRAPH" || block.type === "HEADING" || block.type === "LIST") return { ...block, content: { ...block.content, align: value } } as DocumentBlock;
  return block;
}

/** Sửa chữ của các run: giữ định dạng của run đầu, gộp thành một run. */
function textRuns(runs: Run[], text: string): Run[] {
  return [{ ...(runs[0] ?? { text: "" }), text }];
}

function focusField(root: HTMLElement | null, target: FocusTarget) {
  const block = root?.querySelector<HTMLElement>(`[data-block-id="${target.id}"]`);
  if (!block) return;
  const fields = block.querySelectorAll<HTMLTextAreaElement>("[data-editable]");
  const field = fields[target.field ?? (target.at === "end" ? fields.length - 1 : 0)];
  if (!field) {
    block.focus();
    return;
  }
  field.focus();
  const position = target.at === "start" ? 0 : field.value.length;
  field.setSelectionRange(position, position);
}

/** Phím kiểu trình soạn văn bản: Enter tách đoạn, Backspace ở đoạn trống thì xóa, mũi tên ở mép thì sang block bên cạnh. */
function textKeys(event: KeyboardEvent<HTMLTextAreaElement>, handlers: {
  enter?: (before: string, after: string) => void;
  removeEmpty?: () => void;
  step?: (direction: -1 | 1) => void;
}) {
  if (event.nativeEvent.isComposing) return;
  const field = event.currentTarget;
  const value = field.value;
  if (event.key === "Enter" && !event.shiftKey && handlers.enter) {
    event.preventDefault();
    handlers.enter(value.slice(0, field.selectionStart), value.slice(field.selectionEnd));
  } else if (event.key === "Backspace" && value === "" && handlers.removeEmpty) {
    event.preventDefault();
    handlers.removeEmpty();
  } else if (event.key === "ArrowUp" && field.selectionStart === 0 && field.selectionEnd === 0 && handlers.step) {
    event.preventDefault();
    handlers.step(-1);
  } else if (event.key === "ArrowDown" && field.selectionStart === value.length && handlers.step) {
    event.preventDefault();
    handlers.step(1);
  }
}

/** Ô chữ không viền, tự cao theo nội dung, giống một dòng trên trang giấy. */
export function AutoTextarea({ className, value, ...rest }: Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, "value"> & { value: string }) {
  const ref = useRef<HTMLTextAreaElement>(null);
  useLayoutEffect(() => {
    const field = ref.current;
    if (field) fit(field);
  }, [value]);
  useEffect(() => {
    const field = ref.current;
    if (!field || typeof ResizeObserver === "undefined") return;
    let width = field.clientWidth;
    const observer = new ResizeObserver(() => {
      if (field.clientWidth === width) return;
      width = field.clientWidth;
      fit(field);
    });
    observer.observe(field);
    return () => observer.disconnect();
  }, []);
  return (
    <textarea ref={ref} rows={1} value={value} data-editable
      className={cx("block w-full resize-none overflow-hidden border-0 bg-transparent p-0 outline-none placeholder:text-faint", className)}
      {...rest} />
  );
}

function fit(field: HTMLTextAreaElement) {
  // Thu về auto để đo lại chiều cao; giữ vị trí cuộn vì trang có thể ngắn lại trong chốc lát.
  const y = window.scrollY;
  field.style.height = "auto";
  field.style.height = `${field.scrollHeight}px`;
  if (window.scrollY !== y) window.scrollTo({ top: y });
}

/** Nút trên thanh công cụ của trang soạn; giữ con trỏ trong ô đang gõ khi bấm. */
export function ToolbarButton({ icon, active, className, children, ...rest }: ButtonHTMLAttributes<HTMLButtonElement> & {
  icon?: IconName;
  active?: boolean;
}) {
  return (
    <button type="button" aria-pressed={active} onMouseDown={(event) => event.preventDefault()}
      className={cx("inline-flex h-7 min-w-7 items-center justify-center gap-1 rounded-md px-1.5 text-[13px] font-medium whitespace-nowrap text-body hover:bg-black/5 disabled:opacity-35 disabled:hover:bg-transparent",
        active && "bg-[#d3e3fd] text-[#041e49] hover:bg-[#d3e3fd]", className)}
      {...rest}>
      {icon && <Icon name={icon} size={18} />}
      {children}
    </button>
  );
}

function Separator() {
  return <span aria-hidden className="mx-1 h-5 w-px bg-[#c7c7c7]" />;
}

type GlyphName = "undo" | "redo" | "alignLeft" | "alignCenter" | "alignRight" | "justify" | "bullets" | "numbers" | "indent" | "outdent"
  | "image" | "table" | "diagram" | "highlight";

const GLYPHS: Record<GlyphName, ReactNode> = {
  undo: <><path d="M9 14 4 9l5-5" /><path d="M4 9h10.5a5.5 5.5 0 0 1 0 11H11" /></>,
  redo: <><path d="m15 14 5-5-5-5" /><path d="M20 9H9.5a5.5 5.5 0 0 0 0 11H13" /></>,
  alignLeft: <><path d="M4 6h16M4 10h10M4 14h16M4 18h10" /></>,
  alignCenter: <><path d="M4 6h16M7 10h10M4 14h16M7 18h10" /></>,
  alignRight: <><path d="M4 6h16M10 10h10M4 14h16M10 18h10" /></>,
  justify: <><path d="M4 6h16M4 10h16M4 14h16M4 18h16" /></>,
  bullets: <><path d="M10 7h10M10 12h10M10 17h10" /><circle cx="5" cy="7" r="1" /><circle cx="5" cy="12" r="1" /><circle cx="5" cy="17" r="1" /></>,
  numbers: <><path d="M10 7h10M10 12h10M10 17h10M4 5h1.5v4M4 9h3M4 15.5c.5-.8 2.5-.8 2.5.5S4 17.5 4 19h3" /></>,
  indent: <><path d="M4 6h16M11 10h9M11 14h9M4 18h16" /><path d="m4 9.5 3 2.5-3 2.5" /></>,
  outdent: <><path d="M4 6h16M11 10h9M11 14h9M4 18h16" /><path d="m7 9.5-3 2.5 3 2.5" /></>,
  image: <><rect x="3" y="4" width="18" height="16" rx="2" /><circle cx="8.5" cy="9.5" r="1.5" /><path d="m21 16-5-5L6 20" /></>,
  table: <><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M3 10h18M3 15h18M9 4v16M15 4v16" /></>,
  diagram: <><rect x="3" y="3" width="7" height="6" rx="1" /><rect x="14" y="15" width="7" height="6" rx="1" /><path d="M6.5 9v4.5a1.5 1.5 0 0 0 1.5 1.5h6" /></>,
  highlight: <><path d="m9 11-6 6v3h6l3-3" /><path d="m22 12-4.6 4.6a2 2 0 0 1-2.8 0l-5.2-5.2a2 2 0 0 1 0-2.8L14 4" /></>,
};

function Glyph({ name }: { name: GlyphName }) {
  return (
    <svg aria-hidden viewBox="0 0 24 24" width={18} height={18} fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
      {GLYPHS[name]}
    </svg>
  );
}

/** Ô bật ra dưới nút trên thanh công cụ (bảng màu, chọn kích thước bảng, loại sơ đồ); bấm ra ngoài thì đóng. */
function ToolPopover({ title, label, disabled, children }: {
  title: string;
  label: ReactNode;
  disabled?: boolean;
  children: (close: () => void) => ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const root = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const close = (event: MouseEvent) => { if (!root.current?.contains(event.target as Node)) setOpen(false); };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, [open]);
  return (
    <div ref={root} className="relative">
      <ToolbarButton title={title} aria-label={title} aria-expanded={open} disabled={disabled} active={open} onClick={() => setOpen((value) => !value)}>
        {label}<Icon name="expand_more" size={14} className="-ml-0.5 text-muted" />
      </ToolbarButton>
      {open && (
        <div className="absolute top-8 left-0 z-40 rounded-xl border border-line bg-white p-2 shadow-lg" onMouseDown={(event) => event.preventDefault()}>
          {children(() => setOpen(false))}
        </div>
      )}
    </div>
  );
}

function Swatches({ colors, onPick, noneLabel }: { colors: string[]; onPick: (color: string | undefined) => void; noneLabel: string }) {
  return (
    <div className="flex w-44 flex-col gap-1.5">
      <button type="button" onClick={() => onPick(undefined)} className="rounded-md px-2 py-1 text-left text-xs text-body hover:bg-shell">{noneLabel}</button>
      <div className="grid grid-cols-8 gap-1">
        {colors.map((color) => (
          <button key={color} type="button" title={color} aria-label={color} onClick={() => onPick(color)}
            className="size-4 rounded-full border border-black/10 hover:scale-125" style={{ backgroundColor: color }} />
        ))}
      </div>
    </div>
  );
}

/** Chọn kích thước bảng bằng lưới như Google Docs. */
function TablePicker({ onPick }: { onPick: (rows: number, cols: number) => void }) {
  const [hover, setHover] = useState({ rows: 0, cols: 0 });
  return (
    <div className="flex flex-col gap-1">
      <div className="grid grid-cols-10 gap-0.5" onMouseLeave={() => setHover({ rows: 0, cols: 0 })}>
        {Array.from({ length: 80 }, (_, i) => {
          const row = Math.floor(i / 10) + 1;
          const col = (i % 10) + 1;
          const on = row <= hover.rows && col <= hover.cols;
          return (
            <button key={i} type="button" aria-label={`${row} × ${col}`} onMouseEnter={() => setHover({ rows: row, cols: col })}
              onClick={() => onPick(row, col)}
              className={cx("size-4 border", on ? "border-primary bg-primary-soft" : "border-line-strong bg-white")} />
          );
        })}
      </div>
      <p className="text-center text-xs text-body">{hover.rows > 0 ? `${hover.rows} × ${hover.cols}` : "Bảng"}</p>
    </div>
  );
}

/** Nút "⋮" ở lề trái khi rê chuột vào block: lên, xuống, xóa. */
function BlockMenu({ onUp, onDown, onRemove }: { onUp?: () => void; onDown?: () => void; onRemove: () => void }) {
  const [open, setOpen] = useState(false);
  return (
    <div className="absolute top-0.5 right-full mr-1" onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget as Node)) setOpen(false); }}>
      <button type="button" aria-label="Thao tác khối" title="Thao tác khối" onClick={() => setOpen((value) => !value)}
        className={cx("flex h-6 w-5 items-center justify-center rounded text-muted hover:bg-black/5 hover:text-ink",
          open ? "opacity-100" : "opacity-0 group-hover/block:opacity-100 group-focus-within/block:opacity-100")}>
        ⋮
      </button>
      {open && (
        <div role="menu" className="absolute top-6 left-0 z-30 flex min-w-24 flex-col rounded-lg border border-line bg-white p-1 text-xs shadow-lg">
          <button type="button" role="menuitem" disabled={!onUp} onClick={() => { onUp?.(); setOpen(false); }} className="rounded px-2 py-1 text-left hover:bg-shell disabled:opacity-40">Lên</button>
          <button type="button" role="menuitem" disabled={!onDown} onClick={() => { onDown?.(); setOpen(false); }} className="rounded px-2 py-1 text-left hover:bg-shell disabled:opacity-40">Xuống</button>
          <button type="button" role="menuitem" aria-label="Xóa khối" onClick={() => { onRemove(); setOpen(false); }} className="rounded px-2 py-1 text-left text-danger hover:bg-danger-soft">Xóa</button>
        </div>
      )}
    </div>
  );
}

/**
 * Trình soạn tài liệu dùng chung U07, U08, U12 (BR-U07-10, 15) theo kiểu Google Docs: trang giấy, thanh công cụ dính
 * trên đầu, ghi chú (rubric) ở lề phải dính theo phần. Các phần hiện theo cây tiêu đề: nhánh tô nền đậm dần theo độ
 * sâu, phần lá nền trắng có viền (chỗ viết bài). Người học (yêu cầu 2026-10-21) chỉ viết trong phần lá, không thêm tiêu
 * đề, không sửa chữ của người soạn (chữ nhạt, không bấm sửa được); sơ đồ của người soạn trong phần thì bấm vào để vẽ.
 * Định dạng chữ áp cho cả đoạn.
 */
export function DocumentEditor({
  document, onChange, mode, diagramEnabled, readOnly = false, imageUrl, uploadImage, issues = [],
  header, toolbarExtra, comments, activeComment, highlightBlockId, onFocusBlock, emptyLeafLabel,
}: Props) {
  const origin: BlockOrigin = mode === "author" ? "TEACHER" : "STUDENT";
  const [previews, setPreviews] = useState<Record<string, string>>({});
  const [diagram, setDiagram] = useState<string | null>(null);
  const [uploadError, setUploadError] = useState<Record<string, string>>({});
  const [focusedId, setFocusedId] = useState<string | null>(null);
  const [cell, setCell] = useState<CellFocus | null>(null);
  const [history, setHistory] = useState({ past: 0, future: 0 });
  const paper = useRef<HTMLDivElement>(null);
  const content = useRef<HTMLDivElement>(null);
  const pendingFocus = useRef<FocusTarget | null>(null);
  const latest = useRef(document);
  const past = useRef<DocumentModel[]>([]);
  const future = useRef<DocumentModel[]>([]);
  const lastPush = useRef(0);
  const wide = useWideLayout();
  const previewsRef = useRef(previews);
  useEffect(() => {
    previewsRef.current = previews;
  }, [previews]);
  useEffect(() => {
    latest.current = document;
  }, [document]);
  useEffect(() => () => Object.values(previewsRef.current).forEach((url) => URL.revokeObjectURL(url)), []);

  useEffect(() => {
    const target = pendingFocus.current;
    if (!target) return;
    pendingFocus.current = null;
    focusField(paper.current, target);
  });

  const blocks = document.blocks;
  const student = mode === "student";
  // Người học chỉ viết trong phần: block nằm từ tiêu đề lá tới tiêu đề kế; khung không có tiêu đề thì cả bài.
  const sections = student ? sectionsOf(document, computeParts(document)) : null;
  const inPart = (blockId: string | undefined) => !sections || (!!blockId && sections.has(blockId));
  // Tài liệu trống không có tiêu đề nên cả bài là một phần; còn lại cuối tài liệu luôn thuộc tiêu đề cuối (là lá).
  const atEndInPart = blocks.length === 0 || inPart(blocks.at(-1)!.id);
  const locked = (block: DocumentBlock) => readOnly || (student && block.origin === "TEACHER");
  const focused = blocks.find((b) => b.id === focusedId);
  const insertAllowed = focused ? inPart(focused.id) : atEndInPart;
  const target = focused && !locked(focused) ? focused : undefined;
  const style = styleOf(target);
  const textTarget = target?.type === "PARAGRAPH" || target?.type === "LIST";
  const targetRuns = runsOf(target);
  const tableTarget = target?.type === "TABLE" && cell?.blockId === target.id ? target : undefined;
  const indexOf = new Map(blocks.map((block, index) => [block.id, index]));

  function update(next: DocumentBlock[], focus?: FocusTarget) {
    const now = Date.now();
    if (now - lastPush.current > HISTORY_GAP_MS) {
      past.current = [...past.current.slice(-99), document];
      future.current = [];
      setHistory({ past: past.current.length, future: 0 });
    }
    lastPush.current = now;
    if (focus) pendingFocus.current = focus;
    onChange({ blocks: next });
  }
  function undo() {
    const previous = past.current.at(-1);
    if (!previous) return;
    past.current = past.current.slice(0, -1);
    future.current = [...future.current, document];
    lastPush.current = 0;
    setHistory({ past: past.current.length, future: future.current.length });
    onChange(previous);
  }
  function redo() {
    const next = future.current.at(-1);
    if (!next) return;
    future.current = future.current.slice(0, -1);
    past.current = [...past.current, document];
    lastPush.current = 0;
    setHistory({ past: past.current.length, future: future.current.length });
    onChange(next);
  }
  function replace(id: string, block: DocumentBlock, focus?: FocusTarget) {
    update(blocks.map((b) => (b.id === id ? block : b)), focus);
  }
  function insertAfter(afterId: string | null, block: DocumentBlock) {
    const index = afterId ? blocks.findIndex((b) => b.id === afterId) : -1;
    const at = index < 0 ? blocks.length : index + 1;
    setFocusedId(block.id);
    update([...blocks.slice(0, at), block, ...blocks.slice(at)], { id: block.id, at: "start" });
  }
  function split(current: DocumentBlock, next: DocumentBlock) {
    const index = blocks.findIndex((b) => b.id === current.id);
    setFocusedId(next.id);
    update([...blocks.slice(0, index), current, next, ...blocks.slice(index + 1)], { id: next.id, at: "start" });
  }
  /** Block chữ gần nhất theo hướng {@code direction} mà người dùng sửa được. */
  function neighbour(list: DocumentBlock[], from: number, direction: -1 | 1): DocumentBlock | undefined {
    for (let i = from; i >= 0 && i < list.length; i += direction) {
      const block = list[i]!;
      if (!locked(block) && block.type !== "IMAGE" && block.type !== "DIAGRAM") return block;
    }
    return undefined;
  }
  function remove(id: string, focusPrevious = false) {
    const index = blocks.findIndex((b) => b.id === id);
    const next = blocks.filter((b) => b.id !== id);
    const previous = focusPrevious ? neighbour(next, index - 1, -1) ?? neighbour(next, index, 1) : undefined;
    update(next, previous ? { id: previous.id, at: index > 0 ? "end" : "start" } : undefined);
  }
  /** Người học chỉ đổi chỗ block trong cùng phần, không vượt qua tiêu đề của phần. */
  function canMove(index: number, delta: -1 | 1): boolean {
    const current = blocks[index];
    const other = blocks[index + delta];
    if (!current || !other) return false;
    if (!sections) return true;
    const section = sections.get(current.id);
    return !!section && sections.get(other.id) === section && headingLevel(other) === 0;
  }
  function move(index: number, delta: -1 | 1) {
    const current = blocks[index];
    const swapped = blocks[index + delta];
    if (!current || !swapped || !canMove(index, delta)) return;
    const next = [...blocks];
    next[index] = swapped;
    next[index + delta] = current;
    update(next);
  }
  function step(id: string, direction: -1 | 1) {
    const index = blocks.findIndex((b) => b.id === id);
    const next = neighbour(blocks, index + direction, direction);
    if (next) focusField(paper.current, { id: next.id, at: direction < 0 ? "end" : "start" });
  }
  function focusBlock(id: string) {
    if (id !== focusedId) setFocusedId(id);
    onFocusBlock?.(id);
  }
  /** Viết tiếp ở cuối một phần: đoạn trống cuối cùng của mình thì đặt con trỏ vào, không thì thêm đoạn mới. */
  function continueAfter(lastId: string) {
    const last = blocks.find((b) => b.id === lastId);
    if (last && last.type === "PARAGRAPH" && !locked(last) && !runsText(last.content.runs)) focusField(paper.current, { id: last.id, at: "end" });
    else insertAfter(lastId, newBlock("PARAGRAPH", origin));
  }
  function continueAtEnd() {
    if (!atEndInPart) return;
    const last = blocks.at(-1);
    if (last) continueAfter(last.id);
    else insertAfter(null, newBlock("PARAGRAPH", origin));
  }
  /** Chèn sau block đang chọn (hoặc cuối bài); người học chỉ chèn trong phần. */
  function insert(block: DocumentBlock) {
    if (!insertAllowed) return;
    insertAfter(focused?.id ?? null, block);
  }
  async function upload(blockId: string, file: File) {
    if (!uploadImage) return;
    setUploadError((current) => ({ ...current, [blockId]: "" }));
    try {
      const fileRef = await uploadImage(file);
      const url = URL.createObjectURL(file);
      setPreviews((current) => {
        const previous = current[blockId];
        if (previous) URL.revokeObjectURL(previous);
        return { ...current, [blockId]: url };
      });
      const now = latest.current;
      onChange({ blocks: now.blocks.map((b) => (b.id === blockId && b.type === "IMAGE" ? { ...b, content: { ...b.content, fileRef, hasFile: false } } : b)) });
    } catch (error) {
      setUploadError((current) => ({ ...current, [blockId]: error instanceof Error ? error.message : "Tải ảnh không thành công" }));
    }
  }
  function insertImage(file: File) {
    if (!insertAllowed) return;
    const block = newBlock("IMAGE", origin);
    insert(block);
    void upload(block.id, file);
  }
  function insertTable(rows: number, cols: number) {
    const empty = (): TableCell => ({ runs: [{ text: "" }] });
    insert({ id: newId(), origin, type: "TABLE", content: { rows: Array.from({ length: rows }, () => Array.from({ length: cols }, empty)) } });
  }
  function insertDiagram(type: DiagramType) {
    const block = newBlock("DIAGRAM", origin);
    if (block.type !== "DIAGRAM") return;
    insert({ ...block, content: { ...block.content, diagramType: type } });
    setDiagram(block.id);
  }
  /** Thụt lề mục danh sách đang gõ (Tab, Shift+Tab làm tương tự trong ô). */
  function indentList(delta: -1 | 1) {
    if (target?.type !== "LIST") return;
    const fields = Array.from(paper.current?.querySelectorAll<HTMLTextAreaElement>(`[data-block-id="${target.id}"] [data-editable]`) ?? []);
    const index = Math.max(0, fields.findIndex((field) => field === globalThis.document.activeElement));
    replace(target.id, { ...target, content: { ...target.content, items: target.content.items.map((item, i) => (i === index
      ? { ...item, level: Math.min(4, Math.max(0, (item.level ?? 0) + delta)) } : item)) } });
  }
  function tableChange(rows: TableCell[][]) {
    if (tableTarget) replace(tableTarget.id, { ...tableTarget, content: { ...tableTarget.content, rows } });
  }

  const visible = (comments ?? []).filter((c) => !c.anchorBlockId || blocks.some((b) => b.id === c.anchorBlockId));
  const rail = wide && visible.length > 0;
  const inlineComments = (anchor: string | null) => rail ? null : visible.filter((c) => c.anchorBlockId === anchor).map((c) => (
    <div key={c.key} className="my-2" data-comment={c.key}>{c.content}</div>
  ));
  const editing = blocks.find((b) => b.id === diagram && b.type === "DIAGRAM");

  function renderBlock(block: DocumentBlock): ReactNode {
    const index = indexOf.get(block.id) ?? 0;
    const isLocked = locked(block);
    const teacherBlock = student && block.origin === "TEACHER";
    const drawable = !readOnly && teacherBlock && block.type === "DIAGRAM" && inPart(block.id);
    const muted = teacherBlock && !drawable && block.type !== "HEADING";
    const blockIssues = issues.filter((issue) => issue.blockId === block.id);
    return (
      <div key={block.id} data-testid={`block-${block.id}`} data-block-id={block.id} tabIndex={-1}
        title={muted && !readOnly ? "Đề của giảng viên, không sửa được" : undefined}
        onFocusCapture={() => focusBlock(block.id)} onMouseDownCapture={() => focusBlock(block.id)}
        className={cx("group/block relative -mx-1 rounded px-1 py-0.5 outline-none",
          muted && "opacity-70", muted && !readOnly && "cursor-not-allowed",
          blockIssues.length > 0 && "shadow-[inset_3px_0_0_var(--color-danger)]")}>
        {!readOnly && !isLocked && (
          <BlockMenu onUp={canMove(index, -1) ? () => move(index, -1) : undefined}
            onDown={canMove(index, 1) ? () => move(index, 1) : undefined} onRemove={() => remove(block.id, true)} />
        )}
        {isLocked ? (
          <BlockView block={block} imageUrl={imageUrl} previewUrl={previews[block.id]}
            onOpenDiagram={drawable ? (id) => setDiagram(id) : undefined} />
        ) : (
          <BlockEditor block={block} mode={mode} imageUrl={imageUrl} previewUrl={previews[block.id]} uploadError={uploadError[block.id]}
            onUpload={uploadImage ? (file) => void upload(block.id, file) : undefined}
            onFocusCell={(row, col) => setCell({ blockId: block.id, row, col })}
            ops={{
              change: (next, focus) => replace(block.id, next, focus),
              split,
              removeEmpty: () => remove(block.id, true),
              step: (direction) => step(block.id, direction),
              openDiagram: () => setDiagram(block.id),
            }} />
        )}
        {blockIssues.map((issue, i) => <p key={i} role="alert" className="text-xs text-danger">{issue.message}</p>)}
        {inlineComments(block.id)}
      </div>
    );
  }

  /** Chỗ viết của phần lá chưa có nội dung (người học: chưa có bài làm; người soạn: mới có tiêu đề). */
  function emptyLeaf(section: DocSection, key: string): ReactNode {
    const last = section.blocks.at(-1) ?? section.heading;
    if (!last) return null;
    if (student) {
      if (section.blocks.some((b) => b.origin === "STUDENT")) return null;
      if (readOnly) {
        return emptyLeafLabel
          ? <p data-testid={`empty-part-${key}`} className="py-1 text-xs text-muted">{emptyLeafLabel}</p>
          : <div className="h-8" />;
      }
      return (
        <button type="button" data-testid={`write-part-${key}`} onClick={() => insertAfter(last.id, newBlock("PARAGRAPH", origin))}
          className="block w-full py-1 text-left text-sm text-faint hover:text-body">Viết câu trả lời…</button>
      );
    }
    if (readOnly || section.blocks.length > 0) return null;
    return (
      <button type="button" onClick={() => insertAfter(last.id, newBlock("PARAGRAPH", origin))}
        className="block w-full py-1 text-left text-sm text-faint hover:text-body">Nội dung…</button>
    );
  }

  function renderSection(section: DocSection): ReactNode {
    const heading = section.heading;
    const key = heading?.id ?? "DOCUMENT";
    const body = (
      <>
        {heading && renderBlock(heading)}
        {section.blocks.map(renderBlock)}
        {section.leaf && (heading || blocks.length > 0) && emptyLeaf(section, key)}
        {section.children.map(renderSection)}
      </>
    );
    if (!heading) return <Fragment key={key}>{body}</Fragment>;
    const active = highlightBlockId === heading.id;
    return (
      <section key={key} data-section={key} data-leaf={section.leaf || undefined}
        onClick={(event) => {
          if (!readOnly && section.leaf && event.target === event.currentTarget && (!student || inPart(heading.id))) {
            continueAfter((section.blocks.at(-1) ?? heading).id);
          }
        }}
        style={section.leaf ? undefined : { backgroundColor: BRANCH_BG[Math.min(section.depth - 1, BRANCH_BG.length - 1)] }}
        className={cx("my-2 rounded-lg", section.leaf ? "border border-[#c3cfe2] bg-white px-4 py-2.5" : "px-3 py-2",
          active && "ring-2 ring-primary/50")}>
        {body}
      </section>
    );
  }

  const tree = documentTree(document);

  return (
    <div className="flex flex-col gap-3" data-testid="document-editor"
      onKeyDownCapture={(event) => {
        if (readOnly || !(event.ctrlKey || event.metaKey)) return;
        const key = event.key.toLowerCase();
        if (key === "z" && !event.shiftKey) { event.preventDefault(); undo(); }
        else if (key === "y" || (key === "z" && event.shiftKey)) { event.preventDefault(); redo(); }
        else if ((key === "b" || key === "i" || key === "u") && textTarget && target) {
          event.preventDefault();
          replace(target.id, toggleFlag(target, key === "b" ? "bold" : key === "i" ? "italic" : "underline"));
        }
      }}>
      {(!readOnly || toolbarExtra) && (
        <div role="toolbar" aria-label="Định dạng và chèn"
          className="sticky top-16 z-20 flex flex-wrap items-center gap-0.5 rounded-3xl bg-[#edf2fa] px-3 py-1 shadow-[0_1px_2px_rgba(60,64,67,0.15)]">
          {!readOnly && (
            <>
              <ToolbarButton title="Hoàn tác (Ctrl+Z)" aria-label="Hoàn tác" disabled={history.past === 0} onClick={undo}><Glyph name="undo" /></ToolbarButton>
              <ToolbarButton title="Làm lại (Ctrl+Y)" aria-label="Làm lại" disabled={history.future === 0} onClick={redo}><Glyph name="redo" /></ToolbarButton>
              <Separator />
              {!student && (
                <>
                  <select aria-label="Kiểu đoạn" value={style === "UL" || style === "OL" ? "P" : style} disabled={!style}
                    onChange={(event) => {
                      if (!target || !event.target.value) return;
                      replace(target.id, convertBlock(target, event.target.value as StyleValue), { id: target.id, at: "end" });
                    }}
                    className="h-7 w-36 rounded-md bg-transparent px-1.5 text-[13px] text-ink hover:bg-black/5 disabled:opacity-40">
                    {style === "" && <option value="">Kiểu chữ</option>}
                    {AUTHOR_STYLES.map((value) => <option key={value} value={value}>{STYLE_LABELS[value]}</option>)}
                  </select>
                  <Separator />
                </>
              )}
              {FLAGS.map((flag) => (
                <ToolbarButton key={flag.key} title={flag.title} aria-label={flag.title} disabled={!textTarget}
                  active={textTarget ? targetRuns.length > 0 && targetRuns.every((run) => run[flag.key]) : undefined}
                  onClick={() => target && replace(target.id, toggleFlag(target, flag.key))}>
                  <span className={flag.className}>{flag.label}</span>
                </ToolbarButton>
              ))}
              <ToolPopover title="Màu chữ" disabled={!textTarget}
                label={<span className="flex flex-col items-center leading-none"><span className="font-semibold">A</span><span className="mt-0.5 h-1 w-4 rounded-sm" style={{ backgroundColor: targetRuns[0]?.color ?? "#202124" }} /></span>}>
                {(close) => <Swatches colors={TEXT_COLORS} noneLabel="Mặc định" onPick={(color) => { if (target) replace(target.id, setRunColor(target, "color", color)); close(); }} />}
              </ToolPopover>
              <ToolPopover title="Màu tô nền" disabled={!textTarget} label={<Glyph name="highlight" />}>
                {(close) => <Swatches colors={HIGHLIGHTS} noneLabel="Không tô" onPick={(color) => { if (target) replace(target.id, setRunColor(target, "highlight", color)); close(); }} />}
              </ToolPopover>
              <Separator />
              {ALIGNS.map((align) => (
                <ToolbarButton key={align.value} title={align.title} aria-label={align.title} disabled={!target || target.type === "TABLE" || target.type === "IMAGE" || target.type === "DIAGRAM"}
                  active={!!target && (alignOf(target) ?? "LEFT") === align.value && target.type !== "TABLE" && target.type !== "IMAGE" && target.type !== "DIAGRAM"}
                  onClick={() => target && replace(target.id, setAlign(target, align.value))}>
                  <Glyph name={align.glyph} />
                </ToolbarButton>
              ))}
              <Separator />
              <ToolbarButton title="Danh sách chấm" aria-label="Danh sách chấm" disabled={!textTarget} active={style === "UL"}
                onClick={() => target && replace(target.id, convertBlock(target, style === "UL" ? "P" : "UL"), { id: target.id, at: "end" })}><Glyph name="bullets" /></ToolbarButton>
              <ToolbarButton title="Danh sách số" aria-label="Danh sách số" disabled={!textTarget} active={style === "OL"}
                onClick={() => target && replace(target.id, convertBlock(target, style === "OL" ? "P" : "OL"), { id: target.id, at: "end" })}><Glyph name="numbers" /></ToolbarButton>
              <ToolbarButton title="Giảm thụt lề" aria-label="Giảm thụt lề" disabled={target?.type !== "LIST"} onClick={() => indentList(-1)}><Glyph name="outdent" /></ToolbarButton>
              <ToolbarButton title="Tăng thụt lề" aria-label="Tăng thụt lề" disabled={target?.type !== "LIST"} onClick={() => indentList(1)}><Glyph name="indent" /></ToolbarButton>
              <Separator />
              <ToolPopover title="Chèn bảng" disabled={!insertAllowed} label={<Glyph name="table" />}>
                {(close) => <TablePicker onPick={(rows, cols) => { insertTable(rows, cols); close(); }} />}
              </ToolPopover>
              {uploadImage && (
                <label title="Chèn ảnh" aria-label="Chèn ảnh" onMouseDown={(event) => event.preventDefault()}
                  className={cx("inline-flex h-7 min-w-7 items-center justify-center rounded-md px-1.5 text-body hover:bg-black/5", insertAllowed ? "cursor-pointer" : "pointer-events-none opacity-35")}>
                  <Glyph name="image" />
                  <input type="file" accept={IMAGE_TYPES} className="sr-only" data-testid="add-IMAGE" disabled={!insertAllowed}
                    onChange={(event) => { const file = event.target.files?.[0]; if (file) insertImage(file); event.target.value = ""; }} />
                </label>
              )}
              {diagramEnabled && (
                <ToolPopover title="Chèn sơ đồ Draw.io" disabled={!insertAllowed} label={<Glyph name="diagram" />}>
                  {(close) => (
                    <div className="flex min-w-40 flex-col">
                      {(Object.keys(diagramTypeLabels) as DiagramType[]).map((type) => (
                        <button key={type} type="button" data-testid={`add-DIAGRAM-${type}`} onClick={() => { insertDiagram(type); close(); }}
                          className="rounded-md px-2 py-1.5 text-left text-[13px] hover:bg-shell">{diagramTypeLabels[type]}</button>
                      ))}
                    </div>
                  )}
                </ToolPopover>
              )}
              {tableTarget && cell && (
                <>
                  <Separator />
                  <ToolbarButton title="Hàng đầu là hàng tiêu đề" active={!!tableTarget.content.headerRow}
                    onClick={() => replace(tableTarget.id, { ...tableTarget, content: { ...tableTarget.content, headerRow: !tableTarget.content.headerRow || undefined } })}>
                    Hàng tiêu đề
                  </ToolbarButton>
                  <ToolbarButton title="Xóa hàng đang chọn" disabled={tableTarget.content.rows.length <= 1}
                    onClick={() => { tableChange(tableTarget.content.rows.filter((_, r) => r !== cell.row)); setCell(null); }}>Xóa hàng</ToolbarButton>
                  <ToolbarButton title="Xóa cột đang chọn" disabled={Math.max(...tableTarget.content.rows.map((row) => row.length)) <= 1}
                    onClick={() => { tableChange(tableTarget.content.rows.map((row) => row.filter((_, c) => c !== cell.col))); setCell(null); }}>Xóa cột</ToolbarButton>
                </>
              )}
            </>
          )}
          {toolbarExtra && <div className="ml-auto flex flex-wrap items-center gap-0.5">{toolbarExtra}</div>}
        </div>
      )}

      <div className={rail ? "grid grid-cols-[minmax(0,816px)_300px] items-start justify-center gap-5" : "mx-auto w-full max-w-[816px]"}>
        <div ref={paper} data-testid="document-paper"
          className="min-h-[60vh] min-w-0 rounded-sm border border-line bg-white px-5 pt-10 pb-4 shadow-[0_1px_3px_rgba(60,64,67,0.2)] sm:px-14 sm:pt-12">
          {/* Khung bọc nội dung (không có min-height) để cột ghi chú biết khi nào block đổi chỗ. */}
          <div ref={content}>
            {header}
            {inlineComments(null)}
            {blocks.length === 0 && (readOnly
              ? <div className="h-16" />
              : <button type="button" onClick={() => insertAfter(null, newBlock("PARAGRAPH", origin))} className="w-full py-2 text-left text-sm text-faint">
                {student ? "Viết câu trả lời…" : "Bấm để bắt đầu viết…"}
              </button>)}
            {renderSection(tree)}
          </div>
          {!readOnly && blocks.length > 0 && atEndInPart && (
            <button type="button" aria-label="Viết tiếp ở cuối tài liệu" onClick={continueAtEnd} className="block h-14 w-full cursor-text" />
          )}
        </div>
        {rail && <CommentRail paper={content} comments={visible} activeKey={activeComment} />}
      </div>

      {editing && editing.type === "DIAGRAM" && (
        <DiagramEditor xml={editing.content.xml} title={editing.content.alt || diagramTypeLabels[editing.content.diagramType]}
          onClose={() => setDiagram(null)}
          onSave={(xml, svg) => replace(editing.id, { ...editing, content: { ...editing.content, xml, ...(svg ? { svgPreview: svg } : {}) } })} />
      )}
    </div>
  );
}

type Ops = {
  change: (block: DocumentBlock, focus?: FocusTarget) => void;
  /** Enter: block hiện tại thành {@code current}, chèn {@code next} ngay sau và đặt con trỏ vào đó. */
  split: (current: DocumentBlock, next: DocumentBlock) => void;
  removeEmpty: () => void;
  step: (direction: -1 | 1) => void;
  openDiagram: () => void;
};

const CAPTION = "text-center text-xs text-muted italic";

function BlockEditor({ block, mode, ops, imageUrl, previewUrl, onUpload, uploadError, onFocusCell }: {
  block: DocumentBlock;
  mode: Mode;
  ops: Ops;
  imageUrl?: ImageUrl;
  previewUrl?: string;
  onUpload?: (file: File) => void;
  uploadError?: string;
  onFocusCell: (row: number, col: number) => void;
}) {
  void mode;
  switch (block.type) {
    case "HEADING":
      return (
        <AutoTextarea aria-label="Nội dung tiêu đề" value={block.content.text} maxLength={500} placeholder={headingLabels[block.content.style]}
          className={cx(HEADING_CLASS[block.content.style] ?? HEADING_CLASS.H1, alignClass(block.content.align))}
          onChange={(event) => ops.change({ ...block, content: { ...block.content, text: event.target.value.replace(/\n/g, " ") } })}
          onKeyDown={(event) => textKeys(event, {
            // Enter ở tiêu đề mở đoạn mới bên dưới, không cắt tiêu đề.
            enter: () => ops.split(block, paragraph(block.origin, "")),
            removeEmpty: ops.removeEmpty,
            step: ops.step,
          })} />
      );
    case "PARAGRAPH": {
      const run = block.content.runs[0];
      return (
        <AutoTextarea aria-label="Đoạn văn" value={runsText(block.content.runs)} placeholder="Nhập nội dung…"
          className={cx(PARAGRAPH_CLASS, runClass(run), alignClass(block.content.align))} style={runStyle(run)}
          onChange={(event) => ops.change({ ...block, content: { ...block.content, runs: textRuns(block.content.runs, event.target.value) } })}
          onKeyDown={(event) => textKeys(event, {
            enter: (before, after) => ops.split({ ...block, content: { ...block.content, runs: textRuns(block.content.runs, before) } }, paragraph(block.origin, after, flagsOf(run))),
            removeEmpty: ops.removeEmpty,
            step: ops.step,
          })} />
      );
    }
    case "LIST":
      return <ListEditor block={block} ops={ops} />;
    case "TABLE":
      return <TableEditor block={block} onChange={(next) => ops.change(next)} onFocusCell={onFocusCell} />;
    case "IMAGE": {
      const src = previewUrl ?? (block.content.hasFile ? imageUrl?.(block.id) : null);
      return (
        <figure className="flex flex-col items-center gap-1 py-1">
          <label title={onUpload ? (src ? "Bấm để đổi ảnh" : "Chọn ảnh") : undefined} className={cx("block w-full", onUpload && "cursor-pointer")}>
            {src ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={src} alt={block.content.alt ?? ""} style={{ width: block.content.width ? `${block.content.width}px` : undefined }} className="mx-auto max-w-full" />
            ) : (
              <span className="flex w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-line-strong p-6 text-xs text-muted hover:border-primary hover:text-primary">
                <Icon name="upload_file" size={20} />{block.content.fileRef ? "Đang tải ảnh…" : "Chọn ảnh"}
              </span>
            )}
            {onUpload && (
              <input type="file" accept={IMAGE_TYPES} className="sr-only"
                onChange={(event) => { const file = event.target.files?.[0]; if (file) onUpload(file); event.target.value = ""; }} />
            )}
          </label>
          <AutoTextarea aria-label="Chú thích ảnh" value={block.content.alt ?? ""} maxLength={500} placeholder="Chú thích" className={CAPTION}
            onChange={(event) => ops.change({ ...block, content: { ...block.content, alt: event.target.value.replace(/\n/g, " ") } })} />
          {uploadError && <p role="alert" className="text-xs text-danger">{uploadError}</p>}
        </figure>
      );
    }
    case "DIAGRAM":
      return (
        <figure className="flex flex-col items-center gap-1 py-1">
          <BlockView block={{ ...block, content: { ...block.content, alt: "" } }} onOpenDiagram={ops.openDiagram} />
          <AutoTextarea aria-label="Chú thích sơ đồ" value={block.content.alt ?? ""} maxLength={500}
            placeholder={`Chú thích · ${diagramTypeLabels[block.content.diagramType]}`} className={CAPTION}
            onChange={(event) => ops.change({ ...block, content: { ...block.content, alt: event.target.value.replace(/\n/g, " ") } })} />
        </figure>
      );
  }
}

/** Danh sách: Enter thêm mục, Enter ở mục trống thì thoát danh sách, Tab và Shift+Tab đổi cấp (tối đa 4). */
function ListEditor({ block, ops }: { block: Extract<DocumentBlock, { type: "LIST" }>; ops: Ops }) {
  const items = block.content.items;
  const Tag = block.content.ordered ? "ol" : "ul";
  function set(next: ListItem[], focus?: { field: number; at: "start" | "end" }) {
    ops.change({ ...block, content: { ...block.content, items: next } }, focus ? { id: block.id, ...focus } : undefined);
  }
  function sibling(field: HTMLTextAreaElement, direction: -1 | 1): HTMLTextAreaElement | null {
    const li = field.closest("li");
    const other = direction < 0 ? li?.previousElementSibling : li?.nextElementSibling;
    return other?.querySelector("textarea") ?? null;
  }
  function keys(event: KeyboardEvent<HTMLTextAreaElement>, item: ListItem, i: number) {
    if (event.nativeEvent.isComposing) return;
    const field = event.currentTarget;
    const value = field.value;
    const flags = flagsOf(item.runs[0]);
    if (event.key === "Tab") {
      event.preventDefault();
      const level = Math.min(4, Math.max(0, (item.level ?? 0) + (event.shiftKey ? -1 : 1)));
      set(items.map((x, j) => (j === i ? { ...x, level } : x)));
    } else if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      if (value === "") {
        const rest = items.filter((_, j) => j !== i);
        if (rest.length === 0) ops.change(convertBlock(block, "P"), { id: block.id, at: "start" });
        else ops.split({ ...block, content: { ...block.content, items: rest } }, paragraph(block.origin, "", flags));
        return;
      }
      const before = value.slice(0, field.selectionStart);
      const after = value.slice(field.selectionEnd);
      set([...items.slice(0, i), { ...item, runs: [{ ...flags, text: before }] }, { level: item.level ?? 0, runs: [{ ...flags, text: after }] }, ...items.slice(i + 1)],
        { field: i + 1, at: "start" });
    } else if (event.key === "Backspace" && value === "") {
      event.preventDefault();
      if (items.length === 1) ops.removeEmpty();
      else set(items.filter((_, j) => j !== i), { field: Math.max(0, i - 1), at: i > 0 ? "end" : "start" });
    } else if (event.key === "ArrowUp" && field.selectionStart === 0 && field.selectionEnd === 0) {
      event.preventDefault();
      const other = sibling(field, -1);
      if (other) { other.focus(); other.setSelectionRange(other.value.length, other.value.length); } else ops.step(-1);
    } else if (event.key === "ArrowDown" && field.selectionStart === value.length) {
      event.preventDefault();
      const other = sibling(field, 1);
      if (other) { other.focus(); other.setSelectionRange(0, 0); } else ops.step(1);
    }
  }
  return (
    <Tag className={cx(block.content.ordered ? "list-decimal" : "list-disc", "pl-6", PARAGRAPH_CLASS, alignClass(block.content.align))}>
      {items.map((item, i) => (
        <li key={i} style={{ marginLeft: `${(item.level ?? 0) * 24}px` }}>
          <AutoTextarea aria-label={`Mục ${i + 1}`} value={runsText(item.runs)} placeholder="Mục danh sách"
            className={cx(PARAGRAPH_CLASS, runClass(item.runs[0]), alignClass(block.content.align))} style={runStyle(item.runs[0])}
            onChange={(event) => set(items.map((x, j) => (j === i ? { ...x, runs: textRuns(x.runs, event.target.value) } : x)))}
            onKeyDown={(event) => keys(event, item, i)} />
        </li>
      ))}
    </Tag>
  );
}

/**
 * Bảng kiểu Google Docs: rê chuột vào ô hiện ô vuông "+" ở mép dưới (chèn hàng bên dưới) và mép phải (chèn cột bên
 * phải); xóa hàng, cột và bật hàng tiêu đề trên thanh công cụ khi đang ở trong bảng.
 */
function TableEditor({ block, onChange, onFocusCell }: {
  block: Extract<DocumentBlock, { type: "TABLE" }>;
  onChange: (block: DocumentBlock) => void;
  onFocusCell: (row: number, col: number) => void;
}) {
  const rows = block.content.rows;
  const columns = Math.max(1, ...rows.map((row) => row.length));
  const empty = (): TableCell => ({ runs: [{ text: "" }] });
  function set(next: TableCell[][]) {
    onChange({ ...block, content: { ...block.content, rows: next } });
  }
  const square = "absolute z-10 hidden size-4 items-center justify-center rounded-sm border border-primary bg-white text-[11px] leading-none text-primary shadow-sm hover:bg-primary hover:text-white group-hover/cell:flex";
  return (
    <div className="overflow-x-auto px-2 pt-1 pb-3">
      <table className="w-full border-collapse">
        <tbody>
          {rows.map((row, r) => (
            <tr key={r} className={block.content.headerRow && r === 0 ? "bg-canvas" : undefined}>
              {Array.from({ length: columns }, (_, c) => (
                <td key={c} className="group/cell relative min-w-20 border border-line-strong align-top">
                  <AutoTextarea aria-label={`Ô ${r + 1}-${c + 1}`} value={runsText(row[c]?.runs)} onFocus={() => onFocusCell(r, c)}
                    className={cx("px-2 py-1.5 text-[13px] leading-snug text-ink", block.content.headerRow && r === 0 && "font-semibold")}
                    onChange={(event) => set(rows.map((cells, i) => i !== r ? cells : Array.from({ length: columns }, (_, j) =>
                      j === c ? { runs: [{ ...(cells[j]?.runs[0] ?? { text: "" }), text: event.target.value }] } : cells[j] ?? empty())))} />
                  {rows.length < 200 && (
                    <button type="button" aria-label="Chèn hàng bên dưới" title="Chèn hàng bên dưới" className={cx(square, "-bottom-2 left-1/2 -translate-x-1/2")}
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => set([...rows.slice(0, r + 1), Array.from({ length: columns }, empty), ...rows.slice(r + 1)])}>+</button>
                  )}
                  {columns < 30 && (
                    <button type="button" aria-label="Chèn cột bên phải" title="Chèn cột bên phải" className={cx(square, "top-1/2 -right-2 -translate-y-1/2")}
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => set(rows.map((cells) => [...Array.from({ length: columns }, (_, j) => cells[j] ?? empty()).slice(0, c + 1), empty(),
                        ...Array.from({ length: columns }, (_, j) => cells[j] ?? empty()).slice(c + 1)]))}>+</button>
                  )}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
