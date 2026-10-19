import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { createSubject } from "./api";
import { SubjectFormDialog } from "./SubjectFormDialog";
import type { ManagerCandidate, SearchSubjectManagers } from "./managerSearch";
import type { SubjectDetail } from "./types";

vi.mock("./api", () => ({ createSubject: vi.fn() }));
const created: SubjectDetail = { id: "s1", code: "SWR302", name: "Software Requirements", description: null, managerId: null, manager: null, classCount: null, status: "ACTIVE", version: 1, createdAt: "2026-10-18T00:00:00Z", updatedAt: "2026-10-18T00:00:00Z" };
const manager: ManagerCandidate = { accountId: "m1", displayName: "Chủ nhiệm A", email: "a@example.test", role: "SUBJECT_MANAGER", status: "ACTIVE" };
function fill() {
  fireEvent.change(screen.getByTestId("subject-create-code"), { target: { value: "swr302" } });
  fireEvent.change(screen.getByTestId("subject-create-name"), { target: { value: " Software Requirements " } });
}
function setup(searchManagers?: SearchSubjectManagers) {
  const onClose = vi.fn(), onCreated = vi.fn();
  render(<SubjectFormDialog onClose={onClose} onCreated={onCreated} searchManagers={searchManagers} />);
  return { onClose, onCreated };
}
beforeEach(() => { vi.mocked(createSubject).mockReset().mockResolvedValue(created); });
afterEach(cleanup);

describe("SubjectFormDialog UC17", () => {
  it("tên trắng/mã sai bị chặn trước API, input có maxlength", async () => {
    setup();
    await screen.findByText(/Không tải được danh sách chủ nhiệm/);
    fireEvent.change(screen.getByTestId("subject-create-code"), { target: { value: "SWR_302" } });
    fireEvent.change(screen.getByTestId("subject-create-name"), { target: { value: "   " } });
    fireEvent.submit(screen.getByTestId("subject-create-form"));
    expect(screen.getByText(/Mã môn phải gồm/)).toBeInTheDocument();
    expect(screen.getByText(/Tên môn là bắt buộc/)).toBeInTheDocument();
    expect(createSubject).not.toHaveBeenCalled();
    expect(screen.getByTestId("subject-create-code")).toHaveAttribute("maxlength", "20");
    expect(screen.getByTestId("subject-create-description")).toHaveAttribute("maxlength", "2000");
  });

  it("tạo không manager khi lookup unavailable, uppercase/trim và giữ nguyên mô tả", async () => {
    const { onCreated } = setup();
    await screen.findByText(/Không tải được danh sách chủ nhiệm/);
    fill();
    fireEvent.change(screen.getByTestId("subject-create-description"), { target: { value: "  mô tả  " } });
    fireEvent.submit(screen.getByTestId("subject-create-form"));
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(created));
    expect(createSubject).toHaveBeenCalledWith({ code: "SWR302", name: "Software Requirements", description: "  mô tả  ", managerId: null });
  });

  it("gửi ID chủ nhiệm được chọn, không gửi contact hoặc role từ browser", async () => {
    setup(vi.fn<SearchSubjectManagers>().mockResolvedValue([manager]));
    fireEvent.focus(screen.getByRole("combobox"));
    await screen.findByRole("option", { name: /Chủ nhiệm A/ });
    fireEvent.click(screen.getByRole("option", { name: /Chủ nhiệm A/ }));
    fill();
    fireEvent.submit(screen.getByTestId("subject-create-form"));
    await waitFor(() => expect(createSubject).toHaveBeenCalledWith({ code: "SWR302", name: "Software Requirements", description: null, managerId: "m1" }));
  });

  it("double submit/Enter không gửi hai POST, không đóng hoặc sửa draft đang chờ", async () => {
    let resolve!: (subject: SubjectDetail) => void;
    vi.mocked(createSubject).mockReturnValue(new Promise((done) => { resolve = done; }));
    const { onClose, onCreated } = setup();
    fill();
    fireEvent.submit(screen.getByTestId("subject-create-form"));
    fireEvent.submit(screen.getByTestId("subject-create-form"));
    expect(createSubject).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("subject-create-code")).toBeDisabled();
    expect(screen.getByTestId("subject-create-submit")).toBeDisabled();
    fireEvent.keyDown(document, { key: "Escape" });
    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));
    fireEvent.mouseDown(screen.getByRole("dialog").parentElement!);
    expect(onClose).not.toHaveBeenCalled();
    await act(async () => { resolve(created); });
    expect(onCreated).toHaveBeenCalledTimes(1);
  });

  it("duplicate hiện dưới field và giữ input, sửa mã rồi submit lại được", async () => {
    vi.mocked(createSubject).mockRejectedValueOnce(new ApiError({ status: 409, code: "SUBJECT_CODE_DUPLICATE", detail: "secret sql" }));
    const { onCreated } = setup();
    fill();
    fireEvent.submit(screen.getByTestId("subject-create-form"));
    await screen.findByText(/Mã môn đã tồn tại/);
    expect(screen.getByTestId("subject-create-name")).toHaveValue(" Software Requirements ");
    expect(screen.queryByText(/secret sql/)).not.toBeInTheDocument();
    expect(onCreated).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("subject-create-code"), { target: { value: "SWR303" } });
    fireEvent.submit(screen.getByTestId("subject-create-form"));
    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
  });

  it("manager bị disable lúc submit: giữ draft, cho bỏ chủ nhiệm và retry", async () => {
    vi.mocked(createSubject).mockRejectedValueOnce(new ApiError({ status: 400, code: "SUBJECT_MANAGER_INVALID" }));
    setup(vi.fn<SearchSubjectManagers>().mockResolvedValue([manager]));
    fireEvent.focus(screen.getByRole("combobox"));
    await screen.findByRole("option", { name: /Chủ nhiệm A/ });
    fireEvent.click(screen.getByRole("option", { name: /Chủ nhiệm A/ }));
    fill(); fireEvent.submit(screen.getByTestId("subject-create-form"));
    await screen.findByText(/Chủ nhiệm đã chọn không còn hợp lệ/);
    fireEvent.click(screen.getByTestId("subject-manager-clear"));
    fireEvent.submit(screen.getByTestId("subject-create-form"));
    await waitFor(() => expect(createSubject).toHaveBeenLastCalledWith(expect.objectContaining({ managerId: null })));
  });

  it.each([401, 403, 503])("lỗi%s giữ draft, không tự retry hoặc báo success", async (status) => {
    vi.mocked(createSubject).mockRejectedValue(new ApiError({ status, detail: "secret provider" }));
    const { onCreated } = setup();
    fill(); fireEvent.submit(screen.getByTestId("subject-create-form"));
    await screen.findByText(status === 401 ? /Phiên đăng nhập không còn hợp lệ/ : status === 403 ? /không có quyền thêm/ : /Chưa xác nhận tạo môn thành công/);
    expect(screen.getByTestId("subject-create-code")).toHaveValue("SWR302");
    expect(screen.queryByText(/secret provider/)).not.toBeInTheDocument();
    expect(createSubject).toHaveBeenCalledTimes(1);
    expect(onCreated).not.toHaveBeenCalled();
  });

  it("cancel/Escape không gọi mutation", () => {
    const { onClose } = setup();
    fill();
    fireEvent.click(screen.getByTestId("subject-create-cancel"));
    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).toHaveBeenCalledTimes(2);
    expect(createSubject).not.toHaveBeenCalled();
  });
});
