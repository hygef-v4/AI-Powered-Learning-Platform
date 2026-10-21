"use client";

import { useEffect, useRef, useState } from "react";
import { Button } from "@/shared/ui";

export const DRAWIO_ORIGIN = "https://embed.diagrams.net";
const DRAWIO_URL = `${DRAWIO_ORIGIN}/?embed=1&ui=min&spin=1&proto=json&saveAndExit=1&noExitBtn=0&lang=vi`;

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
 * Draw.io nhúng từ embed.diagrams.net (NFR-U07-32): chỉ nhận message đúng origin và đúng iframe; Lưu thì xuất
 * {@code xmlsvg} để có XML đầy đủ và SVG xem trước (backend làm sạch SVG khi lưu).
 */
export function DiagramEditor({ xml, title, onSave, onClose }: Props) {
  const frame = useRef<HTMLIFrameElement>(null);
  const pending = useRef<string | null>(null);
  const [ready, setReady] = useState(false);
  const handlers = useRef({ onSave, onClose, xml });
  useEffect(() => {
    handlers.current = { onSave, onClose, xml };
  }, [onSave, onClose, xml]);

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
    <div className="fixed inset-0 z-50 flex flex-col bg-ink/60 p-4" role="dialog" aria-modal="true" aria-label={title}>
      <div className="flex items-center justify-between rounded-t-xl bg-white px-4 py-2">
        <p className="text-sm font-semibold text-ink">{title}</p>
        <div className="flex items-center gap-2">
          {!ready && <span role="status" className="text-xs text-muted">Đang mở Draw.io…</span>}
          <Button size="sm" onClick={onClose}>Đóng</Button>
        </div>
      </div>
      {/* embed.diagrams.net trả 403 khi Referer là localhost; không gửi Referer (cũng không lộ URL trang). */}
      <iframe ref={frame} src={DRAWIO_URL} title={title} referrerPolicy="no-referrer" className="min-h-0 flex-1 rounded-b-xl border-0 bg-white" />
    </div>
  );
}
