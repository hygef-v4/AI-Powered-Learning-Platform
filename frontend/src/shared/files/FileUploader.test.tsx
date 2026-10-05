import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import { FileLink } from "./FileLink";
import { FileUploader } from "./FileUploader";
import { precheckFile } from "./purposes";

/** XHR giả: giữ request lại để test chủ động trả kết quả, báo tiến trình hoặc hủy. */
class FakeXhr {
  static last: FakeXhr | undefined;
  status = 0;
  responseText = "";
  withCredentials = false;
  upload: { onprogress?: (e: { lengthComputable: boolean; loaded: number; total: number }) => void } = {};
  onload?: () => void;
  onerror?: () => void;
  onabort?: () => void;
  sent?: FormData;
  url?: string;
  open(_method: string, url: string) {
    this.url = url;
  }
  setRequestHeader() {}
  send(body: FormData) {
    this.sent = body;
    FakeXhr.last = this;
  }
  abort() {
    this.onabort?.();
  }
  respond(status: number, body: unknown) {
    this.status = status;
    this.responseText = JSON.stringify(body);
    this.onload?.();
  }
}

function pick(file: File) {
  fireEvent.change(screen.getByTestId("file-uploader-input"), { target: { files: [file] } });
}

function fileOf(name: string, size: number) {
  const f = new File(["x"], name);
  Object.defineProperty(f, "size", { value: size });
  return f;
}

beforeEach(() => {
  FakeXhr.last = undefined;
  vi.stubGlobal("XMLHttpRequest", FakeXhr);
});
afterEach(() => vi.unstubAllGlobals());

describe("precheckFile", () => {
  it("chặn sai đuôi và vượt trần theo purpose", () => {
    expect(precheckFile(fileOf("a.png", 6 * 1024 * 1024), "DOCUMENT_IMAGE")).toBe("Tệp vượt quá 5 MB.");
    expect(precheckFile(fileOf("a.png", 6 * 1024 * 1024), "AVATAR")).toBeNull();
    expect(precheckFile(fileOf("a.exe", 10), "MATERIAL")).toMatch(/Chỉ nhận tệp/);
  });
});

describe("FileUploader", () => {
  it("không gửi request khi tệp vượt trần phía client", () => {
    render(<FileUploader purpose="DOCUMENT_IMAGE" onUploaded={vi.fn()} />);
    pick(fileOf("hinh.png", 5 * 1024 * 1024 + 1));
    expect(screen.getByRole("alert")).toHaveTextContent("Tệp vượt quá 5 MB.");
    expect(FakeXhr.last).toBeUndefined();
  });

  it("gửi purpose + file, hiện tiến trình rồi trả fileRef", async () => {
    const onUploaded = vi.fn();
    render(<FileUploader purpose="MATERIAL" onUploaded={onUploaded} />);
    pick(fileOf("bai.pdf", 100));

    const xhr = FakeXhr.last!;
    expect(xhr.url).toBe("/api/v1/files");
    expect(xhr.withCredentials).toBe(true);
    expect(xhr.sent?.get("purpose")).toBe("MATERIAL");
    act(() => xhr.upload.onprogress?.({ lengthComputable: true, loaded: 50, total: 100 }));
    expect(screen.getByText("50%")).toBeInTheDocument();

    act(() => xhr.respond(201, { fileRef: "ref", expiresAt: "t", mediaType: "application/pdf", byteSize: 100, originalFileName: "bai.pdf" }));
    await waitFor(() => expect(onUploaded).toHaveBeenCalledWith(expect.objectContaining({ fileRef: "ref" })));
  });

  it("nút Hủy dừng request và không gọi onUploaded", async () => {
    const onUploaded = vi.fn();
    render(<FileUploader purpose="MATERIAL" onUploaded={onUploaded} />);
    pick(fileOf("bai.pdf", 100));

    fireEvent.click(screen.getByTestId("file-uploader-cancel-button"));
    await waitFor(() => expect(screen.queryByTestId("file-uploader-progress")).not.toBeInTheDocument());
    expect(onUploaded).not.toHaveBeenCalled();
  });

  it("hiện thông điệp an toàn từ backend", async () => {
    render(<FileUploader purpose="DOCUMENT_IMAGE" onUploaded={vi.fn()} />);
    pick(fileOf("anh.png", 100));
    act(() => FakeXhr.last!.respond(415, { title: "Loại tệp không được hỗ trợ cho mục đích này.", code: "FILE_TYPE_NOT_ALLOWED" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Loại tệp không được hỗ trợ cho mục đích này.");
  });
});

describe("FileLink", () => {
  it("chỉ lấy URL khi bấm và báo lỗi của unit sở hữu", async () => {
    const openSpy = vi.spyOn(window, "open").mockImplementation(() => null);
    const getUrl = vi.fn().mockResolvedValueOnce({ url: "/api/v1/files/download/t" })
      .mockRejectedValueOnce(new ApiError({ status: 403, title: "Bạn không có quyền xem tệp này." }));
    render(<FileLink fileName="bai.pdf" getDownloadUrl={getUrl} />);
    expect(getUrl).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("file-link"));
    await waitFor(() => expect(openSpy).toHaveBeenCalledWith("/api/v1/files/download/t", "_blank", "noopener"));

    fireEvent.click(screen.getByTestId("file-link"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Bạn không có quyền xem tệp này.");
  });
});
