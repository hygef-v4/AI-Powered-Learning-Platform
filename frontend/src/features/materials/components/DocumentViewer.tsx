"use client";
import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { Icon, cx } from "@/shared/ui";
import { PdfPages } from "./PdfPages";
import { readWorkbook, type Sheet } from "../model/xlsx";

export type DocumentKind = "pdf" | "docx" | "pptx" | "xlsx";

const MIME: Record<string, DocumentKind> = {
  "application/pdf": "pdf",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document": "docx",
  "application/vnd.openxmlformats-officedocument.presentationml.presentation": "pptx",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": "xlsx",
};

export function documentKind(mimeType: string | null): DocumentKind | null {
  return mimeType ? MIME[mimeType] ?? null : null;
}

const ZOOMS = [0.5, 0.67, 0.8, 0.9, 1, 1.1, 1.25, 1.5, 1.75, 2];
const FAILED = "Không mở được tệp, hãy tải xuống để xem.";

/**
 * Trình xem tài liệu kiểu trình đọc của Google: khung nền tối, trang giấy trắng giữa khung, thanh công cụ tối ở trên có tên
 * tệp, số trang, phóng to / thu nhỏ và tải xuống. PDF vẽ bằng pdf.js; Word thành trang giấy, PowerPoint thành danh sách
 * slide, Excel thành bảng theo sheet; mọi thứ dựng trên trình duyệt từ tệp đã tải, thư viện chỉ tải khi mở loại tệp đó.
 */
export function DocumentViewer({ blob, kind, title, onDownload }: { blob: Blob; kind: DocumentKind; title: string; onDownload?: () => void }) {
  const frame = useRef<HTMLElement>(null);
  const body = useRef<HTMLDivElement>(null);
  const [fullscreen, setFullscreen] = useState(false);
  const office = useRef<HTMLDivElement>(null);
  const [zoom, setZoom] = useState(1);
  const [width, setWidth] = useState(0);
  const [pages, setPages] = useState(0);
  const [page, setPage] = useState(1);
  const [sheets, setSheets] = useState<Sheet[] | null>(null);
  const [active, setActive] = useState(0);
  const [error, setError] = useState("");
  const failed = useCallback(() => setError(FAILED), []);

  useEffect(() => {
    const changed = () => setFullscreen(document.fullscreenElement === frame.current);
    document.addEventListener("fullscreenchange", changed);
    return () => document.removeEventListener("fullscreenchange", changed);
  }, []);
  function toggleFullscreen() {
    if (document.fullscreenElement) void document.exitFullscreen();
    else void frame.current?.requestFullscreen?.();
  }

  // Độ rộng khung để PDF vừa trang; đếm lại khi đổi cỡ cửa sổ.
  useEffect(() => {
    const element = body.current;
    if (!element) return;
    const measure = () => setWidth(element.clientWidth - 48);
    measure();
    const observer = typeof ResizeObserver === "undefined" ? null : new ResizeObserver(measure);
    observer?.observe(element);
    return () => observer?.disconnect();
  }, []);

  // Word, PowerPoint: dựng vào khung rồi đánh dấu từng trang / slide để đếm.
  useEffect(() => {
    const container = office.current;
    if (!container || (kind !== "docx" && kind !== "pptx")) return;
    let cancelled = false;
    let destroy: (() => void) | undefined;
    container.replaceChildren();
    (async () => {
      try {
        if (kind === "docx") {
          const { renderAsync } = await import("docx-preview");
          if (cancelled) return;
          await renderAsync(blob, container, undefined, { inWrapper: true, ignoreLastRenderedPageBreak: true });
          container.querySelectorAll(".docx-wrapper > section").forEach((section, index) => { (section as HTMLElement).dataset.page = String(index + 1); });
        } else {
          const { init } = await import("pptx-preview");
          if (cancelled) return;
          const slideWidth = Math.min(container.clientWidth, 960);
          const previewer = init(container, { width: slideWidth, height: Math.round(slideWidth * 9 / 16), mode: "list" });
          destroy = () => previewer.destroy();
          await previewer.preview(await blob.arrayBuffer());
          // Thư viện đặt khung slide cao cố định có thanh cuộn riêng, nền đen: bỏ để chỉ còn thanh cuộn của trình xem.
          container.querySelectorAll<HTMLElement>(".pptx-preview-wrapper").forEach((wrapper) => {
            wrapper.style.height = "auto"; wrapper.style.overflow = "visible"; wrapper.style.background = "transparent";
          });
          container.querySelectorAll(".pptx-preview-slide-wrapper").forEach((slide, index) => { (slide as HTMLElement).dataset.page = String(index + 1); });
        }
        if (!cancelled) setPages(container.querySelectorAll("[data-page]").length);
      } catch {
        if (!cancelled) setError(FAILED);
      }
    })();
    return () => { cancelled = true; destroy?.(); container.replaceChildren(); };
  }, [blob, kind]);

  useEffect(() => {
    if (kind !== "xlsx") return;
    let cancelled = false;
    readWorkbook(blob)
      .then((result) => { if (!cancelled) { setSheets(result); setActive(0); } })
      .catch(() => { if (!cancelled) setError(FAILED); });
    return () => { cancelled = true; };
  }, [blob, kind]);

  // Trang đang xem: trang có mép trên gần đỉnh khung nhất.
  function track() {
    const element = body.current;
    if (!element || pages === 0) return;
    const top = element.getBoundingClientRect().top + 24;
    let current = 1;
    element.querySelectorAll<HTMLElement>("[data-page]").forEach((sheet) => {
      if (sheet.getBoundingClientRect().top <= top) current = Number(sheet.dataset.page);
    });
    setPage(current);
  }
  const step = (offset: number) => setZoom((value) => {
    const index = ZOOMS.findIndex((z) => z >= value - 0.001);
    return ZOOMS[Math.min(ZOOMS.length - 1, Math.max(0, (index < 0 ? 4 : index) + offset))]!;
  });
  const sheet = sheets?.[active];

  return <section ref={frame} aria-label={`Xem ${title}`} className={cx("flex flex-col overflow-hidden bg-[#525659] text-white", !fullscreen && "rounded-xl shadow-[var(--shadow-float)]")}>
    <div className="flex h-12 shrink-0 items-center gap-2 bg-[#323639] px-3 text-[13px]">
      <Icon name="description" size={18} className="shrink-0 text-white/70" />
      <span className="min-w-0 flex-1 truncate font-semibold text-white/90" title={title}>{title}</span>
      {kind === "xlsx" && sheets && sheets.length > 1 && <div role="tablist" aria-label="Sheet" className="flex max-w-[45%] gap-1 overflow-x-auto">
        {sheets.map((s, index) => <button key={`${index}-${s.sheet}`} type="button" role="tab" aria-selected={index === active} onClick={() => setActive(index)}
          className={cx("rounded-md px-2.5 py-1 text-xs font-semibold whitespace-nowrap transition-colors", index === active ? "bg-white/20 text-white" : "text-white/70 hover:bg-white/10")}>{s.sheet}</button>)}
      </div>}
      {pages > 0 && <span className="shrink-0 rounded-md bg-black/25 px-2 py-1 font-mono text-xs text-white/85" data-testid="viewer-page">{page} / {pages}</span>}
      <span className="mx-1 h-5 w-px bg-white/15" aria-hidden="true" />
      <ToolButton label="Thu nhỏ" disabled={zoom <= ZOOMS[0]!} onClick={() => step(-1)}><span className="text-lg leading-none">−</span></ToolButton>
      <span className="w-11 shrink-0 text-center font-mono text-xs text-white/85">{Math.round(zoom * 100)}%</span>
      <ToolButton label="Phóng to" disabled={zoom >= ZOOMS[ZOOMS.length - 1]!} onClick={() => step(1)}><span className="text-lg leading-none">+</span></ToolButton>
      <span className="mx-1 h-5 w-px bg-white/15" aria-hidden="true" />
      <ToolButton label={fullscreen ? "Thoát toàn màn hình" : "Toàn màn hình"} onClick={toggleFullscreen}><Icon name={fullscreen ? "fullscreen_exit" : "fullscreen"} size={18} /></ToolButton>
      {onDownload && <ToolButton label="Tải xuống" onClick={onDownload}><Icon name="download" size={18} /></ToolButton>}
    </div>
    <div ref={body} onScroll={track} className={cx("overflow-auto px-6 py-6", fullscreen ? "h-[calc(100vh-3rem)]" : "h-[calc(100vh-13rem)] min-h-[560px]")}>
      {error ? <p role="alert" className="mx-auto mt-24 max-w-sm text-center text-sm text-white/80">{error}</p>
        : kind === "pdf" ? <PdfPages blob={blob} zoom={zoom} width={width} onPages={setPages} onError={failed} />
        : kind === "xlsx" ? (sheet
          ? <div style={{ zoom }} className="mx-auto w-fit min-w-[min(100%,960px)] bg-white text-ink shadow-[0_2px_12px_rgb(0_0_0/0.45)]" data-testid="material-xlsx-viewer">
            <table className="min-w-full border-collapse text-[13px]">
              <tbody>
                {sheet.data.map((row, r) => <tr key={r} className={r === 0 ? "bg-[#f1f3f4] font-semibold" : undefined}>
                  <th scope="row" className="sticky left-0 w-10 border border-[#e0e0e0] bg-[#f1f3f4] px-2 py-1 text-right font-mono text-[11px] font-normal text-[#5f6368]">{r + 1}</th>
                  {row.map((cell, c) => <td key={c} className="border border-[#e0e0e0] px-2 py-1 whitespace-nowrap">{cellText(cell)}</td>)}
                </tr>)}
              </tbody>
            </table>
            {sheet.data.length === 0 && <p className="p-10 text-center text-sm text-[#5f6368]">Sheet trống</p>}
          </div>
          : <p role="status" className="mt-24 text-center text-sm text-white/70">Đang mở tệp…</p>)
        : <div ref={office} style={{ zoom }} data-testid={`material-${kind}-viewer`}
          className={cx("mx-auto flex flex-col items-center gap-4 text-black",
            // Word: mỗi trang là tờ giấy trắng chữ đen, bất kể giao diện sáng hay tối của ứng dụng.
            "[&_.docx-wrapper]:flex [&_.docx-wrapper]:flex-col [&_.docx-wrapper]:items-center [&_.docx-wrapper]:gap-4 [&_.docx-wrapper]:bg-transparent! [&_.docx-wrapper]:p-0!",
            "[&_.docx-wrapper>section]:m-0! [&_.docx-wrapper>section]:bg-white! [&_.docx-wrapper>section]:text-black [&_.docx-wrapper>section]:shadow-[0_2px_12px_rgb(0_0_0/0.45)]",
            "[&_.pptx-preview-slide-wrapper]:mb-4 [&_.pptx-preview-slide-wrapper]:bg-white [&_.pptx-preview-slide-wrapper]:shadow-[0_2px_12px_rgb(0_0_0/0.45)]")} />}
    </div>
  </section>;
}

function ToolButton({ label, disabled, onClick, children }: { label: string; disabled?: boolean; onClick: () => void; children: ReactNode }) {
  return <button type="button" aria-label={label} title={label} disabled={disabled} onClick={onClick}
    className="flex size-8 shrink-0 items-center justify-center rounded-full text-white/85 transition-colors hover:bg-white/15 disabled:opacity-35 disabled:hover:bg-transparent">
    {children}
  </button>;
}

function cellText(value: unknown): string {
  if (value === null || value === undefined) return "";
  if (value instanceof Date) return value.toLocaleDateString("vi-VN");
  return String(value);
}
