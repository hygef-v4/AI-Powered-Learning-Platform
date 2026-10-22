"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { announcementsApi } from "./api";
import type { Announcement } from "./types";
import styles from "./announcements.module.css";
export function ClassAnnouncementsCard({ classId }: { classId: string }) {
  const [result, setResult] = useState<{ classId: string; items?: Announcement[]; failed?: boolean }>({ classId: "" });
  useEffect(() => { const controller = new AbortController(); void announcementsApi.list(classId, 1, 3, controller.signal).then(data => { if (!controller.signal.aborted) setResult({ classId, items: data.items }); }).catch(() => { if (!controller.signal.aborted) setResult({ classId, failed: true }); }); return () => controller.abort(); }, [classId]);
  const current = result.classId === classId ? result : undefined;
  return <section className={styles.card}><h2>Thông báo lớp</h2>{current?.failed ? <p role="alert">Không thể tải thông báo.</p> : !current?.items ? <p>Đang tải…</p> : current.items.length ? current.items.map(a => <Link className={styles.cardLink} key={a.id} href={`/announcements?classId=${encodeURIComponent(classId)}`}><strong>{a.title}{a.important && <span className={styles.badge}>Quan trọng</span>}</strong><span className={styles.muted}>{a.authorName} · {new Date(a.createdAt).toLocaleString("vi-VN")}</span></Link>) : <p className={styles.muted}>Chưa có thông báo.</p>}<Link className={styles.allLink} href={`/announcements?classId=${encodeURIComponent(classId)}`}>Xem tất cả →</Link></section>;
}
