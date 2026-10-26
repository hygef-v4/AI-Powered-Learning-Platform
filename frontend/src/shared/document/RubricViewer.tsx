import type { ReactNode } from "react";
import { Pill, TableFrame, Td, Th, cx } from "@/shared/ui";
import { atomicItems, rubricComplete, rubricTotal, type RubricCriterion } from "./types";

export function formatPoints(points: number | null | undefined): string {
  if (points === null || points === undefined) return "–";
  return Number(points).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

/** Bảng rubric chỉ đọc (dành cho giảng viên): mỗi dòng một mục chấm atomic kèm điểm; tổng tự tính. */
export function RubricViewer({ title, criteria, total }: { title?: string; criteria: RubricCriterion[]; total: number }) {
  const items = atomicItems(criteria);
  if (items.length === 0) return <p className="text-xs text-muted">{title ? `${title}: ` : ""}Chưa có rubric.</p>;
  return (
    <div className="flex flex-col gap-1.5">
      {title && <p className="text-[13px] font-semibold text-ink">{title}</p>}
      <TableFrame>
        <thead>
          <tr><Th>Mục chấm</Th><Th className="text-right">Điểm</Th></tr>
        </thead>
        <tbody>
          {items.map((item, i) => (
            <tr key={item.itemId ?? i}>
              <Td>{item.text || "(Chưa có nội dung)"}</Td>
              <Td className="text-right">{formatPoints(item.points)}</Td>
            </tr>
          ))}
          <tr>
            <Td className="font-semibold">Tổng</Td>
            <Td className="text-right font-semibold">{formatPoints(total)}</Td>
          </tr>
        </tbody>
      </TableFrame>
    </div>
  );
}

/**
 * Thẻ rubric bên lề của một mục (kiểu comment Google Docs): tên mục, tổng điểm, các mục chấm atomic; thẻ đang chọn
 * hiện đủ nội dung từng mục, hoặc hiện trình sửa {@code editor} khi người soạn sửa được.
 */
export function RubricNote({ title, criteria, active = false, onActivate, issues = [], badge, editor }: {
  title: string;
  criteria: RubricCriterion[];
  active?: boolean;
  onActivate?: () => void;
  issues?: string[];
  badge?: ReactNode;
  editor?: ReactNode;
}) {
  const complete = rubricComplete(criteria);
  const path = title.split(" › ");
  const name = path.at(-1) ?? title;
  return (
    <section aria-label={`Rubric: ${name}`} data-testid="rubric-note" data-active={active || undefined}
      onClick={onActivate} onFocusCapture={onActivate}
      className={cx("flex flex-col gap-2 rounded-xl border bg-surface p-3 shadow-sm transition-shadow",
        issues.length > 0 ? "border-danger" : active ? "border-primary shadow-[var(--shadow-float)]" : "border-line",
        !active && onActivate && "cursor-pointer hover:shadow-[var(--shadow-float)]")}>
      <div className="flex items-start gap-2">
        <div className="min-w-0 flex-1">
          {path.length > 1 && <p className="truncate text-[10.5px] text-faint">{path.slice(0, -1).join(" › ")}</p>}
          <p className={cx("text-[13px] font-semibold text-ink", !active && "truncate")} title={title}>{name}</p>
        </div>
        {badge}
        <Pill tone={complete ? "neutral" : "warning"}>{formatPoints(rubricTotal(criteria))} điểm</Pill>
      </div>
      {editor ?? <RubricLines criteria={criteria} expanded={active} />}
      {issues.map((issue, i) => <p key={i} role="alert" className="text-[11px] text-danger">{issue}</p>)}
    </section>
  );
}

function RubricLines({ criteria, expanded }: { criteria: RubricCriterion[]; expanded: boolean }) {
  const items = atomicItems(criteria);
  if (items.length === 0) return <p className="text-xs text-muted">Chưa có mục chấm.</p>;
  return (
    <ul className="flex flex-col gap-1 text-xs text-body">
      {items.map((item, i) => (
        <li key={item.itemId ?? i} className="flex gap-2">
          <span aria-hidden className="text-faint">☐</span>
          <span className={cx("min-w-0 flex-1", !expanded && "truncate")}>{item.text || "(Chưa có nội dung)"}</span>
          <span className="shrink-0 text-muted">{formatPoints(item.points)}</span>
        </li>
      ))}
    </ul>
  );
}
