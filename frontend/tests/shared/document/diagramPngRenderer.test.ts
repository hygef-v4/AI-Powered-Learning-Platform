import { afterEach, expect, it, vi } from "vitest";
import { DRAWIO_ORIGIN } from "@/shared/document/DiagramEditor";
import { listDiagramsWithoutPng, renderMissingDiagramPngs } from "@/shared/document/diagramPngRenderer";
import type { DocumentBlock } from "@/shared/document";

const diagram = (id: string, extra: Record<string, unknown> = {}): DocumentBlock =>
  ({ id, origin: "TEACHER", type: "DIAGRAM", content: { diagramType: "OTHER", xml: "<mxfile/>", ...extra } }) as DocumentBlock;

afterEach(() => { document.querySelectorAll("iframe").forEach((frame) => frame.remove()); });

it("chỉ dựng ảnh cho sơ đồ có XML mà chưa có ảnh lưu sẵn (yêu cầu 2026-11-27)", () => {
  const blocks = [diagram("a"), diagram("b", { pngPreview: "data:image/png;base64,AAAA" }), diagram("c", { xml: "  " }),
    { id: "p", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [] } } as DocumentBlock];
  expect(listDiagramsWithoutPng(blocks).map((b) => b.id)).toEqual(["a"]);
});

it("không mở Draw.io khi không có sơ đồ cần dựng", async () => {
  expect(await renderMissingDiagramPngs([diagram("b", { pngPreview: "data:image/png;base64,AAAA" })])).toEqual({});
  expect(document.querySelector("iframe")).toBeNull();
});

it("nạp từng sơ đồ vào Draw.io ẩn, xuất xmlpng rồi gỡ iframe", async () => {
  const promise = renderMissingDiagramPngs([diagram("a")]);
  const frame = document.querySelector("iframe")!;
  expect(frame.style.opacity).toBe("0");
  const sent: unknown[] = [];
  vi.spyOn(frame.contentWindow!, "postMessage").mockImplementation((message) => {
    const parsed = JSON.parse(String(message)) as { action: string };
    sent.push(parsed.action);
    const reply = parsed.action === "load" ? { event: "load" } : { event: "export", data: "data:image/png;base64,iVBOR" };
    queueMicrotask(() => window.dispatchEvent(new MessageEvent("message", { origin: DRAWIO_ORIGIN, source: frame.contentWindow, data: JSON.stringify(reply) })));
  });
  window.dispatchEvent(new MessageEvent("message", { origin: DRAWIO_ORIGIN, source: frame.contentWindow, data: JSON.stringify({ event: "init" }) }));
  expect(await promise).toEqual({ a: "data:image/png;base64,iVBOR" });
  expect(sent).toEqual(["load", "export"]);
  expect(document.querySelector("iframe")).toBeNull();
});
