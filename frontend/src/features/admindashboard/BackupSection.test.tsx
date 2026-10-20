import {render, screen} from "@testing-library/react";
import {expect, it} from "vitest";
import {BackupSection} from "./BackupSection";
import {available, dashboardFixture, run, unavailable} from "./testFixtures";

it.each([['CREATED', 'Chờ chạy'], ['RUNNING', 'Đang chạy'], ['SUCCESS', 'Thành công'], ['FAILED', 'Lỗi']] as const)("preserves %s backup state", (state, label) => {
  const backup = dashboardFixture().backup;
  backup.latest = available({...run, state});
  backup.recentRuns = available([]);
  render(<BackupSection value={backup} onHistory={() => {}}/>);
  expect(screen.getByText(label)).toBeInTheDocument();
  expect(screen.getByText("Chưa có dữ liệu")).toBeInTheDocument();
});
it("does not assume an old successful backup is purged", () => {
  const backup = dashboardFixture().backup;
  backup.latest = available({...run, scheduledFor: "2020-01-01"});
  backup.recentRuns = available([]);
  render(<BackupSection value={backup} onHistory={() => {}}/>);
  expect(screen.getByText("Thành công")).toBeInTheDocument();
  expect(screen.queryByText("Đã xóa (quá 7 ngày)")).not.toBeInTheDocument();
});
it("only marks a run purged from owner metadata and renders safeError as text", () => {
  const backup = dashboardFixture().backup;
  backup.latest = available({...run, purgedAt: "2026-10-20T03:00:00Z", safeError: "<img src=x onerror=alert(1)>"});
  backup.recentRuns = available([]);
  render(<BackupSection value={backup} onHistory={() => {}}/>);
  expect(screen.getByText("Đã xóa (quá 7 ngày)")).toBeInTheDocument();
  expect(screen.getByText("<img src=x onerror=alert(1)>")).toBeInTheDocument();
  expect(screen.queryByRole("img")).not.toBeInTheDocument();
});
it("distinguishes successful no-history from a provider outage", () => {
  const backup = dashboardFixture().backup;
  backup.latest = available(null);
  backup.recentRuns = unavailable();
  render(<BackupSection value={backup} onHistory={() => {}}/>);
  expect(screen.getByText("Chưa có lần backup nào.")).toBeInTheDocument();
  expect(screen.getByText("Không khả dụng")).toBeInTheDocument();
});
