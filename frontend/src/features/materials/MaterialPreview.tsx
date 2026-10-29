"use client";
import { useEffect, useState } from "react";
import { apiDownload } from "@/shared/api/client";
import { Button, Icon } from "@/shared/ui";
import { materialApi, materialError } from "./api";
import { DocumentViewer, documentKind } from "./DocumentViewer";
import { fileDescription, type Material } from "./types";

/** Tải tệp hiện hành bằng link tải mới. */
async function download(material: Material, classId?: string) {
  const fresh = await materialApi.token(material.id, classId);
  window.location.assign(fresh.url);
}

/** Nút Tải xuống (tệp không xem được trong trang). */
export function DownloadButton({ material, classId }: { material: Material; classId?: string }) {
  const [busy, setBusy] = useState(false); const [error, setError] = useState("");
  async function run() {
    if (busy) return; setBusy(true); setError("");
    try { await download(material, classId); } catch (e) { setError(materialError(e)); } finally { setBusy(false); }
  }
  return <>
    <Button data-testid="material-download" icon="download" disabled={busy} onClick={() => void run()}>Tải xuống</Button>
    {error && <span role="alert" className="text-xs text-danger">{error}</span>}
  </>;
}

/**
 * Khung xem học liệu: PDF, Word, PowerPoint, Excel mở trong trình xem tối với trang giấy trắng (tải xuống ở thanh công cụ);
 * video YouTube khung 16:9; loại tệp khác hiện thẻ tệp với nút tải.
 */
export function MaterialPreview({ material, classId }: { material: Material; classId?: string }) {
  const { id: materialId, kind, currentVersionNo } = material;
  const viewable = kind === "FILE" ? documentKind(material.current.mimeType) : null;
  const key = `${materialId}:${currentVersionNo}:${classId ?? ""}`;
  const [revision, setRevision] = useState(0);
  const [loaded, setLoaded] = useState<{ key: string; blob?: Blob; error?: string }>({ key: "" });

  useEffect(() => {
    if (!viewable) return;
    let cancelled = false;
    const controller = new AbortController();
    (async () => {
      try {
        const token = await materialApi.token(materialId, classId);
        const blob = await apiDownload(token.url, { signal: controller.signal });
        if (!cancelled) setLoaded({ key, blob });
      } catch (e) { if (!cancelled) setLoaded({ key, error: materialError(e) }); }
    })();
    return () => { cancelled = true; controller.abort(); };
  }, [materialId, classId, viewable, key, revision]);

  if (kind === "YOUTUBE_LINK") {
    const id = material.current.videoId;
    const valid = !!id && /^[A-Za-z0-9_-]{11}$/.test(id);
    return <div className="flex flex-col gap-2">
      <a data-testid="material-youtube-external" className="inline-flex items-center gap-1 self-end text-xs font-semibold text-primary-ink hover:underline"
        href={`https://www.youtube.com/watch?v=${encodeURIComponent(id ?? "")}`} target="_blank" rel="noopener noreferrer">
        Mở trên YouTube <Icon name="arrow_forward" size={13} className="-rotate-45" />
      </a>
      {valid
        ? <iframe data-testid="material-youtube-player" className="aspect-video w-full rounded-xl border-0 bg-black" title={`Video ${material.title}`}
          src={`https://www.youtube-nocookie.com/embed/${id}`} allow="encrypted-media; picture-in-picture" allowFullScreen referrerPolicy="strict-origin-when-cross-origin" />
        : <div className="flex aspect-video w-full items-center justify-center rounded-xl bg-black text-sm text-white/70">Không phát được video</div>}
    </div>;
  }
  if (viewable) {
    const current = loaded.key === key ? loaded : null;
    if (current?.error) return <div role="alert" className="flex flex-col items-center gap-3 rounded-xl bg-[#525659] p-16 text-center text-sm text-white/85">
      {current.error}<Button data-testid="material-token-retry" icon="refresh" onClick={() => setRevision(v => v + 1)}>Thử lại</Button>
    </div>;
    if (!current?.blob) return <div role="status" className="flex h-[calc(100vh-13rem)] min-h-[560px] items-center justify-center rounded-xl bg-[#525659] text-sm text-white/75">Đang mở tệp…</div>;
    return <DocumentViewer blob={current.blob} kind={viewable} title={material.current.fileName ?? material.title} onDownload={() => void download(material, classId)} />;
  }
  return <div className="flex flex-col items-center gap-4 rounded-xl border border-line bg-surface px-6 py-14 text-center">
    <span aria-hidden="true" className="flex size-16 items-center justify-center rounded-2xl bg-primary-soft text-primary-ink"><Icon name="description" size={30} /></span>
    <div><p className="text-[15px] font-semibold text-ink [overflow-wrap:anywhere]">{material.current.fileName}</p><p className="text-xs text-muted">{fileDescription(material)}</p></div>
    <DownloadButton material={material} classId={classId} />
  </div>;
}
