"use client";

import type { NavContext, NavLocation } from "@/shared/layout/navigation";
import { AssignmentNav } from "@/features/assignments/components/AssignmentNav";
import { msg } from "@/i18n/msg";

/**
 * Trang tài liệu nhóm (`/group-docs/{id}`) và trang soạn (`/group-docs/{id}/edit`): sidebar là mục lục của bài như trang làm bài cá nhân (lối về Bài tập, các mục
 * đánh số 1., 1.1.), không có nút "+" vì cấu trúc mục theo đề của giảng viên (BR-U12-30).
 */
export function groupDocContext({ pathname }: NavLocation): NavContext | null {
  const match = /^\/group-docs\/([^/]+)(?:\/edit)?$/.exec(pathname);
  if (!match) return null;
  return { key: `group-doc:${match[1]}`, label: msg("groupdocs.components.groupDocNav.cacPhanCuaBai"), header: <AssignmentNav />, items: [] };
}
