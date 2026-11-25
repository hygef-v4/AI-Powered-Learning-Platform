import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import {
  activateQuestion,
  bulkQuestions,
  getManagedSubjects,
  getQuestion,
  getQuestions,
  importQuestions,
  rejectQuestion,
  updateQuestion,
} from "@/features/quizzes/api";
import { QuestionDetailPage } from "@/features/quizzes/pages/QuestionDetailPage";
import { QuestionImportDialog } from "@/features/quizzes/components/QuestionImportDialog";
import { QuestionListPage } from "@/features/quizzes/pages/QuestionListPage";
import type { QuestionDetail, QuestionView } from "@/features/quizzes/model/types";

vi.mock("@/features/quizzes/api", () => ({
  getManagedSubjects: vi.fn(),
  getQuestions: vi.fn(),
  getQuestion: vi.fn(),
  createQuestion: vi.fn(),
  updateQuestion: vi.fn(),
  openDraft: vi.fn(),
  activateQuestion: vi.fn(),
  retireQuestion: vi.fn(),
  deleteQuestion: vi.fn(),
  importQuestions: vi.fn(),
  approveQuestion: vi.fn(),
  rejectQuestion: vi.fn(),
  bulkQuestions: vi.fn(),
  getDuplicateQuestions: vi.fn(),
}));

const push = vi.fn();
const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push, replace }), useSearchParams: () => new URLSearchParams(window.location.search) }));

const QUESTION: QuestionView = {
  id: "q1", lineageId: "q1", versionNo: 1, subjectId: "s1", type: "MCQ_SINGLE", stem: "Thủ đô của Việt Nam?",
  options: [{ id: "1", text: "Hà Nội" }, { id: "2", text: "Huế" }], correctOptionIds: ["1"], explanation: null,
  defaultPoints: 1, difficulty: "EASY", tags: ["địa lý"], status: "ACTIVE", activatedAt: "2026-10-20T08:00:00Z",
  createdBy: "u1", createdAt: "2026-10-20T08:00:00Z", updatedAt: "2026-10-20T08:00:00Z", version: 1,
};
const detail = (question: Partial<QuestionView> = {}, extra: Partial<QuestionDetail> = {}): QuestionDetail =>
  ({ question: { ...QUESTION, ...question }, versions: [], usedInQuizzes: 3, draftId: null, similar: [], review: null, quizzesShortOfQuestions: [], ...extra });

afterEach(() => vi.clearAllMocks());

describe("Duyệt câu giảng viên đề xuất (BR-U10-09)", () => {
  it("câu chờ duyệt chỉ duyệt hoặc từ chối kèm lý do, không sửa; hiện câu gần giống", async () => {
    const pending = detail({ status: "PENDING" }, { similar: [{ id: "q9", stem: "Thủ đô Việt Nam là gì?", status: "ACTIVE", percent: 82 }] });
    vi.mocked(getQuestion).mockResolvedValue(pending);
    vi.mocked(rejectQuestion).mockResolvedValue(detail({ status: "REJECTED" }, { review: { decision: "REJECTED", reason: "Trùng ý", reviewedAt: "2026-11-01T00:00:00Z" } }));
    render(<QuestionDetailPage id="q1" />);
    expect(await screen.findByTestId("similar-questions")).toHaveTextContent("82%");
    expect(screen.queryByTestId("question-detail-save")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("question-detail-reject"));
    expect(screen.getByTestId("question-detail-confirm")).toBeDisabled();
    fireEvent.change(screen.getByTestId("question-detail-reason"), { target: { value: " Trùng ý " } });
    fireEvent.click(screen.getByTestId("question-detail-confirm"));
    await waitFor(() => expect(rejectQuestion).toHaveBeenCalledWith("q1", "Trùng ý", 1));
    expect(await screen.findByText("Trùng ý")).toBeInTheDocument();
  });
});

describe("Thao tác hàng loạt và câu trùng trên Question List", () => {
  const pendingRow = { id: "q1", lineageId: "q1", versionNo: 1, type: "MCQ_SINGLE" as const, stem: "Câu đề xuất", difficulty: "EASY" as const,
    tags: [], status: "PENDING" as const, hasDraft: false, updatedAt: "2026-10-20T08:00:00Z", version: 7 };

  beforeEach(() => {
    vi.mocked(getManagedSubjects).mockResolvedValue([{ id: "s1", code: "SWT301", name: "Kiểm thử", status: "ACTIVE" }]);
    vi.mocked(getQuestions).mockResolvedValue({ items: [pendingRow], total: 1, page: 0, size: 20 });
  });
  afterEach(() => window.history.replaceState(null, "", "/"));

  it("mở từ thông báo thì lọc sẵn Chờ duyệt; duyệt hàng loạt kèm version và báo câu lỗi", async () => {
    window.history.replaceState(null, "", "/manager/questions?status=PENDING");
    vi.mocked(bulkQuestions).mockResolvedValue({ succeeded: 0, failed: 1, items: [{ id: "q1", ok: false, code: "QUESTION_DUPLICATE", message: "Trùng câu đã có" }] });
    render(<QuestionListPage />);
    await waitFor(() => expect(getQuestions).toHaveBeenCalledWith("s1", expect.objectContaining({ status: "PENDING" })));
    fireEvent.click(await screen.findByTestId("question-list-select"));
    fireEvent.click(screen.getByTestId("question-list-bulk-approve"));
    await waitFor(() => expect(bulkQuestions).toHaveBeenCalledWith("s1", "approve", [{ id: "q1", version: 7 }], undefined));
    expect(await screen.findByRole("alert")).toHaveTextContent("Trùng câu đã có");
  });

  it("từ chối hàng loạt bắt buộc lý do", async () => {
    window.history.replaceState(null, "", "/manager/questions?status=PENDING");
    vi.mocked(bulkQuestions).mockResolvedValue({ succeeded: 1, failed: 0, items: [{ id: "q1", ok: true, code: null, message: null }] });
    render(<QuestionListPage />);
    fireEvent.click(await screen.findByTestId("question-list-select"));
    fireEvent.click(screen.getByTestId("question-list-bulk-reject"));
    expect(screen.getByTestId("question-list-reject-confirm")).toBeDisabled();
    fireEvent.change(screen.getByTestId("question-list-reason"), { target: { value: "Chưa rõ" } });
    fireEvent.click(screen.getByTestId("question-list-reject-confirm"));
    await waitFor(() => expect(bulkQuestions).toHaveBeenCalledWith("s1", "reject", [{ id: "q1", version: 7 }], "Chưa rõ"));
  });
});

describe("QuestionListPage (UC 46)", () => {
  beforeEach(() => {
    vi.mocked(getManagedSubjects).mockResolvedValue([{ id: "s1", code: "SWT301", name: "Kiểm thử", status: "ACTIVE" }]);
    vi.mocked(getQuestions).mockResolvedValue({
      items: [{ id: "q1", lineageId: "q1", versionNo: 1, type: "MCQ_SINGLE", stem: "Thủ đô của Việt Nam?", difficulty: "EASY",
        tags: ["địa lý"], status: "ACTIVE", hasDraft: true, updatedAt: "2026-10-20T08:00:00Z", version: 1 }],
      total: 1, page: 0, size: 20,
    });
  });

  it("đổi tab và bộ lọc thì tải lại từ trang đầu", async () => {
    render(<QuestionListPage />);
    await screen.findByText("Thủ đô của Việt Nam?");
    fireEvent.click(screen.getByTestId("question-list-tab-MCQ_MULTI"));
    await waitFor(() => expect(getQuestions).toHaveBeenLastCalledWith("s1", expect.objectContaining({ type: "MCQ_MULTI", page: 0 })));
    fireEvent.change(screen.getByTestId("question-list-status"), { target: { value: "DRAFT" } });
    await waitFor(() => expect(getQuestions).toHaveBeenLastCalledWith("s1", expect.objectContaining({ status: "DRAFT" })));
  });


  it("restores type tab from URL after remount and ignores invalid types", async () => {
    const view = render(<QuestionListPage />);
    fireEvent.click(await screen.findByTestId("question-list-tab-MCQ_MULTI"));
    view.unmount();
    const restored = render(<QuestionListPage />);
    expect(await screen.findByTestId("question-list-tab-MCQ_MULTI")).toHaveAttribute("aria-selected", "true");
    window.history.replaceState(null, "", "?subjectId=s1&type=unknown");
    restored.rerender(<QuestionListPage />);
    expect(screen.getByTestId("question-list-tab-ALL")).toHaveAttribute("aria-selected", "true");
  });

  it("môn đã lưu trữ chỉ xem", async () => {
    vi.mocked(getManagedSubjects).mockResolvedValue([{ id: "s1", code: "SWT301", name: "Kiểm thử", status: "ARCHIVED" }]);
    render(<QuestionListPage />);
    expect(await screen.findByText("Môn đã lưu trữ, ngân hàng câu hỏi chỉ xem.")).toBeInTheDocument();
    expect(screen.queryByTestId("question-list-import")).not.toBeInTheDocument();
  });
});

describe("QuestionImportDialog (UC 47)", () => {

  it("tệp quá 5 MB bị chặn trước khi gửi", async () => {
    render(<QuestionImportDialog subjectId="s1" onClose={vi.fn()} onImported={vi.fn()} />);
    const big = new File(["x"], "lon.xlsx");
    Object.defineProperty(big, "size", { value: 6 * 1024 * 1024 });
    fireEvent.change(screen.getByTestId("question-import-file"), { target: { files: [big] } });
    expect(await screen.findByText("Tệp tối đa 5 MB.")).toBeInTheDocument();
    expect(importQuestions).not.toHaveBeenCalled();
  });
});

describe("QuestionDetailPage (UC 47)", () => {

  it("xung đột version thì báo và tải lại", async () => {
    vi.mocked(getQuestion).mockResolvedValue(detail({ status: "DRAFT", version: 4 }));
    vi.mocked(updateQuestion).mockRejectedValue(new ApiError({ status: 409, code: "QUESTION_VERSION_CONFLICT" }));
    render(<QuestionDetailPage id="q1" />);

    fireEvent.change(await screen.findByTestId("mcq-editor-stem"), { target: { value: "Câu đã sửa" } });
    fireEvent.click(screen.getByTestId("question-detail-save"));

    expect(await screen.findByText(/thay đổi ở phiên khác/)).toBeInTheDocument();
    await waitFor(() => expect(getQuestion).toHaveBeenCalledTimes(2));
  });

  it("chỉ kích hoạt khi đã lưu hết thay đổi; lỗi nội dung trên form chặn gửi", async () => {
    vi.mocked(getQuestion).mockResolvedValue(detail({ status: "DRAFT", version: 2 }));
    vi.mocked(activateQuestion).mockResolvedValue(detail({ status: "ACTIVE", version: 3 }));
    render(<QuestionDetailPage id="q1" />);

    const activate = await screen.findByTestId("question-detail-activate");
    expect(activate).toBeEnabled();
    fireEvent.change(screen.getByTestId("mcq-editor-stem"), { target: { value: " " } });
    expect(screen.getByTestId("question-detail-activate")).toBeDisabled();
    fireEvent.click(screen.getByTestId("question-detail-save"));
    expect(await screen.findByText("Nội dung câu hỏi không được để trống")).toBeInTheDocument();
    expect(updateQuestion).not.toHaveBeenCalled();
  });
});

beforeEach(() => { window.history.replaceState(null, "", "/manager?subjectId=s1"); });
