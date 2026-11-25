import {
  blockText,
  computeParts,
  joinRunsText,
  numberSections,
  readHeadingLevel,
  type DocumentBlock,
  type DocumentModel,
  type DocumentPart,
} from "@/shared/document";
import { msg } from "@/i18n/msg";

/**
 * Nhập DOCX vào bài làm (yêu cầu 2026-11-25): tệp phải có đủ các mục của đề theo đúng thứ tự (thường là tệp tải về từ
 * chính bài này rồi viết tiếp trong Word), không thì báo lỗi. Mỗi phần chỉ thêm các khối mới của sinh viên, giữ nguyên
 * định dạng; phần không có gì mới hoặc tệp không có chữ ở phần đó thì bỏ qua. Phần đề của giảng viên trong tệp bị bỏ,
 * riêng bảng của giảng viên mà sinh viên điền thì cập nhật ô.
 */
export type DocxMergeResult = { content: DocumentModel; updatedParts: string[] };

export class DocxStructureError extends Error {}

/** Chữ để so sánh: bỏ số mục ở đầu (I., 1.2., a.), gộp khoảng trắng, không phân biệt hoa thường. */
function normalize(text: string): string {
  return text.replace(/\s+/g, " ").trim()
    .replace(/^(?:(?:[0-9]+|[ivxlcdm]+|[a-z]{1,2})\.)+\s*/i, "")
    .toLowerCase();
}

/** Dấu của khối để biết hai khối có cùng nội dung không (chỉ so chữ, không so định dạng). */
function signature(block: DocumentBlock): string {
  switch (block.type) {
    case "IMAGE": return "IMAGE";
    case "DIAGRAM": return `DIAGRAM:${block.content.xml ?? ""}`;
    case "TABLE": return `TABLE:${blockText(block).replace(/\s+/g, " ").trim()}`;
    default: return `${block.type === "LIST" ? "LIST" : "TEXT"}:${blockText(block).replace(/\s+/g, " ").trim()}`;
  }
}

function isEmpty(block: DocumentBlock): boolean {
  return (block.type === "PARAGRAPH" || block.type === "LIST" || block.type === "HEADING") && !blockText(block).trim();
}

/** Tiêu đề không khớp mục nào của đề (sinh viên tự đánh số trong bài): thành đoạn in đậm. */
function toParagraph(block: DocumentBlock): DocumentBlock {
  if (block.type !== "HEADING") return block;
  return { id: block.id, origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: block.content.text ?? "", bold: true }] } };
}

function tableShape(block: DocumentBlock): string {
  return block.type === "TABLE" ? block.content.rows.map((row) => row.length).join("x") : "";
}

/** Các khối của {@code added} không có trong {@code existing} (so theo thứ tự, như diff). */
function findNew(existing: DocumentBlock[], added: DocumentBlock[]): DocumentBlock[] {
  const a = existing.map(signature);
  const b = added.map(signature);
  const lcs: number[][] = Array.from({ length: a.length + 1 }, () => new Array<number>(b.length + 1).fill(0));
  for (let i = a.length - 1; i >= 0; i--) {
    for (let j = b.length - 1; j >= 0; j--) {
      lcs[i]![j] = a[i] === b[j] ? lcs[i + 1]![j + 1]! + 1 : Math.max(lcs[i + 1]![j]!, lcs[i]![j + 1]!);
    }
  }
  const fresh: DocumentBlock[] = [];
  let i = 0;
  let j = 0;
  while (j < b.length) {
    if (i < a.length && a[i] === b[j]) { i++; j++; }
    else if (i < a.length && lcs[i + 1]![j]! >= lcs[i]![j + 1]!) i++;
    else { fresh.push(added[j]!); j++; }
  }
  return fresh;
}

/**
 * Chia tệp nhập theo các mục của bài; trả các khối nằm dưới từng tiêu đề của bài. Báo lỗi (yêu cầu 2026-11-25) khi:
 * thiếu mục hay sai thứ tự; tệp có mục lạ ở chỗ không phải phần viết bài (trước mục đầu tiên, giữa mục cha và mục con —
 * mục sinh viên tự đặt chỉ được nằm trong phần lá); hoặc tệp không có gì của đề này (tên bài, chữ đề, tiêu đề mục).
 */
function splitByHeadings(content: DocumentModel, imported: DocumentModel, title?: string): Map<string | null, DocumentBlock[]> {
  const headings = content.blocks.filter((block) => readHeadingLevel(block) > 0);
  const leaves = new Set(computeParts(content).map((part) => part.headingBlockId).filter((id): id is string => !!id));
  const chunks = new Map<string | null, DocumentBlock[]>([[null, []]]);
  let current: string | null = null;
  let next = 0;
  for (const block of imported.blocks) {
    const expected = headings[next];
    const text = block.type === "HEADING" ? block.content.text ?? "" : block.type === "PARAGRAPH" ? joinRunsText(block.content.runs) : null;
    if (expected && text !== null && (block.type === "HEADING" || normalize(text)) && normalize(text) === normalize(blockText(expected))) {
      current = expected.id;
      chunks.set(current, []);
      next++;
      continue;
    }
    if (headings.length > 0 && block.type === "HEADING" && (current === null || !leaves.has(current))) {
      const name = (block.content.text ?? "").trim() || msg("assignmentwork.model.docxMerge.untitledPart");
      throw new DocxStructureError(msg("assignmentwork.model.docxMerge.unexpectedPart", { title: name }));
    }
    chunks.get(current)!.push(block);
  }
  if (next < headings.length) {
    const missing = headings[next]!;
    const title = blockText(missing).trim() || msg("assignmentwork.model.docxMerge.untitledPart");
    throw new DocxStructureError(msg("assignmentwork.model.docxMerge.structureMismatch", { title }));
  }
  if (headings.length === 0) {
    // Bài không có mục: tệp phải có tên bài hoặc ít nhất một đoạn đề của giảng viên.
    const anchors = new Set(content.blocks.filter((block) => block.origin === "TEACHER").map((block) => normalize(blockText(block))).filter(Boolean));
    if (title && normalize(title)) anchors.add(normalize(title));
    const found = anchors.size === 0 || imported.blocks.some((block) => anchors.has(normalize(blockText(block))));
    if (!found) throw new DocxStructureError(msg("assignmentwork.model.docxMerge.notThisAssignment"));
  }
  return chunks;
}

/** Kiểm tra cấu trúc trước khi xem trước (báo lỗi ngay khi chọn tệp). */
export function checkDocxStructure(content: DocumentModel, imported: DocumentModel, title?: string): void {
  splitByHeadings(content, imported, title);
}

export function mergeImportedWork(content: DocumentModel, parts: DocumentPart[], imported: DocumentModel, title?: string): DocxMergeResult {
  const chunks = splitByHeadings(content, imported, title);
  const sections = numberSections(content, parts);
  const whole = parts.length === 1 && parts[0]!.headingBlockId === null ? parts[0]!.partId : null;
  let blocks = [...content.blocks];
  const updatedParts: string[] = [];

  for (const part of parts) {
    const chunk = whole ? chunks.get(null)! : chunks.get(part.headingBlockId);
    if (!chunk) continue;
    const own = blocks.filter((block) => sections.get(block.id) === part.partId && block.type !== "HEADING");
    const teacher = own.filter((block) => block.origin === "TEACHER");
    const student = own.filter((block) => block.origin !== "TEACHER");
    // Bỏ phần đề của giảng viên có trong tệp (mỗi khối đề dùng một lần); bảng đề cùng kích thước thì lấy ô sinh viên điền.
    const teacherText = teacher.filter((block) => block.type !== "TABLE").map(signature);
    const teacherTables = teacher.filter((block) => block.type === "TABLE");
    const filled = new Map<string, DocumentBlock>();
    const candidates: DocumentBlock[] = [];
    for (const block of chunk) {
      if (isEmpty(block)) continue;
      const seen = teacherText.indexOf(signature(block));
      if (seen >= 0) { teacherText.splice(seen, 1); continue; }
      if (block.type === "TABLE") {
        const table = teacherTables.find((t) => !filled.has(t.id) && tableShape(t) === tableShape(block));
        if (table) {
          if (signature(table) !== signature(block) && table.type === "TABLE") filled.set(table.id, { ...table, content: { ...table.content, rows: block.content.rows } });
          else filled.set(table.id, table);
          continue;
        }
      }
      candidates.push({ ...toParagraph(block), origin: "STUDENT" });
    }
    const fresh = findNew(student, candidates);
    const changedTables = [...filled.values()].filter((table) => teacherTables.find((t) => t.id === table.id) !== table);
    if (fresh.length === 0 && changedTables.length === 0) continue;
    updatedParts.push(part.title);
    blocks = blocks.map((block) => changedTables.find((table) => table.id === block.id) ?? block);
    let at = -1;
    blocks.forEach((block, index) => { if (sections.get(block.id) === part.partId) at = index; });
    blocks = at < 0 ? [...blocks, ...fresh] : [...blocks.slice(0, at + 1), ...fresh, ...blocks.slice(at + 1)];
  }
  return { content: { ...content, blocks }, updatedParts };
}
