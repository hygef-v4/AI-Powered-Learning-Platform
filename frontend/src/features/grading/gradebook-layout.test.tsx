import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ClassGradebookTab } from "./ClassGradebookTab";
import type { GradebookPage } from "./types";

const mocks = vi.hoisted(() => ({
  gradebook: vi.fn(),
  export: vi.fn(),
}));

vi.mock("./api", () => ({ gradingApi: mocks }));

const longTitle1 =
  "Bài tập lớn số 1: Phân tích yêu cầu và thiết kế sơ đồ lớp chi tiết cho hệ thống quản lý sự kiện trường đại học";
const longTitle2 =
  "Bài tập lớn số 2: Thiết kế sơ đồ tuần tự và sơ đồ trạng thái cho quy trình đăng ký và điểm danh sự kiện";

const samplePage: GradebookPage = {
  assignments: [
    { id: "a-1", title: longTitle1, maxScore: 10, submissionMode: "INDIVIDUAL" },
    { id: "a-2", title: longTitle2, maxScore: 10, submissionMode: "GROUP" },
  ],
  rows: [
    {
      student: { accountId: "s-1", name: "Nguyễn Văn An", email: "an@school.test" },
      // Deliberately reversed order compared to assignments array to test assignmentId alignment
      grades: [
        {
          assignmentId: "a-2",
          evaluationId: null,
          status: "NOT_SUBMITTED",
          score: null,
          maxScore: 10,
          late: false,
          submittedAt: null,
          feedback: null,
        },
        {
          assignmentId: "a-1",
          evaluationId: "eval-a1-s1",
          status: "PUBLISHED",
          score: 10,
          maxScore: 10,
          late: false,
          submittedAt: "2026-10-25T10:00:00Z",
          feedback: "Tốt",
        },
      ],
    },
    {
      student: { accountId: "s-2", name: "Trần Thị Bình", email: "binh@school.test" },
      grades: [
        {
          assignmentId: "a-1",
          evaluationId: "eval-a1-s2",
          status: "DRAFT",
          score: 0,
          maxScore: 10,
          late: true,
          submittedAt: "2026-10-25T11:00:00Z",
          feedback: null,
        },
        // a-2 deliberately omitted from grades array to test missing grade handling
      ],
    },
  ],
  total: 25,
  page: 1,
  size: 20,
};

beforeEach(() => {
  vi.resetAllMocks();
  mocks.gradebook.mockResolvedValue(samplePage);
});

afterEach(cleanup);

describe("ClassGradebookTab layout and modes", () => {
  it("defaults to Theo bài tập mode, aligns grades by assignmentId, and preserves real 0.00 score vs missing evaluation", async () => {
    render(<ClassGradebookTab classId="class-1" />);

    const byAssignmentBtn = await screen.findByRole("button", { name: "Theo bài tập" });
    const matrixBtn = screen.getByRole("button", { name: "Tổng hợp" });
    expect(byAssignmentBtn).toHaveAttribute("aria-pressed", "true");
    expect(matrixBtn).toHaveAttribute("aria-pressed", "false");

    const selector = screen.getByRole("combobox", { name: "Bài tập" });
    expect(selector).toHaveValue("a-1");

    // Focused table headers: Sinh viên, Điểm, Trạng thái, Thao tác
    const headers = screen.getAllByRole("columnheader").map((th) => th.textContent?.trim());
    expect(headers).toEqual(["Sinh viên", "Điểm", "Trạng thái", "Thao tác"]);

    // Student 1 has 10.00 / 10.00 on a-1 (even though a-2 was first in row.grades)
    const rowAn = screen.getByText("Nguyễn Văn An").closest("tr")!;
    expect(within(rowAn).getByText("10.00 / 10.00")).toBeInTheDocument();
    expect(within(rowAn).getByText("Đã công bố")).toBeInTheDocument();
    expect(within(rowAn).getByRole("link", { name: "Xem điểm" })).toHaveAttribute(
      "href",
      "/teaching/evaluations/eval-a1-s1",
    );

    // Student 2 has real score 0 on a-1 -> must show 0.00 / 10.00, not —
    const rowBinh = screen.getByText("Trần Thị Bình").closest("tr")!;
    expect(within(rowBinh).getByText("0.00 / 10.00")).toBeInTheDocument();
    expect(within(rowBinh).getByText("Đã chấm (nháp)")).toBeInTheDocument();
    expect(within(rowBinh).getByText(/Trễ/)).toBeInTheDocument();
    expect(within(rowBinh).getByRole("link", { name: "Tiếp tục chấm" })).toHaveAttribute(
      "href",
      "/teaching/evaluations/eval-a1-s2",
    );

    // Switch to assignment a-2: no evaluation -> shows — and Chưa nộp without broken review link
    fireEvent.change(selector, { target: { value: "a-2" } });
    expect(within(rowAn).queryByRole("link")).not.toBeInTheDocument();
    expect(within(rowAn).getByText("Chưa nộp")).toBeInTheDocument();
    expect(within(rowBinh).queryByRole("link")).not.toBeInTheDocument();
    expect(within(rowBinh).getByText("Chưa nộp")).toBeInTheDocument();
  });

  it("supports duplicate assignment titles via UUID identity and falls back safely when selected assignment is removed", async () => {
    const dupPage: GradebookPage = {
      ...samplePage,
      assignments: [
        { id: "dup-1", title: "Bài kiểm tra", maxScore: 10, submissionMode: "INDIVIDUAL" },
        { id: "dup-2", title: "Bài kiểm tra", maxScore: 5, submissionMode: "INDIVIDUAL" },
      ],
      rows: [
        {
          student: { accountId: "s-1", name: "Nguyễn Văn An", email: "an@school.test" },
          grades: [
            { assignmentId: "dup-1", evaluationId: "e-1", status: "FINALIZED", score: 8, maxScore: 10, late: false, submittedAt: null, feedback: null },
            { assignmentId: "dup-2", evaluationId: "e-2", status: "PUBLISHED", score: 4.5, maxScore: 5, late: false, submittedAt: null, feedback: null },
          ],
        },
      ],
    };
    mocks.gradebook.mockResolvedValueOnce(dupPage);
    const { rerender } = render(<ClassGradebookTab classId="class-1" />);

    const selector = await screen.findByRole("combobox", { name: "Bài tập" });
    fireEvent.change(selector, { target: { value: "dup-2" } });
    expect(screen.getByText("4.50 / 5.00")).toBeInTheDocument();

    // Simulate refreshed data where dup-2 was removed
    mocks.gradebook.mockResolvedValueOnce({
      ...dupPage,
      assignments: [{ id: "dup-1", title: "Bài kiểm tra", maxScore: 10, submissionMode: "INDIVIDUAL" }],
    });
    fireEvent.change(screen.getByRole("searchbox", { name: /Tìm sinh viên/ }), { target: { value: "An" } });
    await waitFor(() => expect(mocks.gradebook).toHaveBeenLastCalledWith("class-1", "An", 1));
    expect(await screen.findByText("8.00 / 10.00")).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Bài tập" })).toHaveValue("dup-1");
    rerender(<ClassGradebookTab classId="class-1" />);
  });

  it("switches to Tổng hợp matrix with ordinals, accessible Danh mục bài tập disclosure, and assignmentId-aligned cells", async () => {
    render(<ClassGradebookTab classId="class-1" />);
    const matrixBtn = await screen.findByRole("button", { name: "Tổng hợp" });
    fireEvent.click(matrixBtn);

    expect(matrixBtn).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "Theo bài tập" })).toHaveAttribute("aria-pressed", "false");

    // Ordinals Bài 1 and Bài 2 in headers plus accessible Danh mục bài tập disclosure
    expect(screen.getByText("Bài 1")).toBeInTheDocument();
    expect(screen.getByText("Bài 2")).toBeInTheDocument();
    const directory = screen.getByText(/Danh mục bài tập/).closest("details")!;
    expect(directory).toBeInTheDocument();
    expect(within(directory).getByRole("link", { name: new RegExp(longTitle1) })).toHaveAttribute(
      "href",
      "/teaching/classes/class-1/assignments/a-1/submissions",
    );

    // Matrix row for Nguyễn Văn An aligns a-1 (first assignment col) and a-2 (second assignment col) despite reversed grades array
    const rowAn = screen.getByText("Nguyễn Văn An").closest("tr")!;
    const cellsAn = within(rowAn).getAllByRole("cell");
    expect(cellsAn[0]).toHaveTextContent("10.00 / 10.00");
    expect(cellsAn[1]).toHaveTextContent("—");
    expect(cellsAn[1]).toHaveTextContent("Chưa nộp");
  });

  it("preserves search and page when switching mode or assignment, resets page on search, and resets state on classId change", async () => {
    const { rerender } = render(<ClassGradebookTab classId="class-1" />);
    await screen.findByText("Nguyễn Văn An");

    // Go to page 2
    fireEvent.click(screen.getByTestId("pagination-page-2-button"));
    await waitFor(() => expect(mocks.gradebook).toHaveBeenLastCalledWith("class-1", "", 2));

    // Switch assignment and mode -> does not reset page or trigger redundant fetch
    fireEvent.change(screen.getByRole("combobox", { name: "Bài tập" }), { target: { value: "a-2" } });
    fireEvent.click(screen.getByRole("button", { name: "Tổng hợp" }));
    fireEvent.click(screen.getByRole("button", { name: "Theo bài tập" }));
    expect(mocks.gradebook).toHaveBeenCalledTimes(2);
    expect(screen.getByRole("combobox", { name: "Bài tập" })).toHaveValue("a-2");

    // Changing search resets page to 1
    fireEvent.change(screen.getByRole("searchbox", { name: /Tìm sinh viên/ }), { target: { value: "Bình" } });
    await waitFor(() => expect(mocks.gradebook).toHaveBeenLastCalledWith("class-1", "Bình", 1));

    // Changing classId resets query, page, and assignment selection
    rerender(<ClassGradebookTab classId="class-2" />);
    await waitFor(() => expect(mocks.gradebook).toHaveBeenLastCalledWith("class-2", "", 1));
    expect(screen.getByRole("searchbox", { name: /Tìm sinh viên/ })).toHaveValue("");
  });

  it("keeps explicit export scope in GradebookExportDialog and renders empty/error states with retry", async () => {
    mocks.export.mockResolvedValue(new Blob(["csv"], { type: "text/csv" }));
    URL.createObjectURL = vi.fn(() => "blob:test");
    URL.revokeObjectURL = vi.fn();

    render(<ClassGradebookTab classId="class-1" />);
    await screen.findByText("Nguyễn Văn An");

    fireEvent.click(screen.getByRole("button", { name: "Xuất sổ điểm" }));
    const dialog = screen.getByRole("dialog", { name: "Xuất sổ điểm" });

    fireEvent.change(within(dialog).getByRole("combobox", { name: /Phạm vi/ }), { target: { value: "a-2" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Xuất tệp" }));
    await waitFor(() => expect(mocks.export).toHaveBeenCalledWith("class-1", "csv", "a-2"));
  });

  it("renders readable empty states and retryable error state", async () => {
    mocks.gradebook
      .mockRejectedValueOnce(new Error("Mất kết nối máy chủ"))
      .mockResolvedValueOnce({ ...samplePage, assignments: [], rows: [], total: 0 })
      .mockResolvedValueOnce({ ...samplePage, rows: [], total: 0 });

    render(<ClassGradebookTab classId="class-1" />);
    expect(await screen.findByRole("alert")).toHaveTextContent("Mất kết nối máy chủ");

    fireEvent.click(screen.getByRole("button", { name: "Thử tải lại" }));
    expect(await screen.findByText("Chưa có bài tính điểm trong lớp.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Xuất sổ điểm" })).toBeDisabled();

    fireEvent.change(screen.getByRole("searchbox", { name: /Tìm sinh viên/ }), { target: { value: "không có" } });
    expect(await screen.findByText("Không có sinh viên phù hợp.")).toBeInTheDocument();
  });
});
