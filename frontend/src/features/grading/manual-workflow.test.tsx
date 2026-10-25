import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { RubricChecklistPanel } from "./RubricChecklistPanel";
import { SubmissionViewer } from "./SubmissionViewer";
import { AiProposalPanel } from "./AiProposalPanel";
import type { Rubric } from "./types";

describe("grading review", () => {
  it("renders current document runs, alignment, list levels and table cells without printing metadata", () => {
    const { container } = render(<SubmissionViewer document={{ blocks: [
      { id: "p", type: "PARAGRAPH", content: { align: "CENTER", runs: [{ text: "Bài làm", color: "#ff0000", bold: true }] } },
      { id: "l", type: "LIST", content: { ordered: true, items: [{ level: 2, runs: [{ text: "Mục con", italic: true }] }] } },
      { id: "t", type: "TABLE", content: { headerRow: true, rows: [[{ runs: [{ text: "Ô bảng", highlight: "#00ff00" }] }]] } },
    ] }} />);
    expect(screen.getByText("Bài làm")).toHaveStyle({ color: "#ff0000" });
    expect(screen.getByText("Bài làm")).toHaveClass("font-bold");
    expect(screen.getByText("Bài làm").closest("p")).toHaveClass("text-center");
    expect(screen.getByRole("listitem")).toHaveTextContent(/^Mục con$/);
    expect(screen.getByRole("listitem")).toHaveStyle({ marginLeft: "48px" });
    expect(screen.getByRole("columnheader")).toHaveTextContent(/^Ô bảng$/);
    expect(screen.getByText("Ô bảng")).toHaveStyle({ backgroundColor: "#00ff00" });
    expect(container).not.toHaveTextContent(/CENTER|#ff0000|#00ff00/);
  });
  it("keeps a full-credit checklist shortcut and bounds the item score", () => {
    const onChange = vi.fn();
    const rubrics: Rubric[] = [
      {
        rubricId: "r",
        partId: "p",
        maxPoints: 10,
        criteria: [
          {
            criterionId: "c",
            title: "Nội dung",
            items: [{ itemId: "i", text: "Đạt yêu cầu", points: 4 }],
          },
        ],
      },
    ];
    render(<RubricChecklistPanel rubrics={rubrics} checks={{}} onChange={onChange} />);
    fireEvent.click(screen.getByRole("button", { name: "Đủ điểm: Đạt yêu cầu" }));
    expect(onChange).toHaveBeenCalledWith({ r: ["i"] });
    expect(screen.getByRole("spinbutton", { name: "Điểm: Đạt yêu cầu" })).toHaveAttribute("max", "4");
  });
  it("renders untrusted document text as text, never executable markup", () => {
    const { container } = render(
      <SubmissionViewer
        document={{
          blocks: [
            { id: "b", type: "PARAGRAPH", content: { text: '<img src=x onerror="alert(1)">' } },
          ],
        }}
      />,
    );
    expect(container.querySelector("img")).toBeNull();
    expect(screen.getByText(/onerror/)).toBeInTheDocument();
  });
  it("a ready proposal only prefills form and stale proposals cannot be used", () => {
    const apply = vi.fn();
    render(
      <AiProposalPanel
        evaluationId="e"
        proposals={[
          {
            id: "p",
            versionNo: 1,
            status: "STALE",
            origin: "MCP",
            proposal: { checks: {}, score: "0", feedback: "f" },
          },
        ]}
        onApply={apply}
        onRequest={vi.fn()}
      />,
    );
    expect(screen.getByRole("button", { name: "Áp dụng vào bản nháp" })).toBeDisabled();
    expect(apply).not.toHaveBeenCalled();
  });
  it("preserves table cells and strips executable SVG preview content", () => {
    const { container } = render(
      <SubmissionViewer
        document={{
          blocks: [
            {
              id: "table",
              type: "TABLE",
              content: { headerRow: true, rows: [[[{ text: "Header" }]], [[{ text: "Answer" }]]] },
            },
            {
              id: "diagram",
              type: "DIAGRAM",
              content: {
                svgPreview:
                  '<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script><rect width="10" height="10" onclick="alert(2)"/><foreignObject><img src="https://bad.invalid"/></foreignObject></svg>',
              },
            },
          ],
        }}
      />,
    );
    expect(screen.getByRole("columnheader")).toHaveTextContent("Header");
    expect(screen.getByRole("cell")).toHaveTextContent("Answer");
    fireEvent.click(screen.getByRole("button", { name: "Xem sơ đồ" }));
    const image = container.querySelector("img")!;
    const source = decodeURIComponent(image.getAttribute("src")!);
    expect(source).not.toMatch(/script|onclick|foreignObject|bad.invalid/);
  });
});

it("does not expose the evaluation UUID as visible or assistive proposal text", () => {
  const evaluationId = "b3e8472d-4144-4895-a453-1dd896a890ef";
  const { container } = render(
    <AiProposalPanel evaluationId={evaluationId} proposals={[]} onApply={vi.fn()} onRequest={vi.fn()} />,
  );
  expect(container.textContent).not.toContain(evaluationId);
});


describe("U09 usability", () => {
  const rubric: Rubric = { rubricId: "rubric", partId: "08a88889-de7c-4eef-a90c-7ee32c19e312", maxPoints: 3,
    criteria: [{ criterionId: "criterion", title: "Kiến trúc", items: [{ itemId: "item", text: "Tách service", points: 1.5 }] }] };
  it("uses section titles and shows the selected checklist subtotal without exposing the part UUID", () => {
    const { container } = render(<RubricChecklistPanel rubrics={[rubric]} checks={{ rubric: ["item"] }} onChange={vi.fn()}
      partTitles={{ [rubric.partId]: "Câu 1: Class Diagram" }} />);
    expect(screen.getByText("Câu 1: Class Diagram")).toBeInTheDocument();
    expect(screen.getByText("1.50 / 3.00 điểm")).toBeInTheDocument();
    expect(container.textContent).not.toContain(rubric.partId);
  });
  it("explains AI failure and keeps a manual grading path visible", () => {
    render(<AiProposalPanel evaluationId="e" proposals={[{ id: "p", versionNo: 1, origin: "AI_TASK", status: "FAILED",
      safeError: "Nhà cung cấp AI từ chối yêu cầu (HTTP 400): model hoặc tham số không hợp lệ" }]} onApply={vi.fn()} onRequest={vi.fn()} />);
    expect(screen.getByText("Không tạo được đề xuất")).toBeInTheDocument();
    expect(screen.getByText(/vẫn có thể chấm thủ công/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Kiểm tra kết nối AI" })).toHaveAttribute("href", "/ai/connection");
  });
  it("shows a safe diagram preview immediately and opens a larger read-only view", () => {
    render(<SubmissionViewer document={{ blocks: [{ id: "d", type: "DIAGRAM", content: { alt: "Sơ đồ Event",
      svgPreview: '<svg xmlns="http://www.w3.org/2000/svg"><text x="5" y="20">Event</text></svg>' } }] }} />);
    expect(screen.getByRole("img", { name: "Sơ đồ Event" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Xem sơ đồ" }));
    expect(screen.getByRole("dialog", { name: "Sơ đồ Event" })).toBeInTheDocument();
  });
});


it("preserves Draw.io fallback labels while removing embedded HTML and scripts", () => {
  render(<SubmissionViewer document={{ blocks: [{ id: "d", type: "DIAGRAM", content: {
    svgPreview: '<svg xmlns="http://www.w3.org/2000/svg"><g><switch><foreignObject><div>HTML label</div></foreignObject><text x="0" y="20">- id: UUID</text></switch></g></svg>'
  } }] }} />);
  const svg = decodeURIComponent(screen.getByRole("img").getAttribute("src")!);
  expect(svg).toContain("- id: UUID");
  expect(svg).not.toContain("foreignObject");
});


it("refreshes the read-only diagram when a new snapshot supplies a changed SVG", () => {
  const doc = (text: string) => ({ blocks: [{ id: "d", type: "DIAGRAM", content: { svgPreview: `<svg xmlns="http://www.w3.org/2000/svg"><text>${text}</text></svg>` } }] });
  const { rerender } = render(<SubmissionViewer document={doc("Old diagram")} />);
  rerender(<SubmissionViewer document={doc("New diagram")} />);
  expect(decodeURIComponent(screen.getByRole("img").getAttribute("src")!)).toContain("New diagram");
});
