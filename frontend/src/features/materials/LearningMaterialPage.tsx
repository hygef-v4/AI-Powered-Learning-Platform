"use client";
import Link from "next/link";
import { Button } from "@/shared/ui/Button";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { useSession } from "@/features/identity/session";
import { learningMaterialSections } from "./slots";
import { MaterialPreview } from "./MaterialPreview";
import { materialPath, contextQuery } from "./api";
import { useMaterialResource } from "./useMaterialResource";
import { fileDescription, materialHref, type Material } from "./types";
import styles from "./materials.module.css";

export function LearningMaterialPage({ id, classId }: { id: string; classId?: string }) {
  const session = useSession();
  const student = session.status === "authenticated" && session.profile.role === "STUDENT";
  const resource = useMaterialResource<Material>(`${materialPath(id)}/view${contextQuery(classId)}`);
  const list = useMaterialResource<Material[]>(classId ? `/api/v1/${student ? "me/" : ""}classes/${encodeURIComponent(classId)}/materials` : null);
  const m = resource.data;
  return <main className={styles.page}><Link className="mb-4 inline-block text-sm text-primary-ink" href={classId ? `/${student ? "" : "teaching/"}classes/${encodeURIComponent(classId)}` : "/manager/materials"}>← Quay lại</Link>
    {resource.loading && <p role="status">Đang tải học liệu…</p>}{resource.error && <div role="alert" className={styles.error}>{resource.error}<Button onClick={resource.reload}>Thử lại</Button></div>}
    {m && <><div className={styles.heading}><div><p className={styles.muted}>Lớp của tôi › Học liệu</p><h1>{m.title}</h1></div></div>
      <div className={styles.learning}><aside className={styles.stack}>{classId && <section className={styles.card}><h2>Học liệu của lớp</h2>{list.error && <p role="alert">{list.error}<Button onClick={list.reload}>Thử lại</Button></p>}<nav aria-label="Học liệu của lớp" className={styles.linkList}>{list.data?.map(item => <Link key={item.id} href={materialHref(item.id, classId, true)} aria-current={item.id === id ? "page" : undefined}>{item.title}</Link>)}</nav></section>}
        {classId && <SlotOutlet slot={learningMaterialSections} props={{ materialId: id, classId }} />}</aside>
      <div className={styles.stack}><section className={styles.card}><h2>Mô tả</h2><p className="mb-3 whitespace-pre-wrap text-sm">{m.description || "Chưa có mô tả."}</p><p className={styles.muted}>{fileDescription(m)} · cập nhật {new Date(m.updatedAt).toLocaleDateString("vi-VN")}</p></section><MaterialPreview material={m} classId={classId} /></div></div>
    </>}
  </main>;
}
