import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ErrorPage } from "./ErrorPage";

describe("ErrorPage", () => {
  it("shows the not-found message and a home link", () => {
    render(<ErrorPage code="404" title="Không tìm thấy trang" description="Đường dẫn không tồn tại." />);

    expect(screen.getByText("404")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Không tìm thấy trang" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
    expect(screen.queryByRole("button", { name: "Thử lại" })).not.toBeInTheDocument();
  });

  it("offers a retry action for runtime errors", () => {
    const retry = vi.fn();
    render(<ErrorPage code="500" title="Đã xảy ra lỗi" description="Thử lại sau." retry={retry} />);

    fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(retry).toHaveBeenCalledOnce();
  });
});
