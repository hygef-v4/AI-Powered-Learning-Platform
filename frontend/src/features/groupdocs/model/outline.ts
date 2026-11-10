import type { DocumentBlock } from "@/shared/document";
import type { SectionView } from "./types";

function headingLevel(block: DocumentBlock): number {
  if (block.type !== "HEADING") return Number.POSITIVE_INFINITY;
  const match = /^H([1-6])$/.exec(block.content.style);
  return match ? Number(match[1]) : 0;
}

/**
 * Mục nhóm chứa một heading của khung đề (dòng bấm trên sidebar): chính mục đó nếu heading là phần (`partId`), không thì
 * mục của heading cha gần nhất; heading nằm trên mọi phần thì lấy mục đầu tiên sau nó.
 */
export function findSectionForHeading(blocks: DocumentBlock[], sections: SectionView[], headingId: string): SectionView | null {
  const byPart = new Map(sections.map((s) => [s.partId, s]));
  const exact = byPart.get(headingId);
  if (exact) return exact;
  const at = blocks.findIndex((b) => b.id === headingId);
  if (at < 0) return null;
  let level = headingLevel(blocks[at]!);
  for (let i = at - 1; i >= 0; i--) {
    const block = blocks[i]!;
    if (block.type !== "HEADING" || headingLevel(block) >= level) continue;
    const parent = byPart.get(block.id);
    if (parent) return parent;
    level = headingLevel(block);
  }
  for (let i = at + 1; i < blocks.length; i++) {
    const after = byPart.get(blocks[i]!.id);
    if (after) return after;
  }
  return null;
}
