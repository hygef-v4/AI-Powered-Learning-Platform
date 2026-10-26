import { fireEvent, render, screen, within } from "@testing-library/react";
import { useRef } from "react";
import { describe, expect, it, vi } from "vitest";
import { CommentRail } from "./CommentRail";
import { decodeSvgDataUri } from "./DiagramEditor";
import { DocumentEditor, convertBlock } from "./DocumentEditor";
import { DocumentViewer } from "./DocumentViewer";
import { readRuns, runsHtml, setCaret } from "./RichText";
import { colorRange, joinLines, runLines, splitRuns, toggleFlagRange } from "./runs";
import { RubricNote, RubricViewer } from "./RubricViewer";
import {
  atomicItems,
  computeParts,
  criteriaOf,
  rubricComplete,
  rubricTotal,
  sectionsOf,
  type DocumentBlock,
  type DocumentModel,
  type RubricCriterion,
} from "./types";

const doc: DocumentModel = {
  blocks: [
    { id: "t1", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Phân tích" } },
    { id: "t2", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Đề bài", bold: true }] } },
  ],
};

describe("DocumentEditor", () => {
  it("workspace: answer actions insert into their own section regardless of current focus", () => {
    const second = { id: "q2", origin: "TEACHER" as const, type: "HEADING" as const, content: { style: "H1" as const, text: "Câu 2" } };
    const content = { blocks: [...doc.blocks, second] };
    const onChange = vi.fn();
    const { rerender } = render(<DocumentEditor document={content} onChange={onChange} mode="student" diagramEnabled answerGuidance />);
    expect(screen.getAllByText("Bài làm của bạn")).toHaveLength(2);
    fireEvent.mouseDown(screen.getByTestId("block-q2"));
    fireEvent.click(within(screen.getByTestId("answer-t1")).getByRole("button", { name: "Thêm sơ đồ cho câu này" }));
    const next = onChange.mock.calls[0]![0] as DocumentModel;
    expect(next.blocks.map((b) => b.id).at(-1)).toBe("q2");
    expect(next.blocks[2]).toMatchObject({ origin: "STUDENT", type: "DIAGRAM" });
    rerender(<DocumentEditor document={next} onChange={onChange} mode="student" diagramEnabled answerGuidance />);
    expect(screen.getByTestId("drawio-panel")).toBeInTheDocument();
    // A diagram-only answer still offers an explicit way to add explanation text.
    expect(within(screen.getByTestId("answer-t1")).getByTestId("write-part-t1")).toHaveTextContent("Bấm để bắt đầu viết bài làm");
  });

  it("workspace: guidance is opt-in and cannot enable editing for read-only or author mode", () => {
    const onChange = vi.fn();
    const { rerender } = render(<DocumentEditor document={doc} onChange={onChange} mode="student" diagramEnabled answerGuidance readOnly />);
    expect(screen.queryByRole("button", { name: "Thêm sơ đồ cho câu này" })).toBeNull();
    expect(screen.queryByTestId("write-part-t1")).toBeNull();
    rerender(<DocumentEditor document={doc} onChange={onChange} mode="author" diagramEnabled answerGuidance />);
    expect(screen.queryByText("Bài làm của bạn")).toBeNull();
    rerender(<DocumentEditor document={doc} onChange={onChange} mode="student" diagramEnabled={false} answerGuidance />);
    expect(screen.queryByRole("button", { name: "Thêm sơ đồ cho câu này" })).toBeNull();
    fireEvent.click(screen.getByTestId("write-part-t1"));
    const next = onChange.mock.calls[0]![0] as DocumentModel;
    expect(next.blocks[2]).toMatchObject({ origin: "STUDENT", type: "PARAGRAPH" });
    rerender(<DocumentEditor document={next} onChange={onChange} mode="student" diagramEnabled={false} answerGuidance />);
    const field = screen.getByRole("textbox", { name: "Đoạn văn" });
    expect(field).toHaveAttribute("aria-placeholder", "Nhập bài làm của bạn tại đây…");
    expect(field).toHaveTextContent("");
    expect(next.blocks[2]).toMatchObject({ content: { runs: [{ text: "" }] } });
  });

  it("người học: chữ của giảng viên nhạt, không có nút khóa hay thao tác; viết vào phần trống", () => {
    const onChange = vi.fn();
    render(<DocumentEditor document={doc} onChange={onChange} mode="student" diagramEnabled={false} />);
    const teacherBlock = screen.getByTestId("block-t2");
    expect(teacherBlock).toHaveAttribute("title", "Đề của giảng viên, không sửa được");
    expect(teacherBlock).toHaveClass("opacity-70");
    expect(screen.queryByText(/Không thể chỉnh sửa/)).toBeNull();
    expect(within(teacherBlock).queryByLabelText("Thao tác khối")).toBeNull();
    expect(screen.queryByLabelText("Kiểu đoạn")).toBeNull();
    fireEvent.click(screen.getByTestId("write-part-t1"));
    const next = onChange.mock.calls[0]![0] as DocumentModel;
    expect(next.blocks).toHaveLength(3);
    expect(next.blocks[2]!).toMatchObject({ origin: "STUDENT", type: "PARAGRAPH" });
  });

  it("người soạn đổi đoạn thành tiêu đề; một nút chèn sơ đồ, mở luôn Draw.io ở cột phải đẩy trang sang trái", () => {
    const onChange = vi.fn();
    const { rerender } = render(<DocumentEditor document={{ blocks: [] }} onChange={onChange} mode="author" diagramEnabled />);
    fireEvent.click(screen.getByTestId("add-DIAGRAM"));
    const inserted = onChange.mock.calls[0]![0] as DocumentModel;
    expect(inserted.blocks[0]!).toMatchObject({ origin: "TEACHER", type: "DIAGRAM", content: { diagramType: "OTHER" } });
    rerender(<DocumentEditor document={inserted} onChange={onChange} mode="author" diagramEnabled />);
    expect(screen.getByTestId("drawio-panel")).toBeInTheDocument();
    expect(document.documentElement).toHaveClass("drawio-panel-open");
    expect(screen.queryByRole("dialog", { name: /Draw.io/ })).toHaveAttribute("data-testid", "drawio-panel");
    fireEvent.click(screen.getByRole("button", { name: /Đóng/ }));
    expect(screen.queryByTestId("drawio-panel")).toBeNull();
    expect(document.documentElement).not.toHaveClass("drawio-panel-open");
    rerender(<DocumentEditor document={doc} onChange={onChange} mode="author" diagramEnabled />);
    fireEvent.mouseDown(screen.getByTestId("block-t2"));
    fireEvent.change(screen.getByLabelText("Kiểu đoạn"), { target: { value: "H2" } });
    expect((onChange.mock.calls.at(-1)![0] as DocumentModel).blocks[1]!).toMatchObject({ type: "HEADING", content: { style: "H2", text: "Đề bài" } });
  });

  it("thanh công cụ: gạch ngang, màu chữ, căn giữa áp cho cả đoạn; hoàn tác trả lại bản trước", () => {
    const onChange = vi.fn();
    render(<DocumentEditor document={doc} onChange={onChange} mode="author" diagramEnabled />);
    fireEvent.mouseDown(screen.getByTestId("block-t2"));
    fireEvent.click(screen.getByLabelText("Gạch ngang"));
    expect((onChange.mock.calls[0]![0] as DocumentModel).blocks[1]!.content).toEqual({ runs: [{ text: "Đề bài", bold: true, strike: true }] });
    fireEvent.click(screen.getByLabelText("Màu chữ"));
    fireEvent.click(screen.getByLabelText("#C5221F"));
    expect((onChange.mock.calls[1]![0] as DocumentModel).blocks[1]!.content).toEqual({ runs: [{ text: "Đề bài", bold: true, color: "#C5221F" }] });
    fireEvent.click(screen.getByLabelText("Căn giữa"));
    expect((onChange.mock.calls[2]![0] as DocumentModel).blocks[1]!.content).toMatchObject({ align: "CENTER" });
    fireEvent.click(screen.getByLabelText("Hoàn tác"));
    expect(onChange.mock.calls[3]![0]).toEqual(doc);
  });

  it("sửa đoạn văn giữ định dạng của đoạn", () => {
    const onChange = vi.fn();
    render(<DocumentEditor document={doc} onChange={onChange} mode="author" diagramEnabled={false} />);
    const field = screen.getByLabelText("Đoạn văn");
    field.querySelector("span")!.textContent = "Đề mới";
    fireEvent.input(field);
    const next = onChange.mock.calls[0]![0] as DocumentModel;
    expect(next.blocks[1]!.content).toEqual({ runs: [{ text: "Đề mới", bold: true }] });
  });

  it("bôi đen rồi chỉnh thì chỉ đổi phần bôi đen; không còn nút mã, kiểu đoạn chỉ 4 tiêu đề", () => {
    const onChange = vi.fn();
    render(<DocumentEditor document={doc} onChange={onChange} mode="author" diagramEnabled={false} />);
    fireEvent.mouseDown(screen.getByTestId("block-t2"));
    const field = screen.getByLabelText("Đoạn văn");
    field.focus();
    setCaret(field, 0, 2);
    fireEvent.click(screen.getByLabelText("Nghiêng (Ctrl+I)"));
    expect((onChange.mock.calls[0]![0] as DocumentModel).blocks[1]!.content).toEqual({
      runs: [{ text: "Đề", bold: true, italic: true }, { text: " bài", bold: true }],
    });
    fireEvent.click(screen.getByLabelText("Màu chữ"));
    fireEvent.click(screen.getByLabelText("#C5221F"));
    expect((onChange.mock.calls[1]![0] as DocumentModel).blocks[1]!.content).toEqual({
      runs: [{ text: "Đề", bold: true, color: "#C5221F" }, { text: " bài", bold: true }],
    });
    expect(screen.queryByLabelText("Mã")).toBeNull();
    expect(within(screen.getByLabelText("Kiểu đoạn")).getAllByRole("option").map((o) => o.textContent))
      .toEqual(["Văn bản thường", "Tiêu đề 1", "Tiêu đề 2", "Tiêu đề 3", "Tiêu đề 4"]);
  });

  it("chỉ đọc thì không có thanh thêm khối", () => {
    render(<DocumentEditor document={doc} onChange={vi.fn()} mode="author" diagramEnabled readOnly />);
    expect(screen.queryByTestId("add-PARAGRAPH")).toBeNull();
  });

  it("Enter tách đoạn tại con trỏ, đoạn mới giữ định dạng", () => {
    const onChange = vi.fn();
    render(<DocumentEditor document={doc} onChange={onChange} mode="author" diagramEnabled={false} />);
    const field = screen.getByLabelText("Đoạn văn");
    setCaret(field, 2);
    fireEvent.keyDown(field, { key: "Enter" });
    const next = onChange.mock.calls[0]![0] as DocumentModel;
    expect(next.blocks).toHaveLength(3);
    expect(next.blocks[1]!.content).toEqual({ runs: [{ text: "Đề", bold: true }] });
    expect(next.blocks[2]!).toMatchObject({ origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: " bài", bold: true }] } });
  });

  it("Backspace ở đoạn trống thì xóa đoạn", () => {
    const onChange = vi.fn();
    const empty: DocumentModel = { blocks: [...doc.blocks, { id: "p3", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "" }] } }] };
    render(<DocumentEditor document={empty} onChange={onChange} mode="student" diagramEnabled={false} />);
    fireEvent.keyDown(screen.getByLabelText("Đoạn văn"), { key: "Backspace" });
    expect((onChange.mock.calls[0]![0] as DocumentModel).blocks.map((b) => b.id)).toEqual(["t1", "t2"]);
  });

  it("người học chỉ viết trong phần lá; cùng tầng cùng màu, lá khung màu ruột trắng", () => {
    const onChange = vi.fn();
    const tree: DocumentModel = {
      blocks: [
        { id: "intro", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Phần chung" }] } },
        { id: "a", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Phân tích" } },
        { id: "a1", origin: "TEACHER", type: "HEADING", content: { style: "H2", text: "Use case" } },
        { id: "p1", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Vẽ use case" }] } },
        { id: "b", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Thiết kế" } },
      ],
    };
    const { container } = render(<DocumentEditor document={tree} onChange={onChange} mode="student" diagramEnabled />);
    const branch = container.querySelector<HTMLElement>('[data-section="a"]')!;
    const leaf = container.querySelector<HTMLElement>('[data-section="a1"]')!;
    const sibling = container.querySelector<HTMLElement>('[data-section="b"]')!;
    expect(branch.style.backgroundColor).not.toBe("");
    expect(branch.contains(leaf)).toBe(true);
    expect(leaf).toHaveAttribute("data-leaf");
    expect(leaf.style.backgroundColor).not.toBe(branch.style.backgroundColor);
    expect(sibling.style.backgroundColor).toBe(branch.style.backgroundColor);
    expect(leaf.querySelector("[data-leaf-body]")).toHaveClass("bg-surface");
    expect(within(leaf.querySelector<HTMLElement>("[data-leaf-body]")!).getByTestId("block-p1")).toBeInTheDocument();
    expect(screen.getByTestId("write-part-a1").textContent).toBe("");
    fireEvent.mouseDown(screen.getByTestId("block-intro"));
    expect(screen.getByLabelText("Chèn bảng")).toBeDisabled();
    fireEvent.click(screen.getByTestId("write-part-a1"));
    const next = onChange.mock.calls[0]![0] as DocumentModel;
    expect(next.blocks.map((b) => b.id).slice(0, 4)).toEqual(["intro", "a", "a1", "p1"]);
    expect(next.blocks[4]!).toMatchObject({ origin: "STUDENT", type: "PARAGRAPH" });
  });

  it("người học bấm thẳng vào sơ đồ của giảng viên trong phần để vẽ; sơ đồ ở phần chung thì không bấm được", () => {
    const diagram = (id: string): DocumentBlock => ({ id, origin: "TEACHER", type: "DIAGRAM", content: { diagramType: "CLASS", xml: "<mxfile/>" } });
    const tree: DocumentModel = {
      blocks: [diagram("common"), { id: "h", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Thiết kế" } }, diagram("slot")],
    };
    render(<DocumentEditor document={tree} onChange={vi.fn()} mode="student" diagramEnabled />);
    expect(within(screen.getByTestId("block-slot")).getByRole("button", { name: "Vẽ sơ đồ" })).toBeEnabled();
    expect(within(screen.getByTestId("block-common")).getByRole("button")).toBeDisabled();
    // Sơ đồ của đề trong phần: nút ✕ vẫn có nhưng mờ; ở phần chung thì không có.
    expect(within(screen.getByTestId("block-slot")).getByLabelText("Sơ đồ của đề")).toBeDisabled();
    expect(within(screen.getByTestId("block-common")).queryByLabelText("Sơ đồ của đề")).toBeNull();
  });

  it("bảng: ô vuông + − nằm ngoài bảng, của cột ở trên đầu, của hàng bên trái; ✕ ở góc cùng hàng với nút của cột", () => {
    const onChange = vi.fn();
    const table: DocumentModel = { blocks: [{ id: "tb", origin: "TEACHER", type: "TABLE", content: { rows: [[{ runs: [{ text: "a" }] }], [{ runs: [{ text: "b" }] }]] } }] };
    render(<DocumentEditor document={table} onChange={onChange} mode="author" diagramEnabled />);
    // Nút nổi đè lên chỗ xung quanh, không chiếm hàng, cột riêng của bảng.
    expect(screen.getByTestId("column-tools-0")).toHaveClass("absolute", "bottom-full");
    expect(screen.getByTestId("row-tools-0")).toHaveClass("absolute", "right-full");
    const corner = within(screen.getByTestId("block-tb")).getAllByLabelText("Xóa bảng").find((button) => button.textContent === "✕")!;
    expect(corner.parentElement).toHaveClass("absolute", "right-full", "bottom-full");
    expect(screen.getByLabelText("Ô 1-1").closest("tr")!.children).toHaveLength(1);
    expect(screen.queryByText("Hàng tiêu đề")).toBeNull();
    fireEvent.click(screen.getAllByLabelText("Chèn hàng bên dưới")[0]!);
    expect((onChange.mock.calls[0]![0] as DocumentModel).blocks[0]!.content).toMatchObject({ rows: [[{ runs: [{ text: "a" }] }], [{ runs: [{ text: "" }] }], [{ runs: [{ text: "b" }] }]] });
    fireEvent.click(screen.getByLabelText("Chèn cột bên phải"));
    expect((onChange.mock.calls[1]![0] as DocumentModel).blocks[0]!.content).toMatchObject({ rows: [[{ runs: [{ text: "a" }] }, { runs: [{ text: "" }] }], [{ runs: [{ text: "b" }] }, { runs: [{ text: "" }] }]] });
    fireEvent.click(screen.getAllByLabelText("Xóa hàng")[1]!);
    expect((onChange.mock.calls[2]![0] as DocumentModel).blocks[0]!.content).toMatchObject({ rows: [[{ runs: [{ text: "a" }] }]] });
    // Bảng còn một cột: "−" của cột là xóa cả bảng; nút ✕ ở góc trên trái cũng xóa bảng.
    fireEvent.click(within(screen.getByTestId("column-tools-0")).getByLabelText("Xóa bảng"));
    expect((onChange.mock.calls[3]![0] as DocumentModel).blocks).toEqual([]);
    fireEvent.click(corner);
    expect((onChange.mock.calls[4]![0] as DocumentModel).blocks).toEqual([]);
  });

  it("người học điền bảng của đề trong phần: sửa ô, thêm hàng, không xóa; bảng ở phần chung vẫn khóa", () => {
    const onChange = vi.fn();
    const table = (id: string): DocumentBlock => ({ id, origin: "TEACHER", type: "TABLE", content: { headerRow: true, rows: [[{ runs: [{ text: "Ca" }] }], [{ runs: [{ text: "" }] }]] } });
    const tree: DocumentModel = {
      blocks: [table("common"), { id: "h", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Kiểm thử" } }, table("slot")],
    };
    render(<DocumentEditor document={tree} onChange={onChange} mode="student" diagramEnabled />);
    expect(within(screen.getByTestId("block-common")).queryByRole("textbox")).toBeNull();
    const slot = screen.getByTestId("block-slot");
    expect(within(slot).queryByLabelText("Thao tác khối")).toBeNull();
    expect(within(slot).queryByLabelText("Xóa hàng")).toBeNull();
    fireEvent.change(within(slot).getByLabelText("Ô 2-1"), { target: { value: "Đăng nhập" } });
    expect((onChange.mock.calls[0]![0] as DocumentModel).blocks[2]!).toMatchObject({
      id: "slot", origin: "TEACHER", content: { headerRow: true, rows: [[{ runs: [{ text: "Ca" }] }], [{ runs: [{ text: "Đăng nhập" }] }]] },
    });
    expect(within(slot).queryByLabelText("Xóa bảng")).toBeNull();
    expect(within(slot).getByLabelText("Bảng của đề")).toBeDisabled();
    // Hàng, cột nào cũng có đủ nút; "−" của hàng, cột của đề bị khóa.
    expect(within(slot).getAllByLabelText("Chèn hàng bên dưới")).toHaveLength(2);
    expect(within(slot).getAllByLabelText("Hàng của đề")).toHaveLength(2);
    within(slot).getAllByLabelText("Hàng của đề").forEach((button) => expect(button).toBeDisabled());
    expect(within(slot).getByLabelText("Cột của đề")).toBeDisabled();
    fireEvent.click(within(slot).getAllByLabelText("Chèn hàng bên dưới")[0]!);
    const inserted = (onChange.mock.calls[1]![0] as DocumentModel).blocks[2]!.content as { rows: { added?: boolean }[][] };
    expect(inserted.rows).toHaveLength(3);
    expect(inserted.rows[1]!.every((cell) => cell.added)).toBe(true);
  });

  it("người học xóa được hàng, cột mình thêm (ô có cờ added), cả khi chèn giữa bảng của đề", () => {
    const onChange = vi.fn();
    const added = (text: string) => ({ runs: [{ text }], added: true });
    const filled: DocumentModel = { blocks: [{ id: "h", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Kiểm thử" } },
      { id: "slot", origin: "TEACHER", type: "TABLE", content: { rows: [[{ runs: [{ text: "Ca" }] }, added("ghi chú")], [added("thêm"), added("")], [{ runs: [{ text: "" }] }, added("")]] } }] };
    render(<DocumentEditor document={filled} onChange={onChange} mode="student" diagramEnabled />);
    expect(screen.getByLabelText("Xóa hàng")).toBeEnabled();
    expect(screen.getByLabelText("Xóa cột")).toBeEnabled();
    fireEvent.click(screen.getByLabelText("Xóa hàng"));
    expect(((onChange.mock.calls[0]![0] as DocumentModel).blocks[1]!.content as { rows: unknown[][] }).rows).toHaveLength(2);
    fireEvent.click(screen.getByLabelText("Xóa cột"));
    expect(((onChange.mock.calls[1]![0] as DocumentModel).blocks[1]!.content as { rows: unknown[][] }).rows.every((row) => row.length === 1)).toBe(true);
  });

  it("không còn menu ba chấm; sơ đồ trải hết bề ngang, có nút ✕ ở góc để xóa", () => {
    const onChange = vi.fn();
    const withDiagram: DocumentModel = { blocks: [...doc.blocks, { id: "d", origin: "TEACHER", type: "DIAGRAM", content: { diagramType: "OTHER", xml: "<mxfile/>" } }] };
    render(<DocumentEditor document={withDiagram} onChange={onChange} mode="author" diagramEnabled />);
    expect(screen.queryByLabelText("Thao tác khối")).toBeNull();
    const canvas = within(screen.getByTestId("block-d")).getByRole("button", { name: "Vẽ sơ đồ" });
    expect(canvas).toHaveClass("w-full");
    expect(canvas.parentElement!.parentElement).not.toHaveClass("items-center");
    fireEvent.click(within(screen.getByTestId("block-d")).getByLabelText("Xóa sơ đồ"));
    expect((onChange.mock.calls[0]![0] as DocumentModel).blocks.map((b) => b.id)).toEqual(["t1", "t2"]);
  });

  it("ô chọn cỡ bảng: lưới cột cố định, mỗi ô một ô vuông riêng", () => {
    const onChange = vi.fn();
    render(<DocumentEditor document={{ blocks: [] }} onChange={onChange} mode="author" diagramEnabled />);
    fireEvent.click(screen.getByLabelText("Chèn bảng"));
    const grid = screen.getByTestId("table-picker").firstElementChild!;
    expect(grid).toHaveClass("grid-cols-[repeat(10,18px)]");
    expect(screen.getByLabelText("3 × 4")).toHaveClass("size-[18px]");
    fireEvent.click(screen.getByLabelText("3 × 4"));
    const rows = ((onChange.mock.calls[0]![0] as DocumentModel).blocks[0]!.content as { rows: unknown[][] }).rows;
    expect(rows).toHaveLength(3);
    expect(rows[0]).toHaveLength(4);
  });

  it("dòng trống không có chữ gợi ý", () => {
    const blank: DocumentModel = { blocks: [
      { id: "h", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "" } },
      { id: "p", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "" }] } },
      { id: "l", origin: "TEACHER", type: "LIST", content: { ordered: false, items: [{ level: 0, runs: [{ text: "" }] }] } },
    ] };
    const { container } = render(<DocumentEditor document={blank} onChange={vi.fn()} mode="author" diagramEnabled />);
    expect(container.querySelectorAll("textarea[placeholder]")).toHaveLength(0);
  });

  it("chỉ đọc ở chế độ người học: phần chưa có bài làm hiện chữ báo", () => {
    render(<DocumentEditor document={doc} onChange={vi.fn()} mode="student" diagramEnabled readOnly emptyLeafLabel="Sinh viên viết ở đây" />);
    expect(screen.getByTestId("empty-part-t1")).toHaveTextContent("Sinh viên viết ở đây");
  });

  it("màn hẹp thì ghi chú nằm ngay dưới block của nó", () => {
    render(<DocumentEditor document={doc} onChange={vi.fn()} mode="author" diagramEnabled={false}
      comments={[{ key: "t1", anchorBlockId: "t1", content: <p>Rubric phân tích</p> }, { key: "x", anchorBlockId: "missing", content: <p>Mất neo</p> }]} />);
    expect(within(screen.getByTestId("block-t1")).getByText("Rubric phân tích")).toBeInTheDocument();
    expect(screen.queryByText("Mất neo")).toBeNull();
  });
});

describe("định dạng từng phần", () => {
  const runs = [{ text: "Xin chào", bold: true }, { text: " bạn" }];

  it("bôi đen thì chỉ bật, tắt hoặc tô màu phần đó; phần còn lại giữ định dạng", () => {
    expect(toggleFlagRange(runs, 4, 8, "italic")).toEqual([{ text: "Xin ", bold: true }, { text: "chào", bold: true, italic: true }, { text: " bạn" }]);
    expect(toggleFlagRange(runs, 0, 8, "bold")).toEqual([{ text: "Xin chào bạn" }]);
    expect(colorRange(runs, 8, 12, "color", "#C5221F")).toEqual([{ text: "Xin chào", bold: true }, { text: " bạn", color: "#C5221F" }]);
  });

  it("Enter tách đoạn giữ định dạng hai nửa; dòng ↔ danh sách giữ định dạng", () => {
    expect(splitRuns(runs, 4)).toEqual([[{ text: "Xin ", bold: true }], [{ text: "chào", bold: true }, { text: " bạn" }]]);
    expect(splitRuns(runs, 12)).toEqual([runs, [{ text: "" }]]);
    const lines = runLines([{ text: "Dòng 1\nDòng 2", italic: true }]);
    expect(lines).toEqual([[{ text: "Dòng 1", italic: true }], [{ text: "Dòng 2", italic: true }]]);
    expect(joinLines(lines)).toEqual([{ text: "Dòng 1\nDòng 2", italic: true }]);
  });

  it("ô chữ đọc lại đúng run vẽ ra, cả thẻ trình duyệt tự chèn khi xóa, gộp", () => {
    const root = document.createElement("div");
    root.innerHTML = runsHtml([{ text: "a<b>", bold: true }, { text: "c", color: "#C5221F", highlight: "#FFF475" }]);
    expect(readRuns(root)).toEqual([{ text: "a<b>", bold: true }, { text: "c", color: "#C5221F", highlight: "#FFF475" }]);
    root.innerHTML = '<b>đậm</b><span style="font-style: italic; color: rgb(197, 34, 31)">nghiêng</span>thường<br>';
    expect(readRuns(root)).toEqual([{ text: "đậm", bold: true }, { text: "nghiêng", italic: true, color: "#C5221F" }, { text: "thường" }]);
  });
});

describe("đổi kiểu block", () => {
  const paragraph: DocumentBlock = { id: "p", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Dòng 1\nDòng 2", italic: true }] } };

  it("đoạn thành tiêu đề, danh sách và ngược lại, giữ id và định dạng", () => {
    expect(convertBlock(paragraph, "H2")).toEqual({ id: "p", origin: "TEACHER", type: "HEADING", content: { style: "H2", text: "Dòng 1 Dòng 2" } });
    const list = convertBlock(paragraph, "OL");
    expect(list).toMatchObject({ type: "LIST", content: { ordered: true, items: [{ runs: [{ text: "Dòng 1", italic: true }] }, { runs: [{ text: "Dòng 2", italic: true }] }] } });
    expect(convertBlock(list, "P")).toEqual(paragraph);
  });
});

describe("phần và rubric", () => {
  const heading = (id: string, style: "H1" | "H2", text: string): DocumentBlock => ({ id, origin: "TEACHER", type: "HEADING", content: { style, text } });
  const tree: DocumentModel = {
    blocks: [
      heading("a", "H1", "Phân tích"),
      heading("a1", "H2", "Use case"),
      { id: "p1", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Vẽ use case" }] } },
      heading("a2", "H2", "Lớp"),
      heading("b", "H1", "Thiết kế"),
      { id: "p2", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Bài làm" }] } },
    ],
  };

  it("phần là tiêu đề lá của cây, giống backend", () => {
    const parts = computeParts(tree);
    expect(parts.map((p) => p.partId)).toEqual(["a1", "a2", "b"]);
    expect(parts[0]).toMatchObject({ title: "Phân tích › Use case", ancestorHeadingIds: ["a"] });
    expect(computeParts({ blocks: [] })).toEqual([{ partId: "DOCUMENT", headingBlockId: null, ancestorHeadingIds: [], title: "Toàn bài" }]);
  });

  it("block thuộc mục lá gần nhất phía trên", () => {
    const sections = sectionsOf(tree, computeParts(tree));
    expect(sections.get("p1")).toBe("a1");
    expect(sections.get("p2")).toBe("b");
    expect(sections.has("a")).toBe(false);
  });

  it("tổng điểm và rubric đủ để duyệt", () => {
    const criteria: RubricCriterion[] = [{ criterionId: "c", title: "Đúng ký hiệu", description: "", kind: "TEXT",
      items: [{ itemId: "i", text: "Actor", points: 1.5 }, { itemId: "j", text: "", points: null }] }];
    expect(rubricTotal(criteria)).toBe(1.5);
    expect(rubricComplete(criteria)).toBe(false);
    expect(rubricComplete([{ ...criteria[0]!, items: [criteria[0]!.items[0]!] }])).toBe(true);
  });

  it("thẻ rubric bên lề: tên mục, tổng điểm, các mục chấm; có trình sửa thì luôn hiện, không cần bấm", () => {
    const criteria: RubricCriterion[] = [{ criterionId: "c", title: "Có actor", description: "", kind: "TEXT", items: [{ itemId: "i", text: "Có actor", points: 2 }] }];
    const { rerender } = render(<RubricNote title="Phân tích › Use case" criteria={criteria} />);
    expect(screen.getByText("Use case")).toBeInTheDocument();
    expect(screen.getByText("2 điểm")).toBeInTheDocument();
    expect(screen.getByText("Có actor")).toHaveClass("truncate");
    rerender(<RubricNote title="Phân tích › Use case" criteria={criteria} editor={<p>Trình sửa</p>} />);
    expect(screen.getByText("Trình sửa")).toBeInTheDocument();
  });

  it("rubric atomic: tiêu chí nhiều mục tách mỗi mục một dòng; lưu mỗi dòng thành một tiêu chí", () => {
    const legacy: RubricCriterion[] = [{ criterionId: "c", title: "Ký hiệu", description: "", kind: "DIAGRAM",
      items: [{ itemId: "i", text: "Có actor", points: 1 }, { itemId: "j", text: "Có use case", points: 2 }] }];
    const items = atomicItems(legacy);
    expect(items.map((item) => item.text)).toEqual(["Ký hiệu: Có actor", "Ký hiệu: Có use case"]);
    const saved = criteriaOf(items);
    expect(saved).toHaveLength(2);
    expect(saved[0]!).toMatchObject({ title: "Ký hiệu: Có actor", kind: "TEXT", items: [{ itemId: "i", text: "Ký hiệu: Có actor", points: 1 }] });
    expect(rubricTotal(saved)).toBe(3);
    expect(rubricComplete(saved)).toBe(true);
    expect(criteriaOf([{ criterionId: null, itemId: null, text: "x".repeat(300), points: 1 }])[0]!.title).toHaveLength(200);
  });
});

describe("CommentRail", () => {
  it("thẻ nằm trong dải của phần và dính khi cuộn qua đầu phần", () => {
    function Rail() {
      const paper = useRef<HTMLDivElement>(null);
      return (
        <>
          <div ref={paper}><div data-block-id="h1">Mục 1</div><div data-block-id="h2">Mục 2</div></div>
          <CommentRail paper={paper} comments={[{ key: "h1", anchorBlockId: "h1", content: <p>Rubric 1</p> }, { key: "h2", anchorBlockId: "h2", content: <p>Rubric 2</p> }]} />
        </>
      );
    }
    render(<Rail />);
    const card = screen.getByText("Rubric 1").closest("[data-comment]");
    expect(card).toHaveClass("sticky");
    expect(card?.parentElement).toHaveAttribute("data-comment-band", "h1");
  });
});

describe("DocumentViewer và RubricViewer", () => {
  it("vẽ heading, đoạn đậm và bảng rubric có tổng", () => {
    render(<>
      <DocumentViewer document={doc} />
      <RubricViewer criteria={[{ criterionId: "c", title: "Nội dung", description: "", kind: "TEXT", items: [{ itemId: "i", text: "Đủ ý", points: 2.5 }] }]} total={2.5} />
    </>);
    expect(screen.getByText("Phân tích")).toBeInTheDocument();
    expect(screen.getByText("Đề bài")).toHaveClass("font-bold");
    expect(screen.getByText("Tổng")).toBeInTheDocument();
    expect(screen.getAllByText("2,5")).toHaveLength(2);
  });
});

describe("decodeSvgDataUri", () => {
  it("giải base64 có dấu tiếng Việt và từ chối loại khác", () => {
    const svg = "<svg><text>Sơ đồ</text></svg>";
    const base64 = btoa(String.fromCharCode(...new TextEncoder().encode(svg)));
    expect(decodeSvgDataUri(`data:image/svg+xml;base64,${base64}`)).toBe(svg);
    expect(decodeSvgDataUri(`data:image/svg+xml,${encodeURIComponent(svg)}`)).toBe(svg);
    expect(decodeSvgDataUri("data:image/png;base64,AAAA")).toBeNull();
  });
});
