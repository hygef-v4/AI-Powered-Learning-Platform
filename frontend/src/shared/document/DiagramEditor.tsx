"use client";

import { useEffect, useRef, useState } from "react";
import { Button, cx } from "@/shared/ui";

export const DRAWIO_ORIGIN = "https://embed.diagrams.net";
const DRAWIO_URL = `${DRAWIO_ORIGIN}/?embed=1&ui=min&spin=1&proto=json&saveAndExit=1&noExitBtn=0&lang=vi`;
const MIN_WIDTH = 360;

type Props = {
  xml: string;
  title: string;
  onSave: (xml: string, svg: string | null) => void;
  onClose: () => void;
};

type DrawioMessage = { event?: string; xml?: string; data?: string; format?: string };

/** Giải data URI SVG (base64 hoặc mã hóa URL) mà Draw.io trả khi xuất {@code xmlsvg}. */
export function decodeSvgDataUri(data: string | undefined): string | null {
  if (!data || !data.startsWith("data:image/svg+xml")) return null;
  const comma = data.indexOf(",");
  if (comma < 0) return null;
  const meta = data.slice(0, comma);
  const body = data.slice(comma + 1);
  try {
    if (meta.endsWith(";base64")) {
      const binary = atob(body);
      const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
      return new TextDecoder().decode(bytes);
    }
    return decodeURIComponent(body);
  } catch {
    return null;
  }
}

/**
 * Draw.io nhúng từ embed.diagrams.net (NFR-U07-32) trong cột bên phải kéo giãn được, đẩy trang sang trái thay vì che
 * trang (yêu cầu 2026-10-22, theo bản demo): {@code html.drawio-panel-open} và biến {@code --drawio-panel-width} cho
 * lề phải của body (globals.css). Chỉ nhận message đúng origin và đúng iframe; Lưu thì xuất {@code xmlsvg} để có XML
 * đầy đủ và SVG xem trước (backend làm sạch SVG khi lưu).
 */
export function DiagramEditor({ xml, title, onSave, onClose }: Props) {
  const frame = useRef<HTMLIFrameElement>(null);
  const pending = useRef<string | null>(null);
  const [ready, setReady] = useState(false);
  const [width, setWidth] = useState(() => (typeof window === "undefined" ? 800 : Math.round(window.innerWidth * 0.6)));
  const [dragging, setDragging] = useState(false);
  const handlers = useRef({ onSave, onClose, xml });
  useEffect(() => {
    handlers.current = { onSave, onClose, xml };
  }, [onSave, onClose, xml]);

  useEffect(() => {
    const root = document.documentElement;
    root.classList.add("drawio-panel-open");
    return () => {
      root.classList.remove("drawio-panel-open");
      root.style.removeProperty("--drawio-panel-width");
    };
  }, []);
  useEffect(() => {
    document.documentElement.style.setProperty("--drawio-panel-width", `${width}px`);
  }, [width]);

  // Kéo mép trái để đổi độ rộng; lớp phủ trong lúc kéo để iframe không nuốt sự kiện chuột.
  useEffect(() => {
    if (!dragging) return;
    const move = (event: MouseEvent) => setWidth(Math.round(Math.max(MIN_WIDTH, Math.min(window.innerWidth - event.clientX, window.innerWidth * 0.9))));
    const up = () => setDragging(false);
    window.addEventListener("mousemove", move);
    window.addEventListener("mouseup", up);
    return () => {
      window.removeEventListener("mousemove", move);
      window.removeEventListener("mouseup", up);
    };
  }, [dragging]);

  useEffect(() => {
    function post(message: Record<string, unknown>) {
      frame.current?.contentWindow?.postMessage(JSON.stringify(message), DRAWIO_ORIGIN);
    }
    function receive(event: MessageEvent) {
      if (event.origin !== DRAWIO_ORIGIN || event.source !== frame.current?.contentWindow) return;
      if (typeof event.data !== "string") return;
      let message: DrawioMessage;
      try {
        message = JSON.parse(event.data) as DrawioMessage;
      } catch {
        return;
      }
      if (message.event === "init") {
        setReady(true);
        post({ action: "load", xml: handlers.current.xml, autosave: 0, title });
      } else if (message.event === "save" && message.xml) {
        pending.current = message.xml;
        post({ action: "export", format: "xmlsvg", xml: message.xml, spinKey: "saving" });
      } else if (message.event === "export") {
        const saved = message.xml ?? pending.current;
        if (saved) handlers.current.onSave(saved, decodeSvgDataUri(message.data));
        handlers.current.onClose();
      } else if (message.event === "exit") {
        handlers.current.onClose();
      }
    }
    window.addEventListener("message", receive);
    return () => window.removeEventListener("message", receive);
  }, [title]);

  return (
    <>
      {dragging && <div aria-hidden className="fixed inset-0 z-[59] cursor-col-resize" />}
      <aside role="dialog" aria-label={title} data-testid="drawio-panel" style={{ width }}
        className="fixed inset-y-0 right-0 z-[60] flex animate-[drawio-slide-in_0.25s_ease] flex-col overflow-hidden border-l-2 border-line bg-surface shadow-[-4px_0_24px_rgba(0,0,0,0.18)]">
        <div role="separator" aria-orientation="vertical" aria-label="Kéo để đổi độ rộng" title="Kéo để đổi độ rộng"
          onMouseDown={(event) => { event.preventDefault(); setDragging(true); }}
          className={cx("group absolute inset-y-0 left-0 z-[61] flex w-3.5 cursor-col-resize items-center justify-center hover:bg-primary/10",
            dragging && "bg-primary/20")}>
          <span className="flex flex-col gap-[3px] opacity-35 group-hover:opacity-80">
            {Array.from({ length: 6 }, (_, i) => <span key={i} className="size-1 rounded-full bg-muted" />)}
          </span>
        </div>
        <div className="flex shrink-0 items-center justify-between gap-2 border-b border-line py-2 pr-3.5 pl-[22px]">
          <span className="truncate text-sm font-semibold text-ink">{title}</span>
          <div className="flex shrink-0 items-center gap-2">
            {!ready && <span role="status" className="text-xs text-muted">Đang mở Draw.io…</span>}
            <Button size="sm" onClick={onClose}>✕ Đóng</Button>
          </div>
        </div>
        <div className="relative min-h-0 flex-1">
          {/* embed.diagrams.net trả 403 khi Referer là localhost; không gửi Referer (cũng không lộ URL trang). */}
          <iframe ref={frame} src={DRAWIO_URL} title={title} referrerPolicy="no-referrer" className="absolute inset-0 size-full border-0 bg-surface" />
        </div>
      </aside>
    </>
  );
}
