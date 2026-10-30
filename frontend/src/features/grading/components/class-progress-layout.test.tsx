import { act, cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ClassProgressSection } from "./ClassProgressSection";
import type { GradebookPage, SubmissionPage } from "../model/types";
import type { AssignmentPage } from "@/features/assignments/model/types";


const gradingMocks = vi.hoisted(() => ({
  progress: vi.fn(),
  gradebook: vi.fn(),
  classDistribution: vi.fn(),
}));

const assignmentMocks = vi.hoisted(() => ({
  listForClass: vi.fn(),
}));

vi.mock("../api", () => ({ gradingApi: gradingMocks }));
vi.mock("@/features/assignments/api", () => ({ assignmentsApi: assignmentMocks }));

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

function makeSummaryPage(
  overrides: {
    total?: number;
    submitted?: number;
    PENDING?: number;
    PUBLISHED?: number;
    deadline?: string | null;
  } = {},
): SubmissionPage {
  return {
    rows: [],
    total: overrides.total ?? 10,
    page: 1,
    size: 20,
    deadline: overrides.deadline ?? null,
    summary: {
      total: overrides.total ?? 10,
      submitted: overrides.submitted ?? 5,
      PENDING: overrides.PENDING ?? 0,
      PUBLISHED: overrides.PUBLISHED ?? 0,
    },
  };
}

const emptyOpenPage: AssignmentPage = {
  items: [],
  counts: { DRAFT: 0, OPEN: 0, CLOSED: 0 },
  hiddenCount: 0,
  groupSupported: true,
  ownerArchived: false,
};

describe("U09 ClassProgressSection layout and lazy distribution", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    assignmentMocks.listForClass.mockResolvedValue(emptyOpenPage);
    gradingMocks.classDistribution.mockResolvedValue([]);
  });

  afterEach(cleanup);

  it("lists every graded assignment, pending-grading first, then earliest deadline, no-deadline last", async () => {
    const gradebook: GradebookPage = {
      assignments: [
        { id: "a-no-deadline", title: "Bài không hạn", maxScore: 10, submissionMode: "INDIVIDUAL" },
        { id: "a-late-deadline", title: "Bài hạn muộn", maxScore: 10, submissionMode: "INDIVIDUAL" },
        { id: "a-early-deadline", title: "Bài hạn sớm", maxScore: 10, submissionMode: "INDIVIDUAL" },
        { id: "a-pending-late", title: "Bài chờ chấm hạn muộn", maxScore: 10, submissionMode: "GROUP" },
        { id: "a-pending-early", title: "Bài chờ chấm hạn sớm", maxScore: 10, submissionMode: "INDIVIDUAL" },
        { id: "a-extra", title: "Bài thứ sáu", maxScore: 10, submissionMode: "INDIVIDUAL" },
      ],
      rows: [],
      total: 0,
      page: 1,
      size: 20,
    };
    const progress: Record<string, SubmissionPage> = {
      "a-no-deadline": makeSummaryPage({ PENDING: 0, deadline: null }),
      "a-late-deadline": makeSummaryPage({ PENDING: 0, deadline: "2026-11-10T10:00:00Z" }),
      "a-early-deadline": makeSummaryPage({ PENDING: 0, deadline: "2026-11-01T10:00:00Z" }),
      "a-pending-late": makeSummaryPage({ PENDING: 2, deadline: "2026-11-05T10:00:00Z" }),
      "a-pending-early": makeSummaryPage({ PENDING: 1, deadline: "2026-11-02T10:00:00Z" }),
      "a-extra": makeSummaryPage({ PENDING: 0, deadline: "2026-11-20T10:00:00Z" }),
    };
    gradingMocks.gradebook.mockResolvedValue(gradebook);
    gradingMocks.progress.mockResolvedValue(progress);

    render(<ClassProgressSection classId="class-1" />);

    await screen.findByText("Bài không hạn");
    // Danh sách cuộn trong thẻ nên hiện đủ mọi bài, không có Xem tất cả.
    expect(screen.queryByRole("button", { name: /Xem tất cả/ })).not.toBeInTheDocument();
    expect(screen.getAllByTestId("progress-assignment-row").map((el) => el.getAttribute("data-assignment-id"))).toEqual([
      "a-pending-early",
      "a-pending-late",
      "a-early-deadline",
      "a-late-deadline",
      "a-extra",
      "a-no-deadline",
    ]);
    // Dòng phụ là loại bài, không ghi hạn nộp.
    expect(screen.queryByText("Không có hạn")).not.toBeInTheDocument();
    expect(within(screen.getAllByTestId("progress-assignment-row")[1]!).getByText("Nhóm")).toBeInTheDocument();
    expect(within(screen.getAllByTestId("progress-assignment-row")[0]!).getByText("Cá nhân")).toBeInTheDocument();
  });

  it("handles student vs group units, absent progress, and zero denominator without NaN or 100%", async () => {
    const gradebook: GradebookPage = {
      assignments: [
        { id: "a-zero", title: "Bài cá nhân 0 tổng", maxScore: 10, submissionMode: "INDIVIDUAL" },
        { id: "a-group", title: "Bài nhóm có dữ liệu", maxScore: 10, submissionMode: "GROUP" },
        { id: "a-missing", title: "Bài chưa đồng bộ", maxScore: 10, submissionMode: "INDIVIDUAL" },
      ],
      rows: [],
      total: 0,
      page: 1,
      size: 20,
    };
    const progress: Record<string, SubmissionPage> = {
      "a-zero": makeSummaryPage({ total: 0, submitted: 0, PENDING: 0, deadline: "2026-11-01T10:00:00Z" }),
      "a-group": makeSummaryPage({ total: 4, submitted: 3, PENDING: 1, deadline: "2026-11-02T10:00:00Z" }),
    };
    gradingMocks.gradebook.mockResolvedValue(gradebook);
    gradingMocks.progress.mockResolvedValue(progress);

    const { container } = render(<ClassProgressSection classId="class-1" />);

    await screen.findByText("Bài nhóm có dữ liệu");
    expect(screen.getByTitle("3 / 4 nhóm đã nộp")).toHaveTextContent("3/4");
    // Bài tổng 0 và bài chưa có dữ liệu tiến độ đều là 0/0, không có thanh tiến độ giả.
    expect(screen.getAllByTitle("0 / 0 sinh viên đã nộp")).toHaveLength(2);
    expect(container.textContent).not.toMatch(/NaN|Infinity|100%/);

    // Cả dòng là link sang Chấm điểm mở sẵn bài đó; bài có bài chờ chấm đứng đầu, không ghi số chờ chấm (link đầu là Chấm điểm).
    const links = screen.getAllByRole("link");
    expect(links[0]).toHaveTextContent("Chấm điểm");
    expect(links[0]).toHaveAttribute("href", "/teaching/classes/class-1?tab=gradebook");
    expect(links[1]).toHaveAttribute("href", "/teaching/classes/class-1?tab=gradebook&assignment=a-group");
    expect(links[1]).not.toHaveTextContent("chờ chấm");
  });

  it("shows loading then a retryable error without stat cards, and lists assignments after retry", async () => {
    const pendingProgress = deferred<Record<string, SubmissionPage>>();
    gradingMocks.progress.mockReturnValueOnce(pendingProgress.promise);
    gradingMocks.gradebook.mockResolvedValue({
      assignments: [{ id: "a-1", title: "Bài 1", maxScore: 10, submissionMode: "INDIVIDUAL" }],
      rows: [], total: 0, page: 1, size: 20,
    });

    render(<ClassProgressSection classId="class-1" layout="overview" />);

    expect(screen.getByRole("status")).toHaveTextContent("Đang tải tiến độ nộp bài…");
    expect(screen.queryByTestId("overview-open-card")).not.toBeInTheDocument();
    expect(screen.queryByTestId("overview-pending-card")).not.toBeInTheDocument();

    await act(async () => {
      pendingProgress.reject(new Error("Mất kết nối máy chủ"));
    });

    expect(await screen.findByRole("alert")).toHaveTextContent("Mất kết nối máy chủ");

    gradingMocks.progress.mockResolvedValueOnce({ "a-1": makeSummaryPage({ PENDING: 4 }) });
    fireEvent.click(screen.getByRole("button", { name: "Thử tải lại" }));

    expect(await screen.findByText("Bài 1")).toBeInTheDocument();
    expect(screen.queryByText("4 chờ chấm")).not.toBeInTheDocument();
  });

  it("tab Theo sinh viên: số bài đã nộp của từng người (nộp ít lên trước), bấm để mở Chấm điểm của người đó; không còn Phân bố điểm", async () => {
    const cell = (assignmentId: string, status: string) => ({
      assignmentId, evaluationId: null, status, score: null, maxScore: 10, late: false, submittedAt: null, feedback: null,
    });
    gradingMocks.gradebook.mockResolvedValue({
      assignments: [
        { id: "a-1", title: "Bài 1", maxScore: 10, submissionMode: "INDIVIDUAL" },
        { id: "a-2", title: "Bài 2", maxScore: 10, submissionMode: "INDIVIDUAL" },
      ],
      rows: [
        { student: { accountId: "s-1", name: "An", email: "an@school.test" }, grades: [cell("a-1", "PUBLISHED"), cell("a-2", "PENDING")] },
        { student: { accountId: "s-2", name: "Bình", email: "binh@school.test" }, grades: [cell("a-1", "NOT_SUBMITTED"), cell("a-2", "IN_PROGRESS")] },
      ],
      total: 2, page: 1, size: 100,
    } satisfies GradebookPage);
    gradingMocks.progress.mockResolvedValue({});

    render(<ClassProgressSection classId="class-1" />);
    await screen.findByText("Bài 1");
    expect(gradingMocks.gradebook).toHaveBeenCalledWith("class-1", "", 1, 100);
    expect(screen.queryByRole("button", { name: "Phân bố điểm" })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: "Theo sinh viên" }));
    const rows = screen.getAllByTestId("progress-student-row");
    expect(rows[0]).toHaveTextContent("Bình");
    expect(within(rows[0]!).getByTitle("Đã nộp 0 / 2 bài")).toHaveTextContent("0/2");
    expect(within(rows[1]!).getByTitle("Đã nộp 2 / 2 bài")).toBeInTheDocument();
    expect(within(rows[1]!).getByRole("link")).toHaveAttribute("href", "/teaching/classes/class-1?tab=gradebook&student=s-1");
  });
});
