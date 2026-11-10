import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { useGradingWorkspace } from "@/features/grading/hooks/useGradingWorkspace";
import type { Detail, StructuredFeedback } from "@/features/grading/model/types";

const mocks = vi.hoisted(() => ({ saveEvaluation: vi.fn(), push: vi.fn() }));
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
  expect(mocks.saveEvaluation).not.toHaveBeenCalled();
});
const feedback: StructuredFeedback = { schemaVersion: 1, summary: { strengths: [], improvements: [], nextSteps: [] },
  items: [{ rubricId: "r", itemId: "i", evidence: null, reason: "Review diagram", suggestion: null,
    evidenceBlockId: null, requiresReview: true, reviewReason: "Unreadable diagram" }] };

beforeEach(() => { vi.clearAllMocks(); mocks.saveEvaluation.mockResolvedValue({ ...initial.evaluation, version: 3 }); });

it("preserves edits made during background saving and uses the returned version for the next save", async () => {
  let finish!: (value: Detail["evaluation"]) => void;
  mocks.saveEvaluation.mockReturnValueOnce(new Promise<Detail["evaluation"]>((resolve) => { finish = resolve; }))
    .mockResolvedValueOnce({ ...initial.evaluation, version: 4, score: 1, adjustment: 0.25,
      rubricItemScores: { r: { i: "0.75" } }, adjustmentReason: "New note", feedback: "New edit" });
  const { result } = renderHook(() => useGradingWorkspace(initial, async () => initial));
  act(() => result.current.updateForm({ feedback: "First edit" }));
  let pending!: Promise<Detail["evaluation"] | null>;
  act(() => { pending = result.current.save(false, { background: true }); });
  expect(result.current.disabled).toBe(false);
  expect(await result.current.save(false, { background: true })).toBeNull();
  expect(mocks.saveEvaluation).toHaveBeenCalledTimes(1);
  act(() => result.current.updateForm({ feedback: "New edit", adjustment: "0.5", changeReason: "New note",
    itemScores: { r: { i: "0.75" } } }));
  await act(async () => {
    finish({ ...initial.evaluation, version: 3, feedback: "First edit" });
    await pending;
  });
  expect(result.current.form).toMatchObject({ feedback: "New edit", adjustment: "0.5", changeReason: "New note",
    itemScores: { r: { i: "0.75" } } });
  expect(result.current.dirty).toBe(true);
  await act(async () => { await result.current.save(false, { background: true }); });
  expect(mocks.saveEvaluation).toHaveBeenLastCalledWith("e", expect.objectContaining({
    version: 3, feedback: "New edit", adjustment: "0.5", adjustmentReason: "New note", changeReason: "New note",
    itemScoresByRubric: { r: { i: "0.75" } },
  }));
  expect(result.current.dirty).toBe(false);
});

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
  expect(mocks.saveEvaluation).toHaveBeenCalledWith("e", expect.objectContaining({ itemScoresByRubric: { r: { i: "0.50" } },
    adjustment: "-0.25", adjustmentReason: "Late", feedback: "Updated feedback" }));
});

it("retains AI partial points, feedback and flags while resetting previous review acknowledgements", async () => {
  const source: Detail = { ...initial, evaluation: { ...initial.evaluation, structuredFeedback: feedback,
    reviewedItemScores: { r: { i: 1 } }, acknowledgedReviewItems: { r: ["i"] } } };
  const { result } = renderHook(() => useGradingWorkspace(source, async () => source));
  act(() => result.current.applyProposal({}, "AI feedback", "proposal", { r: { i: "0.75" } }, feedback));
  expect(mocks.saveEvaluation).not.toHaveBeenCalled();
  expect(result.current.form).toMatchObject({ feedback: "AI feedback", itemScores: { r: { i: "0.75" } },
    structuredFeedback: feedback, clearStructuredFeedback: false, acknowledgedReviewItems: {}, reviewedItemScores: {} });
  await act(async () => { await result.current.save(); });
  await waitFor(() => expect(mocks.saveEvaluation).toHaveBeenCalledWith("e", expect.objectContaining({
    structuredFeedback: feedback, itemScoresByRubric: { r: { i: "0.75" } }, usedProposalId: "proposal",
  })));
  expect(mocks.saveEvaluation.mock.calls[0]![1]).not.toHaveProperty("clearStructuredFeedback", true);
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


it("refreshes every group member version including overridden grades after saving", async () => {
  const source: Detail = { ...initial, evaluation: { ...initial.evaluation, kind: "GROUP_DOCUMENT" },
    members: [{ ...initial.evaluation, id: "member", kind: "MEMBER", memberOverride: true, version: 4 }] };
  const refreshed: Detail = { ...source, members: [{ ...source.members[0]!, version: 5 }] };
  mocks.saveEvaluation.mockResolvedValue({ ...source.evaluation, version: 3 });
  const refresh = vi.fn().mockResolvedValue(refreshed);
  const { result } = renderHook(() => useGradingWorkspace(source, refresh));
  act(() => result.current.updateForm({ feedback: "Changed" }));
  await act(async () => { await result.current.save(); });
  expect(result.current.detail.members[0]?.version).toBe(5);
});

it("does not carry saved marks onto a group receipt changed during the refresh", async () => {
  const source: Detail = { ...initial, evaluation: { ...initial.evaluation, kind: "GROUP_DOCUMENT" } };
  const pending: Detail = { ...source, evaluation: { ...source.evaluation, submissionHash: "new", state: "PENDING",
    version: 9, score: null, feedback: null, rubricChecks: "{}", rubricItemScores: {}, adjustment: 0 } };
  mocks.saveEvaluation.mockResolvedValue({ ...source.evaluation, version: 3 });
  const { result } = renderHook(() => useGradingWorkspace(source, async () => pending));
  act(() => result.current.updateForm({ feedback: "Old receipt" }));
  await act(async () => { await result.current.save(); });
  expect(result.current.form.feedback).toBe("");
  expect(result.current.detail.evaluation.submissionHash).toBe("new");
});
