import { useState } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { expect, it } from "vitest";
import { FormattedTextEditor } from "@/shared/document/FormattedTextEditor";
import { FormattedTextView } from "@/shared/document/FormattedTextView";
import { type FormattedText, formattedTextOf } from "@/shared/document/formattedText";

function Editor({ maxLength = 2000 }: { maxLength?: number }) {
  const [value, setValue] = useState<FormattedText>(formattedTextOf("Nội dung cũ"));
  return <><FormattedTextEditor value={value} plainText="Nội dung cũ" maxLength={maxLength}
    aria-label="Mô tả" onChange={(_text, next) => setValue(next)} />
    <output data-testid="document">{JSON.stringify(value)}</output></>;
}

it("keeps old plain descriptions readable and renders runs as text instead of HTML", () => {
  const { rerender, container } = render(<FormattedTextView text="Mô tả cũ\nDòng hai" />);
  expect(container).toHaveTextContent("Mô tả cũ");
  rerender(<FormattedTextView text="" document={{ blocks: [{ type: "UL", align: "RIGHT", indent: 1,
    runs: [{ text: "<img src=x onerror=alert(1)>", bold: true }] }] }} />);
  expect(container.querySelector("img")).toBeNull();
  expect(container.querySelector("ul li strong")).toHaveTextContent("<img src=x onerror=alert(1)>");
  expect(container.querySelector("ul")).toHaveStyle({ textAlign: "right" });
});

it("formats a paragraph and supports list, alignment and undo without a popup", () => {
  render(<Editor />);
  fireEvent.focus(screen.getByRole("textbox", { name: "Mô tả" }));
  fireEvent.click(screen.getByRole("button", { name: "Đậm" }));
  fireEvent.click(screen.getByRole("button", { name: "Danh sách chấm" }));
  fireEvent.click(screen.getByRole("button", { name: "Căn giữa" }));
  expect(JSON.parse(screen.getByTestId("document").textContent!).blocks[0]).toMatchObject({
    type: "UL", align: "CENTER", runs: [{ text: "Nội dung cũ", bold: true }],
  });
  fireEvent.click(screen.getByRole("button", { name: "Hoàn tác" }));
  expect(JSON.parse(screen.getByTestId("document").textContent!).blocks[0].align).toBe("LEFT");
});

it("splits paragraphs with Enter and rejects oversized edits without losing saved text", () => {
  render(<Editor maxLength={12} />);
  const field = screen.getByRole("textbox", { name: "Mô tả" });
  fireEvent.focus(field);
  fireEvent.keyDown(field, { key: "Enter" });
  expect(screen.getAllByRole("textbox")).toHaveLength(2);
  const next = screen.getAllByRole("textbox")[1]!;
  next.textContent = "nội dung quá dài";
  fireEvent.input(next);
  expect(screen.getByRole("alert")).toHaveTextContent("12 ký tự");
  expect(screen.getByTestId("document")).toHaveTextContent("Nội dung cũ");
});
