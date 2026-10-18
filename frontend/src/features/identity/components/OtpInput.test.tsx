import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { OtpInput } from "./OtpInput";

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

  it("dán cả mã thì điền đủ 6 ô", () => {
    render(<Harness />);
    fireEvent.paste(cell(1), { clipboardData: { getData: () => "48 19-27" } });
    expect(screen.getByTestId("value")).toHaveTextContent("481927");
    expect(cell(6)).toHaveValue("7");
  });

  it("trình duyệt tự điền cả mã vào ô đầu thì điền đủ các ô", () => {
    render(<Harness />);
    fireEvent.change(cell(1), { target: { value: "481927" } });
    expect(screen.getByTestId("value")).toHaveTextContent("481927");
  });

  it("Backspace ở ô trống xóa và lùi về ô trước", () => {
    render(<Harness />);
    fireEvent.change(cell(1), { target: { value: "4" } });
    fireEvent.change(cell(2), { target: { value: "8" } });
    fireEvent.keyDown(cell(3), { key: "Backspace" });
    expect(screen.getByTestId("value")).toHaveTextContent(/^4$/);
    expect(cell(2)).toHaveFocus();
  });
});
