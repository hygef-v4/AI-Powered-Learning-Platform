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
import { BlockView, HEADING_CLASS, PARAGRAPH_CLASS, alignClass, type ImageUrl } from "./DocumentViewer";
import { RichText, getCaret, setCaret, type Caret } from "./RichText";
import { colorRange, formatOf, joinLines, runLines, sliceRuns, splitRuns, toggleFlagRange, type RunFlag } from "./runs";
import {
  blockText,
  computeParts,
  documentTree,
  headingLabels,
  newBlock,
  newId,
  runsText,
  sectionsOf,
  type Align,
  type BlockOrigin,
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
  /** Opt-in answer affordances for the individual assignment workspace. */
  answerGuidance?: boolean;
};

type StyleValue = "P" | "UL" | "OL" | HeadingStyle;
/** Nơi đặt con trỏ sau khi tài liệu đổi: ô nhập đầu, cuối, hoặc ô thứ {@code field} của block. */
type FocusTarget = { id: string; at: "start" | "end"; field?: number };
/** Vùng chọn trong ô chữ có định dạng: block, ô thứ {@code field} (đoạn: 0; danh sách: mục), vị trí theo ký tự. */
type Selected = { blockId: string; field: number; start: number; end: number };

const STYLE_LABELS: Record<StyleValue, string> = { P: "Văn bản thường", UL: "Danh sách chấm", OL: "Danh sách số", ...headingLabels };
/** Kiểu đoạn trên thanh công cụ (yêu cầu 2026-10-22): văn bản thường và 4 cấp tiêu đề. */
const AUTHOR_STYLES: StyleValue[] = ["P", "H1", "H2", "H3", "H4"];

const FLAGS: { key: RunFlag; label: string; title: string; className: string }[] = [
  { key: "bold", label: "B", title: "Đậm (Ctrl+B)", className: "font-bold" },
  { key: "italic", label: "I", title: "Nghiêng (Ctrl+I)", className: "font-serif italic" },
  { key: "underline", label: "U", title: "Gạch chân (Ctrl+U)", className: "underline" },
  { key: "strike", label: "S", title: "Gạch ngang", className: "line-through" },
];

const TEXT_COLORS = ["#202124", "#5F6368", "#C5221F", "#E37400", "#188038", "#1A73E8", "#9334E6", "#B80672"];
const HIGHLIGHTS = ["#FFF475", "#CCFF90", "#A7FFEB", "#AECBFA", "#D7AEFB", "#FDCFE8", "#E8EAED"];
/** Màu của phần theo tầng (yêu cầu 2026-10-22): cùng tầng cùng màu, tầng sâu đậm hơn; lá thì khung mang màu tầng, ruột trắng. */
const SECTION_BG = ["#e8eef8", "#d7e1f2", "#c6d4ec", "#b6c7e5"];
const ALIGNS: { value: Align; title: string; glyph: GlyphName }[] = [
  { value: "LEFT", title: "Căn trái", glyph: "alignLeft" },
  { value: "CENTER", title: "Căn giữa", glyph: "alignCenter" },
  { value: "RIGHT", title: "Căn phải", glyph: "alignRight" },
  { value: "JUSTIFY", title: "Căn đều", glyph: "justify" },
];
/** Nút ✕ ở góc block; bảng có nút ✕ riêng nằm cùng hàng với các nút + − của bảng. */
const REMOVE_LABELS: Partial<Record<DocumentBlock["type"], string>> = { DIAGRAM: "Xóa sơ đồ", IMAGE: "Xóa ảnh" };
/** Người học: sơ đồ, ảnh của đề trong phần vẫn có nút ✕ nhưng mờ, như "−" của hàng, cột của đề. */
const LOCKED_LABELS: Partial<Record<DocumentBlock["type"], string>> = { DIAGRAM: "Sơ đồ của đề", IMAGE: "Ảnh của đề" };
const IMAGE_TYPES = ".png,.jpg,.jpeg,.gif,.svg";
const HISTORY_GAP_MS = 700;

function runsOf(block: DocumentBlock | undefined): Run[] {
  if (block?.type === "PARAGRAPH") return block.content.runs;
  if (block?.type === "LIST") return block.content.items.flatMap((item) => item.runs);
  return [];
}

/** Run của ô thứ {@code field} trong block chữ (đoạn: ô 0; danh sách: mục thứ field). */
function fieldRuns(block: DocumentBlock, field: number): Run[] {
  if (block.type === "PARAGRAPH") return block.content.runs;
  if (block.type === "LIST") return block.content.items[field]?.runs ?? [];
  return [];
}

function mapField(block: DocumentBlock, field: number, change: (runs: Run[]) => Run[]): DocumentBlock {
  if (block.type === "PARAGRAPH") return { ...block, content: { ...block.content, runs: change(block.content.runs) } };
  if (block.type === "LIST") {
    return { ...block, content: { ...block.content, items: block.content.items.map((item, i) => (i === field ? { ...item, runs: change(item.runs) } : item)) } };
  }
  return block;
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

function paragraph(origin: BlockOrigin, runs: Run[] = [{ text: "" }], align?: Align): DocumentBlock {
  return { id: newId(), origin, type: "PARAGRAPH", content: { runs, ...(align ? { align } : {}) } };
}

/** Đổi kiểu block chữ (đoạn, tiêu đề, danh sách), giữ id, nguồn, chữ, định dạng từng phần và căn lề. */
export function convertBlock(block: DocumentBlock, style: StyleValue): DocumentBlock {
  const { id, origin } = block;
  const align = alignOf(block);
  const keep = align ? { align } : {};
  const runs: Run[] = block.type === "PARAGRAPH" ? block.content.runs
    : block.type === "LIST" ? joinLines(block.content.items.map((item) => item.runs))
    : [{ text: blockText(block) }];
  if (style === "P") return { id, origin, type: "PARAGRAPH", content: { runs, ...keep } };
  if (style === "UL" || style === "OL") {
    if (block.type === "LIST") return { ...block, content: { ...block.content, ordered: style === "OL" } };
    return { id, origin, type: "LIST", content: { ordered: style === "OL", items: runLines(runs).map((line) => ({ level: 0, runs: line })), ...keep } };
  }
  return { id, origin, type: "HEADING", content: { style, text: runsText(runs).replace(/\s*\n\s*/g, " ").slice(0, 500), ...keep } };
}

function mapRuns(block: DocumentBlock, change: (run: Run) => Run): DocumentBlock {
  if (block.type === "PARAGRAPH") return { ...block, content: { ...block.content, runs: block.content.runs.map(change) } };
  if (block.type === "LIST") return { ...block, content: { ...block.content, items: block.content.items.map((item) => ({ ...item, runs: item.runs.map(change) })) } };
  return block;
}

/** Bật hoặc tắt một định dạng cho cả đoạn hoặc cả danh sách (khi không bôi đen). */
export function toggleFlag(block: DocumentBlock, flag: RunFlag): DocumentBlock {
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

/** Vùng chọn trong ô chữ có định dạng đang có con trỏ, nếu ô đó thuộc trang {@code root}. */
function selectionIn(root: HTMLElement | null): Selected | null {
  const active = globalThis.document.activeElement;
  if (!(active instanceof HTMLElement) || !active.hasAttribute("data-rich") || !root?.contains(active)) return null;
  const block = active.closest<HTMLElement>("[data-block-id]");
  if (!block) return null;
  const caret = getCaret(active);
  const field = Array.from(block.querySelectorAll("[data-rich]")).indexOf(active);
  return { blockId: block.dataset.blockId ?? "", field, start: caret.start, end: caret.end };
}

/** Đặt con trỏ ở đầu hoặc cuối một ô (ô chữ thường hoặc ô chữ có định dạng). */
function caretTo(field: HTMLElement, at: "start" | "end") {
  field.focus();
  if (field instanceof HTMLTextAreaElement) {
    const position = at === "start" ? 0 : field.value.length;
    field.setSelectionRange(position, position);
  } else {
    setCaret(field, at === "start" ? 0 : (field.textContent ?? "").length);
  }
}

function focusField(root: HTMLElement | null, target: FocusTarget) {
  const block = root?.querySelector<HTMLElement>(`[data-block-id="${target.id}"]`);
  if (!block) return;
  const fields = block.querySelectorAll<HTMLElement>("[data-editable]");
  const field = fields[target.field ?? (target.at === "end" ? fields.length - 1 : 0)];
  if (!field) {
    block.focus();
    return;
  }
  caretTo(field, target.at);
}

type KeyHandlers = {
  enter?: (caret: Caret) => void;
  removeEmpty?: () => void;
  step?: (direction: -1 | 1) => void;
};

/** Phím kiểu trình soạn văn bản ở ô tiêu đề: Enter mở đoạn mới, Backspace ở ô trống thì xóa, mũi tên ở mép thì sang block bên cạnh. */
function textKeys(event: KeyboardEvent<HTMLTextAreaElement>, handlers: KeyHandlers) {
  if (event.nativeEvent.isComposing) return;
  const field = event.currentTarget;
  keys(event, { start: field.selectionStart, end: field.selectionEnd, length: field.value.length }, handlers);
}

/** Phím của ô chữ: Enter tách tại con trỏ, Backspace ở ô trống thì xóa block, mũi tên ở mép thì sang block bên cạnh. */
function keys(event: KeyboardEvent<HTMLElement>, caret: Caret, handlers: KeyHandlers) {
  if (event.key === "Enter" && handlers.enter) {
    event.preventDefault();
    handlers.enter(caret);
  } else if (event.key === "Backspace" && caret.length === 0 && handlers.removeEmpty) {
    event.preventDefault();
    handlers.removeEmpty();
  } else if (event.key === "ArrowUp" && caret.start === 0 && caret.end === 0 && handlers.step) {
    event.preventDefault();
    handlers.step(-1);
  } else if (event.key === "ArrowDown" && caret.start === caret.length && handlers.step) {
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

/** Ô bật ra dưới nút trên thanh công cụ (bảng màu, chọn kích thước bảng); bấm ra ngoài thì đóng. */
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

/**
 * Chọn kích thước bảng bằng lưới như Google Docs. Cột lưới cố định 18px (như bản demo): cột co giãn thì ô bật ra hẹp
 * theo nút bấm, các ô 16px chồng lên nhau thành dải chữ nhật.
 */
function TablePicker({ onPick }: { onPick: (rows: number, cols: number) => void }) {
  const [hover, setHover] = useState({ rows: 0, cols: 0 });
  return (
    <div className="flex flex-col gap-2 p-0.5" data-testid="table-picker">
      <div className="grid grid-cols-[repeat(10,18px)] gap-[3px]" onMouseLeave={() => setHover({ rows: 0, cols: 0 })}>
        {Array.from({ length: 80 }, (_, i) => {
          const row = Math.floor(i / 10) + 1;
          const col = (i % 10) + 1;
          const on = row <= hover.rows && col <= hover.cols;
          return (
            <button key={i} type="button" aria-label={`${row} × ${col}`} onMouseEnter={() => setHover({ rows: row, cols: col })}
              onClick={() => onPick(row, col)}
              className={cx("size-[18px] rounded-[2px] border p-0", on ? "border-[#1a73e8] bg-[#d2e3fc]" : "border-[#dadce0] bg-[#f8f9fa]")} />
          );
        })}
      </div>
      <p className="text-center text-xs text-body">{hover.rows > 0 ? `${hover.rows} × ${hover.cols}` : "Bảng"}</p>
    </div>
  );
}

/** Ô vuông "+", "−", "✕" cạnh bảng, sơ đồ (như bản demo); giữ con trỏ trong ô đang gõ khi bấm. */
function Square({ label, danger = false, disabled = false, onClick, children }: {
  label: string;
  danger?: boolean;
  disabled?: boolean;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button type="button" title={label} aria-label={label} disabled={disabled} onMouseDown={(event) => event.preventDefault()} onClick={onClick}
      className={cx("inline-flex size-[18px] flex-none items-center justify-center rounded-[2px] border border-[#747775] bg-white p-0 text-xs leading-none text-[#444746] disabled:cursor-not-allowed disabled:opacity-40",
        danger ? "enabled:hover:border-danger enabled:hover:text-danger" : "enabled:hover:border-[#1a73e8] enabled:hover:text-[#1a73e8]")}>
      {children}
    </button>
  );
}

/** Nút xóa cả block (sơ đồ, ảnh) nổi ở góc trên trái khi trỏ vào block (yêu cầu 2026-10-22, thay menu "⋮"); không được xóa thì mờ. */
function CornerDelete({ label, disabled = false, onClick }: { label: string; disabled?: boolean; onClick: () => void }) {
  return (
    <span className="pointer-events-none absolute -top-2 -left-2 z-20 opacity-0 transition-opacity group-focus-within/block:pointer-events-auto group-focus-within/block:opacity-100 group-hover/block:pointer-events-auto group-hover/block:opacity-100">
      <Square danger label={label} disabled={disabled} onClick={onClick}>✕</Square>
    </span>
  );
}

/**
 * Trình soạn tài liệu dùng chung U07, U08, U12 (BR-U07-10, 15) theo kiểu Google Docs: trang giấy, thanh công cụ dính
 * trên đầu, ghi chú (rubric) ở lề phải dính theo phần. Các phần hiện theo cây tiêu đề: cùng tầng cùng màu, lá có khung
 * màu và ruột trắng (chỗ viết bài). Người học (yêu cầu 2026-10-21) chỉ viết trong phần lá, không thêm tiêu đề, không sửa
 * chữ của người soạn (chữ nhạt); sơ đồ của người soạn trong phần thì bấm vào để vẽ, bảng thì điền thẳng. Định dạng chữ
 * áp cho vùng bôi đen, không bôi đen thì cho cả đoạn (yêu cầu 2026-10-22).
 */
export function DocumentEditor({
  document, onChange, mode, diagramEnabled, readOnly = false, imageUrl, uploadImage, issues = [],
  header, toolbarExtra, comments, activeComment, highlightBlockId, onFocusBlock, emptyLeafLabel, answerGuidance = false,
}: Props) {
  const origin: BlockOrigin = mode === "author" ? "TEACHER" : "STUDENT";
  const [previews, setPreviews] = useState<Record<string, string>>({});
  const [diagram, setDiagram] = useState<string | null>(null);
  const [uploadError, setUploadError] = useState<Record<string, string>>({});
  const [focusedId, setFocusedId] = useState<string | null>(null);
  const [selected, setSelected] = useState<Selected | null>(null);
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

  // Vùng bôi đen để thanh công cụ hiện đúng định dạng của phần đang chọn.
  useEffect(() => {
    const owner = globalThis.document;
    const track = () => {
      const next = selectionIn(paper.current);
      setSelected((current) => (current && next && current.blockId === next.blockId && current.field === next.field
        && current.start === next.start && current.end === next.end) || (!current && !next) ? current : next);
    };
    owner.addEventListener("selectionchange", track);
    return () => owner.removeEventListener("selectionchange", track);
  }, []);

  const blocks = document.blocks;
  const student = mode === "student";
  const guided = student && answerGuidance;
  // Người học chỉ viết trong phần: block nằm từ tiêu đề lá tới tiêu đề kế; khung không có tiêu đề thì cả bài.
  const sections = student ? sectionsOf(document, computeParts(document)) : null;
  const inPart = (blockId: string | undefined) => !sections || (!!blockId && sections.has(blockId));
  // Tài liệu trống không có tiêu đề nên cả bài là một phần; còn lại cuối tài liệu luôn thuộc tiêu đề cuối (là lá).
  const atEndInPart = blocks.length === 0 || inPart(blocks.at(-1)!.id);
  // Bảng của người soạn trong phần: người học điền thẳng vào (yêu cầu 2026-10-22), không xóa bảng.
  const fillable = (block: DocumentBlock) => student && !readOnly && block.origin === "TEACHER" && block.type === "TABLE" && inPart(block.id);
  const locked = (block: DocumentBlock) => readOnly || (student && block.origin === "TEACHER" && !fillable(block));
  const focused = blocks.find((b) => b.id === focusedId);
  const insertAllowed = focused ? inPart(focused.id) : atEndInPart;
  const target = focused && !locked(focused) ? focused : undefined;
  const style = styleOf(target);
  const textTarget = target?.type === "PARAGRAPH" || target?.type === "LIST";
  // Định dạng hiện trên thanh công cụ: của vùng bôi đen nếu có, không thì của cả đoạn (đúng phần mà nút sẽ đổi).
  const shownRuns = target && textTarget && selected?.blockId === target.id && selected.start !== selected.end
    ? sliceRuns(fieldRuns(target, selected.field), selected.start, selected.end) : runsOf(target);

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
  /** Chèn sơ đồ là mở luôn Draw.io, không chọn loại hay mẫu (yêu cầu 2026-10-22; loại do server suy từ hình vẽ). */
  function insertDiagram() {
    if (!insertAllowed) return;
    const block = newBlock("DIAGRAM", origin);
    insert(block);
    setDiagram(block.id);
  }
  /** Thụt lề mục danh sách đang gõ (Tab, Shift+Tab làm tương tự trong ô). */
  function indentList(delta: -1 | 1) {
    if (target?.type !== "LIST") return;
    const fields = Array.from(paper.current?.querySelectorAll<HTMLElement>(`[data-block-id="${target.id}"] [data-editable]`) ?? []);
    const index = Math.max(0, fields.findIndex((field) => field === globalThis.document.activeElement));
    replace(target.id, { ...target, content: { ...target.content, items: target.content.items.map((item, i) => (i === index
      ? { ...item, level: Math.min(4, Math.max(0, (item.level ?? 0) + delta)) } : item)) } });
  }
  /** Định dạng chữ (yêu cầu 2026-10-22): có vùng bôi đen trong ô thì chỉ đổi phần bôi đen, không thì cả đoạn. */
  function formatText(range: (runs: Run[], start: number, end: number) => Run[], whole: (block: DocumentBlock) => DocumentBlock) {
    if (!target || !textTarget) return;
    const selection = selectionIn(paper.current);
    if (selection && selection.blockId === target.id && selection.start !== selection.end) {
      replace(target.id, mapField(target, selection.field, (runs) => range(runs, selection.start, selection.end)));
    } else {
      replace(target.id, whole(target));
    }
  }
  function toggle(flag: RunFlag) {
    formatText((runs, start, end) => toggleFlagRange(runs, start, end, flag), (block) => toggleFlag(block, flag));
  }
  function paint(key: "color" | "highlight", value: string | undefined) {
    formatText((runs, start, end) => colorRange(runs, start, end, key, value), (block) => setRunColor(block, key, value));
  }

  const editing = blocks.find((b) => b.id === diagram && b.type === "DIAGRAM");
  // Đang vẽ (Draw.io chiếm cột phải, trang hẹp lại) thì ẩn ghi chú bên lề như bản demo ẩn cột mục lục.
  const visible = editing ? [] : (comments ?? []).filter((c) => !c.anchorBlockId || blocks.some((b) => b.id === c.anchorBlockId));
  const rail = wide && visible.length > 0;
  const inlineComments = (anchor: string | null) => rail ? null : visible.filter((c) => c.anchorBlockId === anchor).map((c) => (
    <div key={c.key} className="my-2" data-comment={c.key}>{c.content}</div>
  ));

  function renderBlock(block: DocumentBlock): ReactNode {
    const isLocked = locked(block);
    const teacherBlock = student && block.origin === "TEACHER";
    const drawable = !readOnly && teacherBlock && block.type === "DIAGRAM" && inPart(block.id);
    const muted = teacherBlock && !drawable && !fillable(block) && block.type !== "HEADING";
    const removeLabel = !readOnly && !isLocked && !teacherBlock ? REMOVE_LABELS[block.type] : undefined;
    const lockedLabel = !readOnly && teacherBlock && inPart(block.id) ? LOCKED_LABELS[block.type] : undefined;
    const blockIssues = issues.filter((issue) => issue.blockId === block.id);
    return (
      <div key={block.id} data-testid={`block-${block.id}`} data-block-id={block.id} tabIndex={-1}
        title={muted && !readOnly ? "Đề của giảng viên, không sửa được" : undefined}
        onFocusCapture={() => focusBlock(block.id)} onMouseDownCapture={() => focusBlock(block.id)}
        className={cx("group/block relative -mx-1 rounded px-1 py-0.5 outline-none",
          muted && "opacity-70", muted && !readOnly && "cursor-not-allowed",
          blockIssues.length > 0 && "shadow-[inset_3px_0_0_var(--color-danger)]")}>
        {removeLabel && <CornerDelete label={removeLabel} onClick={() => remove(block.id)} />}
        {lockedLabel && <CornerDelete label={lockedLabel} disabled onClick={() => {}} />}
        {isLocked ? (
          <BlockView block={block} imageUrl={imageUrl} previewUrl={previews[block.id]}
            onOpenDiagram={drawable ? (id) => setDiagram(id) : undefined} />
        ) : (
          <BlockEditor block={block} imageUrl={imageUrl} previewUrl={previews[block.id]} uploadError={uploadError[block.id]}
            placeholder={guided && block.origin === "STUDENT" ? "Nhập bài làm của bạn tại đây…" : undefined}
            onUpload={uploadImage ? (file) => void upload(block.id, file) : undefined} guard={teacherBlock}
            ops={{
              change: (next, focus) => replace(block.id, next, focus),
              split,
              removeEmpty: () => remove(block.id, true),
              remove: () => remove(block.id, true),
              step: (direction) => step(block.id, direction),
              openDiagram: () => setDiagram(block.id),
            }} />
        )}
        {blockIssues.map((issue, i) => <p key={i} role="alert" className="text-xs text-danger">{issue.message}</p>)}
        {inlineComments(block.id)}
      </div>
    );
  }

  /**
   * Chỗ viết của phần lá chưa có nội dung (người học: chưa có bài làm; người soạn: mới có tiêu đề): một dòng trống, bấm
   * vào là viết, không có chữ gợi ý (yêu cầu 2026-10-22).
   */
  function emptyLeaf(section: DocSection, key: string): ReactNode {
    const last = section.blocks.at(-1) ?? section.heading;
    if (!last) return null;
    if (student && section.blocks.some((b) => b.origin === "STUDENT" && (!guided || b.type === "PARAGRAPH" || b.type === "LIST"))) return null;
    if (!student && section.blocks.length > 0) return null;
    if (readOnly) {
      return student && emptyLeafLabel
        ? <p data-testid={`empty-part-${key}`} className="py-1 text-xs text-muted">{emptyLeafLabel}</p>
        : section.blocks.length === 0 ? <div className="h-7" /> : null;
    }
    return (
      <button type="button" aria-label="Viết vào phần này" data-testid={`write-part-${key}`}
        onClick={() => insertAfter(last.id, newBlock("PARAGRAPH", origin))} className={cx("block w-full cursor-text text-left text-sm text-muted", guided ? "min-h-16 rounded-md px-2 focus-visible:ring-2 focus-visible:ring-primary" : "h-7")}>
        {guided && "Bấm để bắt đầu viết bài làm…"}
      </button>
    );
  }

  function leafContent(section: DocSection, key: string): ReactNode {
    if (!guided) return <>{section.blocks.map(renderBlock)}{emptyLeaf(section, key)}</>;
    const firstAnswer = section.blocks.findIndex((block) => block.origin === "STUDENT" || block.type === "DIAGRAM" || block.type === "TABLE");
    const splitAt = firstAnswer < 0 ? section.blocks.length : firstAnswer;
    const last = section.blocks.at(-1) ?? section.heading;
    const writable = !readOnly && inPart(last?.id);
    return <>
      {section.blocks.slice(0, splitAt).map(renderBlock)}
      <div data-testid={`answer-${key}`} className="mt-4 rounded-lg border border-line bg-white p-3 focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/15">
        <div className="mb-2 flex flex-wrap items-center justify-between gap-2 border-b border-line pb-2">
          <p className="text-sm font-semibold text-ink">Bài làm của bạn</p>
          {writable && diagramEnabled && <button type="button" className="rounded-md px-2 py-1 text-sm font-medium text-primary hover:bg-primary/5 focus-visible:ring-2 focus-visible:ring-primary"
            onClick={() => {
              if (!last) return;
              const block = newBlock("DIAGRAM", "STUDENT");
              insertAfter(last.id, block);
              setDiagram(block.id);
            }}>Thêm sơ đồ cho câu này</button>}
        </div>
        {section.blocks.slice(splitAt).map(renderBlock)}
        {emptyLeaf(section, key)}
      </div>
    </>;
  }

  function renderSection(section: DocSection): ReactNode {
    const heading = section.heading;
    const key = heading?.id ?? "DOCUMENT";
    if (!heading) {
      return (
        <Fragment key={key}>
          {guided && section.leaf && blocks.length > 0 ? leafContent(section, key) : <>
            {section.blocks.map(renderBlock)}
            {section.leaf && blocks.length > 0 && emptyLeaf(section, key)}
          </>}
          {section.children.map(renderSection)}
        </Fragment>
      );
    }
    const active = highlightBlockId === heading.id;
    const writable = !readOnly && (!student || inPart(heading.id));
    return (
      <section key={key} data-section={key} data-leaf={section.leaf || undefined}
        style={{ backgroundColor: SECTION_BG[Math.min(section.depth - 1, SECTION_BG.length - 1)] }}
        className={cx("my-2 rounded-lg px-3 py-2", active && "ring-2 ring-primary/50")}>
        {renderBlock(heading)}
        {section.leaf ? (
          // Lá: khung mang màu của tầng, ruột trắng là chỗ viết; bấm vào chỗ trống trong ruột là viết tiếp cuối phần.
          <div data-leaf-body className="mt-1 mb-1 rounded-md border border-black/10 bg-white px-3 py-1.5"
            onClick={(event) => { if (writable && event.target === event.currentTarget) continueAfter((section.blocks.at(-1) ?? heading).id); }}>
            {leafContent(section, key)}
          </div>
        ) : (
          <>
            {section.blocks.map(renderBlock)}
            {section.children.map(renderSection)}
          </>
        )}
      </section>
    );
  }

  const tree = documentTree(document);
  const currentStyle = style === "UL" || style === "OL" ? "P" : style;

  return (
    <div className="flex flex-col gap-3" data-testid="document-editor"
      onKeyDownCapture={(event) => {
        if (readOnly || !(event.ctrlKey || event.metaKey)) return;
        const key = event.key.toLowerCase();
        if (key === "z" && !event.shiftKey) { event.preventDefault(); undo(); }
        else if (key === "y" || (key === "z" && event.shiftKey)) { event.preventDefault(); redo(); }
        else if ((key === "b" || key === "i" || key === "u") && textTarget) {
          event.preventDefault();
          toggle(key === "b" ? "bold" : key === "i" ? "italic" : "underline");
        }
      }}>
      {guided && !readOnly && <p className="text-xs text-muted">Chọn vùng bài làm để định dạng chữ.{diagramEnabled && " Dùng nút trong từng câu để thêm sơ đồ đúng vị trí."}</p>}
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
                  <select aria-label="Kiểu đoạn" value={currentStyle} disabled={!style}
                    onChange={(event) => {
                      if (!target || !event.target.value) return;
                      replace(target.id, convertBlock(target, event.target.value as StyleValue), { id: target.id, at: "end" });
                    }}
                    className="h-7 w-36 rounded-md bg-transparent px-1.5 text-[13px] text-ink hover:bg-black/5 disabled:opacity-40">
                    {style === "" && <option value="">Kiểu chữ</option>}
                    {currentStyle !== "" && !AUTHOR_STYLES.includes(currentStyle) && <option value={currentStyle}>{STYLE_LABELS[currentStyle]}</option>}
                    {AUTHOR_STYLES.map((value) => <option key={value} value={value}>{STYLE_LABELS[value]}</option>)}
                  </select>
                  <Separator />
                </>
              )}
              {FLAGS.map((flag) => (
                <ToolbarButton key={flag.key} title={flag.title} aria-label={flag.title} disabled={!textTarget}
                  active={textTarget ? shownRuns.length > 0 && shownRuns.every((run) => run[flag.key]) : undefined}
                  onClick={() => toggle(flag.key)}>
                  <span className={flag.className}>{flag.label}</span>
                </ToolbarButton>
              ))}
              <ToolPopover title="Màu chữ" disabled={!textTarget}
                label={<span className="flex flex-col items-center leading-none"><span className="font-semibold">A</span><span className="mt-0.5 h-1 w-4 rounded-sm" style={{ backgroundColor: shownRuns[0]?.color ?? "#202124" }} /></span>}>
                {(close) => <Swatches colors={TEXT_COLORS} noneLabel="Mặc định" onPick={(color) => { paint("color", color); close(); }} />}
              </ToolPopover>
              <ToolPopover title="Màu tô nền" disabled={!textTarget} label={<Glyph name="highlight" />}>
                {(close) => <Swatches colors={HIGHLIGHTS} noneLabel="Không tô" onPick={(color) => { paint("highlight", color); close(); }} />}
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
                <ToolbarButton title="Chèn sơ đồ Draw.io" aria-label="Chèn sơ đồ Draw.io" data-testid="add-DIAGRAM" disabled={!insertAllowed}
                  onClick={insertDiagram}><Glyph name="diagram" /></ToolbarButton>
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
              : <button type="button" aria-label="Bắt đầu viết" data-testid="write-document"
                onClick={() => insertAfter(null, newBlock("PARAGRAPH", origin))} className="block h-16 w-full cursor-text" />)}
            {renderSection(tree)}
          </div>
          {!readOnly && blocks.length > 0 && atEndInPart && (
            <button type="button" aria-label="Viết tiếp ở cuối tài liệu" onClick={continueAtEnd} className="block h-14 w-full cursor-text" />
          )}
        </div>
        {rail && <CommentRail paper={content} comments={visible} activeKey={activeComment} />}
      </div>

      {editing && editing.type === "DIAGRAM" && (
        <DiagramEditor key={editing.id} xml={editing.content.xml} title={editing.content.alt || "Draw.io"}
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
  /** Xóa cả block (bảng xóa hàng, cột cuối cùng). */
  remove: () => void;
  step: (direction: -1 | 1) => void;
  openDiagram: () => void;
};

const CAPTION = "text-center text-xs text-muted italic";

function BlockEditor({ block, ops, imageUrl, previewUrl, onUpload, uploadError, guard, placeholder }: {
  block: DocumentBlock;
  ops: Ops;
  imageUrl?: ImageUrl;
  previewUrl?: string;
  onUpload?: (file: File) => void;
  uploadError?: string;
  /** Người học điền bảng của đề: ô của đề không xóa được. */
  guard: boolean;
  placeholder?: string;
}) {
  switch (block.type) {
    case "HEADING":
      return (
        <AutoTextarea aria-label="Nội dung tiêu đề" value={block.content.text} maxLength={500}
          className={cx(HEADING_CLASS[block.content.style] ?? HEADING_CLASS.H1, alignClass(block.content.align))}
          onChange={(event) => ops.change({ ...block, content: { ...block.content, text: event.target.value.replace(/\n/g, " ") } })}
          onKeyDown={(event) => textKeys(event, {
            // Enter ở tiêu đề mở đoạn mới bên dưới, không cắt tiêu đề.
            enter: () => ops.split(block, paragraph(block.origin)),
            removeEmpty: ops.removeEmpty,
            step: ops.step,
          })} />
      );
    case "PARAGRAPH":
      return (
        <RichText aria-label="Đoạn văn" placeholder={placeholder} runs={block.content.runs} className={cx(PARAGRAPH_CLASS, alignClass(block.content.align))}
          onChange={(runs) => ops.change({ ...block, content: { ...block.content, runs } })}
          onKey={(event, caret) => keys(event, caret, {
            enter: ({ start, end }) => {
              const [before, after] = splitRuns(block.content.runs, start, end);
              ops.split({ ...block, content: { ...block.content, runs: before } }, paragraph(block.origin, after, block.content.align));
            },
            removeEmpty: ops.removeEmpty,
            step: ops.step,
          })} />
      );
    case "LIST":
      return <ListEditor block={block} ops={ops} />;
    case "TABLE":
      return <TableEditor block={block} onChange={(next) => ops.change(next)} onRemove={ops.remove} guard={guard} />;
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
          <AutoTextarea aria-label="Chú thích ảnh" value={block.content.alt ?? ""} maxLength={500} className={CAPTION}
            onChange={(event) => ops.change({ ...block, content: { ...block.content, alt: event.target.value.replace(/\n/g, " ") } })} />
          {uploadError && <p role="alert" className="text-xs text-danger">{uploadError}</p>}
        </figure>
      );
    }
    case "DIAGRAM":
      // Khung vẽ trải hết bề ngang trang như bản demo (yêu cầu 2026-10-22); chú thích căn giữa dưới khung.
      return (
        <figure className="flex flex-col gap-1 py-1">
          <BlockView block={{ ...block, content: { ...block.content, alt: "" } }} onOpenDiagram={ops.openDiagram} />
          <AutoTextarea aria-label="Chú thích sơ đồ" value={block.content.alt ?? ""} maxLength={500} className={CAPTION}
            onChange={(event) => ops.change({ ...block, content: { ...block.content, alt: event.target.value.replace(/\n/g, " ") } })} />
        </figure>
      );
  }
}

/** Danh sách: Enter thêm mục (tách tại con trỏ), Enter ở mục trống thì thoát danh sách, Tab và Shift+Tab đổi cấp (tối đa 4). */
function ListEditor({ block, ops }: { block: Extract<DocumentBlock, { type: "LIST" }>; ops: Ops }) {
  const items = block.content.items;
  const Tag = block.content.ordered ? "ol" : "ul";
  function set(next: ListItem[], focus?: { field: number; at: "start" | "end" }) {
    ops.change({ ...block, content: { ...block.content, items: next } }, focus ? { id: block.id, ...focus } : undefined);
  }
  function sibling(field: HTMLElement, direction: -1 | 1): HTMLElement | null {
    const li = field.closest("li");
    const other = direction < 0 ? li?.previousElementSibling : li?.nextElementSibling;
    return other?.querySelector<HTMLElement>("[data-editable]") ?? null;
  }
  function itemKeys(event: KeyboardEvent<HTMLDivElement>, caret: Caret, item: ListItem, i: number) {
    const field = event.currentTarget;
    if (event.key === "Tab") {
      event.preventDefault();
      const level = Math.min(4, Math.max(0, (item.level ?? 0) + (event.shiftKey ? -1 : 1)));
      set(items.map((x, j) => (j === i ? { ...x, level } : x)));
    } else if (event.key === "Enter") {
      event.preventDefault();
      if (caret.length === 0) {
        const rest = items.filter((_, j) => j !== i);
        if (rest.length === 0) ops.change(convertBlock(block, "P"), { id: block.id, at: "start" });
        else ops.split({ ...block, content: { ...block.content, items: rest } }, paragraph(block.origin, [{ ...formatOf(item.runs[0]), text: "" }]));
        return;
      }
      const [before, after] = splitRuns(item.runs, caret.start, caret.end);
      set([...items.slice(0, i), { ...item, runs: before }, { level: item.level ?? 0, runs: after }, ...items.slice(i + 1)], { field: i + 1, at: "start" });
    } else if (event.key === "Backspace" && caret.length === 0) {
      event.preventDefault();
      if (items.length === 1) ops.removeEmpty();
      else set(items.filter((_, j) => j !== i), { field: Math.max(0, i - 1), at: i > 0 ? "end" : "start" });
    } else if (event.key === "ArrowUp" && caret.start === 0 && caret.end === 0) {
      event.preventDefault();
      const other = sibling(field, -1);
      if (other) caretTo(other, "end"); else ops.step(-1);
    } else if (event.key === "ArrowDown" && caret.start === caret.length) {
      event.preventDefault();
      const other = sibling(field, 1);
      if (other) caretTo(other, "start"); else ops.step(1);
    }
  }
  return (
    <Tag className={cx(block.content.ordered ? "list-decimal" : "list-disc", "pl-6", PARAGRAPH_CLASS, alignClass(block.content.align))}>
      {items.map((item, i) => (
        <li key={i} style={{ marginLeft: `${(item.level ?? 0) * 24}px` }}>
          <RichText aria-label={`Mục ${i + 1}`} runs={item.runs} className={cx(PARAGRAPH_CLASS, alignClass(block.content.align))}
            onChange={(runs) => set(items.map((x, j) => (j === i ? { ...x, runs } : x)))}
            onKey={(event, caret) => itemKeys(event, caret, item, i)} />
        </li>
      ))}
    </Tag>
  );
}

/**
 * Bảng kiểu Google Docs, nút như bản demo (yêu cầu 2026-10-22): ô vuông "+ −" nổi đè lên chỗ xung quanh, không chiếm chỗ
 * của trang; của cột hiện trên đầu cột, của hàng hiện bên trái hàng, chỉ ở cột, hàng đang trỏ tới; nút ✕ xóa bảng ở góc
 * trên trái, cùng hàng với các nút của cột. "−" khi bảng còn một hàng hoặc một cột là xóa cả bảng. {@code guard} (người
 * học điền bảng của đề): ô thêm vào mang cờ {@code added}; hàng, cột của đề vẫn có nút nhưng "−" và ✕ mờ, không bấm được.
 */
function TableEditor({ block, onChange, onRemove, guard }: {
  block: Extract<DocumentBlock, { type: "TABLE" }>;
  onChange: (block: DocumentBlock) => void;
  onRemove: () => void;
  guard: boolean;
}) {
  const [hoverCol, setHoverCol] = useState<number | null>(null);
  const [focusCol, setFocusCol] = useState<number | null>(null);
  const activeCol = hoverCol ?? focusCol;
  const rows = block.content.rows;
  const columns = Math.max(1, ...rows.map((row) => row.length));
  const header = !!block.content.headerRow;
  const empty = (): TableCell => ({ runs: [{ text: "" }] });
  const fresh = (): TableCell => (guard ? { runs: [{ text: "" }], added: true } : empty());
  const full = (cells: TableCell[]) => Array.from({ length: columns }, (_, j) => cells[j] ?? empty());
  function set(next: TableCell[][]) {
    onChange({ ...block, content: { ...block.content, rows: next } });
  }
  const canDeleteRow = (r: number) => !guard || rows[r]!.every((cell) => cell.added);
  const canDeleteColumn = (c: number) => !guard || rows.every((cells) => !cells[c] || cells[c]!.added);
  const insertRow = (r: number) => set([...rows.slice(0, r + 1), Array.from({ length: columns }, fresh), ...rows.slice(r + 1)]);
  const insertColumn = (c: number) => set(rows.map((cells) => { const all = full(cells); return [...all.slice(0, c + 1), fresh(), ...all.slice(c + 1)]; }));
  const deleteRow = (r: number) => (rows.length <= 1 && !guard ? onRemove() : set(rows.filter((_, i) => i !== r)));
  const deleteColumn = (c: number) => (columns <= 1 && !guard ? onRemove() : set(rows.map((cells) => full(cells).filter((_, j) => j !== c))));
  const rowLabel = (r: number) => (!canDeleteRow(r) ? "Hàng của đề" : rows.length <= 1 && !guard ? "Xóa bảng" : "Xóa hàng");
  const columnLabel = (c: number) => (!canDeleteColumn(c) ? "Cột của đề" : columns <= 1 && !guard ? "Xóa bảng" : "Xóa cột");
  return (
    <div className="py-1" onMouseLeave={() => setHoverCol(null)}
      onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setFocusCol(null); }}>
      <table className="w-full table-fixed border-collapse">
        <tbody>
          {rows.map((row, r) => (
            <tr key={r} className="group/row">
              {Array.from({ length: columns }, (_, c) => (
                <td key={c} onMouseEnter={() => setHoverCol(c)}
                  className={cx("relative border border-line-strong align-top break-words", header && r === 0 && "bg-canvas")}>
                  <AutoTextarea aria-label={`Ô ${r + 1}-${c + 1}`} value={runsText(row[c]?.runs)} onFocus={() => setFocusCol(c)}
                    className={cx("px-2 py-1.5 text-[13px] leading-snug text-ink", header && r === 0 && "font-semibold")}
                    onChange={(event) => set(rows.map((cells, i) => i !== r ? cells : full(cells).map((cell, j) =>
                      j === c ? { ...cell, runs: [{ ...(cell.runs[0] ?? { text: "" }), text: event.target.value }] } : cell)))} />
                  {r === 0 && (
                    // Đệm thay cho lề để không có khe giữa ô và nút: rê từ ô lên nút không làm nút ẩn đi.
                    <span data-testid={`column-tools-${c}`}
                      className={cx("absolute bottom-full left-1/2 z-10 flex -translate-x-1/2 gap-[3px] pb-[3px] transition-opacity",
                        activeCol === c ? "opacity-100" : "pointer-events-none opacity-0")}>
                      {columns < 30 && <Square label="Chèn cột bên phải" onClick={() => insertColumn(c)}>+</Square>}
                      <Square danger label={columnLabel(c)} disabled={!canDeleteColumn(c)} onClick={() => deleteColumn(c)}>−</Square>
                    </span>
                  )}
                  {c === 0 && (
                    <span data-testid={`row-tools-${r}`}
                      className="pointer-events-none absolute top-1/2 right-full z-10 flex -translate-y-1/2 gap-[3px] pr-1.5 opacity-0 transition-opacity group-focus-within/row:pointer-events-auto group-focus-within/row:opacity-100 group-hover/row:pointer-events-auto group-hover/row:opacity-100">
                      {rows.length < 200 && <Square label="Chèn hàng bên dưới" onClick={() => insertRow(r)}>+</Square>}
                      <Square danger label={rowLabel(r)} disabled={!canDeleteRow(r)} onClick={() => deleteRow(r)}>−</Square>
                    </span>
                  )}
                  {r === 0 && c === 0 && (
                    // ✕ xóa bảng ở góc trên trái: cùng hàng với nút của cột, thẳng cột với nút của hàng; bảng của đề thì mờ.
                    <span className="pointer-events-none absolute right-full bottom-full z-10 flex pr-1.5 pb-[3px] opacity-0 transition-opacity group-focus-within/block:pointer-events-auto group-focus-within/block:opacity-100 group-hover/block:pointer-events-auto group-hover/block:opacity-100">
                      <Square danger label={guard ? "Bảng của đề" : "Xóa bảng"} disabled={guard} onClick={onRemove}>✕</Square>
                    </span>
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
