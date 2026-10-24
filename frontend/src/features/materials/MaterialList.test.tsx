import { act, fireEvent, render, renderHook, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { apiFetch } from "@/shared/api/client";
import { MaterialList } from "./MaterialList";
import { useMaterialResource } from "./useMaterialResource";
import { ClassMaterialsCard, MaterialLinkList } from "./MaterialSections";
import type { Material } from "./types";
vi.mock("@/shared/api/client", async importOriginal => ({ ...await importOriginal<typeof import("@/shared/api/client")>(), apiFetch: vi.fn() }));
const row = (id: string, changes: Partial<Material> = {}): Material => ({ id, scope: "SUBJECT", subjectId: "subject", classId: null, kind: "FILE", title: `Document ${id}`, description: "", orderNo: 0, status: "ACTIVE", currentVersionNo: 1, version: 2, updatedAt: "2026-10-21T00:00:00Z", canManage: true, versions: [], current: { versionNo: 1, fileName: "doc.pdf", mimeType: "application/pdf", byteSize: 1000, videoUrl: null, videoId: null, textStatus: "EXTRACTED", createdBy: "actor", createdAt: "2026-10-21T00:00:00Z" }, ...changes });
beforeEach(() => { vi.mocked(apiFetch).mockReset(); });
afterEach(() => vi.useRealTimers());
it("refreshes pending extraction without user actions and stops after completion", async () => {
  vi.useFakeTimers();
  const pending=row("a");pending.current={...pending.current,textStatus:"PENDING"};
  vi.mocked(apiFetch).mockResolvedValueOnce([pending]).mockResolvedValue([row("a")]);
  const view=render(<MaterialList scope="subjects" scopeId="subject" />);
  await act(async()=>{});
  expect(screen.getByText("Đang xử lý")).toBeInTheDocument();
  await act(async()=>{await vi.advanceTimersByTimeAsync(5000);});
  expect(screen.getByText("Đã trích")).toBeInTheDocument();
  await act(async()=>{await vi.advanceTimersByTimeAsync(15000);});
  expect(apiFetch).toHaveBeenCalledTimes(2);
  view.unmount();
});
it("cancels pending extraction refresh when leaving the list", async () => {
  vi.useFakeTimers();const pending=row("a");pending.current={...pending.current,textStatus:"PENDING"};
  vi.mocked(apiFetch).mockResolvedValue([pending]);
  const view=render(<MaterialList scope="subjects" scopeId="subject" />);await act(async()=>{});view.unmount();
  await vi.advanceTimersByTimeAsync(10000);expect(apiFetch).toHaveBeenCalledOnce();
});
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
it("keeps current rows visible and does not overlap slow extraction refreshes", async () => {
  const pending = row("pending"); pending.current = { ...pending.current, textStatus: "PENDING" };
  let finish: (value: Material[]) => void = () => {};
  vi.mocked(apiFetch).mockResolvedValueOnce([pending]).mockImplementationOnce(() => new Promise(resolve => { finish = resolve; }));
  vi.useFakeTimers();
  const view = render(<MaterialList scope="subjects" scopeId="subject" />);
  await act(async () => {});
  await act(async () => { await vi.advanceTimersByTimeAsync(5000); });
  expect(apiFetch).toHaveBeenCalledTimes(2);
  expect(screen.getByText("Document pending")).toBeInTheDocument();
  expect(screen.getByText("Đang xử lý")).toBeInTheDocument();
  await act(async () => { await vi.advanceTimersByTimeAsync(20000); });
  expect(apiFetch).toHaveBeenCalledTimes(2);
  await act(async () => finish([row("pending")]));
  expect(screen.getByText("Đã trích")).toBeInTheDocument();
  view.unmount();
});
it("preserves rows on a refresh error and stops polling until an explicit retry", async () => {
  const pending = row("pending"); pending.current = { ...pending.current, textStatus: "PENDING" };
  vi.mocked(apiFetch).mockResolvedValueOnce([pending]).mockRejectedValueOnce(new Error("offline"));
  vi.useFakeTimers();
  const view = render(<MaterialList scope="subjects" scopeId="subject" />);
  await act(async () => {});
  await act(async () => { await vi.advanceTimersByTimeAsync(5000); });
  expect(screen.getByRole("alert")).toBeInTheDocument();
  expect(screen.getByText("Document pending")).toBeInTheDocument();
  await act(async () => { await vi.advanceTimersByTimeAsync(20000); });
  expect(apiFetch).toHaveBeenCalledTimes(2);
  vi.mocked(apiFetch).mockResolvedValueOnce([row("pending")]);
  fireEvent.click(screen.getByRole("button", { name: "Tải lại" }));
  await act(async () => {});
  expect(screen.getByText("Đã trích")).toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  view.unmount();
});
it("does not poll archived material with pending extraction", async () => {
  const archived = row("old", { status: "ARCHIVED", canManage: false });
  archived.current = { ...archived.current, textStatus: "PENDING" };
  vi.mocked(apiFetch).mockResolvedValue([archived]);
  vi.useFakeTimers();
  const view = render(<MaterialList scope="subjects" scopeId="subject" />);
  await act(async () => {});
  await act(async () => { await vi.advanceTimersByTimeAsync(20000); });
  expect(apiFetch).toHaveBeenCalledOnce();
  view.unmount();
});
it("pauses and resumes refresh while keeping the reload callback stable", async () => {
  const pending = row("a"); pending.current = { ...pending.current, textStatus: "PENDING" };
  vi.mocked(apiFetch).mockResolvedValueOnce([pending]).mockResolvedValue([row("a")]);
  vi.useFakeTimers();
  const pollWhile = (rows: Material[]) => rows.some(item => item.current.textStatus === "PENDING");
  const view = renderHook(({ paused }) => useMaterialResource<Material[]>("/materials", { pollWhile, pollPaused: paused }), { initialProps: { paused: false } });
  await act(async () => {});
  const reload = view.result.current.reload;
  await act(async () => { await vi.advanceTimersByTimeAsync(4000); });
  view.rerender({ paused: true });
  await act(async () => { await vi.advanceTimersByTimeAsync(20000); });
  expect(apiFetch).toHaveBeenCalledOnce();
  view.rerender({ paused: false });
  await act(async () => { await vi.advanceTimersByTimeAsync(5000); });
  expect(apiFetch).toHaveBeenCalledTimes(2);
  expect(view.result.current.data?.[0]?.current.textStatus).toBe("EXTRACTED");
  expect(view.result.current.reload).toBe(reload);
  view.unmount();
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
