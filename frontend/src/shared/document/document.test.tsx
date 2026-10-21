import { fireEvent, render, screen, within } from "@testing-library/react";
import { useRef } from "react";
import { describe, expect, it, vi } from "vitest";
import { CommentRail } from "./CommentRail";
import { decodeSvgDataUri } from "./DiagramEditor";
import { DocumentEditor, convertBlock } from "./DocumentEditor";
import { DocumentViewer } from "./DocumentViewer";
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

  it("người soạn đổi đoạn thành tiêu đề và chèn sơ đồ theo loại từ thanh công cụ", () => {
    const onChange = vi.fn();
    const { rerender } = render(<DocumentEditor document={{ blocks: [] }} onChange={onChange} mode="author" diagramEnabled />);
    fireEvent.click(screen.getByLabelText("Chèn sơ đồ Draw.io"));
    fireEvent.click(screen.getByTestId("add-DIAGRAM-USE_CASE"));
    expect((onChange.mock.calls[0]![0] as DocumentModel).blocks[0]!).toMatchObject({ origin: "TEACHER", type: "DIAGRAM", content: { diagramType: "USE_CASE" } });
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
    fireEvent.change(screen.getByLabelText("Đoạn văn"), { target: { value: "Đề mới" } });
    const next = onChange.mock.calls[0]![0] as DocumentModel;
    expect(next.blocks[1]!.content).toEqual({ runs: [{ text: "Đề mới", bold: true }] });
  });

  it("chỉ đọc thì không có thanh thêm khối", () => {
    render(<DocumentEditor document={doc} onChange={vi.fn()} mode="author" diagramEnabled readOnly />);
    expect(screen.queryByTestId("add-PARAGRAPH")).toBeNull();
  });

  it("Enter tách đoạn tại con trỏ, đoạn mới giữ định dạng", () => {
    const onChange = vi.fn();
    render(<DocumentEditor document={doc} onChange={onChange} mode="author" diagramEnabled={false} />);
    const field = screen.getByLabelText("Đoạn văn") as HTMLTextAreaElement;
    field.setSelectionRange(2, 2);
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

  it("người học chỉ viết trong phần lá; nhánh tô nền theo độ sâu, lá nền trắng", () => {
    const onChange = vi.fn();
    const tree: DocumentModel = {
      blocks: [
        { id: "intro", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Phần chung" }] } },
        { id: "a", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Phân tích" } },
        { id: "a1", origin: "TEACHER", type: "HEADING", content: { style: "H2", text: "Use case" } },
        { id: "p1", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Vẽ use case" }] } },
      ],
    };
    const { container } = render(<DocumentEditor document={tree} onChange={onChange} mode="student" diagramEnabled />);
    const branch = container.querySelector<HTMLElement>('[data-section="a"]')!;
    const leaf = container.querySelector<HTMLElement>('[data-section="a1"]')!;
    expect(branch.style.backgroundColor).not.toBe("");
    expect(branch.contains(leaf)).toBe(true);
    expect(leaf).toHaveAttribute("data-leaf");
    expect(leaf).toHaveClass("bg-white");
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
    expect(within(screen.getByTestId("block-slot")).getByTitle("Bấm để vẽ")).toBeEnabled();
    expect(within(screen.getByTestId("block-common")).getByRole("button")).toBeDisabled();
  });

  it("bảng: rê chuột vào ô có ô vuông chèn hàng dưới, cột phải", () => {
    const onChange = vi.fn();
    const table: DocumentModel = { blocks: [{ id: "tb", origin: "TEACHER", type: "TABLE", content: { rows: [[{ runs: [{ text: "a" }] }]] } }] };
    render(<DocumentEditor document={table} onChange={onChange} mode="author" diagramEnabled />);
    fireEvent.click(screen.getByLabelText("Chèn hàng bên dưới"));
    expect((onChange.mock.calls[0]![0] as DocumentModel).blocks[0]!.content).toMatchObject({ rows: [[{ runs: [{ text: "a" }] }], [{ runs: [{ text: "" }] }]] });
    fireEvent.click(screen.getByLabelText("Chèn cột bên phải"));
    expect((onChange.mock.calls[1]![0] as DocumentModel).blocks[0]!.content).toMatchObject({ rows: [[{ runs: [{ text: "a" }] }, { runs: [{ text: "" }] }]] });
    expect(screen.queryByText("Hàng đầu là tiêu đề")).toBeNull();
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
