import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { Segmented } from "@/shared/ui/Inputs";
import { Menu, MenuItem } from "@/shared/ui/Menu";




function Filter() {
  const [value, setValue] = useState("all");
  return (
    <Segmented
      as="tab"
      ariaLabel="Trạng thái"
      value={value}
      onChange={setValue}
      options={[
        { value: "all", label: "Tất cả", count: 3 },
        { value: "locked", label: "Khóa", disabled: true },
        { value: "open", label: "Đang mở" },
      ]}
    />
  );
}

describe("Segmented", () => {
  it("Tab chỉ dừng ở mục đang chọn; mũi tên đổi lựa chọn, bỏ qua mục khóa và đưa focus theo", () => {
    render(<Filter />);
    const all = screen.getByRole("tab", { name: /^Tất cả/ });
    const open = screen.getByRole("tab", { name: "Đang mở" });
    expect(all).toHaveAttribute("tabindex", "0");
    expect(open).toHaveAttribute("tabindex", "-1");

    all.focus();
    fireEvent.keyDown(all, { key: "ArrowRight" });
    expect(open).toHaveAttribute("aria-selected", "true");
    expect(open).toHaveFocus();
    expect(open).toHaveAttribute("tabindex", "0");

    fireEvent.keyDown(open, { key: "ArrowRight" });
    expect(all).toHaveAttribute("aria-selected", "true");
    fireEvent.keyDown(all, { key: "End" });
    expect(open).toHaveAttribute("aria-selected", "true");
  });
});

describe("Menu", () => {
  it("nút mở báo có menu; mũi tên, Home, End di chuyển giữa các mục", () => {
    render(
      <Menu label="Thao tác">
        <MenuItem>Sửa</MenuItem>
        <MenuItem disabled>Khóa</MenuItem>
        <MenuItem>Xóa</MenuItem>
      </Menu>,
    );
    const details = screen.getByRole("group");
    const summary = details.querySelector("summary")!;
    expect(summary).toHaveAttribute("aria-haspopup", "menu");
    (details as HTMLDetailsElement).open = true;

    const edit = screen.getByRole("menuitem", { name: "Sửa" });
    const remove = screen.getByRole("menuitem", { name: "Xóa" });
    edit.focus();
    fireEvent.keyDown(edit, { key: "ArrowDown" });
    expect(remove).toHaveFocus();
    fireEvent.keyDown(remove, { key: "ArrowDown" });
    expect(edit).toHaveFocus();
    fireEvent.keyDown(edit, { key: "End" });
    expect(remove).toHaveFocus();

    fireEvent.keyDown(document, { key: "Escape" });
    expect(details).not.toHaveAttribute("open");
    expect(summary).toHaveFocus();
  });
});




