import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { PracticeFeedbackPanel } from "@/features/assignmentwork/components/PracticeFeedbackPanel";
const mocks = vi.hoisted(() => ({ feedback: vi.fn() }));
vi.mock("@/features/assignmentwork/api", () => ({ workApi: mocks }));
afterEach(() => { vi.useRealTimers(); vi.clearAllMocks(); });
it("bounds automatic polling and permits an explicit recheck", async () => {
  vi.useFakeTimers();
  mocks.feedback.mockResolvedValue([{ feedbackId: "f", status: "PENDING", provenance: {}, createdAt: "2026-11-10T00:00:00Z" }]);
  render(<PracticeFeedbackPanel attemptId="a" partTitles={{}} />);
  await act(async () => {});
  for (let i = 0; i < 60; i++) await act(async () => { await vi.advanceTimersByTimeAsync(3000); });
  expect(mocks.feedback).toHaveBeenCalledTimes(61);
  await act(async () => { await vi.advanceTimersByTimeAsync(30000); });
  expect(mocks.feedback).toHaveBeenCalledTimes(61);
  fireEvent.click(screen.getByRole("button", { name: "Kiểm tra lại góp ý" }));
  await act(async () => {});
  expect(mocks.feedback).toHaveBeenCalledTimes(62);
});
