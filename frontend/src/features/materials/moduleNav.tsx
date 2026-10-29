"use client";

// U13: khi đang xem một học liệu hay một Quiz (trang soạn của người quản lý, trang làm của sinh viên), sidebar đổi thành
// danh sách module của môn (học liệu và Quiz từng module), đầu sidebar chỉ có lối về trang Học liệu, không có ô chọn.
// Người quản lý thêm học liệu vào module, sửa, xóa (Quiz: lưu trữ) ngay trên sidebar.

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Fragment, useState } from "react";
import { FADE_LABEL, type NavContext, type NavLocation } from "@/shared/layout/navigation";
import { showToast } from "@/shared/ui";
import { cx } from "@/shared/ui/cx";
import { Icon, type IconName } from "@/shared/ui/Icon";
import { contextQuery, materialPath } from "./api";
import { ArchiveItemDialog, ModuleItemDialog } from "./MaterialDialogs";
import { canEditItem, reopen } from "./ModuleBoard";
import { useModuleScope } from "./ModulePlace";
import { boardHref, itemHref, itemIcon, itemKey, itemTitle, notifyOutlineChanged, useModuleOutline, type OutlineItem } from "./outline";
import { useMaterialResource } from "./useMaterialResource";
import type { Material, MaterialModule } from "./types";

type Source = { kind: "material" | "quiz" | "practice"; id: string };
type Dialogs = { type: "item"; module: MaterialModule; item?: OutlineItem } | { type: "archive"; item: OutlineItem; active: boolean };

/** Nút nhỏ trên sidebar; ẩn khi sidebar thu gọn. */
const TOOL = "flex size-6 shrink-0 items-center justify-center rounded-md text-muted transition-colors hover:bg-surface hover:text-ink md:group-data-[collapsed]/sidebar:hidden";

function sourceOf(pathname: string, classId?: string): Source | null {
  const material = /^\/materials\/([^/]+)(\/view)?$/.exec(pathname);
  if (material) return { kind: "material", id: decodeURIComponent(material[1]!) };
  const quiz = /^\/quizzes\/([^/]+)$/.exec(pathname);
  if (quiz) return { kind: "quiz", id: decodeURIComponent(quiz[1]!) };
  const practice = /^\/quiz-practice\/([^/]+)$/.exec(pathname);
  if (practice && classId) return { kind: "practice", id: decodeURIComponent(practice[1]!) };
  return null;
}

export function moduleContext({ pathname, search }: NavLocation): NavContext | null {
  const classId = search.get("classId") ?? undefined;
  const source = sourceOf(pathname, classId);
  if (!source) return null;
  return {
    key: `material-modules:${classId ?? "subject"}`,
    label: "Danh sách module",
    header: <ModuleNav source={source} classId={classId} />,
    items: [],
  };
}

function ModuleNav({ source, classId }: { source: Source; classId?: string }) {
  const pathname = usePathname();
  const router = useRouter();
  // Môn của mục đang mở (trang của môn không có `classId`): đọc từ học liệu hay Quiz.
  const owner = useMaterialResource<Material | { quiz: { subjectId: string } }>(classId ? null
    : source.kind === "material" ? `${materialPath(source.id)}${contextQuery(classId)}` : `/api/v1/quizzes/${encodeURIComponent(source.id)}`);
  const subjectId = owner.data ? "quiz" in owner.data ? owner.data.quiz.subjectId : owner.data.subjectId : undefined;
  const scope = useModuleScope(subjectId, classId);
  const { outline } = useModuleOutline(scope);
  const [dialog, setDialog] = useState<Dialogs | null>(null);
  const back = scope ? boardHref(scope) : "/manager/materials";
  const staff = scope && scope.kind !== "student" ? scope : null;
  const isActive = (item: OutlineItem) => item.kind === "material" ? pathname.startsWith(`/materials/${encodeURIComponent(item.material.id)}`)
    : source.kind !== "material" && item.quiz.quizId === source.id;

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="mb-3 shrink-0 border-b border-line pb-3.5">
        <Link href={back} title="Học liệu"
          className="inline-flex max-w-full items-center gap-2.5 rounded-md px-2.5 py-1 text-xs font-semibold whitespace-nowrap text-muted transition-colors hover:text-ink">
          <Icon name="arrow_back" size={17} className="shrink-0" />
          <span className={FADE_LABEL}>Học liệu</span>
        </Link>
      </div>
      <nav aria-label="Danh sách module" className="flex min-h-0 flex-1 flex-col gap-0.5 overflow-x-hidden overflow-y-auto">
        {!outline && <span className={cx("px-2.5 py-2 text-[13px] text-muted", FADE_LABEL)}>Đang tải…</span>}
        {scope && outline?.map(({ module, items }, index) => (
          <Fragment key={module.id}>
            <div className={cx("mx-2.5 mb-1 flex items-center gap-1", index === 0 ? "mt-1" : "mt-5")}>
              <span className={cx("line-clamp-2 min-w-0 flex-1 text-[11.5px] font-semibold text-muted", FADE_LABEL)}>{module.title}</span>
              {staff && (staff.kind === "class" || module.canManage) && <button type="button" className={TOOL}
                aria-label={`Tạo học liệu trong ${module.title}`} title="Tạo học liệu" onClick={() => setDialog({ type: "item", module })}>
                <Icon name="add" size={15} />
              </button>}
            </div>
            {items.length === 0 && <span className={cx("px-2.5 py-1 text-[12.5px] text-faint", FADE_LABEL)}>Chưa có học liệu</span>}
            {items.map((item) => {
              const active = isActive(item);
              const editable = !!staff && canEditItem(item);
              const reopenable = !!staff && item.kind === "quiz" && item.quiz.canManage && item.quiz.status === "RETIRED";
              return (
                <div key={itemKey(item)} className="group/item relative">
                  <Link href={itemHref(item, scope)} title={itemTitle(item)} aria-current={active ? "page" : undefined}
                    className={cx(
                      "group relative flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-[13px] font-semibold whitespace-nowrap transition-colors duration-200",
                      editable && "group-hover/item:pr-16", reopenable && "group-hover/item:pr-10",
                      active ? "bg-surface text-ink shadow-[0_1px_2px_rgb(60_38_14/0.08)]" : "text-body hover:bg-surface/60 hover:text-ink",
                    )}>
                    {active && <span aria-hidden className="absolute top-2 bottom-2 -left-3 w-[3px] animate-fade rounded-r bg-primary" />}
                    <Icon name={itemIcon(item)} size={17} filled={active}
                      className={cx("shrink-0 transition-colors", active ? "text-primary-ink" : "text-muted group-hover:text-ink")} />
                    <span className={cx("truncate", FADE_LABEL)}>{itemTitle(item)}</span>
                  </Link>
                  {reopenable && <span className="absolute top-1/2 right-1.5 hidden -translate-y-1/2 items-center group-hover/item:flex group-focus-within/item:flex">
                    <ItemTool icon="lock_open" label={`Mở lại ${itemTitle(item)}`}
                      onClick={() => void reopen(item.quiz.quizId, scope).then(() => { showToast("Đã mở lại quiz"); notifyOutlineChanged(); }, () => showToast("Không mở lại được quiz", "danger"))} />
                  </span>}
                  {editable && <span className="absolute top-1/2 right-1.5 hidden -translate-y-1/2 items-center gap-0.5 group-hover/item:flex group-focus-within/item:flex">
                    <ItemTool icon="edit" label={`Sửa ${itemTitle(item)}`} onClick={() => setDialog({ type: "item", module, item })} />
                    <ItemTool icon={item.kind === "quiz" ? "archive" : "trash"} danger label={`${item.kind === "quiz" ? "Lưu trữ" : "Xóa"} ${itemTitle(item)}`}
                      onClick={() => setDialog({ type: "archive", item, active })} />
                  </span>}
                </div>
              );
            })}
          </Fragment>
        ))}
      </nav>
      {dialog?.type === "item" && staff && <ModuleItemDialog scope={staff} moduleId={dialog.module.id} item={dialog.item}
        onClose={() => setDialog(null)} onSaved={(message) => { setDialog(null); showToast(message); notifyOutlineChanged(); }} />}
      {dialog?.type === "archive" && <ArchiveItemDialog item={dialog.item.kind === "material" ? { material: dialog.item.material } : { quiz: dialog.item.quiz }}
        classId={classId} onClose={() => setDialog(null)} onSaved={() => {
          const removed = dialog.item.kind === "material";
          setDialog(null);
          showToast(removed ? "Đã xóa học liệu" : "Đã lưu trữ quiz", "danger");
          notifyOutlineChanged();
          if (removed && dialog.active && scope) router.push(boardHref(scope));
        }} />}
    </div>
  );
}

function ItemTool({ icon, label, danger = false, success = false, onClick }: { icon: IconName; label: string; danger?: boolean; success?: boolean; onClick: () => void }) {
  return <button type="button" aria-label={label} title={label} onClick={onClick}
    className={cx(TOOL, "bg-shell", danger && "text-danger hover:bg-danger-soft hover:text-danger", success && "text-success hover:bg-success-soft hover:text-success")}>
    <Icon name={icon} size={14} />
  </button>;
}
