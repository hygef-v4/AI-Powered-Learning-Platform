import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { updateProfile } from "@/features/identity/api";
import type { Profile } from "@/features/identity/model/types";
import { ProfileForm } from "@/features/identity/profile/components/ProfileForm";

vi.mock("@/features/identity/api", () => ({ updateProfile: vi.fn() }));

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
    render(<ProfileForm profile={PROFILE} onSaved={vi.fn()} />);
    expect(screen.getByLabelText("Họ và tên")).toBeEnabled();
    expect(screen.getByLabelText("Số điện thoại")).toBeEnabled();
    expect(screen.getByLabelText("Email trường")).toBeDisabled();
    expect(screen.getByLabelText("Email trường")).toHaveValue("taivm@fpt.edu.vn");
    expect(screen.getByLabelText("Vai trò")).toBeDisabled();
    expect(screen.getByText("Hoạt động")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Lưu thay đổi" })).toBeDisabled();
  });

  it("số điện thoại sai định dạng bị chặn trước khi gọi API", () => {
    render(<ProfileForm profile={PROFILE} onSaved={vi.fn()} />);
    fireEvent.change(screen.getByLabelText("Số điện thoại"), { target: { value: "091 234" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Số điện thoại");
    expect(updateMock).not.toHaveBeenCalled();
  });
});
