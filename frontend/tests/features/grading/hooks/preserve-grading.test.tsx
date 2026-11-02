import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { useGradingWorkspace } from "@/features/grading/hooks/useGradingWorkspace";
import type { Detail, StructuredFeedback } from "@/features/grading/model/types";

const mocks = vi.hoisted(() => ({ save: vi.fn(), push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: mocks.push }) }));
vi.mock("@/features/grading/api", () => ({ gradingApi: mocks }));
vi.mock("@/features/grading/hooks/useGradingProposals", () => ({ useGradingProposals: () => ({ proposals: [], addProposal: vi.fn() }) }));

const initial: Detail = {
  evaluation: { id: "e", assignmentId: "a", classId: "c", attemptId: "t", accountId: "s", groupDocumentId: null,
    kind: "ATTEMPT", submissionHash: "hash", state: "DRAFT", score: 0.25, maxScore: 1, version: 2,
    rubricChecks: "{}", rubricItemScores: { r: { i: "0.50" } }, adjustment: "-0.25", adjustmentReason: "Late",
    feedback: "Previous feedback", publishedAt: null, changes: [] },
  rubrics: [{ rubricId: "r", partId: "part", maxPoints: 1, criteria: [{ criterionId: "criterion", title: "Design",
    items: [{ itemId: "i", text: "Correct design", points: 1 }] }] }],
  document: { blocks: [] }, navigationIds: ["e"], current: true, activity: null, activityState: "DISABLED",
  members: [], memberActivities: [], sectionAuthors: [],
};
const feedback: StructuredFeedback = { schemaVersion: 1, summary: { strengths: [], improvements: [], nextSteps: [] },
  items: [{ rubricId: "r", itemId: "i", evidence: null, reason: "Review diagram", suggestion: null,
    evidenceBlockId: null, requiresReview: true, reviewReason: "Unreadable diagram" }] };

beforeEach(() => { vi.clearAllMocks(); mocks.save.mockResolvedValue({ ...initial.evaluation, version: 3 }); });

it("preserves explicit partial points and adjustments on a feedback-only save", async () => {
  const { result } = renderHook(() => useGradingWorkspace(initial, async () => initial));
  act(() => result.current.updateForm({ feedback: "Updated feedback" }));
  await act(async () => { await result.current.save(); });
  expect(mocks.save).toHaveBeenCalledWith("e", expect.objectContaining({ itemScoresByRubric: { r: { i: "0.50" } },
    adjustment: "-0.25", adjustmentReason: "Late", feedback: "Updated feedback" }));
});

it("retains AI partial points, feedback and flags while resetting previous review acknowledgements", async () => {
  const source: Detail = { ...initial, evaluation: { ...initial.evaluation, structuredFeedback: feedback,
    reviewedItemScores: { r: { i: 1 } }, acknowledgedReviewItems: { r: ["i"] } } };
  const { result } = renderHook(() => useGradingWorkspace(source, async () => source));
  act(() => result.current.applyProposal({}, "AI feedback", "proposal", { r: { i: "0.75" } }, feedback));
  expect(mocks.save).not.toHaveBeenCalled();
  expect(result.current.form).toMatchObject({ feedback: "AI feedback", itemScores: { r: { i: "0.75" } },
    structuredFeedback: feedback, clearStructuredFeedback: false, acknowledgedReviewItems: {}, reviewedItemScores: {} });
  await act(async () => { await result.current.save(); });
  await waitFor(() => expect(mocks.save).toHaveBeenCalledWith("e", expect.objectContaining({
    structuredFeedback: feedback, itemScoresByRubric: { r: { i: "0.75" } }, usedProposalId: "proposal",
  })));
  expect(mocks.save.mock.calls[0]![1]).not.toHaveProperty("clearStructuredFeedback", true);
});

it("retains existing structured review when applying a legacy proposal without structured feedback", () => {
  const source: Detail = { ...initial, evaluation: { ...initial.evaluation, structuredFeedback: feedback } };
  const { result } = renderHook(() => useGradingWorkspace(source, async () => source));
  act(() => result.current.applyProposal({ r: ["i"] }, "Legacy feedback", "legacy"));
  expect(result.current.form.structuredFeedback).toEqual(feedback);
  expect(result.current.form.clearStructuredFeedback).toBe(false);
  expect(result.current.form.reviewedItemScores).toEqual({ r: { i: "0.50" } });
});

it("does not invent review metadata for a legacy proposal with no structured feedback to review", () => {
  const { result } = renderHook(() => useGradingWorkspace(initial, async () => initial));
  act(() => result.current.applyProposal({ r: ["i"] }, "Legacy feedback", "legacy"));
  expect(result.current.form.structuredFeedback).toBeNull();
  expect(result.current.form.reviewedItemScores).toEqual({});
});

it.each([
  { retained: feedback, expected: { r: { i: "0.50" } } },
  { retained: { ...feedback, items: [] }, expected: {} },
])("only carries review metadata for items present in retained feedback: $expected", ({ retained, expected }) => {
  const source: Detail = { ...initial,
    rubrics: [{ ...initial.rubrics[0]!, criteria: [{ criterionId: "criterion", title: "Design", items: [
      { itemId: "i", text: "First", points: 1 }, { itemId: "other", text: "Second", points: 1 },
    ] }] }],
    evaluation: { ...initial.evaluation, rubricItemScores: { r: { i: "0.50", other: "0.25" } }, structuredFeedback: retained },
  };
  const { result } = renderHook(() => useGradingWorkspace(source, async () => source));
  act(() => result.current.applyProposal({ r: ["i", "other"] }, "Legacy feedback", "legacy"));
  expect(result.current.form.reviewedItemScores).toEqual(expected);
});
