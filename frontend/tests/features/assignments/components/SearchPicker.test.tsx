import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SearchPicker } from "@/features/assignments/components/SearchPicker";

describe("SearchPicker", () => {
  it("shows the hint beside the label and filters by label only", async () => {
    render(<SearchPicker id="p" label="Mẫu đề" onPick={vi.fn()} options={[
      { id: "a", label: "Đề cuối kỳ", hint: "20/11/2026" },
      { id: "b", label: "Đề cuối kỳ", hint: "02/05/2026" },
    ]} />);
    const input = screen.getByRole("combobox", { name: "Mẫu đề" });
    fireEvent.focus(input);
    const list = await screen.findByRole("listbox");
    expect(within(list).getAllByRole("option").map((option) => option.textContent))
      .toEqual(["Đề cuối kỳ 20/11/2026", "Đề cuối kỳ 02/05/2026"]);
    fireEvent.change(input, { target: { value: "2026" } });
    expect(within(screen.getByRole("listbox")).queryAllByRole("option")).toHaveLength(0);
  });
});
