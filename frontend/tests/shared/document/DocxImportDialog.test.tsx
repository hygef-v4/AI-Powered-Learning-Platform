import { fireEvent, render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { DocxImportDialog } from "@/shared/document/DocxImportDialog";

const drag = (files: File[]) => ({ dataTransfer: { types: ["Files"], files, dropEffect: "" } });
const docx = new File(["x"], "De.docx");

it("kéo thả tệp DOCX vào hộp thoại thì đọc và hiện xem trước; nhập theo cách đã chọn (yêu cầu 2026-11-27)", async () => {
  const preview = vi.fn().mockResolvedValue({ imageCount: 1, diagramCount: 2, warnings: [], document: { blocks: [
    { id: "h", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Câu 1" } },
    { id: "p", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Nội dung" }] } },
  ] } });
  const onImport = vi.fn();
  render(<DocxImportDialog preview={preview} onImport={onImport} allowReplace onClose={vi.fn()} />);
  const drop = screen.getByTestId("docx-drop");
  expect(drop).toHaveTextContent("Kéo tệp DOCX vào đây");
  fireEvent.dragEnter(drop, drag([docx]));
  expect(drop).toHaveTextContent("Thả để đọc tệp");
  fireEvent.drop(drop, drag([docx]));

  expect(await screen.findByTestId("docx-preview")).toHaveTextContent("Câu 1");
  expect(preview).toHaveBeenCalledExactlyOnceWith(docx);
  expect(screen.getByTestId("docx-file-card")).toHaveTextContent("De.docx");
  fireEvent.click(screen.getByRole("radio", { name: /Thêm vào cuối|Add to the end/ }));
  fireEvent.click(screen.getByTestId("docx-import-confirm"));
  await vi.waitFor(() => expect(onImport).toHaveBeenCalledWith(expect.objectContaining({ blocks: expect.any(Array) }), "append"));
});

it("tệp không phải DOCX thì báo lỗi, không gửi đi", () => {
  const preview = vi.fn();
  render(<DocxImportDialog preview={preview} onImport={vi.fn()} allowReplace onClose={vi.fn()} />);
  fireEvent.drop(screen.getByTestId("docx-drop"), drag([new File(["x"], "a.pdf")]));
  expect(screen.getByRole("alert")).toHaveTextContent("Chỉ nhận tệp Word");
  expect(preview).not.toHaveBeenCalled();
});
