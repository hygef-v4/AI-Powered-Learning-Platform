"use client";
import { useEffect, useRef, useState } from "react";

/**
 * Vẽ PDF thành từng trang giấy trắng (pdf.js), rộng vừa khung xem nhân với mức phóng; mỗi trang đánh dấu `data-page` để
 * thanh công cụ đếm trang. Vẽ lại khi đổi mức phóng hay độ rộng khung.
 */
export function PdfPages({ blob, zoom, width, onPages, onError }: {
  blob: Blob; zoom: number; width: number; onPages: (count: number) => void; onError: () => void;
}) {
  const host = useRef<HTMLDivElement>(null);
  const [doc, setDoc] = useState<import("pdfjs-dist").PDFDocumentProxy | null>(null);

  useEffect(() => {
    let cancelled = false;
    let task: { destroy: () => Promise<void> } | null = null;
    (async () => {
      try {
        const pdfjs = await import("pdfjs-dist");
        pdfjs.GlobalWorkerOptions.workerSrc = new URL("pdfjs-dist/build/pdf.worker.min.mjs", import.meta.url).toString();
        const loading = pdfjs.getDocument({ data: new Uint8Array(await blob.arrayBuffer()) });
        task = loading;
        const loaded = await loading.promise;
        if (cancelled) return;
        setDoc(loaded);
        onPages(loaded.numPages);
      } catch {
        if (!cancelled) onError();
      }
    })();
    return () => { cancelled = true; void task?.destroy(); };
  }, [blob, onPages, onError]);

  useEffect(() => {
    const container = host.current;
    if (!doc || !container || width <= 0) return;
    let cancelled = false;
    const tasks: { cancel: () => void }[] = [];
    container.replaceChildren();
    (async () => {
      const ratio = window.devicePixelRatio || 1;
      for (let number = 1; number <= doc.numPages && !cancelled; number++) {
        const page = await doc.getPage(number);
        const base = page.getViewport({ scale: 1 });
        const viewport = page.getViewport({ scale: (Math.min(width, 960) / base.width) * zoom });
        const sheet = document.createElement("div");
        sheet.dataset.page = String(number);
        sheet.className = "bg-white shadow-[0_2px_12px_rgb(0_0_0/0.45)]";
        sheet.style.width = `${Math.floor(viewport.width)}px`;
        sheet.style.height = `${Math.floor(viewport.height)}px`;
        const canvas = document.createElement("canvas");
        canvas.width = Math.floor(viewport.width * ratio);
        canvas.height = Math.floor(viewport.height * ratio);
        canvas.style.width = "100%";
        canvas.style.height = "100%";
        sheet.appendChild(canvas);
        container.appendChild(sheet);
        const context = canvas.getContext("2d");
        if (!context) continue;
        const task = page.render({ canvas, canvasContext: context, viewport, transform: ratio !== 1 ? [ratio, 0, 0, ratio, 0, 0] : undefined });
        tasks.push(task);
        try { await task.promise; } catch { /* hủy khi vẽ lại */ }
      }
    })();
    return () => { cancelled = true; tasks.forEach((task) => task.cancel()); };
  }, [doc, zoom, width]);

  return <div ref={host} data-testid="material-pdf-viewer" className="flex flex-col items-center gap-4" />;
}
