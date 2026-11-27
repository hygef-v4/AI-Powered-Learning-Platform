"use client";

import { useId, useRef, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Icon, IconButton, cx, matchesAccept } from "@/shared/ui";
import { blockText, readHeadingLevel, type DocumentModel, type ValidationIssue } from "./types";
import { msg } from "@/i18n/msg";

function describeError(error: unknown): string {
  if (error instanceof ApiError) return error.problem.detail ?? msg("shared.document.DocxImportDialog.couldNotReadTheDocx");
  return error instanceof Error && error.message ? error.message : msg("shared.document.DocxImportDialog.couldNotReadTheDocx");
}

export type DocxPreview = { document: DocumentModel; warnings: ValidationIssue[]; imageCount: number; diagramCount: number };

type Props = {
  title?: string;
  /** Gửi tệp lên backend, trả bản xem trước (không lưu gì). */
  preview: (file: File) => Promise<DocxPreview>;
  /** Người dùng chọn "Thay khung" ({@code replace}) hoặc "Thêm vào cuối". */
  onImport: (document: DocumentModel, mode: "replace" | "append") => Promise<void> | void;
  allowReplace: boolean;
  /** false: không hiện cách nhập (trang tự ghép vào đúng chỗ, ví dụ bài làm của sinh viên ghép theo từng phần). */
  showMode?: boolean;
  onClose: () => void;
};

const MAX_BYTES = 20 * 1024 * 1024;
const ACCEPT = ".docx";

const RESTORED: Record<string, string> = {
  DIAGRAM_RESTORED_BY_CODE: msg("shared.document.DocxImportDialog.fromTheCodeInThe"),
  DIAGRAM_RESTORED_BY_LINK: "theo hyperlink",
  DIAGRAM_RESTORED_BY_PNG: msg("shared.document.DocxImportDialog.fromTheDataInThe"),
  DIAGRAM_RESTORED_BY_SVG: msg("shared.document.DocxImportDialog.fromTheSvgImage"),
};

const formatSize = (bytes: number) => (bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`);

/**
 * t13e Nhập DOCX (F5, F7; BR-U07-61, 62): kéo thả hoặc bấm để chọn tệp (yêu cầu 2026-11-27), xem trước các mục, cách khôi
 * phục sơ đồ, nội dung bị bỏ, rồi chọn cách nhập.
 */
export function DocxImportDialog({ title = msg("shared.document.DocxImportDialog.importDocx"), preview, onImport, allowReplace, showMode = true, onClose }: Props) {
  const inputId = useId();
  const input = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<DocxPreview | null>(null);
  const [mode, setMode] = useState<"replace" | "append">(allowReplace ? "replace" : "append");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [over, setOver] = useState(false);
  const depth = useRef(0);

  async function readFile(next: File) {
    setFile(next);
    setResult(null);
    setError(null);
    if (!matchesAccept(next, ACCEPT)) {
      setError(msg("shared.document.DocxImportDialog.onlyDocxFiles"));
      return;
    }
    if (next.size > MAX_BYTES) {
      setError(msg("shared.document.DocxImportDialog.docxFilesCanBeAt"));
      return;
    }
    setBusy(true);
    try {
      setResult(await preview(next));
    } catch (e) {
      setError(describeError(e));
    } finally {
      setBusy(false);
    }
  }

  async function confirm() {
    if (!result) return;
    setBusy(true);
    try {
      await onImport(result.document, mode);
      onClose();
    } catch (e) {
      setError(describeError(e));
    } finally {
      setBusy(false);
    }
  }

  const sections: { title: string; blocks: number }[] = [];
  for (const block of result?.document.blocks ?? []) {
    if (readHeadingLevel(block) > 0) sections.push({ title: blockText(block), blocks: 0 });
    else {
      const last = sections.at(-1);
      if (last) last.blocks++;
    }
  }
  const restored = (result?.warnings ?? []).filter((w) => RESTORED[w.code]);
  const others = (result?.warnings ?? []).filter((w) => !RESTORED[w.code]);
  const hasFiles = (types: readonly string[] | DOMStringList) => Array.from(types).includes("Files");

  const fileInput = (
    <input ref={input} id={inputId} type="file" accept={ACCEPT} className="sr-only" data-testid="docx-file" disabled={busy}
      aria-label={msg("shared.document.DocxImportDialog.chooseFile")}
      onChange={(e) => { const next = e.target.files?.[0]; if (next) void readFile(next); e.target.value = ""; }} />
  );

  return (
    <Dialog open size="lg" icon="upload_file" title={title} onClose={onClose} dismissible={!busy}
      footer={<>
        <Button variant="primary" icon="upload_file" disabled={busy || !result} onClick={() => void confirm()} data-testid="docx-import-confirm">{msg("shared.document.DocxImportDialog.import")}</Button>
      </>}>
      <div className="flex flex-col gap-4"
        onDragEnter={(event) => { if (busy || !hasFiles(event.dataTransfer.types)) return; event.preventDefault(); depth.current += 1; setOver(true); }}
        onDragOver={(event) => { if (busy || !hasFiles(event.dataTransfer.types)) return; event.preventDefault(); event.dataTransfer.dropEffect = "copy"; }}
        onDragLeave={() => { depth.current = Math.max(0, depth.current - 1); if (depth.current === 0) setOver(false); }}
        onDrop={(event) => {
          if (busy || !hasFiles(event.dataTransfer.types)) return;
          event.preventDefault();
          depth.current = 0;
          setOver(false);
          const next = event.dataTransfer.files[0];
          if (next) void readFile(next);
        }}>
        {error && <Alert tone="danger" role="alert">{error}</Alert>}

        {!file ? (
          // Chưa có tệp: một vùng lớn để kéo tệp vào hoặc bấm để chọn.
          <label htmlFor={inputId} data-testid="docx-drop"
            className={cx("flex cursor-pointer flex-col items-center gap-3 rounded-2xl border-2 border-dashed px-6 py-10 text-center transition-colors duration-200",
              over ? "border-primary bg-primary-soft" : "border-line-strong bg-sunken/60 hover:border-primary hover:bg-primary-soft/60")}>
            <span className={cx("flex size-14 items-center justify-center rounded-2xl bg-surface text-primary-ink shadow-[var(--shadow-lift)] transition-transform duration-200",
              over && "-translate-y-1 scale-105")}>
              <Icon name="upload_file" size={28} />
            </span>
            <span className="flex flex-col gap-1">
              <span className="text-[15px] font-semibold text-ink">
                {over ? msg("shared.document.DocxImportDialog.dropToRead") : msg("shared.document.DocxImportDialog.dragADocxFileHere")}
              </span>
              <span className="text-[13px] text-muted">
                {msg("shared.document.DocxImportDialog.or")}{" "}
                <span className="font-semibold text-primary-ink underline-offset-2 hover:underline">{msg("shared.document.DocxImportDialog.chooseFromComputer")}</span>
              </span>
            </span>
            <span className="text-xs text-muted">{msg("shared.document.DocxImportDialog.atMost20MbThe")}</span>
            {fileInput}
          </label>
        ) : (
          // Đã có tệp: thẻ tệp; kéo tệp khác vào cả hộp thoại hoặc bấm Đổi tệp để đọc lại.
          <div className={cx("flex items-center gap-3 rounded-xl border px-4 py-3 transition-colors",
            over ? "border-primary border-dashed bg-primary-soft" : "border-line bg-surface")} data-testid="docx-file-card">
            <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary-soft text-primary-ink">
              <Icon name="description" size={20} />
            </span>
            <span className="flex min-w-0 flex-1 flex-col">
              <span className="truncate text-[13.5px] font-semibold text-ink" title={file.name}>{file.name}</span>
              <span className="text-xs text-muted" role={busy && !result ? "status" : undefined}>
                {busy && !result ? msg("shared.document.DocxImportDialog.readingFile") : formatSize(file.size)}
              </span>
            </span>
            <Button size="sm" disabled={busy} onClick={() => input.current?.click()}>{msg("shared.document.DocxImportDialog.chooseAnotherFile")}</Button>
            <IconButton icon="close" size={30} label={msg("shared.document.DocxImportDialog.removeFile")} disabled={busy}
              onClick={() => { setFile(null); setResult(null); setError(null); }} />
            {fileInput}
          </div>
        )}

        {result && (
          <div className="flex flex-col gap-3" data-testid="docx-preview">
            <dl className="grid grid-cols-3 gap-2">
              {[
                { label: msg("shared.document.DocxImportDialog.blocksLabel"), value: result.document.blocks.length },
                { label: msg("shared.document.DocxImportDialog.imagesLabel"), value: result.imageCount },
                { label: msg("shared.document.DocxImportDialog.diagramsLabel"), value: result.diagramCount },
              ].map((stat) => (
                <div key={stat.label} className="flex flex-col gap-0.5 rounded-lg bg-sunken px-3 py-2">
                  <dt className="text-xs text-muted">{stat.label}</dt>
                  <dd className="font-display text-lg leading-tight font-bold text-ink tabular-nums">{stat.value}</dd>
                </div>
              ))}
            </dl>
            {sections.length > 0 && (
              <div className="flex flex-col gap-1.5">
                <p className="text-xs font-semibold text-muted">{msg("shared.document.DocxImportDialog.section")} ({sections.length})</p>
                <ol className="flex max-h-56 flex-col overflow-y-auto rounded-xl border border-line">
                  {sections.map((section, index) => (
                    <li key={index} className="flex items-center gap-3 border-b border-line px-3 py-2 text-[13px] last:border-b-0">
                      <span className="w-6 shrink-0 text-right text-xs text-muted tabular-nums">{index + 1}</span>
                      <span className="min-w-0 flex-1 truncate text-ink" title={section.title}>{section.title}</span>
                      <span className="shrink-0 text-xs text-muted tabular-nums">{section.blocks} {msg("shared.document.DocxImportDialog.blocks2")}</span>
                    </li>
                  ))}
                </ol>
              </div>
            )}
            {restored.length > 0 && (
              <Alert tone="success" title={msg("shared.document.DocxImportDialog.diagramsRecovered")}>
                {Object.entries(restored.reduce<Record<string, number>>((acc, w) => ({ ...acc, [w.code]: (acc[w.code] ?? 0) + 1 }), {}))
                  .map(([code, count]) => msg("shared.document.DocxImportDialog.diagrams2", { count: String(count), value: String(RESTORED[code]) })).join("; ")}
              </Alert>
            )}
            {others.map((warning, index) => <Alert key={index} tone="warning">{warning.message}</Alert>)}
            {showMode && (
              <fieldset className="flex flex-col gap-2">
                <legend className="mb-1.5 text-xs font-semibold text-muted">{msg("shared.document.DocxImportDialog.importMethod")}</legend>
                <div className={cx("grid gap-2", allowReplace && "sm:grid-cols-2")}>
                  {([...(allowReplace ? ["replace"] as const : []), "append"] as const).map((value) => (
                    <label key={value} className={cx("flex cursor-pointer items-start gap-2.5 rounded-xl border px-3 py-2.5 text-[13px] transition-colors",
                      mode === value ? "border-primary bg-primary-soft text-ink" : "border-line text-body hover:bg-sunken")}>
                      <input type="radio" name="docx-mode" className="mt-0.5 accent-primary" checked={mode === value} onChange={() => setMode(value)} />
                      {value === "replace" ? msg("shared.document.DocxImportDialog.replaceTheCurrentOutline") : msg("shared.document.DocxImportDialog.addToTheEnd")}
                    </label>
                  ))}
                </div>
              </fieldset>
            )}
          </div>
        )}
      </div>
    </Dialog>
  );
}
