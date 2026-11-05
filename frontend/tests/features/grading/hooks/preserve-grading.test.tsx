import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { useGradingWorkspace } from "@/features/grading/hooks/useGradingWorkspace";
import type { Detail, StructuredFeedback } from "@/features/grading/model/types";

const mocks = vi.hoisted(() => ({ save: vi.fn(), push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: mocks.push }) }));
vi.mock("@/features/grading/api", () => ({ gradingApi: mocks }));
vi.mock("@/features/grading/hooks/useGradingProposals", () => ({ useGradingProposals: () => ({ proposals: [], addProposal: vi.fn(), resumePolling: vi.fn() }) }));

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

it.each(["FINALIZED", "PUBLISHED"] as const)("does not edit or save a %s evaluation, including AI application", async (state) => {
  const source: Detail = { ...initial, evaluation: { ...initial.evaluation, state } };
  const { result } = renderHook(() => useGradingWorkspace(source, async () => source));
  const before = result.current.form;
  act(() => result.current.updateForm({ feedback: "changed" }));
  act(() => result.current.applyProposal({ r: ["i"] }, "AI feedback", "proposal"));
  await act(() => result.current.save());
  expect(result.current.disabled).toBe(true);
  expect(result.current.form).toEqual(before);
  expect(result.current.dirty).toBe(false);
  expect(mocks.save).not.toHaveBeenCalled();
});
const feedback: StructuredFeedback = { schemaVersion: 1, summary: { strengths: [], improvements: [], nextSteps: [] },
  items: [{ rubricId: "r", itemId: "i", evidence: null, reason: "Review diagram", suggestion: null,
    evidenceBlockId: null, requiresReview: true, reviewReason: "Unreadable diagram" }] };

beforeEach(() => { vi.clearAllMocks(); mocks.save.mockResolvedValue({ ...initial.evaluation, version: 3 }); });

it("reloads the official values and clears a stale unsaved draft when another session has finalized", async () => {
  const finalized: Detail = { ...initial, evaluation: { ...initial.evaluation, state: "FINALIZED", feedback: "Official feedback" } };
  const { result } = renderHook(() => useGradingWorkspace(initial, async () => finalized));
  act(() => result.current.updateForm({ feedback: "Unsaved edits" }));
  await act(() => result.current.reload());
  expect(result.current.form.feedback).toBe("Official feedback");
  expect(result.current.dirty).toBe(false);
  expect(result.current.disabled).toBe(true);
});

it("discards old marks and AI selection when a group resubmits using the same evaluation ID", async () => {
  const pending: Detail = { ...initial, evaluation: { ...initial.evaluation, state: "PENDING",
    submissionHash: "new-receipt", version: 3, score: null, rubricChecks: "{}", rubricItemScores: {},
    adjustment: 0, adjustmentReason: null, feedback: null } };
  const { result } = renderHook(() => useGradingWorkspace(initial, async () => pending));
  act(() => result.current.updateForm({ feedback: "Unsaved old feedback", usedProposalId: "old-proposal" }));
  await act(() => result.current.reload());
  expect(result.current.form.feedback).toBe("");
  expect(Number(result.current.form.itemScores?.r?.i ?? 0)).toBe(0);
  expect(result.current.form.adjustment).toBe("0");
  expect(result.current.form.usedProposalId).toBeNull();
  expect(result.current.dirty).toBe(false);
  expect(result.current.disabled).toBe(false);
});

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
