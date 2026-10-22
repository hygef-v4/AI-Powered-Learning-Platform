import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { apiFetch, ApiError } from "@/shared/api/client";
import { MaterialSourceDialog } from "./MaterialDialogs";
vi.mock("@/shared/api/client", async importOriginal => ({ ...await importOriginal<typeof import("@/shared/api/client")>(), apiFetch: vi.fn() }));
beforeEach(() => { vi.mocked(apiFetch).mockReset(); });
it("rejects playlist before creating anything", async () => {
  render(<MaterialSourceDialog path="/materials" kind="YOUTUBE_LINK" onClose={vi.fn()} onSaved={vi.fn()} />);
  fireEvent.change(screen.getByTestId("material-source-title"), { target: { value: "Video" } });
  fireEvent.change(screen.getByTestId("material-source-url"), { target: { value: "https://youtu.be/abcdefghijk?list=x" } });
  fireEvent.click(screen.getByTestId("material-source-save")); expect(await screen.findByRole("alert")).toHaveTextContent("không nhận playlist"); expect(apiFetch).not.toHaveBeenCalled();
});
it("submits a single video and closes through success callback", async () => {
  vi.mocked(apiFetch).mockResolvedValue({}); const saved = vi.fn();
  render(<MaterialSourceDialog path="/materials" kind="YOUTUBE_LINK" onClose={vi.fn()} onSaved={saved} />);
  fireEvent.change(screen.getByTestId("material-source-title"), { target: { value: "Video" } });
  fireEvent.change(screen.getByTestId("material-source-url"), { target: { value: "https://youtu.be/abcdefghijk" } });
  fireEvent.click(screen.getByTestId("material-source-save")); await waitFor(() => expect(saved).toHaveBeenCalledOnce());
  expect(apiFetch).toHaveBeenCalledWith("/materials", expect.objectContaining({ method: "POST" }));
});
it("keeps dialog open and shows conflict with reload guidance", async () => {
  vi.mocked(apiFetch).mockRejectedValue(new ApiError({ status: 409 })); const saved = vi.fn();
  render(<MaterialSourceDialog path="/materials" kind="YOUTUBE_LINK" onClose={vi.fn()} onSaved={saved} />);
  fireEvent.change(screen.getByTestId("material-source-title"), { target: { value: "Video" } });
  fireEvent.change(screen.getByTestId("material-source-url"), { target: { value: "https://youtu.be/abcdefghijk" } });
  fireEvent.click(screen.getByTestId("material-source-save")); expect(await screen.findByRole("alert")).toHaveTextContent("Tải lại"); expect(saved).not.toHaveBeenCalled();
});
