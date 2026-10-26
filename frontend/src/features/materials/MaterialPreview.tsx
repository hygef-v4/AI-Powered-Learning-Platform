"use client";
import { useEffect, useState } from "react";
import { Button } from "@/shared/ui/Button";
import { apiDownload } from "@/shared/api/client";
import { materialApi, materialError } from "./api";
import type { Material, DownloadToken } from "./types";
import styles from "./materials.module.css";

export function MaterialPreview({ material, classId }: { material: Material; classId?: string }) {
  const { id: materialId, kind, currentVersionNo } = material;
  const mimeType = material.current.mimeType;
  const [token, setToken] = useState<DownloadToken>(); const [revision, setRevision] = useState(0);
  const [error, setError] = useState(""); const [loading, setLoading] = useState(false);
  const key = `${material.id}:${material.currentVersionNo}:${classId ?? ""}:${revision}`;
  const [tokenKey, setTokenKey] = useState("");
  const [pdfUrl, setPdfUrl] = useState("");
  useEffect(() => {
    if (kind !== "FILE") return;
    let cancelled = false; let objectUrl: string | undefined;
    const controller = new AbortController();
    async function load() {
      try {
        const value = await materialApi.token(materialId, classId);
        if (cancelled) return;
        if (mimeType === "application/pdf") {
          const blob = await apiDownload(value.url, { signal: controller.signal });
          if (cancelled) return;
          objectUrl = URL.createObjectURL(blob); setPdfUrl(objectUrl);
        }
        setToken(value); setTokenKey(key); setError("");
      } catch (e) { if (!cancelled) setError(materialError(e)); }
    }
    void load();
    return () => { cancelled = true; controller.abort(); if (objectUrl) URL.revokeObjectURL(objectUrl); };
  }, [materialId, currentVersionNo, mimeType, kind, classId, key]);
  const current = tokenKey === key ? token : undefined;
  useEffect(() => {
    if (!current) return;
    const timer = setTimeout(() => setRevision(v => v + 1), Math.max(1000, new Date(current.expiresAt).getTime() - Date.now() - 15_000));
    return () => clearTimeout(timer);
  }, [current]);
  async function download() {
    if (loading) return; setLoading(true); setError("");
    try { const fresh = await materialApi.token(material.id, classId); window.location.assign(fresh.url); }
    catch (e) { setError(materialError(e)); } finally { setLoading(false); }
  }
  if (material.kind === "YOUTUBE_LINK") {
    const id = material.current.videoId;
    return <section className={styles.card}><h2>Xem trước</h2>{id && /^[A-Za-z0-9_-]{11}$/.test(id) && <iframe data-testid="material-youtube-player" className={styles.video} title={`Video ${material.title}`} src={`https://www.youtube-nocookie.com/embed/${id}`} allow="encrypted-media; picture-in-picture" allowFullScreen referrerPolicy="strict-origin-when-cross-origin" />}
      <a data-testid="material-youtube-external" className="mt-3 inline-block text-sm text-primary-ink" href={`https://www.youtube.com/watch?v=${encodeURIComponent(id ?? "")}`} target="_blank" rel="noopener noreferrer">Mở trên YouTube ↗</a><p className={styles.muted}>Video riêng tư hoặc bị xóa sẽ không phát được.</p></section>;
  }
  return <section className={styles.card}><div className={styles.toolbar}><h2 className="mr-auto">Xem trước</h2><Button data-testid="material-download" onClick={() => void download()} disabled={loading}>Tải xuống</Button><Button data-testid="material-token-retry" onClick={() => setRevision(v => v + 1)}>Lấy link mới</Button></div>
    {error && <p role="alert" className={styles.error}>{error}</p>}
    {material.current.mimeType === "application/pdf" ? current && pdfUrl ? <iframe data-testid="material-pdf-viewer" title={`PDF ${material.title}`} className={styles.viewer} src={pdfUrl} /> : <p role="status">Đang lấy link xem PDF…</p>
      : <p className={styles.empty}>{material.current.fileName} — tải tệp để xem nội dung.</p>}
  </section>;
}
