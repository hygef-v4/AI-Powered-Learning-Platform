import { act, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { StudentPublishedFeedback } from "@/features/grading/components/StudentPublishedFeedback";
const mocks = vi.hoisted(() => ({ listMyGrades: vi.fn(), loadMyGradeDistribution: vi.fn() }));
vi.mock("@/features/grading/api", () => ({ gradingApi: mocks }));
afterEach(() => vi.resetAllMocks());
it("shows a published group grade and an enabled anonymous distribution", async () => {
  mocks.listMyGrades.mockResolvedValue([{ assignmentId: "a", score: 5, maxScore: 10, feedback: "Đã duyệt",
    structuredSummary: null, rubricItems: [], memberOverride: true }]);
  mocks.loadMyGradeDistribution.mockResolvedValue([{ label: "5–6", count: 3 }]);
  render(<StudentPublishedFeedback classId="c" assignmentId="a" />);
  expect(await screen.findByText("Đã duyệt")).toBeInTheDocument();
  expect(await screen.findByRole("region", { name: "Phân bố điểm" })).toHaveTextContent("5–6: 3");
  expect(screen.getByText(/điều chỉnh riêng/)).toBeInTheDocument();
});
it("does not expose an unpublished group grade or query its distribution", async () => {
  mocks.listMyGrades.mockResolvedValue([{ assignmentId: "a", score: null }]);
  const { container } = render(<StudentPublishedFeedback classId="c" assignmentId="a" />);
  await act(async () => {});
  expect(container).toBeEmptyDOMElement();
  expect(mocks.loadMyGradeDistribution).not.toHaveBeenCalled();
});
