import { describe, expect, it } from "vitest";
import { computeParts, type DocumentBlock, type DocumentModel } from "@/shared/document";
import { checkDocxStructure, DocxStructureError, mergeImportedWork } from "@/features/assignmentwork/model/docxMerge";

const heading = (id: string, style: "H1" | "H2", text: string, origin: "TEACHER" | "STUDENT" = "TEACHER"): DocumentBlock =>
  ({ id, origin, type: "HEADING", content: { style, text } });
const para = (id: string, text: string, origin: "TEACHER" | "STUDENT" = "TEACHER", bold = false): DocumentBlock =>
  ({ id, origin, type: "PARAGRAPH", content: { runs: [{ text, ...(bold ? { bold: true } : {}) }] } });
const table = (id: string, cells: string[][], origin: "TEACHER" | "STUDENT" = "TEACHER"): DocumentBlock =>
  ({ id, origin, type: "TABLE", content: { rows: cells.map((row) => row.map((text) => ({ runs: [{ text }] }))) } });
const texts = (model: DocumentModel) => model.blocks.map((b) => (b.type === "PARAGRAPH" ? b.content.runs.map((r) => r.text).join("") : b.id));

// Bài: I. Phân tích (đề + bài cũ của sinh viên), II. Thiết kế (II.1 Lớp, II.2 Bảng có bảng đề để điền).
const content: DocumentModel = { blocks: [
  heading("h1", "H1", "Phân tích"), para("t1", "Mô tả hệ thống"), para("s1", "Bài cũ", "STUDENT"),
  heading("h2", "H1", "Thiết kế"),
  heading("h21", "H2", "Lớp"), para("t21", "Vẽ sơ đồ lớp"),
  heading("h22", "H2", "Bảng"), table("tb", [["Tên", "Kiểu"], ["", ""]]),
] };
const parts = computeParts(content);

describe("nhập DOCX vào bài làm", () => {
  it("tệp tải về rồi viết tiếp: chỉ thêm bài mới vào đúng phần, bỏ đề, giữ định dạng; phần không có gì mới thì bỏ qua", () => {
    const imported: DocumentModel = { blocks: [
      para("x0", "Tên bài", "STUDENT", true),
      heading("x1", "H1", "Phân tích", "STUDENT"), para("x2", "Mô tả hệ thống", "STUDENT"), para("x3", "Bài cũ", "STUDENT"),
      para("x4", "Ý mới", "STUDENT", true),
      heading("x5", "H1", "Thiết kế", "STUDENT"),
      heading("x6", "H2", "Lớp", "STUDENT"), para("x7", "Vẽ sơ đồ lớp", "STUDENT"),
      heading("x8", "H2", "Bảng", "STUDENT"), table("x9", [["Tên", "Kiểu"], ["id", "uuid"]], "STUDENT"),
    ] };
    const { content: next, updatedParts } = mergeImportedWork(content, parts, imported);
    expect(texts(next)).toEqual(["h1", "Mô tả hệ thống", "Bài cũ", "Ý mới", "h2", "h21", "Vẽ sơ đồ lớp", "h22", "tb"]);
    expect(next.blocks.find((b) => b.id === "x4")).toMatchObject({ origin: "STUDENT", content: { runs: [{ text: "Ý mới", bold: true }] } });
    // Bảng đề cùng kích thước: cập nhật ô sinh viên điền, không thêm bảng mới.
    const filled = next.blocks.find((b) => b.id === "tb");
    expect(filled?.type === "TABLE" && filled.content.rows[1]![0]!.runs[0]!.text).toBe("id");
    expect(updatedParts).toEqual(["Phân tích", "Thiết kế › Bảng"]);
  });

  it("tệp không có bài mới thì không đổi gì", () => {
    const imported: DocumentModel = { blocks: [heading("x1", "H1", "I. Phân tích"), para("x3", "Bài cũ"), heading("x5", "H1", "II. Thiết kế"),
      heading("x6", "H2", "Lớp"), heading("x8", "H2", "Bảng")] };
    const result = mergeImportedWork(content, parts, imported);
    expect(result.updatedParts).toEqual([]);
    expect(result.content.blocks).toEqual(content.blocks);
  });

  it("thiếu mục hay sai thứ tự thì báo lỗi cấu trúc", () => {
    const wrong: DocumentModel = { blocks: [heading("x1", "H1", "Thiết kế"), heading("x2", "H1", "Phân tích")] };
    expect(() => checkDocxStructure(content, wrong)).toThrow(DocxStructureError);
    expect(() => checkDocxStructure(content, { blocks: [para("p", "Chỉ có chữ")] })).toThrow(/Phân tích/);
  });

  it("tệp không liên quan bị từ chối: mục lạ trước mục đầu tiên hay giữa mục cha và mục con", () => {
    const unrelated: DocumentModel = { blocks: [heading("u1", "H1", "Chương 1. Giới thiệu", "STUDENT"), para("u2", "Nội dung khác"),
      heading("x1", "H1", "Phân tích"), heading("x5", "H1", "Thiết kế"), heading("x6", "H2", "Lớp"), heading("x8", "H2", "Bảng")] };
    expect(() => checkDocxStructure(content, unrelated)).toThrow(/Giới thiệu/);
    const between: DocumentModel = { blocks: [heading("x1", "H1", "Phân tích"), heading("x5", "H1", "Thiết kế"),
      heading("odd", "H2", "Mục lạ", "STUDENT"), heading("x6", "H2", "Lớp"), heading("x8", "H2", "Bảng")] };
    expect(() => checkDocxStructure(content, between)).toThrow(/Mục lạ/);
  });

  it("bài không có mục: tệp phải có tên bài hoặc chữ đề, không thì từ chối", () => {
    const flat: DocumentModel = { blocks: [para("t", "Đề bài: mô tả hệ thống"), para("s", "Cũ", "STUDENT")] };
    expect(() => checkDocxStructure(flat, { blocks: [para("a", "Tài liệu khác hẳn")] }, "Bài 1")).toThrow(DocxStructureError);
    expect(() => checkDocxStructure(flat, { blocks: [para("a", "Bài 1"), para("b", "Mới")] }, "Bài 1")).not.toThrow();
    expect(() => checkDocxStructure(flat, { blocks: [para("a", "Đề bài: mô tả hệ thống"), para("b", "Mới")] })).not.toThrow();
  });

  it("bài không có mục: cả tệp là một phần", () => {
    const flat: DocumentModel = { blocks: [para("t", "Đề"), para("s", "Cũ", "STUDENT")] };
    const { content: next, updatedParts } = mergeImportedWork(flat, computeParts(flat), { blocks: [para("a", "Đề"), para("b", "Cũ"), para("c", "Mới")] });
    expect(texts(next)).toEqual(["Đề", "Cũ", "Mới"]);
    expect(updatedParts).toHaveLength(1);
  });
});
