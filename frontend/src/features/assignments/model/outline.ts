"use client";

export {
  DOCUMENT_ENTRY,
  addPart,
  outlineOf,
  publishOutline,
  showPart,
  useAssignmentOutline,
  type AssignmentOutline,
  type OutlineEntry,
} from "@/shared/document/outline";

const RETURN_KEY = "assignments:return";

/** Trang danh sách bài tập mở một bài: nhớ đúng tab, bộ lọc đang mở để lối về sidebar quay lại đó (yêu cầu 2026-11-04). */
export function rememberAssignmentsPage() {
  try { window.sessionStorage.setItem(RETURN_KEY, `${window.location.pathname}${window.location.search}`); } catch { /* bỏ qua */ }
}

/** Trang danh sách đã nhớ, nếu cùng trang với lối về mặc định ({@code fallback}, không tính tham số). */
export function assignmentsReturn(fallback: string): string {
  try {
    const saved = window.sessionStorage.getItem(RETURN_KEY);
    if (saved && saved.split("?")[0] === fallback.split("?")[0]) return saved;
  } catch { /* bỏ qua */ }
  return fallback;
}

