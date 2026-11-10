import type { DocumentModel, Run } from "@/shared/document/types";

const runsText = (runs: Run[] | undefined) => (runs ?? []).map((run) => run.text).join("");

/**
 * Chữ của mẫu đề (DOCX đã nhập thành khung) gửi cho AI (yêu cầu 2026-11-05): mục đánh số 1., 1.1. như trình soạn,
 * đoạn, danh sách, bảng thành dòng chữ. {@code questionCount}: số mục ngoài cùng, tức số câu hỏi của mẫu.
 */
export function templateText(document: DocumentModel): { text: string; questionCount: number } {
  const lines: string[] = [];
  const open: number[] = [];
  const counters: number[] = [];
  let questionCount = 0;
  for (const block of document.blocks) {
    switch (block.type) {
      case "HEADING": {
        const level = Number(block.content.style.replace(/\D/g, "")) || 1;
        while (open.length > 0 && open[open.length - 1]! >= level) open.pop();
        open.push(level);
        const depth = open.length;
        counters[depth - 1] = (counters[depth - 1] ?? 0) + 1;
        counters.length = depth;
        if (depth === 1) questionCount++;
        lines.push(`${counters.join(".")}. ${block.content.text ?? ""}`.trimEnd());
        break;
      }
      case "PARAGRAPH":
        lines.push(runsText(block.content.runs));
        break;
      case "LIST":
        block.content.items.forEach((item) => lines.push(`${"  ".repeat(item.level ?? 0)}- ${runsText(item.runs)}`));
        break;
      case "TABLE":
        block.content.rows.forEach((row) => lines.push(row.map((cell) => runsText(cell.runs)).join(" | ")));
        break;
      default:
        break;
    }
  }
  return { text: lines.filter((line) => line.trim()).join("\n"), questionCount };
}
