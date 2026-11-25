import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { OtpInput } from "@/features/identity/components/OtpInput";

function Harness() {
  const [value, setValue] = useState("");
  return (
    <>
      <OtpInput value={value} onChange={setValue} />
      <output data-testid="value">{value}</output>
    </>
  );
}

const cell = (n: number) => screen.getByLabelText(`Chữ số ${n}`);

describe("OtpInput", () => {
  it("chỉ nhận chữ số và tự nhảy sang ô kế", () => {
    render(<Harness />);
    fireEvent.change(cell(1), { target: { value: "4" } });
    fireEvent.change(cell(2), { target: { value: "x" } });
    expect(screen.getByTestId("value")).toHaveTextContent(/^4$/);
    expect(cell(2)).toHaveFocus();
  });
});
