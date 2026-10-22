import {render, screen} from "@testing-library/react";
import {expect, it} from "vitest";
import {BackupSection} from "./BackupSection";
import {available, dashboardFixture, run, unavailable} from "./testFixtures";

it("successfulNullIsNotProviderOutage", () => {
  const backup = dashboardFixture().backup;
  const neverSucceeded = {...backup, latestSuccessfulAt: available(null), failuresLast7Days: available({count: 0, latestSafeError: null})};
  const {rerender} = render(<BackupSection value={neverSucceeded} onHistory={() => {}}/>);
  expect(screen.getByTestId("backup-latest-success")).toHaveTextContent("Chưa có backup thành công.");
  expect(screen.getByTestId("backup-failures")).toHaveTextContent("0");
  rerender(<BackupSection value={backup} onHistory={() => {}}/>);
  expect(screen.getByTestId("backup-latest-success")).toHaveTextContent("Không khả dụng");
});

it("shows successful completion time in Vietnam time and failure text without HTML", () => {
  const backup = dashboardFixture().backup;
  backup.latestSuccessfulAt = available("2026-10-22T03:04:00Z");
  backup.failuresLast7Days = available({count: 2, latestSafeError: "<img src=x onerror=alert(1)>"});
  render(<BackupSection value={backup} onHistory={() => {}}/>);
  expect(screen.getByTestId("backup-latest-success")).toHaveTextContent("10:04");
  expect(screen.getByTestId("backup-failures")).toHaveTextContent("2");
  expect(screen.getByTestId("backup-failures")).toHaveTextContent("<img src=x onerror=alert(1)>");
  expect(screen.queryByRole("img")).not.toBeInTheDocument();
});

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
  expect(screen.getAllByText("Không khả dụng")).toHaveLength(3);
});
