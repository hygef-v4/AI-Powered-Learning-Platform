import { fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AdminDashboardPage } from "@/features/admindashboard/pages/AdminDashboardPage";
import { available, dashboardFixture, historyFixture, routes, run, unavailable } from "../model/testFixtures";

afterEach(() => vi.unstubAllGlobals());

it("distinguishes disabled and never-run backup from an unavailable source and links to settings", async () => {
  const dashboard = dashboardFixture();
  dashboard.backup.latest = available(null);
  dashboard.backup.latestSuccessfulAt = available(null);
  vi.stubGlobal("fetch", routes({ dashboard }));
  render(<AdminDashboardPage />);
  expect(await screen.findByText("Sao lưu chưa bật")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Cài đặt sao lưu" })).toHaveAttribute("href", "/admin/settings");
  expect(screen.getByText("Chưa có lần sao lưu nào")).toBeInTheDocument();
  expect(screen.getByText("Chưa có bản sao lưu thành công")).toBeInTheDocument();
});

it("shows schedule, latest failure details and last successful backup independently", async () => {
  const dashboard = dashboardFixture();
  dashboard.backup.schedule = available({ enabled: true, dailyAt: "02:00", timeZone: "Asia/Ho_Chi_Minh", nextPlannedAt: "2026-11-03T19:00:00Z" });
  dashboard.backup.latest = available({ ...run, state: "FAILED", startedAt: "2026-11-02T02:00:00Z", safeError: "Backup storage unavailable" });
  dashboard.backup.latestSuccessfulAt = available("2026-11-01T02:00:00Z");
  dashboard.backup.failuresLast7Days = available({ count: 2, latestSafeError: "Backup storage unavailable" });
  vi.stubGlobal("fetch", routes({ dashboard }));
  render(<AdminDashboardPage />);
  const status = await screen.findByRole("region", { name: "Trạng thái sao lưu" });
  expect(status).toHaveTextContent("02:00");
  expect(status).toHaveTextContent("Asia/Ho_Chi_Minh");
  expect(status).toHaveTextContent("Thất bại");
  expect(status).toHaveTextContent("Backup storage unavailable");
  expect(status).toHaveTextContent("Thành công gần nhất");
  expect(status).toHaveTextContent("Bắt đầu");
  expect(status).toHaveTextContent("Dung lượng");
  expect(status).toHaveTextContent("Thất bại trong 7 ngày: 2");
});

it("keeps unavailable backup metadata explicit without hiding available counts", async () => {
  const dashboard = dashboardFixture();
  dashboard.backup.schedule = unavailable();
  dashboard.backup.latest = unavailable();
  vi.stubGlobal("fetch", routes({ dashboard }));
  render(<AdminDashboardPage />);
  const status = await screen.findByRole("region", { name: "Trạng thái sao lưu" });
  expect(within(status).getAllByText("Không khả dụng").length).toBeGreaterThan(0);
  expect(status).not.toHaveTextContent("Sao lưu chưa bật");
  expect(screen.getByTestId("dashboard-accounts")).toHaveTextContent("3");
});

it("loads backup history on demand, paginates and retries a failed page without fabricating an empty history", async () => {
  const normal = routes();
  let failed = true;
  const fetcher = vi.fn((url: string) => {
    if (url.includes("/backups?page=1&size=20")) {
      return Promise.resolve(failed ? Response.json({}, { status: 503 }) : Response.json(historyFixture(1)));
    }
    return normal(url);
  });
  vi.stubGlobal("fetch", fetcher);
  render(<AdminDashboardPage />);
  fireEvent.click(await screen.findByRole("button", { name: "Xem lịch sử sao lưu" }));
  const history = await screen.findByRole("region", { name: "Lịch sử sao lưu" });
  await within(history).findByRole("table");
  fireEvent.click(within(history).getByRole("button", { name: "Trang sau" }));
  await within(history).findByText("Không tải được lịch sử sao lưu.");
  expect(history).not.toHaveTextContent("Chưa có lịch sử");
  failed = false;
  fireEvent.click(within(history).getByRole("button", { name: "Thử lại" }));
  await within(history).findByText("2026-10-19");
  expect(fetcher).toHaveBeenCalledWith(expect.stringContaining("page=1&size=20"), expect.anything());
});
