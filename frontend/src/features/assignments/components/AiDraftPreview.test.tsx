import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AiDraftPreview } from "./AiDraftPreview";

describe("AI assessment preview", () => {
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
});
