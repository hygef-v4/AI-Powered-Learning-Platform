import type { DocumentBlock, DocumentModel } from "@/shared/document";
import type { SectionView } from "./types";

/**
 * Cả tài liệu nhóm để xem trên một trang: khung đề của giảng viên, nội dung đã Xong của từng mục chèn ngay sau tiêu đề và
 * hướng dẫn của mục đó (trước tiêu đề kế tiếp). Mục không còn tiêu đề trong khung đề thì thêm vào cuối kèm tiêu đề riêng.
 */
export function assembleGroupDocument(shared: DocumentModel, sections: SectionView[]): DocumentModel {
  const byPart = new Map(sections.map((s) => [s.partId, s]));
  const blocks: DocumentBlock[] = [];
  let pending: DocumentBlock[] = [];
  for (const block of shared.blocks) {
    if (block.type === "HEADING") {
      blocks.push(...pending);
      pending = [];
    }
    blocks.push(block);
    const section = byPart.get(block.id);
    if (section) pending = section.publishedBlocks ?? [];
  }
  blocks.push(...pending);
  const present = new Set(shared.blocks.map((b) => b.id));
  for (const section of sections) {
    if (present.has(section.partId)) continue;
    blocks.push({ id: section.partId, origin: "TEACHER", type: "HEADING", content: { style: "H2", text: section.title } });
    blocks.push(...(section.publishedBlocks ?? []));
  }
  return { ...shared, blocks };
}

/**
 * Tài liệu của trang soạn: phần chung của khung đề (không thuộc mục nào) chỉ để xem, các mục mình đang giữ gồm tiêu đề, hướng
 * dẫn của giảng viên và bài viết của mình ({@code drafts} theo ID mục); mục của người khác bỏ ra.
 */
export function assembleEditorDocument(
  shared: DocumentModel,
  sections: SectionView[],
  drafts: Readonly<Record<string, DocumentBlock[]>>,
): DocumentModel {
  const byPart = new Map(sections.map((s) => [s.partId, s]));
  const blocks: DocumentBlock[] = [];
  let pending: DocumentBlock[] = [];
  let skipping = false;
  for (const block of shared.blocks) {
    if (block.type === "HEADING") {
      blocks.push(...pending);
      pending = [];
      const section = byPart.get(block.id);
      const mine = !!section && section.id in drafts;
      skipping = !!section && !mine;
      if (mine) pending = drafts[section.id] ?? [];
    }
    if (!skipping) blocks.push(block);
  }
  blocks.push(...pending);
  const present = new Set(shared.blocks.map((b) => b.id));
  for (const section of sections) {
    if (present.has(section.partId) || !(section.id in drafts)) continue;
    blocks.push({ id: section.partId, origin: "TEACHER", type: "HEADING", content: { style: "H2", text: section.title } });
    blocks.push(...(drafts[section.id] ?? []));
  }
  return { ...shared, blocks };
}

/** Bài viết của mình trong từng mục (block của người học nằm dưới tiêu đề mục, tới tiêu đề kế tiếp), theo ID mục. */
export function splitDrafts(document: DocumentModel, sections: SectionView[], held: ReadonlySet<string>): Record<string, DocumentBlock[]> {
  const byPart = new Map(sections.filter((s) => held.has(s.id)).map((s) => [s.partId, s.id]));
  const drafts: Record<string, DocumentBlock[]> = Object.fromEntries([...held].map((id) => [id, [] as DocumentBlock[]]));
  let current: string | null = null;
  for (const block of document.blocks) {
    if (block.type === "HEADING") current = byPart.get(block.id) ?? null;
    else if (current && block.origin === "STUDENT") drafts[current]!.push(block);
  }
  return drafts;
}
