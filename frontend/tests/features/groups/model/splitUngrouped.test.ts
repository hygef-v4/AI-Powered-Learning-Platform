import { describe, expect, it } from "vitest";
import { splitUngrouped } from "@/features/groups/components/RandomSplitDialog";
import type { Draft } from "@/features/groups/model/types";

const group = (key: string, size: number): Draft => ({
  key, id: key, name: `Nhóm ${key}`, leaderId: `${key}-0`, memberIds: Array.from({ length: size }, (_, i) => `${key}-${i}`),
});
const people = (n: number) => Array.from({ length: n }, (_, i) => `p${i}`);
const sizes = (drafts: Draft[]) => drafts.map((d) => d.memberIds.length);

describe("splitUngrouped", () => {

  it("giữ nguyên thành viên cũ và đủ mọi người chưa có nhóm", () => {
    const result = splitUngrouped([group("a", 2)], people(7), 3);
    expect(result[0]!.memberIds.slice(0, 2)).toEqual(["a-0", "a-1"]);
    expect(result.flatMap((d) => d.memberIds).sort()).toEqual([...people(7), "a-0", "a-1"].sort());
    expect(Math.max(...sizes(result)) - Math.min(...sizes(result))).toBeLessThanOrEqual(1);
  });
});
