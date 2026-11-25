import { act } from "@testing-library/react";
import { afterEach, expect, it } from "vitest";
import { outlineOf, publishOutline } from "@/features/assignments/model/outline";
import type { DocumentBlock } from "@/shared/document";

const heading = (id: string, style: "H1" | "H2" | "H3", text: string): DocumentBlock => ({ id, origin: "TEACHER", type: "HEADING", content: { style, text } });
const blocks: DocumentBlock[] = [
  heading("p1", "H1", "Hệ thống thư viện"),
  { id: "t1", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Mô tả" }] } },
  heading("q1", "H2", "Vẽ use case"),
  heading("q2", "H2", ""),
  heading("q2a", "H3", "Ý a"),
  heading("p2", "H1", "Đề phụ"),
];

afterEach(() => { act(() => publishOutline(null)); });

it("dàn ý theo cây câu hỏi, đánh số I., I.1., I.2.a. như trong trang; đoạn văn không ghi", () => {
  expect(outlineOf(blocks)).toEqual([
    { id: "p1", depth: 1, title: "Hệ thống thư viện", number: "I." },
    { id: "q1", depth: 2, title: "Vẽ use case", number: "I.1." },
    { id: "q2", depth: 2, title: "", number: "I.2." },
    { id: "q2a", depth: 3, title: "Ý a", number: "I.2.a." },
    { id: "p2", depth: 1, title: "Đề phụ", number: "II." },
  ]);
});
