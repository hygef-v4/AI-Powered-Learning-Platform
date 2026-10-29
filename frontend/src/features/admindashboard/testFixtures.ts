import type {AdminDashboardView, BackupHistoryPage, BackupRunItem, Section} from "./types";
import {vi} from "vitest";
import type {UsageSummary} from "@/features/ai/admin/api";
import type {ActivityView} from "./types";
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

export function activityFixture(): ActivityView {
  return {
    calculatedAt: "2026-10-20T03:00:00Z",
    activeAccountChangeToday: available(-2),
    assignmentsByDay: available(["14", "15", "16", "17", "18", "19", "20"].map((d, i) => ({
      day: `2026-10-${d}`, individual: i === 6 ? 3 : 1, group: i === 6 ? 2 : 0, practice: 0,
    }))),
    securityEvents: available([
      {occurredAt: "2026-10-20T02:30:00Z", action: "ACCOUNT_TEMP_LOCKED", result: "DENIED", account: "gv03@fpt.edu.vn", reason: "TOO_MANY_FAILURES"},
      {occurredAt: "2026-10-19T08:00:00Z", action: "LOGIN_FAILED", result: "FAILURE", account: null, reason: "UNKNOWN_EMAIL"},
      {occurredAt: "2026-10-18T08:00:00Z", action: "CLASS_VIEW", result: "DENIED", account: "sv01@fpt.edu.vn", reason: "ROLE_DENIED"},
    ]),
  };
}

export function usageFixture(): UsageSummary {
  return {
    days: 7, todayTokens: 182000, globalDayTokens: 500000, periodTokens: 900000, teachersUsed: 3,
    byDay: ["14", "15", "16", "17", "18", "19", "20"].map((d, i) => ({day: `2026-10-${d}`, tokens: (i + 1) * 1000})),
    byTeacher: [], byModel: [],
  };
}

type Sources = {dashboard?: unknown; activity?: unknown; usage?: unknown; backups?: unknown};

/** fetch giả theo đường dẫn; nguồn nào là Response thì trả nguyên, không có thì dùng dữ liệu mẫu. */
export function routes(sources: Sources = {}) {
  const pick = (value: unknown, fallback: unknown) => {
    const chosen = value === undefined ? fallback : value;
    return chosen instanceof Response ? chosen.clone() : Response.json(chosen);
  };
  return vi.fn((url: string) => Promise.resolve(
    url.includes("/dashboard/activity") ? pick(sources.activity, activityFixture())
      : url.includes("/usage-summary") ? pick(sources.usage, usageFixture())
        : url.includes("/dashboard/backups") ? pick(sources.backups, historyFixture(0))
          : pick(sources.dashboard, dashboardFixture())));
}
