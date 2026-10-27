"use client";
import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useSession } from "@/features/identity/session";
import { Button } from "@/shared/ui/Button";
import { announcementsApi, announcementError } from "./api";
import { AnnouncementFeed } from "./AnnouncementFeed";
import { AnnouncementComposer } from "./AnnouncementComposer";
import type { AnnouncementFeed as Feed } from "./types";
import styles from "./announcements.module.css";
export function ClassAnnouncementsPage() {
  const params = useSearchParams(); const session = useSession(); const queryClassId = params.get("classId") ?? ""; const [filter, setFilter] = useState({ query: queryClassId, value: queryClassId }); const classId = filter.query === queryClassId ? filter.value : queryClassId; const [pagination, setPagination] = useState({ classId, page: 1 }); const page = pagination.classId === classId ? pagination.page : 1; const [revision, setRevision] = useState(0);
  // Lớp đang chọn ghi lên `?classId=` để sidebar của lớp theo đúng lớp đó.
  function setClassId(value: string) { setFilter({ query: queryClassId, value }); const url = new URL(window.location.href); if (value) url.searchParams.set("classId", value); else url.searchParams.delete("classId"); window.history.replaceState(window.history.state, "", url); }
  function setPage(value: number | ((page: number) => number)) { setPagination(previous => ({ classId, page: typeof value === "number" ? value : value(previous.classId === classId ? previous.page : 1) })); }
  const key = `${classId}:${page}:${revision}`; const [result, setResult] = useState<{ key: string; data?: Feed; error?: string }>({ key: "" });
  useEffect(() => { const controller = new AbortController(); void announcementsApi.list(classId, page, 20, controller.signal).then(data => { if (!controller.signal.aborted) setResult({ key, data }); }).catch(e => { if (!controller.signal.aborted) setResult({ key, error: announcementError(e) }); }); return () => controller.abort(); }, [classId, page, key]);
  const data = result.key === key ? result.data : undefined; const error = result.key === key ? result.error : undefined; const student = session.status === "authenticated" && session.profile.role === "STUDENT"; const selected = data?.classes.find(c => c.classId === classId && c.canManage);
  function reload() { setRevision(value => value + 1); }
  return <main className={styles.page}><header className={styles.header}><div><h1>Thông báo lớp</h1><p className={styles.muted}>{student ? "Thông báo của giảng viên ở các lớp bạn đang học" : "Đăng, sửa và xóa thông báo cho từng lớp bạn dạy"}</p></div><label className={styles.muted}>{student ? "Lọc lớp" : "Chọn lớp"}<select data-testid="announcement-class-filter" aria-label={student ? "Lọc lớp" : "Chọn lớp"} className={styles.filter} value={classId} onChange={e => { setClassId(e.target.value); setPage(1); }}><option value="">Tất cả lớp</option>{data?.classes.map(c => <option key={c.classId} value={c.classId}>{c.code} · {c.name}</option>)}</select></label></header>
    {error ? <div role="alert" className={styles.error}>{error} <Button size="sm" onClick={reload}>Thử lại</Button></div> : !data ? <p role="status">Đang tải thông báo…</p> : <><div className={selected ? styles.columns : undefined}><AnnouncementFeed items={data.items} onChanged={reload} />{selected && <AnnouncementComposer key={selected.classId} selected={selected} onCreated={reload} />}</div>{!student && !selected && <p className={`${styles.muted} mt-4`}>Chọn một lớp bạn dạy để đăng thông báo mới.</p>}<nav className={styles.pager} aria-label="Phân trang thông báo"><Button size="sm" disabled={page === 1} onClick={() => setPage(p => p - 1)}>Trước</Button><span className={styles.muted}>Trang {page}</span><Button size="sm" disabled={!data.hasMore} onClick={() => setPage(p => p + 1)}>Sau</Button></nav></>}
  </main>;
}
