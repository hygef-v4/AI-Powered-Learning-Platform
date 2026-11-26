import { render, screen } from "@testing-library/react";
import { it, vi } from "vitest";
import { DocumentEditor } from "@/shared/document/DocumentEditor";
it("root only", () => {
  const cases = {
    teacherPara: { blocks: [{ id: "p1", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Đề bài" }] } }] },
    empty: { blocks: [] },
  } as any;
  for (const [name, d] of Object.entries(cases)) {
    for (const g of [true, false]) {
      const { container, unmount } = render(<DocumentEditor document={d as any} onChange={vi.fn()} mode="student" diagramEnabled answerGuidance={g} />);
      console.log("CASE", name, "guided", g, "answer:", !!screen.queryByTestId("answer-DOCUMENT"), "write:", !!screen.queryByTestId("write-part-DOCUMENT"), "textboxes:", container.querySelectorAll("[contenteditable=true]").length);
      unmount();
    }
    const r = render(<DocumentEditor document={d as any} onChange={vi.fn()} mode="student" diagramEnabled answerGuidance writableParts={new Set(["DOCUMENT"]) as any} />);
    console.log("CASE", name, "writableParts DOCUMENT answer:", !!screen.queryByTestId("answer-DOCUMENT"), "write:", !!screen.queryByTestId("write-part-DOCUMENT"));
    r.unmount();
  }
});
