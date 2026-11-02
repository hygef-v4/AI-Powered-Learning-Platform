import { describe, expect, it } from "vitest";
import { splitUngrouped } from "./RandomSplitDialog";
import type { Draft } from "./types";

const group = (key: string, size: number): Draft => ({
  key, id: key, name: `Nhóm ${key}`, leaderId: `${key}-0`, memberIds: Array.from({ length: size }, (_, i) => `${key}-${i}`),
});
const people = (n: number) => Array.from({ length: n }, (_, i) => `p${i}`);
const sizes = (drafts: Draft[]) => drafts.map((d) => d.memberIds.length);

describe("splitUngrouped", () => {
  it("lấp nhóm ít người trước, các nhóm hơn kém nhau nhiều nhất một người", () => {
    const result = splitUngrouped([group("a", 3), group("b", 1), group("c", 1)], people(4), 5);
    expect(sizes(result)).toEqual([3, 3, 3]);
  });

  it("thiếu chỗ thì thêm nhóm mới, chia đều theo sĩ số tối đa", () => {
    const result = splitUngrouped([], people(11), 4);
    expect(result).toHaveLength(3);
    expect(Math.max(...sizes(result)) - Math.min(...sizes(result))).toBeLessThanOrEqual(1);
    expect(Math.max(...sizes(result))).toBeLessThanOrEqual(4);
    result.forEach((d) => expect(d.memberIds).toContain(d.leaderId));
  });

  it("giữ nguyên thành viên cũ và đủ mọi người chưa có nhóm", () => {
    const result = splitUngrouped([group("a", 2)], people(7), 3);
    expect(result[0]!.memberIds.slice(0, 2)).toEqual(["a-0", "a-1"]);
    expect(result.flatMap((d) => d.memberIds).sort()).toEqual([...people(7), "a-0", "a-1"].sort());
    expect(Math.max(...sizes(result)) - Math.min(...sizes(result))).toBeLessThanOrEqual(1);
  });
});
