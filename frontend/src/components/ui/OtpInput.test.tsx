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

describe("OtpInput", () => {
  it("chỉ nhận chữ số", () => {
    render(<Harness />);
    fireEvent.change(screen.getByLabelText("Chữ số 1"), { target: { value: "a" } });
    expect(screen.getByTestId("value")).toHaveTextContent("");
    fireEvent.change(screen.getByLabelText("Chữ số 1"), { target: { value: "7" } });
    expect(screen.getByTestId("value")).toHaveTextContent("7");
  });

  it("dán cả mã thì điền đủ 6 ô", () => {
    render(<Harness />);
    fireEvent.paste(screen.getByLabelText("Chữ số 1"), { clipboardData: { getData: () => "12-34 56" } });
    expect(screen.getByTestId("value")).toHaveTextContent("123456");
  });
});
