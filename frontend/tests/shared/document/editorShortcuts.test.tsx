import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { expect, it, vi } from "vitest";
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
