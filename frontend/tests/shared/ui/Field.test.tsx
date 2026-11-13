import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Field, Select, TextInput } from "@/shared/ui/inputs";

describe("Field error descriptions", () => {
  it("nối lỗi tới ô nhập, giữ mô tả có sẵn và bỏ liên kết khi hết lỗi", () => {
    function Form({ error }: { error?: string }) {
      return <>
        <p id="name-hint">Tên trên hồ sơ</p>
        <Field label="Họ và tên" htmlFor="name" error={error}>
          <TextInput id="name" aria-describedby="name-hint" invalid={!!error} />
        </Field>
      </>;
    }
    const { rerender } = render(<Form error="Vui lòng nhập họ tên." />);
    const input = screen.getByRole("textbox", { name: "Họ và tên" });
    input.focus();
    expect(screen.getByRole("textbox", { name: "Họ và tên" }))
      .toHaveAccessibleDescription("Tên trên hồ sơ Vui lòng nhập họ tên.");
    rerender(<Form />);
    expect(screen.getByRole("textbox", { name: "Họ và tên" })).toBe(input);
    expect(input).toHaveFocus();
    expect(screen.getByRole("textbox", { name: "Họ và tên" }))
      .toHaveAccessibleDescription("Tên trên hồ sơ");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    rerender(<Form error="Họ tên chưa hợp lệ." />);
    expect(screen.getByRole("textbox", { name: "Họ và tên" })).toBe(input);
    expect(input).toHaveFocus();
    expect(input).toHaveAccessibleDescription("Tên trên hồ sơ Họ tên chưa hợp lệ.");
  });

  it("nối đúng control trong wrapper và không gán lỗi sang ô bên cạnh", () => {
    render(<Field label="Từ ngày" htmlFor="from" error="Khoảng ngày không hợp lệ.">
      <div><TextInput id="from" /><TextInput aria-label="Đến ngày" /></div>
    </Field>);
    expect(screen.getByRole("textbox", { name: "Từ ngày" }))
      .toHaveAccessibleDescription("Khoảng ngày không hợp lệ.");
    expect(screen.getByRole("textbox", { name: "Đến ngày" })).not.toHaveAttribute("aria-describedby");
  });

  it("hỗ trợ select và textarea native với lỗi độc lập", () => {
    render(<>
      <Field label="Vai trò" htmlFor="role" error="Chọn vai trò.">
        <Select id="role"><option value="">Chưa chọn</option></Select>
      </Field>
      <Field label="Mô tả" htmlFor="description" error="Mô tả quá dài.">
        <textarea id="description" />
      </Field>
      <Field label="Số câu" htmlFor="count" error="Từ 1 đến 10.">
        <input id="count" type="number" />
      </Field>
    </>);
    expect(screen.getByRole("combobox", { name: "Vai trò" })).toHaveAccessibleDescription("Chọn vai trò.");
    expect(screen.getByRole("textbox", { name: "Mô tả" })).toHaveAccessibleDescription("Mô tả quá dài.");
    expect(screen.getByRole("spinbutton", { name: "Số câu" })).toHaveAccessibleDescription("Từ 1 đến 10.");
  });
});
