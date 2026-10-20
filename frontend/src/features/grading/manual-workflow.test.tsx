import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { RubricChecklistPanel } from "./RubricChecklistPanel";
import { SubmissionViewer } from "./SubmissionViewer";
import { AiProposalPanel } from "./AiProposalPanel";
import type { Rubric } from "./types";

describe("grading review", () => {
  it("edits only checklist IDs and never a free score", () => {
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
    fireEvent.click(screen.getByRole("checkbox"));
    expect(onChange).toHaveBeenCalledWith({ r: ["i"] });
    expect(screen.queryByRole("spinbutton")).not.toBeInTheDocument();
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
    expect(screen.getByRole("button", { name: "Dùng đề xuất" })).toBeDisabled();
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
