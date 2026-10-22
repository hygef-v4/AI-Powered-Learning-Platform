"use client";

import { useLayoutEffect, useRef, type ClipboardEvent, type KeyboardEvent } from "react";
import { cx } from "@/shared/ui";
import { formatOf, hasFormat, insertText, normalizeRuns, runsLength, sameRuns, type RunFormat } from "./runs";
import type { Run } from "./types";

const HEX = /^#[0-9a-fA-F]{6}$/;

/** Vùng chọn trong ô, tính theo ký tự; con trỏ khi start = end. */
export type Caret = { start: number; end: number; length: number };

/** Vùng chọn hiện tại trong ô; vùng chọn không nằm trong ô thì coi như con trỏ ở cuối ô. */
export function getCaret(root: HTMLElement): Caret {
  const length = (root.textContent ?? "").length;
  const selection = root.ownerDocument.getSelection();
  if (!selection || selection.rangeCount === 0) return { start: length, end: length, length };
  const range = selection.getRangeAt(0);
  if (!root.contains(range.startContainer) || !root.contains(range.endContainer)) return { start: length, end: length, length };
  const offset = (node: Node, at: number) => {
    const before = root.ownerDocument.createRange();
    before.selectNodeContents(root);
    before.setEnd(node, at);
    return before.toString().length;
  };
  return { start: offset(range.startContainer, range.startOffset), end: offset(range.endContainer, range.endOffset), length };
}

/** Đặt vùng chọn [start, end) trong ô (con trỏ khi không có end). */
export function setCaret(root: HTMLElement, start: number, end = start) {
  const doc = root.ownerDocument;
  const place = (target: number): [Node, number] => {
    const walker = doc.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    let pos = 0;
    let last: Text | null = null;
    for (let node = walker.nextNode() as Text | null; node; node = walker.nextNode() as Text | null) {
      if (target <= pos + node.data.length) return [node, target - pos];
      pos += node.data.length;
      last = node;
    }
    return last ? [last, last.data.length] : [root, root.childNodes.length];
  };
  const [startNode, startOffset] = place(start);
  const [endNode, endOffset] = place(end);
  const range = doc.createRange();
  range.setStart(startNode, startOffset);
  range.setEnd(endNode, endOffset);
  const selection = doc.getSelection();
  selection?.removeAllRanges();
  selection?.addRange(range);
}

function escape(text: string): string {
  return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

/** HTML của các run: mỗi run một span, định dạng ghi ở data-* (để đọc lại) và style (để hiện). */
export function runsHtml(runs: Run[]): string {
  return runs.filter((run) => run.text).map((run) => {
    const data: string[] = [];
    const style: string[] = [];
    if (run.bold) { data.push("data-bold"); style.push("font-weight:700"); }
    if (run.italic) { data.push("data-italic"); style.push("font-style:italic"); }
    if (run.underline) data.push("data-underline");
    if (run.strike) data.push("data-strike");
    const lines = [run.underline ? "underline" : "", run.strike ? "line-through" : ""].filter(Boolean);
    if (lines.length > 0) style.push(`text-decoration:${lines.join(" ")}`);
    if (run.code) { data.push("data-code"); style.push("font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:0.92em"); }
    if (run.color && HEX.test(run.color)) { data.push(`data-color="${run.color}"`); style.push(`color:${run.color}`); }
    if (run.highlight && HEX.test(run.highlight)) { data.push(`data-highlight="${run.highlight}"`); style.push(`background-color:${run.highlight}`); }
    return `<span data-run ${data.join(" ")} style="${style.join(";")}">${escape(run.text)}</span>`;
  }).join("");
}

function hexOf(value: string): string | undefined {
  if (HEX.test(value)) return value.toUpperCase();
  const match = /^rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)$/.exec(value.trim());
  if (!match || (match[4] !== undefined && Number(match[4]) === 0)) return undefined;
  return `#${[match[1], match[2], match[3]].map((n) => Number(n).toString(16).padStart(2, "0")).join("").toUpperCase()}`;
}

/** Định dạng của một thẻ: span của ô đọc từ data-*; thẻ trình duyệt tự chèn khi xóa, gộp (b, i, span có style…) đọc theo thẻ và style. */
function formatFrom(element: HTMLElement, inherited: RunFormat): RunFormat {
  if (element.hasAttribute("data-run")) {
    const format: RunFormat = {};
    if (element.hasAttribute("data-bold")) format.bold = true;
    if (element.hasAttribute("data-italic")) format.italic = true;
    if (element.hasAttribute("data-underline")) format.underline = true;
    if (element.hasAttribute("data-strike")) format.strike = true;
    if (element.hasAttribute("data-code")) format.code = true;
    const color = element.getAttribute("data-color");
    if (color && HEX.test(color)) format.color = color;
    const highlight = element.getAttribute("data-highlight");
    if (highlight && HEX.test(highlight)) format.highlight = highlight;
    return format;
  }
  const format = { ...inherited };
  const tag = element.tagName;
  if (tag === "B" || tag === "STRONG") format.bold = true;
  if (tag === "I" || tag === "EM") format.italic = true;
  if (tag === "U") format.underline = true;
  if (tag === "S" || tag === "STRIKE" || tag === "DEL") format.strike = true;
  if (tag === "CODE") format.code = true;
  const style = element.style;
  if (style.fontWeight === "bold" || Number(style.fontWeight) >= 600) format.bold = true;
  if (style.fontStyle === "italic") format.italic = true;
  const decoration = `${style.textDecorationLine ?? ""} ${style.textDecoration ?? ""}`;
  if (decoration.includes("underline")) format.underline = true;
  if (decoration.includes("line-through")) format.strike = true;
  const color = style.color ? hexOf(style.color) : undefined;
  if (color) format.color = color;
  const highlight = style.backgroundColor ? hexOf(style.backgroundColor) : undefined;
  if (highlight) format.highlight = highlight;
  return format;
}

/** Các run đọc từ nội dung ô sau khi người dùng gõ, xóa. */
export function readRuns(root: HTMLElement): Run[] {
  const runs: Run[] = [];
  const walk = (node: Node, format: RunFormat) => {
    if (node.nodeType === Node.TEXT_NODE) {
      runs.push({ ...format, text: node.textContent ?? "" });
      return;
    }
    if (node.nodeType !== Node.ELEMENT_NODE) return;
    const element = node as HTMLElement;
    if (element.tagName === "BR") {
      runs.push({ ...format, text: "\n" });
      return;
    }
    const next = formatFrom(element, format);
    element.childNodes.forEach((child) => walk(child, next));
    if ((element.tagName === "DIV" || element.tagName === "P") && element.nextSibling) runs.push({ ...format, text: "\n" });
  };
  root.childNodes.forEach((child) => walk(child, {}));
  // Trình duyệt thêm một <br> giữ chỗ khi ô trống hoặc khi dòng cuối trống: không phải chữ của người dùng.
  if (root.lastChild?.nodeName === "BR" && runs.at(-1)?.text === "\n") runs.pop();
  return normalizeRuns(runs);
}

type Props = {
  runs: Run[];
  onChange: (runs: Run[]) => void;
  /** Phím của ô (Enter tách đoạn, Backspace, mũi tên…) kèm vùng chọn; xử lý thì gọi preventDefault. */
  onKey?: (event: KeyboardEvent<HTMLDivElement>, caret: Caret) => void;
  onFocus?: () => void;
  className?: string;
  "aria-label": string;
};

/**
 * Ô chữ định dạng được từng phần (yêu cầu 2026-10-22: bôi đen rồi chỉnh thì chỉ đổi phần bôi đen): contentEditable,
 * mỗi run một span. React không vẽ lại ô khi người dùng gõ (giữ con trỏ, gõ dấu tiếng Việt); chỉ viết lại nội dung khi
 * run từ ngoài khác run đang hiện (định dạng, hoàn tác…) rồi đặt lại vùng chọn như cũ. Dán chỉ lấy chữ thuần.
 */
export function RichText({ runs, onChange, onKey, onFocus, className, "aria-label": label }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  /** Run đang hiện trong ô; khác run nhận vào thì viết lại ô. */
  const shown = useRef<Run[] | null>(null);
  /** Chỗ đặt con trỏ sau lần viết lại kế tiếp (sau khi dán). */
  const caretAfter = useRef<number | null>(null);

  useLayoutEffect(() => {
    const root = ref.current;
    if (!root || (shown.current && sameRuns(shown.current, runs))) return;
    const caret = root.ownerDocument.activeElement === root ? getCaret(root) : null;
    root.innerHTML = runsHtml(runs);
    shown.current = runs;
    const length = runsLength(runs);
    const after = caretAfter.current;
    caretAfter.current = null;
    if (after !== null) setCaret(root, Math.min(after, length));
    else if (caret) setCaret(root, Math.min(caret.start, length), Math.min(caret.end, length));
  }, [runs]);

  function input() {
    const root = ref.current;
    if (!root) return;
    const before = shown.current ?? runs;
    let next = readRuns(root);
    if (runsLength(before) === 0 && hasFormat(before[0]) && next.length === 1 && !hasFormat(next[0])) {
      // Đoạn trống đã bật định dạng (ví dụ Đậm) rồi mới gõ: chữ theo định dạng đó, vẽ lại ô cho đúng.
      next = [{ ...formatOf(before[0]), text: next[0]!.text }];
      shown.current = null;
    } else {
      shown.current = next;
    }
    onChange(next);
  }

  function paste(event: ClipboardEvent<HTMLDivElement>) {
    event.preventDefault();
    const root = ref.current;
    const text = event.clipboardData.getData("text/plain").replace(/\r\n?/g, "\n");
    if (!root || !text) return;
    const caret = getCaret(root);
    shown.current = null;
    caretAfter.current = caret.start + text.length;
    onChange(insertText(readRuns(root), caret.start, caret.end, text));
  }

  return (
    <div ref={ref} role="textbox" aria-multiline="true" aria-label={label} contentEditable suppressContentEditableWarning tabIndex={0}
      data-editable data-rich spellCheck
      className={cx("min-h-[1.65em] w-full cursor-text break-words whitespace-pre-wrap outline-none", className)}
      onInput={input} onPaste={paste} onDrop={(event) => event.preventDefault()} onFocus={onFocus}
      onKeyDown={(event) => {
        if (!event.nativeEvent.isComposing && onKey && ref.current) onKey(event, getCaret(ref.current));
      }} />
  );
}
