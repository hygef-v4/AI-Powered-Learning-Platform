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
/** {@code runs}: chữ có định dạng của tiêu đề (yêu cầu 2026-11-04); {@code text} luôn là chữ thuần của nó. */
export type HeadingContent = { style: HeadingStyle; text: string; runs?: Run[]; align?: Align };
/**
 * {@code under}: đoạn viết ở dòng cuối của một mục cha, sau các mục con (yêu cầu 2026-11-02), mang cấp tiêu đề của mục đó
 * (0: cả tài liệu); các block thường theo sau nó cũng thuộc mục đó.
 */
export type ParagraphContent = { runs: Run[]; align?: Align; under?: number };
export type ListItem = { level?: number; runs: Run[] };
export type ListContent = { ordered?: boolean; items: ListItem[]; align?: Align; under?: number };
/** {@code added}: ô người học thêm vào bảng của đề (yêu cầu 2026-10-22), người học chỉ xóa được hàng, cột gồm toàn ô này. */
export type TableCell = { runs: Run[]; added?: boolean; align?: Align };
/** {@code widths}: độ rộng các cột theo % bề ngang bảng (người soạn kéo mép cột); không có thì các cột bằng nhau. */
export type TableContent = { headerRow?: boolean; rows: TableCell[][]; widths?: number[]; under?: number };
/** {@code fileRef}: ảnh vừa tải lên, chưa lưu; {@code hasFile}: ảnh đã lưu, mở theo id block. */
export type ImageContent = { fileRef?: string; hasFile?: boolean; alt?: string; width?: number; under?: number };
export type DiagramContent = { diagramType: DiagramType; xml: string; svgPreview?: string; alt?: string; under?: number };

type Base<T extends BlockType, C> = { id: string; origin: BlockOrigin; type: T; content: C };
export type DocumentBlock =
  | Base<"HEADING", HeadingContent>
  | Base<"PARAGRAPH", ParagraphContent>
  | Base<"LIST", ListContent>
  | Base<"TABLE", TableContent>
  | Base<"IMAGE", ImageContent>
  | Base<"DIAGRAM", DiagramContent>;

/** Tên bài có định dạng của khung đề (yêu cầu 2026-11-04); chữ thuần của tên vẫn ở tên bài. */
export type TitleContent = { runs: Run[]; align?: Align };
export type DocumentModel = { blocks: DocumentBlock[]; title?: TitleContent };

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

/** BR-U07-21: điểm một mục là số dương, tối đa 2 chữ số thập phân (server từ chối 1.255, không làm tròn). */
export function pointsValid(points: number | null | undefined): boolean {
  const value = Number(points);
  return points !== null && points !== undefined && Number.isFinite(value) && value >= 0
    && Math.abs(value * 100 - Math.round(value * 100)) < 1e-6;
}

/** Điểm đã nhập nhưng sai dạng (quá 2 chữ số thập phân); ô trống, 0, số âm là "chưa có điểm", không tính là sai. */
export function pointsMalformed(points: number | null | undefined): boolean {
  return points !== null && points !== undefined && Number(points) >= 0 && !pointsValid(points);
}

/** Tổng điểm rubric: cộng điểm hợp lệ của các mục chấm. */
export function rubricTotal(criteria: RubricCriterion[]): number {
  return criteria.reduce((sum, criterion) => sum + criterionTotal(criterion), 0);
}

export function criterionTotal(criterion: RubricCriterion): number {
  return criterion.items.reduce((sum, item) => sum + (pointsValid(item.points) ? Number(item.points) : 0), 0);
}

/** Rubric đủ để duyệt (giống AssignmentRubric.complete): có tiêu chí; tiêu chí có tên và mục chấm; mục có nội dung, điểm hợp lệ. */
export function rubricComplete(criteria: RubricCriterion[]): boolean {
  return criteria.length > 0 && criteria.every((criterion) => criterion.title.trim() !== "" && criterion.items.length > 0
    && criterion.items.every((item) => item.text.trim() !== "" && pointsValid(item.points)));
}

/** Mục chấm có ghi gì đó (chữ hoặc điểm); dòng không chữ không điểm bỏ qua (yêu cầu 2026-11-05). */
function writtenItems(criteria: RubricCriterion[]) {
  return criteria.flatMap((criterion) => criterion.items)
    .filter((item) => item.text.trim() !== "" || (item.points !== null && item.points !== undefined));
}

/**
 * Thẻ rubric thiếu điểm (giống RubricService.pointsMissing): có mục chấm mà tổng điểm không quá 0, hoặc có mục ghi chữ
 * chưa ghi điểm / ghi điểm chưa ghi chữ. Thẻ chỉ có dòng trống coi như chưa có rubric, không báo.
 */
/** Có mục chấm ghi dở: có chữ chưa điểm (hoặc điểm sai), có điểm chưa chữ. */
export function rubricHalfWritten(criteria: RubricCriterion[]): boolean {
  return writtenItems(criteria).some((item) => item.text.trim() === "" || !pointsValid(item.points));
}

export function rubricPointsMissing(criteria: RubricCriterion[]): boolean {
  const items = writtenItems(criteria);
  if (items.length === 0) return false;
  return items.some((item) => item.text.trim() === "" || !pointsValid(item.points)) || rubricTotal(criteria) <= 0;
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
    .map(({ block, level }) => ({ id: block.id, level, title: blockText(block).trim() }));
  if (headings.length === 0) return [{ partId: WHOLE_DOCUMENT, headingBlockId: null, ancestorHeadingIds: [], title: "Toàn bài" }];
  const parts: DocumentPart[] = [];
  const branch: (typeof headings[number] & { label: string })[] = [];
  // Số thứ tự như trong trang (I., I.1.): câu hỏi chưa đặt tên thì gọi bằng số của nó.
  const counters: number[] = [];
  headings.forEach((heading, index) => {
    while (branch.length > 0 && branch[branch.length - 1]!.level >= heading.level) branch.pop();
    counters.length = branch.length + 1;
    counters[branch.length] = (counters[branch.length] ?? 0) + 1;
    const label = heading.title || sectionNumber(counters);
    const ancestors = branch.map((h) => h.id);
    const path = [...branch.map((h) => h.label), label];
    branch.push({ ...heading, label });
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
    else if (!whole && underOf(block) !== null) current = null;
    if (current) sections.set(block.id, current);
  }
  return sections;
}

/** Câu hỏi lồng tối đa 4 tầng: I., I.1., I.1.a., I.1.a.aa. (yêu cầu 2026-11-10, thay 3 tầng của 2026-11-04). */
export const MAX_SECTION_DEPTH = 4;

const ROMAN: [number, string][] = [[1000, "M"], [900, "CM"], [500, "D"], [400, "CD"], [100, "C"], [90, "XC"], [50, "L"],
  [40, "XL"], [10, "X"], [9, "IX"], [5, "V"], [4, "IV"], [1, "I"]];

function roman(n: number): string {
  let rest = n;
  let out = "";
  for (const [value, symbol] of ROMAN) {
    while (rest >= value) {
      out += symbol;
      rest -= value;
    }
  }
  return out;
}

/** a, b, …, z, aa, ab, … */
function letters(n: number): string {
  let rest = n;
  let out = "";
  while (rest > 0) {
    rest--;
    out = String.fromCharCode(97 + (rest % 26)) + out;
    rest = Math.floor(rest / 26);
  }
  return out;
}

/**
 * Số câu hỏi theo thứ tự ở từng tầng (yêu cầu 2026-11-10), giống SectionNumbering của backend: tầng 1 số La Mã, tầng 2
 * số, tầng 3 chữ thường, tầng 4 chữ thường gấp đôi: [1] → "I.", [1, 2] → "I.2.", [1, 2, 1] → "I.2.a.", [1, 2, 1, 3] → "I.2.a.cc.".
 */
export function sectionNumber(counters: number[]): string {
  return counters.map((count, depth) => {
    const n = Math.max(1, count);
    const part = depth === 0 ? roman(n) : depth === 1 ? String(n) : depth === 2 ? letters(n) : letters(n).repeat(2);
    return `${part}.`;
  }).join("");
}

/** Cấp mục mà đoạn {@code block} quay về (đoạn viết ở dòng cuối của mục cha), null nếu là đoạn thường. */
export function underOf(block: DocumentBlock): number | null {
  return block.type !== "HEADING" && typeof block.content.under === "number" ? block.content.under : null;
}

/**
 * Cây phần của tài liệu theo Tiêu đề 1–6: gốc giữ phần chung (trước tiêu đề đầu tiên); mỗi tiêu đề là một nhánh chứa
 * đoạn dẫn của nó rồi các nhánh con; nhánh không có con là lá (phần của BR-U07-11). Khung không có tiêu đề thì gốc là lá.
 * {@code after}: văn bản của nhánh viết sau các nhánh con (dòng cuối ở đáy ô trắng, yêu cầu 2026-11-02).
 */
export type DocSection = {
  heading: DocumentBlock | null;
  level: number;
  depth: number;
  blocks: DocumentBlock[];
  children: DocSection[];
  after: DocumentBlock[];
  leaf: boolean;
};

export function documentTree(document: DocumentModel): DocSection {
  const root: DocSection = { heading: null, level: 0, depth: 0, blocks: [], children: [], after: [], leaf: false };
  const stack: DocSection[] = [root];
  for (const block of document.blocks) {
    const level = headingLevel(block);
    if (level === 0) {
      const under = underOf(block);
      if (under !== null) while (stack.length > 1 && stack.at(-1)!.level > under) stack.pop();
      const owner = stack.at(-1)!;
      (owner.children.length > 0 ? owner.after : owner.blocks).push(block);
      continue;
    }
    while (stack.length > 1 && stack.at(-1)!.level >= level) stack.pop();
    const node: DocSection = { heading: block, level, depth: stack.length, blocks: [], children: [], after: [], leaf: false };
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
