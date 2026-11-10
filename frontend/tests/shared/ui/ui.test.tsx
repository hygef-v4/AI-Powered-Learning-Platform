import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { Dialog } from "@/shared/ui/Dialog";
import { ChipToggleGroup, Segmented, Switch } from "@/shared/ui/inputs";
import { Menu, MenuItem } from "@/shared/ui/Menu";
import { TableSkeletonRows } from "@/shared/ui/Skeleton";
import { TableFrame, Td } from "@/shared/ui/Table";

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

describe("TableFrame stackOnMobile", () => {
  it("đánh dấu bảng xếp thẻ, ô mang tên cột và ô trang trí được ẩn khi xếp thẻ", () => {
    const { container } = render(
      <TableFrame stackOnMobile>
        <tbody>
          <tr>
            <Td>Bài 1</Td>
            <Td label="Hạn nộp">20/11</Td>
            <Td hideWhenStacked>›</Td>
          </tr>
        </tbody>
      </TableFrame>,
    );
    expect(container.firstElementChild).toHaveClass("table-stack");
    expect(screen.getByText("20/11")).toHaveAttribute("data-label", "Hạn nộp");
    expect(screen.getByText("Bài 1")).not.toHaveAttribute("data-label");
    expect(screen.getByText("›")).toHaveAttribute("data-stack-hidden", "true");
  });
});

describe("TableSkeletonRows", () => {
  it("vẽ đủ số dòng và cột, có nhãn cho trình đọc màn hình", () => {
    render(
      <table>
        <tbody>
          <TableSkeletonRows rows={3} columns={4} label="Đang tải bài tập…" />
        </tbody>
      </table>,
    );
    expect(screen.getByRole("status")).toHaveTextContent("Đang tải bài tập…");
    expect(document.querySelectorAll("tr")).toHaveLength(3);
    expect(document.querySelectorAll("td")).toHaveLength(12);
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
