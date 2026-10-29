import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { changeSubjectStatus } from "./api";
import { SubjectStatusActions } from "./SubjectStatusActions";
import type { SubjectDetail } from "./types";

vi.mock("./api", () => ({ changeSubjectStatus: vi.fn() }));
const subject: SubjectDetail = { id: "s", code: "ABC", name: "Môn A", description: null, managerId: null, manager: null, status: "ACTIVE", classCount: 0, version: 7, createdAt: "2026-10-01T00:00:00Z", updatedAt: "2026-10-01T00:00:00Z" };
function setup(status: SubjectDetail["status"] = "ACTIVE") {
  const onClose = vi.fn(), onSaved = vi.fn(), onReload = vi.fn(), onAccessError = vi.fn();
  render(<SubjectStatusActions subject={{ ...subject, status }} {...{ onClose, onSaved, onReload, onAccessError }} />);
  return { onClose, onSaved, onReload, onAccessError };
}
beforeEach(() => { vi.mocked(changeSubjectStatus).mockReset().mockResolvedValue({ ...subject, status: "ARCHIVED", version: 8 }); });
afterEach(cleanup);

describe("SubjectStatusActions/archive rejection", () => {
  it("popup chỉ có tiêu đề và nút xác nhận; đóng bằng X không ghi", () => {
    const { onClose } = setup();
    expect(changeSubjectStatus).not.toHaveBeenCalled();
    expect(screen.getByRole("dialog", { name: "Lưu trữ môn học?" })).not.toHaveTextContent(/lớp|chủ nhiệm/i);
    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));
    expect(onClose).toHaveBeenCalledTimes(1); expect(changeSubjectStatus).not.toHaveBeenCalled();
  });

  it.each(["ACTIVE", "ARCHIVED"] as const)("xác nhận%s gửi trạng thái đích/version; không success trước response", async (status) => {
    let done!: (value: SubjectDetail) => void;
    vi.mocked(changeSubjectStatus).mockReturnValue(new Promise((resolve) => { done = resolve; }));
    const { onSaved, onClose } = setup(status);
    fireEvent.click(screen.getByTestId("subject-status-confirm")); fireEvent.click(screen.getByTestId("subject-status-confirm"));
    expect(changeSubjectStatus).toHaveBeenCalledTimes(1);
    expect(changeSubjectStatus).toHaveBeenCalledWith("s", { status: status === "ACTIVE" ? "ARCHIVED" : "ACTIVE", version: 7 });
    expect(onSaved).not.toHaveBeenCalled();
    fireEvent.keyDown(document, { key: "Escape" }); expect(onClose).not.toHaveBeenCalled();
    done({ ...subject, status: status === "ACTIVE" ? "ARCHIVED" : "ACTIVE", version: 8 });
    await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
  });

  it("version409 có reload riêng, không bị hiểu là archive bị chặn bởi lớp", async () => {
    vi.mocked(changeSubjectStatus).mockRejectedValue(new ApiError({ status: 409, code: "SUBJECT_VERSION_CONFLICT" }));
    const { onReload } = setup(); fireEvent.click(screen.getByTestId("subject-status-confirm"));
    await screen.findByText(/Bản nhập được giữ lại/);
    expect(screen.queryByRole("dialog", { name: "Chưa thể lưu trữ môn" })).not.toBeInTheDocument();
    expect(screen.getByTestId("subject-status-confirm")).toBeDisabled();
    fireEvent.click(screen.getByTestId("subject-mutation-reload")); expect(onReload).toHaveBeenCalledTimes(1);
  });

  it.each(["SUBJECT_CLASSES_UNAVAILABLE", "SUBJECT_DATA_CONFLICT", "UNKNOWN"])("%s không giả blocker hoặc tự retry", async (code) => {
    vi.mocked(changeSubjectStatus).mockRejectedValue(new ApiError({ status: code === "SUBJECT_CLASSES_UNAVAILABLE" ? 503 : 409, code, detail: "secret" }));
    const { onSaved } = setup(); fireEvent.click(screen.getByTestId("subject-status-confirm"));
    await screen.findByRole("alert");
    expect(screen.queryByRole("dialog", { name: "Chưa thể lưu trữ môn" })).not.toBeInTheDocument();
    expect(screen.queryByText("secret")).not.toBeInTheDocument();
    if (code === "SUBJECT_CLASSES_UNAVAILABLE") expect(screen.getByText(/Chưa kiểm tra được lớp/)).toBeInTheDocument();
    expect(changeSubjectStatus).toHaveBeenCalledTimes(1); expect(onSaved).not.toHaveBeenCalled();
  });

  it.each([401, 403])("lỗi quyền%s báo parent để che privileged detail", async (status) => {
    vi.mocked(changeSubjectStatus).mockRejectedValue(new ApiError({ status }));
    const { onAccessError, onSaved } = setup(); fireEvent.click(screen.getByTestId("subject-status-confirm"));
    await waitFor(() => expect(onAccessError).toHaveBeenCalledTimes(1)); expect(onSaved).not.toHaveBeenCalled();
  });
});
