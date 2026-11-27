import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { DocumentEditor } from "@/shared/document/DocumentEditor";
import type { DocumentBlock, DocumentModel } from "@/shared/document";

const paragraph = (id: string, text: string): DocumentBlock => ({ id, origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text }] } });

const START: DocumentModel = { blocks: [
  { id: "h1", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Câu 1" } },
  paragraph("p1", "Dòng một"),
  paragraph("p2", "Dòng hai"),
  { id: "h2", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Câu 2" } },
  paragraph("p3", "Dòng ba"),
] };

/** Bản mới nhất trình soạn gửi lên trang. */
const changes = vi.fn<(next: DocumentModel) => void>();
function Page() {
  const [doc, setDoc] = useState<DocumentModel>(START);
  return <>
    <button type="button" data-testid="outside">Mục lục</button>
    <DocumentEditor document={doc} onChange={(next) => { changes(next); setDoc(next); }} mode="author" diagramEnabled={false} />
  </>;
}

const field = (blockId: string) => screen.getByTestId(`block-${blockId}`).querySelector<HTMLElement>("[contenteditable]")!;
const editor = () => screen.getByTestId("document-editor");
const texts = () => (changes.mock.lastCall?.[0] ?? START).blocks.map((block) => (block.type === "PARAGRAPH" ? block.content.runs.map((run) => run.text).join("") : block.id));

it("Ctrl+A chọn cả văn bản của câu đang viết, không lan sang câu khác; Delete xóa cả nhóm và Ctrl+Z trả lại", () => {
  render(<Page />);
  fireEvent.focus(field("p1"));
  fireEvent.keyDown(field("p1"), { key: "a", ctrlKey: true });
  expect(screen.getByTestId("block-p1")).toHaveClass("bg-primary-soft");
  expect(screen.getByTestId("block-p2")).toHaveClass("bg-primary-soft");
  expect(screen.getByTestId("block-p3")).not.toHaveClass("bg-primary-soft");

  fireEvent.keyDown(screen.getByTestId("document-paper"), { key: "Delete" });
  expect(texts()).toEqual(["h1", "h2", "Dòng ba"]);

  fireEvent.keyDown(editor(), { key: "z", ctrlKey: true });
  expect(texts()).toEqual(["h1", "Dòng một", "Dòng hai", "h2", "Dòng ba"]);
});

it("gõ đè khi đã chọn cả câu thay cả nhóm bằng một đoạn chữ", () => {
  render(<Page />);
  fireEvent.focus(field("p2"));
  fireEvent.keyDown(field("p2"), { key: "a", ctrlKey: true });
  fireEvent.keyDown(screen.getByTestId("document-paper"), { key: "x" });
  expect(texts()).toEqual(["h1", "x", "h2", "Dòng ba"]);
});

it("con trỏ ngoài trình soạn (bấm Mục lục) vẫn Ctrl+Z, Ctrl+Y được", () => {
  render(<Page />);
  fireEvent.focus(field("p1"));
  fireEvent.keyDown(field("p1"), { key: "a", ctrlKey: true });
  fireEvent.keyDown(screen.getByTestId("document-paper"), { key: "Delete" });
  expect(texts()).toEqual(["h1", "h2", "Dòng ba"]);

  const outside = screen.getByTestId("outside");
  outside.focus();
  fireEvent.keyDown(outside, { key: "z", ctrlKey: true });
  expect(texts()).toEqual(["h1", "Dòng một", "Dòng hai", "h2", "Dòng ba"]);
  fireEvent.keyDown(outside, { key: "y", ctrlKey: true });
  expect(texts()).toEqual(["h1", "h2", "Dòng ba"]);
});

it("Ctrl+A ở chữ nằm giữa hai câu hỏi chỉ chọn đoạn đó, không gộp đoạn mở đầu; gõ đè vẫn ở giữa hai câu (yêu cầu 2026-11-27)", () => {
  const between: DocumentModel = { blocks: [
    paragraph("i1", "Mở đầu 1"), paragraph("i2", "Mở đầu 2"),
    { id: "h1", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Câu 1" } },
    { id: "b1", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Giữa 1" }], under: 0 } },
    paragraph("b2", "Giữa 2"),
    { id: "h2", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Câu 2" } },
  ] };
  let latest = between;
  function Between() {
    const [doc, setDoc] = useState<DocumentModel>(between);
    return <DocumentEditor document={doc} onChange={(next) => { latest = next; setDoc(next); }} mode="author" diagramEnabled={false} />;
  }
  render(<Between />);
  fireEvent.focus(field("b2"));
  fireEvent.keyDown(field("b2"), { key: "a", ctrlKey: true });
  expect(screen.getByTestId("block-b1")).toHaveClass("bg-primary-soft");
  expect(screen.getByTestId("block-b2")).toHaveClass("bg-primary-soft");
  expect(screen.getByTestId("block-i1")).not.toHaveClass("bg-primary-soft");

  fireEvent.keyDown(screen.getByTestId("document-paper"), { key: "x" });
  expect(latest.blocks.map((b) => (b.type === "PARAGRAPH" ? b.content.runs.map((run) => run.text).join("") : b.id)))
    .toEqual(["Mở đầu 1", "Mở đầu 2", "h1", "x", "h2"]);
  const typed = latest.blocks[3]!;
  expect(typed.type === "PARAGRAPH" && typed.content.under).toBe(0);
});

it("Từng phần: chữ giữa hai câu là một trang riêng, trang mở đầu không gộp chữ đó (yêu cầu 2026-11-27)", () => {
  const doc: DocumentModel = { blocks: [
    paragraph("i1", "Mở đầu"),
    { id: "h1", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Câu 1" } },
    paragraph("q1", "Nội dung câu 1"),
    { id: "a1", origin: "TEACHER", type: "HEADING", content: { style: "H2", text: "Ý a" } },
    { id: "m1", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Giữa ý" }], under: 1 } },
    { id: "b1", origin: "TEACHER", type: "HEADING", content: { style: "H2", text: "Ý b" } },
    { id: "g1", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Giữa câu" }], under: 0 } },
    { id: "h2", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Câu 2" } },
  ] };
  render(<DocumentEditor document={doc} onChange={() => {}} mode="author" diagramEnabled={false} initialView="step" />);
  const paper = () => screen.getByTestId("document-paper").textContent ?? "";
  const next = () => fireEvent.click(screen.getAllByRole("button", { name: "Phần sau" })[0]!);
  const pages: string[] = [];
  for (let i = 0; i < 6; i++) { pages.push(paper()); next(); }
  // Mở đầu | Câu 1 | Ý a | Giữa ý | Ý b | Giữa câu
  expect(pages[0]).toContain("Mở đầu");
  expect(pages[0]).not.toContain("Giữa câu");
  expect(pages[3]).toContain("Giữa ý");
  expect(pages[3]).not.toContain("Nội dung câu 1");
  expect(pages[3]).not.toContain("Giữa câu");
  expect(pages[5]).toContain("Giữa câu");
  expect(pages[5]).not.toContain("Mở đầu");
  expect(pages.filter((page) => page.includes("Giữa câu"))).toHaveLength(1);
});

describe("Ctrl+A trong bài làm của sinh viên (yêu cầu 2026-11-27)", () => {
  function Student({ start }: { start: DocumentModel }) {
    const [doc, setDoc] = useState<DocumentModel>(start);
    return <DocumentEditor document={doc} onChange={setDoc} mode="student" answerGuidance diagramEnabled={false} />;
  }
  const answer = (id: string, text: string): DocumentBlock => ({ id, origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text }] } });

  it.each([
    ["đề có câu hỏi", [{ id: "h", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Câu 1" } } as DocumentBlock,
      { id: "t", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Đề" }] } } as DocumentBlock]],
    ["đề chỉ có phần gốc", [{ id: "t", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Đề" }] } } as DocumentBlock]],
  ])("%s: chọn mọi dòng bài làm của câu, không chọn đề", (_name, teacher) => {
    render(<Student start={{ blocks: [...teacher, answer("a1", "Dòng một"), answer("a2", "Dòng hai"), answer("a3", "Dòng ba")] }} />);
    fireEvent.focus(field("a3"));
    fireEvent.keyDown(field("a3"), { key: "a", ctrlKey: true });
    for (const id of ["a1", "a2", "a3"]) expect(screen.getByTestId(`block-${id}`), id).toHaveClass("bg-primary-soft");
    expect(screen.getByTestId("block-t")).not.toHaveClass("bg-primary-soft");
  });
});
