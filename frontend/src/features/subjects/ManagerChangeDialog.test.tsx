import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { changeSubjectManager } from "./api";
import { ManagerChangeDialog } from "./ManagerChangeDialog";
import type { ManagerCandidate, SearchSubjectManagers } from "./managerSearch";
import type { SubjectDetail } from "./types";

vi.mock("./api", () => ({ changeSubjectManager: vi.fn() }));
const subject: SubjectDetail = { id: "s", code: "ABC", name: "Môn A", description: null, managerId: "a", manager: { accountId: "a", displayName: "Người cũ", email: "old@example.test" }, status: "ACTIVE", classCount: 0, version: 7, createdAt: "2026-10-01T00:00:00Z", updatedAt: "2026-10-01T00:00:00Z" };
const candidate: ManagerCandidate = { accountId: "b", displayName: "Người mới", email: "new@example.test", role: "SUBJECT_MANAGER", status: "ACTIVE" };
function setup(search: SearchSubjectManagers = vi.fn<SearchSubjectManagers>().mockResolvedValue([candidate]), overrides: Partial<SubjectDetail> = {}) {
  const onSaved = vi.fn(), onReload = vi.fn(), onClose = vi.fn();
  render(<ManagerChangeDialog subject={{ ...subject, ...overrides }} searchManagers={search} {...{ onSaved, onReload, onClose }} />);
  return { onSaved, onReload, onClose };
}
beforeEach(() => { vi.mocked(changeSubjectManager).mockReset().mockResolvedValue({ ...subject, version: 8 }); });
afterEach(cleanup);

describe("ManagerChangeDialog", () => {
  it("chọn B gửi ID/version thay A, không gửi role/contact", async () => {
    const { onSaved } = setup();
    expect(screen.getByText(/Người cũ/)).toBeInTheDocument();
    expect(screen.getByTestId("subject-manager-save")).toBeDisabled();
    await screen.findByRole("option", { name: /Người mới/ });
    fireEvent.change(screen.getByLabelText("Chọn chủ nhiệm"), { target: { value: "b" } });
    fireEvent.click(screen.getByTestId("subject-manager-save"));
    await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
    expect(changeSubjectManager).toHaveBeenCalledWith("s", { managerId: "b", version: 7 });
  });

  it("bỏ phân công gửi explicit null kể cả lookup lỗi và archived", async () => {
    setup(vi.fn<SearchSubjectManagers>().mockRejectedValue(new Error("offline")), { status: "ARCHIVED" });
    await screen.findByText(/Không tải được danh sách/);
    fireEvent.click(screen.getByTestId("subject-manager-unassign"));
    fireEvent.click(screen.getByTestId("subject-manager-save"));
    await waitFor(() => expect(changeSubjectManager).toHaveBeenCalledWith("s", { managerId: null, version: 7 }));
  });

  it("đổi assign → unassign → assign xóa lựa chọn cũ, cancel không ghi", async () => {
    const { onClose } = setup();
    await screen.findByRole("option", { name: /Người mới/ });
    fireEvent.change(screen.getByLabelText("Chọn chủ nhiệm"), { target: { value: "b" } });
    fireEvent.click(screen.getByTestId("subject-manager-unassign")); fireEvent.click(screen.getByTestId("subject-manager-assign"));
    expect(screen.getByTestId("subject-manager-save")).toBeDisabled();
    expect(screen.queryByText(/Đã chọn:/)).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("subject-manager-cancel"));
    expect(onClose).toHaveBeenCalledTimes(1); expect(changeSubjectManager).not.toHaveBeenCalled();
  });

  it("pending chỉ một PUT và chặn Esc/backdrop/close", () => {
    vi.mocked(changeSubjectManager).mockReturnValue(new Promise(() => {}));
    const { onClose } = setup();
    fireEvent.click(screen.getByTestId("subject-manager-unassign"));
    fireEvent.click(screen.getByTestId("subject-manager-save")); fireEvent.click(screen.getByTestId("subject-manager-save"));
    fireEvent.keyDown(document, { key: "Escape" }); fireEvent.mouseDown(screen.getByRole("dialog").parentElement!);
    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));
    expect(changeSubjectManager).toHaveBeenCalledTimes(1); expect(onClose).not.toHaveBeenCalled();
  });

  it("manager invalid giữ lựa chọn, cho user bỏ phân công rồi gửi null", async () => {
    vi.mocked(changeSubjectManager).mockRejectedValueOnce(new ApiError({ status: 400, code: "SUBJECT_MANAGER_INVALID" }));
    setup(); await screen.findByRole("option", { name: /Người mới/ });
    fireEvent.change(screen.getByLabelText("Chọn chủ nhiệm"), { target: { value: "b" } });
    fireEvent.click(screen.getByTestId("subject-manager-save"));
    await screen.findByText(/Chủ nhiệm đã chọn không còn hợp lệ/);
    expect(screen.getByText(/Đã chọn: Người mới/)).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("subject-manager-unassign")); fireEvent.click(screen.getByTestId("subject-manager-save"));
    await waitFor(() => expect(changeSubjectManager).toHaveBeenLastCalledWith("s", { managerId: null, version: 7 }));
  });

  it("version conflict giữ lựa chọn và không ghi lại bằng version tự lấy mới", async () => {
    vi.mocked(changeSubjectManager).mockRejectedValue(new ApiError({ status: 409, code: "SUBJECT_VERSION_CONFLICT" }));
    const { onReload } = setup();
    fireEvent.click(screen.getByTestId("subject-manager-unassign")); fireEvent.click(screen.getByTestId("subject-manager-save"));
    await screen.findByText(/Bản nhập được giữ lại/);
    expect(screen.getByTestId("subject-manager-save")).toBeDisabled();
    expect(onReload).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("subject-mutation-reload")); expect(onReload).toHaveBeenCalledTimes(1);
  });
});
