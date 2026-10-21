import type { DocumentBlock } from "./types";

/** Lấy chữ của một block theo nội dung của U07 (`text`, `runs`, `items`, `rows`). */
export function blockText(value: unknown): string {
  if (typeof value === "string") return value;
  if (typeof value === "number") return String(value);
  if (Array.isArray(value)) return value.map(blockText).join("");
  if (value && typeof value === "object") {
    const record = value as Record<string, unknown>;
    if ("text" in record) return blockText(record.text);
    if ("runs" in record) return blockText(record.runs);
    return Object.entries(record)
      .filter(([key]) => !["xml", "url", "src", "fileId", "style", "bold", "italic", "ordered", "headerRow"].includes(key))
      .map(([, v]) => blockText(v))
      .join(" ");
  }
  return "";
}

/** Hiện block chỉ đọc; trình soạn và trình xem đầy đủ của U07 thay khi U07 có. */
export function BlockView({ block }: { block: DocumentBlock }) {
  const c = block.content ?? {};
  if (block.type === "HEADING") return <h3 className="mt-3 mb-1 text-[15px] font-bold text-ink">{blockText(c.text)}</h3>;
  if (block.type === "LIST" && Array.isArray(c.items)) {
    const items = c.items.map((item, i) => <li key={i}>{blockText(item)}</li>);
    return c.ordered ? <ol className="ml-5 list-decimal">{items}</ol> : <ul className="ml-5 list-disc">{items}</ul>;
  }
  if (block.type === "DIAGRAM") return <p className="rounded-lg bg-shell px-3 py-2 text-xs text-muted">[Sơ đồ Draw.io]</p>;
  if (block.type === "IMAGE") return <p className="rounded-lg bg-shell px-3 py-2 text-xs text-muted">[Ảnh]</p>;
  return <p className="mb-2 text-[13px] whitespace-pre-wrap text-body">{blockText(c)}</p>;
}

/** Trình soạn đoạn văn tạm: mỗi đoạn (cách nhau dòng trống) thành một block `PARAGRAPH` của người học. */
export function textToBlocks(text: string, previous: DocumentBlock[] = []): DocumentBlock[] {
  return text
    .split(/\n\s*\n/)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean)
    .map((paragraph, i) => ({
      id: previous[i]?.id ?? (typeof crypto !== "undefined" && "randomUUID" in crypto ? crypto.randomUUID() : `${Date.now()}-${i}`),
      origin: "STUDENT" as const,
      type: "PARAGRAPH" as const,
      content: { runs: [{ text: paragraph }] },
    }));
}

export function blocksToText(blocks: DocumentBlock[] | null | undefined): string {
  return (blocks ?? []).map((b) => blockText(b.content)).join("\n\n");
}
