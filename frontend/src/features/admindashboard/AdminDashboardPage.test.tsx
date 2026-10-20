import {act, fireEvent, render, screen, waitFor, within} from "@testing-library/react";
import {afterEach, expect, it, vi} from "vitest";
import {AdminDashboardPage} from "./AdminDashboardPage";
import {dashboardFixture, historyFixture, unavailable} from "./testFixtures";
afterEach(() => vi.unstubAllGlobals());

it("shows real counts, independent unavailable cards and explicit backup gaps", async () => {
  const data = dashboardFixture();
  data.subjects = unavailable();
  vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(Response.json(data))));
  render(<AdminDashboardPage/>);
  expect(screen.getByRole("status")).toHaveTextContent("Đang tải");
  expect(await screen.findByRole("heading", {name: "Tổng quan hệ thống"})).toBeInTheDocument();
  await waitFor(() => expect(within(screen.getByTestId("dashboard-accounts")).getByText("5")).toBeInTheDocument());
  expect(within(screen.getByTestId("dashboard-subjects")).getByText("Không khả dụng")).toBeInTheDocument();
  expect(screen.getByTestId("backup-latest-success")).toHaveTextContent("Chưa khả dụng");
  expect(screen.getByTestId("backup-failures")).toHaveTextContent("Chưa khả dụng");
  expect(screen.getByTestId("backup-schedule")).toHaveTextContent("Chưa bật");
  expect(screen.getByRole("link", {name: "Cài đặt backup"})).toHaveAttribute("href", "/admin/settings");
  expect(screen.queryByRole("button", {name: /Khôi phục|Tải backup|Chạy backup/})).not.toBeInTheDocument();
});

it("clears old metrics when refresh is denied and never displays raw provider details", async () => {
  let denied = false;
  vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(denied ? Response.json({detail: "secret SQL"}, {status: 403}) : Response.json(dashboardFixture()))));
  render(<AdminDashboardPage/>);
  await waitFor(() => expect(screen.getByTestId("dashboard-accounts")).toHaveTextContent("5"));
  denied = true;
  fireEvent.click(screen.getByRole("button", {name: "Tải lại số liệu"}));
  expect(await screen.findByRole("alert")).toHaveTextContent("Không có quyền");
  expect(screen.queryByTestId("dashboard-accounts")).not.toBeInTheDocument();
  expect(screen.queryByText("secret SQL")).not.toBeInTheDocument();
});

it("retries failures and fetches again on remount instead of caching counts", async () => {
  let failing = true;
  const request = vi.fn(() => Promise.resolve(failing ? Response.json({}, {status: 503}) : Response.json(dashboardFixture())));
  vi.stubGlobal("fetch", request);
  const view = render(<AdminDashboardPage/>);
  expect(await screen.findByRole("alert")).toHaveTextContent("Không tải được");
  failing = false;
  fireEvent.click(screen.getByRole("button", {name: "Thử lại"}));
  await screen.findByTestId("dashboard-accounts");
  view.unmount();
  render(<AdminDashboardPage/>);
  await screen.findByTestId("dashboard-accounts");
  expect(request).toHaveBeenCalledTimes(3);
});

it("aborts a pending read when unmounted", async () => {
  let resolve!: (response: Response) => void;
  let signal: AbortSignal | undefined;
  vi.stubGlobal("fetch", vi.fn((_url: string, init: RequestInit) => {
    signal = init.signal as AbortSignal;
    return new Promise<Response>(done => { resolve = done; });
  }));
  const view = render(<AdminDashboardPage/>);
  await waitFor(() => expect(signal).toBeDefined());
  view.unmount();
  expect(signal!.aborted).toBe(true);
  await act(async () => resolve(Response.json(dashboardFixture())));
});

it("returns keyboard focus to the history trigger after Escape", async () => {
  vi.stubGlobal("fetch", vi.fn((url: string) => Promise.resolve(Response.json(
    url.includes("/backups?") ? historyFixture(0) : dashboardFixture()))));
  render(<AdminDashboardPage/>);
  const trigger = await screen.findByRole("button", {name: "Lịch sử backup"});
  trigger.focus();
  fireEvent.click(trigger);
  const dialog = await screen.findByRole("dialog");
  expect(dialog).toHaveFocus();
  fireEvent.keyDown(document, {key: "Tab"});
  expect(within(dialog).getByRole("button", {name: "Đóng"})).toHaveFocus();
  fireEvent.keyDown(document, {key: "Escape"});
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(trigger).toHaveFocus();
});

it.each([401, 403])("clears dashboard metrics when history rejects the session with %s", async status => {
  vi.stubGlobal("fetch", vi.fn((url: string) => Promise.resolve(url.includes("/backups?")
    ? Response.json({detail: "secret SQL"}, {status}) : Response.json(dashboardFixture()))));
  render(<AdminDashboardPage/>);
  fireEvent.click(await screen.findByRole("button", {name: "Lịch sử backup"}));
  expect(await screen.findByRole("alert")).toHaveTextContent("Không có quyền");
  expect(screen.queryByTestId("dashboard-accounts")).not.toBeInTheDocument();
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(screen.queryByText("secret SQL")).not.toBeInTheDocument();
});
