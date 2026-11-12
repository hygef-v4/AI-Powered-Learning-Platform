"use client";

import { SectionNumber } from "./SectionNumber";
import {
  Fragment,
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type KeyboardEvent,
  type MouseEvent as ReactMouseEvent,
  type ReactNode,
  type TextareaHTMLAttributes,
} from "react";
import { createPortal } from "react-dom";
import { Icon, cx, type IconName } from "@/shared/ui";
import { CommentRail, useWideLayout, type DocumentComment } from "./CommentRail";
import { DiagramEditor } from "./DiagramEditor";
import { BlockView, HEADING_CLASS, PARAGRAPH_CLASS, RunsView, alignClass, type ImageUrl } from "./DocumentViewer";
import { RichText, getCaret, setCaret, type Caret } from "./RichText";
import { colorRange, formatOf, joinLines, runLines, sliceRuns, splitRuns, toggleFlagRange, type RunFlag } from "./runs";
import {
  blockText,
  computeParts,
  documentTree,
  newBlock,
  newId,
  runsText,
  sectionsOf,
  type TitleContent,
  MAX_SECTION_DEPTH,
  sectionNumber,
  underOf,
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

/** Chữ gợi ý ở ô Bài làm của bạn (yêu cầu 2026-11-10), ngắn như chữ gợi ý của người soạn. */
const ANSWER_PLACEHOLDER = "Viết câu trả lời…";

/** Mục có chữ riêng (ngoài các mục con): có khối khác đoạn trống. */
function hasOwnText(section: DocSection): boolean {
  return [...section.blocks, ...section.after].some((block) => block.type !== "PARAGRAPH"
    || block.content.runs.some((run) => run.text.trim() !== ""));
}

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
  /** Dòng thông tin chỉ đọc, gắn theo ID tiêu đề của từng phần. */
  sectionBylines?: Readonly<Record<string, string>>;
  /** Nút thao tác của từng phần (theo ID tiêu đề), nằm bên phải, ngang hàng tiêu đề. */
  sectionActions?: Readonly<Record<string, ReactNode>>;
  /** Người học chỉ viết được trong các phần này (ID tiêu đề phần lá); không có thì viết được mọi phần. */
  writableParts?: ReadonlySet<string>;
  /** Số thứ tự của phần theo ID tiêu đề, thay số tự đánh (trang chỉ hiện một số phần mà vẫn giữ số như cả tài liệu). */
  sectionNumbers?: Readonly<Record<string, string>>;
  /** Cách đọc lúc mở: cả trang (mặc định) hoặc từng phần. */
  initialView?: "page" | "step";
  /** Nút thêm ở cuối thanh công cụ; có nút thì thanh công cụ vẫn hiện khi chỉ đọc. */
  toolbarExtra?: ReactNode;
  /**
   * Tên bài (khung đề): trình soạn vẽ ở đầu trang giấy để định dạng được bằng thanh công cụ; định dạng lưu ở
   * {@code document.title}, chữ thuần báo qua {@code onText} (yêu cầu 2026-11-04).
   */
  titleField?: { text: string; placeholder?: string; invalid?: boolean; readOnly?: boolean; onText: (text: string) => void };
  /** Chỗ trên header của trang để đặt nút chọn cách đọc (Cả trang / Từng phần); không có thì nút nằm trên thanh công cụ. */
  viewSlot?: HTMLElement | null;
  /** Chỉ đọc: nút đặt giữa thanh công cụ, thẳng giữa trang giấy (vd. chuyển lượt nộp). */
  toolbarCenter?: ReactNode;
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
  fullWidth?: boolean;
  matchCommentHeight?: boolean;
  /**
   * Trang thay cả tài liệu từ ngoài (AI soạn ghi thẳng lên trang): mỗi {@code id} mới đưa {@code before} vào lịch sử để
   * Ctrl+Z quay về bản trước, Ctrl+Y tiến lại (yêu cầu 2026-11-05).
   */
  externalEdit?: { id: number; before: DocumentModel } | null;
};

/** Chỉ giữ các block thuộc phần được viết ({@code writable}: ID tiêu đề phần lá); không giới hạn thì giữ nguyên. */
function writableOnly(sections: Map<string, string>, writable: ReadonlySet<string> | undefined): Map<string, string> {
  if (!writable) return sections;
  return new Map([...sections].filter(([, leaf]) => writable.has(leaf)));
}

type StyleValue = "P" | "UL" | "OL" | HeadingStyle;
/** Nơi đặt con trỏ sau khi tài liệu đổi: ô nhập đầu, cuối, hoặc ô thứ {@code field} của block. */
type FocusTarget = { id: string; at: "start" | "end"; field?: number };
/** Vùng chọn trong ô chữ có định dạng: block, ô thứ {@code field} (đoạn: 0; danh sách: mục), vị trí theo ký tự. */
type Selected = { blockId: string; field: number; start: number; end: number };

/*
 * Nghiệp vụ soạn (yêu cầu 2026-11-02): chỉ có một loại khung là Mục (Tiêu đề 1–6, đánh số I., I.1., I.1.a., I.1.a.aa.); tiêu đề mục
 * chỉ ghi chữ, nội dung viết trong Ô văn bản (giấy trắng, luôn có Dòng cuối ở đáy). Nút "+ Mục" trên thanh công cụ thêm mục
 * cùng tầng; thanh "+" ở đáy mỗi ô văn bản (cả ô ngoài cùng) thêm mục nằm trong ô đó.
 */

const FLAGS: { key: RunFlag; label: string; title: string; className: string }[] = [
  { key: "bold", label: "B", title: "Đậm (Ctrl+B)", className: "font-bold" },
  { key: "italic", label: "I", title: "Nghiêng (Ctrl+I)", className: "font-serif italic" },
  { key: "underline", label: "U", title: "Gạch chân (Ctrl+U)", className: "underline" },
  { key: "strike", label: "S", title: "Gạch ngang", className: "line-through" },
];

const TEXT_COLORS = ["#202124", "#5F6368", "#C5221F", "#E37400", "#188038", "#1A73E8", "#9334E6", "#B80672"];
const HIGHLIGHTS = ["#FFF475", "#CCFF90", "#A7FFEB", "#AECBFA", "#D7AEFB", "#FDCFE8", "#E8EAED"];
/** Màu của phần theo tầng (yêu cầu 2026-10-22): cùng tầng cùng màu, tầng sâu đậm hơn; lá thì khung mang màu tầng, ruột trắng. */
// Nền các tầng mục: biến theme (globals.css) đậm dần theo tầng, tự đổi theo sáng/tối.
const SECTION_BG = ["var(--c-tier-1)", "var(--c-tier-2)", "var(--c-tier-3)", "var(--c-tier-4)"];
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
const IMAGE_TYPES = ".png,.jpg,.jpeg";
const HISTORY_GAP_MS = 700;

/** Run của tiêu đề mục: tiêu đề cũ chỉ có chữ thì coi như một run. */
function headingRuns(block: Extract<DocumentBlock, { type: "HEADING" }>): Run[] {
  return block.content.runs ?? [{ text: block.content.text }];
}

function runsOf(block: DocumentBlock | undefined): Run[] {
  if (block?.type === "HEADING") return headingRuns(block);
  if (block?.type === "PARAGRAPH") return block.content.runs;
  if (block?.type === "LIST") return block.content.items.flatMap((item) => item.runs);
  return [];
}

function tableCell(block: Extract<DocumentBlock, { type: "TABLE" }>, field: number): TableCell | undefined {
  const columns = tableColumns(block);
  return block.content.rows[Math.floor(field / columns)]?.[field % columns];
}

/** Số cột của bảng (hàng thiếu ô vẫn vẽ đủ cột): ô thứ {@code field} là hàng {@code field / cột}, cột {@code field % cột}. */
function tableColumns(block: Extract<DocumentBlock, { type: "TABLE" }>): number {
  return Math.max(1, ...block.content.rows.map((row) => row.length));
}

/** Run của ô thứ {@code field} trong block chữ (đoạn: ô 0; danh sách: mục thứ field). */
function fieldRuns(block: DocumentBlock, field: number): Run[] {
  if (block.type === "HEADING") return headingRuns(block);
  if (block.type === "PARAGRAPH") return block.content.runs;
  if (block.type === "LIST") return block.content.items[field]?.runs ?? [];
  if (block.type === "TABLE") {
    const columns = tableColumns(block);
    return block.content.rows[Math.floor(field / columns)]?.[field % columns]?.runs ?? [];
  }
  return [];
}

function mapField(block: DocumentBlock, field: number, change: (runs: Run[]) => Run[]): DocumentBlock {
  if (block.type === "HEADING") return withHeadingRuns(block, change(headingRuns(block)));
  if (block.type === "PARAGRAPH") return { ...block, content: { ...block.content, runs: change(block.content.runs) } };
  if (block.type === "LIST") {
    return { ...block, content: { ...block.content, items: block.content.items.map((item, i) => (i === field ? { ...item, runs: change(item.runs) } : item)) } };
  }
  if (block.type === "TABLE") return mapCell(block, field, (cell) => ({ ...cell, runs: change(cell.runs) }));
  return block;
}

/** Đổi ô thứ {@code field} của bảng (hàng rồi cột). */
function mapCell(block: Extract<DocumentBlock, { type: "TABLE" }>, field: number, change: (cell: TableCell) => TableCell): DocumentBlock {
  const columns = tableColumns(block);
  const row = Math.floor(field / columns);
  const column = field % columns;
  return { ...block, content: { ...block.content, rows: block.content.rows.map((cells, r) => r !== row ? cells
    : Array.from({ length: Math.max(cells.length, column + 1) }, (_, c) => {
      const cell = cells[c] ?? { runs: [{ text: "" }] };
      return c === column ? change(cell) : cell;
    })) } };
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

/** Tiêu đề giữ cả chữ thuần (đánh số, dàn ý, xuất file dùng) và run có định dạng. */
function withHeadingRuns(block: Extract<DocumentBlock, { type: "HEADING" }>, runs: Run[]): DocumentBlock {
  return { ...block, content: { ...block.content, runs, text: runsText(runs).replace(/\s*\n\s*/g, " ").slice(0, 500) } };
}

function mapRuns(block: DocumentBlock, change: (run: Run) => Run): DocumentBlock {
  if (block.type === "HEADING") return withHeadingRuns(block, headingRuns(block).map(change));
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
      className={cx("block w-full resize-none overflow-hidden border-0 bg-transparent p-0 outline-none placeholder:text-muted", className)}
      {...rest} />
  );
}

function fit(field: HTMLTextAreaElement) {
  // Thu về auto để đo lại chiều cao; giữ vị trí cuộn vì trang có thể ngắn lại trong chốc lát.
  const y = window.scrollY;
  field.style.height = "auto";
  field.style.height = `${field.scrollHeight + field.offsetHeight - field.clientHeight}px`;
  if (window.scrollY !== y) window.scrollTo({ top: y });
}

/** Nút trên thanh công cụ của trang soạn; giữ con trỏ trong ô đang gõ khi bấm. */
export function ToolbarButton({ icon, active, className, children, ...rest }: ButtonHTMLAttributes<HTMLButtonElement> & {
  icon?: IconName;
  active?: boolean;
}) {
  return (
    <button type="button" aria-pressed={active} onMouseDown={(event) => event.preventDefault()}
      className={cx("inline-flex h-7 min-w-7 items-center justify-center gap-1 rounded-md px-1.5 text-[13px] font-medium whitespace-nowrap text-body transition-colors enabled:cursor-pointer enabled:hover:bg-line-strong enabled:focus-visible:bg-line-strong disabled:cursor-default disabled:opacity-35",
        active && "bg-primary-soft text-ink", className)}
      {...rest}>
      {icon && <Icon name={icon} size={18} />}
      {children}
    </button>
  );
}

const EMPTY_RUNS: Run[] = [{ text: "" }];
const TITLE_CLASS = "font-display text-[28px] leading-tight tracking-[-0.025em] text-ink";

type TailStyle = { format: Omit<Run, "text">; align?: Align; list?: "UL" | "OL" };
const EMPTY_TAIL: TailStyle = { format: {} };

/** Khối (không phải tiêu đề) mang dấu thuộc lại mục cấp {@code level} sau các mục con. */
function withUnder(block: DocumentBlock, level: number): DocumentBlock {
  return block.type === "HEADING" ? block : { ...block, content: { ...block.content, under: level } } as DocumentBlock;
}

/** Run của tên bài: định dạng trong JSON nếu cùng chữ với tên đang có, không thì chữ thuần (tên đổi ở chỗ khác, bài cũ). */
function currentTitleRuns(document: DocumentModel, text: string): Run[] {
  const runs = document.title?.runs;
  return runs && runsText(runs) === text ? runs : [{ text }];
}

/** Dòng cuối đã chọn kiểu danh sách: hiện dấu đầu dòng trước chỗ gõ. */
function TailList({ list, children }: { list?: "UL" | "OL"; children: ReactNode }) {
  if (!list) return <>{children}</>;
  const Tag = list === "OL" ? "ol" : "ul";
  return <Tag className={cx(list === "OL" ? "list-decimal" : "list-disc", "pl-6")}><li>{children}</li></Tag>;
}

/** Danh sách trong ô bảng: mỗi dòng của ô thêm (hoặc bỏ, nếu dòng nào cũng có) dấu "• " hay "1. ", "2. "… ở đầu. */
function toggleCellList(runs: Run[], kind: "UL" | "OL"): Run[] {
  const marker = kind === "UL" ? /^• / : /^\d+\. /;
  const lines = runLines(runs);
  const marked = lines.every((line) => marker.test(runsText(line)));
  const plain = (line: Run[]) => {
    const text = runsText(line);
    const found = /^• |^\d+\. /.exec(text);
    return found ? sliceRuns(line, found[0].length, text.length) : line;
  };
  return joinLines(lines.map((line, i) => {
    const rest = plain(line);
    if (marked) return rest;
    const prefix = kind === "UL" ? "• " : `${i + 1}. `;
    return [{ ...formatOf(rest[0]), text: prefix }, ...rest];
  }));
}

function ViewButton({ label, on, onClick, children }: { label: string; on: boolean; onClick: () => void; children: ReactNode }) {
  return (
    <button type="button" role="radio" aria-checked={on} aria-label={label} title={label} onClick={onClick} onMouseDown={(event) => event.preventDefault()}
      className={cx("flex size-7 items-center justify-center rounded-md transition-colors", on ? "bg-surface text-ink shadow-sm" : "text-muted hover:text-ink")}>
      {children}
    </button>
  );
}

function PageGlyph() {
  return (
    <svg aria-hidden viewBox="0 0 24 24" width={17} height={17} fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
      <rect x="5" y="3" width="14" height="18" rx="2" /><path d="M8.5 8h7M8.5 12h7M8.5 16h4" />
    </svg>
  );
}

function StepGlyph() {
  return (
    <svg aria-hidden viewBox="0 0 24 24" width={17} height={17} fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
      <rect x="6" y="5" width="12" height="14" rx="2" /><path d="M3 9v6M21 9v6" />
    </svg>
  );
}

/**
 * Nút thêm câu hỏi: dấu "+" kèm chữ kéo dài hết bề ngang, luôn hiện ở đáy mỗi ô văn bản (yêu cầu 2026-11-10); chữ nói rõ
 * phải thêm câu hỏi để sinh viên làm, không chỉ gõ đề bài.
 */
function AddBar({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button type="button" aria-label={label} title={label} onClick={onClick}
      className="flex h-7 w-full items-center justify-center gap-1 rounded-md text-xs font-medium text-muted transition-colors hover:bg-primary-soft hover:text-primary-ink focus-visible:bg-primary-soft focus-visible:text-primary-ink">
      <Icon name="add" size={16} />{label}
    </button>
  );
}

function Separator() {
  return <span aria-hidden className="mx-1 h-5 w-px bg-line-strong" />;
}

type GlyphName = "undo" | "redo" | "alignLeft" | "alignCenter" | "alignRight" | "justify" | "bullets" | "numbers"
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
        <div className="absolute top-8 left-0 z-40 rounded-xl border border-line bg-surface p-2 shadow-[var(--shadow-float)]" onMouseDown={(event) => event.preventDefault()}>
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
            className="size-4 rounded-full border border-line-strong hover:scale-125" style={{ backgroundColor: color }} />
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
              className={cx("size-[18px] rounded-[2px] border p-0", on ? "border-primary bg-primary-soft" : "border-line-strong bg-sunken")} />
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
      className={cx("inline-flex size-[18px] flex-none items-center justify-center rounded-[2px] border border-line-strong bg-surface p-0 text-xs leading-none text-body disabled:cursor-not-allowed disabled:opacity-40",
        danger ? "enabled:hover:border-danger enabled:hover:text-danger" : "enabled:hover:border-primary enabled:hover:text-primary-ink")}>
      {children}
    </button>
  );
}

/** Nút xóa cả block (sơ đồ, ảnh) nổi ở góc trên trái khi trỏ vào block (yêu cầu 2026-10-22, thay menu "⋮"); không được xóa thì mờ. */
/** Nút của mục ở mép trái, lệch ra ngoài khung để không chạm các mục con: ✕ xóa, bên dưới ↑ ↓ đổi chỗ trong cùng cha. */
function SectionTools({ shown, onRemove, onUp, onDown }: { shown: boolean; onRemove: () => void; onUp?: () => void; onDown?: () => void }) {
  return (
    <span className={cx("absolute top-0 -left-7 z-10 flex flex-col gap-1 py-1 pr-2.5 pl-1 transition-opacity", shown ? "opacity-100" : "pointer-events-none opacity-0")}>
      <Square danger label="Xóa câu hỏi" onClick={onRemove}>✕</Square>
      <Square label="Đưa câu hỏi lên" disabled={!onUp} onClick={() => onUp?.()}>↑</Square>
      <Square label="Đưa câu hỏi xuống" disabled={!onDown} onClick={() => onDown?.()}>↓</Square>
    </span>
  );
}

const CORNER_REVEAL = {
  block: "group-focus-within/block:pointer-events-auto group-focus-within/block:opacity-100 group-hover/block:pointer-events-auto group-hover/block:opacity-100",
  section: "group-focus-within/section:pointer-events-auto group-focus-within/section:opacity-100 group-hover/section:pointer-events-auto group-hover/section:opacity-100",
};

function CornerDelete({ label, disabled = false, scope = "block", always = false, onClick }: { label: string; disabled?: boolean; scope?: keyof typeof CORNER_REVEAL; always?: boolean; onClick: () => void }) {
  return (
    <span className={cx("absolute -top-2 -left-2 z-10 transition-opacity", always ? "opacity-60 hover:opacity-100" : "pointer-events-none opacity-0", CORNER_REVEAL[scope])}>
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
/** Bóng đặc màu nền trang phủ khe giữa thanh trên cùng và thanh công cụ dính, để nội dung cuộn không lộ lên trên thanh công cụ. */
const TOOLBAR_COVER = "shadow-[0_-0.75rem_0_0.25rem_var(--c-bg)]";

export function DocumentEditor({
  document, onChange, mode, diagramEnabled, readOnly = false, imageUrl, uploadImage, issues = [],
  header, sectionBylines, sectionActions, writableParts, sectionNumbers, initialView = "page", titleField, toolbarExtra, viewSlot, toolbarCenter, comments, activeComment,
  highlightBlockId, onFocusBlock, emptyLeafLabel, answerGuidance = false, fullWidth = false,
  matchCommentHeight = false,
  externalEdit = null,
}: Props) {
  const origin: BlockOrigin = mode === "author" ? "TEACHER" : "STUDENT";
  const [previews, setPreviews] = useState<Record<string, string>>({});
  const [diagram, setDiagram] = useState<string | null>(null);
  const [uploadError, setUploadError] = useState<Record<string, string>>({});
  const [focusedId, setFocusedId] = useState<string | null>(null);
  /** Con trỏ đang ở dòng cuối của một ô trắng: thêm ngay sau {@code afterId}, trong phần {@code sectionKey}. */
  const [cursorLine, setCursorLine] = useState<{ afterId: string | null; sectionKey: string; tailKey: string } | null>(null);
  /** Định dạng, căn lề, kiểu danh sách đặt sẵn cho dòng cuối (chưa có chữ); gõ vào thì đoạn mới mang theo. */
  const [tailStyles, setTailStyles] = useState<Record<string, TailStyle>>({});
  const tailRuns = (key: string): Run[] => {
    const format = tailStyles[key]?.format;
    return format && Object.values(format).some((value) => value !== undefined) ? [{ ...format, text: "" }] : EMPTY_RUNS;
  };
  /** Con trỏ ở phần đầu trang do trang đưa vào (tên bài): thanh công cụ chỉ còn hoàn tác, làm lại. */
  const [inHeader, setInHeader] = useState(false);
  /** Kéo thả mục: mục đang kéo và chỗ thả (trước, sau một mục cùng cha). */
  const [dragKey, setDragKey] = useState<string | null>(null);
  /** Mục đang rê chuột (mục trong cùng), để chỉ mục đó hiện nút ✕ ↑ ↓. */
  const [hoverSection, setHoverSection] = useState<string | null>(null);
  const hoverTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  /** Rê vào một mục: hiện nút ngay; rời ra: đợi một lúc mới ẩn, để kịp đưa chuột tới nút ở ngoài khung. */
  function hoverOn(key: string) {
    if (hoverTimer.current) clearTimeout(hoverTimer.current);
    hoverTimer.current = null;
    if (hoverSection !== key) setHoverSection(key);
  }
  function hoverOff() {
    if (hoverTimer.current || !hoverSection) return;
    hoverTimer.current = setTimeout(() => { hoverTimer.current = null; setHoverSection(null); }, 700);
  }
  const [dropAt, setDropAt] = useState<{ key: string; place: "before" | "after" } | null>(null);
  const [selected, setSelected] = useState<Selected | null>(null);
  const [view, setView] = useState<"page" | "step">(initialView);
  const [stepAt, setStepAt] = useState(0);
  const latestLeaves = useRef<DocSection[]>([]);
  /** Thêm mục con từ sidebar của bài (sự kiện `document:add-part`); null là mục ngoài cùng. Chỉ người soạn. */
  const addPartFrom = useRef<(parentId: string | null) => void>(() => {});
  const latestBlocks = useRef<DocumentBlock[]>([]);
  const [history, setHistory] = useState({ past: 0, future: 0 });
  const paper = useRef<HTMLDivElement>(null);
  const content = useRef<HTMLDivElement>(null);
  const measureToolbar = useCallback((toolbar: HTMLDivElement | null) => {
    if (!toolbar) return;
    const measure = () => toolbar.parentElement?.style.setProperty("--doc-toolbar-height", `${toolbar.offsetHeight}px`);
    measure();
    if (typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(measure);
    observer.observe(toolbar);
    return () => observer.disconnect();
  }, []);
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
  const seenExternal = useRef(externalEdit?.id ?? null);
  useEffect(() => {
    if (!externalEdit || externalEdit.id === seenExternal.current) return;
    seenExternal.current = externalEdit.id;
    past.current = [...past.current.slice(-99), externalEdit.before];
    future.current = [];
    lastPush.current = 0;
    setHistory({ past: past.current.length, future: 0 });
  }, [externalEdit]);
  useEffect(() => () => Object.values(previewsRef.current).forEach((url) => URL.revokeObjectURL(url)), []);

  useEffect(() => {
    const target = pendingFocus.current;
    if (!target) return;
    pendingFocus.current = null;
    focusField(paper.current, target);
  });

  // Sidebar của bài bấm vào một phần (sự kiện `document:show-part`): ở chế độ Từng phần thì chuyển sang phần chứa nó, rồi cuộn tới.
  useEffect(() => {
    const show = (event: Event) => {
      const id = (event as CustomEvent<{ headingId: string }>).detail?.headingId;
      if (!id) return;
      // Mục được bấm trên Mục lục là mục đang xem (sáng trên Mục lục), kể cả khi trang không cuộn tới được mép trên (mục
      // cuối thấp); giữ tới khi người dùng tự cuộn (yêu cầu 2026-11-05).
      pinned.current = id;
      // Mỗi mục là một trang: mở đúng trang của mục được bấm (bấm 1 khi đang ở 1.2 thì về trang 1).
      // "DOCUMENT": văn bản ngoài mọi mục (trang gốc), cuộn tới khối đầu tiên của nó.
      const root = id === "DOCUMENT";
      const pages = latestLeaves.current;
      let page = pages.findIndex((leaf) => (root ? !leaf.heading : leaf.heading?.id === id));
      if (latestView.current === "step" && page >= 0) {
        // Mục cha chưa có chữ riêng (chỉ chứa mục con): mở trang mục con đầu tiên, ví dụ bấm 1.2 thì sang 1.2.1; trang ngay
        // sau mục cha luôn là mục con đầu của nó (yêu cầu 2026-11-05).
        while (pages[page]?.heading && pages[page]!.children.length > 0 && !hasOwnText(pages[page]!) && pages[page + 1]) page++;
        pinned.current = pages[page]!.heading?.id ?? "DOCUMENT";
        // Ở chế độ Từng phần luôn chuyển trang rồi lên đầu trang, kể cả khi mục cha đang hiện trên trang mục con.
        setStepAt(page);
        requestAnimationFrame(() => {
          const top = paper.current ? paper.current.getBoundingClientRect().top + window.scrollY - 140 : 0;
          if (typeof window.scrollTo === "function") window.scrollTo({ top: Math.max(0, top), behavior: "smooth" });
        });
        return;
      }
      if (page >= 0) setStepAt(page);
      const first = root ? latestLeaves.current.find((leaf) => !leaf.heading) : undefined;
      const scrollId = root ? (first?.blocks[0] ?? first?.after[0])?.id : id;
      requestAnimationFrame(() => paper.current?.querySelector(`[data-block-id="${scrollId}"]`)?.scrollIntoView({ behavior: "smooth", block: "center" }));
    };
    window.addEventListener("document:show-part", show);
    return () => window.removeEventListener("document:show-part", show);
  }, []);
  // Báo cho Mục lục mục đang xem (sự kiện `document:current-part`): cả trang thì là mục cuối cùng đã cuộn qua mép trên.
  const lastCurrent = useRef<string | null>(null);
  const latestView = useRef(view);
  useEffect(() => { latestView.current = view; }, [view]);
  const pinned = useRef<string | null>(null);
  const reportCurrent = useRef<() => void>(() => {});
  // Người dùng tự cuộn (con lăn, chạm, phím) thì bỏ mục ghim, lại theo chỗ cuộn tới.
  useEffect(() => {
    const release = (event: Event) => {
      if (event instanceof KeyboardEvent && !["ArrowUp", "ArrowDown", "PageUp", "PageDown", "Home", "End", " "].includes(event.key)) return;
      if (event instanceof KeyboardEvent && (event.target as HTMLElement | null)?.closest?.("[contenteditable], textarea, input")) return;
      pinned.current = null;
    };
    window.addEventListener("wheel", release, { passive: true });
    window.addEventListener("touchmove", release, { passive: true });
    window.addEventListener("keydown", release);
    return () => { window.removeEventListener("wheel", release); window.removeEventListener("touchmove", release); window.removeEventListener("keydown", release); };
  }, []);
  useEffect(() => {
    let frame = 0;
    const onScroll = () => {
      if (frame) return;
      frame = requestAnimationFrame(() => { frame = 0; reportCurrent.current(); });
    };
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => { window.removeEventListener("scroll", onScroll); if (frame) cancelAnimationFrame(frame); };
  }, []);
  // Sau mỗi lần vẽ (đổi trang, đổi chế độ xem, thêm bớt mục) báo lại mục đang xem; trùng mục cũ thì không báo.
  useEffect(() => { reportCurrent.current(); });
  useEffect(() => {
    const add = (event: Event) => addPartFrom.current((event as CustomEvent<{ parentId: string | null }>).detail?.parentId ?? null);
    window.addEventListener("document:add-part", add);
    return () => window.removeEventListener("document:add-part", add);
  }, []);

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
  latestBlocks.current = blocks;
  const student = mode === "student";
  const authoring = !student && !readOnly;
  const guided = student && answerGuidance;
  // Người học chỉ viết trong phần: block nằm từ tiêu đề lá tới tiêu đề kế; khung không có tiêu đề thì cả bài.
  const sections = student ? writableOnly(sectionsOf(document, computeParts(document)), writableParts) : null;
  const inPart = (blockId: string | undefined) => !sections || (!!blockId && sections.has(blockId));
  // Tài liệu trống không có tiêu đề nên cả bài là một phần; còn lại cuối tài liệu luôn thuộc tiêu đề cuối (là lá).
  const atEndInPart = blocks.length === 0 || inPart(blocks.at(-1)!.id);
  // Bảng của người soạn trong phần: người học điền thẳng vào (yêu cầu 2026-10-22), không xóa bảng.
  const fillable = (block: DocumentBlock) => student && !readOnly && block.origin === "TEACHER" && block.type === "TABLE" && inPart(block.id);
  const locked = (block: DocumentBlock) => readOnly || (student && block.origin === "TEACHER" && !fillable(block));
  const focused = blocks.find((b) => b.id === focusedId);
  // Thanh công cụ theo chỗ con trỏ (yêu cầu 2026-11-04): + Mục chỉ ở đoạn văn và dòng cuối; chèn bảng, ảnh, sơ đồ ở đoạn văn,
  // danh sách, dòng cuối; không ở tên bài, tiêu đề mục, ô bảng, ảnh, sơ đồ.
  const onText = !focused || focused.type === "PARAGRAPH";
  // "+ Câu hỏi" (yêu cầu 2026-11-10): con trỏ không ở đâu, ở tên bài hay ở tiêu đề câu hỏi thì thêm câu hỏi xuống cuối;
  // ở đoạn văn, dòng cuối thì thêm tại con trỏ; chỉ khóa ở chỗ không được thêm (bảng, ảnh, sơ đồ, danh sách).
  const addAtEnd = inHeader || (cursorLine === null && (!focused || focused.type === "HEADING"));
  const addSectionAllowed = addAtEnd || cursorLine !== null || onText;
  const insertAllowed = !inHeader && (cursorLine !== null || onText) && (focused ? inPart(focused.id) : atEndInPart);
  // Ở tên bài hay ở dòng cuối (chưa phải đoạn văn) thì không có khối nào để định dạng.
  const target = focused && !locked(focused) && !inHeader && !cursorLine ? focused : undefined;
  const style = styleOf(target);
  const textTarget = target?.type === "PARAGRAPH" || target?.type === "LIST";
  // Ô bảng đang trỏ (theo vùng chọn): căn lề từng ô (yêu cầu 2026-11-04).
  const cellField = target?.type === "TABLE" && selected?.blockId === target.id ? selected.field : null;
  const cellAlign = target?.type === "TABLE" && cellField !== null ? tableCell(target, cellField)?.align : undefined;
  // Đậm, nghiêng, màu… dùng được cả trong ô bảng (yêu cầu 2026-11-04); danh sách, căn lề thì không.
  const titleOn = inHeader && !!titleField && !titleField.readOnly;
  const tailStyle = cursorLine ? tailStyles[cursorLine.tailKey] ?? EMPTY_TAIL : null;
  // Chữ, căn lề: đoạn văn, danh sách, tiêu đề mục, ô bảng, dòng cuối, tên bài. Danh sách: đoạn văn, danh sách, ô bảng, dòng cuối.
  const formatTarget = textTarget || target?.type === "TABLE" || target?.type === "HEADING" || titleOn || !!tailStyle;
  const listAllowed = textTarget || target?.type === "TABLE" || !!tailStyle;
  // Định dạng hiện trên thanh công cụ: của vùng bôi đen nếu có, không thì của cả đoạn (ô bảng: cả ô đang trỏ).
  const titleRuns = currentTitleRuns(document, titleField?.text ?? "");
  const shownRuns = titleOn ? (selected?.blockId === "TITLE" && selected.start !== selected.end ? sliceRuns(titleRuns, selected.start, selected.end) : titleRuns)
    : tailStyle ? [{ ...tailStyle.format, text: "" }]
    : !target || !formatTarget ? [] : selected?.blockId === target.id && (selected.start !== selected.end || target.type === "TABLE")
    ? (selected.start !== selected.end ? sliceRuns(fieldRuns(target, selected.field), selected.start, selected.end) : fieldRuns(target, selected.field))
    : runsOf(target);

  function remember() {
    const now = Date.now();
    if (now - lastPush.current > HISTORY_GAP_MS) {
      past.current = [...past.current.slice(-99), document];
      future.current = [];
      setHistory({ past: past.current.length, future: 0 });
    }
    lastPush.current = now;
  }
  function update(next: DocumentBlock[], focus?: FocusTarget) {
    remember();
    if (focus) pendingFocus.current = focus;
    onChange({ ...document, blocks: next });
  }
  /** Đổi tên bài có định dạng: lưu vào JSON khung đề, chữ thuần báo cho trang. */
  function setTitle(next: TitleContent) {
    remember();
    onChange({ ...document, title: next });
    titleField?.onText(runsText(next.runs).replace(/\s*\n\s*/g, " "));
  }
  /** Hoàn tác, làm lại đổi cả tên bài thì báo lại chữ thuần cho trang. */
  function restore(model: DocumentModel) {
    onChange(model);
    if (titleField && model.title && runsText(model.title.runs) !== titleField.text) titleField.onText(runsText(model.title.runs));
  }
  function undo() {
    const previous = past.current.at(-1);
    if (!previous) return;
    past.current = past.current.slice(0, -1);
    future.current = [...future.current, document];
    lastPush.current = 0;
    setHistory({ past: past.current.length, future: future.current.length });
    restore(previous);
  }
  function redo() {
    const next = future.current.at(-1);
    if (!next) return;
    future.current = future.current.slice(0, -1);
    past.current = [...past.current, document];
    lastPush.current = 0;
    setHistory({ past: past.current.length, future: future.current.length });
    restore(next);
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
    const next = without(blocks, id);
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
    if (cursorLine) setCursorLine(null);
    if (inHeader) setInHeader(false);
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
  /**
   * Thêm tiêu đề mục ngay sau {@code afterId} (cuối bài nếu null); con trỏ vào ô tiêu đề. Nội dung viết ở dòng cuối của ô
   * văn bản, luôn có sẵn nên không cần thêm đoạn văn.
   */
  function addHeading(afterId: string | null, style: HeadingStyle) {
    const heading: DocumentBlock = { id: newId(), origin, type: "HEADING", content: { style, text: "" } };
    const index = afterId ? blocks.findIndex((b) => b.id === afterId) : blocks.length - 1;
    const at = index + 1;
    setFocusedId(heading.id);
    update([...blocks.slice(0, at), heading, ...blocks.slice(at)], { id: heading.id, at: "start" });
  }
  /** Dải block liền nhau của cả mục (tiêu đề, văn bản, mục con, văn bản sau mục con). */
  function rangeOf(section: DocSection): [number, number] {
    const start = blocks.findIndex((block) => block.id === section.heading?.id);
    const last = lastOf(section);
    return [start, last ? blocks.findIndex((block) => block.id === last.id) : start];
  }
  /** Đưa mục {@code section} tới trước hoặc sau mục {@code target} cùng cha (yêu cầu 2026-11-04). */
  function moveSection(section: DocSection, target: DocSection, place: "before" | "after") {
    if (section === target) return;
    const [from, to] = rangeOf(section);
    if (from < 0) return;
    const moving = blocks.slice(from, to + 1);
    const rest = [...blocks.slice(0, from), ...blocks.slice(to + 1)];
    const targetLast = lastOf(target);
    const anchor = place === "before" ? target.heading?.id : targetLast?.id;
    const index = rest.findIndex((block) => block.id === anchor);
    if (index < 0) return;
    const at = place === "before" ? index : index + 1;
    update([...rest.slice(0, at), ...moving, ...rest.slice(at)]);
  }
  /** Thả mục đang kéo vào chỗ đang xem trước (trước, sau một mục cùng cha). */
  function commitDrop() {
    const dragged = dragKey ? sectionByKey.get(dragKey) : undefined;
    const target = dropAt ? sectionByKey.get(dropAt.key) : undefined;
    if (dragged && target && dropAt && parentOf.get(dragged) === parentOf.get(target)) moveSection(dragged, target, dropAt.place);
    setDragKey(null);
    setDropAt(null);
  }
  function stepSection(section: DocSection, direction: -1 | 1) {
    const siblings = parentOf.get(section)?.children ?? [];
    const index = siblings.indexOf(section);
    const other = siblings[index + direction];
    if (other) moveSection(section, other, direction < 0 ? "before" : "after");
  }
  /** Xóa cả mục (tiêu đề, văn bản và các mục con); con trỏ về chỗ viết ngay trước mục. Hoàn tác được. */
  function removeSection(section: DocSection) {
    const ids = new Set<string>();
    const walk = (part: DocSection) => {
      if (part.heading) ids.add(part.heading.id);
      part.blocks.forEach((block) => ids.add(block.id));
      part.after.forEach((block) => ids.add(block.id));
      part.children.forEach(walk);
    };
    walk(section);
    const index = blocks.findIndex((block) => block.id === section.heading?.id);
    const next = blocks.filter((block) => !ids.has(block.id));
    const previous = neighbour(next, index - 1, -1);
    update(next, previous ? { id: previous.id, at: "end" } : undefined);
  }
  /** Bỏ block {@code id}; nếu đó là khối đầu của phần viết sau các mục con thì khối kế tiếp nhận dấu {@code under}. */
  function without(list: DocumentBlock[], id: string): DocumentBlock[] {
    const index = list.findIndex((b) => b.id === id);
    const removed = list[index];
    const under = removed ? underOf(removed) : null;
    const next = list.filter((b) => b.id !== id);
    const follower = next[index];
    if (under === null || !follower || follower.type === "HEADING" || underOf(follower) !== null) return next;
    return next.map((b, i) => (i === index ? withUnder(b, under) : b));
  }

  /**
   * Chèn bảng, ảnh, sơ đồ sau block đang chọn (hoặc cuối bài); người học chỉ chèn trong phần. Chèn xong con trỏ xuống dòng
   * ngay sau (yêu cầu 2026-11-04): đoạn văn kế tiếp nếu có, dòng cuối nếu khối mới là cuối văn bản của mục, không thì một
   * đoạn trống mới.
   */
  function insert(block: DocumentBlock) {
    if (!insertAllowed) return;
    const afterId = cursorLine ? cursorLine.afterId : focused?.id ?? null;
    const index = afterId ? blocks.findIndex((b) => b.id === afterId) : -1;
    const at = index < 0 ? blocks.length : index + 1;
    const next = blocks[at];
    const sectionKey = cursorLine?.sectionKey ?? cursorSection?.heading?.id ?? "DOCUMENT";
    const ownTail = cursorLine?.tailKey ?? (cursorSection && cursorSection.children.length === 0 ? `tail-${sectionKey}` : null);
    setFocusedId(block.id);
    if (next && (next.type === "PARAGRAPH" || next.type === "LIST") && underOf(next) === null) {
      update([...blocks.slice(0, at), block, ...blocks.slice(at)], { id: next.id, at: "start" });
    } else if (ownTail && (!next || next.type === "HEADING")) {
      update([...blocks.slice(0, at), block, ...blocks.slice(at)], { id: ownTail, at: "start" });
    } else {
      const line = paragraph(origin);
      update([...blocks.slice(0, at), block, line, ...blocks.slice(at)], { id: line.id, at: "start" });
    }
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
      onChange({ ...now, blocks: now.blocks.map((b) => (b.id === blockId && b.type === "IMAGE" ? { ...b, content: { ...b.content, fileRef, hasFile: false } } : b)) });
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
  /** Chèn khung sơ đồ trống; Draw.io chỉ mở khi người dùng bấm vào khung (yêu cầu 2026-11-04). */
  function insertDiagram() {
    if (!insertAllowed) return;
    insert(newBlock("DIAGRAM", origin));
  }
  /** Định dạng chữ (yêu cầu 2026-10-22): có vùng bôi đen trong ô thì chỉ đổi phần bôi đen, không thì cả đoạn. */
  function formatText(range: (runs: Run[], start: number, end: number) => Run[], whole: (block: DocumentBlock) => DocumentBlock) {
    if (!target || !formatTarget) return;
    const selection = selectionIn(paper.current);
    if (target.type === "TABLE") {
      // Ô bảng: bôi đen thì chỉ phần đó, không thì cả ô đang trỏ.
      if (!selection || selection.blockId !== target.id) return;
      const length = runsText(fieldRuns(target, selection.field)).length;
      const [start, end] = selection.start !== selection.end ? [selection.start, selection.end] : [0, length];
      replace(target.id, mapField(target, selection.field, (runs) => range(runs, start, end)));
      return;
    }
    if (selection && selection.blockId === target.id && selection.start !== selection.end) {
      replace(target.id, mapField(target, selection.field, (runs) => range(runs, selection.start, selection.end)));
    } else {
      replace(target.id, whole(target));
    }
  }
  /** Tên bài: bôi đen thì chỉ phần đó, không thì cả tên. */
  function formatTitle(range: (runs: Run[], start: number, end: number) => Run[]) {
    const selection = selectionIn(paper.current);
    const [start, end] = selection?.blockId === "TITLE" && selection.start !== selection.end ? [selection.start, selection.end] : [0, runsText(titleRuns).length];
    setTitle({ ...document.title, runs: range(titleRuns, start, end) });
  }
  function setTail(change: (style: TailStyle) => TailStyle) {
    if (!cursorLine) return;
    const key = cursorLine.tailKey;
    setTailStyles((current) => ({ ...current, [key]: change(current[key] ?? EMPTY_TAIL) }));
  }
  function toggle(flag: RunFlag) {
    if (titleOn) return formatTitle((runs, start, end) => toggleFlagRange(runs, start, end, flag));
    if (tailStyle) return setTail((style) => ({ ...style, format: { ...style.format, [flag]: !style.format[flag] || undefined } }));
    formatText((runs, start, end) => toggleFlagRange(runs, start, end, flag), (block) => toggleFlag(block, flag));
  }
  function paint(key: "color" | "highlight", value: string | undefined) {
    if (titleOn) return formatTitle((runs, start, end) => colorRange(runs, start, end, key, value));
    if (tailStyle) return setTail((style) => ({ ...style, format: { ...style.format, [key]: value } }));
    formatText((runs, start, end) => colorRange(runs, start, end, key, value), (block) => setRunColor(block, key, value));
  }
  /** Danh sách: đoạn văn, danh sách đổi kiểu; ô bảng thêm/bỏ dấu đầu dòng từng dòng; dòng cuối đặt sẵn kiểu cho chữ sắp gõ. */
  function toggleList(kind: "UL" | "OL") {
    if (tailStyle && cursorLine) {
      // Dòng cuối: tạo ngay danh sách một mục trống ở chỗ dòng cuối, con trỏ vào mục đó; dòng cuối xuống dưới. Backspace ở mục
      // trống xóa danh sách (yêu cầu 2026-11-04).
      const section = sectionByKey.get(cursorLine.sectionKey);
      const end = cursorLine.tailKey.startsWith("tail-end-");
      const list: DocumentBlock = { id: newId(), origin, type: "LIST", content: {
        ordered: kind === "OL", items: [{ level: 0, runs: [{ ...tailStyle.format, text: "" }] }],
        ...(tailStyle.align ? { align: tailStyle.align } : {}), ...(end && section ? { under: section.level } : {}) } };
      const at = cursorLine.afterId ? blocks.findIndex((b) => b.id === cursorLine.afterId) + 1 : 0;
      const key = cursorLine.tailKey;
      setTailStyles((current) => { const next = { ...current }; delete next[key]; return next; });
      setCursorLine(null);
      setFocusedId(list.id);
      update([...blocks.slice(0, at), list, ...blocks.slice(at)], { id: list.id, at: "start" });
      return;
    }
    if (!target) return;
    if (target.type === "TABLE") {
      const field = selectionIn(paper.current)?.field ?? cellField;
      if (field === null || field < 0) return;
      replace(target.id, mapField(target, field, (runs) => toggleCellList(runs, kind)));
      return;
    }
    replace(target.id, convertBlock(target, style === kind ? "P" : kind), { id: target.id, at: "end" });
  }
  function alignTo(value: Align) {
    const align = value === "LEFT" ? undefined : value;
    if (titleOn) return setTitle({ ...document.title, runs: titleRuns, align });
    if (tailStyle) return setTail((style) => ({ ...style, align }));
    if (!target) return;
    if (target.type !== "TABLE") { replace(target.id, setAlign(target, value)); return; }
    const field = selectionIn(paper.current)?.field ?? cellField;
    if (field === null || field < 0) return;
    replace(target.id, mapCell(target, field, (cell) => ({ ...cell, align })));
  }
  const shownAlign: Align | undefined = titleOn ? document.title?.align : tailStyle ? tailStyle.align
    : target?.type === "TABLE" ? cellAlign : alignOf(target);
  const alignAllowed = titleOn || !!tailStyle || (!!target && target.type !== "IMAGE" && target.type !== "DIAGRAM" && (target.type !== "TABLE" || cellField !== null));

  const tree = documentTree(document);
  // Chế độ Từng phần (yêu cầu 2026-11-04): mỗi mục là một trang (văn bản của riêng mục, kèm khung các mục cha), theo thứ tự
  // trong bài; văn bản ở ngoài mọi mục là một trang riêng ở đầu.
  const leaves: DocSection[] = [];
  const collect = (section: DocSection) => {
    if (section.heading) leaves.push(section);
    section.children.forEach(collect);
  };
  if (tree.blocks.length > 0 || tree.after.length > 0) leaves.push(tree);
  collect(tree);
  // Bài chưa có câu hỏi vẫn chuyển được sang Từng phần (yêu cầu 2026-11-10): cả bài là một trang.
  if (leaves.length === 0) leaves.push(tree);
  const stepping = view === "step";
  const stepIndex = Math.min(stepAt, Math.max(0, leaves.length - 1));
  const currentLeaf = stepping ? leaves[stepIndex]! : null;
  // Từng phần: chỉ các mục đang hiện (mục của trang và các mục cha) mới có ghi chú bên lề, không để cột ghi chú kéo dài trang.
  const shownHeadings = new Set<string>();
  if (currentLeaf) {
    const reaches = (section: DocSection): boolean => section === currentLeaf || section.children.some(reaches);
    const mark = (section: DocSection) => {
      if (!reaches(section)) return;
      if (section.heading) shownHeadings.add(section.heading.id);
      section.children.forEach(mark);
    };
    mark(tree);
  }
  const editing = blocks.find((b) => b.id === diagram && b.type === "DIAGRAM");
  // Đang vẽ (Draw.io chiếm cột phải, trang hẹp lại) thì ẩn ghi chú bên lề như bản demo ẩn cột mục lục.
  const visible = editing ? [] : (comments ?? []).filter((c) => !c.anchorBlockId
    || (currentLeaf ? shownHeadings.has(c.anchorBlockId) : blocks.some((b) => b.id === c.anchorBlockId)));
  const rail = wide && visible.length > 0;
  const inlineComments = (anchor: string | null) => rail ? null : visible.filter((c) => c.anchorBlockId === anchor).map((c) => (
    <div key={c.key} className="my-2" data-comment={c.key}>{c.content}</div>
  ));

  function renderBlock(block: DocumentBlock): ReactNode {
    const byline = block.type === "HEADING" ? sectionBylines?.[block.id] : undefined;
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
        onKeyDown={removeLabel ? (event) => { if ((event.key === "Delete" || event.key === "Backspace") && event.target === event.currentTarget) { event.preventDefault(); remove(block.id, true); } } : undefined}
        className={cx("group/block relative -mx-1 rounded px-1 py-0.5 outline-none",
          fullWidth && (block.type === "PARAGRAPH" || block.type === "LIST") && "[&_.text-sm]:text-base [&_.text-sm]:leading-7",
          muted && "opacity-70", muted && !readOnly && "cursor-not-allowed",
          blockIssues.length > 0 && "shadow-[inset_3px_0_0_var(--color-danger)]")}>
        {/* Ảnh, sơ đồ: nút ✕ luôn hiện mờ ở góc (rê vào thì rõ), để xóa được cả khung chưa vẽ (yêu cầu 2026-11-04). */}
        {removeLabel && <CornerDelete always label={removeLabel} onClick={() => remove(block.id)} />}
        {lockedLabel && <CornerDelete label={lockedLabel} disabled onClick={() => {}} />}
        {isLocked ? (
          <BlockView block={block} imageUrl={imageUrl} previewUrl={previews[block.id]}
            onOpenDiagram={drawable ? (id) => setDiagram(id) : undefined} />
        ) : (
          <BlockEditor block={block} imageUrl={imageUrl} previewUrl={previews[block.id]} uploadError={uploadError[block.id]}
            onUpload={uploadImage ? (file) => void upload(block.id, file) : undefined} guard={teacherBlock}
            placeholder={firstAnswerLine.has(block.id) ? ANSWER_PLACEHOLDER : undefined}
            ops={{
              change: (next, focus) => {
                // Đoạn cuối của khung bị xóa hết chữ: bỏ đoạn đó, con trỏ về dòng cuối (luôn có) của khung.
                const tail = authoring ? tailOf.get(block.id) : undefined;
                if (tail && next.type === "PARAGRAPH" && !runsText(next.content.runs)) {
                  update(without(blocks, block.id), { id: tail, at: "start" });
                  return;
                }
                replace(block.id, next, focus);
              },
              split,
              removeEmpty: () => remove(block.id, true),
              remove: () => remove(block.id, true),
              step: (direction) => step(block.id, direction),
              openDiagram: () => setDiagram(block.id),
            }} />
        )}
        {byline && <p className="mt-1 wrap-break-word text-xs text-muted">{byline}</p>}
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
    if (authoring || (!student && section.blocks.length > 0)) return null;
    if (student && !readOnly && section.heading && !inPart(section.heading.id)) return null;
    if (readOnly) {
      return student && emptyLeafLabel
        ? <p data-testid={`empty-part-${key}`} className="py-1 text-xs text-muted">{emptyLeafLabel}</p>
        : section.blocks.length === 0 ? <div className="h-7" /> : null;
    }
    return (
      <button type="button" aria-label="Viết vào phần này" data-testid={`write-part-${key}`}
        onClick={() => insertAfter(last.id, newBlock("PARAGRAPH", origin))} className={cx("block w-full cursor-text text-left text-sm text-muted", guided ? "min-h-16 rounded-md px-2 focus-visible:ring-2 focus-visible:ring-focus" : "h-7")}>
        {guided && ANSWER_PLACEHOLDER}
      </button>
    );
  }

  /** Đoạn đầu tiên sinh viên viết trong mỗi phần: còn trống thì có chữ gợi ý như của giảng viên (yêu cầu 2026-11-10). */
  const firstAnswerLine = new Set<string>();
  if (guided) {
    const mark = (section: DocSection) => {
      const first = section.blocks.find((b) => b.origin === "STUDENT" && b.type === "PARAGRAPH");
      if (first) firstAnswerLine.add(first.id);
      section.children.forEach(mark);
    };
    mark(tree);
  }

  function leafContent(section: DocSection, key: string): ReactNode {
    // Phần người học không viết được (phần chung của bài nhóm) thì không có ô Bài làm.
    const answerable = !section.heading || inPart(section.heading.id);
    if (!guided || !answerable) return <>{section.blocks.map(renderBlock)}{emptyLeaf(section, key)}</>;
    const firstAnswer = section.blocks.findIndex((block) => block.origin === "STUDENT" || block.type === "DIAGRAM" || block.type === "TABLE");
    const splitAt = firstAnswer < 0 ? section.blocks.length : firstAnswer;
    return <>
      {section.blocks.slice(0, splitAt).map(renderBlock)}
      <div data-testid={`answer-${key}`} className="mt-4 rounded-lg border border-line bg-surface p-3 focus-within:border-focus focus-within:ring-2 focus-within:ring-focus/15">
        <div className="mb-2 flex flex-wrap items-center justify-between gap-2 border-b border-line pb-2">
          <p className="text-sm font-semibold text-ink">Bài làm của bạn</p>
        </div>
        {section.blocks.slice(splitAt).map(renderBlock)}
        {emptyLeaf(section, key)}
      </div>
    </>;
  }

  /** Khối cuối cùng của cả phần (kể cả các phần con). */
  const lastOf = (section: DocSection): DocumentBlock | null => {
    const child = section.children.at(-1);
    return section.after.at(-1) ?? (child && lastOf(child)) ?? section.blocks.at(-1) ?? section.heading;
  };
  /** Khối cuối của riêng phần (chưa tính phần con): đoạn gõ ở dòng cuối của khung chèn ngay sau nó. */
  const ownEnd = (section: DocSection) => section.blocks.at(-1) ?? section.heading;
  const contains = (section: DocSection, target: DocSection): boolean => section === target || section.children.some((child) => contains(child, target));

  /**
   * Dòng cuối của khung văn bản (người soạn, yêu cầu 2026-11-02): luôn có một dòng trống ở cuối khung; gõ gì vào là thành
   * đoạn mới cuối phần (và lại có dòng cuối mới); xóa hết chữ của đoạn cuối thì đoạn đó mất, chỉ còn dòng cuối.
   */
  function tailLine(section: DocSection, key: string, end = false): ReactNode {
    const anchor = end ? lastOf(section) : ownEnd(section);
    const tailKey = end ? `tail-end-${key}` : `tail-${key}`;
    // Ô còn trống: gợi ý cho người mới (yêu cầu 2026-11-10); ngoài mọi câu hỏi chỉ nhắc bấm + để thêm câu hỏi.
    const placeholder = hasOwnText(section) || section.children.length > 0 ? undefined
      : section.heading ? "Viết nội dung câu hỏi…"
        : "Nhập đề bài, bối cảnh hệ thống, yêu cầu chung…";
    return (
      <div key={`${tailKey}-${blocks.length}`} data-block-id={tailKey} data-testid={tailKey}
        className="border-b border-dashed border-line-strong py-0.5 transition-colors focus-within:border-primary"
        onFocusCapture={() => {
          if (anchor) focusBlock(anchor.id);
          setCursorLine({ afterId: anchor?.id ?? null, sectionKey: key, tailKey });
        }}>
        <TailList list={tailStyles[tailKey]?.list}>
        <RichText runs={tailRuns(tailKey)} aria-label="Dòng cuối" placeholder={placeholder} className={cx(PARAGRAPH_CLASS, alignClass(tailStyles[tailKey]?.align))}
          onChange={(runs) => {
            if (!runsText(runs)) return;
            const preset = tailStyles[tailKey];
            const align = preset?.align ? { align: preset.align } : {};
            // Dòng cuối ở đáy ô (sau các mục con): đoạn mang cấp của phần để vẫn thuộc phần này. Đã chọn danh sách thì thành
            // danh sách một mục.
            const block: DocumentBlock = preset?.list
              ? { id: newId(), origin, type: "LIST", content: { ordered: preset.list === "OL", items: [{ level: 0, runs }], ...align } }
              : { id: newId(), origin, type: "PARAGRAPH", content: { runs, ...align, ...(end ? { under: section.level } : {}) } };
            if (preset) setTailStyles((current) => { const next = { ...current }; delete next[tailKey]; return next; });
            const at = anchor ? blocks.findIndex((b) => b.id === anchor.id) + 1 : 0;
            setFocusedId(block.id);
            update([...blocks.slice(0, at), block, ...blocks.slice(at)], { id: block.id, at: "end" });
          }}
          onKey={(event, caret) => {
            if (event.key === "Enter") {
              // Enter ở dòng cuối: thêm một dòng trống, dòng cuối xuống dưới (yêu cầu 2026-11-04).
              event.preventDefault();
              const line: DocumentBlock = { id: newId(), origin, type: "PARAGRAPH", content: { runs: EMPTY_RUNS, ...(end ? { under: section.level } : {}) } };
              const at = anchor ? blocks.findIndex((b) => b.id === anchor.id) + 1 : 0;
              update([...blocks.slice(0, at), line, ...blocks.slice(at)], { id: tailKey, at: "start" });
            } else if (event.key === "Backspace" && caret.start === 0 && caret.end === 0 && tailStyles[tailKey]) {
              // Dòng cuối chưa có chữ mà đã đặt định dạng, căn lề: Backspace bỏ các thứ đặt sẵn.
              event.preventDefault();
              setTailStyles((current) => { const next = { ...current }; delete next[tailKey]; return next; });
            } else if ((event.key === "Backspace" || event.key === "ArrowUp") && caret.start === 0 && caret.end === 0 && anchor) {
              event.preventDefault();
              focusField(paper.current, { id: anchor.id, at: "end" });
            }
          }} />
        </TailList>
      </div>
    );
  }

  /** Cấp tiêu đề của mục con trong {@code section}: theo mục con đang có, không thì sâu hơn một cấp (tối đa Tiêu đề 6). */
  const childLevel = (section: DocSection) => section.children[0]?.level ?? section.level + 1;
  /** Câu hỏi lồng tối đa 4 tầng (I.1.a.aa.), yêu cầu 2026-11-10: tầng 4 không thêm ý. */
  const canAddChild = (section: DocSection) => section.depth < MAX_SECTION_DEPTH && childLevel(section) <= 6;
  /** Thêm mục con vào cuối {@code section} (sau các mục con đang có); gốc tài liệu thì là mục ngoài cùng. */
  function addChild(section: DocSection) {
    const level = childLevel(section);
    if (!canAddChild(section)) return;
    addHeading(lastOf(section)?.id ?? null, `H${level}` as HeadingStyle);
  }
  /**
   * "+ Mục" trên thanh công cụ (yêu cầu 2026-11-04): thêm mục con của mục đang có con trỏ, ngay tại chỗ con trỏ (phần viết sau
   * con trỏ thành của mục con). Ngoài mọi mục thì là mục ngoài cùng; chưa đặt con trỏ thì thêm cuối tài liệu.
   */
  function addAtCursor(section: DocSection | undefined) {
    const parent = section ?? tree;
    if (!canAddChild(parent)) return;
    const afterId = cursorLine ? cursorLine.afterId : focused?.id ?? blocks.at(-1)?.id ?? null;
    // Văn bản, bảng, ảnh, sơ đồ nằm dưới con trỏ vẫn thuộc mục cha, hiện dưới mục con mới (yêu cầu 2026-11-04).
    const index = afterId ? blocks.findIndex((b) => b.id === afterId) : blocks.length - 1;
    const next = blocks[index + 1];
    const keep = next && next.type !== "HEADING" && underOf(next) === null && sectionOfBlock.get(next.id) === parent;
    const heading: DocumentBlock = { id: newId(), origin, type: "HEADING", content: { style: `H${childLevel(parent)}` as HeadingStyle, text: "" } };
    const rest = blocks.slice(index + 1).map((b, i) => (i === 0 && keep ? withUnder(b, parent.level) : b));
    setFocusedId(heading.id);
    update([...blocks.slice(0, index + 1), heading, ...rest], { id: heading.id, at: "start" });
  }

  /**
   * Thanh "+" ở đáy mỗi ô văn bản (cả ô ngoài cùng của tài liệu): thêm mục nằm trong ô văn bản đó, tức nhánh
   * con của mục (ở ô ngoài cùng là mục ngoài cùng).
   */
  function quickAdd(section: DocSection): ReactNode {
    // Luôn hiện để người mới thấy chỗ thêm mục (yêu cầu 2026-11-10); mục tầng 3 không thêm mục con.
    if (!canAddChild(section)) return null;
    return (
      <div className="flex flex-wrap items-center gap-1 pt-1" data-testid={`quick-add-${section.heading?.id ?? "DOCUMENT"}`}
        onMouseDown={(event) => event.preventDefault()}>
        <AddBar label={section.heading ? "Thêm ý" : "Thêm câu hỏi"} onClick={() => addChild(section)} />
      </div>
    );
  }

  /**
   * Một mục: khung màu theo tầng có số và tiêu đề (chỉ ghi chữ tiêu đề) và luôn một ô văn bản (giấy trắng) chứa văn bản của
   * mục; mục con nằm trong ô văn bản đó. Người soạn có một dòng cuối và một thanh "+" ở đáy mỗi ô văn bản; mục đã có mục
   * con thì dòng cuối nằm dưới các mục con, không còn dòng cuối hay "+" nào ở giữa (yêu cầu 2026-11-02).
   * {@code only}: chế độ Từng phần, chỉ vẽ nhánh dẫn tới phần đó.
   */
  function renderSection(section: DocSection, problem: DocSection | null = null, only: DocSection | null = null): ReactNode {
    const heading = section.heading;
    const key = heading?.id ?? "DOCUMENT";
    const children = only ? section.children.filter((child) => contains(child, only)) : section.children;
    if (!heading) {
      return (
        <Fragment key={key}>
          {guided && section.leaf && blocks.length > 0 ? leafContent(section, key) : <>
            {section.blocks.map(renderBlock)}
            {section.leaf && blocks.length > 0 && emptyLeaf(section, key)}
          </>}
          {authoring && section.children.length === 0 && <>{tailLine(section, key)}{quickAdd(section)}</>}
          {children.map((child) => renderSection(child, null, only))}
          {section.after.map(renderBlock)}
          {authoring && !only && section.children.length > 0 && <>{tailLine(section, key, true)}{quickAdd(section)}</>}
        </Fragment>
      );
    }
    const owner = heading.type === "HEADING" && heading.content.style === "H1" ? section : problem;
    const active = highlightBlockId === heading.id;
    const writable = !readOnly && (!student || inPart(heading.id));
    const headingStyle = heading.type === "HEADING" ? heading.content.style : "H1";
    // Kéo một mục cùng cha qua mục này: hiện bản xem trước mờ ở chỗ sẽ thả.
    const dragged = dragKey ? sectionByKey.get(dragKey) : undefined;
    const droppable = !!dragged && dragged !== section && parentOf.get(dragged) === parentOf.get(section);
    const ghost = (place: "before" | "after") => droppable && dropAt?.key === key && dropAt.place === place && dragged?.heading ? (
      <div data-testid="drop-preview" aria-hidden className="my-2 rounded-lg border-2 border-dashed border-primary/60 px-3 py-2 opacity-50"
        onDragOver={(event) => { event.preventDefault(); event.stopPropagation(); }}
        onDrop={(event) => { event.preventDefault(); event.stopPropagation(); commitDrop(); }}
        style={{ backgroundColor: SECTION_BG[Math.min(dragged.depth - 1, SECTION_BG.length - 1)] }}>
        <span className={cx(HEADING_CLASS[dragged.heading.type === "HEADING" ? dragged.heading.content.style : "H1"] ?? HEADING_CLASS.H1)}>
          <SectionNumber value={numbers.get(dragKey!) ?? ""} /> {blockText(dragged.heading)}
        </span>
      </div>
    ) : null;
    return (
      <Fragment key={key}>
      {ghost("before")}
      <section data-section={key} data-leaf={section.leaf || undefined}
        style={{ backgroundColor: SECTION_BG[Math.min(section.depth - 1, SECTION_BG.length - 1)] }}
        onMouseOver={authoring ? (event) => { event.stopPropagation(); hoverOn(key); } : undefined}
        onDragOver={droppable ? (event) => {
          event.preventDefault();
          event.stopPropagation();
          const box = event.currentTarget.getBoundingClientRect();
          const place = event.clientY < box.top + box.height / 2 ? "before" : "after";
          if (dropAt?.key !== key || dropAt.place !== place) setDropAt({ key, place });
        } : undefined}
        onDrop={droppable ? (event) => {
          event.preventDefault();
          event.stopPropagation();
          commitDrop();
        } : undefined}
        className={cx("group/section relative my-2 rounded-lg px-3 py-2", active && "ring-2 ring-primary/50", dragKey === key && "opacity-40")}>
        {authoring && (() => {
          const siblings = parentOf.get(section)?.children ?? [];
          const index = siblings.indexOf(section);
          // Chỉ mục đang rê chuột hay đang có con trỏ hiện nút (mục cha, mục con không hiện theo).
          return <SectionTools shown={hoverSection === key || cursorSection === section} onRemove={() => removeSection(section)}
            onUp={index > 0 ? () => stepSection(section, -1) : undefined}
            onDown={index >= 0 && index < siblings.length - 1 ? () => stepSection(section, 1) : undefined} />;
        })()}
        {/* Số thứ tự I., I.1., II. đứng trước tiêu đề, không sửa được; người soạn kéo số để đổi chỗ mục trong cùng cha. */}
        <div className="flex items-start gap-1.5">
          <span data-testid={`number-${key}`} draggable={authoring || undefined}
            title={authoring ? "Kéo để đổi chỗ câu hỏi" : undefined}
            onDragStart={authoring ? (event) => { event.dataTransfer.effectAllowed = "move"; event.dataTransfer.setData("text/plain", key); setDragKey(key); } : undefined}
            onDragEnd={authoring ? () => { setDragKey(null); setDropAt(null); } : undefined}
            className={cx(HEADING_CLASS[headingStyle] ?? HEADING_CLASS.H1, "shrink-0 py-0.5 tabular-nums", authoring && "cursor-grab active:cursor-grabbing")}><SectionNumber value={numbers.get(key) ?? ""} /></span>
          <div className="min-w-0 flex-1">{renderBlock(heading)}</div>
          {sectionActions?.[heading.id] && <div className="flex shrink-0 items-center gap-1.5 self-center">{sectionActions[heading.id]}</div>}
        </div>
        {/* Ô trắng của phần; bấm vào chỗ trống là viết tiếp ở dòng cuối. */}
        <div data-leaf-body className="mt-1 mb-1 rounded-md border border-black/10 bg-surface px-3 py-1.5"
          onClick={(event) => {
            if (event.target !== event.currentTarget) return;
            if (authoring) focusField(paper.current, { id: children.length > 0 ? `tail-end-${key}` : `tail-${key}`, at: "end" });
            else if (section.leaf && writable) continueAfter((section.blocks.at(-1) ?? heading).id);
          }}>
          {section.leaf ? leafContent(section, key) : section.blocks.map(renderBlock)}
          {authoring && children.length === 0 && tailLine(section, key)}
          {children.map((child) => renderSection(child, owner, only))}
          {section.after.map(renderBlock)}
          {authoring && children.length > 0 && tailLine(section, key, true)}
          {authoring && quickAdd(section)}
        </div>
      </section>
      {ghost("after")}
      </Fragment>
    );
  }

  const topOf = (target: DocSection) => tree.children.find((child) => contains(child, target)) ?? target;
  latestLeaves.current = leaves;
  reportCurrent.current = () => {
    let current: string | null = currentLeaf ? currentLeaf.heading?.id ?? "DOCUMENT" : pinned.current;
    if (!currentLeaf && !current) {
      const sections = Array.from(paper.current?.querySelectorAll<HTMLElement>("section[data-section]") ?? []);
      for (const section of sections) if (section.getBoundingClientRect().top <= 200) current = section.dataset.section ?? null;
      if (!current && (tree.blocks.length > 0 || tree.after.length > 0)) current = "DOCUMENT";
    }
    if (current === lastCurrent.current) return;
    lastCurrent.current = current;
    window.dispatchEvent(new CustomEvent("document:current-part", { detail: { headingId: current } }));
  };
  /** Cuộn để mục {@code id} nằm ngay dưới thanh công cụ dính. */
  function scrollToSection(id: string) {
    const element = paper.current?.querySelector(`[data-section="${id}"]`);
    if (!element || typeof window.scrollTo !== "function") return;
    window.scrollTo({ top: Math.max(0, element.getBoundingClientRect().top + window.scrollY - 170) });
  }
  /** Sang trang khác ở chế độ Từng phần rồi cuộn lên đầu trang (yêu cầu 2026-11-04). */
  function goPage(index: number) {
    setStepAt(index);
    requestAnimationFrame(() => {
      const top = paper.current ? paper.current.getBoundingClientRect().top + window.scrollY - 140 : 0;
      if (typeof window.scrollTo === "function") window.scrollTo({ top: Math.max(0, top), behavior: "smooth" });
    });
  }
  /** Hai mũi tên chuyển trang, không ghi tên phần; có ở trên đầu (trên tên bài) và cuối trang. */
  const stepNav = (place: "top" | "bottom") => (
    <nav aria-label={place === "top" ? "Chuyển phần (đầu trang)" : "Chuyển phần"} data-testid={place === "top" ? "step-nav-top" : "step-nav"}
      className={cx("flex items-center justify-between gap-2", place === "top" ? "-mt-6 mb-4 border-b border-line pb-3 sm:-mt-8" : "mt-4 border-t border-line pt-3")}>
      <button type="button" aria-label="Phần trước" title="Phần trước" disabled={stepIndex === 0} onClick={() => goPage(stepIndex - 1)}
        className="inline-flex size-8 items-center justify-center rounded-lg text-body hover:bg-sunken disabled:opacity-35">
        <Icon name="arrow_back" size={17} />
      </button>
      {place === "bottom" && <span className="sr-only" role="status">Phần {stepIndex + 1} / {leaves.length}</span>}
      <button type="button" aria-label="Phần sau" title="Phần sau" disabled={stepIndex >= leaves.length - 1} onClick={() => goPage(stepIndex + 1)}
        className="inline-flex size-8 items-center justify-center rounded-lg text-body hover:bg-sunken disabled:opacity-35">
        <Icon name="arrow_forward" size={17} />
      </button>
    </nav>
  );
  // Nút chọn cách đọc (Cả trang / Từng phần): trên header của trang nếu trang đưa chỗ ({@code viewSlot}), không thì trên thanh
  // công cụ.
  // Luôn hiện, kể cả khi bài chưa có câu hỏi (yêu cầu 2026-11-10).
  const viewToggle = (
    <span role="radiogroup" aria-label="Chế độ xem" className="flex items-center gap-0.5 rounded-lg bg-line/60 p-0.5">
      <ViewButton label="Cả trang" on={!stepping} onClick={() => {
        pinned.current = currentLeaf ? currentLeaf.heading?.id ?? "DOCUMENT" : null;
        // Về Cả trang thì cuộn tới đúng mục đang xem, không về đầu trang.
        const id = currentLeaf?.heading?.id;
        setView("page");
        if (id) requestAnimationFrame(() => scrollToSection(id));
      }}><PageGlyph /></ViewButton>
      <ViewButton label="Từng phần" on={stepping} onClick={() => {
        // Mở đúng trang của mục đang xem (theo chỗ đã cuộn tới), không thì theo chỗ con trỏ (yêu cầu 2026-11-05).
        const viewing = lastCurrent.current ? leaves.findIndex((leaf) => (leaf.heading?.id ?? "DOCUMENT") === lastCurrent.current) : -1;
        const at = viewing >= 0 ? viewing
          : focused ? leaves.findIndex((leaf) => leaf.heading?.id === focused.id || leaf.blocks.some((b) => b.id === focused.id)) : -1;
        if (at >= 0) setStepAt(at);
        setView("step");
      }}><StepGlyph /></ViewButton>
    </span>
  );
  addPartFrom.current = (parentId) => {
    if (!authoring) return;
    const parent = parentId ? sectionByKey.get(parentId) : tree;
    if (parent) addChild(parent);
  };
  // Số thứ tự của từng câu hỏi theo cây: I., I.1., I.1.a., I.1.a.aa. (yêu cầu 2026-11-10).
  const numbers = new Map<string, string>();
  const numberParts = (section: DocSection, path: number[]) => section.children.forEach((child, i) => {
    const counters = [...path, i + 1];
    if (child.heading) numbers.set(child.heading.id, sectionNumbers?.[child.heading.id] ?? sectionNumber(counters));
    numberParts(child, counters);
  });
  numberParts(tree, []);
  // Đoạn văn cuối của mỗi khung có dòng cuối: id đoạn -> id dòng cuối.
  const tailOf = new Map<string, string>();
  const mapTails = (section: DocSection) => {
    const hasTail = section.children.length === 0;
    const last = section.blocks.at(-1);
    if (hasTail && last?.type === "PARAGRAPH") tailOf.set(last.id, `tail-${section.heading?.id ?? "DOCUMENT"}`);
    const lastAfter = section.after.at(-1);
    if (section.children.length > 0 && lastAfter?.type === "PARAGRAPH") tailOf.set(lastAfter.id, `tail-end-${section.heading?.id ?? "DOCUMENT"}`);
    section.children.forEach(mapTails);
  };
  if (authoring) mapTails(tree);
  const sectionOfBlock = new Map<string, DocSection>();
  const parentOf = new Map<DocSection, DocSection>();
  const sectionByKey = new Map<string, DocSection>();
  const mapSections = (section: DocSection) => {
    sectionByKey.set(section.heading?.id ?? "DOCUMENT", section);
    section.children.forEach((child) => parentOf.set(child, section));
    [section.heading, ...section.blocks, ...section.after].forEach((block) => { if (block) sectionOfBlock.set(block.id, section); });
    section.children.forEach(mapSections);
  };
  mapSections(tree);
  const cursorSection = cursorLine ? sectionByKey.get(cursorLine.sectionKey) : focused ? sectionOfBlock.get(focused.id) : undefined;


  return (
    <div className="flex flex-col gap-3" data-testid="document-editor"
      onKeyDownCapture={(event) => {
        if (readOnly || !(event.ctrlKey || event.metaKey)) return;
        const key = event.key.toLowerCase();
        if (key === "z" && !event.shiftKey) { event.preventDefault(); undo(); }
        else if (key === "y" || (key === "z" && event.shiftKey)) { event.preventDefault(); redo(); }
        else if ((key === "b" || key === "i" || key === "u") && formatTarget) {
          event.preventDefault();
          toggle(key === "b" ? "bold" : key === "i" ? "italic" : "underline");
        }
      }}>
      {viewSlot && viewToggle && createPortal(viewToggle, viewSlot)}
      {readOnly && toolbarCenter && (
        // Cùng lưới với trang giấy và cột ghi chú bên dưới, để nút giữa nằm thẳng giữa trang giấy.
        <div ref={measureToolbar} role="toolbar" aria-label="Thanh công cụ"
          className={`sticky top-[var(--doc-toolbar-top,4rem)] z-20 rounded-xl border border-line bg-sunken px-3 py-1 ${TOOLBAR_COVER}`}>
          <div className={rail ? "grid grid-cols-[minmax(0,816px)_300px] items-center justify-center gap-5" : "grid grid-cols-1 items-center gap-2 sm:grid-cols-[1fr_auto_1fr] sm:gap-0"}>
            {!rail && <span className="hidden sm:block" />}
            <div className="flex items-center justify-center gap-0.5">{toolbarCenter}</div>
            {/* Chỉ đọc (lịch sử nộp bài) cũng đổi được Cả trang / Từng phần (yêu cầu 2026-11-05). */}
            <div className="flex items-center justify-end gap-0.5">{!viewSlot && viewToggle}{toolbarExtra}</div>
          </div>
        </div>
      )}
      {(!readOnly || (toolbarExtra && !toolbarCenter)) && (
        <div ref={measureToolbar} role="toolbar" aria-label="Định dạng và chèn"
          className={`sticky top-[var(--doc-toolbar-top,4rem)] z-20 flex flex-wrap items-center gap-0.5 rounded-xl bg-sunken px-3 py-1 border border-line ${TOOLBAR_COVER}`}>
          {!readOnly && (
            <>
              <ToolbarButton title="Hoàn tác (Ctrl+Z)" aria-label="Hoàn tác" disabled={history.past === 0} onClick={undo}><Glyph name="undo" /></ToolbarButton>
              <ToolbarButton title="Làm lại (Ctrl+Y)" aria-label="Làm lại" disabled={history.future === 0} onClick={redo}><Glyph name="redo" /></ToolbarButton>
              <Separator />
              {!student && (
                <>
                  <ToolbarButton title="Thêm câu hỏi tại chỗ con trỏ" aria-label="Thêm câu hỏi" className="gap-1 pr-2"
                    data-testid="add-block" disabled={!addSectionAllowed || !canAddChild(addAtEnd ? tree : cursorSection ?? tree)}
                    onClick={() => (addAtEnd ? addChild(tree) : addAtCursor(cursorSection))}>
                    <Icon name="add" size={16} />Câu hỏi
                  </ToolbarButton>
                  <Separator />
                </>
              )}
              {FLAGS.map((flag) => (
                <ToolbarButton key={flag.key} title={flag.title} aria-label={flag.title} disabled={!formatTarget}
                  active={formatTarget ? shownRuns.length > 0 && shownRuns.every((run) => run[flag.key]) : undefined}
                  onClick={() => toggle(flag.key)}>
                  <span className={flag.className}>{flag.label}</span>
                </ToolbarButton>
              ))}
              <ToolPopover title="Màu chữ" disabled={!formatTarget}
                label={<span className="flex flex-col items-center leading-none"><span className="font-semibold">A</span><span className="mt-0.5 h-1 w-4 rounded-sm" style={{ backgroundColor: shownRuns[0]?.color ?? "#202124" }} /></span>}>
                {(close) => <Swatches colors={TEXT_COLORS} noneLabel="Mặc định" onPick={(color) => { paint("color", color); close(); }} />}
              </ToolPopover>
              <ToolPopover title="Màu tô nền" disabled={!formatTarget} label={<Glyph name="highlight" />}>
                {(close) => <Swatches colors={HIGHLIGHTS} noneLabel="Không tô" onPick={(color) => { paint("highlight", color); close(); }} />}
              </ToolPopover>
              <Separator />
              {ALIGNS.map((align) => (
                <ToolbarButton key={align.value} title={align.title} aria-label={align.title} disabled={!alignAllowed}
                  active={alignAllowed && (shownAlign ?? "LEFT") === align.value}
                  onClick={() => alignTo(align.value)}>
                  <Glyph name={align.glyph} />
                </ToolbarButton>
              ))}
              <Separator />
              <ToolbarButton title="Danh sách chấm" aria-label="Danh sách chấm" disabled={!listAllowed} active={tailStyle ? tailStyle.list === "UL" : style === "UL"}
                onClick={() => toggleList("UL")}><Glyph name="bullets" /></ToolbarButton>
              <ToolbarButton title="Danh sách số" aria-label="Danh sách số" disabled={!listAllowed} active={tailStyle ? tailStyle.list === "OL" : style === "OL"}
                onClick={() => toggleList("OL")}><Glyph name="numbers" /></ToolbarButton>
              <Separator />
              <ToolPopover title="Chèn bảng" disabled={!insertAllowed} label={<Glyph name="table" />}>
                {(close) => <TablePicker onPick={(rows, cols) => { insertTable(rows, cols); close(); }} />}
              </ToolPopover>
              {uploadImage && (
                <label title="Chèn ảnh" aria-label="Chèn ảnh" onMouseDown={(event) => event.preventDefault()}
                  className={cx("inline-flex h-7 min-w-7 items-center justify-center rounded-md px-1.5 text-body transition-colors", insertAllowed ? "cursor-pointer hover:bg-line-strong focus-within:bg-line-strong has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-focus" : "pointer-events-none opacity-35")}>
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
          {(toolbarExtra || (viewToggle && !viewSlot)) && (
            <div className="ml-auto flex flex-wrap items-center gap-0.5">
              {!viewSlot && viewToggle}
              {toolbarExtra}
            </div>
          )}
        </div>
      )}

      <div className={rail ? cx("grid items-start gap-5", fullWidth ? "grid-cols-[minmax(0,1fr)_300px]" : "grid-cols-[minmax(0,816px)_300px] justify-center") : cx("mx-auto w-full", !fullWidth && "max-w-[816px]")}>
        <div ref={paper} data-testid="document-paper" onMouseLeave={hoverOff} onMouseOver={hoverOff}
          onBlurCapture={(event) => {
            // Con trỏ rời trang (bấm ra ngoài): không còn chỗ con trỏ, "+ Câu hỏi" thêm xuống cuối (yêu cầu 2026-11-10). Nút trên
            // thanh công cụ giữ con trỏ (chặn mousedown) nên không làm mất chỗ con trỏ.
            // Cửa sổ mất focus (mở hộp chọn tệp khi chèn ảnh) thì giữ chỗ con trỏ: kiểm sau khi focus đã chuyển xong.
            if (paper.current?.contains(event.relatedTarget as Node | null)) return;
            setTimeout(() => {
              if (!globalThis.document.hasFocus() || paper.current?.contains(globalThis.document.activeElement)) return;
              setFocusedId(null);
              setCursorLine(null);
              setInHeader(false);
            }, 0);
          }}
          onDragOver={(event) => { if (dragKey && dropAt) event.preventDefault(); }}
          onDrop={(event) => { if (!dragKey) return; event.preventDefault(); commitDrop(); }}
          className={cx("min-h-[60vh] min-w-0 rounded-lg border border-line bg-surface px-5 pt-10 pb-4 shadow-[var(--shadow-lift)] sm:pt-12", fullWidth ? "sm:px-8" : "sm:px-14")}>
          {/* Khung bọc nội dung (không có min-height) để cột ghi chú biết khi nào block đổi chỗ. */}
          {stepping && stepNav("top")}
          <div ref={content}>
            {titleField && (
              // Thiếu tên bài khi hoàn tất, xếp lịch: viền đỏ quanh cả ô tên (yêu cầu 2026-11-12), hết khi gõ tên.
              <div data-block-id="TITLE" data-invalid={titleField.invalid || undefined}
                className={cx("mb-2", titleField.invalid && "rounded-lg px-2 py-1 outline-2 outline-danger")} onFocusCapture={() => { setInHeader(true); if (cursorLine) setCursorLine(null); }}>
                {titleField.readOnly || readOnly
                  ? <h2 className={cx(TITLE_CLASS, alignClass(document.title?.align))}>{titleField.text ? <RunsView runs={titleRuns} /> : "Bài chưa đặt tên"}</h2>
                  : <RichText aria-label="Tên bài" runs={titleRuns} placeholder={titleField.placeholder}
                      className={cx(TITLE_CLASS, alignClass(document.title?.align), titleField.invalid && "rounded-md bg-danger-soft")}
                      onChange={(runs) => setTitle({ ...document.title, runs })}
                      onKey={(event) => { if (event.key === "Enter") event.preventDefault(); }} />}
              </div>
            )}
            {header}
            {inlineComments(null)}
            {blocks.length === 0 && (readOnly
              ? <div className="h-16" />
              : student && <button type="button" aria-label="Bắt đầu viết" data-testid="write-document"
                onClick={() => insertAfter(null, newBlock("PARAGRAPH", origin))} className="block h-16 w-full cursor-text" />)}
            {currentLeaf ? renderSection(topOf(currentLeaf), null, currentLeaf) : renderSection(tree)}
          </div>
          {student && !readOnly && !stepping && blocks.length > 0 && atEndInPart && (
            <button type="button" aria-label="Viết tiếp ở cuối tài liệu" onClick={continueAtEnd} className="block h-14 w-full cursor-text" />
          )}
          {stepping && stepNav("bottom")}
        </div>
        {rail && <CommentRail paper={content} comments={visible} activeKey={activeComment} matchSectionHeight={matchCommentHeight} />}
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
      // Tiêu đề mục định dạng được (đậm, nghiêng, màu…, yêu cầu 2026-11-04), một dòng: Enter mở đoạn mới bên dưới.
      return (
        <RichText aria-label="Nội dung tiêu đề" placeholder="Tên câu hỏi" runs={headingRuns(block)}
          className={cx(HEADING_CLASS[block.content.style] ?? HEADING_CLASS.H1, alignClass(block.content.align))}
          onChange={(runs) => ops.change(withHeadingRuns(block, runs))}
          onKey={(event, caret) => keys(event, caret, {
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
      // Ảnh tải lên một lần lúc chèn (yêu cầu 2026-11-04): bấm vào ảnh không mở chọn file; chỉ ô chưa có ảnh mới chọn được.
      const pick = onUpload && !src && !block.content.fileRef ? onUpload : undefined;
      return (
        <figure className="flex flex-col items-center gap-1 py-1">
          <label title={pick ? "Chọn ảnh" : undefined} className={cx("block w-full", pick && "cursor-pointer")}>
            {src ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={src} alt={block.content.alt ?? ""} style={{ width: block.content.width ? `${block.content.width}px` : undefined }} className="mx-auto max-w-full" />
            ) : (
              <span className="flex w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-line-strong p-6 text-xs text-muted hover:border-primary hover:text-primary-ink">
                <Icon name="upload_file" size={20} />{block.content.fileRef ? "Đang tải ảnh…" : "Chọn ảnh"}
              </span>
            )}
            {pick && (
              <input type="file" accept={IMAGE_TYPES} className="sr-only"
                onChange={(event) => { const file = event.target.files?.[0]; if (file) pick(file); event.target.value = ""; }} />
            )}
          </label>
          {uploadError && <p role="alert" className="text-xs text-danger">{uploadError}</p>}
        </figure>
      );
    }
    case "DIAGRAM":
      // Khung vẽ trải hết bề ngang trang như bản demo (yêu cầu 2026-10-22); không có dòng chú thích bên dưới (2026-11-04).
      return (
        <figure className="py-1">
          <BlockView block={{ ...block, content: { ...block.content, alt: "" } }} onOpenDiagram={ops.openDiagram} />
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
  // Đổi số cột thì bỏ độ rộng đã kéo, các cột lại bằng nhau.
  function set(next: TableCell[][]) {
    const count = Math.max(1, ...next.map((row) => row.length));
    const { widths, ...rest } = block.content;
    onChange({ ...block, content: { ...rest, rows: next, ...(widths && widths.length === count ? { widths } : {}) } });
  }
  // Kéo mép phải của cột (ở hàng đầu) để đổi độ rộng cột đó và cột bên cạnh; mỗi cột tối thiểu 5% (yêu cầu 2026-11-04).
  const tableRef = useRef<HTMLTableElement>(null);
  const widths = block.content.widths?.length === columns ? block.content.widths : Array.from({ length: columns }, () => 100 / columns);
  function startResize(c: number, event: ReactMouseEvent) {
    event.preventDefault();
    const width = tableRef.current?.getBoundingClientRect().width ?? 1;
    const startX = event.clientX;
    const start = [...widths];
    const move = (moveEvent: MouseEvent) => {
      const delta = ((moveEvent.clientX - startX) / width) * 100;
      const left = Math.min(start[c]! + start[c + 1]! - 5, Math.max(5, start[c]! + delta));
      const next = [...start];
      next[c] = Math.round(left * 10) / 10;
      next[c + 1] = Math.round((start[c]! + start[c + 1]! - left) * 10) / 10;
      onChange({ ...block, content: { ...block.content, widths: next } });
    };
    const up = () => { window.removeEventListener("mousemove", move); window.removeEventListener("mouseup", up); };
    window.addEventListener("mousemove", move);
    window.addEventListener("mouseup", up);
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
      <table ref={tableRef} className="w-full table-fixed border-collapse">
        <colgroup>{widths.map((width, c) => <col key={c} style={{ width: `${width}%` }} />)}</colgroup>
        <tbody>
          {rows.map((row, r) => (
            <tr key={r} className="group/row">
              {Array.from({ length: columns }, (_, c) => (
                <td key={c} onMouseEnter={() => setHoverCol(c)}
                  className={cx("relative border border-line-strong align-top break-words", header && r === 0 && "bg-canvas")}>
                  {/* Ô chữ định dạng được như đoạn văn: bôi đen rồi đậm, nghiêng, màu (yêu cầu 2026-11-04). */}
                  <RichText aria-label={`Ô ${r + 1}-${c + 1}`} runs={row[c]?.runs ?? EMPTY_RUNS} onFocus={() => setFocusCol(c)}
                    className={cx("px-2 py-1.5 text-[13px] leading-snug text-ink", header && r === 0 && "font-semibold", alignClass(row[c]?.align))}
                    onChange={(runs) => set(rows.map((cells, i) => i !== r ? cells : full(cells).map((cell, j) =>
                      j === c ? { ...cell, runs } : cell)))} />
                  {r === 0 && c < columns - 1 && (
                    <span role="separator" aria-orientation="vertical" aria-label={`Kéo để đổi độ rộng cột ${c + 1}`} data-testid={`column-resize-${c}`}
                      onMouseDown={(event) => startResize(c, event)}
                      className="absolute top-0 -right-[3px] z-10 h-full w-1.5 cursor-col-resize hover:bg-primary/40" />
                  )}
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
