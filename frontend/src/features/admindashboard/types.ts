import type {Role} from "@/shared/auth/roles";

export type Section<T> = {availability: "AVAILABLE"; data: T} | {availability: "UNAVAILABLE"; data: null};
export type AccountStatus = "PENDING" | "ACTIVE" | "DISABLED";
export type SubjectStatus = "ACTIVE" | "ARCHIVED";
export type ClassStatus = "DRAFT" | "OPEN" | "ARCHIVED";
export type AccountCounts = {byRoleAndStatus: Record<Role, Record<AccountStatus, number>>; total: number; activeTotal: number};
export type BackupRunItem = {
  scheduledFor: string;
  state: "CREATED" | "RUNNING" | "SUCCESS" | "FAILED";
  snapshotAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  byteSize: number | null;
  safeError: string | null;
  purgedAt: string | null;
};
export type BackupSchedule = {enabled: boolean; dailyAt: string | null; timeZone: string; nextPlannedAt: string | null};
export type BackupSectionView = {
  latest: Section<BackupRunItem | null>;
  schedule: Section<BackupSchedule>;
  recentRuns: Section<BackupRunItem[]>;
  latestSuccessfulAt: Section<string>;
  failuresLast7Days: Section<{count: number; latestSafeError: string | null}>;
};
export type AdminDashboardView = {
  calculatedAt: string;
  accounts: Section<AccountCounts>;
  subjects: Section<Record<SubjectStatus, number>>;
  classes: Section<Record<ClassStatus, number>>;
  activeEnrollments: Section<number>;
  backup: BackupSectionView;
};
export type BackupHistoryPage = {items: BackupRunItem[]; page: number; size: number; totalElements: number; totalPages: number};
