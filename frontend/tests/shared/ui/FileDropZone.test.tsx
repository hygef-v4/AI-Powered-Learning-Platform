import { fireEvent, render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { FileDropZone, matchesAccept } from "@/shared/ui";

const docx = new File(["x"], "De.DOCX", { type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document" });
const png = new File(["x"], "a.png", { type: "image/png" });
const drag = (files: File[]) => ({ dataTransfer: { types: ["Files"], files, dropEffect: "" } });

it("matches accept by extension and mime family", () => {
  expect(matchesAccept(docx, ".docx")).toBe(true);
  expect(matchesAccept(png, ".docx")).toBe(false);
  expect(matchesAccept(png, ".pdf,image/*")).toBe(true);
  expect(matchesAccept(png)).toBe(true);
});

it("shows the drop overlay while dragging and passes only accepted files (first one unless multiple)", () => {
  const onFiles = vi.fn();
  render(<FileDropZone accept=".docx" label="Thả tệp" onFiles={onFiles}><span>Ô mẫu đề</span></FileDropZone>);
  const zone = screen.getByTestId("file-drop-zone");
  fireEvent.dragEnter(zone, drag([png, docx]));
  expect(screen.getByText("Thả tệp")).toBeInTheDocument();
  fireEvent.drop(zone, drag([png, docx, docx]));
  expect(onFiles).toHaveBeenCalledExactlyOnceWith([docx]);
  expect(screen.queryByText("Thả tệp")).toBeNull();
});

it("ignores drops while disabled", () => {
  const onFiles = vi.fn();
  render(<FileDropZone accept=".docx" label="Thả tệp" disabled onFiles={onFiles}><span>x</span></FileDropZone>);
  fireEvent.drop(screen.getByTestId("file-drop-zone"), drag([docx]));
  expect(onFiles).not.toHaveBeenCalled();
});
