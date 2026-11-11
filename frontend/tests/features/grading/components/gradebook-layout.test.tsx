import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ClassGradebookTab } from "@/features/grading/components/ClassGradebookTab";
import type { GradebookPage } from "@/features/grading/model/types";

const mocks = vi.hoisted(() => ({
  loadGradebook: vi.fn(),
  listSubmissions: vi.fn(),
  loadSubmissionProgress: vi.fn(),
  exportGrades: vi.fn(),
}));

vi.mock("@/features/grading/api", () => ({ gradingApi: mocks }));
const push = vi.hoisted(() => vi.fn());
vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams(window.location.search), useRouter: () => ({ push }) }));

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
  window.history.replaceState(null, "", "/");
  mocks.loadGradebook.mockResolvedValue(samplePage);
  mocks.listSubmissions.mockResolvedValue({ rows: [], total: 0 });
  mocks.loadSubmissionProgress.mockResolvedValue({ "a-1": { rows: [], total: 2, page: 1, size: 20, deadline: null, summary: { total: 2, submitted: 2, PENDING: 2 } } });
});

afterEach(cleanup);

describe("ClassGradebookTab: danh sách – chi tiết", () => {
  it("links directly to each submitted group's evaluation above the member grades", async () => {
    mocks.listSubmissions.mockResolvedValue({
      rows: [
        { student: { accountId: "group-1", name: "Nhóm 1" }, evaluationId: "group-eval-1" },
        { student: { accountId: "group-2", name: "Nhóm 2" }, evaluationId: "group-eval-2" },
        { student: { accountId: "group-3", name: "Nhóm 3" }, evaluationId: null },
      ],
      total: 3,
    });
    render(<ClassGradebookTab classId="class-1" />);

    const groupAssignment = await screen.findByRole("button", { name: new RegExp(longTitle2) });
    expect(screen.queryByRole("link", { name: /Xem bản nộp chung/ })).not.toBeInTheDocument();
    expect(mocks.listSubmissions).not.toHaveBeenCalled();
    fireEvent.click(groupAssignment);

    const groups = await screen.findByRole("list", { name: "Bản nộp chung theo nhóm" });
    expect(within(groups).getByRole("link", { name: "Xem bản nộp chung của Nhóm 1" }))
      .toHaveAttribute("href", "/teaching/evaluations/group-eval-1");
    expect(within(groups).getByRole("link", { name: "Xem bản nộp chung của Nhóm 2" }))
      .toHaveAttribute("href", "/teaching/evaluations/group-eval-2");
    expect(within(groups).queryByText("Nhóm 3")).not.toBeInTheDocument();
    expect(mocks.listSubmissions).toHaveBeenCalledWith("class-1", "a-2", "ALL", 1);
  });

  it("Theo bài tập: danh sách bài bên trái (chỉ ghi loại), bảng điểm của bài đang chọn bên phải, căn theo assignmentId", async () => {
    render(<ClassGradebookTab classId="class-1" />);

    const first = await screen.findByRole("button", { name: new RegExp(longTitle1) });
    expect(first).toHaveAttribute("aria-pressed", "true");
    expect(first).toHaveTextContent(/Cá nhân$/);
    expect(first).not.toHaveTextContent("chờ chấm");
    expect(screen.queryByRole("link", { name: "Xem bài nộp" })).not.toBeInTheDocument();

    const rowAn = screen.getByText("Nguyễn Văn An").closest("tr")!;
    expect(within(rowAn).getByText("10 / 10")).toBeInTheDocument();
    expect(within(rowAn).getByText("Đã công bố")).toBeInTheDocument();
    expect(within(rowAn).getByRole("link", { name: "Xem điểm" })).toHaveAttribute("href", "/teaching/evaluations/eval-a1-s1");
    const rowBinh = screen.getByText("Trần Thị Bình").closest("tr")!;
    expect(within(rowBinh).getByText("0 / 10")).toBeInTheDocument();
    expect(within(rowBinh).getByText(/Trễ/)).toBeInTheDocument();
    expect(within(rowBinh).getByRole("link", { name: "Tiếp tục chấm" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: new RegExp(longTitle2) }));
    // Chưa có lượt chấm: dòng không có link hay nút điều hướng.
    const rowAn2 = screen.getByText("Nguyễn Văn An").closest("tr")!;
    expect(within(rowAn2).queryByRole("link")).not.toBeInTheDocument();
    expect(within(rowAn2).getByText("Chưa nộp")).toBeInTheDocument();
    fireEvent.click(rowAn2);
    expect(push).not.toHaveBeenCalledWith(expect.stringContaining("submissions"));
    fireEvent.click(screen.getByRole("button", { name: new RegExp(longTitle1) }));
    fireEvent.click(screen.getByText("Nguyễn Văn An").closest("tr")!);
    expect(push).toHaveBeenLastCalledWith("/teaching/evaluations/eval-a1-s1");
    expect(screen.queryByRole("columnheader", { name: /Bài 1/ })).not.toBeInTheDocument();
  });

  it("lọc theo trạng thái trên bảng điểm", async () => {
    render(<ClassGradebookTab classId="class-1" />);
    await screen.findByText("Nguyễn Văn An");
    fireEvent.change(screen.getByRole("combobox", { name: "Lọc theo trạng thái" }), { target: { value: "DRAFT" } });
    expect(screen.queryByText("Nguyễn Văn An")).not.toBeInTheDocument();
    expect(screen.getByText("Trần Thị Bình")).toBeInTheDocument();
  });

  it("mở từ Tiến độ nộp bài: ?assignment= chọn sẵn bài, ?student= mở chế độ Theo sinh viên với người đó", async () => {
    window.history.replaceState(null, "", "/?assignment=a-2");
    const { unmount } = render(<ClassGradebookTab classId="class-1" />);
    expect(await screen.findByRole("button", { name: new RegExp(longTitle2) })).toHaveAttribute("aria-pressed", "true");
    unmount();

    window.history.replaceState(null, "", "/?student=s-2");
    render(<ClassGradebookTab classId="class-1" />);
    expect(await screen.findByRole("button", { name: /Trần Thị Bình/ })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("tab", { name: "Theo sinh viên" })).toHaveAttribute("aria-selected", "true");
  });

  it("tìm bài trong danh sách bên trái (lọc tại chỗ, không gọi lại máy chủ)", async () => {
    render(<ClassGradebookTab classId="class-1" />);
    await screen.findByRole("button", { name: new RegExp(longTitle1) });
    fireEvent.change(screen.getByRole("searchbox", { name: "Tìm bài tập" }), { target: { value: "số 2" } });
    expect(screen.queryByRole("button", { name: new RegExp(longTitle1) })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: new RegExp(longTitle2) })).toBeInTheDocument();
    expect(mocks.loadGradebook).toHaveBeenCalledTimes(1);
  });

  it("Theo sinh viên: danh sách sinh viên, bảng điểm mọi bài của người đang chọn có ô tìm bài; tìm sinh viên hỏi máy chủ, về trang 1", async () => {
    render(<ClassGradebookTab classId="class-1" />);
    fireEvent.click(await screen.findByRole("tab", { name: "Theo sinh viên" }));

    const binh = screen.getByRole("button", { name: /Trần Thị Bình/ });
    expect(binh).not.toHaveTextContent("1/2");
    fireEvent.click(binh);
    const rowA1 = screen.getByText(longTitle1).closest("tr")!;
    expect(within(rowA1).getByText("0 / 10")).toBeInTheDocument();
    expect(within(rowA1).getByRole("link", { name: "Tiếp tục chấm" })).toBeInTheDocument();
    const rowA2 = screen.getByText(longTitle2).closest("tr")!;
    expect(within(rowA2).getByText("Chưa nộp")).toBeInTheDocument();

    fireEvent.change(screen.getByRole("searchbox", { name: "Tìm bài tập" }), { target: { value: "số 2" } });
    expect(screen.queryByText(longTitle1)).not.toBeInTheDocument();
    expect(screen.getByText(longTitle2)).toBeInTheDocument();

    fireEvent.change(screen.getByRole("searchbox", { name: "Tìm sinh viên" }), { target: { value: "Bình" } });
    await waitFor(() => expect(mocks.loadGradebook).toHaveBeenLastCalledWith("class-1", "Bình", 1));
  });

  it("nút Xuất bảng điểm lên chỗ của dòng tiêu đề và giữ phạm vi xuất đã chọn", async () => {
    mocks.exportGrades.mockResolvedValue({ blob: new Blob(["xlsx"]), filename: "Bài tập tuần 1.xlsx" });
    const download = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(function (this: HTMLAnchorElement) {
      expect(this.download).toBe("Bài tập tuần 1.xlsx");
    });
    URL.createObjectURL = vi.fn(() => "blob:test");
    URL.revokeObjectURL = vi.fn();
    const host = document.createElement("div");
    document.body.appendChild(host);

    render(<ClassGradebookTab classId="class-1" headerActions={host} />);
    await screen.findByText("Nguyễn Văn An");
    fireEvent.click(within(host).getByRole("button", { name: "Xuất bảng điểm" }));
    const dialog = screen.getByRole("dialog", { name: "Xuất bảng điểm" });
    expect(within(dialog).getByRole("combobox", { name: /Phạm vi/ })).toHaveValue("a-1");
    fireEvent.change(within(dialog).getByRole("combobox", { name: /Phạm vi/ }), { target: { value: "a-2" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Xuất tệp" }));
    // Mặc định là Excel, đứng trước CSV.
    expect(within(dialog).getAllByRole("radio").map((r) => r.textContent)).toEqual(["Excel", "CSV"]);
    await waitFor(() => expect(mocks.exportGrades).toHaveBeenCalledWith("class-1", "xlsx", "a-2"));
    await waitFor(() => expect(download).toHaveBeenCalledOnce());
    download.mockRestore();
    host.remove();
  });

  it("lỗi tải có Thử tải lại; lớp chưa có bài tính điểm; không có sinh viên phù hợp", async () => {
    mocks.loadGradebook
      .mockRejectedValueOnce(new Error("Mất kết nối máy chủ"))
      .mockResolvedValueOnce({ ...samplePage, assignments: [], rows: [], total: 0 });

    const { unmount } = render(<ClassGradebookTab classId="class-1" />);
    expect(await screen.findByRole("alert")).toHaveTextContent("Mất kết nối máy chủ");
    fireEvent.click(screen.getByRole("button", { name: "Thử tải lại" }));
    expect(await screen.findByText("Chưa có bài tính điểm trong lớp.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Xuất bảng điểm" })).toBeDisabled();
    unmount();

    mocks.loadGradebook.mockResolvedValue({ ...samplePage, rows: [], total: 0 });
    render(<ClassGradebookTab classId="class-1" />);
    expect(await screen.findByText("Không có sinh viên phù hợp.")).toBeInTheDocument();
  });
});

it("restores the selected student, mode and filters after returning", async () => {
  window.history.replaceState(null, "", "/teaching/classes/class-1?tab=gradebook&keep=yes");
  const first = render(<ClassGradebookTab classId="class-1" />);
  fireEvent.click(await screen.findByRole("tab", { name: "Theo sinh viên" }));
  fireEvent.click(await screen.findByRole("button", { name: /Trần Thị Bình/ }));
  fireEvent.change(screen.getByRole("searchbox", { name: "Tìm bài tập" }), { target: { value: "số 1" } });
  fireEvent.change(screen.getByRole("combobox", { name: "Lọc theo trạng thái" }), { target: { value: "DRAFT" } });
  expect(new URLSearchParams(window.location.search).get("keep")).toBe("yes");
  expect(new URLSearchParams(window.location.search).get("student")).toBe("s-2");
  first.unmount();
  render(<ClassGradebookTab classId="class-1" />);
  expect(await screen.findByRole("button", { name: /Trần Thị Bình/ })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("tab", { name: "Theo sinh viên" })).toHaveAttribute("aria-selected", "true");
  expect(screen.getByRole("searchbox", { name: "Tìm bài tập" })).toHaveValue("số 1");
  expect(screen.getByRole("combobox", { name: "Lọc theo trạng thái" })).toHaveValue("DRAFT");
});

it("validates gradebook URL pagination before calling the API", async () => {
  window.history.replaceState(null, "", "/?gradebookPage=NaN&gradebookMode=invalid");
  render(<ClassGradebookTab classId="class-1" />);
  await screen.findByRole("button", { name: new RegExp(longTitle1) });
  expect(mocks.loadGradebook).toHaveBeenCalledWith("class-1", "", 1);
  expect(screen.getByRole("tab", { name: "Theo bài tập" })).toHaveAttribute("aria-selected", "true");
});

it("restores gradebook search and pagination while typing replaces the current entry", async () => {
  window.history.replaceState(null, "", "/?gradebookQuery=Bình&gradebookPage=2&assignment=a-2");
  render(<ClassGradebookTab classId="class-1" />);
  expect(await screen.findByRole("button", { name: new RegExp(longTitle2) })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("searchbox", { name: "Tìm sinh viên" })).toHaveValue("Bình");
  await waitFor(() => expect(mocks.loadGradebook).toHaveBeenLastCalledWith("class-1", "Bình", 2));
  const length = window.history.length;
  fireEvent.change(screen.getByRole("searchbox", { name: "Tìm sinh viên" }), { target: { value: "An" } });
  expect(window.history.length).toBe(length);
  expect(new URLSearchParams(window.location.search).get("gradebookPage")).toBe("1");
  await waitFor(() => expect(mocks.loadGradebook).toHaveBeenLastCalledWith("class-1", "An", 1));
});


it("offers batch selection with server versions in assignment mode", async () => {
  mocks.listSubmissions.mockResolvedValue({ rows: [{ student: { accountId: "s1", name: "An" },
    evaluationId: "batch-e1", version: 7, status: "DRAFT" }], total: 1 });
  render(<ClassGradebookTab classId="class-1" />);
  fireEvent.click(await screen.findByRole("button", { name: "Thao tác hàng loạt" }));
  fireEvent.click(await screen.findByRole("checkbox", { name: "Chọn An" }));
  expect(screen.getByRole("button", { name: "Công bố đã chọn" })).toBeEnabled();
});

it.each([
  ["FINALIZED", true, false], ["PUBLISHED", true, true], ["PENDING", true, true], ["DRAFT", false, false],
] as const)("only offers valid grade actions for a %s submission", async (status, _finalizeDisabled, publishDisabled) => {
  mocks.listSubmissions.mockResolvedValue({ rows: [{ student: { accountId: "s1", name: "An" },
    evaluationId: "batch-e1", version: 7, status }], total: 1 });
  render(<ClassGradebookTab classId="class-1" />);
  fireEvent.click(await screen.findByRole("button", { name: "Thao tác hàng loạt" }));
  fireEvent.click(await screen.findByRole("checkbox", { name: "Chọn An" }));
  const finalize = screen.queryByRole("button", { name: "Chốt điểm đã chọn" });
  const publish = screen.getByRole("button", { name: "Công bố đã chọn" });
  expect(finalize).not.toBeInTheDocument();
  expect(publish).toHaveProperty("disabled", publishDisabled);
});
