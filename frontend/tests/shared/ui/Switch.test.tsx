import { act, fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { expect, it } from "vitest";
import { Switch } from "@/shared/ui";

function Saved({ save }: { save: (next: boolean) => Promise<void> }) {
  const [value, setValue] = useState(false);
  return <Switch checked={value} label="Bật" ariaLabel="Bật" onChange={(next) => save(next).then(() => setValue(next))} />;
}

it("gạt ngay khi bấm, khóa tới khi server trả về rồi theo giá trị đã lưu", async () => {
  let finish!: () => void;
  render(<Saved save={() => new Promise<void>((resolve) => { finish = resolve; })} />);
  const toggle = screen.getByRole("switch", { name: "Bật" });
  fireEvent.click(toggle);
  expect(toggle).toHaveAttribute("aria-checked", "true");
  expect(toggle).toHaveAttribute("aria-busy", "true");
  fireEvent.click(toggle);
  expect(toggle).toHaveAttribute("aria-checked", "true");
  await act(async () => finish());
  expect(toggle).toHaveAttribute("aria-checked", "true");
  expect(toggle).not.toHaveAttribute("aria-busy");
});

it("lưu lỗi thì công tắc về lại giá trị cũ", async () => {
  let fail!: () => void;
  render(<Saved save={() => new Promise<void>((_, reject) => { fail = () => reject(new Error("down")); })} />);
  const toggle = screen.getByRole("switch", { name: "Bật" });
  fireEvent.click(toggle);
  expect(toggle).toHaveAttribute("aria-checked", "true");
  await act(async () => fail());
  expect(toggle).toHaveAttribute("aria-checked", "false");
});
