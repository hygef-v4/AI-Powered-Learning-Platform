"use client";
import Link from "next/link";
import { gradebookHref } from "./ClassProgressSection";

/**
 * Nút Bài nộp trên bảng Bài tập của lớp, chỉ ở bài cá nhân và bài nhóm (bài luyện tập không chấm nên không có): sang trang
 * Chấm điểm mở sẵn bài đó. Bấm nút không kích hoạt bấm dòng.
 */
export function SubmissionsLink({ assignmentId, classId, practice }: { assignmentId: string; classId?: string; practice?: boolean }) {
  if (!classId || practice) return null;
  return (
    <Link href={gradebookHref(classId, { assignment: assignmentId })} onClick={(event) => event.stopPropagation()}
      className="inline-flex h-8 items-center justify-center rounded-lg border border-line-strong bg-surface px-2.5 text-xs font-semibold whitespace-nowrap text-ink transition-colors hover:border-primary hover:bg-primary-soft hover:text-primary-ink">
      Bài nộp
    </Link>
  );
}
