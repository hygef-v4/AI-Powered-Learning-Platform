"use client";

import { useEffect } from "react";
import type { IconName } from "@/shared/ui/Icon";
import { outlinePaths } from "../api";
import { useMaterialResource } from "../hooks/useMaterialResource";
import { materialHref, type Material, type MaterialModule, type ModuleQuiz, type ModuleScope } from "./types";

const enc = encodeURIComponent;

const CHANGED = "module-outline-changed";

/** Báo học liệu, Quiz trong module vừa đổi (sửa ở sidebar hay ở trang): danh sách module và trang đang mở tải lại. */
export function notifyOutlineChanged() {
  window.dispatchEvent(new Event(CHANGED));
}

/** Gọi `reload` mỗi khi học liệu, Quiz trong module đổi ở nơi khác trên màn hình. */
export function useOutlineChanged(reload: () => void) {
  useEffect(() => {
    window.addEventListener(CHANGED, reload);
    return () => window.removeEventListener(CHANGED, reload);
  }, [reload]);
}

export type OutlineItem = { kind: "material"; material: Material } | { kind: "quiz"; quiz: ModuleQuiz };
export type OutlineModule = { module: MaterialModule; items: OutlineItem[] };

function pendingText(materials: Material[]) {
  return materials.some((m) => m.status === "ACTIVE" && m.current.textStatus === "PENDING");
}

/** Module của môn kèm học liệu đang dùng (theo thứ tự) và Quiz gắn trong từng module. */
export function useModuleOutline(scope: ModuleScope | null, options: { poll?: boolean; pollPaused?: boolean } = {}) {
  const paths = scope ? outlinePaths(scope) : null;
  const modules = useMaterialResource<MaterialModule[]>(paths?.modules ?? null);
  const materials = useMaterialResource<Material[]>(paths?.materials ?? null, {
    pollWhile: options.poll ? pendingText : undefined,
    pollPaused: options.pollPaused,
  });
  const outline: OutlineModule[] | undefined = modules.data && materials.data
    ? modules.data.map((module) => ({
      module,
      items: [
        ...materials.data!.filter((m) => m.status === "ACTIVE" && m.moduleId === module.id)
          .sort((a, b) => a.orderNo - b.orderNo || a.title.localeCompare(b.title, "vi"))
          .map((material) => ({ kind: "material" as const, material })),
        ...module.quizzes.map((quiz) => ({ kind: "quiz" as const, quiz })),
      ],
    }))
    : undefined;
  const reloadModules = modules.reload, reloadMaterials = materials.reload;
  useEffect(() => {
    const reload = () => { reloadModules(); reloadMaterials(); };
    window.addEventListener(CHANGED, reload);
    return () => window.removeEventListener(CHANGED, reload);
  }, [reloadModules, reloadMaterials]);
  return {
    outline,
    error: modules.error ?? materials.error,
    loading: modules.loading || materials.loading,
    reload: () => { modules.reload(); materials.reload(); },
  };
}

export function itemKey(item: OutlineItem) {
  return item.kind === "material" ? `m:${item.material.id}` : `q:${item.quiz.quizId}`;
}

export function itemTitle(item: OutlineItem) {
  return item.kind === "material" ? item.material.title : item.quiz.title;
}

export function itemIcon(item: OutlineItem): IconName {
  if (item.kind === "quiz") return "quiz";
  if (item.material.kind === "YOUTUBE_LINK") return "link";
  return "description";
}

/** Trang mở khi bấm một mục: học liệu mở trang xem; Quiz mở trang soạn (môn, lớp dạy) hay trang làm Quiz (sinh viên). */
export function itemHref(item: OutlineItem, scope: ModuleScope) {
  const classId = scope.kind === "subject" ? undefined : scope.classId;
  if (item.kind === "material") return materialHref(item.material.id, classId, true);
  const id = enc(item.quiz.quizId);
  if (scope.kind === "student") return `/quiz-practice/${id}?classId=${enc(scope.classId)}`;
  return scope.kind === "class" ? `/quizzes/${id}?classId=${enc(scope.classId)}` : `/quizzes/${id}`;
}

/** Trang danh sách học liệu của nơi xem (lối về trên sidebar module). */
export function boardHref(scope: ModuleScope) {
  if (scope.kind === "subject") return `/manager/materials?subjectId=${enc(scope.subjectId)}`;
  return scope.kind === "class" ? `/teaching/classes/${enc(scope.classId)}?tab=materials` : `/classes/${enc(scope.classId)}?tab=materials`;
}
