"use client";
import Link from "next/link";
import { useSession } from "@/features/identity/session";
import { Icon } from "@/shared/ui";
import { itemHref, itemKey, itemTitle, useModuleOutline, type OutlineItem } from "./outline";
import type { ModuleScope } from "./types";

/** Nơi xem: lớp (sinh viên, giảng viên) theo `classId`, không có thì là môn (Chủ nhiệm môn) theo `subjectId`. */
export function useModuleScope(subjectId: string | undefined, classId?: string | null): ModuleScope | null {
  const session = useSession();
  if (session.status !== "authenticated") return null;
  if (classId) return session.profile.role === "STUDENT" ? { kind: "student", classId } : { kind: "class", classId };
  return subjectId ? { kind: "subject", subjectId } : null;
}

/** Module chứa mục đang xem (học liệu hay Quiz) cùng mục trước, sau trong danh sách module. */
export function useModulePlace(scope: ModuleScope | null, match: (item: OutlineItem) => boolean) {
  const { outline, reload } = useModuleOutline(scope);
  const home = outline?.find((entry) => entry.items.some(match));
  const flat = outline?.flatMap((entry) => entry.items) ?? [];
  const index = flat.findIndex(match);
  return {
    moduleTitle: home?.module.title,
    moduleId: home?.module.id,
    previous: index > 0 ? flat[index - 1] : undefined,
    next: index >= 0 ? flat[index + 1] : undefined,
    reload,
  };
}

/** Chuyển sang mục trước, sau trong danh sách module (chỉ tên mục và mũi tên); đặt ở cả đầu và cuối trang. */
export function ModuleNeighbours({ scope, previous, next, position = "bottom" }: { scope: ModuleScope | null; previous?: OutlineItem; next?: OutlineItem; position?: "top" | "bottom" }) {
  if (!scope || (!previous && !next)) return null;
  return <nav aria-label={position === "top" ? "Học liệu trước và sau (đầu trang)" : "Học liệu trước và sau"}
    className={`grid gap-3 sm:grid-cols-2 ${position === "bottom" ? "border-t border-line pt-4" : ""}`}>
    {previous ? <Neighbour item={previous} scope={scope} after={false} /> : <span />}
    {next && <Neighbour item={next} scope={scope} after />}
  </nav>;
}

function Neighbour({ item, scope, after }: { item: OutlineItem; scope: ModuleScope; after: boolean }) {
  return <Link href={itemHref(item, scope)} data-testid={`material-${after ? "next" : "previous"}-${itemKey(item)}`} aria-label={`${after ? "Sau" : "Trước"}: ${itemTitle(item)}`}
    className={`group flex items-center gap-3 rounded-xl border border-line bg-surface px-4 py-3 transition-colors hover:border-line-strong ${after ? "sm:col-start-2 sm:justify-end sm:text-right" : ""}`}>
    {!after && <Icon name="chevron_left" size={18} className="shrink-0 text-muted group-hover:text-ink" />}
    <span className="min-w-0 truncate text-[13.5px] font-semibold text-ink">{itemTitle(item)}</span>
    {after && <Icon name="chevron_right" size={18} className="shrink-0 text-muted group-hover:text-ink" />}
  </Link>;
}
