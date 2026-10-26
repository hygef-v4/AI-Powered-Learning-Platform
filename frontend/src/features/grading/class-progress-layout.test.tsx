import { act, cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ClassProgressSection } from "./ClassProgressSection";
import type { GradebookPage, SubmissionPage } from "./types";
import type { AssignmentPage } from "@/features/assignments/types";

type DistributionBucket = { label: string; count: number };

const gradingMocks = vi.hoisted(() => ({
  progress: vi.fn(),
  gradebook: vi.fn(),
  classDistribution: vi.fn(),
}));

const assignmentMocks = vi.hoisted(() => ({
  listForClass: vi.fn(),
}));

vi.mock("./api", () => ({ gradingApi: gradingMocks }));
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

  it("orders pending-grading items first, then earliest deadline, limits to 5 with Xem tất cả / Thu gọn", async () => {
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

    const showAllBtn = await screen.findByRole("button", { name: "Xem tất cả (6)" });
    expect(showAllBtn).toBeInTheDocument();

    const visibleItems = screen.getAllByTestId("progress-assignment-row");
    expect(visibleItems).toHaveLength(5);
    expect(visibleItems.map((el) => el.getAttribute("data-assignment-id"))).toEqual([
      "a-pending-early",
      "a-pending-late",
      "a-early-deadline",
      "a-late-deadline",
      "a-extra",
    ]);
    expect(screen.queryByText("Bài không hạn")).not.toBeInTheDocument();

    fireEvent.click(showAllBtn);
    const expandedItems = screen.getAllByTestId("progress-assignment-row");
    expect(expandedItems).toHaveLength(6);
    expect(expandedItems[5]).toHaveAttribute("data-assignment-id", "a-no-deadline");
    expect(screen.getByText("Bài không hạn")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Thu gọn" }));
    expect(screen.getAllByTestId("progress-assignment-row")).toHaveLength(5);
    expect(screen.getByRole("button", { name: "Xem tất cả (6)" })).toBeInTheDocument();
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
    expect(screen.getByText(/3 \/ 4 nhóm đã nộp \(75%\)/)).toBeInTheDocument();
    expect(screen.getByText(/0 \/ 0 sinh viên đã nộp \(0%\)/)).toBeInTheDocument();
    expect(screen.getByText("Chưa có dữ liệu")).toBeInTheDocument();
    expect(container.textContent).not.toMatch(/NaN|Infinity|100%/);

    const zeroBar = screen.getByRole("progressbar", { name: "Tiến độ nộp Bài cá nhân 0 tổng" });
    expect(zeroBar).toHaveAttribute("max", "1");
    expect(zeroBar).toHaveAttribute("value", "0");

    const submissionLinks = screen.getAllByRole("link", { name: /Xem bài nộp/ });
    expect(submissionLinks[0]).toHaveAttribute(
      "href",
      "/teaching/classes/class-1/assignments/a-group/submissions",
    );
  });

  it("does not fabricate 0 while loading or on error, and notes hidden open assignments when present", async () => {
    const pendingProgress = deferred<Record<string, SubmissionPage>>();
    gradingMocks.progress.mockReturnValueOnce(pendingProgress.promise);
    gradingMocks.gradebook.mockResolvedValue({ assignments: [], rows: [], total: 0, page: 1, size: 20 });
    assignmentMocks.listForClass.mockResolvedValue({
      items: [{ id: "o1", title: "Open 1", type: "GRADED", submissionMode: "INDIVIDUAL", status: "OPEN", closesAt: new Date().toISOString(), maxPoints: 10 }],
      counts: { DRAFT: 0, OPEN: 2, CLOSED: 0 },
      hiddenCount: 1,
    });

    render(<ClassProgressSection classId="class-1" layout="overview" />);

    const openRegion = screen.getByTestId("overview-open-card");
    const pendingRegion = screen.getByTestId("overview-pending-card");
    expect(openRegion).toHaveTextContent("Đang tải…");
    expect(pendingRegion).toHaveTextContent("Đang tải…");

    await act(async () => {
      pendingProgress.reject(new Error("Mất kết nối máy chủ"));
    });

    expect(await screen.findByRole("alert")).toHaveTextContent("Mất kết nối máy chủ");
    expect(openRegion).toHaveTextContent("Không tải được");
    expect(pendingRegion).toHaveTextContent("Không tải được");

    gradingMocks.progress.mockResolvedValueOnce({
      "a-1": makeSummaryPage({ PENDING: 4 }),
    });
    fireEvent.click(screen.getByRole("button", { name: "Thử tải lại" }));

    await waitFor(() => expect(openRegion).toHaveTextContent("1"));
    expect(openRegion).toHaveTextContent("1 bài hết hạn hôm nay");
    expect(openRegion).toHaveTextContent("1 bài ẩn theo quyền");
    expect(pendingRegion).toHaveTextContent("4");
  });

  it("fetches score distribution lazily only when opened and clears stale buckets on assignment switch", async () => {
    const gradebook: GradebookPage = {
      assignments: [
        { id: "a-pub-1", title: "Bài công bố 1", maxScore: 10, submissionMode: "INDIVIDUAL" },
        { id: "a-pub-2", title: "Bài công bố 2", maxScore: 10, submissionMode: "INDIVIDUAL" },
        { id: "a-unpub", title: "Bài chưa công bố", maxScore: 10, submissionMode: "INDIVIDUAL" },
      ],
      rows: [],
      total: 0,
      page: 1,
      size: 20,
    };
    const progress: Record<string, SubmissionPage> = {
      "a-pub-1": makeSummaryPage({ PUBLISHED: 3, deadline: "2026-11-01T10:00:00Z" }),
      "a-pub-2": makeSummaryPage({ PUBLISHED: 2, deadline: "2026-11-02T10:00:00Z" }),
      "a-unpub": makeSummaryPage({ PUBLISHED: 0, deadline: "2026-11-03T10:00:00Z" }),
    };
    gradingMocks.gradebook.mockResolvedValue(gradebook);
    gradingMocks.progress.mockResolvedValue(progress);

    const dist2 = deferred<DistributionBucket[]>();
    gradingMocks.classDistribution.mockImplementation((_classId: string, assignmentId: string) => {
      if (assignmentId === "a-pub-1") {
        return Promise.resolve([
          { label: "8 - 10", count: 2 },
          { label: "5 - 7.9", count: 1 },
        ]);
      }
      return dist2.promise;
    });

    render(<ClassProgressSection classId="class-1" />);
    await screen.findByText("Bài công bố 1");

    expect(gradingMocks.classDistribution).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: /Phân bố điểm đã công bố/ }));

    await waitFor(() => expect(gradingMocks.classDistribution).toHaveBeenCalledTimes(1));
    expect(gradingMocks.classDistribution).toHaveBeenCalledWith("class-1", "a-pub-1");
    expect(await screen.findByText("8 - 10")).toBeInTheDocument();
    expect(screen.getByText("2 sinh viên")).toBeInTheDocument();

    const selector = screen.getByRole("combobox", { name: "Bài tập xem phân bố điểm" });
    expect(within(selector).queryByRole("option", { name: "Bài chưa công bố" })).not.toBeInTheDocument();

    fireEvent.change(selector, { target: { value: "a-pub-2" } });
    expect(screen.queryByText("8 - 10")).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Đang tải phân bố điểm…");

    await act(async () => {
      dist2.resolve([]);
    });
    expect(
      await screen.findByText(/Chưa đủ dữ liệu công bố hoặc ẩn theo ngưỡng riêng tư để hiển thị phân bố điểm\./),
    ).toBeInTheDocument();
  });
});
