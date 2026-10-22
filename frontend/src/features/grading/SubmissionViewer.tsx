"use client";
import Image from "next/image";
import { useState } from "react";
import { apiFetch } from "@/shared/api/client";
import { message } from "./useResource";
import type { DocumentModel } from "./types";
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
  fileId,
  alt,
  evaluationId,
}: {
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
    <figure className="my-4">
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
function Diagram({ content }: { content: Record<string, unknown> }) {
  const [preview, setPreview] = useState("");
  const [error, setError] = useState("");
  function show() {
    const parsed = new DOMParser().parseFromString(String(content.svgPreview), "image/svg+xml");
    if (parsed.querySelector("parsererror") || parsed.documentElement.localName !== "svg") {
      setError("Không thể đọc ảnh xem trước sơ đồ.");
      return;
    }
    const allowed = new Set([
      "svg",
      "g",
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
    setPreview(
      `data:image/svg+xml;charset=utf-8,${encodeURIComponent(new XMLSerializer().serializeToString(parsed))}`,
    );
  }
  return (
    <div className="my-4">
      {typeof content.svgPreview === "string" && <button onClick={show}>Xem sơ đồ</button>}
      {preview && (
        <Image
          src={preview}
          alt={typeof content.alt === "string" ? content.alt : "Sơ đồ bài nộp"}
          width={1000}
          height={700}
          unoptimized
          className="h-auto max-w-full"
        />
      )}
      {error && <p role="alert">{error}</p>}
      <details>
        <summary>Sơ đồ Draw.io — XML gốc</summary>
        <pre className="overflow-auto">
          {typeof content.xml === "string" ? content.xml : "Không có XML"}
        </pre>
      </details>
    </div>
  );
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
      className="min-w-0 whitespace-pre-wrap break-words rounded-xl border border-line bg-white p-6"
    >
      <h2 className="mb-4 font-bold">Bài làm đã nộp — chỉ đọc</h2>
      {document.blocks.map((block) => {
        const c = block.content;
        if (block.type === "DIAGRAM") return <Diagram key={block.id} content={c} />;
        if (block.type === "IMAGE")
          return (
            <SubmittedImage
              key={block.id}
              evaluationId={evaluationId}
              fileId={String(c.fileId ?? "")}
              alt={typeof c.alt === "string" ? c.alt : "Ảnh bài nộp"}
            />
          );
        if (block.type === "TABLE" && Array.isArray(c.rows))
          return (
            <div key={block.id} className="overflow-auto">
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
            <h3 key={block.id} className="my-4 text-lg font-bold">
              <RunsView runs={contentRuns(c.text)} />
            </h3>
          );
        if (block.type === "LIST" && Array.isArray(c.items)) {
          const items = c.items.map((item, i) => (
            <li key={i} style={{ marginLeft: `${listIndent(item)}px` }}><RunsView runs={contentRuns(item)} /></li>
          ));
          return c.ordered ? (
            <ol key={block.id} className={`ml-5 list-decimal ${contentAlignment(c.align)}`}>
              {items}
            </ol>
          ) : (
            <ul key={block.id} className={`ml-5 list-disc ${contentAlignment(c.align)}`}>
              {items}
            </ul>
          );
        }
        return (
          <p key={block.id} className={`mb-4 ${contentAlignment(c.align)}`}>
            <RunsView runs={contentRuns(c)} />
          </p>
        );
      })}
      {document.blocks.length === 0 && <p>Chưa có nội dung bài làm khả dụng.</p>}
    </section>
  );
}
