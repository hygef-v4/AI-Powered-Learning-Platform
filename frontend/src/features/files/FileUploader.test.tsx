import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { FileUploader } from "./FileUploader";

const policies = [
  { purpose: "DOCUMENT_IMAGE", maxSizeMb: 1, allowedTypes: ["PNG"], extensions: ["png"], mediaTypes: ["image/png"] },
];

describe("FileUploader", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("hiện giới hạn hiện hành và từ chối tệp vượt dung lượng trước khi tải lên", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify(policies), { status: 200 })));
    const xhr = vi.fn();
    vi.stubGlobal("XMLHttpRequest", xhr);
    const { container } = render(<FileUploader purpose="DOCUMENT_IMAGE" onUploaded={vi.fn()} />);

    expect(await screen.findByText("PNG, tối đa 1 MB")).toBeInTheDocument();
    const input = container.querySelector("input[type=file]") as HTMLInputElement;
    const big = new File([new Uint8Array(1024 * 1024 + 1)], "anh.png", { type: "image/png" });
    fireEvent.change(input, { target: { files: [big] } });

    expect(await screen.findByRole("alert")).toHaveTextContent("Tệp vượt 1 MB");
    expect(xhr).not.toHaveBeenCalled();
  });
});
