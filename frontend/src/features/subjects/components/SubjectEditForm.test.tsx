import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { updateSubject } from "../api";
import { SubjectEditForm } from "./SubjectEditForm";
import type { SubjectDetail } from "../model/types";

vi.mock("../api", () => ({ updateSubject: vi.fn() }));
const subject: SubjectDetail = { id: "s", code: "ABC", name: "Môn A", description: "Mô tả", managerId: null, manager: null, status: "ACTIVE", classCount: null, version: 7, createdAt: "2026-10-01T00:00:00Z", updatedAt: "2026-10-01T00:00:00Z" };
function setup(overrides: Partial<SubjectDetail> = {}) {
  const onSaved = vi.fn(), onReload = vi.fn(), onCancel = vi.fn(), onAccessError = vi.fn();
  render(<SubjectEditForm subject={{ ...subject, ...overrides }} {...{ onSaved, onReload, onCancel, onAccessError }} />);
  return { onSaved, onReload, onCancel, onAccessError };
}
beforeEach(() => { vi.mocked(updateSubject).mockReset().mockResolvedValue({ ...subject, version: 8 }); });
afterEach(cleanup);

describe("SubjectEditForm — UC19", () => {
  it("mã khóa, tên trắng chặn API, Hủy đưa về giá trị đang lưu và không PATCH", () => {
    setup();
    expect(screen.getByLabelText("Mã môn")).toBeDisabled();
    expect(screen.getByTestId("subject-edit-save")).toBeDisabled();
    expect(screen.getByTestId("subject-edit-cancel")).toBeDisabled();
    fireEvent.change(screen.getByTestId("subject-edit-name"), { target: { value: "  " } });
    fireEvent.submit(screen.getByTestId("subject-edit-form"));
    expect(screen.getByText(/Tên môn là bắt buộc/)).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("subject-edit-cancel"));
    expect(screen.getByTestId("subject-edit-name")).toHaveValue("Môn A");
    expect(updateSubject).not.toHaveBeenCalled();
  });

  it("PATCH chỉ metadata và version hiển thị, trim tên và giữ mô tả", async () => {
    const { onSaved } = setup();
    fireEvent.change(screen.getByTestId("subject-edit-name"), { target: { value: " Tên mới " } });
    fireEvent.change(screen.getByTestId("subject-edit-description"), { target: { value: "  nội dung  " } });
    fireEvent.submit(screen.getByTestId("subject-edit-form"));
    await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
    expect(updateSubject).toHaveBeenCalledWith("s", { name: "Tên mới", description: "  nội dung  ", version: 7 });
  });

  it("môn archived vẫn sửa được metadata, mô tả rỗng gửi null", async () => {
    setup({ status: "ARCHIVED" });
    fireEvent.change(screen.getByTestId("subject-edit-description"), { target: { value: "" } });
    fireEvent.submit(screen.getByTestId("subject-edit-form"));
    await waitFor(() => expect(updateSubject).toHaveBeenCalledWith("s", { name: "Môn A", description: null, version: 7 }));
  });

  it("hai submit pending chỉ một PATCH; khóa input/cancel", () => {
    vi.mocked(updateSubject).mockReturnValue(new Promise(() => {}));
    const { onCancel } = setup();
    fireEvent.submit(screen.getByTestId("subject-edit-form")); fireEvent.submit(screen.getByTestId("subject-edit-form"));
    expect(updateSubject).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("subject-edit-name")).toBeDisabled();
    fireEvent.click(screen.getByTestId("subject-edit-cancel")); expect(onCancel).not.toHaveBeenCalled();
  });

  it("409 version giữ draft, khóa lưu và chỉ reload khi user quyết định", async () => {
    vi.mocked(updateSubject).mockRejectedValue(new ApiError({ status: 409, code: "SUBJECT_VERSION_CONFLICT", detail: "secret" }));
    const { onSaved, onReload } = setup();
    fireEvent.change(screen.getByTestId("subject-edit-name"), { target: { value: "Bản nháp" } });
    fireEvent.submit(screen.getByTestId("subject-edit-form"));
    await screen.findByText(/Bản nhập được giữ lại/);
    expect(screen.getByTestId("subject-edit-name")).toHaveValue("Bản nháp");
    expect(screen.getByTestId("subject-edit-save")).toBeDisabled();
    expect(onSaved).not.toHaveBeenCalled(); expect(onReload).not.toHaveBeenCalled();
    fireEvent.submit(screen.getByTestId("subject-edit-form")); expect(updateSubject).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByTestId("subject-mutation-reload")); expect(onReload).toHaveBeenCalledTimes(1);
    expect(screen.queryByText("secret")).not.toBeInTheDocument();
  });

  it.each([401, 403, 404, 503])("lỗi%s không báo success, giữ draft, quyền bị từ chối báo parent", async (status) => {
    vi.mocked(updateSubject).mockRejectedValue(new ApiError({ status, detail: "sql private" }));
    const { onSaved, onAccessError } = setup();
    fireEvent.change(screen.getByTestId("subject-edit-name"), { target: { value: "Draft" } });
    fireEvent.submit(screen.getByTestId("subject-edit-form"));
    await screen.findByRole("alert");
    expect(screen.getByTestId("subject-edit-name")).toHaveValue("Draft");
    expect(screen.queryByText(/sql private/)).not.toBeInTheDocument();
    expect(onSaved).not.toHaveBeenCalled();
    expect(onAccessError).toHaveBeenCalledTimes(status === 401 || status === 403 ? 1 : 0);
  });
});
