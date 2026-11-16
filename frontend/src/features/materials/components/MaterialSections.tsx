"use client";
import Link from "next/link";
import { Button } from "@/shared/ui/Button";
import { ModuleBoard } from "./ModuleBoard";
import { useMaterialResource } from "../hooks/useMaterialResource";
import { describeFile, buildMaterialHref, type Material } from "../model/types";
import styles from "../styles/materials.module.css";
import { msg } from "@/i18n/msg";

export function MaterialLinkList({
  classId,
  materialIds,
}: {
  classId: string;
  materialIds: string[];
}) {
  const ids = [...new Set(materialIds)].sort();
  return <MaterialLinks classId={classId} ids={ids} title={msg("materials.components.MaterialSections.hocLieuGanKem")} />;
}
/** Học liệu của lớp (sinh viên): module của môn với học liệu và Quiz đang mở của lớp. */
export function ClassMaterialsCard({ classId }: { classId: string }) {
  return <ModuleBoard scope={{ kind: "student", classId }} />;
}
function MaterialLinks({
  classId,
  ids,
  title,
}: {
  classId: string;
  ids?: string[];
  title: string;
}) {
  const params = ids ? `?ids=${ids.map(encodeURIComponent).join(",")}` : "";
  const resource = useMaterialResource<Material[]>(
    ids?.length === 0
      ? null
      : `/api/v1/me/classes/${encodeURIComponent(classId)}/materials${params}`,
  );
  if (ids?.length === 0) return null;
  return (
    <section className={styles.card}>
      <h2>
        {title}
        {resource.data && ` (${resource.data.length})`}
      </h2>
      {resource.loading && <p role="status">{msg("materials.components.MaterialSections.dangTaiHocLieu")}</p>}
      {resource.error && (
        <p role="alert" className={styles.error}>
          {resource.error}
          <Button onClick={resource.reload}>{msg("materials.components.MaterialSections.thuLai")}</Button>
        </p>
      )}
      {resource.data?.length === 0 && <p className={styles.muted}>{msg("materials.components.MaterialSections.chuaCoHocLieu")}</p>}
      <div className={styles.linkList}>
        {resource.data?.map((m, i) => (
          <Link
            data-testid={`material-attached-${m.id}`}
            key={m.id}
            href={buildMaterialHref(m.id, classId, true)}
          >
            {i + 1}. {m.title}
            <span className="ml-2 text-xs text-muted">
              {describeFile(m)}
              {m.scope === "CLASS" && msg("materials.components.MaterialSections.cuaLop")}
            </span>
          </Link>
        ))}
      </div>
    </section>
  );
}
