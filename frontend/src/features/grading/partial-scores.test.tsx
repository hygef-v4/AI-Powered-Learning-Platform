import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { RubricChecklistPanel } from "./RubricChecklistPanel";
import type { Checks, ItemScores } from "./types";

const rubrics = [{ rubricId: "r", partId: "q1", maxPoints: 1, criteria: [{ criterionId: "c", title: "Thiết kế",
  items: [{ itemId: "i", text: "Quan hệ đúng", points: 1 }] }] }];
function Editor() {
  const [checks, setChecks] = useState<Checks>({});
  const [scores, setScores] = useState<ItemScores>({});
  return <RubricChecklistPanel rubrics={rubrics} checks={checks} onChange={setChecks} itemScores={scores} onScoresChange={setScores} />;
}
describe("partial rubric points", () => {
  it("accepts half a point and awards full credit via the accessible Đủ điểm button", () => {
    render(<Editor />);
    const input = screen.getByRole("spinbutton", { name: "Điểm: Quan hệ đúng" });
    const fullBtn = screen.getByRole("button", { name: "Đủ điểm: Quan hệ đúng" });
    expect(input).toHaveAttribute("max", "1");
    expect(fullBtn).toHaveAttribute("aria-pressed", "false");
    fireEvent.change(input, { target: { value: "0.5" } });
    expect(input).toHaveValue(0.5);
    expect(screen.getByText("0.50 / 1.00 điểm")).toBeInTheDocument();
    expect(fullBtn).toHaveAttribute("aria-pressed", "false");
    fireEvent.click(fullBtn);
    expect(input).toHaveValue(1);
    expect(fullBtn).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByText("1.00 / 1.00 điểm")).toBeInTheDocument();
    fireEvent.change(input, { target: { value: "0" } });
    expect(input).toHaveValue(0);
    expect(fullBtn).toHaveAttribute("aria-pressed", "false");
  });
  it("preserves fractional scores and live subtotal when collapsing and expanding a rubric section", () => {
    render(<Editor />);
    const input = screen.getByRole("spinbutton", { name: "Điểm: Quan hệ đúng" });
    fireEvent.change(input, { target: { value: "0.5" } });
    const toggle = screen.getByRole("button", { name: /Phần 1/ });
    expect(toggle).toHaveAttribute("aria-expanded", "true");
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute("aria-expanded", "false");
    expect(screen.getByText("0.50 / 1.00 điểm")).toBeInTheDocument();
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByRole("spinbutton", { name: "Điểm: Quan hệ đúng" })).toHaveValue(0.5);
  });
  it("shows an error indicator when a section with an invalid score is collapsed and allows opening to focus it", () => {
    render(<Editor />);
    const input = screen.getByRole("spinbutton", { name: "Điểm: Quan hệ đúng" });
    fireEvent.change(input, { target: { value: "1.5" } });
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByRole("alert")).toHaveTextContent("0 đến 1");
    const toggle = screen.getByRole("button", { name: /Phần 1/ });
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute("aria-expanded", "false");
    const openErrorBtn = screen.getByRole("button", { name: /Mở mục có điểm chưa hợp lệ/ });
    fireEvent.click(openErrorBtn);
    expect(toggle).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByRole("spinbutton", { name: "Điểm: Quan hệ đúng" })).toHaveFocus();
  });
  it("disables score input and full-credit button in readonly mode", () => {
    render(<RubricChecklistPanel rubrics={rubrics} checks={{}} onChange={() => {}} disabled />);
    expect(screen.getByRole("spinbutton", { name: "Điểm: Quan hệ đúng" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Đủ điểm: Quan hệ đúng" })).toBeDisabled();
  });
});
