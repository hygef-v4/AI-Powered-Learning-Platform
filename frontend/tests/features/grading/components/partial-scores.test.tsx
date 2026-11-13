import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { RubricChecklistPanel } from "@/features/grading/components/RubricChecklistPanel";
import { buildRubricValues } from "@/features/grading/model/rubricScores";
import type { Checks, ItemScores } from "@/features/grading/model/types";

const rubrics = [{ rubricId: "r", partId: "q1", maxPoints: 1, criteria: [{ criterionId: "c", title: "Thiết kế",
  items: [{ itemId: "i", text: "Quan hệ đúng", points: 1 }] }] }];
function Editor({ scores: initial = {} }: { scores?: ItemScores }) {
  const [checks, setChecks] = useState<Checks>({});
  const [scores, setScores] = useState<ItemScores>(initial);
  return <>
    <RubricChecklistPanel rubrics={rubrics} checks={checks} onChange={setChecks} itemScores={scores} onScoresChange={setScores} />
    <output data-testid="scores">{JSON.stringify(scores)}</output>
  </>;
}
describe("rubric checklist (BR-U09-20)", () => {
  it("ticking gives the item its full points and unticking gives zero", () => {
    render(<Editor />);
    const item = screen.getByRole("checkbox", { name: /Quan hệ đúng/ });
    expect(screen.getAllByRole("spinbutton").length).toBeGreaterThan(0);
    expect(screen.queryByRole("button", { name: /Đủ điểm/ })).not.toBeInTheDocument();
    fireEvent.click(item);
    expect(item).toBeChecked();
    expect(screen.getByText("1.00 / 1.00 điểm")).toBeInTheDocument();
    expect(screen.getByTestId("scores")).toHaveTextContent('{"r":{"i":1}}');
    fireEvent.click(item);
    expect(item).not.toBeChecked();
    expect(screen.getByText("0.00 / 1.00 điểm")).toBeInTheDocument();
    expect(screen.getByTestId("scores")).toHaveTextContent('{"r":{"i":0}}');
  });
  it("allows a partial score and clears the full-credit checkbox", () => {
    render(<Editor />);
    fireEvent.change(screen.getByRole("spinbutton", { name: "Điểm mục Quan hệ đúng" }), { target: { value: "0.5" } });
    expect(screen.getByTestId("scores")).toHaveTextContent('{"r":{"i":0.5}}');
    expect(screen.getByRole("checkbox", { name: /Quan hệ đúng/ })).not.toBeChecked();
  });
  it("preserves saved or AI partial scores while deriving full-score checkboxes", () => {
    const two = [...rubrics, { rubricId: "r2", partId: "q2", maxPoints: 2, criteria: [{ criterionId: "c2", title: "", items: [
      { itemId: "a", text: "A", points: 1 }, { itemId: "b", text: "B", points: 1 }] }] }];
    expect(buildRubricValues(two, { r2: ["b"] }, { r: { i: 0.5 }, r2: { a: 1 } })).toEqual({
      checks: { r: [], r2: ["a", "b"] },
      itemScores: { r: { i: 0.5 }, r2: { a: 1, b: 1 } },
    });
  });
  it("disables the checklist in readonly mode", () => {
    render(<RubricChecklistPanel rubrics={rubrics} checks={{}} onChange={() => {}} disabled />);
    expect(screen.getByRole("checkbox", { name: /Quan hệ đúng/ })).toBeDisabled();
  });
});
