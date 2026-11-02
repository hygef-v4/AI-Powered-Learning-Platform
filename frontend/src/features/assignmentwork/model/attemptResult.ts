"use client";

import { useEffect, useState } from "react";
import type { DocumentPart } from "@/shared/document";
import { attemptResultSources, type AttemptResult, type AttemptResultItem } from "../slots";

/** Kết quả đã công bố của một lượt, lấy từ nguồn U09 đăng ký; chưa có nguồn hay chưa chọn lượt thì không tải. */
export function useAttemptResult(attemptId: string | null) {
  const [state, setState] = useState<{ attemptId: string | null; result: AttemptResult | null; failed: boolean }>({ attemptId: null, result: null, failed: false });
  useEffect(() => {
    const source = attemptResultSources.entries()[0];
    if (!attemptId || !source) return;
    let active = true;
    source.load(attemptId).then(
      (result) => { if (active) setState({ attemptId, result, failed: false }); },
      () => { if (active) setState({ attemptId, result: null, failed: true }); },
    );
    return () => { active = false; };
  }, [attemptId]);
  const current = state.attemptId === attemptId;
  return { result: current ? state.result : null, failed: current && state.failed, loading: !!attemptId && !current };
}

export type ResultPart = { key: string; title: string; awarded: number; max: number; items: AttemptResultItem[] };

/**
 * Điểm theo từng bài: gom các mục rubric về nhánh con trực tiếp của gốc tài liệu (heading cấp cao nhất của phần), giữ thứ tự
 * các bài trong tài liệu. Không có danh sách phần thì mỗi phần của rubric là một bài.
 */
export function resultByPart(items: AttemptResultItem[], parts: DocumentPart[] | undefined): ResultPart[] {
  const partOf = new Map((parts ?? []).map((part) => [part.partId, part]));
  const order = new Map<string, number>();
  (parts ?? []).forEach((part, index) => {
    const key = part.ancestorHeadingIds[0] ?? part.partId;
    if (!order.has(key)) order.set(key, index);
  });
  const groups = new Map<string, ResultPart>();
  for (const item of items) {
    const part = partOf.get(item.partId);
    const key = part ? part.ancestorHeadingIds[0] ?? part.partId : item.partId;
    const title = part ? part.title.split(" › ")[0]! : item.criterionTitle || "Bài";
    const group = groups.get(key) ?? { key, title, awarded: 0, max: 0, items: [] };
    group.awarded += item.awarded;
    group.max += item.max;
    group.items.push(item);
    groups.set(key, group);
  }
  return [...groups.values()].sort((a, b) => (order.get(a.key) ?? Infinity) - (order.get(b.key) ?? Infinity));
}

/** Điểm làm tròn 2 chữ số, bỏ số 0 thừa ở cuối: 7.50 → 7.5, 10.00 → 10. */
export function formatPoints(value: number | string | null | undefined) {
  return value == null ? "—" : String(Number(Number(value).toFixed(2)));
}
