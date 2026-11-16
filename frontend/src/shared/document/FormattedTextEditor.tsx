"use client";

import { useLayoutEffect, useRef, useState, type KeyboardEvent } from "react";
import { cx } from "@/shared/ui";
import { RichText, getCaret, setCaret, type Caret } from "./RichText";
import { normalizeRuns, runsLength, splitRuns, toggleFlagRange, type RunFlag } from "./runs";
import { FORMATTED_ALIGN, formattedTextOf, formattedTextPlain, type FormattedText, type FormattedBlock } from "./formattedText";
import { FormattedTextView } from "./FormattedTextView";
import { Glyph } from "./Glyph";
import { ToolbarButton } from "./ToolbarButton";
import type { Align, Run } from "./types";
import { msg } from "@/i18n/msg";

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
      setError(text.length > maxLength ? msg("shared.document.FormattedTextEditor.toiDaKyTu", { label: String(label), value: String(maxLength.toLocaleString("vi-VN")) }) : msg("shared.document.FormattedTextEditor.noiDungCoQuaNhieuDoan"));
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

  return <div ref={root} role="group" aria-label={msg("shared.document.FormattedTextEditor.boSoan", { label: String(label) })}
    className="overflow-hidden rounded-lg border border-field bg-surface"
    onKeyDown={(event) => {
      if (disabled) return;
      if (!(event.ctrlKey || event.metaKey) || event.nativeEvent.isComposing) return;
      const name = event.key.toLowerCase();
      if (name === "z" || name === "y") { event.preventDefault(); travel(name === "y" || event.shiftKey ? "future" : "past"); }
      if (name === "b" || name === "i" || name === "u") { event.preventDefault(); format(name === "b" ? "bold" : name === "i" ? "italic" : "underline"); }
    }}>
    <div role="toolbar" aria-label={msg("shared.document.FormattedTextEditor.dinhDang", { label: String(label) })} className="flex flex-wrap items-center gap-0.5 border-b border-line bg-sunken px-2 py-1">
      <ToolbarButton aria-label={msg("shared.document.FormattedTextEditor.dam")} title={msg("shared.document.FormattedTextEditor.dam")} onClick={() => format("bold")} disabled={disabled} active={block.runs.every((run) => !!run.bold)}><b>B</b></ToolbarButton>
      <ToolbarButton aria-label={msg("shared.document.FormattedTextEditor.nghieng")} title={msg("shared.document.FormattedTextEditor.nghieng")} onClick={() => format("italic")} disabled={disabled} active={block.runs.every((run) => !!run.italic)}><i>I</i></ToolbarButton>
      <ToolbarButton aria-label={msg("shared.document.FormattedTextEditor.gachChan")} title={msg("shared.document.FormattedTextEditor.gachChan")} onClick={() => format("underline")} disabled={disabled} active={block.runs.every((run) => !!run.underline)}><u>U</u></ToolbarButton>
      <span className="mx-1 h-4 border-l border-line" aria-hidden />
      {([ ["LEFT", msg("shared.document.FormattedTextEditor.canTrai"), "alignLeft"], ["CENTER", msg("shared.document.FormattedTextEditor.canGiua"), "alignCenter"], ["RIGHT", msg("shared.document.FormattedTextEditor.canPhai"), "alignRight"], ["JUSTIFY", msg("shared.document.FormattedTextEditor.canDeu"), "justify"] ] as const)
        .map(([align, title, icon]) => <ToolbarButton key={align} aria-label={title} title={title} disabled={disabled}
          onClick={() => changeBlock({ align: align as Align })} active={block.align === align}><Glyph name={icon} /></ToolbarButton>)}
      <span className="mx-1 h-4 border-l border-line" aria-hidden />
      <ToolbarButton aria-label={msg("shared.document.FormattedTextEditor.danhSachCham")} title={msg("shared.document.FormattedTextEditor.danhSachCham")} disabled={disabled} onClick={() => changeBlock({ type: block.type === "UL" ? "PARAGRAPH" : "UL" })} active={block.type === "UL"}><Glyph name="bullets" /></ToolbarButton>
      <ToolbarButton aria-label={msg("shared.document.FormattedTextEditor.danhSachSo")} title={msg("shared.document.FormattedTextEditor.danhSachSo")} disabled={disabled} onClick={() => changeBlock({ type: block.type === "OL" ? "PARAGRAPH" : "OL" })} active={block.type === "OL"}><Glyph name="numbers" /></ToolbarButton>
      <ToolbarButton aria-label={msg("shared.document.FormattedTextEditor.giamThutLe")} title={msg("shared.document.FormattedTextEditor.giamThutLe")} disabled={disabled || block.indent <= 0} onClick={() => changeBlock({ indent: block.indent - 1 })}><Glyph name="outdent" /></ToolbarButton>
      <ToolbarButton aria-label={msg("shared.document.FormattedTextEditor.tangThutLe")} title={msg("shared.document.FormattedTextEditor.tangThutLe")} disabled={disabled || block.indent >= 6} onClick={() => changeBlock({ indent: block.indent + 1 })}><Glyph name="indent" /></ToolbarButton>
      <span className="mx-1 h-4 border-l border-line" aria-hidden />
      <ToolbarButton aria-label={msg("shared.document.FormattedTextEditor.hoanTac")} title={msg("shared.document.FormattedTextEditor.hoanTac")} disabled={disabled || history.past.length === 0} onClick={() => travel("past")}><Glyph name="undo" /></ToolbarButton>
      <ToolbarButton aria-label={msg("shared.document.FormattedTextEditor.lamLai")} title={msg("shared.document.FormattedTextEditor.lamLai")} disabled={disabled || history.future.length === 0} onClick={() => travel("future")}><Glyph name="redo" /></ToolbarButton>
    </div>
    {disabled ? <div className="min-h-24 px-3 py-2 text-[13.5px]"><FormattedTextView document={document} text={plainText} /></div>
      : <div className="min-h-24 px-3 py-2 text-[13.5px] leading-relaxed">
        {document.blocks.map((item, index) => <div key={`${revision}:${index}`} data-formatted-block={index}
          onFocusCapture={() => setActive(index)}
          style={{ textAlign: FORMATTED_ALIGN[item.align], marginInlineStart: `${item.indent * 1.25 + (item.type === "PARAGRAPH" ? 0 : 2)}rem` }}
          className={cx(item.type !== "PARAGRAPH" && "list-item", item.type === "UL" && "list-disc", item.type === "OL" && "list-decimal")}>
          <RichText runs={item.runs} aria-label={index === 0 ? label : msg("shared.document.FormattedTextEditor.doan", { label: String(label), value: String(index + 1) })}
            onKey={(event, caret) => key(event, index, caret)}
            onChange={(runs: Run[]) => commit({ blocks: document.blocks.map((current, position) => position === index
              ? { ...current, runs: runs.map(({ text, bold, italic, underline }) => ({ text, bold, italic, underline })) } : current) })} />
        </div>)}
      </div>}
    {error && <p role="alert" className="px-3 pb-2 text-xs text-danger">{error}</p>}
    <p className="px-3 pb-2 text-right text-xs text-muted">{formattedTextPlain(document).length.toLocaleString("vi-VN")} / {maxLength.toLocaleString("vi-VN")}</p>
  </div>;
}
