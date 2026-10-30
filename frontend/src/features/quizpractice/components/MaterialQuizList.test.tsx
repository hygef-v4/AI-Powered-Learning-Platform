import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { getMaterialQuizzes, startAttempt } from "../api";
import { MaterialQuizList } from "./MaterialQuizList";
import type { PracticeQuizRow } from "../model/types";

vi.mock("../api", () => ({ getMaterialQuizzes: vi.fn(), startAttempt: vi.fn() }));
const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
let role = "STUDENT";
vi.mock("@/features/identity/hooks/session", () => ({
  useSession: () => ({ status: "authenticated", profile: { role } }),
}));

const row = (overrides: Partial<PracticeQuizRow> = {}): PracticeQuizRow => ({
  quizId: "z1", classId: "c1", classCode: "SE1801", className: "Lớp", title: "Ôn chương 1", status: "OPEN",
  timeLimitMinutes: 15, attemptsUsed: 1, maxAttempts: 2, bestScore: 8, lastScore: 8, maxScore: 10, lastAttemptAt: null,
  inProgressAttemptId: null, canStart: true, ...overrides,
});

afterEach(() => {
  vi.clearAllMocks();
  role = "STUDENT";
});

describe("MaterialQuizList (G4)", () => {
  it("hiện lượt đã dùng, điểm cao nhất và bắt đầu lượt mới", async () => {
    vi.mocked(getMaterialQuizzes).mockResolvedValue([row()]);
    vi.mocked(startAttempt).mockResolvedValue({ attemptId: "a9" } as Awaited<ReturnType<typeof startAttempt>>);
    render(<MaterialQuizList materialId="m1" classId="c1" />);

    expect(await screen.findByText("Ôn chương 1")).toBeInTheDocument();
    expect(screen.getByText(/Đã dùng 1 \/ 2 lượt/)).toBeInTheDocument();
    expect(getMaterialQuizzes).toHaveBeenCalledWith("c1", "m1", expect.anything());
    fireEvent.click(screen.getByTestId("material-quiz-start"));
    await waitFor(() => expect(push).toHaveBeenCalledWith("/quiz-attempts/a9"));
  });

  it("hết lượt thì báo, không có nút làm", async () => {
    vi.mocked(getMaterialQuizzes).mockResolvedValue([row({ attemptsUsed: 2, canStart: false })]);
    render(<MaterialQuizList materialId="m1" classId="c1" />);

    expect(await screen.findByText("Bạn đã dùng hết 2/2 lượt làm quiz này.")).toBeInTheDocument();
    expect(screen.queryByTestId("material-quiz-start")).not.toBeInTheDocument();
  });

  it("không hiện gì khi không có quiz hoặc người xem không phải sinh viên", async () => {
    vi.mocked(getMaterialQuizzes).mockResolvedValue([]);
    const { container } = render(<MaterialQuizList materialId="m1" classId="c1" />);
    await waitFor(() => expect(getMaterialQuizzes).toHaveBeenCalled());
    await waitFor(() => expect(container).toBeEmptyDOMElement());

    role = "TEACHER";
    vi.mocked(getMaterialQuizzes).mockClear();
    const teacher = render(<MaterialQuizList materialId="m1" classId="c1" />);
    expect(teacher.container).toBeEmptyDOMElement();
    expect(getMaterialQuizzes).not.toHaveBeenCalled();
  });
});
