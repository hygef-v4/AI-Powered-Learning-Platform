"use client";

import { useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Field, TableFrame, Td, Th } from "@/shared/ui";
import { blockText, readHeadingLevel, type DocumentModel, type ValidationIssue } from "./types";
import { msg } from "@/i18n/msg";

function describeError(error: unknown): string {
  if (error instanceof ApiError) return error.problem.detail ?? msg("shared.document.DocxImportDialog.khongDocDuocTepDocx");
  return error instanceof Error && error.message ? error.message : msg("shared.document.DocxImportDialog.khongDocDuocTepDocx");
}

export type DocxPreview = { document: DocumentModel; warnings: ValidationIssue[]; imageCount: number; diagramCount: number };

type Props = {
  title?: string;
  /** Gửi tệp lên backend, trả bản xem trước (không lưu gì). */
  preview: (file: File) => Promise<DocxPreview>;
  /** Người dùng chọn "Thay khung" ({@code replace}) hoặc "Thêm vào cuối". */
  onImport: (document: DocumentModel, mode: "replace" | "append") => Promise<void> | void;
  allowReplace: boolean;
  onClose: () => void;
};

const RESTORED: Record<string, string> = {
  DIAGRAM_RESTORED_BY_CODE: msg("shared.document.DocxImportDialog.theoMaTrongAltText"),
  DIAGRAM_RESTORED_BY_LINK: "theo hyperlink",
  DIAGRAM_RESTORED_BY_PNG: msg("shared.document.DocxImportDialog.tuDuLieuTrongAnhPng"),
  DIAGRAM_RESTORED_BY_SVG: msg("shared.document.DocxImportDialog.tuAnhSvg"),
};

/** t13e Nhập DOCX (F5, F7; BR-U07-61, 62): xem trước các mục, cách khôi phục sơ đồ, nội dung bị bỏ. */
export function DocxImportDialog({ title = msg("shared.document.DocxImportDialog.nhapDocx"), preview, onImport, allowReplace, onClose }: Props) {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<DocxPreview | null>(null);
  const [mode, setMode] = useState<"replace" | "append">(allowReplace ? "replace" : "append");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function readFile(next: File) {
    setFile(next);
    setResult(null);
    setError(null);
    if (next.size > 20 * 1024 * 1024) {
      setError(msg("shared.document.DocxImportDialog.tepDocxToiDa20Mb"));
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

  return (
    <Dialog open size="lg" icon="upload_file" title={title} onClose={onClose} dismissible={!busy}
      footer={<>
        <Button variant="primary" disabled={busy || !result} onClick={() => void confirm()} data-testid="docx-import-confirm">{msg("shared.document.DocxImportDialog.nhap")}</Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <Field label={msg("shared.document.DocxImportDialog.tepDocx")} hint={msg("shared.document.DocxImportDialog.toiDa20MbTepChi")}>
        <label className="inline-flex w-fit cursor-pointer items-center gap-1.5 rounded-lg border border-line-strong bg-surface px-3 py-1.5 text-xs font-semibold">
          {file ? msg("shared.document.DocxImportDialog.chonTepKhac") : msg("shared.document.DocxImportDialog.chonTep")}
          <input type="file" accept=".docx" className="sr-only" data-testid="docx-file"
            onChange={(e) => { const next = e.target.files?.[0]; if (next) void readFile(next); e.target.value = ""; }} />
        </label>
        {file && <span className="text-xs text-muted">{file.name}</span>}
      </Field>
      {busy && !result && <p role="status" className="text-sm text-muted">{msg("shared.document.DocxImportDialog.dangDocTep")}</p>}
      {result && (
        <div className="flex flex-col gap-3" data-testid="docx-preview">
          <p className="text-[13px] text-ink">{result.document.blocks.length} {msg("shared.document.DocxImportDialog.khoi")}{result.imageCount} {msg("shared.document.DocxImportDialog.anh")}{result.diagramCount} {msg("shared.document.DocxImportDialog.soDo")}</p>
          {sections.length > 0 && (
            <TableFrame className="[&_table]:table-fixed">
              <colgroup><col className="w-[48px]" /><col /><col className="w-[100px]" /></colgroup><thead><tr><Th>#</Th><Th>{msg("shared.document.DocxImportDialog.muc")}</Th><Th>{msg("shared.document.DocxImportDialog.doDai")}</Th></tr></thead>
              <tbody>
                {sections.map((section, index) => (
                  <tr key={index}><Td>{index + 1}</Td><Td>{section.title}</Td><Td>{section.blocks} {msg("shared.document.DocxImportDialog.khoi2")}</Td></tr>
                ))}
              </tbody>
            </TableFrame>
          )}
          {restored.length > 0 && (
            <Alert tone="success" title={msg("shared.document.DocxImportDialog.soDoKhoiPhucDuoc")}>
              {Object.entries(restored.reduce<Record<string, number>>((acc, w) => ({ ...acc, [w.code]: (acc[w.code] ?? 0) + 1 }), {}))
                .map(([code, count]) => msg("shared.document.DocxImportDialog.soDo2", { count: String(count), value: String(RESTORED[code]) })).join("; ")}
            </Alert>
          )}
          {others.map((warning, index) => <Alert key={index} tone="warning">{warning.message}</Alert>)}
          <Field label={msg("shared.document.DocxImportDialog.cachNhap")}>
            <div className="flex flex-col gap-1 text-[13px]">
              {allowReplace && (
                <label className="flex items-center gap-2">
                  <input type="radio" name="docx-mode" checked={mode === "replace"} onChange={() => setMode("replace")} />{msg("shared.document.DocxImportDialog.thayKhungHienTai")}</label>
              )}
              <label className="flex items-center gap-2">
                <input type="radio" name="docx-mode" checked={mode === "append"} onChange={() => setMode("append")} />{msg("shared.document.DocxImportDialog.themVaoCuoi")}</label>
            </div>
          </Field>
        </div>
      )}
    </Dialog>
  );
}
