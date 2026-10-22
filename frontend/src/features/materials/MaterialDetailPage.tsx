"use client";
import Link from "next/link";
import { useRef, useState } from "react";
import { Button } from "@/shared/ui/Button";
import { materialApi, materialError, materialPath, contextQuery } from "./api";
import { useMaterialResource } from "./useMaterialResource";
import { ArchiveMaterialDialog, MaterialSourceDialog } from "./MaterialDialogs";
import { MaterialPreview } from "./MaterialPreview";
import { fileDescription, textLabels, type Material } from "./types";
import styles from "./materials.module.css";

function MaterialInfoForm({ material, onSaved }: { material: Material; onSaved: () => void }) {
  const [title, setTitle] = useState(material.title); const [description, setDescription] = useState(material.description);
  const [error, setError] = useState(""); const [busy, setBusy] = useState(false); const lock = useRef(false);
  async function save() {
    if (lock.current) return; if (!title.trim()) { setError("Nhập tên học liệu."); return; }
    lock.current = true; setBusy(true); setError("");
    try { await materialApi.update(material, title, description); onSaved(); }
    catch (e) { setError(materialError(e)); } finally { lock.current = false; setBusy(false); }
  }
  return <section className={styles.card}><h2>Thông tin</h2>{error && <p role="alert" className={styles.error}>{error}</p>}
    <label className={styles.field}>Tên học liệu *<input data-testid="material-info-title" value={title} maxLength={200} disabled={!material.canManage || busy} onChange={e => setTitle(e.target.value)} /></label>
    <label className={styles.field}>Mô tả (tùy chọn)<textarea data-testid="material-info-description" value={description} maxLength={2000} disabled={!material.canManage || busy} onChange={e => setDescription(e.target.value)} /></label>
    <p className={styles.muted}>Tệp hiện hành: {material.current.fileName ?? "YouTube"} · phiên bản {material.currentVersionNo}</p><p className={styles.muted}>{fileDescription(material)} · Trích chữ: {material.current.textStatus ? textLabels[material.current.textStatus] : "—"}</p>
    {material.canManage && <Button data-testid="material-info-save" className="mt-4" variant="primary" disabled={busy} onClick={() => void save()}>Lưu thay đổi</Button>}
  </section>;
}

export function MaterialDetailPage({ id, classId }: { id: string; classId?: string }) {
  const resource = useMaterialResource<Material>(materialPath(id) + contextQuery(classId));
  const [dialog, setDialog] = useState<"replace" | "archive">(); const m = resource.data;
  return <main className={styles.page}><Link className="mb-4 inline-block text-sm text-primary" href={classId ? `/teaching/classes/${encodeURIComponent(classId)}?tab=materials` : "/manager/materials"}>← Quay lại học liệu</Link>
    {resource.loading && <p role="status">Đang tải học liệu…</p>}{resource.error && <div role="alert" className={styles.error}>{resource.error} <Button onClick={resource.reload}>Thử lại</Button></div>}
    {m && <><div className={styles.heading}><div><p className={styles.muted}>Học liệu › {m.scope === "SUBJECT" ? "Của môn" : "Của lớp"}</p><h1>{m.title}</h1><span className={styles.badge}>{m.status === "ACTIVE" ? "Đang dùng" : "Lưu trữ"}</span></div>
      <div className={styles.toolbar}>{m.canManage && <><Button data-testid="material-replace-open" onClick={() => setDialog("replace")}>{m.kind === "FILE" ? "Thay tệp" : "Thay link"}</Button><Button data-testid="material-archive-open" variant="danger" onClick={() => setDialog("archive")}>Lưu trữ</Button></>}<Button data-testid="material-detail-reload" onClick={resource.reload}>Tải lại</Button></div></div>
      <div className={styles.detail}><MaterialPreview material={m} classId={classId} /><div className={styles.stack}><MaterialInfoForm key={`${m.id}:${m.version}`} material={m} onSaved={resource.reload} />
        <section className={styles.card}><h2>Lịch sử phiên bản</h2><div className={styles.tableWrap}><table className={styles.table}><thead><tr><th>Phiên bản</th><th>Thời điểm</th><th>Trích chữ</th><th>Thao tác</th></tr></thead><tbody>{m.versions.map(v => <tr key={v.versionNo}><td>{v.versionNo}{v.versionNo === m.currentVersionNo && <span className={styles.badge}>Hiện hành</span>}<div className={styles.muted}>Người tạo: {v.createdBy}</div></td><td>{new Date(v.createdAt).toLocaleString("vi-VN")}</td><td>{v.textStatus ? textLabels[v.textStatus] : "—"}</td><td>{m.kind === "FILE" ? <VersionDownload id={m.id} classId={classId} number={v.versionNo} /> : v.videoUrl && <a href={v.videoUrl} target="_blank" rel="noopener noreferrer">Xem video</a>}</td></tr>)}</tbody></table></div></section>
      </div></div>
      {dialog === "replace" && <MaterialSourceDialog kind={m.kind} material={m} onClose={() => setDialog(undefined)} onSaved={() => { setDialog(undefined); resource.reload(); }} />}
      {dialog === "archive" && <ArchiveMaterialDialog material={m} onClose={() => setDialog(undefined)} onSaved={() => { setDialog(undefined); resource.reload(); }} />}
    </>}
  </main>;
}
function VersionDownload({ id, classId, number }: { id: string; classId?: string; number: number }) {
  const [error, setError] = useState(""); const [busy, setBusy] = useState(false);
  async function download() { if (busy) return; setBusy(true); try { const token = await materialApi.token(id, classId, number); window.location.assign(token.url); } catch (e) { setError(materialError(e)); } finally { setBusy(false); } }
  return <><Button size="sm" disabled={busy} onClick={() => void download()}>Tải phiên bản {number}</Button>{error && <p role="alert">{error}</p>}</>;
}
