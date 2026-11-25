import { useState } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DateInput } from "@/shared/ui/DateInput";
import { Field } from "@/shared/ui/Inputs";

function Controlled({ initial = "2026-11-15", min, max }: {
  initial?: string; min?: string; max?: string;
}) {
  const [value, setValue] = useState(initial);
  return <>
    <Field label="Ngày bắt đầu" htmlFor="date">
      <DateInput id="date" value={value} onChange={setValue} min={min} max={max} />
    </Field>
    <output data-testid="iso">{value}</output>
    <button onClick={() => setValue("2027-04-03")}>Reset</button>
  </>;
}

describe("DateInput", () => {

  it("validates inclusive ISO min/max and leap centuries", () => {
    render(<Controlled min="2000-01-01" max="2099-12-31" />);
    const input = screen.getByRole("textbox");
    for (const date of ["31/12/1999", "01/01/2100", "29/02/2100"]) {
      fireEvent.change(input, { target: { value: date } });
      expect(input).toHaveAttribute("aria-invalid", "true");
      expect(screen.getByTestId("iso")).toBeEmptyDOMElement();
    }
    for (const [date, iso] of [["01/01/2000", "2000-01-01"], ["29/02/2000", "2000-02-29"], ["31/12/2099", "2099-12-31"]] as const) {
      fireEvent.change(input, { target: { value: date } });
      expect(input).not.toHaveAttribute("aria-invalid");
      expect(screen.getByTestId("iso")).toHaveTextContent(iso);
    }
  });

  it("opens the native picker from the accessible button and accepts its ISO date", () => {
    const onChange = vi.fn();
    const { container, rerender } = render(<DateInput id="date" aria-label="Ngày" data-testid="date-visible"
      value="2026-11-15" min="2000-01-01" max="2099-12-31" onChange={onChange} />);
    const native = container.querySelector<HTMLInputElement>('input[type="date"]')!;
    native.showPicker = vi.fn();
    const button = screen.getByRole("button", { name: "Chọn ngày từ lịch" });
    expect(button).toHaveAttribute("type", "button");
    button.focus();
    fireEvent.click(button);
    expect(native.showPicker).toHaveBeenCalledOnce();
    fireEvent.keyDown(screen.getByTestId("date-visible"), { altKey: true, key: "ArrowDown" });
    expect(native.showPicker).toHaveBeenCalledTimes(2);
    expect(native).toHaveAttribute("min", "2000-01-01");
    expect(native).toHaveAttribute("max", "2099-12-31");
    fireEvent.change(native, { target: { value: "2028-02-29" } });
    expect(onChange).toHaveBeenCalledWith("2028-02-29");
    rerender(<DateInput id="date" aria-label="Ngày" data-testid="date-visible" value="2028-02-29" onChange={onChange} />);
    expect(screen.getByTestId("date-visible")).toHaveValue("29/02/2028");
    expect(screen.getAllByLabelText("Ngày")).toHaveLength(1);
  });
});
