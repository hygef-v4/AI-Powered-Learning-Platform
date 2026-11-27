"use client";

import { SectionNumber } from "./SectionNumber";
import {
  Fragment,
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type KeyboardEvent,
  type MouseEvent as ReactMouseEvent,
  type ReactNode,
  type TextareaHTMLAttributes,
} from "react";
import { createPortal } from "react-dom";
import { Icon, cx } from "@/shared/ui";
import { CommentRail, useWideLayout, type DocumentComment } from "./CommentRail";
import { DiagramEditor } from "./DiagramEditor";
import { DIAGRAM_TEMPLATE_TYPES, buildTemplatePng, buildTemplateSvg, buildTemplateXml } from "./diagramTemplates";
import { BlockView, HEADING_CLASS, PARAGRAPH_CLASS, RunsView, alignClass, type ImageUrl } from "./DocumentViewer";
import { Glyph, type GlyphName } from "./Glyph";
import { ToolbarButton } from "./ToolbarButton";
import { RichText, getCaret, setCaret, type Caret } from "./RichText";
import { colorRange, formatOf, joinLines, runLines, sliceRuns, splitRuns, toggleFlagRange, type RunFlag } from "./runs";
import {
  blockText,
  computeParts,
  buildDocumentTree,
  createBlock,
  diagramTypeLabels,
  generateId,
  joinRunsText,
  numberSections,
  WHOLE_DOCUMENT,
  type TitleContent,
  MAX_SECTION_DEPTH,
  formatSectionNumber,
  findUnderLevel,
  pageKey,
  type Align,
  type BlockOrigin,
  type DocSection,
  type DocumentBlock,
  type DiagramType,
  type DocumentModel,
  type HeadingStyle,
  type ListItem,
  type Run,
  type TableCell,
  type ValidationIssue,
} from "./types";
import { msg } from "@/i18n/msg";

type Mode = "author" | "student";

/** Chữ gợi ý ở ô Bài làm của bạn (yêu cầu 2026-11-10), ngắn như chữ gợi ý của người soạn. */
const ANSWER_PLACEHOLDER = msg("shared.document.DocumentEditor.writeYourAnswer");

/** Độ rộng cột ghi chú bên lề khi kéo được (yêu cầu 2026-11-27): mặc định 300px như cột cố định. */
const RAIL_DEFAULT = 300;
const RAIL_MIN = 280;
const RAIL_MAX = 560;
const RAIL_STORAGE_KEY = "document-rail-width";
/** Không quá 40% bề ngang cửa sổ để trang giấy luôn rộng hơn cột ghi chú. */
const clampRail = (value: number) => Math.round(Math.max(RAIL_MIN, Math.min(RAIL_MAX,
  typeof window === "undefined" ? RAIL_MAX : window.innerWidth * 0.4, value)));

/** Độ rộng cột ghi chú; kéo được thì nhớ lần kéo trước trong trình duyệt. */
function useRailWidth(resizable: boolean): [number, (value: number) => void] {
  const [width, setWidth] = useState(RAIL_DEFAULT);
  useEffect(() => {
    if (!resizable) return;
    const saved = Number(window.localStorage?.getItem(RAIL_STORAGE_KEY));
    // eslint-disable-next-line react-hooks/set-state-in-effect -- đọc độ rộng đã lưu sau khi hydrate.
    if (saved) setWidth(clampRail(saved));
    const fit = () => setWidth((current) => clampRail(current));
    window.addEventListener("resize", fit);
    return () => window.removeEventListener("resize", fit);
  }, [resizable]);
  const update = useCallback((value: number) => {
    const next = clampRail(value);
    setWidth(next);
    try { window.localStorage?.setItem(RAIL_STORAGE_KEY, String(next)); } catch { /* bộ nhớ trình duyệt bị chặn: chỉ không nhớ */ }
  }, []);
  return [resizable ? width : RAIL_DEFAULT, update];
}

/** Mép trái cột ghi chú: kéo chuột hoặc dùng phím mũi tên để đổi độ rộng. */
function RailResizer({ width, onWidth }: { width: number; onWidth: (value: number) => void }) {
  const [dragging, setDragging] = useState(false);
  useEffect(() => {
    if (!dragging) return;
    const root = globalThis.document.documentElement;
    root.style.setProperty("cursor", "col-resize");
    root.style.setProperty("user-select", "none");
    return () => { root.style.removeProperty("cursor"); root.style.removeProperty("user-select"); };
  }, [dragging]);
  function startDrag(event: ReactMouseEvent<HTMLDivElement>) {
    event.preventDefault();
    const startX = event.clientX;
    const startWidth = width;
    setDragging(true);
    // Kéo sang trái thì cột rộng ra.
    const move = (next: MouseEvent) => onWidth(startWidth + startX - next.clientX);
    const stop = () => {
      setDragging(false);
      window.removeEventListener("mousemove", move);
      window.removeEventListener("mouseup", stop);
    };
    window.addEventListener("mousemove", move);
    window.addEventListener("mouseup", stop);
  }
  return (
    <div role="separator" aria-orientation="vertical" tabIndex={0} data-testid="rail-resizer"
      aria-label={msg("shared.document.DocumentEditor.resizeCommentColumn")} title={msg("shared.document.DocumentEditor.resizeCommentColumn")}
      aria-valuenow={width} aria-valuemin={RAIL_MIN} aria-valuemax={RAIL_MAX}
      onMouseDown={startDrag}
      onKeyDown={(event) => {
        const step = event.key === "ArrowLeft" ? 24 : event.key === "ArrowRight" ? -24 : 0;
        if (!step) return;
        event.preventDefault();
        onWidth(width + step);
      }}
      className="group absolute inset-y-0 -left-4 z-10 flex w-3 cursor-col-resize justify-center rounded-full outline-none focus-visible:outline-2 focus-visible:outline-focus">
      <span aria-hidden className={cx("h-full w-0.5 rounded-full transition-colors", dragging ? "bg-primary" : "bg-transparent group-hover:bg-primary/40 group-focus-visible:bg-primary/60")} />
      <span aria-hidden className="sticky top-1/2 -mt-6 flex h-12 w-3 -translate-x-[7px] items-center justify-center self-start rounded-full border border-line bg-surface opacity-0 shadow-[var(--shadow-lift)] transition-opacity group-hover:opacity-100 group-focus-visible:opacity-100"
        style={{ position: "sticky" }}>
        <span className="flex flex-col gap-[3px]">{Array.from({ length: 3 }, (_, i) => <span key={i} className="size-[3px] rounded-full bg-muted" />)}</span>
      </span>
    </div>
  );
}

/**
 * Sơ đồ mẫu của đề trong bài làm (yêu cầu 2026-11-27): người học chưa vẽ thì khung hiện "Bấm để vẽ" thay vì hình mẫu;
 * bấm vào vẫn mở Draw.io với hình mẫu để vẽ tiếp.
 */
function hideUntouchedTemplate(block: DocumentBlock, teacherBlock: boolean): DocumentBlock {
  if (!teacherBlock || block.type !== "DIAGRAM" || block.content.studentEdited) return block;
  const content = { ...block.content };
  delete content.svgPreview;
  delete content.pngPreview;
  return { ...block, content };
}

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
  /** Khung bên phải thay thế ghi chú bên lề; nội dung tự xử lý cách mở trên màn hẹp. */
  sidePanel?: ReactNode;
  /**
   * Cột ghi chú / khung bên phải kéo mép trái để đổi độ rộng, trong khoảng {@link RAIL_MIN}–{@link RAIL_MAX} px. Mặc định
   * bật cho mọi trang có trang bài bên trái, rubric hay ghi chú bên phải (yêu cầu 2026-11-27).
   */
  resizableRail?: boolean;
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
function filterWritable(sections: Map<string, string>, writable: ReadonlySet<string> | undefined): Map<string, string> {
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
  { key: "bold", label: "B", title: msg("shared.document.DocumentEditor.boldCtrlB"), className: "font-bold" },
  { key: "italic", label: "I", title: msg("shared.document.DocumentEditor.italicCtrlI"), className: "font-serif italic" },
  { key: "underline", label: "U", title: msg("shared.document.DocumentEditor.underlineCtrlU"), className: "underline" },
  { key: "strike", label: "S", title: msg("shared.document.DocumentEditor.strikethrough"), className: "line-through" },
];

const TEXT_COLORS = ["#202124", "#5F6368", "#C5221F", "#E37400", "#188038", "#1A73E8", "#9334E6", "#B80672"];
const HIGHLIGHTS = ["#FFF475", "#CCFF90", "#A7FFEB", "#AECBFA", "#D7AEFB", "#FDCFE8", "#E8EAED"];
/** Màu của phần theo tầng (yêu cầu 2026-10-22): cùng tầng cùng màu, tầng sâu đậm hơn; lá thì khung mang màu tầng, ruột trắng. */
// Nền các tầng mục: biến theme (globals.css) đậm dần theo tầng, tự đổi theo sáng/tối.
const SECTION_BG = ["var(--c-tier-1)", "var(--c-tier-2)", "var(--c-tier-3)", "var(--c-tier-4)"];
const ALIGNS: { value: Align; title: string; glyph: GlyphName }[] = [
  { value: "LEFT", title: msg("shared.document.DocumentEditor.alignLeft"), glyph: "alignLeft" },
  { value: "CENTER", title: msg("shared.document.DocumentEditor.alignCenter"), glyph: "alignCenter" },
  { value: "RIGHT", title: msg("shared.document.DocumentEditor.alignRight"), glyph: "alignRight" },
  { value: "JUSTIFY", title: msg("shared.document.DocumentEditor.justify"), glyph: "justify" },
];
/** Nút ✕ ở góc block; bảng có nút ✕ riêng nằm cùng hàng với các nút + − của bảng. */
const REMOVE_LABELS: Partial<Record<DocumentBlock["type"], string>> = { DIAGRAM: msg("shared.document.DocumentEditor.deleteDiagram"), IMAGE: msg("shared.document.DocumentEditor.deleteImage") };
/** Người học: sơ đồ, ảnh của đề trong phần vẫn có nút ✕ nhưng mờ, như "−" của hàng, cột của đề. */
const LOCKED_LABELS: Partial<Record<DocumentBlock["type"], string>> = { DIAGRAM: msg("shared.document.DocumentEditor.assignmentDiagram"), IMAGE: msg("shared.document.DocumentEditor.assignmentImage") };
const IMAGE_TYPES = ".png,.jpg,.jpeg";
const HISTORY_GAP_MS = 700;

/** Run của tiêu đề mục: tiêu đề cũ chỉ có chữ thì coi như một run. */
function readHeadingRuns(block: Extract<DocumentBlock, { type: "HEADING" }>): Run[] {
  return block.content.runs ?? [{ text: block.content.text }];
}

function readRuns(block: DocumentBlock | undefined): Run[] {
  if (block?.type === "HEADING") return readHeadingRuns(block);
  if (block?.type === "PARAGRAPH") return block.content.runs;
  if (block?.type === "LIST") return block.content.items.flatMap((item) => item.runs);
  return [];
}

function findTableCell(block: Extract<DocumentBlock, { type: "TABLE" }>, field: number): TableCell | undefined {
  const columns = countTableColumns(block);
  return block.content.rows[Math.floor(field / columns)]?.[field % columns];
}

/** Số cột của bảng (hàng thiếu ô vẫn vẽ đủ cột): ô thứ {@code field} là hàng {@code field / cột}, cột {@code field % cột}. */
function countTableColumns(block: Extract<DocumentBlock, { type: "TABLE" }>): number {
  return Math.max(1, ...block.content.rows.map((row) => row.length));
}

/** Run của ô thứ {@code field} trong block chữ (đoạn: ô 0; danh sách: mục thứ field). */
function readFieldRuns(block: DocumentBlock, field: number): Run[] {
  if (block.type === "HEADING") return readHeadingRuns(block);
  if (block.type === "PARAGRAPH") return block.content.runs;
  if (block.type === "LIST") return block.content.items[field]?.runs ?? [];
  if (block.type === "TABLE") {
    const columns = countTableColumns(block);
    return block.content.rows[Math.floor(field / columns)]?.[field % columns]?.runs ?? [];
  }
  return [];
}

function mapField(block: DocumentBlock, field: number, change: (runs: Run[]) => Run[]): DocumentBlock {
  if (block.type === "HEADING") return replaceHeadingRuns(block, change(readHeadingRuns(block)));
  if (block.type === "PARAGRAPH") return { ...block, content: { ...block.content, runs: change(block.content.runs) } };
  if (block.type === "LIST") {
    return { ...block, content: { ...block.content, items: block.content.items.map((item, i) => (i === field ? { ...item, runs: change(item.runs) } : item)) } };
  }
  if (block.type === "TABLE") return mapCell(block, field, (cell) => ({ ...cell, runs: change(cell.runs) }));
  return block;
}

/** Đổi ô thứ {@code field} của bảng (hàng rồi cột). */
function mapCell(block: Extract<DocumentBlock, { type: "TABLE" }>, field: number, change: (cell: TableCell) => TableCell): DocumentBlock {
  const columns = countTableColumns(block);
  const row = Math.floor(field / columns);
  const column = field % columns;
  return { ...block, content: { ...block.content, rows: block.content.rows.map((cells, r) => r !== row ? cells
    : Array.from({ length: Math.max(cells.length, column + 1) }, (_, c) => {
      const cell = cells[c] ?? { runs: [{ text: "" }] };
      return c === column ? change(cell) : cell;
    })) } };
}

function resolveStyle(block: DocumentBlock | undefined): StyleValue | "" {
  if (block?.type === "PARAGRAPH") return "P";
  if (block?.type === "HEADING") return block.content.style;
  if (block?.type === "LIST") return block.content.ordered ? "OL" : "UL";
  return "";
}

function alignOf(block: DocumentBlock | undefined): Align | undefined {
  return block?.type === "PARAGRAPH" || block?.type === "HEADING" || block?.type === "LIST" ? block.content.align : undefined;
}

function createParagraph(origin: BlockOrigin, runs: Run[] = [{ text: "" }], align?: Align): DocumentBlock {
  return { id: generateId(), origin, type: "PARAGRAPH", content: { runs, ...(align ? { align } : {}) } };
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
  return { id, origin, type: "HEADING", content: { style, text: joinRunsText(runs).replace(/\s*\n\s*/g, " ").slice(0, 500), ...keep } };
}

/** Tiêu đề giữ cả chữ thuần (đánh số, dàn ý, xuất file dùng) và run có định dạng. */
function replaceHeadingRuns(block: Extract<DocumentBlock, { type: "HEADING" }>, runs: Run[]): DocumentBlock {
  return { ...block, content: { ...block.content, runs, text: joinRunsText(runs).replace(/\s*\n\s*/g, " ").slice(0, 500) } };
}

function mapRuns(block: DocumentBlock, change: (run: Run) => Run): DocumentBlock {
  if (block.type === "HEADING") return replaceHeadingRuns(block, readHeadingRuns(block).map(change));
  if (block.type === "PARAGRAPH") return { ...block, content: { ...block.content, runs: block.content.runs.map(change) } };
  if (block.type === "LIST") return { ...block, content: { ...block.content, items: block.content.items.map((item) => ({ ...item, runs: item.runs.map(change) })) } };
  return block;
}

/** Bật hoặc tắt một định dạng cho cả đoạn hoặc cả danh sách (khi không bôi đen). */
export function toggleFlag(block: DocumentBlock, flag: RunFlag): DocumentBlock {
  const runs = readRuns(block);
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
function readSelection(root: HTMLElement | null): Selected | null {
  const active = globalThis.document.activeElement;
  if (!(active instanceof HTMLElement) || !active.hasAttribute("data-rich") || !root?.contains(active)) return null;
  const block = active.closest<HTMLElement>("[data-block-id]");
  if (!block) return null;
  const caret = getCaret(active);
  const field = Array.from(block.querySelectorAll("[data-rich]")).indexOf(active);
  return { blockId: block.dataset.blockId ?? "", field, start: caret.start, end: caret.end };
}

/** Đặt con trỏ ở đầu hoặc cuối một ô (ô chữ thường hoặc ô chữ có định dạng). */
function moveCaret(field: HTMLElement, at: "start" | "end") {
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
  moveCaret(field, target.at);
}

type KeyHandlers = {
  enter?: (caret: Caret) => void;
  removeEmpty?: () => void;
  step?: (direction: -1 | 1) => void;
};

/** Phím của ô chữ: Enter tách tại con trỏ, Backspace ở ô trống thì xóa block, mũi tên ở mép thì sang block bên cạnh. */
function handleKeys(event: KeyboardEvent<HTMLElement>, caret: Caret, handlers: KeyHandlers) {
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

const EMPTY_RUNS: Run[] = [{ text: "" }];
const TITLE_CLASS = "font-display text-[28px] leading-tight tracking-[-0.025em] text-ink";

type TailStyle = { format: Omit<Run, "text">; align?: Align; list?: "UL" | "OL" };
const EMPTY_TAIL: TailStyle = { format: {} };

/** Khối (không phải tiêu đề) mang dấu thuộc lại mục cấp {@code level} sau các mục con. */
function setUnderLevel(block: DocumentBlock, level: number): DocumentBlock {
  return block.type === "HEADING" ? block : { ...block, content: { ...block.content, under: level } } as DocumentBlock;
}

/** Run của tên bài: định dạng trong JSON nếu cùng chữ với tên đang có, không thì chữ thuần (tên đổi ở chỗ khác, bài cũ). */
function buildTitleRuns(document: DocumentModel, text: string): Run[] {
  const runs = document.title?.runs;
  return runs && joinRunsText(runs) === text ? runs : [{ text }];
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
  const marked = lines.every((line) => marker.test(joinRunsText(line)));
  const joinPlain = (line: Run[]) => {
    const text = joinRunsText(line);
    const found = /^• |^\d+\. /.exec(text);
    return found ? sliceRuns(line, found[0].length, text.length) : line;
  };
  return joinLines(lines.map((line, i) => {
    const rest = joinPlain(line);
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

/** Menu sơ đồ (yêu cầu 2026-11-27): đầu menu là Tạo mới (khung trống), dưới là các mẫu theo loại sơ đồ. */
function DiagramTemplateMenu({ onPick }: { onPick: (template?: Exclude<DiagramType, "OTHER">) => void }) {
  const item = "flex w-full items-center gap-2 rounded-md px-2.5 py-1.5 text-left text-[13px] text-body hover:bg-line-strong focus-visible:bg-line-strong";
  return (
    <div role="menu" className="flex w-52 flex-col gap-0.5">
      <button type="button" role="menuitem" data-testid="add-DIAGRAM" className={cx(item, "font-semibold")} onClick={() => onPick()}>
        <Icon name="add" size={16} />{msg("shared.document.DocumentEditor.newDiagram")}
      </button>
      <div className="my-1 border-t border-line" />
      {DIAGRAM_TEMPLATE_TYPES.map((type) => (
        <button key={type} type="button" role="menuitem" data-testid={`add-DIAGRAM-${type}`} className={item} onClick={() => onPick(type)}>
          {diagramTypeLabels[type]}
        </button>
      ))}
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
            <button key={i} type="button" aria-label={msg("shared.document.DocumentEditor.text", { row: String(row), col: String(col) })} onMouseEnter={() => setHover({ rows: row, cols: col })}
              onClick={() => onPick(row, col)}
              className={cx("size-[18px] rounded-[2px] border p-0", on ? "border-primary bg-primary-soft" : "border-line-strong bg-sunken")} />
          );
        })}
      </div>
      <p className="text-center text-xs text-body">{hover.rows > 0 ? msg("shared.document.DocumentEditor.text2", { rows: String(hover.rows), cols: String(hover.cols) }) : msg("shared.document.DocumentEditor.table")}</p>
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
      <Square danger label={msg("shared.document.DocumentEditor.deleteQuestion")} onClick={onRemove}>✕</Square>
      <Square label={msg("shared.document.DocumentEditor.moveQuestionUp")} disabled={!onUp} onClick={() => onUp?.()}>↑</Square>
      <Square label={msg("shared.document.DocumentEditor.moveQuestionDown")} disabled={!onDown} onClick={() => onDown?.()}>↓</Square>
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
  matchCommentHeight = false, sidePanel, resizableRail = true,
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
  const readTailRuns = (key: string): Run[] => {
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
  function startHover(key: string) {
    if (hoverTimer.current) clearTimeout(hoverTimer.current);
    hoverTimer.current = null;
    if (hoverSection !== key) setHoverSection(key);
  }
  function stopHover() {
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
  /**
   * Ctrl+A chọn cả phần đang viết (yêu cầu 2026-11-25): mỗi đoạn là một ô riêng nên vùng chọn của trình duyệt chỉ ôm được
   * một dòng; trình soạn tự giữ các khối được chọn, tô nền, rồi xử lý xóa, gõ đè, chép, cắt, dán và định dạng cho cả nhóm.
   */
  const [allSelected, setAllSelected] = useState<{ key: string; ids: string[] } | null>(null);
  const root = useRef<HTMLDivElement>(null);
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
  const [railWidth, setRailWidth] = useRailWidth(resizableRail);
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
      const pages = latestLeaves.current;
      let page = pages.findIndex((leaf) => pageKey(leaf) === id);
      if (latestView.current === "step" && page >= 0) {
        // Mục cha chưa có chữ riêng (chỉ chứa mục con): mở trang mục con đầu tiên, ví dụ bấm 1.2 thì sang 1.2.1; trang ngay
        // sau mục cha luôn là mục con đầu của nó (yêu cầu 2026-11-05).
        while (pages[page]?.heading && pages[page]!.children.length > 0 && !hasOwnText({ ...pages[page]!, after: [] }) && pages[page + 1]) page++;
        pinned.current = pageKey(pages[page]!);
        // Ở chế độ Từng phần luôn chuyển trang rồi lên đầu trang, kể cả khi mục cha đang hiện trên trang mục con.
        setStepAt(page);
        requestAnimationFrame(() => {
          const top = paper.current ? paper.current.getBoundingClientRect().top + window.scrollY - 140 : 0;
          if (typeof window.scrollTo === "function") window.scrollTo({ top: Math.max(0, top), behavior: "smooth" });
        });
        return;
      }
      if (page >= 0) setStepAt(page);
      // Văn bản không có tiêu đề (mở đầu, đoạn sau một mục): cuộn tới khối đầu tiên của nó.
      const loose = page >= 0 && !pages[page]!.heading ? pages[page]! : undefined;
      const scrollId = loose ? loose.blocks[0]?.id : id;
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
    const addPart = (event: Event) => addPartFrom.current((event as CustomEvent<{ parentId: string | null }>).detail?.parentId ?? null);
    window.addEventListener("document:add-part", addPart);
    return () => window.removeEventListener("document:add-part", addPart);
  }, []);

  // Vùng bôi đen để thanh công cụ hiện đúng định dạng của phần đang chọn.
  useEffect(() => {
    const owner = globalThis.document;
    const track = () => {
      const next = readSelection(paper.current);
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
  const sections = student ? filterWritable(numberSections(document, computeParts(document)), writableParts) : null;
  const isInPart = (blockId: string | undefined) => !sections || (!!blockId && sections.has(blockId));
  // Người học được viết vào phần gốc (khung không có tiêu đề là phần "Toàn bài"; bài nhóm thì phải giữ phần đó).
  const rootWritable = student && !readOnly && (!writableParts || writableParts.has(WHOLE_DOCUMENT));
  // Tài liệu trống không có tiêu đề nên cả bài là một phần; còn lại cuối tài liệu luôn thuộc tiêu đề cuối (là lá).
  const atEndInPart = blocks.length === 0 || isInPart(blocks.at(-1)!.id);
  // Bảng của người soạn trong phần: người học điền thẳng vào (yêu cầu 2026-10-22), không xóa bảng.
  const isFillable = (block: DocumentBlock) => student && !readOnly && block.origin === "TEACHER" && block.type === "TABLE" && isInPart(block.id);
  const isBlockLocked = (block: DocumentBlock) => readOnly || (student && block.origin === "TEACHER" && !isFillable(block));
  const focused = blocks.find((b) => b.id === focusedId);
  // Thanh công cụ theo chỗ con trỏ (yêu cầu 2026-11-04): + Mục chỉ ở đoạn văn và dòng cuối; chèn bảng, ảnh, sơ đồ ở đoạn văn,
  // danh sách, dòng cuối; không ở tên bài, tiêu đề mục, ô bảng, ảnh, sơ đồ.
  const onText = !focused || focused.type === "PARAGRAPH";
  // "+ Câu hỏi" (yêu cầu 2026-11-10): con trỏ không ở đâu, ở tên bài hay ở tiêu đề câu hỏi thì thêm câu hỏi xuống cuối;
  // ở đoạn văn, dòng cuối thì thêm tại con trỏ; chỉ khóa ở chỗ không được thêm (bảng, ảnh, sơ đồ, danh sách).
  const addAtEnd = inHeader || (cursorLine === null && (!focused || focused.type === "HEADING"));
  const addSectionAllowed = addAtEnd || cursorLine !== null || onText;
  const insertAllowed = !inHeader && (cursorLine !== null || onText) && (focused ? isInPart(focused.id) : atEndInPart);
  // Ở tên bài hay ở dòng cuối (chưa phải đoạn văn) thì không có khối nào để định dạng.
  const target = focused && !isBlockLocked(focused) && !inHeader && !cursorLine ? focused : undefined;
  const style = resolveStyle(target);
  const textTarget = target?.type === "PARAGRAPH" || target?.type === "LIST";
  // Ô bảng đang trỏ (theo vùng chọn): căn lề từng ô (yêu cầu 2026-11-04).
  const cellField = target?.type === "TABLE" && selected?.blockId === target.id ? selected.field : null;
  const cellAlign = target?.type === "TABLE" && cellField !== null ? findTableCell(target, cellField)?.align : undefined;
  // Đậm, nghiêng, màu… dùng được cả trong ô bảng (yêu cầu 2026-11-04); danh sách, căn lề thì không.
  const titleOn = inHeader && !!titleField && !titleField.readOnly;
  const tailStyle = cursorLine ? tailStyles[cursorLine.tailKey] ?? EMPTY_TAIL : null;
  // Chữ, căn lề: đoạn văn, danh sách, tiêu đề mục, ô bảng, dòng cuối, tên bài. Danh sách: đoạn văn, danh sách, ô bảng, dòng cuối.
  const formatTarget = textTarget || target?.type === "TABLE" || target?.type === "HEADING" || titleOn || !!tailStyle;
  const listAllowed = textTarget || target?.type === "TABLE" || !!tailStyle;
  // Định dạng hiện trên thanh công cụ: của vùng bôi đen nếu có, không thì của cả đoạn (ô bảng: cả ô đang trỏ).
  const titleRuns = buildTitleRuns(document, titleField?.text ?? "");
  const shownRuns = titleOn ? (selected?.blockId === "TITLE" && selected.start !== selected.end ? sliceRuns(titleRuns, selected.start, selected.end) : titleRuns)
    : tailStyle ? [{ ...tailStyle.format, text: "" }]
    : !target || !formatTarget ? [] : selected?.blockId === target.id && (selected.start !== selected.end || target.type === "TABLE")
    ? (selected.start !== selected.end ? sliceRuns(readFieldRuns(target, selected.field), selected.start, selected.end) : readFieldRuns(target, selected.field))
    : readRuns(target);

  function remember() {
    const now = Date.now();
    if (now - lastPush.current > HISTORY_GAP_MS) {
      past.current = [...past.current.slice(-99), document];
      future.current = [];
      setHistory({ past: past.current.length, future: 0 });
    }
    lastPush.current = now;
  }
  function updateBlocks(next: DocumentBlock[], focus?: FocusTarget) {
    remember();
    if (focus) pendingFocus.current = focus;
    onChange({ ...document, blocks: next });
  }
  /** Đổi tên bài có định dạng: lưu vào JSON khung đề, chữ thuần báo cho trang. */
  function setTitle(next: TitleContent) {
    remember();
    onChange({ ...document, title: next });
    titleField?.onText(joinRunsText(next.runs).replace(/\s*\n\s*/g, " "));
  }
  /** Hoàn tác, làm lại đổi cả tên bài thì báo lại chữ thuần cho trang. */
  function restore(model: DocumentModel) {
    onChange(model);
    if (titleField && model.title && joinRunsText(model.title.runs) !== titleField.text) titleField.onText(joinRunsText(model.title.runs));
  }
  function undoChange() {
    const previous = past.current.at(-1);
    if (!previous) return;
    past.current = past.current.slice(0, -1);
    future.current = [...future.current, document];
    lastPush.current = 0;
    setHistory({ past: past.current.length, future: future.current.length });
    restore(previous);
  }
  function redoChange() {
    const next = future.current.at(-1);
    if (!next) return;
    future.current = future.current.slice(0, -1);
    past.current = [...past.current, document];
    lastPush.current = 0;
    setHistory({ past: past.current.length, future: future.current.length });
    restore(next);
  }
  // Con trỏ ở ngoài trình soạn (bấm Mục lục, đóng popup nhập đề, khối vừa hoàn tác biến mất): Ctrl+Z, Ctrl+Y vẫn chạy
  // (yêu cầu 2026-11-25); ô nhập khác và popup giữ phím của riêng chúng.
  const historyKeys = useRef({ undo: undoChange, redo: redoChange });
  historyKeys.current = { undo: undoChange, redo: redoChange };
  useEffect(() => {
    if (readOnly) return;
    const onKey = (event: globalThis.KeyboardEvent) => {
      if (event.defaultPrevented || !(event.ctrlKey || event.metaKey) || event.altKey) return;
      const key = event.key.toLowerCase();
      if (key !== "z" && key !== "y") return;
      const target = event.target as HTMLElement | null;
      if (target && (root.current?.contains(target) || target.closest("input, textarea, select, [contenteditable], [role=dialog]"))) return;
      if (globalThis.document.querySelector("[role=dialog]")) return;
      event.preventDefault();
      if (key === "z" && !event.shiftKey) historyKeys.current.undo();
      else historyKeys.current.redo();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [readOnly]);
  function replace(id: string, block: DocumentBlock, focus?: FocusTarget) {
    updateBlocks(blocks.map((b) => (b.id === id ? block : b)), focus);
  }
  function insertAfter(afterId: string | null, block: DocumentBlock) {
    const index = afterId ? blocks.findIndex((b) => b.id === afterId) : -1;
    const at = index < 0 ? blocks.length : index + 1;
    setFocusedId(block.id);
    updateBlocks([...blocks.slice(0, at), block, ...blocks.slice(at)], { id: block.id, at: "start" });
  }
  function split(current: DocumentBlock, next: DocumentBlock) {
    const index = blocks.findIndex((b) => b.id === current.id);
    setFocusedId(next.id);
    updateBlocks([...blocks.slice(0, index), current, next, ...blocks.slice(index + 1)], { id: next.id, at: "start" });
  }
  /** Block chữ gần nhất theo hướng {@code direction} mà người dùng sửa được. */
  function findNeighbour(list: DocumentBlock[], from: number, direction: -1 | 1): DocumentBlock | undefined {
    for (let i = from; i >= 0 && i < list.length; i += direction) {
      const block = list[i]!;
      if (!isBlockLocked(block) && block.type !== "IMAGE" && block.type !== "DIAGRAM") return block;
    }
    return undefined;
  }
  function removeBlock(id: string, focusPrevious = false) {
    const index = blocks.findIndex((b) => b.id === id);
    const next = removeFromList(blocks, id);
    const previous = focusPrevious ? findNeighbour(next, index - 1, -1) ?? findNeighbour(next, index, 1) : undefined;
    updateBlocks(next, previous ? { id: previous.id, at: index > 0 ? "end" : "start" } : undefined);
  }
  function step(id: string, direction: -1 | 1) {
    const index = blocks.findIndex((b) => b.id === id);
    const next = findNeighbour(blocks, index + direction, direction);
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
    if (last && last.type === "PARAGRAPH" && !isBlockLocked(last) && !joinRunsText(last.content.runs)) focusField(paper.current, { id: last.id, at: "end" });
    else insertAfter(lastId, createBlock("PARAGRAPH", origin));
  }
  function continueAtEnd() {
    if (!atEndInPart) return;
    const last = blocks.at(-1);
    if (last) continueAfter(last.id);
    else insertAfter(null, createBlock("PARAGRAPH", origin));
  }
  /**
   * Thêm tiêu đề mục ngay sau {@code afterId} (cuối bài nếu null); con trỏ vào ô tiêu đề. Nội dung viết ở dòng cuối của ô
   * văn bản, luôn có sẵn nên không cần thêm đoạn văn.
   */
  function addHeading(afterId: string | null, style: HeadingStyle) {
    const heading: DocumentBlock = { id: generateId(), origin, type: "HEADING", content: { style, text: "" } };
    const index = afterId ? blocks.findIndex((b) => b.id === afterId) : blocks.length - 1;
    const at = index + 1;
    setFocusedId(heading.id);
    updateBlocks([...blocks.slice(0, at), heading, ...blocks.slice(at)], { id: heading.id, at: "start" });
  }
  /** Dải block liền nhau của cả mục (tiêu đề, văn bản, mục con, văn bản sau mục con). */
  function findRange(section: DocSection): [number, number] {
    const start = blocks.findIndex((block) => block.id === section.heading?.id);
    const last = findLast(section);
    return [start, last ? blocks.findIndex((block) => block.id === last.id) : start];
  }
  /** Đưa mục {@code section} tới trước hoặc sau mục {@code target} cùng cha (yêu cầu 2026-11-04). */
  function moveSection(section: DocSection, target: DocSection, place: "before" | "after") {
    if (section === target) return;
    const [from, to] = findRange(section);
    if (from < 0) return;
    const moving = blocks.slice(from, to + 1);
    const rest = [...blocks.slice(0, from), ...blocks.slice(to + 1)];
    const targetLast = findLast(target);
    const anchor = place === "before" ? target.heading?.id : targetLast?.id;
    const index = rest.findIndex((block) => block.id === anchor);
    if (index < 0) return;
    const at = place === "before" ? index : index + 1;
    updateBlocks([...rest.slice(0, at), ...moving, ...rest.slice(at)]);
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
    const previous = findNeighbour(next, index - 1, -1);
    updateBlocks(next, previous ? { id: previous.id, at: "end" } : undefined);
  }
  /** Bỏ block {@code id}; nếu đó là khối đầu của phần viết sau các mục con thì khối kế tiếp nhận dấu {@code under}. */
  function removeFromList(list: DocumentBlock[], id: string): DocumentBlock[] {
    const index = list.findIndex((b) => b.id === id);
    const removed = list[index];
    const under = removed ? findUnderLevel(removed) : null;
    const next = list.filter((b) => b.id !== id);
    const follower = next[index];
    if (under === null || !follower || follower.type === "HEADING" || findUnderLevel(follower) !== null) return next;
    return next.map((b, i) => (i === index ? setUnderLevel(b, under) : b));
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
    if (next && (next.type === "PARAGRAPH" || next.type === "LIST") && findUnderLevel(next) === null) {
      updateBlocks([...blocks.slice(0, at), block, ...blocks.slice(at)], { id: next.id, at: "start" });
    } else if (ownTail && (!next || next.type === "HEADING")) {
      updateBlocks([...blocks.slice(0, at), block, ...blocks.slice(at)], { id: ownTail, at: "start" });
    } else {
      const line = createParagraph(origin);
      updateBlocks([...blocks.slice(0, at), block, line, ...blocks.slice(at)], { id: line.id, at: "start" });
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
      setUploadError((current) => ({ ...current, [blockId]: error instanceof Error ? error.message : msg("shared.document.DocumentEditor.imageUploadFailed") }));
    }
  }
  function insertImage(file: File) {
    if (!insertAllowed) return;
    const block = createBlock("IMAGE", origin);
    insert(block);
    void upload(block.id, file);
  }
  function insertTable(rows: number, cols: number) {
    const createEmptyCell = (): TableCell => ({ runs: [{ text: "" }] });
    insert({ id: generateId(), origin, type: "TABLE", content: { rows: Array.from({ length: rows }, () => Array.from({ length: cols }, createEmptyCell)) } });
  }
  /**
   * Chèn khung sơ đồ trống; Draw.io chỉ mở khi người dùng bấm vào khung (yêu cầu 2026-11-04). Chọn một mẫu trong menu
   * sơ đồ thì khung có sẵn hình mẫu, bấm vào vẫn mở Draw.io để sửa (yêu cầu 2026-11-27).
   */
  async function insertDiagram(template?: Exclude<DiagramType, "OTHER">) {
    if (!insertAllowed) return;
    const block = createBlock("DIAGRAM", origin);
    if (!template || block.type !== "DIAGRAM") {
      insert(block);
      return;
    }
    // Ảnh PNG của mẫu để tải DOCX ra đúng hình ngay cả khi chưa mở Draw.io.
    const png = await buildTemplatePng(template);
    insert({ ...block, content: { ...block.content, diagramType: template, xml: buildTemplateXml(template),
      svgPreview: buildTemplateSvg(template), ...(png ? { pngPreview: png } : {}) } });
  }
  /** Định dạng chữ (yêu cầu 2026-10-22): có vùng bôi đen trong ô thì chỉ đổi phần bôi đen, không thì cả đoạn. */
  function formatText(range: (runs: Run[], start: number, end: number) => Run[], whole: (block: DocumentBlock) => DocumentBlock) {
    if (allSelected) {
      // Cả phần đang được chọn (Ctrl+A): định dạng mọi đoạn văn, danh sách trong phần.
      const ids = new Set(allSelected.ids);
      updateBlocks(blocks.map((b) => (ids.has(b.id) && (b.type === "PARAGRAPH" || b.type === "LIST") ? whole(b) : b)));
      return;
    }
    if (!target || !formatTarget) return;
    const selection = readSelection(paper.current);
    if (target.type === "TABLE") {
      // Ô bảng: bôi đen thì chỉ phần đó, không thì cả ô đang trỏ.
      if (!selection || selection.blockId !== target.id) return;
      const length = joinRunsText(readFieldRuns(target, selection.field)).length;
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
    const selection = readSelection(paper.current);
    const [start, end] = selection?.blockId === "TITLE" && selection.start !== selection.end ? [selection.start, selection.end] : [0, joinRunsText(titleRuns).length];
    setTitle({ ...document.title, runs: range(titleRuns, start, end) });
  }
  function setTail(change: (style: TailStyle) => TailStyle) {
    if (!cursorLine) return;
    const key = cursorLine.tailKey;
    setTailStyles((current) => ({ ...current, [key]: change(current[key] ?? EMPTY_TAIL) }));
  }
  function toggle(flag: RunFlag) {
    if (titleOn) return formatTitle((runs, start, end) => toggleFlagRange(runs, start, end, flag));
    if (tailStyle && !allSelected) return setTail((style) => ({ ...style, format: { ...style.format, [flag]: !style.format[flag] || undefined } }));
    formatText((runs, start, end) => toggleFlagRange(runs, start, end, flag), (block) => toggleFlag(block, flag));
  }
  function paint(key: "color" | "highlight", value: string | undefined) {
    if (titleOn) return formatTitle((runs, start, end) => colorRange(runs, start, end, key, value));
    if (tailStyle && !allSelected) return setTail((style) => ({ ...style, format: { ...style.format, [key]: value } }));
    formatText((runs, start, end) => colorRange(runs, start, end, key, value), (block) => setRunColor(block, key, value));
  }
  /** Danh sách: đoạn văn, danh sách đổi kiểu; ô bảng thêm/bỏ dấu đầu dòng từng dòng; dòng cuối đặt sẵn kiểu cho chữ sắp gõ. */
  function toggleList(kind: "UL" | "OL") {
    if (tailStyle && cursorLine) {
      // Dòng cuối: tạo ngay danh sách một mục trống ở chỗ dòng cuối, con trỏ vào mục đó; dòng cuối xuống dưới. Backspace ở mục
      // trống xóa danh sách (yêu cầu 2026-11-04).
      const section = sectionByKey.get(cursorLine.sectionKey);
      const end = cursorLine.tailKey.startsWith("tail-end-");
      const list: DocumentBlock = { id: generateId(), origin, type: "LIST", content: {
        ordered: kind === "OL", items: [{ level: 0, runs: [{ ...tailStyle.format, text: "" }] }],
        ...(tailStyle.align ? { align: tailStyle.align } : {}), ...(end && section ? { under: section.level } : {}) } };
      const at = cursorLine.afterId ? blocks.findIndex((b) => b.id === cursorLine.afterId) + 1 : 0;
      const key = cursorLine.tailKey;
      setTailStyles((current) => { const next = { ...current }; delete next[key]; return next; });
      setCursorLine(null);
      setFocusedId(list.id);
      updateBlocks([...blocks.slice(0, at), list, ...blocks.slice(at)], { id: list.id, at: "start" });
      return;
    }
    if (!target) return;
    if (target.type === "TABLE") {
      const field = readSelection(paper.current)?.field ?? cellField;
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
    const field = readSelection(paper.current)?.field ?? cellField;
    if (field === null || field < 0) return;
    replace(target.id, mapCell(target, field, (cell) => ({ ...cell, align })));
  }
  const shownAlign: Align | undefined = titleOn ? document.title?.align : tailStyle ? tailStyle.align
    : target?.type === "TABLE" ? cellAlign : alignOf(target);
  const alignAllowed = titleOn || !!tailStyle || (!!target && target.type !== "IMAGE" && target.type !== "DIAGRAM" && (target.type !== "TABLE" || cellField !== null));

  const tree = buildDocumentTree(document);
  // Chế độ Từng phần (yêu cầu 2026-11-04): mỗi mục là một trang (văn bản của riêng mục, kèm khung các mục cha), theo thứ tự
  // trong bài; văn bản ở ngoài mọi mục là một trang riêng ở đầu.
  // Chữ nằm ngay sau một mục là một trang riêng (yêu cầu 2026-11-27), không gộp với văn bản mở đầu của mục cha.
  const leaves: DocSection[] = [];
  const collect = (section: DocSection) => {
    if (section.heading) leaves.push(section);
    section.children.forEach((child) => {
      collect(child);
      if (child.trailing.length > 0) leaves.push({ heading: null, level: section.level, depth: child.depth, blocks: child.trailing,
        children: [], after: [], trailing: [], leaf: true, segment: { parent: section, child } });
    });
  };
  if (tree.blocks.length > 0) leaves.push(tree);
  collect(tree);
  // Bài chưa có câu hỏi vẫn chuyển được sang Từng phần (yêu cầu 2026-11-10): cả bài là một trang.
  if (leaves.length === 0) leaves.push(tree);
  const stepping = view === "step";
  const stepIndex = Math.min(stepAt, Math.max(0, leaves.length - 1));
  const currentLeaf = stepping ? leaves[stepIndex]! : null;
  // Từng phần: chỉ các mục đang hiện (mục của trang và các mục cha) mới có ghi chú bên lề, không để cột ghi chú kéo dài trang.
  const shownHeadings = new Set<string>();
  if (currentLeaf) {
    const goal = currentLeaf.segment?.parent ?? currentLeaf;
    const reachesCurrent = (section: DocSection): boolean => section === goal || section.children.some(reachesCurrent);
    const mark = (section: DocSection) => {
      if (!reachesCurrent(section)) return;
      if (section.heading) shownHeadings.add(section.heading.id);
      section.children.forEach(mark);
    };
    mark(tree);
  }
  const editing = blocks.find((b) => b.id === diagram && b.type === "DIAGRAM");
  // Đang vẽ (Draw.io chiếm cột phải, trang hẹp lại) thì ẩn ghi chú bên lề như bản demo ẩn cột mục lục.
  const visible = editing ? [] : (comments ?? []).filter((c) => !c.anchorBlockId
    || (currentLeaf ? shownHeadings.has(c.anchorBlockId) : blocks.some((b) => b.id === c.anchorBlockId)));
  const rail = wide && (Boolean(sidePanel) || visible.length > 0);
  const listInlineComments = (anchor: string | null) => rail || sidePanel ? null : visible.filter((c) => c.anchorBlockId === anchor).map((c) => (
    <div key={c.key} className="my-2" data-comment={c.key}>{c.content}</div>
  ));

  function renderBlock(block: DocumentBlock): ReactNode {
    const byline = block.type === "HEADING" ? sectionBylines?.[block.id] : undefined;
    const isLocked = isBlockLocked(block);
    const teacherBlock = student && block.origin === "TEACHER";
    const drawable = !readOnly && teacherBlock && block.type === "DIAGRAM" && isInPart(block.id);
    const muted = teacherBlock && !drawable && !isFillable(block) && block.type !== "HEADING";
    const removeLabel = !readOnly && !isLocked && !teacherBlock ? REMOVE_LABELS[block.type] : undefined;
    const lockedLabel = !readOnly && teacherBlock && isInPart(block.id) ? LOCKED_LABELS[block.type] : undefined;
    const blockIssues = issues.filter((issue) => issue.blockId === block.id);
    return (
      <div key={block.id} data-testid={`block-${block.id}`} data-block-id={block.id} tabIndex={-1}
        title={muted && !readOnly ? msg("shared.document.DocumentEditor.theLecturersAssignmentCannotBe") : undefined}
        onFocusCapture={() => focusBlock(block.id)} onMouseDownCapture={() => focusBlock(block.id)}
        onKeyDown={removeLabel ? (event) => { if ((event.key === "Delete" || event.key === "Backspace") && event.target === event.currentTarget) { event.preventDefault(); removeBlock(block.id, true); } } : undefined}
        className={cx("group/block relative -mx-1 rounded px-1 py-0.5 outline-none",
          fullWidth && (block.type === "PARAGRAPH" || block.type === "LIST") && "[&_.text-sm]:text-base [&_.text-sm]:leading-7",
          muted && "opacity-70", muted && !readOnly && "cursor-not-allowed",
          selectedIds.has(block.id) && "bg-primary-soft",
          blockIssues.length > 0 && "shadow-[inset_3px_0_0_var(--color-danger)]")}>
        {/* Ảnh, sơ đồ: nút ✕ luôn hiện mờ ở góc (rê vào thì rõ), để xóa được cả khung chưa vẽ (yêu cầu 2026-11-04). */}
        {removeLabel && <CornerDelete always label={removeLabel} onClick={() => removeBlock(block.id)} />}
        {lockedLabel && <CornerDelete label={lockedLabel} disabled onClick={() => {}} />}
        {isLocked ? (
          <BlockView block={hideUntouchedTemplate(block, teacherBlock)} imageUrl={imageUrl} previewUrl={previews[block.id]}
            onOpenDiagram={drawable ? (id) => setDiagram(id) : undefined} />
        ) : (
          <BlockEditor block={block} imageUrl={imageUrl} previewUrl={previews[block.id]} uploadError={uploadError[block.id]}
            onUpload={uploadImage ? (file) => void upload(block.id, file) : undefined} guard={teacherBlock}
            placeholder={firstAnswerLine.has(block.id) ? ANSWER_PLACEHOLDER : undefined}
            ops={{
              change: (next, focus) => {
                // Đoạn cuối của khung bị xóa hết chữ: bỏ đoạn đó, con trỏ về dòng cuối (luôn có) của khung.
                const tail = authoring ? tailOf.get(block.id) : undefined;
                if (tail && next.type === "PARAGRAPH" && !joinRunsText(next.content.runs)) {
                  updateBlocks(removeFromList(blocks, block.id), { id: tail, at: "start" });
                  return;
                }
                replace(block.id, next, focus);
              },
              split,
              removeEmpty: () => removeBlock(block.id, true),
              remove: () => removeBlock(block.id, true),
              step: (direction) => step(block.id, direction),
              openDiagram: () => setDiagram(block.id),
            }} />
        )}
        {byline && <p className="mt-1 wrap-break-word text-xs text-muted">{byline}</p>}
        {blockIssues.map((issue, i) => <p key={i} role="alert" className="text-xs text-danger">{issue.message}</p>)}
        {listInlineComments(block.id)}
      </div>
    );
  }

  /**
   * Chỗ viết của phần lá chưa có nội dung (người học: chưa có bài làm; người soạn: mới có tiêu đề): một dòng trống, bấm
   * vào là viết, không có chữ gợi ý (yêu cầu 2026-10-22).
   */
  function renderEmptyLeaf(section: DocSection, key: string): ReactNode {
    const last = section.blocks.at(-1) ?? section.heading;
    // Đề chỉ có phần gốc và chưa có khối nào: người học vẫn có chỗ viết ngay dưới gốc (yêu cầu 2026-11-27).
    if (!last && !(section.heading === null && rootWritable)) return null;
    if (student && section.blocks.some((b) => b.origin === "STUDENT" && (!guided || b.type === "PARAGRAPH" || b.type === "LIST"))) return null;
    if (authoring || (!student && section.blocks.length > 0)) return null;
    if (student && !readOnly && section.heading && !isInPart(section.heading.id)) return null;
    if (readOnly) {
      return student && emptyLeafLabel
        ? <p data-testid={`empty-part-${key}`} className="py-1 text-xs text-muted">{emptyLeafLabel}</p>
        : section.blocks.length === 0 ? <div className="h-7" /> : null;
    }
    return (
      <button type="button" aria-label={msg("shared.document.DocumentEditor.writeInThisPart")} data-testid={`write-part-${key}`}
        onClick={() => insertAfter(last?.id ?? null, createBlock("PARAGRAPH", origin))} className={cx("block w-full cursor-text text-left text-sm text-muted", guided ? "min-h-16 rounded-md px-2 focus-visible:ring-2 focus-visible:ring-focus" : "h-7")}>
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

  function renderLeafContent(section: DocSection, key: string): ReactNode {
    // Phần người học không viết được (phần chung của bài nhóm) thì không có ô Bài làm.
    const answerable = !section.heading || isInPart(section.heading.id);
    if (!guided || !answerable) return <>{section.blocks.map(renderBlock)}{renderEmptyLeaf(section, key)}</>;
    const firstAnswer = section.blocks.findIndex((block) => block.origin === "STUDENT" || block.type === "DIAGRAM" || block.type === "TABLE");
    const splitAt = firstAnswer < 0 ? section.blocks.length : firstAnswer;
    return <>
      {section.blocks.slice(0, splitAt).map(renderBlock)}
      <div data-testid={`answer-${key}`} className="mt-4 rounded-lg border border-line bg-surface p-3 focus-within:border-focus focus-within:ring-2 focus-within:ring-focus/15">
        <div className="mb-2 flex flex-wrap items-center justify-between gap-2 border-b border-line pb-2">
          <p className="text-sm font-semibold text-ink">{msg("shared.document.DocumentEditor.yourWork")}</p>
        </div>
        {section.blocks.slice(splitAt).map(renderBlock)}
        {renderEmptyLeaf(section, key)}
      </div>
    </>;
  }

  /** Khối cuối cùng của cả phần (kể cả các phần con). */
  const findLast = (section: DocSection): DocumentBlock | null => {
    const child = section.children.at(-1);
    return child?.trailing.at(-1) ?? (child && findLast(child)) ?? section.blocks.at(-1) ?? section.heading;
  };
  /** Khối cuối của riêng phần (chưa tính phần con): đoạn gõ ở dòng cuối của khung chèn ngay sau nó. */
  const findOwnEnd = (section: DocSection) => section.blocks.at(-1) ?? section.heading;
  const contains = (section: DocSection, target: DocSection): boolean => section === target || section.children.some((child) => contains(child, target));

  /**
   * Dòng cuối của khung văn bản (người soạn, yêu cầu 2026-11-02): luôn có một dòng trống ở cuối khung; gõ gì vào là thành
   * đoạn mới cuối phần (và lại có dòng cuối mới); xóa hết chữ của đoạn cuối thì đoạn đó mất, chỉ còn dòng cuối.
   */
  function renderTailLine(section: DocSection, key: string, end = false): ReactNode {
    const anchor = end ? findLast(section) : findOwnEnd(section);
    const tailKey = end ? `tail-end-${key}` : `tail-${key}`;
    // Ô còn trống: gợi ý cho người mới (yêu cầu 2026-11-10); ngoài mọi câu hỏi chỉ nhắc bấm + để thêm câu hỏi.
    const placeholder = hasOwnText(section) || section.children.length > 0 ? undefined
      : section.heading ? msg("shared.document.DocumentEditor.writeTheQuestion")
        : msg("shared.document.DocumentEditor.enterTheBriefSystemContext");
    return (
      <div key={`${tailKey}-${blocks.length}`} data-block-id={tailKey} data-testid={tailKey}
        className="border-b border-dashed border-line-strong py-0.5 transition-colors focus-within:border-primary"
        onFocusCapture={() => {
          if (anchor) focusBlock(anchor.id);
          setCursorLine({ afterId: anchor?.id ?? null, sectionKey: key, tailKey });
        }}>
        <TailList list={tailStyles[tailKey]?.list}>
        <RichText runs={readTailRuns(tailKey)} aria-label={msg("shared.document.DocumentEditor.lastLine")} placeholder={placeholder} className={cx(PARAGRAPH_CLASS, alignClass(tailStyles[tailKey]?.align))}
          onChange={(runs) => {
            if (!joinRunsText(runs)) return;
            const preset = tailStyles[tailKey];
            const align = preset?.align ? { align: preset.align } : {};
            // Dòng cuối ở đáy ô (sau các mục con): đoạn mang cấp của phần để vẫn thuộc phần này. Đã chọn danh sách thì thành
            // danh sách một mục.
            const block: DocumentBlock = preset?.list
              ? { id: generateId(), origin, type: "LIST", content: { ordered: preset.list === "OL", items: [{ level: 0, runs }], ...align } }
              : { id: generateId(), origin, type: "PARAGRAPH", content: { runs, ...align, ...(end ? { under: section.level } : {}) } };
            if (preset) setTailStyles((current) => { const next = { ...current }; delete next[tailKey]; return next; });
            const at = anchor ? blocks.findIndex((b) => b.id === anchor.id) + 1 : 0;
            setFocusedId(block.id);
            updateBlocks([...blocks.slice(0, at), block, ...blocks.slice(at)], { id: block.id, at: "end" });
          }}
          onKey={(event, caret) => {
            if (event.key === "Enter") {
              // Enter ở dòng cuối: thêm một dòng trống, dòng cuối xuống dưới (yêu cầu 2026-11-04).
              event.preventDefault();
              const line: DocumentBlock = { id: generateId(), origin, type: "PARAGRAPH", content: { runs: EMPTY_RUNS, ...(end ? { under: section.level } : {}) } };
              const at = anchor ? blocks.findIndex((b) => b.id === anchor.id) + 1 : 0;
              updateBlocks([...blocks.slice(0, at), line, ...blocks.slice(at)], { id: tailKey, at: "start" });
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
  const resolveChildLevel = (section: DocSection) => section.children[0]?.level ?? section.level + 1;
  /** Câu hỏi lồng tối đa 4 tầng (I.1.a.aa.), yêu cầu 2026-11-10: tầng 4 không thêm ý. */
  const canAddChild = (section: DocSection) => section.depth < MAX_SECTION_DEPTH && resolveChildLevel(section) <= 6;
  /** Thêm mục con vào cuối {@code section} (sau các mục con đang có); gốc tài liệu thì là mục ngoài cùng. */
  function addChild(section: DocSection) {
    const level = resolveChildLevel(section);
    if (!canAddChild(section)) return;
    addHeading(findLast(section)?.id ?? null, `H${level}` as HeadingStyle);
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
    const keep = next && next.type !== "HEADING" && findUnderLevel(next) === null && sectionOfBlock.get(next.id) === parent;
    const heading: DocumentBlock = { id: generateId(), origin, type: "HEADING", content: { style: `H${resolveChildLevel(parent)}` as HeadingStyle, text: "" } };
    const rest = blocks.slice(index + 1).map((b, i) => (i === 0 && keep ? setUnderLevel(b, parent.level) : b));
    setFocusedId(heading.id);
    updateBlocks([...blocks.slice(0, index + 1), heading, ...rest], { id: heading.id, at: "start" });
  }

  /**
   * Thanh "+" ở đáy mỗi ô văn bản (cả ô ngoài cùng của tài liệu): thêm mục nằm trong ô văn bản đó, tức nhánh
   * con của mục (ở ô ngoài cùng là mục ngoài cùng).
   */
  function renderQuickAdd(section: DocSection): ReactNode {
    // Luôn hiện để người mới thấy chỗ thêm mục (yêu cầu 2026-11-10); mục tầng 3 không thêm mục con.
    if (!canAddChild(section)) return null;
    return (
      <div className="flex flex-wrap items-center gap-1 pt-1" data-testid={`quick-add-${section.heading?.id ?? "DOCUMENT"}`}
        onMouseDown={(event) => event.preventDefault()}>
        <AddBar label={section.heading ? msg("shared.document.DocumentEditor.addPoint") : msg("shared.document.DocumentEditor.addQuestion")} onClick={() => addChild(section)} />
      </div>
    );
  }

  /**
   * Một mục: khung màu theo tầng có số và tiêu đề (chỉ ghi chữ tiêu đề) và luôn một ô văn bản (giấy trắng) chứa văn bản của
   * mục; mục con nằm trong ô văn bản đó. Người soạn có một dòng cuối và một thanh "+" ở đáy mỗi ô văn bản; mục đã có mục
   * con thì dòng cuối nằm dưới các mục con, không còn dòng cuối hay "+" nào ở giữa (yêu cầu 2026-11-02).
   * {@code only}: chế độ Từng phần, chỉ vẽ nhánh dẫn tới phần đó.
   */
  /**
   * Mục con kèm văn bản của mục cha nằm ngay sau từng mục con (yêu cầu 2026-11-27: chữ viết giữa hai mục con ở đúng chỗ).
   * Chế độ Từng phần chỉ vẽ nhánh đang xem nhưng vẫn giữ văn bản của mục cha.
   */
  function renderChildren(section: DocSection, shown: DocSection[], problem: DocSection | null, only: DocSection | null): ReactNode {
    return section.children.map((child) => (
      <Fragment key={`children-${child.heading?.id}`}>
        {shown.includes(child) && renderSection(child, problem, only)}
        {/* Từng phần: đoạn sau một mục chỉ hiện ở trang riêng của nó. */}
        {(!only || only.segment?.child === child) && child.trailing.map(renderBlock)}
      </Fragment>
    ));
  }

  function renderSection(section: DocSection, problem: DocSection | null = null, only: DocSection | null = null): ReactNode {
    const heading = section.heading;
    const key = heading?.id ?? "DOCUMENT";
    // Trang của một đoạn sau mục con: đi theo nhánh tới mục cha của đoạn; mục cha không vẽ văn bản mở đầu của nó.
    const target = only?.segment ? only.segment.parent : only;
    const children = target ? section.children.filter((child) => contains(child, target)) : section.children;
    const segmentHere = only?.segment?.parent === section;
    if (!heading) {
      return (
        <Fragment key={key}>
          {guided && section.leaf && (blocks.length > 0 || rootWritable) ? renderLeafContent(section, key) : <>
            {section.blocks.map(renderBlock)}
            {section.leaf && (blocks.length > 0 || rootWritable) && renderEmptyLeaf(section, key)}
          </>}
          {authoring && section.children.length === 0 && <>{renderTailLine(section, key)}{renderQuickAdd(section)}</>}
          {renderChildren(section, children, null, only)}
          {authoring && !only && section.children.length > 0 && <>{renderTailLine(section, key, true)}{renderQuickAdd(section)}</>}
        </Fragment>
      );
    }
    const owner = heading.type === "HEADING" && heading.content.style === "H1" ? section : problem;
    const active = highlightBlockId === heading.id;
    const writable = !readOnly && (!student || isInPart(heading.id));
    const headingStyle = heading.type === "HEADING" ? heading.content.style : "H1";
    // Kéo một mục cùng cha qua mục này: hiện bản xem trước mờ ở chỗ sẽ thả.
    const dragged = dragKey ? sectionByKey.get(dragKey) : undefined;
    const droppable = !!dragged && dragged !== section && parentOf.get(dragged) === parentOf.get(section);
    const showGhost = (place: "before" | "after") => droppable && dropAt?.key === key && dropAt.place === place && dragged?.heading ? (
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
      {showGhost("before")}
      <section data-section={key} data-leaf={section.leaf || undefined}
        style={{ backgroundColor: SECTION_BG[Math.min(section.depth - 1, SECTION_BG.length - 1)] }}
        onMouseOver={authoring ? (event) => { event.stopPropagation(); startHover(key); } : undefined}
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
            title={authoring ? msg("shared.document.DocumentEditor.dragToReorderQuestions") : undefined}
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
          {segmentHere ? null : section.leaf ? renderLeafContent(section, key) : section.blocks.map(renderBlock)}
          {authoring && children.length === 0 && renderTailLine(section, key)}
          {renderChildren(section, children, owner, only)}
          {authoring && children.length > 0 && renderTailLine(section, key, true)}
          {authoring && renderQuickAdd(section)}
        </div>
      </section>
      {showGhost("after")}
      </Fragment>
    );
  }

  const findTop = (target: DocSection) => tree.children.find((child) => contains(child, target)) ?? target;
  /**
   * Một trang Từng phần: mục kèm khung các mục cha; trang của đoạn nằm sau một mục chỉ có đoạn đó, trong khung của mục cha
   * (đoạn ở ngoài mọi mục thì không khung).
   */
  function renderPage(page: DocSection): ReactNode {
    const segment = page.segment;
    if (!segment) return renderSection(findTop(page), null, page);
    if (!segment.parent.heading) return <Fragment key={pageKey(page)}>{segment.child.trailing.map(renderBlock)}</Fragment>;
    return renderSection(findTop(segment.parent), null, page);
  }
  latestLeaves.current = leaves;
  reportCurrent.current = () => {
    let current: string | null = currentLeaf ? pageKey(currentLeaf) : pinned.current;
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
    <nav aria-label={place === "top" ? msg("shared.document.DocumentEditor.switchPartTopOfPage") : msg("shared.document.DocumentEditor.switchPart")} data-testid={place === "top" ? "step-nav-top" : "step-nav"}
      className={cx("flex items-center justify-between gap-2", place === "top" ? "-mt-6 mb-4 border-b border-line pb-3 sm:-mt-8" : "mt-4 border-t border-line pt-3")}>
      <button type="button" aria-label={msg("shared.document.DocumentEditor.previousPart")} title={msg("shared.document.DocumentEditor.previousPart")} disabled={stepIndex === 0} onClick={() => goPage(stepIndex - 1)}
        className="inline-flex size-8 items-center justify-center rounded-lg text-body hover:bg-sunken disabled:opacity-35">
        <Icon name="arrow_back" size={17} />
      </button>
      {place === "bottom" && <span className="sr-only" role="status">{msg("shared.document.DocumentEditor.part")}{stepIndex + 1} / {leaves.length}</span>}
      <button type="button" aria-label={msg("shared.document.DocumentEditor.nextPart")} title={msg("shared.document.DocumentEditor.nextPart")} disabled={stepIndex >= leaves.length - 1} onClick={() => goPage(stepIndex + 1)}
        className="inline-flex size-8 items-center justify-center rounded-lg text-body hover:bg-sunken disabled:opacity-35">
        <Icon name="arrow_forward" size={17} />
      </button>
    </nav>
  );
  // Nút chọn cách đọc (Cả trang / Từng phần): trên header của trang nếu trang đưa chỗ ({@code viewSlot}), không thì trên thanh
  // công cụ.
  // Luôn hiện, kể cả khi bài chưa có câu hỏi (yêu cầu 2026-11-10).
  const viewToggle = (
    <span role="radiogroup" aria-label={msg("shared.document.DocumentEditor.viewMode")} className="flex items-center gap-0.5 rounded-lg bg-line/60 p-0.5">
      <ViewButton label={msg("shared.document.DocumentEditor.wholePage")} on={!stepping} onClick={() => {
        pinned.current = currentLeaf ? pageKey(currentLeaf) : null;
        // Về Cả trang thì cuộn tới đúng mục đang xem, không về đầu trang.
        const id = currentLeaf?.heading?.id;
        setView("page");
        if (id) requestAnimationFrame(() => scrollToSection(id));
      }}><PageGlyph /></ViewButton>
      <ViewButton label={msg("shared.document.DocumentEditor.partByPart")} on={stepping} onClick={() => {
        // Mở đúng trang của mục đang xem (theo chỗ đã cuộn tới), không thì theo chỗ con trỏ (yêu cầu 2026-11-05).
        const viewing = lastCurrent.current ? leaves.findIndex((leaf) => pageKey(leaf) === lastCurrent.current) : -1;
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
    if (child.heading) numbers.set(child.heading.id, sectionNumbers?.[child.heading.id] ?? formatSectionNumber(counters));
    numberParts(child, counters);
  });
  numberParts(tree, []);
  // Đoạn văn cuối của mỗi khung có dòng cuối: id đoạn -> id dòng cuối.
  const tailOf = new Map<string, string>();
  const mapTails = (section: DocSection) => {
    const hasTail = section.children.length === 0;
    const last = section.blocks.at(-1);
    if (hasTail && last?.type === "PARAGRAPH") tailOf.set(last.id, `tail-${section.heading?.id ?? "DOCUMENT"}`);
    const lastAfter = section.children.at(-1)?.trailing.at(-1);
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
  const selectedIds = new Set(allSelected?.ids.filter((id) => blocks.some((b) => b.id === id)) ?? []);

  /**
   * Văn bản liền một khối của {@code section} chứa con trỏ: đoạn mở đầu (trước mục con đầu tiên) hoặc đoạn nằm ngay sau
   * một mục con (yêu cầu 2026-11-27: chữ giữa hai mục con là một đoạn riêng, không gộp với đoạn mở đầu). Dòng cuối ở đáy
   * ô thuộc đoạn sau mục con cuối cùng.
   */
  function findCursorSegment(section: DocSection): DocumentBlock[] {
    const last = section.children.at(-1);
    if (cursorLine) return cursorLine.tailKey.startsWith("tail-end-") && last ? last.trailing : section.blocks;
    const owner = section.children.find((child) => child.trailing.some((b) => b.id === focused?.id));
    return owner ? owner.trailing : section.blocks;
  }
  /**
   * Ctrl+A ở đoạn văn, danh sách hay dòng cuối: chọn cả đoạn văn bản liền khối đang có con trỏ (không gồm tiêu đề mục, mục
   * con và đoạn văn bản ở phía bên kia mục con). Tên bài, tiêu đề mục, ô bảng, hay đoạn chỉ có đúng khối đang viết thì để
   * trình duyệt tự chọn hết ô. Trả false nếu không xử lý.
   */
  function selectPart(): boolean {
    if (inHeader) return false;
    if (!cursorLine && (!focused || focused.type === "HEADING" || focused.type === "TABLE")) return false;
    const section = cursorSection;
    if (!section) return false;
    const own = findCursorSegment(section).filter((b) => !isBlockLocked(b));
    if (own.length === 0) return false;
    if (!cursorLine && own.length === 1 && own[0]!.id === focused?.id && focused.type === "PARAGRAPH") return false;
    setAllSelected({ key: section.heading?.id ?? "DOCUMENT", ids: own.map((b) => b.id) });
    globalThis.document.getSelection()?.removeAllRanges();
    paper.current?.focus({ preventScroll: true });
    return true;
  }
  function selectedText(): string {
    return blocks.filter((b) => selectedIds.has(b.id)).map(blockText).filter(Boolean).join("\n");
  }
  /**
   * Thay cả phần đang chọn bằng một đoạn chữ {@code text} (gõ đè, dán); null là xóa. Người soạn xóa hết thì con trỏ về dòng
   * cuối của phần; người học (không có dòng cuối) còn lại một đoạn trống để viết tiếp. Hoàn tác được.
   */
  function replaceSelected(text: string | null) {
    const section = allSelected ? sectionByKey.get(allSelected.key) : undefined;
    setAllSelected(null);
    if (!section || selectedIds.size === 0) return;
    const at = blocks.findIndex((b) => selectedIds.has(b.id));
    const under = findUnderLevel(blocks[at]!);
    // Xóa đoạn có dòng cuối ngay dưới thì con trỏ về dòng cuối; đoạn ở giữa hai mục con thì còn một dòng trống giữ chỗ.
    const tailKey = section.children.length === 0 ? `tail-${allSelected!.key}`
      : section.children.at(-1)!.trailing.some((b) => selectedIds.has(b.id)) ? `tail-end-${allSelected!.key}` : null;
    if (text === null && authoring && tailKey) {
      let next = blocks;
      selectedIds.forEach((id) => { next = removeFromList(next, id); });
      updateBlocks(next, { id: tailKey, at: "start" });
      return;
    }
    const line: DocumentBlock = { id: generateId(), origin, type: "PARAGRAPH",
      content: { runs: text ? [{ text }] : EMPTY_RUNS, ...(under !== null ? { under } : {}) } };
    const rest = blocks.filter((b) => !selectedIds.has(b.id));
    setFocusedId(line.id);
    updateBlocks([...rest.slice(0, at), line, ...rest.slice(at)], { id: line.id, at: "end" });
  }
  /** Phím khi cả phần đang được chọn; trả true nếu đã xử lý. */
  function handleSelectedKey(event: KeyboardEvent<HTMLDivElement>): boolean {
    const key = event.key;
    const command = event.ctrlKey || event.metaKey;
    if (key === "Delete" || key === "Backspace") {
      event.preventDefault();
      replaceSelected(null);
      return true;
    }
    if (!command && !event.altKey && key.length === 1) {
      event.preventDefault();
      replaceSelected(key);
      return true;
    }
    if (command && key.toLowerCase() === "a") {
      event.preventDefault();
      return true;
    }
    if (key === "Escape" || key === "Enter" || key.startsWith("Arrow") || key === "Home" || key === "End" || key === "PageUp" || key === "PageDown") {
      event.preventDefault();
      const ordered = blocks.filter((b) => selectedIds.has(b.id));
      const toStart = key === "ArrowUp" || key === "ArrowLeft" || key === "Home" || key === "PageUp";
      const edge = toStart ? ordered[0] : ordered.at(-1);
      setAllSelected(null);
      if (edge) focusField(paper.current, { id: edge.id, at: toStart ? "start" : "end" });
      return true;
    }
    // Ctrl+Z, Ctrl+Y bỏ vùng chọn rồi hoàn tác như thường; Ctrl+C, X, V đi qua sự kiện chép, cắt, dán.
    if (command && (key.toLowerCase() === "z" || key.toLowerCase() === "y")) setAllSelected(null);
    return false;
  }


  return (
    <div ref={root} className="flex flex-col gap-3" data-testid="document-editor"
      onCopyCapture={(event) => {
        if (!allSelected) return;
        event.preventDefault();
        event.clipboardData.setData("text/plain", selectedText());
      }}
      onCutCapture={(event) => {
        if (!allSelected || readOnly) return;
        event.preventDefault();
        event.clipboardData.setData("text/plain", selectedText());
        replaceSelected(null);
      }}
      onPasteCapture={(event) => {
        if (!allSelected || readOnly) return;
        event.preventDefault();
        event.stopPropagation();
        replaceSelected(event.clipboardData.getData("text/plain").replace(/\r\n?/g, "\n"));
      }}
      onKeyDownCapture={(event) => {
        if (readOnly) return;
        if (allSelected && handleSelectedKey(event)) return;
        if (!(event.ctrlKey || event.metaKey)) return;
        const key = event.key.toLowerCase();
        if (key === "a" && !event.shiftKey && !event.altKey) { if (selectPart()) event.preventDefault(); }
        else if (key === "z" && !event.shiftKey) { event.preventDefault(); undoChange(); }
        else if (key === "y" || (key === "z" && event.shiftKey)) { event.preventDefault(); redoChange(); }
        else if ((key === "b" || key === "i" || key === "u") && (formatTarget || allSelected)) {
          event.preventDefault();
          toggle(key === "b" ? "bold" : key === "i" ? "italic" : "underline");
        }
      }}>
      {viewSlot && viewToggle && createPortal(viewToggle, viewSlot)}
      {readOnly && toolbarCenter && (
        // Cùng lưới với trang giấy và cột ghi chú bên dưới, để nút giữa nằm thẳng giữa trang giấy.
        <div ref={measureToolbar} role="toolbar" aria-label={msg("shared.document.DocumentEditor.toolbar")}
          className={`sticky top-[var(--doc-toolbar-top,4rem)] z-20 rounded-xl border border-line bg-sunken px-3 py-1 ${TOOLBAR_COVER}`}>
          <div className={rail ? "grid items-center justify-center gap-5" : "grid grid-cols-1 items-center gap-2 sm:grid-cols-[1fr_auto_1fr] sm:gap-0"}
            style={rail ? { gridTemplateColumns: `minmax(0,816px) ${railWidth}px` } : undefined}>
            {!rail && <span className="hidden sm:block" />}
            <div className="flex items-center justify-center gap-0.5">{toolbarCenter}</div>
            {/* Chỉ đọc (lịch sử nộp bài) cũng đổi được Cả trang / Từng phần (yêu cầu 2026-11-05). */}
            <div className="flex items-center justify-end gap-0.5">{!viewSlot && viewToggle}{toolbarExtra}</div>
          </div>
        </div>
      )}
      {(!readOnly || (toolbarExtra && !toolbarCenter)) && (
        <div ref={measureToolbar} role="toolbar" aria-label={msg("shared.document.DocumentEditor.formatAndInsert")}
          className={`sticky top-[var(--doc-toolbar-top,4rem)] z-20 flex flex-wrap items-center gap-0.5 rounded-xl bg-sunken px-3 py-1 border border-line ${TOOLBAR_COVER}`}>
          {!readOnly && (
            <>
              <ToolbarButton title={msg("shared.document.DocumentEditor.undoCtrlZ")} aria-label={msg("shared.document.DocumentEditor.undo")} disabled={history.past === 0} onClick={undoChange}><Glyph name="undo" /></ToolbarButton>
              <ToolbarButton title={msg("shared.document.DocumentEditor.redoCtrlY")} aria-label={msg("shared.document.DocumentEditor.redo")} disabled={history.future === 0} onClick={redoChange}><Glyph name="redo" /></ToolbarButton>
              <Separator />
              {!student && (
                <>
                  <ToolbarButton title={msg("shared.document.DocumentEditor.addAQuestionAtThe")} aria-label={msg("shared.document.DocumentEditor.addQuestion")} className="gap-1 pr-2"
                    data-testid="add-block" disabled={!addSectionAllowed || !canAddChild(addAtEnd ? tree : cursorSection ?? tree)}
                    onClick={() => (addAtEnd ? addChild(tree) : addAtCursor(cursorSection))}>
                    <Icon name="add" size={16} />{msg("shared.document.DocumentEditor.question")}</ToolbarButton>
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
              <ToolPopover title={msg("shared.document.DocumentEditor.textColor")} disabled={!formatTarget}
                label={<span className="flex flex-col items-center leading-none"><span className="font-semibold">A</span><span className="mt-0.5 h-1 w-4 rounded-sm" style={{ backgroundColor: shownRuns[0]?.color ?? "#202124" }} /></span>}>
                {(close) => <Swatches colors={TEXT_COLORS} noneLabel={msg("shared.document.DocumentEditor.default")} onPick={(color) => { paint("color", color); close(); }} />}
              </ToolPopover>
              <ToolPopover title={msg("shared.document.DocumentEditor.highlightColor")} disabled={!formatTarget} label={<Glyph name="highlight" />}>
                {(close) => <Swatches colors={HIGHLIGHTS} noneLabel={msg("shared.document.DocumentEditor.noHighlight")} onPick={(color) => { paint("highlight", color); close(); }} />}
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
              <ToolbarButton title={msg("shared.document.DocumentEditor.bulletedList")} aria-label={msg("shared.document.DocumentEditor.bulletedList")} disabled={!listAllowed} active={tailStyle ? tailStyle.list === "UL" : style === "UL"}
                onClick={() => toggleList("UL")}><Glyph name="bullets" /></ToolbarButton>
              <ToolbarButton title={msg("shared.document.DocumentEditor.numberedList")} aria-label={msg("shared.document.DocumentEditor.numberedList")} disabled={!listAllowed} active={tailStyle ? tailStyle.list === "OL" : style === "OL"}
                onClick={() => toggleList("OL")}><Glyph name="numbers" /></ToolbarButton>
              <Separator />
              <ToolPopover title={msg("shared.document.DocumentEditor.insertTable")} disabled={!insertAllowed} label={<Glyph name="table" />}>
                {(close) => <TablePicker onPick={(rows, cols) => { insertTable(rows, cols); close(); }} />}
              </ToolPopover>
              {uploadImage && (
                <label title={msg("shared.document.DocumentEditor.insertImage")} aria-label={msg("shared.document.DocumentEditor.insertImage")} onMouseDown={(event) => event.preventDefault()}
                  className={cx("inline-flex h-7 min-w-7 items-center justify-center rounded-md px-1.5 text-body transition-colors", insertAllowed ? "cursor-pointer hover:bg-line-strong focus-within:bg-line-strong has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-focus" : "pointer-events-none opacity-35")}>
                  <Glyph name="image" />
                  <input type="file" accept={IMAGE_TYPES} className="sr-only" data-testid="add-IMAGE" disabled={!insertAllowed}
                    onChange={(event) => { const file = event.target.files?.[0]; if (file) insertImage(file); event.target.value = ""; }} />
                </label>
              )}
              {diagramEnabled && (
                <ToolPopover title={msg("shared.document.DocumentEditor.insertDrawIoDiagram")} disabled={!insertAllowed} label={<Glyph name="diagram" />}>
                  {(close) => <DiagramTemplateMenu onPick={(template) => { void insertDiagram(template); close(); }} />}
                </ToolPopover>
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

      <div className={rail ? cx("grid items-start gap-5", !fullWidth && "justify-center") : cx("mx-auto w-full", !fullWidth && "max-w-[816px]")}
        style={rail ? { gridTemplateColumns: `${fullWidth ? "minmax(0,1fr)" : "minmax(0,816px)"} ${railWidth}px` } : undefined}>
        <div ref={paper} data-testid="document-paper" tabIndex={-1} onMouseLeave={stopHover} onMouseOver={stopHover}
          onMouseDownCapture={() => { if (allSelected) setAllSelected(null); }}
          onFocusCapture={(event) => { if (allSelected && event.target !== paper.current) setAllSelected(null); }}
          onBlurCapture={(event) => {
            if (event.target === paper.current && allSelected) setAllSelected(null);
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
          className={cx("min-h-[60vh] min-w-0 rounded-lg border outline-none border-line bg-surface px-5 pt-10 pb-4 shadow-[var(--shadow-lift)] sm:pt-12", fullWidth ? "sm:px-8" : "sm:px-14")}>
          {/* Khung bọc nội dung (không có min-height) để cột ghi chú biết khi nào block đổi chỗ. */}
          {stepping && stepNav("top")}
          <div ref={content}>
            {titleField && (
              // Thiếu tên bài khi hoàn tất, xếp lịch: viền đỏ quanh cả ô tên (yêu cầu 2026-11-12), hết khi gõ tên.
              <div data-block-id="TITLE" data-invalid={titleField.invalid || undefined}
                className={cx("mb-2", titleField.invalid && "rounded-lg px-2 py-1 outline-2 outline-danger")} onFocusCapture={() => { setInHeader(true); if (cursorLine) setCursorLine(null); }}>
                {titleField.readOnly || readOnly
                  ? <h2 className={cx(TITLE_CLASS, alignClass(document.title?.align))}>{titleField.text ? <RunsView runs={titleRuns} /> : msg("shared.document.DocumentEditor.untitledAssignment")}</h2>
                  : <RichText aria-label={msg("shared.document.DocumentEditor.assignmentTitle")} runs={titleRuns} placeholder={titleField.placeholder}
                      className={cx(TITLE_CLASS, alignClass(document.title?.align), titleField.invalid && "rounded-md bg-danger-soft")}
                      onChange={(runs) => setTitle({ ...document.title, runs })}
                      onKey={(event) => { if (event.key === "Enter") event.preventDefault(); }} />}
              </div>
            )}
            {header}
            {listInlineComments(null)}
            {blocks.length === 0 && (readOnly
              ? <div className="h-16" />
              : student && <button type="button" aria-label={msg("shared.document.DocumentEditor.startWriting")} data-testid="write-document"
                onClick={() => insertAfter(null, createBlock("PARAGRAPH", origin))} className="block h-16 w-full cursor-text" />)}
            {currentLeaf ? renderPage(currentLeaf) : renderSection(tree)}
          </div>
          {student && !readOnly && !stepping && blocks.length > 0 && atEndInPart && (
            <button type="button" aria-label={msg("shared.document.DocumentEditor.continueWritingAtTheEnd")} onClick={continueAtEnd} className="block h-14 w-full cursor-text" />
          )}
          {stepping && stepNav("bottom")}
        </div>
        {(() => {
          const column = sidePanel ? <aside data-testid="document-side-panel" className={wide ? "sticky min-w-0 overflow-y-auto" : undefined}
            style={wide ? {
              top: "calc(var(--doc-toolbar-top, 4rem) + var(--doc-toolbar-height, 2.25rem) + 0.5rem)",
              maxHeight: "calc(100dvh - var(--doc-toolbar-top, 4rem) - var(--doc-toolbar-height, 2.25rem) - 1rem)",
            } : undefined}>{sidePanel}</aside>
            : rail && <CommentRail paper={content} comments={visible} activeKey={activeComment} matchSectionHeight={matchCommentHeight} />;
          if (!resizableRail || !rail) return column;
          return <div className="relative min-w-0 self-stretch"><RailResizer width={railWidth} onWidth={setRailWidth} />{column}</div>;
        })()}
      </div>

      {editing && editing.type === "DIAGRAM" && (
        <DiagramEditor key={editing.id} xml={editing.content.xml} title={editing.content.alt || "Draw.io"}
          onClose={() => setDiagram(null)}
          onSave={(xml, svg, png) => replace(editing.id, { ...editing, content: { ...editing.content, xml,
            ...(svg ? { svgPreview: svg } : {}), ...(png ? { pngPreview: png } : {}),
            // Người học đã vẽ trên sơ đồ mẫu của đề: từ giờ khung hiện hình của người học (yêu cầu 2026-11-27).
            ...(student && editing.origin === "TEACHER" ? { studentEdited: true } : {}) } })} />
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
        <RichText aria-label={msg("shared.document.DocumentEditor.headingContent")} placeholder={msg("shared.document.DocumentEditor.questionTitle")} runs={readHeadingRuns(block)}
          className={cx(HEADING_CLASS[block.content.style] ?? HEADING_CLASS.H1, alignClass(block.content.align))}
          onChange={(runs) => ops.change(replaceHeadingRuns(block, runs))}
          onKey={(event, caret) => handleKeys(event, caret, {
            enter: () => ops.split(block, createParagraph(block.origin)),
            removeEmpty: ops.removeEmpty,
            step: ops.step,
          })} />
      );
    case "PARAGRAPH":
      return (
        <RichText aria-label={msg("shared.document.DocumentEditor.paragraph")} placeholder={placeholder} runs={block.content.runs} className={cx(PARAGRAPH_CLASS, alignClass(block.content.align))}
          onChange={(runs) => ops.change({ ...block, content: { ...block.content, runs } })}
          onKey={(event, caret) => handleKeys(event, caret, {
            enter: ({ start, end }) => {
              const [before, after] = splitRuns(block.content.runs, start, end);
              ops.split({ ...block, content: { ...block.content, runs: before } }, createParagraph(block.origin, after, block.content.align));
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
          <label title={pick ? msg("shared.document.DocumentEditor.chooseImage") : undefined} className={cx("block w-full", pick && "cursor-pointer")}>
            {src ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={src} alt={block.content.alt ?? ""} style={{ width: block.content.width ? `${block.content.width}px` : undefined }} className="mx-auto max-w-full" />
            ) : (
              <span className="flex w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-line-strong p-6 text-xs text-muted hover:border-primary hover:text-primary-ink">
                <Icon name="upload_file" size={20} />{block.content.fileRef ? msg("shared.document.DocumentEditor.loadingImage") : msg("shared.document.DocumentEditor.chooseImage")}
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
  function setItems(next: ListItem[], focus?: { field: number; at: "start" | "end" }) {
    ops.change({ ...block, content: { ...block.content, items: next } }, focus ? { id: block.id, ...focus } : undefined);
  }
  function findSibling(field: HTMLElement, direction: -1 | 1): HTMLElement | null {
    const li = field.closest("li");
    const other = direction < 0 ? li?.previousElementSibling : li?.nextElementSibling;
    return other?.querySelector<HTMLElement>("[data-editable]") ?? null;
  }
  function handleItemKeys(event: KeyboardEvent<HTMLDivElement>, caret: Caret, item: ListItem, i: number) {
    const field = event.currentTarget;
    if (event.key === "Tab") {
      event.preventDefault();
      const level = Math.min(4, Math.max(0, (item.level ?? 0) + (event.shiftKey ? -1 : 1)));
      setItems(items.map((x, j) => (j === i ? { ...x, level } : x)));
    } else if (event.key === "Enter") {
      event.preventDefault();
      if (caret.length === 0) {
        const rest = items.filter((_, j) => j !== i);
        if (rest.length === 0) ops.change(convertBlock(block, "P"), { id: block.id, at: "start" });
        else ops.split({ ...block, content: { ...block.content, items: rest } }, createParagraph(block.origin, [{ ...formatOf(item.runs[0]), text: "" }]));
        return;
      }
      const [before, after] = splitRuns(item.runs, caret.start, caret.end);
      setItems([...items.slice(0, i), { ...item, runs: before }, { level: item.level ?? 0, runs: after }, ...items.slice(i + 1)], { field: i + 1, at: "start" });
    } else if (event.key === "Backspace" && caret.length === 0) {
      event.preventDefault();
      if (items.length === 1) ops.removeEmpty();
      else setItems(items.filter((_, j) => j !== i), { field: Math.max(0, i - 1), at: i > 0 ? "end" : "start" });
    } else if (event.key === "ArrowUp" && caret.start === 0 && caret.end === 0) {
      event.preventDefault();
      const other = findSibling(field, -1);
      if (other) moveCaret(other, "end"); else ops.step(-1);
    } else if (event.key === "ArrowDown" && caret.start === caret.length) {
      event.preventDefault();
      const other = findSibling(field, 1);
      if (other) moveCaret(other, "start"); else ops.step(1);
    }
  }
  return (
    <Tag className={cx(block.content.ordered ? "list-decimal" : "list-disc", "pl-6", PARAGRAPH_CLASS, alignClass(block.content.align))}>
      {items.map((item, i) => (
        <li key={i} style={{ marginLeft: `${(item.level ?? 0) * 24}px` }}>
          <RichText aria-label={msg("shared.document.DocumentEditor.section", { value: String(i + 1) })} runs={item.runs} className={cx(PARAGRAPH_CLASS, alignClass(block.content.align))}
            onChange={(runs) => setItems(items.map((x, j) => (j === i ? { ...x, runs } : x)))}
            onKey={(event, caret) => handleItemKeys(event, caret, item, i)} />
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
  const createEmptyCell = (): TableCell => ({ runs: [{ text: "" }] });
  const createFreshCell = (): TableCell => (guard ? { runs: [{ text: "" }], added: true } : createEmptyCell());
  const fillRow = (cells: TableCell[]) => Array.from({ length: columns }, (_, j) => cells[j] ?? createEmptyCell());
  // Đổi số cột thì bỏ độ rộng đã kéo, các cột lại bằng nhau.
  function setCells(next: TableCell[][]) {
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
    const stopResize = () => { window.removeEventListener("mousemove", move); window.removeEventListener("mouseup", stopResize); };
    window.addEventListener("mousemove", move);
    window.addEventListener("mouseup", stopResize);
  }
  const canDeleteRow = (r: number) => !guard || rows[r]!.every((cell) => cell.added);
  const canDeleteColumn = (c: number) => !guard || rows.every((cells) => !cells[c] || cells[c]!.added);
  const insertRow = (r: number) => setCells([...rows.slice(0, r + 1), Array.from({ length: columns }, createFreshCell), ...rows.slice(r + 1)]);
  const insertColumn = (c: number) => setCells(rows.map((cells) => { const all = fillRow(cells); return [...all.slice(0, c + 1), createFreshCell(), ...all.slice(c + 1)]; }));
  const deleteRow = (r: number) => (rows.length <= 1 && !guard ? onRemove() : setCells(rows.filter((_, i) => i !== r)));
  const deleteColumn = (c: number) => (columns <= 1 && !guard ? onRemove() : setCells(rows.map((cells) => fillRow(cells).filter((_, j) => j !== c))));
  const describeRow = (r: number) => (!canDeleteRow(r) ? msg("shared.document.DocumentEditor.assignmentRow") : rows.length <= 1 && !guard ? msg("shared.document.DocumentEditor.deleteTable") : msg("shared.document.DocumentEditor.deleteRow"));
  const describeColumn = (c: number) => (!canDeleteColumn(c) ? msg("shared.document.DocumentEditor.assignmentColumn") : columns <= 1 && !guard ? msg("shared.document.DocumentEditor.deleteTable") : msg("shared.document.DocumentEditor.deleteColumn"));
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
                  <RichText aria-label={msg("shared.document.DocumentEditor.cell", { value: String(r + 1), value2: String(c + 1) })} runs={row[c]?.runs ?? EMPTY_RUNS} onFocus={() => setFocusCol(c)}
                    className={cx("px-2 py-1.5 text-[13px] leading-snug text-ink", header && r === 0 && "font-semibold", alignClass(row[c]?.align))}
                    onChange={(runs) => setCells(rows.map((cells, i) => i !== r ? cells : fillRow(cells).map((cell, j) =>
                      j === c ? { ...cell, runs } : cell)))} />
                  {r === 0 && c < columns - 1 && (
                    <span role="separator" aria-orientation="vertical" aria-label={msg("shared.document.DocumentEditor.dragToResizeColumn", { value: String(c + 1) })} data-testid={`column-resize-${c}`}
                      onMouseDown={(event) => startResize(c, event)}
                      className="absolute top-0 -right-[3px] z-10 h-full w-1.5 cursor-col-resize hover:bg-primary/40" />
                  )}
                  {r === 0 && (
                    // Đệm thay cho lề để không có khe giữa ô và nút: rê từ ô lên nút không làm nút ẩn đi.
                    <span data-testid={`column-tools-${c}`}
                      className={cx("absolute bottom-full left-1/2 z-10 flex -translate-x-1/2 gap-[3px] pb-[3px] transition-opacity",
                        activeCol === c ? "opacity-100" : "pointer-events-none opacity-0")}>
                      {columns < 30 && <Square label={msg("shared.document.DocumentEditor.insertColumnToTheRight")} onClick={() => insertColumn(c)}>+</Square>}
                      <Square danger label={describeColumn(c)} disabled={!canDeleteColumn(c)} onClick={() => deleteColumn(c)}>−</Square>
                    </span>
                  )}
                  {c === 0 && (
                    <span data-testid={`row-tools-${r}`}
                      className="pointer-events-none absolute top-1/2 right-full z-10 flex -translate-y-1/2 gap-[3px] pr-1.5 opacity-0 transition-opacity group-focus-within/row:pointer-events-auto group-focus-within/row:opacity-100 group-hover/row:pointer-events-auto group-hover/row:opacity-100">
                      {rows.length < 200 && <Square label={msg("shared.document.DocumentEditor.insertRowBelow")} onClick={() => insertRow(r)}>+</Square>}
                      <Square danger label={describeRow(r)} disabled={!canDeleteRow(r)} onClick={() => deleteRow(r)}>−</Square>
                    </span>
                  )}
                  {r === 0 && c === 0 && (
                    // ✕ xóa bảng ở góc trên trái: cùng hàng với nút của cột, thẳng cột với nút của hàng; bảng của đề thì mờ.
                    <span className="pointer-events-none absolute right-full bottom-full z-10 flex pr-1.5 pb-[3px] opacity-0 transition-opacity group-focus-within/block:pointer-events-auto group-focus-within/block:opacity-100 group-hover/block:pointer-events-auto group-hover/block:opacity-100">
                      <Square danger label={guard ? msg("shared.document.DocumentEditor.assignmentTable") : msg("shared.document.DocumentEditor.deleteTable")} disabled={guard} onClick={onRemove}>✕</Square>
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
