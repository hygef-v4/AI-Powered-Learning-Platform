import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ErrorPage } from "@/shared/ui/ErrorPage";

describe("ErrorPage", () => {
  it("shows the not-found message and a home link", () => {
    render(<ErrorPage kind="not-found" />);

    expect(screen.getByText("Lỗi 404")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Không tìm thấy trang" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
    expect(screen.queryByRole("button", { name: "Tải lại trang" })).not.toBeInTheDocument();
  });

  it("offers a retry action for runtime errors", () => {
    const retry = vi.fn();
    render(<ErrorPage kind="generic" retry={retry} />);

    expect(screen.getByRole("heading", { name: "Đã xảy ra lỗi" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Tải lại trang" }));
    expect(retry).toHaveBeenCalledOnce();
  });

  it("có trang riêng cho hệ thống bận và mất kết nối", () => {
    const { unmount } = render(<ErrorPage kind="unavailable" />);
    expect(screen.getByText("Lỗi 503")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Hệ thống đang bận" })).toBeInTheDocument();
    unmount();
    render(<ErrorPage kind="offline" />);
    expect(screen.getByRole("heading", { name: "Mất kết nối mạng" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Tải lại trang" })).toBeInTheDocument();
  });
});
