import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { changeSubjectStatus } from "@/features/subjects/api";
import { SubjectStatusActions } from "@/features/subjects/components/SubjectStatusActions";
import type { SubjectDetail } from "@/features/subjects/model/types";

vi.mock("@/features/subjects/api", () => ({ changeSubjectStatus: vi.fn() }));
const subject: SubjectDetail = {
  id: "s",
  code: "ABC",
  name: "Môn A",
  description: null,
  managerId: null,
  manager: null,
  status: "ACTIVE",
  classCount: 0,
  version: 7,
  createdAt: "2026-10-01T00:00:00Z",
  updatedAt: "2026-10-01T00:00:00Z",
};
function setup(status: SubjectDetail["status"] = "ACTIVE") {
  const onClose = vi.fn(),
    onSaved = vi.fn(),
    onReload = vi.fn(),
    onAccessError = vi.fn();
  render(
    <SubjectStatusActions
      subject={{ ...subject, status }}
      {...{ onClose, onSaved, onReload, onAccessError }}
    />,
  );
  return { onClose, onSaved, onReload, onAccessError };
}
beforeEach(() => {
  vi.mocked(changeSubjectStatus)
    .mockReset()
    .mockResolvedValue({ ...subject, status: "ARCHIVED", version: 8 });
});
afterEach(cleanup);

describe("SubjectStatusActions/archive rejection", () => {
  it("giải thích lưu trữ lớp, gỡ chủ nhiệm và giữ lịch sử; đóng bằng X không ghi", () => {
    const { onClose } = setup();
    expect(changeSubjectStatus).not.toHaveBeenCalled();
    const dialog = screen.getByRole("dialog", { name: "Lưu trữ môn học?" });
    expect(dialog).toHaveTextContent(/lớp.*Nháp.*Đang mở.*lưu trữ/i);
    expect(dialog).toHaveTextContent(/Chủ nhiệm môn.*gỡ/i);
    expect(dialog).toHaveTextContent(/ghi danh.*lịch sử học tập.*giữ/i);
    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));
    expect(onClose).toHaveBeenCalledTimes(1);
    expect(changeSubjectStatus).not.toHaveBeenCalled();
  });
});
