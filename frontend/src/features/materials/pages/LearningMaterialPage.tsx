"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert, Button, PageHeader, Section, Switch, showToast } from "@/shared/ui";
import { FormattedTextView } from "@/shared/document/FormattedTextView";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { ArchiveItemDialog, ModuleItemDialog } from "../components/MaterialDialogs";
import { MaterialPreview } from "../components/MaterialPreview";
import { ModuleNeighbours, useModulePlace, useModuleScope } from "../components/ModulePlace";
import { buildContextQuery, materialApi, describeMaterialError, buildMaterialPath } from "../api";
import { buildBoardHref, notifyOutlineChanged, useOutlineChanged } from "../model/outline";
import { useMaterialResource } from "../hooks/useMaterialResource";
import type { Material, ModuleScope } from "../model/types";
import { msg } from "@/i18n/msg";

/** Nơi xem học liệu: lớp (sinh viên, giảng viên) theo `?classId=`, không có thì là môn (Chủ nhiệm môn). */
export function useMaterialScope(
  material: Material | undefined,
  classId?: string,
): ModuleScope | null {
  return useModuleScope(material?.subjectId, classId);
}

/**
 * Trang chi tiết một học liệu (trang riêng): tên module nhỏ phía trên tên học liệu, trình xem rộng hết trang, mô tả, rồi
 * mục trước / sau trong danh sách module. Người quản lý có Sửa (cùng popup Tạo học liệu) và Xóa như ở danh sách.
 */
export function LearningMaterialPage({ id, classId }: { id: string; classId?: string }) {
  const router = useRouter();
  const resource = useMaterialResource<Material>(
    `${buildMaterialPath(id)}/view${buildContextQuery(classId)}`,
  );
  const [updated, setUpdated] = useState<Material | null>(null);
  const m = updated && resource.data && updated.id === resource.data.id && updated.version >= resource.data.version
    ? updated : resource.data;
  const scope = useMaterialScope(m, classId);
  const place = useModulePlace(
    scope,
    (item) => item.kind === "material" && item.material.id === id,
  );
  const [dialog, setDialog] = useState<"edit" | "archive" | null>(null);
  const [saving, setSaving] = useState(false);
  const [changeError, setChangeError] = useState("");
  useOutlineChanged(resource.reload);
  const student = scope?.kind === "student";

  if (resource.error)
    return <PageLoadError onRetry={resource.reload}>{resource.error}</PageLoadError>;
  if (!m) return <PageLoading label={msg("materials.pages.LearningMaterialPage.dangTaiHocLieu")} />;
  async function handleEnabledChange(enabled: boolean) {
    if (!m || saving) return;
    setSaving(true); setChangeError("");
    try {
      setUpdated(await materialApi.updateMaterial(m, m.title, m.description, undefined,
        { enabled, descriptionDocument: m.descriptionDocument ?? null }));
      notifyOutlineChanged();
    } catch (cause) { setChangeError(describeMaterialError(cause)); }
    finally { setSaving(false); }
  }
  return (
    <div className="flex min-w-0 flex-col gap-5">
      <PageHeader
        title={<span className="min-w-0 [overflow-wrap:anywhere]">{m.title}</span>}
        breadcrumb={
          place.moduleTitle ? [{ label: place.moduleTitle }, { label: m.title }] : undefined
        }
        actions={
          !student &&
          m.canManage &&
          m.status === "ACTIVE" && (
            <>
              <Switch checked={m.enabled !== false} onChange={handleEnabledChange} disabled={saving}
                ariaLabel={msg("materials.pages.LearningMaterialPage.batHocLieu")} label={m.enabled !== false ? msg("materials.pages.LearningMaterialPage.bat") : msg("materials.pages.LearningMaterialPage.tat")} />
              <Button
                icon="edit"
                onClick={() => setDialog("edit")}
                data-testid="material-edit-open"
                disabled={saving}
              >
                {msg("materials.pages.LearningMaterialPage.sua")}</Button>
              <Button
                variant="danger"
                icon="trash"
                onClick={() => setDialog("archive")}
                data-testid="material-archive-open"
                disabled={saving}
              >
                {msg("materials.pages.LearningMaterialPage.xoa")}</Button>
            </>
          )
        }
      />
      {changeError && <Alert tone="danger" role="alert">{changeError}</Alert>}
      <ModuleNeighbours scope={scope} previous={place.previous} next={place.next} position="top" />
      <MaterialPreview material={m} classId={classId} />
      {m.description && (
        <Section title={msg("materials.pages.LearningMaterialPage.moTa")}>
          <FormattedTextView document={m.descriptionDocument} text={m.description}
            className="text-[14px] leading-relaxed" />
        </Section>
      )}
      <ModuleNeighbours scope={scope} previous={place.previous} next={place.next} />
      {dialog === "edit" && scope && scope.kind !== "student" && (
        <ModuleItemDialog
          scope={scope}
          moduleId={m.moduleId}
          item={{ kind: "material", material: m }}
          onClose={() => setDialog(null)}
          onSaved={(message) => {
            setDialog(null);
            showToast(message);
            notifyOutlineChanged();
          }}
        />
      )}
      {dialog === "archive" && (
        <ArchiveItemDialog
          item={{ material: m }}
          classId={classId}
          onClose={() => setDialog(null)}
          onSaved={() => {
            showToast(msg("materials.pages.LearningMaterialPage.daXoaHocLieu"), "danger");
            notifyOutlineChanged();
            router.push(scope ? buildBoardHref(scope) : "/");
          }}
        />
      )}
    </div>
  );
}
