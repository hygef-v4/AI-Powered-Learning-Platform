import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SubmissionDetailPage } from "@/features/grading/pages/SubmissionDetailPage";
import type { SubmissionPage } from "@/features/grading/model/types";
import { ApiError } from "@/shared/api/client";

const mocks = vi.hoisted(() => ({ listSubmissions: vi.fn(), loadAssignmentContext: vi.fn(), classDetail: vi.fn() }));
vi.mock("@/features/grading/api", () => ({ gradingApi: mocks }));
vi.mock("@/features/academics/api", () => ({ academicsApi: { loadClass: mocks.classDetail } }));
vi.mock("@/features/grading/components/BatchActionDialog", () => ({
  BatchActionDialog: ({ items, onDone }: { items: unknown; onDone: () => void }) => (
    <div role="dialog" aria-label="Batch preview">
      <output data-testid="batch-items">{JSON.stringify(items)}</output>
      <button onClick={onDone}>Refresh list</button>
    </div>
  ),
}));

const context = { id: "a", scope: "CLASS", classId: "c", title: "Phân tích yêu cầu", ownerName: "SE01 — Lớp một" };
const page: SubmissionPage = {
  rows: [
    { student: { accountId: "s", name: "Sinh viên A", email: "a@example.test" }, evaluationId: "e", attemptId: "attempt", status: "DRAFT", score: 5, maxScore: 10, late: false, submittedAt: "2026-10-25T01:00:00Z", attemptCount: 1, version: 7 },
    { student: { accountId: "s2", name: "Sinh viên B", email: "b@example.test" }, evaluationId: null, attemptId: null, status: "NOT_SUBMITTED", score: null, maxScore: null, late: false, submittedAt: null, attemptCount: 0, version: 0 },
  ],
  total: 21, page: 1, size: 20, summary: { submitted: 1, notSubmitted: 1 }, deadline: null,
};

it("excludes already published grades from a mixed publish selection", async () => {
  mocks.listSubmissions.mockResolvedValue({ ...page, rows: [page.rows[0], {
    ...page.rows[0], student: page.rows[1]!.student, evaluationId: "published", status: "PUBLISHED", version: 9,
  }] });
  render(<SubmissionDetailPage classId="c" assignmentId="a" />);
  await screen.findByRole("checkbox", { name: "Chọn Sinh viên A" });
  fireEvent.click(screen.getByRole("checkbox", { name: "Chọn tất cả bài trong trang" }));
  fireEvent.click(screen.getByRole("button", { name: "Công bố điểm" }));
  expect(screen.getByTestId("batch-items")).toHaveTextContent(JSON.stringify([{ evaluationId: "e", version: 7 }]));
});
function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => { resolve = done; });
  return { promise, resolve };
}
beforeEach(() => {
  vi.resetAllMocks();
  mocks.listSubmissions.mockResolvedValue(page);
  mocks.loadAssignmentContext.mockResolvedValue(context);
});
afterEach(cleanup);

async function ready() {
  await screen.findByRole("checkbox", { name: "Chọn Sinh viên A" });
}

describe("submission assignment context", () => {
  it("carries the selected status and late filters into the grading workspace", async () => {
    render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await ready();
    fireEvent.change(screen.getByTestId("submissions-status-filter"), { target: { value: "DRAFT" } });
    fireEvent.change(screen.getByTestId("submissions-late-filter"), { target: { value: "true" } });
    expect(screen.getByRole("link", { name: "Tiếp tục chấm" })).toHaveAttribute("href", "/teaching/evaluations/e?status=DRAFT&late=true");
  });
  it("labels subject-scoped work using the authorized class in the same subject", async () => {
    mocks.loadAssignmentContext.mockResolvedValue({ ...context, scope: "SUBJECT", classId: null, subjectId: "subject", ownerName: "Subject name" });
    mocks.classDetail.mockResolvedValue({ id: "c", subject: { id: "subject" }, code: "SE_JS", name: "Software Design" });
    render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    expect(await screen.findByText("Lớp: SE_JS — Software Design")).toBeInTheDocument();
    expect(mocks.classDetail).toHaveBeenCalledWith("teaching", "c");
    expect(screen.queryByText("Không tải được thông tin bài tập/lớp.")).not.toBeInTheDocument();
  });
  it.each([
    { id: "c", subject: { id: "other" }, code: "OTHER", name: "Other subject" },
    { id: "other", subject: { id: "subject" }, code: "OTHER", name: "Other class" },
  ])("rejects subject work with a mismatched authorized class: %j", async (courseClass) => {
    mocks.loadAssignmentContext.mockResolvedValue({ ...context, scope: "SUBJECT", classId: null, subjectId: "subject" });
    mocks.classDetail.mockResolvedValue(courseClass);
    render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await screen.findByText("Không tải được thông tin bài tập/lớp.");
    expect(screen.queryByText(/Lớp: OTHER/)).not.toBeInTheDocument();
  });
  it("does not expose subject context when the teaching class lookup is unavailable", async () => {
    mocks.loadAssignmentContext.mockResolvedValue({ ...context, scope: "SUBJECT", classId: null, subjectId: "subject" });
    mocks.classDetail.mockRejectedValue(new ApiError({ status: 503, title: "Unavailable" }));
    render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await screen.findByText("Không tải được thông tin bài tập/lớp.");
    await ready();
    expect(screen.queryByText(`Lớp: ${context.ownerName}`)).not.toBeInTheDocument();
  });
  it("ignores a subject class lookup that finishes after navigating to another assignment", async () => {
    const oldClass = deferred<{ id: string; subject: { id: string }; code: string; name: string }>();
    mocks.loadAssignmentContext.mockImplementation((id: string) => Promise.resolve(id === "a"
      ? { ...context, scope: "SUBJECT", classId: null, subjectId: "subject" }
      : { ...context, id, title: "Bài mới" }));
    mocks.classDetail.mockReturnValue(oldClass.promise);
    const view = render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await waitFor(() => expect(mocks.classDetail).toHaveBeenCalled());
    view.rerender(<SubmissionDetailPage classId="c" assignmentId="new" />);
    await screen.findByRole("heading", { name: "Bài nộp — Bài mới" });
    await act(async () => oldClass.resolve({ id: "c", subject: { id: "subject" }, code: "OLD", name: "Old class" }));
    expect(screen.getByRole("heading", { name: "Bài nộp — Bài mới" })).toBeInTheDocument();
    expect(screen.queryByText("Lớp: OLD — Old class")).not.toBeInTheDocument();
  });
  it("names the assignment and class using authorized metadata", async () => {
    render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    expect(await screen.findByRole("heading", { level: 1, name: "Bài nộp — Phân tích yêu cầu" })).toBeInTheDocument();
    expect(screen.getByText("Lớp: SE01 — Lớp một")).toBeInTheDocument();
    expect(mocks.loadAssignmentContext).toHaveBeenCalledWith("a");
  });

  it("loads rows independently while metadata is pending", async () => {
    const pending = deferred<typeof context>();
    mocks.loadAssignmentContext.mockReturnValue(pending.promise);
    render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await ready();
    expect(screen.getByRole("heading", { level: 1, name: "Bài nộp" })).toBeInTheDocument();
    expect(screen.getByText("Đang tải thông tin bài tập/lớp.")).toBeInTheDocument();
    await act(async () => pending.resolve(context));
    expect(await screen.findByText("Lớp: SE01 — Lớp một")).toBeInTheDocument();
  });

  it.each([403, 404, 503])("isolates metadata HTTP %i and retries it without reloading submissions", async (status) => {
    mocks.loadAssignmentContext.mockRejectedValueOnce(new ApiError({ status, title: "Unavailable" }));
    render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await ready();
    expect(await screen.findByText("Không tải được thông tin bài tập/lớp.")).toBeInTheDocument();
    expect(screen.queryByText(context.ownerName, { exact: false })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Thử tải lại thông tin bài tập/lớp" }));
    await screen.findByText("Lớp: SE01 — Lớp một");
    expect(mocks.loadAssignmentContext).toHaveBeenCalledTimes(2);
    expect(mocks.listSubmissions).toHaveBeenCalledTimes(1);
  });

  it.each([
    { ...context, id: "wrong" },
    { ...context, classId: "wrong" },
    { ...context, scope: "SUBJECT", classId: null },
  ])("does not label rows using mismatched metadata: %j", async (metadata) => {
    mocks.loadAssignmentContext.mockResolvedValue(metadata);
    render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await ready();
    await screen.findByText("Không tải được thông tin bài tập/lớp.");
    expect(screen.queryByText(context.ownerName, { exact: false })).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(/^Bài nộp$/);
  });

  it("uses readable fallbacks for empty names rather than IDs", async () => {
    mocks.loadAssignmentContext.mockResolvedValue({ ...context, title: "   ", ownerName: "" });
    render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await screen.findByRole("heading", { name: "Bài nộp — Bài tập chưa có tiêu đề" });
    expect(screen.getByText("Chưa có thông tin lớp")).toBeInTheDocument();
  });

  it("renders long metadata and HTML-looking titles as text", async () => {
    const title = '<img src=x onerror="alert(1)">' + " Tên bài tập dài".repeat(30);
    mocks.loadAssignmentContext.mockResolvedValue({ ...context, title });
    const { container } = render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    expect(await screen.findByRole("heading", { name: `Bài nộp — ${title}` })).toBeInTheDocument();
    expect(container.querySelector("img")).toBeNull();
  });

  it("ignores an earlier response after navigating to another assignment", async () => {
    const pending = deferred<typeof context>();
    mocks.loadAssignmentContext.mockImplementation((id: string) => id === "a" ? pending.promise : Promise.resolve({ ...context, id, title: "Bài mới" }));
    const { rerender } = render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await waitFor(() => expect(mocks.loadAssignmentContext).toHaveBeenCalledWith("a"));
    rerender(<SubmissionDetailPage classId="c" assignmentId="b" />);
    await screen.findByRole("heading", { name: "Bài nộp — Bài mới" });
    await act(async () => pending.resolve(context));
    expect(screen.getByRole("heading", { name: "Bài nộp — Bài mới" })).toBeInTheDocument();
  });

  it("hides loaded metadata immediately when the route changes", async () => {
    const pending = deferred<typeof context>();
    const { rerender } = render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await screen.findByText("Lớp: SE01 — Lớp một");
    mocks.loadAssignmentContext.mockReturnValue(pending.promise);
    rerender(<SubmissionDetailPage classId="other" assignmentId="b" />);
    expect(screen.queryByText("Lớp: SE01 — Lớp một")).not.toBeInTheDocument();
    await act(async () => pending.resolve({ ...context, id: "b", classId: "other" }));
  });
});

describe("submission filters and batch behavior", () => {
  it("hides bulk actions until selection, resets page and selection on filter/page change, and does not reload metadata", async () => {
    render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await ready();
    expect(screen.queryByRole("button", { name: "Chốt điểm" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("checkbox", { name: "Chọn Sinh viên A" }));
    expect(screen.getByRole("button", { name: "Chốt điểm" })).toBeEnabled();
    fireEvent.click(screen.getByTestId("pagination-page-2-button"));
    await waitFor(() => expect(mocks.listSubmissions).toHaveBeenLastCalledWith("c", "a", "ALL", 2, ""));
    await ready();
    expect(screen.queryByRole("button", { name: "Chốt điểm" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("checkbox", { name: "Chọn Sinh viên A" }));
    expect(screen.getByRole("button", { name: "Chốt điểm" })).toBeEnabled();
    fireEvent.change(screen.getByRole("combobox", { name: "Trạng thái" }), { target: { value: "DRAFT" } });
    await waitFor(() => expect(mocks.listSubmissions).toHaveBeenLastCalledWith("c", "a", "DRAFT", 1, ""));
    await ready();
    expect(screen.queryByRole("button", { name: "Chốt điểm" })).not.toBeInTheDocument();
    fireEvent.change(screen.getByRole("combobox", { name: "Nộp bài" }), { target: { value: "true" } });
    await waitFor(() => expect(mocks.listSubmissions).toHaveBeenLastCalledWith("c", "a", "DRAFT", 1, "true"));
    await ready();
    expect(mocks.loadAssignmentContext).toHaveBeenCalledTimes(1);
    expect(screen.getByText("Lớp: SE01 — Lớp một")).toBeInTheDocument();
  });

  it("renders state-specific action links, single-line tabular scores, and clear-selection control", async () => {
    mocks.listSubmissions.mockResolvedValue({
      ...page,
      rows: [
        { student: { accountId: "s1", name: "SV Pending", email: "p@example.test" }, evaluationId: "e-pending", attemptId: "a1", status: "PENDING", score: null, maxScore: 10, late: false, submittedAt: "2026-10-25T01:00:00Z", attemptCount: 1, version: 1 },
        { student: { accountId: "s2", name: "SV Draft", email: "d@example.test" }, evaluationId: "e-draft", attemptId: "a2", status: "DRAFT", score: 7.5, maxScore: 10, late: false, submittedAt: "2026-10-25T01:00:00Z", attemptCount: 1, version: 2 },
        { student: { accountId: "s3", name: "SV Final", email: "f@example.test" }, evaluationId: "e-final", attemptId: "a3", status: "FINALIZED", score: 9, maxScore: 10, late: false, submittedAt: "2026-10-25T01:00:00Z", attemptCount: 1, version: 3 },
      ],
    });
    render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await screen.findByRole("checkbox", { name: "Chọn SV Pending" });
    expect(screen.getByRole("link", { name: "Chấm bài" })).toHaveAttribute("href", "/teaching/evaluations/e-pending?status=ALL");
    expect(screen.getByRole("link", { name: "Tiếp tục chấm" })).toHaveAttribute("href", "/teaching/evaluations/e-draft?status=ALL");
    expect(screen.getByRole("link", { name: "Xem điểm" })).toHaveAttribute("href", "/teaching/evaluations/e-final?status=ALL");
    expect(screen.getByText("7.50 / 10.00")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("checkbox", { name: "Chọn SV Pending" }));
    expect(screen.getByText("Đã chọn 1 bài")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Bỏ chọn" }));
    expect(screen.queryByRole("button", { name: "Chốt điểm" })).not.toBeInTheDocument();
  });

  it("keeps unsubmitted rows unselectable and passes the current evaluation version to batch", async () => {
    render(<SubmissionDetailPage classId="c" assignmentId="a" />);
    await ready();
    expect(screen.getByRole("checkbox", { name: "Chọn Sinh viên B" })).toBeDisabled();
    fireEvent.click(screen.getByRole("checkbox", { name: "Chọn tất cả bài trong trang" }));
    fireEvent.click(screen.getByRole("button", { name: "Chốt điểm" }));
    expect(screen.getByTestId("batch-items")).toHaveTextContent(JSON.stringify([{ evaluationId: "e", version: 7 }]));
    mocks.listSubmissions.mockRejectedValueOnce(new Error("List refresh failed"));
    fireEvent.click(screen.getByRole("button", { name: "Refresh list" }));
    await screen.findByText("List refresh failed", { exact: false });
    expect(screen.getByText("Lớp: SE01 — Lớp một")).toBeInTheDocument();
    expect(mocks.loadAssignmentContext).toHaveBeenCalledTimes(1);
  });
});
