"use client";

import { Button, cx } from "@/shared/ui";

export type PagerTone = "current" | "done" | "right" | "wrong" | "blank";

const TONES: Record<PagerTone, string> = {
  current: "border-primary text-primary-ink",
  done: "border-success bg-success-soft text-success",
  right: "border-success bg-success-soft text-success",
  wrong: "border-danger bg-danger-soft text-danger",
  blank: "border-line text-body",
};

/** Bảng số câu dùng chung cho màn làm bài và xem kết quả (mỗi câu một trang). */
export function QuestionNav({ count, index, onChange, toneOf, labelOf }: {
  count: number;
  index: number;
  onChange: (index: number) => void;
  toneOf: (index: number) => PagerTone;
  labelOf?: (index: number) => string;
}) {
  return (
    // Màn hẹp hơn lg: một dải số cuộn ngang thay vì nhiều hàng, để không đẩy câu hỏi xuống dưới.
    <div className="flex flex-wrap gap-1.5 max-lg:flex-nowrap max-lg:overflow-x-auto max-lg:p-0.5">
      {Array.from({ length: count }, (_, i) => (
        <button key={i} type="button" onClick={() => onChange(i)} aria-current={i === index ? "true" : undefined}
          aria-label={labelOf ? labelOf(i) : `Câu ${i + 1}`}
          className={cx("h-8 w-8 shrink-0 rounded-lg border text-xs font-semibold pointer-coarse:size-11", TONES[toneOf(i)], i === index && "ring-2 ring-primary")}>
          {i + 1}
        </button>
      ))}
    </div>
  );
}

/** Nút Câu trước, Câu tiếp. */
export function QuestionPrevNext({ count, index, onChange, testIdPrefix }: {
  count: number;
  index: number;
  onChange: (index: number) => void;
  testIdPrefix: string;
}) {
  return (
    <div className="mt-4 flex justify-between">
      <Button size="sm" disabled={index === 0} onClick={() => onChange(index - 1)} data-testid={`${testIdPrefix}-prev`}>Câu trước</Button>
      <Button size="sm" disabled={index >= count - 1} onClick={() => onChange(index + 1)} data-testid={`${testIdPrefix}-next`}>Câu tiếp</Button>
    </div>
  );
}

/** Đọc số câu (đếm từ 1) trên URL thành chỉ số hợp lệ. */
export function questionIndex(param: string, count: number): number {
  const value = Number(param);
  return Number.isInteger(value) && value >= 1 ? Math.min(value - 1, Math.max(count - 1, 0)) : 0;
}
