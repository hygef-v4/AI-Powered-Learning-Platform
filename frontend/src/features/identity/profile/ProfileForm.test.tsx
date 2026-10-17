import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { updateProfile } from "../api";
import type { Profile } from "../types";
import { ProfileForm } from "./ProfileForm";

vi.mock("../api", () => ({ updateProfile: vi.fn() }));

const updateMock = vi.mocked(updateProfile);

const PROFILE: Profile = {
  accountId: "1",
  email: "taivm@fpt.edu.vn",
  displayName: "Vũ Minh Tài",
  phoneNumber: null,
  role: "STUDENT",
  status: "ACTIVE",
  homePath: "/classes",
};

describe("ProfileForm", () => {
  afterEach(() => vi.clearAllMocks());

  it("email, vai trò, trạng thái chỉ đọc; chỉ sửa được tên và số điện thoại", () => {
    render(<ProfileForm profile={PROFILE} onSaved={vi.fn()} onChangePassword={vi.fn()} />);
    expect(screen.getAllByRole("textbox")).toHaveLength(2);
    expect(screen.getByText("taivm@fpt.edu.vn")).toBeInTheDocument();
    expect(screen.getByText("Đang hoạt động")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Lưu thay đổi" })).toBeDisabled();
  });

  it("lưu tên và số điện thoại đã cắt khoảng trắng", async () => {
    const saved = { ...PROFILE, displayName: "Tài Mới", phoneNumber: "0912345678" };
    updateMock.mockResolvedValue(saved);
    const onSaved = vi.fn();
    render(<ProfileForm profile={PROFILE} onSaved={onSaved} onChangePassword={vi.fn()} />);

    fireEvent.change(screen.getByLabelText("Họ và tên"), { target: { value: "  Tài Mới " } });
    fireEvent.change(screen.getByLabelText("Số điện thoại"), { target: { value: "0912345678" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));

    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(saved));
    expect(updateMock).toHaveBeenCalledWith("Tài Mới", "0912345678");
    expect(screen.getByRole("status")).toHaveTextContent("Đã lưu thay đổi.");
  });

  it("số điện thoại sai định dạng bị chặn trước khi gọi API", () => {
    render(<ProfileForm profile={PROFILE} onSaved={vi.fn()} onChangePassword={vi.fn()} />);
    fireEvent.change(screen.getByLabelText("Số điện thoại"), { target: { value: "091 234" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Số điện thoại");
    expect(updateMock).not.toHaveBeenCalled();
  });
});
