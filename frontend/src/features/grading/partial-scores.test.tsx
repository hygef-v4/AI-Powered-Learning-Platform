import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { RubricChecklistPanel } from "./RubricChecklistPanel";
import type { Checks, ItemScores } from "./types";

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
    expect(screen.queryByRole("spinbutton")).not.toBeInTheDocument();
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
  it("shows a saved fractional point as a note until the teacher ticks the item", () => {
    render(<Editor scores={{ r: { i: 0.5 } }} />);
    const item = screen.getByRole("checkbox", { name: /Quan hệ đúng/ });
    expect(item).not.toBeChecked();
    expect(screen.getByText("0.50 / 1.00 điểm")).toBeInTheDocument();
    expect(screen.getByText(/Điểm lẻ đã lưu: 0.50 \/ 1.00/)).toBeInTheDocument();
    fireEvent.click(item);
    expect(screen.queryByText(/Điểm lẻ đã lưu/)).not.toBeInTheDocument();
    expect(screen.getByText("1.00 / 1.00 điểm")).toBeInTheDocument();
  });
  it("disables the checklist in readonly mode", () => {
    render(<RubricChecklistPanel rubrics={rubrics} checks={{}} onChange={() => {}} disabled />);
    expect(screen.getByRole("checkbox", { name: /Quan hệ đúng/ })).toBeDisabled();
  });
});
