import { act, fireEvent, render, renderHook, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { apiFetch } from "@/shared/api/client";
import { ModuleBoard } from "@/features/materials/components/ModuleBoard";
import { ClassMaterialsCard, MaterialLinkList } from "@/features/materials/components/MaterialSections";
import { useMaterialResource } from "@/features/materials/hooks/useMaterialResource";
import type { Material, MaterialModule } from "@/features/materials/model/types";
vi.mock("@/shared/api/client", async importOriginal => ({ ...await importOriginal<typeof import("@/shared/api/client")>(), apiFetch: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

const row = (id: string, changes: Partial<Material> = {}): Material => ({ id, scope: "SUBJECT", subjectId: "subject", classId: null, moduleId: "m1", kind: "FILE", title: `Document ${id}`, description: "", orderNo: 0, status: "ACTIVE", currentVersionNo: 1, version: 2, updatedAt: "2026-10-21T00:00:00Z", canManage: true, versions: [], current: { versionNo: 1, fileName: "doc.pdf", mimeType: "application/pdf", byteSize: 1000, videoUrl: null, videoId: null, textStatus: "EXTRACTED", createdBy: "actor", createdAt: "2026-10-21T00:00:00Z" }, ...changes });
const mod = (id: string, title: string, changes: Partial<MaterialModule> = {}): MaterialModule => ({ id, title, orderNo: 0, version: 0, canManage: true, quizzes: [], ...changes });

function backend(modules: MaterialModule[], materials: Material[]) {
  vi.mocked(apiFetch).mockImplementation(async (path, init) => {
    if (init?.method && init.method !== "GET") return undefined;
    if (String(path).includes("/files/")) return [];
    return String(path).includes("/modules") ? modules : materials;
  });
}
beforeEach(() => { vi.mocked(apiFetch).mockReset(); });
afterEach(() => vi.useRealTimers());

it("groups active materials and quizzes under their modules", async () => {
  backend([mod("m1", "Module 1", { quizzes: [{ quizId: "q1", title: "Quiz chương 1", status: "OPEN", scope: "SUBJECT", classId: null, canManage: true }] }), mod("m2", "Module 2")],
    [row("a"), row("b", { moduleId: "m2" }), row("old", { status: "ARCHIVED" })]);
  render(<ModuleBoard scope={{ kind: "subject", subjectId: "subject" }} header />);
  const first = await screen.findByRole("region", { name: "Module 1" });
  expect(within(first).getByText("Document a")).toBeInTheDocument();
  expect(within(first).getByText("Quiz chương 1")).toHaveAttribute("href", "/quizzes/q1");
  expect(within(screen.getByRole("region", { name: "Module 2" })).getByText("Document b")).toHaveAttribute("href", "/materials/b/view");
  expect(screen.queryByText("Document old")).not.toBeInTheDocument();
});

it("the chevron at the end of a module header collapses and reopens it", async () => {
  backend([mod("m1", "Module 1")], [row("a")]);
  render(<ModuleBoard scope={{ kind: "subject", subjectId: "subject" }} header />);
  const toggle = await screen.findByTestId("module-toggle-m1");
  expect(toggle).toHaveAttribute("aria-expanded", "true");
  fireEvent.click(toggle);
  expect(toggle).toHaveAttribute("aria-expanded", "false");
  expect(document.getElementById("module-body-m1")).toHaveAttribute("inert");
  fireEvent.click(screen.getByRole("heading", { name: "Module 1" }));
  expect(toggle).toHaveAttribute("aria-expanded", "true");
  expect(document.getElementById("module-body-m1")).not.toHaveAttribute("inert");
});

it("students open materials and quizzes in the class context without management controls", async () => {
  backend([mod("m1", "Module 1", { canManage: false, quizzes: [{ quizId: "q1", title: "Quiz", status: "OPEN", scope: "SUBJECT", classId: null, canManage: false }] })], [row("a", { canManage: false })]);
  render(<ClassMaterialsCard classId="class1" />);
  expect(await screen.findByText("Document a")).toHaveAttribute("href", "/materials/a/view?classId=class1");
  expect(screen.getByText("Quiz")).toHaveAttribute("href", "/quiz-practice/q1?classId=class1");
  expect(screen.queryByRole("button", { name: /Tải tệp/ })).not.toBeInTheDocument();
  expect(apiFetch).toHaveBeenCalledWith("/api/v1/me/classes/class1/modules", expect.any(Object));
});

it("creates a module from the page header", async () => {
  backend([], []);
  render(<ModuleBoard scope={{ kind: "subject", subjectId: "subject" }} header />);
  fireEvent.click(await screen.findByTestId("module-create"));
  fireEvent.change(screen.getByTestId("module-name-input"), { target: { value: "Use case" } });
  fireEvent.click(screen.getByTestId("module-name-save"));
  await waitFor(() => expect(apiFetch).toHaveBeenCalledWith("/api/v1/subjects/subject/modules", expect.objectContaining({ method: "POST", body: JSON.stringify({ title: "Use case" }) })));
});

it("read-only subjects hide add and module controls", async () => {
  backend([mod("m1", "Module 1")], [row("a")]);
  render(<ModuleBoard scope={{ kind: "subject", subjectId: "subject" }} header readOnly />);
  await screen.findByText("Document a");
  expect(screen.queryByTestId("module-create")).not.toBeInTheDocument();
  expect(screen.queryByTestId("module-add-file-m1")).not.toBeInTheDocument();
});

it("adds a YouTube link into the chosen module", async () => {
  backend([mod("m1", "Module 1")], []);
  render(<ModuleBoard scope={{ kind: "class", classId: "class1" }} />);
  fireEvent.click(await screen.findByTestId("module-add-m1"));
  fireEvent.click(screen.getByRole("radio", { name: "YouTube" }));
  fireEvent.change(screen.getByTestId("material-source-title"), { target: { value: "Video" } });
  fireEvent.change(screen.getByTestId("material-source-url"), { target: { value: "https://youtu.be/abcdefghijk" } });
  fireEvent.click(screen.getByTestId("material-source-save"));
  await waitFor(() => expect(apiFetch).toHaveBeenCalledWith("/api/v1/classes/class1/materials", expect.objectContaining({ method: "POST" })));
  const body = JSON.parse(String(vi.mocked(apiFetch).mock.calls.find(([, init]) => init?.method === "POST")![1]!.body));
  expect(body.moduleId).toBe("m1");
});

it("refreshes pending extraction without user actions and stops after completion", async () => {
  vi.useFakeTimers();
  const pending = row("a"); pending.current = { ...pending.current, textStatus: "PENDING" };
  let calls = 0;
  vi.mocked(apiFetch).mockImplementation(async (path) => String(path).includes("/modules") ? [mod("m1", "Module 1")] : (calls++ === 0 ? [pending] : [row("a")]));
  const view = render(<ModuleBoard scope={{ kind: "subject", subjectId: "subject" }} />);
  await act(async () => {});
  await act(async () => { await vi.advanceTimersByTimeAsync(5000); });
  await act(async () => { await vi.advanceTimersByTimeAsync(15000); });
  expect(calls).toBe(2);
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

it("empty attachment slot makes no request", () => {
  render(<MaterialLinkList classId="class1" materialIds={[]} />); expect(apiFetch).not.toHaveBeenCalled(); expect(screen.queryByText("Học liệu gắn kèm")).not.toBeInTheDocument();
});

it("attachment requests constrain IDs and class", async () => {
  vi.mocked(apiFetch).mockResolvedValue([]); render(<MaterialLinkList classId="class1" materialIds={["a", "b"]} />);
  await waitFor(() => expect(apiFetch).toHaveBeenCalledWith("/api/v1/me/classes/class1/materials?ids=a,b", expect.any(Object)));
});
