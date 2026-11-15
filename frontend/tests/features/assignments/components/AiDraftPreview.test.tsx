import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AiDraftPreview } from "@/features/assignments/components/AiDraftPreview";

describe("AI assessment preview", () => {
  it("shows the server warning without treating an oversized rubric as invalid", () => {
    render(<AiDraftPreview targetPoints={12} proposal={{ title: "Exam", businessContext: "", instructions: "Answer",
      sections: [{ level: 1, title: "Design", guidance: "", rubric: [] }],
      validation: { status: "NEEDS_REVIEW", questionCount: 1, rubricLeafCount: 1, totalPoints: 12,
        issues: [{ severity: "WARNING", code: "AI_GRADING_RUBRIC_LIMIT", path: "sections.rubric",
          message: "Rubric vượt giới hạn 30 mục cho một lượt chấm AI. Bạn vẫn có thể chấm thủ công." }] } }} />);
    expect(screen.getByText(/vượt giới hạn 30 mục/)).toBeVisible();
    expect(screen.queryByText("Cần sửa trước khi áp dụng")).not.toBeInTheDocument();
    expect(screen.getByText("12.00 / 12 điểm")).toBeVisible();
  });
  it("keeps invalid empty output visible ahead of long collapsed prose", () => {
    const context = "Repeated context. ".repeat(300);
    render(<AiDraftPreview targetPoints={10} proposal={{ title: "Event exam", businessContext: context,
      instructions: "Work individually", sections: [], validation: { status: "INVALID", questionCount: 0,
        rubricLeafCount: 0, totalPoints: 0, issues: [] } }} />);
    expect(screen.getByText(/AI chưa tạo câu hỏi và rubric/)).toBeVisible();
    expect(screen.getByText(context.trim()).closest("details")).not.toHaveAttribute("open");
    expect(screen.getByText(/Không có câu hỏi để hiển thị/)).toBeVisible();
  });

  it("shows a separate question and rubric panel with item scores", () => {
    render(<AiDraftPreview targetPoints={10} proposal={{ title: "Event exam", businessContext: "Actors and rules.\n\nException flows.",
      instructions: "Work individually", sections: [{ level: 1, title: "Class diagram", guidance: "Model the event entities.",
        rubric: [{ title: "Relationships", description: "", kind: "DIAGRAM", items: [{ text: "Correct multiplicities", points: 10 }] }] }],
      validation: { status: "VALID", questionCount: 1, rubricLeafCount: 1, totalPoints: 10, issues: [] } }} />);
    expect(screen.getByRole("article", { name: "Câu 1" })).toBeVisible();
    expect(screen.getByRole("heading", { name: "Rubric · tiêu chí và điểm" })).toBeVisible();
    expect(screen.getByText("Correct multiplicities")).toBeVisible();
    expect(screen.getByText("Exception flows.")).toBeVisible();
  });
  it("previews native emphasis and alignment without dropping selective run styles", () => {
    render(<AiDraftPreview proposal={{ title: "Exam", instructions: "", businessContext: "Context",
      businessContextFormat: { align: "CENTER", bold: true },
      sections: [{ level: 1, title: "Diagram", titleFormat: { italic: true }, guidance: "Draw UML",
        guidanceFormat: { align: "RIGHT", runs: [{ text: "Draw ", bold: true }, { text: "UML", underline: true }] }, rubric: [] }] }} />);
    expect(screen.getByText("Context")).toHaveClass("font-bold");
    expect(screen.getByText("Context").closest("p")).toHaveClass("text-center");
    expect(screen.getByText("Diagram")).toHaveClass("italic");
    expect(screen.getByText("Draw")).toHaveClass("font-bold");
    expect(screen.getByText("UML")).toHaveStyle({ textDecorationLine: "underline" });
  });

});
