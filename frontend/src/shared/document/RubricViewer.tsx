import type { ReactNode } from "react";
import { Pill, TableFrame, Td, Th, cx } from "@/shared/ui";
import { splitAtomicItems, sumRubricTotal, type RubricCriterion } from "./types";
import { msg } from "@/i18n/msg";

export function formatPoints(points: number | null | undefined): string {
  if (points === null || points === undefined) return "–";
  return Number(points).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

/** Bảng rubric chỉ đọc (dành cho giảng viên): mỗi dòng một mục chấm atomic kèm điểm; tổng tự tính. */
export function RubricViewer({ title, criteria, total }: { title?: string; criteria: RubricCriterion[]; total: number }) {
  const items = splitAtomicItems(criteria);
  if (items.length === 0) return <p className="text-xs text-muted">{title ? `${title}: ` : ""}{msg("shared.document.RubricViewer.noRubricYet")}</p>;
  return (
    <div className="flex flex-col gap-1.5">
      {title && <p className="text-[13px] font-semibold text-ink">{title}</p>}
      <TableFrame className="[&_table]:table-fixed">
        <colgroup><col /><col className="w-[100px]" /></colgroup><thead>
          <tr><Th>{msg("shared.document.RubricViewer.criterion")}</Th><Th className="text-right">{msg("shared.document.RubricViewer.points")}</Th></tr>
        </thead>
        <tbody>
          {items.map((item, i) => (
            <tr key={item.itemId ?? i}>
              <Td>{item.text || msg("shared.document.RubricViewer.noContentYet")}</Td>
              <Td className="text-right">{formatPoints(item.points)}</Td>
            </tr>
          ))}
          <tr>
            <Td className="font-semibold">{msg("shared.document.RubricViewer.total")}</Td>
            <Td className="text-right font-semibold">{formatPoints(total)}</Td>
          </tr>
        </tbody>
      </TableFrame>
    </div>
  );
}

/**
 * Thẻ rubric bên lề của một mục (kiểu comment Google Docs): tên mục, tổng điểm, các mục chấm atomic. Thẻ chưa chọn ghi mỗi
 * mục một dòng (dài thì cắt); bấm vào thẻ thì hiện đủ nội dung từng mục, hay trình sửa {@code editor} (cũng hiện đủ chữ) khi
 * người soạn sửa được (yêu cầu 2026-11-05). {@code total}: điểm ghi trên thẻ thay cho tổng của chính thẻ (thẻ Toàn bài ghi
 * tổng điểm cả bài).
 */
export function RubricNote({ title, criteria, active = false, onActivate, issues = [], badge, editor, editOnActive = false, total }: {
  title: string;
  criteria: RubricCriterion[];
  active?: boolean;
  onActivate?: () => void;
  issues?: string[];
  badge?: ReactNode;
  editor?: ReactNode;
  /** Chỉ hiện trình sửa khi thẻ đang chọn (trang soạn bài); không thì trình sửa luôn hiện (bảng chấm). */
  editOnActive?: boolean;
  total?: number;
}) {
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
          {/* Chỉ tên mục (dòng cuối của đường dẫn), không ghi các mục cha phía trên (yêu cầu 2026-11-04). */}
          <p className={cx("text-[13px] font-semibold text-ink", !active && "truncate")} title={title}>{name}</p>
        </div>
        {badge}
        {/* Mục chưa có mục chấm không phải phần chấm: không ghi "0 điểm"; ghi dở cũng không tô vàng, chỉ báo khi xếp lịch
            (yêu cầu 2026-11-05). */}
        {(total !== undefined || criteria.length > 0) && (
          <Pill>{total !== undefined ? msg("shared.document.RubricViewer.totalPoints", { value: String(formatPoints(total)) }) : msg("shared.document.RubricViewer.points2", { value: String(formatPoints(sumRubricTotal(criteria))) })}</Pill>
        )}
      </div>
      {editor && (active || !editOnActive) ? editor : <RubricLines criteria={criteria} expanded={active} editable={!!editor} />}
      {issues.map((issue, i) => <p key={i} role="alert" className="text-xs text-danger">{issue}</p>)}
    </section>
  );
}

function RubricLines({ criteria, expanded, editable = false }: { criteria: RubricCriterion[]; expanded: boolean; editable?: boolean }) {
  const items = splitAtomicItems(criteria);
  // Người soạn sửa được: chỉ cách thêm mục chấm cho người mới (yêu cầu 2026-11-10).
  if (items.length === 0) return <p className="text-xs text-muted">{editable ? msg("shared.document.RubricViewer.noCriteriaYetClickThe") : msg("shared.document.RubricViewer.noCriteriaYet")}</p>;
  return (
    <ul className="flex flex-col gap-1 text-xs text-body">
      {items.map((item, i) => (
        <li key={item.itemId ?? i} className="flex gap-2">
          <span aria-hidden className="text-faint">☐</span>
          <span className={cx("min-w-0 flex-1", !expanded && "truncate")}>{item.text || msg("shared.document.RubricViewer.noContentYet")}</span>
          <span className="shrink-0 text-muted">{formatPoints(item.points)}</span>
        </li>
      ))}
    </ul>
  );
}
