"use client";

import { useEffect, useRef } from "react";
import { atomicItems, criteriaOf, newId, type AtomicItem, type RubricCriterion } from "@/shared/document";
import { cx } from "@/shared/ui";

const FIELD = "min-w-0 rounded-md border bg-transparent px-1.5 py-1 text-xs text-ink outline-none hover:border-line focus:border-primary focus:bg-white";
const INPUT = `${FIELD} border-transparent`;
/** Ô điểm luôn có viền nhạt: không có chữ gợi ý (yêu cầu 2026-10-22) nên viền cho biết chỗ nhập điểm. */
const POINTS = `${FIELD} border-line`;
/** BR-U07-21: tối đa 20 tiêu chí mỗi phần; mỗi mục atomic là một tiêu chí. */
const LIMIT = 20;

/**
 * Rubric gửi lên server: bỏ dòng vừa thêm mà chưa gõ gì (chưa có chữ, chưa có điểm); điểm là số dương, tối đa 2 chữ số
 * thập phân; ô trống, 0 hoặc số âm là chưa có điểm.
 */
export function cleanCriteria(criteria: RubricCriterion[]): RubricCriterion[] {
  return criteria
    .map((criterion) => ({
      ...criterion,
      items: criterion.items
        .filter((item) => item.text.trim() !== "" || item.points !== null)
        .map((item) => ({
          ...item,
          points: item.points !== null && Number.isFinite(item.points) && item.points > 0 ? Math.round(item.points * 100) / 100 : null,
        })),
    }))
    .filter((criterion) => criterion.items.length > 0);
}

function parsePoints(value: string): number | null {
  return value === "" ? null : Number(value);
}

/**
 * Rubric atomic của một mục, sửa thẳng trong thẻ bên lề (#27, F5, UC 36): mỗi dòng một ý kiểm được kèm điểm; dòng cuối
 * là dấu "+" để thêm mục (yêu cầu 2026-10-22), Enter ở một mục cũng thêm mục ngay dưới. Mỗi lần sửa báo lên trang để
 * trang tự lưu.
 */
export function RubricEditor({ criteria, onChange }: {
  criteria: RubricCriterion[];
  onChange: (criteria: RubricCriterion[]) => void;
}) {
  const root = useRef<HTMLDivElement>(null);
  const focus = useRef<number | null>(null);
  const items = atomicItems(criteria);
  useEffect(() => {
    const index = focus.current;
    if (index === null) return;
    focus.current = null;
    root.current?.querySelectorAll<HTMLInputElement>("[data-atomic-text]")[index]?.focus();
  });

  function set(next: AtomicItem[]) {
    onChange(criteriaOf(next));
  }
  function edit(index: number, patch: Partial<AtomicItem>) {
    set(items.map((x, j) => (j === index ? { ...x, ...patch } : x)));
  }
  function add(at: number) {
    if (items.length >= LIMIT) return;
    focus.current = at;
    set([...items.slice(0, at), { criterionId: newId(), itemId: newId(), text: "", points: null }, ...items.slice(at)]);
  }

  return (
    <div ref={root} className="flex flex-col" data-testid="rubric-editor">
      {items.map((item, i) => (
        <div key={item.itemId ?? i} className="group/row flex items-center gap-0.5">
          <span aria-hidden className="w-3 text-center text-[11px] text-muted">☐</span>
          <input data-atomic-text aria-label={`Mục chấm ${i + 1}`} value={item.text} maxLength={1000} className={cx(INPUT, "flex-1")}
            onChange={(e) => edit(i, { text: e.target.value })}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.nativeEvent.isComposing) {
                e.preventDefault();
                add(i + 1);
              }
            }} />
          <input data-atomic-points aria-label={`Điểm mục ${i + 1}`} type="number" step="0.01" min="0" max="1000"
            value={item.points ?? ""}
            className={cx(POINTS, "w-12 text-right [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none")}
            onChange={(e) => edit(i, { points: parsePoints(e.target.value) })} />
          <button type="button" aria-label={`Xóa mục chấm ${i + 1}`} title="Xóa" onClick={() => set(items.filter((_, j) => j !== i))}
            className="inline-flex size-5 shrink-0 items-center justify-center rounded text-[11px] text-muted opacity-0 group-hover/row:opacity-100 hover:bg-danger-soft hover:text-danger focus:opacity-100">
            ✕
          </button>
        </div>
      ))}
      {items.length < LIMIT && (
        <button type="button" aria-label="Thêm mục chấm" title="Thêm mục chấm" onClick={() => add(items.length)}
          className="flex h-6 w-full items-center justify-center rounded-md text-sm leading-none text-muted hover:bg-shell hover:text-primary">
          +
        </button>
      )}
    </div>
  );
}
