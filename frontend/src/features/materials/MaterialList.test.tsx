import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { apiFetch } from "@/shared/api/client";
import { MaterialList } from "./MaterialList";
import { ClassMaterialsCard, MaterialLinkList } from "./MaterialSections";
import type { Material } from "./types";
vi.mock("@/shared/api/client", async importOriginal => ({ ...await importOriginal<typeof import("@/shared/api/client")>(), apiFetch: vi.fn() }));
const row = (id: string, changes: Partial<Material> = {}): Material => ({ id, scope: "SUBJECT", subjectId: "subject", classId: null, kind: "FILE", title: `Document ${id}`, description: "", orderNo: 0, status: "ACTIVE", currentVersionNo: 1, version: 2, updatedAt: "2026-10-21T00:00:00Z", canManage: true, versions: [], current: { versionNo: 1, fileName: "doc.pdf", mimeType: "application/pdf", byteSize: 1000, videoUrl: null, videoId: null, textStatus: "EXTRACTED", createdBy: "actor", createdAt: "2026-10-21T00:00:00Z" }, ...changes });
beforeEach(() => { vi.mocked(apiFetch).mockReset(); });
afterEach(() => { vi.useRealTimers(); });
it("filters active and archived rows", async () => {
  vi.mocked(apiFetch).mockResolvedValue([row("a"), row("b", { status: "ARCHIVED", canManage: false })]);
  render(<MaterialList scope="subjects" scopeId="subject" />);
  expect(await screen.findByText("Document a")).toBeInTheDocument(); expect(screen.queryByText("Document b")).not.toBeInTheDocument();
  fireEvent.click(screen.getByTestId("materials-filter-archived")); expect(screen.getByText("Document b")).toBeInTheDocument();
});
it("readonly scope hides add and reorder controls", async () => {
  vi.mocked(apiFetch).mockResolvedValue([row("a", { canManage: false })]);
  render(<MaterialList scope="subjects" scopeId="subject" readOnly />); await screen.findByText("Document a");
  expect(screen.queryByTestId("materials-add-menu")).not.toBeInTheDocument(); expect(screen.queryByLabelText("Đưa Document a lên")).not.toBeInTheDocument();
});
it("reorders with optimistic versions and reloads after success", async () => {
  vi.mocked(apiFetch).mockImplementation(async (_path, init) => init?.method === "PUT" ? undefined : [row("a"), row("b")]);
  render(<MaterialList scope="subjects" scopeId="subject" />); await screen.findByText("Document b");
  fireEvent.click(screen.getByLabelText("Đưa Document b lên"));
  await waitFor(() => expect(apiFetch).toHaveBeenCalledWith("/api/v1/subjects/subject/materials:reorder", expect.objectContaining({ method: "PUT", body: JSON.stringify({ items: [{ id: "b", version: 2 }, { id: "a", version: 2 }] }) })));
});
it("discards an old response when switching subjects", async () => {
  let finish: (value: unknown) => void = () => {};
  vi.mocked(apiFetch).mockImplementation(path => path.includes("old") ? new Promise(resolve => { finish = resolve; }) : Promise.resolve([row("new")]));
  const view = render(<MaterialList scope="subjects" scopeId="old" />); view.rerender(<MaterialList scope="subjects" scopeId="new" />);
  await screen.findByText("Document new"); await act(async () => finish([row("old")]));
  expect(screen.queryByText("Document old")).not.toBeInTheDocument();
});
it("refreshes pending text extraction until the status changes without reloading the page", async () => {
  const pending = row("pending"); pending.current = { ...pending.current, textStatus: "PENDING" };
  const extracted = row("pending"); extracted.current = { ...extracted.current, textStatus: "EXTRACTED" };
  vi.mocked(apiFetch).mockResolvedValueOnce([pending]).mockResolvedValueOnce([extracted]);
  vi.useFakeTimers();

  render(<MaterialList scope="subjects" scopeId="subject" />);
  await act(async () => { await Promise.resolve(); });
  expect(screen.getByText("Đang xử lý")).toBeInTheDocument();

  await act(async () => { await vi.advanceTimersByTimeAsync(3000); });

  expect(screen.getByText("Đã trích")).toBeInTheDocument();
  expect(apiFetch).toHaveBeenCalledTimes(2);
});
it("learner links retain class context and lead to viewing route", async () => {
  vi.mocked(apiFetch).mockResolvedValue([row("a", { canManage: false })]); render(<ClassMaterialsCard classId="class1" />);
  expect(await screen.findByTestId("material-attached-a")).toHaveAttribute("href", "/materials/a/view?classId=class1");
});
it("empty attachment slot makes no request", () => {
  render(<MaterialLinkList classId="class1" materialIds={[]} />); expect(apiFetch).not.toHaveBeenCalled(); expect(screen.queryByText("Học liệu gắn kèm")).not.toBeInTheDocument();
});
it("attachment requests constrain IDs and class", async () => {
  vi.mocked(apiFetch).mockResolvedValue([]); render(<MaterialLinkList classId="class1" materialIds={["a", "b"]} />);
  await waitFor(() => expect(apiFetch).toHaveBeenCalledWith("/api/v1/me/classes/class1/materials?ids=a,b", expect.any(Object)));
});
