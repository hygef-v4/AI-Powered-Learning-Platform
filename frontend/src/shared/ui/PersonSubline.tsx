import { cx } from "./cx";

/**
 * Dòng phụ dưới tên người: `SE123456 · email` (frontend-components U01 mục 6). Không có MSSV thì chỉ hiện email; không có
 * cả hai thì không hiện gì.
 */
export function PersonSubline({
  studentCode,
  email,
  className,
}: {
  studentCode?: string | null;
  email?: string | null;
  className?: string;
}) {
  const parts = [studentCode, email].filter((part): part is string => !!part);
  if (parts.length === 0) return null;
  return (
    <span className={cx("block break-all text-xs text-muted", className)} data-testid="person-subline">
      {studentCode && <span className="font-semibold text-body">{studentCode}</span>}
      {studentCode && email && " · "}
      {email}
    </span>
  );
}
