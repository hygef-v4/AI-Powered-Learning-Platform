"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Button, PageHeader, Section, showToast } from "@/shared/ui";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { ArchiveItemDialog, ModuleItemDialog } from "./MaterialDialogs";
import { MaterialPreview } from "./MaterialPreview";
import { ModuleNeighbours, useModulePlace, useModuleScope } from "./ModulePlace";
import { contextQuery, materialPath } from "./api";
import { boardHref, notifyOutlineChanged, useOutlineChanged } from "./outline";
import { learningMaterialSections } from "./slots";
import { useMaterialResource } from "./useMaterialResource";
import type { Material, ModuleScope } from "./types";

/** Nơi xem học liệu: lớp (sinh viên, giảng viên) theo `?classId=`, không có thì là môn (Chủ nhiệm môn). */
export function useMaterialScope(material: Material | undefined, classId?: string): ModuleScope | null {
  return useModuleScope(material?.subjectId, classId);
}

/**
 * Trang chi tiết một học liệu (trang riêng): tên module nhỏ phía trên tên học liệu, trình xem rộng hết trang, mô tả, rồi
 * mục trước / sau trong danh sách module. Người quản lý có Sửa (cùng popup Tạo học liệu) và Xóa như ở danh sách.
 */
export function LearningMaterialPage({ id, classId }: { id: string; classId?: string }) {
  const router = useRouter();
  const resource = useMaterialResource<Material>(`${materialPath(id)}/view${contextQuery(classId)}`);
  const m = resource.data;
  const scope = useMaterialScope(m, classId);
  const place = useModulePlace(scope, (item) => item.kind === "material" && item.material.id === id);
  const [dialog, setDialog] = useState<"edit" | "archive" | null>(null);
  useOutlineChanged(resource.reload);
  const student = scope?.kind === "student";

  if (resource.error) return <PageLoadError onRetry={resource.reload}>{resource.error}</PageLoadError>;
  if (!m) return <PageLoading label="Đang tải học liệu…" />;
  return <div className="flex min-w-0 flex-col gap-5">
    <PageHeader title={<span className="min-w-0 [overflow-wrap:anywhere]">{m.title}</span>}
      breadcrumb={place.moduleTitle ? [{ label: place.moduleTitle }, { label: m.title }] : undefined}
      actions={!student && m.canManage && m.status === "ACTIVE" && <>
        <Button icon="edit" onClick={() => setDialog("edit")} data-testid="material-edit-open">Sửa</Button>
        <Button variant="danger" icon="trash" onClick={() => setDialog("archive")} data-testid="material-archive-open">Xóa</Button>
      </>} />
    <ModuleNeighbours scope={scope} previous={place.previous} next={place.next} position="top" />
    <MaterialPreview material={m} classId={classId} />
    {m.description && <Section title="Mô tả"><p className="text-[14px] leading-relaxed whitespace-pre-wrap text-body">{m.description}</p></Section>}
    {classId && <SlotOutlet slot={learningMaterialSections} props={{ materialId: id, classId }} />}
    <ModuleNeighbours scope={scope} previous={place.previous} next={place.next} />
    {dialog === "edit" && scope && scope.kind !== "student" && <ModuleItemDialog scope={scope} moduleId={m.moduleId} item={{ kind: "material", material: m }}
      onClose={() => setDialog(null)} onSaved={(message) => { setDialog(null); showToast(message); notifyOutlineChanged(); }} />}
    {dialog === "archive" && <ArchiveItemDialog item={{ material: m }} classId={classId} onClose={() => setDialog(null)}
      onSaved={() => { showToast("Đã xóa học liệu", "danger"); notifyOutlineChanged(); router.push(scope ? boardHref(scope) : "/"); }} />}
  </div>;
}
