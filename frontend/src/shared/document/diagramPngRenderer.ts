import { DRAWIO_ORIGIN } from "./DiagramEditor";
import type { DocumentBlock } from "./types";

/** Draw.io ẩn chỉ để xuất ảnh: không hiện giao diện, không lưu gì. */
const EXPORT_URL = `${DRAWIO_ORIGIN}/?embed=1&ui=min&spin=0&proto=json&noSaveBtn=1&noExitBtn=1`;
const INIT_TIMEOUT_MS = 25_000;
const STEP_TIMEOUT_MS = 15_000;
const PNG_PREFIX = "data:image/png;base64,";

type DrawioMessage = { event?: string; data?: string };

/** Sơ đồ cần dựng ảnh: có XML nhưng chưa có ảnh PNG lưu sẵn (lưu trước 2026-11-27 hoặc chưa mở Draw.io). */
export function listDiagramsWithoutPng(blocks: DocumentBlock[]): Extract<DocumentBlock, { type: "DIAGRAM" }>[] {
  return blocks.filter((block): block is Extract<DocumentBlock, { type: "DIAGRAM" }> =>
    block.type === "DIAGRAM" && !block.content.pngPreview && !!block.content.xml?.trim());
}

/**
 * Dựng ảnh PNG cho các sơ đồ chưa có ảnh lưu sẵn trước khi tải DOCX (yêu cầu 2026-11-27): máy chủ không vẽ được sơ đồ
 * từ XML, nên trình duyệt mở một Draw.io ẩn, nạp lần lượt từng sơ đồ và xuất {@code xmlpng}. Trả blockId → data URI PNG;
 * sơ đồ nào lỗi hay quá giờ thì bỏ qua (tệp tải về dùng hình đại diện như trước). Không có sơ đồ nào thì không mở Draw.io.
 */
export async function renderMissingDiagramPngs(blocks: DocumentBlock[]): Promise<Record<string, string>> {
  const targets = listDiagramsWithoutPng(blocks);
  if (targets.length === 0 || typeof document === "undefined") return {};
  const frame = document.createElement("iframe");
  frame.src = EXPORT_URL;
  frame.title = "Draw.io";
  frame.setAttribute("aria-hidden", "true");
  frame.tabIndex = -1;
  frame.referrerPolicy = "no-referrer";
  Object.assign(frame.style, { position: "fixed", left: "-20000px", top: "0", width: "1200px", height: "900px", opacity: "0", pointerEvents: "none", border: "0" });

  let waiting: { event: string; resolve: (message: DrawioMessage) => void } | null = null;
  const receive = (event: MessageEvent) => {
    if (event.origin !== DRAWIO_ORIGIN || event.source !== frame.contentWindow || typeof event.data !== "string") return;
    let message: DrawioMessage;
    try {
      message = JSON.parse(event.data) as DrawioMessage;
    } catch {
      return;
    }
    if (waiting && message.event === waiting.event) {
      const done = waiting;
      waiting = null;
      done.resolve(message);
    }
  };
  const next = (event: string, timeout: number, send?: Record<string, unknown>) => new Promise<DrawioMessage | null>((resolve) => {
    const timer = setTimeout(() => { waiting = null; resolve(null); }, timeout);
    waiting = { event, resolve: (message) => { clearTimeout(timer); resolve(message); } };
    if (send) frame.contentWindow?.postMessage(JSON.stringify(send), DRAWIO_ORIGIN);
  });

  window.addEventListener("message", receive);
  document.body.appendChild(frame);
  const out: Record<string, string> = {};
  try {
    if (!(await next("init", INIT_TIMEOUT_MS))) return out;
    for (const block of targets) {
      if (!(await next("load", STEP_TIMEOUT_MS, { action: "load", xml: block.content.xml, autosave: 0 }))) continue;
      const exported = await next("export", STEP_TIMEOUT_MS, { action: "export", format: "xmlpng" });
      if (exported?.data?.startsWith(PNG_PREFIX)) out[block.id] = exported.data;
    }
    return out;
  } finally {
    window.removeEventListener("message", receive);
    frame.remove();
  }
}
