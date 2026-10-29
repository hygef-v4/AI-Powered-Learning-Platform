import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { apiDownload, apiFetch } from "@/shared/api/client";
import { MaterialPreview } from "./MaterialPreview";
import type { Material } from "./types";
vi.mock("@/shared/api/client", async original => ({ ...await original<typeof import("@/shared/api/client")>(), apiFetch: vi.fn(), apiDownload: vi.fn() }));
// pdf.js cần canvas và worker thật; ở đây chỉ kiểm tệp được tải đúng và đưa vào trình xem.
vi.mock("./PdfPages", () => ({ PdfPages: ({ blob }: { blob: Blob }) => <div data-testid="material-pdf-viewer">{blob.size} bytes</div> }));
const file = { id: "a", currentVersionNo: 1, title: "PDF reference", kind: "FILE", current: { mimeType: "application/pdf", fileName: "ref.pdf" } } as Material;
const token = { url: "/api/v1/files/download/signed", expiresAt: new Date(Date.now() + 300_000).toISOString() };
beforeEach(() => { vi.mocked(apiFetch).mockReset(); vi.mocked(apiDownload).mockReset(); });

it("downloads the PDF through an authorized token into the dark document viewer", async () => {
  vi.mocked(apiFetch).mockResolvedValue(token);
  vi.mocked(apiDownload).mockResolvedValue(new Blob(["pdf"], { type: "application/pdf" }));
  render(<MaterialPreview material={file} classId="class1" />);
  expect(await screen.findByTestId("material-pdf-viewer")).toHaveTextContent("3 bytes");
  expect(apiDownload).toHaveBeenCalledWith("/api/v1/files/download/signed", expect.objectContaining({ signal: expect.any(AbortSignal) }));
  expect(screen.getByRole("button", { name: "Phóng to" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Tải xuống" })).toBeInTheDocument();
});

it("retries token acquisition after failure", async () => {
  vi.mocked(apiFetch).mockRejectedValueOnce(new Error("offline")).mockResolvedValue(token);
  vi.mocked(apiDownload).mockResolvedValue(new Blob(["pdf"]));
  render(<MaterialPreview material={file} />);
  await screen.findByRole("alert"); fireEvent.click(screen.getByTestId("material-token-retry"));
  await screen.findByTestId("material-pdf-viewer"); expect(apiFetch).toHaveBeenCalledTimes(2);
});

it("embeds only a validated ID using youtube-nocookie", () => {
  render(<MaterialPreview material={{ ...file, kind: "YOUTUBE_LINK", current: { ...file.current, videoId: "abcdefghijk" } }} />);
  expect(screen.getByTestId("material-youtube-player")).toHaveAttribute("src", "https://www.youtube-nocookie.com/embed/abcdefghijk");
  expect(apiFetch).not.toHaveBeenCalled();
});

it("does not embed a malformed video ID", () => {
  render(<MaterialPreview material={{ ...file, kind: "YOUTUBE_LINK", current: { ...file.current, videoId: "../malicious" } }} />);
  expect(screen.queryByTestId("material-youtube-player")).not.toBeInTheDocument();
});
