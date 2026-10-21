"use client";
import Link from "next/link";
import { useRef, useState } from "react";
import { Button } from "@/shared/ui/Button";
import { MaterialSourceDialog } from "./MaterialDialogs";
import { materialApi, materialError, scopePath } from "./api";
import { useMaterialResource } from "./useMaterialResource";
import { fileDescription, materialHref, textLabels, type Material, type MaterialKind } from "./types";
import styles from "./materials.module.css";

export function MaterialList({ scopeId, scope, readOnly = false }: { scopeId: string; scope: "subjects" | "classes"; readOnly?: boolean }) {
  const path = scopePath(scopeId, scope);
  const resource = useMaterialResource<Material[]>(path);
  const [filter, setFilter] = useState("ACTIVE"); const [menu, setMenu] = useState(false);
  const [adding, setAdding] = useState<MaterialKind>(); const [error, setError] = useState("");
  const [busy, setBusy] = useState(false); const drag = useRef<string | null>(null); const lock = useRef(false);
  const rows = resource.data ?? [];
  const visible = rows.filter(m => filter === "ARCHIVED" ? m.status === "ARCHIVED" : m.status === "ACTIVE" && (filter === "CLASS" || filter === "SUBJECT" ? m.scope === filter : true));
  const own = rows.filter(m => m.status === "ACTIVE" && m.scope === (scope === "subjects" ? "SUBJECT" : "CLASS"));
  async function move(id: string, offset: number, targetId?: string) {
    if (lock.current || own.some(m => !m.canManage)) return;
    const ordered = [...own]; const from = ordered.findIndex(m => m.id === id);
    const to = targetId ? ordered.findIndex(m => m.id === targetId) : from + offset;
    if (from < 0 || to < 0 || to >= ordered.length || from === to) return;
    const moved = ordered.splice(from, 1)[0]; if (!moved) return;
    ordered.splice(to, 0, moved); lock.current = true; setBusy(true); setError("");
    try { await materialApi.reorder(path, ordered); resource.reload(); }
    catch (e) { setError(materialError(e)); } finally { lock.current = false; setBusy(false); }
  }
  const saved = () => { setAdding(undefined); resource.reload(); };
  return <section className={styles.card}>
    <div className={styles.toolbar}>
      {["ACTIVE", ...(scope === "classes" ? ["CLASS", "SUBJECT"] : []), "ARCHIVED"].map(value => <Button key={value} data-testid={`materials-filter-${value.toLowerCase()}`} variant={filter === value ? "primary" : "ghost"} onClick={() => setFilter(value)}>{value === "ACTIVE" ? scope === "classes" ? "Tất cả" : "Đang dùng" : value === "CLASS" ? "Của lớp" : value === "SUBJECT" ? "Của môn" : "Lưu trữ"} ({rows.filter(m => value === "ARCHIVED" ? m.status === value : m.status === "ACTIVE" && (value === "ACTIVE" || m.scope === value)).length})</Button>)}
      {!readOnly && <div className="relative ml-auto"><Button data-testid="materials-add-menu" variant="primary" icon="add" onClick={() => setMenu(!menu)} aria-expanded={menu}>Thêm học liệu</Button>
        {menu && <div className="absolute right-0 z-10 mt-2 flex w-64 flex-col rounded-xl border border-line bg-white p-2 shadow-lg">
          <Button data-testid="materials-add-file" variant="ghost" icon="upload_file" onClick={() => { setMenu(false); setAdding("FILE"); }}>Tải tệp · PDF, DOCX, PPTX</Button>
          <Button data-testid="materials-add-youtube" variant="ghost" onClick={() => { setMenu(false); setAdding("YOUTUBE_LINK"); }}>Link YouTube · Một video</Button>
        </div>}
      </div>}
    </div>
    {(resource.error || error) && <div role="alert" className={styles.error}>{resource.error || error} <Button onClick={() => { setError(""); resource.reload(); }}>Tải lại</Button></div>}
    {resource.loading && <p role="status" className={styles.empty}>Đang tải học liệu…</p>}
    {resource.data && <><div className={styles.tableWrap}><table className={styles.table}><thead><tr><th aria-label="Sắp xếp" /><th>#</th><th>Học liệu</th>{scope === "classes" && <th>Phạm vi</th>}<th>Trích chữ</th><th>Trạng thái</th></tr></thead><tbody>
      {visible.map((m, index) => <tr key={m.id} draggable={!busy && m.canManage && m.status === "ACTIVE"} onDragStart={() => { drag.current = m.id; }} onDragOver={event => { if (m.canManage) event.preventDefault(); }} onDrop={event => { event.preventDefault(); if (drag.current) void move(drag.current, 0, m.id); drag.current = null; }}>
        <td>{m.canManage && !readOnly && <div className="flex items-center gap-1"><span aria-hidden="true">⠿</span><button data-testid={`material-move-up-${m.id}`} type="button" aria-label={`Đưa ${m.title} lên`} disabled={busy || own[0]?.id === m.id} onClick={() => void move(m.id, -1)}>↑</button><button type="button" aria-label={`Đưa ${m.title} xuống`} disabled={busy || own.at(-1)?.id === m.id} onClick={() => void move(m.id, 1)}>↓</button></div>}</td>
        <td>{index + 1}</td><td><Link data-testid={`material-link-${m.id}`} href={materialHref(m.id, scope === "classes" ? scopeId : undefined)}>{m.title}</Link><div className={styles.muted}>{fileDescription(m)} · phiên bản {m.currentVersionNo}</div></td>
        {scope === "classes" && <td><span className={styles.badge}>{m.scope === "SUBJECT" ? "Của môn · Chỉ xem" : "Của lớp"}</span></td>}
        <td>{m.current.textStatus ? textLabels[m.current.textStatus] : "—"}</td><td><span className={styles.badge}>{m.status === "ACTIVE" ? "Đang dùng" : "Lưu trữ"}</span></td>
      </tr>)}
    </tbody></table></div>{!visible.length && <p className={styles.empty}>Chưa có học liệu trong danh sách này.</p>}</>}
    <p className={`${styles.muted} mt-4`}>{scope === "classes" ? "Học liệu của lớp chỉ sinh viên lớp này thấy. Học liệu của môn do Chủ nhiệm môn quản lý, bạn chỉ xem." : "Kéo ở cột đầu tiên hoặc dùng nút lên, xuống để sắp thứ tự. Sinh viên thấy đúng thứ tự này."}</p>
    <p className={styles.muted}>Chỉ tệp có trạng thái trích chữ. Link YouTube chỉ lưu đường dẫn video.</p>
    {adding && <MaterialSourceDialog key={`${path}:${adding}`} path={path} kind={adding} onClose={() => setAdding(undefined)} onSaved={saved} />}
  </section>;
}

export function SubjectMaterialListPage() {
  const subjects = useMaterialResource<{ id: string; code: string; name: string; status?: string }[]>("/api/v1/manager/subjects");
  const [selected, setSelected] = useState("");
  const subject = subjects.data?.find(s => s.id === selected) ?? subjects.data?.[0];
  return <main className={styles.page}><div className={styles.heading}><div><h1>Học liệu</h1><p className={styles.muted}>Học liệu của môn là một danh sách có thứ tự; sinh viên thấy đúng thứ tự này.</p></div>
    <select data-testid="materials-subject-select" className={styles.select} aria-label="Chọn môn học" value={subject?.id ?? ""} onChange={e => setSelected(e.target.value)}><option value="" disabled>Chọn môn học</option>{subjects.data?.map(s => <option key={s.id} value={s.id}>{s.code} · {s.name}</option>)}</select></div>
    {subjects.loading && <p role="status">Đang tải môn học…</p>}
    {subjects.error && <div role="alert" className={styles.error}>{subjects.error}<Button onClick={subjects.reload}>Thử lại</Button></div>}
    {subject ? <MaterialList key={subject.id} scope="subjects" scopeId={subject.id} readOnly={subject.status === "ARCHIVED"} /> : subjects.data && <p className={styles.empty}>Bạn chưa được phân công môn học.</p>}
  </main>;
}

export function ClassMaterialsTab({ classId }: { classId: string }) {
  const info = useMaterialResource<{ status: string }>(`/api/v1/teaching/classes/${encodeURIComponent(classId)}`);
  return <div className={styles.stack}><h2 className="text-lg font-bold">Học liệu</h2><MaterialList key={classId} scopeId={classId} scope="classes" readOnly={info.data?.status === "ARCHIVED"} /></div>;
}
