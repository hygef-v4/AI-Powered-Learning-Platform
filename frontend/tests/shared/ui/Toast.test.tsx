import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { showErrorToast, Toaster, TOAST_VISIBLE_MS } from "@/shared/ui/Toast";

beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  cleanup();
  vi.useRealTimers();
});

describe("Toaster", () => {

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
});
