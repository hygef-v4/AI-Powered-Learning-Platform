import { render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { SourceImagePreview } from "@/features/assignments/components/SourceImagePreview";

afterEach(() => vi.unstubAllGlobals());

it("previews local images and releases each object URL on replace or unmount", () => {
  const create = vi.fn().mockReturnValueOnce("blob:first").mockReturnValueOnce("blob:next");
  const revoke = vi.fn();
  vi.stubGlobal("URL", { createObjectURL: create, revokeObjectURL: revoke });
  const first = new File(["png"], "one.png", { type: "image/png" });
  const next = new File(["jpeg"], "two.jpg", { type: "image/jpeg" });
  const view = render(<SourceImagePreview file={first} />);
  expect(screen.getByRole("img")).toHaveAttribute("src", "blob:first");
  view.rerender(<SourceImagePreview file={next} />);
  expect(revoke).toHaveBeenCalledWith("blob:first");
  expect(screen.getByRole("img")).toHaveAttribute("src", "blob:next");
  view.unmount();
  expect(revoke).toHaveBeenCalledWith("blob:next");
});
