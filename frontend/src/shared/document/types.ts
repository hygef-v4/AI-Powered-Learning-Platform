// Tài liệu dùng chung cho khung đề (U07), bài làm (U08) và tài liệu nhóm (U12). Khớp DocumentModel của backend U07.

export type BlockOrigin = "TEACHER" | "STUDENT";
export type BlockType = "HEADING" | "PARAGRAPH" | "LIST" | "TABLE" | "IMAGE" | "DIAGRAM";
export type HeadingStyle = "TITLE" | "SUBTITLE" | "H1" | "H2" | "H3" | "H4" | "H5" | "H6";
export type DiagramType = "USE_CASE" | "CLASS" | "SEQUENCE" | "ACTIVITY" | "ER" | "COMPONENT" | "STATE" | "OTHER";

export type Align = "LEFT" | "CENTER" | "RIGHT" | "JUSTIFY";
/** Định dạng áp cho cả đoạn: đậm, nghiêng, gạch chân, gạch ngang, mã; màu chữ, màu tô nền (hex). */
export type Run = {
  text: string;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  strike?: boolean;
  code?: boolean;
  color?: string;
  highlight?: string;
};
export type HeadingContent = { style: HeadingStyle; text: string; align?: Align };
export type ParagraphContent = { runs: Run[]; align?: Align };
export type ListItem = { level?: number; runs: Run[] };
export type ListContent = { ordered?: boolean; items: ListItem[]; align?: Align };
/** {@code added}: ô người học thêm vào bảng của đề (yêu cầu 2026-10-22), người học chỉ xóa được hàng, cột gồm toàn ô này. */
export type TableCell = { runs: Run[]; added?: boolean };
export type TableContent = { headerRow?: boolean; rows: TableCell[][] };
/** {@code fileRef}: ảnh vừa tải lên, chưa lưu; {@code hasFile}: ảnh đã lưu, mở theo id block. */
export type ImageContent = { fileRef?: string; hasFile?: boolean; alt?: string; width?: number };
export type DiagramContent = { diagramType: DiagramType; xml: string; svgPreview?: string; alt?: string };

type Base<T extends BlockType, C> = { id: string; origin: BlockOrigin; type: T; content: C };
export type DocumentBlock =
  | Base<"HEADING", HeadingContent>
  | Base<"PARAGRAPH", ParagraphContent>
  | Base<"LIST", ListContent>
  | Base<"TABLE", TableContent>
  | Base<"IMAGE", ImageContent>
  | Base<"DIAGRAM", DiagramContent>;

export type DocumentModel = { blocks: DocumentBlock[] };

export type DocumentPart = { partId: string; headingBlockId: string | null; ancestorHeadingIds: string[]; title: string };

export type ValidationIssue = { code: string; blockId: string | null; message: string };

export type RubricItem = { itemId: string | null; text: string; points: number | null };
export type RubricCriterion = {
  criterionId: string | null;
  title: string;
  description: string;
  kind: "TEXT" | "DIAGRAM";
  items: RubricItem[];
};
export type RubricView = { rubricId: string; assignmentId: string; partId: string; criteria: RubricCriterion[]; maxPoints: number };

/**
 * Mục chấm atomic: một ý kiểm được đạt hay không, kèm điểm (yêu cầu 2026-10-21). Mỗi mục lưu thành một tiêu chí có
 * đúng một mục chấm (tên tiêu chí là 200 ký tự đầu của nội dung) để dùng lại mô hình rubric của backend và U09.
 */
export type AtomicItem = { criterionId: string | null; itemId: string | null; text: string; points: number | null };

export const ATOMIC_TITLE_MAX = 200;

/** Rubric thành danh sách mục atomic; tiêu chí nhiều mục (bản cũ, đề xuất AI) tách mỗi mục một dòng kèm tên tiêu chí. */
export function atomicItems(criteria: RubricCriterion[]): AtomicItem[] {
  return criteria.flatMap((criterion): AtomicItem[] => {
    if (criterion.items.length === 0) return [{ criterionId: criterion.criterionId, itemId: null, text: criterion.title, points: null }];
    if (criterion.items.length === 1) {
      const item = criterion.items[0]!;
      return [{ criterionId: criterion.criterionId, itemId: item.itemId, text: item.text || criterion.title, points: item.points }];
    }
    return criterion.items.map((item) => ({
      criterionId: null,
      itemId: item.itemId,
      text: [criterion.title.trim(), item.text.trim()].filter(Boolean).join(": "),
      points: item.points,
    }));
  });
}

/** Danh sách mục atomic thành rubric gửi lên: mỗi mục một tiêu chí; mục chưa có id tiêu chí thì cấp id mới. */
export function criteriaOf(items: AtomicItem[]): RubricCriterion[] {
  return items.map((item) => ({
    criterionId: item.criterionId ?? newId(),
    title: item.text.trim().slice(0, ATOMIC_TITLE_MAX),
    description: "",
    kind: "TEXT",
    items: [{ itemId: item.itemId ?? newId(), text: item.text, points: item.points }],
  }));
}

/** Tổng điểm rubric: cộng điểm dương của các mục chấm. */
export function rubricTotal(criteria: RubricCriterion[]): number {
  return criteria.reduce((sum, criterion) => sum + criterionTotal(criterion), 0);
}

export function criterionTotal(criterion: RubricCriterion): number {
  return criterion.items.reduce((sum, item) => sum + (Number(item.points) > 0 ? Number(item.points) : 0), 0);
}

/** Rubric đủ để duyệt (giống AssignmentRubric.complete): có tiêu chí; tiêu chí có tên và mục chấm; mục có nội dung, điểm > 0. */
export function rubricComplete(criteria: RubricCriterion[]): boolean {
  return criteria.length > 0 && criteria.every((criterion) => criterion.title.trim() !== "" && criterion.items.length > 0
    && criterion.items.every((item) => item.text.trim() !== "" && item.points !== null && item.points > 0));
}

export const headingLabels: Record<HeadingStyle, string> = {
  TITLE: "Tiêu đề", SUBTITLE: "Phụ đề", H1: "Tiêu đề 1", H2: "Tiêu đề 2", H3: "Tiêu đề 3",
  H4: "Tiêu đề 4", H5: "Tiêu đề 5", H6: "Tiêu đề 6",
};

export const diagramTypeLabels: Record<DiagramType, string> = {
  USE_CASE: "Use case", CLASS: "Sơ đồ lớp", SEQUENCE: "Sơ đồ tuần tự", ACTIVITY: "Sơ đồ hoạt động",
  ER: "Sơ đồ ER", COMPONENT: "Sơ đồ thành phần", STATE: "Sơ đồ trạng thái", OTHER: "Loại khác",
};

export const blockTypeLabels: Record<BlockType, string> = {
  HEADING: "Tiêu đề", PARAGRAPH: "Đoạn văn", LIST: "Danh sách", TABLE: "Bảng", IMAGE: "Ảnh", DIAGRAM: "Sơ đồ",
};

export function newId(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : "xxxxxxxx-xxxx-4xxx-8xxx-xxxxxxxxxxxx".replace(/x/g, () => Math.floor(Math.random() * 16).toString(16));
}

/** Block mới rỗng theo loại; người học không có block tiêu đề. */
export function newBlock(type: BlockType, origin: BlockOrigin): DocumentBlock {
  const id = newId();
  switch (type) {
    case "HEADING":
      return { id, origin, type, content: { style: "H1", text: "" } };
    case "PARAGRAPH":
      return { id, origin, type, content: { runs: [{ text: "" }] } };
    case "LIST":
      return { id, origin, type, content: { ordered: false, items: [{ level: 0, runs: [{ text: "" }] }] } };
    case "TABLE":
      return { id, origin, type, content: { headerRow: true, rows: [[{ runs: [{ text: "" }] }, { runs: [{ text: "" }] }], [{ runs: [{ text: "" }] }, { runs: [{ text: "" }] }]] } };
    case "IMAGE":
      return { id, origin, type, content: { alt: "" } };
    case "DIAGRAM":
      return { id, origin, type, content: { diagramType: "OTHER", xml: EMPTY_DIAGRAM, alt: "" } };
  }
}

export const EMPTY_DIAGRAM =
  '<mxfile><diagram id="page-1" name="Trang 1"><mxGraphModel><root><mxCell id="0"/><mxCell id="1" parent="0"/></root></mxGraphModel></diagram></mxfile>';

export function runsText(runs: Run[] | undefined): string {
  return (runs ?? []).map((run) => run.text ?? "").join("");
}

/** Chữ thuần của block để hiện tóm tắt hoặc kiểm rỗng. */
export function blockText(block: DocumentBlock): string {
  switch (block.type) {
    case "HEADING":
      return block.content.text ?? "";
    case "PARAGRAPH":
      return runsText(block.content.runs);
    case "LIST":
      return block.content.items.map((item) => runsText(item.runs)).join("\n");
    case "TABLE":
      return block.content.rows.map((row) => row.map((cell) => runsText(cell.runs)).join(" | ")).join("\n");
    case "IMAGE":
      return block.content.alt ?? "";
    case "DIAGRAM":
      return block.content.alt ?? "";
  }
}

/** Ảnh SVG xem trước dạng data URI cho thẻ img (không chạy script, không tải tài nguyên ngoài). */
export function svgDataUri(svg: string): string {
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

export function headingLevel(block: DocumentBlock): number {
  if (block.type !== "HEADING") return 0;
  const match = /^H([1-6])$/.exec(block.content.style);
  return match ? Number(match[1]) : 0;
}

export const WHOLE_DOCUMENT = "DOCUMENT";

/**
 * Phần của tài liệu, giống PartCalculator của backend (BR-U07-11): heading nhỏ nhất của mỗi nhánh trong cây Tiêu đề
 * 1–6; không có Tiêu đề 1–6 thì cả bài là một phần {@code DOCUMENT}. Dùng để hiện rubric ngay khi vừa gõ tiêu đề.
 */
export function computeParts(document: DocumentModel): DocumentPart[] {
  const headings = document.blocks
    .map((block) => ({ block, level: headingLevel(block) }))
    .filter((h) => h.level > 0)
    .map(({ block, level }) => ({ id: block.id, level, title: blockText(block).trim() || "(Chưa đặt tên)" }));
  if (headings.length === 0) return [{ partId: WHOLE_DOCUMENT, headingBlockId: null, ancestorHeadingIds: [], title: "Toàn bài" }];
  const parts: DocumentPart[] = [];
  const branch: typeof headings = [];
  headings.forEach((heading, index) => {
    while (branch.length > 0 && branch[branch.length - 1]!.level >= heading.level) branch.pop();
    const ancestors = branch.map((h) => h.id);
    const path = [...branch.map((h) => h.title), heading.title];
    branch.push(heading);
    const next = headings[index + 1];
    if (!next || next.level <= heading.level) {
      parts.push({ partId: heading.id, headingBlockId: heading.id, ancestorHeadingIds: ancestors, title: path.join(" › ") });
    }
  });
  return parts;
}

/** Mục chứa từng block: block thuộc mục lá gần nhất phía trên; khung không có Tiêu đề 1–6 thì cả bài là một mục. */
export function sectionsOf(document: DocumentModel, parts: DocumentPart[]): Map<string, string> {
  const leaves = new Set(parts.map((part) => part.headingBlockId));
  const whole = parts.length === 1 && parts[0]!.headingBlockId === null ? parts[0]!.partId : null;
  const sections = new Map<string, string>();
  let current = whole;
  for (const block of document.blocks) {
    if (!whole && headingLevel(block) > 0) current = leaves.has(block.id) ? block.id : null;
    if (current) sections.set(block.id, current);
  }
  return sections;
}

/**
 * Cây phần của tài liệu theo Tiêu đề 1–6: gốc giữ phần chung (trước tiêu đề đầu tiên); mỗi tiêu đề là một nhánh chứa
 * đoạn dẫn của nó rồi các nhánh con; nhánh không có con là lá (phần của BR-U07-11). Khung không có tiêu đề thì gốc là lá.
 */
export type DocSection = {
  heading: DocumentBlock | null;
  level: number;
  depth: number;
  blocks: DocumentBlock[];
  children: DocSection[];
  leaf: boolean;
};

export function documentTree(document: DocumentModel): DocSection {
  const root: DocSection = { heading: null, level: 0, depth: 0, blocks: [], children: [], leaf: false };
  const stack: DocSection[] = [root];
  for (const block of document.blocks) {
    const level = headingLevel(block);
    if (level === 0) {
      stack.at(-1)!.blocks.push(block);
      continue;
    }
    while (stack.length > 1 && stack.at(-1)!.level >= level) stack.pop();
    const node: DocSection = { heading: block, level, depth: stack.length, blocks: [], children: [], leaf: false };
    stack.at(-1)!.children.push(node);
    stack.push(node);
  }
  const mark = (section: DocSection) => {
    section.leaf = section.children.length === 0;
    section.children.forEach(mark);
  };
  mark(root);
  return root;
}
