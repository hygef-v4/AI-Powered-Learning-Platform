import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { activateGradingAiTracking, addGradingProposal, readGradingProposals,
  subscribeGradingProposals } from "@/features/grading/model/gradingAiTasks";

const mocks = vi.hoisted(() => ({ listProposals: vi.fn(), loadTask: vi.fn() }));
vi.mock("@/features/grading/api", () => ({ gradingApi: mocks }));
let stopTracking: (() => void) | undefined;
beforeEach(() => { vi.useFakeTimers(); vi.resetAllMocks(); });
afterEach(() => { stopTracking?.(); vi.useRealTimers(); });

it("polls once across subscribers and keeps processing the right evaluation after its page unmounts", async () => {
  const pending = { id: "p", status: "PENDING", versionNo: 1, origin: "AI_TASK" };
  mocks.listProposals.mockResolvedValue([pending]);
  stopTracking = activateGradingAiTracking("teacher");
  const first = subscribeGradingProposals("a", vi.fn());
  const second = subscribeGradingProposals("a", vi.fn());
  await vi.advanceTimersByTimeAsync(0);
  expect(mocks.listProposals).toHaveBeenCalledTimes(1);
  addGradingProposal("a", pending);
  first(); second();
  mocks.listProposals.mockResolvedValue([{ ...pending, status: "READY", proposal: { feedback: "For A" } }]);
  await vi.advanceTimersByTimeAsync(3000);
  expect(readGradingProposals("a").proposals[0]?.status).toBe("READY");
  expect(readGradingProposals("b").proposals).toEqual([]);
  expect(mocks.listProposals).toHaveBeenLastCalledWith("a");
});

it("drops cached work when the authenticated teacher changes", async () => {
  stopTracking = activateGradingAiTracking("teacher-a");
  addGradingProposal("a", { id: "p", status: "PENDING", versionNo: 1, origin: "AI_TASK" });
  stopTracking();
  stopTracking = activateGradingAiTracking("teacher-b");
  expect(readGradingProposals("a").proposals).toEqual([]);
});

it("does not let a previous account's cleanup or delayed response erase the new account's cache", async () => {
  let finish!: (proposals: unknown[]) => void;
  mocks.listProposals.mockReturnValue(new Promise((resolve) => { finish = resolve; }));
  const stopPrevious = activateGradingAiTracking("old-teacher");
  const unsubscribePrevious = subscribeGradingProposals("same-evaluation", vi.fn());
  stopTracking = activateGradingAiTracking("new-teacher");
  addGradingProposal("same-evaluation", { id: "new", status: "READY", versionNo: 2, origin: "AI_TASK" });
  stopPrevious();
  unsubscribePrevious();
  finish([{ id: "old", status: "READY", versionNo: 1, origin: "AI_TASK" }]);
  await vi.advanceTimersByTimeAsync(0);
  expect(readGradingProposals("same-evaluation").proposals.map((p) => p.id)).toEqual(["new"]);
});
