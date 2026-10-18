import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { changeSubjectStatus } from "./api";
import { SubjectStatusActions } from "./SubjectStatusActions";
import type { SubjectApiProblem, SubjectDetail } from "./types";

vi.mock("./api", () => ({ changeSubjectStatus: vi.fn() }));
const subject: SubjectDetail = { id: "s", code: "ABC", name: "Môn A", description: null, managerId: null, manager: null, status: "ACTIVE", classCount: 0, version: 7, createdAt: "2026-10-01T00:00:00Z", updatedAt: "2026-10-01T00:00:00Z" };
const blocked: SubjectApiProblem = { status: 409, code: "SUBJECT_HAS_UNARCHIVED_CLASSES", unarchivedCount: 3, blockingClasses: [
  { classId: "c1", code: "OPEN1", name: "Lớp mở", term: "2026-1", status: "OPEN", teacherName: "Giảng viên", activeStudentCount: 20 },
  { classId: "c2", code: "DRAFT1", name: "Lớp nháp", term: "2026-1", status: "DRAFT", teacherName: null, activeStudentCount: 0 },
] };
function setup(status: SubjectDetail["status"] = "ACTIVE") {
  const onClose = vi.fn(), onSaved = vi.fn(), onReload = vi.fn(), onAccessError = vi.fn();
  render(<SubjectStatusActions subject={{ ...subject, status }} {...{ onClose, onSaved, onReload, onAccessError }} />);
  return { onClose, onSaved, onReload, onAccessError };
}
beforeEach(() => { vi.mocked(changeSubjectStatus).mockReset().mockResolvedValue({ ...subject, status: "ARCHIVED", version: 8 }); });
afterEach(cleanup);

describe("SubjectStatusActions/archive rejection", () => {
  it("mở confirmation chưa gọi API; cancel không ghi", () => {
    const { onClose } = setup();
    expect(changeSubjectStatus).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("subject-status-cancel"));
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

  it("archive reject có tổng/blocker OPEN+DRAFT chỉ đọc, không giả toàn bộ count bằng số dòng", async () => {
    vi.mocked(changeSubjectStatus).mockRejectedValue(new ApiError(blocked));
    const { onSaved, onClose } = setup();
    fireEvent.click(screen.getByTestId("subject-status-confirm"));
    const dialog = await screen.findByRole("dialog", { name: "Chưa thể lưu trữ môn" });
    expect(within(dialog).getByText(/Môn còn 3 lớp/)).toBeInTheDocument();
    expect(within(dialog).getByText("OPEN1")).toBeInTheDocument(); expect(within(dialog).getByText("DRAFT1")).toBeInTheDocument();
    expect(within(dialog).getByText("Đang hiển thị 2 / 3 lớp chưa lưu trữ.")).toBeInTheDocument();
    expect(within(dialog).queryByRole("link")).not.toBeInTheDocument();
    expect(within(dialog).queryByText("20")).not.toBeInTheDocument(); expect(onSaved).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("subject-archive-rejected-close")); expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("blocker thiếu/malformed không crash, không suy0 và không render ARCHIVED như blocker", async () => {
    const malformed = { status: 409, code: "SUBJECT_HAS_UNARCHIVED_CLASSES", blockingClasses: [null, { status: "ARCHIVED" }] } as unknown as SubjectApiProblem;
    vi.mocked(changeSubjectStatus).mockRejectedValue(new ApiError(malformed));
    setup(); fireEvent.click(screen.getByTestId("subject-status-confirm"));
    await screen.findByRole("dialog", { name: "Chưa thể lưu trữ môn" });
    expect(screen.getByText(/không phải xác nhận môn có 0 lớp/)).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
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
