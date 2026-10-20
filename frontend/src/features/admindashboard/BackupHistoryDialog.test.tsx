import {act, fireEvent, render, screen, waitFor, within} from "@testing-library/react";
import {afterEach, expect, it, vi} from "vitest";
import {BackupHistoryDialog} from "./BackupHistoryDialog";
import {historyFixture} from "./testFixtures";
afterEach(() => vi.unstubAllGlobals());

it("pages history from zero, distinguishes missing start time and supports Escape", async () => {
  const close = vi.fn();
  const request = vi.fn((url: string) => Promise.resolve(Response.json(historyFixture(url.includes("page=1") ? 1 : 0))));
  vi.stubGlobal("fetch", request);
  render(<BackupHistoryDialog onClose={close}/>);
  await waitFor(() => expect(within(screen.getByRole("dialog")).getByText("20/10/2026")).toBeInTheDocument());
  expect(screen.getByText("Chưa có dữ liệu")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", {name: "Trang sau"}));
  expect(await screen.findByText("19/10/2026")).toBeInTheDocument();
  expect(request).toHaveBeenLastCalledWith("/api/v1/admin/dashboard/backups?page=1&size=20", expect.anything());
  fireEvent.keyDown(document, {key: "Escape"});
  expect(close).toHaveBeenCalledOnce();
});

it("keeps failed history distinct from empty history and retries", async () => {
  let failed = true;
  vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(failed ? Response.json({}, {status: 503}) : Response.json({items: [], page: 0, size: 20, totalElements: 0, totalPages: 0}))));
  render(<BackupHistoryDialog onClose={() => {}}/>);
  expect(await screen.findByRole("alert")).toHaveTextContent("Không tải được");
  expect(screen.queryByText("Chưa có lần backup nào.")).not.toBeInTheDocument();
  failed = false;
  fireEvent.click(screen.getByRole("button", {name: "Thử lại"}));
  expect(await screen.findByText("Chưa có lần backup nào.")).toBeInTheDocument();
});

it("aborts a closed dialog request and a reopened dialog ignores its late response", async () => {
  let resolve!: (response: Response) => void;
  let signal: AbortSignal | undefined;
  let calls = 0;
  vi.stubGlobal("fetch", vi.fn((_url: string, init: RequestInit) => {
    if (++calls > 1) return Promise.resolve(Response.json(historyFixture(1)));
    signal = init.signal as AbortSignal;
    return new Promise<Response>(done => { resolve = done; });
  }));
  const old = render(<BackupHistoryDialog onClose={() => {}}/>);
  await waitFor(() => expect(signal).toBeDefined());
  old.unmount();
  expect(signal!.aborted).toBe(true);
  render(<BackupHistoryDialog onClose={() => {}}/>);
  expect(await screen.findByText("19/10/2026")).toBeInTheDocument();
  await act(async () => resolve(Response.json(historyFixture(0))));
  expect(screen.queryByText("20/10/2026")).not.toBeInTheDocument();
});
