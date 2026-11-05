import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { DocumentEditor } from "@/shared/document/DocumentEditor";
import type { DocumentModel } from "@/shared/document";

/** Trang soạn giữ tài liệu trong state như AssignmentFormPage: kiểm tra cả luồng soạn từ tài liệu trống. */
function Page() {
  const [doc, setDoc] = useState<DocumentModel>({ blocks: [] });
  return <DocumentEditor document={doc} onChange={setDoc} mode="author" diagramEnabled={false} />;
}

const labels = (row: HTMLElement) => within(row).getAllByRole("button").map((button) => button.getAttribute("aria-label"));
const sectionIds = () => Array.from(document.querySelectorAll<HTMLElement>("section[data-section]")).map((section) => section.dataset.section!);
const focusLine = (testId: string) => fireEvent.focus(within(screen.getByTestId(testId)).getByLabelText("Dòng cuối"));
const typeInto = (line: HTMLElement, text: string) => {
  const field = within(line).getByLabelText("Dòng cuối");
  fireEvent.focus(field);
  field.textContent = text;
  fireEvent.input(field);
};

it("soạn từ trống: + Mục trên thanh công cụ thêm mục con tại con trỏ, thanh + ở đáy ô văn bản thêm mục con ở cuối; mỗi ô chỉ một dòng cuối và một +", () => {
  render(<Page />);
  // "+" ẩn cho tới khi con trỏ vào ô văn bản đó.
  expect(screen.queryByTestId("quick-add-DOCUMENT")).toBeNull();
  focusLine("tail-DOCUMENT");
  expect(labels(screen.getByTestId("quick-add-DOCUMENT"))).toEqual(["Thêm mục"]);
  fireEvent.click(within(screen.getByTestId("quick-add-DOCUMENT")).getByRole("button", { name: "Thêm mục" }));
  const [bai1] = sectionIds();
  expect(screen.getByTestId(`number-${bai1}`)).toHaveTextContent("1.");
  expect(labels(screen.getByTestId(`quick-add-${bai1}`))).toEqual(["Thêm mục"]);

  // Con trỏ trong mục 1 → "+ Mục" trên thanh công cụ thêm mục con 1.1 ngay tại con trỏ.
  typeInto(screen.getByTestId(`tail-${bai1}`), "Mô tả bài 1");
  fireEvent.click(screen.getByTestId("add-block"));
  const cau1a = sectionIds()[1]!;
  expect(screen.getByTestId(`number-${cau1a}`)).toHaveTextContent("1.1.");
  // Con trỏ ở dòng cuối của cả tài liệu → mục ngoài cùng 2.
  focusLine("tail-end-DOCUMENT");
  fireEvent.click(screen.getByTestId("add-block"));
  // Con trỏ trong 1.1 → mục con 1.1.1; mục tầng 3 thì nút "+ Mục" khóa.
  typeInto(screen.getByTestId(`tail-${cau1a}`), "Nội dung 1.1");
  fireEvent.click(screen.getByTestId("add-block"));
  expect(sectionIds().map((id) => screen.getByTestId(`number-${id}`).textContent)).toEqual(["1.", "1.1.", "1.1.1.", "2."]);
  expect(screen.getByTestId("add-block")).toBeDisabled();
  // "+" ở đáy ô văn bản của mục 1 → mục con 1.2 ở cuối mục 1.
  focusLine(`tail-end-${bai1}`);
  fireEvent.click(within(screen.getByTestId(`quick-add-${bai1}`)).getByRole("button", { name: "Thêm mục" }));
  expect(sectionIds().map((id) => screen.getByTestId(`number-${id}`).textContent)).toEqual(["1.", "1.1.", "1.1.1.", "1.2.", "2."]);

  // Mục 1 đã có mục con: dòng cuối ở giữa (trên 1.1) mất, giữa 1.1 và 1.2 không có "+"; chữ "Mô tả bài 1" vẫn còn.
  expect(screen.queryByTestId(`tail-${bai1}`)).toBeNull();
  expect(document.querySelector(`[data-section="${bai1}"] > [data-leaf-body]`)).toHaveTextContent("Mô tả bài 1");
  focusLine(`tail-end-${bai1}`);
  expect(document.querySelectorAll(`[data-section="${bai1}"] > [data-leaf-body] > [data-testid^="quick-add"]`)).toHaveLength(1);

  // Mục có mục con: dòng cuối ở đáy ô văn bản, gõ vào thì chữ nằm dưới các mục con, vẫn thuộc mục đó.
  const body = document.querySelector(`[data-section="${bai1}"] > [data-leaf-body]`)!;
  const endLine = screen.getByTestId(`tail-end-${bai1}`);
  expect(endLine.parentElement).toBe(body);
  typeInto(endLine, "Ghi chú cuối mục 1");
  const note = Array.from(body.children).find((child) => child.textContent === "Ghi chú cuối mục 1");
  expect(note).toBeDefined();
  expect(screen.getByTestId(`tail-end-${bai1}`)).toBeInTheDocument();

  // Cuối tài liệu cũng có dòng cuối và nút thêm mục ngoài cùng.
  focusLine("tail-end-DOCUMENT");
  expect(labels(screen.getByTestId("quick-add-DOCUMENT"))).toEqual(["Thêm mục"]);
});

it("ô bảng định dạng được: bôi đen rồi bấm Đậm chỉ đổi phần bôi đen; không có nút thụt lề, không có dòng chú thích dưới sơ đồ", async () => {
  const { setCaret } = await import("@/shared/document/RichText");
  let latest: DocumentModel | null = null;
  function TablePage() {
    const [doc, setDoc] = useState<DocumentModel>({ blocks: [
      { id: "tb", origin: "TEACHER", type: "TABLE", content: { rows: [[{ runs: [{ text: "Ô một" }] }]] } },
      { id: "dg", origin: "TEACHER", type: "DIAGRAM", content: { diagramType: "OTHER", xml: "<mxfile/>", alt: "" } },
    ] });
    return <DocumentEditor document={doc} onChange={(next) => { latest = next; setDoc(next); }} mode="author" diagramEnabled />;
  }
  render(<TablePage />);
  const cell = screen.getByLabelText("Ô 1-1");
  fireEvent.mouseDown(screen.getByTestId("block-tb"));
  cell.focus();
  setCaret(cell, 0, 1);
  fireEvent.click(screen.getByLabelText("Đậm (Ctrl+B)"));
  const table = (latest as DocumentModel | null)!.blocks[0]!;
  expect(table.type === "TABLE" && table.content.rows[0]![0]!.runs).toEqual([{ text: "Ô", bold: true }, { text: " một" }]);
  expect(screen.queryByLabelText("Tăng thụt lề")).toBeNull();
  expect(screen.queryByLabelText("Giảm thụt lề")).toBeNull();
  expect(screen.queryByLabelText("Chú thích sơ đồ")).toBeNull();
});

it("căn lề từng ô bảng; con trỏ trong bảng thì không thêm mục; mục tầng 3 (1.1.1.) không có nút thêm mục con; sidebar thêm được mục", async () => {
  const { setCaret } = await import("@/shared/document/RichText");
  let latest: DocumentModel | null = null;
  function TablePage() {
    const [doc, setDoc] = useState<DocumentModel>({ blocks: [
      { id: "a", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "A" } },
      { id: "b", origin: "TEACHER", type: "HEADING", content: { style: "H2", text: "B" } },
      { id: "c", origin: "TEACHER", type: "HEADING", content: { style: "H3", text: "C" } },
      { id: "tb", origin: "TEACHER", type: "TABLE", content: { rows: [[{ runs: [{ text: "Ô" }] }]] } },
    ] });
    return <DocumentEditor document={doc} onChange={(next) => { latest = next; setDoc(next); }} mode="author" diagramEnabled={false} />;
  }
  render(<TablePage />);
  const cell = screen.getByLabelText("Ô 1-1");
  fireEvent.mouseDown(screen.getByTestId("block-tb"));
  cell.focus();
  setCaret(cell, 0, 0);
  fireEvent(globalThis.document, new Event("selectionchange"));
  expect(screen.getByTestId("add-block")).toBeDisabled();
  fireEvent.click(screen.getByLabelText("Căn giữa"));
  const table = (latest as DocumentModel | null)!.blocks.find((b) => b.id === "tb")!;
  expect(table.type === "TABLE" && table.content.rows[0]![0]!.align).toBe("CENTER");

  // Mục 1.1.1. không có "+" thêm mục con; sidebar thêm mục vào mục 1.
  fireEvent.focus(within(screen.getByTestId("tail-c")).getByLabelText("Dòng cuối"));
  expect(screen.queryByTestId("quick-add-c")).toBeNull();
  act(() => { window.dispatchEvent(new CustomEvent("document:add-part", { detail: { parentId: "a" } })); });
  expect((latest as DocumentModel | null)!.blocks.map((b) => b.type === "HEADING" ? b.content.style : b.id)).toEqual(["H1", "H2", "H3", "tb", "H2"]);
});

it("trang đưa chỗ trên header thì nút chọn cách đọc nằm ở đó, không nằm trên thanh công cụ", () => {
  const slot = document.createElement("div");
  document.body.appendChild(slot);
  const doc: DocumentModel = { blocks: [{ id: "a", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "A" } }] };
  render(<DocumentEditor document={doc} onChange={() => {}} mode="author" diagramEnabled={false} viewSlot={slot} />);
  expect(within(slot).getByRole("radiogroup", { name: "Chế độ xem" })).toBeInTheDocument();
  expect(within(screen.getByRole("toolbar", { name: "Định dạng và chèn" })).queryByRole("radiogroup")).toBeNull();
  slot.remove();
});

describe("mục: định dạng tiêu đề, lên xuống, kéo thả; thanh công cụ theo chỗ con trỏ; độ rộng cột", () => {
  const blocks = (): DocumentModel => ({ blocks: [
    { id: "a", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "A" } },
    { id: "a1", origin: "TEACHER", type: "HEADING", content: { style: "H2", text: "A1" } },
    { id: "a2", origin: "TEACHER", type: "HEADING", content: { style: "H2", text: "A2" } },
    { id: "b", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "B" } },
    { id: "tb", origin: "TEACHER", type: "TABLE", content: { rows: [[{ runs: [{ text: "x" }] }, { runs: [{ text: "y" }] }]] } },
  ] });
  function Page({ withTitle = false }: { withTitle?: boolean }) {
    const [doc, setDoc] = useState<DocumentModel>(blocks());
    const [title, setTitle] = useState("Bài tập lớn");
    return <><output data-testid="order">{doc.blocks.map((b) => b.id).join(",")}</output>
      <output data-testid="json">{JSON.stringify(doc)}</output>
      <output data-testid="title">{title}</output>
      <DocumentEditor document={doc} onChange={setDoc} mode="author" diagramEnabled
        titleField={withTitle ? { text: title, onText: setTitle } : undefined} /></>;
  }
  const json = () => JSON.parse(screen.getByTestId("json").textContent!) as DocumentModel;

  it("tiêu đề mục đậm được; ở tiêu đề thì không chèn bảng, ảnh, sơ đồ", async () => {
    const { setCaret } = await import("@/shared/document/RichText");
    render(<Page />);
    const title = within(screen.getByTestId("block-a1")).getByLabelText("Nội dung tiêu đề");
    fireEvent.mouseDown(screen.getByTestId("block-a1"));
    title.focus();
    setCaret(title, 0, 1);
    expect(screen.getByLabelText("Chèn sơ đồ Draw.io")).toBeDisabled();
    fireEvent.click(screen.getByLabelText("Đậm (Ctrl+B)"));
    const heading = json().blocks.find((b) => b.id === "a1")!;
    expect(heading.type === "HEADING" && heading.content).toMatchObject({ text: "A1", runs: [{ text: "A", bold: true }, { text: "1" }] });
  });

  it("nút lên, xuống chỉ đổi chỗ trong cùng cha; kéo số mục thả lên mục cùng cha hiện bản xem trước mờ rồi đổi chỗ", () => {
    render(<Page />);
    const a1 = document.querySelector<HTMLElement>('[data-section="a1"]')!;
    expect(within(a1).getByRole("button", { name: "Đưa mục lên" })).toBeDisabled();
    fireEvent.click(within(a1).getByRole("button", { name: "Đưa mục xuống" }));
    expect(screen.getByTestId("order")).toHaveTextContent("a,a2,a1,b,tb");
    // Kéo A thả xuống nửa dưới của B (cùng cha là gốc; jsdom không có toạ độ chuột nên là nửa dưới).
    const data = { effectAllowed: "", setData: () => {} };
    fireEvent.dragStart(screen.getByTestId("number-a"), { dataTransfer: data });
    // Mục khác cha (A1 nằm trong A) không nhận thả.
    fireEvent.dragOver(document.querySelector<HTMLElement>('[data-section="a1"]')!, { dataTransfer: data });
    expect(screen.queryByTestId("drop-preview")).toBeNull();
    const b = document.querySelector<HTMLElement>('[data-section="b"]')!;
    fireEvent.dragOver(b, { dataTransfer: data });
    expect(screen.getByTestId("drop-preview")).toHaveTextContent("A");
    // Thả ở chỗ bản xem trước (không phải trên mục) vẫn đổi chỗ.
    fireEvent.drop(screen.getByTestId("drop-preview"), { dataTransfer: data });
    expect(screen.getByTestId("order")).toHaveTextContent("b,tb,a,a2,a1");
  });

  it("tên bài: định dạng, căn lề được và lưu trong JSON khung đề; không thêm mục, không chèn; kéo mép cột đổi độ rộng cột", () => {
    render(<Page withTitle />);
    const title = screen.getByLabelText("Tên bài");
    fireEvent.focus(title);
    expect(screen.getByTestId("add-block")).toBeDisabled();
    expect(screen.getByLabelText("Chèn sơ đồ Draw.io")).toBeDisabled();
    expect(screen.getByLabelText("Danh sách chấm")).toBeDisabled();
    fireEvent.click(screen.getByLabelText("Đậm (Ctrl+B)"));
    fireEvent.click(screen.getByLabelText("Căn giữa"));
    expect(json().title).toEqual({ runs: [{ text: "Bài tập lớn", bold: true }], align: "CENTER" });
    expect(screen.getByTestId("title")).toHaveTextContent("Bài tập lớn");
    const table = screen.getByTestId("block-tb").querySelector("table")!;
    table.getBoundingClientRect = () => ({ top: 0, height: 0, bottom: 0, left: 0, right: 200, width: 200, x: 0, y: 0, toJSON: () => ({}) });
    fireEvent.mouseDown(screen.getByTestId("column-resize-0"), { clientX: 100 });
    fireEvent.mouseMove(window, { clientX: 140 });
    fireEvent.mouseUp(window);
    const tb = json().blocks.find((b) => b.id === "tb")!;
    expect(tb.type === "TABLE" && tb.content.widths).toEqual([70, 30]);
  });
});

describe("thanh công cụ theo chỗ con trỏ: dòng cuối, ô bảng, danh sách", () => {
  function Page() {
    const [doc, setDoc] = useState<DocumentModel>({ blocks: [
      { id: "a", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "A" } },
      { id: "l", origin: "TEACHER", type: "LIST", content: { ordered: false, items: [{ level: 0, runs: [{ text: "ý" }] }] } },
      { id: "b", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "B" } },
      { id: "tb", origin: "TEACHER", type: "TABLE", content: { rows: [[{ runs: [{ text: "dòng 1\ndòng 2" }] }]] } },
    ] });
    return <><output data-testid="json">{JSON.stringify(doc)}</output><DocumentEditor document={doc} onChange={setDoc} mode="author" diagramEnabled /></>;
  }
  const json = () => JSON.parse(screen.getByTestId("json").textContent!) as DocumentModel;

  it("dòng cuối: thêm mục, định dạng, căn lề; danh sách tạo ngay một mục trống, xóa được khi chưa có chữ", () => {
    render(<Page />);
    const line = within(screen.getByTestId("tail-a")).getByLabelText("Dòng cuối");
    fireEvent.focus(line);
    expect(screen.getByTestId("add-block")).toBeEnabled();
    expect(screen.getByLabelText("Chèn sơ đồ Draw.io")).toBeEnabled();
    fireEvent.click(screen.getByLabelText("Đậm (Ctrl+B)"));
    fireEvent.click(screen.getByLabelText("Căn phải"));
    // Chọn danh sách ở dòng cuối: tạo ngay danh sách một mục trống (mang định dạng, căn lề đặt sẵn); Backspace ở mục trống xóa nó.
    fireEvent.click(screen.getByLabelText("Danh sách số"));
    const added = json().blocks.find((block) => block.type === "LIST" && block.id !== "l")!;
    expect(added).toMatchObject({ type: "LIST", content: { ordered: true, align: "RIGHT", items: [{ level: 0, runs: [{ text: "", bold: true }] }] } });
    const item = within(screen.getByTestId(`block-${added.id}`)).getByLabelText("Mục 1");
    fireEvent.keyDown(item, { key: "Backspace" });
    expect(json().blocks.some((block) => block.id === added.id)).toBe(false);
  });

  it("danh sách: không thêm mục, không chèn; ô bảng: không thêm mục, không chèn, danh sách thêm dấu đầu dòng từng dòng", async () => {
    const { setCaret } = await import("@/shared/document/RichText");
    render(<Page />);
    fireEvent.mouseDown(screen.getByTestId("block-l"));
    expect(screen.getByTestId("add-block")).toBeDisabled();
    expect(screen.getByLabelText("Chèn sơ đồ Draw.io")).toBeDisabled();
    const cell = screen.getByLabelText("Ô 1-1");
    fireEvent.mouseDown(screen.getByTestId("block-tb"));
    cell.focus();
    setCaret(cell, 0, 0);
    fireEvent(globalThis.document, new Event("selectionchange"));
    expect(screen.getByTestId("add-block")).toBeDisabled();
    expect(screen.getByLabelText("Chèn sơ đồ Draw.io")).toBeDisabled();
    fireEvent.click(screen.getByLabelText("Danh sách chấm"));
    const table = json().blocks.find((block) => block.id === "tb")!;
    expect(table.type === "TABLE" && table.content.rows[0]![0]!.runs.map((run) => run.text).join("")).toBe("• dòng 1\n• dòng 2");
  });
});

it("chèn sơ đồ: con trỏ xuống dòng ngay sau (đoạn kế, dòng cuối của mục, hoặc một đoạn trống mới trước mục con)", () => {
  const ids = (doc: DocumentModel) => doc.blocks.map((b) => (b.type === "PARAGRAPH" && !b.content.runs.some((r) => r.text) ? "_" : b.type === "DIAGRAM" ? "D" : b.id));
  let latest: DocumentModel | null = null;
  const docs: DocumentModel[] = [
    { blocks: [{ id: "a", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "A" } }, { id: "t", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "x" }] } }] },
    { blocks: [{ id: "a", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "A" } }, { id: "t", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "x" }] } },
      { id: "c", origin: "TEACHER", type: "HEADING", content: { style: "H2", text: "C" } }] },
  ];
  const expected = [["a", "t", "D"], ["a", "t", "D", "_", "c"]];
  docs.forEach((doc, i) => {
    const view = render(<DocumentEditor document={doc} onChange={(next) => { latest = next; }} mode="author" diagramEnabled />);
    fireEvent.mouseDown(screen.getByTestId("block-t"));
    fireEvent.click(screen.getByLabelText("Chèn sơ đồ Draw.io"));
    expect(ids(latest!)).toEqual(expected[i]);
    view.unmount();
  });
});

it("dòng cuối: Enter thêm dòng trống; + Mục tại con trỏ để khối bên dưới ở lại mục cha, dưới mục con mới", () => {
  let latest: DocumentModel | null = null;
  function Editor() {
    const [doc, setDoc] = useState<DocumentModel>({ blocks: [
      { id: "a", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "A" } },
      { id: "t", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "x" }] } },
      { id: "d", origin: "TEACHER", type: "DIAGRAM", content: { diagramType: "OTHER", xml: "<mxfile/>", alt: "" } },
    ] });
    return <DocumentEditor document={doc} onChange={(next) => { latest = next; setDoc(next); }} mode="author" diagramEnabled />;
  }
  render(<Editor />);
  fireEvent.keyDown(within(screen.getByTestId("tail-a")).getByLabelText("Dòng cuối"), { key: "Enter" });
  expect((latest as DocumentModel | null)!.blocks.map((b) => b.type)).toEqual(["HEADING", "PARAGRAPH", "DIAGRAM", "PARAGRAPH"]);
  fireEvent.mouseDown(screen.getByTestId("block-t"));
  fireEvent.click(screen.getByTestId("add-block"));
  const blocks = (latest as DocumentModel | null)!.blocks;
  expect(blocks.map((b) => b.type)).toEqual(["HEADING", "PARAGRAPH", "HEADING", "DIAGRAM", "PARAGRAPH"]);
  expect(blocks[3]!.type === "DIAGRAM" && blocks[3]!.content.under).toBe(1);
  // Sơ đồ vẫn trong ô văn bản của A (sau mục con), không vào mục con mới.
  const child = document.querySelector<HTMLElement>(`[data-section="${blocks[2]!.id}"]`)!;
  expect(child.querySelector('[data-testid="block-d"]')).toBeNull();
  expect(document.querySelector('[data-section="a"] [data-testid="block-d"]')).not.toBeNull();
});

it("đang cuộn tới mục 3 mà chuyển sang Từng phần thì mở đúng trang của mục 3", () => {
  const doc: DocumentModel = { blocks: ["a", "b", "c"].map((id) => ({ id, origin: "TEACHER" as const, type: "HEADING" as const, content: { style: "H1" as const, text: id.toUpperCase() } })) };
  const tops: Record<string, number> = { a: -900, b: -400, c: 120 };
  const rect = vi.spyOn(HTMLElement.prototype, "getBoundingClientRect").mockImplementation(function (this: HTMLElement) {
    const top = tops[this.dataset.section ?? ""] ?? 0;
    return { top, bottom: top + 100, left: 0, right: 0, width: 0, height: 100, x: 0, y: top, toJSON: () => ({}) };
  });
  render(<DocumentEditor document={doc} onChange={() => {}} mode="author" diagramEnabled={false} />);
  fireEvent.click(screen.getByRole("radio", { name: "Từng phần" }));
  rect.mockRestore();
  expect(screen.getByTestId("step-nav")).toHaveTextContent("3 / 3");
  expect(document.querySelector('[data-section="c"]')).not.toBeNull();
});
