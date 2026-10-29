"use client";

import { useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Field, TableFrame, Td, Th } from "@/shared/ui";
import { blockText, headingLevel, type DocumentModel, type ValidationIssue } from "./types";

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.problem.detail ?? "Không đọc được tệp DOCX";
  return error instanceof Error && error.message ? error.message : "Không đọc được tệp DOCX";
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
  DIAGRAM_RESTORED_BY_CODE: "theo mã trong alt text",
  DIAGRAM_RESTORED_BY_LINK: "theo hyperlink",
  DIAGRAM_RESTORED_BY_PNG: "từ dữ liệu trong ảnh PNG",
  DIAGRAM_RESTORED_BY_SVG: "từ ảnh SVG",
};

/** t13e Nhập DOCX (F5, F7; BR-U07-61, 62): xem trước các mục, cách khôi phục sơ đồ, nội dung bị bỏ. */
export function DocxImportDialog({ title = "Nhập DOCX", preview, onImport, allowReplace, onClose }: Props) {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<DocxPreview | null>(null);
  const [mode, setMode] = useState<"replace" | "append">(allowReplace ? "replace" : "append");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function read(next: File) {
    setFile(next);
    setResult(null);
    setError(null);
    if (next.size > 20 * 1024 * 1024) {
      setError("Tệp DOCX tối đa 20 MB");
      return;
    }
    setBusy(true);
    try {
      setResult(await preview(next));
    } catch (e) {
      setError(errorMessage(e));
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
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  const sections: { title: string; blocks: number }[] = [];
  for (const block of result?.document.blocks ?? []) {
    if (headingLevel(block) > 0) sections.push({ title: blockText(block), blocks: 0 });
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
        <Button variant="primary" disabled={busy || !result} onClick={() => void confirm()} data-testid="docx-import-confirm">Nhập</Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <Field label="Tệp DOCX" hint="Tối đa 20 MB; tệp chỉ đọc trong bộ nhớ, không lưu.">
        <label className="inline-flex w-fit cursor-pointer items-center gap-1.5 rounded-lg border border-line-strong bg-surface px-3 py-1.5 text-xs font-semibold">
          {file ? "Chọn tệp khác" : "Chọn tệp"}
          <input type="file" accept=".docx" className="sr-only" data-testid="docx-file"
            onChange={(e) => { const next = e.target.files?.[0]; if (next) void read(next); e.target.value = ""; }} />
        </label>
        {file && <span className="text-xs text-muted">{file.name}</span>}
      </Field>
      {busy && !result && <p role="status" className="text-sm text-muted">Đang đọc tệp…</p>}
      {result && (
        <div className="flex flex-col gap-3" data-testid="docx-preview">
          <p className="text-[13px] text-ink">{result.document.blocks.length} khối · {result.imageCount} ảnh · {result.diagramCount} sơ đồ</p>
          {sections.length > 0 && (
            <TableFrame className="[&_table]:table-fixed">
              <colgroup><col className="w-[48px]" /><col /><col className="w-[100px]" /></colgroup><thead><tr><Th>#</Th><Th>Mục</Th><Th>Độ dài</Th></tr></thead>
              <tbody>
                {sections.map((section, index) => (
                  <tr key={index}><Td>{index + 1}</Td><Td>{section.title}</Td><Td>{section.blocks} khối</Td></tr>
                ))}
              </tbody>
            </TableFrame>
          )}
          {restored.length > 0 && (
            <Alert tone="success" title="Sơ đồ khôi phục được">
              {Object.entries(restored.reduce<Record<string, number>>((acc, w) => ({ ...acc, [w.code]: (acc[w.code] ?? 0) + 1 }), {}))
                .map(([code, count]) => `${count} sơ đồ ${RESTORED[code]}`).join("; ")}
            </Alert>
          )}
          {others.map((warning, index) => <Alert key={index} tone="warning">{warning.message}</Alert>)}
          <Field label="Cách nhập">
            <div className="flex flex-col gap-1 text-[13px]">
              {allowReplace && (
                <label className="flex items-center gap-2">
                  <input type="radio" name="docx-mode" checked={mode === "replace"} onChange={() => setMode("replace")} />Thay khung hiện tại
                </label>
              )}
              <label className="flex items-center gap-2">
                <input type="radio" name="docx-mode" checked={mode === "append"} onChange={() => setMode("append")} />Thêm vào cuối
              </label>
            </div>
          </Field>
        </div>
      )}
    </Dialog>
  );
}
