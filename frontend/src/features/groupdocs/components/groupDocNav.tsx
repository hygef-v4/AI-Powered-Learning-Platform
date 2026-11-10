"use client";

import type { NavContext, NavLocation } from "@/shared/layout/navigation";
import { AssignmentNav } from "@/features/assignments/components/assignmentNav";

/**
 * Trang tài liệu nhóm (`/group-docs/{id}`): sidebar là mục lục của bài như trang làm bài cá nhân (lối về Bài tập, các mục
 * đánh số 1., 1.1.), không có nút "+" vì cấu trúc mục theo đề của giảng viên (BR-U12-30).
 */
export function groupDocContext({ pathname }: NavLocation): NavContext | null {
  const match = /^\/group-docs\/([^/]+)$/.exec(pathname);
  if (!match) return null;
  return { key: `group-doc:${match[1]}`, label: "Các phần của bài", header: <AssignmentNav />, items: [] };
}
