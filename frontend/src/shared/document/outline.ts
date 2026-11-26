"use client";

import { useSyncExternalStore } from "react";
import { blockText, buildDocumentTree, formatSectionNumber, segmentKey, type DocSection, type DocumentBlock } from "./types";

/**
 * Mục lục của một bài trên sidebar (dùng chung cho trang soạn, xem trước của giảng viên và trang làm bài, lịch sử nộp
 * của sinh viên — yêu cầu 2026-11-05): trang đăng dàn ý, sidebar vẽ; bấm một mục thì trình soạn chuyển tới mục đó.
 */
/**
 * Một dòng trong sidebar của bài: một mục (tầng {@code depth}, 1 là ngoài cùng), kèm số thứ tự như trong trang (I., I.1.).
 * {@code loose}: đoạn văn không có tiêu đề (văn bản mở đầu, hoặc chữ nằm ngay sau một mục), ghi mấy chữ đầu, không số.
 */
export type OutlineEntry = { id: string; depth: number; title: string; number: string; loose?: boolean };
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
 * Dàn ý từ khung đề theo cây mục, đánh số như trong trang: I., I.1., I.1.a., I.1.a.aa.; đoạn văn mở đầu trong mục không ghi.
 * Văn bản mở đầu ở ngoài mọi mục (yêu cầu 2026-11-04) là một dòng tầng 0, ghi mấy chữ đầu, không số. Chữ nằm ngay sau một
 * mục là một dòng riêng ngay dưới nhánh của mục đó, cùng tầng (yêu cầu 2026-11-27: không gộp với văn bản mở đầu).
 */
export function buildOutline(blocks: DocumentBlock[]): OutlineEntry[] {
  const entries: OutlineEntry[] = [];
  const tree = buildDocumentTree({ blocks });
  const firstText = (list: DocumentBlock[]) => list.map((block) => blockText(block).trim()).find(Boolean) ?? "";
  if (tree.blocks.length > 0) entries.push({ id: DOCUMENT_ENTRY, depth: 0, title: firstText(tree.blocks), number: "", loose: true });
  const walk = (section: DocSection, path: number[]) => section.children.forEach((child, i) => {
    const counters = [...path, i + 1];
    const heading = child.heading;
    if (heading?.type === "HEADING") {
      entries.push({ id: heading.id, depth: counters.length, title: heading.content.text.trim(), number: formatSectionNumber(counters) });
    }
    walk(child, counters);
    if (child.trailing.length > 0) {
      entries.push({ id: segmentKey(child), depth: section.heading ? counters.length : 0, title: firstText(child.trailing), number: "", loose: true });
    }
  });
  walk(tree, []);
  return entries;
}

/** Sidebar bấm "+": trình soạn thêm mục con vào cuối mục {@code parentId} (null: mục ngoài cùng). */
export function addPart(parentId: string | null) {
  window.dispatchEvent(new CustomEvent("document:add-part", { detail: { parentId } }));
}

/** Sidebar bấm một phần: trình soạn chuyển tới phần đó (chế độ Từng phần thì đổi phần) và cuộn tới. */
export function showPart(headingId: string) {
  window.dispatchEvent(new CustomEvent("document:show-part", { detail: { headingId } }));
}
