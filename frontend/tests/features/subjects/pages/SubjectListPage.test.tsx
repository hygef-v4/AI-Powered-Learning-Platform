import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { Toaster } from "@/shared/ui/Toast";
import { createSubject, getSubjects } from "@/features/subjects/api";
import { SubjectListPage } from "@/features/subjects/pages/SubjectListPage";
import type { SubjectDetail, SubjectListItem, SubjectPage } from "@/features/subjects/model/types";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
vi.mock("@/features/subjects/api", () => ({ getSubjects: vi.fn(), createSubject: vi.fn() }));

function item(overrides: Partial<SubjectListItem> = {}): SubjectListItem {
  return { id: "subject-a", code: "ABC", name: "Môn A", status: "ACTIVE", managerId: null, manager: null, classCount: 0, ...overrides };
}
function page(items = [item()], overrides: Partial<SubjectPage> = {}): SubjectPage {
  return { items, page: 0, size: 20, totalElements: items.length, statusCounts: { ACTIVE: items.length, ARCHIVED: 0 }, ...overrides };
}
function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => { resolve = done; });
  return { promise, resolve };
}

describe("SubjectListPage", () => {
  beforeEach(() => { vi.mocked(getSubjects).mockReset().mockResolvedValue(page()); vi.mocked(createSubject).mockReset(); push.mockReset(); });
  afterEach(() => { cleanup(); vi.useRealTimers(); });

  it("POST thành công mới đóng dialog/refetch; lỗi refetch không biến thành lỗi tạo và mở lại draft rỗng", async () => {
    vi.mocked(createSubject).mockResolvedValue({ ...item(), description: null, version: 1, createdAt: "2026-10-18T00:00:00Z", updatedAt: "2026-10-18T00:00:00Z" } satisfies SubjectDetail);
    render(<><SubjectListPage /><Toaster /></>);
    await screen.findByRole("link", { name: "Môn A" });
    fireEvent.click(screen.getByTestId("subjects-create-open"));
    fireEvent.change(screen.getByTestId("subject-create-code"), { target: { value: "ABC" } });
    fireEvent.change(screen.getByTestId("subject-create-name"), { target: { value: "Môn A" } });
    vi.mocked(getSubjects).mockRejectedValueOnce(new Error("refresh failed"));
    fireEvent.submit(screen.getByTestId("subject-create-form"));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(await screen.findByText("Đã thêm môn học")).toBeInTheDocument();
    await screen.findByText(/Không tải được dữ liệu môn học/);
    expect(createSubject).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByTestId("subjects-read-retry"));
    await screen.findByRole("link", { name: "Môn A" });
    fireEvent.click(screen.getByTestId("subjects-create-open"));
    expect(screen.getByTestId("subject-create-code")).toHaveValue("");
  });

  it("sau create/refetch thành công trả focus về nút mở khi nút đã hết disabled", async () => {
    vi.mocked(createSubject).mockResolvedValue({ ...item(), description: null, version: 1, createdAt: "2026-10-18T00:00:00Z", updatedAt: "2026-10-18T00:00:00Z" });
    render(<SubjectListPage />);
    await screen.findByRole("link", { name: "Môn A" });
    fireEvent.click(screen.getByTestId("subjects-create-open"));
    fireEvent.change(screen.getByTestId("subject-create-code"), { target: { value: "ABC" } });
    fireEvent.change(screen.getByTestId("subject-create-name"), { target: { value: "Môn A" } });
    fireEvent.submit(screen.getByTestId("subject-create-form"));
    await waitFor(() => expect(screen.getByTestId("subjects-create-open")).toHaveFocus());
  });

  it("tải API thật qua boundary, hiển thị số liệu và link chi tiết", async () => {
    render(<SubjectListPage />);
    expect(screen.getByRole("status")).toHaveTextContent("Đang tải");
    expect(await screen.findByRole("link", { name: "Môn A" })).toHaveAttribute("href", "/admin/subjects/subject-a");
    expect(getSubjects).toHaveBeenCalledWith({ q: "", status: undefined, page: 0, size: 20 });
    expect(screen.getByTestId("subjects-filter-ALL")).toHaveAttribute("aria-selected", "true");
    // Không còn dòng đếm số môn dưới tiêu đề.
    expect(screen.queryByText("1 môn học")).not.toBeInTheDocument();
    fireEvent.click(screen.getByText("ABC"));
    expect(push).toHaveBeenCalledWith("/admin/subjects/subject-a");
  });

  it("phân biệt chưa phân công và contact không tra được; không có cột Số lớp", async () => {
    vi.mocked(getSubjects).mockResolvedValue(page([
      item(), item({ id: "b", code: "DEF", name: "Môn B", managerId: "missing", classCount: null }),
      item({ id: "c", code: "XYZ", name: "Môn C", managerId: "manager", manager: { accountId: "manager", displayName: "Chủ nhiệm", email: "m@example.test" }, classCount: 3 }),
    ]));
    render(<SubjectListPage />);
    expect(await screen.findByText("Không tra được chủ nhiệm")).toBeInTheDocument();
    expect(screen.getByText("Chưa phân công")).toBeInTheDocument();
    expect(screen.getByText("Chủ nhiệm")).toBeInTheDocument();
    expect(screen.queryByRole("columnheader", { name: "Số lớp" })).not.toBeInTheDocument();
  });

  it("debounce300ms và response cũ không ghi đè kết quả mới", async () => {
    vi.useFakeTimers();
    const first = deferred<SubjectPage>(), second = deferred<SubjectPage>();
    vi.mocked(getSubjects).mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    render(<SubjectListPage />);
    await act(async () => {});
    fireEvent.change(screen.getByLabelText("Lọc theo mã hoặc tên môn"), { target: { value: "  mới  " } });
    await act(async () => { vi.advanceTimersByTime(299); });
    expect(getSubjects).toHaveBeenCalledTimes(1);
    await act(async () => { vi.advanceTimersByTime(1); });
    expect(getSubjects).toHaveBeenLastCalledWith({ q: "mới", status: undefined, page: 0, size: 20 });
    await act(async () => { second.resolve(page([item({ name: "Kết quả mới" })])); });
    expect(screen.getByText("Kết quả mới")).toBeInTheDocument();
    await act(async () => { first.resolve(page([item({ name: "Kết quả cũ" })])); });
    expect(screen.queryByText("Kết quả cũ")).not.toBeInTheDocument();
    expect(screen.getByText("Kết quả mới")).toBeInTheDocument();
  });

  it("đổi tab reset trang, thanh trang dùng totalElements API và không có cột STT", async () => {
    vi.mocked(getSubjects).mockImplementation(async (query) => page([item()], { page: query?.page ?? 0, size: query?.size ?? 20,
      totalElements: 41, statusCounts: { ACTIVE: 41, ARCHIVED: 2 } }));
    render(<SubjectListPage />);
    await screen.findByText("Môn A");
    expect(screen.queryByRole("columnheader", { name: "STT" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Trang trước" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Trang sau" }));
    await waitFor(() => expect(getSubjects).toHaveBeenLastCalledWith({ q: "", status: undefined, page: 1, size: 20 }));
    await waitFor(() => expect(screen.getByRole("button", { name: "Trang 2" })).toHaveAttribute("aria-current", "page"));
    fireEvent.click(screen.getByTestId("subjects-filter-ARCHIVED"));
    // Đổi tab: bảng cũ vẫn ở đó (mờ đi) trong lúc tải, không thay bằng dòng "Đang tải".
    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(screen.queryByText("Đang tải môn học…")).not.toBeInTheDocument();
    await waitFor(() => expect(getSubjects).toHaveBeenLastCalledWith({ q: "", status: "ARCHIVED", page: 0, size: 20 }));
  });

  it("empty không bị coi là lỗi và không bật trang sau", async () => {
    vi.mocked(getSubjects).mockResolvedValue(page([]));
    render(<SubjectListPage />);
    expect(await screen.findByText("Không có môn học phù hợp với bộ lọc.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Trang sau" })).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("lỗi API hiện retry, không hiển thị mẫu hoặc kết quả rỗng giả", async () => {
    vi.mocked(getSubjects).mockRejectedValueOnce(new ApiError({ status: 503, detail: "internal SQL" })).mockResolvedValueOnce(page([]));
    render(<SubjectListPage />);
    expect(await screen.findByRole("alert")).toHaveTextContent("Không tải được dữ liệu");
    expect(screen.queryByText("internal SQL")).not.toBeInTheDocument();
    expect(screen.queryByText("Không có môn học phù hợp với bộ lọc.")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(await screen.findByText("Không có môn học phù hợp với bộ lọc.")).toBeInTheDocument();
    expect(getSubjects).toHaveBeenCalledTimes(2);
  });

  it.each([401, 403])("HTTP%s không hiển thị dữ liệu hay dùng layout ADMIN làm quyền", async (status) => {
    vi.mocked(getSubjects).mockRejectedValue(new ApiError({ status }));
    render(<SubjectListPage />);
    const error = await screen.findByRole("alert");
    expect(error).toHaveTextContent(status === 401 ? "Vui lòng đăng nhập" : "không có quyền");
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});
