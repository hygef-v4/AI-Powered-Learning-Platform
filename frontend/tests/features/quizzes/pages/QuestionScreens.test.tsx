import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { activateQuestion, getManagedSubjects, getQuestion, getQuestions, importQuestions, openDraft, updateQuestion } from "@/features/quizzes/api";
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
}));

const push = vi.fn();
const replace = vi.fn();
let search = new URLSearchParams("subjectId=s1");
vi.mock("next/navigation", () => ({ useRouter: () => ({ push, replace }), useSearchParams: () => search }));

const QUESTION: QuestionView = {
  id: "q1", lineageId: "q1", versionNo: 1, subjectId: "s1", type: "MCQ_SINGLE", stem: "Thủ đô của Việt Nam?",
  options: [{ id: "1", text: "Hà Nội" }, { id: "2", text: "Huế" }], correctOptionIds: ["1"], explanation: null,
  defaultPoints: 1, difficulty: "EASY", tags: ["địa lý"], status: "ACTIVE", activatedAt: "2026-10-20T08:00:00Z",
  createdBy: "u1", createdAt: "2026-10-20T08:00:00Z", updatedAt: "2026-10-20T08:00:00Z", version: 1,
};
const detail = (question: Partial<QuestionView> = {}, extra: Partial<QuestionDetail> = {}): QuestionDetail =>
  ({ question: { ...QUESTION, ...question }, versions: [], usedInQuizzes: 3, draftId: null, ...extra });

afterEach(() => vi.clearAllMocks());

describe("QuestionListPage (UC 46)", () => {
  beforeEach(() => {
    search = new URLSearchParams("subjectId=s1");
    vi.mocked(getManagedSubjects).mockResolvedValue([{ id: "s1", code: "SWT301", name: "Kiểm thử", status: "ACTIVE" }]);
    vi.mocked(getQuestions).mockResolvedValue({
      items: [{ id: "q1", lineageId: "q1", versionNo: 1, type: "MCQ_SINGLE", stem: "Thủ đô của Việt Nam?", difficulty: "EASY",
        tags: ["địa lý"], status: "ACTIVE", hasDraft: true, updatedAt: "2026-10-20T08:00:00Z" }],
      total: 1, page: 0, size: 20,
    });
  });

  it("hiện câu của môn kèm cờ có bản nháp mới; bấm dòng mở chi tiết", async () => {
    render(<QuestionListPage />);
    expect(await screen.findByText("Thủ đô của Việt Nam?")).toBeInTheDocument();
    expect(screen.getByText("Có bản nháp mới")).toBeInTheDocument();
    expect(getQuestions).toHaveBeenCalledWith("s1", expect.objectContaining({ page: 0, size: 20 }));
    fireEvent.click(screen.getByTestId("question-list-row"));
    expect(push).toHaveBeenCalledWith("/manager/questions/q1");
  });

  it("đổi tab và bộ lọc thì tải lại từ trang đầu", async () => {
    render(<QuestionListPage />);
    await screen.findByText("Thủ đô của Việt Nam?");
    fireEvent.click(screen.getByTestId("question-list-tab-MCQ_MULTI"));
    await waitFor(() => expect(getQuestions).toHaveBeenLastCalledWith("s1", expect.objectContaining({ type: "MCQ_MULTI", page: 0 })));
    fireEvent.change(screen.getByTestId("question-list-status"), { target: { value: "DRAFT" } });
    await waitFor(() => expect(getQuestions).toHaveBeenLastCalledWith("s1", expect.objectContaining({ status: "DRAFT" })));
  });

  it("môn đã lưu trữ chỉ xem", async () => {
    vi.mocked(getManagedSubjects).mockResolvedValue([{ id: "s1", code: "SWT301", name: "Kiểm thử", status: "ARCHIVED" }]);
    render(<QuestionListPage />);
    expect(await screen.findByText("Môn đã lưu trữ, ngân hàng câu hỏi chỉ xem.")).toBeInTheDocument();
    expect(screen.queryByTestId("question-list-import")).not.toBeInTheDocument();
  });
});

describe("QuestionImportDialog (UC 47)", () => {
  it("kiểm tệp trước rồi mới nhập các dòng hợp lệ", async () => {
    vi.mocked(importQuestions)
      .mockResolvedValueOnce({ dryRun: true, total: 2, valid: 1, invalid: 1, created: 0, rows: [
        { row: 2, stem: "Câu đúng", optionCount: 2, valid: true, reasons: [] },
        { row: 3, stem: "Câu lỗi", optionCount: 1, valid: false, reasons: [{ field: "options", code: "OPTION_COUNT", message: "Cần 2–6 lựa chọn" }] },
      ] })
      .mockResolvedValueOnce({ dryRun: false, total: 2, valid: 1, invalid: 1, created: 1, rows: [] });
    const onImported = vi.fn();
    render(<QuestionImportDialog subjectId="s1" onClose={vi.fn()} onImported={onImported} />);
    const file = new File(["x"], "cau-hoi.csv", { type: "text/csv" });

    fireEvent.change(screen.getByTestId("question-import-file"), { target: { files: [file] } });

    expect(await screen.findByText("Cần 2–6 lựa chọn")).toBeInTheDocument();
    expect(importQuestions).toHaveBeenCalledWith("s1", file, true);
    fireEvent.click(screen.getByTestId("question-import-commit"));
    await waitFor(() => expect(onImported).toHaveBeenCalledWith(1));
    expect(importQuestions).toHaveBeenLastCalledWith("s1", file, false);
  });

  it("tệp quá 5 MB bị chặn trước khi gửi", async () => {
    render(<QuestionImportDialog subjectId="s1" onClose={vi.fn()} onImported={vi.fn()} />);
    const big = new File(["x"], "lon.csv");
    Object.defineProperty(big, "size", { value: 6 * 1024 * 1024 });
    fireEvent.change(screen.getByTestId("question-import-file"), { target: { files: [big] } });
    expect(await screen.findByText("Tệp tối đa 5 MB.")).toBeInTheDocument();
    expect(importQuestions).not.toHaveBeenCalled();
  });
});

describe("QuestionDetailPage (UC 47)", () => {
  it("sửa câu đang dùng thì mở bản nháp mới, lưu vào đó rồi chuyển sang bản nháp", async () => {
    vi.mocked(getQuestion).mockResolvedValue(detail());
    vi.mocked(openDraft).mockResolvedValue(detail({ id: "q2", versionNo: 2, status: "DRAFT", version: 0 }));
    vi.mocked(updateQuestion).mockResolvedValue(detail({ id: "q2", versionNo: 2, status: "DRAFT", version: 1 }));
    render(<QuestionDetailPage id="q1" />);

    expect(await screen.findByText(/đang dùng trong 3 quiz/i)).toBeInTheDocument();
    fireEvent.change(screen.getByTestId("mcq-editor-stem"), { target: { value: "Thủ đô nước ta?" } });
    fireEvent.click(screen.getByTestId("question-detail-save"));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/manager/questions/q2"));
    expect(openDraft).toHaveBeenCalledWith("q1");
    expect(updateQuestion).toHaveBeenCalledWith("q2", expect.objectContaining({ stem: "Thủ đô nước ta?" }), 0);
  });

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

  it("thêm câu mà chưa chọn môn thì nhắc chọn môn", () => {
    render(<QuestionDetailPage id="new" subjectId={null} />);
    expect(screen.getByText("Chưa chọn môn")).toBeInTheDocument();
  });
});
