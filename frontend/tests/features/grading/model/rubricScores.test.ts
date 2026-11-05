import { expect, it } from "vitest";
import { checklistPoints, invalidItemScores, rubricValues } from "@/features/grading/model/rubricScores";

const rubrics = [{ rubricId: "r", partId: "q", maxPoints: 0.3, criteria: [{ criterionId: "c", title: "Accuracy", items: [
  { itemId: "a", text: "A", points: 0.1 }, { itemId: "b", text: "B", points: 0.2 }, { itemId: "zero", text: "Zero", points: 0 },
] }] }];

it("sums decimal points exactly and gives explicit partial scores precedence over legacy checks", () => {
  expect(checklistPoints(rubrics, { r: ["a", "b"] })).toBe(0.3);
  expect(checklistPoints(rubrics, { r: ["a", "b"] }, { r: { a: 0.05 } })).toBe(0.25);
  expect(rubricValues(rubrics, { r: ["zero"] }, { r: { zero: 0 } }).checks.r).toEqual([]);
});

it("rejects out-of-range and excessive-precision rubric marks", () => {
  expect(invalidItemScores(rubrics, { r: { a: 0.05 } })).toBe(false);
  for (const a of [-1, 0.11, 0.001, NaN]) expect(invalidItemScores(rubrics, { r: { a } })).toBe(true);
});
