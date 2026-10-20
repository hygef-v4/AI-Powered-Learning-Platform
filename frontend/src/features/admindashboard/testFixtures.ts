import type {AdminDashboardView, BackupHistoryPage, BackupRunItem, Section} from "./types";
export const available = <T,>(data: T): Section<T> => ({availability: "AVAILABLE", data});
export const unavailable = <T,>(): Section<T> => ({availability: "UNAVAILABLE", data: null});
export const run: BackupRunItem = {
  scheduledFor: "2026-10-20", state: "SUCCESS", snapshotAt: "2026-10-20T02:00:00Z",
  startedAt: null, completedAt: "2026-10-20T02:01:00Z", byteSize: 1024, safeError: null, purgedAt: null,
};
export function dashboardFixture(): AdminDashboardView {
  return {
    calculatedAt: "2026-10-20T03:00:00Z",
    accounts: available({total: 5, activeTotal: 3, byRoleAndStatus: {
      ADMIN: {ACTIVE: 0, PENDING: 0, DISABLED: 0}, SUBJECT_MANAGER: {ACTIVE: 0, PENDING: 0, DISABLED: 0},
      TEACHER: {ACTIVE: 0, PENDING: 2, DISABLED: 0}, STUDENT: {ACTIVE: 3, PENDING: 0, DISABLED: 0},
    }}),
    subjects: available({ACTIVE: 4, ARCHIVED: 0}), classes: available({DRAFT: 1, OPEN: 2, ARCHIVED: 1}),
    activeEnrollments: available(7), backup: {
      latest: available(run), schedule: available({enabled: false, dailyAt: null, timeZone: "Asia/Ho_Chi_Minh", nextPlannedAt: null}),
      recentRuns: available([run]), latestSuccessfulAt: unavailable(), failuresLast7Days: unavailable(),
    },
  };
}
export function historyFixture(page = 0): BackupHistoryPage {
  return {items: [{...run, scheduledFor: page === 0 ? "2026-10-20" : "2026-10-19"}], page, size: 20, totalElements: 21, totalPages: 2};
}
