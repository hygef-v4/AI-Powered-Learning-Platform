import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { getSubject, getSubjectClasses } from "./api";
import { SubjectDetailPage } from "./SubjectDetailPage";
import type { SubjectDetail } from "./types";

vi.mock("./api", () => ({ getSubject: vi.fn(), getSubjectClasses: vi.fn() }));

function detail(overrides: Partial<SubjectDetail> = {}): SubjectDetail {
  return { id: "a", code: "ABC", name: "Môn A", status: "ACTIVE", description: "Mô tả môn", managerId: null, manager: null,
    classCount: null, createdAt: "2026-10-01T23:00:00Z", updatedAt: "2026-10-01T23:00:00Z", version: 1, ...overrides };
}
function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => { resolve = done; });
  return { promise, resolve };
}

describe("SubjectDetailPage", () => {
  beforeEach(() => {
    vi.mocked(getSubject).mockReset().mockResolvedValue(detail());
    vi.mocked(getSubjectClasses).mockReset().mockResolvedValue({ availability: "AVAILABLE", items: [] });
  });
  afterEach(cleanup);

  it("đọc detail, ngày Việt Nam, unknown count và chỉ tải lớp khi mở tab", async () => {
    render(<SubjectDetailPage subjectId="a" />);
    expect(await screen.findByDisplayValue("Mô tả môn")).toBeInTheDocument();
    expect(screen.getByText("02/10/2026")).toBeInTheDocument();
    expect(screen.getByText("Chưa phân công")).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Lớp học" })).toBeInTheDocument();
    expect(getSubjectClasses).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("tab", { name: /Lớp học/ }));
    expect(await screen.findByText("Không có lớp phù hợp.")).toBeInTheDocument();
    expect(getSubjectClasses).toHaveBeenCalledWith("a");
  });

  it("contact thiếu khác chưa phân công; archived vẫn hiển thị dữ liệu lịch sử", async () => {
    vi.mocked(getSubject).mockResolvedValue(detail({ managerId: "missing", status: "ARCHIVED" }));
    render(<SubjectDetailPage subjectId="a" />);
    expect(await screen.findByText("Không tra được thông tin chủ nhiệm môn.")).toBeInTheDocument();
    expect(screen.queryByText("Chưa phân công")).not.toBeInTheDocument();
    expect(screen.getAllByText("Lưu trữ").length).toBeGreaterThan(0);
    expect(screen.getByRole("button", { name: "Mở lại môn học" })).toBeInTheDocument();
  });

  it("contact có tên/email dùng initials, không có upload avatar", async () => {
    vi.mocked(getSubject).mockResolvedValue(detail({ managerId: "m", manager: { accountId: "m", displayName: "Nguyễn Bình", email: "binh@example.test" } }));
    render(<SubjectDetailPage subjectId="a" />);
    expect(await screen.findByText("Nguyễn Bình")).toBeInTheDocument();
    expect(screen.getByText("binh@example.test")).toBeInTheDocument();
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });

  it("unavailable của provider có retry và khác verified empty", async () => {
    vi.mocked(getSubjectClasses).mockResolvedValueOnce({ availability: "UNAVAILABLE", items: null }).mockResolvedValueOnce({ availability: "AVAILABLE", items: [] });
    render(<SubjectDetailPage subjectId="a" />);
    await screen.findByDisplayValue("Mô tả môn");
    fireEvent.click(screen.getByRole("tab", { name: /Lớp học/ }));
    expect(await screen.findByText("Dữ liệu lớp hiện không khả dụng.")).toBeInTheDocument();
    expect(screen.queryByText("Không có lớp phù hợp.")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("subjects-classes-retry"));
    expect(await screen.findByText("Không có lớp phù hợp.")).toBeInTheDocument();
  });

  it.each([401, 403, 404])("detail HTTP%s không hiển thị metadata hoặc gọi lớp", async (status) => {
    vi.mocked(getSubject).mockRejectedValue(new ApiError({ status }));
    render(<SubjectDetailPage subjectId="a" />);
    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(screen.queryByDisplayValue("Mô tả môn")).not.toBeInTheDocument();
    expect(getSubjectClasses).not.toHaveBeenCalled();
  });

  it("403 khi mở lớp che cả dữ liệu môn; retry xác minh lại detail trước hiển thị", async () => {
    vi.mocked(getSubjectClasses).mockRejectedValueOnce(new ApiError({ status: 403 })).mockResolvedValueOnce({ availability: "AVAILABLE", items: [] });
    render(<SubjectDetailPage subjectId="a" />);
    await screen.findByDisplayValue("Mô tả môn");
    fireEvent.click(screen.getByRole("tab", { name: /Lớp học/ }));
    await screen.findByRole("alert");
    expect(screen.queryByText("Môn A")).not.toBeInTheDocument();
    const recheck = deferred<SubjectDetail>();
    vi.mocked(getSubject).mockReturnValueOnce(recheck.promise);
    fireEvent.click(screen.getByTestId("subjects-read-retry"));
    expect(screen.queryByText("Môn A")).not.toBeInTheDocument();
    await act(async () => { recheck.resolve(detail()); });
    expect(await screen.findByText("Không có lớp phù hợp.")).toBeInTheDocument();
    expect(getSubject).toHaveBeenCalledTimes(2);
  });

  it("đổi id bỏ response cũ và không hiển thị nhầm môn", async () => {
    const old = deferred<SubjectDetail>(), current = deferred<SubjectDetail>();
    vi.mocked(getSubject).mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise);
    const { rerender } = render(<SubjectDetailPage subjectId="a" />);
    await waitFor(() => expect(getSubject).toHaveBeenCalledWith("a"));
    rerender(<SubjectDetailPage subjectId="b" />);
    await act(async () => { current.resolve(detail({ id: "b", name: "Môn B", description: "Mô tả B" })); });
    expect(await screen.findByDisplayValue("Mô tả B")).toBeInTheDocument();
    await act(async () => { old.resolve(detail()); });
    expect(screen.queryByDisplayValue("Mô tả môn")).not.toBeInTheDocument();
    expect(screen.getByDisplayValue("Mô tả B")).toBeInTheDocument();
  });

  it("chuyển tab bằng phím mũi tên và giữ focus", async () => {
    render(<SubjectDetailPage subjectId="a" />);
    await screen.findByDisplayValue("Mô tả môn");
    const information = screen.getByRole("tab", { name: "Thông tin" });
    information.focus();
    fireEvent.keyDown(information, { key: "ArrowRight" });
    expect(screen.getByRole("tab", { name: /Lớp học/ })).toHaveFocus();
    expect(await screen.findByText("Không có lớp phù hợp.")).toBeInTheDocument();
  });
});
