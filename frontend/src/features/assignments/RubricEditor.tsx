"use client";

import { useEffect, useRef } from "react";
import { atomicItems, criteriaOf, newId, type AtomicItem, type RubricCriterion } from "@/shared/document";
import { cx } from "@/shared/ui";

const INPUT = "min-w-0 rounded-md border border-transparent bg-transparent px-1.5 py-1 text-xs text-ink outline-none placeholder:text-faint hover:border-line focus:border-primary focus:bg-white";
/** BR-U07-21: tối đa 20 tiêu chí mỗi phần; mỗi mục atomic là một tiêu chí. */
const LIMIT = 20;

/** Điểm gửi lên server: số dương, tối đa 2 chữ số thập phân; ô trống, 0 hoặc số âm là chưa có điểm. */
export function cleanCriteria(criteria: RubricCriterion[]): RubricCriterion[] {
  return criteria.map((criterion) => ({
    ...criterion,
    items: criterion.items.map((item) => ({
      ...item,
      points: item.points !== null && Number.isFinite(item.points) && item.points > 0 ? Math.round(item.points * 100) / 100 : null,
    })),
  }));
}

type Field = "text" | "points";

function parsePoints(value: string): number | null {
  return value === "" ? null : Number(value);
}

/**
 * Rubric atomic của một mục, sửa thẳng trong thẻ bên lề (#27, F5, UC 36; yêu cầu 2026-10-21): mỗi dòng một ý kiểm được
 * kèm điểm; luôn có một dòng trống ở cuối, gõ vào là thành mục mới (không cần nút thêm). Mỗi lần sửa báo lên trang để
 * trang tự lưu.
 */
export function RubricEditor({ criteria, onChange }: {
  criteria: RubricCriterion[];
  onChange: (criteria: RubricCriterion[]) => void;
}) {
  const root = useRef<HTMLDivElement>(null);
  const focus = useRef<{ index: number; field: Field } | null>(null);
  const items = atomicItems(criteria);
  useEffect(() => {
    const target = focus.current;
    if (!target) return;
    focus.current = null;
    const field = root.current?.querySelectorAll<HTMLInputElement>(target.field === "text" ? "[data-atomic-text]" : "[data-atomic-points]")[target.index];
    if (!field) return;
    field.focus();
    if (target.field === "text") field.setSelectionRange(field.value.length, field.value.length);
  });

  function set(next: AtomicItem[]) {
    onChange(criteriaOf(next));
  }
  function edit(index: number, patch: Partial<AtomicItem>) {
    set(items.map((x, j) => (j === index ? { ...x, ...patch } : x)));
  }
  function append(patch: Partial<AtomicItem>, field: Field) {
    focus.current = { index: items.length, field };
    set([...items, { criterionId: newId(), itemId: newId(), text: "", points: null, ...patch }]);
  }

  const rows = items.map((item, i) => ({ item, i, blank: false }));
  const canAdd = items.length < LIMIT;
  return (
    <div ref={root} className="flex flex-col" data-testid="rubric-editor">
      {[...rows, ...(canAdd ? [{ item: { criterionId: null, itemId: null, text: "", points: null } as AtomicItem, i: items.length, blank: true }] : [])]
        .map(({ item, i, blank }) => (
          <div key={blank ? "new" : item.itemId ?? i} className="group/row flex items-center gap-0.5">
            <span aria-hidden className={cx("w-3 text-center text-[11px]", blank ? "text-faint" : "text-muted")}>{blank ? "+" : "☐"}</span>
            <input data-atomic-text aria-label={blank ? "Mục chấm mới" : `Mục chấm ${i + 1}`} placeholder={blank ? "Thêm mục chấm" : ""}
              value={item.text} maxLength={1000} className={cx(INPUT, "flex-1")}
              onChange={(e) => (blank ? append({ text: e.target.value }, "text") : edit(i, { text: e.target.value }))} />
            <input data-atomic-points aria-label={blank ? "Điểm mục mới" : `Điểm mục ${i + 1}`} type="number" step="0.25" min="0" max="1000"
              placeholder="đ" value={item.points ?? ""}
              className={cx(INPUT, "w-12 text-right [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none")}
              onChange={(e) => (blank ? append({ points: parsePoints(e.target.value) }, "points") : edit(i, { points: parsePoints(e.target.value) }))} />
            {!blank ? (
              <button type="button" aria-label={`Xóa mục chấm ${i + 1}`} title="Xóa" onClick={() => set(items.filter((_, j) => j !== i))}
                className="inline-flex size-5 shrink-0 items-center justify-center rounded text-[11px] text-muted opacity-0 group-hover/row:opacity-100 hover:bg-danger-soft hover:text-danger focus:opacity-100">
                ✕
              </button>
            ) : <span className="size-5 shrink-0" />}
          </div>
        ))}
    </div>
  );
}
