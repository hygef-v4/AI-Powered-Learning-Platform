import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { Toaster } from "@/shared/ui/Toast";
import { changeSubjectManager, changeSubjectStatus, getSubject, getSubjectClasses, updateSubject } from "../api";
import { SubjectDetailPage } from "./SubjectDetailPage";
import type { ManagerCandidate, SearchSubjectManagers } from "../model/managerSearch";
import type { SubjectDetail } from "../model/types";

vi.mock("../api", () => ({ getSubject: vi.fn(), getSubjectClasses: vi.fn(), updateSubject: vi.fn(), changeSubjectManager: vi.fn(), changeSubjectStatus: vi.fn() }));
let server: SubjectDetail;
const base: SubjectDetail = { id: "a", code: "ABC", name: "Môn A", description: "Mô tả môn", managerId: null, manager: null, status: "ACTIVE", classCount: 0, version: 7, createdAt: "2026-10-01T00:00:00Z", updatedAt: "2026-10-01T00:00:00Z" };
const candidate: ManagerCandidate = { accountId: "b", displayName: "Người B", email: "b@example.test", role: "SUBJECT_MANAGER", status: "ACTIVE" };
const search = vi.fn<SearchSubjectManagers>();
beforeEach(() => {
  server = { ...base }; search.mockReset().mockResolvedValue([candidate]);
  vi.mocked(getSubject).mockReset().mockImplementation(async () => ({ ...server }));
  vi.mocked(getSubjectClasses).mockReset().mockResolvedValue({ availability: "AVAILABLE", items: [] });
  vi.mocked(updateSubject).mockReset().mockImplementation(async (_, request) => { server = { ...server, ...request, version: server.version + 1 }; return { ...server, manager: null, classCount: null }; });
  vi.mocked(changeSubjectManager).mockReset().mockImplementation(async (_, request) => { server = { ...server, managerId: request.managerId, manager: request.managerId ? candidate : null, version: server.version + 1 }; return { ...server, manager: null, classCount: null }; });
  vi.mocked(changeSubjectStatus).mockReset().mockImplementation(async (_, request) => { server = { ...server, status: request.status, version: server.version + 1 }; return { ...server, manager: null, classCount: null }; });
});
afterEach(cleanup);

async function pickB() {
  fireEvent.focus(screen.getByRole("combobox"));
  await screen.findByRole("option", { name: /Người B/ });
  fireEvent.click(screen.getByRole("option", { name: /Người B/ }));
}

describe("Detail mutations dùng chung version và làm mới snapshot", () => {
  it("sửa→phân công→bỏ phân công→lưu trữ→mở lại luôn dùng version GET mới nhất", async () => {
    render(<SubjectDetailPage subjectId="a" searchManagers={search} />);
    await screen.findByDisplayValue("Mô tả môn");
    fireEvent.change(screen.getByTestId("subject-edit-name"), { target: { value: "Tên mới" } });
    fireEvent.submit(screen.getByTestId("subject-edit-form"));
    await screen.findByRole("heading", { name: /Tên mới/ });
    expect(updateSubject).toHaveBeenCalledWith("a", { name: "Tên mới", description: "Mô tả môn", version: 7 });
    fireEvent.click(screen.getByTestId("subject-manager-open"));
    await pickB();
    fireEvent.click(screen.getByTestId("subject-manager-save"));
    await screen.findByText("Người B");
    expect(changeSubjectManager).toHaveBeenLastCalledWith("a", { managerId: "b", version: 8 });
    fireEvent.click(screen.getByTestId("subject-manager-open"));
    fireEvent.click(screen.getByTestId("subject-manager-clear")); fireEvent.click(screen.getByTestId("subject-manager-save"));
    await screen.findByText("Chưa phân công");
    expect(changeSubjectManager).toHaveBeenLastCalledWith("a", { managerId: null, version: 9 });
    fireEvent.click(screen.getByTestId("subject-status-open")); fireEvent.click(screen.getByTestId("subject-status-confirm"));
    await screen.findByRole("button", { name: "Mở lại môn học" });
    expect(changeSubjectStatus).toHaveBeenLastCalledWith("a", { status: "ARCHIVED", version: 10 });
    fireEvent.click(screen.getByTestId("subject-status-open")); fireEvent.click(screen.getByTestId("subject-status-confirm"));
    await screen.findByRole("button", { name: "Lưu trữ môn học" });
    expect(changeSubjectStatus).toHaveBeenLastCalledWith("a", { status: "ACTIVE", version: 11 });
    expect(getSubject).toHaveBeenCalledTimes(6);
  });

  it("Hủy đưa form về giá trị đang lưu, không PATCH", async () => {
    render(<SubjectDetailPage subjectId="a" />); await screen.findByDisplayValue("Mô tả môn");
    fireEvent.change(screen.getByTestId("subject-edit-name"), { target: { value: "Bản nháp" } });
    fireEvent.click(screen.getByTestId("subject-edit-cancel"));
    expect(screen.queryByDisplayValue("Bản nháp")).not.toBeInTheDocument();
    expect(screen.getByTestId("subject-edit-name")).toHaveValue("Môn A");
    expect(updateSubject).not.toHaveBeenCalled();
  });

  it("409 giữ draft đến khi chọn reload, không lấy version mới rồi tự gửi lại", async () => {
    vi.mocked(updateSubject).mockRejectedValueOnce(new ApiError({ status: 409, code: "SUBJECT_VERSION_CONFLICT" }));
    render(<SubjectDetailPage subjectId="a" />); await screen.findByDisplayValue("Mô tả môn");
    fireEvent.change(screen.getByTestId("subject-edit-name"), { target: { value: "Draft" } });
    fireEvent.submit(screen.getByTestId("subject-edit-form")); await screen.findByText(/Bản nhập được giữ lại/);
    server = { ...server, name: "Người khác đã sửa", version: 8 };
    expect(getSubject).toHaveBeenCalledTimes(1); expect(screen.getByTestId("subject-edit-name")).toHaveValue("Draft");
    fireEvent.click(screen.getByTestId("subject-mutation-reload"));
    await screen.findByRole("heading", { name: /Người khác đã sửa/ });
    expect(screen.getByTestId("subject-edit-name")).toHaveValue("Người khác đã sửa");
    expect(updateSubject).toHaveBeenCalledTimes(1);
    fireEvent.change(screen.getByTestId("subject-edit-name"), { target: { value: "Lần hai" } });
    fireEvent.submit(screen.getByTestId("subject-edit-form"));
    await waitFor(() => expect(updateSubject).toHaveBeenLastCalledWith("a", expect.objectContaining({ version: 8 })));
  });

  it("HTTP success rồi GET lỗi: vẫn báo đã lưu, retry chỉ đọc, không PUT lần hai", async () => {
    render(<><SubjectDetailPage subjectId="a" /><Toaster /></>); await screen.findByDisplayValue("Mô tả môn");
    fireEvent.click(screen.getByTestId("subject-status-open"));
    vi.mocked(getSubject).mockRejectedValueOnce(new Error("GET failed"));
    fireEvent.click(screen.getByTestId("subject-status-confirm"));
    await screen.findByRole("alert");
    expect(await screen.findByText("Đã lưu trữ môn học")).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("subjects-read-retry"));
    await screen.findByRole("button", { name: "Mở lại môn học" });
    expect(changeSubjectStatus).toHaveBeenCalledTimes(1);
  });

  it("mutation403 che dữ liệu/controls, retry reauthorize trước khi hiện lại", async () => {
    vi.mocked(changeSubjectManager).mockRejectedValue(new ApiError({ status: 403 }));
    render(<SubjectDetailPage subjectId="a" searchManagers={search} />); await screen.findByDisplayValue("Mô tả môn");
    fireEvent.click(screen.getByTestId("subject-manager-open"));
    await pickB();
    fireEvent.click(screen.getByTestId("subject-manager-save"));
    await screen.findByRole("alert");
    expect(screen.queryByDisplayValue("Mô tả môn")).not.toBeInTheDocument(); expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("subjects-read-retry"));
    await screen.findByDisplayValue("Mô tả môn"); expect(getSubject).toHaveBeenCalledTimes(2);
  });

  it("đổi subjectId khi PUT đang chờ bỏ draft; response cũ không tác động môn mới", async () => {
    let resolve!: (value: SubjectDetail) => void;
    vi.mocked(updateSubject).mockReturnValueOnce(new Promise((done) => { resolve = done; }));
    const view = render(<><SubjectDetailPage subjectId="a" /><Toaster /></>); await screen.findByDisplayValue("Mô tả môn");
    fireEvent.change(screen.getByTestId("subject-edit-name"), { target: { value: "Đang lưu" } });
    fireEvent.submit(screen.getByTestId("subject-edit-form"));
    server = { ...base, id: "b", code: "BBB", name: "Môn B", description: "Mô tả B" };
    view.rerender(<><SubjectDetailPage subjectId="b" /><Toaster /></>); await screen.findByDisplayValue("Mô tả B");
    await act(async () => { resolve({ ...base, version: 8 }); });
    expect(screen.getByDisplayValue("Mô tả B")).toBeInTheDocument();
    expect(screen.queryByText("Đã lưu thay đổi")).not.toBeInTheDocument();
    expect(getSubject).toHaveBeenCalledTimes(2);
  });
});
