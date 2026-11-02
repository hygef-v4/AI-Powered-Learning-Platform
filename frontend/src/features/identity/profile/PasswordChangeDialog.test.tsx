import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { changePassword } from "../api";
import { PasswordChangeDialog } from "./PasswordChangeDialog";

vi.mock("../api", () => ({ changePassword: vi.fn() }));

const changeMock = vi.mocked(changePassword);

function fill(current: string, next: string, confirm = next) {
  fireEvent.change(screen.getByLabelText("Mật khẩu hiện tại"), { target: { value: current } });
  fireEvent.change(screen.getByLabelText("Mật khẩu mới"), { target: { value: next } });
  fireEvent.change(screen.getByLabelText("Nhập lại mật khẩu mới"), { target: { value: confirm } });
  fireEvent.click(screen.getByRole("button", { name: "Đổi mật khẩu" }));
}

describe("PasswordChangeDialog", () => {
  afterEach(() => vi.clearAllMocks());

  it("đổi thành công thì báo cho trang", async () => {
    changeMock.mockResolvedValue(undefined);
    const onChanged = vi.fn();
    render(<PasswordChangeDialog onClose={vi.fn()} onChanged={onChanged} />);

    fill("hocbai2026", "matkhaumoi9");

    await waitFor(() => expect(onChanged).toHaveBeenCalled());
    expect(changeMock).toHaveBeenCalledWith("hocbai2026", "matkhaumoi9");
  });

  it("mật khẩu hiện tại sai thì báo lỗi và ở lại popup", async () => {
    changeMock.mockRejectedValue(new ApiError({ status: 400, code: "CURRENT_PASSWORD_INVALID" }));
    const onChanged = vi.fn();
    render(<PasswordChangeDialog onClose={vi.fn()} onChanged={onChanged} />);

    fill("sai", "matkhaumoi9");

    expect(await screen.findByRole("alert")).toHaveTextContent("Mật khẩu hiện tại không đúng.");
    expect(onChanged).not.toHaveBeenCalled();
  });

  it("nhập sai quá nhiều lần thì báo chờ thử lại", async () => {
    changeMock.mockRejectedValue(new ApiError({ status: 429, code: "TOO_MANY_ATTEMPTS" }));
    render(<PasswordChangeDialog onClose={vi.fn()} onChanged={vi.fn()} />);

    fill("sai", "matkhaumoi9");

    expect(await screen.findByRole("alert")).toHaveTextContent("quá nhiều lần");
    expect(screen.getByLabelText("Mật khẩu hiện tại")).toHaveValue("");
  });

  it("mật khẩu mới yếu hoặc nhập lại không khớp thì không gọi API", () => {
    render(<PasswordChangeDialog onClose={vi.fn()} onChanged={vi.fn()} />);

    fill("hocbai2026", "abcdefgh");
    expect(screen.getByRole("alert")).toHaveTextContent("ít nhất 8 ký tự, có cả chữ và số");

    fill("hocbai2026", "matkhaumoi9", "matkhaumoi8");
    expect(screen.getByRole("alert")).toHaveTextContent("chưa khớp");
    expect(changeMock).not.toHaveBeenCalled();
  });
});
