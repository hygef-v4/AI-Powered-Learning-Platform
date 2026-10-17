import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { requestOtp, verifyOtp } from "../api";
import { OTP_INVALID_MESSAGE, OtpFlow, TICKET_EXPIRED_MESSAGE, type OtpFlowCopy } from "./OtpFlow";

vi.mock("../api", () => ({ requestOtp: vi.fn(), verifyOtp: vi.fn() }));

const requestOtpMock = vi.mocked(requestOtp);
const verifyOtpMock = vi.mocked(verifyOtp);

const COPY: OtpFlowCopy = {
  purpose: "ACTIVATION",
  title: "Kích hoạt tài khoản",
  subtitle: "Xác minh email",
  passwordTitle: "Kích hoạt tài khoản",
  passwordSubtitle: () => "Đặt mật khẩu",
  steps: ["Email", "Mã OTP", "Đặt mật khẩu"],
  submitLabel: "Kích hoạt và đăng nhập",
  submitNote: "Ghi chú",
};

async function reachOtpStep() {
  requestOtpMock.mockResolvedValue({ message: "ok" });
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: "taivm@fpt.edu.vn" } });
  fireEvent.click(screen.getByRole("button", { name: "Gửi mã" }));
  await screen.findByRole("button", { name: "Xác nhận mã" });
}

function typeCode(code: string) {
  fireEvent.paste(screen.getByLabelText("Chữ số 1"), { clipboardData: { getData: () => code } });
  fireEvent.click(screen.getByRole("button", { name: "Xác nhận mã" }));
}

describe("OtpFlow", () => {
  afterEach(() => vi.clearAllMocks());

  it("mật khẩu chỉ mở sau khi OTP đúng và gửi kèm ticket", async () => {
    const onComplete = vi.fn().mockResolvedValue(undefined);
    render(<OtpFlow copy={COPY} onComplete={onComplete} />);
    await reachOtpStep();
    expect(screen.queryByLabelText("Mật khẩu mới")).not.toBeInTheDocument();
    expect(requestOtpMock).toHaveBeenCalledWith("ACTIVATION", "taivm@fpt.edu.vn");

    verifyOtpMock.mockResolvedValue({ otpTicket: "ticket-1" });
    typeCode("048213");
    fireEvent.change(await screen.findByLabelText("Mật khẩu mới"), { target: { value: "hocbai2026" } });
    fireEvent.change(screen.getByLabelText("Nhập lại mật khẩu"), { target: { value: "hocbai2026" } });
    fireEvent.click(screen.getByRole("button", { name: "Kích hoạt và đăng nhập →" }));

    await waitFor(() => expect(onComplete).toHaveBeenCalledWith("ticket-1", "hocbai2026"));
    expect(verifyOtpMock).toHaveBeenCalledWith("ACTIVATION", "taivm@fpt.edu.vn", "048213");
  });

  it("mã sai ở lại bước OTP với thông báo chung", async () => {
    render(<OtpFlow copy={COPY} onComplete={vi.fn()} />);
    await reachOtpStep();

    verifyOtpMock.mockRejectedValue(new ApiError({ status: 400, code: "OTP_INVALID" }));
    typeCode("111111");

    expect(await screen.findByRole("alert")).toHaveTextContent(OTP_INVALID_MESSAGE);
    expect(screen.queryByLabelText("Mật khẩu mới")).not.toBeInTheDocument();
  });

  it("nút gửi lại bị khóa trong 60 giây", async () => {
    render(<OtpFlow copy={COPY} onComplete={vi.fn()} />);
    await reachOtpStep();
    expect(screen.getByRole("button", { name: /Gửi lại mã \(\d+s\)/ })).toBeDisabled();
    expect(screen.getByText(/Mã hết hạn sau 10:00|Mã hết hạn sau 09:5\d/)).toBeInTheDocument();
  });

  it("ticket hết hạn thì quay về bước email", async () => {
    const onComplete = vi.fn().mockRejectedValue(new ApiError({ status: 400, code: "TICKET_EXPIRED" }));
    render(<OtpFlow copy={COPY} onComplete={onComplete} />);
    await reachOtpStep();
    verifyOtpMock.mockResolvedValue({ otpTicket: "t" });
    typeCode("048213");
    fireEvent.change(await screen.findByLabelText("Mật khẩu mới"), { target: { value: "hocbai2026" } });
    fireEvent.change(screen.getByLabelText("Nhập lại mật khẩu"), { target: { value: "hocbai2026" } });
    fireEvent.click(screen.getByRole("button", { name: "Kích hoạt và đăng nhập →" }));

    expect(await screen.findByRole("status")).toHaveTextContent(TICKET_EXPIRED_MESSAGE);
    expect(screen.getByRole("button", { name: "Gửi mã" })).toBeInTheDocument();
  });

  it("mật khẩu bị backend từ chối thì hiện lý do và ở lại bước 3", async () => {
    const onComplete = vi
      .fn()
      .mockRejectedValue(new ApiError({ status: 400, code: "PASSWORD_POLICY", violations: ["CONTAINS_EMAIL_NAME"] } as never));
    render(<OtpFlow copy={COPY} onComplete={onComplete} />);
    await reachOtpStep();
    verifyOtpMock.mockResolvedValue({ otpTicket: "t" });
    typeCode("048213");
    fireEvent.change(await screen.findByLabelText("Mật khẩu mới"), { target: { value: "taivm2026" } });
    fireEvent.change(screen.getByLabelText("Nhập lại mật khẩu"), { target: { value: "taivm2026" } });
    fireEvent.click(screen.getByRole("button", { name: "Kích hoạt và đăng nhập →" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("không được chứa tên trong email");
    expect(screen.getByLabelText("Mật khẩu mới")).toBeInTheDocument();
  });
});
