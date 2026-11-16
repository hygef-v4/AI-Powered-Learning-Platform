import { fetchApi } from "@/shared/api/client";
import type { ActivityView, AdminDashboardView, BackupHistoryPage } from "../model/types";

const BASE = "/api/v1/admin/dashboard";

export function loadAdminDashboard(signal?: AbortSignal): Promise<AdminDashboardView> {
  return fetchApi(BASE, { signal, cache: "no-store" });
}

export function loadDashboardActivity(signal?: AbortSignal): Promise<ActivityView> {
  return fetchApi(`${BASE}/activity`, { signal, cache: "no-store" });
}

export function listBackupHistory(
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<BackupHistoryPage> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  return fetchApi(`${BASE}/backups?${params}`, { signal, cache: "no-store" });
}
