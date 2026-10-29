import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { apiFetch, ApiError } from "@/shared/api/client";
import { ModuleItemDialog } from "./MaterialDialogs";
vi.mock("@/shared/api/client", async importOriginal => ({ ...await importOriginal<typeof import("@/shared/api/client")>(), apiFetch: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
beforeEach(() => {
  vi.mocked(apiFetch).mockReset();
  vi.mocked(apiFetch).mockImplementation(async (path) => String(path).includes("/files/") ? [] : {});
});
const posts = () => vi.mocked(apiFetch).mock.calls.filter(([, init]) => init?.method === "POST");

const scope = { kind: "subject" as const, subjectId: "s1" };
function youtube(saved = vi.fn()) {
  render(<ModuleItemDialog scope={scope} moduleId="m1" onClose={vi.fn()} onSaved={saved} />);
  fireEvent.click(screen.getByRole("radio", { name: "YouTube" }));
  fireEvent.change(screen.getByTestId("material-source-title"), { target: { value: "Video" } });
  return saved;
}

it("lays out name, content choice and description in that order", () => {
  render(<ModuleItemDialog scope={scope} moduleId="m1" onClose={vi.fn()} onSaved={vi.fn()} />);
  const labels = [...document.querySelectorAll("label, span")].map(node => node.textContent ?? "");
  expect(labels.findIndex(t => t.startsWith("Tên học liệu"))).toBeLessThan(labels.findIndex(t => t.startsWith("Nội dung")));
  expect(labels.findIndex(t => t.startsWith("Nội dung"))).toBeLessThan(labels.findIndex(t => t.startsWith("Mô tả")));
  expect(screen.getByRole("radio", { name: "Tệp" })).toBeChecked();
});

it("rejects playlist before creating anything", async () => {
  youtube();
  fireEvent.change(screen.getByTestId("material-source-url"), { target: { value: "https://youtu.be/abcdefghijk?list=x" } });
  fireEvent.click(screen.getByTestId("material-source-save"));
  expect(await screen.findByRole("alert")).toHaveTextContent("không nhận playlist");
  expect(posts()).toHaveLength(0);
});

it("creates a single video inside the module", async () => {
  const saved = youtube();
  fireEvent.change(screen.getByTestId("material-source-url"), { target: { value: "https://youtu.be/abcdefghijk" } });
  fireEvent.click(screen.getByTestId("material-source-save"));
  await waitFor(() => expect(saved).toHaveBeenCalledOnce());
  const [path, init] = posts()[0]!;
  expect(path).toBe("/api/v1/subjects/s1/materials");
  expect(JSON.parse(String(init!.body))).toMatchObject({ kind: "YOUTUBE_LINK", title: "Video", moduleId: "m1" });
});

it("keeps the dialog open and shows conflict with reload guidance", async () => {
  vi.mocked(apiFetch).mockImplementation(async (path, init) => { if (init?.method === "POST") throw new ApiError({ status: 409 }); return String(path).includes("/files/") ? [] : {}; });
  const saved = youtube();
  fireEvent.change(screen.getByTestId("material-source-url"), { target: { value: "https://youtu.be/abcdefghijk" } });
  fireEvent.click(screen.getByTestId("material-source-save"));
  expect(await screen.findByRole("alert")).toHaveTextContent("Tải lại");
  expect(saved).not.toHaveBeenCalled();
});
