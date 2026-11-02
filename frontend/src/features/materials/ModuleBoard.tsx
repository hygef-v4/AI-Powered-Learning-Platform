"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useId, useState, type ReactNode } from "react";
import { getQuiz, reopenQuiz } from "@/features/quizzes/api";
import { QUIZ_STATUS_LABELS, QUIZ_STATUS_TONES } from "@/features/quizzes/labels";
import { Button, Dialog, Field, Icon, IconButton, Menu, MenuItem, PageHeader, Pill, TextInput, cx, showToast } from "@/shared/ui";
import { ArchiveItemDialog, ModuleItemDialog } from "./MaterialDialogs";
import { materialError, moduleApi } from "./api";
import { useModuleCollapse } from "./moduleCollapse";
import { itemHref, itemIcon, itemKey, itemTitle, useModuleOutline, type OutlineItem } from "./outline";
import type { MaterialModule, ModuleScope } from "./types";

type Dialogs =
  | { type: "item"; module: MaterialModule; item?: OutlineItem }
  | { type: "archive"; item: OutlineItem }
  | { type: "rename" | "delete"; module: MaterialModule }
  | { type: "create" };

/** Người xem sửa, lưu trữ được mục này: học liệu của môn chỉ Chủ nhiệm môn, của lớp chỉ giảng viên lớp đó. */
export function canEditItem(item: OutlineItem) {
  return item.kind === "material" ? item.material.canManage : item.quiz.canManage && item.quiz.status !== "RETIRED";
}

/**
 * Học liệu chia theo module (U13). Mỗi module một thẻ: học liệu (tệp, link YouTube) và Quiz theo thứ tự, mỗi dòng chỉ có
 * tên, bấm cả dòng mở trang chi tiết. Bấm đầu thẻ (mũi tên cuối hàng) để thu gọn / mở module. Chủ nhiệm môn thêm, đổi tên, xóa, sắp xếp module; Chủ nhiệm môn và giảng viên tạo,
 * sửa, lưu trữ học liệu của mình trong module (giảng viên chỉ xem học liệu và Quiz của môn).
 */
export function ModuleBoard({ scope, readOnly = false, header = false }: { scope: ModuleScope; readOnly?: boolean; header?: boolean }) {
  const [dialog, setDialog] = useState<Dialogs | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const data = useModuleOutline(scope, { poll: true, pollPaused: busy || dialog !== null });
  const { collapsed, toggle } = useModuleCollapse("board");
  const outline = data.outline ?? [];
  const staff = scope.kind !== "student";
  const writable = staff && !readOnly;
  const canCreate = scope.kind === "subject" && !readOnly && !!data.outline;

  async function run(action: () => Promise<unknown>, message?: string, tone: "success" | "danger" = "success") {
    if (busy) return;
    setBusy(true); setError("");
    try { await action(); if (message) showToast(message, tone); setDialog(null); data.reload(); }
    catch (e) { setError(materialError(e)); }
    finally { setBusy(false); }
  }
  function move(index: number, offset: number) {
    if (scope.kind !== "subject") return;
    const order = outline.map((m) => m.module);
    const [moved] = order.splice(index, 1);
    order.splice(index + offset, 0, moved!);
    void run(() => moduleApi.reorder(scope.subjectId, order));
  }
  const closed = () => { setDialog(null); setError(""); };
  const saved = (message: string, tone: "success" | "danger" = "success") => { setDialog(null); showToast(message, tone); data.reload(); };
  const createButton = <Button variant="primary" icon="add" onClick={() => setDialog({ type: "create" })} data-testid="module-create">Thêm module</Button>;

  return <div className="flex flex-col gap-4">
    {header && <PageHeader title="Học liệu" actions={canCreate && createButton} />}
    {!header && canCreate && <div className="flex justify-end">{createButton}</div>}
    {data.error && <p role="alert" className="text-sm text-danger">{data.error} <Button size="sm" onClick={data.reload}>Tải lại</Button></p>}
    {error && !dialog && <p role="alert" className="text-sm text-danger">{error}</p>}
    {!data.outline && !data.error && <p role="status" className="py-10 text-center text-sm text-muted">Đang tải học liệu…</p>}
    {data.outline && outline.length === 0 && <section className="rounded-xl border border-dashed border-line-strong bg-surface p-10 text-center text-sm text-muted">Chưa có module</section>}
    {outline.map(({ module, items }, index) => {
      const addable = writable && (scope.kind === "class" || module.canManage);
      const open = !collapsed.has(module.id);
      return <section key={module.id} aria-label={module.title} data-testid={`module-${module.id}`} className="flex flex-col rounded-xl border border-line bg-surface">
        {/* Cả hàng đầu bấm được để thu gọn / mở; các nút thao tác không làm đổi trạng thái. */}
        <div className="flex cursor-pointer flex-wrap items-center gap-2 py-3.5 pr-3 pl-5 select-none" onClick={() => toggle(module.id)}>
          <h2 className="min-w-0 flex-1 text-[15px] font-semibold text-ink [overflow-wrap:anywhere]">{module.title}</h2>
          <span className="contents" onClick={(event) => event.stopPropagation()}>
          {addable && <Button size="sm" variant="primary" icon="add" onClick={() => setDialog({ type: "item", module })} data-testid={`module-add-${module.id}`}>Tạo học liệu</Button>}
          {scope.kind === "subject" && module.canManage && !readOnly && <Menu label={null} icon="more_horiz" ariaLabel={`Thao tác module ${module.title}`} variant="tool" align="right" summaryClassName="[&>svg:last-child]:hidden">
            <MenuItem icon="edit" onClick={() => setDialog({ type: "rename", module })}>Đổi tên</MenuItem>
            <MenuItem icon="chevron_left" className="[&>svg]:rotate-90" disabled={busy || index === 0} onClick={() => move(index, -1)}>Lên trên</MenuItem>
            <MenuItem icon="chevron_right" className="[&>svg]:rotate-90" disabled={busy || index === outline.length - 1} onClick={() => move(index, 1)}>Xuống dưới</MenuItem>
            <MenuItem icon="trash" tone="danger" onClick={() => setDialog({ type: "delete", module })}>Xóa module</MenuItem>
          </Menu>}
          </span>
          <button type="button" aria-expanded={open} aria-controls={`module-body-${module.id}`} data-testid={`module-toggle-${module.id}`}
            aria-label={open ? `Thu gọn ${module.title}` : `Mở ${module.title}`} title={open ? "Thu gọn" : "Mở"}
            className="flex size-8 shrink-0 items-center justify-center rounded-lg text-muted transition-colors hover:bg-sunken hover:text-ink">
            <Icon name="expand_more" size={18} className={cx("transition-transform duration-300 ease-[cubic-bezier(.16,1,.3,1)]", open && "rotate-180")} />
          </button>
        </div>
        <CollapseBody id={`module-body-${module.id}`} open={open}>
          <ul className="flex flex-col">
            {items.length === 0 && <li className="border-t border-line px-5 py-4 text-[13px] text-muted">Chưa có học liệu</li>}
            {items.map((item) => <ModuleRow key={itemKey(item)} item={item} scope={scope} staff={staff} editable={writable && canEditItem(item)}
              reopenable={writable && item.kind === "quiz" && item.quiz.canManage && item.quiz.status === "RETIRED"} busy={busy}
              onReopen={() => item.kind === "quiz" && void run(() => reopen(item.quiz.quizId, scope), "Đã mở lại quiz")}
              onEdit={() => setDialog({ type: "item", module, item })} onArchive={() => setDialog({ type: "archive", item })} />)}
          </ul>
        </CollapseBody>
      </section>;
    })}
    {dialog?.type === "create" && scope.kind === "subject" && <ModuleNameDialog title="Thêm module" initial="" busy={busy} error={error}
      onClose={closed} onSubmit={(name) => void run(() => moduleApi.create(scope.subjectId, name), "Đã thêm module")} />}
    {dialog?.type === "rename" && <ModuleNameDialog title="Đổi tên module" initial={dialog.module.title} busy={busy} error={error}
      onClose={closed} onSubmit={(name) => void run(() => moduleApi.rename(dialog.module, name), "Đã lưu thay đổi")} />}
    {dialog?.type === "delete" && <Dialog open title="Xóa module?" tone="danger" onClose={closed} dismissible={!busy}
      footer={<Button variant="danger" disabled={busy} onClick={() => void run(() => moduleApi.remove(dialog.module), "Đã xóa module", "danger")} data-testid="module-delete-confirm">Xóa</Button>}>
      <p>Bạn có chắc muốn xóa module {dialog.module.title} không?</p>
      {error && <p role="alert" className="text-sm text-danger">{error}</p>}
    </Dialog>}
    {dialog?.type === "item" && scope.kind !== "student" &&
      <ModuleItemDialog key={dialog.item ? itemKey(dialog.item) : dialog.module.id} scope={scope} moduleId={dialog.module.id} item={dialog.item}
        onClose={closed} onSaved={(message) => saved(message)} />}
    {dialog?.type === "archive" && <ArchiveItemDialog item={dialog.item.kind === "material" ? { material: dialog.item.material } : { quiz: dialog.item.quiz }}
      classId={scope.kind === "subject" ? undefined : scope.classId} onClose={closed} onSaved={() => saved(dialog.item.kind === "quiz" ? "Đã lưu trữ quiz" : "Đã xóa học liệu", "danger")} />}
  </div>;
}

/** Phần thân co giãn theo chiều cao thật (grid 0fr / 1fr); thu gọn thì không tab vào được các dòng bên trong. */
export function CollapseBody({ id, open, children }: { id: string; open: boolean; children: ReactNode }) {
  return <div id={id} inert={!open} className={cx("grid transition-[grid-template-rows,opacity] duration-300 ease-[cubic-bezier(.16,1,.3,1)] motion-reduce:transition-none",
    open ? "grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0")}>
    <div className="min-h-0 overflow-hidden">{children}</div>
  </div>;
}

/** Một dòng: biểu tượng loại, tên; người quản lý thấy trạng thái Quiz, nút Sửa, Lưu trữ; cuối dòng là mũi tên, bấm cả dòng để mở. */
/** Mở lại Quiz đã lưu trữ (lấy version hiện tại trước). */
export async function reopen(quizId: string, scope: ModuleScope) {
  const detail = await getQuiz(quizId, scope.kind === "subject" ? null : scope.classId);
  await reopenQuiz(quizId, detail.quiz.version);
}

function ModuleRow({ item, scope, staff, editable, reopenable, busy, onReopen, onEdit, onArchive }: {
  item: OutlineItem; scope: ModuleScope; staff: boolean; editable: boolean; reopenable: boolean; busy: boolean;
  onReopen: () => void; onEdit: () => void; onArchive: () => void;
}) {
  const router = useRouter();
  const href = itemHref(item, scope);
  const quiz = item.kind === "quiz" ? item.quiz : null;
  const material = item.kind === "material" ? item.material : null;
  const title = itemTitle(item);
  return <li onClick={() => router.push(href)} data-testid={`module-row-${itemKey(item)}`}
    className="group flex cursor-pointer items-center gap-3 border-t border-line px-5 py-2.5 transition-colors hover:bg-canvas">
    <span aria-hidden="true" className={cx("flex size-8 shrink-0 items-center justify-center rounded-lg",
      quiz ? "bg-accent-soft text-accent" : material?.kind === "YOUTUBE_LINK" ? "bg-danger-soft text-danger" : "bg-primary-soft text-primary-ink")}>
      <Icon name={itemIcon(item)} size={16} />
    </span>
    <Link href={href} data-testid={`module-item-${itemKey(item)}`} onClick={(event) => event.stopPropagation()}
      className="min-w-0 flex-1 truncate text-[13.5px] font-semibold text-ink group-hover:text-primary-ink">{title}</Link>
    {quiz && staff && <Pill tone={QUIZ_STATUS_TONES[quiz.status]}>{QUIZ_STATUS_LABELS[quiz.status]}</Pill>}
    {reopenable && <span className="flex shrink-0 items-center" onClick={(event) => event.stopPropagation()}>
      <IconButton icon="lock_open" label={`Mở lại ${title}`} disabled={busy} className="text-success! hover:bg-success-soft" onClick={onReopen} />
    </span>}
    {editable && <span className="flex shrink-0 items-center gap-0.5" onClick={(event) => event.stopPropagation()}>
      <IconButton icon="edit" label={`Sửa ${title}`} onClick={onEdit} />
      {quiz
        ? <IconButton icon="archive" label={`Lưu trữ ${title}`} className="text-danger! hover:bg-danger-soft" onClick={onArchive} />
        : <IconButton icon="trash" label={`Xóa ${title}`} className="text-danger! hover:bg-danger-soft" onClick={onArchive} />}
    </span>}
    <Icon name="chevron_right" size={18} className="shrink-0 text-faint group-hover:text-ink" />
  </li>;
}

function ModuleNameDialog({ title, initial, busy, error, onClose, onSubmit }: { title: string; initial: string; busy: boolean; error: string; onClose: () => void; onSubmit: (name: string) => void }) {
  const id = useId();
  const [name, setName] = useState(initial);
  const [invalid, setInvalid] = useState(false);
  function submit() {
    if (!name.trim()) { setInvalid(true); return; }
    onSubmit(name.trim());
  }
  return <Dialog open title={title} onClose={onClose} dismissible={!busy}
    footer={<Button variant="primary" disabled={busy} onClick={submit} data-testid="module-name-save">Lưu</Button>}>
    <form onSubmit={(event) => { event.preventDefault(); submit(); }} noValidate className="flex flex-col gap-3">
      <Field label="Tên module" htmlFor={`${id}-name`} required error={invalid ? "Nhập tên module." : null}>
        <TextInput id={`${id}-name`} value={name} maxLength={200} disabled={busy} invalid={invalid} autoFocus
          onChange={(event) => { setName(event.target.value); setInvalid(false); }} data-testid="module-name-input" />
      </Field>
      {error && <p role="alert" className="text-sm text-danger">{error}</p>}
    </form>
  </Dialog>;
}
