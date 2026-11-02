import {act, fireEvent, render, screen, waitFor} from "@testing-library/react";
import {afterEach, expect, it, vi} from "vitest";
import {ClassDetailRoute, ClassesRoute} from "@/features/academics/pages/routes";
import type {CourseClassView} from "@/features/academics/model/types";

vi.mock("next/navigation", () => ({useRouter: () => ({push: vi.fn()}), useSearchParams: () => new URLSearchParams(window.location.search)}));
afterEach(() => vi.unstubAllGlobals());

function course(id = "c"): CourseClassView {
  return {
    id, subject: {id: "s", code: "S", name: "Subject"}, code: id,
    name: `Class ${id}`, description: "", term: "1", status: "DRAFT",
    teacher: {id: "t", name: "Teacher", email: "t@example.test"},
    version: 1, activeStudentCount: 0, removedStudentCount: 0,
    showGradeDistribution: false, createdAt: "2026-01-01",
  };
}

function deferredResponse() {
  let resolve!: (response: Response) => void;
  const promise = new Promise<Response>(done => { resolve = done; });
  return {promise, resolve};
}

it("ignores the old route response even when HTTP completes after cancellation", async () => {
  const oldResponse = deferredResponse();
  let oldSignal: AbortSignal | undefined;
  vi.stubGlobal("fetch", vi.fn((url: string, init: RequestInit) => {
    if (url === "/api/v1/manager/classes/old") {
      oldSignal = init.signal as AbortSignal;
      return oldResponse.promise;
    }
    return Promise.resolve(Response.json(url.includes("/students?") ? [] : course("new")));
  }));
  const view = render(<ClassDetailRoute scope="manager" id="old"/>);
  await waitFor(() => expect(oldSignal).toBeDefined());
  view.rerender(<ClassDetailRoute scope="manager" id="new"/>);
  expect(await screen.findByLabelText(/Tên lớp/)).toHaveValue("Class new");
  expect(oldSignal!.aborted).toBe(true);
  await act(async () => { oldResponse.resolve(Response.json(course("old"))); });
  expect(screen.getByLabelText(/Tên lớp/)).toHaveValue("Class new");
});

it("cancels an in-flight refresh when the detail route unmounts", async () => {
  const refreshing = deferredResponse();
  let detailReads = 0;
  let refreshSignal: AbortSignal | undefined;
  vi.stubGlobal("fetch", vi.fn((url: string, init: RequestInit) => {
    if (init.method === "PATCH") return Promise.resolve(Response.json(course()));
    if (url.includes("/students?")) return Promise.resolve(Response.json([]));
    if (++detailReads === 1) return Promise.resolve(Response.json(course()));
    refreshSignal = init.signal as AbortSignal;
    return refreshing.promise;
  }));
  const view = render(<ClassDetailRoute scope="manager" id="c"/>);
  fireEvent.change(await screen.findByLabelText(/Tên lớp/), {target: {value: "Edited"}});
  fireEvent.click(screen.getByTestId("class-info-save"));
  await waitFor(() => expect(refreshSignal).toBeDefined());
  view.unmount();
  expect(refreshSignal!.aborted).toBe(true);
  await act(async () => { refreshing.resolve(Response.json(course())); });
});

it("preserves the failed-read screen after a successful write and retries only the read", async () => {
  let saved = false;
  let readAvailable = false;
  const request = vi.fn((url: string, init: RequestInit) => {
    if (init.method === "PATCH") {
      saved = true;
      return Promise.resolve(Response.json({...course(), version: 2}));
    }
    if (url.includes("/students?")) return Promise.resolve(Response.json([]));
    if (saved && !readAvailable) return Promise.resolve(Response.json({}, {status: 503}));
    return Promise.resolve(Response.json({...course(), name: saved ? "Saved remotely" : "Original"}));
  });
  vi.stubGlobal("fetch", request);
  render(<ClassDetailRoute scope="manager" id="c"/>);
  fireEvent.change(await screen.findByLabelText(/Tên lớp/), {target: {value: "Local edit"}});
  fireEvent.click(screen.getByTestId("class-info-save"));
  expect(await screen.findByRole("alert")).toHaveTextContent("Không thể hoàn tất thao tác");
  expect(screen.queryByLabelText(/Tên lớp/)).not.toBeInTheDocument();
  readAvailable = true;
  fireEvent.click(screen.getByRole("button", {name: "Tải lại trang"}));
  expect(await screen.findByLabelText(/Tên lớp/)).toHaveValue("Saved remotely");
  expect(request.mock.calls.filter(([, init]) => init.method === "PATCH")).toHaveLength(1);
});

it("keeps string and object reopen conflicts visible after refreshing the class", async () => {
  const archived = {...course(), status: "ARCHIVED"};
  vi.stubGlobal("fetch", vi.fn((url: string, init: RequestInit) => Promise.resolve(
    init.method === "PUT"
      ? Response.json({conflictingStudents: ["student-1", {accountId: "student-2"}]}, {status: 409})
      : Response.json(url.includes("/students?") ? [] : archived),
  )));
  render(<ClassDetailRoute scope="manager" id="c"/>);
  fireEvent.click(await screen.findByTestId("class-state-open"));
  fireEvent.click(screen.getByTestId("class-state-confirm"));
  expect(await screen.findByText("student-1")).toBeInTheDocument();
  expect(screen.getByText('{"accountId":"student-2"}')).toBeInTheDocument();
  expect(screen.getByTestId("class-mutation-conflict")).toHaveTextContent("Dữ liệu đã thay đổi");
  expect(screen.getByTestId("class-state-confirm")).toBeEnabled();
});

it("retains all class pages and the existing first-page refetch for a full capability page", async () => {
  const firstPage = Array.from({length: 100}, (_, index) => ({...course(`c${index}`), status: "OPEN"}));
  const request = vi.fn((url: string) => Promise.resolve(Response.json(
    url.includes("page=2") ? [{...course("last-class"), status: "OPEN"}] : firstPage,
  )));
  vi.stubGlobal("fetch", request);
  render(<ClassesRoute teaching/>);
  expect(await screen.findByTestId("dashboard-open-last-class")).toBeInTheDocument();
  expect(screen.getAllByRole("button", {name: /Xem lớp/})).toHaveLength(101);
  expect(request.mock.calls.map(([url]) => url)).toEqual([
    "/api/v1/teaching/classes?page=1&size=100",
    "/api/v1/teaching/classes?page=1&size=100",
    "/api/v1/teaching/classes?page=2&size=100",
  ]);
});
