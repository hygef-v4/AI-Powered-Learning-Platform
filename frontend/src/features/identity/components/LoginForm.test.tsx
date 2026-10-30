import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { login } from "../api";
import { LOGIN_FAILED_MESSAGE, LoginForm, SYSTEM_BUSY_MESSAGE } from "./LoginForm";

vi.mock("../api", () => ({ login: vi.fn() }));

const loginMock = vi.mocked(login);

function fill(email: string, password: string) {
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: email } });
  fireEvent.change(screen.getByLabelText("Mật khẩu"), { target: { value: password } });
  fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));
}

describe("LoginForm", () => {
  afterEach(() => vi.clearAllMocks());

  it("đăng nhập đúng thì trả phiên cho trang", async () => {
    const session = { accountId: "1", displayName: "Tài", role: "STUDENT" as const, homePath: "/classes" };
    loginMock.mockResolvedValue(session);
    const onSuccess = vi.fn();
    render(<LoginForm onSuccess={onSuccess} />);

    fill(" taivm@fpt.edu.vn ", "hocbai2026");

    await waitFor(() => expect(onSuccess).toHaveBeenCalledWith(session));
    expect(loginMock).toHaveBeenCalledWith("taivm@fpt.edu.vn", "hocbai2026");
  });

  it("mọi lỗi 401 hiện cùng một câu và xóa ô mật khẩu", async () => {
    loginMock.mockRejectedValue(new ApiError({ status: 401, code: "AUTH_FAILED" }));
    render(<LoginForm onSuccess={vi.fn()} />);

    fill("taivm@fpt.edu.vn", "sai");

    expect(await screen.findByRole("alert")).toHaveTextContent(LOGIN_FAILED_MESSAGE);
    expect(screen.getByLabelText("Mật khẩu")).toHaveValue("");
  });

  it("hệ thống bận thì báo khác lỗi đăng nhập", async () => {
    loginMock.mockRejectedValue(new ApiError({ status: 503, code: "SYSTEM_BUSY" }));
    render(<LoginForm onSuccess={vi.fn()} />);

    fill("taivm@fpt.edu.vn", "hocbai2026");

    expect(await screen.findByRole("alert")).toHaveTextContent(SYSTEM_BUSY_MESSAGE);
  });

  it("kiểm định dạng email trước khi gọi API", () => {
    render(<LoginForm onSuccess={vi.fn()} />);

    fill("khong-phai-email", "hocbai2026");

    expect(screen.getByRole("alert")).toHaveTextContent("Nhập email trường hợp lệ.");
    expect(loginMock).not.toHaveBeenCalled();
  });

  it("có lối sang kích hoạt và quên mật khẩu", () => {
    render(<LoginForm onSuccess={vi.fn()} />);
    expect(screen.getByRole("link", { name: "Kích hoạt tài khoản" })).toHaveAttribute("href", "/activate");
    expect(screen.getByRole("link", { name: "Quên mật khẩu?" })).toHaveAttribute("href", "/reset-password");
  });
});
