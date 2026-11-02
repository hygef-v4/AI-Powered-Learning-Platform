"use client";
import Image from "next/image";
import { useMemo, useState } from "react";
import { Button, Dialog } from "@/shared/ui";
import { apiFetch } from "@/shared/api/client";
import { message } from "../hooks/useResource";
import type { DocumentModel } from "../model/types";
import { RunsView, alignClass } from "@/shared/document/DocumentViewer";
import type { Align, Run } from "@/shared/document/types";

/** Only document text fields become visible content; formatting metadata stays in RunsView. */
function contentRuns(value: unknown): Run[] {
  if (typeof value === "string") return [{ text: value }];
  if (typeof value === "number") return [{ text: String(value) }];
  if (Array.isArray(value)) return value.flatMap(contentRuns);
  if (!value || typeof value !== "object") return [];
  const content = value as Record<string, unknown>;
  if (Array.isArray(content.runs)) return contentRuns(content.runs);
  if (typeof content.text !== "string") return [];
  const run: Run = { text: content.text };
  for (const flag of ["bold", "italic", "underline", "strike", "code"] as const)
    if (content[flag] === true) run[flag] = true;
  for (const field of ["color", "highlight"] as const) {
    const color = content[field];
    if (typeof color === "string" && /^#[0-9a-f]{6}$/i.test(color)) run[field] = color;
  }
  return [run];
}

function contentAlignment(value: unknown): string {
  return alignClass(typeof value === "string" && ["LEFT", "CENTER", "RIGHT", "JUSTIFY"].includes(value)
    ? value as Align : undefined);
}

function listIndent(item: unknown): number {
  const level = item && typeof item === "object" ? (item as Record<string, unknown>).level : undefined;
  return typeof level === "number" && Number.isInteger(level) && level >= 0 && level <= 4 ? level * 24 : 0;
}
function SubmittedImage({
  blockId,
  fileId,
  alt,
  evaluationId,
}: {
  blockId: string;
  fileId: string;
  alt: string;
  evaluationId?: string;
}) {
  const [url, setUrl] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function load() {
    setBusy(true);
    setError("");
    try {
      const token = await apiFetch<{ url: string }>(
        `/api/v1/evaluations/${evaluationId}/images/${encodeURIComponent(fileId)}`,
      );
      const parsed = new URL(token.url, window.location.origin);
      if (!["https:", "http:"].includes(parsed.protocol))
        throw new Error("Liên kết ảnh không hợp lệ.");
      setUrl(parsed.href);
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <figure id={`submission-block-${blockId}`} className="my-4 scroll-mt-24">
      <figcaption>{alt}</figcaption>
      {url && (
        <Image
          src={url}
          alt={alt}
          width={1000}
          height={700}
          unoptimized
          className="h-auto max-w-full"
        />
      )}
      <button disabled={!evaluationId || busy} onClick={() => void load()}>
        {busy ? "Đang tải ảnh…" : url ? "Tải lại ảnh" : "Xem ảnh bài nộp"}
      </button>
      {error && <p role="alert">{error}</p>}
    </figure>
  );
}
function safeDiagramPreview(value: unknown): string {
    if (typeof value !== "string" || !value.trim() || typeof DOMParser === "undefined") return "";
    const parsed = new DOMParser().parseFromString(value, "image/svg+xml");
    if (parsed.querySelector("parsererror") || parsed.documentElement.localName !== "svg") {
      return "";
    }
    const allowed = new Set([
      "svg",
      "g",
      "switch",
      "path",
      "rect",
      "circle",
      "ellipse",
      "line",
      "polyline",
      "polygon",
      "text",
      "tspan",
      "defs",
      "marker",
    ]);
    const attributes = new Set([
      "xmlns",
      "viewBox",
      "width",
      "height",
      "x",
      "y",
      "x1",
      "y1",
      "x2",
      "y2",
      "cx",
      "cy",
      "r",
      "rx",
      "ry",
      "d",
      "points",
      "fill",
      "stroke",
      "stroke-width",
      "stroke-dasharray",
      "transform",
      "font-size",
      "font-family",
      "font-weight",
      "text-anchor",
      "markerWidth",
      "markerHeight",
      "refX",
      "refY",
      "orient",
      "id",
    ]);
    for (const node of [...parsed.querySelectorAll("*")]) {
      if (!allowed.has(node.localName)) {
        node.remove();
        continue;
      }
      for (const attr of [...node.attributes])
        if (!attributes.has(attr.name) || /url\s*\(|javascript:|data:/i.test(attr.value))
          node.removeAttribute(attr.name);
    }
    return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(new XMLSerializer().serializeToString(parsed))}`;
}
function Diagram({ blockId, content }: { blockId: string; content: Record<string, unknown> }) {
  const [expanded, setExpanded] = useState(false);
  const preview = useMemo(() => safeDiagramPreview(content.svgPreview), [content.svgPreview]);
  const title = typeof content.alt === "string" && content.alt ? content.alt : "Sơ đồ bài nộp";
  return <figure id={`submission-block-${blockId}`} className="my-4 min-w-0 scroll-mt-24 rounded-lg border border-line bg-shell/30 p-3">
    <figcaption className="mb-2 text-xs font-medium text-muted">{title}</figcaption>
    {preview ? <>
      <Image src={preview} alt={title} width={1000} height={700} unoptimized className="h-auto w-full rounded bg-surface" />
      <Button size="sm" className="mt-2" onClick={() => setExpanded(true)}>Xem sơ đồ</Button>
      <Dialog open={expanded} title={title} size="xl" onClose={() => setExpanded(false)}>
        <div className="max-h-[70vh] overflow-auto rounded border border-line bg-surface p-3">
          <Image src={preview} alt={title} width={1600} height={1200} unoptimized className="h-auto max-w-none" />
        </div>
        <p className="mt-2 text-sm text-muted">Cuộn ngang và dọc để xem chi tiết. Đây là bản đã nộp, chỉ đọc.</p>
      </Dialog>
    </> : <p className="text-sm text-muted">Chưa có ảnh xem trước sơ đồ khả dụng.</p>}
    <details className="mt-3 text-xs text-muted">
      <summary className="cursor-pointer">Dữ liệu sơ đồ Draw.io</summary>
      <pre className="mt-2 max-h-48 overflow-auto whitespace-pre rounded border border-line bg-surface p-2">{typeof content.xml === "string" ? content.xml : "Không có XML"}</pre>
    </details>
  </figure>;
}
export function SubmissionViewer({
  document,
  evaluationId,
}: {
  document: DocumentModel;
  evaluationId?: string;
}) {
  return (
    <section
      aria-label="Bài làm đã nộp"
      className="min-w-0 break-words rounded-xl border border-line bg-surface p-4 text-sm leading-relaxed sm:p-5"
    >
      <h2 className="mb-4 text-base font-bold">Bài làm đã nộp — chỉ đọc</h2>
      {document.blocks.map((block) => {
        const c = block.content;
        const anchorId = `submission-block-${block.id}`;
        if (block.type === "DIAGRAM") return <Diagram key={block.id} blockId={block.id} content={c} />;
        if (block.type === "IMAGE")
          return (
            <SubmittedImage
              key={block.id}
              blockId={block.id}
              evaluationId={evaluationId}
              fileId={String(c.fileId ?? "")}
              alt={typeof c.alt === "string" ? c.alt : "Ảnh bài nộp"}
            />
          );
        if (block.type === "TABLE" && Array.isArray(c.rows))
          return (
            <div key={block.id} id={anchorId} className="scroll-mt-24 overflow-auto">
              <table className="my-4 border-collapse">
                {c.rows.map((row, index) => (
                  <tbody key={index}>
                    <tr>
                      {(Array.isArray(row) ? row : [row]).map((cell, i) =>
                        c.headerRow && index === 0 ? (
                          <th key={i} className="border p-2">
                            <RunsView runs={contentRuns(cell)} />
                          </th>
                        ) : (
                          <td key={i} className="border p-2">
                            <RunsView runs={contentRuns(cell)} />
                          </td>
                        ),
                      )}
                    </tr>
                  </tbody>
                ))}
              </table>
            </div>
          );
        if (block.type === "HEADING")
          return (
            <h3 key={block.id} id={anchorId} className="my-4 scroll-mt-24 text-lg font-bold">
              <RunsView runs={contentRuns(c.text)} />
            </h3>
          );
        if (block.type === "LIST" && Array.isArray(c.items)) {
          const items = c.items.map((item, i) => (
            <li key={i} style={{ marginLeft: `${listIndent(item)}px` }}><RunsView runs={contentRuns(item)} /></li>
          ));
          return c.ordered ? (
            <ol key={block.id} id={anchorId} className={`ml-5 scroll-mt-24 list-decimal ${contentAlignment(c.align)}`}>
              {items}
            </ol>
          ) : (
            <ul key={block.id} id={anchorId} className={`ml-5 scroll-mt-24 list-disc ${contentAlignment(c.align)}`}>
              {items}
            </ul>
          );
        }
        return (
          <p key={block.id} id={anchorId} className={`mb-4 scroll-mt-24 ${contentAlignment(c.align)}`}>
            <RunsView runs={contentRuns(c)} />
          </p>
        );
      })}
      {document.blocks.length === 0 && <p>Chưa có nội dung bài làm khả dụng.</p>}
    </section>
  );
}
