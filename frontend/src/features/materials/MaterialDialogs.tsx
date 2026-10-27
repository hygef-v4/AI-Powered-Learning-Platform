"use client";
import { useRef, useState } from "react";
import { Dialog } from "@/shared/ui/Dialog";
import { Button } from "@/shared/ui/Button";
import { FileUploader } from "@/features/files/FileUploader";
import { materialApi, materialError, validYoutube } from "./api";
import type { Material, MaterialKind, MaterialSource } from "./types";
import styles from "./materials.module.css";

export function MaterialSourceDialog({ path, kind, material, onClose, onSaved }: {
  path?: string; kind: MaterialKind; material?: Material; onClose: () => void; onSaved: () => void;
}) {
  const [title, setTitle] = useState(""); const [description, setDescription] = useState("");
  const [url, setUrl] = useState(""); const [fileRef, setFileRef] = useState<string>();
  const [busy, setBusy] = useState(false); const [error, setError] = useState(""); const lock = useRef(false);
  async function save() {
    if (lock.current) return;
    if (!material && !title.trim()) { setError("Nhập tên học liệu."); return; }
    if (kind === "YOUTUBE_LINK" ? !validYoutube(url) : !fileRef) {
      setError(kind === "FILE" ? "Chọn tệp đã tải lên." : "Chỉ nhận link HTTPS của một video YouTube, không nhận playlist."); return;
    }
    lock.current = true; setBusy(true); setError("");
    const source: MaterialSource = kind === "FILE" ? { fileRef } : { videoUrl: url.trim() };
    try {
      if (material) await materialApi.replace(material, source);
      else await materialApi.create(path!, { kind, title: title.trim(), description, orderNo: 0, source });
      onSaved();
    } catch (e) { setError(materialError(e)); }
    finally { lock.current = false; setBusy(false); }
  }
  return <Dialog open title={material ? "Thay phiên bản học liệu" : kind === "FILE" ? "Thêm học liệu — Tải tệp" : "Thêm học liệu — Link YouTube"}
    onClose={onClose} dismissible={!busy} footer={<><Button disabled={busy} onClick={onClose}>Hủy</Button><Button data-testid="material-source-save" variant="primary" disabled={busy} onClick={() => void save()}>{busy ? "Đang lưu…" : "Lưu học liệu"}</Button></>}>
    {error && <p role="alert" className={styles.error}>{error}</p>}
    {!material && <><label className={styles.field}>Tên học liệu *<input data-testid="material-source-title" maxLength={200} value={title} onChange={e => setTitle(e.target.value)} disabled={busy} /></label>
      <label className={styles.field}>Mô tả (tùy chọn)<textarea maxLength={2000} value={description} onChange={e => setDescription(e.target.value)} disabled={busy} /></label></>}
    {material && <p className={styles.muted}>Thay tệp hoặc link tạo phiên bản mới; phiên bản cũ vẫn được giữ lại.</p>}
    {kind === "FILE" ? <FileUploader purpose="MATERIAL" onUploaded={result => setFileRef(result.fileRef.value)} />
      : <label className={styles.field}>Đường dẫn YouTube *<input data-testid="material-source-url" type="url" value={url} onChange={e => setUrl(e.target.value)} disabled={busy} placeholder="https://www.youtube.com/watch?v=…" /></label>}
  </Dialog>;
}

export function ArchiveMaterialDialog({ material, onClose, onSaved }: { material: Material; onClose: () => void; onSaved: () => void }) {
  const [busy, setBusy] = useState(false); const [error, setError] = useState(""); const lock = useRef(false);
  async function archive() {
    if (lock.current) return; lock.current = true; setBusy(true);
    try { await materialApi.archive(material); onSaved(); }
    catch (e) { setError(materialError(e)); } finally { lock.current = false; setBusy(false); }
  }
  return <Dialog open title="Lưu trữ học liệu?" onClose={onClose} dismissible={!busy} tone="danger" footer={<><Button className="flex-1" disabled={busy} onClick={onClose}>Hủy</Button><Button className="flex-1" data-testid="material-archive-confirm" variant="danger" disabled={busy} onClick={() => void archive()}>Lưu trữ</Button></>}>
    <p className="text-sm leading-6"><strong>{material.title}</strong> sẽ ẩn với sinh viên. Học liệu không bị xóa; bạn có thể xem ở tab Lưu trữ.</p>
    <p className="mt-3 text-sm leading-6">Bài tập, Quiz đang gắn học liệu vẫn giữ liên kết nhưng sinh viên không mở được học liệu đã lưu trữ.</p>
    {error && <p role="alert" className={styles.error}>{error}</p>}
  </Dialog>;
}
