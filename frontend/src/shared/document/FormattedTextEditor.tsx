"use client";

import { useLayoutEffect, useRef, useState, type KeyboardEvent, type ReactNode } from "react";
import { cx } from "@/shared/ui";
import { RichText, getCaret, setCaret, type Caret } from "./RichText";
import { normalizeRuns, runsLength, splitRuns, toggleFlagRange, type RunFlag } from "./runs";
import { FORMATTED_ALIGN, formattedTextOf, formattedTextPlain, type FormattedText, type FormattedBlock } from "./formattedText";
import { FormattedTextView } from "./FormattedTextView";
import type { Align, Run } from "./types";

type Glyph = "left" | "center" | "right" | "justify" | "bullets" | "numbers" | "undo" | "redo" | "indent" | "outdent";
// Cùng ký hiệu với thanh công cụ DocumentEditor của dự án.
const GLYPHS: Record<Glyph, ReactNode> = {
  left: <path d="M4 6h16M4 10h10M4 14h16M4 18h10" />,
  center: <path d="M4 6h16M7 10h10M4 14h16M7 18h10" />,
  right: <path d="M4 6h16M10 10h10M4 14h16M10 18h10" />,
  justify: <path d="M4 6h16M4 10h16M4 14h16M4 18h16" />,
  bullets: <><path d="M10 7h10M10 12h10M10 17h10" /><circle cx="5" cy="7" r="1" /><circle cx="5" cy="12" r="1" /><circle cx="5" cy="17" r="1" /></>,
  numbers: <path d="M10 7h10M10 12h10M10 17h10M4 5h1.5v4M4 9h3M4 15.5c.5-.8 2.5-.8 2.5.5S4 17.5 4 19h3" />,
  undo: <><path d="M9 14 4 9l5-5" /><path d="M4 9h10.5a5.5 5.5 0 0 1 0 11H11" /></>,
  redo: <><path d="m15 14 5-5-5-5" /><path d="M20 9H9.5a5.5 5.5 0 0 0 0 11H13" /></>,
  indent: <path d="M4 5h16M10 10h10M10 14h10M4 19h16M3 9l3 3-3 3" />,
  outdent: <path d="M4 5h16M10 10h10M10 14h10M4 19h16M6 9l-3 3 3 3" />,
};

function GlyphIcon({ name }: { name: Glyph }) {
  return <svg aria-hidden viewBox="0 0 24 24" width={17} height={17} fill="none" stroke="currentColor"
    strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">{GLYPHS[name]}</svg>;
}

function FormatTool({ name, children, onClick, pressed, disabled }: {
  name: string; children: ReactNode; onClick: () => void; pressed?: boolean; disabled?: boolean;
}) {
  return <button type="button" aria-label={name} title={name} aria-pressed={pressed} disabled={disabled}
    onMouseDown={(event) => event.preventDefault()} onClick={onClick}
    className={cx("inline-flex size-8 items-center justify-center rounded-md text-body hover:bg-shell focus-visible:outline-2 focus-visible:outline-focus disabled:cursor-not-allowed disabled:opacity-40",
      pressed && "bg-primary-soft text-primary-ink")}>{children}</button>;
}

export function FormattedTextEditor({ value, plainText, onChange, maxLength, disabled, "aria-label": label }: {
  value?: FormattedText | null;
  plainText: string;
  onChange: (text: string, document: FormattedText) => void;
  maxLength: number;
  disabled?: boolean;
  "aria-label": string;
}) {
  const document = value?.blocks.length ? value : formattedTextOf(plainText);
  const root = useRef<HTMLDivElement>(null);
  const focusAfter = useRef<{ index: number; at: number } | null>(null);
  const [history, setHistory] = useState<{ past: FormattedText[]; future: FormattedText[] }>({ past: [], future: [] });
  const [active, setActive] = useState(0);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const selected = Math.min(active, document.blocks.length - 1);
  const block = document.blocks[selected]!;

  const field = (index: number) => root.current?.querySelector<HTMLElement>(`[data-formatted-block="${index}"] [role="textbox"]`);
  useLayoutEffect(() => {
    const next = focusAfter.current;
    if (!next) return;
    const target = field(next.index);
    if (target) { target.focus(); setCaret(target, Math.min(next.at, (target.textContent ?? "").length)); }
    focusAfter.current = null;
  });

  function commit(next: FormattedText, record = true) {
    const text = formattedTextPlain(next);
    if (text.length > maxLength || next.blocks.length > 100
      || next.blocks.some((item) => item.runs.length > 200)
      || next.blocks.reduce((count, item) => count + item.runs.length, 0) > 1000) {
      setError(text.length > maxLength ? `${label} tối đa ${maxLength.toLocaleString("vi-VN")} ký tự.` : "Nội dung có quá nhiều đoạn hoặc định dạng.");
      const current = field(selected);
      focusAfter.current = { index: selected, at: current ? getCaret(current).start : 0 };
      setRevision((number) => number + 1);
      return;
    }
    setError("");
    if (record) {
      setHistory((current) => ({ past: [...current.past.slice(-49), document], future: [] }));
    }
    onChange(text, next);
  }

  function changeBlock(change: Partial<FormattedBlock>) {
    commit({ blocks: document.blocks.map((item, index) => index === selected ? { ...item, ...change } : item) });
  }

  function format(flag: RunFlag) {
    const current = field(selected);
    const caret = current ? getCaret(current) : { start: 0, end: 0 };
    const length = runsLength(block.runs);
    const start = caret.start === caret.end ? 0 : caret.start;
    const end = caret.start === caret.end ? length : caret.end;
    changeBlock({ runs: length ? toggleFlagRange(block.runs, start, end, flag)
      : [{ text: "", [flag]: !block.runs[0]?.[flag as "bold" | "italic" | "underline"] }] });
    current?.focus();
  }

  function travel(direction: "past" | "future") {
    const stack = history[direction];
    const next = stack.at(-1);
    if (!next) return;
    const opposite = direction === "past" ? "future" : "past";
    setHistory({ ...history, [direction]: stack.slice(0, -1), [opposite]: [...history[opposite], document] });
    commit(next, false);
  }

  function key(event: KeyboardEvent<HTMLDivElement>, index: number, caret: Caret) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      const current = document.blocks[index]!;
      if (current.type !== "PARAGRAPH" && runsLength(current.runs) === 0) {
        changeBlock({ type: "PARAGRAPH", indent: 0 });
        return;
      }
      const [before, after] = splitRuns(current.runs, caret.start, caret.end);
      focusAfter.current = { index: index + 1, at: 0 };
      commit({ blocks: [...document.blocks.slice(0, index), { ...current, runs: before },
        { ...current, runs: after }, ...document.blocks.slice(index + 1)] });
    } else if (event.key === "Backspace" && caret.start === 0 && caret.end === 0 && index > 0) {
      event.preventDefault();
      const previous = document.blocks[index - 1]!;
      focusAfter.current = { index: index - 1, at: runsLength(previous.runs) };
      commit({ blocks: [...document.blocks.slice(0, index - 1),
        { ...previous, runs: normalizeRuns([...previous.runs, ...document.blocks[index]!.runs]) },
        ...document.blocks.slice(index + 1)] });
    } else if (event.key === "Tab") {
      // Tab vẫn chuyển focus; dùng các nút thụt lề để không giữ người dùng trong ô.
      return;
    }
  }

  return <div ref={root} role="group" aria-label={`Bộ soạn ${label}`}
    className="overflow-hidden rounded-lg border border-field bg-surface"
    onKeyDown={(event) => {
      if (disabled) return;
      if (!(event.ctrlKey || event.metaKey) || event.nativeEvent.isComposing) return;
      const name = event.key.toLowerCase();
      if (name === "z" || name === "y") { event.preventDefault(); travel(name === "y" || event.shiftKey ? "future" : "past"); }
      if (name === "b" || name === "i" || name === "u") { event.preventDefault(); format(name === "b" ? "bold" : name === "i" ? "italic" : "underline"); }
    }}>
    <div role="toolbar" aria-label={`Định dạng ${label}`} className="flex flex-wrap items-center gap-0.5 border-b border-line bg-sunken px-2 py-1">
      <FormatTool name="Đậm" onClick={() => format("bold")} disabled={disabled} pressed={block.runs.every((run) => !!run.bold)}><b>B</b></FormatTool>
      <FormatTool name="Nghiêng" onClick={() => format("italic")} disabled={disabled} pressed={block.runs.every((run) => !!run.italic)}><i>I</i></FormatTool>
      <FormatTool name="Gạch chân" onClick={() => format("underline")} disabled={disabled} pressed={block.runs.every((run) => !!run.underline)}><u>U</u></FormatTool>
      <span className="mx-1 h-4 border-l border-line" aria-hidden />
      {([ ["LEFT", "Căn trái", "left"], ["CENTER", "Căn giữa", "center"], ["RIGHT", "Căn phải", "right"], ["JUSTIFY", "Căn đều", "justify"] ] as const)
        .map(([align, title, icon]) => <FormatTool key={align} name={title} disabled={disabled}
          onClick={() => changeBlock({ align: align as Align })} pressed={block.align === align}><GlyphIcon name={icon} /></FormatTool>)}
      <span className="mx-1 h-4 border-l border-line" aria-hidden />
      <FormatTool name="Danh sách chấm" disabled={disabled} onClick={() => changeBlock({ type: block.type === "UL" ? "PARAGRAPH" : "UL" })} pressed={block.type === "UL"}><GlyphIcon name="bullets" /></FormatTool>
      <FormatTool name="Danh sách số" disabled={disabled} onClick={() => changeBlock({ type: block.type === "OL" ? "PARAGRAPH" : "OL" })} pressed={block.type === "OL"}><GlyphIcon name="numbers" /></FormatTool>
      <FormatTool name="Giảm thụt lề" disabled={disabled || block.indent <= 0} onClick={() => changeBlock({ indent: block.indent - 1 })}><GlyphIcon name="outdent" /></FormatTool>
      <FormatTool name="Tăng thụt lề" disabled={disabled || block.indent >= 6} onClick={() => changeBlock({ indent: block.indent + 1 })}><GlyphIcon name="indent" /></FormatTool>
      <span className="mx-1 h-4 border-l border-line" aria-hidden />
      <FormatTool name="Hoàn tác" disabled={disabled || history.past.length === 0} onClick={() => travel("past")}><GlyphIcon name="undo" /></FormatTool>
      <FormatTool name="Làm lại" disabled={disabled || history.future.length === 0} onClick={() => travel("future")}><GlyphIcon name="redo" /></FormatTool>
    </div>
    {disabled ? <div className="min-h-24 px-3 py-2 text-[13.5px]"><FormattedTextView document={document} text={plainText} /></div>
      : <div className="min-h-24 px-3 py-2 text-[13.5px] leading-relaxed">
        {document.blocks.map((item, index) => <div key={`${revision}:${index}`} data-formatted-block={index}
          onFocusCapture={() => setActive(index)}
          style={{ textAlign: FORMATTED_ALIGN[item.align], marginInlineStart: `${item.indent * 1.25 + (item.type === "PARAGRAPH" ? 0 : 2)}rem` }}
          className={cx(item.type !== "PARAGRAPH" && "list-item", item.type === "UL" && "list-disc", item.type === "OL" && "list-decimal")}>
          <RichText runs={item.runs} aria-label={index === 0 ? label : `${label}, đoạn ${index + 1}`}
            onKey={(event, caret) => key(event, index, caret)}
            onChange={(runs: Run[]) => commit({ blocks: document.blocks.map((current, position) => position === index
              ? { ...current, runs: runs.map(({ text, bold, italic, underline }) => ({ text, bold, italic, underline })) } : current) })} />
        </div>)}
      </div>}
    {error && <p role="alert" className="px-3 pb-2 text-xs text-danger">{error}</p>}
    <p className="px-3 pb-2 text-right text-[11px] text-muted">{formattedTextPlain(document).length.toLocaleString("vi-VN")} / {maxLength.toLocaleString("vi-VN")}</p>
  </div>;
}
