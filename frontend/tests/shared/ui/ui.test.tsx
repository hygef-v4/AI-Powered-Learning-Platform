import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { Dialog } from "@/shared/ui/Dialog";
import { ChipToggleGroup, Switch } from "@/shared/ui/inputs";

describe("Dialog", () => {
  it("đóng khi bấm Esc hoặc nút Đóng", () => {
    const onClose = vi.fn();
    render(
      <Dialog open title="Xác nhận" onClose={onClose}>
        Nội dung
      </Dialog>,
    );

    expect(screen.getByRole("dialog", { name: "Xác nhận" })).toBeInTheDocument();
    fireEvent.keyDown(document, { key: "Escape" });
    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));
    expect(onClose).toHaveBeenCalledTimes(2);
  });

  it("không vẽ gì khi đóng", () => {
    render(
      <Dialog open={false} title="Xác nhận" onClose={vi.fn()}>
        Nội dung
      </Dialog>,
    );
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});

function Chips() {
  const [value, setValue] = useState<string[]>(["PPTX"]);
  return (
    <>
      <ChipToggleGroup ariaLabel="Loại" options={["PDF", "DOCX", "PPTX"]} value={value} onChange={setValue} />
      <output>{value.join(",")}</output>
    </>
  );
}

describe("ChipToggleGroup", () => {
  it("bật tắt từng lựa chọn và giữ thứ tự của danh sách lựa chọn", () => {
    render(<Chips />);

    fireEvent.click(screen.getByRole("button", { name: "PDF" }));
    expect(screen.getByRole("status")).toHaveTextContent("PDF,PPTX");
    expect(screen.getByRole("button", { name: "PDF" })).toHaveAttribute("aria-pressed", "true");

    fireEvent.click(screen.getByRole("button", { name: "PPTX" }));
    expect(screen.getByRole("status")).toHaveTextContent("PDF");
  });
});

describe("Switch", () => {
  it("đảo trạng thái khi bấm", () => {
    const onChange = vi.fn();
    render(<Switch checked={false} onChange={onChange} label="Tắt" ariaLabel="Bật sao lưu" />);

    fireEvent.click(screen.getByRole("switch", { name: "Bật sao lưu" }));

    expect(onChange).toHaveBeenCalledWith(true);
  });
});
