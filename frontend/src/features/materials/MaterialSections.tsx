"use client";
import Link from "next/link";
import { useId } from "react";
import { Field, LockedInput } from "@/shared/ui";
import { Button } from "@/shared/ui/Button";
import { useMaterialResource } from "./useMaterialResource";
import { fileDescription, materialHref, type Material } from "./types";
import styles from "./materials.module.css";

/** Ô Học liệu trong thẻ Thông tin lớp của giảng viên: số học liệu đang hoạt động của lớp (kể cả của môn). */
export function ClassMaterialCountField({ classId }: { classId: string }) {
  const id = useId();
  const resource = useMaterialResource<Material[]>(`/api/v1/classes/${encodeURIComponent(classId)}/materials`);
  const value = resource.data ? String(resource.data.filter((m) => m.status === "ACTIVE").length) : resource.error ? "Không tải được" : "…";
  return <Field label="Học liệu" htmlFor={`${id}-materials`}><LockedInput id={`${id}-materials`} value={value} /></Field>;
}
export function MaterialLinkList({ classId, materialIds }: { classId: string; materialIds: string[] }) {
  const ids = [...new Set(materialIds)].sort();
  return <MaterialLinks classId={classId} ids={ids} title="Học liệu gắn kèm" />;
}
export function ClassMaterialsCard({ classId }: { classId: string }) {
  return <MaterialLinks classId={classId} title="Học liệu" />;
}
function MaterialLinks({ classId, ids, title }: { classId: string; ids?: string[]; title: string }) {
  const params = ids ? `?ids=${ids.map(encodeURIComponent).join(",")}` : "";
  const resource = useMaterialResource<Material[]>(ids?.length === 0 ? null : `/api/v1/me/classes/${encodeURIComponent(classId)}/materials${params}`);
  if (ids?.length === 0) return null;
  return <section className={styles.card}><h2>{title}{resource.data && ` (${resource.data.length})`}</h2>{resource.loading && <p role="status">Đang tải học liệu…</p>}
    {resource.error && <p role="alert" className={styles.error}>{resource.error}<Button onClick={resource.reload}>Thử lại</Button></p>}
    {resource.data?.length === 0 && <p className={styles.muted}>Chưa có học liệu.</p>}
    <div className={styles.linkList}>{resource.data?.map((m, i) => <Link data-testid={`material-attached-${m.id}`} key={m.id} href={materialHref(m.id, classId, true)}>{i + 1}. {m.title}<span className="ml-2 text-xs text-muted">{fileDescription(m)}{m.scope === "CLASS" && " · của lớp"}</span></Link>)}</div>
  </section>;
}
