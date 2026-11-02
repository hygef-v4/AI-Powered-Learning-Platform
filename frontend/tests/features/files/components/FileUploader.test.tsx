import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { FileUploader } from "@/features/files/components/FileUploader";

const policies = [
  { purpose: "DOCUMENT_IMAGE", maxSizeMb: 1, allowedTypes: ["PNG"], extensions: ["png"], mediaTypes: ["image/png"] },
];

describe("FileUploader", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("từ chối tệp vượt dung lượng trước khi tải lên, không hiện chữ giới hạn", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify(policies), { status: 200 })));
    const xhr = vi.fn();
    vi.stubGlobal("XMLHttpRequest", xhr);
    const { container } = render(<FileUploader purpose="DOCUMENT_IMAGE" onUploaded={vi.fn()} />);

    await waitFor(() => expect(screen.getByRole("button", { name: "Chọn tệp" })).toBeEnabled());
    expect(screen.queryByText("PNG, tối đa 1 MB")).not.toBeInTheDocument();
    const input = container.querySelector("input[type=file]") as HTMLInputElement;
    const big = new File([new Uint8Array(1024 * 1024 + 1)], "anh.png", { type: "image/png" });
    fireEvent.change(input, { target: { files: [big] } });

    expect(await screen.findByRole("alert")).toHaveTextContent("Tệp vượt 1 MB");
    expect(xhr).not.toHaveBeenCalled();
  });
});
