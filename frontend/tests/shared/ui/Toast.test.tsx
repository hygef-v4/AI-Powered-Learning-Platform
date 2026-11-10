import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { showErrorToast, showToast, Toaster, TOAST_VISIBLE_MS } from "@/shared/ui/Toast";

beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  cleanup();
  vi.useRealTimers();
});

describe("Toaster", () => {
  it("câu báo thành công tự ẩn sau thời gian hiển thị", () => {
    render(<Toaster />);
    act(() => showToast("Đã lưu."));
    expect(screen.getByRole("status")).toHaveTextContent("Đã lưu.");
    act(() => {
      vi.advanceTimersByTime(TOAST_VISIBLE_MS);
    });
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("câu màu đỏ báo đã xong thao tác xóa vẫn tự ẩn như câu thường", () => {
    render(<Toaster />);
    act(() => showToast("Đã xóa thông báo", "danger"));
    expect(screen.getByRole("status")).toHaveTextContent("Đã xóa thông báo");
    act(() => {
      vi.advanceTimersByTime(TOAST_VISIBLE_MS);
    });
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("câu báo lỗi dùng role alert và ở lại tới khi bấm đóng", () => {
    render(<Toaster />);
    act(() => showErrorToast("Không lưu được."));
    act(() => {
      vi.advanceTimersByTime(TOAST_VISIBLE_MS * 3);
    });
    expect(screen.getByRole("alert")).toHaveTextContent("Không lưu được.");
    fireEvent.click(screen.getByRole("button", { name: "Đóng thông báo" }));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("rê chuột vào thì dừng đếm giờ, rời ra thì đếm lại", () => {
    render(<Toaster />);
    act(() => showToast("Đã lưu."));
    const stack = screen.getByRole("status").parentElement!;
    fireEvent.mouseEnter(stack);
    act(() => {
      vi.advanceTimersByTime(TOAST_VISIBLE_MS * 2);
    });
    expect(screen.getByRole("status")).toBeInTheDocument();
    fireEvent.mouseLeave(stack);
    act(() => {
      vi.advanceTimersByTime(TOAST_VISIBLE_MS);
    });
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });
});
