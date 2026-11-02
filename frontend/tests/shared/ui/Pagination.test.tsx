import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Pagination, pageItems } from "@/shared/ui/Pagination";

describe("pageItems", () => {
  it("hiện hết khi ít trang", () => {
    expect(pageItems(0, 5)).toEqual([0, 1, 2, 3, 4]);
  });

  it("rút gọn bằng dấu … quanh trang hiện tại", () => {
    expect(pageItems(0, 29)).toEqual([0, 1, 2, 3, "gap", 28]);
    expect(pageItems(14, 29)).toEqual([0, "gap", 13, 14, 15, "gap", 28]);
    expect(pageItems(28, 29)).toEqual([0, "gap", 25, 26, 27, 28]);
  });
});

describe("Pagination", () => {
  it("hiện dòng đếm theo design và chuyển trang", () => {
    const onPageChange = vi.fn();
    render(<Pagination page={0} pageSize={50} total={1428} onPageChange={onPageChange} note="50 dòng mỗi trang" />);

    expect(screen.getByText("Hiển thị 1–50 trong 1.428 bản ghi · 50 dòng mỗi trang")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "1" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("button", { name: "Trang trước" })).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: "Trang sau" }));
    fireEvent.click(screen.getByRole("button", { name: "29" }));
    expect(onPageChange.mock.calls).toEqual([[1], [28]]);
  });

  it("trang cuối không bấm Trang sau được và không có nút khi chỉ một trang", () => {
    const { rerender } = render(<Pagination page={2} pageSize={50} total={120} onPageChange={vi.fn()} />);
    expect(screen.getByText("Hiển thị 101–120 trong 120 bản ghi")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Trang sau" })).toBeDisabled();

    rerender(<Pagination page={0} pageSize={50} total={0} onPageChange={vi.fn()} />);
    expect(screen.getByText("Không có bản ghi")).toBeInTheDocument();
    expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
  });
});
