"use client";

import { useSyncExternalStore } from "react";
import { blockText, documentTree, type DocSection, type DocumentBlock } from "./types";

/**
 * Mục lục của một bài trên sidebar (dùng chung cho trang soạn, xem trước của giảng viên và trang làm bài, lịch sử nộp
 * của sinh viên — yêu cầu 2026-11-05): trang đăng dàn ý, sidebar vẽ; bấm một mục thì trình soạn chuyển tới mục đó.
 */
/** Một dòng trong sidebar của bài: một mục (tầng {@code depth}, 1 là ngoài cùng), kèm số thứ tự như trong trang (1., 1.1.). */
export type OutlineEntry = { id: string; depth: number; title: string; number: string };
/** {@code editable}: người soạn đang sửa được bài, sidebar có nút "+" thêm mục. */
export type AssignmentOutline = { assignmentId: string; backHref: string; backLabel: string; entries: OutlineEntry[]; editable?: boolean };

let current: AssignmentOutline | null = null;
const listeners = new Set<() => void>();

/** Trang soạn bài đăng dàn ý (đổi theo từng lần sửa) để sidebar của bài vẽ; null khi rời trang. */
export function publishOutline(next: AssignmentOutline | null) {
  current = next;
  listeners.forEach((listener) => listener());
}

export function useAssignmentOutline(): AssignmentOutline | null {
  return useSyncExternalStore(
    (listener) => { listeners.add(listener); return () => { listeners.delete(listener); }; },
    () => current,
    () => null,
  );
}

/** Id của dòng "văn bản ngoài mọi mục" trong dàn ý (trùng key của phần gốc trong trình soạn). */
export const DOCUMENT_ENTRY = "DOCUMENT";

/**
 * Dàn ý từ khung đề theo cây mục, đánh số như trong trang: 1., 1.1., 1.1.1.; đoạn văn trong mục không ghi. Văn bản ở ngoài
 * mọi mục (yêu cầu 2026-11-04) là một dòng tầng 0, ghi mấy chữ đầu, không số.
 */
export function outlineOf(blocks: DocumentBlock[]): OutlineEntry[] {
  const entries: OutlineEntry[] = [];
  const tree = documentTree({ blocks });
  const rootText = [...tree.blocks, ...tree.after].map((block) => blockText(block).trim()).find(Boolean);
  const rootEntry: OutlineEntry | null = tree.blocks.length > 0 || tree.after.length > 0
    ? { id: DOCUMENT_ENTRY, depth: 0, title: rootText ?? "", number: "" } : null;
  const walk = (section: DocSection, prefix: string, depth: number) => section.children.forEach((child, i) => {
    const number = `${prefix}${i + 1}.`;
    const heading = child.heading;
    if (heading?.type === "HEADING") entries.push({ id: heading.id, depth, title: heading.content.text.trim(), number });
    walk(child, number, depth + 1);
  });
  walk(tree, "", 1);
  if (!rootEntry) return entries;
  return tree.blocks.length > 0 ? [rootEntry, ...entries] : [...entries, rootEntry];
}

/** Sidebar bấm "+": trình soạn thêm mục con vào cuối mục {@code parentId} (null: mục ngoài cùng). */
export function addPart(parentId: string | null) {
  window.dispatchEvent(new CustomEvent("document:add-part", { detail: { parentId } }));
}

/** Sidebar bấm một phần: trình soạn chuyển tới phần đó (chế độ Từng phần thì đổi phần) và cuộn tới. */
export function showPart(headingId: string) {
  window.dispatchEvent(new CustomEvent("document:show-part", { detail: { headingId } }));
}
